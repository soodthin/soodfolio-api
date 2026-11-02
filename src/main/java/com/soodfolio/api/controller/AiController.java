package com.soodfolio.api.controller;

import com.soodfolio.api.dto.AiDtos.AiPolishRequestDto;
import com.soodfolio.api.dto.AiDtos.AiPolishResponseDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestRequestDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestResponseDto;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.AiRateLimiter;
import com.soodfolio.api.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Studio", description = "AI Studio APIs for Portfolio Polish and Theme Matching")
public class AiController {

    private final AiService aiService;
    private final AiRateLimiter aiRateLimiter;

    @PostMapping("/polish")
    @Operation(
            summary = "Polish portfolio text with Google X-Y-Z formula and ATS standards",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AiPolishResponseDto> polishText(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AiPolishRequestDto request
    ) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để sử dụng tính năng AI.");
        }
        aiRateLimiter.checkRateLimit(userPrincipal.getId());
        AiPolishResponseDto response = aiService.polishText(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/suggest-theme")
    @Operation(
            summary = "Suggest optimal portfolio theme based on technical profile",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AiThemeSuggestResponseDto> suggestTheme(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AiThemeSuggestRequestDto request
    ) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để sử dụng tính năng AI.");
        }
        aiRateLimiter.checkRateLimit(userPrincipal.getId());
        AiThemeSuggestResponseDto response = aiService.suggestTheme(request);
        return ResponseEntity.ok(response);
    }
}
