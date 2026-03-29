package uk.anbu.schemabroker.schema.client;

public final class SchemaClientException extends RuntimeException {

    private final int httpStatusCode;

    public SchemaClientException(String message) {
        super(message);
        this.httpStatusCode = -1;
    }

    public SchemaClientException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = -1;
    }

    public SchemaClientException(int statusCode, String message) {
        super(message);
        this.httpStatusCode = statusCode;
    }

    public SchemaClientException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = statusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
