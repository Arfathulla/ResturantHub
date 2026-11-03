package com.restauranthub.repository;

import com.restauranthub.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByBranchIdAndIsAvailableTrue(Long branchId);

    List<MenuItem> findByBranchIdAndCategoryIdAndIsAvailableTrue(Long branchId, Long categoryId);

    @Query("SELECT m FROM MenuItem m WHERE m.branch.id = :branchId AND m.isAvailable = true AND " +
           "(LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<MenuItem> searchMenuItems(@Param("branchId") Long branchId, @Param("query") String query);

    /**
     * Cross-branch search across all active Bangalore branches.
     */
    @Query("SELECT m FROM MenuItem m WHERE m.isAvailable = true AND " +
           "(LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.category.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY m.branch.id, m.name")
    List<MenuItem> searchAllBranches(@Param("query") String query);
}
