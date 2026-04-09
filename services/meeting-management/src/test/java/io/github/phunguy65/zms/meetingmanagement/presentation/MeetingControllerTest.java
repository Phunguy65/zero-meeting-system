package io.github.phunguy65.zms.meetingmanagement.presentation;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetHostMeetingsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.CancelMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.CreateInstantMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.EndMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetHostMeetingsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetMeetingByShortCodeUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.ScheduleMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.StartMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.UpdateMeetingSettingsUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingType;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.web.WebConfig;
import io.github.phunguy65.zms.shared.domain.CursorErrorCode;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.CursorTokenEncoder;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeetingController.class)
@Import(WebConfig.class)
class MeetingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ScheduleMeetingUseCase scheduleMeetingUseCase;

    @MockitoBean
    CreateInstantMeetingUseCase createInstantMeetingUseCase;

    @MockitoBean
    GetMeetingUseCase getMeetingUseCase;

    @MockitoBean
    GetMeetingByShortCodeUseCase getMeetingByShortCodeUseCase;

    @MockitoBean
    GetHostMeetingsUseCase getHostMeetingsUseCase;

    @MockitoBean
    StartMeetingUseCase startMeetingUseCase;

    @MockitoBean
    EndMeetingUseCase endMeetingUseCase;

    @MockitoBean
    CancelMeetingUseCase cancelMeetingUseCase;

    @MockitoBean
    UpdateMeetingSettingsUseCase updateMeetingSettingsUseCase;

    @MockitoBean
    CursorTokenEncoder cursorTokenEncoder;

    @Test
    void listHostMeetings_returnsCursorEnvelopeAndNextPageToken() throws Exception {
        UUID hostId = UUID.randomUUID();
        UUID firstMeetingId = UUID.randomUUID();
        UUID secondMeetingId = UUID.randomUUID();
        Instant secondCreatedAt = Instant.parse("2026-04-01T09:00:00Z");
        when(getHostMeetingsUseCase.execute(argThat(query -> query.hostId().equals(hostId)
                        && query.pageSize() == 10
                        && query.cursor() == null)))
                .thenReturn(CursorPageResponse.of(
                        List.of(
                                new MeetingResponse(
                                        firstMeetingId,
                                        hostId,
                                        "ABC123",
                                        "Design Review",
                                        "Sprint planning",
                                        Instant.parse("2026-04-02T10:00:00Z"),
                                        Instant.parse("2026-04-02T11:00:00Z"),
                                        MeetingType.SCHEDULED,
                                        MeetingStatus.SCHEDULED,
                                        new MeetingSettingsResponse(
                                                "MANUAL_APPROVAL",
                                                90,
                                                true,
                                                true,
                                                50,
                                                true,
                                                "HOST_ONLY",
                                                true,
                                                true),
                                        Instant.parse("2026-04-01T08:00:00Z")),
                                new MeetingResponse(
                                        secondMeetingId,
                                        hostId,
                                        "XYZ789",
                                        null,
                                        null,
                                        null,
                                        null,
                                        MeetingType.INSTANT,
                                        MeetingStatus.LIVE,
                                        new MeetingSettingsResponse(
                                                "AUTO_APPROVE",
                                                null,
                                                false,
                                                false,
                                                10,
                                                false,
                                                "EVERYONE",
                                                false,
                                                false),
                                        secondCreatedAt)),
                        10,
                        true));
        when(cursorTokenEncoder.encode(secondCreatedAt, secondMeetingId)).thenReturn("next-token");

        mockMvc.perform(get("/api/v1/meetings")
                        .param("pageSize", "10")
                        .principal(new TestingAuthenticationToken(hostId.toString(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(firstMeetingId.toString()))
                .andExpect(jsonPath("$.data.content[1].id").value(secondMeetingId.toString()))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.nextPageToken").value("next-token"));

        verify(cursorTokenEncoder).encode(secondCreatedAt, secondMeetingId);
    }

    @Test
    void listHostMeetings_decodesPageTokenBeforeCallingUseCase() throws Exception {
        UUID hostId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        ScrollCursor cursor =
                new ScrollCursor(Instant.parse("2026-04-01T08:00:00Z"), UUID.randomUUID());
        when(cursorTokenEncoder.decode("valid-token")).thenReturn(Result.success(cursor));
        when(getHostMeetingsUseCase.execute(argThat(
                        query -> query.equals(new GetHostMeetingsQuery(hostId, 15, cursor)))))
                .thenReturn(CursorPageResponse.of(
                        List.of(new MeetingResponse(
                                meetingId,
                                hostId,
                                "ABC123",
                                null,
                                null,
                                null,
                                null,
                                MeetingType.INSTANT,
                                MeetingStatus.LIVE,
                                new MeetingSettingsResponse(
                                        "AUTO_APPROVE",
                                        null,
                                        false,
                                        false,
                                        10,
                                        false,
                                        "EVERYONE",
                                        false,
                                        false),
                                Instant.parse("2026-04-01T09:00:00Z"))),
                        15,
                        false));

        mockMvc.perform(get("/api/v1/meetings")
                        .param("pageSize", "15")
                        .param("pageToken", "valid-token")
                        .principal(new TestingAuthenticationToken(hostId.toString(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(15))
                .andExpect(jsonPath("$.data.nextPageToken").doesNotExist());

        verify(cursorTokenEncoder).decode("valid-token");
    }

    @Test
    void listHostMeetings_returns400ForInvalidPageToken() throws Exception {
        UUID hostId = UUID.randomUUID();
        when(cursorTokenEncoder.decode("bad-token"))
                .thenReturn(Result.failure(CursorErrorCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/v1/meetings")
                        .param("pageToken", "bad-token")
                        .principal(new TestingAuthenticationToken(hostId.toString(), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("INVALID_CURSOR"));

        verifyNoInteractions(getHostMeetingsUseCase);
    }
}
