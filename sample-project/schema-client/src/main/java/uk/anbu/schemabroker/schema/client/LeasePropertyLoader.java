package uk.anbu.schemabroker.schema.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class LeasePropertyLoader {

    private LeasePropertyLoader() {
    }

    static LeaseInfo load(Path source) {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Lease file not found: " + source.toAbsolutePath());
        }

        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException ex) {
            throw new SchemaClientException(
                "Unable to read lease data from " + source.toAbsolutePath(), ex);
        }

        String leaseId = props.getProperty("schema.lease.id");
        if (leaseId == null || leaseId.isBlank()) {
            throw new IllegalArgumentException(
                "schema.lease.id is missing in " + source.toAbsolutePath());
        }

        String schema = props.getProperty("schema.name", "");
        return new LeaseInfo(leaseId.trim(), schema);
    }

    record LeaseInfo(String leaseId, String schema) {
    }
}
