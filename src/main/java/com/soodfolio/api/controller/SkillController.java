package com.soodfolio.api.controller;

import com.soodfolio.api.dto.PortfolioResponseDto.SkillDto;
import com.soodfolio.api.dto.SkillDtos.SkillReorderRequestDto;
import com.soodfolio.api.dto.SkillDtos.SkillRequestDto;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.SkillService;
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
@RequestMapping("/portfolios/me/skills")
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Capabilities and Skills Matrix APIs for Creators")
@SecurityRequirement(name = "bearerAuth")
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    @Operation(summary = "Get all technical skills for current creator")
    public ResponseEntity<List<SkillDto>> getMySkills(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(skillService.getSkillsByUserId(userPrincipal.getId()));
    }

    @GetMapping("/{skillId}")
    @Operation(summary = "Get single technical skill by ID")
    public ResponseEntity<SkillDto> getSkillById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID skillId
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(skillService.getSkillById(userPrincipal.getId(), skillId));
    }

    @PostMapping
    @Operation(summary = "Create a new technical skill entry")
    public ResponseEntity<SkillDto> createSkill(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SkillRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        SkillDto created = skillService.createSkill(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{skillId}")
    @Operation(summary = "Update an existing technical skill entry")
    public ResponseEntity<SkillDto> updateSkill(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID skillId,
            @Valid @RequestBody SkillRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        SkillDto updated = skillService.updateSkill(userPrincipal.getId(), skillId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{skillId}")
    @Operation(summary = "Delete a technical skill entry (soft delete)")
    public ResponseEntity<Void> deleteSkill(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID skillId
    ) {
        validateAuthenticated(userPrincipal);
        skillService.deleteSkill(userPrincipal.getId(), skillId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder technical skills")
    public ResponseEntity<List<SkillDto>> reorderSkills(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SkillReorderRequestDto request
    ) {
        validateAuthenticated(userPrincipal);
        List<SkillDto> reordered = skillService.reorderSkills(userPrincipal.getId(), request.getSkillIds());
        return ResponseEntity.ok(reordered);
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
    }
}
