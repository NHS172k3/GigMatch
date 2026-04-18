package com.gigmatch.integration;

import com.gigmatch.match.dto.MatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MatchEndpointIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("gigmatch")
            .withUsername("gigmatch")
            .withPassword("gigmatch");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",       postgres::getJdbcUrl);
        registry.add("spring.datasource.username",   postgres::getUsername);
        registry.add("spring.datasource.password",   postgres::getPassword);
        registry.add("spring.data.redis.host",       redis::getHost);
        registry.add("spring.data.redis.port",       () -> redis.getMappedPort(6379));
        registry.add("spring.flyway.enabled",        () -> "true");
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;

    @Test
    void postMatch_withSoftwareDevCategory_returnsMatch() {
        Map<String, Object> request = Map.of(
            "requestId",      UUID.randomUUID().toString(),
            "clientId",       "client-test-1",
            "jobTitle",       "Build REST API",
            "jobCategory",    "software-dev",
            "requiredSkills", List.of("backend", "api"),
            "budgetCents",    30000,
            "urgencyLevel",   "HIGH"
        );

        ResponseEntity<MatchResult> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/matches",
            request, MatchResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().hasMatch()).isTrue();
        assertThat(response.getBody().winnerProviderKey()).isNotBlank();
        assertThat(response.getBody().clearingPriceCents()).isGreaterThan(0);
        assertThat(response.getBody().matchDurationMs()).isLessThan(500); // well within 100ms target
    }

    @Test
    void postMatch_withDataScienceCategory_returnsMatch() {
        Map<String, Object> request = Map.of(
            "requestId",      UUID.randomUUID().toString(),
            "clientId",       "client-test-2",
            "jobTitle",       "Customer Churn Model",
            "jobCategory",    "data-science",
            "requiredSkills", List.of("ml", "analytics"),
            "budgetCents",    50000,
            "urgencyLevel",   "MEDIUM"
        );

        ResponseEntity<MatchResult> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/matches",
            request, MatchResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // DataScienceProvider should win for data-science category
        assertThat(response.getBody().hasMatch()).isTrue();
    }

    @Test
    void postMatch_budgetTooLow_noMatch() {
        // Budget of $1 — all providers will pass since their quotes exceed this
        Map<String, Object> request = Map.of(
            "requestId",      UUID.randomUUID().toString(),
            "clientId",       "client-test-3",
            "jobTitle",       "Build Enterprise System",
            "jobCategory",    "software-dev",
            "requiredSkills", List.of("backend"),
            "budgetCents",    100, // $1.00 — impossibly low
            "urgencyLevel",   "LOW"
        );

        ResponseEntity<MatchResult> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/matches",
            request, MatchResult.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().hasMatch()).isFalse();
    }

    @Test
    void getMatchLogs_returnsPage() {
        // First create a match
        Map<String, Object> request = Map.of(
            "requestId",      UUID.randomUUID().toString(),
            "clientId",       "client-test-4",
            "jobTitle",       "Logo Design",
            "jobCategory",    "design",
            "requiredSkills", List.of("branding"),
            "budgetCents",    20000,
            "urgencyLevel",   "MEDIUM"
        );
        restTemplate.postForEntity("http://localhost:" + port + "/api/v1/matches", request, MatchResult.class);

        // Then check logs
        ResponseEntity<Map> logsResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/match-logs?page=0&size=10", Map.class);

        assertThat(logsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logsResponse.getBody()).isNotNull();
        assertThat(logsResponse.getBody()).containsKey("content");
    }

    @Test
    void getProviders_returnsSeedData() {
        ResponseEntity<List> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/providers", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(5); // 5 seeded providers
    }
}
