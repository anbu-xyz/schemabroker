package uk.anbu.schemabroker.schema.client;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

public final class HeartbeatCliOptions {

    private static final String DEFAULT_BROKER_URL = "http://localhost:8080";
    private static final Path DEFAULT_INPUT = Path.of("schema-lease.properties");
    private static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_MAX_RUNTIME = Duration.ofMinutes(30);

    private final URI brokerUrl;
    private final Path input;
    private final long mavenPid;
    private final Duration interval;
    private final Duration maxRuntime;
    private final Path logFile;

    private HeartbeatCliOptions(URI brokerUrl, Path input, long mavenPid, Duration interval,
                                Duration maxRuntime, Path logFile) {
        this.brokerUrl = brokerUrl;
        this.input = input;
        this.mavenPid = mavenPid;
        this.interval = interval;
        this.maxRuntime = maxRuntime;
        this.logFile = logFile;
    }

    public static HeartbeatCliOptions parse(String[] args) {
        URI brokerUrl = URI.create(DEFAULT_BROKER_URL);
        Path input = DEFAULT_INPUT;
        Long mavenPid = null;
        Duration interval = DEFAULT_INTERVAL;
        Duration maxRuntime = DEFAULT_MAX_RUNTIME;
        Path logFile = null;

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
                case "--maven-pid" -> {
                    index = requireNext(index, args);
                    mavenPid = Long.parseLong(args[index]);
                }
                case "--interval-seconds" -> {
                    index = requireNext(index, args);
                    interval = Duration.ofSeconds(Long.parseLong(args[index]));
                }
                case "--max-runtime-seconds" -> {
                    index = requireNext(index, args);
                    maxRuntime = Duration.ofSeconds(Long.parseLong(args[index]));
                }
                case "--log-file" -> {
                    index = requireNext(index, args);
                    logFile = Path.of(args[index]);
                }
                default -> throw new IllegalArgumentException("Unknown argument '" + token + "'");
            }
        }

        if (mavenPid == null) {
            throw new IllegalArgumentException("--maven-pid is required");
        }

        if (logFile == null) {
            Path parent = input.getParent();
            logFile = parent != null ? parent.resolve("schema-client-heartbeat.log") : Path.of("schema-client-heartbeat.log");
        }

        return new HeartbeatCliOptions(brokerUrl, input, mavenPid, interval, maxRuntime, logFile);
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

    public long parentPidToWatch() {
        return mavenPid;
    }

    public Duration interval() {
        return interval;
    }

    public Duration maxRuntime() {
        return maxRuntime;
    }

    public Path logFile() {
        return logFile;
    }
}
