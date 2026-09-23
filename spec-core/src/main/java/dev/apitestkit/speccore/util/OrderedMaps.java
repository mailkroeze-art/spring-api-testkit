package dev.apitestkit.speccore.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map.copyOf() geeft bewust een ongespecificeerde volgorde die per JVM-opstart kan wisselen
 * (bedoeld tegen hash-flooding). Voor reproduceerbare foutmeldingen en gegenereerde broncode
 * behoudt deze helper altijd de invoervolgorde.
 */
public final class OrderedMaps {

    private OrderedMaps() {
    }

    public static <K, V> Map<K, V> copyOf(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
