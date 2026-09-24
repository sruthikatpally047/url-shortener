# Validation evidence

Executed locally on **2026-09-21 (America/Chicago)** with Java 21.0.9, Gradle wrapper 9.7.1, and Spring Boot 4.1.1. These are observed local results, not production guarantees.

## Build and automated tests

- Greenfield baseline: `./gradlew test --no-daemon` — **5 tests passed** before the analytics/lifecycle enhancement.
- Expanded regression run initially found **5 failing assertions** because Spring Boot's default ProblemDetail advice omitted custom `fieldErrors`. Giving the application advice highest precedence corrected the response contract.
- Clean build during implementation: `./gradlew clean check bootJar --no-daemon` — **passed**.
- Final source after OpenAPI/error-path additions: `./gradlew check bootJar --no-daemon` — **43 tests, 0 failures, 0 errors, 0 skipped**.
- JaCoCo aggregate coverage: **109 / 115 lines (94.8%)**, **54 / 60 branches (90.0%)**. Generated code can be filtered by JaCoCo; coverage is not a substitute for semantic tests.
- Executable jar produced at `build/libs/url-shortener.jar`.

Reports are generated locally at `build/reports/tests/test/index.html` and `build/reports/jacoco/test/html/index.html`; run the build to reproduce them.

## Covered behaviors

| Area | Evidence |
|---|---|
| URL parsing | HTTP(S), ports, encoded paths, IPv6; rejects unsupported schemes, missing hosts, credentials, whitespace, controls and overlong input |
| HTTP contracts | 201 creation and Location, metadata, 302 destination/no-store, 400/404/409/410 errors |
| OpenAPI | Swagger HTML and JSON respond; spec exposes 201 creation and 302 redirect responses |
| Lifecycle | Exact expiry boundary, disabled state, idempotent DELETE, no alias reuse |
| Analytics | GET increments; HEAD/metadata/failures do not; timestamps remain monotonic |
| Concurrency | 80 redirects over eight workers yield 80 clicks; eight contenders for one alias yield one winner |
| Allocation | Forced generated-code collision retries in a fresh transaction; five collisions produce bounded failure |
| Migration | A populated V1 schema upgrades to V2 without losing the original URL; defaults enable the legacy row with zero clicks |
| Storage failures | Data access failure and transaction-start connection failure map to sanitized 503 responses |
| Health/startup | Application context and health endpoint succeed |

## Live packaged-application verification

Launched the final jar on isolated port **18081** with a separate file-backed H2 database, then ran:

```bash
BASE_URL=http://localhost:18081 python3 scripts/smoke-test.py
```

Observed final result:

```json
{
  "result": "PASS",
  "concurrentRequests": 80,
  "workers": 8,
  "recordedClicks": 80,
  "medianRedirectMs": 4.58,
  "p95RedirectMs": 17.09
}
```

The smoke test checked health, Swagger, API schema, creation, HEAD exclusion, redirect headers, exact concurrent counts, duplicate aliases, invalid input, unknown links, and deactivation. It never followed destination URLs.

Stopped and restarted the application against the same file database. The test link retained **80 clicks** and **DISABLED** status. Both final test processes completed graceful shutdown and were stopped after verification.

These latency values describe a small warm local run with eight workers, one hot link, and no external network. They are not a throughput capacity claim, performance SLO, production benchmark, or PostgreSQL result.

## Docker / PostgreSQL status

- `docker compose config --quiet` — **passed**; Compose configuration is valid.
- Dockerfile and PostgreSQL profile are provided, with a multi-stage non-root application image, database health check, persistent volume, and Flyway migrations.
- **Container build/run and real PostgreSQL behavior remain unverified.** Docker's daemon returned HTTP 500 for image listing/pulling through its local socket. Pulling `postgres:17-alpine` failed with both the default API v1.54 and an explicit API v1.44 retry.
- No Docker restart/reset was performed. This environment failure is not treated as a successful PostgreSQL test, and H2's PostgreSQL compatibility mode is not a substitute for testing PostgreSQL itself.

After Docker is healthy, run:

```bash
docker compose up --build -d
python3 scripts/smoke-test.py
docker compose down
```

A private ignored `.env` with a generated local database password was prepared during setup; `.env.example` documents configuration for a fresh checkout. Do not commit `.env`.

## Tooling observations and remaining limits

- Flyway emits a warning that the Boot-managed H2 2.4.240 is newer than its latest verified H2 2.3.232. Fresh migration, schema validation and V1→V2 upgrade tests passed locally; PostgreSQL still needs the separate run described above.
- Gradle emitted an FSEvents watcher warning in the restricted environment; compilation and tests completed successfully.
- A preliminary shutdown check overlapped rebuilding its running jar and encountered a class-loading error. Final runtime verification used a stable, completed jar and passed both shutdowns. Stop a packaged app before replacing its jar.
- The smoke script passed Python syntax compilation. README/document links were checked locally.
- No dedicated Checkstyle/SpotBugs/SAST, dependency vulnerability scan, penetration test, production load test, backup/restore test, database outage injection, or multi-node failover exercise was performed.
- Management authorization, rate limiting and abuse prevention remain future work. Exception-mapping tests simulate failures; they do not establish outage-recovery behavior.
- Engineer review/sign-off is pending. No public deployment was attempted or implied.
