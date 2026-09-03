package com.lecture.order.repository;

import com.lecture.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);
    Page<Order> findBySupplierId(Long supplierId, Pageable pageable);
    Page<Order> findBySupplierIdAndStatus(Long supplierId, Order.Status status, Pageable pageable);
}
