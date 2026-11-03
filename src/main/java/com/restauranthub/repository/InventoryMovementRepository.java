package com.restauranthub.repository;

import com.restauranthub.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByInventoryItemIdOrderByCreatedAtDesc(Long inventoryItemId);
}
