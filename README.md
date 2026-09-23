# spring-api-testkit

Herbruikbaar Java-testautomatiseringsframework dat een OpenAPI 3.x YAML-specificatie inleest en
daar automatisch JUnit 5-tests uit genereert voor een Spring Boot REST API.

Zoek je een uitleg zonder testautomatiseringsjargon? Lees [docs/UITLEG.md](docs/UITLEG.md).

## Doel

Je onderhoudt een OpenAPI-spec toch al. Dit framework leest die spec en genereert daaruit
testcases die controleren of je API zich houdt aan wat de spec belooft: geldige aanvragen,
ontbrekende verplichte velden, verkeerde datatypes, grenswaarden, ongeldige enums/formaten,
authenticatie, en niet-bestaande resources. Elke response wordt bovendien gevalideerd tegen het
gedocumenteerde schema, zodat een test ook faalt als de API een ongedocumenteerde statuscode of
een niet-schema-valide body teruggeeft.

## Architectuur

| Module | Verantwoordelijkheid |
|---|---|
| `spec-core` | OpenAPI-spec inlezen (`OpenApiSpecLoader`), `test-config.yaml` inlezen en valideren (`TestConfigLoader`), testwaarden genereren (`ExampleValueGenerator`) en testcases samenstellen (`TestCaseGenerator`) |
| `test-runner` | `ApiTestFactory` (JUnit 5 `@TestFactory`) genereert `DynamicTest`s tijdens runtime; `TestCaseExecutor` voert ze uit via REST Assured en valideert de response tegen de spec (`swagger-request-validator`); `AuthResolver` regelt authenticatie; `TestFailureReporter` bouwt duidelijke foutmeldingen |
| `codegen` | `CodegenMain` (Maven-profiel `-Pgenerate`) schrijft per OpenAPI-tag een bewerkbare, committable JUnit-testklasse naar `src/test/java/generated` |
| `examples` | Voorbeeldspec (`openapi.yaml`), voorbeeldconfig (`test-config.yaml`) en WireMock-gebaseerde end-to-end-tests die bewijzen dat het geheel werkt |

Alle modules zijn plain Java 21 + Maven, geen Spring-afhankelijkheid in het framework zelf --
`test-runner` wordt als test-dependency toegevoegd aan een willekeurig Spring Boot-project.

## Quickstart

Vereisten: Java 21, Maven (of gebruik de meegeleverde `mvnw`).

```bash
mvn clean verify
```

Zelf tests draaien tegen een spec via `@TestFactory`:

```bash
mvn -pl examples test -Dopenapi.spec=openapi.yaml -Dapi.baseUrl=http://localhost:8080
```

`test-config.yaml` wordt standaard naast de spec gezocht; override met `-Dtestconfig.path=...`.

Bewerkbare testklassen genereren (per OpenAPI-tag, naar `src/test/java/generated`):

```bash
cd examples
mvn -Pgenerate generate-test-sources -Dopenapi.spec=openapi.yaml
```

Zie [docs/UITLEG.md](docs/UITLEG.md) voor een volledig genummerd stappenplan, inclusief
IntelliJ-run-configuratie en het lezen van een rode testfout.

## Config-referentie: test-config.yaml

| Sleutel | Betekenis |
|---|---|
| `baseUrl` | Basis-URL van de API. Overschrijfbaar via env var `API_BASE_URL` of `-Dapi.baseUrl` |
| `auth.type` | `bearer`, `basic` of `apiKey` |
| `auth.tokenEnv` | Env var met het bearer-token (bij `type: bearer`) |
| `auth.usernameEnv` / `auth.passwordEnv` | Env vars met gebruikersnaam/wachtwoord (bij `type: basic`) |
| `auth.headerName` | Headernaam voor de sleutel (bij `type: apiKey`, default `X-API-Key`) |
| `auth.valueEnv` | Env var met de apiKey-waarde (bij `type: apiKey`) |
| `operations.<operationId>.skip` | Sla deze operatie volledig over |
| `operations.<operationId>.expectedStatusOverrides.<CASETYPE>` | Overschrijf de verwachte statuscode voor een specifiek casetype |
| `operations.<operationId>.fixedTestData.<veld>` | Vaste waarde in plaats van een gegenereerde waarde |
| `operations.<operationId>.setupDependsOn` | Voer eerst deze andere operatie uit en gebruik het `id`-veld uit de response als pad-parameter |
| `caseTypes.<CASETYPE>` | Zet een hele categorie testcases aan (`true`) of uit (`false`) |

Onbekende operationIds of sleutels geven bij het opstarten een foutmelding met bestandsnaam en
regelnummer -- zie [docs/UITLEG.md](docs/UITLEG.md#de-override-test-configyaml) voor voorbeelden.

## Casetypes

| Casetype | Wanneer gegenereerd | Standaard verwachte status |
|---|---|---|
| `HAPPY_PATH` | Altijd (als de operatie een 2xx-response documenteert) | eerste gedocumenteerde 2xx |
| `MISSING_REQUIRED` | Per verplicht body-veld of verplichte query-parameter, als 400 gedocumenteerd is | 400 |
| `WRONG_TYPE` | Per veld met een gedefinieerd type, als 400 gedocumenteerd is | 400 |
| `BOUNDARY_VALID` | Per veld met `minimum`/`maximum`/`minLength`/`maxLength` | eerste gedocumenteerde 2xx |
| `BOUNDARY_INVALID` | Idem, net buiten de grens, als 400 gedocumenteerd is | 400 |
| `INVALID_ENUM` | Per veld met een `enum`, als 400 gedocumenteerd is | 400 |
| `INVALID_FORMAT` | Per veld met `format: uuid/email/date-time/date`, als 400 gedocumenteerd is | 400 |
| `UNAUTHORIZED` | Als de operatie beveiligd is en 401 gedocumenteerd is | 401 |
| `FORBIDDEN` | Als de operatie beveiligd is en 403 gedocumenteerd is | 403 |
| `NOT_FOUND` | Als de operatie een pad-parameter heeft en 404 gedocumenteerd is | 404 |

Elke uitgevoerde testcase valideert bovendien -- ongeacht type -- dat de werkelijke statuscode
gedocumenteerd is in de spec en dat de response-body schema-valide is (via
`swagger-request-validator`).

## Integratie met een bestaand Spring Boot-project

Voeg `test-runner` (en transitief `spec-core`) toe als test-dependency, en gebruik
`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` met `@LocalServerPort`
om de daadwerkelijke poort als `-Dapi.baseUrl` door te geven, bijvoorbeeld via een
`DynamicPropertySource` of door de poort in een systeemproperty te zetten vóór het draaien van
`ApiTestFactory`. Zie [docs/UITLEG.md](docs/UITLEG.md) voor een concreet voorbeeld.

Let op: `ApiTestFactory` zelf heeft een klassenaam die niet matcht met de standaard
Surefire/Failsafe-includepatronen (`*Test`, `*Tests`, ...). Maak in je eigen project een lege
subklasse met een passende naam, bijvoorbeeld:

```java
class GeneratedApiTest extends ApiTestFactory {}
```

## Rapportage

Standaard: Surefire XML-rapporten onder `target/surefire-reports`. Optioneel: Allure
(`allure-junit5` staat al op het klassenpad) -- resultaten komen in `target/allure-results`;
bekijk ze met de losstaande Allure-CLI (`allure serve target/allure-results`).

## Beveiliging en databescherming

Dit framework draait volledig lokaal (of in jouw eigen CI) en stuurt zelf niets naar externe
diensten. Er zit geen telemetrie, geen "check for updates"-aanroep en geen analytics in. De enige
netwerkcall die het framework maakt, is de aanroep naar de `baseUrl` van **jouw eigen API** --
precies het adres dat jij in `test-config.yaml` of via `-Dapi.baseUrl` opgeeft.

**Jouw OpenAPI-spec en test-config.yaml blijven lokaal.** Ze worden alleen van schijf gelezen en
in het geheugen verwerkt; er wordt niets van de inhoud (endpoints, schema's, voorbeeldwaarden)
ergens naartoe verzonden, gelogd naar een extern systeem, of in telemetrie verpakt.

**Wat je zelf in de gaten moet houden:**

- **Geen secrets in `test-config.yaml` of in de spec.** `auth.tokenEnv` / `usernameEnv` /
  `passwordEnv` / `valueEnv` zijn bewust alleen *namen van omgevingsvariabelen* -- nooit de
  waarden zelf. Zet ook geen echte wachtwoorden, tokens of klantgegevens in `fixedTestData`: die
  waarden komen letterlijk terug in testfoutmeldingen (dus ook in CI-logs) én in gegenereerde,
  te committen Java-bestanden (zie `-Pgenerate`).
- **Voordat je een spec of `test-config.yaml` naar een (publieke) repo pusht:** controleer of er
  geen interne hostnamen, klantnamen of productiegegevens in staan. Gebruik voor een openbare repo
  altijd een voorbeeld-/dummyspec, zoals `examples/openapi.yaml` in dit project.
- **SSRF-bescherming:** OpenAPI-specs kunnen `$ref`-verwijzingen naar externe URL's bevatten.
  Zonder controle zou het inlezen van zo'n spec het framework kunnen laten verzoeken sturen naar
  een willekeurig (mogelijk intern) adres. `OpenApiSpecLoader` **weigert daarom elke spec met een
  `$ref` naar een externe URL** (http/https/ftp/ws), vóórdat er iets wordt opgehaald -- ongeacht of
  je de spec via `-Dopenapi.spec` inleest, tests genereert (`-Pgenerate`), of via `ApiTestFactory`
  draait. Lokale `$refs` (naar een ander bestand, of een fragment binnen dezelfde spec) blijven
  gewoon werken.
- **Geen headers in foutmeldingen.** `TestFailureReporter` toont bewust alleen pathParams,
  queryParams en de body van een mislukte aanvraag -- nooit de HTTP-headers. Een Authorization- of
  API-sleutel-header lekt dus nooit mee in een testrapport, ook niet als de authenticatie zelf de
  oorzaak van de mislukking is.
- **YAML-parsing is veilig ingericht.** `test-config.yaml` wordt gelezen met SnakeYAML's
  `compose()`, die alleen een boomstructuur van de tekst opbouwt en nooit automatisch Java-objecten
  instantieert op basis van tags in het bestand (dat laatste is de bekende SnakeYAML-kwetsbaarheid
  die dit framework dus niet raakt). SnakeYAML 2.x beschermt bovendien standaard tegen
  "billion laughs"-achtige YAML-bommen (buitensporige alias/anchor-expansie).
- **Draai `mvn org.owasp:dependency-check-maven:check`** (niet standaard meegeleverd) als je
  periodiek wilt controleren op nieuw ontdekte kwetsbaarheden in de gebruikte bibliotheken
  (swagger-parser, REST Assured, swagger-request-validator, ...).

## Beperkingen

- Path-parameters worden niet automatisch gefuzzed op verkeerd type/formaat/grenswaarde -- alleen
  `NOT_FOUND` gebruikt bewust een niet-bestaande waarde. Body- en query-parameter-velden wel.
  Zie [docs/UITLEG.md](docs/UITLEG.md#faq) voor de achtergrond.
- `setupDependsOn` verwacht dat de setup-operatie een JSON-response met een `id`-veld teruggeeft.
- Business-regels (bijvoorbeeld "een gebruiker mag maximaal 3 huisdieren aanmaken") worden niet
  automatisch getest -- daarvoor schrijf je zelf tests in de gegenereerde klassen, zie
  [docs/UITLEG.md](docs/UITLEG.md#wanneer-schrijf-ik-zelf-een-test).
- `allOf`-composities in schema's worden ondersteund door de spec-parser, maar sommige strikte
  OpenAPI-validators (waaronder `swagger-request-validator`) accepteren `allOf` met verspreide
  `properties` niet altijd probleemloos -- de meegeleverde voorbeeldspec gebruikt daarom platte
  schema's voor response-objecten.
- Alleen JSON-request-/response-bodies worden ondersteund (content-type `application/json`).

## Licentie

MIT, zie [LICENSE](LICENSE).
