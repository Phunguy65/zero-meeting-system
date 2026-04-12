package io.github.phunguy65.zms.data.remote.interceptor;

import android.content.Context;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.github.phunguy65.zms.frontends.R;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Android-specific {@link ErrorTranslator} that maps machine-readable error codes
 * to locale-specific messages using Android string resources.
 *
 * <p>Handles both top-level error codes (e.g. {@code INVALID_CREDENTIALS}) and
 * field-level violation codes (e.g. {@code REQUIRED}, {@code INVALID_FORMAT}).
 * Codes that are not in the map fall through to {@code defaultMessage}.
 *
 * <p>Since {@link Context#getString(int)} reads the current configuration at call time,
 * locale changes take effect on the next translation without caching issues.
 */
@Singleton
public final class AndroidErrorTranslator implements ErrorTranslator {

    private final Context context;

    /**
     * Maps both top-level error codes and violation codes to Android string resource IDs.
     * The two namespaces do not overlap (verified by inspection of backend enums).
     */
    private static final Map<String, Integer> CODE_MAP = Map.ofEntries(
            // Top-level error codes (AuthErrorCode / CommonErrorCode)
            Map.entry("INVALID_CREDENTIALS", R.string.error_invalid_credentials),
            Map.entry("EMAIL_ALREADY_EXISTS", R.string.error_email_already_exists),
            Map.entry("USERNAME_ALREADY_EXISTS", R.string.error_username_already_exists),
            Map.entry("USER_DELETED", R.string.error_user_deleted),
            Map.entry("INVALID_FIREBASE_TOKEN", R.string.error_invalid_firebase_token),
            Map.entry("FIREBASE_AUTH_ERROR", R.string.error_firebase_auth),
            Map.entry("VALIDATION_ERROR", R.string.error_validation),
            // Violation codes (ViolationCode)
            Map.entry("REQUIRED", R.string.validation_required),
            Map.entry("INVALID_FORMAT", R.string.validation_invalid_format),
            Map.entry("TOO_SHORT", R.string.validation_too_short),
            Map.entry("TOO_LONG", R.string.validation_too_long),
            Map.entry("INVALID_VALUE", R.string.validation_invalid_value));

    @Inject
    public AndroidErrorTranslator(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public String translate(String code, String defaultMessage) {
        Integer resId = CODE_MAP.get(code);
        if (resId != null) {
            return context.getString(resId);
        }
        return defaultMessage;
    }
}
