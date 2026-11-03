package com.restauranthub.repository;

import com.restauranthub.entity.HallReservation;
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
public interface HallReservationRepository extends JpaRepository<HallReservation, Long> {

    Optional<HallReservation> findByReferenceNo(String referenceNo);

    List<HallReservation> findByUserIdOrderByReservationDateDescStartTimeDesc(Long userId);

    List<HallReservation> findByBranchIdAndReservationDateOrderByStartTimeAsc(Long branchId, LocalDate date);

    @Query("SELECT r FROM HallReservation r WHERE r.hall.id = :hallId AND r.reservationDate = :date " +
           "AND r.status NOT IN (com.restauranthub.entity.enums.ReservationStatus.CANCELLED, com.restauranthub.entity.enums.ReservationStatus.EXPIRED) " +
           "AND (r.status != com.restauranthub.entity.enums.ReservationStatus.PENDING_PAYMENT OR r.expiresAt > :now) " +
           "AND (r.startTime < :bufferedEndTime AND r.endTime > :bufferedStartTime)")
    List<HallReservation> findConflictingReservations(
            @Param("hallId") Long hallId,
            @Param("date") LocalDate date,
            @Param("bufferedStartTime") LocalTime bufferedStartTime,
            @Param("bufferedEndTime") LocalTime bufferedEndTime,
            @Param("now") Instant now
    );

    List<HallReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
