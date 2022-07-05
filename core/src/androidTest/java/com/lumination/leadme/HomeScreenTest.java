package com.lumination.leadme;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeScreenTest extends BaseTest {

    @Test
    public void canToggleLeaderLearnerRadio() throws InterruptedException {
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

        Thread.sleep(1500);

        onView(isRoot()).perform(waitId(R.id.learner_btn, 2000));
        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.learner_btn), withText("Learner"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                1),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

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
    public void loginPasswordRequired() throws InterruptedException {
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

        Thread.sleep(1500);

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

        Thread.sleep(1500);

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

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.error_text), withText("Please check you have entered your details correctly."),
                        withParent(allOf(withId(R.id.login_signup_view),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        textView2.check(matches(isDisplayed()));

        Thread.sleep(1500);

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.error_text), withText("Please check you have entered your details correctly."),
                        withParent(allOf(withId(R.id.login_signup_view),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        textView3.check(matches(withText("Please check you have entered your details correctly.")));
    }

    @Test
    public void emailAddressTest() throws InterruptedException {
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

        Thread.sleep(1500);

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

        Thread.sleep(1500);

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

        Thread.sleep(1500);

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

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        Thread.sleep(1500);

        onView(isRoot()).perform(waitId(R.id.error_text, 2000));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.error_text), withText("The email address is badly formatted."),
                        withParent(allOf(withId(R.id.login_signup_view),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        textView3.check(matches(isDisplayed()));
    }

    @Test
    public void leadMeMainTest00() {
        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(withId(R.id.skip_guide));
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
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());
        onView(isRoot()).perform(waitId(R.id.login_enter, 2000));
        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_back, 2000));

        ViewInteraction button3 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button3.perform(click());
    }


    @Test
    public void leadMeLeaderActions() throws InterruptedException {

        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(withId(R.id.skip_guide));
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

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.login_enter, 2000));
        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());


        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.app_core_btn, 5000));

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.app_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                0)));
        linearLayout.perform(scrollTo(), click());
        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(0);
        linearLayout2.perform(scrollTo(), longClick());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button3.perform(click());

        Thread.sleep(1500);

        DataInteraction linearLayout3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(1);
        linearLayout3.perform(scrollTo(), longClick());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button4.perform(click());

        Thread.sleep(1500);

        DataInteraction linearLayout4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.fav_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                3)))
                .atPosition(0);
        linearLayout4.perform(scrollTo(), longClick());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button5.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 1000));
        onView(isRoot()).perform(waitId(R.id.app_list_grid, 5000));
        Thread.sleep(1500);

        DataInteraction linearLayout5 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(0);
        linearLayout5.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.button_view),
                                        1),
                                1),
                        isDisplayed()));
        button6.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout6 = onView(
                allOf(withId(R.id.curated_content_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                1)));
        linearLayout6.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.filter_button), withText("Filter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        1),
                                0),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction radioButton = onView(
                allOf(withId(R.id.radio2), withText("Within"),
                        childAtPosition(
                                allOf(withId(R.id.video_type_radio),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                1),
                        isDisplayed()));
        radioButton.perform(click());

        Thread.sleep(1500);

        ViewInteraction button8 = onView(
                allOf(withId(R.id.apply_filters), withText("Apply"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        4),
                                1),
                        isDisplayed()));
        button8.perform(click());

        DataInteraction linearLayout7 = onData(anything())
                .inAdapterView(allOf(withId(R.id.curated_content_list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(0);
        linearLayout7.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox = onView(
                allOf(withId(R.id.fav_checkbox_curated_content), withText("Add to favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        checkBox.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button9 = onView(
                allOf(withId(R.id.select_item), withText("Push"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        button9.perform(click());

        Thread.sleep(1500);

        ViewInteraction button10 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.within_select_btns),
                                        2),
                                1),
                        isDisplayed()));
        button10.perform(click());

        Thread.sleep(1500);

        ViewInteraction button11 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.push_success_view),
                                                3)),
                                0),
                        isDisplayed()));
        button11.perform(click());

        Thread.sleep(1500);

        ViewInteraction button12 = onView(
                allOf(withId(R.id.video_back_btn), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.playback_btns),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                3)),
                                2),
                        isDisplayed()));
        button12.perform(click());
        Thread.sleep(1500);
        ViewInteraction linearLayout8 = onView(
                allOf(withId(R.id.url_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                2)));
        linearLayout8.perform(scrollTo(), click());
        Thread.sleep(1500);
        ViewInteraction textView3 = onView(
                allOf(withId(R.id.open_favourites), withText("Browse favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.url_entry_view),
                                        2),
                                0),
                        isDisplayed()));
        textView3.perform(click());
        Thread.sleep(1500);
        ViewInteraction button13 = onView(
                allOf(withId(R.id.yt_del_btn), withText("Clear"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        button13.perform(click());

        Thread.sleep(1500);

        ViewInteraction button14 = onView(
                allOf(withId(R.id.ok_btn), withText("Yes, delete them"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.custom),
                                        0),
                                3),
                        isDisplayed()));
        button14.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout9 = onView(
                allOf(withId(R.id.url_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                2)));
        linearLayout9.perform(scrollTo(), click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.url_input_field),
                        childAtPosition(
                                allOf(withId(R.id.url_entry_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                0)),
                                1),
                        isDisplayed()));
        editText2.perform(click());

        Thread.sleep(1500);


        ViewInteraction editText3 = onView(
                allOf(withId(R.id.url_input_field),
                        childAtPosition(
                                allOf(withId(R.id.url_entry_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                0)),
                                1),
                        isDisplayed()));
        editText3.perform(replaceText("google.com"), closeSoftKeyboard());
        Thread.sleep(1500);
        ViewInteraction button15 = onView(
                allOf(withId(R.id.confirm_btn), withText("Next"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                1)),
                                0),
                        isDisplayed()));
        button15.perform(click());
        Thread.sleep(1500);
        ViewInteraction button16 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.playback_btns),
                                        1),
                                0),
                        isDisplayed()));
        button16.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction button17 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.push_success_view),
                                                3)),
                                0),
                        isDisplayed()));
        button17.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout10 = onView(
                allOf(withId(R.id.xray_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                4)));
        linearLayout10.perform(scrollTo(), click());
        Thread.sleep(1500);
        ViewInteraction linearLayout11 = onView(
                allOf(withId(R.id.vr_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                5)));
        linearLayout11.perform(scrollTo(), click());
        Thread.sleep(1500);
        ViewInteraction button18 = onView(
                allOf(withId(R.id.select_source_btn), withText("Choose Video"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                0),
                        isDisplayed()));
        button18.perform(click());
        Thread.sleep(1500);
        ViewInteraction button19 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.warning_dialog_view),
                                                3)),
                                0),
                        isDisplayed()));
        button19.perform(click());
        Thread.sleep(1500);
        ViewInteraction button20 = onView(
                allOf(withId(R.id.cancel_btn), withText("Cancel"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                2),
                        isDisplayed()));
        button20.perform(click());
        Thread.sleep(1500);
        ViewInteraction linearLayout12 = onView(
                allOf(withId(R.id.file_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                6)));
        linearLayout12.perform(scrollTo(), click());
        Thread.sleep(1500);
        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.menu_btn), withContentDescription("Lumination Logo"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView2.perform(click());
        Thread.sleep(1500);
        ViewInteraction switch_ = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_.perform(click());
        Thread.sleep(1500);
        ViewInteraction switch_2 = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_2.perform(click());
        Thread.sleep(1500);
        ViewInteraction switch_3 = onView(
                allOf(withId(R.id.file_transfer), withText("Allow File Transfer"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                8),
                        isDisplayed()));
        switch_3.perform(click());
        Thread.sleep(1500);
        ViewInteraction switch_4 = onView(
                allOf(withId(R.id.file_transfer), withText("Allow File Transfer"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                8),
                        isDisplayed()));
        switch_4.perform(click());
        Thread.sleep(1500);
        ViewInteraction button21 = onView(
                allOf(withId(R.id.options_endSess), withText("End Session"),
                        childAtPosition(
                                allOf(withId(R.id.connected_only_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                0)),
                                2),
                        isDisplayed()));
        button21.perform(click());
    }

    @Test
    public void leadMeMainTest01() {
        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(withId(R.id.skip_guide));
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
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.app_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                0)));
        linearLayout.perform(scrollTo(), click());

        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(1);
        linearLayout2.perform(scrollTo(), click());

        ViewInteraction button3 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.button_view),
                                        1),
                                1),
                        isDisplayed()));
        button3.perform(click());

        ViewInteraction linearLayout3 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout3.perform(scrollTo(), click());
    }

    @Test
    public void forgotPassword() throws InterruptedException {

        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(withId(R.id.skip_guide));
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

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());
        Thread.sleep(1500);
        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText(""), closeSoftKeyboard());
        Thread.sleep(1500);
        ViewInteraction textView2 = onView(
                allOf(withId(R.id.login_forgotten), withText("Forgot password?"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        4),
                                1),
                        isDisplayed()));
        textView2.perform(click());
        Thread.sleep(1500);
        ViewInteraction editText2 = onView(
                allOf(withId(R.id.forgot_email),
                        childAtPosition(
                                allOf(withId(R.id.forgot_layout),
                                        childAtPosition(
                                                withId(R.id.top_view),
                                                1)),
                                2),
                        isDisplayed()));
        editText2.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());
        Thread.sleep(1500);
        ViewInteraction button4 = onView(
                allOf(withId(R.id.forgot_enter), withText("Send"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                1)),
                                0),
                        isDisplayed()));
        button4.perform(click());
        Thread.sleep(1500);
        ViewInteraction button5 = onView(
                allOf(withId(R.id.forgot_enter), withText("Done"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                1)),
                                0),
                        isDisplayed()));
        button5.perform(click());
    }

    @Test
    public void signupPWDError() throws InterruptedException {
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

        onView(isRoot()).perform(waitId(R.id.learner_btn, 3000));

        ViewInteraction button = onView(
                allOf(withId(R.id.learner_btn), withText("Learner"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                1),
                        isDisplayed()));
        button.perform(click());

        onView(isRoot()).perform(waitId(R.id.leader_btn, 3000));
        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.leader_btn), withText("Leader"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                0),
                        isDisplayed()));
        button2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button3.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_signup, 3000));
        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.login_signup), withText("Sign up for account"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        4),
                                0),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox = onView(
                allOf(withId(R.id.tou_check), withText("I agree"),
                        childAtPosition(
                                allOf(withId(R.id.terms_of_use),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                3)),
                                2),
                        isDisplayed()));
        checkBox.perform(click());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button4.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.signup_name),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("Tester"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.signup_email),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                3),
                        isDisplayed()));
        editText2.perform(replaceText("kabilan1033@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.signup_password),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                4),
                        isDisplayed()));
        editText3.perform(replaceText("0987654"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText4 = onView(
                allOf(withId(R.id.signup_confirmpass),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText4.perform(replaceText("9876543"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.signup_error), withText("Please enter a password with 8 or more characters"),
                        withParent(allOf(withId(R.id.signup_page),
                                withParent(IsInstanceOf.<View>instanceOf(ViewGroup.class)))),
                        isDisplayed()));
        textView3.check(matches(withText("Please enter a password with 8 or more characters")));

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button6.perform(click());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button7.perform(click());
    }

    @Test
    public void loginwithPIN() throws InterruptedException {
        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(withId(R.id.skip_guide));
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

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.login_forgotten), withText("Forgot password?"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        4),
                                1),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.forgot_email),
                        childAtPosition(
                                allOf(withId(R.id.forgot_layout),
                                        childAtPosition(
                                                withId(R.id.top_view),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.forgot_enter), withText("Send"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                1)),
                                0),
                        isDisplayed()));
        button2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.forgot_enter), withText("Done"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                1)),
                                0),
                        isDisplayed()));
        button3.perform(click());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button4.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText2.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText3.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button6.perform(click());

        Thread.sleep(1500);

        ViewInteraction pinEntryEditText = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText.perform(replaceText("3214"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction frameLayout = onView(
                allOf(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class), isDisplayed()));
        frameLayout.check(matches(isDisplayed()));

        Thread.sleep(1500);

        ViewInteraction button8 = onView(
                allOf(withId(R.id.close_login_alert_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.wrong_code_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                0)),
                                5),
                        isDisplayed()));
        button8.perform(click());

        Thread.sleep(1500);

        ViewInteraction pinEntryEditText2 = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText2.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction pinEntryEditText3 = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText3.perform(replaceText("1234"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button9 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button9.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout2 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout2.perform(scrollTo(), click());
    }

    @Test
    public void createNewPin() throws InterruptedException {
        onView(isRoot()).perform(waitId(R.id.skip_guide, 2000));

        ViewInteraction textView = onView(withId(R.id.skip_guide));
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

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout.perform(scrollTo(), click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.login_forgot_pin), withText("Forgot pin?"),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                2),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.pin_reset_password),
                        childAtPosition(
                                allOf(withId(R.id.pin_reset_pass_view),
                                        childAtPosition(
                                                withId(R.id.view_holder),
                                                2)),
                                3),
                        isDisplayed()));
        editText2.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.pin_reset_confirm), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        button6.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.signup_pin1),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                0),
                        isDisplayed()));
        editText3.perform(replaceText("6"), closeSoftKeyboard());


        ViewInteraction editText4 = onView(
                allOf(withId(R.id.signup_pin2),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                1),
                        isDisplayed()));
        editText4.perform(replaceText("7"), closeSoftKeyboard());


        ViewInteraction editText5 = onView(
                allOf(withId(R.id.signup_pin3),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                2),
                        isDisplayed()));
        editText5.perform(replaceText("8"), closeSoftKeyboard());


        ViewInteraction editText6 = onView(
                allOf(withId(R.id.signup_pin4),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                3),
                        isDisplayed()));
        editText6.perform(replaceText("9"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText7 = onView(
                allOf(withId(R.id.signup_pin5),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                0),
                        isDisplayed()));
        editText7.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText8 = onView(
                allOf(withId(R.id.signup_pin5),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                0),
                        isDisplayed()));
        editText8.perform(replaceText("6"), closeSoftKeyboard());


        ViewInteraction editText9 = onView(
                allOf(withId(R.id.signup_pin6),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                1),
                        isDisplayed()));
        editText9.perform(replaceText("7"), closeSoftKeyboard());


        ViewInteraction editText10 = onView(
                allOf(withId(R.id.signup_pin7),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                2),
                        isDisplayed()));
        editText10.perform(replaceText("8"), closeSoftKeyboard());


        ViewInteraction editText11 = onView(
                allOf(withId(R.id.signup_pin8),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                3),
                        isDisplayed()));
        editText11.perform(replaceText("9"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.pin_reset_confirm), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction button8 = onView(
                allOf(withId(R.id.pin_reset_confirm), withText("Finish"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        button8.perform(click());

        Thread.sleep(1500);

        ViewInteraction button9 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button9.perform(click());

        Thread.sleep(1500);

        ViewInteraction pinEntryEditText2 = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText2.perform(replaceText("6789"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button10 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button10.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout3 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout3.perform(scrollTo(), click());
    }

    @Test
    public void loginFromOption() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.viewswitcher),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_ = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_2 = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.options_loginBtn), withText("Login"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                10),
                        isDisplayed()));
        button3.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button4.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
               Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout.perform(scrollTo(), click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction pinEntryEditText = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText.perform(replaceText("4567"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button6.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout2 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout2.perform(scrollTo(), click());
    }


        @Test
        public void learnerTrial1() throws InterruptedException {
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

            Thread.sleep(1500);

            ViewInteraction button = onView(
                    allOf(withId(R.id.learner_btn), withText("Learner"),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(is("android.widget.LinearLayout")),
                                            2),
                                    1),
                            isDisplayed()));
            button.perform(click());

            Thread.sleep(1500);


        }

    @Test
    public void diffLogin() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.viewswitcher),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_ = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_2 = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.options_loginBtn), withText("Login"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                10),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button6.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout.perform(scrollTo(), click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction pinEntryEditText = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText.perform(replaceText("4567"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button8 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button8.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout2 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout2.perform(scrollTo(), click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction button9 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button9.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.login_forgot_pin), withText("Forgot pin?"),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                2),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.pin_reset_password),
                        childAtPosition(
                                allOf(withId(R.id.pin_reset_pass_view),
                                        childAtPosition(
                                                withId(R.id.view_holder),
                                                2)),
                                3),
                        isDisplayed()));
        editText2.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button10 = onView(
                allOf(withId(R.id.pin_reset_confirm), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        button10.perform(click());


        Thread.sleep(1500);

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.signup_pin1),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                0),
                        isDisplayed()));
        editText3.perform(replaceText("4"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText4 = onView(
                allOf(withId(R.id.signup_pin2),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                1),
                        isDisplayed()));
        editText4.perform(replaceText("5"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText5 = onView(
                allOf(withId(R.id.signup_pin3),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                2),
                        isDisplayed()));
        editText5.perform(replaceText("6"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText6 = onView(
                allOf(withId(R.id.signup_pin4),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        3),
                                3),
                        isDisplayed()));
        editText6.perform(replaceText("7"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText7 = onView(
                allOf(withId(R.id.signup_pin5),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                0),
                        isDisplayed()));
        editText7.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText8 = onView(
                allOf(withId(R.id.signup_pin5),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                0),
                        isDisplayed()));
        editText8.perform(replaceText("4"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText9 = onView(
                allOf(withId(R.id.signup_pin6),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                1),
                        isDisplayed()));
        editText9.perform(replaceText("5"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText10 = onView(
                allOf(withId(R.id.signup_pin7),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                2),
                        isDisplayed()));
        editText10.perform(replaceText("6"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText11 = onView(
                allOf(withId(R.id.signup_pin8),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.set_pin),
                                        5),
                                3),
                        isDisplayed()));
        editText11.perform(replaceText("7"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button11 = onView(
                allOf(withId(R.id.pin_reset_confirm), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        button11.perform(click());

        Thread.sleep(1500);

        ViewInteraction button12 = onView(
                allOf(withId(R.id.pin_reset_confirm), withText("Finish"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        button12.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction button13 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button13.perform(click());

        Thread.sleep(1500);

        ViewInteraction pinEntryEditText2 = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText2.perform(replaceText("4567"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button14 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button14.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout3 = onView(
                allOf(withId(R.id.xray_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                4)));
        linearLayout3.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction linearLayout4 = onView(
                allOf(withId(R.id.app_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                0)));
        linearLayout4.perform(scrollTo(), click());

        Thread.sleep(1500);

        DataInteraction linearLayout5 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(0);
        linearLayout5.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button15 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.button_view),
                                        1),
                                1),
                        isDisplayed()));
        button15.perform(click());

        Thread.sleep(1500);

        ViewInteraction button16 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.push_success_view),
                                                3)),
                                0),
                        isDisplayed()));
        button16.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout6 = onView(
                allOf(withId(R.id.curated_content_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                1)));
        linearLayout6.perform(scrollTo(), click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Preview image"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        0),
                                0),
                        isDisplayed()));
        imageView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout7 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout7.perform(scrollTo(), click());
    }

    @Test
    public void manualConnect() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.viewswitcher),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.manual_connect), withText("Manual connect"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                5),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.manual_name),
                        childAtPosition(
                                allOf(withId(R.id.manual_learner_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        editText.perform(replaceText("Learner"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.manual_enterIP), withText("10.0.2."),
                        childAtPosition(
                                allOf(withId(R.id.manual_learner_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText2.perform(replaceText("190.1.1.2."));

        Thread.sleep(1500);

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.manual_enterIP), withText("190.1.1.2."),
                        childAtPosition(
                                allOf(withId(R.id.manual_learner_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText3.perform(closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.manual_ok), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.onboardperm_cancel_btn), withText("Cancel"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.RelativeLayout")),
                                        0),
                                7),
                        isDisplayed()));
        button2.perform(click());
    }

    @Test
    public void displayOnboarding() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.viewswitcher),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.on_boarding), withText("Display Onboarding"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView3.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView4 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView4.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView5 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView5.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView6 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView6.perform(click());

        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.onboard_ok_btn), withText("Finish"),
                        childAtPosition(
                                allOf(withId(R.id.onboard_buttons),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                7)),
                                0),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.on_boarding), withText("Display Onboarding"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        textView3.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView7 = onView(
                allOf(withId(R.id.onboard_btn_2),
                        childAtPosition(
                                allOf(withId(R.id.onboard_pages),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                4)),
                                1),
                        isDisplayed()));
        imageView7.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView8 = onView(
                allOf(withId(R.id.onboard_btn_3),
                        childAtPosition(
                                allOf(withId(R.id.onboard_pages),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                4)),
                                2),
                        isDisplayed()));
        imageView8.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView9 = onView(
                allOf(withId(R.id.onboard_btn_4),
                        childAtPosition(
                                allOf(withId(R.id.onboard_pages),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                4)),
                                3),
                        isDisplayed()));
        imageView9.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView10 = onView(
                allOf(withId(R.id.onboard_btn_5),
                        childAtPosition(
                                allOf(withId(R.id.onboard_pages),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                4)),
                                4),
                        isDisplayed()));
        imageView10.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView11 = onView(
                allOf(withId(R.id.onboard_btn_4),
                        childAtPosition(
                                allOf(withId(R.id.onboard_pages),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                4)),
                                3),
                        isDisplayed()));
        imageView11.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView12 = onView(
                allOf(withId(R.id.onboard_btn_3),
                        childAtPosition(
                                allOf(withId(R.id.onboard_pages),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                4)),
                                2),
                        isDisplayed()));
        imageView12.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.skip_intro), withText("Skip Intro"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                6),
                        isDisplayed()));
        textView4.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.on_boarding), withText("Display Onboarding"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        textView5.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView13 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView13.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView14 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView14.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView15 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView15.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView16 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView16.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView17 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView17.perform(click());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.onboard_ok_btn), withText("Finish"),
                        childAtPosition(
                                allOf(withId(R.id.onboard_buttons),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                7)),
                                0),
                        isDisplayed()));
        button2.perform(click());
    }

    @Test
    public void optionsMenu() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.viewswitcher),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.on_boarding), withText("Display Onboarding"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView3.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView4 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView4.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView5 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView5.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView6 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView6.perform(click());

        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.onboard_ok_btn), withText("Finish"),
                        childAtPosition(
                                allOf(withId(R.id.onboard_buttons),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                7)),
                                0),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.manual_connect), withText("Manual connect"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                5),
                        isDisplayed()));
        textView3.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.manual_name),
                        childAtPosition(
                                allOf(withId(R.id.manual_learner_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        editText.perform(replaceText("learner"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.manual_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                2)),
                                1),
                        isDisplayed()));
        button2.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_ = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_2 = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_2.perform(click());

        Thread.sleep(1500);


        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.options_notsigned),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                11),
                        isDisplayed()));
        linearLayout.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox = onView(
                allOf(withId(R.id.tou_check), withText("I agree"),
                        childAtPosition(
                                allOf(withId(R.id.terms_of_use),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                3)),
                                2),
                        isDisplayed()));
        checkBox.perform(click());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox2 = onView(
                allOf(withId(R.id.signup_marketing), withText("I want to recieve emails about product updates, new features and offerings (optional)"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                6),
                        isDisplayed()));
        checkBox2.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox3 = onView(
                allOf(withId(R.id.signup_marketing), withText("I want to recieve emails about product updates, new features and offerings (optional)"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                6),
                        isDisplayed()));
        checkBox3.perform(click());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button6.perform(click());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView7 = onView(
                allOf(withId(R.id.back_btn),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        imageView7.perform(click());
    }

    @Test
    public void leaderOverview() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());


        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn), withContentDescription("Lumination Logo"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_ = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_2 = onView(
                allOf(withId(R.id.file_transfer), withText("Allow File Transfer"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                8),
                        isDisplayed()));
        switch_2.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_3 = onView(
                allOf(withId(R.id.file_transfer), withText("Allow File Transfer"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                8),
                        isDisplayed()));
        switch_3.perform(click());

        Thread.sleep(1500);

        ViewInteraction switch_4 = onView(
                allOf(withId(R.id.server_discovery), withText("Server Discovery"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                6),
                        isDisplayed()));
        switch_4.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.back_btn),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        imageView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.alerts_button), withText("Alerts (0)"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.top_bar),
                                        0),
                                3),
                        isDisplayed()));
        button3.perform(click());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.confirm_btn), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.custom),
                                        0),
                                3),
                        isDisplayed()));
        button4.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.menu_btn), withContentDescription("Lumination Logo"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView3.perform(click());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.options_endSess), withText("End Session"),
                        childAtPosition(
                                allOf(withId(R.id.connected_only_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                0)),
                                2),
                        isDisplayed()));
        button5.perform(click());
    }

    @Test
    public void favourites() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.curated_content_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                1)));
        linearLayout.perform(scrollTo(), click());

        Thread.sleep(1500);

        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.curated_content_list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(2);
        linearLayout2.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox = onView(
                allOf(withId(R.id.fav_checkbox_curated_content), withText("Add to favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        checkBox.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.close_curated_content_single), withText("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        button3.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Preview image"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout3 = onView(
                allOf(withId(R.id.url_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                2)));
        linearLayout3.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.url_input_field),
                        childAtPosition(
                                allOf(withId(R.id.url_entry_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                0)),
                                1),
                        isDisplayed()));
        editText2.perform(replaceText("google.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.confirm_btn), withText("Next"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                1)),
                                0),
                        isDisplayed()));
        button4.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox2 = onView(
                allOf(withId(R.id.fav_checkbox), withText("Add to favourites"),
                        childAtPosition(
                                allOf(withId(R.id.preview_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                0)),
                                4),
                        isDisplayed()));
        checkBox2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.back_btn), withText("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.playback_btns),
                                        1),
                                1),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.open_favourites), withText("Browse favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.url_entry_view),
                                        2),
                                0),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.url_add_btn), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        3),
                                1),
                        isDisplayed()));
        button6.perform(click());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.confirm_btn), withText("Next"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                1)),
                                0),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction button8 = onView(
                allOf(withId(R.id.push_btn), withText("Add to favourites?"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.playback_btns),
                                        1),
                                0),
                        isDisplayed()));
        button8.perform(click());

        Thread.sleep(1500);

        ViewInteraction button9 = onView(
                allOf(withId(R.id.url_del_btn), withText("Clear"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        3),
                                2),
                        isDisplayed()));
        button9.perform(click());

        Thread.sleep(1500);

        ViewInteraction button10 = onView(
                allOf(withId(R.id.ok_btn), withText("Yes, delete them"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.custom),
                                        0),
                                3),
                        isDisplayed()));
        button10.perform(click());

        Thread.sleep(1500);

        ViewInteraction button11 = onView(
                allOf(withId(R.id.url_add_btn), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        3),
                                1),
                        isDisplayed()));
        button11.perform(click());

        Thread.sleep(1500);

        ViewInteraction button12 = onView(
                allOf(withId(R.id.confirm_btn), withText("Next"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                1)),
                                0),
                        isDisplayed()));
        button12.perform(click());

        Thread.sleep(1500);

        ViewInteraction button13 = onView(
                allOf(withId(R.id.push_btn), withText("Add to favourites?"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.playback_btns),
                                        1),
                                0),
                        isDisplayed()));
        button13.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction button14 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.push_success_view),
                                                3)),
                                0),
                        isDisplayed()));
        button14.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.clear_fav_btn, 5000));
        Thread.sleep(1500);

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.clear_fav_btn), withText("Clear"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        textView3.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.ok_btn, 5000));
        Thread.sleep(1500);


        ViewInteraction button15 = onView(
                allOf(withId(R.id.ok_btn), withText("Yes, delete them"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.custom),
                                        0),
                                3),
                        isDisplayed()));
        button15.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        imageView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.menu_btn), withContentDescription("Lumination Logo"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView3.perform(click());

        Thread.sleep(1500);

        ViewInteraction button16 = onView(
                allOf(withId(R.id.options_endSess), withText("End Session"),
                        childAtPosition(
                                allOf(withId(R.id.connected_only_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                0)),
                                2),
                        isDisplayed()));
        button16.perform(click());
    }

    @Test
    public void pUSHVR() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.vr_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                5)));
        linearLayout.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.cancel_btn), withText("Cancel"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                1),
                        isDisplayed()));
        button3.perform(click());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.select_video_source_btn), withText("Videos"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                0),
                        isDisplayed()));
        button4.perform(click());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.video_back_btn), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.playback_btns),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.select_photo_source_btn), withText("Photos"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                1),
                        isDisplayed()));
        button6.perform(click());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.photo_back_btn), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.playback_btns),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction button8 = onView(
                allOf(withId(R.id.cancel_btn), withText("Cancel"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                2),
                        isDisplayed()));
        button8.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout2 = onView(
                allOf(withId(R.id.vr_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                5)));
        linearLayout2.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button9 = onView(
                allOf(withId(R.id.select_video_source_btn), withText("Videos"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                0),
                        isDisplayed()));
        button9.perform(click());

        Thread.sleep(1500);

        ViewInteraction button10 = onView(
                allOf(withId(R.id.video_back_btn), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.playback_btns),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        button10.perform(click());

        Thread.sleep(1500);

        ViewInteraction button11 = onView(
                allOf(withId(R.id.cancel_btn), withText("Cancel"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                2),
                        isDisplayed()));
        button11.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout3 = onView(
                allOf(withId(R.id.vr_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                5)));
        linearLayout3.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button12 = onView(
                allOf(withId(R.id.cancel_btn), withText("Cancel"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.first_time_vr_dialog_view),
                                                3)),
                                2),
                        isDisplayed()));
        button12.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout4 = onView(
                allOf(withId(R.id.file_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                6)));
        linearLayout4.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction linearLayout5 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout5.perform(scrollTo(), click());
    }

    @Test
    public void pUSHButtons() throws InterruptedException {
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

        Thread.sleep(1500);

        ViewInteraction button = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(replaceText("Tester123"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button2 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        Thread.sleep(1500);

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.app_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                0)));
        linearLayout.perform(scrollTo(), click());

        Thread.sleep(1500);

        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(0);
        linearLayout2.perform(scrollTo(), longClick());

        Thread.sleep(1500);

        ViewInteraction button3 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button3.perform(click());

        Thread.sleep(1500);

        DataInteraction linearLayout3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.fav_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                3)))
                .atPosition(0);
        linearLayout3.perform(scrollTo(), longClick());

        Thread.sleep(1500);

        ViewInteraction button4 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button4.perform(click());

        Thread.sleep(1500);

        DataInteraction linearLayout4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(1);
        linearLayout4.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button5 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.button_view),
                                        1),
                                1),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout5 = onView(
                allOf(withId(R.id.curated_content_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                1)));
        linearLayout5.perform(scrollTo(), click());

        Thread.sleep(1500);

        DataInteraction linearLayout6 = onData(anything())
                .inAdapterView(allOf(withId(R.id.curated_content_list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(1);
        linearLayout6.perform(click());

        Thread.sleep(1500);

        ViewInteraction checkBox = onView(
                allOf(withId(R.id.fav_checkbox_curated_content), withText("Add to favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        checkBox.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button6 = onView(
                allOf(withId(R.id.close_curated_content_single), withText("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        button6.perform(click());

        Thread.sleep(1500);

        ViewInteraction button7 = onView(
                allOf(withId(R.id.filter_button), withText("Filter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        1),
                                0),
                        isDisplayed()));
        button7.perform(click());

        Thread.sleep(1500);

        ViewInteraction radioButton = onView(
                allOf(withId(R.id.radio2), withText("Within"),
                        childAtPosition(
                                allOf(withId(R.id.video_type_radio),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                1),
                        isDisplayed()));
        radioButton.perform(click());

        Thread.sleep(1500);

        ViewInteraction button8 = onView(
                allOf(withId(R.id.apply_filters), withText("Apply"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        4),
                                1),
                        isDisplayed()));
        button8.perform(click());

        Thread.sleep(1500);

        DataInteraction linearLayout7 = onData(anything())
                .inAdapterView(allOf(withId(R.id.curated_content_list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(1);
        linearLayout7.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.fav_checkbox_curated_content, 5000));
        Thread.sleep(1500);

        ViewInteraction checkBox2 = onView(
                allOf(withId(R.id.fav_checkbox_curated_content), withText("Add to favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        checkBox2.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction button9 = onView(
                allOf(withId(R.id.close_curated_content_single), withText("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        button9.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Preview image"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        Thread.sleep(1500);

        ViewInteraction linearLayout8 = onView(
                allOf(withId(R.id.url_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                2)));
        linearLayout8.perform(scrollTo(), click());

        Thread.sleep(1500);

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.open_favourites), withText("Browse favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.url_entry_view),
                                        2),
                                0),
                        isDisplayed()));
        textView2.perform(click());

        Thread.sleep(1500);

        ViewInteraction button10 = onView(
                allOf(withId(R.id.url_add_btn), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        3),
                                1),
                        isDisplayed()));
        button10.perform(click());

        Thread.sleep(1500);

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.url_input_field),
                        childAtPosition(
                                allOf(withId(R.id.url_entry_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                0)),
                                1),
                        isDisplayed()));
        editText2.perform(replaceText("google.com"), closeSoftKeyboard());

        Thread.sleep(1500);

        ViewInteraction button11 = onView(
                allOf(withId(R.id.confirm_btn), withText("Next"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                1)),
                                0),
                        isDisplayed()));
        button11.perform(click());

        Thread.sleep(1500);

        ViewInteraction button12 = onView(
                allOf(withId(R.id.push_btn), withText("Add to favourites?"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.playback_btns),
                                        1),
                                0),
                        isDisplayed()));
        button12.perform(click());

        Thread.sleep(1500);

        DataInteraction linearLayout9 = onData(anything())
                .inAdapterView(allOf(withId(R.id.yt_favourites),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(0);
        linearLayout9.perform(scrollTo(), longClick());

        Thread.sleep(1500);

        ViewInteraction button13 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button13.perform(click());

        Thread.sleep(1500);

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.clear_fav_btn), withText("Clear"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        textView3.perform(click());

        Thread.sleep(1500);

        ViewInteraction button14 = onView(
                allOf(withId(R.id.ok_btn), withText("Yes, delete them"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.custom),
                                        0),
                                3),
                        isDisplayed()));
        button14.perform(click());

        Thread.sleep(1500);

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        imageView2.perform(click());

    }


};








