package com.autoconsultancy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Stores a pending admin OTP challenge.
 * <p>
 * Security design:
 * <ul>
 *   <li>The raw OTP is NEVER stored; only its BCrypt hash is kept.</li>
 *   <li>The {@code otpToken} (UUID) is what the frontend receives and sends back
 *       for correlation — it does not reveal the OTP.</li>
 *   <li>The document is deleted on first successful verification.</li>
 *   <li>MongoDB TTL index on {@code expiresAt} auto-purges expired records.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admin_otps")
public class AdminOtp {

    @Id
    private String id;

    /** Opaque correlation token returned to the frontend — not the OTP itself. */
    @Indexed(unique = true)
    private String otpToken;

    /** Email of the admin who initiated the challenge. */
    private String adminEmail;

    /** BCrypt hash of the 6-digit OTP. Never store the raw OTP. */
    private String hashedOtp;

    /** When this OTP expires (5 minutes after creation). */
    @Indexed(expireAfterSeconds = 0)   // MongoDB TTL — expires at this instant
    private Instant expiresAt;

    /** Timestamp of last OTP generation (used for resend cooldown enforcement). */
    private Instant generatedAt;

    /** Number of incorrect OTP attempts so far. */
    @Builder.Default
    private int failedAttempts = 0;

    /** True after max attempts exceeded — prevents further guesses. */
    @Builder.Default
    private boolean locked = false;
}
