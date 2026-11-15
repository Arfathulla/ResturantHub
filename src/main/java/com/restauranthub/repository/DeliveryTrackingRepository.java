package com.restauranthub.repository;

import com.restauranthub.entity.DeliveryTracking;
import com.restauranthub.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, Long> {

    Optional<DeliveryTracking> findByOrderId(Long orderId);

    Optional<DeliveryTracking> findByOrderOrderNumber(String orderNumber);

    List<DeliveryTracking> findByOrderUserIdOrderByCreatedAtDesc(Long userId);

    List<DeliveryTracking> findByOrderUserIdAndStatusNotInOrderByCreatedAtDesc(Long userId, Collection<OrderStatus> terminalStatuses);
}
