package uk.anbu.schemabroker.schema.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public record SchemaLeaseRequest(@JsonProperty("owner") String owner,
                                 @JsonProperty("metadata") Map<String, String> metadata) {

    public SchemaLeaseRequest {
        metadata = metadata == null ? Collections.emptyMap() : Map.copyOf(metadata);
    }
}

