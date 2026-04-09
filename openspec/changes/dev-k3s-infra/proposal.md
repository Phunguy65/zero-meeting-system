# Dev k3s Infrastructure

## Summary

Set up a complete dev environment on bare-metal k3s with container build
pipeline, missing database manifests, Kustomize overlay system, resource tuning,
and Kong/LiveKit route sync.

## Motivation

The project has 3 Spring Boot microservices + 1 notification service with a rich
platform layer (Kong, Kafka, LiveKit, RustFS, Valkey, PLG). However, the
infrastructure is incomplete:

- **No container build pipeline** — no Dockerfiles, no `bootBuildImage` config
- **Missing database manifests** — Postgres x2 and MongoDB x1 are referenced in
  service manifests but have no k8s definitions
- **Missing service manifests** — notification service has no k8s deployment
- **No environment overlay system** — single flat set of manifests, no dev/prod
  differentiation
- **Bug: Kafka namespace mismatch** — manifests use namespace `kafka`, service
  env vars reference `kafka-system`
- **Bug: Kafka bootstrap DNS mismatch** — CR named `zms-kafka`, env vars
  reference `kafka-cluster-kafka-bootstrap`
- **Bug: Strimzi KRaft config** — uses deprecated `zookeeper.replicas: 0`
  instead of proper KRaft syntax
- **Missing env vars** — CURSOR_SECRET, gRPC port, chat LiveKit/JWT secrets not
  in manifests
- **Stale Kong route** — webhook still at `/webhook/livekit`, should be
  `/api/v1/webhook/livekit`
- **storageClass mismatch** — all PVCs use `standard`, k3s uses `local-path`
- **Consul referenced but unused** — env vars + Helm, but zero application code
  uses it

## Scope

### In scope

- Container build: `bootBuildImage` (CNB/Paketo) configuration in shared
  build-logic
- Database manifests: user-postgres, meeting-postgres, chat-mongo (single-node
  ReplicaSet)
- Fix all existing k8s manifest bugs (env vars, ports, Kafka, Kong route,
  LiveKit webhook URL)
- Notification service k8s manifest (Deployment only — Kafka consumer, no HTTP)
- Kustomize base/overlays structure for dev environment
- Dev resource tuning (~4.5Gi RAM budget, fits 8GB+ machine)
- Dev setup script + documentation
- Remove Consul from dev

### Out of scope

- Prod environment (AWS EC2 + k3s) — future change
- CI/CD pipeline (GitHub Actions) — future change
- GHCR publish automation — future change
- TLS/cert-manager — future change
- Sealed Secrets / SOPS — future change

## Decisions

| #   | Decision             | Value                                                                   |
| --- | -------------------- | ----------------------------------------------------------------------- |
| 1   | Container build      | `bootBuildImage` (CNB/Paketo), centralized in `service.base.gradle.kts` |
| 2   | Registry             | GHCR (`ghcr.io/phunguy65/zms/<service>:<version>`)                      |
| 3   | MongoDB dev          | Single-node ReplicaSet (rs.initiate via postStart lifecycle hook)       |
| 4   | Consul               | Remove from dev (no application code uses it)                           |
| 5   | LoadBalancer         | k3s Klipper (dev), MetalLB (prod — future)                              |
| 6   | storageClass         | `local-path` (dev), patched via Kustomize overlay                       |
| 7   | PLG monitoring       | Disabled by default in dev overlay                                      |
| 8   | LiveKit Egress       | Included in dev                                                         |
| 9   | chat-management port | Fix to 8080 (consistent with other services)                            |
| 10  | gRPC port            | Add 9090 to user-management k8s Service                                 |
| 11  | Kustomize + Helm     | Helm stays separate (install scripts), Kustomize for raw manifests only |
| 12  | k3s flags            | `--disable traefik --disable metrics-server`                            |
| 13  | Kafka namespace      | Unified to `kafka` (matching kafka-cluster.yaml)                        |
| 14  | Kong webhook         | `/webhook/livekit` → `/api/v1/webhook/livekit`                          |

## Non-goals

- Production-grade HA (all replicas = 1 in dev)
- Automated secret rotation
- Automated backup/restore
- Network policies beyond what exists for LiveKit
