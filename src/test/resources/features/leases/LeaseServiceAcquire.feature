Feature: Acquire schema lease

  Background:
    Given the database is empty

  Scenario: Acquire a lease from a free enabled schema
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
      | SCHEMA_02  | true    |
    And there are no active leases
    When a client "client-1" acquires a lease with metadata "project=my-service"
    Then a lease is returned
    And the lease status is "ACTIVE"
    And the lease owner is "client-1"
    And the lease schema is one of:
      | SCHEMA_01 |
      | SCHEMA_02 |

  Scenario: No free schemas when all have active leases
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
      | SCHEMA_02  | true    |
    And each schema has an active non-expired lease
    When a client "client-2" acquires a lease with metadata "project=my-service"
    Then no lease is returned

  Scenario: Disabled schemas are skipped when acquiring a lease
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | false   |
      | SCHEMA_02  | true    |
    And there are no active leases
    When a client "client-3" acquires a lease with metadata "project=my-service"
    Then a lease is returned
    And the lease schema is "SCHEMA_02"

  Scenario: Expired leases free schemas for new leases
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
    And there is an expired lease for schema "SCHEMA_01"
    When a client "client-4" acquires a lease with metadata "project=my-service"
    Then a lease is returned
    And the lease schema is "SCHEMA_01"