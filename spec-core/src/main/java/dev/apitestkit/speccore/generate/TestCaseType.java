package dev.apitestkit.speccore.generate;

public enum TestCaseType {
    HAPPY_PATH(200),
    MISSING_REQUIRED(400),
    WRONG_TYPE(400),
    BOUNDARY_VALID(200),
    BOUNDARY_INVALID(400),
    INVALID_ENUM(400),
    INVALID_FORMAT(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404);

    private final int defaultExpectedStatusCode;

    TestCaseType(int defaultExpectedStatusCode) {
        this.defaultExpectedStatusCode = defaultExpectedStatusCode;
    }

    public int defaultExpectedStatusCode() {
        return defaultExpectedStatusCode;
    }
}
