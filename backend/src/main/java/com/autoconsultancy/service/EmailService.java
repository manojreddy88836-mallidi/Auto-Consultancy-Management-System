package com.autoconsultancy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.autoconsultancy.exception.BadRequestException;
import jakarta.mail.internet.MimeMessage;

/**
 * Sends transactional emails via Gmail SMTP.
 * Credentials are read from environment variables — never hardcoded.
 * The OTP is embedded in the email body but is NEVER logged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * Sends an OTP verification email to the admin.
     *
     * @param toEmail  recipient address
     * @param otp      the raw 6-digit OTP (sent in email body — never logged)
     * @throws RuntimeException if email delivery fails
     */
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // MimeMessageHelper(msg, multipart, charset) — may throw UnsupportedEncodingException
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // setFrom with personal name may throw UnsupportedEncodingException
            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Admin Login Verification Code");
            helper.setText(buildOtpEmailHtml(otp), true);
            mailSender.send(message);
            log.info("OTP email dispatched to {}", maskEmail(toEmail));

        } catch (MailException e) {
            log.error("SMTP error sending OTP to {}: {}", maskEmail(toEmail), e.getMessage());
            throw new com.autoconsultancy.exception.BadRequestException(
                "Failed to send OTP email. Please check your Gmail App Password configuration.");
        } catch (Exception e) {
            log.error("Unexpected error sending OTP to {}: {}", maskEmail(toEmail), e.getMessage());
            throw new com.autoconsultancy.exception.BadRequestException(
                "Failed to send OTP email. Please try again.");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildOtpEmailHtml(String otp) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Admin Login Verification</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                        <!-- Header -->
                        <tr>
                          <td style="background:#0F1B35;padding:32px 40px;text-align:center;">
                            <span style="color:#ffffff;font-size:20px;font-weight:700;">&#127949; Auto Consultancy</span>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="padding:40px 48px;">
                            <h2 style="color:#0F1B35;font-size:22px;margin:0 0 12px;">Admin Login Verification</h2>
                            <p style="color:#555;font-size:15px;line-height:1.6;margin:0 0 28px;">
                              We received a login request for your Admin account. Use the verification code below to complete sign-in.
                            </p>

                            <!-- OTP box -->
                            <div style="background:#EFF6FF;border:2px dashed #1E88E5;border-radius:12px;padding:28px;text-align:center;margin-bottom:28px;">
                              <p style="margin:0 0 8px;color:#666;font-size:13px;text-transform:uppercase;letter-spacing:1px;">Your Verification Code</p>
                              <div style="font-size:48px;font-weight:900;letter-spacing:12px;color:#0F1B35;font-family:monospace;">%s</div>
                              <p style="margin:12px 0 0;color:#888;font-size:13px;">&#9201; Expires in <strong>5 minutes</strong></p>
                            </div>

                            <!-- Warning -->
                            <div style="background:#FFF3CD;border-left:4px solid #FFC107;border-radius:6px;padding:14px 18px;margin-bottom:28px;">
                              <p style="margin:0;color:#856404;font-size:13px;line-height:1.5;">
                                <strong>&#9888; Security Notice:</strong> Never share this code with anyone. Auto Consultancy staff will never ask for your OTP.
                                If you did not attempt to log in, your account may be under attack &mdash; change your password immediately.
                              </p>
                            </div>

                            <p style="color:#888;font-size:12px;line-height:1.5;margin:0;">
                              This code is single-use and expires exactly 5 minutes from the time of this email.
                              If it has expired, go back and request a new code.
                            </p>
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="background:#f8fafc;padding:20px 48px;border-top:1px solid #e8ecf0;">
                            <p style="margin:0;color:#aaa;font-size:12px;text-align:center;">
                              Auto Consultancy Management System &bull; This is an automated message &bull; Do not reply
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(otp);
    }

    /** Masks email for safe logging: "venkatamanojreddymallidi@gmail.com" → "v***i@gmail.com" */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) return "***@" + parts[1];
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }
}
