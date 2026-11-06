package com.autoconsultancy.service;

import com.autoconsultancy.dto.response.OtpChallengeResponse;
import com.autoconsultancy.entity.AdminOtp;
import com.autoconsultancy.exception.BadRequestException;
import com.autoconsultancy.exception.UnauthorizedException;
import com.autoconsultancy.repository.AdminOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Handles all OTP lifecycle operations for Admin two-factor login.
 *
 * <h3>Security guarantees</h3>
 * <ul>
 *   <li>OTP is generated with {@link SecureRandom} — cryptographically secure.</li>
 *   <li>Only the BCrypt hash is persisted; the raw OTP lives in memory only long
 *       enough to send the email, then is discarded.</li>
 *   <li>Generating a new OTP invalidates all previous ones for the same email.</li>
 *   <li>After {@code maxAttempts} wrong guesses the challenge is locked.</li>
 *   <li>Resend is rate-limited to once per {@code resendCooldownSeconds}.</li>
 *   <li>The correlation token ({@code otpToken}) is a UUID — reveals nothing.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final AdminOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String authorizedAdminEmail;

    @Value("${admin.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${admin.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${admin.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a new OTP for the given admin email, stores its hash, sends the
     * email, and returns a challenge response (no OTP, no JWT).
     */
    public OtpChallengeResponse generateAndSendOtp(String adminEmail) {
        // Resend cooldown: check if an OTP was generated recently
        otpRepository.findFirstByAdminEmailOrderByGeneratedAtDesc(adminEmail).ifPresent(existing -> {
            if (!isCooldownExpired(existing)) {
                long waitSeconds = resendCooldownSeconds -
                        ChronoUnit.SECONDS.between(existing.getGeneratedAt(), Instant.now());
                throw new BadRequestException(
                        "Please wait " + waitSeconds + " seconds before requesting a new OTP.");
            }
        });

        // Invalidate all previous OTPs for this email
        otpRepository.deleteByAdminEmail(adminEmail);

        // Generate cryptographically secure 6-digit OTP
        String rawOtp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        // Hash before storage — raw OTP is not persisted
        String hashedOtp = passwordEncoder.encode(rawOtp);

        String otpToken = UUID.randomUUID().toString();
        Instant now = Instant.now();

        AdminOtp record = AdminOtp.builder()
                .otpToken(otpToken)
                .adminEmail(adminEmail)
                .hashedOtp(hashedOtp)
                .expiresAt(now.plus(expiryMinutes, ChronoUnit.MINUTES))
                .generatedAt(now)
                .failedAttempts(0)
                .locked(false)
                .build();

        otpRepository.save(record);

        // Send email — if this fails, the saved record will auto-expire via TTL
        emailService.sendOtpEmail(adminEmail, rawOtp);
        // rawOtp goes out of scope — no longer in memory after return

        log.info("OTP generated and sent for admin: {}", EmailService.maskEmail(adminEmail));

        return OtpChallengeResponse.builder()
                .otpToken(otpToken)
                .message("Verification code sent to your registered Gmail address.")
                .expiresIn(expiryMinutes * 60)
                .sentTo(EmailService.maskEmail(adminEmail))
                .build();
    }

    /**
     * Verifies the submitted OTP against the stored hash.
     *
     * @param otpToken   correlation token from the challenge response
     * @param rawOtp     the 6-digit OTP submitted by the admin
     * @param adminEmail email of the admin (must match the challenge record)
     */
    public void verifyOtp(String otpToken, String rawOtp, String adminEmail) {
        AdminOtp record = otpRepository.findByOtpToken(otpToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP session."));

        // Validate that the token belongs to this admin
        if (!record.getAdminEmail().equalsIgnoreCase(adminEmail)) {
            throw new UnauthorizedException("Invalid OTP session.");
        }

        // Check expiry
        if (Instant.now().isAfter(record.getExpiresAt())) {
            otpRepository.delete(record);
            throw new UnauthorizedException("OTP has expired. Please log in again to get a new code.");
        }

        // Check lock (too many wrong attempts)
        if (record.isLocked()) {
            throw new UnauthorizedException(
                    "Too many incorrect attempts. Please restart the login process.");
        }

        // Verify OTP hash
        boolean matches = passwordEncoder.matches(rawOtp, record.getHashedOtp());

        if (!matches) {
            int newFailed = record.getFailedAttempts() + 1;
            boolean nowLocked = newFailed >= maxAttempts;

            record.setFailedAttempts(newFailed);
            record.setLocked(nowLocked);
            otpRepository.save(record);

            if (nowLocked) {
                log.warn("Admin OTP locked after {} failed attempts: {}",
                        maxAttempts, EmailService.maskEmail(adminEmail));
                throw new UnauthorizedException(
                        "Too many incorrect attempts. Please restart the login process.");
            }

            int remaining = maxAttempts - newFailed;
            throw new UnauthorizedException(
                    "Incorrect verification code. " + remaining + " attempt(s) remaining.");
        }

        // ✅ OTP valid — delete immediately (one-time use)
        otpRepository.delete(record);
        log.info("Admin OTP verified successfully for: {}", EmailService.maskEmail(adminEmail));
    }

    /**
     * Re-sends a fresh OTP. Respects the resend cooldown.
     * The previous OTP is invalidated and a new one is generated.
     */
    public OtpChallengeResponse resendOtp(String otpToken, String adminEmail) {
        AdminOtp existing = otpRepository.findByOtpToken(otpToken)
                .orElseThrow(() -> new BadRequestException("OTP session not found. Please log in again."));

        if (!existing.getAdminEmail().equalsIgnoreCase(adminEmail)) {
            throw new UnauthorizedException("Invalid OTP session.");
        }

        if (!isCooldownExpired(existing)) {
            long waitSeconds = resendCooldownSeconds -
                    ChronoUnit.SECONDS.between(existing.getGeneratedAt(), Instant.now());
            throw new BadRequestException(
                    "Please wait " + waitSeconds + " more seconds before resending the code.");
        }

        // generateAndSendOtp will delete the old record (via deleteByAdminEmail) and create fresh one
        return generateAndSendOtp(adminEmail);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isCooldownExpired(AdminOtp record) {
        return ChronoUnit.SECONDS.between(record.getGeneratedAt(), Instant.now()) >= resendCooldownSeconds;
    }
}
