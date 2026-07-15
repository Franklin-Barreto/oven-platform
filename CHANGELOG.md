# Changelog

All notable changes to this project will be documented in this file.

This project currently does not use formal versioned releases. Changes are grouped under `Unreleased` until a release process is introduced.

---

## Unreleased

### Added

- Initial multi-tenant SaaS foundation.
- Tenant management foundation.
- Identity module with tenant-scoped users.
- BCrypt password hashing.
- JWT generation and validation.
- JWT authentication filter.
- Stateless authentication flow.
- `/auth/login` endpoint.
- Product catalog foundation.
- Rich `Product` domain model with validation and behavior.
- Product persistence through application port and JPA adapter.
- Order aggregate and order item model.
- Tenant-scoped order creation.
- Backend-calculated order totals using catalog prices.
- Order item price snapshots.
- Order lookup by id.
- Order listing by tenant.
- Operational order lifecycle with `CREATED`, `READY`, `DELIVERED`, and `CANCELLED`.
- Idempotent order status transitions.
- Lifecycle timestamps for order readiness, delivery, and cancellation.
- Invalid order transition protection.
- Order creation with required initial payment information.
- Durable order-created application event publication through Spring Modulith.
- Fulfillment-order-ready consumption to mark orders as ready.
- Kitchen module.
- Kitchen ticket creation from order-created events.
- Kitchen ticket lookup by order id.
- Kitchen ticket preparation workflow.
- Durable kitchen-ticket-ready application event publication through Spring Modulith.
- Fulfillment module.
- Kitchen-ticket-ready consumption in Fulfillment.
- Durable fulfillment-order-ready application event publication through Spring Modulith.
- Initial Payment module.
- `Payment` aggregate.
- `PaymentMethod` and `PaymentStatus`.
- Payment creation from order-created integration events.
- Payment repository port and JPA adapter.
- Tenant-scoped payment lookup by order.
- Application use case to mark an order payment as paid by tenant and order.
- Application-managed `paidAt` handling using `Clock`.
- Liquibase migration for payments.
- Typed internal application event contracts.
- PostgreSQL-backed Spring Modulith event publication registry.
- Configurable recovery and retention maintenance for durable event publications.
- Global API error response contract.
- Stable API error codes.
- Shared `ResourceNotFoundException`.
- Request trace ID propagation.
- MDC-based request logging.
- Trace ID included in API error responses.
- Shared API header constants.
- Shared domain preconditions.
- Architecture tests and module boundary guardrails.
- Static analysis with SpotBugs.
- Code formatting with Spotless.
- Code coverage checks with JaCoCo.
- SonarCloud configuration.

### Changed

- Order creation now requires payment information.
- Order totals remain owned by Orders and are not accepted from the frontend.
- Internal module communication uses typed Spring Modulith application events.
- Payments are created from durable order-created application events instead of direct API calls.
- Kitchen and Payment listeners translate publishing-module event contracts into local application commands.
- Business timestamps such as `paidAt`, `readyAt`, `deliveredAt`, and `cancelledAt` are handled by application/domain flows instead of database defaults.
- Web request/response DTOs for Orders were organized under the web DTO package.
- Architecture tests were tightened to prevent `shared` from depending on business modules.

### Fixed

- Prevented invalid order status transitions.
- Prevented repeated status commands from overwriting lifecycle timestamps.
- Prevented paid payments from being created without `paidAt`.
- Prevented pending payments from receiving a paid timestamp during creation.
- Prevented repeated paid-payment commands from overwriting the original `paidAt`.
- Prevented internal event contracts from depending on transport-specific payloads.

### Removed

- Internal Kafka producer, consumer, retry, topic, and test infrastructure.
- Custom internal outbox domain, persistence, scheduler, and publisher infrastructure.
- The legacy `outbox_events` table through a forward-only Liquibase migration.

---

## Notes

This changelog starts from the current project state instead of reconstructing every historical pull request individually.

Future entries should keep describing relevant user-facing, architectural, and operational changes at a level useful for maintainers and reviewers.
