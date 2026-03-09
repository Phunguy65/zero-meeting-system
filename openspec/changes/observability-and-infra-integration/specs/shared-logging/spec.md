# ADDED Requirements

## Requirement: HTTP request logging with inbound direction marker

The system SHALL log every inbound HTTP request with a `-->` direction prefix at
INFO level, including: HTTP method, URI, remote address, and `correlationId`
from MDC. The `correlationId` SHALL be taken from the `X-Correlation-ID` request
header if present, or generated as a UUID v4 if absent. The generated or
received `correlationId` SHALL be written back to the `X-Correlation-ID`
response header.

### Scenario: Inbound request is logged with direction marker

- **WHEN** an HTTP request arrives at any endpoint
- **THEN** a log entry at INFO level is emitted with `direction: "-->"`,
  `method`, `uri`, `remoteAddr`, and `correlationId` fields

### Scenario: Missing correlation ID is auto-generated

- **WHEN** an HTTP request arrives without an `X-Correlation-ID` header
- **THEN** a UUID v4 is generated, placed in MDC as `correlationId`, and
  returned in the `X-Correlation-ID` response header

### Scenario: Existing correlation ID is propagated

- **WHEN** an HTTP request arrives with an `X-Correlation-ID` header value
- **THEN** that value is used as `correlationId` in MDC and echoed back in the
  response header unchanged

## Requirement: HTTP response logging with outbound direction marker

The system SHALL log every HTTP response with a `<--` direction prefix at INFO
level, including: HTTP method, URI, HTTP status code, response duration in
milliseconds, and `correlationId`. MDC SHALL be cleared after the response log
entry is written.

### Scenario: Outbound response is logged with direction marker and duration

- **WHEN** an HTTP response is sent to the client
- **THEN** a log entry at INFO level is emitted with `direction: "<--"`,
  `method`, `uri`, `status`, `duration_ms`, and `correlationId` fields

### Scenario: MDC is cleared after response

- **WHEN** the response filter chain completes
- **THEN** all MDC keys set by the filter (`correlationId`, `requestId`) are
  removed to prevent leakage across thread-pool reuse

## Requirement: Service-layer AOP logging with directional markers

The system SHALL intercept all methods on beans annotated with `@Service` and
emit DEBUG-level log entries with `-->` on entry and `<--` on exit, including
class name, method name, and execution duration in milliseconds. Exception
logging SHALL be at ERROR level with the exception message included.

### Scenario: Service method entry is logged

- **WHEN** a `@Service` bean method is invoked
- **THEN** a DEBUG log entry is emitted with `direction: "-->"`, `class`, and
  `method` fields

### Scenario: Service method exit is logged with duration

- **WHEN** a `@Service` bean method returns successfully
- **THEN** a DEBUG log entry is emitted with `direction: "<--"`, `class`,
  `method`, and `duration_ms` fields

### Scenario: Service method exception is logged

- **WHEN** a `@Service` bean method throws an exception
- **THEN** an ERROR log entry is emitted with `direction: "<--"`, `class`,
  `method`, `duration_ms`, and `error` (exception message) fields, and the
  exception is re-thrown

## Requirement: Argument logging is opt-in and disabled by default

The system SHALL NOT log method arguments by default. Argument logging SHALL
only be enabled when `logging.aspect.include-args=true` is set.

### Scenario: Arguments are not logged by default

- **WHEN** a `@Service` method is invoked and `logging.aspect.include-args` is
  not set
- **THEN** no argument values appear in the log entry

### Scenario: Arguments are logged when opt-in property is set

- **WHEN** `logging.aspect.include-args=true` is configured
- **THEN** method argument values are included in the `-->` entry log

## Requirement: Logging auto-configuration is activated automatically

The `LoggingAutoConfiguration` SHALL be registered via Spring Boot SPI
(`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`)
so that any service including the `shared` module gets `HttpLoggingFilter` and
`LoggingAspect` beans without any explicit `@Import` or `@ComponentScan` change.

### Scenario: Logging beans are present without explicit configuration

- **WHEN** a Spring Boot service includes the `shared` module as a dependency
- **THEN** `HttpLoggingFilter` and `LoggingAspect` beans are registered in the
  application context automatically

## Requirement: Structured JSON logging on k8s profile

The system SHALL emit log entries as structured JSON in ECS (Elastic Common
Schema) format when the `k8s` Spring profile is active. On the `local` profile,
human-readable console output SHALL be used.

### Scenario: JSON output on k8s profile

- **WHEN** the application starts with `SPRING_PROFILES_ACTIVE=k8s`
- **THEN** all log output is JSON with ECS field names (`@timestamp`,
  `log.level`, `message`, etc.)

### Scenario: Human-readable output on local profile

- **WHEN** the application starts without the `k8s` profile
- **THEN** log output uses the default Spring Boot console pattern
  (human-readable)
