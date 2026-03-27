Feature: Lease service status

  Background:
    Given the database is empty

  Scenario: Status reflects pools and active leases
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
      | SCHEMA_02  | true    |
      | SCHEMA_03  | false   |
    And there is an active lease for schema "SCHEMA_01" owned by "client-1"
    When I request the lease service status
    Then the status ttlSeconds equals the configured TTL
    And status for schema "SCHEMA_01" is "LEASED" and enabled true
    And status for schema "SCHEMA_02" is "FREE" and enabled true
    And status for schema "SCHEMA_03" is "FREE" and enabled false

  Scenario: Expired leases do not mark schemas as leased
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
    And there is an expired lease for schema "SCHEMA_01"
    When I request the lease service status
    Then status for schema "SCHEMA_01" is "FREE" and enabled true

