package io.github.phunguy65.zms.data.remote.interceptor;

import io.github.phunguy65.zms.data.local.TokenManager;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that injects Bearer token for authenticated API requests.
 *
 * <p>When an access token is available in {@link TokenManager}, this interceptor
 * adds the {@code Authorization: Bearer <token>} header to outgoing requests.
 * If no token is available, the request proceeds without the header.
 *
 * <p>This interceptor should be added to the OkHttpClient chain before
 * {@link JsendUnwrapInterceptor} to ensure the token is included in requests
 * before response processing.
 */
@Singleton
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    @Inject
    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        Request authenticatedRequest = originalRequest
                .newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
