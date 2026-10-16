package com.soodfolio.api.controller;

import com.soodfolio.api.config.I18nConfig;
import com.soodfolio.api.service.I18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/i18n")
@RequiredArgsConstructor
@Tag(name = "Internationalization", description = "Bilingual (EN / VI) Localization and Translation APIs")
public class I18nController {

    private final I18nService i18nService;

    @GetMapping("/locale")
    @Operation(summary = "Get current resolved locale and list of supported languages")
    public ResponseEntity<LocaleInfoResponse> getCurrentLocale() {
        Locale current = i18nService.getCurrentLocale();
        return ResponseEntity.ok(LocaleInfoResponse.builder()
                .currentLocale(current.getLanguage())
                .displayName(current.getDisplayName(current))
                .isDefault(current.equals(I18nConfig.DEFAULT_LOCALE))
                .supportedLocales(i18nService.getSupportedLanguages())
                .build());
    }

    @GetMapping("/translations")
    @Operation(summary = "Get complete dictionary of translations for specified or active language")
    public ResponseEntity<TranslationsResponse> getTranslations(
            @RequestParam(value = "lang", required = false) String lang
    ) {
        Locale locale = (lang != null && lang.equalsIgnoreCase("vi"))
                ? Locale.forLanguageTag("vi")
                : (lang != null && lang.equalsIgnoreCase("en"))
                ? Locale.ENGLISH
                : i18nService.getCurrentLocale();

        Map<String, String> dictionary = i18nService.getAllTranslations(locale);

        return ResponseEntity.ok(TranslationsResponse.builder()
                .locale(locale.getLanguage())
                .totalKeys(dictionary.size())
                .translations(dictionary)
                .build());
    }

    @Data
    @Builder
    public static class LocaleInfoResponse {
        private String currentLocale;
        private String displayName;
        private boolean isDefault;
        private List<String> supportedLocales;
    }

    @Data
    @Builder
    public static class TranslationsResponse {
        private String locale;
        private int totalKeys;
        private Map<String, String> translations;
    }
}
