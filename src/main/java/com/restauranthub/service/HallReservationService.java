package com.restauranthub.service;

import com.restauranthub.dto.HallReservationRequestDto;
import com.restauranthub.dto.HallReservationResponseDto;
import com.restauranthub.entity.*;
import com.restauranthub.entity.enums.ReservationStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.BookingConflictException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.exception.UnauthorizedException;
import com.restauranthub.repository.HallPackageRepository;
import com.restauranthub.repository.HallReservationRepository;
import com.restauranthub.repository.PartyHallRepository;
import com.restauranthub.repository.RestaurantBranchRepository;
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
public class HallReservationService {

    private static final Logger log = LoggerFactory.getLogger(HallReservationService.class);

    private final HallReservationRepository reservationRepository;
    private final PartyHallRepository hallRepository;
    private final HallPackageRepository packageRepository;
    private final RestaurantBranchRepository branchRepository;
    private final BranchService branchService;

    @Value("${app.reservation.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Value("${app.reservation.hall-buffer-minutes:60}")
    private int bufferMinutes;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public HallReservationResponseDto reserveHall(User user, HallReservationRequestDto request) {
        LocalTime endTime = request.getStartTime().plusHours(request.getDurationHours());

        LocalDateTime startDateTime = LocalDateTime.of(request.getReservationDate(), request.getStartTime());
        if (startDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("PAST_TIME", "Cannot reserve party halls for past dates or times.");
        }

        RestaurantBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", request.getBranchId()));

        if (!branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), request.getStartTime()) ||
            !branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), endTime)) {
            throw new BadRequestException("BRANCH_CLOSED", "Requested time falls outside of restaurant branch operating hours.");
        }

        // Concurrency control: Acquire pessimistic write lock on the target party hall
        PartyHall hall = hallRepository.findByIdWithLock(request.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("PartyHall", "id", request.getHallId()));

        if (!hall.getBranch().getId().equals(branch.getId())) {
            throw new BadRequestException("INVALID_HALL", "Party hall does not belong to the selected branch.");
        }
        if (!hall.isActive()) {
            throw new BadRequestException("HALL_INACTIVE", "The selected party hall is currently unavailable.");
        }
        if (request.getGuestCount() > hall.getCapacity()) {
            throw new BadRequestException("CAPACITY_EXCEEDED",
                    String.format("Guest count (%d) exceeds party hall capacity (%d)", request.getGuestCount(), hall.getCapacity()));
        }
        if (request.getDurationHours() < hall.getMinDurationHours() || request.getDurationHours() > hall.getMaxDurationHours()) {
            throw new BadRequestException("INVALID_DURATION",
                    String.format("Booking duration must be between %d and %d hours for this hall",
                            hall.getMinDurationHours(), hall.getMaxDurationHours()));
        }

        LocalTime bufferedStartTime = request.getStartTime().minusMinutes(bufferMinutes);
        LocalTime bufferedEndTime = endTime.plusMinutes(bufferMinutes);
        Instant now = Instant.now();

        List<HallReservation> conflicts = reservationRepository.findConflictingReservations(
                hall.getId(), request.getReservationDate(), bufferedStartTime, bufferedEndTime, now
        );

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Party Hall", hall.getId(),
                    request.getReservationDate() + " " + request.getStartTime() + " - " + endTime);
        }

        // Server-side financial computation
        BigDecimal durationCost = hall.getHourlyRate().multiply(BigDecimal.valueOf(request.getDurationHours()));
        BigDecimal rentalCharge = hall.getBaseRentalPrice().add(durationCost);

        HallPackage selectedPkg = null;
        BigDecimal packageCharge = BigDecimal.ZERO;
        if (request.getPackageId() != null) {
            selectedPkg = packageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new ResourceNotFoundException("HallPackage", "id", request.getPackageId()));
            if (!selectedPkg.getHall().getId().equals(hall.getId())) {
                throw new BadRequestException("INVALID_PACKAGE", "Selected catering package does not belong to this hall.");
            }
            packageCharge = selectedPkg.getPricePerGuest().multiply(BigDecimal.valueOf(request.getGuestCount()));
        }

        BigDecimal totalPrice = rentalCharge.add(packageCharge);
        BigDecimal deposit = hall.getDepositAmount();

        String referenceNo = "HR-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        HallReservation reservation = HallReservation.builder()
                .referenceNo(referenceNo)
                .user(user)
                .branch(branch)
                .hall(hall)
                .packageSelected(selectedPkg)
                .eventType(request.getEventType())
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .guestCount(request.getGuestCount())
                .durationHours(request.getDurationHours())
                .hallRentalCharge(rentalCharge)
                .packageCharge(packageCharge)
                .totalPrice(totalPrice)
                .depositAmount(deposit)
                .status(ReservationStatus.PENDING_PAYMENT)
                .specialRequests(request.getSpecialRequests())
                .expiresAt(now.plus(holdDurationMinutes, ChronoUnit.MINUTES))
                .build();

        HallReservation saved = reservationRepository.save(reservation);
        log.info("Created party hall reservation Ref: {} for user: {} (Total: ${}, Deposit: ${})",
                referenceNo, user.getEmail(), totalPrice, deposit);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public HallReservationResponseDto getReservationByReference(String referenceNo, User currentUser) {
        HallReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("HallReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);
        return mapToDto(res);
    }

    @Transactional(readOnly = true)
    public List<HallReservationResponseDto> getCustomerReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReservationDateDescStartTimeDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public HallReservationResponseDto cancelReservation(String referenceNo, User currentUser) {
        HallReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("HallReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);

        if (res.getStatus() == ReservationStatus.CANCELLED || res.getStatus() == ReservationStatus.COMPLETED) {
            throw new BadRequestException("INVALID_STATUS", "Reservation cannot be cancelled in status: " + res.getStatus());
        }

        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        if (!isStaffOrAdmin) {
            LocalDateTime bookingDateTime = LocalDateTime.of(res.getReservationDate(), res.getStartTime());
            if (LocalDateTime.now().plusHours(48).isAfter(bookingDateTime)) {
                throw new BadRequestException("CANCELLATION_DEADLINE_PASSED",
                        "Party hall reservations require at least 48 hours notice prior to event time for cancellation.");
            }
        }

        res.setStatus(ReservationStatus.CANCELLED);
        HallReservation updated = reservationRepository.save(res);
        log.info("Cancelled party hall reservation Ref: {} by user: {}", referenceNo, currentUser.getEmail());
        return mapToDto(updated);
    }

    @Transactional
    public int releaseExpiredHolds() {
        List<HallReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.PENDING_PAYMENT, Instant.now()
        );
        for (HallReservation res : expired) {
            res.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(res);
        }
        return expired.size();
    }

    private void validateOwnershipOrStaff(Long resourceOwnerId, User currentUser) {
        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isStaffOrAdmin && !resourceOwnerId.equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: You do not own this hall reservation.");
        }
    }

    private HallReservationResponseDto mapToDto(HallReservation res) {
        return HallReservationResponseDto.builder()
                .id(res.getId())
                .referenceNo(res.getReferenceNo())
                .branchId(res.getBranch().getId())
                .branchName(res.getBranch().getName())
                .hallId(res.getHall().getId())
                .hallName(res.getHall().getHallName())
                .packageId(res.getPackageSelected() != null ? res.getPackageSelected().getId() : null)
                .packageName(res.getPackageSelected() != null ? res.getPackageSelected().getPackageName() : "Room Only")
                .eventType(res.getEventType())
                .reservationDate(res.getReservationDate())
                .startTime(res.getStartTime())
                .endTime(res.getEndTime())
                .durationHours(res.getDurationHours())
                .guestCount(res.getGuestCount())
                .hallRentalCharge(res.getHallRentalCharge())
                .packageCharge(res.getPackageCharge())
                .totalPrice(res.getTotalPrice())
                .depositAmount(res.getDepositAmount())
                .status(res.getStatus().name())
                .expiresAt(res.getExpiresAt())
                .specialRequests(res.getSpecialRequests())
                .build();
    }
}
