package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.remote.api.AuthApi;
import io.github.phunguy65.zms.data.remote.dto.UserManagementGoogleLoginRequest;
import io.github.phunguy65.zms.data.remote.dto.UserManagementLoginRequest;
import io.github.phunguy65.zms.data.remote.dto.UserManagementLoginResponse;
import io.github.phunguy65.zms.data.remote.dto.UserManagementRegisterRequest;
import io.github.phunguy65.zms.data.remote.dto.UserManagementRegisterResponse;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.model.RegisterResult;
import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Implementation of {@link AuthRepository} backed by the remote API via Retrofit.
 *
 * <p>All API calls are executed asynchronously using {@link CompletableFuture} on the I/O
 * executor. Exceptions from network calls are wrapped in {@link CompletionException}.
 */
public class AuthRepositoryImpl implements AuthRepository {

    private final AuthApi authApi;
    private final Executor ioExecutor;

    @Inject
    public AuthRepositoryImpl(AuthApi authApi, @IoExecutor Executor ioExecutor) {
        this.authApi = authApi;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<LoginResult> login(String email, String password) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UserManagementLoginRequest request = new UserManagementLoginRequest();
                        request.setEmail(email);
                        request.setPassword(password);

                        Response<UserManagementLoginResponse> response =
                                authApi.login(request).execute();
                        return mapLoginResponse(response);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<RegisterResult> register(
            String fullName, String username, String email, String password) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UserManagementRegisterRequest request = new UserManagementRegisterRequest();
                        request.setEmail(email);
                        request.setPassword(password);
                        request.setFullName(fullName);
                        request.setUsername(username);

                        Response<UserManagementRegisterResponse> response =
                                authApi.register(request).execute();
                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Register failed: HTTP " + response.code());
                        }

                        UserManagementRegisterResponse body = response.body();
                        return new RegisterResult(
                                body.getUserId(),
                                body.getEmail(),
                                body.getFullName(),
                                body.getUsername());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<LoginResult> googleLogin(String idToken) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UserManagementGoogleLoginRequest request =
                                new UserManagementGoogleLoginRequest();
                        request.setIdToken(idToken);

                        Response<UserManagementLoginResponse> response =
                                authApi.googleLogin(request).execute();
                        return mapLoginResponse(response);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    private LoginResult mapLoginResponse(Response<UserManagementLoginResponse> response)
            throws IOException {
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Auth request failed: HTTP " + response.code());
        }

        UserManagementLoginResponse body = response.body();
        return new LoginResult(
                body.getAccessToken(),
                body.getRefreshToken(),
                body.getExpiresIn() != null ? body.getExpiresIn() : 0L);
    }
}
