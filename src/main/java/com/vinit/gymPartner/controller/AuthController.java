package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.LoginRequest;
import com.vinit.gymPartner.dto.LoginResponse;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.repository.UserRepository;
import com.vinit.gymPartner.security.JwtService;
import com.vinit.gymPartner.service.EmailService;
import com.vinit.gymPartner.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request)
    {
        String email = normalizeEmail(request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow();

        // Record login timestamp for inactivity tracking
        userService.recordLogin(user);

        String token = jwtService.generateToken(user.getId() , user.getEmail());

        return ResponseEntity.ok(
                LoginResponse.builder()
                        .token(token)
                        .userId(user.getId())
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        userService.recordLogout(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // ──── Email Verification ────

    @PostMapping("/send-verification")
    public ResponseEntity<String> sendVerification(@RequestParam String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new RuntimeException("Email already registered.");
        }

        emailService.generateAndSendCode(normalizedEmail, 15);
        return ResponseEntity.ok("Verification code sent to " + normalizedEmail);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String email, @RequestParam String code) {
        boolean valid = emailService.validateCode(normalizeEmail(email), code);
        if (!valid) {
            return ResponseEntity.badRequest().body("Invalid or expired verification code.");
        }

        return ResponseEntity.ok("Email verified successfully.");
    }

    // ──── Forgot Password ────

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No account found with this email."));

        emailService.generateAndSendCode(normalizedEmail, 15);
        return ResponseEntity.ok("OTP sent to " + normalizedEmail);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        boolean valid = emailService.verifyCode(email, otp);
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP."));
        }

        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElseThrow();
        String resetToken = jwtService.generatePasswordResetToken(user.getId(), user.getEmail());

        return ResponseEntity.ok(Map.of("resetToken", resetToken, "message", "OTP verified."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
        String resetToken = body.get("resetToken");
        String newPassword = body.get("newPassword");

        if (!jwtService.isPasswordResetTokenValid(resetToken)) {
            return ResponseEntity.badRequest().body("Invalid or expired reset token.");
        }

        String email = jwtService.extractEmail(resetToken);
        userService.resetPassword(email, newPassword);

        return ResponseEntity.ok("Password reset successfully.");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
