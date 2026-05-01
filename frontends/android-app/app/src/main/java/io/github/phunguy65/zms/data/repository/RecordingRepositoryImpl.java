package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.remote.api.RecordingsApi;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.repository.RecordingRepository;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Implementation of {@link RecordingRepository} using the generated {@link RecordingsApi}
 * to start and stop meeting recordings through the meeting-management service.
 */
public class RecordingRepositoryImpl implements RecordingRepository {

    private final RecordingsApi recordingsApi;
    private final Executor ioExecutor;

    @Inject
    public RecordingRepositoryImpl(RecordingsApi recordingsApi, @IoExecutor Executor ioExecutor) {
        this.recordingsApi = recordingsApi;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<Void> startRecording(String meetingId) {
        UUID meetingUuid;
        try {
            meetingUuid = UUID.fromString(meetingId);
        } catch (IllegalArgumentException e) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                    new IllegalArgumentException("Invalid meeting id: " + meetingId, e));
            return failed;
        }

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<?> response =
                                recordingsApi.startRecording(meetingUuid).execute();

                        if (!response.isSuccessful()) {
                            throw new IOException(
                                    "Start recording failed: HTTP " + response.code());
                        }

                        return null;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<Void> stopRecording(String meetingId) {
        UUID meetingUuid;
        try {
            meetingUuid = UUID.fromString(meetingId);
        } catch (IllegalArgumentException e) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                    new IllegalArgumentException("Invalid meeting id: " + meetingId, e));
            return failed;
        }

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<Void> response =
                                recordingsApi.stopRecording(meetingUuid).execute();

                        if (!response.isSuccessful()) {
                            throw new IOException("Stop recording failed: HTTP " + response.code());
                        }

                        return null;
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }
}
