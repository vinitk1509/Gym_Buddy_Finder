<div align="center">
  <h1>🏋️‍♂️ GymPartner</h1>
  <p><strong>Find your perfect workout partner and crush your fitness goals together.</strong></p>
  [![Live Demo](https://img.shields.io/badge/Live_Demo-gym--partner--five.vercel.app-brightgreen?style=for-the-badge&logo=vercel)](https://gym-partner-five.vercel.app/)
</div>
<br />
## 🚀 Live Application
**Try it out here:** [https://gym-partner-five.vercel.app/](https://gym-partner-five.vercel.app/)
## 📖 Overview
GymPartner is a full-stack platform designed to help fitness enthusiasts find and connect with compatible gym buddies. The application facilitates matching based on fitness goals, experience, and preferences, providing a seamless way to send, accept, and manage match requests. With features like daily request limits, user blocking, and reliability scoring, GymPartner ensures a safe and engaging community for everyone.
## ✨ Key Features
*   **User Registration & Authentication**: Secure user registration and login utilizing Spring Security and JWT.
*   **Intelligent Matchmaking System**:
    *   Send, accept, reject, cancel, and terminate match requests seamlessly.
    *   Enforcement of daily match request limits to maintain quality interactions.
    *   Prevention of interactions between blocked users.
    *   Comprehensive match state handling (PENDING, ACCEPTED, REJECTED, CANCELLED, TERMINATED, EXPIRED).
*   **User Reliability Scoring**: A dynamic system that adjusts a user's reliability score based on their matchmaking actions to encourage positive community behavior.
*   **Smart Suggestions**: Recommends potential partners that you haven't recently viewed.
*   **Profile Management**: Easily update fitness goals, experience level, and preferences to find the best match.
*   **Modern Frontend Experience**: A responsive, interactive UI built with React, Tailwind CSS, and Framer Motion for smooth animations.
## 🛠️ Technology Stack
### Backend
*   **Java 21**
*   **Spring Boot**
*   **Spring Data JPA (Hibernate)**
*   **Spring Security (JWT)**
*   **MySQL**
*   **Lombok**
### Frontend
*   **React 19**
*   **TypeScript**
*   **Tailwind CSS v4**
*   **TanStack Query & Router**
*   **Framer Motion** (for animations)
*   **Radix UI** (for accessible components)
## 💻 Getting Started
### Prerequisites
*   Java 21+
*   Node.js & npm/bun (for frontend)
*   Maven
*   MySQL Server
### Backend Setup
1. **Configure the database:**
   Update `src/main/resources/application.properties` with your MySQL database credentials.
2. **Build and run the Spring Boot application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The API will be available at `http://localhost:8080/api/`.
### Frontend Setup
1. **Navigate to the frontend directory:**
   ```bash
   cd frontend_gym/pair-your-pump
   ```
2. **Install dependencies:**
   ```bash
   npm install
   # or bun install
   ```
3. **Run the development server:**
   ```bash
   npm run dev
   # or bun run dev
   ```
   The frontend will be accessible locally via Vite.
## 📁 Project Structure
*   **`src/`** (Backend): Contains all the Java Spring Boot code.
    *   `entity/` - JPA entities (User, Match, Gym, etc.)
    *   `controller/` - REST controllers mapping API endpoints
    *   `service/` - Core business logic and rules
    *   `repository/` - Spring Data JPA repositories for DB operations
    *   `dto/` - Data Transfer Objects for API requests/responses
    *   `config/` - Security and global application configurations
*   **`frontend_gym/pair-your-pump/`** (Frontend): The React application source code.
## 🔮 Future Scope
*   Advanced compatibility and matching algorithm based on location and schedules.
*   Real-time notifications and chat via WebSocket integration.
*   Mobile application development.
*   Detailed analytics and user reporting features.
*   Enhanced administrative tools and moderation dashboard.
*   Localization and timezone support for global availability.
## 🤝 Contributing
Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to modify.
## 📄 License
This project is licensed under the [MIT License](LICENSE).
