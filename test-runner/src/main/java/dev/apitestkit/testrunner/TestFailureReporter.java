package dev.apitestkit.testrunner;

import dev.apitestkit.speccore.generate.TestCase;
import io.restassured.response.Response;

/**
 * Bouwt foutmeldingen die altijd tonen: endpoint + methode, operationId, casetype,
 * de verstuurde request en de verwachte versus werkelijke uitkomst.
 *
 * <p><b>Beveiliging:</b> deze klasse toont bewust alleen pathParams, queryParams en de body --
 * nooit de HTTP-headers. Zo lekt een Authorization- of API-sleutel-header nooit mee in een
 * testfoutmelding of in build-/CI-logs, ook niet als de authenticatie zelf de oorzaak van de
 * mislukte test is (zie {@link AuthResolver}).
 */
public final class TestFailureReporter {

    private TestFailureReporter() {
    }

    public static String statusMismatch(TestCase testCase, Response response) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, testCase);
        sb.append("Reden: statuscode komt niet overeen met de verwachting").append(System.lineSeparator());
        sb.append("Verwachte statuscode: ").append(testCase.expectedStatusCode()).append(System.lineSeparator());
        sb.append("Werkelijke statuscode: ").append(response.statusCode()).append(System.lineSeparator());
        sb.append("Werkelijke response body: ").append(safeBody(response)).append(System.lineSeparator());
        return sb.toString();
    }

    public static String validationFailure(TestCase testCase, String originalMessage) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, testCase);
        sb.append("Reden: response voldoet niet aan de OpenAPI-spec (ongedocumenteerde statuscode of ongeldig schema)")
                .append(System.lineSeparator());
        sb.append("Verwachte statuscode: ").append(testCase.expectedStatusCode()).append(System.lineSeparator());
        sb.append("Validatordetails: ").append(originalMessage).append(System.lineSeparator());
        return sb.toString();
    }

    private static void appendHeader(StringBuilder sb, TestCase testCase) {
        sb.append("Endpoint: ").append(testCase.httpMethod()).append(' ').append(testCase.path()).append(System.lineSeparator());
        sb.append("OperationId: ").append(testCase.operationId()).append(System.lineSeparator());
        sb.append("Casetype: ").append(testCase.type()).append(System.lineSeparator());
        sb.append("Omschrijving: ").append(testCase.description()).append(System.lineSeparator());
        sb.append("Verstuurde pathParams: ").append(testCase.pathParams()).append(System.lineSeparator());
        sb.append("Verstuurde queryParams: ").append(testCase.queryParams()).append(System.lineSeparator());
        sb.append("Verstuurde body: ").append(testCase.requestBody()).append(System.lineSeparator());
    }

    private static String safeBody(Response response) {
        try {
            return response.getBody().asString();
        } catch (RuntimeException e) {
            return "(kon response body niet lezen: " + e.getMessage() + ")";
        }
    }
}
