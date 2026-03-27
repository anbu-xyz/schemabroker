package uk.anbu.schemabroker.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReleaseLeaseResponse {
    private final String leaseId;
    private final String schema;
    private final String status;
}

