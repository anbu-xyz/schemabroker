package uk.anbu.schemabroker.schema.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class LeasePropertyWriter {

    private final Path target;

    public LeasePropertyWriter(Path target) {
        this.target = target;
    }

    public void write(SchemaLease lease) {
        var leaseProperties = leaseProperties(lease);

        Path parent = target.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException ignored) {
                // ignore if parent already exists or cannot be created, will surface when writing
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            leaseProperties.store(writer, "Schema lease fetched from broker");
        } catch (IOException ex) {
            throw new SchemaClientException("Unable to write lease data to " + target, ex);
        }
    }

    private static Properties leaseProperties(SchemaLease lease) {
        Properties properties = new Properties();
        properties.setProperty("schema.lease.id", lease.leaseId());
        properties.setProperty("schema.name", lease.schema());
        properties.setProperty("schema.expiresAt", lease.expiresAt().toString());
        properties.setProperty("spring.datasource.url", safe(lease.jdbcUrl()));
        properties.setProperty("spring.datasource.username", safe(lease.loginUser()));
        properties.setProperty("spring.datasource.password", safe(lease.loginPassword()));
        return properties;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

