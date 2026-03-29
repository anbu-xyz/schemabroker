package uk.anbu.schemabroker.schema.client;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class SchemaClientAcquireLease {

    public static void main(String[] args) {
        try {
            LeaseCliOptions options = LeaseCliOptions.parse(args);
            SchemaBrokerClient client = new SchemaBrokerClient(options.brokerUrl().toString());
            SchemaLease lease = client.acquireLease(new SchemaLeaseRequest(options.owner(), options.metadata()));
            LeasePropertyWriter writer = new LeasePropertyWriter(options.output());
            writer.write(lease);
            System.out.printf("Acquired lease %s for %s and wrote %s%n",
                    lease.leaseId(), lease.schema(), options.output().toAbsolutePath());

            startHeartbeat(options);
        } catch (SchemaClientException | IllegalArgumentException ex) {
            System.err.println("Schema client failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void startHeartbeat(LeaseCliOptions options) {
        long mavenPid = findParentPid();
        Path logFile = defaultLog(options.output());

        String classpath = resolveClasspath();

        ProcessBuilder builder = new ProcessBuilder(
                "java",
                "-cp",
                classpath,
                "uk.anbu.schemabroker.schema.client.SchemaClientHeartbeat",
                "--broker-url", options.brokerUrl().toString(),
                "--input", options.output().toString(),
                "--maven-pid", Long.toString(mavenPid),
                "--log-file", logFile.toString()
        );

        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

        try {
            var childProcess = builder.start();
            int currentPid = childProcess.pid() > Integer.MAX_VALUE ? -1 : (int) childProcess.pid();
            System.out.printf("Started heartbeat watcher pid: %d (maven-pid=%d) logging to %s%n", currentPid,
                    mavenPid, logFile.toAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Failed to start heartbeat watcher: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    private static long findParentPid() {
        return ProcessHandle.current()
                .parent()
                .map(ProcessHandle::pid)
                .orElseThrow(() -> new IllegalStateException("Unable to determine parent (maven) pid"));
    }

    private static Path defaultLog(Path leaseFile) {
        Path parent = leaseFile.getParent();
        return parent != null ? parent.resolve("schema-client-heartbeat.log") : Path.of("schema-client-heartbeat.log");
    }

    private static String resolveClasspath() {
        // Try thread context classloader first (exec-maven-plugin uses this with test scope)
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        String classpath = extractClasspathFromLoader(contextLoader);
        
        // Fall back to current class's classloader
        if (classpath == null || classpath.isBlank()) {
            classpath = extractClasspathFromLoader(SchemaClientAcquireLease.class.getClassLoader());
        }
        
        // Fall back to system properties
        if (classpath == null || classpath.isBlank()) {
            classpath = expandArgFile(System.getProperty("java.class.path"));
        }
        
        if (classpath == null || classpath.isBlank()) {
            classpath = expandArgFile(ManagementFactory.getRuntimeMXBean().getClassPath());
        }

        if (classpath == null || classpath.isBlank()) {
            String selfLocation = pathFromUrl(SchemaClientHeartbeat.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation());
            return selfLocation;
        }

        return classpath;
    }
    
    private static String extractClasspathFromLoader(ClassLoader loader) {
        if (loader instanceof URLClassLoader urlClassLoader) {
            return Arrays.stream(urlClassLoader.getURLs())
                    .map(SchemaClientAcquireLease::pathFromUrl)
                    .collect(Collectors.joining(File.pathSeparator));
        }
        return null;
    }

    private static String expandArgFile(String classpath) {
        if (classpath == null || classpath.isBlank()) {
            return classpath;
        }

        // Exec Maven plugin may pass classpath via @argfile to avoid long command lines
        if (classpath.startsWith("@")) {
            Path argFile = Path.of(classpath.substring(1));
            if (Files.isRegularFile(argFile)) {
                try {
                    return Files.readString(argFile).trim();
                } catch (IOException ignored) {
                    // fall through to return original
                }
            }
        }

        return classpath;
    }

    private static String pathFromUrl(URL url) {
        try {
            return Paths.get(url.toURI()).toString();
        } catch (URISyntaxException ex) {
            return url.getPath();
        }
    }
}

