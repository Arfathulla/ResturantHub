package com.restauranthub.repository;

import com.restauranthub.entity.RestaurantBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantBranchRepository extends JpaRepository<RestaurantBranch, Long> {

    List<RestaurantBranch> findAllByIsActiveTrue();

    Optional<RestaurantBranch> findByCode(String code);

    @Query("SELECT b FROM RestaurantBranch b WHERE b.isActive = true AND " +
           "(LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.state) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<RestaurantBranch> searchBranches(@Param("query") String query);
}
