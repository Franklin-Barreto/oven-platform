# Local observability

Oven Platform exposes application metrics and distributed traces through Spring Boot Actuator,
Micrometer, and OpenTelemetry. The local observability stack stores metrics in Prometheus, stores
traces in Tempo, and uses Grafana to explore both signals.

```text
Oven Platform
  -> /actuator/prometheus -> Prometheus -> Grafana dashboards
  -> OTLP/HTTP -> Tempo -> Grafana Explore
```

Prometheus, Tempo, and Grafana are optional operational dependencies. Trace export is asynchronous:
Tempo being unavailable may produce exporter warnings, but must not block application requests or
change business outcomes. Metrics continue to use Prometheus; the OTLP metrics exporter is disabled.

## Start the local environment

Create the local environment file once:

```shell
cp .env.example .env
```

Export the variables for the Spring Boot process and start the containers:

```shell
set -a
source .env
set +a
docker compose up -d
```

Start the application from another terminal with the same exported environment:

```shell
set -a
source .env
set +a
./mvnw spring-boot:run
```

The Prometheus target can be temporarily `DOWN` while the application is starting. Prometheus
automatically resumes scraping after the application becomes available.

## Local endpoints

| Component | URL | Purpose |
| --- | --- | --- |
| Application health | `http://localhost:8080/actuator/health` | Application and dependency health |
| Application info | `http://localhost:8080/actuator/info` | Maven build metadata |
| Prometheus exposition | `http://localhost:8080/actuator/prometheus` | Metrics scraping endpoint |
| Prometheus | `http://localhost:9090` | PromQL queries |
| Prometheus targets | `http://localhost:9090/targets` | Scrape status and errors |
| Grafana | `http://localhost:3000` | Dashboards and metric exploration |
| Tempo API | `http://localhost:3200` | Local trace storage and query API |
| OTLP/HTTP receiver | `http://localhost:4318/v1/traces` | Application trace ingestion |

Grafana uses `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD` from `.env`. The values in
`.env.example` are local defaults only and must not be reused outside local development.

The provisioned dashboards are available under the `Oven Platform` folder:

| Dashboard | Purpose | Source |
| --- | --- | --- |
| `Oven Platform Overview` | Application and runtime health | `observability/grafana/dashboards/platform-overview.json` |
| `Order Creation Overview` | Order creation throughput, outcomes, latency, and HTTP status distribution | `observability/grafana/dashboards/order-creation-overview.json` |

## Stop and reset

Stop the containers while preserving their data:

```shell
docker compose down
```

Remove the containers and all local Prometheus, Tempo, and Grafana data:

```shell
docker compose down --volumes
```

The second command is destructive. Grafana recreates its datasource and dashboards from the
version-controlled provisioning files the next time it starts.

## Distributed tracing

Spring Boot creates the server span for each HTTP request and exports sampled traces through
OpenTelemetry OTLP/HTTP. Spring Modulith adds spans for application-module invocations and event
listeners, making the order-creation flow visible without custom tracing code:

```text
POST /orders
  -> Orders.create
     -> Kitchen event listener
     -> Payment event listener
```

Open Grafana, select **Explore**, and choose the `Tempo` datasource. `{}` finds recent traces. Useful
TraceQL filters include:

```traceql
{ resource.service.name = "oven-platform" }
{ name = "http post /orders" }
{ status = error }
```

Select a result to inspect its spans, duration, status, attributes, and events. Copy its trace ID to
correlate the request with application logs. Spring Boot puts the current trace and span identifiers
in its supported logging context; centralized log storage and a direct Grafana logs-to-traces link
are outside the local stack. W3C `traceparent` is the propagation contract. The application does not
generate or accept a custom `X-Trace-Id` correlation contract.

The `Orders.create` span covers validation, persistence, event publication, and the surrounding
transaction boundary. The current stack does not emit separate JDBC/Hibernate spans, so it cannot
attribute part of that duration precisely to SQL. Add supported database instrumentation in a
separate change before using traces to make database-level latency claims; a span merely wrapped
around `repository.save` would be misleading because Hibernate may defer SQL until flush or commit.

### Asynchronous events and retries

Kitchen and Payment consume the order-created event asynchronously. Their spans may outlive the HTTP
response and run concurrently, so child durations must not be added to obtain the request duration.
Context propagated during immediate event delivery can keep these listeners in the originating
trace. A later retry or recovery may start a different trace when the original context is no longer
available. Trace IDs are operational metadata and must not be persisted in business events merely
to force both executions into the same trace.

### Sampling and privacy

`OVEN_TRACING_SAMPLING_PROBABILITY` controls the probability that a trace is sampled. The local
default is `1.0` so every request can be studied. Production must choose a lower value based on
traffic, storage capacity, incident-response needs, and privacy requirements rather than inheriting
the local default.

Use stable, low-cardinality span names such as normalized HTTP routes, module names, and operations.
Do not add request or response bodies, secrets, tokens, exception messages containing user input,
or tenant, order, customer, user, payment, and delivery identifiers as span attributes. In
particular, `orderId` is excluded by default; add identifiers only after an explicit operational and
privacy review.

The API error response uses the active Micrometer trace when one exists. If no span is active, its
`traceId` is `null`; the application does not invent a fallback identifier that Tempo cannot resolve.

### Trace configuration

| Variable | Local default | Purpose |
| --- | --- | --- |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP/HTTP trace destination |
| `OVEN_TRACING_SAMPLING_PROBABILITY` | `1.0` | Fraction of traces sampled, from `0.0` to `1.0` |

After changing either variable, restart the Spring Boot application. Restarting only the Compose
services does not reload application tracing configuration.

## Metric conventions

- Use lowercase dot notation in application code, for example `oven.orders.creation.successes`.
- Use base units and let the registry apply backend naming conventions.
- Use counters for totals, timers for duration and frequency, and gauges for current state.
- Keep tag names stable and values bounded.
- Use the common `application` and `environment` tags for portable platform filtering.
- Do not encode identifiers or other variable data in metric names.

The following values are forbidden as metric tags because they are sensitive or have unbounded
cardinality:

- tenant, order, customer, user, payment, or delivery identifiers;
- email addresses, phone numbers, or customer-provided values;
- raw URLs, request parameters, exception messages, or free-form text.

Prefer bounded dimensions such as operation, outcome, HTTP method, normalized route, status class,
service type, and deployment environment.

## Durable event publication metrics

Spring Modulith persists one durable publication for each transactional module listener. A business
transaction can therefore commit successfully while Kitchen, Payment, Orders, or Fulfillment work
remains pending. HTTP metrics describe the committed request, while the publication metrics below
describe the current asynchronous backlog and the executions that maintain it.

| Micrometer name | Prometheus series | Meaning |
| --- | --- | --- |
| `oven.events.publications.incomplete` | `oven_events_publications_incomplete` | Publications without a completion date |
| `oven.events.publications.failed` | `oven_events_publications_failed` | Incomplete publications currently in the Modulith `FAILED` state |
| `oven.events.publications.oldest.incomplete.age` | `oven_events_publications_oldest_incomplete_age_seconds` | Age in seconds of the oldest incomplete publication |
| `oven.events.publications.resubmissions` | `oven_events_publications_resubmissions_total{result="success|failure"}` | Scheduled resubmission executions by outcome |
| `oven.events.publications.cleanup` | `oven_events_publications_cleanup_total{result="success|failure"}` | Scheduled completed-publication cleanup executions by outcome |

The gauges have no event, listener, aggregate, order, or tenant labels. Maintenance counters use
only the bounded `result=success|failure` tag in addition to the global application and environment
tags. No event payload is read or exported.

An incomplete publication is any row whose `completion_date` is null, regardless of whether its
current status is `PUBLISHED`, `PROCESSING`, `FAILED`, or `RESUBMITTED`. The failed gauge is a subset
of the incomplete gauge. When there is no backlog, both the incomplete count and oldest age are
zero. A zero age is an operational convention that avoids confusing an empty backlog with missing
Prometheus data.

### Spring Modulith 2.0.3 measurement boundaries

The project uses Spring Modulith 2.0.3 with the JPA event publication repository and completion mode
`update`. The available framework APIs have the following boundaries:

- `EventPublicationRegistry.findIncompletePublications()` returns the correct incomplete set, but
  materializes every publication and can deserialize payloads. Its cost grows with the backlog.
- `EventPublicationRepository.countByStatus(Status)` is implemented efficiently by the current JPA
  repository, but it cannot calculate the oldest publication and counting every non-completed
  state would require several queries plus an exhaustive status list.
- the JPA schema exposes a reliable `FAILED` state, so the failed gauge is measured directly;
- `FailedEventPublications.resubmit(...)` and
  `CompletedEventPublications.deletePublicationsOlderThan(...)` return `void`. A successful counter
  increment therefore means that the maintenance call returned without an exception. It does not
  claim how many publications were recovered or deleted;
- the framework does not expose an accurate deleted-publication count, so
  `oven.events.publications.completed.deleted` is deliberately not published.

The framework's open [listener invocation metrics proposal](https://github.com/spring-projects/spring-modulith/issues/1076)
counts listener success and error invocations. That is complementary to registry gauges: invocation
counters describe historical attempts, while backlog and age describe durable work that is still
pending. Re-evaluate the custom maintenance counters when a stable upstream implementation becomes
available in a future Modulith upgrade.

### Snapshot query and collection cost

The framework does not provide one aggregate API for backlog count, failed count, and oldest
publication date. `JdbcPublicationBacklogReader` therefore performs one isolated, read-only query
against the Liquibase-owned `event_publication` table:

```sql
select count(*) as incomplete_count,
       count(*) filter (where status = 'FAILED') as failed_count,
       min(publication_date) as oldest_publication_date
from event_publication
where completion_date is null;
```

The query projects only aggregate values and never selects `serialized_event`. PostgreSQL can use
the existing completion-date index to locate incomplete rows, but an exact count necessarily visits
the matching backlog entries. The application runs this query once per configured monitoring
interval (one minute by default), stores one immutable snapshot in memory, and serves Prometheus
scrapes from that snapshot. It does not execute SQL once per gauge or once per scrape.

If snapshot collection fails, the monitor logs the failure and preserves the last successful
snapshot. It never changes publication state and runs independently from retry and cleanup. On a
fresh process that has not completed a successful collection yet, gauges retain their initial zero
values; application logs must be checked when the database or collection path is unhealthy.

Configure the collection interval with:

```yaml
oven:
  events:
    publication:
      monitoring:
        fixed-delay: 1m
```

### Expected healthy local behavior

With no outstanding listener work, the three gauges are zero. When maintenance is enabled, the
successful resubmission and cleanup counters normally increase once per maintenance execution even
when there are no eligible rows; they count executions, not affected publications. Failure counters
remain unchanged.

Inspect the exposed series locally:

```shell
curl --silent http://localhost:8080/actuator/prometheus \
  | rg 'oven_events_publications_(incomplete|failed|oldest|resubmissions|cleanup)'
```

Useful PromQL checks before the Grafana dashboard is available:

```promql
oven_events_publications_incomplete
oven_events_publications_failed
oven_events_publications_oldest_incomplete_age_seconds
sum by (result) (rate(oven_events_publications_resubmissions_total[5m]))
sum by (result) (rate(oven_events_publications_cleanup_total[5m]))
```

### Investigate a growing backlog

1. Confirm that the Prometheus target is `UP` and check application logs for snapshot collection,
   listener, resubmission, or database failures.
2. Compare incomplete count, failed count, and oldest age. A growing age confirms that at least one
   publication is not recovering; a growing count shows new work accumulating as well.
3. Inspect bounded registry state without selecting payloads:

   ```sql
   select status, count(*), min(publication_date) as oldest
   from event_publication
   where completion_date is null
   group by status
   order by status;
   ```

4. Correlate the affected time window with listener logs and Modulith traces. A failed count of zero
   does not prove health: publications can be stuck in another incomplete state.
5. Verify `oven.events.publication.maintenance` settings and whether resubmission failure counters
   are increasing. Confirm database connectivity before restarting the application.
6. Do not edit or replay registry rows manually. Preserve payload privacy and use the configured
   recovery path. Escalate persistent Kitchen or Payment gaps as business-impacting incidents.

Alert thresholds and the specific order-to-Kitchen one-minute SLA remain separate follow-up work.
They require a bounded business-flow signal and notification routing; the generic registry metrics
implemented here are intentionally alert-ready but do not define paging policy.

## Histograms, SLOs, and percentiles

Histogram buckets and percentiles have a storage and query cost. Do not enable them globally.
Configure them only for metrics queried by percentile or measured against an explicit service-level
objective.

The platform currently publishes bounded SLO buckets for `http.server.requests`. The overview
dashboard calculates p95 latency from those Prometheus histogram buckets. Add or change boundaries
only when the expected latency or an explicit SLO justifies the additional series.

The `oven.orders.creation` timer publishes a percentile histogram scoped to the order creation
business operation. Its expected range is 1 ms to 5 s. This produces the
`oven_orders_creation_seconds_bucket` series used to calculate p50, p95, and p99 in Prometheus.

## Order creation metrics

Orders publishes a small set of aggregate, low-cardinality business metrics:

| Micrometer name | Prometheus series | Meaning |
| --- | --- | --- |
| `oven.orders.creation.successes` | `oven_orders_creation_successes_total` | Orders successfully created and committed |
| `oven.orders.creation.failures` | `oven_orders_creation_failures_total` | Creation attempts that reached the application operation and failed |
| `oven.orders.creation` | `oven_orders_creation_seconds_count` | Total creation attempts that reached the application operation |
| `oven.orders.creation` | `oven_orders_creation_seconds_sum` | Accumulated application-operation duration in seconds |
| `oven.orders.creation` | `oven_orders_creation_seconds_bucket` | Histogram buckets for aggregable latency percentiles |

These metrics use only the global bounded `application` and `environment` tags. They do not expose
tenant, order, product, customer, request, address, exception, or validation-message values as
labels.

The creation timer measures the transactional `OrderService.createOrder` operation. It includes
application validation, persistence, and publication of the order-created application event. A
successful count is recorded only after the transactional invocation returns successfully, which
means its transaction committed. A thrown exception records one failure and one timer observation;
it does not record a success. Event listeners do not increment these creation counters, preventing
one order from being counted again while downstream work is processed.

Business outcome and HTTP outcome intentionally describe different boundaries:

- `http.server.requests` measures the complete transport request, including the security and MVC
  layers, request validation, serialization, and the HTTP response status;
- `oven.orders.creation` measures only the application-level creation operation after it is
  reached;
- a request rejected before `OrderService.createOrder` is invoked appears in HTTP metrics but not
  in the business attempt or failure metrics;
- if the order transaction commits but writing the HTTP response later fails, the order remains a
  successful business creation even though the client may observe a transport failure.

The `Order Creation Overview` dashboard shows:

- attempt, success, and failure rates per minute over time;
- successful and failed totals plus the success ratio for the selected dashboard range;
- average, p50, p95, and p99 application-operation latency;
- HTTP status distribution for `POST /orders`.

A fresh environment without order traffic displays no data until Prometheus observes the relevant
series. This is distinct from a measured value of zero.

## Validate the order creation dashboard locally

Start the application, Prometheus, and Grafana as described above. With a tenant that has at least
one active product, export the credentials used for local testing:

```shell
export TENANT_ID=<tenant-uuid>
export EMAIL=<owner-email>
export PASSWORD=<owner-password>
```

Authenticate and select an active product:

```shell
TOKEN="$(curl --silent --show-error \
  --header 'Content-Type: application/json' \
  --data "{\"tenantId\":\"$TENANT_ID\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  http://localhost:8080/auth/login | jq --raw-output '.token')"

PRODUCT_ID="$(curl --silent --show-error \
  --header 'X-API-Version: 1.0' \
  --header "Authorization: Bearer $TOKEN" \
  http://localhost:8080/products | jq --raw-output '.[0].id')"
```

Create successful orders:

```shell
for attempt in $(seq 1 25); do
  curl --silent --show-error --output /dev/null \
    --header 'Content-Type: application/json' \
    --header 'X-API-Version: 1.0' \
    --header "Authorization: Bearer $TOKEN" \
    --data "{\"serviceType\":\"COUNTER\",\"items\":[{\"productId\":\"$PRODUCT_ID\",\"quantity\":1}],\"paymentInfo\":{\"method\":\"CASH\",\"status\":\"PAID\"}}" \
    http://localhost:8080/orders
done
```

Send one application-level failure by using an unavailable product:

```shell
curl --silent --show-error \
  --header 'Content-Type: application/json' \
  --header 'X-API-Version: 1.0' \
  --header "Authorization: Bearer $TOKEN" \
  --data '{"serviceType":"COUNTER","items":[{"productId":"00000000-0000-0000-0000-000000000001","quantity":1}],"paymentInfo":{"method":"CASH","status":"PAID"}}' \
  http://localhost:8080/orders
```

Confirm that the application exposes the business metrics:

```shell
curl --silent http://localhost:8080/actuator/prometheus | rg 'oven_orders_creation'
```

Wait for at least one 15-second Prometheus scrape, then open `Order Creation Overview` in Grafana.
Change the dashboard time range to verify that totals follow the selected range while the rate and
latency panels continue to show their evolution over time.

## Local and production boundaries

The local profile exposes `health`, `info`, and `prometheus` on the application port so the local
Compose stack can scrape the application without extra credentials. Diagnostic endpoints such as
`metrics`, `env`, `beans`, `heapdump`, and `threaddump` are not exposed.

A production deployment must not publish the management surface directly to the public internet.
It must provide all of the following at the infrastructure boundary:

- an internal management network, private ingress, or separate management port;
- TLS in transit;
- authenticated or network-restricted Prometheus access;
- access control for Grafana and non-public credentials;
- health details limited to information suitable for the intended audience.

Production must set `OVEN_ENVIRONMENT` explicitly. Production authentication, TLS, ingress,
network policies, long-term metric retention, and infrastructure deployment are outside the local
Compose scope.

## Troubleshooting

- If the Grafana dashboard shows `DOWN`, inspect `http://localhost:9090/targets` first.
- If Prometheus cannot reach the application, confirm it is listening on port `8080` and that
  `host.docker.internal` resolves inside the container.
- If Grafana has no datasource or dashboard, inspect its logs with `docker compose logs grafana`
  and verify the files under `observability/grafana/provisioning`.
- If HTTP metrics are absent, send at least one request to the application and wait for the next
  scrape interval.
- If the Tempo datasource is absent, verify the datasource provisioning file and restart Grafana.
- If Tempo returns no traces, restart the application after exporting `.env`, send a request, and
  inspect `docker compose logs tempo` plus the application logs for OTLP export errors.
- If a trace has no Kitchen or Payment span, allow asynchronous processing to finish and check for a
  separate retry trace when the event was recovered after its original context was lost.
