package com.soodfolio.api.controller;

import com.soodfolio.api.dto.PortfolioResponseDto;
import com.soodfolio.api.exception.ResourceNotFoundException;
import com.soodfolio.api.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Public Portfolio APIs for Viewer and Next.js SSR")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/{username}")
    @Operation(summary = "Get full portfolio by username", description = "Returns complete portfolio data for the slide deck")
    public ResponseEntity<PortfolioResponseDto> getPortfolio(@PathVariable String username) {
        PortfolioResponseDto dto = portfolioService.getPortfolioByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", "username", username));
        return ResponseEntity.ok(dto);
    }
}
