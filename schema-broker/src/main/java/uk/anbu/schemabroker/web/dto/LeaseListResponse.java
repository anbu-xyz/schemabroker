package uk.anbu.schemabroker.web.dto;

import java.util.List;
import uk.anbu.schemabroker.model.SchemaLease;

public record LeaseListResponse(List<SchemaLease> activeLeases,
                                List<SchemaLease> activeExpiredLeases,
                                List<SchemaLease> nonActive) {
}