# Uitleg voor iedereen

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
voor CI, niet fijn leesbaar voor mensen).

Wil je een mooi overzicht in de browser, met kleurtjes, grafiekjes en een lijst van alle tests?
Dat heet een **Allure-rapport**, en dat zit al ingebouwd -- je hoeft niets te installeren. Ga naar
de map `examples` en draai:

```bash
mvn allure:report
```

Op je scherm verschijnt:

```
Report successfully generated to /pad/naar/examples/target/site/allure-maven-plugin
```

Open het bestand `examples/target/site/allure-maven-plugin/index.html` in je browser (dubbelklikken
werkt meestal, of sleep het bestand naar een browservenster). Je ziet dan een overzicht met hoeveel
tests slaagden, hoeveel er faalden, en je kunt op elke test klikken voor de details.

Liever meteen een rapport dat automatisch opent? Gebruik `mvn allure:serve` in plaats van
`mvn allure:report`.

**Onthoudt dit ook oude testrondes (trends over tijd)?** Niet automatisch. Een Allure-rapport
onthoudt standaard alleen de *laatste* keer dat je het genereerde. Wil je een grafiekje zien van
"hoeveel tests slaagden er vorige week, en hoeveel nu", dan moet je zelf één extra stap doen:
vóórdat je een nieuw rapport maakt, kopieer je de map met de geschiedenis van het vórige rapport
terug naar de testresultaten:

```bash
cp -r target/site/allure-maven-plugin/history target/allure-results/history
mvn allure:report
```

Doe je dat niet, dan begint elk rapport weer "op nul". Let ook op: het commando `mvn clean` (of
`mvn clean verify`) ruimt de map `target` helemaal leeg, inclusief een eerder gemaakt rapport --
kopieer de `history`-map dus éérst, en run daarna pas `clean`.

### Werkt op mijn (bedrijfs)netwerk geen Allure? Dat kan gewoon.

Sommige bedrijven laten je alleen bouwstenen (dependency's, zie hoofdstuk 10) gebruiken die eerst
zijn goedgekeurd en in een eigen, interne "winkel" (bijvoorbeeld Artifactory of Nexus) staan.
Allure staat daar soms niet in, en jij mag het misschien niet zelf toevoegen. Geen probleem: dit
framework werkt hier gewoon zonder.

Zet Allure uit met dit extra stukje achter je commando:

```bash
mvn clean verify -P!allure-reporting
```

(Werk je in IntelliJ? Zet `-P!allure-reporting` in het vakje "Command line" van je run-configuratie.)

Alles blijft werken -- je mist dan alleen het mooie Allure-overzicht. Wil je toch een overzichtelijk
rapport in de browser, zonder Allure? Draai dan dit (gebruikt alleen standaard Maven-onderdelen die
vrijwel altijd wél beschikbaar zijn):

```bash
mvn -pl examples surefire-report:report-only
```

Het rapport staat dan op `examples/target/reports/surefire.html` -- gewoon openen in je browser.

### Checklist: een geheel nieuwe, eigen spec testen (klik voor klik)

Dit is het complete stappenplan vanaf het moment dat je een eigen, nieuw OpenAPI-bestand hebt --
in IntelliJ, met precies waar je moet klikken. Geen Java-kennis nodig: je gebruikt een
kant-en-klare, lege testklasse (`AdHocApiTest`) die al in dit project zit.

**Wat heb je nodig, vóór je begint?**

1. Dit project (`spring-api-testkit`) open in IntelliJ.
2. Je eigen OpenAPI-bestand (een `.yaml`-bestand).
3. Een adres waar de bijbehorende API al draait (bijvoorbeeld `http://localhost:8080`, of een
   test-omgeving-URL). Zonder een draaiende API kun je nog niets testen -- dit framework verzint
   geen antwoorden, het stuurt écht verzoeken.

**Stap 1 -- zet je spec in het project**

- Klik in het linker Project-paneel met de rechtermuisknop op de map `examples`.
- Kies in het menu de optie om de map in de Verkenner te openen (op Windows heet dat meestal
  **Show in Explorer** of **Open in Explorer**, afhankelijk van je IntelliJ-versie).
- Sleep je `.yaml`-bestand daar naartoe, bijvoorbeeld met de naam `mijn-api.yaml`. Ga terug naar
  IntelliJ: het bestand verschijnt vanzelf in de mapstructuur (eventueel na een klik op het
  ververs-icoontje boven de Project-boom).
- Kun je die menuoptie niet vinden? Sleep het bestand dan rechtstreeks vanuit de Windows Verkenner
  naar de map `examples` in het Project-paneel van IntelliJ -- dat werkt net zo goed.

**Stap 2 -- (aanbevolen) maak je eigen test-config.yaml**

- Rechtermuisklik op `examples/test-config.yaml` in de Project-boom -> **Copy** (Ctrl+C), dan
  rechtermuisklik op de map `examples` -> **Paste** (Ctrl+V). Geef de kopie een naam, bijvoorbeeld
  `mijn-test-config.yaml`.
- Dubbelklik het bestand om te openen, en pas `baseUrl` (en eventueel `auth`) aan naar jouw
  situatie -- zie hoofdstuk 4 voor wat elke regel betekent.

**Stap 3 -- open de kant-en-klare testklasse**

- Ga in de Project-boom naar `examples/src/test/java/dev/apitestkit/examples/adhoc/AdHocApiTest.java`
  en dubbelklik om te openen. Dit bestand is expres leeg -- je hoeft er niets in te typen.

**Stap 4 -- maak de run-configuratie**

- Klik op het groene driehoekje in de marge naast `class AdHocApiTest` (of rechtermuisklik in de
  editor -> **Run 'AdHocApiTest'**). Dit maakt automatisch een run-configuratie aan, en laat meteen
  een (nog verkeerde) poging zien -- dat is prima, die gaan we zo aanvullen.
- Ga naar het menu **Run** (bovenin) -> **Edit Configurations...**.
- Selecteer `AdHocApiTest` in de lijst aan de linkerkant.
- Zoek het veld **VM options**. Zie je dat veld niet, klik dan op **Modify options** (rechtsboven in
  dat scherm) en vink **Add VM options** aan.
- Typ in het VM options-veld (pas de bestandsnamen en het adres aan naar wat jij hebt):
  ```
  -Dopenapi.spec=mijn-api.yaml -Dapi.baseUrl=http://localhost:8080 -Dtestconfig.path=mijn-test-config.yaml
  ```
  (Laat `-Dtestconfig.path=...` weg als je stap 2 hebt overgeslagen.)
- Controleer het veld **Working directory**: dat moet de map `examples` zijn (dus
  `.../spring-api-testkit/examples`), anders vindt IntelliJ je spec-bestand niet. Klik op het
  mapicoontje aan het eind van het veld om te wijzigen indien nodig.
- Klik **Apply**, dan **OK**.

**Stap 5 -- draaien**

- Klik bovenin op het groene driehoekje (Run) naast de naam "AdHocApiTest" in de werkbalk.
- Onderin verschijnt het testpaneel: je ziet een lijst met testnamen, elk met een groen vinkje
  (geslaagd) of rood kruisje (mislukt).

**Stap 6 -- een rode test lezen**

- Klik op een testnaam met een rood kruisje.
- Rechts (of onderin, afhankelijk van je indeling) verschijnt de volledige foutmelding: endpoint,
  operationId, casetype, wat er verstuurd is, en verwacht-versus-werkelijk. Zie hoofdstuk 3 voor
  hoe je dit leest.

**Gebruik je Allure niet (bijvoorbeeld op een afgeschermd bedrijfsnetwerk)?** Als het project al
succesvol is geopend/geïmporteerd in IntelliJ (dus als `mvn verify` via de terminal al werkte, al
dan niet met `-P!allure-reporting`), heeft deze checklist daar verder niets extra's voor nodig --
het groene driehoekje gebruikt gewoon de dependency's die IntelliJ al heeft ingeladen. Zie de
sectie hierboven ("Werkt op mijn (bedrijfs)netwerk geen Allure?") als je nog niet zover was.

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

## 8. Alle onderdelen van dit framework, in gewone taal

Je hoeft geen code te kunnen lezen om te snappen wat er onder de motorkap gebeurt. Hieronder staat
elk onderdeel, alsof het een medewerker in een fabriek is die zijn eigen taakje doet.

### De map `spec-core` -- "leest de spec en bedenkt de tests"

- **OpenApiSpecLoader** -- de eerste die de spec in handen krijgt. Leest het YAML-bestand, checkt
  of het geen gevaarlijke externe verwijzingen bevat (zie hoofdstuk 9), en zet alles om in een
  overzichtelijke, interne beschrijving die de rest van het framework begrijpt.
- **TestConfigLoader** -- leest jouw `test-config.yaml` en controleert hem meteen: klopt elke
  operationId, ken ik elke sleutel? Zo niet, dan krijg je direct een duidelijke foutmelding met
  bestandsnaam en regelnummer (zie hoofdstuk 4).
- **ExampleValueGenerator** -- de "verzinner". Bedenkt voorbeeldwaarden voor elk veld: een geldige
  naam, een geldig e-mailadres, een getal precies op de grens, of juist een bewust foute waarde.
- **TestCaseGenerator** -- de "planner". Bepaalt, op basis van je spec én je `test-config.yaml`,
  wélke tests er precies gemaakt moeten worden, en roept daarvoor de verzinner (hierboven) aan.
- De overige bestanden in deze map (`OperationModel`, `SchemaModel`, `TestCase`, ...) zijn simpele
  "invulformulieren" die de tussenresultaten netjes vasthouden, bijvoorbeeld "dit is één endpoint"
  of "dit is één testcase". Ze doen zelf niets bijzonders, ze bewaren alleen gegevens.

### De map `test-runner` -- "voert de tests echt uit"

- **ApiTestFactory** -- de "starter". Dit is het onderdeel dat JUnit aanroept: "geef me de spec en
  de config, en ik genereer en draai de tests".
- **TestCaseExecutor** -- de "uitvoerder". Stuurt het daadwerkelijke HTTP-verzoek naar je API, en
  controleert of het antwoord klopt: zowel de statuscode als de vorm van de data.
- **AuthResolver** -- de "inlogger". Zet de juiste inloggegevens op een verzoek (of juist expres
  foute, om te testen of je API dat weigert).
- **TestFailureReporter** -- de "verslaggever". Maakt van een mislukte test een duidelijk verhaal:
  wat werd er verstuurd, wat werd er verwacht, wat kwam er terug (zie hoofdstuk 3).

### De map `codegen` -- "schrijft de tests op papier"

- **CodegenMain** -- het startpunt van het commando `-Pgenerate` (zie stap 5 in hoofdstuk 2).
- **GeneratedTestClassWriter** -- schrijft een leesbaar Java-bestand per groep endpoints, en zorgt
  ervoor dat jouw eigen toevoegingen daaraan bewaard blijven (zie hoofdstuk 6).
- **JavaLiteralRenderer** -- een klein hulpje dat waarden (tekst, getallen, lijstjes) omzet naar
  geldige Java-code, zodat het gegenereerde bestand meteen werkt.

### De map `examples` -- "een kant-en-klaar voorbeeld"

- **openapi.yaml** -- een verzonnen, veilige voorbeeldspec (een "dierenwinkel"-API) zonder enige
  echte of gevoelige data.
- **test-config.yaml** -- een volledig ingevuld voorbeeld van de override, met uitleg per regel
  (zie hoofdstuk 4).
- **ExamplesEndToEndTest** -- een test die laat zien dat het hele plaatje werkt, met een nep-server
  erbij zodat je niets zelf hoeft op te starten.
- **generated/PetsGeneratedTest.java** -- het resultaat van `-Pgenerate` op de voorbeeldspec, als
  voorbeeld van wat jij ook in jouw eigen project krijgt.

### Overige bestanden

- **README.md** -- de technische samenvatting (voor ontwikkelaars).
- **docs/UITLEG.md** -- dit bestand.
- **`.github/workflows/verify.yml`** -- laat GitHub automatisch `mvn verify` draaien bij elke
  wijziging, zodat fouten meteen opvallen.
- **LICENSE** -- de MIT-licentie: iedereen mag dit framework gratis gebruiken, aanpassen en
  verspreiden.

## 9. Veiligheid: wat gebeurt er met mijn gegevens?

Een logische zorg als je hier je eigen (bedrijfs-)API-spec in stopt: gaat er iets van die
informatie naar buiten? Kort antwoord: **nee.**

- Dit framework draait volledig op jouw eigen computer of in jouw eigen CI. Het stuurt niets naar
  ons, naar de makers van de gebruikte bibliotheken, of naar wie dan ook, behalve naar het adres
  van **jouw eigen API** dat jij zelf opgeeft (`baseUrl`).
- Je spec-bestand en `test-config.yaml` worden alleen van schijf gelezen. Er zit geen "checken op
  updates", geen analytics en geen enkele andere achtergrondverbinding in.
- OpenAPI-specs kunnen technisch gezien verwijzen (`$ref`) naar bestanden op het internet. Dat zou
  betekenen dat het inlezen van een spec ongemerkt een verzoek naar een extern (of intern) adres
  stuurt. Dit framework **weigert dat automatisch**: een spec met een `$ref` naar `http://` of
  `https://` wordt meteen afgewezen, met een duidelijke foutmelding, nog vóórdat er iets wordt
  opgehaald. Verwijzingen binnen je eigen spec, of naar een ander lokaal bestand, blijven gewoon
  werken.
- Een mislukte test laat je precies zien wat er verstuurd is (zie hoofdstuk 3) -- maar bewust
  **nooit** de inloggegevens (headers zoals een API-sleutel of een Authorization-token) die daarbij
  gebruikt zijn. Zo kun je een testrapport gerust delen met een collega zonder dat er een sleutel in
  meegaat.

**Wat blijft jouw eigen verantwoordelijkheid:**

- Zet nooit een échte wachtwoord- of tokenwaarde in `test-config.yaml` -- alleen de *naam* van een
  omgevingsvariabele (zie hoofdstuk 4). Diezelfde regel geldt voor `fixedTestData`: die waarden
  belanden letterlijk in foutmeldingen én in gegenereerde, te committen Java-bestanden.
- Als je dit framework in een **publieke** repository zet: gebruik dan alleen een voorbeeld- of
  dummyspec (zoals `examples/openapi.yaml`), nooit je echte, interne API-beschrijving met
  bedrijfsgevoelige endpoints, klantgegevens of interne adressen.
- Controleer vóór een eerste push altijd zelf even of er geen wachtwoorden, tokens of interne
  URL's in je bestanden zijn geslopen (bijvoorbeeld met `git grep`).

## 10. Gebruiken in een bestaand Spring Boot-project

Dit hoofdstuk is voor als je dit framework wilt toevoegen aan een Spring Boot-project dat er al is
-- met een eigen `pom.xml`, eigen code, eigen team. Ook zonder testervaring moet je hierna begrijpen
wát er moet gebeuren, ook al plak je de code misschien niet zelf in het project (zie het kadertje
onderaan).

### Wat is een "dependency", en wat is een pom.xml?

Een Java-project heeft meestal een bestand met de naam `pom.xml`. Dat is een soort
ingrediëntenlijst: één regel per bouwsteen die het project gebruikt. Zo'n bouwsteen heet een
**dependency** (Engels voor "afhankelijkheid"): kant-en-klare code die iemand anders al gemaakt
heeft, en die jij gewoon mag gebruiken zonder hem zelf te schrijven -- vergelijkbaar met een
voorgebakken pizzabodem die je koopt in plaats van zelf deeg te maken. `spring-api-testkit` is zo'n
bouwsteen: door hem aan de ingrediëntenlijst toe te voegen, krijgt het project er "kan OpenAPI-tests
genereren" als vaardigheid bij.

### Stap 1: de bouwsteen toevoegen aan pom.xml

Dit framework staat (nog) niet op de standaard, wereldwijde ingrediëntenwinkel (Maven Central). Er
zijn twee manieren om hem toch te gebruiken.

**Optie A -- JitPack (aanbevolen, werkt voor iedereen).** JitPack is een gratis dienst die een
openbare GitHub-repository (zoals deze) automatisch klaarzet als bouwsteen, zonder dat iemand iets
hoeft te installeren. Dit is getest en werkt. Voeg dit toe aan de `pom.xml` van het bestaande
project (binnen de bestaande `<project>...</project>`-tags, naast wat er al staat -- niets
verwijderen):

```xml
<!-- Vertelt Maven waar het JitPack kan vinden -->
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <!-- ... hier staan waarschijnlijk al andere dependencies, die blijven gewoon staan ... -->

  <!-- spring-api-testkit: genereert tests uit een OpenAPI-spec -->
  <dependency>
    <groupId>com.github.mailkroeze-art.spring-api-testkit</groupId>
    <artifactId>test-runner</artifactId>
    <version>main-SNAPSHOT</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

**Optie B -- lokaal installeren.** Als je liever niets van internet afhankelijk maakt: download deze
repository, en draai daarin eenmalig `mvn install`. Dat zet de bouwstenen klaar op de eigen computer.
Nadeel: dit moet dan op élke computer (en in de CI-server) apart gebeuren. Voor de meeste mensen is
optie A daarom makkelijker.

### Stap 2: het framework vertellen waar je eigen API draait

Dit is het stukje dat écht code raakt, en waarschijnlijk door een ontwikkelaar gedaan moet worden.
Maak een nieuw testbestand aan (bijvoorbeeld `GeneratedApiTest.java`) met deze inhoud:

```java
package com.jouwbedrijf.api;   // <- vervang dit door het package van jouw project

import dev.apitestkit.testrunner.ApiTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

// Start de echte Spring Boot-applicatie op een willekeurige, vrije poort
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeneratedApiTest extends ApiTestFactory {

    // Spring Boot vult dit veld zelf in met de poort waarop de app nu draait
    @LocalServerPort
    private int port;

    // Dit draait vlak vóórdat de tests gegenereerd en uitgevoerd worden
    @BeforeEach
    void wijsNaarDeDraaiendeApp() {
        System.setProperty("api.baseUrl", "http://localhost:" + port);
        System.setProperty("openapi.spec", "src/test/resources/openapi.yaml"); // pad naar jullie spec
    }
}
```

Zet de eigen OpenAPI-spec op het pad dat je hierboven invult (bijvoorbeeld
`src/test/resources/openapi.yaml`), en eventueel een `test-config.yaml` ernaast (zie hoofdstuk 4).

### Stap 3: draaien

Vanuit de root van het bestaande project:

```bash
mvn test -Dtest=GeneratedApiTest
```

Je ziet dezelfde soort uitkomst als in stap 3 van hoofdstuk 2: een lijst van gegenereerde tests,
groen of rood.

### Als je zelf geen ontwikkelaar bent

Je kunt de bovenstaande twee code-stukjes (het `pom.xml`-stukje en het Java-bestand) gewoon
doorsturen naar iemand die wél met het project werkt, met de vraag: "kun je dit toevoegen?". Er
hoeft verder niets aangepast te worden aan de rest van het project -- deze twee stukken zijn
zelfstandig. Wat je zelf al wél kunt doen zonder ontwikkelaar nodig te hebben:

- De OpenAPI-spec zelf aanleveren of controleren (het YAML-bestand, zie hoofdstuk 1).
- `test-config.yaml` invullen of aanpassen (zie hoofdstuk 4) -- dat is gewoon tekst, geen Java-code.
- Een testrapport lezen en begrijpen wat er rood is (zie hoofdstuk 3), en dat teruggeven aan het team.
