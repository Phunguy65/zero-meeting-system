# Tasks

## 1. Build Configuration

- [x] 1.1 Add `springCloud = "2025.1.1"` version entry to
      `gradle/libs.versions.toml`
- [x] 1.2 Add library entries to `gradle/libs.versions.toml`:
      `spring-cloud-starter-consul-discovery`,
      `spring-cloud-starter-consul-config`, `spring-boot-starter-data-redis`,
      `spring-boot-starter-aop`
- [x] 1.3 Add Spring Cloud BOM import (`spring-cloud-dependencies:2025.1.1`) to
      `build-logic/src/main/kotlin/io.github.phunguy65.zms.plugin.service.base.gradle.kts`
      via `dependencyManagement { imports { mavenBom(...) } }`
- [x] 1.4 Add `spring-boot-starter-aop`, `spring-boot-starter-data-redis`,
      `spring-cloud-starter-consul-discovery`,
      `spring-cloud-starter-consul-config` to the `dependencies` block in
      `service.base` convention plugin
- [x] 1.5 Verify all services build cleanly: `./gradlew build -x test`

## 2. Shared Logging Module

- [x] 2.1 Create
      `services/shared/src/main/java/io/github/phunguy65/zms/shared/infrastructure/logging/HttpLoggingFilter.java`
      — `OncePerRequestFilter` that sets `correlationId` in MDC (from
      `X-Correlation-ID` header or generated UUID), logs `-->` on request entry
      and `<--` on response exit with `method`, `uri`, `status`, `duration_ms`,
      then clears MDC
- [x] 2.2 Create
      `services/shared/src/main/java/io/github/phunguy65/zms/shared/infrastructure/logging/LoggingAspect.java`
      — `@Aspect` with
      `@Around("@within(org.springframework.stereotype.Service)")` pointcut;
      logs `-->` on entry and `<--` on exit (DEBUG level) with `class`,
      `method`, `duration_ms`; logs `<--` at ERROR on exception; respects
      `logging.aspect.include-args` property (default `false`)
- [x] 2.3 Create
      `services/shared/src/main/java/io/github/phunguy65/zms/shared/infrastructure/logging/LoggingAutoConfiguration.java`
      — `@AutoConfiguration` that registers `HttpLoggingFilter` and
      `LoggingAspect` beans; annotate with `@EnableAspectJAutoProxy`
- [x] 2.4 Create
      `services/shared/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
      and add
      `io.github.phunguy65.zms.shared.infrastructure.logging.LoggingAutoConfiguration`
- [x] 2.5 Verify shared module compiles: `./gradlew :shared:compileJava`

## 3. Shared Cache Configuration

- [x] 3.1 Create
      `services/shared/src/main/java/io/github/phunguy65/zms/shared/infrastructure/cache/CacheConfig.java`
      —
      `@Configuration @EnableCaching @ConditionalOnProperty(name = "spring.data.redis.host")`
      that declares a `RedisCacheManager` bean using
      `RedisCacheConfiguration.defaultCacheConfig()`
- [x] 3.2 Verify shared module compiles after cache config addition

## 4. user-management: Structured Logging Configuration

- [x] 4.1 Create
      `services/user-management/src/main/resources/logback-spring.xml` with two
      `<springProfile>` blocks: `local` (default Spring Boot console pattern)
      and `k8s` (`logging.structured.format.console=ecs` equivalent — use
      `<springProperty>` to activate ECS JSON appender)
- [x] 4.2 Add logging properties to
      `services/user-management/src/main/resources/application.properties`:
      `logging.aspect.include-args=false`, `logging.structured.format.console`
      conditional on profile

## 5. user-management: Consul Integration

- [x] 5.1 Add Consul connection and discovery properties to
      `application.properties`:
      `spring.cloud.consul.host=${CONSUL_HOST:localhost}`,
      `spring.cloud.consul.port=${CONSUL_PORT:8500}`,
      `spring.cloud.consul.discovery.enabled=true`,
      `spring.cloud.consul.discovery.prefer-ip-address=true`,
      `spring.cloud.consul.discovery.health-check-path=/actuator/health`,
      `spring.cloud.consul.discovery.health-check-interval=10s`
- [x] 5.2 Add Consul config properties to `application.properties`:
      `spring.cloud.consul.config.enabled=true`,
      `spring.cloud.consul.config.prefix=config`,
      `spring.cloud.consul.config.format=yaml`,
      `spring.cloud.consul.config.watch.enabled=true`,
      `spring.config.import=optional:consul:`
- [ ] 5.3 Verify user-management starts locally without Consul (should start
      with warnings, not errors): `./gradlew :user-management:bootRun`

## 6. user-management: Valkey Configuration

- [x] 6.1 Add Valkey connection properties to `application.properties`:
      `spring.data.redis.host=${REDIS_HOST:localhost}`,
      `spring.data.redis.port=${REDIS_PORT:6379}`,
      `spring.data.redis.password=${REDIS_PASSWORD:}`
- [ ] 6.2 Verify `CacheConfig` bean is absent when `REDIS_HOST` is not set
      (local dev default)

## 7. K8s Manifests — Consul

- [x] 7.1 Create `services/k8s/consul/values.yaml` for `hashicorp/consul` Helm
      chart with: `server.replicas=1` (dev) or `3` (prod),
      `connectInject.enabled=false`, `syncCatalog.enabled=true`,
      `ui.enabled=true`
- [x] 7.2 Create `services/k8s/consul/README.md` with install command:
      `helm install consul hashicorp/consul -n consul --create-namespace -f values.yaml`

## 8. K8s Manifests — Valkey

- [x] 8.1 Create `services/k8s/valkey/statefulset.yaml` — `StatefulSet` with
      `bitnami/valkey` image, 1 replica, PVC `1Gi`, `storageClassName: standard`
- [x] 8.2 Create `services/k8s/valkey/service.yaml` — `ClusterIP` Service on
      port `6379`

## 9. K8s Manifests — Kafka (Strimzi)

- [x] 9.1 Create `services/k8s/kafka/strimzi-operator.yaml` — reference to
      Strimzi operator install (or Helm values for
      `strimzi/strimzi-kafka-operator`)
- [x] 9.2 Create `services/k8s/kafka/kafka-cluster.yaml` — `Kafka` CRD with
      KRaft mode, 1 broker (dev) / 3 brokers (prod), `ephemeral` storage for dev
- [x] 9.3 Create `services/k8s/kafka/kafka-topics.yaml` — `KafkaTopic` CRDs for
      existing user-management topics (user events)

## 10. K8s Manifests — PLG Stack

- [x] 10.1 Create `services/k8s/plg/loki-values.yaml` for `grafana/loki` Helm
      chart — `deploymentMode: SingleBinary`, persistence enabled `2Gi`,
      `storageClassName: standard`
- [x] 10.2 Create `services/k8s/plg/promtail-values.yaml` for `grafana/promtail`
      Helm chart — `config.lokiAddress: http://loki:3100/loki/api/v1/push`,
      pipeline stage for JSON parsing and multiline Java stack traces
- [x] 10.3 Create `services/k8s/plg/grafana-values.yaml` for `grafana/grafana`
      Helm chart — pre-configured Loki datasource via `datasources.yaml`,
      persistence `1Gi`
- [x] 10.4 Create `services/k8s/plg/README.md` with Helm install commands for
      all three components in correct order (Loki → Promtail → Grafana)

## 11. Verification

- [x] 11.1 Run full build: `./gradlew build` — all services compile and tests
      pass
- [ ] 11.2 Start user-management locally and confirm `-->` / `<--` log entries
      appear in console for a test HTTP request
- [ ] 11.3 Confirm service starts without errors when Consul and Valkey are not
      running (local dev)
