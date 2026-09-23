package dev.apitestkit.speccore;

/**
 * Wordt gegooid als de OpenAPI-spec niet gevonden, niet gelezen of niet geparset kan worden --
 * bijvoorbeeld een verkeerd bestandspad, kapotte YAML, of een spec die om veiligheidsredenen wordt
 * geweigerd (zie {@link OpenApiSpecLoader}). De boodschap is altijd bedoeld om direct te tonen aan
 * degene die de tests draait.
 */
public class SpecLoadException extends RuntimeException {

    public SpecLoadException(String message) {
        super(message);
    }

    public SpecLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
