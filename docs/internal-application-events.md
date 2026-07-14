# Internal Application Events

Oven Platform uses Spring Modulith application events for asynchronous communication between
modules that belong to the same deployable monolith.

## Publishing and consuming

The publishing module owns the event contract and exposes its event package as a Spring Modulith
named interface.

```text
transactional application service
→ ApplicationEventPublisher
→ typed event from the publishing module
→ @ApplicationModuleListener in the consuming module
→ consumer application service or command
```

Cross-module side effects that must survive process failures use `@ApplicationModuleListener`.
This annotation represents an asynchronous transactional listener and makes the listener eligible
for the persistent Event Publication Registry.

Plain `@EventListener` is not a replacement for this pattern. A synchronous listener joins the
publisher call and can extend or roll back the originating transaction. Use that behavior only when
the coupling is deliberate and documented.

## Transaction semantics

The event publication entry is stored in `event_publication` as part of the originating business
transaction.

```text
originating transaction commits
→ listener runs asynchronously in its own transaction
→ publication becomes COMPLETED

originating transaction rolls back
→ business state and publication both roll back

listener fails
→ publication becomes FAILED and remains available for recovery
```

Events must contain immutable business data and identifiers. Do not place JPA entities, lazy
collections, repositories, or transport-specific JSON payload types in an internal event contract.

## Recovery and retention

The runtime policy is configured in `application.yml`:

- outstanding publications are resubmitted when the application restarts;
- publications stuck in `PUBLISHED`, `PROCESSING`, or `RESUBMITTED` for ten minutes are marked as
  failed by the Spring Modulith staleness monitor;
- failed publications are retried after a minimum age, in bounded batches and with a maximum number
  of attempts;
- completed publications are retained for seven days and then deleted;
- Liquibase owns the `event_publication` schema.

The maintenance scheduler can be disabled for a specialized process or test with:

```yaml
oven:
  events:
    publication:
      maintenance:
        enabled: false
```

Operational inspection can group publications by status:

```sql
SELECT status, count(*)
FROM event_publication
GROUP BY status
ORDER BY status;
```

Repeated failures that reach the configured attempt limit remain in `FAILED` for investigation.

## Idempotency

Durable delivery is at least once. A listener may commit its business transaction and fail before
the registry records completion, so the same event can be delivered again.

Every listener must therefore make its effect idempotent through an aggregate rule, a tenant-scoped
unique constraint, an inbox/idempotency record, or another explicit business guarantee.

## Testing convention

Cross-module event tests must cover:

- successful publication and listener completion;
- listener failure followed by recovery;
- rollback of the originating transaction;
- idempotent redelivery in the consuming module;
- Spring Modulith module-boundary verification.

Kafka payloads, topics, retry policies, and dead-letter handling remain external transport concerns
and must not leak into canonical internal event contracts.
