# 🧪 Testing Guide: Push Notifications & Admin Panel

This guide walks you through verifying that both Feature 4 (Firebase Push Notifications) and Feature 5 (Admin Panel) are working correctly in your Spring Boot backend.

---

# 🔔 Testing Feature 4: Push Notifications

Testing push notifications purely from a backend without an actual mobile app can be tricky because you need a real device token to get an actual notification popup. However, we can test that the **backend is successfully communicating with Firebase**.

### Step 1: Register a "Dummy" Device Token
Since we don't have a mobile app right now, we'll pretend to be a mobile app registering its token.

1.  Open **Postman**.
2.  Login as a regular user (e.g., `rahul@example.com`) to get a **Bearer Token**.
3.  Create a `POST` request to: `http://localhost:8080/api/users/device-token`
4.  Go to the **Params** tab and add:
    *   `token` = `test-token-12345` (In reality, the mobile app generates a long string here)
    *   `platform` = `ANDROID`
5.  Go to the **Auth** tab, select **Bearer Token**, and paste Rahul's token.
6.  Click **Send**.
    *   **Success Response:** `200 OK` with "Device token registered"

### Step 2: Trigger a Notification (Send a Match Request)
Now we will trigger the backend to send a notification to Rahul's device.

1.  Login as a *different* user (e.g., `priya@example.com`) to get a **Bearer Token**.
2.  Send a match request to Rahul using Priya's token.
    *   `POST http://localhost:8080/api/matches/request?receiverId=<RAHUL_ID>`
3.  Click **Send**.

### Step 3: Verify the Spring Boot Console Logs
Since `test-token-12345` is a fake token, Firebase will reject it. We want to see our backend attempt to send it and gracefully handle the failure.

1.  Open your **IntelliJ IDEA console** (or wherever your Spring app is running).
2.  Look for a log message from `NotificationService`.
    *   You should see a warning like: `Failed to send notification to token: test-token-12345. Error: The registration token is not a valid FCM registration token`
    *   **🎉 SUCCESS!** This log means your Spring Boot backend successfully reached out to Google Firebase! Once a real frontend sends a valid token, it will work perfectly.

*(Note: The exact same flow applies to sending a Chat Message via WebSocket. It will trigger a notification to the receiver's token).*

---

# 🛡️ Testing Feature 5: Admin Panel

The Admin Panel relies heavily on **Role-Based Access Control (RBAC)**. We must verify that normal users are blocked, but Admins are allowed.

### Step 1: Prove Normal Users are Blocked (Security Test)
1.  Open **Postman**.
2.  Login as a standard user to get a **Bearer Token**.
3.  Try to access the Admin Dashboard:
    *   `GET http://localhost:8080/api/admin/dashboard`
    *   Auth: Bearer Token (paste the normal user's token)
4.  Click **Send**.
    *   **Success Response:** `403 Forbidden`. The `@PreAuthorize("hasRole('ADMIN')")` correctly blocked a normal user.

### Step 2: Promote a User to Admin
Because we intentionally have no "admin registration" endpoint (for security reasons), you must promote an admin directly in the database.

1.  Open **MySQL Workbench**.
2.  Run this query to upgrade a specific user:
    ```sql
    UPDATE users SET role = 'ADMIN' WHERE email = 'rahul@example.com';
    ```

### Step 3: Login to Get the Admin Token
**Crucial:** You must login *again* to get a new JWT token! The old token still says "USER".

1.  In Postman, `POST http://localhost:8080/api/auth/login` with your Admin's credentials (`rahul@example.com`).
2.  Copy the new **Bearer Token**.

### Step 4: Test the Admin Dashboard
1.  Create a `GET` request to: `http://localhost:8080/api/admin/dashboard`
2.  Auth tab: Paste the **Admin Bearer Token**.
3.  Click **Send**.
    *   **Success Response:** `200 OK`
    *   You should see a JSON payload containing stats: `totalUsers`, `activeUsers`, `bannedUsers`, `totalMatches`, etc.

### Step 5: Test Banning a User
Let's ban user ID `x` (make sure you pick the ID of a normal user, not the admin!).

1.  Create a `POST` request to: `http://localhost:8080/api/admin/users/2/ban` (assuming Priya is user ID 2)
2.  Auth tab: Admin Bearer Token.
3.  Click **Send**.
    *   **Success Response:** `User banned successfully`
4.  **(Verification):** Try logging in as Priya via `/api/auth/login`. Since she is banned, your `UserService` or `CustomUserDetailsService` logic should ideally block her login. If it doesn't currently, you can modify `UserService` login to reject `UserStatus.BANNED`.

### Step 6: Test Unbanning a User
1.  Create a `POST` request to: `http://localhost:8080/api/admin/users/2/unban`
2.  Auth tab: Admin Bearer Token.
3.  Click **Send**.
    *   **Success Response:** `User unbanned successfully`

### Step 7: Test Resolving a Report
*(You will need to create a Report first if your database doesn't have one).*

1.  Create a `POST` request to: `http://localhost:8080/api/admin/reports/1/resolve`
2.  Go to **Params** tab and add:
    *   `resolution` = `RESOLVED`
3.  Auth tab: Admin Bearer Token.
4.  Click **Send**.
    *   **Success Response:** `Report resolved`
5.  Check your MySQL database (`select * from user_report;`) to see that the status changed to `RESOLVED`.
