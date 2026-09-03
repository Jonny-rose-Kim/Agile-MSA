package com.lecture.user.dto;

import com.lecture.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {

    // 회원가입 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        private String password;

        @NotBlank(message = "담당자 이름은 필수입니다")
        private String name;

        /** BUYER(제약사·연구실) 또는 SUPPLIER(공급 공장) */
        private User.UserRole role;

        @NotBlank(message = "기관명 또는 공장명은 필수입니다")
        private String companyName;

        private String businessNumber;

        /** 공급 공장(SUPPLIER)일 때만 사용한다. */
        private Boolean gmpCertified;
    }

    // 사용자 정보 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserResponse {
        private Long id;
        private String email;
        private String name;

        /**
         * 프론트엔드가 사용하는 도메인 역할(BUYER/SUPPLIER).
         * DB의 role 컬럼(STUDENT/INSTRUCTOR)은 인증 서버 호환용이라 응답에 노출하지 않는다.
         */
        private User.UserRole role;

        private String companyName;
        private String businessNumber;
        private Boolean gmpCertified;
        private LocalDateTime createdAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.resolveUserRole())
                    .companyName(user.getCompanyName())
                    .businessNumber(user.getBusinessNumber())
                    .gmpCertified(user.getGmpCertified())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    // 공통 API 응답 래퍼
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
