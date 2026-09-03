package com.lecture.payment.repository;

import com.lecture.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserId(Long userId);

    Page<Payment> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Optional<Payment> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<Payment> findByTransactionId(String transactionId);
}