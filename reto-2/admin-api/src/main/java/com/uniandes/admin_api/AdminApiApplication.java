package com.uniandes.admin_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.uniandes.admin_api.infrastructure.config.ContextScorerProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(ContextScorerProperties.class)
public class AdminApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminApiApplication.class, args);
	}

}
