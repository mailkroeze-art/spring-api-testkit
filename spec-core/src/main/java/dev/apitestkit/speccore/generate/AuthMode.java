package dev.apitestkit.speccore.generate;

/**
 * VALID: geldige credentials meesturen (uit env vars via auth-config).
 * NONE: geen credentials meesturen.
 * INVALID: bewust foutieve credentials meesturen.
 */
public enum AuthMode {
    VALID,
    NONE,
    INVALID
}
