package com.rudyii.hsw.client;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.rudyii.hsw.client.activities.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testSubscribeAndLog() throws InterruptedException {
        onView(withId(R.id.informationTextView)).check(matches(isDisplayed()));

        // Click subscribe button and check toast
        onView(allOf(withId(R.id.SAVE_SERVER_BUTTON), withText(R.string.SAVE_SERVER_BUTTON)))
                .check(matches(isDisplayed()))
                .perform(click());
        confirmToastStartsWith(mActivityRule.getActivity().getString(R.string.POPUP_SAVED_SERVER));

        // Sleep so the Toast goes away, this is lazy but it works (Toast.LENGTH_SHORT = 2000)
        Thread.sleep(2000);

        // Click log token and check toast
        onView(allOf(withId(R.id.SHOW_TOKEN_BUTTON), withText(R.string.REGISTER_USER_BUTTON)))
                .check(matches(isDisplayed()))
                .perform(click());
        confirmToastStartsWith(mActivityRule.getActivity().getString(R.string.POPUP_TOKEN, ""));
    }

    private void confirmToastStartsWith(String string) {
        View activityWindowDecorView = mActivityRule.getActivity().getWindow().getDecorView();
        onView(withText(startsWith(string)))
                .inRoot(withDecorView(not(is(activityWindowDecorView))))
                .check(matches(isDisplayed()));
    }

}
