Feature: Lease heartbeat

  Background:
    Given the database is empty

  Scenario: Heartbeat extends an active non-expired lease
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
    And there is an active lease for schema "SCHEMA_01" owned by "client-1"
    And I remember the lease expiry time
    When I send a heartbeat for that lease
    Then the heartbeat returns a lease
    And the heartbeat lease status is "ACTIVE"
    And the lease expiry time is later than the remembered expiry time

  Scenario: Heartbeat for a non-existent lease returns nothing
    When I send a heartbeat for a random lease id
    Then no heartbeat lease is returned

  Scenario: Heartbeat for a non-active lease does not extend expiry
    Given the following schema pools exist:
      | schemaName | enabled |
      | SCHEMA_01  | true    |
    And there is a released lease for schema "SCHEMA_01" owned by "client-2"
    And I remember the lease expiry time
    When I send a heartbeat for that lease
    Then the heartbeat returns a lease
    And the heartbeat lease status is "RELEASED"
    And the lease expiry time is not later than the remembered expiry time

