package com.skala.fund.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * dev 전용 정적 리소스 매핑.
 * prod 는 S3 URL 을 그대로 내려주므로 이 매핑이 필요 없다.
 */
@Profile("dev")
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local-path:./uploads}")
    private String localPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /images/** 요청을 로컬 uploads 폴더로 연결한다.
        String location = "file:" + Paths.get(localPath).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/images/**").addResourceLocations(location);
    }
}
