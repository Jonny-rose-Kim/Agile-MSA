package com.lecture.material.service;

import com.lecture.material.client.UserClient;
import com.lecture.material.dto.MaterialDto;
import com.lecture.material.entity.Material;
import com.lecture.material.exception.ApiException;
import com.lecture.material.repository.MaterialRepository;
import com.lecture.material.repository.MaterialSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository repository;
    private final UserClient userClient;

    // ===== 3-1. 목록 / 검색 =====
    public MaterialDto.PageResponse<MaterialDto.MaterialResponse> search(
            String keyword, Material.Category category, String certification,
            Integer minCapacity, Material.Status status, Pageable pageable) {

        Material.Status effectiveStatus = status != null ? status : Material.Status.ACTIVE;

        Specification<Material> spec = Specification.where(MaterialSpecs.statusEq(effectiveStatus))
                .and(MaterialSpecs.keyword(keyword))
                .and(MaterialSpecs.categoryEq(category))
                .and(MaterialSpecs.hasCertification(certification))
                .and(MaterialSpecs.minCapacity(minCapacity));

        Pageable p = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                 Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<MaterialDto.MaterialResponse> page = repository.findAll(spec, p)
                .map(MaterialDto.MaterialResponse::from);
        return MaterialDto.PageResponse.from(page);
    }

    // ===== 3-2. 상세 =====
    public MaterialDto.MaterialResponse get(Long id) {
        return MaterialDto.MaterialResponse.from(findById(id));
    }

    // ===== 3-3. 공급 가능 인증 공장 목록 =====
    public MaterialDto.SuppliersResponse findSuppliersByCode(
            String materialCode, Integer requiredQuantity, String certification, Sort sort) {

        List<Material> list = new ArrayList<>(
                repository.findByMaterialCodeAndStatus(materialCode, Material.Status.ACTIVE));

        List<MaterialDto.SupplierItem> suppliers = list.stream()
                .filter(m -> requiredQuantity == null || m.getAvailableCapacity() >= requiredQuantity)
                .filter(m -> certification == null || certification.isBlank()
                        || m.getCertifications().contains(certification))
                .sorted(supplierComparator(sort))
                .map(m -> MaterialDto.SupplierItem.from(m, requiredQuantity))
                .toList();

        String materialName = list.isEmpty() ? null : list.get(0).getMaterialName();

        return MaterialDto.SuppliersResponse.builder()
                .materialCode(materialCode)
                .materialName(materialName)
                .requiredQuantity(requiredQuantity)
                .suppliers(suppliers)
                .totalCount(suppliers.size())
                .build();
    }

    private Comparator<Material> supplierComparator(Sort sort) {
        Comparator<Material> byLead = Comparator.comparing(Material::getLeadTimeDays);
        if (sort == null || sort.isUnsorted()) return byLead;
        Sort.Order order = sort.iterator().next();
        Comparator<Material> c = switch (order.getProperty()) {
            case "unitPrice" -> Comparator.comparing(Material::getUnitPrice);
            case "availableCapacity" -> Comparator.comparing(Material::getAvailableCapacity);
            default -> byLead;
        };
        return order.isDescending() ? c.reversed() : c;
    }

    // ===== 3-4. 등록 =====
    @Transactional
    public MaterialDto.MaterialResponse create(MaterialDto.CreateRequest req, Long supplierId, String role) {
        requireSupplier(role);
        if (repository.existsByMaterialCodeAndSupplierId(req.getMaterialCode(), supplierId)) {
            throw ApiException.conflict("이미 등록한 원료코드입니다: " + req.getMaterialCode());
        }
        Material m = Material.builder()
                .materialCode(req.getMaterialCode())
                .materialName(req.getMaterialName())
                .description(req.getDescription())
                .casNumber(req.getCasNumber())
                .category(req.getCategory())
                .unit(req.getUnit())
                .unitPrice(req.getUnitPrice())
                .minOrderQuantity(req.getMinOrderQuantity())
                .supplierId(supplierId)
                .supplierName(userClient.fetchSupplierName(supplierId))
                .country(req.getCountry())
                .availableCapacity(req.getAvailableCapacity())
                .leadTimeDays(req.getLeadTimeDays())
                .certifications(req.getCertifications() == null ? new ArrayList<>() : req.getCertifications())
                .status(Material.Status.ACTIVE)
                .build();
        return MaterialDto.MaterialResponse.from(repository.save(m));
    }

    // ===== 3-5. 수정 =====
    @Transactional
    public MaterialDto.MaterialResponse update(Long id, MaterialDto.UpdateRequest req, Long supplierId, String role) {
        requireSupplier(role);
        Material m = findOwned(id, supplierId);
        m.setMaterialName(req.getMaterialName());
        m.setDescription(req.getDescription());
        m.setCategory(req.getCategory());
        m.setUnit(req.getUnit());
        m.setUnitPrice(req.getUnitPrice());
        m.setMinOrderQuantity(req.getMinOrderQuantity());
        m.setAvailableCapacity(req.getAvailableCapacity());
        m.setLeadTimeDays(req.getLeadTimeDays());
        m.setCountry(req.getCountry());
        m.setCertifications(req.getCertifications() == null ? new ArrayList<>() : req.getCertifications());
        if (req.getStatus() != null) m.setStatus(req.getStatus());
        return MaterialDto.MaterialResponse.from(m);
    }

    // ===== 3-6. 생산능력만 갱신 =====
    @Transactional
    public MaterialDto.MaterialResponse updateCapacity(Long id, Integer availableCapacity, Long supplierId, String role) {
        requireSupplier(role);
        Material m = findOwned(id, supplierId);
        m.setAvailableCapacity(availableCapacity);
        return MaterialDto.MaterialResponse.from(m);
    }

    // ===== 3-7. 내 공급 품목 =====
    public MaterialDto.PageResponse<MaterialDto.MaterialResponse> findMine(
            Long supplierId, Material.Status status, Pageable pageable) {
        Page<Material> page = (status == null)
                ? repository.findBySupplierId(supplierId, pageable)
                : repository.findBySupplierIdAndStatus(supplierId, status, pageable);
        return MaterialDto.PageResponse.from(page.map(MaterialDto.MaterialResponse::from));
    }

    // ===== 3-8. 공급 중단 (soft delete) =====
    @Transactional
    public void stopSupply(Long id, Long supplierId, String role) {
        requireSupplier(role);
        Material m = findOwned(id, supplierId);
        m.setStatus(Material.Status.INACTIVE);
    }

    // ===== 내부용 =====
    public MaterialDto.MaterialResponse getInternal(Long id) {
        return MaterialDto.MaterialResponse.from(findById(id));
    }

    @Transactional
    public void deductCapacity(Long id, int quantity) {
        Material m = findById(id);
        m.setAvailableCapacity(Math.max(0, m.getAvailableCapacity() - quantity));
    }

    // ===== helpers =====
    private Material findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("존재하지 않는 원료입니다: " + id));
    }

    private Material findOwned(Long id, Long supplierId) {
        Material m = findById(id);
        if (!m.getSupplierId().equals(supplierId)) {
            throw ApiException.forbidden("본인이 등록한 원료만 수정할 수 있습니다");
        }
        return m;
    }

    private void requireSupplier(String role) {
        // 게이트웨이가 넣어주는 X-User-Role 값은 STUDENT / INSTRUCTOR 다.
        boolean supplier = "INSTRUCTOR".equalsIgnoreCase(role) || "SUPPLIER".equalsIgnoreCase(role);
        if (!supplier) {
            throw ApiException.forbidden("공급사(SUPPLIER)만 원료를 등록·수정할 수 있습니다");
        }
    }
}
