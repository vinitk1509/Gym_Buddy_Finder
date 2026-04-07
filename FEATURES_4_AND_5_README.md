# Feature 4️⃣ — Push Notifications (Firebase Cloud Messaging)

## 🧠 Concept
Push notifications let you **alert users instantly** — even when they're not on the app. When someone sends a match request or a chat message, the receiver gets a notification on their phone/browser.

**Technology:** We'll use **Firebase Cloud Messaging (FCM)**. It's free, works on Android/iOS/Web, and is the industry standard.

**How it works:**
1. User's device registers with Firebase → gets a unique **device token**.
2. Your backend stores this token in a `device_tokens` table.
3. When an event happens (match request, new message), your backend sends a push notification to Firebase using the token.
4. Firebase delivers it to the user's device.

## 🛠️ Required Dependency — Add to `pom.xml`

```xml
<!-- Firebase Admin SDK for sending push notifications -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>
```

## 🛠️ What Changes

### Entity: `DeviceToken.java` — Already Exists ✅

This file already exists in your codebase. No changes needed.

```java
package com.vinit.gymPartner.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "device_tokens")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which user this device belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The FCM registration token (unique per device)
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    // Device platform: "ANDROID", "IOS", or "WEB"
    private String platform;
}
```

### Repository: `DeviceTokenRepository.java` — [NEW]

> [!WARNING]
> This file is **MISSING** from your codebase. Without it, `NotificationService` cannot compile. Create it now.

```java
package com.vinit.gymPartner.repository;

import com.vinit.gymPartner.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    // Get all device tokens for a specific user (user may have multiple devices)
    List<DeviceToken> findByUserId(Long userId);

    // Find a specific token (to avoid duplicates when registering)
    Optional<DeviceToken> findByToken(String token);

    // Delete all tokens for a user (when they log out or deactivate)
    void deleteByUserId(Long userId);
}
```

### Config: `FirebaseConfig.java` — [FIX]

> [!WARNING]
> Your current file has a **wrong file path** on line 17. It uses `/src/main/resources/...` which is an absolute Linux path that will never resolve on Windows. Replace the entire file with this corrected version that uses Spring's `ClassPathResource` — this works in both local dev and deployed JAR.

```java
package com.vinit.gymPartner.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    /**
     * Initialize Firebase when the Spring application starts.
     * The service account JSON file is downloaded from your Firebase Console.
     *
     * We use ClassPathResource instead of FileInputStream because:
     * - FileInputStream("src/main/resources/...") only works when running from IDE
     * - ClassPathResource works everywhere — IDE, JAR, Docker, cloud deployment
     */
    @PostConstruct
    public void initialize() throws IOException {
        InputStream serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
```

### Service: `NotificationService.java` — [FIX]

> [!WARNING]
> Your current file is **broken** — line 11 cuts off mid-word (`private final DeviceToke`). It will not compile. Replace the entire file.

```java
package com.vinit.gymPartner.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.vinit.gymPartner.entity.DeviceToken;
import com.vinit.gymPartner.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Send a push notification to a specific user (on ALL their registered devices).
     *
     * @param userId  The recipient's user ID
     * @param title   Notification title (e.g., "New Match Request! 🏋️")
     * @param body    Notification body (e.g., "Rahul wants to be your gym partner!")
     */
    public void sendToUser(Long userId, String title, String body) {
        // Get all device tokens for this user
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);

        if (tokens.isEmpty()) {
            log.info("No device tokens found for user {}, skipping notification", userId);
            return;
        }

        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Notification sent successfully: {}", response);

            } catch (Exception e) {
                // Token might be expired — log and clean up
                log.warn("Failed to send notification to token: {}. Error: {}",
                        deviceToken.getToken(), e.getMessage());
            }
        }
    }
}
```

### Controller: Add Device Token Registration to `UserController.java`

When a user logs in on their phone or browser, the frontend sends its FCM token to the backend so we can send them notifications later.

```java
// Add these imports to UserController.java:
import com.vinit.gymPartner.entity.DeviceToken;
import com.vinit.gymPartner.repository.DeviceTokenRepository;

// Add this field (inject via constructor):
private final DeviceTokenRepository deviceTokenRepository;

// Add this new endpoint to UserController.java:

/**
 * Register a device token for push notifications.
 * Called by the frontend/mobile app after Firebase gives it a token.
 */
@PostMapping("/device-token")
public ResponseEntity<String> registerDeviceToken(
        @RequestParam String token,
        @RequestParam(defaultValue = "ANDROID") String platform,
        Authentication authentication) {

    User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Avoid duplicate tokens (same device registering twice)
    if (deviceTokenRepository.findByToken(token).isEmpty()) {
        DeviceToken deviceToken = DeviceToken.builder()
                .user(user)
                .token(token)
                .platform(platform)
                .build();
        deviceTokenRepository.save(deviceToken);
    }

    return ResponseEntity.ok("Device token registered");
}
```

### Where to Trigger Notifications — Modify Existing Services

```java
// ─── In MatchService.java ────────────────────────────────────────
// Add this field (inject via constructor):
private final NotificationService notificationService;

// In sendMatchRequest(), after saving the match:
notificationService.sendToUser(
    receiverId,
    "New Match Request! 🏋️",
    requester.getName() + " wants to be your gym partner!"
);

// ─── In ChatService.java ─────────────────────────────────────────
// Add this field (inject via constructor):
private final NotificationService notificationService;

// In sendMessage(), after saving the message:
notificationService.sendToUser(
    dto.getReceiverId(),
    "New Message 💬",
    "You have a new message from your gym partner"
);
```

### Setup Steps (One-Time)
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project → "GymPartner"
3. Go to **Project Settings → Service Accounts → Generate New Private Key**
4. Download the JSON file → save as `src/main/resources/firebase-service-account.json`
5. **Add to `.gitignore`**: `firebase-service-account.json` (NEVER commit secrets)

> [!IMPORTANT]
> The Firebase JSON key file contains your private credentials. If you push it to GitHub, anyone can hijack your Firebase project. Your `.gitignore` already has `src/main/resources/gympartner-*.json` — make sure the new file also matches.

---

# Feature 5️⃣ — Admin Panel

## 🧠 Concept
An Admin Panel lets you **moderate the platform**: view reports, ban users, resolve issues, and see platform stats. You already have `UserRole.ADMIN` and `UserReport` entity — we just need the admin-specific endpoints and authorization.

## 🛠️ What Changes

### DTO: `AdminDashboardDTO.java` — [NEW]

```java
package com.vinit.gymPartner.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdminDashboardDTO {
    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;
    private long totalMatches;
    private long openReports;
    private List<UserReportSummaryDTO> recentReports;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class UserReportSummaryDTO {
        private Long reportId;
        private String reporterName;
        private String reportedUserName;
        private String reason;
        private String status;
        private String createdAt;
    }
}
```

### Service: `AdminService.java` — [NEW]

```java
package com.vinit.gymPartner.service;

import com.vinit.gymPartner.dto.AdminDashboardDTO;
import com.vinit.gymPartner.entity.User;
import com.vinit.gymPartner.entity.UserReport;
import com.vinit.gymPartner.entity.enums.*;
import com.vinit.gymPartner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserReportRepository userReportRepository;

    /**
     * Build the admin dashboard with platform-wide statistics.
     */
    public AdminDashboardDTO getDashboard() {
        return AdminDashboardDTO.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .bannedUsers(userRepository.countByStatus(UserStatus.BANNED))
                .totalMatches(matchRepository.count())
                .openReports(userReportRepository.countByStatus(ReportStatus.OPEN))
                .recentReports(getRecentReports())
                .build();
    }

    /**
     * Ban a user: set their status to BANNED.
     * Banned users cannot login, send matches, or chat.
     */
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
    }

    /**
     * Unban a user: reactivate their account.
     */
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    /**
     * Resolve a user report after admin reviews it.
     * Resolution can be: RESOLVED, REJECTED, or UNDER_REVIEW.
     */
    public void resolveReport(Long reportId, ReportStatus resolution) {
        UserReport report = userReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(resolution);
        userReportRepository.save(report);
    }

    /**
     * Get the 10 most recent reports for the dashboard quick view.
     */
    private List<AdminDashboardDTO.UserReportSummaryDTO> getRecentReports() {
        return userReportRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(r -> AdminDashboardDTO.UserReportSummaryDTO.builder()
                        .reportId(r.getId())
                        .reporterName(r.getReporter().getName())
                        .reportedUserName(r.getReportedUser().getName())
                        .reason(r.getReason().name())
                        .status(r.getStatus().name())
                        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "N/A")
                        .build())
                .toList();
    }
}
```

### Controller: `AdminController.java` — [NEW]

```java
package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.dto.AdminDashboardDTO;
import com.vinit.gymPartner.entity.enums.ReportStatus;
import com.vinit.gymPartner.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // Only users with ADMIN role can access
public class AdminController {

    private final AdminService adminService;

    /**
     * GET /api/admin/dashboard
     * Returns platform-wide stats: total users, active, banned, matches, reports.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    /**
     * POST /api/admin/users/{userId}/ban
     * Ban a user — they can no longer access the platform.
     */
    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<String> banUser(@PathVariable Long userId) {
        adminService.banUser(userId);
        return ResponseEntity.ok("User banned successfully");
    }

    /**
     * POST /api/admin/users/{userId}/unban
     * Unban a user — reactivate their account.
     */
    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return ResponseEntity.ok("User unbanned successfully");
    }

    /**
     * POST /api/admin/reports/{reportId}/resolve?resolution=RESOLVED
     * Resolve a user report. Allowed values: RESOLVED, REJECTED, UNDER_REVIEW
     */
    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<String> resolveReport(
            @PathVariable Long reportId,
            @RequestParam ReportStatus resolution) {
        adminService.resolveReport(reportId, resolution);
        return ResponseEntity.ok("Report resolved");
    }
}
```

### Repository Updates Needed

```java
// Add to UserRepository.java:
long countByStatus(UserStatus status);

// Add to UserReportRepository.java:
long countByStatus(ReportStatus status);
List<UserReport> findTop10ByOrderByCreatedAtDesc();
```

### Modify `SecurityConfig.java` — Add Admin Authorization

```java
// 1. Add this import at the top:
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// 2. Add @EnableMethodSecurity to the class (alongside @EnableWebSecurity):
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity  // <-- ADD THIS — enables @PreAuthorize annotations

// 3. In your filterChain, add this line BEFORE .anyRequest().authenticated():
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

### Create an Admin User — One-Time SQL

Since there's no admin registration endpoint (intentionally, for security), insert via MySQL directly:

```sql
-- First register a user normally via Postman, then promote them:
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@gympartner.com';
```

> [!TIP]
> **For your resume:** Highlight these specific keywords: *Firebase Cloud Messaging, role-based access control (RBAC), admin dashboard, @PreAuthorize, user moderation system*. These are exactly what interviewers look for in a backend/full-stack portfolio.

---

## 🚀 Implementation Checklist

Use this to track your progress:

### Feature 4 — Push Notifications
- [ ] Create `DeviceTokenRepository.java` — [NEW]
- [ ] Fix `NotificationService.java` — replace broken file
- [ ] Fix `FirebaseConfig.java` — correct the file path
- [ ] Add device token registration endpoint in `UserController.java`
- [ ] Inject & trigger notifications in `MatchService.java`
- [ ] Inject & trigger notifications in `ChatService.java`
- [ ] Download Firebase service account JSON → `src/main/resources/`
- [ ] Add `firebase-service-account.json` to `.gitignore`

### Feature 5 — Admin Panel
- [ ] Create `AdminDashboardDTO.java`
- [ ] Create `AdminService.java`
- [ ] Create `AdminController.java`
- [ ] Add `countByStatus()` to `UserRepository.java`
- [ ] Add `countByStatus()` + `findTop10ByOrderByCreatedAtDesc()` to `UserReportRepository.java`
- [ ] Update `SecurityConfig.java` — add admin route + `@EnableMethodSecurity`
- [ ] Create admin user in database via SQL
