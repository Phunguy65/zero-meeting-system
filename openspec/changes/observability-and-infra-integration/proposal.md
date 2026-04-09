# Why

The user-management service currently has no structured observability, no
dynamic configuration, and its infrastructure dependencies (Kafka, cache) are
only wired for local Docker Compose. Before adding more features, we need a
solid operational foundation: centralised log aggregation with directional
tracing, service discovery, dynamic config from Consul, a pre-configured Valkey
cache layer, and production-ready K8s manifests for all backing services.

## What Changes

- Add shared AOP-based HTTP + service-layer logging (directional `-->` / `<--`
  symbols) to `services/shared`, reusable by all services
- Add structured JSON logging (Spring Boot 4 native ECS format) via
  `logback-spring.xml` with `local` and `k8s` profiles
- Deploy PLG stack (Promtail DaemonSet, Loki StatefulSet, Grafana Deployment) to
  K8s
- Add Spring Cloud Consul 5.0.x (release train 2025.1.x / Oakwood) for service
  discovery and YAML-format KV config
- Add Valkey (Redis-compatible) dependency and `CacheConfig` — use cases
  deferred, infrastructure ready
- Add Spring Cloud BOM (`2025.1.1`) to `build-logic` service base convention
  plugin
- Create K8s manifests under `services/k8s/` for: Kafka (Strimzi), Valkey,
  Consul, Promtail, Loki, Grafana
- Update `libs.versions.toml` with all new dependency entries

## Capabilities

### New Capabilities

- `shared-logging`: Shared AOP logging module in `services/shared` —
  `HttpLoggingFilter` (directional HTTP logs with MDC), `LoggingAspect` (@Around
  all `@Service` beans), `LoggingAutoConfiguration` (Spring Boot auto-config
  SPI)
- `plg-stack`: Promtail + Loki + Grafana K8s deployment for log aggregation and
  visualisation
- `consul-integration`: Spring Cloud Consul service discovery and YAML KV config
  for user-management (and all services via shared convention plugin)
- `valkey-integration`: Valkey (Redis-compatible) K8s deployment + Spring Data
  Redis wiring in shared config, ready for use-case-specific caching

### Modified Capabilities

<!-- none — no existing spec-level requirements are changing -->

## Impact

- `build-logic/src/main/kotlin/io.github.phunguy65.zms.plugin.service.base.gradle.kts`:
  add Spring Cloud BOM import, `spring-boot-starter-aop`,
  `spring-boot-starter-data-redis`, `spring-cloud-starter-consul-discovery`,
  `spring-cloud-starter-consul-config`
- `gradle/libs.versions.toml`: add `springCloud`, `spring-cloud-*`,
  `spring-boot-starter-data-redis` entries
- `services/shared/src/main/java/.../infrastructure/logging/`: 3 new classes
- `services/user-management/src/main/resources/logback-spring.xml`: new file
- `services/user-management/src/main/resources/application.properties`: add
  Consul, Valkey, logging config
- `services/k8s/`: new directory with Helm values / raw manifests for all
  backing services
- No breaking changes to existing APIs or domain model
