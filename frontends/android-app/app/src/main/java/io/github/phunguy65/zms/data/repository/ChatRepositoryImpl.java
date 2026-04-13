package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.domain.repository.ChatRepository;
import javax.inject.Inject;

/** Implementation of {@link ChatRepository} backed by remote API / WebSocket. */
public class ChatRepositoryImpl implements ChatRepository {

    @Inject
    public ChatRepositoryImpl() {}
}
