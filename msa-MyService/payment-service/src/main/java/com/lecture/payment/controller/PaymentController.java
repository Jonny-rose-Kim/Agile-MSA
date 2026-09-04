package com.lecture.payment.controller;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.service.PaymentService;
import com.lecture.payment.support.CallerRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * /api/payments
 *
 * buyerId 는 요청 본문에서 받지 않는다.
 * API Gateway 가 JWT 를 검증한 뒤 주입한 X-User-Id / X-User-Role 만 신뢰한다.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentService paymentService;

    /** POST /api/payments — 조달 결제 요청 (BUYER) */
    @PostMapping
    public ResponseEntity<PaymentDto.ApiResponse<PaymentDto.PaymentResponse>> pay(
            @Valid @RequestBody PaymentDto.PaymentRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        requireBuyer(role);
        PaymentDto.PaymentResponse created = paymentService.pay(userId, request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentDto.ApiResponse.success("결제가 완료되었습니다", created));
    }

    /**
     * GET /api/payments/my — 내 결제 내역
     * "/{id}" 보다 먼저 선언해야 my 가 id 로 잡히지 않는다.
     */
    @GetMapping("/my")
    public ResponseEntity<PaymentDto.ApiResponse<Page<PaymentDto.PaymentResponse>>> my(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PaymentDto.PaymentResponse> result =
                paymentService.getMyPayments(userId, pageable(page, size));
        return ResponseEntity.ok(PaymentDto.ApiResponse.success(result));
    }

    /** GET /api/payments/{id} — 결제 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto.ApiResponse<PaymentDto.PaymentResponse>> get(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(PaymentDto.ApiResponse.success(paymentService.get(id, userId)));
    }

    // ===== 공통 =====

    private void requireBuyer(String rawRole) {
        CallerRole role = CallerRole.of(rawRole);
        if (role != null && role != CallerRole.BUYER) {
            throw new SecurityException("제약사·연구실 계정만 결제할 수 있습니다");
        }
    }

    private PageRequest pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }
}
