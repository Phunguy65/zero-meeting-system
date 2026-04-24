package io.github.phunguy65.zms.domain.model;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link ChatMessageMerger}.
 * Validates de-duplication, ordering, and merge behavior under mixed
 * history and real-time arrival scenarios.
 */
public class ChatMessageMergerTest {

    private static ChatMessage msg(String id, long seqNum) {
        return new ChatMessage(
                id,
                seqNum,
                "u1",
                "Alice",
                "content",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now());
    }

    @Test
    public void merge_bothNull_returnsEmptyList() {
        List<ChatMessage> result = ChatMessageMerger.merge(null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void merge_emptyExistingWithIncoming_returnsIncoming() {
        List<ChatMessage> incoming = Arrays.asList(msg("m1", 1), msg("m2", 2));

        List<ChatMessage> result = ChatMessageMerger.merge(new ArrayList<>(), incoming);

        assertEquals(2, result.size());
        assertEquals("m1", result.get(0).getId());
        assertEquals("m2", result.get(1).getId());
    }

    @Test
    public void merge_existingWithNullIncoming_returnsExisting() {
        List<ChatMessage> existing = Arrays.asList(msg("m1", 1));

        List<ChatMessage> result = ChatMessageMerger.merge(existing, null);

        assertEquals(1, result.size());
        assertEquals("m1", result.get(0).getId());
    }

    @Test
    public void merge_deduplicatesById() {
        List<ChatMessage> existing = Arrays.asList(msg("m1", 1), msg("m2", 2));
        List<ChatMessage> incoming = Arrays.asList(msg("m2", 2), msg("m3", 3));

        List<ChatMessage> result = ChatMessageMerger.merge(existing, incoming);

        assertEquals(3, result.size());
        assertEquals("m1", result.get(0).getId());
        assertEquals("m2", result.get(1).getId());
        assertEquals("m3", result.get(2).getId());
    }

    @Test
    public void merge_deduplicatesBySeqNumWhenIdsDiffer() {
        List<ChatMessage> existing = Arrays.asList(msg("history-1", 7));
        List<ChatMessage> incoming = Arrays.asList(new ChatMessage(
                "realtime-1",
                7,
                "u2",
                "Bob",
                "same seq",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now().plusSeconds(1)));

        List<ChatMessage> result = ChatMessageMerger.merge(existing, incoming);

        assertEquals(1, result.size());
        assertEquals("realtime-1", result.get(0).getId());
        assertEquals("same seq", result.get(0).getContent());
    }

    @Test
    public void merge_duplicateWithHigherSeqNum_usesHigher() {
        List<ChatMessage> existing = Arrays.asList(msg("m1", 1));
        List<ChatMessage> incoming = Arrays.asList(new ChatMessage(
                "m1",
                5,
                "u1",
                "Alice",
                "updated",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now()));

        List<ChatMessage> result = ChatMessageMerger.merge(existing, incoming);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getSeqNum());
        assertEquals("updated", result.get(0).getContent());
    }

    @Test
    public void merge_maintainsSeqNumOrder() {
        List<ChatMessage> existing = Arrays.asList(msg("m3", 3), msg("m1", 1));
        List<ChatMessage> incoming = Arrays.asList(msg("m2", 2));

        List<ChatMessage> result = ChatMessageMerger.merge(existing, incoming);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getSeqNum());
        assertEquals(2, result.get(1).getSeqNum());
        assertEquals(3, result.get(2).getSeqNum());
    }

    @Test
    public void mergeSingle_nullMessage_returnsExisting() {
        List<ChatMessage> existing = Arrays.asList(msg("m1", 1));

        List<ChatMessage> result = ChatMessageMerger.mergeSingle(existing, null);

        assertEquals(1, result.size());
    }

    @Test
    public void mergeSingle_newMessage_appendsAndSorts() {
        List<ChatMessage> existing = Arrays.asList(msg("m1", 1), msg("m3", 3));

        List<ChatMessage> result = ChatMessageMerger.mergeSingle(existing, msg("m2", 2));

        assertEquals(3, result.size());
        assertEquals(1, result.get(0).getSeqNum());
        assertEquals(2, result.get(1).getSeqNum());
        assertEquals(3, result.get(2).getSeqNum());
    }

    @Test
    public void mergeSingle_duplicateMessage_deduplicates() {
        List<ChatMessage> existing = Arrays.asList(msg("m1", 1), msg("m2", 2));

        List<ChatMessage> result = ChatMessageMerger.mergeSingle(existing, msg("m1", 1));

        assertEquals(2, result.size());
    }

    @Test
    public void merge_nullIdsIgnored() {
        ChatMessage noId = new ChatMessage(
                null,
                1,
                "u1",
                "Alice",
                "no id",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now());
        List<ChatMessage> result =
                ChatMessageMerger.merge(new ArrayList<>(), Arrays.asList(noId, msg("m1", 2)));

        assertEquals(1, result.size());
        assertEquals("m1", result.get(0).getId());
    }
}
