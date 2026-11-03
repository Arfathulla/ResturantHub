package com.restauranthub.repository;

import com.restauranthub.entity.StaffBranchAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffBranchAssignmentRepository extends JpaRepository<StaffBranchAssignment, Long> {

    List<StaffBranchAssignment> findByUserId(Long userId);

    List<StaffBranchAssignment> findByBranchId(Long branchId);

    Optional<StaffBranchAssignment> findByUserIdAndBranchId(Long userId, Long branchId);

    boolean existsByUserIdAndBranchId(Long userId, Long branchId);
}
