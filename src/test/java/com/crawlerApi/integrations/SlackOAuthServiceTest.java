package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

class SlackOAuthServiceTest {

    @Test
    void buildAuthorizationUrlIncludesStateAndExpectedParameters() {
        SlackOAuthService service = new SlackOAuthService(new ObjectMapper());
        configure(service);

        String url = service.buildAuthorizationUrl(42L);

        assertTrue(url.startsWith("https://slack.com/oauth/v2/authorize"));
        assertTrue(url.contains("client_id=client-id"));
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fv1%2Fintegrations%2Fslack%2Fcallback"));
        assertTrue(url.contains("scope=chat%3Awrite%2Cchannels%3Aread"));
        assertTrue(url.contains("state="));
    }

    @Test
    void exchangeCodeForConfigParsesSuccessfulSlackResponse() {
        StubSlackOAuthService service = new StubSlackOAuthService("""
            {
              "ok": true,
              "access_token": "xoxb-123",
              "scope": "chat:write,channels:read",
              "team": {"id": "T123", "name": "Workspace"},
              "authed_user": {"id": "U123"}
            }
            """);
        configure(service);

        String url = service.buildAuthorizationUrl(7L);
        String state = url.substring(url.indexOf("state=") + 6);

        Map<String, Object> config = service.exchangeCodeForConfig(7L, "oauth-code", state);

        assertEquals("xoxb-123", config.get("accessToken"));
        assertEquals("T123", config.get("teamId"));
        assertEquals("Workspace", config.get("teamName"));
        assertEquals("U123", config.get("authedUserId"));
    }

    @Test
    void exchangeCodeForConfigRejectsInvalidState() {
        StubSlackOAuthService service = new StubSlackOAuthService("{\"ok\":true,\"access_token\":\"xoxb\"}");
        configure(service);

        service.buildAuthorizationUrl(11L);

        assertThrows(IllegalArgumentException.class, () -> service.exchangeCodeForConfig(11L, "code", "bad-state"));
    }

    @Test
    void buildAuthorizationUrlFailsWhenOauthIsNotConfigured() {
        SlackOAuthService service = new SlackOAuthService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "clientId", "");
        ReflectionTestUtils.setField(service, "clientSecret", "");
        ReflectionTestUtils.setField(service, "redirectUri", "");

        assertThrows(IllegalStateException.class, () -> service.buildAuthorizationUrl(1L));
    }

    private static void configure(SlackOAuthService service) {
        ReflectionTestUtils.setField(service, "clientId", "client-id");
        ReflectionTestUtils.setField(service, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(service, "redirectUri", "https://app.example.com/v1/integrations/slack/callback");
        ReflectionTestUtils.setField(service, "scopes", "chat:write,channels:read");
    }

    private static final class StubSlackOAuthService extends SlackOAuthService {

        private final String tokenResponse;

        private StubSlackOAuthService(String tokenResponse) {
            super(new ObjectMapper());
            this.tokenResponse = tokenResponse;
        }

        @Override
        protected String performTokenExchange(String code) {
            return tokenResponse;
        }
    }
}
