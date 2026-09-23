package dev.apitestkit.speccore.config;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.util.Map;

/**
 * type: "bearer" | "basic" | "apiKey". settings bevat alleen namen van env vars
 * (bijv. tokenEnv, usernameEnv, passwordEnv, headerName, valueEnv) -- nooit de waarden zelf.
 */
public record AuthConfig(
        String type,
        Map<String, String> settings
) {
    public AuthConfig {
        settings = OrderedMaps.copyOf(settings);
    }
}
