package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class IntegrationProviderFactoryTest {

    @Test
    void getProviderIsCaseInsensitiveAfterInit() {
        IntegrationProvider provider = new StubProvider("slack");
        IntegrationProviderFactory factory = new IntegrationProviderFactory(List.of(provider));
        factory.init();

        assertTrue(factory.getProvider("SLACK").isPresent());
        assertEquals("slack", factory.getProvider("sLaCk").orElseThrow().getType());
    }

    @Test
    void factoryReturnsEmptyWhenProviderDoesNotExistOrTypeIsNull() {
        IntegrationProviderFactory factory = new IntegrationProviderFactory(Collections.emptyList());
        factory.init();

        assertTrue(factory.getProvider("jira").isEmpty());
        assertTrue(factory.getProvider(null).isEmpty());
    }

    @Test
    void factoryReturnsAllMetadataFromProviders() {
        IntegrationProviderFactory factory = new IntegrationProviderFactory(Arrays.asList(
            new StubProvider("jira"),
            new StubProvider("github")
        ));
        factory.init();

        List<IntegrationMetadata> metadataList = factory.getAllMetadata();
        assertEquals(2, metadataList.size());
        assertTrue(metadataList.stream().anyMatch(m -> m.getId().equals("jira")));
        assertTrue(metadataList.stream().anyMatch(m -> m.getId().equals("github")));
    }

    @Test
    void duplicateProviderTypesKeepFirstRegistration() {
        IntegrationProvider first = new StubProvider("gitlab", "Gitlab A");
        IntegrationProvider second = new StubProvider("gitlab", "Gitlab B");
        IntegrationProviderFactory factory = new IntegrationProviderFactory(List.of(first, second));
        factory.init();

        assertEquals("Gitlab A", factory.getProvider("gitlab").orElseThrow().getMetadata().getName());
    }

    private static final class StubProvider implements IntegrationProvider {
        private final String type;
        private final String name;

        private StubProvider(String type) {
            this(type, type + " name");
        }

        private StubProvider(String type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public IntegrationMetadata getMetadata() {
            return new IntegrationMetadata(type, name, "category", List.of("token"));
        }

        @Override
        public boolean validateConfig(Map<String, Object> config) {
            return config != null && !config.isEmpty();
        }
    }
}
