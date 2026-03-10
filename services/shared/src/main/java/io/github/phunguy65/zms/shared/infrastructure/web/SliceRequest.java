package io.github.phunguy65.zms.shared.infrastructure.web;

import io.github.phunguy65.zms.shared.domain.SliceParams;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Generic HTTP query-parameter object for slice-based pagination.
 *
 * <p>Use this record directly when an endpoint only needs pagination (no domain-specific filters).
 * For endpoints that also carry filter fields, define a feature-specific record that implements
 * {@link SliceParams} instead of wrapping this one.
 *
 * <p>Spring MVC binds query parameters automatically – no {@code @ModelAttribute} annotation is
 * required:
 *
 * <pre>{@code
 * GET /api/v1/items?page=0&size=20&sort=createdAt,desc
 * }</pre>
 *
 * <p>Validation is enforced in the compact constructor so constraints hold regardless of how the
 * record is constructed (HTTP binding, unit tests, etc.).
 *
 * @param page 0-indexed page number (default {@code 0})
 * @param size items per page, clamped to [1, 100] (default {@code 20})
 * @param sortRaw optional sort expression {@code "field,direction"}, e.g. {@code "createdAt,desc"}
 */
public record SliceRequest(
        @Min(0) int page,
        @Min(1) @Max(100) int size,
        @Nullable String sortRaw) implements SliceParams {

    /** Default page size used when no {@code size} query parameter is supplied. */
    public static final int DEFAULT_SIZE = 20;

    /** Hard upper bound on page size to prevent runaway queries. */
    public static final int MAX_SIZE = 100;

    /**
     * Compact constructor – enforces invariants so callers never receive an invalid state.
     *
     * <ul>
     *   <li>{@code page} is floored to 0 if negative.
     *   <li>{@code size} is clamped to [1, {@value MAX_SIZE}].
     * </ul>
     */
    public SliceRequest {
        if (page < 0) page = 0;
        size = Math.max(1, Math.min(size, MAX_SIZE));
    }

    @Override
    public Optional<String> sort() {
        return Optional.ofNullable(sortRaw);
    }

    /** Convenience factory with default pagination values and no sort. */
    public static SliceRequest defaults() {
        return new SliceRequest(0, DEFAULT_SIZE, null);
    }
}
