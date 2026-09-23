package dev.apitestkit.speccore.config;

/**
 * Wordt gegooid als test-config.yaml iets bevat dat het framework niet begrijpt: een onbekende
 * operationId, een onbekende sleutel, of een verkeerd opgebouwd bestand. De boodschap bevat altijd
 * de bestandsnaam en het regelnummer, zodat je meteen weet waar je moet kijken.
 */
public class ConfigValidationException extends RuntimeException {

    public ConfigValidationException(String message) {
        super(message);
    }
}
