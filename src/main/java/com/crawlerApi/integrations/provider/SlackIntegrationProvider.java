package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Slack integration driven by OAuth flow. End users connect via Slack auth screen,
 * so manual token/webhook entry is not required.
 */
@Component
public class SlackIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("accessToken");

    public SlackIntegrationProvider() {
        super("slack", "Slack", "Messaging", CONFIG_KEYS);
    }

    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return false;
        }
        Object token = config.get("accessToken");
        return token instanceof String && !((String) token).isBlank();
    }
}
