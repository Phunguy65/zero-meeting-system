package io.github.phunguy65.zms.sdk.jsend;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Mirrors the server-side {@code JsendResponse} record.
 *
 * <p>The {@code data} field is kept as a raw {@link JsonElement} so the interceptor can
 * re-serialize it for Retrofit without knowing the concrete type.
 *
 * <p>On {@code "success"}: {@code data} holds the payload, {@code message} is null.<br>
 * On {@code "fail"}:    {@code data} holds a {@code FailData} object.<br>
 * On {@code "error"}:   {@code data} is null, {@code message} holds the error description.
 */
public final class JsendEnvelope {

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private JsonElement data;

    @SerializedName("message")
    private String message;

    public String getStatus() {
        return status;
    }

    public JsonElement getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
