package io.github.phunguy65.zms.usermanagement.domain.model;

import io.github.phunguy65.zms.shared.domain.ValueObject;
import java.util.regex.Pattern;

/**
 * Value object representing a validated, lowercase-normalized email address.
 */
public record Email(String value) implements ValueObject {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        value = value.strip().toLowerCase();
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    public static Email of(String raw) {
        return new Email(raw);
    }
}
