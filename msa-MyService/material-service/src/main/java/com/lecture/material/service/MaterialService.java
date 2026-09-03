package com.lecture.material.service;

import com.lecture.material.dto.MaterialDto;
import com.lecture.material.entity.Material;
import com.lecture.material.repository.MaterialRepository;
import com.lecture.material.repository.MaterialSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final UserServiceClient userServiceClient;

    // ===== 조회 =====

    /** GET /api/materials — 공급 중(ACTIVE)인 원료만 검색된다. */
    public Page<MaterialDto.MaterialResponse> search(String keyword, Material.Category category,
                                                     String certification, Integer minCapacity,
                                                     Pageable pageable) {
        Specification<Material> spec = Specification.where(MaterialSpecs.status(Material.Status.ACTIVE))
                .and(MaterialSpecs.keyword(keyword))
                .and(MaterialSpecs.category(category))
                .and(MaterialSpecs.certification(certification))
                .and(MaterialSpecs.minCapacity(minCapacity));

        return materialRepository.findAll(spec, pageable).map(MaterialDto.MaterialResponse::from);
    }

    public MaterialDto.MaterialResponse get(Long id) {
        return MaterialDto.MaterialResponse.from(findById(id));
    }

    /** GET /api/materials/my — 공급사 본인 품목. INACTIVE 도 보여야 한다. */
    public Page<MaterialDto.MaterialResponse> getMy(Long supplierId, Pageable pageable) {
        return materialRepository.findBySupplierId(supplierId, pageable)
                .map(MaterialDto.MaterialResponse::from);
    }

    /**
     * Ep-01 US1: 공급 가능한 인증 공장 조회.
     * 필요 수량을 넘기면 그 물량을 실제로 댈 수 있는 공장만 남긴다.
     * 결과가 0건인 것은 에러가 아니라 정상 응답이다.
     */
    public MaterialDto.SupplierSearchResponse findSuppliers(String materialCode, Integer requiredQuantity,
                                                            String certification) {
        Specification<Material> spec = Specification.where(MaterialSpecs.status(Material.Status.ACTIVE))
                .and(MaterialSpecs.materialCode(materialCode))
                .and(MaterialSpecs.certification(certification))
                .and(MaterialSpecs.minCapacity(requiredQuantity));

        List<MaterialDto.SupplierOption> suppliers = materialRepository.findAll(spec).stream()
                .sorted(Comparator.comparing(Material::getLeadTimeDays)
                        .thenComparing(Material::getUnitPrice))
                .map(MaterialDto.SupplierOption::from)
                .toList();

        return MaterialDto.SupplierSearchResponse.builder()
                .materialCode(materialCode)
                .totalCount(suppliers.size())
                .suppliers(suppliers)
                .build();
    }

    // ===== 변경 =====

    @Transactional
    public MaterialDto.MaterialResponse create(Long supplierId, String fallbackName,
                                               MaterialDto.MaterialRequest request) {
        if (materialRepository.existsByMaterialCodeAndSupplierId(request.getMaterialCode(), supplierId)) {
            throw new IllegalArgumentException("이미 등록한 원료코드입니다: " + request.getMaterialCode());
        }

        // 표시할 공급사명은 user-service 의 소속(companyName)을 우선한다.
        String supplierName = userServiceClient.resolveSupplierName(supplierId, fallbackName);

        Material material = Material.builder()
                .materialCode(request.getMaterialCode())
                .materialName(request.getMaterialName())
                .casNumber(request.getCasNumber())
                .category(request.getCategory())
                .unit(request.getUnit())
                .unitPrice(request.getUnitPrice())
                .minOrderQuantity(request.getMinOrderQuantity())
                .availableCapacity(request.getAvailableCapacity())
                .leadTimeDays(request.getLeadTimeDays())
                .certifications(normalize(request.getCertifications()))
                .country(request.getCountry())
                .description(request.getDescription())
                .status(Material.Status.ACTIVE)
                .supplierId(supplierId)
                .supplierName(supplierName)
                .build();

        Material saved = materialRepository.save(material);
        log.info("[MaterialService] 공급 품목 등록 - id: {}, code: {}, supplierId: {}",
                saved.getId(), saved.getMaterialCode(), supplierId);
        return MaterialDto.MaterialResponse.from(saved);
    }

    @Transactional
    public MaterialDto.MaterialResponse update(Long id, Long supplierId, MaterialDto.MaterialRequest request) {
        Material material = findOwned(id, supplierId);
        material.update(
                request.getMaterialName(), request.getCasNumber(), request.getCategory(), request.getUnit(),
                request.getUnitPrice(), request.getMinOrderQuantity(), request.getAvailableCapacity(),
                request.getLeadTimeDays(), normalize(request.getCertifications()),
                request.getCountry(), request.getDescription());
        return MaterialDto.MaterialResponse.from(material);
    }

    /** Ep-02 US2: 여유 생산능력만 갱신 */
    @Transactional
    public MaterialDto.MaterialResponse updateCapacity(Long id, Long supplierId, Integer availableCapacity) {
        Material material = findOwned(id, supplierId);
        material.changeCapacity(availableCapacity);
        return MaterialDto.MaterialResponse.from(material);
    }

    /** 공급 중단 (Soft Delete) — 기존 주문 이력이 남아야 하므로 삭제하지 않는다. */
    @Transactional
    public void deactivate(Long id, Long supplierId) {
        findOwned(id, supplierId).deactivate();
    }

    // ===== 내부 호출 (order-service) =====

    @Transactional
    public MaterialDto.MaterialResponse reserveCapacity(Long id, int quantity) {
        Material material = findById(id);
        if (material.getStatus() != Material.Status.ACTIVE) {
            throw new IllegalArgumentException("공급이 중단된 원료입니다");
        }
        material.reserveCapacity(quantity);
        log.info("[MaterialService] 생산능력 차감 - id: {}, quantity: {}, 남은 capacity: {}",
                id, quantity, material.getAvailableCapacity());
        return MaterialDto.MaterialResponse.from(material);
    }

    @Transactional
    public MaterialDto.MaterialResponse releaseCapacity(Long id, int quantity) {
        Material material = findById(id);
        material.releaseCapacity(quantity);
        log.info("[MaterialService] 생산능력 복원 - id: {}, quantity: {}", id, quantity);
        return MaterialDto.MaterialResponse.from(material);
    }

    // ===== 내부 유틸 =====

    private Material findById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("원료를 찾을 수 없습니다: " + id));
    }

    private Material findOwned(Long id, Long supplierId) {
        Material material = findById(id);
        if (!material.isOwnedBy(supplierId)) {
            throw new SecurityException("본인이 등록한 원료만 수정할 수 있습니다");
        }
        return material;
    }

    private List<String> normalize(List<String> certifications) {
        if (certifications == null) return List.of();
        return certifications.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toUpperCase())
                .distinct()
                .toList();
    }
}
