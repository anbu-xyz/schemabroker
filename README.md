# Schema Broker

Lease-based schema pool broker built with Spring Boot. It hands out database schema credentials to callers, 
tracks leases with TTL/heartbeats, and exposes both JSON APIs and a lightweight HTML status page.

The [sample-project](sample-project/README.md) demonstrates how to use the schema broker from Java tests, acquiring 
schemas for test isolation and releasing them back to the pool when done.

## Quick start
- Prereqs: Java 21+, Maven, network access to your target DBs (defaults use in-memory H2).
- Build & test:
  ```bash
  cd schema-broker
  mvn clean verify
  ```
- Run with local profile (enables H2 console at `/h2-console`):
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=local
  ```

## Configuration
Key properties (`src/main/resources/application.yaml`):
- `server.port` – defaults to 8080.
- `lease.ttlSeconds` – lease time-to-live; default `600` seconds.
- `database.init.schemas` – YAML file used to seed `schema_pool` entries on startup; defaults to `classpath:schema.yaml`. 
   Override with env var `DATABASE_INIT_SCHEMAS` to point at another file/location.
- `h2Console.enabled` – toggles H2 console; default `false` (set `true` in `application-local.yaml`).

Schema seed format (`schema.yaml`):
```yaml
schemas:
  - schemaName: SCHEMA_01
    groupName: teamcity   # optional; defaults to "default"
    loginUser: sa
    jdbcUrl: jdbc:h2:mem:schema_01;DB_CLOSE_DELAY=-1
```
Each entry becomes an enabled `schema_pool` row. If a schema is removed from the file, it will be disabled (not deleted) 
on next startup.

## API
Base path: `/api/v1`.

**Lease lifecycle**
- `POST /leases` – acquire a lease.
  - Request body: `{ "owner": "alice", "groupName": "teamcity"?, "metadata": {…}? }` (`groupName` optional, defaults to `default`).
  - Responses: `201` with `{ leaseId, schema, loginUser, jdbcUrl, expiresAt, ttlSeconds }`; `409` when no free schemas.
- `POST /leases/{leaseId}/heartbeat` – extend TTL; `200` with same shape as acquire, `404` if missing, `410` if expired/released.
- `POST /leases/{leaseId}/release` – release early; `200` with `{ leaseId, schema, status }`, `404` if missing.

**Status**
- `GET /status/schemas` – current pool with lease status (`SchemaStatusDto`).
- `GET /status/leases` – lists active, expired, and non-active leases.
- `GET /status/lease/{leaseId}` – details for a lease (or `404`).

**HTML views**
- `GET /schemas` – human-friendly dashboard showing pool and lease status.
- `GET /lease/{leaseId}` – HTML details for a lease.

Sample requests are available in `src/test/resources/sample.http` (usable with IntelliJ REST Client).

## Data model
Managed via Liquibase changelog `src/main/resources/db/changelog/changelog-001.yaml`:
- `schema_pool` – configured schemas; unique `(schema_name, jdbc_url)`, `enabled` flag, optional `group_name` (defaults to `default`).
- `schema_lease` – issued leases with status, timestamps, owner, IP/hostname, metadata; foreign key to `schema_pool`; 
  unique `lease_id` and one active lease per schema enforced by a computed column index.

## Operational notes
- TTL expiration runs every minute; expired leases move to `EXPIRED` status, making the schema available again.
- When acquiring, schemas are filtered by `groupName` first, then the `default` group is used as a fallback; 
  selection order is randomized per request.