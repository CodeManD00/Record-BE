// WebConfig: API 키 인터셉터를 포함한 전역 웹 설정을 담당하는 Spring WebMvc 설정 클래스입니다.

package com.example.record;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    ApiKeyInterceptor apiKeyInterceptor; // API 키 유효성 검사를 위한 인터셉터

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // "/api/image/**" 경로 요청에 대해 ApiKeyInterceptor 적용
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/api/image/**");
    }
}
