package com.soodfolio.api.config;

import com.soodfolio.api.service.storage.CloudinaryStorageProvider;
import com.soodfolio.api.service.storage.FileStorageProvider;
import com.soodfolio.api.service.storage.LocalFileStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.file.Paths;

@Configuration
@Slf4j
public class FileStorageConfig {

    @Bean
    public FileStorageProvider fileStorageProvider(
            @Value("${app.cloudinary.url:}") String cloudinaryUrl,
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        if (StringUtils.hasText(cloudinaryUrl) && cloudinaryUrl.trim().startsWith("cloudinary://")) {
            log.info("Cloudinary URL detected. Initializing CloudinaryStorageProvider.");
            return new CloudinaryStorageProvider(cloudinaryUrl.trim());
        }
        log.info("Cloudinary URL not configured or invalid. Initializing LocalFileStorageProvider (dir: {}).", uploadDir);
        return new LocalFileStorageProvider(Paths.get(uploadDir).toAbsolutePath().normalize());
    }
}
