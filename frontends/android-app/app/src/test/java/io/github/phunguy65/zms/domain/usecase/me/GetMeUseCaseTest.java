package io.github.phunguy65.zms.domain.usecase.me;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.MeRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link GetMeUseCase}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GetMeUseCaseTest {

    @Mock
    private MeRepository meRepository;

    private GetMeUseCase useCase;

    @Before
    public void setup() {
        useCase = new GetMeUseCase(meRepository);
    }

    @Test
    public void execute_callsRepository() {
        User user = new User("id", "email@test.com", "Full Name", "username", null);
        when(meRepository.getMe()).thenReturn(CompletableFuture.completedFuture(user));

        useCase.execute();

        verify(meRepository).getMe();
    }

    @Test
    public void execute_success_returnsUser() throws Exception {
        User expectedUser = new User("user123", "test@example.com", "Test User", "testuser", "https://avatar.url");
        when(meRepository.getMe()).thenReturn(CompletableFuture.completedFuture(expectedUser));

        CompletableFuture<User> future = useCase.execute();
        User actualUser = future.get();

        assertEquals(expectedUser, actualUser);
        assertEquals("user123", actualUser.id());
        assertEquals("test@example.com", actualUser.email());
        assertEquals("Test User", actualUser.fullName());
        assertEquals("testuser", actualUser.username());
        assertEquals("https://avatar.url", actualUser.avatarUrl());
    }

    @Test(expected = ExecutionException.class)
    public void execute_failure_throwsException() throws Exception {
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Network error"));
        when(meRepository.getMe()).thenReturn(failedFuture);

        CompletableFuture<User> future = useCase.execute();
        future.get(); // Should throw ExecutionException
    }
}
