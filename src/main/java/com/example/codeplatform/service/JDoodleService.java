package com.example.codeplatform.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Thin client for the JDoodle remote code-execution API. */
@Service
public class JDoodleService {

    private static final String URL = "https://api.jdoodle.com/v1/execute";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;

    public JDoodleService(RestTemplateBuilder builder,
                          @Value("${jdoodle.clientId:}") String clientId,
                          @Value("${jdoodle.clientSecret:}") String clientSecret) {
        // Without timeouts a hung upstream call ties up a request thread indefinitely.
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String code, String input, String language, String versionIndex) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Code execution is not configured: set JDOODLE_CLIENT_ID and JDOODLE_CLIENT_SECRET");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("clientId", clientId);
        request.put("clientSecret", clientSecret);
        request.put("script", code);
        request.put("language", language);
        request.put("versionIndex", versionIndex);
        request.put("stdin", input);

        try {
            Map<String, Object> response = restTemplate.postForObject(URL, request, Map.class);
            return response == null ? Map.of() : response;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Code execution service is unavailable", e);
        }
    }
}
