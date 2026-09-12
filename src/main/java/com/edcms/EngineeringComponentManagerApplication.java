package com.edcms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Engineering Component & Assembly Management System (EDCMS).
 *
 * <p>This application provides a REST API backend for managing:
 * <ul>
 *   <li>Engineering projects and assemblies</li>
 *   <li>Versioned components (Mechanical, Electrical, Software)</li>
 *   <li>Component dependencies and compatibility rules</li>
 *   <li>Impact analysis when component versions change</li>
 * </ul>
 *
 * <p>Tech Stack: Java 21, Spring Boot 3.3, Spring Data JPA, MySQL, Lombok
 *
 * <p>API docs available at: <a href="http://localhost:8080/swagger-ui.html">Swagger UI</a>
 */
@SpringBootApplication
public class EngineeringComponentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineeringComponentManagerApplication.class, args);
    }
}
