package com.vet_saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VetSaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(VetSaasApplication.class, args);
	}

}
