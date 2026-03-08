package io.github.phunguy65.zms.usermanagement.domain.port;

import org.jspecify.annotations.Nullable;

/**
 * Filter criteria for paginated user queries. All fields are optional (null = no filter).
 *
 * @param emailContains case-insensitive substring match on the user's email address
 * @param authProvider  exact match on the auth provider ({@code EMAIL}, {@code GOOGLE},
 *                      or {@code BOTH})
 */
public record UserFilter(
        @Nullable String emailContains, @Nullable String authProvider) {

    /** Returns a filter with no constraints (returns all active users). */
    public static UserFilter empty() {
        return new UserFilter(null, null);
    }
}
