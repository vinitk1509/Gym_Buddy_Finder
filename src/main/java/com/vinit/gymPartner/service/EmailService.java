package com.vinit.gymPartner.service;

import com.vinit.gymPartner.entity.EmailVerificationCode;
import com.vinit.gymPartner.repository.EmailVerificationCodeRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailVerificationCodeRepository codeRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

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
        validateMailConfig();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail.trim(), fromName.trim());
            helper.setTo(email);
            helper.setSubject("Verify your GymPartner email");
            helper.setText(buildVerificationEmail(code, expiryMinutes), true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
            log.error("Failed to send verification email to {}", email, ex);
            throw new RuntimeException("Could not send verification email: " + getRootMessage(ex));
        }
    }

    private void sendEventEmail(String receiverEmail, String receiverName, String subject, String bodyHtml) {
        if (isBlank(receiverEmail)) {
            log.warn("Skipping event email because receiver/email is missing");
            return;
        }

        try {
            validateMailConfig();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail.trim(), fromName.trim());
            helper.setTo(receiverEmail);
            helper.setSubject(subject);
            helper.setText(buildEventEmail(receiverName, bodyHtml), true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException ex) {
            log.warn("Failed to send event email '{}' to {}: {}", subject, receiverEmail, getRootMessage(ex));
        }
    }

    private void validateMailConfig() {
        if (isBlank(mailUsername) || isBlank(mailPassword) || isBlank(fromEmail)) {
            throw new RuntimeException(
                    "SMTP is not configured. Set SMTP_PASSWORD in your backend run environment and restart the server."
            );
        }
    }

    private String getRootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : throwable.getMessage();
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
