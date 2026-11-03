package com.restauranthub.repository;

import com.restauranthub.entity.ParkingSlot;
import com.restauranthub.entity.enums.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParkingSlot p WHERE p.id = :id")
    Optional<ParkingSlot> findByIdWithLock(@Param("id") Long id);

    List<ParkingSlot> findByBranchIdAndIsActiveTrue(Long branchId);

    List<ParkingSlot> findByBranchIdAndVehicleTypeAndIsActiveTrue(Long branchId, VehicleType vehicleType);
}
