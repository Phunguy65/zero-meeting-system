package io.github.phunguy65.zms.presentation.meeting.chat;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.repository.ChatRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.chat.LoadChatHistoryUseCase;
import io.github.phunguy65.zms.domain.usecase.chat.SendMessageUseCase;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link MeetingChatViewModel}.
 * Tests loading, empty, content, error, send, and real-time receive behaviors.
 */
public class MeetingChatViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private LoadChatHistoryUseCase loadChatHistoryUseCase;

    @Mock
    private SendMessageUseCase sendMessageUseCase;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Captor
    private ArgumentCaptor<ChatRepository.IncomingMessageListener> listenerCaptor;

    private MeetingChatViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(sessionRepository.getSession())
                .thenReturn(new SessionInfo("user-1", "test@test.com", "Alice", "alice", null));

        SavedStateHandle savedStateHandle = new SavedStateHandle();
        viewModel = new MeetingChatViewModel(
                loadChatHistoryUseCase,
                sendMessageUseCase,
                chatRepository,
                sessionRepository,
                savedStateHandle);
    }

    @Test
    public void initialState_isLoading() {
        assertInstanceOf(
                MeetingChatViewModel.ChatUiState.Loading.class,
                viewModel.getUiState().getValue());
    }

    @Test
    public void initialize_withNullRoomId_emitsError() {
        viewModel.initialize(null);

        assertInstanceOf(
                MeetingChatViewModel.ChatUiState.Error.class,
                viewModel.getUiState().getValue());
    }

    @Test
    public void initialize_withEmptyRoomId_emitsError() {
        viewModel.initialize("");

        assertInstanceOf(
                MeetingChatViewModel.ChatUiState.Error.class,
                viewModel.getUiState().getValue());
    }

    @Test
    public void loadHistory_success_withMessages_emitsContent() {
        List<ChatMessage> messages = Arrays.asList(new ChatMessage(
                "m1",
                1,
                "u1",
                "Alice",
                "Hello",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now()));

        when(loadChatHistoryUseCase.execute("room-1"))
                .thenReturn(CompletableFuture.completedFuture(messages));

        viewModel.initialize("room-1");

        MeetingChatViewModel.ChatUiState state = viewModel.getUiState().getValue();
        assertInstanceOf(MeetingChatViewModel.ChatUiState.Content.class, state);
        assertEquals(
                1, ((MeetingChatViewModel.ChatUiState.Content) state).messages().size());
    }

    @Test
    public void loadHistory_success_empty_emitsEmpty() {
        when(loadChatHistoryUseCase.execute("room-1"))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        viewModel.initialize("room-1");

        assertInstanceOf(
                MeetingChatViewModel.ChatUiState.Empty.class,
                viewModel.getUiState().getValue());
    }

    @Test
    public void loadHistory_failure_emitsError() {
        CompletableFuture<List<ChatMessage>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Network error"));

        when(loadChatHistoryUseCase.execute("room-1")).thenReturn(failedFuture);

        viewModel.initialize("room-1");

        MeetingChatViewModel.ChatUiState state = viewModel.getUiState().getValue();
        assertInstanceOf(MeetingChatViewModel.ChatUiState.Error.class, state);
    }

    @Test
    public void sendMessage_success_updatesContent() {
        when(loadChatHistoryUseCase.execute("room-1"))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        viewModel.initialize("room-1");

        ChatMessage sent = new ChatMessage(
                "m1",
                1,
                "user-1",
                "Alice",
                "Hello",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now());
        when(sendMessageUseCase.execute(eq("room-1"), anyString(), eq("Hello")))
                .thenReturn(CompletableFuture.completedFuture(sent));

        viewModel.sendMessage("Hello");

        assertFalse(viewModel.isSending().getValue());
        MeetingChatViewModel.ChatUiState state = viewModel.getUiState().getValue();
        assertInstanceOf(MeetingChatViewModel.ChatUiState.Content.class, state);
    }

    @Test
    public void sendMessage_failure_emitsSendError() {
        when(loadChatHistoryUseCase.execute("room-1"))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        viewModel.initialize("room-1");

        CompletableFuture<ChatMessage> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Send failed"));
        when(sendMessageUseCase.execute(eq("room-1"), anyString(), eq("test")))
                .thenReturn(failedFuture);

        viewModel.sendMessage("test");

        assertFalse(viewModel.isSending().getValue());
        assertNotNull(viewModel.getSendError().getValue());
    }

    @Test
    public void sendMessage_emptyContent_ignored() {
        viewModel.sendMessage("");
        viewModel.sendMessage("   ");

        verifyNoInteractions(sendMessageUseCase);
    }

    @Test
    public void incomingMessage_mergesIntoTimeline() {
        when(loadChatHistoryUseCase.execute("room-1"))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(new ChatMessage(
                        "m1",
                        1,
                        "u1",
                        "Alice",
                        "Hi",
                        ChatMessage.MessageType.TEXT,
                        OffsetDateTime.now()))));

        viewModel.initialize("room-1");

        verify(chatRepository).setIncomingMessageListener(listenerCaptor.capture());

        ChatRepository.IncomingMessageListener listener = listenerCaptor.getValue();
        assertNotNull(listener);

        ChatMessage incoming = new ChatMessage(
                "m2", 2, "u2", "Bob", "Hello", ChatMessage.MessageType.TEXT, OffsetDateTime.now());
        listener.onMessageReceived(incoming);

        MeetingChatViewModel.ChatUiState state = viewModel.getUiState().getValue();
        assertInstanceOf(MeetingChatViewModel.ChatUiState.Content.class, state);
        assertEquals(
                2, ((MeetingChatViewModel.ChatUiState.Content) state).messages().size());
    }

    @Test
    public void getCurrentUserId_returnsSessionUserId() {
        assertEquals("user-1", viewModel.getCurrentUserId());
    }

    @Test
    public void clearSendError_setsNull() {
        viewModel.clearSendError();
        assertNull(viewModel.getSendError().getValue());
    }

    private static <T> void assertInstanceOf(Class<T> expected, Object actual) {
        assertNotNull("Expected non-null value of type " + expected.getSimpleName(), actual);
        assertTrue(
                "Expected " + expected.getSimpleName() + " but got "
                        + actual.getClass().getSimpleName(),
                expected.isInstance(actual));
    }
}
