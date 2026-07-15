# Oven Platform

Oven Platform is a multi-tenant Java/Spring Boot backend platform for restaurant and pizzeria operations, built as a modular monolith using Spring Modulith.

## Project status

This is a personal learning and portfolio project.

I use this repository to practice backend architecture, Spring Boot, modular monolith design,
domain modeling, testing, observability, and system design decisions.

Issues are used to plan and document my own implementation work.

I am not currently hiring paid contributors for issues in this repository.

---

The current MVP focuses on the foundations required for a SaaS platform:

- tenant management
- identity and authentication
- JWT-based security
- product catalog
- order management
- kitchen preparation workflow
- fulfillment readiness flow
- initial payment tracking
- platform-level observability and API consistency

Beyond the business domain, this project is also intended to demonstrate backend architecture decisions commonly required in product companies: modular design, ports and adapters, API contracts, persistence boundaries, automated quality checks, event-driven module communication, and future readiness for asynchronous processing.

---

## Goals

The main goals of this project are:

- Build a realistic SaaS backend using a modular monolith approach
- Keep business modules isolated and tenant-aware
- Model domain entities with explicit invariants and behavior
- Avoid anemic domain models where business rules belong in the domain
- Use a pragmatic ports-and-adapters architecture
- Keep infrastructure details isolated from application use cases
- Use events to reduce coupling between modules where it adds business value
- Provide a strong foundation for future event-driven and distributed-system patterns
- Use Spring Modulith's durable event publication registry for asynchronous module communication
- Maintain production-oriented quality through tests, static analysis, architecture rules, and CI

---

## Architecture

Oven Platform follows a pragmatic ports-and-adapters style inside a Spring Modulith modular monolith.

The project is not intended to be a dogmatic Clean Architecture implementation. Instead, it focuses on controlling dependency direction and isolating important boundaries without adding unnecessary abstractions.

At a high level:

```text
HTTP / Security / Persistence / Events / External Tools
        ↓
Infrastructure adapters
        ↓
Application services and ports
        ↓
Domain model
```

Example module structure:

```text
br.com.f2e.ovenplatform
├── tenant
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── identity
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── catalog
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── orders
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── kitchen
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── fulfillment
│   ├── application
│   └── infrastructure
│
├── payment
│   ├── domain
│   ├── application
│   └── infrastructure
│
└── shared
    ├── domain
    ├── application
    └── infrastructure
```

### Architectural principles

- Application services implement use cases directly.
- Ports are introduced when there is a real boundary or volatility, such as persistence, token generation, messaging, or external integrations.
- Infrastructure implements application ports through adapters.
- Web controllers do not access Spring Data repositories directly.
- Application code does not depend on Spring Data repositories.
- Domain entities enforce core invariants and cannot be created in an invalid state.
- Cross-module JPA relationships are avoided where they would increase coupling.
- Tenant isolation is treated as a first-class concern.
- Modules communicate through explicit contracts when direct coupling would make future evolution harder.
- Modules publish typed application events and consume them through explicit module listeners.
- Durable listeners use Spring Modulith's event publication registry instead of a second internal broker abstraction.

---

## Current Features

### Tenant Management

The platform includes the foundation for tenant management:

- tenant entity
- tenant repository
- tenant creation API
- tenant retrieval API
- tenant-scoped data model foundations

### Identity and Authentication

The identity module provides:

- tenant-scoped users
- email normalization
- password hashing using BCrypt
- Spring Security integration
- JWT generation and validation
- JWT authentication filter
- `/auth/login` endpoint
- stateless authentication
- user creation API
- user retrieval API
- tenant-aware user lookup

### Catalog

The catalog module currently includes:

- rich `Product` domain entity
- tenant-scoped products
- product name validation and trimming
- price validation using `BigDecimal`
- active/inactive product state
- domain behavior methods:
  - rename
  - change price
  - activate
  - deactivate
- product persistence boundary
- product repository port
- JPA repository adapter
- catalog application service
- product creation and lookup foundations
- integration tests validating product persistence and tenant isolation

### Orders

The orders module currently includes:

- rich `Order` aggregate
- order item entity
- tenant-scoped order creation
- backend-calculated order totals using catalog prices
- order items with price snapshots
- order lookup by id
- order listing by tenant
- operational order lifecycle:
  - `CREATED`
  - `READY`
  - `DELIVERED`
  - `CANCELLED`
- lifecycle timestamps:
  - `readyAt`
  - `deliveredAt`
  - `cancelledAt`
- idempotent status transitions
- invalid transition protection
- order creation with required initial payment information
- typed order-created application event publication
- consumption of fulfillment readiness application events to mark orders as ready
- explicit application event contracts for downstream modules such as Kitchen and Payments

### Kitchen

The kitchen module currently includes:

- tenant-scoped kitchen tickets created from order-created application events
- ticket item snapshots based on order items
- ticket lookup by order id
- ticket preparation workflow
- ticket readiness publication through Spring Modulith
- typed kitchen ticket-ready event for downstream fulfillment processing

### Fulfillment

The fulfillment module currently includes:

- consumption of kitchen ticket-ready application events
- translation of module events into local preparation-ready commands
- publication of typed fulfillment readiness application events
- idempotent readiness processing

### Payments

The payment module currently includes:

- `Payment` aggregate
- tenant-scoped payment persistence
- payment registration from `order.created` events
- initial payment statuses:
  - `PENDING`
  - `PAID`
- payment methods:
  - `CASH`
  - `CARD`
  - `PIX`
- `paidAt` handling through application time using `Clock`
- one payment per order in the current MVP scope
- durable module listener that maps order-created events into Payments commands
- payment repository port and JPA adapter

### Platform and Cross-Cutting Concerns

The shared platform layer includes:

- durable Spring Modulith event publication registry backed by PostgreSQL
- failed publication resubmission and completed publication cleanup
- typed application events for internal module communication
- global exception handling
- standardized API error response
- stable API error codes
- request trace ID propagation
- MDC-based request logging
- trace ID included in API error responses
- API header constants
- reusable domain preconditions
- shared resource not found exception
- architecture guardrails
- static analysis with SpotBugs
- formatting with Spotless
- code coverage with JaCoCo
- SonarCloud configuration

---

## Engineering Practices

This project intentionally emphasizes engineering discipline.

### Testing

The project uses different test levels depending on the concern:

- domain tests for invariants and behavior
- WebMvc tests for HTTP contracts
- integration tests for application flows
- JPA-backed tests for persistence behavior through application services
- event publication tests where module communication is part of the use case
- integration tests for durable module event publication, completion, and recovery
- architecture tests with ArchUnit and Spring Modulith

### Code Quality

The build includes:

- Spotless for formatting
- SpotBugs for static analysis
- JaCoCo for coverage checks
- SonarCloud configuration
- architecture guardrails to prevent layer violations

### API Consistency

API errors follow a standardized response structure including:

- timestamp
- HTTP status
- error reason
- stable error code
- human-readable message
- request path
- trace ID

This makes the API easier to consume and easier to debug.

---

## Tech Stack

- Java 25
- Spring Boot
- Spring Modulith
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- H2 for tests
- Liquibase
- JJWT
- JUnit 5
- Testcontainers
- Mockito
- AssertJ
- ArchUnit
- Spotless
- SpotBugs
- JaCoCo
- SonarCloud
- Maven

---

## Design Decisions

### Modular Monolith First

The project starts as a modular monolith instead of microservices.

This allows faster development and simpler operations while still enforcing module boundaries. The goal is to keep the codebase modular enough that some modules could later evolve into independent services if there is a real business or scalability need.

### Pragmatic Ports and Adapters

The project uses ports where there is a meaningful boundary, such as persistence, token generation, messaging, or cross-module event consumption.

It avoids creating interfaces for every service when there is only one implementation and no real volatility. This keeps the codebase simpler while preserving the most important architectural benefits.

### Rich Domain Model

Domain entities are responsible for protecting their own invariants.

For example:

- `Product` validates tenant ownership, name, price, and active state transitions.
- `Order` controls valid lifecycle transitions and lifecycle timestamps.
- `Payment` controls whether a payment can be created as paid or pending and protects paid timestamp invariants.

### Tenant Isolation

Tenant isolation is enforced through tenant-scoped queries and database constraints.

Entities store `tenantId` directly instead of using cross-module JPA relationships where that would increase coupling between modules.

### Stateless Authentication

Authentication is based on JWT and stateless Spring Security configuration.

The current token contract includes user identity and role. Tenant context is still handled separately and can evolve later depending on authorization requirements.

### Event-Driven Module Communication

The project uses typed Spring application events between modules when communication represents an asynchronous business fact. Spring Modulith records durable listener publications in the same database transaction as the business change.

Current event flow:

```text
Orders
  -> order.created
  -> Kitchen creates a preparation ticket
  -> Payments registers the initial payment

Kitchen
  -> kitchen.ticket.ready
  -> Fulfillment handles preparation readiness

Fulfillment
  -> fulfillment.order.ready
  -> Orders marks the order as ready
```

Business modules expose local application commands and typed events. Consumers use durable `@ApplicationModuleListener` handlers, keeping internal communication in-process while preserving transactional publication and recovery.

### Durable Module Event Publication

Spring Modulith stores listener publications in the `event_publication` registry. Completed publications can be cleaned up and failed or incomplete publications can be resubmitted by the platform maintenance service.

Maintenance is controlled by:

```yaml
oven:
  events:
    publication:
      maintenance:
        enabled: true
        fixed-delay: 1m
        retry-min-age: 30s
        retry-max-attempts: 5
        completed-retention: 7d
```

This is the only internal durable event mechanism. The custom `outbox_events` table and broker-specific producer/consumer infrastructure are not part of the modular monolith.

### Local Development

The tracked local Compose setup provides:

- PostgreSQL on port `5432`

No message broker is required to run the application locally.

---

## Roadmap

The next planned steps are:

### Orders

- Status filtering
- Pagination
- Kitchen queue views
- Delivery queue views
- Payment summary in order responses or operational read models

### Payments

- Mark pending payment as paid
- Expose payment read operations
- Payment summary for operational screens
- Payment reconciliation foundations
- External payment providers later

### Platform / Distributed Systems Readiness

The project will continue evolving its internal reliability with:

- domain events
- asynchronous processing
- retry strategies
- idempotency
- batch processing
- eventual consistency scenarios

An external broker or integration outbox will only be introduced by a dedicated integration issue when a real boundary such as iFood, 99, or a payment provider requires it. That external mechanism must not duplicate Spring Modulith's internal module communication.

---

## Why this project matters

Oven Platform is not just a CRUD project.

It is a practical backend architecture project focused on the kinds of decisions expected from senior backend engineers:

- how to structure modules
- how to isolate infrastructure
- how to enforce tenant boundaries
- how to model domain behavior
- how to design API contracts
- how to keep tests valuable
- how to introduce quality gates
- how to communicate between modules without unnecessary coupling
- how to prepare a system for asynchronous and distributed workflows

The project is intentionally built step by step, with small pull requests, clear issue scopes, and production-oriented engineering practices.

---

## Status

This project is actively under development.

Implemented foundations:

- tenant management
- identity foundation
- JWT authentication
- catalog domain and persistence
- order creation, lookup, listing, and operational lifecycle
- order payment information capture
- durable order-created application event publication
- kitchen ticket creation from order-created events
- ticket readiness and fulfillment readiness events
- order readiness from fulfillment events
- initial payment registration from order-created events
- durable event publication recovery and cleanup
- API error contract
- request tracing and logging
- architecture guardrails
- static analysis and formatting
- code coverage gates

Planned / upcoming:

- marking pending payments as paid
- payment read operations
- order payment summary
- pagination and filtering
- operational queue views
- stronger asynchronous reliability patterns
- broader idempotency coverage
