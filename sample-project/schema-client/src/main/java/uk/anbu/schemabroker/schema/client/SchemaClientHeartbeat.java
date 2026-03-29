package uk.anbu.schemabroker.schema.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class SchemaClientHeartbeat {

    public static void main(String[] args) {
        HeartbeatCliOptions options;
        Logger logger;
        try {
            options = HeartbeatCliOptions.parse(args);
            logger = buildLogger(options.logFile());
        } catch (Exception ex) {
            System.err.println("Schema client heartbeat failed to start: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
            return;
        }

        LeasePropertyLoader.LeaseInfo leaseInfo;
        try {
            leaseInfo = LeasePropertyLoader.load(options.input());
        } catch (Exception ex) {
            logger.severe("Unable to read lease info: " + ex.getMessage());
            System.exit(1);
            return;
        }

        ProcessHandle mavenProcess = ProcessHandle.of(options.parentPidToWatch())
            .orElseThrow(() -> new IllegalArgumentException(
                "Maven process not found for pid " + options.parentPidToWatch()));

        SchemaBrokerClient client = new SchemaBrokerClient(options.brokerUrl().toString());

        logger.info("Heartbeat started for lease " + leaseInfo.leaseId() + " (schema="
            + leaseInfo.schema() + ") "
            + "watching parent pid to watch=" + options.parentPidToWatch() + " interval="
            + options.interval().getSeconds() + "s " + "maxRuntime="
            + options.maxRuntime().toMinutes()
            + "m log=" + options.logFile().toAbsolutePath());

        Instant deadline = Instant.now().plus(options.maxRuntime());
        while (Instant.now().isBefore(deadline)) {
            if (!mavenProcess.isAlive()) {
                logger.info("Maven pid " + options.parentPidToWatch()
                    + " is no longer alive; releasing lease");
                safeRelease(client, leaseInfo.leaseId(), logger);
                return;
            }

            try {
                SchemaLease lease = client.heartbeat(leaseInfo.leaseId());
                logger.info("Heartbeat ok for lease " + lease.leaseId() + " expiresAt="
                    + lease.expiresAt());
            } catch (SchemaClientException ex) {
                int httpStatusCode = ex.getHttpStatusCode();
                // 404 = HttpStatus.NOT_FOUND, 410 = HttpStatus.GONE
                if (httpStatusCode == 404 || httpStatusCode == 410) {
                    logger.warning("Lease " + leaseInfo.leaseId()
                        + " is no longer active (httpStatusCode " + httpStatusCode
                        + "); stopping heartbeat");
                } else {
                    logger.warning("Heartbeat failed (httpStatusCode " + httpStatusCode + "): "
                        + ex.getMessage());
                }
                if (httpStatusCode >= 400) {
                    logger.warning("Stopping heartbeat due to lease issue");
                    return;
                }
            } catch (Exception ex) {
                logger.warning("Heartbeat failed: " + ex.getMessage());
            }

            if (!sleep(options.interval())) {
                logger.info("Interrupted; releasing lease");
                safeRelease(client, leaseInfo.leaseId(), logger);
                return;
            }
        }

        logger.info("Max runtime reached; releasing lease");
        safeRelease(client, leaseInfo.leaseId(), logger);
    }

    private static void safeRelease(SchemaBrokerClient client, String leaseId, Logger logger) {
        try {
            SchemaLease lease = client.release(leaseId);
            logger.info("Released lease " + lease.leaseId() + " for schema " + lease.schema());
        } catch (Exception ex) {
            logger.warning("Release attempt failed: " + ex.getMessage());
        }
    }

    private static boolean sleep(Duration interval) {
        try {
            Thread.sleep(interval.toMillis());
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static Logger buildLogger(Path logFile) throws IOException {
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Logger logger = Logger.getLogger("SchemaClientHeartbeat");
        logger.setUseParentHandlers(false);

        FileHandler handler = new FileHandler(logFile.toString(), true);
        handler.setFormatter(new SimpleFormatter());
        logger.addHandler(handler);

        return logger;
    }
}
