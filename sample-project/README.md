# schemabroker/sample-project

This directory hosts a lightweight multi-module Maven project with two independent Spring Boot services that each expose 
an H2-backed data store for demos or integration testing.

## Project layout

- `note-module` & `task-module` - Spring Boot web apps that share the same parent.
- `schema-client` - a simple Java client library for interacting with the schema broker API, used by both modules 
  to acquire/release schemas used in testing.

## Running

```bash
mvn clean install
mvn -pl note-module spring-boot:run
```

Or run `task-module` instead:

```bash
mvn -pl task-module spring-boot:run
```

### Running individual tests through maven

```bash
mvn -pl note-module -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=NoteApplicationTest test
```