package com.soodfolio.api.controller;

import com.soodfolio.api.dto.ExperienceDtos.ExperienceReorderRequestDto;
import com.soodfolio.api.dto.ExperienceDtos.ExperienceRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.ExperienceDto;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/portfolios/me/experiences")
@RequiredArgsConstructor
@Tag(name = "Experiences", description = "Career Experience APIs for Creators")
@SecurityRequirement(name = "bearerAuth")
public class ExperienceController {

    private final ExperienceService experienceService;

    @GetMapping
    @Operation(summary = "Get all career experiences for current creator")
    public ResponseEntity<List<ExperienceDto>> getMyExperiences(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(experienceService.getExperiencesByUserId(userPrincipal.getId()));
    }

    @GetMapping("/{experienceId}")
    @Operation(summary = "Get single career experience by ID")
    public ResponseEntity<ExperienceDto> getExperienceById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID experienceId
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(experienceService.getExperienceById(userPrincipal.getId(), experienceId));
    }

    @PostMapping
    @Operation(summary = "Create a new career experience entry")
    public ResponseEntity<ExperienceDto> createExperience(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ExperienceRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        ExperienceDto created = experienceService.createExperience(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{experienceId}")
    @Operation(summary = "Update an existing career experience entry")
    public ResponseEntity<ExperienceDto> updateExperience(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID experienceId,
            @Valid @RequestBody ExperienceRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        ExperienceDto updated = experienceService.updateExperience(userPrincipal.getId(), experienceId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{experienceId}")
    @Operation(summary = "Delete a career experience entry (soft delete)")
    public ResponseEntity<Void> deleteExperience(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID experienceId
    ) {
        validateAuthenticated(userPrincipal);
        experienceService.deleteExperience(userPrincipal.getId(), experienceId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder career experiences")
    public ResponseEntity<List<ExperienceDto>> reorderExperiences(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ExperienceReorderRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        List<ExperienceDto> reordered = experienceService.reorderExperiences(userPrincipal.getId(), request.getExperienceIds());
        return ResponseEntity.ok(reordered);
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
    }
}
