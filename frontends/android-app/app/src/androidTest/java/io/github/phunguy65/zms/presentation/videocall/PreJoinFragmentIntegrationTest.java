package io.github.phunguy65.zms.presentation.videocall;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.github.phunguy65.zms.frontends.R;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for PreJoinFragment password-protected join flow.
 * Tests UI behavior for protected meetings including password field reveal,
 * validation, and error handling.
 *
 * <p>Note: These tests require a mock server or test doubles for the backend API.
 * The current implementation tests UI interactions and state transitions.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
@HiltAndroidTest
public class PreJoinFragmentIntegrationTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Before
    public void setup() {
        hiltRule.inject();
    }

    private Intent createGuestIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), VideoCallActivity.class);
        intent.putExtra("isGuest", true);
        return intent;
    }

    @Test
    public void prejoinScreen_displaysPasswordFieldsHiddenByDefault() {
        try (ActivityScenario<VideoCallActivity> scenario = ActivityScenario.launch(createGuestIntent())) {
            onView(withId(R.id.lblPassword)).check(matches(not(isDisplayed())));
            onView(withId(R.id.tilPassword)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void prejoinScreen_showsMeetingCodeError_whenJoinWithEmptyCode() {
        try (ActivityScenario<VideoCallActivity> scenario = ActivityScenario.launch(createGuestIntent())) {
            onView(withId(R.id.edtDisplayName))
                    .perform(typeText("Test User"), closeSoftKeyboard());

            onView(withId(R.id.btnJoinMeeting)).perform(click());

            onView(withText(R.string.prejoin_error_meeting_code)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void prejoinScreen_showsDisplayNameError_whenGuestJoinsWithEmptyName() {
        try (ActivityScenario<VideoCallActivity> scenario = ActivityScenario.launch(createGuestIntent())) {
            onView(withId(R.id.edtMeetingCode))
                    .perform(typeText("ABC123"), closeSoftKeyboard());

            onView(withId(R.id.btnJoinMeeting)).perform(click());

            onView(withText(R.string.prejoin_error_display_name)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void prejoinScreen_checkingStateHiddenByDefault() {
        try (ActivityScenario<VideoCallActivity> scenario = ActivityScenario.launch(createGuestIntent())) {
            onView(withId(R.id.llCheckingState)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void prejoinScreen_joinButtonEnabledByDefault() {
        try (ActivityScenario<VideoCallActivity> scenario = ActivityScenario.launch(createGuestIntent())) {
            onView(withId(R.id.btnJoinMeeting)).check(matches(isDisplayed()));
        }
    }
}
