package io.github.phunguy65.zms.data.mapper;

import io.github.phunguy65.zms.data.remote.dto.ChatManagementChatMessageResponse;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import java.time.OffsetDateTime;
import javax.inject.Inject;
import org.json.JSONObject;

/**
 * Maps chat message representations from REST API DTOs and LiveKit
 * JSON payloads to domain {@link ChatMessage} entities.
 */
public class ChatMessageMapper {

    private static final String TYPE_SYSTEM = "SYSTEM";

    @Inject
    public ChatMessageMapper() {}

    /**
     * Converts a REST API response DTO to a domain model.
     *
     * @param dto the API response DTO
     * @return the mapped domain model, or null if dto is null
     */
    public ChatMessage fromDto(ChatManagementChatMessageResponse dto) {
        if (dto == null) return null;

        return new ChatMessage(
                dto.getId(),
                dto.getSeqNum() != null ? dto.getSeqNum() : 0L,
                dto.getSenderId(),
                dto.getSenderName(),
                dto.getContent(),
                mapType(dto.getType()),
                dto.getCreatedAt());
    }

    /**
     * Parses a LiveKit reliable data-packet JSON payload into a domain model.
     *
     * @param json the parsed JSON object from the data packet
     * @return the mapped domain model, or null if required fields are missing
     */
    public ChatMessage fromLiveKitPayload(JSONObject json) {
        if (json == null) return null;

        String id = json.optString("id", null);
        long seqNum = json.optLong("seqNum", 0L);
        String senderId = json.optString("senderId", null);
        String senderName = json.optString("senderName", null);
        String content = json.optString("content", null);
        String type = json.optString("type", null);
        String createdAtStr = json.optString("createdAt", null);

        if (id == null || content == null) return null;

        OffsetDateTime createdAt = parseCreatedAt(createdAtStr);

        return new ChatMessage(id, seqNum, senderId, senderName, content, mapType(type), createdAt);
    }

    private ChatMessage.MessageType mapType(String type) {
        if (TYPE_SYSTEM.equalsIgnoreCase(type)) {
            return ChatMessage.MessageType.SYSTEM;
        }
        return ChatMessage.MessageType.TEXT;
    }

    private OffsetDateTime parseCreatedAt(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
