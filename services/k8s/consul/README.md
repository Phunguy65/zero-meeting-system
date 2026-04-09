# Consul — Kubernetes deployment via Helm

## Prerequisites

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update
```

## Install

```bash
helm install consul hashicorp/consul \
    --namespace consul \
    --create-namespace \
    -f values.yaml
```

## Upgrade

```bash
helm upgrade consul hashicorp/consul \
    --namespace consul \
    -f values.yaml
```

## Notes

- `server.replicas=1` is for dev/k3s. Set to `3` for production.
- `connectInject.enabled=false` — Consul Connect (mTLS) is deferred to a
  security hardening change.
- `syncCatalog.enabled=true` — K8s services are synced into the Consul catalog
  automatically.
- Services register via Spring Cloud Consul using
  `spring.config.import=optional:consul:`.
- Consul KV path for user-management config: `config/user-management/`
