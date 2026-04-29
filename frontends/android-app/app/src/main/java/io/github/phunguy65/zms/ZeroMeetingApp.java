package io.github.phunguy65.zms;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import dagger.hilt.android.HiltAndroidApp;
import io.github.phunguy65.zms.domain.model.Theme;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import javax.inject.Inject;

/**
 * Application class for the Zero Meeting app.
 *
 * <p>Initializes Hilt dependency injection and applies the saved theme preference on startup.
 */
@HiltAndroidApp
public class ZeroMeetingApp extends Application {

    @Inject
    SessionRepository sessionRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        applyTheme();
    }

    /**
     * Applies the saved theme preference from SessionRepository.
     * Called during app startup to ensure consistent theming.
     */
    private void applyTheme() {
        Theme theme = sessionRepository.getTheme();
        int nightMode =
                switch (theme) {
                    case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
                    case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
                    case SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                };
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
