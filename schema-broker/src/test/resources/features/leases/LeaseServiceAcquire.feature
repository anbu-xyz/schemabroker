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

  Scenario: Acquire a lease from a specific group
    Given the following schema pools exist:
      | schemaName    | groupName | enabled |
      | SCHEMA_TEAM   | teamcity  | true    |
      | SCHEMA_OTHER  | other     | true    |
    And there are no active leases
    When a client "client-5" acquires a lease from group "teamcity" with metadata "project=my-service"
    Then a lease is returned
    And the lease schema is "SCHEMA_TEAM"

  Scenario: Acquire a lease from default group if requested group has exhausted eligible pools
    Given the following schema pools exist:
      | schemaName   | groupName | enabled |
      | SCHEMA_TEAM  | teamcity  | true    |
      | SCHEMA_OTHER | default   | true    |
    And there are no active leases
    And a client "client-5" has acquired a lease from group "teamcity" with metadata "project=my-service-5"
    When a client "client-6" acquires a lease from group "teamcity" with metadata "project=my-service-6"
    Then a lease is returned
    And the lease schema is "SCHEMA_OTHER"

  Scenario: No schemas available when requested group has no eligible pools
    Given the following schema pools exist:
      | schemaName   | groupName | enabled |
      | SCHEMA_ONE   | alpha     | true    |
      | SCHEMA_TWO   | beta      | true    |
    And there are no active leases
    When a client "client-6" acquires a lease from group "gamma" with metadata "project=my-service"
    Then no lease is returned
