package com.lecture.order.controller;

import com.lecture.order.dto.OrderDto;
import com.lecture.order.entity.Order;
import com.lecture.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    // 내 조달 주문 목록
    @GetMapping("/my")
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.PageResponse<OrderDto.OrderResponse>>> my(
            @RequestHeader("X-User-Id") Long buyerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(OrderDto.ApiResponse.success(service.findMine(buyerId, pageable)));
    }

    // 공장이 수주한 주문 목록
    @GetMapping("/supplier")
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.PageResponse<OrderDto.OrderResponse>>> supplier(
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestParam(required = false) Order.Status status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(OrderDto.ApiResponse.success(service.findForSupplier(supplierId, status, pageable)));
    }

    // 조달 신청
    @PostMapping
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.OrderResponse>> create(
            @Valid @RequestBody OrderDto.CreateRequest request,
            @RequestHeader("X-User-Id") Long buyerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderDto.ApiResponse.success(service.create(request, buyerId)));
    }

    // 주문 상태 (폴링용)
    @GetMapping("/{id}/status")
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.StatusResponse>> status(@PathVariable Long id) {
        return ResponseEntity.ok(OrderDto.ApiResponse.success(service.getStatus(id)));
    }

    // 주문 상세
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.ApiResponse<OrderDto.OrderResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(OrderDto.ApiResponse.success(service.get(id)));
    }

    // 내부 전용
    @GetMapping("/internal/{id}")
    public ResponseEntity<OrderDto.OrderResponse> getInternal(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }
}
