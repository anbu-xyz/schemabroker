package uk.anbu.schemabroker.web.dto;

import java.util.List;

public record StatusResponse(long ttlSeconds, List<SchemaStatusDto> schemas) {
}