package com.lecture.material.controller;

import com.lecture.material.dto.MaterialDto;
import com.lecture.material.entity.Material;
import com.lecture.material.service.MaterialService;
import com.lecture.material.support.CallerRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * /api/materials
 *
 * supplierId 는 요청 본문에서 받지 않는다. API Gateway 가 JWT 를 검증한 뒤
 * X-User-Id / X-User-Role 헤더로 주입한 값만 신뢰한다.
 */
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MaterialService materialService;

    /** GET /api/materials — 목록·검색 */
    @GetMapping
    public ResponseEntity<MaterialDto.ApiResponse<Page<MaterialDto.MaterialResponse>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Material.Category category,
            @RequestParam(required = false) String certification,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<MaterialDto.MaterialResponse> result = materialService.search(
                keyword, category, certification, minCapacity, pageable(page, size));
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(result));
    }

    /**
     * GET /api/materials/my — 내 공급 품목 (SUPPLIER)
     * "/{id}" 보다 먼저 선언해야 my 가 id 로 잡히지 않는다.
     */
    @GetMapping("/my")
    public ResponseEntity<MaterialDto.ApiResponse<Page<MaterialDto.MaterialResponse>>> my(
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<MaterialDto.MaterialResponse> result = materialService.getMy(supplierId, pageable(page, size));
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(result));
    }

    /** GET /api/materials/code/{materialCode}/suppliers — 공급 가능 인증 공장 조회 (Ep-01 US1) */
    @GetMapping("/code/{materialCode}/suppliers")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.SupplierSearchResponse>> suppliers(
            @PathVariable String materialCode,
            @RequestParam(required = false) Integer requiredQuantity,
            @RequestParam(required = false) String certification) {

        MaterialDto.SupplierSearchResponse result =
                materialService.findSuppliers(materialCode, requiredQuantity, certification);
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(result));
    }

    /** GET /api/materials/{id} — 상세 */
    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(materialService.get(id)));
    }

    /** POST /api/materials — 공급 원료 등록 (SUPPLIER) */
    @PostMapping
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> create(
            @Valid @RequestBody MaterialDto.MaterialRequest request,
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        requireSupplier(role);
        MaterialDto.MaterialResponse created = materialService.create(supplierId, email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MaterialDto.ApiResponse.success("공급 품목을 등록했습니다", created));
    }

    /** PUT /api/materials/{id} — 원료 정보 수정 (본인 것만) */
    @PutMapping("/{id}")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MaterialDto.MaterialRequest request,
            @RequestHeader("X-User-Id") Long supplierId) {

        return ResponseEntity.ok(MaterialDto.ApiResponse.success(
                "수정했습니다", materialService.update(id, supplierId, request)));
    }

    /** PATCH /api/materials/{id}/capacity — 여유 생산능력 갱신 (Ep-02 US2) */
    @PatchMapping("/{id}/capacity")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> updateCapacity(
            @PathVariable Long id,
            @Valid @RequestBody MaterialDto.CapacityRequest request,
            @RequestHeader("X-User-Id") Long supplierId) {

        return ResponseEntity.ok(MaterialDto.ApiResponse.success(
                "여유 생산능력을 갱신했습니다",
                materialService.updateCapacity(id, supplierId, request.getAvailableCapacity())));
    }

    /** DELETE /api/materials/{id} — 공급 중단 (Soft Delete) */
    @DeleteMapping("/{id}")
    public ResponseEntity<MaterialDto.ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long supplierId) {

        materialService.deactivate(id, supplierId);
        return ResponseEntity.ok(MaterialDto.ApiResponse.success("공급을 중단했습니다", null));
    }

    // ===== 내부 API: order-service 전용. Gateway 에 라우트를 열지 않는다. =====

    @GetMapping("/internal/{id}")
    public ResponseEntity<MaterialDto.MaterialResponse> internalGet(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.get(id));
    }

    @PostMapping("/internal/{id}/reserve")
    public ResponseEntity<MaterialDto.MaterialResponse> internalReserve(
            @PathVariable Long id, @Valid @RequestBody MaterialDto.ReserveRequest request) {
        return ResponseEntity.ok(materialService.reserveCapacity(id, request.getQuantity()));
    }

    @PostMapping("/internal/{id}/release")
    public ResponseEntity<MaterialDto.MaterialResponse> internalRelease(
            @PathVariable Long id, @Valid @RequestBody MaterialDto.ReserveRequest request) {
        return ResponseEntity.ok(materialService.releaseCapacity(id, request.getQuantity()));
    }

    // ===== 공통 =====

    private void requireSupplier(String rawRole) {
        CallerRole role = CallerRole.of(rawRole);
        if (role != null && role != CallerRole.SUPPLIER) {
            throw new SecurityException("공급 공장 계정만 원료를 등록할 수 있습니다");
        }
    }

    private PageRequest pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }
}
