package com.lecture.order.controller;

import com.lecture.order.dto.OrderDto;
import com.lecture.order.entity.Order;
import com.lecture.order.service.OrderService;
import com.lecture.order.support.CallerRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * /api/orders
 *
 * buyerId / supplierId 는 요청 본문에서 받지 않는다.
 * API Gateway 가 JWT 를 검증한 뒤 주입한 X-User-Id / X-User-Role 만 신뢰한다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    /** POST /api/orders — 조달 신청 (BUYER) */
    @PostMapping
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.OrderResponse>> create(
            @Valid @RequestBody OrderDto.OrderRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        requireBuyer(role);
        OrderDto.OrderResponse created = orderService.create(userId, email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderDto.ApiResponse.success("조달을 신청했습니다", created));
    }

    /**
     * GET /api/orders/my — 내 조달 주문 목록
     * "/{id}" 보다 먼저 선언해야 my 가 id 로 잡히지 않는다.
     */
    @GetMapping("/my")
    public ResponseEntity<OrderDto.ApiResponse<Page<OrderDto.OrderResponse>>> my(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderDto.OrderResponse> result = orderService.getMyOrders(userId, pageable(page, size));
        return ResponseEntity.ok(OrderDto.ApiResponse.success(result));
    }

    /** GET /api/orders/supplier — 공장이 수주한 주문 목록 (SUPPLIER, Ep-01 US2) */
    @GetMapping("/supplier")
    public ResponseEntity<OrderDto.ApiResponse<Page<OrderDto.OrderResponse>>> supplier(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Order.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderDto.OrderResponse> result =
                orderService.getSupplierOrders(userId, status, pageable(page, size));
        return ResponseEntity.ok(OrderDto.ApiResponse.success(result));
    }

    /** GET /api/orders/{id}/status — 상태 폴링용 경량 조회 */
    @GetMapping("/{id}/status")
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.OrderStatusResponse>> status(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(OrderDto.ApiResponse.success(orderService.getStatus(id, userId)));
    }

    /** GET /api/orders/{id} — 주문 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.OrderResponse>> get(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(OrderDto.ApiResponse.success(orderService.get(id, userId)));
    }

    // ===== 내부 API: payment-service 전용. Gateway 에 라우트를 열지 않는다. =====

    /** 주문 조회. payment-service 가 결제 금액과 소유자를 확인할 때 쓴다. */
    @GetMapping("/internal/{id}")
    public ResponseEntity<OrderDto.OrderResponse> internalGet(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getInternal(id));
    }

    /** 결제 완료 시 주문을 CONFIRMED 로 바꾼다. (Sprint 2) */
    @PostMapping("/internal/{id}/confirm")
    public ResponseEntity<OrderDto.OrderResponse> internalConfirm(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirm(id));
    }

    // ===== 공통 =====

    private void requireBuyer(String rawRole) {
        CallerRole role = CallerRole.of(rawRole);
        if (role != null && role != CallerRole.BUYER) {
            throw new SecurityException("제약사·연구실 계정만 조달을 신청할 수 있습니다");
        }
    }

    private PageRequest pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }
}
