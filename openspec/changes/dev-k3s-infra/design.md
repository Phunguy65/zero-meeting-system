# Dev k3s Infrastructure — Design

## 1. Container Build Pipeline

### Approach: Spring Boot `bootBuildImage` (CNB/Paketo)

Centralized in
`build-logic/src/main/kotlin/io.github.phunguy65.zms.plugin.service.base.gradle.kts`:

```kotlin
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName.set("ghcr.io/phunguy65/zms/${project.name}:${project.version}")
    environment.set(mapOf(
        "BP_JVM_VERSION" to "25"
    ))
}
```

- Uses default Paketo builder (JVM mode, not native — GraalVM plugin presence
  does not force native)
- Each service inherits this config automatically via the shared plugin
- `imagePullPolicy: IfNotPresent` in k8s manifests (local builds for dev)

## 2. Directory Structure (Kustomize)

```
services/k8s/
├── base/
│   ├── kustomization.yaml
│   ├── namespaces.yaml              ← kong-system, livekit-system, rustfs-system, kafka
│   ├── databases/
│   │   ├── user-postgres.yaml       ← NEW
│   │   ├── meeting-postgres.yaml    ← NEW
│   │   └── chat-mongo.yaml          ← NEW
│   ├── services/
│   │   ├── user-management.yaml     ← MODIFIED (add gRPC port, CURSOR_SECRET)
│   │   ├── meeting-management.yaml  ← MODIFIED (add CURSOR_SECRET, gRPC env, remove Consul)
│   │   ├── chat-management.yaml     ← MODIFIED (add LiveKit/JWT env, fix port)
│   │   └── notification.yaml        ← NEW
│   ├── valkey/
│   │   ├── statefulset.yaml
│   │   └── service.yaml
│   ├── rustfs/
│   │   ├── 00-namespace.yaml
│   │   ├── 01-secrets.yaml
│   │   ├── 02-pvc.yaml
│   │   ├── 03-deployment.yaml
│   │   ├── 04-service.yaml
│   │   └── 05-init-buckets-job.yaml
│   ├── livekit/
│   │   ├── 00-namespace.yaml
│   │   ├── 01-redis.yaml
│   │   ├── 02-secrets.yaml
│   │   └── 05-network-policy.yaml
│   └── kong/
│       ├── 00-namespace.yaml
│       ├── 03-gateway.yaml
│       ├── 04-plugins.yaml
│       ├── 05-consumers.yaml
│       └── 06-routes.yaml           ← MODIFIED (webhook path)
│
├── overlays/
│   └── dev/
│       ├── kustomization.yaml       ← NEW
│       └── patches/
│           └── dev-resources.yaml   ← NEW (strategic merge patches for all resources)
│
├── helm/                            ← NEW (Helm install scripts, separate from Kustomize)
│   ├── install-strimzi.sh
│   ├── install-kong.sh
│   ├── install-livekit.sh
│   └── install-plg.sh              ← optional, disabled by default
│
├── kafka/
│   ├── strimzi-operator.yaml        ← install instructions (unchanged)
│   ├── kafka-cluster.yaml           ← MODIFIED (proper KRaft syntax)
│   └── kafka-topics.yaml            ← unchanged
│
└── scripts/
    └── dev-setup.sh                 ← NEW
```

### Kustomize Strategy

Helm-managed components (Strimzi operator, Kong operator, LiveKit server/egress,
PLG) are installed via shell scripts in `helm/`. They are NOT managed by
Kustomize.

Kustomize manages only raw YAML manifests: databases, services, valkey, rustfs
namespace/secrets/PVC, livekit namespace/redis/secrets/network-policy, kong
namespace/gateway/plugins/consumers/routes.

The dev overlay patches resources (CPU/RAM limits) and storageClass
(`local-path`).

## 3. Database Manifests

### 3.1 user-postgres

```yaml
# Deployment + Service + PVC + Secret
# postgres:18-alpine, DB: zms_users
# Flyway handles schema migration at app startup
```

### 3.2 meeting-postgres

```yaml
# Same pattern as user-postgres
# DB: zms_meetings
```

### 3.3 chat-mongo

```yaml
# StatefulSet + headless Service + PVC + ConfigMap (init-mongo.js)
# mongo:8.0, command: ["mongod", "--replSet", "rs0", "--bind_ip_all"]
# postStart lifecycle hook: rs.initiate() + wait for RS ready
# ConfigMap: init-mongo.js indexes (from existing chat-management/init-mongo.js)
# No auth for dev (simplicity)
```

**Key: MongoDB ReplicaSet init sequence:**

1. mongod starts with `--replSet rs0`
2. postStart hook waits for mongod to accept connections
3. `rs.initiate({_id: "rs0", members: [{_id: 0, host: "chat-mongo-0.chat-mongo:27017"}]})`
4. Wait for RS status OK
5. Run init-mongo.js (indexes) via configMap mounted at
   `/docker-entrypoint-initdb.d/`

**Important**: `/docker-entrypoint-initdb.d/` scripts only run on first init
(empty data dir). For ReplicaSet mode, the postStart hook is more reliable for
ensuring RS is initialized before app connects.

## 4. Manifest Fixes

### 4.1 Kafka namespace + DNS

All service env vars change from:

```
kafka-cluster-kafka-bootstrap.kafka-system.svc.cluster.local:9092
```

to:

```
zms-kafka-kafka-bootstrap.kafka.svc.cluster.local:9092
```

### 4.2 Strimzi KRaft

Remove `zookeeper` section. Use proper KRaft config (Strimzi 0.45+ handles KRaft
natively via `.spec.kafka` only — do NOT set `process.roles` or
`controller.quorum.voters` manually, Strimzi manages these).

### 4.3 Service env vars

| Service            | Add                                                                                                   | Remove                       |
| ------------------ | ----------------------------------------------------------------------------------------------------- | ---------------------------- |
| user-management    | `CURSOR_SECRET`                                                                                       | `CONSUL_HOST`, `CONSUL_PORT` |
| meeting-management | `CURSOR_SECRET`, `USER_MANAGEMENT_GRPC_HOST`, `USER_MANAGEMENT_GRPC_PORT`                             | `CONSUL_HOST`, `CONSUL_PORT` |
| chat-management    | `LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`, `CHAT_JWT_SECRET`, fix `containerPort` → 8080 | —                            |

### 4.4 user-management Service — add gRPC port

```yaml
ports:
    - name: http
      port: 8080
      targetPort: 8080
    - name: grpc
      port: 9090
      targetPort: 9090
```

### 4.5 Kong webhook route

```yaml
# Change from Exact /webhook/livekit to:
- path:
      type: PathPrefix
      value: /api/v1/webhook/livekit
```

### 4.6 LiveKit webhook URL

In `03-livekit-values.yaml`:

```yaml
webhook:
    urls:
        - 'http://kong-gateway-kong-proxy.kong-system.svc.cluster.local/api/v1/webhook/livekit'
```

## 5. Dev Resource Budget

| Component              |  CPU req  | CPU lim  |  RAM req   |  RAM lim   |
| ---------------------- | :-------: | :------: | :--------: | :--------: |
| user-management        |   100m    |   300m   |   256Mi    |   768Mi    |
| meeting-management     |   100m    |   300m   |   256Mi    |   768Mi    |
| chat-management        |   100m    |   300m   |   256Mi    |   768Mi    |
| notification           |    50m    |   200m   |   128Mi    |   384Mi    |
| user-postgres          |    50m    |   200m   |   128Mi    |   256Mi    |
| meeting-postgres       |    50m    |   200m   |   128Mi    |   256Mi    |
| chat-mongo             |   100m    |   300m   |   128Mi    |   256Mi    |
| Kong ctrl+proxy        |   50m×2   |  200m×2  |  128Mi×2   |  256Mi×2   |
| Kafka (Strimzi broker) |   100m    |   500m   |   256Mi    |   512Mi    |
| Valkey                 |    25m    |   100m   |    32Mi    |   128Mi    |
| LiveKit Server         |   250m    |  1000m   |   256Mi    |    1Gi     |
| LiveKit Redis          |    25m    |   100m   |    32Mi    |   128Mi    |
| LiveKit Egress         |   200m    |   500m   |   256Mi    |   512Mi    |
| RustFS                 |    50m    |   200m   |    64Mi    |   256Mi    |
| **TOTAL**              | **~1.45** | **~4.8** | **~2.6Gi** | **~6.5Gi** |

## 6. chat-management Port Fix

Change `server.port` from `8082` to `8080` in `application.yaml` to be
consistent with all other services.

## 7. Dev Setup Script

`k8s/scripts/dev-setup.sh`:

1. Check k3s installed, fail with install instructions if not
2. Install Helm charts: Strimzi operator, Kong operator, LiveKit server + egress
3. Apply Kafka cluster + topics
4. `kustomize build ../overlays/dev | kubectl apply -f -`
5. Wait for all deployments ready (timeout 5min)
6. Print access URLs (Kong proxy, Grafana if enabled)
