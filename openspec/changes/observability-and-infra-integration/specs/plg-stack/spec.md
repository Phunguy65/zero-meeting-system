# ADDED Requirements

## Requirement: Promtail DaemonSet scrapes pod logs from all namespaces

The system SHALL deploy Promtail as a Kubernetes DaemonSet that scrapes
stdout/stderr logs from all pods. Promtail SHALL attach the following
low-cardinality labels to every log stream: `app` (from pod label), `namespace`,
`pod`, `container`. Promtail SHALL forward all scraped logs to Loki.

### Scenario: Logs from user-management pods are collected

- **WHEN** the user-management pod emits a log line to stdout
- **THEN** Promtail on the same node picks it up and forwards it to Loki with
  labels `app=user-management`, `namespace`, `pod`, `container`

### Scenario: Multiline Java stack traces are handled

- **WHEN** a Java exception stack trace spans multiple lines in stdout
- **THEN** Promtail's pipeline stage groups them into a single Loki log entry

## Requirement: Loki stores and indexes log streams

The system SHALL deploy Loki as a Kubernetes StatefulSet with persistent
storage. Loki SHALL accept log pushes from Promtail and make them queryable via
LogQL. Loki SHALL use `SingleBinary` mode for the initial deployment.

### Scenario: Logs are queryable by app label

- **WHEN** a LogQL query `{app="user-management"}` is executed in Grafana
- **THEN** log entries from user-management pods are returned in chronological
  order

### Scenario: JSON fields are extractable via LogQL pipeline

- **WHEN** a LogQL query `{app="user-management"} | json | direction="-->"` is
  executed
- **THEN** only inbound HTTP request log entries are returned

## Requirement: Grafana provides log visualisation

The system SHALL deploy Grafana as a Kubernetes Deployment with Loki configured
as a data source. Grafana SHALL be accessible via a Kubernetes Service.

### Scenario: Loki data source is pre-configured

- **WHEN** Grafana starts
- **THEN** a Loki data source pointing to `http://loki:3100` is available
  without manual setup

### Scenario: Logs are browsable in Explore view

- **WHEN** an operator opens Grafana Explore and selects the Loki data source
- **THEN** they can query logs by label and see structured JSON fields parsed
  inline
