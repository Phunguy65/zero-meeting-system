package io.github.phunguy65.zms.usermanagement.domain.port;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.GoogleAuthClaims;

public interface GoogleAuthVerifier {

    Result<GoogleAuthClaims, AuthErrorCode> verify(String idToken);
}
