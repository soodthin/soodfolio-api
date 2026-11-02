package com.soodfolio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soodfolio.api.client.GeminiRestClient;
import com.soodfolio.api.dto.AiDtos.AiPolishRequestDto;
import com.soodfolio.api.dto.AiDtos.AiPolishResponseDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestRequestDto;
import com.soodfolio.api.dto.AiDtos.AiThemeSuggestResponseDto;
import com.soodfolio.api.exception.AiServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private static final String POLISH_SYSTEM_PROMPT = """
            You are an expert Principal Software Engineer and ATS Resume Auditor adhering strictly to the ats-validator specification and the Google / IBM X-Y-Z formula.

            INPUT:
            The user will provide a raw text segment (field: project_desc, exp_bullet, or bio) from their developer portfolio.

            CRITICAL RULES:
            1. Google / IBM X-Y-Z FORMULA:
               For 'metricXyz', strictly construct: Accomplished [X], measured by [Y], by doing [Z].
               - [X] Action & Outcome: Core engineering feat achieved (e.g. Architected, Optimized, Migrated, Engineered).
               - [Y] Quantifiable Metric: Concrete numbers, percentages, latency reduction, or throughput scale (e.g. reduced p99 latency by 45%, cut build time from 20m to 4m, scaled to 100k daily active users).
               - [Z] Technical Method: Specific tools and architectural mechanisms used (e.g. by implementing Redis caching and tuning PostgreSQL composite indexes).
            2. CANONICAL N-GRAMS:
               Strictly use canonical casing for technical terms:
               - Java, Spring Boot 3, Next.js, Redis, Docker, PostgreSQL, TypeScript, React, Kafka, Kubernetes, Tailwind CSS.
            3. ANTI-PATTERNS (ZERO TOLERANCE):
               - NEVER use passive duty descriptions such as: "Responsible for...", "Assisted in...", "Worked on...", "Handled daily tasks...".
               - NEVER use hollow clichés: "hard-working", "passionate developer", "team player".
            4. THREE STYLES REQUIRED:
               - "metricXyz": Strict X-Y-Z formula with prominent hard metrics.
               - "architectural": Focuses on architecture, design patterns, fault tolerance, and trade-offs.
               - "concise": Direct, razor-sharp 1-sentence impact statement optimized for recruiter 7-second scan.

            OUTPUT REQUIREMENT:
            Output MUST be a single raw JSON object without markdown fences:
            {
              "metricXyz": "Accomplished [X], measured by [Y], by doing [Z]",
              "architectural": "Architectural emphasis with technical decisions and patterns",
              "concise": "Punchy 1-sentence summary"
            }
            """;

    private static final String THEME_SYSTEM_PROMPT = """
            You are a Lead Portfolio Art Director and Technical Branding Specialist for SoodFolio.
            Evaluate the candidate's Title, Bio, and Technical Skills to recommend the single most suitable presentation template from the 4 available presets:

            AVAILABLE TEMPLATES:
            1. "classic": Balanced, elegant, and corporate-ready. Best for Fullstack Engineers, Enterprise Developers, Consultants, and Senior Engineering Managers.
            2. "spotlight": High-impact, visual showcase with centered keynote cover and full-width media. Best for Product Engineers, Mobile Developers, Frontend Specialists, and UI/UX Engineers.
            3. "terminal": Monospaced, high-density matrix aesthetic inspired by CLI and low-level computing. Best for Backend Architects, DevOps/SRE, Systems Engineers, Cloud/Platform Infrastructure Developers, and Security Researchers.
            4. "editorial": Clean, typography-focused, warm publication aesthetic. Best for Tech Leads, Engineering Directors, Technical Writers, Open Source Maintainers, and Researchers.

            OUTPUT REQUIREMENT:
            Output MUST be a single raw JSON object without markdown fences:
            {
              "recommendedTemplateId": "classic" | "spotlight" | "terminal" | "editorial",
              "reason": "1-2 concise sentences in Vietnamese explaining why this template best fits the candidate's technical profile."
            }
            """;

    private final GeminiRestClient geminiRestClient;
    private final ObjectMapper objectMapper;

    @Override
    public AiPolishResponseDto polishText(AiPolishRequestDto request) {
        try {
            String json = geminiRestClient.generateContent(POLISH_SYSTEM_PROMPT, buildPolishUserPrompt(request));
            return objectMapper.readValue(json, AiPolishResponseDto.class);
        } catch (AiServiceUnavailableException ex) {
            log.warn("Gemini service unavailable for polishText. Using fallback heuristic: {}", ex.getMessage());
            return fallbackPolish(request);
        } catch (Exception ex) {
            log.error("Failed to process Gemini response for polishText. Using fallback heuristic: {}", ex.getMessage());
            return fallbackPolish(request);
        }
    }

    @Override
    public AiThemeSuggestResponseDto suggestTheme(AiThemeSuggestRequestDto request) {
        try {
            String json = geminiRestClient.generateContent(THEME_SYSTEM_PROMPT, buildThemeUserPrompt(request));
            AiThemeSuggestResponseDto response = objectMapper.readValue(json, AiThemeSuggestResponseDto.class);
            if (response != null && response.getRecommendedTemplateId() != null && !response.getRecommendedTemplateId().isBlank()) {
                return response;
            }
            return fallbackThemeSuggestion(request);
        } catch (AiServiceUnavailableException ex) {
            log.warn("Gemini service unavailable for suggestTheme. Using fallback heuristic: {}", ex.getMessage());
            return fallbackThemeSuggestion(request);
        } catch (Exception ex) {
            log.error("Failed to process Gemini response for suggestTheme. Using fallback heuristic: {}", ex.getMessage());
            return fallbackThemeSuggestion(request);
        }
    }

    private String buildPolishUserPrompt(AiPolishRequestDto request) {
        return "Field: " + request.getField() + "\nRaw Text: " + request.getRawText();
    }

    private String buildThemeUserPrompt(AiThemeSuggestRequestDto request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(request.getTitle() != null ? request.getTitle() : "").append("\n");
        if (request.getBio() != null && !request.getBio().isBlank()) {
            sb.append("Bio: ").append(request.getBio()).append("\n");
        }
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            sb.append("Skills: ").append(String.join(", ", request.getSkills())).append("\n");
        }
        return sb.toString();
    }

    private AiPolishResponseDto fallbackPolish(AiPolishRequestDto request) {
        String text = request.getRawText() != null ? request.getRawText().trim() : "";
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        text = text.replaceAll("(?i)^(responsible for|assisted in|worked on|helped with)\\s+", "");
        if (text.isBlank()) {
            text = "core application services";
        }

        return AiPolishResponseDto.builder()
                .metricXyz("Optimized " + text + ", improving system throughput by 35% through standardized engineering best practices.")
                .architectural("Architected scalable solution for " + text + " utilizing modular service boundaries and resilient data patterns.")
                .concise("Engineered high-performance delivery for " + text + ".")
                .build();
    }

    private AiThemeSuggestResponseDto fallbackThemeSuggestion(AiThemeSuggestRequestDto request) {
        String combined = ((request.getTitle() != null ? request.getTitle() : "") + " "
                + (request.getBio() != null ? request.getBio() : "") + " "
                + (request.getSkills() != null ? String.join(" ", request.getSkills()) : "")).toLowerCase();

        if (combined.matches(".*\\b(backend|java|spring|docker|kubernetes|redis|postgresql|devops|cloud|system|go|rust|c\\+\\+|linux)\\b.*")) {
            return AiThemeSuggestResponseDto.builder()
                    .recommendedTemplateId("terminal")
                    .reason("Đề xuất giao diện Terminal Matrix dựa trên năng lực chuyên sâu về Backend, Cloud và Hệ thống phân tán.")
                    .build();
        }
        if (combined.matches(".*\\b(frontend|ui|ux|mobile|flutter|react|design|animation|vue|css|web)\\b.*")) {
            return AiThemeSuggestResponseDto.builder()
                    .recommendedTemplateId("spotlight")
                    .reason("Đề xuất giao diện Spotlight Focus làm nổi bật các sản phẩm trực quan và trải nghiệm người dùng ấn tượng.")
                    .build();
        }
        if (combined.matches(".*\\b(lead|manager|architect|writer|consultant|director|author|researcher)\\b.*")) {
            return AiThemeSuggestResponseDto.builder()
                    .recommendedTemplateId("editorial")
                    .reason("Đề xuất giao diện Editorial Clean thể hiện chiều sâu lãnh đạo kỹ thuật và phong cách xuất bản chuyên nghiệp.")
                    .build();
        }
        return AiThemeSuggestResponseDto.builder()
                .recommendedTemplateId("classic")
                .reason("Đề xuất giao diện Classic Deck cân đối, chuẩn mực và phù hợp với mọi hồ sơ kỹ sư phần mềm.")
                .build();
    }
}
