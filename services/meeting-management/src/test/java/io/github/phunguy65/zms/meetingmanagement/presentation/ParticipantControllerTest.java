package io.github.phunguy65.zms.meetingmanagement.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.phunguy65.zms.meetingmanagement.application.response.ParticipantListItemResponse;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetParticipantsUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.web.WebConfig;
import io.github.phunguy65.zms.shared.domain.Result;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ParticipantController.class)
@Import(WebConfig.class)
class ParticipantControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GetParticipantsUseCase getParticipantsUseCase;

    @Test
    void getParticipants_returnsSimpleListResponse() throws Exception {
        UUID meetingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(getParticipantsUseCase.execute(any()))
                .thenReturn(Result.success(List.of(new ParticipantListItemResponse(
                        21L,
                        meetingId,
                        userId,
                        "Alice",
                        ParticipantRole.HOST,
                        Instant.parse("2026-04-01T10:00:00Z"),
                        null))));

        mockMvc.perform(get("/api/v1/meetings/{id}/participants", meetingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(21))
                .andExpect(jsonPath("$.data[0].meetingId").value(meetingId.toString()))
                .andExpect(jsonPath("$.data[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.data[0].displayName").value("Alice"))
                .andExpect(jsonPath("$.data[0].role").value("HOST"))
                .andExpect(jsonPath("$.data[0].joinedAt").value("2026-04-01T10:00:00Z"))
                .andExpect(jsonPath("$.data[0].leftAt").isEmpty())
                .andExpect(jsonPath("$.data.nextPageToken").doesNotExist());

        verify(getParticipantsUseCase).execute(any());
    }

    @Test
    void getParticipants_paginationParamsAreIgnored() throws Exception {
        UUID meetingId = UUID.randomUUID();
        when(getParticipantsUseCase.execute(any())).thenReturn(Result.success(List.of()));

        mockMvc.perform(get("/api/v1/meetings/{id}/participants", meetingId)
                        .param("pageSize", "10")
                        .param("pageToken", "legacy-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getParticipants_meetingNotFound_returns404() throws Exception {
        UUID meetingId = UUID.randomUUID();
        when(getParticipantsUseCase.execute(any()))
                .thenReturn(Result.failure(new MeetingError.MeetingNotFound(meetingId)));

        mockMvc.perform(get("/api/v1/meetings/{id}/participants", meetingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("MEETING_NOT_FOUND"));
    }

    @Test
    void getParticipants_multipleParticipants_returnsAllInJsonArray() throws Exception {
        UUID meetingId = UUID.randomUUID();
        when(getParticipantsUseCase.execute(any()))
                .thenReturn(Result.success(List.of(
                        new ParticipantListItemResponse(
                                1L,
                                meetingId,
                                UUID.randomUUID(),
                                "Host",
                                ParticipantRole.HOST,
                                Instant.parse("2026-04-01T10:00:00Z"),
                                null),
                        new ParticipantListItemResponse(
                                2L,
                                meetingId,
                                null,
                                "Guest",
                                ParticipantRole.GUEST,
                                Instant.parse("2026-04-01T10:05:00Z"),
                                Instant.parse("2026-04-01T10:30:00Z")))));

        mockMvc.perform(get("/api/v1/meetings/{id}/participants", meetingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].displayName").value("Host"))
                .andExpect(jsonPath("$.data[1].displayName").value("Guest"))
                .andExpect(jsonPath("$.data[1].userId").isEmpty())
                .andExpect(jsonPath("$.data[1].leftAt").value("2026-04-01T10:30:00Z"));
    }

    @Test
    void getParticipants_invalidMeetingId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/meetings/{id}/participants", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getParticipantsUseCase);
    }
}
