# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A squash court booking system built as Spring Boot microservices, backed by Postgres. This is a learning project, currently in early development — `court-service` is the only service with real implementation; `booking-service` and `member-service` are stubs (pom.xml only, no source yet). The intended domain split (see `sql/schema.txt` for the full target schema across all three services):

- **court-service** (port 8081): courts, time slots, and pricing.
- **booking-service**: bookings and payment obligations for opponents.
- **member-service**: members, account balances, and transaction history.

Services are independent — cross-service references (e.g. a booking's `court_id`) are stored as plain UUIDs with no FK, since each service owns its own database.

## Commands

All commands run from the `services/` directory (the Maven multi-module root).

```bash
# Build all services
mvn -B compile

# Run all unit/integration tests
mvn -B test

# Run tests for a single module
mvn -B test -pl court-service

# Run a single test class
mvn -B test -pl court-service -Dtest=CourtPricingServiceTest

# Run a single test method
mvn -B test -pl court-service -Dtest=CourtPricingRepositoryTest#shouldReturnEmptyWhenNoCourtPricingFound

# Run a service locally (requires Postgres reachable at DB_URL/DB_USERNAME/DB_PASSWORD, see application.yml)
mvn -pl court-service spring-boot:run

# Run Checkstyle only (also runs automatically at the validate phase, so mvn compile/test already enforce it)
mvn -B checkstyle:check
```

Repository tests (`*RepositoryTest`) use Testcontainers to spin up a real Postgres instance — Docker must be available to run them.

CI (`.github/workflows/ci.yml`) runs a `Checkstyle` step and `Build` (`mvn -B compile`) in parallel, then `mvn -B test`, against Java 25 with the same working directory (`./services`).

## Architecture

### Module layout

Each service is a Spring Boot module under `services/<name>-service`, inheriting from the parent POM at `services/pom.xml` (Spring Boot 4.1.0, Java 25). Shared dependency versions (Testcontainers, springdoc-openapi) are pinned in the parent's `dependencyManagement`.

Within `court-service`, code is organized by domain concept, not by layer — e.g. `court/`, `pricing/`, `timeslot/`, each containing its own Entity, Repository, Service, and Controller:

```
com.rdp.courts.courtservice/
  court/       Court entity, CourtRepository, CourtService, CourtController
  pricing/     CourtPricing entity, CourtPricingRepository, CourtPricingService, CourtPricingController, DayType
  timeslot/    TimeSlot entity, TimeSlotRepository, TimeSlotService, TimeSlotController
```

Classes and constructors are package-private by default (only `@SpringBootApplication` and JPA-required members are `public`) — each domain package is a self-contained vertical slice, not meant to expose internals to other packages. Prefer package-private for entities, repositories, and services. Use `mock(ClassName.class)` in cross-package tests rather than widening visibility just for testing purposes. Only widen to `public` when genuinely needed for cross-package production code.

### Database migrations

Each service manages its own schema via Flyway migrations in `src/main/resources/db/migration` (`V1__...sql`, `V2__...sql`, ...). JPA is `ddl-auto: validate` — entities must match the migration-defined schema exactly rather than generating it.

`sql/schema.txt` is a design-level overview of the full target schema across all three services (some columns described there, e.g. `courts.slot_start_offset_minutes`, are not yet implemented). `sql/scripts/<service>/` contains standalone SQL scripts (create DB, schema, seed) — these are a reference/dev-provisioning copy, not what's actually run by Flyway; the source of truth for court-service is `court-service/src/main/resources/db/migration`.

### Pricing lookup pattern

`CourtPricingRepository.findApplicablePricing` looks up the fee for a `(dayType, time, date)` triple by finding the pricing row whose period contains `time` and whose `validFrom` is the most recent one `<= date` (`ORDER BY valid_from DESC LIMIT 1`) — this is how price changes over time are supported without altering history.

### API docs

springdoc-openapi is wired in for court-service; Swagger UI and `/v3/api-docs` are enabled via `application.yml`.

### Code style

`final` is used everywhere it can be: fields, every constructor/method parameter, and every local variable (`final var x = ...`). Two exceptions, both structural rather than stylistic:
- Lambda parameters are left unannotated — Java requires explicit types on *all* of a lambda's parameters if any one of them has a modifier, so adding `final` would force `(final Court court) -> ...` over the idiomatic `court -> ...`.
- Repository interface methods (e.g. `CourtRepository.findByIsActiveTrue`) don't need it — Checkstyle's `FinalParameters` check (see below) skips abstract/interface method parameters automatically, since there's no method body for `final` to protect.

Enforced by Checkstyle (`services/checkstyle.xml`: `FinalParameters`, `FinalLocalVariable`), wired into `services/pom.xml` at the Maven `validate` phase so `mvn compile`/`mvn test` catch violations locally regardless of editor tooling, and run as a dedicated step in CI.

### Git workflow

Work happens on a feature branch with as many WIP commits as needed. Before raising a PR, rebase onto the base branch and squash/fixup/reword down to a single clean commit — the PR should land as one commit with a clear message, not a trail of `fix`/`wip` commits. Don't leave multiple commits on a branch expecting them to be squashed at merge time; do it yourself as part of preparing the PR.

### Testing conventions

- `*ServiceTest` — unit tests, mock the repository.
- `*RepositoryTest` — `@DataJpaTest` + `@Testcontainers` with a real `PostgreSQLContainer`, `@AutoConfigureTestDatabase(replace = Replace.NONE)` so Flyway migrations run against the container instead of an in-memory DB.
- `*ControllerTest` — MVC slice tests.
- Parameterized tests (`@ParameterizedTest` + `@MethodSource`) are used heavily for pricing period boundary cases, grouped with `@Nested` classes per scenario (e.g. weekday vs. weekend pricing).
