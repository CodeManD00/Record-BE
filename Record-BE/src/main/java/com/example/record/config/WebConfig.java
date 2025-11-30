package com.example.record.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// S3로 마이그레이션 완료 - 로컬 파일 서빙 비활성화
// import org.springframework.beans.factory.annotation.Value;
// import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // S3로 마이그레이션 완료 - 로컬 파일 서빙 비활성화
    // @Value("${app.upload.profile-image-dir:uploads/profile-images}")
    // private String profileImageDir;
    //
    // @Value("${app.upload.generated-image-dir:uploads/generated-images}")
    // private String generatedImageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // S3로 마이그레이션 완료 - 로컬 파일 서빙 비활성화
        // 모든 이미지는 S3에서 제공되므로 로컬 uploads 폴더는 더 이상 필요 없음
        // Path baseDir = PathUtils.getRecordBEDir();
        // 
        // // 절대 경로 가져오기
        // Path profileImageAbsolutePath = baseDir.resolve(profileImageDir).normalize();
        // Path generatedImageAbsolutePath = baseDir.resolve(generatedImageDir).normalize();
        // 
        // // file: URI 형식으로 변환 (끝에 / 필수)
        // String profileImagePath = profileImageAbsolutePath.toUri().toString();
        // if (!profileImagePath.endsWith("/")) {
        //     profileImagePath += "/";
        // }
        // 
        // String generatedImagePath = generatedImageAbsolutePath.toUri().toString();
        // if (!generatedImagePath.endsWith("/")) {
        //     generatedImagePath += "/";
        // }
        //
        // // ResourceHandler 등록
        // registry.addResourceHandler("/uploads/profile-images/**")
        //         .addResourceLocations(profileImagePath)
        //         .setCachePeriod(3600);
        //
        // registry.addResourceHandler("/uploads/generated-images/**")
        //         .addResourceLocations(generatedImagePath)
        //         .setCachePeriod(3600);
    }
}
