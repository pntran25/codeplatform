package com.example.codeplatform.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class JDoodleService {
    @Value("${jdoodle.clientId}")
    private String clientId;

    @Value("${jdoodle.clientSecret}")
    private String clientSecret;

    private final String url = "https://api.jdoodle.com/v1/execute";

    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String code, String input, String language) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> request = new HashMap<>();
        request.put("clientId", clientId);
        request.put("clientSecret", clientSecret);
        request.put("script", code);
        request.put("language", language);
        request.put("versionIndex", "4");
        request.put("stdin", input);

        return restTemplate.postForObject(url, request, Map.class);
    }
}