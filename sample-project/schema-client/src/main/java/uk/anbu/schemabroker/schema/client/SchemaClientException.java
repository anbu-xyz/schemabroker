package uk.anbu.schemabroker.schema.client;

public final class SchemaClientException extends RuntimeException {

    public SchemaClientException(String message) {
        super(message);
    }

    public SchemaClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

