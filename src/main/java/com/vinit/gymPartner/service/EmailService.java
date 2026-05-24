package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.EmailVerificationCode;
import com.vinit.gymPartner.repository.EmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailVerificationCodeRepository codeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CHAT_PREVIEW_LIMIT = 140;

    /**
     * Generates a 6-digit code, stores it, sends it to the user, and logs it for local development.
     */
    public String generateAndSendCode(String email, int expiryMinutes) {
        String normalizedEmail = normalizeEmail(email);
        String code = String.format("%06d", RANDOM.nextInt(999999));

        EmailVerificationCode entity = EmailVerificationCode.builder()
                .email(normalizedEmail)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        codeRepository.save(entity);
        sendVerificationEmail(normalizedEmail, code, expiryMinutes);

        log.info("===================================================");
        log.info("  EMAIL VERIFICATION CODE for {}: {}", normalizedEmail, code);
        log.info("  Expires in {} minutes", expiryMinutes);
        log.info("===================================================");

        return code;
    }

    /**
     * Verifies a code for the given email. Returns true if valid and marks it as used.
     */
    public boolean verifyCode(String email, String code) {
        return codeRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(normalizeEmail(email))
                .filter(c -> matchesCode(c, code))
                .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(c -> {
                    c.setUsed(true);
                    codeRepository.save(c);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Checks whether the latest code is valid without consuming it.
     * Registration consumes the code when the account is finally created.
     */
    public boolean validateCode(String email, String code) {
        return codeRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(normalizeEmail(email))
                .filter(c -> matchesCode(c, code))
                .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Async
    public void sendMatchRequestEmail(String receiverEmail, String receiverName, String requesterName) {
        sendEventEmail(
                receiverEmail,
                receiverName,
                "New match request on GymPartner",
                """
                        <p><strong>%s</strong> wants to be your gym partner.</p>
                        <p>Open GymPartner to review and accept or decline the request.</p>
                        """.formatted(escapeHtml(requesterName))
        );
    }

    @Async
    public void sendSessionProposalEmail(
            String receiverEmail,
            String receiverName,
            String requesterName,
            String timeText
    ) {
        sendEventEmail(
                receiverEmail,
                receiverName,
                "New workout session proposal",
                """
                        <p><strong>%s</strong> proposed a workout session for <strong>%s</strong>.</p>
                        <p>Open GymPartner to accept or decline the proposal.</p>
                        """.formatted(escapeHtml(requesterName), escapeHtml(timeText))
        );
    }

    @Async
    public void sendChatMessageEmail(String receiverEmail, String receiverName, String senderName, String messagePreview) {
        String bodyHtml = isBlank(messagePreview)
                ? """
                        <p><strong>%s</strong> sent you a message on GymPartner.</p>
                        <p>Open GymPartner to read and reply.</p>
                        """.formatted(escapeHtml(senderName))
                : """
                        <p><strong>%s</strong> sent you a message:</p>
                        <blockquote style="border-left: 3px solid #16a34a; margin: 16px 0; padding-left: 12px; color: #374151;">%s</blockquote>
                        <p>Open GymPartner to reply.</p>
                        """.formatted(escapeHtml(senderName), escapeHtml(limitPreview(messagePreview)));

        sendEventEmail(
                receiverEmail,
                receiverName,
                "New message on GymPartner",
                bodyHtml
        );
    }

    @Async
    public void sendUserNoticeEmail(String receiverEmail, String receiverName, String subject, String body) {
        sendEventEmail(
                receiverEmail,
                receiverName,
                subject,
                "<p>%s</p>".formatted(escapeHtml(body))
        );
    }

    private void sendVerificationEmail(String email, String code, int expiryMinutes) {
        if (!validateMailConfig()) return;

        sendBrevoEmail(
                email,
                "Verify your GymPartner email",
                buildVerificationEmail(code, expiryMinutes)
        );
    }

    private void sendEventEmail(String receiverEmail, String receiverName, String subject, String bodyHtml) {
        if (isBlank(receiverEmail)) {
            log.warn("Skipping event email because receiver/email is missing");
            return;
        }

        if (!validateMailConfig()) return;

        sendBrevoEmail(
                receiverEmail,
                subject,
                buildEventEmail(receiverName, bodyHtml)
        );
    }

    private void sendBrevoEmail(String toEmail, String subject, String htmlContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", fromName.trim(), "email", fromEmail.trim()),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject("https://api.brevo.com/v3/smtp/email", request, String.class);
            log.info("Sent email to {} via Brevo", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send Brevo email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private boolean validateMailConfig() {
        if (isBlank(brevoApiKey) || isBlank(fromEmail)) {
            log.warn("BREVO_API_KEY is not configured. Real emails will not be sent.");
            return false;
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildVerificationEmail(String code, int expiryMinutes) {
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.5; color: #111827;">
                  <h2 style="margin-bottom: 8px;">Verify your GymPartner email</h2>
                  <p>Use this OTP to continue your registration:</p>
                  <div style="font-size: 28px; font-weight: 700; letter-spacing: 6px; margin: 20px 0;">%s</div>
                  <p>This code expires in %d minutes.</p>
                  <p style="color: #6b7280; font-size: 13px;">If you did not request this, you can ignore this email.</p>
                </div>
                """.formatted(code, expiryMinutes);
    }

    private String buildEventEmail(String name, String bodyHtml) {
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.5; color: #111827;">
                  <h2 style="margin-bottom: 8px;">GymPartner update</h2>
                  <p>Hi %s,</p>
                  %s
                  <p style="color: #6b7280; font-size: 13px;">You are receiving this because this update is related to your GymPartner account.</p>
                </div>
                """.formatted(escapeHtml(name), bodyHtml);
    }

    private String limitPreview(String messagePreview) {
        if (messagePreview == null) {
            return "";
        }
        String normalized = messagePreview.trim();
        if (normalized.length() <= CHAT_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, CHAT_PREVIEW_LIMIT - 3) + "...";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean matchesCode(EmailVerificationCode verificationCode, String code) {
        return code != null && verificationCode.getCode().equals(code.trim());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
