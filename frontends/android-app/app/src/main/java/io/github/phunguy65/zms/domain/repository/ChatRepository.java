package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.ChatMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for in-meeting chat operations.
 *
 * <p>Handles message history retrieval via REST, message sending,
 * and real-time message reception through LiveKit data channel.
 */
public interface ChatRepository {

    /**
     * Loads message history for the given room.
     *
     * @param roomId       the chat room identifier (same as meetingId)
     * @param size         maximum number of messages to retrieve
     * @param beforeSeqNum cursor for pagination; null for first page
     * @return a future containing the list of messages in ascending seqNum order
     */
    CompletableFuture<List<ChatMessage>> loadHistory(String roomId, int size, Long beforeSeqNum);

    /**
     * Sends a text message in the given room.
     *
     * @param roomId     the chat room identifier (same as meetingId)
     * @param senderName the display name of the sender
     * @param content    the message text
     * @return a future containing the sent message or failing on error
     */
    CompletableFuture<ChatMessage> sendMessage(String roomId, String senderName, String content);

    /**
     * Registers a listener for real-time incoming chat messages from LiveKit.
     *
     * @param listener the callback, or null to unregister
     */
    void setIncomingMessageListener(IncomingMessageListener listener);

    /**
     * Callback for real-time incoming chat messages.
     */
    interface IncomingMessageListener {
        void onMessageReceived(ChatMessage message);
    }
}
