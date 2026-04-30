# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with
code in this repository.

## Architecture Overview

Monorepo with mixed stack: Java/Spring microservices + Android + Next.js web
app. API-first: backend services generate OpenAPI specs, a root script merges
them into `openapi/unified-openapi.yaml`, and clients are generated for web and
Android.

**Components:**

- `services/` — Spring Boot 4 / Java 25 backend microservices (hexagonal
  architecture)
- `frontends/android-app/` — Native Android (MVVM + Clean Architecture)
- `frontends/web/` — Next.js 16 / React 19 web client
- `services/k8s/` — Kubernetes manifests (Kong gateway, Kafka, DBs, LiveKit,
  Valkey)
- `openspec/` — Product specs and change artifacts
- `build-logic/` — Shared Gradle convention plugins

**Backend services** (`user-management`, `meeting-management`,
`chat-management`, `notification`, `proto`, `shared`): each follows
`domain → application → infrastructure → presentation` layering.

**Service communication:**

- External: Kong API gateway routes to each service
- Sync: `meeting-management` → `user-management` via gRPC
- Async: Kafka + CloudEvents for domain events (publishers in user/meeting,
  consumers in chat/notification)
- Real-time: SSE in `meeting-management` (`MeetingSseManager`), backed by Kafka
  and Redis

**Persistence:**

- Postgres + Flyway: `user-management`, `meeting-management`
- MongoDB: `chat-management`
- Redis/Valkey: join-request state in `meeting-management`

**Key integrations:** LiveKit (video), Firebase (auth/storage), Resend (email
notifications)

## Build Commands

### Backend services (Gradle)

```sh
./services/gradlew build                               # all services
./services/gradlew -p services/ < service-name > build # single service
./services/gradlew spotlessApply                       # format Java/KTS/XML
./services/gradlew bufFormatApply                      # format proto
```

### Android app

```sh
./frontends/android-app/gradlew -p frontends/android-app :app:assembleDebug
./frontends/android-app/gradlew -p frontends/android-app build
./frontends/android-app/gradlew -p frontends/android-app spotlessApply
```

### Web app

```sh
pnpm --dir frontends/web dev
pnpm --dir frontends/web build
pnpm --dir frontends/web lint
pnpm --dir frontends/web lint:fix
pnpm --dir frontends/web format
```

### OpenAPI / SDK generation

```sh
pnpm run openapi:services                 # generate per-service OpenAPI via tests
pnpm run openapi:join                     # merge specs with Redocly
pnpm run openapi:unified                  # full pipeline
pnpm --dir frontends/web run generate:sdk # regenerate web SDK from unified spec
```

### Root-level formatting

```sh
pnpm lint   # markdownlint
pnpm format # prettier for md/json/toml/yaml/sh
```

## Test Commands

### Backend services

```sh
./services/gradlew test                               # all services
./services/gradlew -p services/ < service-name > test # single service
./services/gradlew -p services/ < service-name > generateOpenApiDocsFromTests
```

### Android

```sh
./frontends/android-app/gradlew -p frontends/android-app :app:testDebugUnitTest
./frontends/android-app/gradlew -p frontends/android-app :app:connectedDebugAndroidTest
```

## Pre-commit Hooks (lefthook)

Git hooks run automatically: Spotless for Java/KTS/XML, Buf for proto, Biome for
web, gitleaks scan.

## Code Conventions

- Backend: JSend envelope pattern for HTTP responses; interceptors on
  web/Android clients unwrap envelopes automatically.
- Android: Hilt DI, Retrofit/OkHttp with auth + JSend interceptors in
  `NetworkModule`.
- Web: generated SDK lives in `frontends/web/src/generated`; do not edit
  manually.
- Biome is the formatter/linter for web (not ESLint/Prettier per-file).
