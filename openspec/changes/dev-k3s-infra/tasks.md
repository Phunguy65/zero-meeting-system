# Dev k3s Infrastructure — Tasks

## Phase 1: Container Build Foundation

- [x] 1.1 Configure `bootBuildImage` in
      `build-logic/src/main/kotlin/io.github.phunguy65.zms.plugin.service.base.gradle.kts`
      — add task config with image name
      `ghcr.io/phunguy65/zms/${project.name}:${project.version}`,
      `BP_JVM_VERSION=25`, JVM mode
- [x] 1.2 Fix chat-management `server.port` from 8082 to 8080 in
      `services/chat-management/src/main/resources/application.yaml`

## Phase 2: Database Manifests

- [x] 2.1 Create `services/k8s/base/databases/user-postgres.yaml` — Deployment
      (postgres:18-alpine) + Service (ClusterIP:5432) + PVC (1Gi) + Secret
      (zms_user/zms_secret), DB name `zms_users`
- [x] 2.2 Create `services/k8s/base/databases/meeting-postgres.yaml` — same
      pattern, DB name `zms_meetings`
- [x] 2.3 Create `services/k8s/base/databases/chat-mongo.yaml` — StatefulSet
      (mongo:8.0, `--replSet rs0 --bind_ip_all --wiredTigerCacheSizeGB 0.25`) +
      headless Service (ClusterIP:None, port 27017) + PVC (2Gi) + ConfigMap
      (init-mongo.js from existing file) + postStart lifecycle hook for
      `rs.initiate()`

## Phase 3: Fix Existing Manifests

- [x] 3.1 Fix `services/k8s/services/user-management.yaml` — add `CURSOR_SECRET`
      env var (from Secret), add gRPC port 9090 to Service, remove
      `CONSUL_HOST`/`CONSUL_PORT` env vars, fix Kafka bootstrap DNS to
      `zms-kafka-kafka-bootstrap.kafka.svc.cluster.local:9092`
- [x] 3.2 Fix `services/k8s/services/meeting-management.yaml` — add
      `CURSOR_SECRET` env var (from Secret), add
      `USER_MANAGEMENT_GRPC_HOST=user-management.default.svc.cluster.local` +
      `USER_MANAGEMENT_GRPC_PORT=9090`, remove `CONSUL_HOST`/`CONSUL_PORT`, fix
      Kafka bootstrap DNS
- [x] 3.3 Fix `services/k8s/services/chat-management.yaml` — add `LIVEKIT_URL`,
      `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET` (from livekit-secrets),
      `CHAT_JWT_SECRET` (from chat-management-secrets), fix containerPort to
      8080, fix MongoDB URI to
      `mongodb://chat-mongo-0.chat-mongo:27017/zms_chat?replicaSet=rs0`, fix
      Kafka bootstrap DNS
- [x] 3.4 Create `services/k8s/base/services/notification.yaml` — Deployment
      only (no Service, no Kong route), env vars: `KAFKA_BOOTSTRAP_SERVERS`,
      `RESEND_API_KEY`, `RESEND_FROM_EMAIL`, `RESEND_FROM_NAME`,
      `INVITATION_JOIN_BASE_URL` + Secret for RESEND_API_KEY
- [x] 3.5 Fix `services/k8s/kafka/kafka-cluster.yaml` — proper KRaft syntax:
      remove `zookeeper` section entirely, keep only `spec.kafka` +
      `spec.entityOperator`, ensure namespace is `kafka`
- [x] 3.6 Update `services/k8s/kong/06-routes.yaml` — change webhook route from
      Exact `/webhook/livekit` to PathPrefix `/api/v1/webhook/livekit`
- [x] 3.7 Update `services/k8s/livekit/03-livekit-values.yaml` — change webhook
      URL to use `/api/v1/webhook/livekit`

## Phase 4: Kustomize Structure

- [x] 4.1 Move existing manifests into `services/k8s/base/` — create base
      directory, move `services/`, `valkey/`, `rustfs/`, `kong/`, `livekit/`
      subdirectories under `base/`. Create `base/kustomization.yaml` referencing
      all resources. Create `base/namespaces.yaml` consolidating namespace
      definitions.
- [x] 4.2 Create `services/k8s/overlays/dev/kustomization.yaml` — reference
      `../../base`, patch all storageClassName to `local-path`, apply dev
      resource limits (per design budget table)
- [x] 4.3 Move `services/k8s/kong/01-metallb-pool.yaml` to
      `services/k8s/overlays/prod/metallb-pool.yaml` (create prod overlay
      directory, just this file for now)
- [x] 4.4 Create `services/k8s/helm/` directory with install scripts:
      `install-strimzi.sh`, `install-kong.sh`, `install-livekit.sh`,
      `install-plg.sh` (PLG script prints "optional, disabled by default")

## Phase 5: Dev Setup & Docs

- [x] 5.1 Create `services/k8s/scripts/dev-setup.sh` — checks k3s, runs helm
      install scripts, applies Kafka CR + topics, runs `kustomize build`, waits
      for pods, prints URLs. k3s install hint:
      `curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server --disable traefik --disable metrics-server" sh -`
- [x] 5.2 Remove Consul references — delete `services/k8s/consul/` directory,
      remove from kustomization if referenced
