package com.lecture.order.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * internal 경로(/api/{도메인}/internal/...)는 서비스 간 호출 전용이다.
 *
 * API Gateway 의 라우트가 /api/{도메인}/** 전체를 넘기고 discovery locator 도 켜져 있어서,
 * 경로만으로는 외부 노출을 막을 수 없다. (로그인한 사용자라면 누구나 호출할 수 있다)
 * 그래서 호출하는 서비스만 아는 키를 요구한다.
 *
 * 운영에서는 INTERNAL_API_KEY 를 실제 비밀값으로 주입하고,
 * 가능하면 Client Credentials 토큰(SCOPE_service.*)으로 대체하는 편이 낫다.
 */
@Slf4j
@Component
public class InternalApiInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Internal-Key";

    private final String apiKey;

    public InternalApiInterceptor(@Value("${internal.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (apiKey.equals(request.getHeader(HEADER))) {
            return true;
        }
        log.warn("[InternalApiInterceptor] 내부 API 외부 호출 차단 - path: {}, from: {}",
                request.getRequestURI(), request.getRemoteAddr());

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write("{\"success\":false,\"message\":\"존재하지 않는 경로입니다\",\"data\":null}");
        } catch (Exception ignored) {
            // 응답을 쓰지 못해도 상태 코드는 이미 404 로 내려간다
        }
        return false;
    }
}
