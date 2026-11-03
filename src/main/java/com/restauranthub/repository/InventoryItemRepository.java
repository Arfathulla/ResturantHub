package com.restauranthub.repository;

import com.restauranthub.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByMenuItemId(Long menuItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.menuItem.id = :menuItemId")
    Optional<InventoryItem> findByMenuItemIdWithLock(@Param("menuItemId") Long menuItemId);

    @Query("SELECT i FROM InventoryItem i WHERE i.quantityInStock <= i.lowStockThreshold")
    List<InventoryItem> findLowStockItems();
}
