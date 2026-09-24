# URL Shortener API

A Java 21 / Spring Boot service that creates short links, redirects visitors, and records aggregate click analytics. Built as an AI-assisted engineering assignment with documented requirements, implementation decisions, regression scenarios, and validation evidence.

**Create → redirect → measure**, with optional custom aliases, expiration, and permanent deactivation.

## Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 4.1.1: Web MVC, Data JPA, Validation, Actuator |
| Build | Gradle wrapper 9.7.1 |
| Local persistence | H2 file database; data survives application restarts |
| PostgreSQL | PostgreSQL 17 via Docker Compose; separate `postgres` profile |
| Schema management | Flyway migrations; Hibernate validates the schema |
| API documentation | springdoc-openapi 3.1.1 / Swagger UI |
| Boilerplate | Lombok for entity getters and constructor injection |
| Development | Spring Boot DevTools; optional local H2 console |
| Tests | JUnit, AssertJ, MockMvc, Mockito, H2, JaCoCo |

## Quick start

Prerequisite: **JDK 21**. Gradle is included; no separate installation is required.

```bash
./gradlew test
./gradlew bootRun
```

On macOS, if necessary:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
chmod +x gradlew
```

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |
| Health | http://localhost:8080/actuator/health |

The root `/` has no controller; use Swagger to explore the API. Stop another application on port 8080 or run `PORT=8081 APP_BASE_URL=http://localhost:8081 ./gradlew bootRun`.

For the local H2 console, run `./gradlew bootRun --args='--spring.profiles.active=dev'` and visit `/h2-console`. Use JDBC URL `jdbc:h2:file:./data/url-shortener;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`, user `sa`, and an empty password. The console is disabled by default and allows only local access.

## Run with Docker and PostgreSQL

Requires Docker with Compose. From the project root:

```bash
cp .env.example .env
# Edit .env and choose DATABASE_PASSWORD before continuing.
docker compose up --build -d
```

The image build runs the test suite. PostgreSQL must pass its health check before the app starts. Database data is stored in a named volume; the app runs as a non-root user. Ports bind to loopback for the local demo.

```bash
docker compose logs -f app
docker compose down
```

`down` preserves the database volume. Do not use `down -v` unless you intend to delete the stored links.

To run Java locally against the Compose database:

```bash
docker compose up -d postgres
# Set this to the password you chose in .env; Gradle does not load .env automatically.
export DATABASE_PASSWORD='your-local-password'
export DATABASE_URL='jdbc:postgresql://localhost:5433/urlshortener'
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

## API walkthrough

### 1. Create a short link

```bash
curl -i -X POST http://localhost:8080/api/v1/links   -H 'Content-Type: application/json'   -d '{
    "originalUrl": "https://example.com/articles/spring-boot",
    "customAlias": "spring-guide",
    "expiresAt": "2030-12-31T23:59:59Z"
  }'
```

Returns **201 Created** with `Location: /api/v1/links/spring-guide` and:

```json
{
  "code": "spring-guide",
  "shortUrl": "http://localhost:8080/r/spring-guide",
  "originalUrl": "https://example.com/articles/spring-boot",
  "createdAt": "2026-09-22T01:00:00Z",
  "expiresAt": "2030-12-31T23:59:59Z",
  "status": "ACTIVE"
}
```

`createdAt` above is illustrative. Omit `customAlias` to generate an eight-character Base62 code. Omit `expiresAt` for no expiration. Repeating the same alias returns 409; use a different alias for each walkthrough. Repeated URLs are allowed and receive distinct codes when an alias is omitted.

### 2. Follow the redirect

```bash
curl -i http://localhost:8080/r/spring-guide
```

Returns **302 Found**, `Location` set to the original URL, and `Cache-Control: no-store`. Use `curl -L` to follow the destination. `curl -I` sends HEAD and does **not** increment the counter.

### 3. Inspect metadata and analytics

```bash
curl http://localhost:8080/api/v1/links/spring-guide
curl http://localhost:8080/api/v1/links/spring-guide/analytics
```

Analytics after one accepted GET redirect:

```json
{
  "code": "spring-guide",
  "totalClicks": 1,
  "lastClickedAt": "2026-09-22T01:01:00Z"
}
```

Counts include repeat requests and bots. They measure accepted redirect requests, not unique people or confirmed arrival at the destination. HEAD, metadata reads, and failed redirects do not count. Before the first click, `totalClicks` is 0 and `lastClickedAt` is null.

### 4. Deactivate a link

```bash
curl -i -X DELETE http://localhost:8080/api/v1/links/spring-guide
```

Returns **204 No Content**. Subsequent redirects return **410 Gone**. Repeating DELETE also returns 204. Metadata and analytics remain available, and the alias remains reserved.

## Endpoints

| Method | Path | Successful response |
|---|---|---|
| POST | `/api/v1/links` | 201 + created link and resource Location |
| GET | `/api/v1/links/{code}` | 200 + metadata and computed status |
| GET | `/r/{code}` | 302 + destination; records one click |
| HEAD | `/r/{code}` | 302 + destination; records no click |
| GET | `/api/v1/links/{code}/analytics` | 200 + total clicks and last click time |
| DELETE | `/api/v1/links/{code}` | 204; permanently disables the link |

Ready-to-run IntelliJ HTTP requests are in [requests.http](requests.http). The live OpenAPI definition is the API schema source of truth.

## Validation and error responses

- Original URLs: absolute HTTP(S), maximum 2048 characters, valid host, no credentials, literal whitespace, or control characters. Unicode hostnames must use their ASCII/punycode form.
- Custom aliases: 4–32 ASCII letters, digits, `_`, or `-`; case-sensitive and never reused.
- Expiration: a future timestamp with an offset, stored at microsecond precision. Redirects are valid only while request time is strictly before expiration.
- Disabled status takes precedence over expired status.

| Scenario | Status |
|---|---|
| Invalid URL, alias, expiry, or JSON | 400 |
| Unknown code | 404 |
| Alias already allocated, including disabled/expired links | 409 |
| Expired or disabled redirect | 410 |
| Database unavailable or generated-code retries exhausted | 503 |

Errors use `application/problem+json` with status, title, detail, and instance. Bean-validation failures also include `fieldErrors`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/links",
  "fieldErrors": {"customAlias": "must contain 4-32 letters, digits, hyphens or underscores"}
}
```

## Testing

```bash
./gradlew clean check bootJar
python3 scripts/smoke-test.py
```

The smoke test requires a running application, uses a fresh alias, sends 80 redirects across eight workers, verifies exact counts and error behavior, then deactivates its test link. It does not follow external URLs. Override the target using `BASE_URL=http://localhost:8081 python3 scripts/smoke-test.py`.

- **URL-policy unit tests:** valid URLs, unsupported schemes, credentials, malformed hosts, whitespace, and length limits.
- **HTTP integration tests:** creation, redirects, validation, metadata, lifecycle, analytics, Swagger, and health.
- **Concurrency tests:** exact click totals and one winner for a contested alias.
- **Collision tests:** generated-code retry and bounded exhaustion using real database constraints.
- **Migration test:** upgrade a populated V1 schema to V2 without losing links.
- **Deterministic time:** injected test clock covers the exact expiration boundary without sleeping.

HTML reports: `build/reports/tests/test/index.html` and `build/reports/jacoco/test/html/index.html`. See [validation evidence](docs/VALIDATION.md) for actual runs and limitations.

## Configuration

| Variable | Default / purpose |
|---|---|
| `PORT` | `8080`; local app port / Compose host port |
| `APP_BASE_URL` | `http://localhost:8080`; canonical public origin used in returned short URLs |
| `SPRING_PROFILES_ACTIVE` | unset = H2; `postgres` = PostgreSQL; `dev` enables local H2 console |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/urlshortener` in postgres profile |
| `DATABASE_USERNAME` | `urlshortener` in postgres profile |
| `DATABASE_PASSWORD` | required for postgres profile |
| `POSTGRES_PORT` | `5433`; Compose host database port only |

Set `APP_BASE_URL` to your real public origin behind a proxy. Forwarded/Host headers are deliberately not used to construct links. Environment variables override YAML. DevTools is excluded from the packaged production jar by Spring Boot.

## Design notes

- **Uniqueness:** the database unique constraint is authoritative. Generated collisions retry up to five times, each in a fresh transaction; alias conflicts return 409.
- **Analytics correctness:** one conditional SQL UPDATE checks lifecycle state and increments the counter atomically. A follow-up indexed read retrieves the immutable destination. A failed transaction produces no successful redirect.
- **No cache initially:** direct database access keeps expiry, deactivation, and analytics consistent. Hot links contend on a counter row; higher throughput would require a deliberate analytics redesign.
- **Persistence:** H2 is convenient for single-process development. PostgreSQL is the deployment path; Flyway owns schema changes, and `ddl-auto=validate` prevents silent schema mutation.
- **Privacy:** only aggregate counts and timestamps are stored. No visitor IP addresses, user agents, or referrers are collected.
- **Redirect safety:** destination URLs are validated but never fetched by the server. This avoids a server-side URL-fetching SSRF path; it does not make arbitrary destinations trustworthy.

## Project layout

```text
src/main/java/com/assignment/url_shortener/
├── UrlShortenerApplication.java
├── config/             # UTC clock and OpenAPI metadata
├── domain/             # Entity and indexed/atomic repository operations
├── exception/          # HTTP-mapped domain failures
├── service/            # Link lifecycle, collision retry, code generation
├── validation/         # Destination URL policy
└── web/                # Controllers, DTOs, ProblemDetail handler
src/main/resources/
├── application*.yaml   # H2, PostgreSQL, optional local console
└── db/migration/       # Versioned SQL schema
src/test/               # Unit, integration, concurrency, migration tests
docs/                   # Architecture, requirements, scenarios, AI log, evidence
scripts/smoke-test.py    # Live HTTP validation
```

## Assignment documentation

- [Requirements and acceptance criteria](docs/REQUIREMENTS.md)
- [Architecture and trade-offs](docs/ARCHITECTURE.md)
- [Greenfield, brownfield, and ambiguous scenarios](docs/SCENARIOS.md)
- [AI-assisted execution and review log](docs/AI_ASSISTANCE.md)
- [Validation results and limitations](docs/VALIDATION.md)

## Production limitations

This is a runnable engineering prototype, **not an internet-ready public shortening service**. Management APIs and analytics currently have no authentication or ownership checks. Before public deployment, add authorization, abuse/rate controls, destination policy, TLS, secret management, backup/restore checks, and operational monitoring. Public link creation permits phishing/redirect abuse unless controlled; local/private destinations are currently allowed. Short codes are identifiers, not access tokens.

There is no unique-visitor tracking, event history, cache, queue, retry-idempotency key, tenant model, or retention job. Click counting is synchronous and favors consistency over availability. In-flight requests admitted before a disable or expiry transition may complete afterward. See the architecture document for the exact trade-offs.
