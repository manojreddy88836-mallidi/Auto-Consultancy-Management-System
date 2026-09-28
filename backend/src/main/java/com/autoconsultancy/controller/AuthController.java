package com.autoconsultancy.controller;

import com.autoconsultancy.dto.request.LoginRequest;
import com.autoconsultancy.dto.request.RegisterRequest;
import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.dto.response.AuthResponse;
import com.autoconsultancy.security.JwtUtil;
import com.autoconsultancy.security.TokenBlacklist;
import com.autoconsultancy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Registration successful"));
    }

    /**
     * Invalidates the current access token so it cannot be reused after logout.
     * The token is extracted from the Authorization header, added to the blacklist,
     * and any future request bearing the same token is rejected by {@link com.autoconsultancy.security.JwtAuthFilter}.
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
