package io.github.phunguy65.zms.data.remote.interceptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.data.local.TokenManager;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AuthInterceptor}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthInterceptorTest {

    @Mock
    private TokenManager tokenManager;

    @Mock
    private Interceptor.Chain chain;

    private AuthInterceptor interceptor;
    private Request originalRequest;

    @Before
    public void setup() {
        interceptor = new AuthInterceptor(tokenManager);
        originalRequest = new Request.Builder()
                .url("https://api.example.com/test")
                .build();
        when(chain.request()).thenReturn(originalRequest);
    }

    @Test
    public void intercept_withValidToken_addsAuthorizationHeader() throws Exception {
        when(tokenManager.getAccessToken()).thenReturn("valid_token_123");
        Response mockResponse = createMockResponse(originalRequest);
        when(chain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor.intercept(chain);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(requestCaptor.capture());
        Request modifiedRequest = requestCaptor.getValue();

        assertEquals("Bearer valid_token_123", modifiedRequest.header("Authorization"));
    }

    @Test
    public void intercept_withNullToken_proceedsWithoutHeader() throws Exception {
        when(tokenManager.getAccessToken()).thenReturn(null);
        Response mockResponse = createMockResponse(originalRequest);
        when(chain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor.intercept(chain);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(requestCaptor.capture());
        Request modifiedRequest = requestCaptor.getValue();

        assertNull(modifiedRequest.header("Authorization"));
    }

    @Test
    public void intercept_withEmptyToken_proceedsWithoutHeader() throws Exception {
        when(tokenManager.getAccessToken()).thenReturn("");
        Response mockResponse = createMockResponse(originalRequest);
        when(chain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor.intercept(chain);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(requestCaptor.capture());
        Request modifiedRequest = requestCaptor.getValue();

        assertNull(modifiedRequest.header("Authorization"));
    }

    @Test
    public void intercept_headerFormat_bearerPrefix() throws Exception {
        when(tokenManager.getAccessToken()).thenReturn("mytoken");
        Response mockResponse = createMockResponse(originalRequest);
        when(chain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor.intercept(chain);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(requestCaptor.capture());
        Request modifiedRequest = requestCaptor.getValue();

        String auth = modifiedRequest.header("Authorization");
        assertTrue("Header should start with 'Bearer '", auth.startsWith("Bearer "));
        assertEquals("Bearer mytoken", auth);
    }

    @Test
    public void intercept_doesNotModifyOtherHeaders() throws Exception {
        originalRequest = new Request.Builder()
                .url("https://api.example.com/test")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();
        when(chain.request()).thenReturn(originalRequest);
        when(tokenManager.getAccessToken()).thenReturn("token");
        Response mockResponse = createMockResponse(originalRequest);
        when(chain.proceed(any(Request.class))).thenReturn(mockResponse);

        interceptor.intercept(chain);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(requestCaptor.capture());
        Request modifiedRequest = requestCaptor.getValue();

        assertEquals("application/json", modifiedRequest.header("Content-Type"));
        assertEquals("application/json", modifiedRequest.header("Accept"));
    }

    private Response createMockResponse(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("{}", okhttp3.MediaType.parse("application/json")))
                .build();
    }
}
