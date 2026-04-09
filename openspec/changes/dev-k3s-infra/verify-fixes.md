## [2026-04-07] Round 1 (from opsx-apply auto-verify)

### opsx-arch-verifier

- Fixed: [CRITICAL] Added `CURSOR_SECRET` env var and `cursor-secret` key to
  meeting-management k8s manifest
  (`services/k8s/base/services/meeting-management.yaml`). Also added
  `app.cursor.secret=${CURSOR_SECRET}` to
  `services/meeting-management/src/main/resources/application.properties` so
  `CursorEncoder` can resolve the property.
- Fixed: [CRITICAL] Extended chat-mongo postStart lifecycle hook to execute init
  scripts from `/docker-entrypoint-initdb.d/*.js` after replica set
  initialization, since `command` override bypasses the Mongo image's default
  init flow (`services/k8s/base/databases/chat-mongo.yaml`).
- Fixed: [WARNING] Added readiness and liveness probes to notification
  deployment using Spring Actuator health endpoints
  (`/actuator/health/readiness` and `/actuator/health/liveness`) on port 8080
  (`services/k8s/base/services/notification.yaml`).
- Fixed: [WARNING] Removed hardcoded `-U zms_user` from Postgres probe commands
  in both `user-postgres.yaml` and `meeting-postgres.yaml`. `pg_isready` checks
  server availability without requiring a specific username, so probes now work
  regardless of the username configured in Secrets.
- Acknowledged: [WARNING] LiveKit credential duplication across Helm values and
  k8s Secrets is a known trade-off. Deduplication requires external secret
  management (e.g., Sealed Secrets, SOPS) which is explicitly out of scope for
  this change.

## [2026-04-07] Round 2 (from opsx-apply re-verify)

### opsx-arch-verifier

- Fixed: [WARNING] Corrected Valkey StatefulSet `serviceName` from `valkey` to
  `valkey-service` to match the actual Service name in
  `services/k8s/base/valkey/service.yaml`.
- Fixed: [WARNING] Updated `install-livekit.sh` to read egress credentials
  (api_key, api_secret, s3.access_key, s3.secret) from existing Kubernetes
  Secrets via `kubectl get secret` and pass them via `--set` overrides during
  Helm install, so the egress deployment is fully wired.
- Fixed: [WARNING] Updated meeting-management default webhook URL from
  `http://localhost:8080/webhook/livekit` to
  `http://localhost:8080/api/v1/webhook/livekit` in `application.properties` to
  be consistent with the new routing design.
