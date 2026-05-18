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
- `OrderPlacedEvent` publication after order creation.
- Modulith named interface for the Orders event contract.
- Initial Payment module.
- `Payment` aggregate.
- `PaymentMethod` and `PaymentStatus`.
- Payment creation from `OrderPlacedEvent`.
- Payment repository port and JPA adapter.
- Tenant-scoped payment lookup by order.
- Application-managed `paidAt` handling using `Clock`.
- Liquibase migration for payments.
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
- Payments are created internally from order events instead of direct API calls.
- Business timestamps such as `paidAt`, `readyAt`, `deliveredAt`, and `cancelledAt` are handled by application/domain flows instead of database defaults.
- Web request/response DTOs for Orders were organized under the web DTO package.

### Fixed

- Prevented invalid order status transitions.
- Prevented repeated status commands from overwriting lifecycle timestamps.
- Prevented paid payments from being created without `paidAt`.
- Prevented pending payments from receiving a paid timestamp during creation.

---

## Notes

This changelog starts from the current project state instead of reconstructing every historical pull request individually.

Future entries should keep describing relevant user-facing, architectural, and operational changes at a level useful for maintainers and reviewers.
