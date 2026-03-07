package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.RegisterRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.RegisterResponse;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Result<RegisterResponse, AuthErrorCode> execute(RegisterRequest request) {
        Email email = Email.of(request.email());

        if (userRepository.existsByEmail(email)) {
            return Result.failure(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        var hashedPassword = passwordHasher.hash(request.password());
        var fullName = FullName.of(request.fullName());
        var user = User.register(email, hashedPassword, fullName);
        var saved = userRepository.save(user);

        return Result.success(new RegisterResponse(
                saved.getId(), saved.getEmail().value(), saved.getFullName().value()));
    }
}
