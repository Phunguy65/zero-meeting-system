package io.github.phunguy65.zms.chatmanagement.presentation.rest.response;

import java.time.Instant;

/** JSON response for a chat room. */
public record ChatRoomResponse(String roomId, String meetingId, String status, Instant createdAt) {}
