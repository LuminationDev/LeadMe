package com.lumination.leadme.managers;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.StudentAlertsAdapter;
import com.lumination.leadme.utilities.FileUtilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A single class to manage and create dialog pop ups for information or errors.
 * */
public class DialogManager {
    private final String TAG = "DialogManager";
    private final LeadMeMain main;
    private final Resources resources;

    private View confirmPushDialogView, loginDialogView, toggleBtnView, manView, permissionDialogView, requestDialogView;
    private AlertDialog warningDialog, waitingDialog, appPushDialog, confirmPushDialog, studentAlertsDialog, loginDialog, recallPrompt, manualDialog, permissionDialog, requestDialog, fileTypeDialog, firstTimeVRDialog;
    private TextView appPushMessageView, warningDialogTitle, warningDialogMessage, recallMessage, permissionDialogMessage, requestDialogMessage, additionalInfo;
    private String appPushPackageName, appPushTitle;
    private ArrayList<String> permissions = null;

    private TextView nameView;

    private boolean destroying = false;

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
     * On click attempts to connect a peer to a guide.
     * */
    public Button readyBtn;

    /**
     * Basic constructor, setups up dialogs ready for use.
     * @param main LeadMeMain instance
     * */
    public DialogManager(LeadMeMain main) {
        this.main = main;
        this.resources = main.getResources();
        setupDialogs();
    }

    private void setupDialogs() {
        setupWaitingDialog();
        setupPermissionDialog();
        setupAppPushDialog();
        setupConfirmPushDialog();
        setupWarningDialog();
        setupAlertsViewDialog();
        setupLoginDialogView();
        setupLoginDialog();
        setupManualDialog();
        setupRecallDialog();
        setupVRFirstTime();
        setupFileTypes();
        setupRequestDialog();
    }

    //SETUP FUNCTIONS
    private void setupWaitingDialog() {
        View waitingDialogView = View.inflate(main, R.layout.e__waiting_to_connect, null);
        Button backBtn = waitingDialogView.findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> {
            dialogShowing = false;
            main.getNearbyManager().cancelConnection();
            waitingDialog.dismiss();
        });

        waitingDialog = new AlertDialog.Builder(main)
                .setView(waitingDialogView)
                .create();
        waitingDialog.setOnDismissListener(dialog -> hideSystemUI());
    }

    private void setupAppPushDialog() {
        appPushDialogView = View.inflate(main, R.layout.e__preview_app_push, null);

        appPushDialogView.findViewById(R.id.push_btn).setOnClickListener(v -> {
            main.getAppManager().launchApp(appPushPackageName, appPushTitle, false, "false", false, main.getNearbyManager().getSelectedPeerIDsOrAll());
            Log.d(TAG, "LAUNCHING! " + appPushPackageName);
            hideAppPushDialogView();
            showConfirmPushDialog(true, false);
        });

        setupPushToggle(appPushDialogView, false);

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

//    /**
//     * Generic alert dialog creator, builds a new alert from the supplied view.
//     * @param view The view to set the alert to.
//     */
//    public AlertDialog createAlert(View view) {
//        AlertDialog newAlert = new AlertDialog.Builder(main)
//                .setView(view)
//                .create();
//
//        return newAlert;
//    }

//    //Generic dialog creator
//    public void setupDialog(int layout, ViewGroup root) {
//        View newDialog = View.inflate(main, layout, root);
//        Button backBtn = newDialog.findViewById(R.id.back_btn);
//        backBtn.setOnClickListener(v -> {
//
//        });
//
//    }

    //Dialog to show information about the VR player on the first time usage
    private void setupVRFirstTime() {
        View firstTimeVRDialogView = View.inflate(main, R.layout.e__leader_vr_popup, null);
        firstTimeVRDialog = new AlertDialog.Builder(main)
                .setView(firstTimeVRDialogView)
                .create();

        firstTimeVRDialog.setOnDismissListener(dialog -> {
            dialogShowing = false;
            hideSystemUI();
        });

        Button allowBtn = firstTimeVRDialogView.findViewById(R.id.select_source_btn);
        Button cancelBtn = firstTimeVRDialogView.findViewById(R.id.cancel_btn);

        allowBtn.setOnClickListener(v -> {
            firstTimeVRDialog.dismiss();

            showFileTypeDialog();
        });

        cancelBtn.setOnClickListener(v -> {
            firstTimeVRDialog.dismiss();
        });
    }

    //Dialog to describe what sort of files can be chosen
    private void setupFileTypes() {
        View fileTypeDialogView = View.inflate(main, R.layout.e__leader_filetype_popup, null);
        fileTypeDialog = new AlertDialog.Builder(main)
                .setView(fileTypeDialogView)
                .create();

        fileTypeDialog.setOnDismissListener(dialog -> {
            dialogShowing = false;
            hideSystemUI();
        });

        Button allowBtn = fileTypeDialogView.findViewById(R.id.ok_btn);

        allowBtn.setOnClickListener(v -> {
            fileTypeDialog.dismiss();

            if(LeadMeMain.isMiUiV9()) {
                main.alternateFileChoice(LeadMeMain.VR_FILE_CHOICE);
            } else {
                FileUtilities.browseFiles(main, LeadMeMain.VR_FILE_CHOICE);
            }
        });
    }

    //Request dialog for file requests from learner devices
    private void setupRequestDialog() {
        requestDialogView = View.inflate(main, R.layout.e__learner_request_popup, null);
        requestDialogMessage = requestDialogView.findViewById(R.id.request_comment);
        additionalInfo = requestDialogView.findViewById(R.id.request_additional_info);
        requestDialog = new AlertDialog.Builder(main)
                .setView(requestDialogView)
                .create();

        requestDialog.setOnDismissListener(dialog -> {
            dialogShowing = false;
            additionalInfo.setVisibility(View.GONE);
            hideSystemUI();
        });

        Button allowBtn = requestDialogView.findViewById(R.id.allow_btn);
        Button blockBtn = requestDialogView.findViewById(R.id.block_btn);

        allowBtn.setOnClickListener(v -> {
            if(LeadMeMain.isMiUiV9()) {
                main.getFileTransferManager().startFileServer(main.vrVideoPath, true);
            } else {
                main.getFileTransferManager().startFileServer(main.vrVideoURI, true);
            }

            Set<String> peerSet = new HashSet<>();
            for(int ID : LeadMeMain.fileRequests) {
                peerSet.add(String.valueOf(ID));
            }

            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.RETURN_TAG, peerSet);
            requestDialog.dismiss();
        });

        blockBtn.setOnClickListener(v -> {
            LeadMeMain.fileRequests = new ArrayList<>();
            requestDialog.dismiss();
        });
    }

    //Permission dialog for file transfer and auto installing applications
    private void setupPermissionDialog() {
        permissionDialogView = View.inflate(main, R.layout.e__peer_permission_popup, null);
        permissionDialogMessage = permissionDialogView.findViewById(R.id.permission_comment);
        permissionDialog = new AlertDialog.Builder(main)
                .setView(permissionDialogView)
                .create();

        permissionDialog.setCancelable(false);

        permissionDialog.setOnDismissListener(dialog -> {
            dialogShowing = false;
            permissionDialogMessage.setVisibility(View.GONE);
            hideSystemUI();
        });
    }


    /**
     * Displays an alert box, notifying the guide that there are files missing on a learner devices.
     * Will wait x seconds before showing the allow button as to check if any other devices report
     * the same issue.
     * @param delay An int representing the time delay before making the allow button clickable.
     */
    @SuppressLint("SetTextI18n")
    public void showRequestDialog(int delay) {
        additionalInfo.setText("Waiting for any other file requests.");
        additionalInfo.setVisibility(View.VISIBLE);

        if(requestDialog.isShowing()) {
            //in case a guide switched on auto installer and transfer quickly
            requestDialogMessage.setText(LeadMeMain.fileRequests.size() + " learners do not have the video. " +
                    "\nDo you want to transfer it?"); //update the text if there are more requests
        } else {
            requestDialogMessage.setText(LeadMeMain.fileRequests.size() + " learner does not have the video. " +
                    "\nDo you want to transfer it?");

            waitForOthers(requestDialogView.findViewById(R.id.allow_btn), delay);
        }

        requestDialog.show();
    }

    /**
     * Makes a button wait x seconds until clickable - useful when waiting for responses
     */
    private void waitForOthers(Button button, int seconds) {
        button.setEnabled(false);
        String originalText = (String) button.getText();

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        final Runnable runnable = new Runnable() {
            int countdownStarter = seconds;

            public void run() {

                button.setText("Wait (" + countdownStarter + ")");
                countdownStarter--;

                if (countdownStarter < 0) {
                    additionalInfo.setVisibility(View.GONE);
                    button.setText(originalText);
                    button.setEnabled(true);
                    scheduler.shutdown();
                }
            }
        };
        scheduler.scheduleAtFixedRate(runnable, 0, 1, SECONDS);
    }

    /**
     * Show a dialog that is directed at getting a permission enabled by a learner.
     * @param msg A description of the permission that needs to be enabled.
     * @param permission The permission that needs enabling.
     */
    public void showPermissionDialog(String msg, String permission) {
        Button allowBtn = permissionDialogView.findViewById(R.id.allow_btn);
        Button blockBtn = permissionDialogView.findViewById(R.id.block_btn);

        if(permissions == null) {
            permissions = new ArrayList<>();
        }

        if(permissionDialog.isShowing()) {
            //in case a guide switched on auto installer and transfer quickly
            permissionDialogMessage.setText(permissionDialogMessage.getText() + "\n\n" + msg); //update the text
        } else {
            permissionDialogMessage.setText(msg);
        }

        permissions.add(permission);
        permissionDialogMessage.setVisibility(View.VISIBLE);

        //Add permissions if the messages stack
        allowBtn.setOnClickListener(view -> {
            for ( String pm : permissions) {
                main.permissionAllowed(pm, true);
            }
            permissionDialog.dismiss();
            permissions = null;
        });

        blockBtn.setOnClickListener(view -> {
            for ( String pm : permissions) {
                main.permissionAllowed(pm, false);
            }
            permissions = null;
            permissionDialog.dismiss();
        });

        permissionDialog.show();
    }

    /**
     * Show a dialog alert that describes what file types can be selected for a certain application.
     */
    public void showFileTypeDialog() {
        if (destroying) {
            return;
        }
        if (fileTypeDialog == null) {
            setupFileTypes();
        }

        fileTypeDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }

    /**
     * Show a dialog alert that describes the first time use for the VR player
     */
    public void showVRFirstTimeDialog() {
        if(firstTimeVRDialog == null) {
            setupVRFirstTime();
        }

        firstTimeVRDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }

    //Turn into generic functions
    public void closeDialog(String type) {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            dialogShowing = false;
            waitingDialog.dismiss();
        }
    }

    public void showWaitingDialog() {
        if (waitingDialog == null) {
            setupWaitingDialog();
        }

        waitingDialog.show();
        dialogShowing = true;
    }

    /**
     * Display an AlertDialog for first time users with a link to the online manual.
     */
    public void displayGuidePrompt() {
        View firstDialog = View.inflate(main, R.layout.a__first_time, null);

        AlertDialog alert = new AlertDialog.Builder(main)
                .setView(firstDialog)
                .show();

        firstDialog.findViewById(R.id.open_guide).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
            main.startActivity(browserIntent);
            //todo link to guide
        });
        firstDialog.findViewById(R.id.skip_guide).setOnClickListener(v -> alert.dismiss());
    }

    /**
     * An AlertDialog that displays what application is about to be pushed and if the guide wishes to
     * continue, including it's name and icon.
     * @param title A String representing the title of the application.
     * @param icon A Drawable representing the icon of the applicaiton.
     * @param packageName A String representing the packageName of the application.
     */
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
            Button appPushBtn = appPushDialogView.findViewById(R.id.push_btn);
        }

        toggleSelectedView(appPushDialogView);

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
        warningDialogTitle.setText(resources.getString(R.string.oops_something_went_wrong));
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
     * Setup the recall function for returning students to the LeadMe home screen.
     */
    public void setupRecallDialog() {
        View recallView = View.inflate(main, R.layout.e__recall_confirm_popup, null);
        recallMessage = recallView.findViewById(R.id.recall_comment);

        toggleBtnView = recallView.findViewById(R.id.toggleBtnView);

        recallView.findViewById(R.id.ok_btn).setOnClickListener(v -> {
            main.returnToAppFromMainAction(main.getSelectedOnly());
            dialogShowing = false;
            recallPrompt.dismiss();
        });

        recallView.findViewById(R.id.back_btn).setOnClickListener(v -> {
            dialogShowing = false;
            recallPrompt.dismiss();
        });

        setupPushToggle(recallView, true);

        recallPrompt = new AlertDialog.Builder(main)
                .setView(recallView)
                .create();
        recallPrompt.setOnDismissListener(dialog -> hideSystemUI());
    }

    /**
     * Display an AlertDialog for confirming a recall of peers back to LeadMe.
     */
    public void showRecallDialog() {
        dialogShowing = true;
        Log.w(TAG, "Showing recall dialog");

        if (recallPrompt == null) {
            setupRecallDialog();
        }

        if (main.getConnectedLearnersAdapter().someoneIsSelected() && (main.getNearbyManager().getSelectedPeerIDs().size() < main.getNearbyManager().getAllPeerIDs().size())) {
            recallMessage.setText(resources.getString(R.string.recall_comment_selected));
            toggleBtnView.setVisibility(View.VISIBLE);
        } else {
            recallMessage.setText(resources.getString(R.string.recall_comment_all));
            toggleBtnView.setVisibility(View.GONE);
        }

        recallPrompt.show();
        dialogShowing = true;
    }

    /**
     * Control the visibility of the selected or everyone button in reference to pushing actions to
     * learners.
     * @param preview A view representing the action layout.
     * @param recall A boolean representing if the related dialog is the recall function.
     */
    public void setupPushToggle(View preview, boolean recall) {
        Button selectedBtn = preview.findViewById(R.id.selected_btn);
        Button everyoneBtn = preview.findViewById(R.id.everyone_btn);
        Button pushBtn = recall ? null : preview.findViewById(R.id.push_btn);

        selectedBtn.setOnClickListener(v -> {
            main.setSelectedOnly(true);
            makeSelectedBtnActive(selectedBtn, everyoneBtn);
            if (pushBtn != null) {
                pushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));
            }
        });

        everyoneBtn.setOnClickListener(v -> {
            main.setSelectedOnly(false);
            makeEveryoneBtnActive(selectedBtn, everyoneBtn);
            if (pushBtn != null) {
                pushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone));
            }
        });

        everyoneBtn.callOnClick(); //Makes everyone the default each time a dialog is loaded.
    }

    private void makeSelectedBtnActive(Button selectedBtn, Button everyoneBtn) {
        selectedBtn.setBackground(ResourcesCompat.getDrawable(resources, R.drawable.bg_active_right, null));
        everyoneBtn.setBackground(ResourcesCompat.getDrawable(resources, R.drawable.bg_passive_left, null));
        selectedBtn.setTextColor(resources.getColor(R.color.leadme_light_grey, null));
        everyoneBtn.setTextColor(resources.getColor(R.color.light, null));
    }

    private void makeEveryoneBtnActive(Button selectedBtn, Button everyoneBtn) {
        selectedBtn.setBackground(ResourcesCompat.getDrawable(resources, R.drawable.bg_passive_right, null));
        everyoneBtn.setBackground(ResourcesCompat.getDrawable(resources, R.drawable.bg_active_left, null));
        everyoneBtn.setTextColor(resources.getColor(R.color.leadme_light_grey, null));
        selectedBtn.setTextColor(resources.getColor(R.color.light, null));
    }

    /**
     * If a user is selected display the selection buttons to the guide so they can choose whether
     * to launch the experience for just the selected or for everyone. Does not appear if there is
     * only one learner.
     * @param preview A view representing the action layout.
     */
    public void toggleSelectedView(View preview) {
        View toggleSelectedBtn = preview.findViewById(R.id.toggleBtnView);
        toggleSelectedBtn.setVisibility(main.getConnectedLearnersAdapter().someoneIsSelected()
                && (main.getNearbyManager().getSelectedPeerIDs().size() < main.getNearbyManager().getAllPeerIDs().size())
                ? View.VISIBLE : View.GONE);
    }

    /**
     * Displays an AlertDialog whilst a peer is connecting to a guide.
     */
    public void showWaitingForConnectDialog() {
        this.loginDialog.dismiss();
        showWaitingDialog();
        dialogShowing = true;
    }

    /**
     *
     */
    public void showLoginAlertMessage() {
        setIndeterminateBar(View.GONE);
        changeLoginViewOptions(-1, View.GONE, -1);
        closeKeyboard();
        hideSystemUI();
    }

    /**
     *
     */
    private void setupManualDialog() {
        manView = View.inflate(main, R.layout.e__manual_popup, null);
        manualDialog = new AlertDialog.Builder(main)
                .setView(manView)
                .create();

        Button back = manView.findViewById(R.id.manual_back);
        back.setOnClickListener(v1 -> manualDialog.dismiss());
    }

    /**
     * Displays the AlertDialog to connect to a Guide by manually entering the ipAddress to connect to.
     * Sets up the display depending on if the user is a peer or a guide. A guide is shown their ipAddress
     * for a peer to see and copy and the peer sees inputs for their name and the guides ipAddress.
     * @param isGuide A boolean determining if the user is a guide.
     * @param ipAddress A String representing the Guide's ipAddress.
     */
    public void showManualDialog(boolean isGuide, String ipAddress) {
        if(isGuide) {
            manView.findViewById(R.id.manual_leader_view).setVisibility(View.VISIBLE);
            manView.findViewById(R.id.manual_learner_view).setVisibility(View.GONE);
            manView.findViewById(R.id.manual_ok).setVisibility(View.GONE);
            TextView IpAddress = manView.findViewById(R.id.manual_ip);
            IpAddress.setText(ipAddress);
        } else {
            if(main.getNearbyManager().isConnectedAsFollower()){
                manualDialog.dismiss();
                Toast.makeText(main, "You are already connected to a leader", Toast.LENGTH_SHORT).show();
                return;
            }
            manView.findViewById(R.id.manual_learner_view).setVisibility(View.VISIBLE);
            manView.findViewById(R.id.manual_ok).setVisibility(View.VISIBLE);
            manView.findViewById(R.id.manual_leader_view).setVisibility(View.GONE);
            EditText IpEnter = manView.findViewById(R.id.manual_enterIP);
            EditText ManName = manView.findViewById(R.id.manual_name);
            Button connect = manView.findViewById(R.id.manual_ok);
            IpEnter.setText(ipAddress.substring(0, ipAddress .lastIndexOf(".")+1)   );
            IpEnter.setSelection(IpEnter.getText().length());
            //add to the leaders list

            connect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(IpEnter!=null && ManName!=null &&ManName.getText().toString().length()>0 && IpEnter.getText().toString().length()>0) {
                        Log.d(TAG, "onClick: "+IpEnter.getText().toString());
                        nameView.setText(ManName.getText().toString());

                        manualDialog.dismiss();
                        LeadMeMain.isGuide = false;
                        main.directIpConnection(ManName, IpEnter);
                    }
                }
            });
        }

        manualDialog.show();
    }

    /**
     * Close the waiting dialog and login dialog if they are showing.
     * @param success A boolean representing if the action was successful.
     */
    public void closeWaitingDialog(boolean success) {
        closeDialog("waiting");

        Log.d(TAG, "Closing waiting dialog! " + success + ", " + waitingDialog + ", " + loginDialog);

        if (waitingDialog != null && waitingDialog.isShowing()) {
            dialogShowing = false;
            waitingDialog.dismiss();
        }

        if (loginDialog != null && loginDialog.isShowing()) {
            dialogShowing = false;
            loginDialog.dismiss();
        }

        dialogShowing = (waitingDialog != null && waitingDialog.isShowing()) || (loginDialog != null && loginDialog.isShowing());
        Log.d(TAG, "Are they showing now?? " + (waitingDialog != null && waitingDialog.isShowing()) + " || " + (loginDialog != null && loginDialog.isShowing()));


    }

    private void setupLoginDialogView() {
        getNameView(); //sets up the loginDialog

        loginDialogView.findViewById(R.id.back_btn).setOnClickListener(view -> {
            hideLoginDialog(true);

            //clear the pin entry
            getAndClearPinEntry();
        });

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
            readyBtn.setOnClickListener(v -> main.tryLogin());
        }

        TextView forgotPin = loginDialogView.findViewById(R.id.login_forgot_pin);
        forgotPin.setOnClickListener(v -> main.setAndDisplayPinReset(0));
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

        enterBtn.setOnClickListener(view -> {
            if(password.getText().toString().length()>0 && email.getText().toString().length()>0) {
                main.firebaseEmailSignIn(email.getText().toString(), password.getText().toString(), errorText);
            } else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Please check you have entered your details correctly.");
            }
        });

        password.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                main.getInputManager().showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        });

        forgotPassword.setOnClickListener(view -> main.forgotPasswordController(loginDialog));

        googleSignin.setOnClickListener(view -> main.googleSignIn());

        signup.setOnClickListener(view -> {
            loginDialog.dismiss();
            main.buildLoginSignupController(0);
        });

        backBtn.setOnClickListener(v -> loginDialog.dismiss());
    }

    /**
     * Hide the login dialog if a user has cancelled the process halfway through.
     * @param cancelled A boolean representing if the login has been cancelled.
     */
    public void hideLoginDialog(boolean cancelled) {
        Log.d(TAG, "Hiding dialog box");
        closeKeyboard();
        hideSystemUI();

        if (loginDialog != null) {
            dialogShowing = false;
            loginDialog.dismiss();
            if (cancelled) {
                main.startShakeDetection();

                //Only start discovery again if trying to login as a learner
                if(main.loginActor.equals("learner")) {
                    main.getNearbyManager().nsdManager.startDiscovery();
                }
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
    public String getAndClearPinEntry() {
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
     * Change the visibility of the login view options for a leader.
     * @param codeDisplay An int representing if the code input view should be visibility.
     * @param learnerLeaderDisplay An int representing if the leader name list view should be visibility.
     */
    public void changeLeaderLoginViewOptions(int codeDisplay, int learnerLeaderDisplay) {
        loginDialogView.findViewById(R.id.code_entry_view).setVisibility(codeDisplay);
        loginDialogView.findViewById(R.id.learner_leader_view).setVisibility(learnerLeaderDisplay);
    }

    /**
     * Set the name of the current guide to be displayed.
     * @param guideName A String representing the current guide.
     */
    public void setLeaderName(String guideName) {
        ((TextView) loginDialogView.findViewById(R.id.current_leader_name)).setText(guideName);
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

    /**
     * Hide the keyboard when tapping outside of the keyboard area. Used within an onClickListener
     * for the view.
     * @param view A View which the current screen is on (Inside an onClickListener).
     */
    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)main.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Dismiss any dialogs that are currently open.
     */
    public void cleanUpDialogs() {
        if (loginDialog != null)
            loginDialog.dismiss();
        if (waitingDialog != null)
            waitingDialog.dismiss();
        if (warningDialog != null)
            warningDialog.dismiss();
        if (appPushDialog != null)
            appPushDialog.dismiss();
        if (confirmPushDialog != null)
            confirmPushDialog.dismiss();
        if (recallPrompt != null)
            recallPrompt.dismiss();
        if  (main.getLumiAppInstaller().installDialog != null) {
            main.getLumiAppInstaller().installDialog.dismiss();
        }
        if (main.getWebManager() != null) {
            main.getWebManager().cleanUp();
        }
    }
}
