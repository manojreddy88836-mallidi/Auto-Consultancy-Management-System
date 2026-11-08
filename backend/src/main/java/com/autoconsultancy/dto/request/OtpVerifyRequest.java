package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerifyRequest {

    /** Opaque token received in the OTP challenge response. */
    @NotBlank(message = "OTP token is required")
    private String otpToken;

    /** The 6-digit OTP the admin received by email. */
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    private String otp;
}
