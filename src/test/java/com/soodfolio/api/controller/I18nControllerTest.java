package com.soodfolio.api.controller;

import com.soodfolio.api.service.I18nService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class I18nControllerTest {

    @Mock
    private I18nService i18nService;

    @InjectMocks
    private I18nController i18nController;

    @Test
    @DisplayName("Should return current locale information")
    void shouldReturnCurrentLocaleInfo() {
        when(i18nService.getCurrentLocale()).thenReturn(Locale.ENGLISH);
        when(i18nService.getSupportedLanguages()).thenReturn(List.of("en", "vi"));

        ResponseEntity<I18nController.LocaleInfoResponse> response = i18nController.getCurrentLocale();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCurrentLocale()).isEqualTo("en");
        assertThat(response.getBody().isDefault()).isTrue();
        assertThat(response.getBody().getSupportedLocales()).containsExactly("en", "vi");
    }

    @Test
    @DisplayName("Should return dictionary of translations for requested language")
    void shouldReturnTranslationsDictionary() {
        when(i18nService.getAllTranslations(any(Locale.class)))
                .thenReturn(Map.of("common.success", "Thành công", "auth.unauthorized", "Yêu cầu đăng nhập"));

        ResponseEntity<I18nController.TranslationsResponse> response = i18nController.getTranslations("vi");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLocale()).isEqualTo("vi");
        assertThat(response.getBody().getTotalKeys()).isEqualTo(2);
        assertThat(response.getBody().getTranslations()).containsEntry("common.success", "Thành công");
    }
}
