package com.autoconsultancy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned when admin credentials are valid but OTP verification is still required.
 * Does NOT contain the OTP or any JWT — just an opaque token the client uses
 * to correlate the subsequent OTP submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpChallengeResponse {

    /** Opaque UUID the client must send back with the OTP. */
    private String otpToken;

    /** Human-readable status message. */
    private String message;

    /** Seconds until OTP expires (always 300 = 5 min). */
    private int expiresIn;

    /** Masked destination email shown to the admin (e.g. "v***i@gmail.com"). */
    private String sentTo;
}
