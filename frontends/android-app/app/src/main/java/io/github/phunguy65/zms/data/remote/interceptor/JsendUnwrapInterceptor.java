package io.github.phunguy65.zms.data.remote.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper;
    private final ErrorTranslator translator;

    /**
     * Creates an interceptor with the given {@link ErrorTranslator}.
     *
     * @param translator translates machine-readable error codes to locale-specific messages
     */
    public JsendUnwrapInterceptor(ErrorTranslator translator) {
        this.objectMapper = new ObjectMapper();
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
            envelope = objectMapper.readValue(rawJson, JsendEnvelope.class);
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

    private Response handleSuccess(Response response, MediaType contentType, JsendEnvelope envelope)
            throws IOException {
        JsonNode data = envelope.getData();
        String dataJson =
                (data != null && !data.isNull()) ? objectMapper.writeValueAsString(data) : "null";
        return response.newBuilder()
                .body(ResponseBody.create(dataJson, JSON_MEDIA_TYPE))
                .build();
    }

    private ApiFailException handleFail(JsendEnvelope envelope) {
        JsonNode data = envelope.getData();
        String code = "";
        String message = "Request failed";
        List<ApiFailException.Violation> violations = new ArrayList<>();

        if (data != null && data.isObject()) {
            if (data.has("code") && !data.get("code").isNull()) {
                code = data.get("code").asText();
            }
            if (data.has("message") && !data.get("message").isNull()) {
                message = data.get("message").asText();
            }
            if (data.has("errors") && data.get("errors").isArray()) {
                JsonNode errors = data.get("errors");
                for (JsonNode elem : errors) {
                    if (elem.isObject()) {
                        String field = elem.has("field") ? elem.get("field").asText() : "";
                        String msg = elem.has("message") ? elem.get("message").asText() : "";
                        String vCode = elem.has("code") ? elem.get("code").asText() : "";
                        String translatedViolationMsg = translator.translate(vCode, msg);
                        violations.add(new ApiFailException.Violation(
                                field, translatedViolationMsg, vCode));
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
        String translatedMessage = translator.translate("SERVER_ERROR", message);
        return new ApiErrorException(translatedMessage);
    }
}
