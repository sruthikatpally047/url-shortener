# AI-assisted engineering

## Approach and developer responsibilities

The developer established the project through Spring Initializr, selected the initial dependencies, and directed the scope: a URL shortener with Swagger, tests, and supporting documentation. Follow-up evaluation covered API behavior, redirect methods, database access, code generation, and the trade-off between independent short links and destination deduplication.

AI supported implementation, test generation, debugging, technical research, and documentation within that scope. Its contributions included generated application code and tests as well as revisions to configuration and documentation. Engineering ownership includes understanding these contributions, checking their behavior, and deciding whether the result meets the requirements. Final review and acceptance remain pending.

## Tasks, assistance, and evidence

| Engineering task | Intent and constraints | AI assistance | Result / validation |
|---|---|---|---|
| Requirement analysis | Define testable behavior for a URL-shortener prototype | Helped decompose APIs, lifecycle rules, and failure cases | Acceptance criteria and assumptions in REQUIREMENTS.md |
| Stack integration | Preserve Java 21, Boot 4, Gradle, and the supplied dependencies | Checked Swagger compatibility and added migration support | Application startup and API documentation tests |
| Greenfield implementation | Establish create, lookup, and redirect behavior | Generated implementation and baseline tests | Five baseline tests passed before enhancement |
| Brownfield enhancement | Add analytics without losing existing links or concurrent counts | Added the V2 migration, conditional counter update, and regression tests | Existing-row migration and 80-request concurrency checks |
| Ambiguous requirements | Make expiration, alias reuse, and click semantics explicit | Proposed policies and implemented boundary tests | Deterministic expiry, HEAD exclusion, and alias-conflict tests |
| Debugging | Preserve field-level validation errors | Investigated failing responses and corrected exception-handler precedence | Previously failing assertions passed without weakening the contract |
| API review | Document actual responses and avoid exposing database details | Added response annotations and error-path tests | OpenAPI 201/302 checks and sanitized 503 responses |
| Documentation | Make setup and design decisions reviewable | Drafted README, architecture, examples, and validation notes | Documents checked against the implemented API and configuration |
| Runtime verification | Exercise the packaged application independently of MockMvc | Ran live HTTP checks and persistence/restart verification | Results and remaining gaps in VALIDATION.md |

## Implementation traceability

- **Generated with AI assistance:** application components, schema migrations, automated tests, container setup, smoke-test script, and documentation drafts.
- **Edited with AI assistance:** starter build configuration, application settings, context test, and ignore rules. The starter's group, package, and technology choices were retained.
- **Alternatives rejected during implementation:** a lookup as the sole uniqueness guarantee; read/increment/save counters that risk lost updates; collision retries inside a rollback-only transaction; and premature cache or queue infrastructure. Database constraints, isolated insert transactions, and atomic counter updates address these concerns directly.

This is a task-level record. It does not replace a verbatim prompt transcript or change-by-change review history.

## Validation and review controls

Implementation work used explicit acceptance criteria and iterative checks: establish the baseline, extend it, exercise failure cases, inspect regressions, and rerun validation after corrections. The final recorded run contains 43 passing tests, with separate live HTTP and restart checks. Coverage and detailed results are in VALIDATION.md.

The validation-error regression provides one concrete refinement example: tests expected fieldErrors, the actual response lacked them, inspection identified the default advice taking precedence, and the custom advice was corrected. The intended API contract remained unchanged.

Review responsibilities include confirming business semantics, inspecting generated changes, interpreting test results, and accepting known limitations. Production deployment and database rollout require a separate release review. Docker/PostgreSQL execution remains unverified because of the local Docker daemon failure; dedicated static analysis, vulnerability scanning, and production load testing were not performed.

## Secure AI usage

Public research concerned dependency documentation. Credentials and proprietary source were not intentionally included in web searches. Local secrets belong in the ignored .env file and must not be copied into prompts or committed. Test requests use example destinations and the smoke test does not follow external redirects.

AI output is subject to the same correctness, security, and maintainability review as other changes. Design review and automated tests provide evidence; they do not establish production certification.
