package com.lumination.leadme.players;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.documentfile.provider.DocumentFile;

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.accessibility.VRAccessibilityManager;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.managers.NearbyPeersManager;

import java.io.File;
import java.util.Set;

/*
* Operation flow
* Open a file picker to select a video - returning the path or uri
* Display the VideoView preview screen to select a starting point
* Dispatch an action to open the VR player
* (Peer device) VRAccessibilityManager sends intent with the selected path to the app
* Opens the video controller, buttons dispatch actions
* (Peer device) sends intent to the VR app depending on the action received
*/
public class VREmbedPhotoPlayer {
    private final static String TAG = "embedPlayerVR";
    //Package name of the external VR player
    public final static String packageName = "com.lumination.VRPlayer";
    private final String appName = "VRPlayer"; //Past to the app manager

    private String fileName;
    private Boolean firstOpen = true;

    private AlertDialog videoControlDialog, playbackSettingsDialog;

    private final View photoControllerDialogView; //, videoControls
    private final ImageView controllerImageView;

    private PopupWindow popupWindow;
    private ImageView changeProjectionBtn;
    private TextView monoText, eacText, eac3dText, ouText, sbsText;

    private Button vrplayerPreviewPushBtn, vrplayerSetSourceBtn;
    private ImageView vrplayerPreviewPhotoView;
    private View vrplayerSettingsDialogView;
    private View vrplayerPhotoControls, vrplayerNoVideo;

    private final LeadMeMain main;

    public VREmbedPhotoPlayer(LeadMeMain main) {
        this.main = main;

        photoControllerDialogView = View.inflate(main, R.layout.f__playback_control_vr_photoplayer, null);

        controllerImageView = photoControllerDialogView.findViewById(R.id.photo_stream_imageview);
        controllerImageView.setTag("CONTROLLER");

        setupProjectionDropdown();

        createPlaybackSettingsPopup();
        setupGuideVideoControllerButtons();
    }

    /**
     * Setup the projection dropdown by adding onclick listeners for each of the
     * projection types available.
     */
    private void setupProjectionDropdown() {
        changeProjectionBtn = photoControllerDialogView.findViewById(R.id.change_projection_btn);
        View projectionDropdown = View.inflate(main, R.layout.e__projection_menu, null);
        popupWindow = new PopupWindow(
                projectionDropdown,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(10);
        popupWindow.setOnDismissListener(() -> {});

        monoText = projectionDropdown.findViewById(R.id.mono_text);
        LinearLayout monoBtn = projectionDropdown.findViewById(R.id.mono_toggle);
        monoBtn.setOnClickListener(v -> {
            changeProjection("mono");
            changeSelectedProjection(monoText);
        });

        //TODO EAC mode is not enabled yet for VR photo player
        eacText = projectionDropdown.findViewById(R.id.eac_text);
        LinearLayout eacBtn = projectionDropdown.findViewById(R.id.eac_toggle);
        eacText.setVisibility(View.GONE);
        eacBtn.setVisibility(View.GONE);
        eacBtn.setOnClickListener(v -> {
//            changeProjection("eac");
//            changeSelectedProjection(eacText);
        });

        eac3dText = projectionDropdown.findViewById(R.id.eac3d_text);
        LinearLayout eac3dBtn = projectionDropdown.findViewById(R.id.eac3d_toggle);
        eac3dText.setVisibility(View.GONE);
        eac3dBtn.setVisibility(View.GONE);
        eac3dBtn.setOnClickListener(v -> {
//            changeProjection("eac3d");
//            changeSelectedProjection(eac3dText);
        });

        ouText = projectionDropdown.findViewById(R.id.over_under_text);
        LinearLayout ouBtn = projectionDropdown.findViewById(R.id.over_under_toggle);
        ouBtn.setOnClickListener(v -> {
            changeProjection("ou");
            changeSelectedProjection(ouText);
        });

        sbsText = projectionDropdown.findViewById(R.id.side_by_side_text);
        LinearLayout sbsBtn = projectionDropdown.findViewById(R.id.side_by_side_toggle);
        sbsBtn.setOnClickListener(v -> {
            changeProjection("sbs");
            changeSelectedProjection(sbsText);
        });
    }

    /**
     * Takes a String and sets the video preview for the EmbedPlayer, sets one for the preview and
     * one for the control. Separated for ease of use and customisation in the future.
     * @param path A String of the content that is going to be played.
     */
    public void setFilepath(String path) {
        File file = new File(path);
        Log.d(TAG,"File name is: " + file.getName());

        this.fileName = file.getName(); //get file name

        //hide the choose video button if fileName is null - first time choice?
        disableSetSourceBtn(true);

        //For testing purposes
        Log.e(TAG, path);
        Log.e(TAG, this.fileName);

        int fileSize = Integer.parseInt(String.valueOf(file.length()/1024));
        if (fileSize > 1000000) {
            Log.e(TAG, "VR Image is way too big");
            Controller.getInstance().getDialogManager().showImageTooBigDialog();
            return;
        }

        //In case a file is not chosen or there is an error
        if(path == null || this.fileName == null) {
            Log.e(TAG, "File is missing or path is incorrect");
            return;
        }

        vrplayerPreviewPhotoView.setImageURI(Uri.parse(path));
        //vrplayerPreviewPhotoView.setVideoPath(path);

        noPhotoChosen(false);
    }

    /**
     * Takes a URI and set s the video preview for the EmbedPlayer, sets one for the preview and
     * one for the control. Separated for ease of use and customisation in the future.
     * @param path A URI of the content that is going to be played.
     */
    public void setFilepath(Uri path) {
        this.fileName = getFileName(path);

        //hide the choose video button if fileName is null - first time choice?
        disableSetSourceBtn(true);

        //For testing purposes
        Log.e(TAG, String.valueOf(path));
        Log.e(TAG, this.fileName);

        //In case a file is not chosen or there is an error
        if(path == null || this.fileName == null) {
            Log.e(TAG, "File is missing or path is incorrect");
            return;
        }

        DocumentFile file = DocumentFile.fromSingleUri(main.getApplicationContext(), path);
        int fileSize = Integer.parseInt(String.valueOf(file.length()/1024));
        if (fileSize > 1000000) {
            Log.e(TAG, "VR Image is way too big");
            Controller.getInstance().getDialogManager().showImageTooBigDialog();
            return;
        }

        vrplayerPreviewPhotoView.setImageURI(path);
        //vrplayerPreviewPhotoView.setVideoURI(path);

        noPhotoChosen(false);

        vrplayerPreviewPhotoView.setVisibility(View.VISIBLE);
    }

    /**
     * If the user has just selected a video for viewing hide the choose video button.
     */
    private void disableSetSourceBtn(boolean disable) {
        vrplayerSetSourceBtn.setVisibility(disable ? View.GONE : View.VISIBLE);
    }

    //Get a file name from a provided URI
    private String getFileName(Uri uri) {
        String displayName;

        Cursor cursor = main.getContentResolver().query(uri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        displayName = cursor.getString(nameIndex);

        cursor.close();

        return displayName;
    }

    //Sets up the UI for selecting where to start the video from.
    private void createPlaybackSettingsPopup() {
        vrplayerSettingsDialogView = View.inflate(main, R.layout.f__playback_settings_vr_photoplayer, null);
        vrplayerSetSourceBtn = vrplayerSettingsDialogView.findViewById(R.id.set_source_btn);
        vrplayerPreviewPushBtn = vrplayerSettingsDialogView.findViewById(R.id.vr_push_btn);
        vrplayerPhotoControls = vrplayerSettingsDialogView.findViewById(R.id.photo_controls);
        vrplayerNoVideo = vrplayerSettingsDialogView.findViewById(R.id.no_source_yet);
        vrplayerPreviewPhotoView = vrplayerSettingsDialogView.findViewById(R.id.photo_stream_imageview);
        Controller.getInstance().getDialogManager().setupPushToggle(vrplayerSettingsDialogView, false);

        vrplayerSettingsDialogView.findViewById(R.id.photo_back_btn).setOnClickListener(v -> {
            this.fileName = null;
            LeadMeMain.vrPath = null;
            LeadMeMain.vrURI = null;
            playbackSettingsDialog.dismiss();
            Controller.getInstance().getDialogManager().showVRContentDialog();
        });

        vrplayerSetSourceBtn.setOnClickListener(view -> selectSource());

        vrplayerPreviewPushBtn.setOnClickListener(view -> {
            LeadMeMain.UIHandler.post(this::pushToLearners);

            //TODO test this v
            //VR default is always set to mono
            //changeProjection("mono");
            //changeSelectedProjection(monoText);
        });
    }

    /**
     * Opens the correct file picker depending on the users device. If this is the first time
     * using the VR player a information popup appears.
     */
    private void selectSource() {
        //Reset the file path each time
        this.fileName = null;

        Log.d(TAG, "FileUtilities: picking a file");

        //Function to let leaders know what files can be picked
        Controller.getInstance().getDialogManager().showFileTypeDialog();
    }

    /**
     * Check that the source is not null and push to the appropriate learners. Determines if the
     * device needs a Uri or an absolute path for the playback controller.
     */
    private void pushToLearners() {
        if (LeadMeMain.vrURI == null && LeadMeMain.vrPath == null) {
            Toast.makeText(main, "A video has not been selected", Toast.LENGTH_SHORT).show();
            return;
        }

        playbackSettingsDialog.dismiss();

        if(Controller.isMiUiV9()) {
            controllerImageView.setImageURI(Uri.parse(LeadMeMain.vrPath));
        } else {

            controllerImageView.setImageURI(LeadMeMain.vrURI);
        }

        //LAUNCH THE APPLICATION FROM HERE
        Controller.getInstance().getAppManager().launchApp(
                packageName,
                appName,
                false,
                "false",
                true,
                NearbyPeersManager.getSelectedPeerIDsOrAll());

        main.showLeaderScreen();
        showPushConfirmed();

        //Set the source for the peers device
        setPhotoSource();
    }

    //CONTROL FUNCTIONS
    //Used when repushing the application as the appName will already be set
    public void showPlaybackPreview() {
        openPreview(appName);
    }

    public void showPlaybackPreview(String title) {
        openPreview(title);
    }

    private void openPreview(String title) {
        if(LeadMeMain.vrURI != null || LeadMeMain.vrPath != null) {
            if(Controller.isMiUiV9()) {
                controllerImageView.setImageURI(Uri.parse(LeadMeMain.vrPath));
            } else {
                controllerImageView.setImageURI(LeadMeMain.vrURI);
            }
        }

        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        if (playbackSettingsDialog == null) {
            playbackSettingsDialog = new AlertDialog.Builder(main)
                    .setView(vrplayerSettingsDialogView)
                    .create();

            playbackSettingsDialog.setCancelable(false);

            playbackSettingsDialog.setOnDismissListener(dialog -> main.hideSystemUI());
        }

        noPhotoChosen(fileName == null);

        if(firstOpen) {
            disableSetSourceBtn(fileName != null);
        } else {
            disableSetSourceBtn(firstOpen);
        }

        Controller.getInstance().getDialogManager().toggleSelectedView(vrplayerSettingsDialogView);
        playbackSettingsDialog.show();
    }

    private void showPushConfirmed() {
        playbackSettingsDialog.dismiss();
        View confirmPushDialogView = View.inflate(main, R.layout.e__confirm_popup, null);
        AlertDialog confirmPopup = new AlertDialog.Builder(main)
                .setView(confirmPushDialogView)
                .show();
        ((TextView)confirmPushDialogView.findViewById(R.id.push_success_comment)).setText(R.string.video_launch_confirm);
        Button ok = confirmPushDialogView.findViewById(R.id.ok_btn);
        ok.setOnClickListener(v -> {
            confirmPopup.dismiss();
            //return to main screen
            Controller.getInstance().getDialogManager().hideConfirmPushDialog();
            showVideoController();
        });
    }

    /**
     * Disables or enables the push button depending on if the user has selected a video.
     * Hides or shows the no chosen video element. Tells the user that nothing has been selected.
     * @param show A boolean representing if the no video selected layout should be shown.
     */
    private void noPhotoChosen(boolean show) {
        vrplayerPreviewPushBtn.setVisibility(show ? View.GONE : View.VISIBLE);
        vrplayerNoVideo.setVisibility(show ? View.VISIBLE : View.GONE);
        vrplayerPhotoControls.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Relaunches the last VR experience with the selected video source.
     * @param peerSet A set of strings representing the learner ID's to send the action to.
     */
    public void relaunchVR(Set<String> peerSet) {
        Controller.getInstance().getAppManager().launchApp(packageName, appName, false, "false", true, peerSet);
        setPhotoSource();
    }

    private void setupGuideVideoControllerButtons() {
        photoControllerDialogView.findViewById(R.id.push_again_btn).setOnClickListener(v ->
            relaunchVR(NearbyPeersManager.getSelectedPeerIDsOrAll())
        );

        photoControllerDialogView.findViewById(R.id.new_photo_btn).setOnClickListener(v -> {
            videoControlDialog.dismiss();
            this.fileName = null;
            firstOpen = true;
            vrplayerPreviewPhotoView.setVisibility(View.INVISIBLE);
            showPlaybackPreview(appName);
            selectSource(); //go straight to file picker
        });

        photoControllerDialogView.findViewById(R.id.photo_back_btn).setOnClickListener(v -> {
            hidePhotoController();
            firstOpen = false;
        });

        //TODO not anchored correctly
        changeProjectionBtn.setOnClickListener(v ->
            popupWindow.showAsDropDown(v,-200,-100)
        );
    }

    private void showVideoController() {
        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(photoControllerDialogView)
                    .create();

            videoControlDialog.setCancelable(false);

            videoControlDialog.setOnDismissListener(dialog -> {
                main.hideSystemUI();
            });
        }

        videoControlDialog.show();
    }

    private void hidePhotoController() {
        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        videoControlDialog.dismiss();
    }

    //VR Player Controls
    //sends an action to the connected peers.
    private void setPhotoSource() {
        //Send action to peers to play
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_SET_SOURCE + ":" + fileName + ":" + 0  + ":" + "Photo",
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }

    /**
     * Send an action to the connected peers.
     * @param type A string representing what type the projection should change to.
     */
    private void changeProjection(String type) {
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_PROJECTION + ":" + type,
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }

    /**
     * Change the drawable to display which of the projection types is currently active
     * @param view A view representing which of the projection types has just been selected.
     */
    private void changeSelectedProjection(TextView view) {
        view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_arrow ,0,0,0);

        changeToDefault(view, monoText);
        changeToDefault(view, eacText);
        changeToDefault(view, eac3dText);
        changeToDefault(view, ouText);
        changeToDefault(view, sbsText);
    }

    /**
     * Change a textview back to the default if it has not been selected.
     * @param selected A textview that has just been selected.
     * @param old A textview to compare against.
     */
    private void changeToDefault(TextView selected, TextView old) {
        if(selected != old) {
            old.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_disconnect_peer ,0,0,0);
        }
    }
}

