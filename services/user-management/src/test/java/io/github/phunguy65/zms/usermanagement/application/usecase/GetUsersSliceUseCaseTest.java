package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.shared.domain.PageResult;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.SliceHttpResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserFilter;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GetUsersSliceUseCaseTest {

    @Mock
    UserRepository userRepository;

    GetUsersSliceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUsersSliceUseCase(
                userRepository, new UserPreferencesParser(new ObjectMapper()));
    }

    private User buildUser(String email) {
        return User.reconstitute(
                UUID.randomUUID(),
                Email.of(email),
                null,
                FullName.of("User"),
                null,
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void execute_returnsMappedSlice() {
        var users = List.of(buildUser("a@example.com"), buildUser("b@example.com"));
        when(userRepository.findActiveUsers(anyInt(), anyInt(), any()))
                .thenReturn(PageResult.of(users, 0, 20, false));

        var result = useCase.execute(0, 20, UserFilter.empty());

        assertThat(result).isInstanceOf(Result.Success.class);
        @SuppressWarnings("unchecked")
        var slice = (SliceHttpResponse<UserResponse>) ((Result.Success<?, ?>) result).value();
        assertThat(slice.content()).hasSize(2);
        assertThat(slice.page()).isZero();
        assertThat(slice.size()).isEqualTo(20);
        assertThat(slice.hasNext()).isFalse();
        assertThat(slice.hasPrevious()).isFalse();
    }

    @Test
    void execute_emptyResult_returnsEmptySlice() {
        when(userRepository.findActiveUsers(anyInt(), anyInt(), any()))
                .thenReturn(PageResult.of(List.of(), 0, 20, false));

        var result = useCase.execute(0, 20, UserFilter.empty());

        assertThat(result).isInstanceOf(Result.Success.class);
        @SuppressWarnings("unchecked")
        var slice = (SliceHttpResponse<UserResponse>) ((Result.Success<?, ?>) result).value();
        assertThat(slice.content()).isEmpty();
        assertThat(slice.hasNext()).isFalse();
    }
}
