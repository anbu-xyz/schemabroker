package uk.anbu.schemabroker.schema.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SchemaClientReleaseLease {

    public static void main(String[] args) {
        try {
            ReleaseCliOptions options = ReleaseCliOptions.parse(args);
            LeaseInfo leaseInfo = loadLeaseInfo(options.input());

            SchemaBrokerClient client = new SchemaBrokerClient(options.brokerUrl().toString());
            SchemaLease lease = client.release(leaseInfo.leaseId());

            System.out.printf("Released lease %s for %s from %s%n",
                lease.leaseId(), lease.schema(), options.input().toAbsolutePath());
        } catch (SchemaClientException | IllegalArgumentException ex) {
            System.err.println("Schema client failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static LeaseInfo loadLeaseInfo(Path source) {
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

    private record LeaseInfo(String leaseId, String schema) {
    }
}
