package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.LoginRequest;
import com.autoconsultancy.dto.request.OtpResendRequest;
import com.autoconsultancy.dto.request.OtpVerifyRequest;
import com.autoconsultancy.dto.request.RegisterRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.AuthResponse;
import com.autoconsultancy.dto.response.OtpChallengeResponse;
import com.autoconsultancy.entity.AdminOtp;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.AdminOtpRepository;
import com.autoconsultancy.security.JwtUtil;
import com.autoconsultancy.security.TokenBlacklist;
import com.autoconsultancy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 *
 * <pre>
 * POST /api/auth/login            — Worker / Customer standard login (unchanged)
 * POST /api/auth/admin/login      — Admin step-1: validate credentials, send OTP
 * POST /api/auth/admin/verify-otp — Admin step-2: verify OTP, receive JWT
 * POST /api/auth/admin/resend-otp — Admin: resend OTP (60-second cooldown)
 * POST /api/auth/register         — Customer self-registration
 * POST /api/auth/logout           — Invalidate JWT
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final AdminOtpRepository adminOtpRepository;

    // ── Worker / Customer login ───────────────────────────────────────────────

    /**
     * Standard one-step login for WORKER and CUSTOMER roles.
     * Admin accounts are blocked at this endpoint and must use /admin/login.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    // ── Admin two-step login ──────────────────────────────────────────────────

    /**
     * Admin login Step 1: validate credentials and send OTP.
     * Returns an opaque {@code otpToken} — no JWT is issued yet.
     */
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> adminInitiateLogin(
            @Valid @RequestBody LoginRequest request) {
        OtpChallengeResponse challenge = authService.initiateAdminLogin(request);
        return ResponseEntity.ok(ApiResponse.success(challenge,
                "Credentials verified. OTP sent to your registered Gmail."));
    }

    /**
     * Admin login Step 2: verify OTP and receive JWT.
     * The admin's email is resolved server-side from the OTP record —
     * the client only needs to send the {@code otpToken} and the 6-digit {@code otp}.
     */
    @PostMapping("/admin/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> adminVerifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {
        // Resolve admin email from the OTP record — prevents email substitution attacks
        String adminEmail = adminOtpRepository.findByOtpToken(request.getOtpToken())
                .map(AdminOtp::getAdminEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP session."));

        AuthResponse authResponse = authService.completeAdminLogin(request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Admin login successful"));
    }

    /**
     * Admin resend OTP. Subject to a 60-second cooldown.
     */
    @PostMapping("/admin/resend-otp")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> adminResendOtp(
            @Valid @RequestBody OtpResendRequest request) {
        OtpChallengeResponse challenge = authService.resendAdminOtp(
                request.getOtpToken(), request.getAdminEmail());
        return ResponseEntity.ok(ApiResponse.success(challenge, "New verification code sent"));
    }

    // ── Customer registration ─────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Registration successful"));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Invalidates the current access token so it cannot be reused after logout.
     * The token is extracted from the Authorization header, added to the blacklist,
     * and any future request bearing the same token is rejected by JwtAuthFilter.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                tokenBlacklist.blacklist(token, jwtUtil.extractExpiration(token));
            } catch (Exception ex) {
                // Token may already be expired — still return success
            }
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
