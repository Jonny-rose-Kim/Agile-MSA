package com.lecture.material.controller;

import com.lecture.material.dto.MaterialDto;
import com.lecture.material.entity.Material;
import com.lecture.material.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService service;

    // 3-1. 목록 / 검색
    @GetMapping
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.PageResponse<MaterialDto.MaterialResponse>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Material.Category category,
            @RequestParam(required = false) String certification,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Material.Status status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(
                service.search(keyword, category, certification, minCapacity, status, pageable)));
    }

    // 3-7. 내 공급 품목  (주의: /{id} 보다 먼저 매핑되도록 위에 둔다)
    @GetMapping("/my")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.PageResponse<MaterialDto.MaterialResponse>>> my(
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestParam(required = false) Material.Status status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(
                service.findMine(supplierId, status, pageable)));
    }

    // 3-3. 공급 가능 인증 공장
    @GetMapping("/code/{materialCode}/suppliers")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.SuppliersResponse>> suppliersByCode(
            @PathVariable String materialCode,
            @RequestParam(required = false) Integer requiredQuantity,
            @RequestParam(required = false) String certification,
            Sort sort) {
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(
                service.findSuppliersByCode(materialCode, requiredQuantity, certification, sort)));
    }

    // 3-2. 상세
    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(service.get(id)));
    }

    // 3-4. 등록
    @PostMapping
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> create(
            @Valid @RequestBody MaterialDto.CreateRequest request,
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MaterialDto.ApiResponse.success(service.create(request, supplierId, role)));
    }

    // 3-5. 수정
    @PutMapping("/{id}")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MaterialDto.UpdateRequest request,
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(service.update(id, request, supplierId, role)));
    }

    // 3-6. 생산능력만 갱신
    @PatchMapping("/{id}/capacity")
    public ResponseEntity<MaterialDto.ApiResponse<MaterialDto.MaterialResponse>> updateCapacity(
            @PathVariable Long id,
            @Valid @RequestBody MaterialDto.CapacityRequest request,
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(MaterialDto.ApiResponse.success(
                service.updateCapacity(id, request.getAvailableCapacity(), supplierId, role)));
    }

    // 3-8. 공급 중단
    @DeleteMapping("/{id}")
    public ResponseEntity<MaterialDto.ApiResponse<Void>> stopSupply(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long supplierId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        service.stopSupply(id, supplierId, role);
        return ResponseEntity.ok(MaterialDto.ApiResponse.<Void>builder()
                .success(true).message("공급이 중단되었습니다").build());
    }

    // 3-9. 내부 전용
    @GetMapping("/internal/{id}")
    public ResponseEntity<MaterialDto.MaterialResponse> getInternal(@PathVariable Long id) {
        return ResponseEntity.ok(service.getInternal(id));
    }

    @GetMapping("/internal/exists/{id}")
    public ResponseEntity<Boolean> exists(@PathVariable Long id) {
        try { service.getInternal(id); return ResponseEntity.ok(true); }
        catch (Exception e) { return ResponseEntity.ok(false); }
    }

    /** 주문 생성 실패 시 차감분 복원 (order-service 보상 경로) */
    @PostMapping("/internal/{id}/capacity-restore")
    public ResponseEntity<Void> restore(@PathVariable Long id, @RequestParam int quantity) {
        service.restoreCapacity(id, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/{id}/capacity-deduct")
    public ResponseEntity<Void> deduct(@PathVariable Long id, @RequestParam int quantity) {
        service.deductCapacity(id, quantity);
        return ResponseEntity.ok().build();
    }
}
