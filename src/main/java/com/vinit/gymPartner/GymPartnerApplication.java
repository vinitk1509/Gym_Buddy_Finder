package com.vinit.gymPartner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GymPartnerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GymPartnerApplication.class, args);
	}

}
