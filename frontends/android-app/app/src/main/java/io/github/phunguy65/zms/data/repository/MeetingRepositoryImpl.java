package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.mapper.MeetingMapper;
import io.github.phunguy65.zms.data.remote.api.InviteManagementApi;
import io.github.phunguy65.zms.data.remote.api.InviteTokensApi;
import io.github.phunguy65.zms.data.remote.api.MeetingsApi;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementCreateInstantMeetingRequest;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementCursorScrollResponseMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementInviteeListResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementInviteeRequest;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingSettingsRequest;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingSettingsResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementScheduleMeetingRequest;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementValidateInviteTokenRequest;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementValidateInviteTokenResponse;
import io.github.phunguy65.zms.data.remote.interceptor.AndroidErrorTranslator;
import io.github.phunguy65.zms.data.remote.interceptor.ApiErrorException;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.InstantMeetingSettings;
import io.github.phunguy65.zms.domain.model.InviteTokenValidationResult;
import io.github.phunguy65.zms.domain.model.InviteeInfo;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.model.MeetingDetail;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.ScheduleMeetingRequest;
import io.github.phunguy65.zms.domain.model.UpcomingMeeting;
import io.github.phunguy65.zms.domain.model.UpdateSettingsResult;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Implementation of {@link MeetingRepository} backed by remote API.
 * Handles meeting creation with error translation and background execution.
 */
public class MeetingRepositoryImpl implements MeetingRepository {

    private static final String ADMISSION_POLICY_WAITING_ROOM = "MANUAL_APPROVAL";
    private static final String ADMISSION_POLICY_OPEN = "ALLOW_ALL";
    private static final int DEFAULT_MAX_PARTICIPANTS = 100;
    private static final int UPCOMING_PAGE_SIZE = 20;

    private final MeetingsApi meetingsApi;
    private final InviteTokensApi inviteTokensApi;
    private final InviteManagementApi inviteManagementApi;
    private final MeetingMapper meetingMapper;
    private final AndroidErrorTranslator errorTranslator;
    private final Executor ioExecutor;

    @Inject
    public MeetingRepositoryImpl(
            MeetingsApi meetingsApi,
            InviteTokensApi inviteTokensApi,
            InviteManagementApi inviteManagementApi,
            MeetingMapper meetingMapper,
            AndroidErrorTranslator errorTranslator,
            @IoExecutor Executor ioExecutor) {
        this.meetingsApi = meetingsApi;
        this.inviteTokensApi = inviteTokensApi;
        this.inviteManagementApi = inviteManagementApi;
        this.meetingMapper = meetingMapper;
        this.errorTranslator = errorTranslator;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<MeetingCreationResult> createInstantMeeting(
            InstantMeetingSettings settings) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        MeetingManagementCreateInstantMeetingRequest request =
                                buildInstantMeetingRequest(settings);
                        Response<MeetingManagementMeetingResponse> response =
                                meetingsApi.createInstantMeeting(request).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Create instant meeting failed: HTTP " + response.code());
                        }

                        return meetingMapper.toMeetingCreationResult(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<MeetingCreationResult> scheduleMeeting(
            ScheduleMeetingRequest request) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        MeetingManagementScheduleMeetingRequest apiRequest =
                                buildScheduleMeetingRequest(request);
                        Response<MeetingManagementMeetingResponse> response =
                                meetingsApi.scheduleMeeting(apiRequest).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Schedule meeting failed: HTTP " + response.code());
                        }

                        return meetingMapper.toMeetingCreationResult(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<List<UpcomingMeeting>> getUpcomingHostMeetings() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<MeetingManagementCursorScrollResponseMeetingResponse> response =
                                meetingsApi
                                        .listHostMeetings(UPCOMING_PAGE_SIZE, null)
                                        .execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "List host meetings failed: HTTP " + response.code());
                        }

                        List<MeetingManagementMeetingResponse> content =
                                response.body().getContent();
                        if (content == null) {
                            return List.of();
                        }

                        OffsetDateTime now = OffsetDateTime.now();

                        return content.stream()
                                .filter(m -> m.getStatus() != null
                                        && MeetingManagementMeetingResponse.StatusEnum.SCHEDULED
                                                == m.getStatus())
                                .filter(m -> m.getStartTime() != null
                                        && m.getStartTime().isAfter(now))
                                .map(meetingMapper::toUpcomingMeeting)
                                .sorted(Comparator.comparing(UpcomingMeeting::startTime))
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<MeetingDetail> getMeetingDetail(String meetingId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID id = UUID.fromString(meetingId);
                        Response<MeetingManagementMeetingResponse> response =
                                meetingsApi.getMeeting(id).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Get meeting detail failed: HTTP " + response.code());
                        }

                        return meetingMapper.toMeetingDetail(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<UpdateSettingsResult> updateMeetingSettings(
            String meetingId, MeetingSettings settings) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID id = UUID.fromString(meetingId);
                        MeetingManagementMeetingSettingsRequest request =
                                buildMeetingSettingsRequest(settings);

                        Response<MeetingManagementMeetingSettingsResponse> response =
                                meetingsApi.putMeetingSettings(id, request).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Update meeting settings failed: HTTP " + response.code());
                        }

                        MeetingManagementMeetingSettingsResponse body = response.body();
                        MeetingSettings updatedSettings = meetingMapper.toMeetingSettings(body);

                        int invalidatedCount = body.getInvalidatedInviteCount() != null
                                ? body.getInvalidatedInviteCount()
                                : 0;
                        boolean resendRecommended =
                                Boolean.TRUE.equals(body.getResendInvitesRecommended());

                        return new UpdateSettingsResult(
                                updatedSettings, invalidatedCount, resendRecommended);
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<Void> cancelMeeting(String meetingId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID id = UUID.fromString(meetingId);
                        Response<Void> response = meetingsApi.cancelMeeting(id).execute();

                        if (!response.isSuccessful()) {
                            throw new IOException("Cancel meeting failed: HTTP " + response.code());
                        }

                        return null;
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<Void> endMeeting(String meetingId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID id = UUID.fromString(meetingId);
                        Response<Void> response = meetingsApi.endMeeting(id).execute();

                        if (!response.isSuccessful()) {
                            throw new IOException("End meeting failed: HTTP " + response.code());
                        }

                        return null;
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<MeetingDetail> getMeetingByShortCode(String shortCode) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<MeetingManagementMeetingResponse> response =
                                meetingsApi.getMeetingByShortCode(shortCode).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            int code = response.code();
                            if (code == 404) {
                                throw new MeetingNotFoundException("Meeting not found");
                            }
                            throw new IOException("Get meeting by short code failed: HTTP " + code);
                        }

                        return meetingMapper.toMeetingDetail(response.body());
                    } catch (MeetingNotFoundException e) {
                        throw new CompletionException(e);
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<InviteTokenValidationResult> validateInviteToken(String token) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        MeetingManagementValidateInviteTokenRequest request =
                                new MeetingManagementValidateInviteTokenRequest().token(token);
                        Response<MeetingManagementValidateInviteTokenResponse> response =
                                inviteTokensApi.validateToken(request).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Invite token validation failed: HTTP " + response.code());
                        }

                        MeetingManagementValidateInviteTokenResponse body = response.body();
                        return new InviteTokenValidationResult(
                                body.getMeetingId() != null
                                        ? body.getMeetingId().toString()
                                        : null,
                                body.getShortCode(),
                                Boolean.TRUE.equals(body.getPreApproved()));
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<List<InviteeInfo>> getInvitees(String meetingId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID id = UUID.fromString(meetingId);
                        Response<List<MeetingManagementInviteeListResponse>> response =
                                inviteManagementApi.getInvitees(id).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Get invitees failed: HTTP " + response.code());
                        }

                        return response.body().stream()
                                .map(this::toInviteeInfo)
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<InviteeInfo> resendInvite(String meetingId, String inviteeId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID mId = UUID.fromString(meetingId);
                        UUID iId = UUID.fromString(inviteeId);
                        Response<MeetingManagementInviteeListResponse> response =
                                inviteManagementApi.resendInvite(mId, iId).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Resend invite failed: HTTP " + response.code());
                        }

                        return toInviteeInfo(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<InviteeInfo> revokeInvite(String meetingId, String inviteeId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID mId = UUID.fromString(meetingId);
                        UUID iId = UUID.fromString(inviteeId);
                        Response<MeetingManagementInviteeListResponse> response =
                                inviteManagementApi.revokeInvite(mId, iId).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Revoke invite failed: HTTP " + response.code());
                        }

                        return toInviteeInfo(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    /**
     * Builds the API request for instant meeting creation.
     * Uses default settings with waiting room enabled.
     */
    private MeetingManagementCreateInstantMeetingRequest buildInstantMeetingRequest(
            InstantMeetingSettings settings) {
        MeetingManagementMeetingSettingsRequest settingsRequest =
                buildMeetingSettings(settings.isWaitingRoomEnabled());

        return new MeetingManagementCreateInstantMeetingRequest().settings(settingsRequest);
    }

    /**
     * Builds the API request for scheduled meeting creation.
     * Maps all user-selected settings to the backend request.
     * Invitee emails are mapped to {@link MeetingManagementInviteeRequest} objects
     * when the domain request carries a non-null invitee list.
     */
    private MeetingManagementScheduleMeetingRequest buildScheduleMeetingRequest(
            ScheduleMeetingRequest request) {
        MeetingManagementMeetingSettingsRequest settingsRequest =
                buildScheduleMeetingSettings(request);

        MeetingManagementScheduleMeetingRequest apiRequest =
                new MeetingManagementScheduleMeetingRequest()
                        .title(request.getTitle())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .settings(settingsRequest);

        List<String> domainInvitees = request.getInvitees();
        if (domainInvitees != null && !domainInvitees.isEmpty()) {
            List<MeetingManagementInviteeRequest> inviteeRequests = domainInvitees.stream()
                    .map(email -> new MeetingManagementInviteeRequest().email(email))
                    .collect(Collectors.toList());
            apiRequest.invitees(inviteeRequests);
        }

        return apiRequest;
    }

    /**
     * Builds meeting settings for scheduled meetings using actual user selections.
     *
     * <p>Uses the simplified meeting settings contract: admissionPolicy, allowGuest,
     * maxParticipants, allowScreenShare, chatEnabled, allowMicrophone, allowVideo,
     * and optional password.
     *
     * @param request the schedule meeting request containing user-selected settings
     */
    private MeetingManagementMeetingSettingsRequest buildScheduleMeetingSettings(
            ScheduleMeetingRequest request) {
        var settings = request.getSettings();

        MeetingManagementMeetingSettingsRequest settingsRequest =
                new MeetingManagementMeetingSettingsRequest()
                        .admissionPolicy(
                                settings.isWaitingRoomEnabled()
                                        ? ADMISSION_POLICY_WAITING_ROOM
                                        : ADMISSION_POLICY_OPEN)
                        .allowGuest(settings.isAllowGuest())
                        .maxParticipants(settings.getMaxParticipants())
                        .allowScreenShare(settings.isAllowScreenShare())
                        .chatEnabled(settings.isChatEnabled())
                        .allowMicrophone(settings.isAllowMicrophone())
                        .allowVideo(settings.isAllowVideo());

        if (settings.hasPassword()) {
            settingsRequest.password(settings.getPassword());
        }

        return settingsRequest;
    }

    /**
     * Builds meeting settings request with sensible defaults using the simplified contract.
     *
     * <p>Uses the simplified field set: admissionPolicy, allowGuest, maxParticipants,
     * allowScreenShare, chatEnabled, allowMicrophone, allowVideo.
     *
     * @param waitingRoomEnabled whether to enable the waiting room (maps to admission policy)
     */
    private MeetingManagementMeetingSettingsRequest buildMeetingSettings(
            boolean waitingRoomEnabled) {
        return new MeetingManagementMeetingSettingsRequest()
                .admissionPolicy(
                        waitingRoomEnabled ? ADMISSION_POLICY_WAITING_ROOM : ADMISSION_POLICY_OPEN)
                .allowGuest(true)
                .maxParticipants(DEFAULT_MAX_PARTICIPANTS)
                .allowScreenShare(true)
                .chatEnabled(true)
                .allowMicrophone(true)
                .allowVideo(true);
    }

    /**
     * Builds meeting settings request from a MeetingSettings domain model.
     * Used for updating meeting settings.
     *
     * @param settings the domain settings to convert
     */
    private MeetingManagementMeetingSettingsRequest buildMeetingSettingsRequest(
            MeetingSettings settings) {
        MeetingManagementMeetingSettingsRequest request =
                new MeetingManagementMeetingSettingsRequest()
                        .admissionPolicy(
                                settings.isWaitingRoomEnabled()
                                        ? ADMISSION_POLICY_WAITING_ROOM
                                        : ADMISSION_POLICY_OPEN)
                        .allowGuest(settings.isAllowGuest())
                        .maxParticipants(settings.getMaxParticipants())
                        .allowScreenShare(settings.isAllowScreenShare())
                        .chatEnabled(settings.isChatEnabled())
                        .allowMicrophone(settings.isAllowMicrophone())
                        .allowVideo(settings.isAllowVideo());

        if (settings.hasPassword()) {
            request.password(settings.getPassword());
        }

        return request;
    }

    /**
     * Maps an invitee list response DTO to the domain InviteeInfo model.
     */
    private InviteeInfo toInviteeInfo(MeetingManagementInviteeListResponse source) {
        return new InviteeInfo(
                source.getInviteeId() != null ? source.getInviteeId().toString() : null,
                source.getUserId() != null ? source.getUserId().toString() : null,
                source.getEmail(),
                source.getDisplayName(),
                source.getStatus() != null ? source.getStatus().getValue() : null,
                source.getInvitedAt(),
                source.getRespondedAt(),
                source.getTokenStatus());
    }

    /**
     * Translates various exceptions into user-friendly localized messages.
     */
    private Exception translateException(Exception e) {
        if (e instanceof ApiFailException) {
            ApiFailException failException = (ApiFailException) e;
            String translatedMessage =
                    errorTranslator.translate(failException.getCode(), failException.getMessage());
            return new MeetingCreationException(translatedMessage);
        }

        if (e instanceof ApiErrorException) {
            return new MeetingCreationException(
                    errorTranslator.translate("SERVER_ERROR", e.getMessage()));
        }

        if (e instanceof UnknownHostException || e instanceof SocketTimeoutException) {
            return new MeetingCreationException(errorTranslator.translate(
                    "NETWORK_ERROR", "No internet connection. Please check your network."));
        }

        if (e instanceof IOException) {
            return new MeetingCreationException(errorTranslator.translate(
                    "SERVER_ERROR", "Something went wrong. Please try again later."));
        }

        if (e instanceof CompletionException && e.getCause() != null) {
            return translateException((Exception) e.getCause());
        }

        return new MeetingCreationException(
                errorTranslator.translate("UNKNOWN_ERROR", "An unexpected error occurred."));
    }

    /**
     * Custom exception for meeting creation errors with user-friendly messages.
     */
    public static class MeetingCreationException extends RuntimeException {
        public MeetingCreationException(String message) {
            super(message);
        }
    }

    /**
     * Exception indicating a meeting was not found by short code.
     * Used to distinguish 404 from other errors for UI handling.
     */
    public static class MeetingNotFoundException extends RuntimeException {
        public MeetingNotFoundException(String message) {
            super(message);
        }
    }
}
