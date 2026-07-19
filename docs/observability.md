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
