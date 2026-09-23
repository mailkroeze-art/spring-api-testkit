package dev.apitestkit.speccore;

public class SpecLoadException extends RuntimeException {

    public SpecLoadException(String message) {
        super(message);
    }

    public SpecLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
