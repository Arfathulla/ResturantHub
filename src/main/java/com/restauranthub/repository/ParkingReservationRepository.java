package com.restauranthub.repository;

import com.restauranthub.entity.ParkingReservation;
import com.restauranthub.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingReservationRepository extends JpaRepository<ParkingReservation, Long> {

    Optional<ParkingReservation> findByReferenceNo(String referenceNo);

    List<ParkingReservation> findByUserIdOrderByReservationDateDescStartTimeDesc(Long userId);

    List<ParkingReservation> findByBranchIdAndReservationDateOrderByStartTimeAsc(Long branchId, LocalDate date);

    @Query("SELECT r FROM ParkingReservation r WHERE r.parkingSlot.id = :slotId AND r.reservationDate = :date " +
           "AND r.status NOT IN (com.restauranthub.entity.enums.ReservationStatus.CANCELLED, com.restauranthub.entity.enums.ReservationStatus.EXPIRED) " +
           "AND (r.status != com.restauranthub.entity.enums.ReservationStatus.PENDING_PAYMENT OR r.expiresAt > :now) " +
           "AND (r.startTime < :endTime AND r.endTime > :startTime)")
    List<ParkingReservation> findConflictingReservations(
            @Param("slotId") Long slotId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("now") Instant now
    );

    List<ParkingReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
