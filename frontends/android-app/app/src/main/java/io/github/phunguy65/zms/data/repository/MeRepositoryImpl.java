package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.remote.api.MeApi;
import io.github.phunguy65.zms.data.remote.dto.UserManagementPutUserRequest;
import io.github.phunguy65.zms.data.remote.dto.UserManagementUserResponse;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.MeRepository;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Implementation of {@link MeRepository} backed by the remote API via Retrofit.
 *
 * <p>All API calls are executed asynchronously using {@link CompletableFuture} on the I/O
 * executor. Exceptions from network calls are wrapped in {@link CompletionException}.
 */
public class MeRepositoryImpl implements MeRepository {

    private final MeApi meApi;
    private final Executor ioExecutor;

    @Inject
    public MeRepositoryImpl(MeApi meApi, @IoExecutor Executor ioExecutor) {
        this.meApi = meApi;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<User> getMe() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<UserManagementUserResponse> response =
                                meApi.getMe().execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Get me failed: HTTP " + response.code());
                        }

                        return mapToUser(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<User> updateMe(String fullName, String username, String avatarUrl) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UserManagementPutUserRequest request = new UserManagementPutUserRequest();
                        request.setFullName(fullName);
                        request.setUsername(username);
                        request.setAvatarUrl(avatarUrl);

                        Response<UserManagementUserResponse> response =
                                meApi.putMe(request).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Update me failed: HTTP " + response.code());
                        }

                        return mapToUser(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<Void> deleteMe() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<?> response = meApi.deleteMe().execute();

                        if (!response.isSuccessful()) {
                            throw new IOException("Delete me failed: HTTP " + response.code());
                        }

                        return null;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    private User mapToUser(UserManagementUserResponse body) {
        return new User(
                body.getId() != null ? body.getId().toString() : null,
                body.getEmail(),
                body.getFullName(),
                body.getUsername(),
                body.getAvatarUrl());
    }
}
