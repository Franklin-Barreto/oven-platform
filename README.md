# Oven Platform

Oven Platform is a multi-tenant backend platform for restaurant and pizzeria operations.

The project is being built as a modular monolith using Java and Spring Boot, with a strong focus on clean boundaries, tenant isolation, rich domain modeling, automated tests, and production-oriented engineering practices.

The current MVP focuses on the foundations required for a SaaS platform:

- tenant management
- identity and authentication
- JWT-based security
- product catalog
- order management
- platform-level observability and API consistency

Beyond the business domain, this project is also intended to demonstrate backend architecture decisions commonly required in product companies: modular design, ports and adapters, API contracts, persistence boundaries, automated quality checks, and future readiness for asynchronous processing.

---

## Goals

The main goals of this project are:

- Build a realistic SaaS backend using a modular monolith approach
- Keep business modules isolated and tenant-aware
- Model domain entities with explicit invariants and behavior
- Avoid anemic domain models where business rules belong in the domain
- Use a pragmatic ports-and-adapters architecture
- Keep infrastructure details isolated from application use cases
- Provide a strong foundation for future event-driven and distributed-system patterns
- Maintain production-oriented quality through tests, static analysis, architecture rules, and CI

---

## Architecture

Oven Platform follows a pragmatic ports-and-adapters style inside a Spring Modulith modular monolith.

The project is not intended to be a dogmatic Clean Architecture implementation. Instead, it focuses on controlling dependency direction and isolating important boundaries without adding unnecessary abstractions.

At a high level:

```text
HTTP / Security / Persistence / External Tools
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
└── shared
    ├── domain
    ├── config
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
- Liquibase migration for products
- product persistence boundary
- product repository port
- JPA repository adapter
- minimal `CatalogService`
- integration tests validating product persistence and tenant isolation

### Platform and Cross-Cutting Concerns

The shared platform layer includes:

- global exception handling
- standardized API error response
- stable API error codes
- request trace ID propagation
- MDC-based request logging
- trace ID included in API error responses
- API header constants
- reusable domain preconditions
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

The project uses ports where there is a meaningful boundary, such as persistence or token generation.

It avoids creating interfaces for every service when there is only one implementation and no real volatility. This keeps the codebase simpler while preserving the most important architectural benefits.

### Rich Domain Model

Domain entities are responsible for protecting their own invariants.

For example, `Product` validates tenant ownership, name, price, and active state transitions through domain behavior instead of relying only on external validation annotations.

### Tenant Isolation

Tenant isolation is enforced through tenant-scoped queries and database constraints.

Entities store `tenantId` directly instead of using cross-module JPA relationships where that would increase coupling between modules.

### Stateless Authentication

Authentication is based on JWT and stateless Spring Security configuration.

The current token contract includes user identity and role. Tenant context is still handled separately and can evolve later depending on authorization requirements.

---

## Roadmap

The next planned steps are:

### Catalog

- Product creation API
- Product listing API
- Product update operations
- Product activation/deactivation endpoints

### Orders

- Order aggregate
- Order item entity
- Order repository
- Create order flow
- Add item to order
- Change order status
- List orders by tenant
- Price snapshots for order items

### Distributed Systems Readiness

After the order flow is in place, the project will be extended with patterns commonly used in distributed systems:

- domain events
- outbox pattern
- asynchronous processing
- retry strategies
- idempotency
- dead-letter handling
- batch processing
- eventual consistency scenarios

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
- how to prepare a system for asynchronous and distributed workflows

The project is intentionally built step by step, with small pull requests, clear issue scopes, and production-oriented engineering practices.

---

## Status

This project is actively under development.

Completed foundations:

- tenant management
- identity foundation
- JWT authentication
- API error contract
- request tracing and logging
- architecture guardrails
- static analysis and formatting
- product domain model
- product persistence and catalog application service

In progress / planned:

- catalog APIs
- order management MVP
- asynchronous processing patterns
- batch and idempotency flows
