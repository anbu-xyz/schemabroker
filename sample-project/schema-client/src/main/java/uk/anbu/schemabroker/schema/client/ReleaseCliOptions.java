package uk.anbu.schemabroker.schema.client;

import java.net.URI;
import java.nio.file.Path;

public final class ReleaseCliOptions {

    private static final String DEFAULT_BROKER_URL = "http://localhost:8080";
    private static final Path DEFAULT_INPUT = Path.of("schema-lease.properties");

    private final URI brokerUrl;
    private final Path input;

    private ReleaseCliOptions(URI brokerUrl, Path input) {
        this.brokerUrl = brokerUrl;
        this.input = input;
    }

    public static ReleaseCliOptions parse(String[] args) {
        URI brokerUrl = URI.create(DEFAULT_BROKER_URL);
        Path input = DEFAULT_INPUT;

        for (int index = 0; index < args.length; index++) {
            String token = args[index];
            switch (token) {
                case "--broker-url" -> {
                    index = requireNext(index, args);
                    brokerUrl = URI.create(args[index]);
                }
                case "--input" -> {
                    index = requireNext(index, args);
                    input = Path.of(args[index]);
                }
                default -> throw new IllegalArgumentException("Unknown argument '" + token + "'");
            }
        }

        return new ReleaseCliOptions(brokerUrl, input);
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

    public Path input() {
        return input;
    }
}
