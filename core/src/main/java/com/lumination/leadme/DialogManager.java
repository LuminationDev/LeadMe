package com.lumination.leadme;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;

/**
 * A single class to manage and create dialog pop ups for information or errors.
 * */
public class DialogManager {
    private final String TAG = "DialogManager";
    private LeadMeMain main;

    private View confirmPushDialogView, loginDialogView;
    private AlertDialog warningDialog, waitingDialog, appPushDialog, confirmPushDialog, studentAlertsDialog, loginDialog;
    private TextView warningDialogTitle, warningDialogMessage;

    //login stuff - might be able to localise
    private TextView nameView;

    private boolean destroying = false;
    private boolean currentlySelectedOnly = false;

    /**
     * View displayed for pushing applications to peers.
     * */
    public View appPushDialogView;

    /**
     * View displayed for showing student alerts to a guide.
     * */
    public View studentAlertsView;

    /**
     * If a dialog is currently open.
     * */
    public boolean dialogShowing = false;

    /**
     *  Custom adapter for keeping track of peer alerts.
     * */
    public StudentAlertsAdapter alertsAdapter;

    /**
     *
     * */
    public Button readyBtn;

    /**
     * Basic constructor, setups up dialogs ready for use.
     * @param main LeadMeMain instance
     * */
    DialogManager(LeadMeMain main) {
        this.main = main;
        setupDialogs();
    }

    private void setupDialogs() {
        setupWaitingDialog();
        setupAppPushDialog();
        setupConfirmPushDialog();
        setupWarningDialog();
        setupAlertsViewDialog();
        setupLoginDialogView();
        setupLoginDialog();
    }

    //SETUP FUNCTIONS
    private void setupWaitingDialog() {
        View waitingDialogView = View.inflate(main, R.layout.e__waiting_to_connect, null);
        Button backBtn = waitingDialogView.findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> {
            waitingDialog.dismiss();
            dialogShowing = false;
            main.getNearbyManager().cancelConnection();
        });

        waitingDialog = new AlertDialog.Builder(main)
                .setView(waitingDialogView)
                .create();
        waitingDialog.setOnDismissListener(dialog -> hideSystemUI());
    }

    private void setupAppPushDialog() {
        appPushDialogView = View.inflate(main, R.layout.e__preview_app_push, null);

        appPushDialogView.findViewById(R.id.push_btn).setOnClickListener(v -> {
            main.getAppManager().launchApp(appPushPackageName, appPushTitle, false);
            Log.d(TAG, "LAUNCHING! " + appPushPackageName);
            hideAppPushDialogView();
            showConfirmPushDialog(true, false);
        });

        appPushDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> hideAppPushDialogView());
    }

    private void setupConfirmPushDialog() {
        confirmPushDialogView = View.inflate(main, R.layout.e__confirm_popup, null);
        confirmPushDialogView.findViewById(R.id.ok_btn).setOnClickListener(v -> hideConfirmPushDialog());
    }

    private void setupWarningDialog() {
        View warningDialogView = View.inflate(main, R.layout.e__warning_popup, null);
        warningDialogTitle = warningDialogView.findViewById(R.id.warning_title);
        warningDialogMessage = warningDialogView.findViewById(R.id.warning_comment);
        Button okBtn = warningDialogView.findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(v -> {
            warningDialog.dismiss();
            dialogShowing = false;
            warningDialogMessage.setVisibility(View.GONE);
        });

        warningDialog = new AlertDialog.Builder(main)
                .setView(warningDialogView)
                .create();
        warningDialog.setOnDismissListener(dialog -> hideSystemUI());
    }

    private void setupAlertsViewDialog() {
        studentAlertsView = View.inflate(main, R.layout.d__alerts_list, null);
        ListView studentAlerts = studentAlertsView.findViewById(R.id.current_alerts_list);

        View no_alerts_view = studentAlertsView.findViewById(R.id.no_alerts_message);
        View alerts_list = studentAlertsView.findViewById(R.id.current_alerts_list);

        alertsAdapter = new StudentAlertsAdapter(main, alerts_list, no_alerts_view);
        studentAlerts.setAdapter(alertsAdapter);

        studentAlertsView.findViewById(R.id.confirm_btn).setOnClickListener(v -> hideAlertsDialog());

        studentAlertsView.findViewById(R.id.clear_alerts_btn).setOnClickListener(v -> alertsAdapter.hideCurrentAlerts());
    }

    /**
     * Generic alert dialog creator, builds a new alert from the supplied view.
     * @param view The view to set the alert to.
     */
    public AlertDialog createAlert(View view) {
        AlertDialog newAlert = new AlertDialog.Builder(main)
                .setView(view)
                .create();

        return newAlert;
    }

    public void closeDialog(String type) {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
        }
    }

    public void showWaitingDialog() {
        if (waitingDialog == null) {
            setupWaitingDialog();
        }

        waitingDialog.show();
    }

    //Generic dialog creator
//    public void setupDialog(int layout, ViewGroup root) {
//        View newDialog = View.inflate(main, layout, root);
//        Button backBtn = newDialog.findViewById(R.id.back_btn);
//        backBtn.setOnClickListener(v -> {
//
//        });
//
//    }

    //DISPLAY FUNCTIONS
    TextView appPushMessageView;
    String appPushPackageName, appPushTitle;
    Button appPushBtn;



    public void showAppPushDialog(String title, Drawable icon, String packageName) {
        if(appPushDialogView == null) {
            setupAppPushDialog();
        }

        //TODO include display a message if errors occur
        appPushPackageName = packageName; //keep track of what should launch
        appPushTitle = title;

        //update appearance
        ((TextView) appPushDialogView.findViewById(R.id.push_app_title)).setText(title);
        ((ImageView) appPushDialogView.findViewById(R.id.push_app_icon)).setImageDrawable(icon);

        if (appPushMessageView == null) {
            appPushMessageView = appPushDialogView.findViewById(R.id.push_confirm_txt);
            appPushBtn = appPushDialogView.findViewById(R.id.push_btn);

            appPushDialogView.findViewById(R.id.everyone_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedOrEveryoneBtn(false);
                }
            });

            appPushDialogView.findViewById(R.id.selected_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedOrEveryoneBtn(true);
                }
            });
            setSelectedOrEveryoneBtn(true);
        }

        //display push
        if (appPushDialog == null) {
            appPushDialog = new AlertDialog.Builder(main)
                    .setView(appPushDialogView)
                    .show();
            appPushDialog.setOnDismissListener(dialog -> hideSystemUI());
        } else {
            appPushDialog.show();
        }
        dialogShowing = true;
    }

    private void hideAppPushDialogView() {
        if (appPushDialog != null) {
            dialogShowing = false;
            appPushDialog.dismiss();

        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setSelectedOrEveryoneBtn(boolean selected) {
        currentlySelectedOnly = selected;
        if (!selected) {
            appPushDialogView.findViewById(R.id.everyone_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left, null));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);

            appPushDialogView.findViewById(R.id.selected_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right_white, null));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            appPushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setElevation(Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 5, main.getResources().getDisplayMetrics())));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setElevation(0);


        } else {
            appPushDialogView.findViewById(R.id.everyone_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left_white, null));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

            appPushDialogView.findViewById(R.id.selected_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right, null));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);
            appPushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setElevation(Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 5, main.getResources().getDisplayMetrics())));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setElevation(0);
        }
    }

    /**
     * Show confirmation for an application being pushed to learners.
     * @param isApp A boolean determining if the push is for an app or website.
     * @param isSavedOnly A boolean for if something is going to be saved to favourites.
     */
    public void showConfirmPushDialog(boolean isApp, boolean isSavedOnly) {
        if(confirmPushDialogView == null) {
            setupConfirmPushDialog();
        }

        //TODO include display a message if errors occur

        if (confirmPushDialog == null) {
            confirmPushDialog = new AlertDialog.Builder(main)
                    .setView(confirmPushDialogView)
                    .show();
            confirmPushDialog.setOnDismissListener(dialog -> hideSystemUI());
        } else {
            confirmPushDialog.show();
        }
        dialogShowing = true;

        if (isSavedOnly) {
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_comment)).setText(R.string.fav_save_success);
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_title)).setText(R.string.save_success_title);
        } else if (isApp) {
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_comment)).setText(R.string.app_push_success);
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_title)).setText(R.string.push_success_title);
        } else { //isLink
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_comment)).setText(R.string.link_push_success);
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_title)).setText(R.string.push_success_title);
        }

        closeKeyboard();

        //auto close after 1.5 seconds
        if (isSavedOnly) {
            main.getHandler().postDelayed(() -> {
                hideConfirmPushDialog();
                main.getWebManager().launchUrlYtFavourites();
            }, 1500);
        } else {
            main.getHandler().postDelayed(this::hideConfirmPushDialog, 1500);
        }
    }

    public void hideConfirmPushDialog() {
        if (confirmPushDialog != null) {
            dialogShowing = false;
            confirmPushDialog.dismiss();
        }

        //return to main screen
        main.returnToMain();
    }

    /**
     * Display a custom pop up message about a warning.
     * @param message A string that is going to be displayed to the user.
     */
    public void showWarningDialog(String message) {
        if (destroying) {
            return;
        }
        if (warningDialog == null) {
            setupWarningDialog();
        }
        warningDialogTitle.setText(main.getResources().getString(R.string.oops_something_went_wrong));
        warningDialogMessage.setText(message);
        warningDialogMessage.setVisibility(View.VISIBLE);
        warningDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }

    /**
     * Display a custom pop up message about a warning.
     * @param title A string that describes what the warning is about.
     * @param message A string that is going to be displayed to the user.
     */
    public void showWarningDialog(String title, String message) {
        if (destroying) {
            return;
        }
        if (warningDialog == null) {
            setupWarningDialog();
        }
        warningDialogTitle.setText(title);
        warningDialogMessage.setText(message);
        warningDialogMessage.setVisibility(View.VISIBLE);
        warningDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }


    public void showAlertsDialog() {
        if (destroying) {
            return;
        }

        if (studentAlertsDialog == null) {
            studentAlertsDialog = new AlertDialog.Builder(main)
                    .setView(studentAlertsView)
                    .create();
            studentAlertsDialog.setOnDismissListener(dialog -> hideSystemUI());
        }

        hideSystemUI();
        dialogShowing = true;
        main.getConnectedLearnersAdapter().refreshAlertsView();
        studentAlertsDialog.show();
    }

    private void hideAlertsDialog() {
        closeKeyboard();
        hideSystemUI();
        if (studentAlertsDialog != null) {
            dialogShowing = false;
            studentAlertsDialog.dismiss();
        }
    }

    /**
     * Close the waiting dialog and login dialog if they are showing.
     * @param success A boolean representing if the action was successful.
     */
    public void closeWaitingDialog(boolean success) {
        closeDialog("waiting");

        Log.d(TAG, "Closing waiting dialog! " + success + ", " + waitingDialog + ", " + loginDialog);

        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
        }

        if (loginDialog != null && loginDialog.isShowing()) {
            loginDialog.dismiss();
        }

        dialogShowing = (waitingDialog != null && waitingDialog.isShowing()) || (loginDialog != null && loginDialog.isShowing());
        Log.d(TAG, "Are they showing now?? " + (waitingDialog != null && waitingDialog.isShowing()) + " || " + (loginDialog != null && loginDialog.isShowing()));
    }

    private void setupLoginDialogView() {
        getNameView(); //sets up the loginDialog

        loginDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> hideLoginDialog(true));
        loginDialogView.findViewById(R.id.connect_btn).setOnClickListener(v -> main.initiateLeaderAdvertising());

        readyBtn = loginDialogView.findViewById(R.id.connect_btn);
        loginDialogView.findViewById(R.id.close_login_alert_btn).setOnClickListener(v -> {
            if (nameView.getText().toString().trim().length() == 0) {
                nameView.requestFocus();
            } else {
                loginDialogView.findViewById(R.id.login_pin_entry).requestFocus();
            }
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
            openKeyboard();
            hideSystemUI();
        });

        if (readyBtn != null) {
            readyBtn.setOnClickListener(v -> {
                main.tryLogin();
            });
        }

        TextView forgotPin = loginDialogView.findViewById(R.id.login_forgot_pin);
        forgotPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.setAndDisplayPinReset(0);
            }
        });
    }

    private void setupLoginDialog() {
        if (loginDialog == null) {
            loginDialog = new AlertDialog.Builder(main)
                    .setView(loginDialogView)
                    .create();
        }

        loginDialog.setOnDismissListener(dialog -> hideSystemUI());
        loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText email = loginDialogView.findViewById(R.id.login_email);
        EditText password = loginDialogView.findViewById(R.id.login_password);
        TextView forgotPassword = loginDialogView.findViewById(R.id.login_forgotten);
        TextView errorText = loginDialogView.findViewById(R.id.error_text);
        LinearLayout googleSignin = loginDialogView.findViewById(R.id.login_google);
        Button enterBtn = loginDialogView.findViewById(R.id.login_enter);
        Button backBtn = loginDialogView.findViewById(R.id.login_back);
        TextView signup = loginDialogView.findViewById(R.id.login_signup);

        enterBtn.setOnClickListener(v -> {
            if(password.getText().toString().length()>0 && email.getText().toString().length()>0) {
                main.firebaseEmailSignIn(email.getText().toString(), password.getText().toString(), errorText);
            } else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Please check you have entered your details correctly.");
            }
        });

        password.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                main.getInputManager().showSoftInput(v, InputMethodManager.SHOW_FORCED);
            }
        });

        forgotPassword.setOnClickListener(v -> {
            main.forgotPasswordController(loginDialog);
        });

        googleSignin.setOnClickListener(v -> {
            main.googleSignIn();
        });

        signup.setOnClickListener(v -> {
            loginDialog.dismiss();
            main.buildLoginSignupController(0);
        });

        backBtn.setOnClickListener(v -> loginDialog.dismiss());
    }

    public void hideLoginDialog(boolean cancelled) {
        Log.d(TAG, "Hiding dialog box");
        closeKeyboard();
        hideSystemUI();

        if (loginDialog != null) {
            dialogShowing = false;
            loginDialog.dismiss();
            if (cancelled) {
                main.startShakeDetection();
            }
        }
    }

    /**
     * Get the current instance of the loginDialog.
     * @return An AlertDialog instance of the loginDialog.
     */
    public AlertDialog getLoginDialog() {
        return this.loginDialog;
    }

    /**
     * Get the Indeterminate Bar instance associated with this class.
     * @return A ProgressBar instance.
     */
    public ProgressBar getIndeterminateBar() {
        return loginDialogView.findViewById(R.id.indeterminateBar);
    }

    /**
     * Query the text entry and retrieve the string.
     * @return A String representing the what has been entered by a user.
     */
    public String getPinEntry() {
        final PinEntryEditText pinEntry = loginDialogView.findViewById(R.id.login_pin_entry);
        String entry = pinEntry.getText().toString();
        pinEntry.setText("");

        return entry;
    }

    /**
     * Get the name of the current user.
     * */
    public TextView getNameView() {
        if (loginDialogView == null || nameView == null) {
            loginDialogView = View.inflate(main, R.layout.b__login_popup, null);
            nameView = loginDialogView.findViewById(R.id.name_input_field);

            //TODO temporary code to allow me to skip login while testing
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.GONE);
        }
        return nameView;
    }

    /**
     * Change the visibility of the login view options. Use -1 if no change is required for a display
     * @param loginDisplay An int representing if the login sign view should be visibility.
     * @param wrongDisplay An int representing if the wrong code view should be visibility.
     * @param nameDisplay An int representing if the name view should be visibility.
     * */
    public void changeLoginViewOptions(int loginDisplay, int wrongDisplay, int nameDisplay) {
        if(loginDisplay != -1) {
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(loginDisplay);
        }
        if(wrongDisplay != -1) {
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(wrongDisplay);
        }
        if(nameDisplay != -1) {
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(nameDisplay);
        }
    }

    /**
     * Change the visibility of the login view options for a teacher.
     * @param codeDisplay An int representing if the code input view should be visibility.
     * @param studentTeacherDisplay An int representing if the teacher name list view should be visibility.
     */
    public void changeTeacherLoginViewOptions(int codeDisplay, int studentTeacherDisplay) {
        loginDialogView.findViewById(R.id.code_entry_view).setVisibility(codeDisplay);
        loginDialogView.findViewById(R.id.student_teacher_view).setVisibility(studentTeacherDisplay);
    }

    /**
     * Set the name of the current guide to be displayed.
     * @param guideName A String representing the current guide.
     */
    public void setTeacherName(String guideName) {
        ((TextView) loginDialogView.findViewById(R.id.teacher_name)).setText(guideName);
    }

    /**
     * Control the visibility of the indeterminate Bar.
     * @param display An int representing the visibility of the bar.
     * */
    public void setIndeterminateBar(int display) {
        loginDialogView.findViewById(R.id.indeterminateBar).setVisibility(display);
    }

    //HELPER FUNCTIONS
    /**
     * Calls the hideSystemUI from the LeadMe main activity.
     * */
    private void hideSystemUI() {
        main.hideSystemUI();
    }

    /**
     * Calls the openKeyboard from the LeadMe main activity.
     * */
    private void openKeyboard() {
        main.openKeyboard();
    }

    /**
     * Calls the closeKeyboard from the LeadMe main activity
     * */
    private void closeKeyboard() {
        main.closeKeyboard();
    }
}
