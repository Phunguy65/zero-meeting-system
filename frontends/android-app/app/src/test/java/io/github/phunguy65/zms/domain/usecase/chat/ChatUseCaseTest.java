package io.github.phunguy65.zms.domain.usecase.chat;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.domain.model.ChatMessage;
import io.github.phunguy65.zms.domain.repository.ChatRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link LoadChatHistoryUseCase} and {@link SendMessageUseCase}.
 * Validates success/failure mapping from repository to use case callers.
 */
public class ChatUseCaseTest {

    @Mock
    private ChatRepository chatRepository;

    private LoadChatHistoryUseCase loadChatHistoryUseCase;
    private SendMessageUseCase sendMessageUseCase;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        loadChatHistoryUseCase = new LoadChatHistoryUseCase(chatRepository);
        sendMessageUseCase = new SendMessageUseCase(chatRepository);
    }

    @Test
    public void loadHistory_success_returnsMessages()
            throws ExecutionException, InterruptedException {
        List<ChatMessage> expected = Arrays.asList(
                new ChatMessage(
                        "m1",
                        1,
                        "u1",
                        "Alice",
                        "Hello",
                        ChatMessage.MessageType.TEXT,
                        OffsetDateTime.now()),
                new ChatMessage(
                        "m2",
                        2,
                        "u2",
                        "Bob",
                        "Hi",
                        ChatMessage.MessageType.TEXT,
                        OffsetDateTime.now()));

        when(chatRepository.loadHistory("room-1", 30, null))
                .thenReturn(CompletableFuture.completedFuture(expected));

        List<ChatMessage> result = loadChatHistoryUseCase.execute("room-1").get();

        assertEquals(2, result.size());
        assertEquals("m1", result.get(0).getId());
        assertEquals("m2", result.get(1).getId());
        verify(chatRepository).loadHistory("room-1", 30, null);
    }

    @Test
    public void loadHistory_withCursor_passesBeforeSeqNum()
            throws ExecutionException, InterruptedException {
        when(chatRepository.loadHistory("room-1", 30, 50L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        List<ChatMessage> result = loadChatHistoryUseCase.execute("room-1", 50L).get();

        assertTrue(result.isEmpty());
        verify(chatRepository).loadHistory("room-1", 30, 50L);
    }

    @Test
    public void loadHistory_failure_propagatesException() {
        CompletableFuture<List<ChatMessage>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Network error"));

        when(chatRepository.loadHistory("room-1", 30, null)).thenReturn(failedFuture);

        try {
            loadChatHistoryUseCase.execute("room-1").get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertEquals("Network error", e.getCause().getMessage());
        } catch (InterruptedException e) {
            fail("Unexpected interruption");
        }
    }

    @Test
    public void sendMessage_success_returnsSentMessage()
            throws ExecutionException, InterruptedException {
        ChatMessage expected = new ChatMessage(
                "m3",
                3,
                "u1",
                "Alice",
                "My message",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now());

        when(chatRepository.sendMessage("room-1", "Alice", "My message"))
                .thenReturn(CompletableFuture.completedFuture(expected));

        ChatMessage result =
                sendMessageUseCase.execute("room-1", "Alice", "My message").get();

        assertNotNull(result);
        assertEquals("m3", result.getId());
        assertEquals("My message", result.getContent());
    }

    @Test
    public void sendMessage_failure_propagatesException() {
        CompletableFuture<ChatMessage> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Failed to send"));

        when(chatRepository.sendMessage("room-1", "Alice", "test")).thenReturn(failedFuture);

        try {
            sendMessageUseCase.execute("room-1", "Alice", "test").get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertEquals("Failed to send", e.getCause().getMessage());
        } catch (InterruptedException e) {
            fail("Unexpected interruption");
        }
    }

    @Test
    public void loadHistory_emptyRoomId_propagatesValidationError() {
        CompletableFuture<List<ChatMessage>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalArgumentException("Meeting is not active"));
        when(chatRepository.loadHistory("", 30, null)).thenReturn(failedFuture);

        try {
            loadChatHistoryUseCase.execute("").get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertEquals("Meeting is not active", e.getCause().getMessage());
        } catch (InterruptedException e) {
            fail("Unexpected interruption");
        }
    }

    @Test
    public void sendMessage_inactiveMeeting_propagatesValidationError() {
        CompletableFuture<ChatMessage> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Meeting is not active"));
        when(chatRepository.sendMessage("room-1", "Alice", "test")).thenReturn(failedFuture);

        try {
            sendMessageUseCase.execute("room-1", "Alice", "test").get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertEquals("Meeting is not active", e.getCause().getMessage());
        } catch (InterruptedException e) {
            fail("Unexpected interruption");
        }
    }
}
