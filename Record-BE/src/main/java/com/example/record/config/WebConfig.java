package com.example.record.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.profile-image-dir:uploads/profile-images}")
    private String profileImageDir;

    @Value("${app.upload.generated-image-dir:uploads/generated-images}")
    private String generatedImageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String profileImagePath = Paths.get(profileImageDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();   // 예: file:/Users/.../uploads/profile-images/

        String generatedImagePath = Paths.get(generatedImageDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();   // 예: file:/Users/.../uploads/generated-images/

        registry.addResourceHandler("/uploads/profile-images/**")
                .addResourceLocations(profileImagePath);

        registry.addResourceHandler("/uploads/generated-images/**")
                .addResourceLocations(generatedImagePath);
    }
}
