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
public class OllamaHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;

    public OllamaHealthIndicator() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public Health health() {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:11434/api/tags",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    Map.class
            );

            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("models")) {
                return Health.up()
                        .withDetail("service", "Ollama")
                        .withDetail("url", "http://localhost:11434")
                        .withDetail("models", body.get("models"))
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "Ollama")
                        .withDetail("url", "http://localhost:11434")
                        .withDetail("error", "Unexpected response format")
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "Ollama")
                    .withDetail("url", "http://localhost:11434")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}