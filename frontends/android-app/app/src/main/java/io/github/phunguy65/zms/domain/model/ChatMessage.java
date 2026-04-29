package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Domain entity representing a single chat message within a meeting.
 *
 * <p>Messages are ordered by {@code seqNum} (ascending) for deterministic
 * timeline rendering. The {@code type} field distinguishes user-authored
 * messages from system-generated events.
 */
public final class ChatMessage {

    private final String id;
    private final long seqNum;
    private final String senderId;
    private final String senderName;
    private final String content;
    private final MessageType type;
    private final OffsetDateTime createdAt;

    public ChatMessage(
            String id,
            long seqNum,
            String senderId,
            String senderName,
            String content,
            MessageType type,
            OffsetDateTime createdAt) {
        this.id = id;
        this.seqNum = seqNum;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.type = type;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public MessageType getType() {
        return type;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns whether this message is a system-generated event.
     */
    public boolean isSystem() {
        return type == MessageType.SYSTEM;
    }

    /**
     * Returns whether this message was sent by the given user.
     */
    public boolean isMine(String currentUserId) {
        return currentUserId != null && currentUserId.equals(senderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Chat message type discriminator.
     */
    public enum MessageType {
        TEXT,
        SYSTEM
    }
}
