package com.lecture.payment.support;

/**
 * API Gateway 가 넘기는 X-User-Role 헤더를 서비스 도메인 역할로 정규화한다.
 *
 * 인증 서버(msa-lecture/auth-server)는 users.role 컬럼을 그대로 JWT 에 싣는데,
 * 이 컬럼은 STUDENT / INSTRUCTOR 만 저장할 수 있다(인증 서버 호환 제약).
 * 실제 도메인 역할 BUYER / SUPPLIER 는 users.user_role 에 따로 있고,
 * user-service 의 User.toAuthRole() 이 둘을 매핑한다.
 *
 * 따라서 헤더로는 STUDENT / INSTRUCTOR 가 올라오며, 두 표기를 모두 받아들인다.
 */
public enum CallerRole {
    BUYER,      // 제약사 · 연구실 (JWT 상으로는 STUDENT)
    SUPPLIER;   // 공급 공장     (JWT 상으로는 INSTRUCTOR)

    /** 헤더가 없으면 null 을 돌려준다. 호출자가 검사를 건너뛸지 결정한다. */
    public static CallerRole of(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) return null;
        return switch (rawRole.trim().toUpperCase()) {
            case "BUYER", "STUDENT" -> BUYER;
            case "SUPPLIER", "INSTRUCTOR" -> SUPPLIER;
            default -> null;
        };
    }
}
