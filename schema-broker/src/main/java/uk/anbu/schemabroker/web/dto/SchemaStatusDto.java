package uk.anbu.schemabroker.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
public class SchemaStatusDto {

    private String schema;
    private String groupName;
    private String loginUser;
    private String jdbcUrl;
    private boolean enabled;
    private String status;
    private String leaseId;
    private Instant expiresAt;
    private String owner;
}
