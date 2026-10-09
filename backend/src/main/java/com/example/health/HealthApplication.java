package com.example.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/** Starts the Spring Boot API and embedded Tomcat web server. */
public class HealthApplication {
    public static void main(String[] args) { SpringApplication.run(HealthApplication.class, args); }
}
