package com.restauranthub.service;

import com.restauranthub.dto.TableReservationRequestDto;
import com.restauranthub.dto.TableReservationResponseDto;
import com.restauranthub.entity.DiningTable;
import com.restauranthub.entity.RestaurantBranch;
import com.restauranthub.entity.TableReservation;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.ReservationStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.BookingConflictException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.exception.UnauthorizedException;
import com.restauranthub.repository.DiningTableRepository;
import com.restauranthub.repository.RestaurantBranchRepository;
import com.restauranthub.repository.TableReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableReservationService {

    private static final Logger log = LoggerFactory.getLogger(TableReservationService.class);

    private final TableReservationRepository reservationRepository;
    private final DiningTableRepository tableRepository;
    private final RestaurantBranchRepository branchRepository;
    private final BranchService branchService;

    @Value("${app.reservation.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Value("${app.reservation.table-buffer-minutes:15}")
    private int bufferMinutes;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TableReservationResponseDto reserveTable(User user, TableReservationRequestDto request) {
        validateReservationTiming(request.getReservationDate(), request.getStartTime(), request.getEndTime());

        RestaurantBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", request.getBranchId()));

        if (!branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), request.getStartTime()) ||
            !branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), request.getEndTime())) {
            throw new BadRequestException("BRANCH_CLOSED", "The restaurant branch is closed during the requested date and time slot.");
        }

        LocalTime bufferedStartTime = request.getStartTime().minusMinutes(bufferMinutes);
        LocalTime bufferedEndTime = request.getEndTime().plusMinutes(bufferMinutes);
        Instant now = Instant.now();

        DiningTable selectedTable;

        if (request.getTableId() != null) {
            // Explicit table selection: Acquire pessimistic write lock on the target table
            selectedTable = tableRepository.findByIdWithLock(request.getTableId())
                    .orElseThrow(() -> new ResourceNotFoundException("DiningTable", "id", request.getTableId()));

            if (!selectedTable.getBranch().getId().equals(branch.getId())) {
                throw new BadRequestException("INVALID_TABLE", "The selected table does not belong to the specified branch.");
            }
            if (!selectedTable.isActive()) {
                throw new BadRequestException("TABLE_INACTIVE", "The selected table is currently inactive.");
            }
            if (request.getGuestCount() > selectedTable.getSeatingCapacity()) {
                throw new BadRequestException("CAPACITY_EXCEEDED",
                        String.format("Guest count (%d) exceeds table capacity (%d)",
                                request.getGuestCount(), selectedTable.getSeatingCapacity()));
            }

            List<TableReservation> conflicts = reservationRepository.findConflictingReservations(
                    selectedTable.getId(), request.getReservationDate(), bufferedStartTime, bufferedEndTime, now
            );

            if (!conflicts.isEmpty()) {
                throw new BookingConflictException("Table", selectedTable.getId(),
                        request.getReservationDate() + " " + request.getStartTime() + " - " + request.getEndTime());
            }

        } else {
            // Automatic allocation: Search suitable tables ordered by capacity and lock the first available
            List<DiningTable> candidates = tableRepository
                    .findByBranchIdAndSeatingCapacityGreaterThanEqualAndIsActiveTrueOrderBySeatingCapacityAsc(
                            branch.getId(), request.getGuestCount()
                    );

            selectedTable = null;
            for (DiningTable candidate : candidates) {
                // Lock candidate table row
                DiningTable lockedCandidate = tableRepository.findByIdWithLock(candidate.getId()).orElse(null);
                if (lockedCandidate != null) {
                    List<TableReservation> conflicts = reservationRepository.findConflictingReservations(
                            lockedCandidate.getId(), request.getReservationDate(), bufferedStartTime, bufferedEndTime, now
                    );
                    if (conflicts.isEmpty()) {
                        selectedTable = lockedCandidate;
                        break;
                    }
                }
            }

            if (selectedTable == null) {
                throw new BookingConflictException("No suitable tables available for " + request.getGuestCount() +
                        " guests on " + request.getReservationDate() + " between " + request.getStartTime() + " and " + request.getEndTime());
            }
        }

        BigDecimal deposit = selectedTable.getDepositAmount();
        boolean requiresDeposit = deposit.compareTo(BigDecimal.ZERO) > 0;
        ReservationStatus initialStatus = requiresDeposit ? ReservationStatus.PENDING_PAYMENT : ReservationStatus.CONFIRMED;
        Instant expiresAt = requiresDeposit ? now.plus(holdDurationMinutes, ChronoUnit.MINUTES) : null;

        String referenceNo = "TR-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TableReservation reservation = TableReservation.builder()
                .referenceNo(referenceNo)
                .user(user)
                .branch(branch)
                .table(selectedTable)
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .guestCount(request.getGuestCount())
                .status(initialStatus)
                .specialRequests(request.getSpecialRequests())
                .depositAmount(deposit)
                .depositPaid(!requiresDeposit)
                .expiresAt(expiresAt)
                .build();

        TableReservation saved = reservationRepository.save(reservation);
        log.info("Created table reservation Ref: {} for user: {} (Status: {})", referenceNo, user.getEmail(), initialStatus);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public TableReservationResponseDto getReservationByReference(String referenceNo, User currentUser) {
        TableReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("TableReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);
        return mapToDto(res);
    }

    @Transactional(readOnly = true)
    public List<TableReservationResponseDto> getCustomerReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReservationDateDescStartTimeDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TableReservationResponseDto> getBranchReservations(Long branchId, LocalDate date) {
        return reservationRepository.findByBranchIdAndReservationDateOrderByStartTimeAsc(branchId, date).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TableReservationResponseDto cancelReservation(String referenceNo, User currentUser, String reason) {
        TableReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("TableReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);

        if (res.getStatus() == ReservationStatus.CANCELLED || res.getStatus() == ReservationStatus.COMPLETED) {
            throw new BadRequestException("INVALID_STATUS", "Reservation cannot be cancelled in status: " + res.getStatus());
        }

        // Enforce 2-hour cancellation notice policy for customers
        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isStaffOrAdmin) {
            LocalDateTime bookingDateTime = LocalDateTime.of(res.getReservationDate(), res.getStartTime());
            if (LocalDateTime.now().plusHours(2).isAfter(bookingDateTime)) {
                throw new BadRequestException("CANCELLATION_DEADLINE_PASSED",
                        "Table reservations can only be cancelled at least 2 hours prior to scheduled dining time.");
            }
        }

        res.setStatus(ReservationStatus.CANCELLED);
        TableReservation updated = reservationRepository.save(res);
        log.info("Cancelled table reservation Ref: {} by user: {} (Reason: {})", referenceNo, currentUser.getEmail(), reason);
        return mapToDto(updated);
    }

    @Transactional
    public TableReservationResponseDto checkIn(String referenceNo) {
        TableReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("TableReservation", "referenceNo", referenceNo));

        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException("INVALID_STATUS", "Only CONFIRMED reservations can be checked in.");
        }

        res.setStatus(ReservationStatus.CHECKED_IN);
        return mapToDto(reservationRepository.save(res));
    }

    @Transactional
    public TableReservationResponseDto completeReservation(String referenceNo) {
        TableReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("TableReservation", "referenceNo", referenceNo));

        if (res.getStatus() != ReservationStatus.CHECKED_IN && res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException("INVALID_STATUS", "Cannot complete reservation in status: " + res.getStatus());
        }

        res.setStatus(ReservationStatus.COMPLETED);
        return mapToDto(reservationRepository.save(res));
    }

    @Transactional
    public int releaseExpiredHolds() {
        List<TableReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.PENDING_PAYMENT, Instant.now()
        );
        for (TableReservation res : expired) {
            res.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(res);
        }
        if (!expired.isEmpty()) {
            log.info("Released {} expired table reservation holds.", expired.size());
        }
        return expired.size();
    }

    private void validateReservationTiming(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("INVALID_TIME_RANGE", "Start time must be strictly before end time.");
        }
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        if (startDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("PAST_TIME", "Cannot make reservations for past dates or times.");
        }
        if (date.isAfter(LocalDate.now().plusDays(90))) {
            throw new BadRequestException("ADVANCE_LIMIT", "Reservations can only be made up to 90 days in advance.");
        }
    }

    private void validateOwnershipOrStaff(Long resourceOwnerId, User currentUser) {
        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isStaffOrAdmin && !resourceOwnerId.equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: You do not own this reservation.");
        }
    }

    private TableReservationResponseDto mapToDto(TableReservation res) {
        return TableReservationResponseDto.builder()
                .id(res.getId())
                .referenceNo(res.getReferenceNo())
                .branchId(res.getBranch().getId())
                .branchName(res.getBranch().getName())
                .tableId(res.getTable().getId())
                .tableNumber(res.getTable().getTableNumber())
                .seatingCapacity(res.getTable().getSeatingCapacity())
                .section(res.getTable().getSection() != null ? res.getTable().getSection().name() : "INDOOR")
                .reservationDate(res.getReservationDate())
                .startTime(res.getStartTime())
                .endTime(res.getEndTime())
                .guestCount(res.getGuestCount())
                .status(res.getStatus().name())
                .depositAmount(res.getDepositAmount())
                .depositPaid(res.isDepositPaid())
                .expiresAt(res.getExpiresAt())
                .specialRequests(res.getSpecialRequests())
                .build();
    }
}
