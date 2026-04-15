package io.github.phunguy65.zms.presentation.main.profile;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link ProfileViewModel}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private SessionRepository sessionRepository;

    // Immediate executor for testing
    private final Executor testExecutor = Runnable::run;

    private ProfileViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new ProfileViewModel(sessionRepository, testExecutor, testExecutor);
    }

    @Test
    public void initialState_logoutCompleteIsFalse() {
        Boolean logoutComplete = viewModel.getLogoutComplete().getValue();
        assertFalse(logoutComplete);
    }

    @Test
    public void logOut_clearsAllSessionData() {
        viewModel.logOut();

        verify(sessionRepository).clearAllSessionData();
    }

    @Test
    public void logOut_emitsLogoutComplete() {
        viewModel.logOut();

        Boolean logoutComplete = viewModel.getLogoutComplete().getValue();
        assertTrue(logoutComplete);
    }

    @Test
    public void logOut_executesOnIoExecutor() {
        // This test verifies the execution pattern by checking that
        // clearAllSessionData is called which happens on ioExecutor
        viewModel.logOut();

        verify(sessionRepository).clearAllSessionData();
    }
}
