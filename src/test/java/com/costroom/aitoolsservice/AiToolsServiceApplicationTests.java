package com.costroom.aitoolsservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.com/.well-known/jwks.json",
    "app.encryption.key=T3kFLvZ3jd0cCHwo86oGr478LCZHpUTo4cX4byAiTkQ=",
    "app.ingestion.fixed-rate-ms=999999999"
})
class AiToolsServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context assembles without errors
    }
}
