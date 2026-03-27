# Schema Broker Client Module

This module builds a small standalone client that obtains schema leases from the Schema Broker and writes the 
connection details to a properties file that tests can load.

## Usage

```bash
java -cp schema-client-0.0.1-SNAPSHOT.jar uk.anbu.schemabroker.schema.client.SchemaClientAcquireLease \
  --broker-url http://localhost:8080 \
  --owner sample-tests \
  --output target/schema-lease.properties
```

Each invocation produces a `schema-lease.properties` file that contains the following keys:

```
schema.expiresAt
schema.lease.id
schema.name
spring.datasource.password
spring.datasource.url
spring.datasource.username
```

Tests can load that file with Spring Boot by pointing `spring.config.additional-location` at it.

