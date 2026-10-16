package com.soodfolio.api.service;

import com.soodfolio.api.config.I18nConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class I18nService {

    private final MessageSource messageSource;

    public String getMessage(String code, Object... args) {
        Locale locale = getCurrentLocale();
        return getMessage(code, locale, args);
    }

    public String getMessage(String code, Locale locale, Object... args) {
        if (code == null) {
            return "";
        }
        try {
            return messageSource.getMessage(code, args, code, locale != null ? locale : I18nConfig.DEFAULT_LOCALE);
        } catch (Exception e) {
            return code;
        }
    }

    public Locale getCurrentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null || locale.getLanguage().isBlank()) {
            return I18nConfig.DEFAULT_LOCALE;
        }
        return locale.getLanguage().equalsIgnoreCase("vi") ? Locale.forLanguageTag("vi") : Locale.ENGLISH;
    }

    public List<String> getSupportedLanguages() {
        return List.of("en", "vi");
    }

    public Map<String, String> getAllTranslations(Locale targetLocale) {
        Locale locale = targetLocale != null ? targetLocale : getCurrentLocale();
        String lang = locale.getLanguage().equalsIgnoreCase("vi") ? "vi" : "en";
        String filename = lang.equals("vi") ? "messages_vi.properties" : "messages.properties";

        Map<String, String> map = new LinkedHashMap<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:" + filename);
            if (resource.exists()) {
                Properties props = new Properties();
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
                for (String key : props.stringPropertyNames()) {
                    map.put(key, props.getProperty(key));
                }
            }
        } catch (Exception e) {
            log.error("Failed to load translation properties for locale [{}]: {}", locale, e.getMessage());
        }
        return map;
    }
}
