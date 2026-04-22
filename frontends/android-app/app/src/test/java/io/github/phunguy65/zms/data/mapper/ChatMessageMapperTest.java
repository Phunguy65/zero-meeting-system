package io.github.phunguy65.zms.data.mapper;

import static org.junit.Assert.*;

import io.github.phunguy65.zms.data.remote.dto.ChatManagementChatMessageResponse;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import java.time.OffsetDateTime;
import org.junit.Before;
import org.junit.Test;
import org.json.JSONObject;

/**
 * Unit tests for {@link ChatMessageMapper} REST DTO conversion.
 *
 * <p>LiveKit payload tests (which depend on {@code org.json.JSONObject})
 * require the Android runtime and are covered by the
 * {@code ChatDataMessageHandler} integration or instrumented tests.
 */
public class ChatMessageMapperTest {

    private ChatMessageMapper mapper;

    @Before
    public void setup() {
        mapper = new ChatMessageMapper();
    }

    @Test
    public void fromDto_nullInput_returnsNull() {
        assertNull(mapper.fromDto(null));
    }

    @Test
    public void fromDto_validTextMessage_mapsAllFields() {
        OffsetDateTime now = OffsetDateTime.now();
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("msg-1");
        dto.setSeqNum(42L);
        dto.setSenderId("user-1");
        dto.setSenderName("Alice");
        dto.setContent("Hello world");
        dto.setType("TEXT");
        dto.setCreatedAt(now);

        ChatMessage result = mapper.fromDto(dto);

        assertNotNull(result);
        assertEquals("msg-1", result.getId());
        assertEquals(42L, result.getSeqNum());
        assertEquals("user-1", result.getSenderId());
        assertEquals("Alice", result.getSenderName());
        assertEquals("Hello world", result.getContent());
        assertEquals(ChatMessage.MessageType.TEXT, result.getType());
        assertEquals(now, result.getCreatedAt());
    }

    @Test
    public void fromDto_systemType_mapsToSystemEnum() {
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("sys-1");
        dto.setSeqNum(1L);
        dto.setContent("User joined");
        dto.setType("SYSTEM");

        ChatMessage result = mapper.fromDto(dto);

        assertNotNull(result);
        assertEquals(ChatMessage.MessageType.SYSTEM, result.getType());
        assertTrue(result.isSystem());
    }

    @Test
    public void fromDto_nullSeqNum_defaultsToZero() {
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("msg-2");
        dto.setSeqNum(null);
        dto.setContent("test");
        dto.setType("TEXT");

        ChatMessage result = mapper.fromDto(dto);

        assertNotNull(result);
        assertEquals(0L, result.getSeqNum());
    }

    @Test
    public void fromDto_unknownType_defaultsToText() {
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("msg-3");
        dto.setSeqNum(1L);
        dto.setContent("test");
        dto.setType("UNKNOWN_TYPE");

        ChatMessage result = mapper.fromDto(dto);

        assertNotNull(result);
        assertEquals(ChatMessage.MessageType.TEXT, result.getType());
    }

    @Test
    public void fromDto_nullType_defaultsToText() {
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("msg-4");
        dto.setSeqNum(1L);
        dto.setContent("test");
        dto.setType(null);

        ChatMessage result = mapper.fromDto(dto);

        assertNotNull(result);
        assertEquals(ChatMessage.MessageType.TEXT, result.getType());
    }

    @Test
    public void fromDto_nullCreatedAt_setsNull() {
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("msg-5");
        dto.setSeqNum(1L);
        dto.setContent("test");
        dto.setCreatedAt(null);

        ChatMessage result = mapper.fromDto(dto);

        assertNotNull(result);
        assertNull(result.getCreatedAt());
    }

    @Test
    public void fromDto_systemMessage_isSystemReturnsTrue() {
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("sys-2");
        dto.setSeqNum(10L);
        dto.setContent("Alice joined the meeting");
        dto.setType("SYSTEM");

        ChatMessage result = mapper.fromDto(dto);

        assertTrue(result.isSystem());
        assertFalse(result.isMine("user-1"));
    }

    @Test
    public void fromDto_textMessage_isMineWithMatchingUser() {
        ChatManagementChatMessageResponse dto = new ChatManagementChatMessageResponse();
        dto.setId("msg-6");
        dto.setSeqNum(1L);
        dto.setSenderId("user-1");
        dto.setContent("test");
        dto.setType("TEXT");

        ChatMessage result = mapper.fromDto(dto);

        assertTrue(result.isMine("user-1"));
        assertFalse(result.isMine("user-2"));
        assertFalse(result.isMine(null));
    }

    @Test
    public void fromLiveKitPayload_nullInput_returnsNull() {
        assertNull(mapper.fromLiveKitPayload(null));
    }

    @Test
    public void fromLiveKitPayload_validSystemPayload_mapsFields() throws Exception {
        JSONObject payload = new JSONObject()
                .put("id", "sys-3")
                .put("seqNum", 12)
                .put("senderId", "system")
                .put("senderName", "System")
                .put("content", "Alice joined")
                .put("type", "SYSTEM")
                .put("createdAt", "2026-04-22T10:15:30+07:00");

        ChatMessage result = mapper.fromLiveKitPayload(payload);

        assertNotNull(result);
        assertEquals("sys-3", result.getId());
        assertEquals(12L, result.getSeqNum());
        assertEquals(ChatMessage.MessageType.SYSTEM, result.getType());
        assertEquals("Alice joined", result.getContent());
    }

    @Test
    public void fromLiveKitPayload_missingRequiredFields_returnsNull() throws Exception {
        JSONObject payload = new JSONObject().put("id", "msg-1");

        assertNull(mapper.fromLiveKitPayload(payload));
    }
}
