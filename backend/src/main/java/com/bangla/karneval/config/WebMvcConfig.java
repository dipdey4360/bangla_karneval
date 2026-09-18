package com.bangla.karneval.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @org.springframework.beans.factory.annotation.Value("${app.upload-root:/app/uploads}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(java.nio.file.Paths.get(uploadRoot).toAbsolutePath().toUri().toString().replaceAll("/?$", "/"));
    }
}
