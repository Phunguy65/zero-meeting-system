package io.github.phunguy65.zms.sdk.jsend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * OkHttp {@link Interceptor} that unwraps JSend envelopes before Retrofit deserialisation.
 *
 * <ul>
 *   <li>{@code "success"}: replaces the response body with the raw JSON value of {@code data}.</li>
 *   <li>{@code "fail"}: throws {@link ApiFailException} with {@code code}, {@code message},
 *       and {@code violations} parsed from the {@code data.FailData} object.</li>
 *   <li>{@code "error"}: throws {@link ApiErrorException} with the {@code message}.</li>
 * </ul>
 *
 * <p>The interceptor applies only to responses with a JSON content type. Non-JSON responses
 * (e.g. file downloads, SSE streams) are passed through unchanged.
 */
public final class JsendUnwrapInterceptor implements Interceptor {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private final Gson gson;
    private final ErrorTranslator translator;

    /**
     * Creates an interceptor with the given {@link ErrorTranslator}.
     *
     * @param translator translates machine-readable error codes to locale-specific messages
     */
    public JsendUnwrapInterceptor(ErrorTranslator translator) {
        this.gson = new Gson();
        this.translator = translator != null ? translator : ErrorTranslator.DEFAULT;
    }

    /** Creates an interceptor with the default (pass-through) translator. */
    public JsendUnwrapInterceptor() {
        this(ErrorTranslator.DEFAULT);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        ResponseBody body = response.body();
        if (body == null) {
            return response;
        }

        MediaType contentType = body.contentType();
        if (contentType == null || !contentType.subtype().contains("json")) {
            return response;
        }

        String rawJson = body.string();

        JsendEnvelope envelope;
        try {
            envelope = gson.fromJson(rawJson, JsendEnvelope.class);
        } catch (Exception e) {
            return response.newBuilder()
                    .body(ResponseBody.create(rawJson, contentType))
                    .build();
        }

        if (envelope == null || envelope.getStatus() == null) {
            return response.newBuilder()
                    .body(ResponseBody.create(rawJson, contentType))
                    .build();
        }

        switch (envelope.getStatus()) {
            case "success":
                return handleSuccess(response, contentType, envelope);

            case "fail":
                throw handleFail(envelope);

            case "error":
                throw handleError(envelope);

            default:
                return response.newBuilder()
                        .body(ResponseBody.create(rawJson, contentType))
                        .build();
        }
    }

    private Response handleSuccess(
            Response response, MediaType contentType, JsendEnvelope envelope) {
        JsonElement data = envelope.getData();
        String dataJson = (data != null && !data.isJsonNull()) ? gson.toJson(data) : "null";
        return response.newBuilder()
                .body(ResponseBody.create(dataJson, JSON_MEDIA_TYPE))
                .build();
    }

    private ApiFailException handleFail(JsendEnvelope envelope) {
        JsonElement data = envelope.getData();
        String code = "";
        String message = "Request failed";
        List<ApiFailException.Violation> violations = new ArrayList<>();

        if (data != null && data.isJsonObject()) {
            JsonObject obj = data.getAsJsonObject();

            if (obj.has("code") && !obj.get("code").isJsonNull()) {
                code = obj.get("code").getAsString();
            }
            if (obj.has("message") && !obj.get("message").isJsonNull()) {
                message = obj.get("message").getAsString();
            }
            if (obj.has("errors") && obj.get("errors").isJsonArray()) {
                JsonArray errors = obj.getAsJsonArray("errors");
                for (JsonElement elem : errors) {
                    if (elem.isJsonObject()) {
                        JsonObject v = elem.getAsJsonObject();
                        String field = v.has("field") ? v.get("field").getAsString() : "";
                        String msg = v.has("message") ? v.get("message").getAsString() : "";
                        String vCode = v.has("code") ? v.get("code").getAsString() : "";
                        violations.add(new ApiFailException.Violation(field, msg, vCode));
                    }
                }
            }
        }

        String translatedMessage = translator.translate(code, message);
        return new ApiFailException(code, translatedMessage, violations);
    }

    private ApiErrorException handleError(JsendEnvelope envelope) {
        String message = envelope.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An unexpected server error occurred";
        }
        return new ApiErrorException(message);
    }
}
