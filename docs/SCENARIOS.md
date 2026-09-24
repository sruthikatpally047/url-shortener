# Three engineering scenarios

The project progresses through three scenarios: establishing a working baseline, enhancing that baseline, and resolving ambiguous lifecycle requirements. The brownfield scenario builds on the first stage of this project.

## 1. Greenfield: runnable short-link service

**Intent:** turn a Spring Initializr starter into a minimal, end-to-end service.

**Context and constraints:** Java 21 / Boot 4.1.1 / Gradle; use controller/service/repository separation; use the neutral package supplied in the starter; include Swagger and simple setup.

**Decomposition:** establish V1 schema → URL policy and code generator → isolated insert transaction → create/metadata/redirect APIs → errors/OpenAPI/config → baseline integration tests.

**Acceptance:** POST returns 201 with a usable short URL; lookup returns stored data; GET returns a 302 destination; unsupported schemes fail; absent codes fail predictably; Swagger and health respond.

**Execution and validation:** implemented V1 and core endpoints, then ran the baseline before enhancement. Five tests passed, covering application startup, round-trip creation/read/redirect, unsafe scheme rejection, unknown code, and Swagger/health. The implementation was then extended rather than presenting later tests as baseline evidence.

## 2. Brownfield: enhance the tested baseline with analytics

**Intent:** record useful analytics without losing clicks or breaking existing links.

**Impacted modules/data flows:** `ShortLink` persistence fields, V2 migration, repository UPDATE, redirect service, analytics DTO/controller, and regression tests. Creation and redirect endpoint paths remain stable.

**Decomposition:** additive migration with defaults → atomic increment and timestamp logic → analytics endpoint → HEAD exclusion → migration and concurrency regression tests.

**Acceptance:** existing V1 rows remain usable with zero clicks; 80 concurrent accepted redirects record 80 clicks; analytics reads do not increment; HEAD does not increment; last click time does not move backward.

**Execution and validation:** added V2 without editing V1; tested migration of an existing row; exercised 80 service calls across eight workers; tested HTTP HEAD and metadata exclusion and out-of-order timestamps. Retained the original core integration suite. Also ran a live HTTP smoke test against the packaged jar with 80 requests and eight workers.

**Rejected approach:** load entity → increment in Java → save. Concurrent requests can overwrite each other's count unless locked/versioned. A conditional atomic UPDATE makes the intended invariant explicit with less application complexity.

## 3. Ambiguous: expiration and alias lifecycle

**Intent:** make “reliability features” concrete without pretending the assignment specifies every behavior.

**Ambiguities:** Is an exact expiry instant still valid? Are aliases case-sensitive? Can an expired alias be reused? Are inactive links missing? What counts as a click? Does DELETE erase analytics?

**Decisions:** exact expiry is inactive; aliases are case-sensitive, 4–32 characters, and permanently reserved; expired/disabled redirects return 410 while unknown codes return 404; DELETE disables permanently but preserves metadata and analytics; only accepted GET redirects count. Optional timestamps are normalized to microseconds to match persistence.

**Decomposition:** document semantics → DTO validation and service invariants → optional expiration/deactivation fields → conditional redirect admission → deterministic clock tests → conflict and concurrency tests.

**Acceptance:** a redirect just before expiry succeeds; at expiry it fails without counting; repeated DELETE succeeds for existing links; reuse returns 409; eight contenders for one alias yield one 201 and seven 409 responses; generated collisions retry and eventually stop.

**Execution and validation:** implemented policies and tests without sleep-based timing. The first expanded run found five failures: Boot's default ProblemDetail handler intercepted bean-validation errors and omitted `fieldErrors`. Inspected the actual response, raised custom advice precedence, and reran successfully. The fix preserved the intended API contract instead of weakening the tests.

## Review ownership

Engineering review covers the requirement decisions, implementation, regression evidence, and deployment limitations. Final acceptance remains pending; local automated validation does not establish production readiness.
