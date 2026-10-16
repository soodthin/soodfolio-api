package com.soodfolio.api.service;

import com.soodfolio.api.config.I18nConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class I18nServiceTest {

    private I18nService i18nService;
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setDefaultLocale(I18nConfig.DEFAULT_LOCALE);
        ms.setFallbackToSystemLocale(false);

        messageSource = ms;
        i18nService = new I18nService(messageSource);
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Should resolve English by default when no locale is set")
    void shouldResolveEnglishByDefault() {
        Locale current = i18nService.getCurrentLocale();
        assertThat(current).isEqualTo(Locale.ENGLISH);

        String msg = i18nService.getMessage("common.success");
        assertThat(msg).isEqualTo("Success");
    }

    @Test
    @DisplayName("Should resolve Vietnamese message when context locale is vi")
    void shouldResolveVietnameseWhenLocaleIsVi() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));

        String msg = i18nService.getMessage("common.success");
        assertThat(msg).isEqualTo("Thành công");

        String authMsg = i18nService.getMessage("auth.unauthorized");
        assertThat(authMsg).isEqualTo("Yêu cầu đăng nhập để thực hiện thao tác này.");
    }

    @Test
    @DisplayName("Should resolve English message when explicitly querying with Locale.ENGLISH")
    void shouldResolveExplicitLocaleEnglish() {
        String msg = i18nService.getMessage("auth.unauthorized", Locale.ENGLISH);
        assertThat(msg).isEqualTo("Authentication required. Please log in to continue.");
    }

    @Test
    @DisplayName("Should format parameter arguments in message correctly")
    void shouldFormatArgumentsCorrectly() {
        String enMsg = i18nService.getMessage("upload.size_exceeded", Locale.ENGLISH, 5);
        assertThat(enMsg).isEqualTo("File size exceeds maximum allowed limit (5MB).");

        String viMsg = i18nService.getMessage("upload.size_exceeded", Locale.forLanguageTag("vi"), 5);
        assertThat(viMsg).isEqualTo("Kích thước tệp vượt quá giới hạn tối đa (5MB).");
    }

    @Test
    @DisplayName("Should return code as fallback when key does not exist in bundle")
    void shouldReturnCodeWhenKeyNotFound() {
        String missing = i18nService.getMessage("non.existent.key");
        assertThat(missing).isEqualTo("non.existent.key");
    }

    @Test
    @DisplayName("Should return full translation dictionary for English and Vietnamese")
    void shouldReturnAllTranslations() {
        Map<String, String> enDict = i18nService.getAllTranslations(Locale.ENGLISH);
        assertThat(enDict).isNotEmpty();
        assertThat(enDict.get("common.success")).isEqualTo("Success");

        Map<String, String> viDict = i18nService.getAllTranslations(Locale.forLanguageTag("vi"));
        assertThat(viDict).isNotEmpty();
        assertThat(viDict.get("common.success")).isEqualTo("Thành công");
    }
}
