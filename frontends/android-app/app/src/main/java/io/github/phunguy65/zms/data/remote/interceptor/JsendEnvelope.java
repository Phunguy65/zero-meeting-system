package io.github.phunguy65.zms.data.remote.interceptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Mirrors the server-side {@code JsendResponse} record.
 *
 * <p>The {@code data} field is kept as a raw {@link JsonNode} so the interceptor can
 * re-serialize it for Retrofit without knowing the concrete type.
 *
 * <p>On {@code "success"}: {@code data} holds the payload, {@code message} is null.<br>
 * On {@code "fail"}:    {@code data} holds a {@code FailData} object.<br>
 * On {@code "error"}:   {@code data} is null, {@code message} holds the error description.
 */
public final class JsendEnvelope {

    @JsonProperty("status")
    private String status;

    @JsonProperty("data")
    private JsonNode data;

    @JsonProperty("message")
    private String message;

    public String getStatus() {
        return status;
    }

    public JsonNode getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
