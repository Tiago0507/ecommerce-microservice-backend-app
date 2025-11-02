package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.selimhorri.app.domain.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByIsActiveTrue();
    Optional<OrderItem> findByOrderIdAndIsActiveTrue(Integer orderId);

    List<OrderItem> findAllByOrderIdAndIsActiveTrue(Integer orderId);
    Optional<OrderItem> findByOrderIdAndProductIdAndIsActiveTrue(Integer orderId, Integer productId);

}