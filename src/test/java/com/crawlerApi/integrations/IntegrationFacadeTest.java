package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.crawlerApi.models.IntegrationConfig;
import com.crawlerApi.models.repository.IntegrationConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class IntegrationFacadeTest {

    @Mock
    private IntegrationProviderFactory factory;

    @Mock
    private IntegrationConfigRepository repository;

    @Mock
    private IntegrationProvider provider;

    @Mock
    private IntegrationConfigEncryption encryption;

    @Mock
    private ObjectMapper objectMapper;

    private IntegrationFacade facade;

    @BeforeEach
    void setUp() {
        facade = new IntegrationFacade(factory, repository, objectMapper, encryption);
    }

    @Test
    void listAvailableIntegrationsDelegatesToFactory() {
        List<IntegrationMetadata> metadata = List.of(new IntegrationMetadata("slack", "Slack", "Messaging", List.of("token")));
        when(factory.getAllMetadata()).thenReturn(metadata);

        assertEquals(metadata, facade.listAvailableIntegrations());
    }

    @Test
    void putConfigReturnsFalseWhenProviderIsUnknown() {
        when(factory.getProvider("unknown")).thenReturn(Optional.empty());

        boolean saved = facade.putConfig(1L, "UNKNOWN", Map.of("token", "abc"));

        assertFalse(saved);
        verify(repository, never()).save(any());
    }

    @Test
    void putConfigReturnsFalseWhenValidationFails() {
        when(factory.getProvider("slack")).thenReturn(Optional.of(provider));
        when(provider.validateConfig(Map.of())).thenReturn(false);

        boolean saved = facade.putConfig(1L, "SLACK", Map.of());

        assertFalse(saved);
        verify(repository, never()).save(any());
    }

    @Test
    void putConfigCreatesNewEntityWhenNoPreviousConfigExists() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("token", "secret");

        when(factory.getProvider("slack")).thenReturn(Optional.of(provider));
        when(provider.validateConfig(config)).thenReturn(true);
        when(objectMapper.writeValueAsString(config)).thenReturn("{\"token\":\"secret\"}");
        when(encryption.encrypt("{\"token\":\"secret\"}")).thenReturn("encrypted");
        when(repository.findByAccountIdAndIntegrationType(7L, "slack")).thenReturn(Optional.empty());

        boolean saved = facade.putConfig(7L, "SLACK", config);

        assertTrue(saved);
        verify(repository).save(any(IntegrationConfig.class));
    }

    @Test
    void putConfigReturnsFalseWhenSerializationFails() throws Exception {
        Map<String, Object> config = Map.of("token", "secret");

        when(factory.getProvider("slack")).thenReturn(Optional.of(provider));
        when(provider.validateConfig(config)).thenReturn(true);
        when(objectMapper.writeValueAsString(config)).thenThrow(new JsonProcessingException("bad") {});

        assertFalse(facade.putConfig(1L, "slack", config));
        verify(repository, never()).save(any());
    }

    @Test
    void getConfigMaskedMasksSchemaValues() throws Exception {
        IntegrationConfig entity = new IntegrationConfig(10L, "slack", "encrypted", true);

        when(factory.getProvider("slack")).thenReturn(Optional.of(provider));
        when(provider.getMetadata()).thenReturn(new IntegrationMetadata("slack", "Slack", "Messaging", List.of("token")));
        when(repository.findByAccountIdAndIntegrationType(10L, "slack")).thenReturn(Optional.of(entity));
        when(encryption.decrypt("encrypted")).thenReturn("{\"token\":\"abcdef\",\"channel\":\"general\"}");
        when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .thenReturn(new HashMap<>(Map.of("token", "abcdef", "channel", "general")));

        Map<String, Object> masked = facade.getConfigMasked(10L, "SLACK").orElseThrow();

        assertEquals("***ef", masked.get("token"));
        assertEquals("general", masked.get("channel"));
    }

    @Test
    void testConnectionReturnsFalseWhenConfigDoesNotExist() {
        when(factory.getProvider("slack")).thenReturn(Optional.of(provider));
        when(repository.findByAccountIdAndIntegrationType(9L, "slack")).thenReturn(Optional.empty());

        assertFalse(facade.testConnection(9L, "slack"));
    }

    @Test
    void deleteConfigReturnsFalseForUnknownProvider() {
        when(factory.getProvider("unknown")).thenReturn(Optional.empty());

        assertFalse(facade.deleteConfig(1L, "UNKNOWN"));
        verify(repository, never()).deleteByAccountIdAndIntegrationType(any(), any());
    }

    @Test
    void deleteConfigDeletesWhenProviderExists() {
        when(factory.getProvider("slack")).thenReturn(Optional.of(provider));

        assertTrue(facade.deleteConfig(2L, "SLACK"));
        verify(repository).deleteByAccountIdAndIntegrationType(2L, "slack");
    }
}
