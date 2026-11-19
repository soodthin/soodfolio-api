package com.soodfolio.api.service;

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
import com.soodfolio.api.security.UserPrincipal;
import com.soodfolio.api.service.impl.AuthServiceImpl;
import com.soodfolio.api.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private Portfolio portfolio;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("creator@soodfolio.dev")
                .fullName("Sood Creator")
                .passwordHash("hashed_password")
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .build();

        portfolio = Portfolio.builder()
                .id(UUID.randomUUID())
                .user(user)
                .username("soodcreator")
                .title("SOOD CREATOR")
                .tagline("DEVELOPER & CREATOR")
                .build();

        refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token(SecurityUtils.sha256("valid-refresh-token-uuid"))
                .rawToken("valid-refresh-token-uuid")
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user and generate access & refresh tokens")
    void shouldRegisterUserSuccessfully() {
        AuthDtos.RegisterRequest request = AuthDtos.RegisterRequest.builder()
                .email("newuser@soodfolio.dev")
                .username("newuser")
                .fullName("New User")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("newuser@soodfolio.dev")).thenReturn(false);
        when(portfolioRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock-access-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        AuthDtos.AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken.getRawToken());
        assertThat(response.getUser().getUsername()).isEqualTo("newuser");

        verify(userRepository).save(any(User.class));
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void shouldThrowWhenEmailAlreadyExists() {
        AuthDtos.RegisterRequest request = AuthDtos.RegisterRequest.builder()
                .email("existing@soodfolio.dev")
                .username("uniqueuser")
                .fullName("User")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("existing@soodfolio.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when username is already taken")
    void shouldThrowWhenUsernameAlreadyTaken() {
        AuthDtos.RegisterRequest request = AuthDtos.RegisterRequest.builder()
                .email("new@soodfolio.dev")
                .username("existinguser")
                .fullName("User")
                .password("Password123!")
                .build();

        when(userRepository.existsByEmail("new@soodfolio.dev")).thenReturn(false);
        when(portfolioRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate and return tokens on valid login credentials")
    void shouldLoginSuccessfully() {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("creator@soodfolio.dev", "Password123!");

        UserPrincipal principal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(portfolioRepository.findByUserId(user.getId())).thenReturn(Optional.of(portfolio));
        when(tokenProvider.generateAccessToken(principal)).thenReturn("mock-access-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthDtos.AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token-uuid");
        assertThat(response.getUser().getUsername()).isEqualTo("soodcreator");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when bad credentials provided")
    void shouldThrowWhenCredentialsInvalid() {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("creator@soodfolio.dev", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email, username, or password");
    }

    @Test
    @DisplayName("Should rotate token and issue new access token on valid refresh token")
    void shouldRefreshTokenSuccessfully() {
        AuthDtos.RefreshTokenRequest request = new AuthDtos.RefreshTokenRequest(refreshToken.getToken());
        RefreshToken newRotatedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("new-rotated-token-uuid")
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(refreshTokenService.verifyExpiration(refreshToken.getToken())).thenReturn(refreshToken);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(newRotatedToken);
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("new-access-token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(portfolioRepository.findByUserId(user.getId())).thenReturn(Optional.of(portfolio));

        AuthDtos.AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-rotated-token-uuid");
        verify(refreshTokenService).revokeToken(refreshToken.getToken());
    }

    @Test
    @DisplayName("Should revoke token on logout")
    void shouldLogoutSuccessfully() {
        authService.logout("mock-refresh-token");

        verify(refreshTokenService).revokeToken("mock-refresh-token");
    }

    @Test
    @DisplayName("Should generate password reset token and dispatch email when user exists")
    void shouldHandleForgotPasswordForExistingUser() {
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "http://localhost:3000");
        AuthDtos.ForgotPasswordRequest request = new AuthDtos.ForgotPasswordRequest("creator@soodfolio.dev");

        when(userRepository.findByEmail("creator@soodfolio.dev")).thenReturn(Optional.of(user));

        AuthDtos.MessageResponse response = authService.forgotPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Nếu email tồn tại trong hệ thống, link khôi phục mật khẩu đã được tạo thành công.");
        verify(passwordResetTokenRepository).deleteByUser(user);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).hasSize(64); // SHA-256 hex string length
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.isUsed()).isFalse();

        // Verify email dispatched with reset link starting with FRONTEND_BASE_URL
        verify(emailService).sendPasswordResetEmail(
                eq(user.getEmail()),
                eq(user.getFullName()),
                argThat(link -> link.startsWith("http://localhost:3000/reset-password?token="))
        );
    }

    @Test
    @DisplayName("Should return generic success message and not dispatch email when user does not exist (anti-enumeration)")
    void shouldHandleForgotPasswordSilentlyForNonExistingUser() {
        AuthDtos.ForgotPasswordRequest request = new AuthDtos.ForgotPasswordRequest("nonexistent@soodfolio.dev");

        when(userRepository.findByEmail("nonexistent@soodfolio.dev")).thenReturn(Optional.empty());

        AuthDtos.MessageResponse response = authService.forgotPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Nếu email tồn tại trong hệ thống, link khôi phục mật khẩu đã được tạo thành công.");
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should verify valid password reset token")
    void shouldVerifyValidResetToken() {
        String rawToken = "valid-hex-token";
        String hashedToken = SecurityUtils.sha256(rawToken);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(resetToken));

        AuthDtos.VerifyResetTokenResponse response = authService.verifyResetToken(rawToken);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("Should return valid=false for expired or used password reset token")
    void shouldRejectInvalidResetToken() {
        String rawToken = "used-hex-token";
        String hashedToken = SecurityUtils.sha256(rawToken);
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .used(true)
                .build();

        when(passwordResetTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(usedToken));

        AuthDtos.VerifyResetTokenResponse response = authService.verifyResetToken(rawToken);

        assertThat(response.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should reset password and invalidate refresh tokens on valid reset token")
    void shouldResetPasswordSuccessfully() {
        String rawToken = "active-hex-token";
        String hashedToken = SecurityUtils.sha256(rawToken);
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiryDate(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        AuthDtos.ResetPasswordRequest request = new AuthDtos.ResetPasswordRequest(rawToken, "NewSuperSecret123!");

        when(passwordResetTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewSuperSecret123!")).thenReturn("new_hash");

        AuthDtos.MessageResponse response = authService.resetPassword(request);

        assertThat(response).isNotNull();
        assertThat(resetToken.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    @DisplayName("Should throw 403 ForbiddenException when sandboxEnabled is false for processOAuthSandbox and processOAuthLogin")
    void shouldThrowForbiddenWhenSandboxDisabled() {
        ReflectionTestUtils.setField(authService, "sandboxEnabled", false);

        AuthDtos.OAuthSandboxRequest sandboxReq = new AuthDtos.OAuthSandboxRequest("GITHUB", "dev@test.com", "Dev", "dev", null);
        assertThatThrownBy(() -> authService.processOAuthSandbox(sandboxReq))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("OAuth sandbox mode is disabled");

        AuthDtos.OAuthLoginRequest loginReq = new AuthDtos.OAuthLoginRequest("GITHUB", "test-code", null);
        assertThatThrownBy(() -> authService.processOAuthLogin(loginReq))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("OAuth sandbox mode is disabled");
    }

    @Test
    @DisplayName("Should allow processOAuthSandbox and processOAuthLogin when sandboxEnabled is true")
    void shouldAllowOAuthSandboxWhenEnabled() {
        ReflectionTestUtils.setField(authService, "sandboxEnabled", true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(portfolioRepository.existsByUsername(anyString())).thenReturn(false);
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock-sandbox-token");
        when(tokenProvider.getExpirationMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);

        AuthDtos.OAuthSandboxRequest sandboxReq = new AuthDtos.OAuthSandboxRequest("GITHUB", "dev@github.test", "Dev", "github_dev", null);
        AuthDtos.AuthResponse response = authService.processOAuthSandbox(sandboxReq);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-sandbox-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token-uuid");
    }

    @Test
    @DisplayName("Should reject OAuth code exchange with prefix-bypass redirectUri")
    void shouldRejectOAuthCodeExchangeWhenRedirectUriPrefixBypassed() {
        ReflectionTestUtils.setField(authService, "allowedRedirectUrisConfig", "http://localhost:3000/auth/callback/github,http://localhost:3000/auth/callback/google");

        // 1. Google with malicious domain appending prefix
        AuthDtos.OAuthExchangeRequest evilGoogle = new AuthDtos.OAuthExchangeRequest(
                "GOOGLE",
                "auth-code-123",
                "http://localhost:3000/auth/callback/google.evil.com",
                "state-123"
        );
        assertThatThrownBy(() -> authService.exchangeOAuthCode(evilGoogle))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("redirect_uri không hợp lệ hoặc không nằm trong danh sách được phép");

        // 2. Google with null redirectUri
        AuthDtos.OAuthExchangeRequest nullGoogle = new AuthDtos.OAuthExchangeRequest(
                "GOOGLE",
                "auth-code-123",
                null,
                "state-123"
        );
        assertThatThrownBy(() -> authService.exchangeOAuthCode(nullGoogle))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("redirect_uri không hợp lệ hoặc không nằm trong danh sách được phép");

        // 3. GitHub with attacker redirectUri
        AuthDtos.OAuthExchangeRequest evilGithub = new AuthDtos.OAuthExchangeRequest(
                "GITHUB",
                "auth-code-123",
                "http://localhost:3000/auth/callback/github.attacker.org",
                "state-123"
        );
        assertThatThrownBy(() -> authService.exchangeOAuthCode(evilGithub))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("redirect_uri không nằm trong danh sách được phép");
    }

    @Test
    @DisplayName("Should reject OAuth code exchange when client credentials are missing")
    void shouldRejectOAuthWhenCredentialsMissing() {
        ReflectionTestUtils.setField(authService, "allowedRedirectUrisConfig", "http://localhost:3000/auth/callback/github,http://localhost:3000/auth/callback/google");
        ReflectionTestUtils.setField(authService, "githubClientId", "");
        ReflectionTestUtils.setField(authService, "githubClientSecret", "");

        AuthDtos.OAuthExchangeRequest request = new AuthDtos.OAuthExchangeRequest(
                "GITHUB",
                "auth-code-123",
                "http://localhost:3000/auth/callback/github",
                "state-123"
        );

        assertThatThrownBy(() -> authService.exchangeOAuthCode(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("GitHub OAuth client credentials are not configured");
    }
}
