package io.github.phunguy65.zms.domain.usecase.chat;

import io.github.phunguy65.zms.domain.repository.ChatRepository;
import javax.inject.Inject;

/** Use case for sending a chat message within a meeting. */
public class SendMessageUseCase {

    private final ChatRepository chatRepository;

    @Inject
    public SendMessageUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }
}
