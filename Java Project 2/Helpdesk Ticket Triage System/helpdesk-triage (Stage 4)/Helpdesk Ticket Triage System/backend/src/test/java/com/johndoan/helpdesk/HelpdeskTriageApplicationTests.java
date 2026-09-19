package com.johndoan.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: verifies that the application's beans — controller, mapper, service,
 * repository, the JPA entity manager — wire together and the context starts. If this
 * fails, something in the wiring is broken.
 *
 * <p>It does NOT verify the production configuration. {@code src/test/resources/application.yml}
 * REPLACES {@code src/main/resources/application.yml} rather than merging with it (Spring Boot
 * loads the first {@code application.yml} on the classpath, and test-classes precedes classes),
 * so the datasource, the ddl-auto mode and the actuator exposure exercised here are the test
 * values. A mistake confined to the main YAML — a bad actuator exposure list, say — would not
 * show up in this test.
 *
 * <p>The annotations deliberately match {@link com.johndoan.helpdesk.api.TicketControllerTest}
 * exactly, including {@code @AutoConfigureMockMvc} which this class has no direct use for.
 * Differing annotations give the two classes different context-cache keys, so Spring boots two
 * contexts against the same named in-memory database; under {@code ddl-auto: create-drop} the
 * second boot drops and recreates the first one's schema. It is harmless while this class
 * writes nothing, but it is a real flake waiting for the first test that does. Keeping the
 * annotations identical means one cached context and no such race.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HelpdeskTriageApplicationTests {

    @Test
    void contextLoads() {
    }
}
