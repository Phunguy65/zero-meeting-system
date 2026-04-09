# ADDED Requirements

## Requirement: Valkey is deployed to Kubernetes as a StatefulSet

The system SHALL deploy Valkey (Redis-compatible) as a Kubernetes StatefulSet
with a PersistentVolumeClaim for data durability. A Kubernetes Service SHALL
expose Valkey on port 6379 within the cluster.

### Scenario: Valkey pod is running and accessible within the cluster

- **WHEN** the Valkey StatefulSet is applied to the cluster
- **THEN** a pod is running and reachable at
  `valkey-service.<namespace>.svc.cluster.local:6379`

## Requirement: Spring Data Redis is wired and conditionally activated

The `service.base` convention plugin SHALL include
`spring-boot-starter-data-redis` as a dependency. A `CacheConfig` class in
`shared/infrastructure/cache/` SHALL configure a `RedisCacheManager` bean. The
`CacheConfig` SHALL only activate when `spring.data.redis.host` is set, using
`@ConditionalOnProperty`.

### Scenario: CacheConfig activates when Redis host is configured

- **WHEN** `spring.data.redis.host` is set in the application configuration
- **THEN** a `RedisCacheManager` bean is present in the application context

### Scenario: CacheConfig does not activate when Redis host is absent

- **WHEN** `spring.data.redis.host` is not set
- **THEN** no `RedisCacheManager` bean is created and the application starts
  without error

## Requirement: Valkey connection properties are externalised

The Valkey host, port, and password SHALL be configurable via environment
variables (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`) with safe local
defaults (`localhost`, `6379`, empty password).

### Scenario: Valkey connection uses environment variable overrides in K8s

- **WHEN** `REDIS_HOST` is set to the in-cluster Valkey service hostname
- **THEN** the application connects to Valkey at that address
