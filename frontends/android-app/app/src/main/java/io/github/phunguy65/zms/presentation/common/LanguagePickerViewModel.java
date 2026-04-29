package io.github.phunguy65.zms.presentation.common;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import javax.inject.Inject;

/**
 * ViewModel for the language picker bottom sheet.
 *
 * <p>Provides language preference management through domain abstractions,
 * following the same pattern as {@link io.github.phunguy65.zms.presentation.main.settings.SettingsViewModel}.
 */
@HiltViewModel
public class LanguagePickerViewModel extends ViewModel {

    private final SessionRepository sessionRepository;

    @Inject
    public LanguagePickerViewModel(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Saves the selected language preference.
     *
     * @param language the BCP 47 language tag (e.g., "en", "vi")
     */
    public void saveLanguage(String language) {
        sessionRepository.setLanguage(language);
    }

    /**
     * Gets the current language preference.
     *
     * @return the current language tag
     */
    public String getLanguage() {
        return sessionRepository.getLanguage();
    }
}
