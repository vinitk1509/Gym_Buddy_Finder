# GymPartner (Gym Buddy Finder)

Find your perfect workout partner and crush your fitness goals together.

---

IMPORTANT: this repository contains the Spring Boot backend for the GymPartner project. The README previously referenced a React frontend at `frontend_gym/pair-your-pump` and a live demo; at the time of this update the frontend sources are not present in this repository. If the frontend is maintained in a separate repository or deployed elsewhere, please add a link or include it as a subdirectory/submodule.

## Live Application

A live demo is linked historically in this README — if you maintain a hosted frontend, add the live URL here. If the live demo is in a different repo, link that repo instead.

## Overview

GymPartner is a matchmaking backend that helps fitness enthusiasts find and connect with compatible gym partners. The backend provides REST endpoints for users, matchmaking, gyms, chat, file uploads, reporting and admin operations.

## Technology (backend)

- Language: Java 21
- Framework: Spring Boot
- Persistence: Spring Data JPA (Hibernate)
- Security: Spring Security (JWT via jjwt)
- Database: MySQL (development can use H2)
- Notable libs: Lombok, Cloudinary SDK (for image uploads)

## Project structure (important parts)

```
src/
  main/
    java/com/vinit/gymPartner/
      GymPartnerApplication.java   # Spring Boot entrypoint
      config/                      # Security + application configuration
      controller/                  # REST controllers for API endpoints
      service/                     # Business logic
      repository/                  # Spring Data JPA repositories
      entity/                      # JPA entities (User, Match, Gym, ...)
resources/                       # (see note on application.properties)
```

There is an `uploads/` directory at the repository root used by the backend for file handling.

## How to run (backend)

1. Prepare configuration

- Create `src/main/resources/application.properties` or set the required environment variables (recommended for secrets). Example keys the application expects include (but may not be limited to):

  - spring.datasource.url=jdbc:mysql://localhost:3306/gym_partner_db
  - spring.datasource.username=your_db_user
  - spring.datasource.password=your_db_password
  - spring.jpa.hibernate.ddl-auto=update
  - app.jwt.secret=your_jwt_secret
  - cloudinary.cloud_name=...
  - cloudinary.api_key=...
  - cloudinary.api_secret=...
  - spring.mail.host=...
  - spring.mail.port=...
  - spring.mail.username=...
  - spring.mail.password=...

For local development you can create `src/main/resources/application.properties` from the example above (do not commit secrets). A file `application.properties.example` would be helpful — consider adding one to the repo.

2. Build & run

From the repository root:

```bash
# make sure mvnw is executable: chmod +x ./mvnw
./mvnw spring-boot:run
```

The API is expected at: `http://localhost:8080/api/` (controllers map endpoints under `/api` in this project). If you prefer to build a jar and run it:

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar
```

## Frontend

The README previously referenced a frontend folder `frontend_gym/pair-your-pump`. That path is not present as a frontend app in this repository at the time of this update. If you have a separate frontend repository, add a link here and provide run instructions (install, dev server, environment variables). If you want the frontend inside this repo, add it under `frontend_gym/pair-your-pump/` and include a `package.json`.

## Docker

A Dockerfile exists in the repository root. Add explicit build/run instructions here and consider a `docker-compose.yml` that brings up MySQL + the backend for easier local dev.

## Required environment variables and external services

Document and provide example values for the following external integrations (if used by your code):

- JWT secret (app.jwt.secret)
- Database credentials (spring.datasource.*)
- Cloudinary credentials for file uploads
- SMTP credentials for email verification and notifications

## Tests / CI

The project should include unit/integration tests and a CI workflow to run `./mvnw test` on PRs. Note: `pom.xml` currently contains some non-standard `-test` starter dependencies (for example `spring-boot-starter-data-jpa-test`) which are not standard Spring Boot artifact coordinates and will cause Maven to fail to resolve dependencies. Recommended changes:

- Use `org.springframework.boot:spring-boot-starter-test` (scope=test) for the main test bundle
- Add `org.springframework.security:spring-security-test` (scope=test) for security-related tests

If you want, I can prepare a follow-up PR that corrects the pom test dependencies.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you'd like to modify. Consider adding a `CONTRIBUTING.md` with a short developer setup and PR checklist.

## License

The README previously referenced the MIT License. If this project is intended to be MIT licensed, add a `LICENSE` file to the repository root with the MIT text and keep this section. If not, update the license reference here.

---

Changelog of this README update:
- Clarified that this repository contains the backend and that the frontend is not present at `frontend_gym/pair-your-pump`.
- Added explicit backend run instructions and example environment variables.
- Added notes about incorrect test dependency artifacts in `pom.xml` and recommended fixes.
- Suggested adding `application.properties.example`, `CONTRIBUTING.md`, `LICENSE`, and a docker-compose file for a better developer experience.
