package dev.apitestkit.speccore.config;

import dev.apitestkit.speccore.util.OrderedMaps;

import java.util.Map;

/**
 * De instellingen uit test-config.yaml voor precies één operation (herkenbaar aan de operationId).
 * Dit is de "overrule-knop": zonder dit bestand raadt het framework alles zelf; hiermee vertel je
 * het wat het écht moet doen.
 *
 * <p><b>Let op:</b> {@code fixedTestData} komt letterlijk terecht in testfoutmeldingen én in
 * gegenereerde, te committen Java-bestanden (zie codegen). Zet hier dus nooit een echt wachtwoord,
 * token of ander gevoelig gegeven in -- alleen onschuldige testwaarden (bijvoorbeeld een voorbeeldnaam).
 *
 * @param skip                     sla deze operation helemaal over
 * @param expectedStatusOverrides  per casetype (bijv. "HAPPY_PATH") een andere verwachte statuscode
 * @param fixedTestData            per veldnaam een vaste waarde in plaats van een gegenereerde waarde
 * @param setupDependsOn           operationId die eerst uitgevoerd moet worden om een bestaand id te krijgen
 */
public record OperationOverride(
        boolean skip,
        Map<String, Integer> expectedStatusOverrides,
        Map<String, Object> fixedTestData,
        String setupDependsOn
) {
    public OperationOverride {
        expectedStatusOverrides = OrderedMaps.copyOf(expectedStatusOverrides);
        fixedTestData = OrderedMaps.copyOf(fixedTestData);
    }

    public static OperationOverride empty() {
        return new OperationOverride(false, Map.of(), Map.of(), null);
    }
}
