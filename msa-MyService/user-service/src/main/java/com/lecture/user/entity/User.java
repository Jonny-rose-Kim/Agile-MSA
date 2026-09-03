package com.lecture.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    /**
     * 인증 서버 호환용 컬럼. <b>값을 변경하면 로그인이 깨진다.</b>
     *
     * auth-server(사전 빌드 이미지, 소스 없음)가 같은 users 테이블을
     * {@code @Enumerated(EnumType.STRING)} + {@code enum Role { STUDENT, INSTRUCTOR }} 로 매핑한다.
     * 이 컬럼에 BUYER/SUPPLIER가 저장되면 auth-server가 사용자를 로드할 때
     * "No enum constant ... BUYER" 예외로 로그인이 실패한다.
     *
     * 따라서 도메인 역할은 아래 {@link #userRole} 컬럼에 따로 저장하고,
     * 이 컬럼에는 {@link #toAuthRole(UserRole)}로 변환한 값만 넣는다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** 서비스 도메인 역할. 프론트엔드가 사용하는 실제 역할 값이다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 20)
    private UserRole userRole;

    /** 소속 기관명 (제약사·연구실) 또는 공장명 (공급 공장) */
    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "business_number", length = 50)
    private String businessNumber;

    /** GMP 인증 보유 여부. 공급 공장(SUPPLIER)에만 의미가 있다. */
    @Column(name = "gmp_certified")
    private Boolean gmpCertified;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** auth-server가 읽는 값. 절대 확장하지 말 것. */
    public enum Role {
        STUDENT, INSTRUCTOR
    }

    /** 원료의약품 수급 매칭 도메인 역할 */
    public enum UserRole {
        BUYER,    // 제약사 · 연구실
        SUPPLIER  // 공급 공장
    }

    /** 도메인 역할을 auth-server 호환 값으로 변환한다. */
    public static Role toAuthRole(UserRole userRole) {
        return userRole == UserRole.SUPPLIER ? Role.INSTRUCTOR : Role.STUDENT;
    }

    /**
     * 응답에 실을 도메인 역할.
     * auth-server가 만든 시드 계정처럼 user_role이 비어 있는 기존 행은
     * role 컬럼에서 역으로 유추해 준다. (INSTRUCTOR → SUPPLIER, 그 외 → BUYER)
     */
    public UserRole resolveUserRole() {
        if (userRole != null) {
            return userRole;
        }
        return role == Role.INSTRUCTOR ? UserRole.SUPPLIER : UserRole.BUYER;
    }
}
