package io.github.phunguy65.zms.meetingmanagement.application.response;

import java.time.Instant;

public record ParticipatedMeetingListItemResponse(Instant lastJoinedAt, MeetingResponse meeting) {}
