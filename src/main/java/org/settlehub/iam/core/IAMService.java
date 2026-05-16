package org.settlehub.iam.core;

import org.settlehub.commons.health.annotation.EnableHealthCheck;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableHealthCheck
@ComponentScan(basePackages = {"org.settlehub.iam"})
public class IAMService {

	public static void main(String[] args) {
		SpringApplication.run(IAMService.class, args);
	}

}
