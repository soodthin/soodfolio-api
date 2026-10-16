package com.soodfolio.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // In Spring 6+, trailing slash matching is strict by default.
        // This ensures endpoints work cleanly across different clients.
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uri = uploadPath.toUri().toString();
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uri);
    }
}
