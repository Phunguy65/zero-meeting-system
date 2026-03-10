package io.github.phunguy65.zms.shared.domain;

import java.util.Optional;

/**
 * Marker interface for slice-based pagination request parameters.
 *
 * <p>Implement this interface on any request DTO that carries {@code page}, {@code size}, and
 * optional {@code sort} parameters. Because Java records are final and cannot extend classes,
 * this interface is the idiomatic way to share a pagination contract across modules without
 * forcing inheritance.
 *
 * <p>Example usage in a feature module:
 *
 * <pre>{@code
 * public record GetUsersRequest(
 *         Integer page,
 *         Integer size,
 *         String sortRaw,
 *         String email) implements SliceParams {
 *     public int pageNumber() { return page; }
 *     public int pageSize() { return size; }
 *     public Optional<String> sort() { return Optional.ofNullable(sortRaw); }
 * }
 * }</pre>
 *
 * <p>Pure Java interface – zero framework dependencies.
 */
public interface SliceParams {

    /** 0-indexed page number. Must be ≥ 0. */
    int pageNumber();

    /** Number of items per page. Must be in range [1, 100]. */
    int pageSize();

    /**
     * Optional sort expression in the form {@code "field,direction"} (e.g. {@code "createdAt,desc"}).
     * Returns empty when no explicit sort is requested.
     */
    Optional<String> sort();

    /** Derived offset for use in SQL/repository queries. */
    default long offset() {
        return (long) pageNumber() * pageSize();
    }

    /**
     * Parses the sort expression into a {@link SortDirection}.
     * Returns {@link SortDirection#ASC} when {@code sort} is empty or has no direction part.
     */
    default SortDirection sortDirection() {
        return sort().map(s -> {
                    var parts = s.split(",", 2);
                    if (parts.length < 2) return SortDirection.ASC;
                    return parts[1].trim().equalsIgnoreCase("desc")
                            ? SortDirection.DESC
                            : SortDirection.ASC;
                })
                .orElse(SortDirection.ASC);
    }

    /**
     * Parses the field name from the sort expression.
     * Returns empty when {@code sort} is absent or blank.
     */
    default Optional<String> sortField() {
        return sort().filter(s -> !s.isBlank()).map(s -> s.split(",", 2)[0].trim());
    }
}
