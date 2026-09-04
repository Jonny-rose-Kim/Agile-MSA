package com.lecture.order.controller;

import com.lecture.order.dto.InventoryDto;
import com.lecture.order.dto.OrderDto;
import com.lecture.order.service.InventoryService;
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
 * /api/inventories — 보유 재고·임계치 관리 (Ep-02 US1)
 *
 * OrderController 와 같은 규약을 따른다.
 * buyerId 는 요청 본문에서 받지 않고 Gateway 가 주입한 X-User-Id 만 신뢰한다.
 */
@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryService inventoryService;

    /** POST /api/inventories — 보유 원료 재고·임계치 등록 (BUYER) */
    @PostMapping
    public ResponseEntity<OrderDto.ApiResponse<InventoryDto.InventoryResponse>> create(
            @Valid @RequestBody InventoryDto.InventoryRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        requireBuyer(role);
        InventoryDto.InventoryResponse created = inventoryService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderDto.ApiResponse.success("재고를 등록했습니다", created));
    }

    /**
     * GET /api/inventories/my — 내 재고 목록
     * "/alerts" 보다 먼저 잡히지 않도록 경로가 겹치지 않게 둔다.
     */
    @GetMapping("/my")
    public ResponseEntity<OrderDto.ApiResponse<Page<InventoryDto.InventoryResponse>>> my(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<InventoryDto.InventoryResponse> result =
                inventoryService.getMyInventories(userId, pageable(page, size));
        return ResponseEntity.ok(OrderDto.ApiResponse.success(result));
    }

    /** GET /api/inventories/alerts — 재고 부족 감지 알림 (Ep-02 US1) */
    @GetMapping("/alerts")
    public ResponseEntity<OrderDto.ApiResponse<InventoryDto.AlertListResponse>> alerts(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(OrderDto.ApiResponse.success(inventoryService.getAlerts(userId)));
    }

    /** PATCH /api/inventories/{id}/stock — 현재 재고량 수정 (입고·소진 반영) */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<OrderDto.ApiResponse<InventoryDto.InventoryResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody InventoryDto.StockUpdateRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        InventoryDto.InventoryResponse updated =
                inventoryService.updateStock(id, userId, request.getCurrentStock());
        return ResponseEntity.ok(OrderDto.ApiResponse.success("재고를 수정했습니다", updated));
    }

    // ===== 공통 =====

    private void requireBuyer(String rawRole) {
        CallerRole role = CallerRole.of(rawRole);
        if (role != null && role != CallerRole.BUYER) {
            throw new SecurityException("제약사·연구실 계정만 재고를 등록할 수 있습니다");
        }
    }

    private PageRequest pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }
}
