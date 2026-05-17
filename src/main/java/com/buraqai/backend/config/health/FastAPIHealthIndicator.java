package com.buraqai.backend.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;

@Component
public class FastAPIHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;

    public FastAPIHealthIndicator() {
        // Create a RestTemplate with a 3-second timeout
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 3 seconds to establish connection
        factory.setReadTimeout(3000);     // 3 seconds to receive response
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public Health health() {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:8000/health",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    Map.class
            );

            Map<String, Object> body = response.getBody();

            if (body != null && "UP".equals(body.get("status"))) {
                return Health.up()
                        .withDetail("service", "FastAPI")
                        .withDetail("url", "http://localhost:8000/health")
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "FastAPI")
                        .withDetail("url", "http://localhost:8000/health")
                        .withDetail("error", "Unexpected response: " + body)
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "FastAPI")
                    .withDetail("url", "http://localhost:8000/health")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}