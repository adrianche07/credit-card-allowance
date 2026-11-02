# Credit Card Allowance API

A credit card allowance backend built with Java 21, Spring Boot 3.5, Spring MVC, Spring Data JPA, Bean Validation, Spring Security, and H2.

Authenticated users manage monthly allowance records for cardholders. Each record belongs to the user who created it. Only a card's last four digits are stored. This app records allowance limits; it does not process payments, connect to banks, track spending, or reset balances each month.

## Run

Requires JDK 21. The Maven wrapper downloads Maven 3.9.11 on its first run.

```sh
export APP_USERNAME=alex
# Choose a local password of at least 12 characters.
read -r -s -p 'API password: ' APP_PASSWORD; export APP_PASSWORD; echo
./mvnw spring-boot:run
```

The API listens on port 8080. `APP_USERNAME` and `APP_PASSWORD` are required; startup fails if they are absent or the password is too short. The demo has one configured in-memory login. Records are owner-scoped so a future persistent user directory can support multiple users without changing the API. Changing the username does not transfer existing records.

The file database persists in `data/allowances.mv.db`, which is ignored by Git. Stop the server before removing `data/` to reset local records. Tests use a separate in-memory database. Optional configuration: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and Spring Boot's `SERVER_PORT`.

### This cloud workspace

The JDK and Maven proxy settings are prepared outside the checkout. In this instance, build with:

```sh
export JAVA_HOME=/workspace/jdk21
export MAVEN_USER_HOME=/workspace/.m2
export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
./mvnw -s /workspace/maven-settings.xml verify
```

After setting `APP_USERNAME` and `APP_PASSWORD` as above, start with `"$JAVA_HOME/bin/java" -jar target/credit-card-allowance-0.0.1-SNAPSHOT.jar`. These workspace paths are specific to this prepared environment; on your own computer use a standard JDK 21 installation and the ordinary commands above.

## Use the API

In another terminal, use the same username. `curl -u "$APP_USERNAME"` prompts for the password.

```sh
export APP_USERNAME=alex
curl -i -u "$APP_USERNAME" -H 'Content-Type: application/json' \
  -d '{"cardholderName":"Sam","lastFour":"1234","monthlyLimit":250.00,"currency":"USD"}' \
  http://localhost:8080/api/allowances

curl -u "$APP_USERNAME" 'http://localhost:8080/api/allowances?page=0&size=20'
curl -u "$APP_USERNAME" http://localhost:8080/api/allowances/1

curl -i -u "$APP_USERNAME" -X PUT -H 'Content-Type: application/json' \
  -d '{"cardholderName":"Sam","lastFour":"1234","monthlyLimit":300.50,"currency":"USD"}' \
  http://localhost:8080/api/allowances/1

curl -i -u "$APP_USERNAME" -X DELETE http://localhost:8080/api/allowances/1
```

Use the actual ID or `Location` returned by creation. A successful creation returns `201`, a `Location` header, and a record such as:

```json
{"id":1,"cardholderName":"Sam","lastFour":"1234","monthlyLimit":250.00,"currency":"USD"}
```

| Method | Path | Success |
| --- | --- | --- |
| POST | `/api/allowances` | 201 with created record |
| GET | `/api/allowances?page=0&size=20` | 200 with `content`, `page`, `size`, `totalElements`, `totalPages` |
| GET | `/api/allowances/{id}` | 200 with record |
| PUT | `/api/allowances/{id}` | 200 with updated record; all fields required |
| DELETE | `/api/allowances/{id}` | 204 with no body |

Lists are sorted by ID, with page sizes from 1 to 100. Names must be nonblank and at most 100 characters. Last four digits must be exactly four ASCII digits. Monthly limits must be positive, have at most two decimal places, and not exceed 9,999,999,999.99. Supported currencies are USD, EUR, GBP, CAD, and AUD. Unknown request fields are rejected; the owner is always taken from authentication.

Missing/invalid credentials return `401`. Invalid requests return `400`. Missing records and records owned by another user return `404`. MVC errors use Spring's Problem Detail responses.

## Test and package

```sh
./mvnw test
./mvnw package
java -jar target/credit-card-allowance-0.0.1-SNAPSHOT.jar
```

The integration suite exercises CRUD and persisted updates, real HTTP Basic authentication, owner isolation on reads/updates/deletes/lists, invalid input, pagination, and missing records.

## Project structure

- `AllowanceController`: HTTP endpoints, owner-scoped access, and transactions.
- `AllowanceRepository`: JPA persistence and owner-filtered queries.
- `Allowance`: database entity, separated from public request/response records.
- `SecurityConfiguration`: stateless HTTP Basic authentication with BCrypt passwords.
- `AllowanceApiTest`: full Spring application context with MockMvc and H2.

This is a local development backend. Before a public deployment, configure HTTPS, a persistent user directory, managed database migrations and a production database. The API disables CSRF for stateless API clients; browser integration requires revisiting CSRF and authentication. No CORS origins are enabled by default.
