package com.lumination.leadme;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.View;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeScreenTest extends BaseTest {

    @Test
    public void canToggleLeaderLearnerRadio() {
        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(
                allOf(withId(R.id.skip_guide), withText("Continue"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_root_layout),
                                        4),
                                1),
                        isDisplayed()));
        textView.perform(click());

        onView(isRoot()).perform(waitId(R.id.learner_btn, 2000));

        ViewInteraction button = onView(
                allOf(withId(R.id.learner_btn), withText("Learner"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                1),
                        isDisplayed()));
        button.perform(click());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.leader_btn), withText("Leader"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                0),
                        isDisplayed()));
        button2.perform(click());
    }

    @Test
    public void loginPasswordRequired() {
        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(
                allOf(withId(R.id.skip_guide), withText("Continue"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_root_layout),
                                        4),
                                1),
                        isDisplayed()));
        textView.perform(click());

        onView(isRoot()).perform(waitId(R.id.app_login, 2000));

        ViewInteraction button = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("dkearns@lumination.com.au"), closeSoftKeyboard());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.error_text), withText("Please check you have entered your details correctly."),
                        withParent(allOf(withId(R.id.login_signup_view),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        textView2.check(matches(isDisplayed()));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.error_text), withText("Please check you have entered your details correctly."),
                        withParent(allOf(withId(R.id.login_signup_view),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        textView3.check(matches(withText("Please check you have entered your details correctly.")));
    }

    @Test
    public void emailAddressTest() {
        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(
                allOf(withId(R.id.skip_guide), withText("Continue"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_root_layout),
                                        4),
                                1),
                        isDisplayed()));
        textView.perform(click());

        onView(isRoot()).perform(waitId(R.id.app_login, 2000));
        ViewInteraction button = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("test"), closeSoftKeyboard());

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("test"), closeSoftKeyboard());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitId(R.id.error_text, 2000));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.error_text), withText("The email address is badly formatted."),
                        withParent(allOf(withId(R.id.login_signup_view),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        textView3.check(matches(isDisplayed()));
    }
}
