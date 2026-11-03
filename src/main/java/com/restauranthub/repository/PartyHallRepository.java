package com.restauranthub.repository;

import com.restauranthub.entity.PartyHall;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyHallRepository extends JpaRepository<PartyHall, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM PartyHall h WHERE h.id = :id")
    Optional<PartyHall> findByIdWithLock(@Param("id") Long id);

    List<PartyHall> findByBranchIdAndIsActiveTrue(Long branchId);

    List<PartyHall> findByBranchIdAndCapacityGreaterThanEqualAndIsActiveTrueOrderByCapacityAsc(
            Long branchId, Integer minCapacity
    );
}
