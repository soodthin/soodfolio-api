package com.soodfolio.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class I18nConfig {

    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH,
            Locale.forLanguageTag("vi")
    );

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(DEFAULT_LOCALE);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new LocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                // 1. Query parameter (?lang=vi or ?lang=en)
                String langParam = request.getParameter("lang");
                if (StringUtils.hasText(langParam)) {
                    if (langParam.trim().equalsIgnoreCase("vi")) {
                        return Locale.forLanguageTag("vi");
                    } else if (langParam.trim().equalsIgnoreCase("en")) {
                        return Locale.ENGLISH;
                    }
                }

                // 2. Accept-Language HTTP Header
                String acceptLanguage = request.getHeader("Accept-Language");
                if (StringUtils.hasText(acceptLanguage)) {
                    try {
                        List<Locale.LanguageRange> list = Locale.LanguageRange.parse(acceptLanguage);
                        Locale matched = Locale.lookup(list, SUPPORTED_LOCALES);
                        if (matched != null) {
                            return matched;
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Invalid header format fallback
                    }
                }

                // 3. Fallback to English (Default)
                return DEFAULT_LOCALE;
            }

            @Override
            public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
                // Stateless REST API - No-op
            }
        };
    }
}
