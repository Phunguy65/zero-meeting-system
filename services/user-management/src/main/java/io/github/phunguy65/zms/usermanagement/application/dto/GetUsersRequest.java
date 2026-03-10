package io.github.phunguy65.zms.usermanagement.application.dto;

import io.github.phunguy65.zms.shared.domain.SliceParams;
import io.github.phunguy65.zms.usermanagement.domain.port.UserFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Query parameters for the {@code GET /{version}/users} endpoint.
 *
 * <p>Implements {@link SliceParams} to carry pagination fields ({@code page}, {@code size},
 * {@code sort}) alongside user-specific filter fields ({@code email}, {@code authProvider}).
 * Spring MVC binds all query parameters via constructor binding – absent {@code pageParam} and
 * {@code sizeParam} default to {@code 0} and {@code 20} respectively via the compact constructor.
 *
 * <p>Example request:
 *
 * <pre>{@code
 * GET /v1/users?page=0&size=20&sort=createdAt,desc&email=alice&authProvider=GOOGLE
 * }</pre>
 *
 * @param pageParam 0-indexed page number (default {@code 0})
 * @param sizeParam items per page, clamped to [1, 100] (default {@code 20})
 * @param sortRaw optional sort expression {@code "field,direction"}
 * @param email optional case-insensitive substring filter on email
 * @param authProvider optional exact-match filter on auth provider
 */
public record GetUsersRequest(
        @Min(0) Integer pageParam,
        @Min(1) @Max(100) Integer sizeParam,
        @Nullable String sortRaw,
        @Nullable String email,
        @Nullable String authProvider)
        implements SliceParams {

    /** Compact constructor – applies defaults and enforces pagination invariants. */
    public GetUsersRequest {
        if (pageParam == null || pageParam < 0) pageParam = 0;
        if (sizeParam == null) sizeParam = 20;
        sizeParam = Math.max(1, Math.min(sizeParam, 100));
    }

    @Override
    public int page() {
        return pageParam;
    }

    @Override
    public int size() {
        return sizeParam;
    }

    @Override
    public Optional<String> sort() {
        return Optional.ofNullable(sortRaw);
    }

    /** Optional wrapper for the email filter. */
    public Optional<String> emailFilter() {
        return Optional.ofNullable(email);
    }

    /** Optional wrapper for the authProvider filter. */
    public Optional<String> authProviderFilter() {
        return Optional.ofNullable(authProvider);
    }

    /** Converts the filter fields to the domain {@link UserFilter}. */
    public UserFilter toFilter() {
        return new UserFilter(email, authProvider);
    }
}
