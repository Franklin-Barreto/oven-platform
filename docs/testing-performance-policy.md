# Testing performance policy

## Objective

Keep the Maven test build fast without reducing coverage, isolation, determinism, or quality
gates. `./mvnw clean verify` remains the canonical validation command.

## Baseline

The issue 235 baseline and retained result were measured on the same development machine with
Hyperfine:

```bash
hyperfine --warmup 1 --runs 3 './mvnw -q test'
hyperfine --warmup 1 --runs 3 './mvnw -q clean verify'
```

The test lifecycle decreased from 125.556 seconds to 53.905 seconds while preserving all 752
tests, a reduction of approximately 57%. Absolute durations vary by machine; compare equivalent
commands under equivalent conditions.

## Optimization record

| Experiment | Before | After | Decision |
|---|---:|---:|---|
| Reuse the Catalog application context | 30.217 s | 20.376 s | Retained |
| Reuse the Data JPA PostgreSQL container | 125.556 s | 101.902 s | Retained |
| Reuse the global PostgreSQL container | 101.902 s | 81.849 s | Retained |
| Run test classes with two workers | 81.849 s | 62.350 s | Retained |
| Run test classes with three workers | 62.350 s | 57.004 s | Retained |
| Run test classes with four workers | 57.004 s | 56.939 s | Rejected: no meaningful gain |
| Skip Spotless and SpotBugs during `test` | 59.874 s | 56.162 s | Rejected: noisy result and delayed feedback |

Durations are Hyperfine means. Each comparison used at least three runs after one warm-up; the
Spotless and SpotBugs experiment used five runs because its expected difference was smaller.

The retained changes reuse compatible Spring contexts, reuse bounded PostgreSQL containers,
disable unavailable test telemetry exporters, isolate shared-database data, and run safe test
classes with three workers. Four workers did not improve wall-clock time, while moving Spotless or
SpotBugs later in the lifecycle would delay local feedback without improving the canonical
`verify` build.

## Required test structure

- Data JPA integration tests use `DataJpaIntegrationTest`.
- MVC slice tests use `AbstractControllerTest`.
- New full `@SpringBootTest` contexts require an explicit architecture-test exception and a
  measured justification.
- `@DirtiesContext` is not allowed without revisiting this policy and its architecture rule.
- PostgreSQL containers are created only by the shared configuration or documented isolated tests.
- Shared-database tests must isolate data by tenant and must not depend on execution order.
- Test classes run with the bounded worker count configured in `junit-platform.properties`.
- Formatting, SpotBugs, JaCoCo, Cucumber, architecture tests, and other quality gates must not be
  skipped to improve build time.

`TestBuildArchitectureTests` enforces the structural rules that can be checked statically.

## Measuring a proposed optimization

1. Record at least three equivalent baseline runs.
2. Change one variable at a time.
3. Repeat the same command and number of runs.
4. Compare mean, standard deviation, and range; overlapping noisy results are not sufficient.
5. Run `./mvnw clean verify`.
6. Run the known random-order seeds and at least one new seed for shared-database changes.
7. Keep the change only when the gain outweighs its complexity and maintenance cost.

Surefire reports identify slow classes and methods. The Maven Profiler identifies slow lifecycle
plugins. Spring context-cache logs identify cache fragmentation. Spring Test Profiler may be used
temporarily with parallel execution disabled, because it does not currently support parallel test
execution.

## Monitoring

The `Test build performance` GitHub Actions workflow runs weekly and on demand. It performs three
canonical builds plus a known random-order run, publishes the measurements in the workflow
summary, and uploads the raw logs and TSV metrics.

The workflow fails when:

- fewer than the baseline 752 tests run;
- the Spring context count differs from the expected 29;
- the number of PostgreSQL starts differs from the expected three per build; or
- known OTLP, Hikari, Surefire shutdown, test, or build failures reappear.

Wall-clock duration is monitored rather than used as a hard gate because shared GitHub-hosted
runners have variable performance. Investigate a sustained increase of 15% or more across multiple
workflow executions.

## Updating the policy

When an intentional change adds tests, contexts, or an isolated container, update the corresponding
baseline only after documenting why reuse is unsafe or inappropriate. Performance regressions must
be measured and reviewed rather than hidden by increasing thresholds.
