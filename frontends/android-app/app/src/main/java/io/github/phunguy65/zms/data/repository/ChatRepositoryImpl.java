package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.mapper.ChatMessageMapper;
import io.github.phunguy65.zms.data.remote.api.ChatApi;
import io.github.phunguy65.zms.data.remote.dto.ChatManagementChatMessageResponse;
import io.github.phunguy65.zms.data.remote.dto.ChatManagementChatRoomResponse;
import io.github.phunguy65.zms.data.remote.dto.ChatManagementCursorScrollResponseChatMessageResponse;
import io.github.phunguy65.zms.data.remote.dto.ChatManagementSendMessageRequest;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import io.github.phunguy65.zms.domain.repository.ChatRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementation of {@link ChatRepository} backed by the chat-management REST API
 * for history retrieval and message sending, with LiveKit data-channel message
 * reception delegated through a registered listener.
 */
@Singleton
public class ChatRepositoryImpl implements ChatRepository {

    private static final String ROOM_STATUS_ACTIVE = "ACTIVE";

    private final ChatApi chatApi;
    private final ChatMessageMapper mapper;

    private volatile IncomingMessageListener incomingMessageListener;

    @Inject
    public ChatRepositoryImpl(ChatApi chatApi, ChatMessageMapper mapper) {
        this.chatApi = chatApi;
        this.mapper = mapper;
    }

    @Override
    public CompletableFuture<List<ChatMessage>> loadHistory(
            String roomId, int size, Long beforeSeqNum) {
        CompletableFuture<List<ChatMessage>> future = new CompletableFuture<>();

        validateActiveRoom(roomId).whenComplete((roomResponse, roomError) -> {
            if (roomError != null) {
                future.completeExceptionally(unwrap(roomError));
                return;
            }

            if (!isRoomActive(roomResponse)) {
                future.complete(Collections.emptyList());
                return;
            }

            chatApi.getMessages(roomId, size, beforeSeqNum)
                    .enqueue(new Callback<ChatManagementCursorScrollResponseChatMessageResponse>() {
                        @Override
                        public void onResponse(
                                Call<ChatManagementCursorScrollResponseChatMessageResponse> call,
                                Response<ChatManagementCursorScrollResponseChatMessageResponse>
                                        response) {
                            if (!response.isSuccessful() || response.body() == null) {
                                future.completeExceptionally(new RuntimeException(
                                        "Failed to load chat history: HTTP " + response.code()));
                                return;
                            }

                            List<ChatManagementChatMessageResponse> dtos =
                                    response.body().getContent();
                            if (dtos == null || dtos.isEmpty()) {
                                future.complete(Collections.emptyList());
                                return;
                            }

                            List<ChatMessage> messages = new ArrayList<>(dtos.size());
                            for (ChatManagementChatMessageResponse dto : dtos) {
                                ChatMessage mapped = mapper.fromDto(dto);
                                if (mapped != null) {
                                    messages.add(mapped);
                                }
                            }

                            messages.sort((a, b) -> Long.compare(a.getSeqNum(), b.getSeqNum()));
                            future.complete(messages);
                        }

                        @Override
                        public void onFailure(
                                Call<ChatManagementCursorScrollResponseChatMessageResponse> call,
                                Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
        });

        return future;
    }

    @Override
    public CompletableFuture<ChatMessage> sendMessage(
            String roomId, String senderName, String content) {
        CompletableFuture<ChatMessage> future = new CompletableFuture<>();

        ChatManagementSendMessageRequest request = new ChatManagementSendMessageRequest();
        request.setSenderName(senderName);
        request.setContent(content);
        request.setReplyToSeqNum(null);

        validateActiveRoom(roomId).whenComplete((roomResponse, roomError) -> {
            if (roomError != null) {
                future.completeExceptionally(unwrap(roomError));
                return;
            }

            if (!isRoomActive(roomResponse)) {
                future.completeExceptionally(new IllegalStateException("Meeting is not active"));
                return;
            }

            chatApi.sendMessage(roomId, request)
                    .enqueue(new Callback<ChatManagementChatMessageResponse>() {
                        @Override
                        public void onResponse(
                                Call<ChatManagementChatMessageResponse> call,
                                Response<ChatManagementChatMessageResponse> response) {
                            if (!response.isSuccessful() || response.body() == null) {
                                future.completeExceptionally(new RuntimeException(
                                        "Failed to send message: HTTP " + response.code()));
                                return;
                            }

                            ChatMessage mapped = mapper.fromDto(response.body());
                            if (mapped != null) {
                                future.complete(mapped);
                            } else {
                                future.completeExceptionally(new RuntimeException(
                                        "Failed to map sent message response"));
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<ChatManagementChatMessageResponse> call, Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
        });

        return future;
    }

    @Override
    public void setIncomingMessageListener(IncomingMessageListener listener) {
        this.incomingMessageListener = listener;
    }

    /**
     * Dispatches a parsed incoming chat message to the registered listener.
     * Called by the LiveKit data-message handler after successful payload parsing.
     *
     * @param message the parsed domain message
     */
    public void dispatchIncomingMessage(ChatMessage message) {
        IncomingMessageListener listener = this.incomingMessageListener;
        if (listener != null && message != null) {
            listener.onMessageReceived(message);
        }
    }

    private CompletableFuture<ChatManagementChatRoomResponse> validateActiveRoom(String roomId) {
        CompletableFuture<ChatManagementChatRoomResponse> future = new CompletableFuture<>();

        if (roomId == null || roomId.isBlank()) {
            future.completeExceptionally(new IllegalArgumentException("Meeting is not active"));
            return future;
        }

        chatApi.getRoom(roomId).enqueue(new Callback<ChatManagementChatRoomResponse>() {
            @Override
            public void onResponse(
                    Call<ChatManagementChatRoomResponse> call,
                    Response<ChatManagementChatRoomResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    future.completeExceptionally(new RuntimeException(
                            "Failed to validate chat room: HTTP " + response.code()));
                    return;
                }

                future.complete(response.body());
            }

            @Override
            public void onFailure(Call<ChatManagementChatRoomResponse> call, Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private boolean isRoomActive(ChatManagementChatRoomResponse roomResponse) {
        return roomResponse != null
                && roomResponse.getStatus() != null
                && ROOM_STATUS_ACTIVE.equalsIgnoreCase(roomResponse.getStatus());
    }

    private Throwable unwrap(Throwable throwable) {
        return throwable.getCause() != null ? throwable.getCause() : throwable;
    }
}
