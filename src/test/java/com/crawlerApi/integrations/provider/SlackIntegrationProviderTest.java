package com.crawlerApi.integrations.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SlackIntegrationProviderTest {

    private final SlackIntegrationProvider provider = new SlackIntegrationProvider();

    @Test
    void metadataRepresentsOAuthBasedIntegration() {
        assertEquals("slack", provider.getType());
        assertEquals("Slack", provider.getMetadata().getName());
        assertEquals("Messaging", provider.getMetadata().getCategory());
        assertTrue(provider.getMetadata().getConfigSchema().contains("accessToken"));
    }

    @Test
    void validateConfigRequiresAccessToken() {
        assertTrue(provider.validateConfig(Map.of("accessToken", "xoxb-token")));
        assertFalse(provider.validateConfig(Map.of("accessToken", "")));
        assertFalse(provider.validateConfig(Map.of("channel", "general")));
        assertFalse(provider.validateConfig(null));
    }
}
