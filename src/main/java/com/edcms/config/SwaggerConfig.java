package com.edcms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures Swagger / OpenAPI documentation for the EDCMS REST API.
 *
 * <p>Access the interactive docs at: http://localhost:8080/swagger-ui.html
 *
 * <p>OOP Note: This is a pure configuration class — it has a single responsibility
 * (configuring API documentation) and no business logic. Keeping this in
 * a dedicated config package follows the Single Responsibility Principle.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI edcmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Engineering Component & Assembly Management System API")
                        .description("""
                                REST API for managing engineering projects, versioned components,
                                assemblies, dependency graphs, and compatibility rules.
                                
                                This is an independent educational project inspired by engineering
                                data management challenges found in industrial software systems.
                                
                                Key capabilities:
                                - Project and Assembly lifecycle management
                                - Component versioning with state machine (DRAFT → APPROVED → DEPRECATED)
                                - Compatibility rule checking before assembly composition
                                - Circular dependency detection (DFS algorithm)
                                - Impact analysis — find all assemblies affected by a version change
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EDCMS Project")
                                .email("your-email@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ));
    }
}
