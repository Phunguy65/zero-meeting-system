package io.github.phunguy65.zms.data.repository;

import android.util.Log;
import io.github.phunguy65.zms.data.mapper.ChatMessageMapper;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.json.JSONObject;

/**
 * Processes raw LiveKit data-message payloads, parses them as UTF-8 JSON
 * chat messages, and dispatches valid results through {@link ChatRepositoryImpl}.
 *
 * <p>Malformed or unparseable payloads are silently ignored to avoid
 * disrupting call or chat state.
 */
@Singleton
public class ChatDataMessageHandler {

    private static final String TAG = "ChatDataMessageHandler";

    private final ChatMessageMapper mapper;
    private final ChatRepositoryImpl chatRepository;

    @Inject
    public ChatDataMessageHandler(ChatMessageMapper mapper, ChatRepositoryImpl chatRepository) {
        this.mapper = mapper;
        this.chatRepository = chatRepository;
    }

    /**
     * Attempts to parse raw bytes as a UTF-8 JSON chat payload and
     * dispatches the resulting domain message to the chat repository.
     *
     * @param data raw bytes from the LiveKit data channel
     */
    public void handleDataReceived(byte[] data) {
        if (data == null || data.length == 0) return;

        try {
            String jsonString = new String(data, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(jsonString);

            if (!json.has("id") || !json.has("content")) {
                return;
            }

            ChatMessage message = mapper.fromLiveKitPayload(json);
            if (message != null) {
                chatRepository.dispatchIncomingMessage(message);
            }
        } catch (Exception e) {
            Log.d(TAG, "Ignored non-chat or malformed data payload", e);
        }
    }
}
