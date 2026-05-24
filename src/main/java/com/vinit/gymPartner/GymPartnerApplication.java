package com.vinit.gymPartner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class GymPartnerApplication {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void fixDbSchema() {
		try {
			jdbcTemplate.execute("ALTER TABLE users MODIFY date_of_birth DATE NULL");
			jdbcTemplate.execute("ALTER TABLE users MODIFY gender VARCHAR(255) NULL");
		} catch (Exception e) {
			// ignore if columns don't exist or other errors
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(GymPartnerApplication.class, args);
	}

}
