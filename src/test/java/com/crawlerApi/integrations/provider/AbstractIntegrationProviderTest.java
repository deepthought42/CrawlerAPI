package com.crawlerApi.integrations.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AbstractIntegrationProviderTest {

    private final TestProvider provider = new TestProvider();

    @Test
    void metadataContainsConfiguredValues() {
        assertEquals("sample", provider.getType());
        assertEquals("Sample", provider.getMetadata().getName());
        assertEquals("Messaging", provider.getMetadata().getCategory());
        assertEquals(List.of("token", "channel"), provider.getMetadata().getConfigSchema());
    }

    @Test
    void validateConfigReturnsTrueWhenAllRequiredFieldsExist() {
        assertTrue(provider.validateConfig(Map.of("token", "abc", "channel", "alerts")));
    }

    @Test
    void validateConfigReturnsFalseForNullMap() {
        assertFalse(provider.validateConfig(null));
    }

    @Test
    void validateConfigReturnsFalseWhenRequiredFieldMissingOrBlank() {
        assertFalse(provider.validateConfig(Map.of("token", "abc")));
        assertFalse(provider.validateConfig(Map.of("token", "", "channel", "alerts")));
        assertFalse(provider.validateConfig(Map.of("token", "abc", "channel", "   ")));
    }

    private static class TestProvider extends AbstractIntegrationProvider {
        private TestProvider() {
            super("sample", "Sample", "Messaging", List.of("token", "channel"));
        }
    }
}
