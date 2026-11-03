package com.restauranthub.repository;

import com.restauranthub.entity.DiningRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiningRoomRepository extends JpaRepository<DiningRoom, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DiningRoom r WHERE r.id = :id")
    Optional<DiningRoom> findByIdWithLock(@Param("id") Long id);

    List<DiningRoom> findByBranchIdAndIsActiveTrue(Long branchId);

    List<DiningRoom> findByBranchIdAndCapacityGreaterThanEqualAndIsActiveTrueOrderByCapacityAsc(
            Long branchId, Integer minCapacity
    );
}
