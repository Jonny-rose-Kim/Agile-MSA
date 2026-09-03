package com.lecture.payment.controller;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /payments/internal/request - 내부 결제 요청 (Enrollment Service 호출)
     */
    @PostMapping("/internal/request")
    public ResponseEntity<PaymentDto.InternalPaymentResult> processInternalPayment(
            @RequestBody PaymentDto.InternalPaymentRequest request) {

        PaymentDto.InternalPaymentResult result = paymentService.processInternalPayment(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/payments/my - 내 결제 내역 (X-User-Id 기준)
     */
    @GetMapping("/my")
    public ResponseEntity<PaymentDto.ApiResponse<PaymentDto.PageResponse<PaymentDto.PaymentResponse>>> getMyPayments(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PaymentDto.ApiResponse.success(paymentService.getMyPayments(userId, pageable)));
    }

    /**
     * POST /api/payments - 조달 결제 요청 (BUYER)
     */
    @PostMapping
    public ResponseEntity<PaymentDto.ApiResponse<PaymentDto.PaymentResponse>> pay(
            @Valid @RequestBody PaymentDto.ProcurementPaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        PaymentDto.PaymentResponse res =
                paymentService.createProcurementPayment(userId, request.getOrderId(), request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentDto.ApiResponse.success(res));
    }

    /**
     * GET /payments/{id} - 결제 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto.ApiResponse<PaymentDto.PaymentResponse>> getPayment(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                PaymentDto.ApiResponse.success(paymentService.getPayment(id)));
    }

    /**
     * GET /payments/user/{userId} - 사용자 결제 내역 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PaymentDto.ApiResponse<List<PaymentDto.PaymentResponse>>> getPaymentsByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                PaymentDto.ApiResponse.success(paymentService.getPaymentsByUser(userId)));
    }
}
