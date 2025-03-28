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
import android.os.Handler;
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

import androidx.core.content.res.ResourcesCompat;

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.accessibility.VRAccessibilityManager;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.adapters.StudentAlertsAdapter;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.services.NetworkService;
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
    private static final String TAG = "DialogManager";
    private final LeadMeMain main;
    private final Resources resources;

    private View confirmPushDialogView,
            loginDialogView,
            reauthDialogView,
            toggleBtnView,
            permissionDialogView,
            requestDialogView;

    private AlertDialog warningDialog,
            waitingDialog,
            appPushDialog,
            confirmPushDialog,
            studentAlertsDialog,
            loginDialog,
            reauthDialog,
            recallPrompt,
            permissionDialog,
            requestDialog,
            fileTypeDialog,
            updateDialog,
            vrContentTypeDialog,
            firstTimeVRDialog,
            vrInstallDialog,
            imageTooBigDialog;

    private TextView appPushMessageView,
            warningDialogTitle,
            warningDialogMessage,
            recallMessage,
            permissionDialogMessage,
            requestDialogMessage,
            additionalInfo;

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
        setupUpdateDialog();
        setupVRContentDialog();
        setupVRInstallDialog();
        setupAlertsViewDialog();
        setupLoginDialogView();
        setupReauthDialogView();
        setupLoginDialog();
        setupReauthDialog();
        setupRecallDialog();
        setupVRFirstTime();
        setupFileTypes();
        setupRequestDialog();
        setupImageTooBigDialog();
    }

    //SETUP FUNCTIONS
    private void setupWaitingDialog() {
        View waitingDialogView = View.inflate(main, R.layout.e__waiting_to_connect, null);
        Button backBtn = waitingDialogView.findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> {
            dialogShowing = false;
            Controller.getInstance().getNearbyManager().cancelConnection();
            waitingDialog.dismiss();
        });

        waitingDialog = new AlertDialog.Builder(main)
                .setView(waitingDialogView)
                .create();

        waitingDialog.setCancelable(false);

        waitingDialog.setOnDismissListener(dialog -> LeadMeMain.getInstance().hideSystemUI());
    }

    private void setupImageTooBigDialog() {
        View imageDialogView = View.inflate(main, R.layout.image_too_big_dialog, null);
        Button closeBtn = imageDialogView.findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(v -> {
            dialogShowing = false;
            imageTooBigDialog.dismiss();
        });

        imageTooBigDialog = new AlertDialog.Builder(main)
                .setView(imageDialogView)
                .create();

        imageTooBigDialog.setCancelable(false);

        imageTooBigDialog.setOnDismissListener(dialog -> LeadMeMain.getInstance().hideSystemUI());
    }

    private void setupAppPushDialog() {
        appPushDialogView = View.inflate(main, R.layout.e__preview_app_push, null);

        appPushDialogView.findViewById(R.id.push_btn).setOnClickListener(v -> {
            Set<String> peerSet;
            if(LeadMeMain.selectedOnly) {
                peerSet = NearbyPeersManager.getSelectedPeerIDsOrAll();
            } else {
                peerSet = NearbyPeersManager.getAllPeerIDs();
            }
            Controller.getInstance().getAppManager().launchApp(appPushPackageName, appPushTitle, false, "false", false, peerSet);
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

    private void setupUpdateDialog() {
        View updateDialogView = View.inflate(main, R.layout.e__update_popup, null);
        Button okBtn = updateDialogView.findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(v -> {
            updateDialog.dismiss();
            dialogShowing = false;
        });

        Button updateBtn = updateDialogView.findViewById(R.id.update_btn);
        updateBtn.setOnClickListener(v -> {
            updateDialog.dismiss();
            dialogShowing = false;

            Uri uri = Uri.parse("http://play.google.com/store/apps/details?id=com.lumination.leadme");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            main.startActivity(intent);
        });

        updateDialog = new AlertDialog.Builder(main)
                .setView(updateDialogView)
                .create();
        updateDialog.setOnDismissListener(dialog -> hideSystemUI());
    }

    private void setupVRContentDialog() {
        View vrContentType = View.inflate(main, R.layout.e__leader_vr_type_popup, null);

        Button videoBtn = vrContentType.findViewById(R.id.select_video_source_btn);
        videoBtn.setOnClickListener(v -> {
            vrContentTypeDialog.dismiss();
            dialogShowing = false;
            LeadMeMain.defaultVideo = true;
            Controller.getInstance().getVrEmbedVideoPlayer().showPlaybackPreview();
        });

        Button linkBtn = vrContentType.findViewById(R.id.select_link_source_btn);
        linkBtn.setOnClickListener(v -> {
            vrContentTypeDialog.dismiss();
            dialogShowing = false;
            LeadMeMain.defaultVideo = true;
            Controller.getInstance().getVrEmbedLinkPlayer().showPlaybackPreview();
        });

        Button photoBtn = vrContentType.findViewById(R.id.select_photo_source_btn);
        photoBtn.setOnClickListener(v -> {
            vrContentTypeDialog.dismiss();
            dialogShowing = false;
            LeadMeMain.defaultVideo = false;
            Controller.getInstance().getVrEmbedPhotoPlayer().showPlaybackPreview();
        });

        Button cancelBtn = vrContentType.findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(v -> {
            vrContentTypeDialog.dismiss();
            dialogShowing = false;
        });

        vrContentTypeDialog = new AlertDialog.Builder(main)
                .setView(vrContentType)
                .create();
        vrContentTypeDialog.setOnDismissListener(dialog -> hideSystemUI());
    }

    private void setupVRInstallDialog() {
        View updateDialogView = View.inflate(main, R.layout.e__install_vr_popup, null);
        Button updateBtn = updateDialogView.findViewById(R.id.update_btn);
        updateBtn.setOnClickListener(v -> {
            vrInstallDialog.dismiss();
            dialogShowing = false;

            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.lumination.VRPlayer");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            main.startActivity(intent);

            //Can auto install if needed?
            //main.getLumiAppInstaller().autoInstall("com.lumination.VRPlayer", "VR Player", "true", null);
        });

        Button cancelBtn = updateDialogView.findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(v -> {
            vrInstallDialog.dismiss();
            dialogShowing = false;
        });

        vrInstallDialog = new AlertDialog.Builder(main)
                .setView(updateDialogView)
                .create();
        vrInstallDialog.setOnDismissListener(dialog -> hideSystemUI());
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

        cancelBtn.setOnClickListener(v ->
                firstTimeVRDialog.dismiss()
        );
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

            if(Controller.isMiUiV9()) {
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

        Button blockBtn = requestDialogView.findViewById(R.id.block_btn);

        blockBtn.setOnClickListener(v -> {
            LeadMeMain.fileRequests = new HashSet<>();
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
                    "\nPlease transfer any required videos before starting your lesson");
        } else {
            requestDialogMessage.setText(LeadMeMain.fileRequests.size() + " learner does not have the video. " +
                    "\nPlease transfer any required videos before starting your lesson");
            waitForOthers(requestDialogView.findViewById(R.id.block_btn), delay);
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
                    button.setText(originalText);
                    button.setEnabled(true);
                    additionalInfo.setVisibility(View.GONE);
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
                DispatchManager.permissionAllowed(pm, true);
            }
            permissionDialog.dismiss();
            permissions = null;
        });

        blockBtn.setOnClickListener(view -> {
            for ( String pm : permissions) {
                DispatchManager.permissionAllowed(pm, false);
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

    /**
     * Display an AlertDialog for first time users with a link to the online manual.
     */
    public void displayGuidePrompt() {
        View firstDialog = View.inflate(main, R.layout.a__first_time, null);

        AlertDialog alert = new AlertDialog.Builder(main)
                .setView(firstDialog)
                .show();

        firstDialog.findViewById(R.id.open_guide).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/viewerng/viewer?embedded=true&url=https://github.com/LuminationDev/public/raw/main/LeadMeEdu-FirstTimeSetup.pdf"));
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

    public void showImageTooBigDialog() {
        if (imageTooBigDialog == null) {
            setupImageTooBigDialog();
        }
        imageTooBigDialog.show();
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
            LeadMeMain.UIHandler.postDelayed(() -> {
                hideConfirmPushDialog();
                Controller.getInstance().getFavouritesManager().launchUrlYtFavourites(FavouritesManager.LAUNCHTYPE_WEB);
            }, 1500);
        } else {
            LeadMeMain.UIHandler.postDelayed(this::hideConfirmPushDialog, 1500);
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
     * Display a custom pop up message about an available play store update.
     */
    public void showUpdateDialog() {
        if (destroying) {
            return;
        }
        if (updateDialog == null) {
            setupUpdateDialog();
        }
        updateDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }

    /**
     * Display a custom pop up message about the VR player available play store.
     */
    public void showVRContentDialog() {
        if (destroying) {
            return;
        }
        if (vrContentTypeDialog == null) {
            setupVRContentDialog();
        }
        vrContentTypeDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }

    /**
     * Display a custom pop up message about the VR player available play store.
     */
    public void showVRInstallDialog() {
        if (destroying) {
            return;
        }
        if (vrInstallDialog == null) {
            setupVRInstallDialog();
        }
        vrInstallDialog.show();
        hideSystemUI();
        dialogShowing = true;
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
        Controller.getInstance().getConnectedLearnersAdapter().refreshAlertsView();
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
            main.returnToAppFromMainAction(LeadMeMain.selectedOnly);
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

        if (ConnectedLearnersAdapter.someoneIsSelected() && (NearbyPeersManager.getSelectedPeerIDs().size() < NearbyPeersManager.getAllPeerIDs().size())) {
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
            LeadMeMain.selectedOnly = true;
            makeSelectedBtnActive(selectedBtn, everyoneBtn);
            if (pushBtn != null) {
                pushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));
            }
        });

        everyoneBtn.setOnClickListener(v -> {
            LeadMeMain.selectedOnly = false;
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
        toggleSelectedBtn.setVisibility(ConnectedLearnersAdapter.someoneIsSelected()
                && (NearbyPeersManager.getSelectedPeerIDs().size() < NearbyPeersManager.getAllPeerIDs().size())
                ? View.VISIBLE : View.GONE);
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
                loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.VISIBLE);
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

    private void setupReauthDialogView() {
        inflateReauthDialog();

        reauthDialogView.findViewById(R.id.login_back).setOnClickListener(view -> {
            hideReauthDialog(true);
        });
    }

    private void setupReauthDialog() {
        if (reauthDialog == null) {
            reauthDialog = new AlertDialog.Builder(main)
                    .setView(reauthDialogView)
                    .create();
        }

        reauthDialog.setOnDismissListener(dialog -> hideSystemUI());
        reauthDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        EditText email = reauthDialogView.findViewById(R.id.login_email);
        EditText password = reauthDialogView.findViewById(R.id.login_password);
        Button enterBtn = reauthDialogView.findViewById(R.id.login_enter);
        Button backBtn = reauthDialogView.findViewById(R.id.login_back);
        TextView errorText = reauthDialogView.findViewById(R.id.error_text);


        enterBtn.setOnClickListener(view -> {
            main.setProgressSpinner(3000, reauthDialogView.findViewById(R.id.indeterminateBar));
            errorText.setVisibility(View.GONE);
            if(!password.getText().toString().isEmpty() && !email.getText().toString().isEmpty()) {
                AuthCredential credential = EmailAuthProvider
                        .getCredential(email.getText().toString(), password.getText().toString());
                Controller.getInstance().getAuthenticationManager().getCurrentAuthUser().reauthenticate(credential).addOnSuccessListener(task -> {
                    FirebaseFirestore.getInstance().collection("users").document(Controller.getInstance().getAuthenticationManager().getCurrentAuthUser().getUid()).delete().addOnSuccessListener(task1 -> {
                        Controller.getInstance().getAuthenticationManager().getCurrentAuthUser().delete().addOnSuccessListener(task3 -> {
                            main.optionsScreen.findViewById(R.id.options_leader).setVisibility(View.GONE);
                            main.logoutResetController();
                        }).addOnFailureListener(failTask -> {
                            errorText.setVisibility(View.VISIBLE);
                            errorText.setText("Deletion failed with message: " + failTask.getMessage());
                        });
                    }).addOnFailureListener(failTask -> {
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText("Deletion failed with message: " + failTask.getMessage());
                    });
                }).addOnFailureListener(failTask -> {
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("Deletion failed with message: " + failTask.getMessage());
                });
            } else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText(R.string.check_details);
            }
        });

        password.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                main.getInputManager().showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        });

        backBtn.setOnClickListener(v -> reauthDialog.dismiss());
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
                errorText.setText(R.string.check_details);
            }
        });

        password.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                main.getInputManager().showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        });

        forgotPassword.setOnClickListener(view -> Controller.getInstance().getAuthenticationManager().showForgottenPassword(loginDialog));

        googleSignin.setOnClickListener(view -> main.googleSignIn());

        signup.setOnClickListener(view -> {
            loginDialog.dismiss();

            if(!PermissionManager.isInternetAvailable(LeadMeMain.getInstance().getApplicationContext())) {
                Controller.getInstance().getDialogManager().showWarningDialog("Currently Offline", "No internet access detected. Please connect to continue.");
                return;
            }

            Controller.getInstance().getAuthenticationManager().buildloginsignup(0);
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
            }
        }
    }

    /**
     * Hide the login dialog if a user has cancelled the process halfway through.
     * @param cancelled A boolean representing if the login has been cancelled.
     */
    public void hideReauthDialog(boolean cancelled) {
        Log.d(TAG, "Hiding dialog box");
        closeKeyboard();
        hideSystemUI();

        if (reauthDialog != null) {
            dialogShowing = false;
            reauthDialog.dismiss();
        }
    }

    /**
     * Get the current instance of the loginDialog.
     * @return An AlertDialog instance of the loginDialog.
     */
    public AlertDialog getLoginDialog() {
        return loginDialog;
    }

    /**
     * Get the current instance of the reauthDialog.
     * @return An AlertDialog instance of the reauthDialog.
     */
    public AlertDialog getReauthDialog() {
        return reauthDialog;
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
     * Get the name of the current user.
     * */
    public TextView inflateReauthDialog() {
        if (reauthDialogView == null || nameView == null) {
            reauthDialogView = View.inflate(main, R.layout.b__reauth_popup, null);
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
        if (loginDialog != null) {
            loginDialog.dismiss();
        }
        if (reauthDialog != null) {
            reauthDialog.dismiss();
        }
        if (waitingDialog != null) {
            waitingDialog.dismiss();
        }
        if (imageTooBigDialog != null) {
            imageTooBigDialog.dismiss();
        }
        if (warningDialog != null) {
            warningDialog.dismiss();
        }
        if (appPushDialog != null) {
            appPushDialog.dismiss();
        }
        if (confirmPushDialog != null) {
            confirmPushDialog.dismiss();
        }
        if (recallPrompt != null) {
            recallPrompt.dismiss();
        }
        if (Controller.getInstance().getWebManager() != null) {
            Controller.getInstance().getWebManager().cleanUp();
        }
    }

    /**
     * Present the user with a choice to push selected content through the VR Player or the default
     * option.
     * @param linkTitle A string which displays what is about to be pushed.
     * @param link A string of the URL which is about to be launched.
     * @param leader A boolean representing if the popup is being opened by a teacher.
     */
    public void createContentLaunchChoiceDialog(String linkTitle, String link, boolean leader) {
        View contentLaunchChoice = View.inflate(main, R.layout.e__app_push_selection, null);
        AlertDialog contentLaunchChoiceDialog = new AlertDialog.Builder(main)
                .setView(contentLaunchChoice)
                .create();

        //Set the app title
        TextView title = contentLaunchChoice.findViewById(R.id.push_app_title);
        title.setText(linkTitle);

        //Configure the button effects
        Button vrPlayerBtn = contentLaunchChoice.findViewById(R.id.vr_player_btn);
        vrPlayerBtn.setOnClickListener(v -> {
            contentLaunchChoiceDialog.dismiss();
            dialogShowing = false;

            if(leader) {
                Controller.getInstance().getVrEmbedLinkPlayer().openPreview(link);
            } else {
                CuratedContentManager.curatedContentScreen.findViewById(R.id.back_btn).setOnClickListener(view -> main.leadmeAnimator.setDisplayedChild(main.ANIM_LEARNER_INDEX));
                //Launch the VR Player application
                Controller.getInstance().getAppManager().launchLocalApp(
                        "com.lumination.VRPlayer",
                        "VRPlayer",
                        true,
                        false,
                        "false",
                        null);

                //Wait while the VR Player is opening then send through the source lin
                new Handler().postDelayed(() -> {
                    //Send through the source message
                    //Modify the link so that we don't split it accidentally
                    String safeURL = link.replace(':', '|');

                    NetworkService.receiveMessage("ACTION," + DispatchManager.encodeMessage(
                            "Action",
                            Controller.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_SET_SOURCE + ":" + safeURL + ":" + 0 + ":" + "Link"));
                }, 3000);
            }
        });

        Button defaultBtn = contentLaunchChoice.findViewById(R.id.default_btn);
        defaultBtn.setOnClickListener(v -> {
            contentLaunchChoiceDialog.dismiss();
            dialogShowing = false;

            if(leader) {
                WebManager webManager = new WebManager(main);
                webManager.showPreview(link, true);
            } else {
                CuratedContentManager.curatedContentScreen.findViewById(R.id.back_btn).setOnClickListener(view -> main.leadmeAnimator.setDisplayedChild(main.ANIM_LEARNER_INDEX));
                NetworkService.receiveMessage("ACTION," + DispatchManager.encodeMessage("Action", Controller.LAUNCH_URL + link + ":::" + title));
            }
        });

        Button backBtn = contentLaunchChoice.findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> {
            contentLaunchChoiceDialog.dismiss();
            dialogShowing = false;
        });

        contentLaunchChoiceDialog.setOnDismissListener(dialog -> hideSystemUI());
        contentLaunchChoiceDialog.show();
    }
}
