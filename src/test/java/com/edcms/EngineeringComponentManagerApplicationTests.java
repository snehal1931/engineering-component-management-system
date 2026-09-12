package com.edcms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the Spring application context loads successfully.
 *
 * <p>This test uses the "test" profile which connects to an H2 in-memory
 * database instead of MySQL, so no external database is required to run tests.
 *
 * <p>If this test passes, it means:
 * <ul>
 *   <li>All Spring beans are correctly configured</li>
 *   <li>No circular dependencies exist in the Spring context</li>
 *   <li>The application.yml configuration is valid</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class EngineeringComponentManagerApplicationTests {

    @Test
    void contextLoads() {
        // If the application context loads without errors, this test passes.
        // No assertions needed — Spring's context loading IS the assertion.
    }
}
