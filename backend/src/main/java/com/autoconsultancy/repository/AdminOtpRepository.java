package com.autoconsultancy.repository;

import com.autoconsultancy.entity.AdminOtp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AdminOtpRepository extends MongoRepository<AdminOtp, String> {

    Optional<AdminOtp> findByOtpToken(String otpToken);

    /** Find the most recently generated OTP for a given email (for cooldown check). */
    Optional<AdminOtp> findFirstByAdminEmailOrderByGeneratedAtDesc(String adminEmail);

    /** Delete all OTP records for the given email (invalidate previous OTPs). */
    void deleteByAdminEmail(String adminEmail);
}
