package com.restauranthub.service;

import com.restauranthub.dto.RoomReservationRequestDto;
import com.restauranthub.dto.RoomReservationResponseDto;
import com.restauranthub.entity.DiningRoom;
import com.restauranthub.entity.RestaurantBranch;
import com.restauranthub.entity.RoomReservation;
import com.restauranthub.entity.User;
import com.restauranthub.entity.enums.ReservationStatus;
import com.restauranthub.exception.BadRequestException;
import com.restauranthub.exception.BookingConflictException;
import com.restauranthub.exception.ResourceNotFoundException;
import com.restauranthub.exception.UnauthorizedException;
import com.restauranthub.repository.DiningRoomRepository;
import com.restauranthub.repository.RestaurantBranchRepository;
import com.restauranthub.repository.RoomReservationRepository;
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
public class RoomReservationService {

    private static final Logger log = LoggerFactory.getLogger(RoomReservationService.class);

    private final RoomReservationRepository reservationRepository;
    private final DiningRoomRepository roomRepository;
    private final RestaurantBranchRepository branchRepository;
    private final BranchService branchService;

    @Value("${app.reservation.hold-duration-minutes:15}")
    private int holdDurationMinutes;

    @Value("${app.reservation.room-buffer-minutes:30}")
    private int bufferMinutes;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public RoomReservationResponseDto reserveRoom(User user, RoomReservationRequestDto request) {
        LocalTime endTime = request.getStartTime().plusHours(request.getDurationHours());

        LocalDateTime startDateTime = LocalDateTime.of(request.getReservationDate(), request.getStartTime());
        if (startDateTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("PAST_TIME", "Cannot reserve private rooms in the past.");
        }

        RestaurantBranch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantBranch", "id", request.getBranchId()));

        if (!branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), request.getStartTime()) ||
            !branchService.isBranchOpenAt(branch.getId(), request.getReservationDate(), endTime)) {
            throw new BadRequestException("BRANCH_CLOSED", "Requested time falls outside of restaurant branch operating hours.");
        }

        // Concurrency control: Acquire pessimistic write lock on the target private dining room
        DiningRoom room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("DiningRoom", "id", request.getRoomId()));

        if (!room.getBranch().getId().equals(branch.getId())) {
            throw new BadRequestException("INVALID_ROOM", "The room does not belong to the selected branch.");
        }
        if (!room.isActive()) {
            throw new BadRequestException("ROOM_INACTIVE", "The requested room is currently out of service.");
        }
        if (request.getGuestCount() > room.getCapacity()) {
            throw new BadRequestException("CAPACITY_EXCEEDED",
                    String.format("Guest count (%d) exceeds room capacity (%d)", request.getGuestCount(), room.getCapacity()));
        }
        if (request.getDurationHours() < room.getMinDurationHours() || request.getDurationHours() > room.getMaxDurationHours()) {
            throw new BadRequestException("INVALID_DURATION",
                    String.format("Duration must be between %d and %d hours for this room",
                            room.getMinDurationHours(), room.getMaxDurationHours()));
        }

        LocalTime bufferedStartTime = request.getStartTime().minusMinutes(bufferMinutes);
        LocalTime bufferedEndTime = endTime.plusMinutes(bufferMinutes);
        Instant now = Instant.now();

        List<RoomReservation> conflicts = reservationRepository.findConflictingReservations(
                room.getId(), request.getReservationDate(), bufferedStartTime, bufferedEndTime, now
        );

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Private Dining Room", room.getId(),
                    request.getReservationDate() + " " + request.getStartTime() + " - " + endTime);
        }

        // Server-side charge computation: Base + (Hourly * Duration)
        BigDecimal durationCost = room.getHourlyPrice().multiply(BigDecimal.valueOf(request.getDurationHours()));
        BigDecimal totalPrice = room.getBasePrice().add(durationCost);
        BigDecimal deposit = room.getDepositAmount();

        String referenceNo = "RR-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RoomReservation reservation = RoomReservation.builder()
                .referenceNo(referenceNo)
                .user(user)
                .branch(branch)
                .room(room)
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .guestCount(request.getGuestCount())
                .durationHours(request.getDurationHours())
                .totalPrice(totalPrice)
                .depositAmount(deposit)
                .status(ReservationStatus.PENDING_PAYMENT)
                .specialRequests(request.getSpecialRequests())
                .expiresAt(now.plus(holdDurationMinutes, ChronoUnit.MINUTES))
                .build();

        RoomReservation saved = reservationRepository.save(reservation);
        log.info("Created room reservation Ref: {} for user: {} (Total: ${}, Deposit: ${})",
                referenceNo, user.getEmail(), totalPrice, deposit);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public RoomReservationResponseDto getReservationByReference(String referenceNo, User currentUser) {
        RoomReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("RoomReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);
        return mapToDto(res);
    }

    @Transactional(readOnly = true)
    public List<RoomReservationResponseDto> getCustomerReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReservationDateDescStartTimeDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomReservationResponseDto cancelReservation(String referenceNo, User currentUser) {
        RoomReservation res = reservationRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new ResourceNotFoundException("RoomReservation", "referenceNo", referenceNo));

        validateOwnershipOrStaff(res.getUser().getId(), currentUser);

        if (res.getStatus() == ReservationStatus.CANCELLED || res.getStatus() == ReservationStatus.COMPLETED) {
            throw new BadRequestException("INVALID_STATUS", "Reservation cannot be cancelled in status: " + res.getStatus());
        }

        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        if (!isStaffOrAdmin) {
            LocalDateTime bookingDateTime = LocalDateTime.of(res.getReservationDate(), res.getStartTime());
            if (LocalDateTime.now().plusHours(12).isAfter(bookingDateTime)) {
                throw new BadRequestException("CANCELLATION_DEADLINE_PASSED",
                        "Private room reservations must be cancelled at least 12 hours prior to scheduled dining time.");
            }
        }

        res.setStatus(ReservationStatus.CANCELLED);
        RoomReservation updated = reservationRepository.save(res);
        log.info("Cancelled room reservation Ref: {} by user: {}", referenceNo, currentUser.getEmail());
        return mapToDto(updated);
    }

    @Transactional
    public int releaseExpiredHolds() {
        List<RoomReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.PENDING_PAYMENT, Instant.now()
        );
        for (RoomReservation res : expired) {
            res.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(res);
        }
        return expired.size();
    }

    private void validateOwnershipOrStaff(Long resourceOwnerId, User currentUser) {
        boolean isStaffOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isStaffOrAdmin && !resourceOwnerId.equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied: You do not own this room reservation.");
        }
    }

    private RoomReservationResponseDto mapToDto(RoomReservation res) {
        return RoomReservationResponseDto.builder()
                .id(res.getId())
                .referenceNo(res.getReferenceNo())
                .branchId(res.getBranch().getId())
                .branchName(res.getBranch().getName())
                .roomId(res.getRoom().getId())
                .roomName(res.getRoom().getRoomName())
                .reservationDate(res.getReservationDate())
                .startTime(res.getStartTime())
                .endTime(res.getEndTime())
                .durationHours(res.getDurationHours())
                .guestCount(res.getGuestCount())
                .totalPrice(res.getTotalPrice())
                .depositAmount(res.getDepositAmount())
                .status(res.getStatus().name())
                .expiresAt(res.getExpiresAt())
                .specialRequests(res.getSpecialRequests())
                .build();
    }
}
