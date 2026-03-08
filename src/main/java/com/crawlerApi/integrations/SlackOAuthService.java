package com.crawlerApi.integrations;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles Slack OAuth connect flow so end-users only click-to-connect without manually entering tokens/webhooks.
 */
@Service
public class SlackOAuthService {

    private static final Logger log = LoggerFactory.getLogger(SlackOAuthService.class);
    private static final String AUTHORIZE_URL = "https://slack.com/oauth/v2/authorize";
    private static final String ACCESS_URL = "https://slack.com/api/oauth.v2.access";

    private final ObjectMapper objectMapper;

    private final Map<String, Long> pendingStates = new ConcurrentHashMap<>();

    @Value("${integrations.slack.client-id:}")
    private String clientId;

    @Value("${integrations.slack.client-secret:}")
    private String clientSecret;

    @Value("${integrations.slack.redirect-uri:}")
    private String redirectUri;

    @Value("${integrations.slack.scopes:chat:write,channels:read,groups:read}")
    private String scopes;

    public SlackOAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public String buildAuthorizationUrl(Long accountId) {
        validateConfig();
        String state = generateState(accountId);

        return AUTHORIZE_URL
                + "?client_id=" + urlEncode(clientId)
                + "&scope=" + urlEncode(scopes)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&state=" + urlEncode(state);
    }

    public Map<String, Object> exchangeCodeForConfig(Long accountId, String code, String state) {
        validateConfig();

        if (!isValidState(accountId, state)) {
            throw new IllegalArgumentException("Invalid Slack OAuth state");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Missing Slack OAuth code");
        }

        try {
            String responseBody = performTokenExchange(code);
            JsonNode response = objectMapper.readTree(responseBody);
            if (!response.path("ok").asBoolean(false)) {
                throw new IllegalStateException("Slack OAuth exchange failed: " + response.path("error").asText("unknown_error"));
            }
            return buildStoredConfig(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Slack OAuth exchange request failed", e);
        } catch (IOException e) {
            throw new IllegalStateException("Slack OAuth exchange request failed", e);
        }
    }

    protected String performTokenExchange(String code) throws IOException, InterruptedException {
        String body = "client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&code=" + urlEncode(code)
                + "&redirect_uri=" + urlEncode(redirectUri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ACCESS_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Slack token exchange HTTP status: " + response.statusCode());
        }
        return response.body();
    }

    private void validateConfig() {
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(redirectUri)) {
            throw new IllegalStateException("Slack OAuth is not configured. Set integrations.slack.client-id, client-secret, and redirect-uri");
        }
    }

    private Map<String, Object> buildStoredConfig(JsonNode response) {
        Map<String, Object> config = new HashMap<>();

        String accessToken = response.path("access_token").asText("");
        if (accessToken.isBlank()) {
            throw new IllegalStateException("Slack OAuth exchange succeeded without access_token");
        }

        config.put("accessToken", accessToken);
        config.put("scope", response.path("scope").asText(""));

        JsonNode team = response.path("team");
        if (!team.isMissingNode()) {
            config.put("teamId", team.path("id").asText(""));
            config.put("teamName", team.path("name").asText(""));
        }

        JsonNode authedUser = response.path("authed_user");
        if (!authedUser.isMissingNode()) {
            config.put("authedUserId", authedUser.path("id").asText(""));
        }

        return config;
    }

    private String generateState(Long accountId) {
        String payload = accountId + ":" + Instant.now().toEpochMilli() + ":" + Math.random();
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        pendingStates.put(state, accountId);
        return state;
    }

    private boolean isValidState(Long accountId, String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        Long expected = pendingStates.remove(state);
        return Objects.equals(expected, accountId);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
