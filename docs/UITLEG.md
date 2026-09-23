# Uitleg voor iedereen (ook zonder testervaring)

Deze pagina legt uit hoe je met **spring-api-testkit** werkt. Je hoeft niets van
testautomatisering te weten. Korte zinnen. Elk moeilijk woord leggen we bij de eerste keer uit.

## 1. Wat doet dit framework?

Je hebt een bestand dat beschrijft hoe je API werkt. Dat bestand heet een **OpenAPI-spec**
("spec" is Engels voor specificatie: een beschrijving van de regels). Het staat in **YAML**
-- dat is gewoon een manier om tekst netjes in te delen met inspringen, zoals een boodschappenlijstje
met kopjes. Dit framework leest dat bestand. Daarna maakt het zelf tests. Die tests kijken of je
API doet wat in het bestand staat. Bijvoorbeeld: geeft de API echt een foutmelding als je een
verplicht veld vergeet? Je hoeft die tests niet zelf te schrijven -- je krijgt ze cadeau uit je
eigen spec.

## 2. Stap voor stap aan de slag

### Stap 1 -- de spec neerzetten

Zet je OpenAPI-bestand ergens in je project, bijvoorbeeld `openapi.yaml`. Een **endpoint** is één
adres van je API waarop je iets kunt doen, bijvoorbeeld `/pets` (huisdieren ophalen). Een
**statuscode** is het getal dat de API teruggeeft om te zeggen hoe het ging: `200` betekent "gelukt",
`400` betekent "jij stuurde iets fout", `404` betekent "niet gevonden". Een **schema** is de
beschrijving van hoe een stukje data eruit moet zien (welke velden, welk type, verplicht of niet).

In dit project vind je al een kant-en-klaar voorbeeld: [`examples/openapi.yaml`](../examples/openapi.yaml).

### Stap 2 -- test-config.yaml aanmaken

Maak naast je spec een bestand `test-config.yaml`. Dit bestand is optioneel, maar je hebt het al
snel nodig -- zie hoofdstuk 4. Een compleet voorbeeld staat al klaar in
[`examples/test-config.yaml`](../examples/test-config.yaml).

### Stap 3 -- tests draaien via mvn

Ga naar de map `examples` en voer uit:

```bash
mvn test -Dtest=ExamplesEndToEndTest
```

Wat je ziet op je scherm:

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Deze test start zelf een nep-serverjfe (WireMock, zie hoofdstuk 3 voor uitleg) om te laten zien
dat het hele plaatje werkt, zonder dat jij eerst een echte API hoeft te starten.

Wil je de tests draaien tegen je **eigen**, echt draaiende API? Gebruik dan:

```bash
mvn -pl examples test -Dopenapi.spec=openapi.yaml -Dapi.baseUrl=http://localhost:8080
```

`-Dopenapi.spec` is het pad naar je spec-bestand. `-Dapi.baseUrl` is het adres waar je API
draait. Je ziet dan een lijst van gegenereerde tests voorbijkomen, elk met een eigen naam zoals
`POST /pets [HAPPY_PATH] -- Geldige aanvraag conform spec`.

### Stap 4 -- tests draaien in IntelliJ

1. Open het project in IntelliJ.
2. Zoek het testbestand dat je wilt draaien, bijvoorbeeld
   `examples/src/test/java/dev/apitestkit/examples/ExamplesEndToEndTest.java`.
3. Klik op het groene driehoekje naast de klassenaam en kies **Run**.
   Voor deze test heb je geen extra instellingen nodig -- hij regelt zijn eigen nep-server.
4. Wil je in je **eigen** project tests draaien tegen je eigen spec via `ApiTestFactory`
   (de motor die dit framework aanbiedt, zie hoofdstuk 4 van [README.md](../README.md)),
   maak dan een eigen run-configuratie aan:
   - Menu **Run > Edit Configurations... > + > JUnit**.
   - **Class**: jouw testklasse (bijvoorbeeld `GeneratedApiTest`, die `ApiTestFactory` uitbreidt).
   - **VM options**: `-Dopenapi.spec=src/test/resources/openapi.yaml -Dapi.baseUrl=http://localhost:8080`
     (pas het pad aan naar waar jouw spec staat).
   - **Working directory**: de map van je module (zodat het relatieve pad bij `-Dopenapi.spec`
     klopt).
   - Klik **Apply** en dan **Run**.

### Stap 5 -- testklassen genereren met -Pgenerate

Wil je de gegenereerde tests aanpassen en aan je eigen businesslogica toevoegen? Genereer dan
bewerkbare Java-bestanden:

```bash
cd examples
mvn -Pgenerate generate-test-sources -Dopenapi.spec=openapi.yaml
```

Op je scherm verschijnt:

```
[codegen] 20 testcases gegenereerd naar /pad/naar/examples/src/test/java/generated
[INFO] BUILD SUCCESS
```

Er verschijnt een bestand per OpenAPI-**tag** (een label waarmee je endpoints groepeert, bijvoorbeeld
"Pets") in `src/test/java/generated`, bijvoorbeeld `PetsGeneratedTest.java`. Dit bestand mag je
committen (aan git toevoegen) en zelf uitbreiden -- zie hoofdstuk 6.

### Stap 6 -- rapport openen

Standaard schrijft Maven een rapport in `target/surefire-reports` (XML-bestanden, vooral handig
voor CI). Wil je een mooi overzicht in de browser? Dit project heeft `allure-junit5` al aan boord.
Na een testrun met resultaten in `target/allure-results` kun je (met de losstaande Allure-CLI
geïnstalleerd) een rapport openen met:

```bash
allure serve target/allure-results
```

## 3. Rood en groen

- **Groen** betekent: de test is geslaagd. Je API deed wat de spec beloofde.
- **Rood** betekent: de test is mislukt. Er klopt iets niet tussen je spec en je API.

Een voorbeeld van een echte, rode foutmelding uit dit framework (dit is precies wat je te zien
krijgt, woord voor woord, gegenereerd door `TestFailureReporter`):

```
Endpoint: POST /pets
OperationId: createPet
Casetype: UNAUTHORIZED
Omschrijving: Aanvraag zonder credentials
Verstuurde pathParams: {}
Verstuurde queryParams: {}
Verstuurde body: {name=teststring, status=available, email=gebruiker@voorbeeld.nl, createdAt=...}
Reden: statuscode komt niet overeen met de verwachting
Verwachte statuscode: 401
Werkelijke statuscode: 201
Werkelijke response body: {"id":"...","name":"teststring","status":"available"}
```

Zo lees je dit:

- **Endpoint** en **OperationId**: welk stukje van je API het probleem heeft (`POST /pets`,
  ofwel de operatie `createPet`).
- **Casetype**: welk soort test dit was. Hier `UNAUTHORIZED` -- de test stuurde een aanvraag
  *zonder* inloggegevens.
- **Verstuurde ...**: wat het framework precies naar je API heeft gestuurd.
- **Verwachte statuscode** versus **Werkelijke statuscode**: de test verwachtte `401`
  ("niet ingelogd, dus geweigerd"), maar de API antwoordde met `201` ("aangemaakt"). Dat betekent:
  je API accepteert een aanvraag zonder inloggegevens, terwijl je spec zegt dat dat niet mag.

Wat doe je dan? Je kijkt naar **Casetype** en **Endpoint**, gaat naar die plek in je
Spring Boot-code, en repareert de authenticatiecontrole (of, als de test verkeerd is -- bijvoorbeeld
omdat dit endpoint expres geen inloggegevens vereist -- pas je de spec of `test-config.yaml` aan).

## 4. De override: test-config.yaml

### Waarom bestaat dit bestand?

Het framework raadt soms verkeerd. Het weet bijvoorbeeld niet dat `getPetById` een huisdier nodig
heeft dat eerst is aangemaakt, of dat een veld altijd een vaste testwaarde moet krijgen. Hier
vertel je het framework wat het wél moet doen.

### Per instelling

- **`baseUrl`**: het adres waar je API draait tijdens het testen. Nodig zodra je niet met
  `-Dapi.baseUrl` werkt. Voorbeeld: `baseUrl: http://localhost:8080`.
- **`auth`**: hoe het framework moet inloggen bij beveiligde endpoints. Nodig zodra je spec
  een `securitySchemes`-blok heeft. De écht geheime waarde (token, wachtwoord, sleutel) zet je
  **nooit** in dit bestand, maar in een omgevingsvariabele (env var); je geeft hier alleen de
  *naam* van die omgevingsvariabele op.
- **`skip`**: sla een hele operatie over. Nodig als een endpoint nog niet af is, of expres niet
  automatisch getest moet worden.
- **`expectedStatusOverrides`**: verander de statuscode die een test verwacht. Nodig als jouw API
  bewust een andere (maar nog steeds gedocumenteerde) statuscode gebruikt dan het framework
  standaard aanneemt.
- **`fixedTestData`**: gebruik een vaste waarde in plaats van een automatisch gegenereerde waarde.
  Nodig als een veld een specifiek formaat moet hebben dat het framework niet kan raden (bijvoorbeeld
  een postcode of een bestaande productcode).
- **`setupDependsOn`**: voer eerst een andere operatie uit (bijvoorbeeld eerst iets aanmaken) en
  gebruik het `id`-veld uit die response. Nodig voor endpoints die een bestaand ID nodig hebben,
  zoals "haal huisdier met dit ID op".
- **`caseTypes`**: zet een hele *categorie* testcases (zie de tabel in [README.md](../README.md))
  aan of uit. Nodig als een categorie voor jouw API niet van toepassing is.

### Volledig voorbeeldbestand (met commentaar per regel)

Dit is precies het bestand dat in dit project gebruikt wordt -- [`examples/test-config.yaml`](../examples/test-config.yaml)
-- en dat we ook echt hebben gedraaid (zie hoofdstuk 7, "Controle"):

```yaml
# baseUrl: het adres waar de API draait tijdens het testen.
# Overschrijfbaar via de omgevingsvariabele API_BASE_URL of het argument -Dapi.baseUrl.
baseUrl: http://localhost:8080

# auth: hoe het framework zich moet aanmelden bij beveiligde endpoints.
auth:
  # type: bearer, basic of apiKey.
  type: apiKey
  # headerName: de headernaam waarin de apiKey wordt meegestuurd (alleen bij type apiKey).
  headerName: X-API-Key
  # valueEnv: naam van de omgevingsvariabele die de échte sleutel bevat -- nooit de sleutel zelf hier.
  valueEnv: PETS_API_KEY

# operations: per operationId instellingen die de automatische generatie overschrijven.
operations:
  createPet:
    # skip: sla deze operatie helemaal over als dit op true staat.
    skip: false
    # fixedTestData: gebruik deze vaste waarde in plaats van een automatisch gegenereerde waarde.
    fixedTestData:
      name: "Voorbeeldhuisdier"
  getPetById:
    # setupDependsOn: voer eerst deze operatie uit en gebruik het 'id'-veld uit die response
    # als pad-parameter, zodat er een écht bestaand huisdier wordt opgevraagd.
    setupDependsOn: createPet

# caseTypes: zet hele categorieën testcases aan (true) of uit (false).
caseTypes:
  # FORBIDDEN: deze voorbeeld-API documenteert geen 403-response voor createPet, dus deze
  # categorie levert toch al geen testcases op -- hier expliciet uitgezet als voorbeeld van de syntax.
  FORBIDDEN: false
```

Een voorbeeld met `expectedStatusOverrides`, dat niet in het meegeleverde bestand staat maar wel
geldige syntax is:

```yaml
operations:
  createPet:
    # expectedStatusOverrides: overschrijf de statuscode die één casetype verwacht.
    expectedStatusOverrides:
      HAPPY_PATH: 200   # deze API geeft bij het aanmaken bewust 200 terug in plaats van 201
```

### Veelgemaakte fouten

1. **Verkeerde operationId.** Je typt `creatPet` in plaats van `createPet`:

   ```yaml
   baseUrl: http://localhost:8080
   operations:
     creatPet:
       skip: true
   ```

   Foutmelding (echt getest, woord voor woord):

   ```
   ConfigValidationException: Onbekende operationId 'creatPet' in test-config.yaml -- deze komt
   niet voor in de OpenAPI-spec (test-config.yaml, regel 3)
   ```

   Herstel: controleer de exacte `operationId` in je `openapi.yaml`.

2. **Fout inspringen in YAML.** YAML gebruikt spaties (geen tabs!) om aan te geven wat bij wat
   hoort. Je bedoelt `fixedTestData` onder `createPet` te zetten, maar per ongeluk staat het
   op hetzelfde inspringniveau als `createPet` zelf:

   ```yaml
   baseUrl: http://localhost:8080
   operations:
     createPet:
       skip: false
     fixedTestData:
       name: "Rex"
   ```

   Omdat `fixedTestData` hier op hetzelfde niveau staat als `createPet`, denkt het framework dat
   `fixedTestData` een *tweede operationId* is. Foutmelding (echt getest, woord voor woord):

   ```
   ConfigValidationException: Onbekende operationId 'fixedTestData' in test-config.yaml -- deze
   komt niet voor in de OpenAPI-spec (test-config.yaml, regel 5)
   ```

   Herstel: tel de spaties na en zorg dat `fixedTestData` twee spaties verder inspringt, zodat het
   onder `createPet` valt.

3. **Wachtwoord in het bestand zetten in plaats van een env var.** Bijvoorbeeld
   `passwordEnv: geheim123` in plaats van de naam van een omgevingsvariabele. Dit geeft geen
   directe foutmelding, maar je test faalt met een authenticatiefout, omdat het framework
   `geheim123` als *naam van een omgevingsvariabele* opzoekt (die niet bestaat) in plaats van als
   wachtwoord te gebruiken. Herstel: zet het echte wachtwoord in een omgevingsvariabele
   (bijvoorbeeld `export API_PASSWORD=geheim123`) en verwijs er in `test-config.yaml` alleen naar:
   `passwordEnv: API_PASSWORD`.

## 5. TDD-werkwijze in 5 stappen

TDD staat voor Test-Driven Development: eerst een test, dan pas de code.

1. **Spec aanpassen.** Voeg een nieuw endpoint of veld toe aan `openapi.yaml`.
2. **Tests draaien.** `mvn -pl examples test -Dopenapi.spec=openapi.yaml -Dapi.baseUrl=...`
3. **Rood.** Je Spring Boot-API kent het nieuwe endpoint nog niet -- de nieuwe tests falen.
4. **Spring Boot-code schrijven.** Implementeer het endpoint of veld in je eigen API-code.
5. **Groen.** Draai de tests opnieuw. Als alles klopt met je spec, worden ze groen.

## 6. Wanneer schrijf ik zelf een test?

Businessregels worden **niet** automatisch getest. Het framework weet niets over jouw specifieke
regels, bijvoorbeeld "een klant mag maximaal 3 huisdieren tegelijk hebben". Daarvoor schrijf je
zelf een test.

Gebruik de gegenereerde klasse als startpunt (zie stap 5 hierboven). Open bijvoorbeeld
`examples/src/test/java/generated/PetsGeneratedTest.java`. Onderaan het bestand staat:

```java
    // <<< EINDE GEGENEREERD BLOK

    // Voeg hieronder je eigen tests toe -- deze blijven behouden bij opnieuw genereren.

}
```

Voeg je eigen test toe **onder** de regel `// <<< EINDE GEGENEREERD BLOK`, bijvoorbeeld:

```java
    // Voeg hieronder je eigen tests toe -- deze blijven behouden bij opnieuw genereren.

    @Test
    void klantMagNietMeerDanDrieHuisdierenHebben() {
        // jouw eigen testcode hier
    }
}
```

Alles **boven** die marker wordt bij een volgende `-Pgenerate` overschreven. Alles **onder** die
marker (tot en met de laatste `}`) blijft staan, ook als je opnieuw genereert.

## 7. FAQ

**Moet ik zelf tests schrijven, of doet het framework alles?**
Het framework genereert tests die controleren of je API zich aan de spec houdt (structuur,
statuscodes, verplichte velden). Businessregels test je zelf, zie hoofdstuk 6.

**Wat als mijn API geen authenticatie gebruikt?**
Prima -- laat het `auth`-blok in `test-config.yaml` gewoon weg. Endpoints zonder
`security`-vereiste in de spec krijgen dan geen `UNAUTHORIZED`/`FORBIDDEN`-tests.

**Waarom genereert het framework geen test voor een verkeerd type in een pad-parameter (zoals een
tekst in plaats van een getal in de URL)?**
Dat wordt bewust niet automatisch gedaan (zie "Beperkingen" in [README.md](../README.md)) --
alleen `NOT_FOUND` gebruikt bewust een niet-bestaand ID in het pad. Body- en query-velden worden
wel op verkeerd type, formaat en grenswaarden getest.

**Ik zie geen `WRONG_TYPE`- of `MISSING_REQUIRED`-tests voor mijn operatie, hoe kan dat?**
Die casetypes worden alleen gegenereerd als je spec voor die operatie ook echt een `400`-response
documenteert. Zonder gedocumenteerde `400` is er niets zinnigs om tegen te toetsen.

**Kan ik dit framework gebruiken zonder Spring Boot?**
Ja. Het framework praat via HTTP met je API en weet niets van Spring Boot specifiek. Voor een
Spring Boot-project is `@SpringBootTest(webEnvironment = RANDOM_PORT)` handig om de test tegen een
tijdelijke, willekeurige poort te laten draaien.

**Waar komen de testwaarden vandaan (bijvoorbeeld de tekst die in een naam-veld wordt gestopt)?**
Eerst kijkt het framework naar een `example`-waarde in je spec. Is die er niet, dan pakt het de
eerste `enum`-waarde. Is er geen `enum`, dan genereert het zelf een waarde die past bij het type en
de grenzen (`minLength`, `maximum`, etc.) uit je schema.

**Wat gebeurt er als ik `test-config.yaml` helemaal weglaat?**
Dan gebruikt het framework overal standaardwaarden: geen `baseUrl` (moet je dan via
`-Dapi.baseUrl` of de omgevingsvariabele `API_BASE_URL` meegeven), geen authenticatie, alle
casetypes aan, geen overrides.

**Mag ik de gegenereerde bestanden in `src/test/java/generated` aanpassen?**
Ja, dat is precies de bedoeling -- zie hoofdstuk 6. Alleen het stuk tussen de markers wordt
overschreven bij het opnieuw genereren.
