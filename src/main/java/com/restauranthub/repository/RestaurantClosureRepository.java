package com.restauranthub.repository;

import com.restauranthub.entity.RestaurantClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantClosureRepository extends JpaRepository<RestaurantClosure, Long> {

    List<RestaurantClosure> findByBranchId(Long branchId);

    Optional<RestaurantClosure> findByBranchIdAndClosureDate(Long branchId, LocalDate closureDate);

    boolean existsByBranchIdAndClosureDate(Long branchId, LocalDate closureDate);
}
