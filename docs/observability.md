# Local observability

Oven Platform exposes application metrics through Spring Boot Actuator and Micrometer. The local
observability stack stores those metrics in Prometheus and visualizes them in Grafana.

```text
Oven Platform
  -> /actuator/prometheus
  -> Prometheus
  -> Grafana
  -> Oven Platform Overview dashboard
```

Prometheus and Grafana are optional operational dependencies. They query the application; the
application does not connect to either service and must remain runnable when they are unavailable.

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

Grafana uses `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD` from `.env`. The values in
`.env.example` are local defaults only and must not be reused outside local development.

The provisioned dashboard is available under the `Oven Platform` folder as
`Oven Platform Overview`. Its source is
`observability/grafana/dashboards/platform-overview.json`.

## Stop and reset

Stop the containers while preserving their data:

```shell
docker compose down
```

Remove the containers and all local Prometheus and Grafana data:

```shell
docker compose down --volumes
```

The second command is destructive. Grafana recreates its datasource and dashboards from the
version-controlled provisioning files the next time it starts.

## Metric conventions

- Use lowercase dot notation in application code, for example `oven.orders.created`.
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
