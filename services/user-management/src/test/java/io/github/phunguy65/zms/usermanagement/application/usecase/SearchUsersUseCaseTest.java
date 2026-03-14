package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.shared.domain.CursorPageResult;
import io.github.phunguy65.zms.usermanagement.application.command.SearchUsersQuery;
import io.github.phunguy65.zms.usermanagement.application.helper.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
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
class SearchUsersUseCaseTest {

    @Mock
    UserRepository userRepository;

    SearchUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        var preferencesParser = new UserPreferencesParser(new ObjectMapper());
        useCase = new SearchUsersUseCase(userRepository, preferencesParser);
    }

    private User buildUser(String email) {
        return User.reconstitute(
                UUID.randomUUID(),
                Email.of(email),
                null,
                FullName.of("User"),
                null,
                null,
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void execute_firstPage_returnsMappedContent() {
        var users = List.of(buildUser("a@example.com"), buildUser("b@example.com"));
        when(userRepository.searchUsers(isNull(), anyInt(), any()))
                .thenReturn(CursorPageResult.of(users, 20, false));

        var result = useCase.execute(new SearchUsersQuery(null, 20, null), null);

        assertThat(result.items()).hasSize(2);
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void execute_hasNextPage_flagsHasNext() {
        var users = List.of(buildUser("a@example.com"), buildUser("b@example.com"));
        when(userRepository.searchUsers(isNull(), anyInt(), any()))
                .thenReturn(CursorPageResult.of(users, 20, true));

        var result = useCase.execute(new SearchUsersQuery(null, 20, null), null);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.items()).hasSize(2);
    }

    @Test
    void execute_emptyResult_returnsEmptyContent() {
        when(userRepository.searchUsers(isNull(), anyInt(), any()))
                .thenReturn(CursorPageResult.empty(20));

        var result = useCase.execute(new SearchUsersQuery(null, 20, null), null);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }
}
