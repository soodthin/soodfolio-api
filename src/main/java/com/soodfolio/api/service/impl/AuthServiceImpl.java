package com.soodfolio.api.service.impl;

import com.soodfolio.api.dto.AuthDtos;
import com.soodfolio.api.entity.PasswordResetToken;
import com.soodfolio.api.entity.Portfolio;
import com.soodfolio.api.entity.RefreshToken;
import com.soodfolio.api.entity.User;
import com.soodfolio.api.exception.DuplicateResourceException;
import com.soodfolio.api.exception.ForbiddenException;
import com.soodfolio.api.exception.InvalidCredentialsException;
import com.soodfolio.api.repository.PasswordResetTokenRepository;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.RefreshTokenRepository;
import com.soodfolio.api.repository.UserRepository;
import com.soodfolio.api.security.JwtTokenProvider;
import com.soodfolio.api.security.SecurityUtils;
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.AuthService;
import com.soodfolio.api.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.oauth2.sandbox-enabled:false}")
    private boolean sandboxEnabled;

    @Value("${app.oauth2.allowed-redirect-uris:http://localhost:3000/auth/callback/github,http://localhost:3000/auth/callback/google,http://localhost:3001/auth/callback/github,http://localhost:3001/auth/callback/google}")
    private String allowedRedirectUrisConfig;

    @Value("${app.oauth2.github.client-id:}")
    private String githubClientId;

    @Value("${app.oauth2.github.client-secret:}")
    private String githubClientSecret;

    @Value("${app.oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth2.google.client-secret:}")
    private String googleClientSecret;

    @Value("${app.auth.log-reset-link:false}")
    private boolean logResetLink;

    private final RestClient restClient = RestClient.create();

    // Concurrency guards to prevent duplicate external token exchange with GitHub / Google
    private final Map<String, CompletableFuture<AuthDtos.AuthResponse>> activeOAuthExchanges = new ConcurrentHashMap<>();
    private final Map<String, AuthDtos.AuthResponse> recentOAuthExchanges = new ConcurrentHashMap<>();
    private final Map<String, Long> recentOAuthTimestamps = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String cleanUsername = request.getUsername().toLowerCase().trim();

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered!");
        }

        if (portfolioRepository.existsByUsername(cleanUsername)) {
            throw new DuplicateResourceException("Username '" + cleanUsername + "' is already taken!");
        }

        // 1. Create User
        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .build();
        userRepository.save(user);

        // 2. Automatically create initial Portfolio for this user
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .username(cleanUsername)
                .title(user.getFullName().toUpperCase())
                .tagline("DEVELOPER & CREATOR")
                .bio("Welcome to my interactive pitch-deck portfolio.")
                .statusText("AVAILABLE FOR WORK")
                .themeMode("DARK")
                .build();
        portfolioRepository.save(portfolio);

        // 3. Authenticate & generate tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthDtos.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRawToken() != null ? refreshToken.getRawToken() : refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(buildUserDto(user, cleanUsername))
                .build();
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail().trim(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new InvalidCredentialsException("User account not found"));

            String username = portfolioRepository.findByUserId(user.getId())
                    .map(Portfolio::getUsername)
                    .orElse("user");

            String accessToken = tokenProvider.generateAccessToken(userPrincipal);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return AuthDtos.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRawToken() != null ? refreshToken.getRawToken() : refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(buildUserDto(user, username))
                .build();
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid email, username, or password");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthDtos.AuthResponse refreshToken(AuthDtos.RefreshTokenRequest request) {
        RefreshToken oldRefreshToken = refreshTokenService.verifyExpiration(request.getRefreshToken());
        User user = oldRefreshToken.getUser();

        // SEC-03 Remediation: Single-use Refresh Token Rotation (pass raw token to prevent double-hashing)
        refreshTokenService.revokeToken(request.getRefreshToken());
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);

        String username = portfolioRepository.findByUserId(user.getId())
                .map(Portfolio::getUsername)
                .orElse("user");

        return AuthDtos.AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getRawToken() != null ? newRefreshToken.getRawToken() : newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(buildUserDto(user, username))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenString) {
        refreshTokenService.revokeToken(refreshTokenString);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.UserDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new InvalidCredentialsException("User is not authenticated");
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        String username = portfolioRepository.findByUserId(user.getId())
                .map(Portfolio::getUsername)
                .orElse("user");

        return buildUserDto(user, username);
    }

    @Override
    @Transactional
    public AuthDtos.MessageResponse forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail).orElse(null);

        if (user != null) {
            passwordResetTokenRepository.deleteByUser(user);

            String rawToken = UUID.randomUUID().toString().replace("-", "") + 
                           UUID.randomUUID().toString().replace("-", "");
            String hashedToken = SecurityUtils.sha256(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(hashedToken)
                    .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            if (logResetLink) {
                log.info("=======================================================================");
                log.info("[SOODFOLIO] PASSWORD RESET REQUESTED FOR: {}", cleanEmail);
                log.info("[SOODFOLIO] RESET LINK: http://localhost:3000/reset-password?token={}", rawToken);
                log.info("=======================================================================");
            }
        }

        return new AuthDtos.MessageResponse("Nếu email tồn tại trong hệ thống, link khôi phục mật khẩu đã được tạo thành công.");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDtos.VerifyResetTokenResponse verifyResetToken(String token) {
        if (token == null || token.isBlank()) {
            return AuthDtos.VerifyResetTokenResponse.builder().valid(false).build();
        }

        String hashedToken = SecurityUtils.sha256(token.trim());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(hashedToken).orElse(null);
        if (resetToken == null || resetToken.isUsed() || resetToken.isExpired()) {
            return AuthDtos.VerifyResetTokenResponse.builder().valid(false).build();
        }

        return AuthDtos.VerifyResetTokenResponse.builder()
                .valid(true)
                .email(resetToken.getUser().getEmail())
                .build();
    }

    @Override
    @Transactional
    public AuthDtos.MessageResponse resetPassword(AuthDtos.ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new InvalidCredentialsException("Link khôi phục mật khẩu không hợp lệ hoặc không tồn tại.");
        }

        String hashedToken = SecurityUtils.sha256(request.getToken().trim());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new InvalidCredentialsException("Link khôi phục mật khẩu không hợp lệ hoặc không tồn tại."));

        if (resetToken.isUsed()) {
            throw new InvalidCredentialsException("Link khôi phục này đã được sử dụng trước đó.");
        }

        if (resetToken.isExpired()) {
            throw new InvalidCredentialsException("Link khôi phục mật khẩu đã hết hạn (chỉ có hiệu lực trong 15 phút).");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.deleteByUser(user);

        log.info("[SOODFOLIO] Password successfully reset for user: {}", user.getEmail());
        return new AuthDtos.MessageResponse("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới ngay bây giờ.");
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse processOAuthSandbox(AuthDtos.OAuthSandboxRequest request) {
        if (!sandboxEnabled) {
            throw new ForbiddenException("OAuth sandbox mode is disabled");
        }

        String provider = request.getProvider().toUpperCase().trim();
        String defaultEmail = provider.equalsIgnoreCase("GITHUB") ? "developer@github.test" : "developer@google.test";
        String email = request.getEmail() != null && !request.getEmail().isBlank() 
                ? request.getEmail().trim().toLowerCase() 
                : defaultEmail;

        String fullName = request.getFullName() != null && !request.getFullName().isBlank() 
                ? request.getFullName().trim() 
                : (provider.equalsIgnoreCase("GITHUB") ? "GitHub Elite Engineer" : "Google Cloud Craftsman");

        String baseUsername = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername().trim().toLowerCase().replaceAll("[^a-z0-9_-]", "")
                : (provider.equalsIgnoreCase("GITHUB") ? "github_dev" : "google_dev");

        // SEC-01 Remediation: Prevent Account Takeover of real accounts via Sandbox Mode
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && (existingUser.getProviderId() == null || !existingUser.getProviderId().startsWith("sandbox-"))) {
            throw new InvalidCredentialsException("Tài khoản '" + email + "' đã được đăng ký chính thức trên hệ thống. Không thể đăng nhập bằng Sandbox Mode.");
        }

        return findOrCreateOAuthUser(email, fullName, baseUsername, null, provider, "sandbox-" + System.currentTimeMillis());
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse processOAuthLogin(AuthDtos.OAuthLoginRequest request) {
        if (!sandboxEnabled) {
            throw new ForbiddenException("OAuth sandbox mode is disabled");
        }

        String provider = request.getProvider().toUpperCase().trim();
        AuthDtos.OAuthSandboxRequest sandboxReq = new AuthDtos.OAuthSandboxRequest(provider, null, null, null, null);
        return processOAuthSandbox(sandboxReq);
    }

    @Override
    @Transactional
    public AuthDtos.AuthResponse exchangeOAuthCode(AuthDtos.OAuthExchangeRequest request) {
        String provider = request.getProvider().toUpperCase().trim();
        String code = request.getCode() != null ? request.getCode().trim() : "";
        String cacheKey = provider + ":" + code;

        // Clean up entries older than 60 seconds
        long now = System.currentTimeMillis();
        recentOAuthTimestamps.entrySet().removeIf(entry -> now - entry.getValue() > 60_000);
        recentOAuthExchanges.keySet().removeIf(k -> !recentOAuthTimestamps.containsKey(k));

        // 1. Return cached response for recent duplicate exchange (e.g. StrictMode double mount or network retry)
        AuthDtos.AuthResponse cached = recentOAuthExchanges.get(cacheKey);
        if (cached != null) {
            log.info("[SOODFOLIO] Returning cached OAuth exchange for duplicate request: {}", cacheKey);
            return cached;
        }

        // 2. Coordinate concurrent in-flight requests for the exact same (provider, code)
        CompletableFuture<AuthDtos.AuthResponse> newFuture = new CompletableFuture<>();
        CompletableFuture<AuthDtos.AuthResponse> existingFuture = activeOAuthExchanges.putIfAbsent(cacheKey, newFuture);
        if (existingFuture != null) {
            log.info("[SOODFOLIO] Awaiting concurrent in-flight OAuth exchange for key: {}", cacheKey);
            try {
                return existingFuture.get(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("[SOODFOLIO] Error waiting for concurrent OAuth exchange: {}", e.getMessage());
                throw new InvalidCredentialsException("Lỗi xử lý xác thực đồng thời qua " + provider);
            }
        }

        try {
            AuthDtos.AuthResponse response = doExchangeOAuthCode(request);
            recentOAuthExchanges.put(cacheKey, response);
            recentOAuthTimestamps.put(cacheKey, System.currentTimeMillis());
            newFuture.complete(response);
            return response;
        } catch (Throwable t) {
            newFuture.completeExceptionally(t);
            throw t;
        } finally {
            activeOAuthExchanges.remove(cacheKey);
        }
    }

    private List<String> getAllowedRedirectUris() {
        if (allowedRedirectUrisConfig == null || allowedRedirectUrisConfig.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedRedirectUrisConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private AuthDtos.AuthResponse doExchangeOAuthCode(AuthDtos.OAuthExchangeRequest request) {
        String provider = request.getProvider().toUpperCase().trim();
        log.info("[SOODFOLIO] Exchanging OAuth authorization code for provider: {}", provider);

        List<String> allowedUris = getAllowedRedirectUris();
        String redirectUri = request.getRedirectUri();

        // Exact-match validation against allowed redirect URIs whitelist
        if ("GOOGLE".equals(provider)) {
            if (redirectUri == null || redirectUri.isBlank() || !allowedUris.contains(redirectUri.trim())) {
                throw new InvalidCredentialsException("redirect_uri không hợp lệ hoặc không nằm trong danh sách được phép");
            }
        } else {
            if (redirectUri != null && !redirectUri.isBlank() && !allowedUris.contains(redirectUri.trim())) {
                throw new InvalidCredentialsException("redirect_uri không nằm trong danh sách được phép");
            }
        }

        String email = null;
        String fullName = null;
        String avatarUrl = null;
        String providerId = null;
        String baseUsername = null;

        try {
            if ("GITHUB".equals(provider)) {
                if (githubClientId == null || githubClientId.isBlank() || githubClientSecret == null || githubClientSecret.isBlank()) {
                    log.error("[SOODFOLIO] GitHub OAuth client credentials are not configured");
                    throw new InvalidCredentialsException("GitHub OAuth client credentials are not configured");
                }

                // 1. Exchange code for access_token with GitHub
                Map<String, String> tokenPayload = Map.of(
                        "client_id", githubClientId.trim(),
                        "client_secret", githubClientSecret.trim(),
                        "code", request.getCode()
                );

                Map<String, Object> tokenResponse = restClient.post()
                        .uri("https://github.com/login/oauth/access_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(tokenPayload)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                    log.error("[SOODFOLIO] GitHub token exchange failed: {}", tokenResponse);
                    throw new InvalidCredentialsException("Không thể nhận Access Token từ GitHub");
                }

                String githubAccessToken = (String) tokenResponse.get("access_token");

                // 2. Fetch GitHub user profile
                Map<String, Object> userProfile = restClient.get()
                        .uri("https://api.github.com/user")
                        .header("Authorization", "Bearer " + githubAccessToken)
                        .header("User-Agent", "SoodFolio-App")
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (userProfile == null) {
                    throw new InvalidCredentialsException("Không thể lấy thông tin người dùng từ GitHub");
                }

                providerId = String.valueOf(userProfile.get("id"));
                String login = (String) userProfile.get("login");
                fullName = (String) userProfile.get("name");
                if (fullName == null || fullName.isBlank()) {
                    fullName = login;
                }
                avatarUrl = (String) userProfile.get("avatar_url");
                email = (String) userProfile.get("email");
                baseUsername = login.toLowerCase().replaceAll("[^a-z0-9_-]", "");

                // If email is private on GitHub, fetch from /user/emails
                if (email == null || email.isBlank()) {
                    try {
                        List<Map<String, Object>> emails = restClient.get()
                                .uri("https://api.github.com/user/emails")
                                .header("Authorization", "Bearer " + githubAccessToken)
                                .header("User-Agent", "SoodFolio-App")
                                .retrieve()
                                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

                        if (emails != null) {
                            for (Map<String, Object> e : emails) {
                                Boolean primary = (Boolean) e.get("primary");
                                Boolean verified = (Boolean) e.get("verified");
                                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                                    email = (String) e.get("email");
                                    break;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("[SOODFOLIO] Could not fetch private emails from GitHub: {}", ex.getMessage());
                    }
                }

                if (email == null || email.isBlank()) {
                    email = login + "@users.noreply.github.com";
                }

            } else if ("GOOGLE".equals(provider)) {
                if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
                    log.error("[SOODFOLIO] Google OAuth client credentials are not configured");
                    throw new InvalidCredentialsException("Google OAuth client credentials are not configured");
                }

                // 1. Exchange code for access_token with Google
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("code", request.getCode());
                form.add("client_id", googleClientId.trim());
                form.add("client_secret", googleClientSecret.trim());
                form.add("redirect_uri", redirectUri.trim());
                form.add("grant_type", "authorization_code");

                Map<String, Object> tokenResponse = restClient.post()
                        .uri("https://oauth2.googleapis.com/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                    log.error("[SOODFOLIO] Google token exchange failed: {}", tokenResponse);
                    throw new InvalidCredentialsException("Không thể nhận Access Token từ Google");
                }

                String googleAccessToken = (String) tokenResponse.get("access_token");

                // 2. Fetch Google user profile
                Map<String, Object> userProfile = restClient.get()
                        .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                        .header("Authorization", "Bearer " + googleAccessToken)
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (userProfile == null) {
                    throw new InvalidCredentialsException("Không thể lấy thông tin người dùng từ Google");
                }

                // Verify Google email_verified
                Object emailVerifiedObj = userProfile.get("email_verified");
                boolean isEmailVerified = Boolean.TRUE.equals(emailVerifiedObj) || "true".equalsIgnoreCase(String.valueOf(emailVerifiedObj));
                if (!isEmailVerified) {
                    log.warn("[SOODFOLIO] Google email is not verified for userinfo payload");
                    throw new InvalidCredentialsException("Tài khoản Google chưa được xác thực email (email_verified is false)");
                }

                providerId = (String) userProfile.get("sub");
                email = (String) userProfile.get("email");
                fullName = (String) userProfile.get("name");
                avatarUrl = (String) userProfile.get("picture");
                baseUsername = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9_-]", "");
            } else {
                throw new InvalidCredentialsException("Unsupported OAuth provider: " + provider);
            }

        } catch (InvalidCredentialsException ice) {
            throw ice;
        } catch (Exception ex) {
            log.error("[SOODFOLIO] OAuth code exchange exception for " + provider + ": " + ex.getMessage(), ex);
            throw new InvalidCredentialsException("Lỗi giao tiếp xác thực với " + provider + ": " + ex.getMessage());
        }

        return findOrCreateOAuthUser(email, fullName, baseUsername, avatarUrl, provider, providerId);
    }

    private AuthDtos.AuthResponse findOrCreateOAuthUser(String email, String fullName, String baseUsername, String avatarUrl, String provider, String providerId) {
        String cleanEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail).orElse(null);
        String finalUsername = baseUsername;

        if (user == null) {
            int counter = 1;
            while (portfolioRepository.existsByUsername(finalUsername)) {
                finalUsername = baseUsername + "_" + counter++;
            }

            user = User.builder()
                    .email(cleanEmail)
                    .fullName(fullName != null ? fullName : baseUsername)
                    .avatarUrl(avatarUrl)
                    .role("ROLE_USER")
                    .authProvider(provider)
                    .providerId(providerId)
                    .build();
            userRepository.save(user);

            Portfolio portfolio = Portfolio.builder()
                    .user(user)
                    .username(finalUsername)
                    .title((fullName != null ? fullName : baseUsername).toUpperCase())
                    .tagline("VERIFIED " + provider + " DEVELOPER")
                    .bio("Welcome to my interactive presentation portfolio.")
                    .statusText("OPEN TO OPPORTUNITIES")
                    .themeMode("DARK")
                    .build();
            portfolioRepository.save(portfolio);
            log.info("[SOODFOLIO] Created new OAuth user: {} with username: {}", cleanEmail, finalUsername);
        } else {
            // Update profile info if user exists
            if (user.getAvatarUrl() == null && avatarUrl != null) {
                user.setAvatarUrl(avatarUrl);
                userRepository.save(user);
            }
            finalUsername = portfolioRepository.findByUserId(user.getId())
                    .map(Portfolio::getUsername)
                    .orElse(baseUsername);
            log.info("[SOODFOLIO] Existing user logged in via OAuth: {}", cleanEmail);
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthDtos.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRawToken() != null ? refreshToken.getRawToken() : refreshToken.getToken())
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(buildUserDto(user, finalUsername))
                .build();
    }

    private AuthDtos.UserDto buildUserDto(User user, String username) {
        return AuthDtos.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .username(username)
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .build();
    }
}
