# Requirements and acceptance criteria

## Source and interpretation

The supplied **Interview Assignment: Build an AI-Assisted Software Engineering System — URL Shortener** asks for a working prototype with core APIs, analytics, reliability, engineering decomposition, AI-assisted execution, validation, and three scenarios (greenfield, brownfield, ambiguous). It does not prescribe a framework, exact API routes, analytics dimensions, authentication scheme, or expiration policy.

The project uses a layered Spring Boot architecture with Swagger and setup documentation. The supplied starter selected Java 21, Spring Boot 4.1.1, Gradle, `com.assignment`, H2, PostgreSQL, Lombok, DevTools, and Actuator. Those choices were retained. The original starter package `com.assignment.url_shortener` was kept to avoid an unnecessary rename.

Policies below are implementation decisions, not quotations from the assignment. They are explicit and testable so the engineer can review and defend them.

## Acceptance criteria

| ID | Requirement / decision | Acceptance evidence |
|---|---|---|
| R1 | Store an absolute HTTP(S) URL and return a short link | POST returns 201, resource Location, code and metadata; record is persisted |
| R2 | Resolve a valid code | GET `/r/{code}` returns 302 and exact destination; no-store header |
| R3 | Prevent duplicate codes | Unique database constraint; generated collisions retry; aliases return 409 |
| R4 | Record aggregate analytics | Accepted GET increments once; total and last click exposed |
| R5 | Handle concurrent visits | 80 concurrent service redirects produce exactly 80 clicks |
| R6 | Support optional custom aliases | 4–32 permitted characters, case-sensitive, permanent reservation |
| R7 | Support optional expiration | Exact expiry returns 410; absent expiry means no time limit |
| R8 | Support permanent deactivation | DELETE is idempotent for known links; future redirects return 410 |
| R9 | Provide predictable failures | 400 validation, 404 absent, 409 conflict, 410 inactive, 503 storage/retry failure |
| R10 | Make API reviewable | Swagger UI and generated OpenAPI with request constraints and response codes |
| R11 | Make setup repeatable | Gradle wrapper, H2 default, PostgreSQL profile, Compose, documented commands |
| R12 | Demonstrate safe schema evolution | V1→V2 migration preserves an existing link with valid defaults |
| R13 | Show disciplined AI assistance | Actual baseline, enhancement, defect/fix and validation recorded in scenario/AI logs |

## Defined semantics

- A click means a GET redirect admitted by the service and committed to the database. It is not proof of delivery to the browser or arrival at the destination.
- HEAD probes do not count. Bots and repeated GET requests do count.
- Destination and code are immutable. Updating destinations is out of scope.
- Expiration uses UTC instants; timestamps are normalized to microseconds. Active means `now < expiresAt` when an expiration exists.
- A request admitted before expiration or deactivation may complete after the transition. Each redirect uses one sampled request time.
- Deactivation is a soft delete. Metadata and counts remain; codes are never recycled.
- No URL deduplication or idempotency key: repeated creation can create distinct links.
- Anonymous prototype: there are no accounts, per-user ownership checks, or restricted management endpoints. Local/demo deployment is the intended scope.

## Sequencing and dependencies

1. Inspect the starter, assignment, architectural conventions, and dependency compatibility.
2. Add V1 schema, URL policy, short-code generator, creation, metadata, redirect and documentation plumbing.
3. Validate the greenfield baseline before extending it.
4. Extend V1 through V2 with counters and lifecycle state; retain endpoint contracts.
5. Resolve ambiguous expiry/alias/analytics behavior and encode tests at the boundaries.
6. Verify collisions, concurrent updates, migrations, error handling, Swagger and packaged HTTP behavior.
7. Prepare setup, architecture, examples, AI traceability and honest validation evidence.

## Out of scope

Authentication, multi-tenancy, billing, QR codes, a custom web frontend, malware scanning, geographic/device/unique-visitor analytics, message queues, CDN caching, and production infrastructure. The assignment's broad reliability goals are addressed through constraints, transactions, migrations, bounded retries, input validation, graceful shutdown and tests; they do not imply that every production capability is implemented.
