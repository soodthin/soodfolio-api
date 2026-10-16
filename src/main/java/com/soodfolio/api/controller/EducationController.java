package com.soodfolio.api.controller;

import com.soodfolio.api.dto.EducationDtos.EducationReorderRequestDto;
import com.soodfolio.api.dto.EducationDtos.EducationRequestDto;
import com.soodfolio.api.dto.PortfolioResponseDto.EducationDto;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.EducationService;
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
@RequestMapping("/portfolios/me/educations")
@RequiredArgsConstructor
@Tag(name = "Educations", description = "Education & Academic Background APIs for Creators")
@SecurityRequirement(name = "bearerAuth")
public class EducationController {

    private final EducationService educationService;

    @GetMapping
    @Operation(summary = "Get all education entries for current creator")
    public ResponseEntity<List<EducationDto>> getMyEducations(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(educationService.getEducationsByUserId(userPrincipal.getId()));
    }

    @GetMapping("/{educationId}")
    @Operation(summary = "Get single education entry by ID")
    public ResponseEntity<EducationDto> getEducationById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID educationId
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(educationService.getEducationById(userPrincipal.getId(), educationId));
    }

    @PostMapping
    @Operation(summary = "Create a new education entry")
    public ResponseEntity<EducationDto> createEducation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody EducationRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        EducationDto created = educationService.createEducation(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{educationId}")
    @Operation(summary = "Update an existing education entry")
    public ResponseEntity<EducationDto> updateEducation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID educationId,
            @Valid @RequestBody EducationRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        EducationDto updated = educationService.updateEducation(userPrincipal.getId(), educationId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{educationId}")
    @Operation(summary = "Delete an education entry (soft delete)")
    public ResponseEntity<Void> deleteEducation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID educationId
    ) {
        validateAuthenticated(userPrincipal);
        educationService.deleteEducation(userPrincipal.getId(), educationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder education entries")
    public ResponseEntity<List<EducationDto>> reorderEducations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody EducationReorderRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        List<EducationDto> reordered = educationService.reorderEducations(userPrincipal.getId(), request.getEducationIds());
        return ResponseEntity.ok(reordered);
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
    }
}
