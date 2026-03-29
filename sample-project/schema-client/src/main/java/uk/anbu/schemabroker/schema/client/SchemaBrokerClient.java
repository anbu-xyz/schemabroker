package uk.anbu.schemabroker.schema.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public final class SchemaBrokerClient {

    private final HttpClient httpClient;
    private final URI baseUri;
    private final ObjectMapper objectMapper;

    public SchemaBrokerClient(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(10));
    }

    public SchemaBrokerClient(String baseUrl, Duration timeout) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        this.baseUri = normalizeBaseUri(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public SchemaLease acquireLease(SchemaLeaseRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/api/v1/leases"))
                .header("Content-Type", "application/json")
                .POST(buildBodyPublisher(request))
                .build();
        return sendRequest(httpRequest);
    }

    public SchemaLease heartbeat(String leaseId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/api/v1/leases/" + leaseId + "/heartbeat"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return sendRequest(request);
    }

    public SchemaLease release(String leaseId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/api/v1/leases/" + leaseId + "/release"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return sendRequest(request);
    }

    private SchemaLease sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int httpStatusCode = response.statusCode();
            if (httpStatusCode >= 200 && httpStatusCode < 300) {
                return objectMapper.readValue(response.body(), SchemaLease.class);
            }
            throw new SchemaClientException(httpStatusCode,
                    "Schema Broker responded with " + httpStatusCode + ": " + response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SchemaClientException(-1, "Schema Broker request interrupted", ex);
        } catch (IOException ex) {
            throw new SchemaClientException(-1, "Failed to communicate with Schema Broker", ex);
        }
    }

    private HttpRequest.BodyPublisher buildBodyPublisher(SchemaLeaseRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            return HttpRequest.BodyPublishers.ofString(payload);
        } catch (IOException ex) {
            throw new SchemaClientException(-1, "Unable to serialize lease request", ex);
        }
    }

    private static URI normalizeBaseUri(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(trimmed);
    }
}

