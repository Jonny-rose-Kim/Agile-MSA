package com.lecture.material.config;

import com.lecture.material.entity.Material;
import com.lecture.material.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 데모용 초기 원료 데이터.
 *
 * materials 테이블이 비어 있을 때만 1회 적재한다. 이미 데이터가 있으면 아무것도 하지 않으므로
 * 재기동해도 중복되지 않고, 팀원이 볼륨을 새로 만들어도 같은 화면이 재현된다.
 * 운영에 올릴 때는 app.seed.enabled=false 로 끄면 된다.
 *
 * supplierId 2 / 3 은 init-db 의 시드 계정(INSTRUCTOR = 공급 공장)에 대응한다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder {

    private static final Long SUPPLIER_KOREA_API = 3L;   // factory@koreaapi.com / 한국API공장
    private static final Long SUPPLIER_DAEHAN = 2L;      // instructor@lecture.com

    @Bean
    public ApplicationRunner seedMaterials(MaterialRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                log.info("[DataSeeder] 원료 데이터가 이미 있어 시드를 건너뜁니다 ({}건)", repository.count());
                return;
            }

            List<Material> seeds = List.of(
                    // 같은 원료코드를 두 공장이 공급한다 → "공급 가능 인증 공장 조회"(Ep-01 US1) 시연용
                    material("API-CEFA-500", "세파졸린나트륨", "27164-46-1",
                            Material.Category.API_INGREDIENT, Material.Unit.KG,
                            42000, 100, 12000, 14, List.of("GMP", "DMF"), "KR",
                            SUPPLIER_KOREA_API, "한국API공장",
                            "주사용 세팔로스포린계 항생제 원료. cGMP 생산 라인."),
                    material("API-CEFA-500", "세파졸린나트륨", "27164-46-1",
                            Material.Category.API_INGREDIENT, Material.Unit.KG,
                            39500, 200, 4500, 21, List.of("GMP"), "KR",
                            SUPPLIER_DAEHAN, "대한파마텍",
                            "동일 규격 대체 공급처. 단가는 낮으나 리드타임이 길다."),

                    material("API-AMOX-250", "아목시실린삼수화물", "61336-70-7",
                            Material.Category.API_INGREDIENT, Material.Unit.KG,
                            58000, 50, 8200, 18, List.of("GMP", "DMF", "KGMP"), "KR",
                            SUPPLIER_KOREA_API, "한국API공장",
                            "경구용 페니실린계 항생제 원료."),
                    material("API-METF-850", "메트포르민염산염", "1115-70-4",
                            Material.Category.API_INGREDIENT, Material.Unit.KG,
                            21000, 100, 15000, 16, List.of("GMP", "DMF"), "IN",
                            SUPPLIER_KOREA_API, "한국API공장",
                            "제2형 당뇨 치료제 원료. 대량 생산 가능."),
                    material("EXC-LAC-200", "유당수화물", "64044-51-5",
                            Material.Category.EXCIPIENT, Material.Unit.KG,
                            3200, 500, 60000, 7, List.of("GMP", "ISO9001"), "KR",
                            SUPPLIER_KOREA_API, "한국API공장",
                            "정제용 희석제. 직타용 그레이드."),
                    material("INT-PIP-041", "피페라진중간체", "110-85-0",
                            Material.Category.INTERMEDIATE, Material.Unit.KG,
                            8900, 100, 3000, 12, List.of("GMP"), "CN",
                            SUPPLIER_KOREA_API, "한국API공장",
                            "합성 중간체. 수입 통관 기간이 리드타임에 포함된다."),

                    material("EXC-MCC-101", "미결정셀룰로오스", "9004-34-6",
                            Material.Category.EXCIPIENT, Material.Unit.KG,
                            5400, 300, 25000, 10, List.of("GMP", "ISO9001"), "KR",
                            SUPPLIER_DAEHAN, "대한파마텍",
                            "정제 결합제 및 붕해제."),
                    material("EXC-MGST-01", "스테아르산마그네슘", "557-04-0",
                            Material.Category.EXCIPIENT, Material.Unit.KG,
                            6800, 100, 11000, 8, List.of("GMP"), "KR",
                            SUPPLIER_DAEHAN, "대한파마텍",
                            "활택제. 식물성 원료."),
                    material("SOL-ETH-999", "무수에탄올", "64-17-5",
                            Material.Category.SOLVENT, Material.Unit.L,
                            2800, 1000, 90000, 5, List.of("ISO9001"), "KR",
                            SUPPLIER_DAEHAN, "대한파마텍",
                            "99.9% 이상. 위험물 운송 규정 적용."),
                    material("REA-HPLC-01", "HPLC 등급 아세토니트릴", "75-05-8",
                            Material.Category.REAGENT, Material.Unit.L,
                            12500, 20, 1800, 9, List.of("ISO9001"), "DE",
                            SUPPLIER_DAEHAN, "대한파마텍",
                            "품질관리 시험용 시약.")
            );

            repository.saveAll(seeds);
            log.info("[DataSeeder] 데모 원료 {}건을 적재했습니다", seeds.size());
        };
    }

    private static Material material(String code, String name, String cas,
                                     Material.Category category, Material.Unit unit,
                                     int unitPrice, int minOrder, int capacity, int leadTime,
                                     List<String> certifications, String country,
                                     Long supplierId, String supplierName, String description) {
        return Material.builder()
                .materialCode(code)
                .materialName(name)
                .casNumber(cas)
                .category(category)
                .unit(unit)
                .unitPrice(unitPrice)
                .minOrderQuantity(minOrder)
                .availableCapacity(capacity)
                .leadTimeDays(leadTime)
                .certifications(certifications)
                .country(country)
                .description(description)
                .status(Material.Status.ACTIVE)
                .supplierId(supplierId)
                .supplierName(supplierName)
                .build();
    }
}
