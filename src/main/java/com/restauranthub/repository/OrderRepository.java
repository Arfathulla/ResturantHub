package com.restauranthub.repository;

import com.restauranthub.entity.Order;
import com.restauranthub.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<Order> findByBranchIdAndStatusInOrderByCreatedAtAsc(Long branchId, List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
