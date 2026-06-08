package com.pos.pos_backend.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final Resend resend;

    @Value("${app.resend.from-email}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * We inject the API key via constructor so the Resend client
     * is created once and reused — not recreated on every send.
     */
    public EmailService(@Value("${app.resend.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    // ── Password Reset Email ──────────────────────────────────────────────────

    /**
     * Sends a password reset link to the user's email.
     *
     * The rawToken is the un-hashed token — what we put in the URL.
     * The hashed version is stored in the DB. When the user clicks the link,
     * the raw token comes back, we hash it, and compare to the DB value.
     *
     * The reset link format: {frontendUrl}/reset-password?token={rawToken}
     */
    public void sendPasswordReset(String toEmail, String rawToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        String htmlBody = buildPasswordResetHtml(toEmail, resetLink);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("onboarding@resend.dev")   // ← temp until you verify a domain
                .to(toEmail)
                .subject("Reset your POS password")
                .html(htmlBody)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            log.info("Password reset email sent to {} — Resend ID: {}", toEmail, response.getId());
        } catch (ResendException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            // Catch any RuntimeException the SDK might wrap errors in
            log.error("Unexpected error sending password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
    // ── HTML Template ─────────────────────────────────────────────────────────

    /**
     * Builds a clean HTML email for the password reset.
     * Inline styles are used because many email clients strip <style> tags.
     */
    private String buildPasswordResetHtml(String toEmail, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Reset your password</title>
                </head>
                <body style="margin:0;padding:0;background-color:#F0F4F8;font-family:Inter,Arial,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F0F4F8;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table width="560" cellpadding="0" cellspacing="0"
                                       style="background-color:#FFFFFF;border-radius:12px;
                                              box-shadow:0 1px 3px rgba(0,0,0,0.1);overflow:hidden;">

                                    <!-- Header -->
                                    <tr>
                                        <td style="background-color:#1A3C5E;padding:32px 40px;text-align:center;">
                                            <h1 style="margin:0;color:#FFFFFF;font-size:22px;font-weight:700;
                                                        letter-spacing:-0.5px;">
                                                POS
                                            </h1>
                                            <p style="margin:6px 0 0;color:rgba(255,255,255,0.6);font-size:13px;">
                                                Smart Point of Sale
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:40px 40px 32px;">
                                            <h2 style="margin:0 0 12px;color:#1A3C5E;font-size:20px;font-weight:700;">
                                                Reset your password
                                            </h2>
                                            <p style="margin:0 0 24px;color:#64748B;font-size:15px;line-height:1.6;">
                                                We received a request to reset the password for your account
                                                (<strong style="color:#1A1A2E;">%s</strong>).
                                                Click the button below to set a new password.
                                            </p>

                                            <!-- CTA Button -->
                                            <table cellpadding="0" cellspacing="0" style="margin:0 0 28px;">
                                                <tr>
                                                    <td style="background-color:#0D7377;border-radius:8px;">
                                                        <a href="%s"
                                                           style="display:inline-block;padding:14px 32px;
                                                                  color:#FFFFFF;font-size:15px;font-weight:600;
                                                                  text-decoration:none;border-radius:8px;">
                                                            Reset Password
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin:0 0 8px;color:#64748B;font-size:13px;line-height:1.6;">
                                                If the button doesn't work, copy and paste this link into your browser:
                                            </p>
                                            <p style="margin:0 0 28px;word-break:break-all;">
                                                <a href="%s"
                                                   style="color:#0D7377;font-size:13px;text-decoration:underline;">
                                                    %s
                                                </a>
                                            </p>

                                            <!-- Warning box -->
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="background-color:#FFF3E0;border-left:4px solid #B85C00;
                                                                border-radius:4px;padding:14px 16px;">
                                                        <p style="margin:0;color:#B85C00;font-size:13px;line-height:1.5;">
                                                            <strong>This link expires in 1 hour</strong> and can only
                                                            be used once. If you didn't request a password reset,
                                                            you can safely ignore this email.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color:#F8FAFD;padding:24px 40px;
                                                    border-top:1px solid #BFCFE0;text-align:center;">
                                            <p style="margin:0;color:#64748B;font-size:12px;line-height:1.6;">
                                                This email was sent to %s.<br>
                                                If you have questions, contact your system administrator.
                                            </p>
                                            <p style="margin:8px 0 0;color:#BFCFE0;font-size:11px;">
                                                © 2026 POS — Smart Point of Sale for Nigerian Businesses
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(toEmail, resetLink, resetLink, resetLink, toEmail);
    }
}