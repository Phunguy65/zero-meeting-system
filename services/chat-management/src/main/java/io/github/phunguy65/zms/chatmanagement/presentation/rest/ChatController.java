package io.github.phunguy65.zms.chatmanagement.presentation.rest;

import io.github.phunguy65.zms.chatmanagement.application.usecase.GetMessagesUseCase;
import io.github.phunguy65.zms.chatmanagement.application.usecase.GetRoomUseCase;
import io.github.phunguy65.zms.chatmanagement.application.usecase.SendMessageUseCase;
import io.github.phunguy65.zms.chatmanagement.domain.model.ChatError;
import io.github.phunguy65.zms.chatmanagement.domain.model.ChatMessage;
import io.github.phunguy65.zms.chatmanagement.domain.model.ChatRoom;
import io.github.phunguy65.zms.chatmanagement.presentation.rest.request.GetMessagesRequest;
import io.github.phunguy65.zms.chatmanagement.presentation.rest.request.SendMessageRequest;
import io.github.phunguy65.zms.chatmanagement.presentation.rest.response.ChatMessageResponse;
import io.github.phunguy65.zms.chatmanagement.presentation.rest.response.ChatRoomResponse;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.CursorScrollResponse;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/rooms")
public class ChatController extends BaseController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessagesUseCase getMessagesUseCase;
    private final GetRoomUseCase getRoomUseCase;

    public ChatController(
            SendMessageUseCase sendMessageUseCase,
            GetMessagesUseCase getMessagesUseCase,
            GetRoomUseCase getRoomUseCase) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.getMessagesUseCase = getMessagesUseCase;
        this.getRoomUseCase = getRoomUseCase;
    }

    /**
     * Sends a message to a chat room.
     *
     * <p>The senderId is extracted from the JWT in the Authorization header.
     * Errors are mapped to HTTP responses by the controller base class.
     */
    @PostMapping("/{roomId}/messages")
    public ResponseEntity<JsendResponse<?>> sendMessage(
            @PathVariable String roomId, @RequestBody SendMessageRequest request) {

        String senderId = extractSenderId();
        Result<ChatMessage, ChatError> result = sendMessageUseCase.execute(
                roomId, senderId, request.senderName(), request.content(), request.replyToSeqNum());

        return switch (result) {
            case Result.Success<ChatMessage, ChatError> s -> {
                ChatMessageResponse response = toMessageResponse(s.value());
                yield ResponseEntity.ok(JsendResponse.success(response));
            }
            case Result.Failure<ChatMessage, ChatError> f -> errorResponse(f.error());
        };
    }

    /**
     * Retrieves a page of messages from a chat room with cursor pagination.
     *
     * <p>Pass the {@code beforeSeqNum} from the previous response's last item to get the next page.
     * This endpoint does not surface domain errors; when the room is missing, the use case returns
     * an empty page.
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<JsendResponse<?>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long beforeSeqNum) {

        int clampedSize = Math.clamp(size, 1, GetMessagesRequest.MAX_SIZE);
        CursorPageResponse<ChatMessage> page =
                getMessagesUseCase.execute(roomId, clampedSize, Optional.ofNullable(beforeSeqNum));

        List<ChatMessageResponse> items =
                page.items().stream().map(ChatController::toMessageResponse).toList();

        String nextPageToken = null;
        if (page.hasNext() && !items.isEmpty()) {
            nextPageToken = String.valueOf(items.getLast().seqNum());
        }

        var response = new CursorScrollResponse<>(items, page.pageSize(), nextPageToken);
        return ResponseEntity.ok(JsendResponse.success(response));
    }

    /**
     * Returns the chat room info for the given room ID.
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<JsendResponse<?>> getRoom(@PathVariable String roomId) {
        Result<ChatRoom, ChatError> result = getRoomUseCase.execute(roomId);

        return switch (result) {
            case Result.Success<ChatRoom, ChatError> s -> {
                ChatRoomResponse response = toRoomResponse(s.value());
                yield ResponseEntity.ok(JsendResponse.success(response));
            }
            case Result.Failure<ChatRoom, ChatError> f -> errorResponse(f.error());
        };
    }

    private String extractSenderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return "anonymous";
        }
        return auth.getPrincipal().toString();
    }

    private static ChatMessageResponse toMessageResponse(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getSeqNum(),
                m.getRoomId(),
                m.getSenderId(),
                m.getSenderName(),
                m.getContent(),
                m.getType(),
                m.getCreatedAt());
    }

    private static ChatRoomResponse toRoomResponse(ChatRoom r) {
        return new ChatRoomResponse(
                r.getRoomId(), r.getMeetingId(), r.getStatus(), r.getCreatedAt());
    }
}
