package com.vinit.gymPartner.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkoutSessionSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String databaseProductName = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName()
        );

        if (databaseProductName == null || !databaseProductName.toLowerCase().contains("mysql")) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE workout_sessions MODIFY COLUMN state VARCHAR(32) NOT NULL");
        log.info("Verified workout_sessions.state can store session request states");
    }
}
