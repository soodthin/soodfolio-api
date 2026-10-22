package com.soodfolio.api.controller;

import com.soodfolio.api.dto.SnapshotDtos.CreateSnapshotRequest;
import com.soodfolio.api.dto.SnapshotDtos.RollbackResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotResponseDto;
import com.soodfolio.api.dto.SnapshotDtos.SnapshotSummaryDto;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.SnapshotService;
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

@RestController
@RequestMapping("/portfolios/me/snapshots")
@RequiredArgsConstructor
@Tag(name = "Portfolio Snapshots", description = "Version management, immutable snapshots, and rollback for portfolios")
@SecurityRequirement(name = "bearerAuth")
public class SnapshotController {

    private final SnapshotService snapshotService;

    @PostMapping("/publish")
    @Operation(summary = "Publish current portfolio draft into an immutable snapshot version")
    public ResponseEntity<SnapshotResponseDto> publishSnapshot(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody(required = false) CreateSnapshotRequest request
    ) {
        ensureAuthenticated(userPrincipal);
        String changelog = request != null ? request.getChangelog() : null;
        SnapshotResponseDto response = snapshotService.publishSnapshot(userPrincipal.getId(), changelog);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all published snapshot versions for the authenticated user")
    public ResponseEntity<List<SnapshotSummaryDto>> getSnapshots(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ensureAuthenticated(userPrincipal);
        return ResponseEntity.ok(snapshotService.getSnapshots(userPrincipal.getId()));
    }

    @GetMapping("/{version}")
    @Operation(summary = "Get detailed snapshot data by version number")
    public ResponseEntity<SnapshotResponseDto> getSnapshot(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable int version
    ) {
        ensureAuthenticated(userPrincipal);
        return ResponseEntity.ok(snapshotService.getSnapshot(userPrincipal.getId(), version));
    }

    @PostMapping("/{version}/rollback")
    @Operation(summary = "Rollback current draft portfolio to a previous snapshot version")
    public ResponseEntity<RollbackResponseDto> rollback(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable int version
    ) {
        ensureAuthenticated(userPrincipal);
        return ResponseEntity.ok(snapshotService.rollbackToSnapshot(userPrincipal.getId(), version));
    }

    private void ensureAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new InvalidCredentialsException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
    }
}
