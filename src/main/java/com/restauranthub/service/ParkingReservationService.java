package com.restauranthub.service;

import com.restauranthub.dto.ParkingReservationRequestDto;
import com.restauranthub.dto.ParkingReservationResponseDto;
import com.restauranthub.entity.*;
import com.restauranthub.entity.enums.ReservationStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.BookingConflictException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.exception.UnauthorizedException;
import com.restauranthub.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingReservationService {

    private static final Logger log = LoggerFactory.getLogger(ParkingReservationService.class);

    private final ParkingReservationRepository reservationRepository;
    private final ParkingSlotRepository slotRepository;
    private final ParkingCheckinRecordRepository checkinRecordRepository;
    private final RestaurantBranchRepository branchRepository;
    private final BranchService branchService;

    @Value("${app.reservation.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ParkingReservationResponseDto reserveParking(User user, ParkingReservationRequestDto request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("INVALID_TIME_RANGE", "Start time must be strictly before end time.");
        }

        LocalDateTime startDateTime = LocalDateTime.of(request.getReservationDate(), request.getStartTime());
        if (startDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("PAST_TIME", "Cannot reserve parking slots in the past.");
        }

        RestaurantBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", request.getBranchId()));

        if (!branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), request.getStartTime()) ||
            !branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), request.getEndTime())) {
            throw new BadRequestException("BRANCH_CLOSED", "Requested slot time is outside of branch operational hours.");
        }

        Duration duration = Duration.between(request.getStartTime(), request.getEndTime());
        double hours = (double) duration.toMinutes() / 60.0;
        if (hours > 24.0) {
            throw new BadRequestException("MAX_DURATION_EXCEEDED", "Maximum parking reservation duration is 24 hours.");
        }

        Instant now = Instant.now();
        ParkingSlot selectedSlot;

        if (request.getParkingSlotId() != null) {
            // Explicit slot selection: acquire pessimistic write lock
            selectedSlot = slotRepository.findByIdWithLock(request.getParkingSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("ParkingSlot", "id", request.getParkingSlotId()));

            if (!selectedSlot.getBranch().getId().equals(branch.getId())) {
                throw new BadRequestException("INVALID_SLOT", "Parking slot does not belong to the selected branch.");
            }
            if (!selectedSlot.isActive()) {
                throw new BadRequestException("SLOT_INACTIVE", "Selected parking slot is currently inactive.");
            }
            if (selectedSlot.getVehicleType() != request.getVehicleType()) {
                throw new BadRequestException("VEHICLE_TYPE_MISMATCH",
                        String.format("Slot is designated for %s, but vehicle is %s",
                                selectedSlot.getVehicleType(), request.getVehicleType()));
            }

            List<ParkingReservation> conflicts = reservationRepository.findConflictingReservations(
                    selectedSlot.getId(), request.getReservationDate(), request.getStartTime(), request.getEndTime(), now
            );

            if (!conflicts.isEmpty()) {
                throw new BookingConflictException("Parking Slot", selectedSlot.getId(),
                        request.getReservationDate() + " " + request.getStartTime() + " - " + request.getEndTime());
            }

        } else {
            // Automatic allocation: search compatible slots and lock first conflict-free slot
            List<ParkingSlot> candidates = slotRepository
                    .findByBranchIdAndVehicleTypeAndIsActiveTrue(branch.getId(), request.getVehicleType());

            selectedSlot = null;
            for (ParkingSlot candidate : candidates) {
                ParkingSlot lockedCandidate = slotRepository.findByIdWithLock(candidate.getId()).orElse(null);
                if (lockedCandidate != null) {
                    List<ParkingReservation> conflicts = reservationRepository.findConflictingReservations(
                            lockedCandidate.getId(), request.getReservationDate(), request.getStartTime(), request.getEndTime(), now
                    );
                    if (conflicts.isEmpty()) {
                        selectedSlot = lockedCandidate;
                        break;
                    }
                }
            }

            if (selectedSlot == null) {
                throw new BookingConflictException("No compatible parking slots available for " + request.getVehicleType() +
                        " on " + request.getReservationDate() + " between " + request.getStartTime() + " and " + request.getEndTime());
            }
        }

        BigDecimal totalPrice = selectedSlot.getHourlyRate()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, RoundingMode.HALF_UP);

        String referenceNo = "PR-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ParkingReservation reservation = ParkingReservation.builder()
                .referenceNo(referenceNo)
                .user(user)
                .branch(branch)
                .parkingSlot(selectedSlot)
                .vehicleRegistrationNo(request.getVehicleRegistrationNo().trim().toUpperCase())
                .vehicleType(request.getVehicleType())
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING_PAYMENT)
                .expiresAt(now.plus(holdDurationMinutes, ChronoUnit.MINUTES))
                .build();

        ParkingReservation saved = reservationRepository.save(reservation);
        log.info("Created parking reservation Ref: {} for user: {} (Slot: {}, Total: ${})",
                referenceNo, user.getEmail(), selectedSlot.getSlotNumber(), totalPrice);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public ParkingReservationResponseDto getReservationByReference(String referenceNo, User currentUser) {
        ParkingReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);
        return mapToDto(res);
    }

    @Transactional(readOnly = true)
    public List<ParkingReservationResponseDto> getCustomerReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReservationDateDescStartTimeDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ParkingReservationResponseDto checkInVehicle(String referenceNo, Long staffUserId) {
        ParkingReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingReservation", "referenceNo", referenceNo));

        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException("INVALID_STATUS", "Only confirmed reservations can check in.");
        }

        res.setStatus(ReservationStatus.CHECKED_IN);
        ParkingReservation saved = reservationRepository.save(res);

        ParkingCheckinRecord record = ParkingCheckinRecord.builder()
                .parkingReservation(saved)
                .actualEntryTime(Instant.now())
                .handledByUserId(staffUserId)
                .build();
        checkinRecordRepository.save(record);

        return mapToDto(saved);
    }

    @Transactional
    public ParkingReservationResponseDto checkOutVehicle(String referenceNo, Long staffUserId) {
        ParkingReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingReservation", "referenceNo", referenceNo));

        if (res.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BadRequestException("INVALID_STATUS", "Only checked-in vehicles can check out.");
        }

        res.setStatus(ReservationStatus.COMPLETED);
        ParkingReservation saved = reservationRepository.save(res);

        checkinRecordRepository.findByParkingReservationId(res.getId()).ifPresent(record -> {
            record.setActualExitTime(Instant.now());
            record.setHandledByUserId(staffUserId);
            checkinRecordRepository.save(record);
        });

        return mapToDto(saved);
    }

    @Transactional
    public ParkingReservationResponseDto cancelReservation(String referenceNo, User currentUser) {
        ParkingReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("ParkingReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);

        if (res.getStatus() == ReservationStatus.CANCELLED || res.getStatus() == ReservationStatus.COMPLETED) {
            throw new BadRequestException("INVALID_STATUS", "Reservation cannot be cancelled in status: " + res.getStatus());
        }

        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isStaffOrAdmin) {
            LocalDateTime bookingDateTime = LocalDateTime.of(res.getReservationDate(), res.getStartTime());
            if (LocalDateTime.now().plusHours(1).isAfter(bookingDateTime)) {
                throw new BadRequestException("CANCELLATION_DEADLINE_PASSED",
                        "Parking reservations can only be cancelled at least 1 hour prior to scheduled start time.");
            }
        }

        res.setStatus(ReservationStatus.CANCELLED);
        ParkingReservation updated = reservationRepository.save(res);
        log.info("Cancelled parking reservation Ref: {} by user: {}", referenceNo, currentUser.getEmail());
        return mapToDto(updated);
    }

    @Transactional
    public int releaseExpiredHolds() {
        List<ParkingReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.PENDING_PAYMENT, Instant.now()
        );
        for (ParkingReservation res : expired) {
            res.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(res);
        }
        return expired.size();
    }

    private void validateOwnershipOrStaff(Long resourceOwnerId, User currentUser) {
        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isStaffOrAdmin && !resourceOwnerId.equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: You do not own this parking reservation.");
        }
    }

    private ParkingReservationResponseDto mapToDto(ParkingReservation res) {
        return ParkingReservationResponseDto.builder()
                .id(res.getId())
                .referenceNo(res.getReferenceNo())
                .branchId(res.getBranch().getId())
                .branchName(res.getBranch().getName())
                .parkingSlotId(res.getParkingSlot().getId())
                .slotNumber(res.getParkingSlot().getSlotNumber())
                .vehicleType(res.getVehicleType().name())
                .isCovered(res.getParkingSlot().isCovered())
                .reservationDate(res.getReservationDate())
                .startTime(res.getStartTime())
                .endTime(res.getEndTime())
                .totalPrice(res.getTotalPrice())
                .status(res.getStatus().name())
                .expiresAt(res.getExpiresAt())
                .build();
    }
}
