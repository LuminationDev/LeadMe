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
        Thread.sleep(3000);
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

        onView(isRoot()).perform(waitId(R.id.login_email, 2000));

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

        onView(isRoot()).perform(waitId(R.id.login_password, 2000));

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

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.vr_core_btn, 2000));

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.vr_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                5)));
        linearLayout.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.cancel_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.select_video_source_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.video_back_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.select_photo_source_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.photo_back_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.cancel_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.vr_core_btn, 2000));

        ViewInteraction linearLayout2 = onView(
                allOf(withId(R.id.vr_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                5)));
        linearLayout2.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.select_video_source_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.video_back_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.cancel_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.vr_core_btn, 2000));

        ViewInteraction linearLayout3 = onView(
                allOf(withId(R.id.vr_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                5)));
        linearLayout3.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.cancel_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.file_core_btn, 2000));

        ViewInteraction linearLayout4 = onView(
                allOf(withId(R.id.file_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                6)));
        linearLayout4.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.end_core_btn, 2000));

        ViewInteraction linearLayout5 = onView(
                allOf(withId(R.id.end_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                7)));
        linearLayout5.perform(scrollTo(), click());
        Thread.sleep(3000);
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

        onView(isRoot()).perform(waitId(R.id.login_email, 2000));

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

        onView(isRoot()).perform(waitId(R.id.login_password, 2000));

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

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.app_core_btn, 2000));

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.app_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                0)));
        linearLayout.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.app_list_grid, 2000));

        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(0);
        linearLayout2.perform(scrollTo(), longClick());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.fav_list_grid, 2000));

        DataInteraction linearLayout3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.fav_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                3)))
                .atPosition(0);
        linearLayout3.perform(scrollTo(), longClick());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.app_list_grid, 2000));

        DataInteraction linearLayout4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(1);
        linearLayout4.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.push_btn, 2000));

        ViewInteraction button5 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.button_view),
                                        1),
                                1),
                        isDisplayed()));
        button5.perform(click());

        Thread.sleep(2000);
        onView(isRoot()).perform(waitId(R.id.curated_content_btn, 2000));

        ViewInteraction linearLayout5 = onView(
                allOf(withId(R.id.curated_content_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                1)));
        linearLayout5.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.curated_content_list, 2000));

        DataInteraction linearLayout6 = onData(anything())
                .inAdapterView(allOf(withId(R.id.curated_content_list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(1);
        linearLayout6.perform(click());

        onView(isRoot()).perform(waitId(R.id.fav_checkbox_curated_content, 2000));

        ViewInteraction checkBox = onView(
                allOf(withId(R.id.fav_checkbox_curated_content), withText("Add to favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        checkBox.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.close_curated_content_single, 2000));

        ViewInteraction button6 = onView(
                allOf(withId(R.id.close_curated_content_single), withText("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        button6.perform(click());

        onView(isRoot()).perform(waitId(R.id.filter_button, 2000));

        ViewInteraction button7 = onView(
                allOf(withId(R.id.filter_button), withText("Filter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        1),
                                0),
                        isDisplayed()));
        button7.perform(click());

        onView(isRoot()).perform(waitId(R.id.radio2, 2000));

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

        onView(isRoot()).perform(waitId(R.id.apply_filters, 2000));

        ViewInteraction button8 = onView(
                allOf(withId(R.id.apply_filters), withText("Apply"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        4),
                                1),
                        isDisplayed()));
        button8.perform(click());

        onView(isRoot()).perform(waitId(R.id.curated_content_list, 2000));

        DataInteraction linearLayout7 = onData(anything())
                .inAdapterView(allOf(withId(R.id.curated_content_list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(1);
        linearLayout7.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.fav_checkbox_curated_content, 5000));

        ViewInteraction checkBox2 = onView(
                allOf(withId(R.id.fav_checkbox_curated_content), withText("Add to favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        checkBox2.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.close_curated_content_single, 2000));

        ViewInteraction button9 = onView(
                allOf(withId(R.id.close_curated_content_single), withText("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        button9.perform(click());

        onView(isRoot()).perform(waitId(R.id.back_btn, 2000));

        ViewInteraction imageView = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Preview image"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        onView(isRoot()).perform(waitId(R.id.url_core_btn, 2000));

        ViewInteraction linearLayout8 = onView(
                allOf(withId(R.id.url_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                2)));
        linearLayout8.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.open_favourites, 2000));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.open_favourites), withText("Browse favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.url_entry_view),
                                        2),
                                0),
                        isDisplayed()));
        textView2.perform(click());

        onView(isRoot()).perform(waitId(R.id.url_add_btn, 2000));

        ViewInteraction button10 = onView(
                allOf(withId(R.id.url_add_btn), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        3),
                                1),
                        isDisplayed()));
        button10.perform(click());

        onView(isRoot()).perform(waitId(R.id.url_input_field, 2000));

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

        onView(isRoot()).perform(waitId(R.id.confirm_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.push_btn, 2000));

        ViewInteraction button12 = onView(
                allOf(withId(R.id.push_btn), withText("Add to favourites?"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.playback_btns),
                                        1),
                                0),
                        isDisplayed()));
        button12.perform(click());

        Thread.sleep(2000);
        onView(isRoot()).perform(waitId(R.id.yt_favourites, 2000));

        DataInteraction linearLayout9 = onData(anything())
                .inAdapterView(allOf(withId(R.id.yt_favourites),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(0);
        linearLayout9.perform(scrollTo(), longClick());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.clear_fav_btn, 2000));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.clear_fav_btn), withText("Clear"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        textView3.perform(click());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

        ViewInteraction button14 = onView(
                allOf(withId(R.id.ok_btn), withText("Yes, delete them"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.custom),
                                        0),
                                3),
                        isDisplayed()));
        button14.perform(click());

        onView(isRoot()).perform(waitId(R.id.back_btn, 2000));

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.back_btn), withContentDescription("Back"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        imageView2.perform(click());
        Thread.sleep(3000);
    }

    @Test
    public void complete00() throws InterruptedException {
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
        onView(isRoot()).perform(waitForTime(R.id.content, 5000));
        onView(isRoot()).perform(waitId(R.id.menu_btn, 2000));

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.viewswitcher),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());


        onView(isRoot()).perform(waitId(R.id.options_notsigned, 2000));

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

        onView(isRoot()).perform(waitId(R.id.tou_check, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button2 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_name, 2000));

        ViewInteraction editText = onView(
                allOf(withId(R.id.signup_name),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("Kabilan"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button3 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button3.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_email, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_password, 2000));

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.signup_password),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                4),
                        isDisplayed()));
        editText3.perform(replaceText("123456"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText4 = onView(
                allOf(withId(R.id.signup_confirmpass),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText4.perform(replaceText("321654"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_password, 2000));

        ViewInteraction editText5 = onView(
                allOf(withId(R.id.signup_password), withText("123456"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                4),
                        isDisplayed()));
        editText5.perform(replaceText("12345645"));

        onView(isRoot()).perform(waitId(R.id.signup_password, 2000));

        ViewInteraction editText6 = onView(
                allOf(withId(R.id.signup_password), withText("12345645"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                4),
                        isDisplayed()));
        editText6.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText7 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("321654"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText7.perform(replaceText("32165412"));

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText8 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("32165412"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText8.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button6 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button6.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_password, 2000));

        ViewInteraction editText9 = onView(
                allOf(withId(R.id.signup_password), withText("12345645"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                4),
                        isDisplayed()));
        editText9.perform(replaceText("12345678"));

        onView(isRoot()).perform(waitId(R.id.signup_page, 2000));

        ViewInteraction editText10 = onView(
                allOf(withId(R.id.signup_password), withText("12345678"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                4),
                        isDisplayed()));
        editText10.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_page, 2000));

        ViewInteraction editText11 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("32165412"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText11.perform(replaceText("12345678"));

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText12 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("12345678"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText12.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button7 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button7.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_back, 2000));

        ViewInteraction button8 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button8.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_back, 2000));

        ViewInteraction button9 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button9.perform(click());

        onView(isRoot()).perform(waitId(R.id.options_loginBtn, 5000));

        ViewInteraction button10 = onView(
                allOf(withId(R.id.options_loginBtn), withText("Login"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                10),
                        isDisplayed()));
        button10.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_email, 2000));

        ViewInteraction editText13 = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText13.perform(replaceText("kabivishal@gmail.com"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.login_password, 2000));

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

        ViewInteraction button11 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button11.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 5000));
        onView(isRoot()).perform(waitId(R.id.menu_btn, 5000));

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.menu_btn), withContentDescription("Lumination Logo"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView2.perform(click());

        onView(isRoot()).perform(waitId(R.id.options_endSess, 5000));

        ViewInteraction button12 = onView(
                allOf(withId(R.id.options_endSess), withText("End Session"),
                        childAtPosition(
                                allOf(withId(R.id.connected_only_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                0)),
                                2),
                        isDisplayed()));
        button12.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.app_login, 5000));

        ViewInteraction button13 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button13.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_pin_entry, 2000));

        ViewInteraction pinEntryEditText = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText.perform(replaceText("6788"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.connect_btn, 2000));

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

        onView(isRoot()).perform(waitId(R.id.close_login_alert_btn, 2000));

        ViewInteraction button15 = onView(
                allOf(withId(R.id.close_login_alert_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.wrong_code_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                0)),
                                5),
                        isDisplayed()));
        button15.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_pin_entry, 2000));

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

        onView(isRoot()).perform(waitId(R.id.login_pin_entry, 2000));

        ViewInteraction pinEntryEditText3 = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText3.perform(replaceText("6789"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.connect_btn, 2000));

        ViewInteraction button16 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button16.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.menu_btn, 2000));

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView3.perform(click());

        onView(isRoot()).perform(waitId(R.id.on_boarding, 2000));

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

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView4 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView4.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView5 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView5.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView6 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView6.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView7 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView7.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView8 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView8.perform(click());

        onView(isRoot()).perform(waitId(R.id.onboard_ok_btn, 2000));

        ViewInteraction button17 = onView(
                allOf(withId(R.id.onboard_ok_btn), withText("Finish"),
                        childAtPosition(
                                allOf(withId(R.id.onboard_buttons),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                7)),
                                0),
                        isDisplayed()));
        button17.perform(click());

        onView(isRoot()).perform(waitId(R.id.manual_connect, 2000));

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

        onView(isRoot()).perform(waitId(R.id.manual_back, 2000));

        ViewInteraction button18 = onView(
                allOf(withId(R.id.manual_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                2)),
                                1),
                        isDisplayed()));
        button18.perform(click());

        onView(isRoot()).perform(waitId(R.id.server_discovery, 2000));

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

        onView(isRoot()).perform(waitId(R.id.server_discovery, 2000));

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

        onView(isRoot()).perform(waitId(R.id.file_transfer, 2000));

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

        onView(isRoot()).perform(waitId(R.id.file_transfer, 2000));

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

        onView(isRoot()).perform(waitId(R.id.back_btn, 2000));

        ViewInteraction imageView9 = onView(
                allOf(withId(R.id.back_btn),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        imageView9.perform(click());

        onView(isRoot()).perform(waitId(R.id.app_core_btn, 2000));

        ViewInteraction linearLayout2 = onView(
                allOf(withId(R.id.app_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                0)));
        linearLayout2.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.app_list_grid, 2000));

        DataInteraction linearLayout3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(0);
        linearLayout3.perform(scrollTo(), longClick());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

        ViewInteraction button19 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button19.perform(click());
//
        onView(isRoot()).perform(waitId(R.id.fav_list_grid, 2000));

        DataInteraction linearLayout4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.fav_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                3)))
                .atPosition(0);
        linearLayout4.perform(scrollTo(), longClick());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

        ViewInteraction button20 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                5)),
                                0),
                        isDisplayed()));
        button20.perform(click());

        onView(isRoot()).perform(waitId(R.id.app_list_grid, 2000));

        DataInteraction linearLayout5 = onData(anything())
                .inAdapterView(allOf(withId(R.id.app_list_grid),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(1);
        linearLayout5.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.push_btn, 2000));

        ViewInteraction button21 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.button_view),
                                        1),
                                1),
                        isDisplayed()));
        button21.perform(click());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

        ViewInteraction button22 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.push_success_view),
                                                3)),
                                0),
                        isDisplayed()));
        button22.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.curated_content_btn, 5000));

        ViewInteraction linearLayout6 = onView(
                allOf(withId(R.id.curated_content_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                1)));
        linearLayout6.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.filter_button, 2000));

        ViewInteraction button23 = onView(
                allOf(withId(R.id.filter_button), withText("Filter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.curated_content_list_parent),
                                        1),
                                0),
                        isDisplayed()));
        button23.perform(click());

        onView(isRoot()).perform(waitId(R.id.radio2, 2000));

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

        onView(isRoot()).perform(waitId(R.id.apply_filters, 2000));

        ViewInteraction button24 = onView(
                allOf(withId(R.id.apply_filters), withText("Apply"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        4),
                                1),
                        isDisplayed()));
        button24.perform(click());

        onView(isRoot()).perform(waitId(R.id.curated_content_list, 5000));

        DataInteraction linearLayout7 = onData(anything())
                .inAdapterView(allOf(withId(R.id.curated_content_list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                0)))
                .atPosition(0);
        linearLayout7.perform(click());

        onView(isRoot()).perform(waitId(R.id.fav_checkbox_curated_content, 5000));

        ViewInteraction checkBox2 = onView(
                allOf(withId(R.id.fav_checkbox_curated_content), withText("Add to favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        checkBox2.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.select_item, 5000));

        ViewInteraction button25 = onView(
                allOf(withId(R.id.select_item), withText("Push"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        button25.perform(click());

        onView(isRoot()).perform(waitId(R.id.push_btn, 2000));

        ViewInteraction button26 = onView(
                allOf(withId(R.id.push_btn), withText("Push to everyone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.within_select_btns),
                                        2),
                                1),
                        isDisplayed()));
        button26.perform(click());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

        ViewInteraction button27 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.push_success_view),
                                                3)),
                                0),
                        isDisplayed()));
        button27.perform(click());

        onView(isRoot()).perform(waitId(R.id.video_back_btn, 2000));

        ViewInteraction button28 = onView(
                allOf(withId(R.id.video_back_btn), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.playback_btns),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                3)),
                                2),
                        isDisplayed()));
        button28.perform(click());

        onView(isRoot()).perform(waitId(R.id.url_core_btn, 5000));

        ViewInteraction linearLayout8 = onView(
                allOf(withId(R.id.url_core_btn),
                        childAtPosition(
                                allOf(withId(R.id.internal_wrapper),
                                        childAtPosition(
                                                withClassName(is("android.widget.HorizontalScrollView")),
                                                0)),
                                2)));
        linearLayout8.perform(scrollTo(), click());

        onView(isRoot()).perform(waitId(R.id.open_favourites, 2000));

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.open_favourites), withText("Browse favourites"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.url_entry_view),
                                        2),
                                0),
                        isDisplayed()));
        textView4.perform(click());

        onView(isRoot()).perform(waitId(R.id.url_add_btn, 2000));

        ViewInteraction button29 = onView(
                allOf(withId(R.id.url_add_btn), withText("Add"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        3),
                                1),
                        isDisplayed()));
        button29.perform(click());

        onView(isRoot()).perform(waitId(R.id.url_input_field, 2000));

        ViewInteraction editText14 = onView(
                allOf(withId(R.id.url_input_field),
                        childAtPosition(
                                allOf(withId(R.id.url_entry_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                0)),
                                1),
                        isDisplayed()));
        editText14.perform(replaceText("google.com"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.confirm_btn, 2000));

        ViewInteraction button30 = onView(
                allOf(withId(R.id.confirm_btn), withText("Next"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.url_task_view),
                                                1)),
                                0),
                        isDisplayed()));
        button30.perform(click());

        onView(isRoot()).perform(waitId(R.id.push_btn, 2000));

        ViewInteraction button31 = onView(
                allOf(withId(R.id.push_btn), withText("Add to favourites?"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.playback_btns),
                                        1),
                                0),
                        isDisplayed()));
        button31.perform(click());

        onView(isRoot()).perform(waitId(R.id.ok_btn, 2000));

        ViewInteraction button32 = onView(
                allOf(withId(R.id.ok_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.push_success_view),
                                                3)),
                                0),
                        isDisplayed()));
        button32.perform(click());

        Thread.sleep(5000);
    }

    @Test
    public void loginSignUP() throws InterruptedException {
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

        Thread.sleep(2000);
        onView(isRoot()).perform(waitId(R.id.menu_btn, 3500));

        ViewInteraction imageView = onView(
                allOf(withId(R.id.menu_btn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.viewswitcher),
                                        0),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        onView(isRoot()).perform(waitId(R.id.on_boarding, 2000));

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

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView2.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView3.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView4 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView4.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView5 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView5.perform(click());

        onView(isRoot()).perform(waitId(R.id.onboard_btn_4, 2000));

        ViewInteraction imageView6 = onView(
                allOf(withId(R.id.onboard_btn_4),
                        childAtPosition(
                                allOf(withId(R.id.onboard_pages),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                4)),
                                3),
                        isDisplayed()));
        imageView6.perform(click());

        onView(isRoot()).perform(waitId(R.id.next_button, 2000));

        ViewInteraction imageView7 = onView(
                allOf(withId(R.id.next_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                5),
                        isDisplayed()));
        imageView7.perform(click());

        onView(isRoot()).perform(waitId(R.id.skip_intro, 2000));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.skip_intro), withText("Skip Intro"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                        0),
                                6),
                        isDisplayed()));
        textView3.perform(click());

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.manual_connect), withText("Manual connect"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                5),
                        isDisplayed()));
        textView4.perform(click());

        onView(isRoot()).perform(waitId(R.id.manual_enterIP, 2000));

        ViewInteraction editText = onView(
                allOf(withId(R.id.manual_enterIP), withText("10.0.2."),
                        childAtPosition(
                                allOf(withId(R.id.manual_learner_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText.perform(replaceText("10.0.2.33"));

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.manual_enterIP), withText("10.0.2.33"),
                        childAtPosition(
                                allOf(withId(R.id.manual_learner_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText2.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.manual_name, 2000));

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.manual_name),
                        childAtPosition(
                                allOf(withId(R.id.manual_learner_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        editText3.perform(replaceText("stud"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.manual_back, 2000));

        ViewInteraction button = onView(
                allOf(withId(R.id.manual_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                2)),
                                1),
                        isDisplayed()));
        button.perform(click());

        onView(isRoot()).perform(waitId(R.id.server_discovery, 2000));

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

        onView(isRoot()).perform(waitId(R.id.server_discovery, 2000));

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

        onView(isRoot()).perform(waitId(R.id.options_notsigned, 2000));

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

        onView(isRoot()).perform(waitId(R.id.tou_check, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button2 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button2.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button3 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button3.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_name, 2000));

        ViewInteraction editText4 = onView(
                allOf(withId(R.id.signup_name),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                2),
                        isDisplayed()));
        editText4.perform(replaceText("kabilan"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_email, 2000));

        ViewInteraction editText5 = onView(
                allOf(withId(R.id.signup_email),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                3),
                        isDisplayed()));
        editText5.perform(replaceText("ksh1234"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_password, 2000));

        ViewInteraction editText6 = onView(
                allOf(withId(R.id.signup_password),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                4),
                        isDisplayed()));
        editText6.perform(replaceText("12345678"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText7 = onView(
                allOf(withId(R.id.signup_confirmpass),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText7.perform(replaceText("123456788"), closeSoftKeyboard());

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

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText8 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("123456788"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText8.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText9 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("123456788"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText9.perform(replaceText("12345678"));

        ViewInteraction editText10 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("12345678"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText10.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button6 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button6.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_email, 2000));

        ViewInteraction editText11 = onView(
                allOf(withId(R.id.signup_email), withText("ksh1234"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                3),
                        isDisplayed()));
        editText11.perform(replaceText("kabilan1033@gmail.com"));

        ViewInteraction editText12 = onView(
                allOf(withId(R.id.signup_email), withText("kabilan1033@gmail.com"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                3),
                        isDisplayed()));
        editText12.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.signup_marketing, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_marketing, 2000));

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

        onView(isRoot()).perform(waitId(R.id.signup_confirmpass, 2000));

        ViewInteraction editText13 = onView(
                allOf(withId(R.id.signup_confirmpass), withText("12345678"),
                        childAtPosition(
                                allOf(withId(R.id.signup_page),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                2)),
                                5),
                        isDisplayed()));
        editText13.perform(pressImeActionButton());

        onView(isRoot()).perform(waitId(R.id.signup_enter, 2000));

        ViewInteraction button7 = onView(
                allOf(withId(R.id.signup_enter), withText("Enter"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                0),
                        isDisplayed()));
        button7.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_back, 2000));

        ViewInteraction button8 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button8.perform(click());

        onView(isRoot()).perform(waitId(R.id.signup_back, 2000));

        ViewInteraction button9 = onView(
                allOf(withId(R.id.signup_back), withText("Back"),
                        childAtPosition(
                                allOf(withId(R.id.linearLayout),
                                        childAtPosition(
                                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                                8)),
                                1),
                        isDisplayed()));
        button9.perform(click());

        onView(isRoot()).perform(waitId(R.id.options_loginBtn, 2000));

        ViewInteraction button10 = onView(
                allOf(withId(R.id.options_loginBtn), withText("Login"),
                        childAtPosition(
                                allOf(withId(R.id.help_menu),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                10),
                        isDisplayed()));
        button10.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_email, 2000));

        ViewInteraction editText14 = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText14.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_email, 2000));

        ViewInteraction editText15 = onView(
                allOf(withId(R.id.login_email),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText15.perform(replaceText("kabivishal@gmail.com1"), closeSoftKeyboard());

        ViewInteraction showHidePasswordEditText = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText.perform(longClick());

        onView(isRoot()).perform(waitId(R.id.login_password, 2000));

        ViewInteraction showHidePasswordEditText2 = onView(
                allOf(withId(R.id.login_password),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText2.perform(replaceText("234567"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.login_enter, 2000));

        ViewInteraction button11 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button11.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_email, 2000));

        ViewInteraction editText16 = onView(
                allOf(withId(R.id.login_email), withText("kabivishal@gmail.com1"),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText16.perform(replaceText("kabivishal@gmail.com"));

        ViewInteraction editText17 = onView(
                allOf(withId(R.id.login_email), withText("kabivishal@gmail.com"),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        editText17.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.login_enter, 2000));

        ViewInteraction button12 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button12.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_password, 2000));

        ViewInteraction showHidePasswordEditText3 = onView(
                allOf(withId(R.id.login_password), withText("234567"),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText3.perform(replaceText("Tester123"));

        ViewInteraction showHidePasswordEditText4 = onView(
                allOf(withId(R.id.login_password), withText("Tester123"),
                        childAtPosition(
                                allOf(withId(R.id.login_signup_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        showHidePasswordEditText4.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.login_enter, 2000));

        ViewInteraction button13 = onView(
                allOf(withId(R.id.login_enter), withText("Enter"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.login_signup_view),
                                        7),
                                0),
                        isDisplayed()));
        button13.perform(click());

        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.menu_btn, 5000));

        ViewInteraction imageView8 = onView(
                allOf(withId(R.id.menu_btn), withContentDescription("Lumination Logo"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView8.perform(click());

        onView(isRoot()).perform(waitId(R.id.options_endSess, 2000));

        ViewInteraction button14 = onView(
                allOf(withId(R.id.options_endSess), withText("End Session"),
                        childAtPosition(
                                allOf(withId(R.id.connected_only_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                0)),
                                2),
                        isDisplayed()));
        button14.perform(click());


        onView(isRoot()).perform(waitForTime(R.id.content, 2000));
        onView(isRoot()).perform(waitId(R.id.app_login, 5000));

        ViewInteraction button15 = onView(
                allOf(withId(R.id.app_login), withText("Quick Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                3),
                        isDisplayed()));
        button15.perform(click());

        onView(isRoot()).perform(waitId(R.id.name_input_field, 2000));

        ViewInteraction editText18 = onView(
                allOf(withId(R.id.name_input_field), withText("Kabilan"),
                        childAtPosition(
                                allOf(withId(R.id.name_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                1)),
                                1),
                        isDisplayed()));
        editText18.perform(replaceText(""));

        onView(isRoot()).perform(waitId(R.id.name_input_field, 2000));

        ViewInteraction editText19 = onView(
                allOf(withId(R.id.name_input_field),
                        childAtPosition(
                                allOf(withId(R.id.name_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                1)),
                                1),
                        isDisplayed()));
        editText19.perform(closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.connect_btn, 2000));

        ViewInteraction button16 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button16.perform(click());

        onView(isRoot()).perform(waitId(R.id.close_login_alert_btn, 2000));

        ViewInteraction button17 = onView(
                allOf(withId(R.id.close_login_alert_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.wrong_code_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                0)),
                                5),
                        isDisplayed()));
        button17.perform(click());

        onView(isRoot()).perform(waitId(R.id.name_input_field, 2000));

        ViewInteraction editText20 = onView(
                allOf(withId(R.id.name_input_field),
                        childAtPosition(
                                allOf(withId(R.id.name_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                1)),
                                1),
                        isDisplayed()));
        editText20.perform(replaceText("kabilan"), closeSoftKeyboard());

        ViewInteraction pinEntryEditText = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText.perform(replaceText("6788"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.connect_btn, 2000));

        ViewInteraction button18 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button18.perform(click());

        onView(isRoot()).perform(waitId(R.id.close_login_alert_btn, 2000));

        ViewInteraction button19 = onView(
                allOf(withId(R.id.close_login_alert_btn), withText("OK"),
                        childAtPosition(
                                allOf(withId(R.id.wrong_code_view),
                                        childAtPosition(
                                                withClassName(is("android.widget.FrameLayout")),
                                                0)),
                                5),
                        isDisplayed()));
        button19.perform(click());

        onView(isRoot()).perform(waitId(R.id.login_pin_entry, 2000));

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

        onView(isRoot()).perform(waitId(R.id.login_pin_entry, 2000));

        ViewInteraction pinEntryEditText3 = onView(
                allOf(withId(R.id.login_pin_entry),
                        childAtPosition(
                                allOf(withId(R.id.code_entry_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                2)),
                                1),
                        isDisplayed()));
        pinEntryEditText3.perform(replaceText("6789"), closeSoftKeyboard());

        onView(isRoot()).perform(waitId(R.id.connect_btn, 2000));

        ViewInteraction button20 = onView(
                allOf(withId(R.id.connect_btn), withText("Connect"),
                        childAtPosition(
                                allOf(withId(R.id.button_view),
                                        childAtPosition(
                                                withId(R.id.name_code_entry_view),
                                                4)),
                                0),
                        isDisplayed()));
        button20.perform(click());

        onView(isRoot()).perform(waitId(R.id.menu_btn, 2000));

        ViewInteraction imageView9 = onView(
                allOf(withId(R.id.menu_btn), withContentDescription("Lumination Logo"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.c__leader_main),
                                        0),
                                2),
                        isDisplayed()));
        imageView9.perform(click());

        onView(isRoot()).perform(waitId(R.id.options_endSess, 2000));

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

};








