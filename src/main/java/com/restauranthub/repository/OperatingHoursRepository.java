package com.restauranthub.repository;

import com.restauranthub.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperatingHoursRepository extends JpaRepository<OperatingHours, Long> {

    List<OperatingHours> findByBranchId(Long branchId);

    Optional<OperatingHours> findByBranchIdAndDayOfWeek(Long branchId, DayOfWeek dayOfWeek);
}
