package io.github.phunguy65.zms.domain.usecase.chat;

import io.github.phunguy65.zms.domain.model.ChatMessage;
import io.github.phunguy65.zms.domain.repository.ChatRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for sending a chat message within an active meeting.
 *
 * <p>Delegates to the repository with the current meeting's roomId.
 * The caller is responsible for ensuring the meeting is active before invoking.
 */
public class SendMessageUseCase {

    private final ChatRepository chatRepository;

    @Inject
    public SendMessageUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    /**
     * Sends a text message to the specified room.
     *
     * @param roomId     the meeting/room identifier
     * @param senderName the display name of the sender
     * @param content    the message text
     * @return a future that resolves to the sent message
     */
    public CompletableFuture<ChatMessage> execute(
            String roomId, String senderName, String content) {
        return chatRepository.sendMessage(roomId, senderName, content);
    }
}
