package io.github.phunguy65.zms.sdk.jsend;

/**
 * Hook for client-side i18n of error messages.
 *
 * <p>Implementations map a machine-readable {@code code} to a locale-specific message.
 * If no translation is available, return {@code defaultMessage} unchanged.
 *
 * <p>A no-op default is provided via {@link #DEFAULT}.
 */
@FunctionalInterface
public interface ErrorTranslator {

    /** Returns a translated message for the given error code, or {@code defaultMessage} as-is. */
    String translate(String code, String defaultMessage);

    /** Pass-through translator that always returns the original message. */
    ErrorTranslator DEFAULT = (code, defaultMessage) -> defaultMessage;
}
