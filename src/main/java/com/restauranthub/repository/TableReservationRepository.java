package com.restauranthub.repository;

import com.restauranthub.entity.TableReservation;
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
public interface TableReservationRepository extends JpaRepository<TableReservation, Long> {

    Optional<TableReservation> findByReferenceNo(String referenceNo);

    List<TableReservation> findByUserIdOrderByReservationDateDescStartTimeDesc(Long userId);

    List<TableReservation> findByBranchIdAndReservationDateOrderByStartTimeAsc(Long branchId, LocalDate date);

    @Query("SELECT r FROM TableReservation r WHERE r.table.id = :tableId AND r.reservationDate = :date " +
           "AND r.status NOT IN (com.restauranthub.entity.enums.ReservationStatus.CANCELLED, com.restauranthub.entity.enums.ReservationStatus.EXPIRED) " +
           "AND (r.status != com.restauranthub.entity.enums.ReservationStatus.PENDING_PAYMENT OR r.expiresAt > :now) " +
           "AND (r.startTime < :bufferedEndTime AND r.endTime > :bufferedStartTime)")
    List<TableReservation> findConflictingReservations(
            @Param("tableId") Long tableId,
            @Param("date") LocalDate date,
            @Param("bufferedStartTime") LocalTime bufferedStartTime,
            @Param("bufferedEndTime") LocalTime bufferedEndTime,
            @Param("now") Instant now
    );

    List<TableReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
