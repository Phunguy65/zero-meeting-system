package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.remote.api.ParticipantsApi;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementParticipantListItemResponse;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.ParticipantRole;
import io.github.phunguy65.zms.domain.model.ParticipantRoleInfo;
import io.github.phunguy65.zms.domain.repository.ParticipantRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Implementation of {@link ParticipantRepository} that uses the generated {@link ParticipantsApi}
 * SDK client to fetch participant role metadata from the meeting-management service.
 */
public class ParticipantRepositoryImpl implements ParticipantRepository {

    private final ParticipantsApi participantsApi;
    private final Executor ioExecutor;

    @Inject
    public ParticipantRepositoryImpl(
            ParticipantsApi participantsApi, @IoExecutor Executor ioExecutor) {
        this.participantsApi = participantsApi;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<List<ParticipantRoleInfo>> getParticipantRoles(String meetingId) {
        UUID meetingUuid;
        try {
            meetingUuid = UUID.fromString(meetingId);
        } catch (IllegalArgumentException e) {
            CompletableFuture<List<ParticipantRoleInfo>> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                    new IllegalArgumentException("Invalid meeting id: " + meetingId, e));
            return failed;
        }

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<List<MeetingManagementParticipantListItemResponse>> response =
                                participantsApi.getParticipants(meetingUuid).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Get participants failed: HTTP " + response.code());
                        }

                        return response.body().stream()
                                .map(ParticipantRepositoryImpl::toDomain)
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    private static ParticipantRoleInfo toDomain(MeetingManagementParticipantListItemResponse dto) {
        String userId = dto.getUserId() != null ? dto.getUserId().toString() : null;
        String displayName = dto.getDisplayName() != null ? dto.getDisplayName() : "";
        ParticipantRole role = toDomainRole(dto.getRole());
        return new ParticipantRoleInfo(userId, displayName, role);
    }

    private static ParticipantRole toDomainRole(
            MeetingManagementParticipantListItemResponse.RoleEnum roleEnum) {
        if (roleEnum == null) {
            return ParticipantRole.PARTICIPANT;
        }
        try {
            return ParticipantRole.valueOf(roleEnum.name());
        } catch (IllegalArgumentException e) {
            return ParticipantRole.PARTICIPANT;
        }
    }
}
