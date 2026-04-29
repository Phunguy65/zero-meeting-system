package io.github.phunguy65.zms.presentation.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.domain.repository.SessionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link LanguagePickerViewModel}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LanguagePickerViewModelTest {

    @Mock
    private SessionRepository sessionRepository;

    private LanguagePickerViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new LanguagePickerViewModel(sessionRepository);
    }

    @Test
    public void saveLanguage_delegatesToSessionRepository() {
        viewModel.saveLanguage("vi");

        verify(sessionRepository).setLanguage("vi");
    }

    @Test
    public void getLanguage_delegatesToSessionRepository() {
        when(sessionRepository.getLanguage()).thenReturn("vi");

        assertEquals("vi", viewModel.getLanguage());
    }

    @Test
    public void getLanguage_returnsEnglishByDefault() {
        when(sessionRepository.getLanguage()).thenReturn("en");

        assertEquals("en", viewModel.getLanguage());
    }
}
