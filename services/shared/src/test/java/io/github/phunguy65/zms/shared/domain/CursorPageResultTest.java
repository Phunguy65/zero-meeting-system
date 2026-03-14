package io.github.phunguy65.zms.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CursorPageResultTest {

    // ─── of() factory ────────────────────────────────────────────────────────

    @Test
    void of_withItems_returnsRecordWithAllFields() {
        List<String> items = List.of("a", "b", "c");

        CursorPageResult<String> result = CursorPageResult.of(items, 20, true);

        assertThat(result.items()).containsExactly("a", "b", "c");
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void of_hasNextFalse_flagsEndOfResults() {
        CursorPageResult<String> result = CursorPageResult.of(List.of("x"), 10, false);

        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void of_itemsAreCopied_mutationDoesNotAffectResult() {
        List<String> mutable = new ArrayList<>(List.of("a", "b"));

        CursorPageResult<String> result = CursorPageResult.of(mutable, 20, false);
        mutable.add("c");

        assertThat(result.items()).hasSize(2).doesNotContain("c");
    }

    @Test
    void of_resultItemsAreImmutable() {
        CursorPageResult<String> result = CursorPageResult.of(List.of("a"), 20, false);

        assertThatThrownBy(() -> result.items().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ─── empty() factory ─────────────────────────────────────────────────────

    @Test
    void empty_returnsEmptyItemsWithHasNextFalse() {
        CursorPageResult<Integer> result = CursorPageResult.empty(20);

        assertThat(result.items()).isEmpty();
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void empty_preservesPageSize() {
        CursorPageResult<Integer> result = CursorPageResult.empty(50);

        assertThat(result.pageSize()).isEqualTo(50);
    }
}
