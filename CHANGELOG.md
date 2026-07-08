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
- Order-created integration event publication through the outbox.
- Fulfillment-order-ready consumption to mark orders as ready.
- Kitchen module.
- Kitchen ticket creation from order-created events.
- Kitchen ticket lookup by order id.
- Kitchen ticket preparation workflow.
- Kitchen-ticket-ready integration event publication through the outbox.
- Fulfillment module.
- Kitchen-ticket-ready consumption in Fulfillment.
- Fulfillment-order-ready integration event publication through the outbox.
- Initial Payment module.
- `Payment` aggregate.
- `PaymentMethod` and `PaymentStatus`.
- Payment creation from order-created integration events.
- Payment repository port and JPA adapter.
- Tenant-scoped payment lookup by order.
- Application use case to mark an order payment as paid by tenant and order.
- Application-managed `paidAt` handling using `Clock`.
- Liquibase migration for payments.
- Shared integration event payload contracts.
- Shared outbox domain model and application services.
- Kafka outbox event publisher.
- Scheduled outbox event publisher.
- Configurable outbox publishing enablement and fixed delay.
- Configurable Kafka topics and consumer groups.
- Local Kafka and Kafka UI support through Compose.
- Kafka topic auto-creation support for local/development environments.
- Dead-letter topic creation for configured Kafka topics.
- Configurable Kafka consumer retry interval and max retry count.
- Kafka consumer error handling with `IllegalArgumentException` classified as non-retryable.
- Kafka/Testcontainers coverage for consumer dead-letter handling.
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
- Module communication moved toward shared Kafka integration payloads instead of Spring Modulith application events.
- Payments are created from order-created Kafka events instead of direct API calls.
- Kitchen and Payment consumers now translate shared integration payloads into local application commands.
- Event publisher ports now represent module-specific integration publishing boundaries implemented by outbox adapters.
- Outbox persistence adapters were moved under `shared.infrastructure.outbox.persistence`.
- Business timestamps such as `paidAt`, `readyAt`, `deliveredAt`, and `cancelledAt` are handled by application/domain flows instead of database defaults.
- Web request/response DTOs for Orders were organized under the web DTO package.
- Architecture tests were tightened to prevent `shared` from depending on business modules.

### Fixed

- Prevented invalid order status transitions.
- Prevented repeated status commands from overwriting lifecycle timestamps.
- Prevented paid payments from being created without `paidAt`.
- Prevented pending payments from receiving a paid timestamp during creation.
- Prevented repeated paid-payment commands from overwriting the original `paidAt`.
- Prevented shared integration payload contracts from depending on module-specific domain types.
- Prevented invalid Kafka consumer use-case failures from being retried before dead-letter recovery.

---

## Notes

This changelog starts from the current project state instead of reconstructing every historical pull request individually.

Future entries should keep describing relevant user-facing, architectural, and operational changes at a level useful for maintainers and reviewers.
