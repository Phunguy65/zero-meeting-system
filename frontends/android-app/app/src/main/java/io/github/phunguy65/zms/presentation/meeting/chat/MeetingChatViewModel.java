package io.github.phunguy65.zms.presentation.meeting.chat;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import io.github.phunguy65.zms.domain.model.ChatMessageMerger;
import io.github.phunguy65.zms.domain.repository.ChatRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.chat.LoadChatHistoryUseCase;
import io.github.phunguy65.zms.domain.usecase.chat.SendMessageUseCase;
import io.github.phunguy65.zms.frontends.R;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

/**
 * ViewModel for in-meeting chat. Manages loading, empty, content, error, and
 * send-in-progress/failure states. Observes real-time incoming messages and
 * merges them into the timeline deterministically.
 */
@HiltViewModel
public class MeetingChatViewModel extends ViewModel {

    /**
     * Discriminated chat UI state.
     */
    public sealed interface ChatUiState {
        record Loading() implements ChatUiState {}

        record Empty() implements ChatUiState {}

        record Content(List<ChatMessage> messages) implements ChatUiState {}

        record Error(@StringRes int messageResId, String message) implements ChatUiState {
            public Error(String message) {
                this(0, message);
            }

            public Error(@StringRes int messageResId) {
                this(messageResId, null);
            }
        }
    }

    private final LoadChatHistoryUseCase loadChatHistoryUseCase;
    private final SendMessageUseCase sendMessageUseCase;
    private final ChatRepository chatRepository;
    private final SessionRepository sessionRepository;

    private final MutableLiveData<ChatUiState> _uiState =
            new MutableLiveData<>(new ChatUiState.Loading());
    private final MutableLiveData<Boolean> _isSending = new MutableLiveData<>(false);
    private final MutableLiveData<String> _sendError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _sendSuccess = new MutableLiveData<>(null);

    private String roomId;
    private List<ChatMessage> currentMessages = Collections.emptyList();

    @Inject
    public MeetingChatViewModel(
            LoadChatHistoryUseCase loadChatHistoryUseCase,
            SendMessageUseCase sendMessageUseCase,
            ChatRepository chatRepository,
            SessionRepository sessionRepository,
            SavedStateHandle savedStateHandle) {
        this.loadChatHistoryUseCase = loadChatHistoryUseCase;
        this.sendMessageUseCase = sendMessageUseCase;
        this.chatRepository = chatRepository;
        this.sessionRepository = sessionRepository;

        this.roomId = savedStateHandle.get("roomId");

        chatRepository.setIncomingMessageListener(this::onIncomingMessage);
    }

    public LiveData<ChatUiState> getUiState() {
        return _uiState;
    }

    public LiveData<Boolean> isSending() {
        return _isSending;
    }

    public LiveData<String> getSendError() {
        return _sendError;
    }

    /**
     * One-shot signal indicating the last send succeeded. Observe to clear input.
     */
    public LiveData<Boolean> getSendSuccess() {
        return _sendSuccess;
    }

    /**
     * Returns the current user's ID from the session, or null for guests.
     */
    public String getCurrentUserId() {
        var session = sessionRepository.getSession();
        return session != null ? session.userId() : null;
    }

    /**
     * Sets the room identifier and triggers initial history load.
     * Should be called once when the chat sheet opens.
     */
    public void initialize(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            _uiState.setValue(new ChatUiState.Error(R.string.chat_meeting_not_active));
            return;
        }
        this.roomId = roomId;
        loadHistory();
    }

    /**
     * Loads chat history from the backend and transitions UI state accordingly.
     */
    public void loadHistory() {
        if (roomId == null || roomId.isEmpty()) {
            _uiState.setValue(new ChatUiState.Error(R.string.chat_meeting_not_active));
            return;
        }

        _uiState.setValue(new ChatUiState.Loading());

        loadChatHistoryUseCase.execute(roomId).whenComplete((messages, error) -> {
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                _uiState.postValue(
                        new ChatUiState.Error(R.string.chat_failed_to_load, cause.getMessage()));
                return;
            }

            currentMessages = ChatMessageMerger.merge(Collections.emptyList(), messages);

            if (currentMessages.isEmpty()) {
                _uiState.postValue(new ChatUiState.Empty());
            } else {
                _uiState.postValue(new ChatUiState.Content(List.copyOf(currentMessages)));
            }
        });
    }

    /**
     * Sends a text message in the current room.
     */
    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty()) return;
        if (roomId == null || roomId.isEmpty()) return;

        _isSending.setValue(true);
        _sendError.setValue(null);
        _sendSuccess.setValue(null);

        String senderName = getSenderName();

        sendMessageUseCase
                .execute(roomId, senderName, content.trim())
                .whenComplete((sentMessage, error) -> {
                    _isSending.postValue(false);

                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        String errorMsg = cause.getMessage();
                        _sendError.postValue(errorMsg != null ? errorMsg : "send_failed");
                        return;
                    }

                    currentMessages = ChatMessageMerger.mergeSingle(currentMessages, sentMessage);
                    _uiState.postValue(new ChatUiState.Content(List.copyOf(currentMessages)));
                    _sendSuccess.postValue(true);
                });
    }

    /**
     * Clears the one-shot send error after the UI has consumed it.
     */
    public void clearSendError() {
        _sendError.setValue(null);
    }

    /**
     * Clears the one-shot send success signal after the UI has consumed it.
     */
    public void clearSendSuccess() {
        _sendSuccess.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatRepository.setIncomingMessageListener(null);
    }

    private void onIncomingMessage(ChatMessage message) {
        if (message == null) return;

        currentMessages = ChatMessageMerger.mergeSingle(currentMessages, message);
        _uiState.postValue(new ChatUiState.Content(List.copyOf(currentMessages)));
    }

    private String getSenderName() {
        var session = sessionRepository.getSession();
        if (session != null && session.fullName() != null && !session.fullName().isEmpty()) {
            return session.fullName();
        }
        return "Me";
    }
}
