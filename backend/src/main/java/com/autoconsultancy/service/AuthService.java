package com.autoconsultancy.service;

import com.autoconsultancy.dto.request.LoginRequest;
import com.autoconsultancy.dto.request.OtpVerifyRequest;
import com.autoconsultancy.dto.request.RegisterRequest;
import com.autoconsultancy.dto.response.AuthResponse;
import com.autoconsultancy.dto.response.OtpChallengeResponse;
import com.autoconsultancy.entity.Customer;
import com.autoconsultancy.entity.Role;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.CustomerRepository;
import com.autoconsultancy.repository.UserRepository;
import com.autoconsultancy.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication service.
 *
 * <h3>Admin login flow (two-step)</h3>
 * <ol>
 *   <li>{@link #initiateAdminLogin} — validates credentials, sends OTP, returns challenge token.</li>
 *   <li>{@link #completeAdminLogin} — verifies OTP, issues JWT.</li>
 * </ol>
 *
 * <h3>Worker / Customer login flow (unchanged)</h3>
 * {@link #login} — validates credentials, issues JWT immediately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final OtpService otpService;

    @Value("${admin.emails}")
    private String authorizedAdminEmailsRaw;

    /** Lazily-parsed, trimmed list of authorized admin emails. */
    private List<String> getAuthorizedAdminEmails() {
        return Arrays.stream(authorizedAdminEmailsRaw.split(","))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());
    }

    // ── Worker / Customer login (unchanged) ───────────────────────────────────

    /**
     * Standard single-step login for WORKER and CUSTOMER roles.
     * Returns a JWT immediately.
     * <p>
     * Admin accounts are explicitly blocked here — they must use
     * {@link #initiateAdminLogin} and {@link #completeAdminLogin}.
     */
    public AuthResponse login(LoginRequest request) {
        User user = authenticateCredentials(request.getEmail(), request.getPassword());

        if (user.getRole() == Role.ADMIN) {
            // Prevent admins from bypassing OTP via the standard login endpoint
            throw new UnauthorizedException(
                    "Admin accounts require two-factor authentication. Use the Admin login portal.");
        }

        return buildAuthResponse(user);
    }

    // ── Admin two-step login ──────────────────────────────────────────────────

    /**
     * Step 1: Validates admin credentials, sends OTP, returns challenge token.
     * Does NOT return a JWT.
     */
    public OtpChallengeResponse initiateAdminLogin(LoginRequest request) {
        User user = authenticateCredentials(request.getEmail(), request.getPassword());

        if (user.getRole() != Role.ADMIN) {
            // Worker/Customer attempting admin OTP endpoint
            throw new UnauthorizedException("Access denied.");
        }

        // Extra guard: only authorized admin emails may proceed
        boolean isAuthorized = getAuthorizedAdminEmails().stream()
                .anyMatch(e -> e.equalsIgnoreCase(user.getEmail()));
        if (!isAuthorized) {
            log.warn("Unauthorized admin login attempt for email: {}", user.getEmail());
            throw new UnauthorizedException("This email is not authorized for admin access.");
        }

        return otpService.generateAndSendOtp(user.getEmail());
    }

    /**
     * Step 2: Verifies OTP and issues the JWT.
     */
    public AuthResponse completeAdminLogin(OtpVerifyRequest request, String adminEmail) {
        // The adminEmail is passed from the controller which got it from the OTP challenge record
        // We re-verify that the token belongs to the admin's email inside OtpService
        otpService.verifyOtp(request.getOtpToken(), request.getOtp(), adminEmail);

        User user = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UnauthorizedException("Admin account not found."));

        if (user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Access denied.");
        }

        return buildAuthResponse(user);
    }

    /**
     * Resends OTP for an active admin challenge.
     */
    public OtpChallengeResponse resendAdminOtp(String otpToken, String adminEmail) {
        return otpService.resendOtp(otpToken, adminEmail);
    }

    // ── Customer registration ─────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .id(user.getId())
                .user(user)
                .profileComplete(false)
                .build();
        customerRepository.save(customer);

        return buildAuthResponse(user);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Authenticates credentials and returns the User entity.
     * Throws {@link UnauthorizedException} on any failure.
     */
    private User authenticateCredentials(String email, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException | DisabledException e) {
            throw new UnauthorizedException("Invalid email or password");
        } catch (Exception e) {
            throw new UnauthorizedException("Authentication failed");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
