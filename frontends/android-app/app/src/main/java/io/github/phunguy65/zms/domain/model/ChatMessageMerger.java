package io.github.phunguy65.zms.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges and de-duplicates chat messages from history and real-time sources
 * into a deterministic timeline ordered by sequence number ascending.
 *
 * <p>De-duplication is keyed by message {@code id} or {@code seqNum}. When
 * duplicates are detected the entry with the higher {@code seqNum} wins. After
 * de-duplication the list is sorted by {@code seqNum} ascending, with
 * {@code createdAt} as a secondary tie-breaker for equal sequence numbers.
 */
public final class ChatMessageMerger {

    private ChatMessageMerger() {}

    /**
     * Merges an existing message list with newly received messages.
     *
     * @param existing current messages (may be null or empty)
     * @param incoming new messages to merge in
     * @return a new list containing all unique messages in deterministic order
     */
    public static List<ChatMessage> merge(List<ChatMessage> existing, List<ChatMessage> incoming) {
        List<ChatMessage> merged = new ArrayList<>();

        if (existing != null) {
            for (ChatMessage msg : existing) {
                upsert(merged, msg);
            }
        }

        if (incoming != null) {
            for (ChatMessage msg : incoming) {
                upsert(merged, msg);
            }
        }

        merged.sort((a, b) -> {
            int cmp = Long.compare(a.getSeqNum(), b.getSeqNum());
            if (cmp != 0) return cmp;
            if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            }
            return 0;
        });

        return merged;
    }

    private static void upsert(List<ChatMessage> messages, ChatMessage candidate) {
        if (candidate == null || candidate.getId() == null) {
            return;
        }

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage existing = messages.get(i);
            if (isDuplicate(existing, candidate)) {
                messages.set(i, choosePreferred(existing, candidate));
                return;
            }
        }

        messages.add(candidate);
    }

    private static boolean isDuplicate(ChatMessage left, ChatMessage right) {
        if (left == null || right == null) {
            return false;
        }

        if (left.getId() != null && right.getId() != null && left.getId().equals(right.getId())) {
            return true;
        }

        return left.getSeqNum() > 0 && left.getSeqNum() == right.getSeqNum();
    }

    private static ChatMessage choosePreferred(ChatMessage current, ChatMessage candidate) {
        if (candidate.getSeqNum() != current.getSeqNum()) {
            return candidate.getSeqNum() > current.getSeqNum() ? candidate : current;
        }

        if (candidate.getId() != null && current.getId() == null) {
            return candidate;
        }

        if (candidate.getCreatedAt() != null && current.getCreatedAt() != null) {
            return candidate.getCreatedAt().isAfter(current.getCreatedAt()) ? candidate : current;
        }

        if (candidate.getCreatedAt() != null) {
            return candidate;
        }

        return current;
    }

    /**
     * Merges a single incoming message into an existing list.
     *
     * @param existing current messages (may be null or empty)
     * @param message  the message to merge
     * @return a new list containing all unique messages in deterministic order
     */
    public static List<ChatMessage> mergeSingle(List<ChatMessage> existing, ChatMessage message) {
        if (message == null) {
            return existing != null ? new ArrayList<>(existing) : new ArrayList<>();
        }
        List<ChatMessage> single = new ArrayList<>(1);
        single.add(message);
        return merge(existing, single);
    }
}
