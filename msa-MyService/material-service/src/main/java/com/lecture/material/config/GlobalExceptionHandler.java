package com.lecture.material.config;

import com.lecture.material.dto.MaterialDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MaterialDto.ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(MaterialDto.ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<MaterialDto.ApiResponse<Void>> handleSecurity(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(MaterialDto.ApiResponse.error(e.getMessage()));
    }

    /** Gateway 를 거치지 않고 직접 호출하면 X-User-Id 가 없다. 원인을 분명히 알려준다. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<MaterialDto.ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MaterialDto.ApiResponse.error("인증 정보가 없습니다 (" + e.getHeaderName() + ")"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MaterialDto.ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(MaterialDto.ApiResponse.error(message));
    }

    /** 존재하지 않는 경로는 404 로 돌려준다. (500 으로 덮이면 원인 파악이 어렵다) */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<MaterialDto.ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MaterialDto.ApiResponse.error("존재하지 않는 경로입니다"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MaterialDto.ApiResponse<Void>> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MaterialDto.ApiResponse.error("서버 오류가 발생했습니다"));
    }
}
