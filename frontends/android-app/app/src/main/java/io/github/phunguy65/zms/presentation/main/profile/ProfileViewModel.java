package io.github.phunguy65.zms.presentation.main.profile;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    @Inject
    public ProfileViewModel() {
        // TODO: Inject AuthRepository to handle logout (clear tokens)
    }

    public void logOut() {
        // TODO: Clear login info from SharedPreferences or DataStore
    }
}
