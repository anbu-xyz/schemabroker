package uk.anbu.schemabroker.schema.client;

import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CliOptions {

    private static final String DEFAULT_BROKER_URL = "http://localhost:8080";
    private static final String DEFAULT_OWNER = "schema-client";
    private static final Path DEFAULT_OUTPUT = Path.of("schema-lease.properties");

    private final URI brokerUrl;
    private final String owner;
    private final Path output;
    private final Map<String, String> metadata;

    private CliOptions(URI brokerUrl, String owner, Path output, Map<String, String> metadata) {
        this.brokerUrl = brokerUrl;
        this.owner = owner;
        this.output = output;
        this.metadata = metadata;
    }

    public static CliOptions parse(String[] args) {
        URI brokerUrl = URI.create(DEFAULT_BROKER_URL);
        String owner = DEFAULT_OWNER;
        Path output = DEFAULT_OUTPUT;
        Map<String, String> metadata = new LinkedHashMap<>();

        for (int index = 0; index < args.length; index++) {
            String token = args[index];
            switch (token) {
                case "--broker-url" -> {
                    index = requireNext(index, args);
                    brokerUrl = URI.create(args[index]);
                }
                case "--owner" -> {
                    index = requireNext(index, args);
                    owner = args[index];
                }
                case "--output" -> {
                    index = requireNext(index, args);
                    output = Path.of(args[index]);
                }
                case "--metadata" -> {
                    index = requireNext(index, args);
                    String[] parts = args[index].split("=", 2);
                    if (parts.length != 2 || parts[0].isBlank()) {
                        throw new IllegalArgumentException("Metadata must be in key=value format");
                    }
                    metadata.put(parts[0].trim(), parts[1]);
                }
                default -> throw new IllegalArgumentException("Unknown argument '" + token + "'");
            }
        }
        metadata.putAll(additionalMetadata());
        return new CliOptions(brokerUrl, owner, output, Map.copyOf(metadata));
    }

    private static Map<String, String> additionalMetadata() {
        return Map.of(
                "OS", System.getProperty("os.name") + " " + System.getProperty("os.version"),
                "Java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")",
                "User", System.getProperty("user.name")
        );
    }

    private static int requireNext(int index, String[] args) {
        int next = index + 1;
        if (next >= args.length) {
            throw new IllegalArgumentException("Expected a value after " + args[index]);
        }
        return next;
    }

    public URI brokerUrl() {
        return brokerUrl;
    }

    public String owner() {
        return owner;
    }

    public Path output() {
        return output;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}

