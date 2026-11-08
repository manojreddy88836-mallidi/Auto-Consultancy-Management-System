package com.autoconsultancy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpResendRequest {

    @NotBlank(message = "OTP token is required")
    private String otpToken;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Must be a valid email address")
    private String adminEmail;
}
