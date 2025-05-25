package com.example.record;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        System.out.println("=== Authorization 헤더: " + header);

        if (header == null || !header.startsWith("Bearer ")) {
            System.out.println(">>> Authorization 헤더 누락 또는 잘못된 형식");
            try {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid Authorization header");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        String apiKey = header.substring(7);
        System.out.println("=== 추출된 API 키: " + apiKey);

        boolean exists = apiKeyRepository.existsByApiKey(apiKey);
        System.out.println("=== DB에 키 존재 여부: " + exists);

        if (!exists) {
            System.out.println(">>> 존재하지 않는 API 키");
            try {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API Key");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        return true;
    }

}
