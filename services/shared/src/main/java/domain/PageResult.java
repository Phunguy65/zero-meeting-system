package domain;

import java.util.List;

/**
 * A page of sliced results without a total count.
 *
 * <p>Pure Java domain abstraction for offset-based Slice pagination. Consistent with other shared
 * domain types ({@link Result}, {@link Money}, {@link UserId}) – zero Spring/JPA dependencies.
 *
 * <p>The factory method {@code of(...)} is used by infrastructure adapters to build a
 * {@code PageResult} from a Spring Data {@code Slice<Entity>} after entity-to-domain mapping.
 * Infrastructure code is responsible for the mapping; domain code only consumes this record.
 *
 * @param <T> the domain type for each item in the page
 */
public record PageResult<T>(
        List<T> items, int pageNumber, int pageSize, boolean hasNext, boolean hasPrevious) {

    /**
     * Compact canonical constructor – defensively copies {@code items} to make the record
     * immutable.
     */
    public PageResult {
        items = List.copyOf(items);
    }

    /**
     * Factory for infrastructure adapters.
     *
     * <p>Infrastructure code should:
     * <ol>
     *   <li>Map domain objects from entity objects.
     *   <li>Call this factory with the mapped list, page metadata, and the {@code hasNext} flag
     *       derived from Spring Data's {@code Slice.hasNext()}.
     * </ol>
     *
     * @param items       already-mapped domain items (must NOT include the extra probe row)
     * @param pageNumber  0-indexed page number
     * @param pageSize    requested page size
     * @param hasNext     {@code true} if more pages exist (typically from {@code Slice.hasNext()})
     */
    public static <T> PageResult<T> of(
            List<T> items, int pageNumber, int pageSize, boolean hasNext) {
        boolean hasPrevious = pageNumber > 0;
        return new PageResult<>(items, pageNumber, pageSize, hasNext, hasPrevious);
    }

    /** Convenience factory for an empty result set on page 0. */
    public static <T> PageResult<T> empty(int pageSize) {
        return new PageResult<>(List.of(), 0, pageSize, false, false);
    }
}
