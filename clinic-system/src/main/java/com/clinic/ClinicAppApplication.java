package com.clinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

/**
 * ClinicAppApplication — entry point for the
 * Local Clinic Appointment & Queue Management System.
 *
 * Annotations:
 *  @SpringBootApplication   — enables auto-configuration, component scan, configuration
 *  @EnableJpaAuditing       — activates @CreatedDate / @LastModifiedDate on entities
 *  @EnableScheduling        — activates @Scheduled tasks (appointment reminders)
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class ClinicAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicAppApplication.class, args);
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}
