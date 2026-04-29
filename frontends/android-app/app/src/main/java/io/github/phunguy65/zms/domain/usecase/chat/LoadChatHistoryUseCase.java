package io.github.phunguy65.zms.domain.usecase.chat;

import io.github.phunguy65.zms.domain.model.ChatMessage;
import io.github.phunguy65.zms.domain.repository.ChatRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for loading chat message history for an active meeting.
 *
 * <p>Fetches the most recent messages from the chat backend using cursor
 * pagination keyed on sequence number. The caller is responsible for
 * ensuring the meeting is active before invoking.
 */
public class LoadChatHistoryUseCase {

    private static final int DEFAULT_PAGE_SIZE = 30;

    private final ChatRepository chatRepository;

    @Inject
    public LoadChatHistoryUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    /**
     * Loads the initial page of chat history for the given room.
     *
     * @param roomId the meeting/room identifier
     * @return a future that resolves to a list of messages in ascending seqNum order
     */
    public CompletableFuture<List<ChatMessage>> execute(String roomId) {
        return chatRepository.loadHistory(roomId, DEFAULT_PAGE_SIZE, null);
    }

    /**
     * Loads an older page of chat history before the given cursor.
     *
     * @param roomId       the meeting/room identifier
     * @param beforeSeqNum the exclusive upper bound for pagination
     * @return a future that resolves to a list of messages in ascending seqNum order
     */
    public CompletableFuture<List<ChatMessage>> execute(String roomId, long beforeSeqNum) {
        return chatRepository.loadHistory(roomId, DEFAULT_PAGE_SIZE, beforeSeqNum);
    }
}
