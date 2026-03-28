package uk.anbu.schemabroker.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AcquireLeaseRequest {
    private String owner;
    private String groupName;
    private Object metadata;
}

