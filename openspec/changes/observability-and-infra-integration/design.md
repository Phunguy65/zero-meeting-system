# Context

The zero-meeting-system is a Spring Boot 4.0.3 / Java 25 microservices monorepo.
The `user-management` service is the first to reach production-readiness, but it
currently has:

- Default Logback console output (no structured JSON, no correlation IDs, no
  directional tracing)
- No AOP — cross-cutting logging must be added per-service manually
- Kafka wired only for Docker Compose (`localhost:9092`); no K8s manifests exist
- No cache layer (Valkey/Redis)
- No service discovery or dynamic configuration (all URLs are hardcoded
  `localhost` defaults)
- `services/k8s/` directory exists but is empty

The `services/shared` module already provides web infrastructure (JSend,
GlobalExceptionHandler, pagination). It is the natural home for reusable
cross-cutting concerns.

Spring Cloud 2025.1.1 (Oakwood) is confirmed compatible with Spring Boot 4.0.3
and provides Spring Cloud Consul 5.0.x.

## Goals / Non-Goals

**Goals:**

- Structured JSON logging (ECS format) on the `k8s` Spring profile;
  human-readable on `local`
- Directional HTTP log entries (`-->` inbound, `<--` outbound) with MDC
  correlation ID propagation
- AOP service-layer logging (`-->` method entry, `<--` method exit with
  duration) in `shared`, zero boilerplate per service
- PLG stack (Promtail DaemonSet + Loki StatefulSet + Grafana Deployment) K8s
  manifests
- Consul service discovery + YAML KV config for user-management; Spring Cloud
  BOM added to convention plugin so all services inherit it
- Valkey K8s manifest + Spring Data Redis wiring in shared `CacheConfig`;
  use-case-specific caching deferred
- All K8s manifests under `services/k8s/` using Helm values files where
  applicable

**Non-Goals:**

- Distributed tracing (Micrometer Tracing / Tempo) — separate change
- Grafana alerting rules — operational concern, post-deployment
- Valkey use-case caching (`@Cacheable`, session management) — deferred
- meeting-management / chat-management service wiring — follow-on change
- Consul ACLs / mTLS (Consul Connect) — security hardening change

## Decisions

### D1 — Logging placement: `shared/infrastructure/logging/` (not a separate starter module)

The monorepo already uses `services/shared` as a composite build included by
every service. Adding a separate Gradle module for logging would require new
`settings.gradle.kts`, publication config, and version management. Placing the
three logging classes directly in `shared/infrastructure/logging/` with a
`LoggingAutoConfiguration` registered via Spring Boot SPI achieves the same
reusability with zero extra build complexity.

**Alternative considered**: Separate `logging-starter` module. Rejected —
over-engineering for a monorepo where all services already share one build
graph.

### D2 — HTTP logging: `OncePerRequestFilter` over Spring MVC `HandlerInterceptor`

`OncePerRequestFilter` fires at the Servlet layer, before and after the full
filter chain (including Spring Security). This means:

- `-->` log captures the raw request before authentication
- `<--` log captures the final response status after exception handling
- MDC is set early enough for all downstream logs (including security filters)
  to inherit `correlationId`

`HandlerInterceptor` fires after security and cannot log responses from
exception handlers cleanly.

### D3 — AOP pointcut: `@within(org.springframework.stereotype.Service)` (all `@Service` beans)

Targeting all `@Service`-annotated classes means every use case and application
service is automatically covered without any `@Loggable` annotation on
individual methods. This matches the project's convention where all use cases
are `@Service`.

`@RestController` methods are intentionally excluded from the AOP aspect —
HTTP-layer logging is already handled by `HttpLoggingFilter` to avoid
double-logging.

**Alternative considered**: Custom `@Loggable` annotation per method. Rejected —
requires developer discipline and adds boilerplate.

### D4 — Structured logging: Spring Boot 4 native ECS format (no `logstash-logback-encoder`)

Spring Boot 4.x has built-in structured logging support
(`logging.structured.format.console=ecs`). This eliminates the
`logstash-logback-encoder` dependency entirely. The `logback-spring.xml` uses
`<springProfile name="k8s">` to activate JSON output only in K8s, keeping local
development readable.

### D5 — Spring Cloud Consul 5.0.x with release train 2025.1.1 (Oakwood)

Confirmed compatible with Spring Boot 4.0.3. The BOM is added to `service.base`
convention plugin so all services inherit it. Consul config format is YAML
(matches existing `application.properties` migration path).
`spring.config.import=optional:consul:` allows the service to start without
Consul in local dev.

**Alternative considered**: K8s ConfigMap + Spring Cloud Kubernetes. Rejected —
Consul provides both service discovery and dynamic config in one tool, and is
not K8s-specific (supports future hybrid deployments).

### D6 — Valkey: pre-configure infrastructure, defer use cases

Adding `spring-boot-starter-data-redis` + `CacheConfig` now means any service
can add `@Cacheable` without a build change. The `CacheConfig` uses
`@ConditionalOnProperty(name = "spring.data.redis.host")` so it only activates
when Valkey is configured. No `@Cacheable` annotations are added in this change.

### D7 — Kafka K8s: Strimzi operator for production, existing KRaft Compose for local

Strimzi manages Kafka lifecycle via CRDs (rolling updates, rebalancing, TLS).
The existing `compose.yaml` KRaft setup is unchanged for local dev. K8s
manifests use `Kafka` and `KafkaTopic` CRDs.

## Risks / Trade-offs

- **[Risk] Spring Cloud Consul 5.0.x internal HTTP client rewrite** → Test
  service registration and config watch in staging before production rollout.
  The `optional:consul:` import prefix prevents startup failure if Consul is
  unreachable.
- **[Risk] AOP on all `@Service` beans may log sensitive fields (passwords,
  tokens)** → `LoggingAspect` logs method name and class only by default;
  argument logging is opt-in via `logging.aspect.include-args=true` (default
  `false`).
- **[Risk] High log volume in K8s with DEBUG-level AOP** → Default AOP log level
  is `DEBUG`; Promtail/Loki label cardinality is kept low (app, namespace, level
  only). Loki retention policy should be set at deployment time.
- **[Risk] Valkey PVC on self-managed K8s requires StorageClass** → Manifests
  use `storageClassName: standard` (k3s default); override via Helm values for
  other providers.
- **[Trade-off] Consul adds operational overhead** → Mitigated by `consul-k8s`
  Helm chart with sane defaults. K8s health checks are delegated to
  `/actuator/health`.

## Migration Plan

1. Add dependencies to `libs.versions.toml` and `service.base` plugin — all
   services rebuild, no runtime change
2. Add `shared/infrastructure/logging/` classes — auto-configured, no service
   code changes needed
3. Add `logback-spring.xml` to user-management — activates on `k8s` profile only
4. Update `application.properties` with Consul + Valkey config (all `optional:`
   / env-var defaulted)
5. Deploy K8s infrastructure in order: Consul → Valkey → Kafka (Strimzi) → PLG
   stack
6. Deploy user-management with `SPRING_PROFILES_ACTIVE=k8s`

**Rollback**: Remove `SPRING_PROFILES_ACTIVE=k8s` to revert to plain console
logging. Consul config is `optional:` so service starts without it. No database
migrations involved.

## Open Questions

- Loki retention period — to be decided by ops team at deployment time
- Consul KV initial seed — who populates the KV store for first deployment?
  (manual vs GitOps)
- Strimzi version pin — confirm K8s cluster version to select correct Strimzi
  operator version
