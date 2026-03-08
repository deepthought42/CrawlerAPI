package com.crawlerApi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.crawlerApi.integrations.IntegrationMetadata;
import com.crawlerApi.integrations.IntegrationWithConfigResponse;

class IntegrationModelsTest {

    @Test
    void integrationMetadataExposesConstructorFields() {
        IntegrationMetadata metadata = new IntegrationMetadata("jira", "Jira", "Product", List.of("token"));

        assertEquals("jira", metadata.getId());
        assertEquals("Jira", metadata.getName());
        assertEquals("Product", metadata.getCategory());
        assertEquals(List.of("token"), metadata.getConfigSchema());
    }

    @Test
    void integrationWithConfigResponseExposesMetadataAndConfig() {
        IntegrationMetadata metadata = new IntegrationMetadata("slack", "Slack", "Messaging", List.of("webhookUrl"));
        Map<String, Object> config = Map.of("webhookUrl", "https://example");

        IntegrationWithConfigResponse response = new IntegrationWithConfigResponse(metadata, config);

        assertEquals(metadata, response.getMetadata());
        assertEquals(config, response.getConfig());
    }

    @Test
    void integrationConfigGettersAndSettersWork() {
        IntegrationConfig config = new IntegrationConfig();
        LocalDateTime now = LocalDateTime.now();

        config.setId(99L);
        config.setAccountId(7L);
        config.setIntegrationType("jira");
        config.setConfig("{\"token\":\"x\"}");
        config.setEnabled(false);
        config.setCreatedAt(now);
        config.setUpdatedAt(now.plusMinutes(1));

        assertEquals(99L, config.getId());
        assertEquals(7L, config.getAccountId());
        assertEquals("jira", config.getIntegrationType());
        assertEquals("{\"token\":\"x\"}", config.getConfig());
        assertFalse(config.isEnabled());
        assertEquals(now, config.getCreatedAt());
        assertEquals(now.plusMinutes(1), config.getUpdatedAt());
    }

    @Test
    void integrationConfigConstructorSetsExpectedDefaults() {
        IntegrationConfig config = new IntegrationConfig(12L, "github", "encrypted", true);

        assertEquals(12L, config.getAccountId());
        assertEquals("github", config.getIntegrationType());
        assertEquals("encrypted", config.getConfig());
        assertTrue(config.isEnabled());
        assertTrue(config.getCreatedAt() != null);
        assertTrue(config.getUpdatedAt() != null);
    }
}
