package uk.anbu.schemabroker.web.dto;

import uk.anbu.schemabroker.model.SchemaLease;

import java.util.List;

public record LeaseListResponse(List<SchemaLease> activeLeases,
                                List<SchemaLease> activeExpiredLeases,
                                List<SchemaLease> nonActive) {
}