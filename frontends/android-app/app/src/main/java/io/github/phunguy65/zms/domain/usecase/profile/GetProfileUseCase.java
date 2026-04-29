package io.github.phunguy65.zms.domain.usecase.profile;

import io.github.phunguy65.zms.domain.repository.ProfileRepository;
import javax.inject.Inject;

/** Use case for retrieving the current user profile. */
public class GetProfileUseCase {

    private final ProfileRepository profileRepository;

    @Inject
    public GetProfileUseCase(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }
}
