# GymPartner

GymPartner is a Spring Boot application designed to help users find and connect with suitable gym partners. The platform enables users to send, accept, reject, cancel, and terminate match requests, while enforcing business rules such as daily request limits, user blocking, and reliability scoring.

## Features

- **User Registration & Authentication**: Secure user registration and login with Spring Security and JWT.
- **Matchmaking System**:
  - Send, accept, reject, cancel, and terminate match requests.
  - Enforces daily match request limits.
  - Prevents interaction between blocked users.
  - Handles all match states: PENDING, ACCEPTED, REJECTED, CANCELLED, TERMINATED, EXPIRED.
- **User Reliability Scoring**: Adjusts user reliability based on match actions to encourage positive behavior.
- **Suggested Users**: Recommends potential partners not recently viewed.
- **Profile Management**: Users can update their fitness goals, experience, and preferences.
- **Admin & Moderation Tools** (planned): For reporting and managing users.

## Technology Stack
- Java 21
- Spring Boot 4
- Spring Data JPA (Hibernate)
- Spring Security (JWT)
- MySQL
- Lombok

## Getting Started

### Prerequisites
- Java 21+
- Maven
- MySQL

### Setup
1. **Clone the repository**
2. **Configure the database**
   - Update `src/main/resources/application.properties` with your MySQL credentials.
3. **Build and run**
   ```bash
   ./mvnw spring-boot:run
   ```
4. **API Endpoints**
   - Main endpoints are under `/api/` (see controller classes for details).

## Project Structure
- `entity/` - JPA entities (User, Match, Gym, etc.)
- `controller/` - REST controllers
- `service/` - Business logic
- `repository/` - Spring Data JPA repositories
- `dto/` - Data transfer objects
- `config/` - Security and application configuration

## Future Scope
- Advanced compatibility/matching algorithm
- Real-time notifications (WebSocket)
- Mobile app integration
- Analytics and reporting
- Enhanced admin tools and moderation
- Localization and timezone support

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License
[MIT](LICENSE)
