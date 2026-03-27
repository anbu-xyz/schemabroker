# schemabroker/modules

This directory hosts a lightweight multi-module Maven project with two independent Spring Boot services that each expose an H2-backed data store for demos or integration testing.

## Project layout

- `note-module` & `task-module` — Spring Boot web apps that share the same parent for dependency management. They each expose their own REST endpoints and H2 console.

## Running

From `project/`:

```powershell
mvn clean install
mvn -pl note-module spring-boot:run
```

Or run `task-module` instead:

```powershell
mvn -pl task-module spring-boot:run
```

Both modules listen on their own ports (`8081` and `8082`) and use in-memory H2 instances (`module1db`, `module2db`). CLI commands automatically pick up the shared Spring Boot parent.

