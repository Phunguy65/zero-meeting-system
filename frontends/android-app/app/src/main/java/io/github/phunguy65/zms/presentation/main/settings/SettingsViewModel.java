package io.github.phunguy65.zms.presentation.main.settings;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.domain.model.Theme;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import javax.inject.Inject;

/**
 * ViewModel for the settings screen.
 *
 * <p>Provides theme preference management through domain abstractions.
 */
@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final SessionRepository sessionRepository;

    @Inject
    public SettingsViewModel(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Gets the current theme preference.
     *
     * @return the current Theme
     */
    public Theme getTheme() {
        return sessionRepository.getTheme();
    }

    /**
     * Sets the theme preference.
     *
     * @param theme the theme to set
     */
    public void setTheme(Theme theme) {
        sessionRepository.setTheme(theme);
    }
}
