# ADDED Requirements

## Requirement: Service registers itself with Consul on startup

The user-management service SHALL register itself with Consul using its
`spring.application.name` as the service name. The registration SHALL include
the pod IP address (not hostname), a health check pointing to
`/actuator/health`, and a health check interval of 10 seconds. The service SHALL
deregister from Consul on graceful shutdown.

### Scenario: Service appears in Consul catalog after startup

- **WHEN** the user-management service starts with Consul reachable
- **THEN** a service entry named `user-management` appears in the Consul service
  catalog with a passing health check

### Scenario: Service starts without Consul in local dev

- **WHEN** the application starts without the `k8s` profile and Consul is not
  reachable
- **THEN** the application starts successfully (Consul discovery is `optional`)

## Requirement: Service reads configuration from Consul KV store

The user-management service SHALL import configuration from Consul KV at the
path `config/user-management/` in YAML format. The service SHALL watch for KV
changes and refresh `@ConfigurationProperties` beans without restart. The Consul
config import SHALL be declared as `optional:consul:` so the service starts
without Consul in local dev.

### Scenario: Property from Consul KV overrides application.properties

- **WHEN** a key exists at `config/user-management/` in Consul KV
- **THEN** its value overrides the corresponding property from
  `application.properties`

### Scenario: Config is refreshed on KV change without restart

- **WHEN** a value is updated in Consul KV
- **THEN** the corresponding `@ConfigurationProperties` bean is refreshed within
  the watch interval (default 55 seconds)

### Scenario: Service starts when Consul KV is unavailable

- **WHEN** Consul is not reachable at startup
- **THEN** the service starts using values from `application.properties` only

## Requirement: Spring Cloud BOM is applied to all services via convention plugin

The `service.base` Gradle convention plugin SHALL import the Spring Cloud BOM
(`org.springframework.cloud:spring-cloud-dependencies:2025.1.1`) so that all
services in the monorepo inherit consistent Spring Cloud dependency versions
without declaring versions individually.

### Scenario: Spring Cloud dependency version is resolved without explicit version

- **WHEN** a service declares `spring-cloud-starter-consul-discovery` without a
  version
- **THEN** Gradle resolves the version from the Spring Cloud BOM imported by the
  convention plugin
