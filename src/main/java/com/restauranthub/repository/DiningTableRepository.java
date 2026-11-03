package com.restauranthub.repository;

import com.restauranthub.entity.DiningTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM DiningTable t WHERE t.id = :id")
    Optional<DiningTable> findByIdWithLock(@Param("id") Long id);

    List<DiningTable> findByBranchIdAndIsActiveTrue(Long branchId);

    List<DiningTable> findByBranchIdAndSeatingCapacityGreaterThanEqualAndIsActiveTrueOrderBySeatingCapacityAsc(
            Long branchId, Integer minCapacity
    );
}
