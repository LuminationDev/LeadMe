package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.VRAccessibilityManager;

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
public class VREmbedPlayer {
    private final static String TAG = "embedPlayerVR";
    //Package name of the external VR player
    //TODO change this to Lumination at some point
    public final static String packageName = "com.Edward.VRPlayer";

    private String appName, fileName;
    private Boolean firstOpen = true;

    private AlertDialog videoControlDialog, playbackSettingsDialog;

    private final View videoControllerDialogView; //, videoControls
    private final VideoView controllerVideoView;

    private Button vrplayerPreviewPushBtn, vrplayerSetSourceBtn;
    private VideoView vrplayerPreviewVideoView;
    private View vrplayerSettingsDialogView;
    private View vrplayerVideoControls, vrplayerNoVideo;

    private TextView playFromTime, totalTimeText, elapsedTimeText;
    private int totalTime = -1;
    private float currentTime = 0;
    private int startFromTime = 0;
    private SeekBar progressBar;

    private final ImageView playBtn, pauseBtn;

    Switch viewModeToggle;

    private final LeadMeMain main;

    public VREmbedPlayer(LeadMeMain main) {
        this.main = main;

        videoControllerDialogView = View.inflate(main, R.layout.f__playback_control_vrplayer, null);

        controllerVideoView = videoControllerDialogView.findViewById(R.id.video_stream_videoview);
        controllerVideoView.setTag("CONTROLLER");

        playBtn = videoControllerDialogView.findViewById(R.id.play_btn);
        pauseBtn = videoControllerDialogView.findViewById(R.id.pause_btn);

        createPlaybackSettingsPopup();
        setupGuideVideoControllerButtons();

        //listener to manage when a video has loaded properly
        vrplayerPreviewVideoView.setOnPreparedListener(mp -> {
            int duration = mp.getDuration();
            int videoDuration = vrplayerPreviewVideoView.getDuration()/1000;
            Log.d(TAG, String.format("onPrepared: (ms)duration=%d, (s)videoDuration=%d", duration,
                    videoDuration));
            setTotalTime(videoDuration);
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

        //In case a file is not chosen or there is an error
        if(path == null || this.fileName == null) {
            Log.e(TAG, "File is missing or path is incorrect");
            return;
        }

        vrplayerPreviewVideoView.setVideoPath(path);

        noVideoChosen(false);

        //setting the preview video
        setupVideoPreview(vrplayerPreviewVideoView);
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

        vrplayerPreviewVideoView.setVideoURI(path);

        noVideoChosen(false);

        //setting the preview video
        setupVideoPreview(vrplayerPreviewVideoView);
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

    //Sets the video source and moves it to the top of the UI as some phones will display it behind
    //the pop up dialog.
    private void setupVideoPreview(VideoView video) {
        video.setZOrderOnTop(true);

        if(startFromTime != 0) {
            video.seekTo(startFromTime * 1000);
        } else {
            //display the first frame instead of black space
            video.seekTo(1);
        }

        TextView touchDesc = videoControllerDialogView.findViewById(R.id.touch_screen_desc);
        viewModeToggle = videoControllerDialogView.findViewById(R.id.view_mode_toggle);
        viewModeToggle.setChecked(true);
        viewModeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                viewModeToggle.setText(R.string.view_mode_on);
                touchDesc.setText(R.string.touch_screens_disabled);
                ImageViewCompat.setImageTintList(videoControllerDialogView.findViewById(R.id.view_mode_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_blue)));
                main.lockFromMainAction();
            }else{
                ImageViewCompat.setImageTintList(videoControllerDialogView.findViewById(R.id.view_mode_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_medium_grey)));
                touchDesc.setText(R.string.touch_screens_enabled);
                viewModeToggle.setText(R.string.view_mode_off);
                main.unlockFromMainAction();
            }
        });
    }

    //Sets up the UI for selecting where to start the video from.
    private void createPlaybackSettingsPopup() {
        vrplayerSettingsDialogView = View.inflate(main, R.layout.f__playback_settings_vrplayer, null);
        vrplayerSetSourceBtn = vrplayerSettingsDialogView.findViewById(R.id.set_source_btn);
        vrplayerPreviewPushBtn = vrplayerSettingsDialogView.findViewById(R.id.vr_push_btn);
        vrplayerVideoControls = vrplayerSettingsDialogView.findViewById(R.id.video_controls);
        vrplayerNoVideo = vrplayerSettingsDialogView.findViewById(R.id.no_source_yet);
        progressBar = vrplayerSettingsDialogView.findViewById(R.id.progressBar);
        playFromTime = vrplayerSettingsDialogView.findViewById(R.id.video_play_from_input);
        elapsedTimeText = vrplayerSettingsDialogView.findViewById(R.id.elapsedTimeText);
        totalTimeText = vrplayerSettingsDialogView.findViewById(R.id.totalTimeText);
        vrplayerPreviewVideoView = vrplayerSettingsDialogView.findViewById(R.id.video_stream_videoview);
        vrplayerPreviewVideoView.setTag("PREVIEW/PLAYBACK SETTINGS");
        main.getDialogManager().setupPushToggle(vrplayerSettingsDialogView, false);

        vrplayerSettingsDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v -> {
            this.fileName = null;
            main.vrVideoPath = null;
            main.vrVideoURI = null;
            playbackSettingsDialog.dismiss();
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //convert from percentage to seconds
                int durationCalc = (int) ((seekBar.getProgress() / 100.0) * totalTime);
                if (durationCalc < 0) {
                    durationCalc = 0; //make sure it's sensible
                }
                setNewTime(durationCalc);
                playFromTime.setText(intToTime(durationCalc));
            }
        });

        vrplayerSetSourceBtn.setOnClickListener(view -> selectSource());

        vrplayerPreviewPushBtn.setOnClickListener(view -> main.getHandler().post(this::pushToLearners));
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
        main.getDialogManager().showFileTypeDialog();
    }

    /**
     * Check that the source is not null and push to the appropriate learners. Determines if the
     * device needs a Uri or an absolute path for the playback controller.
     */
    private void pushToLearners() {
        if (main.vrVideoURI == null && main.vrVideoPath == null) {
            Toast.makeText(main, "A video has not been selected", Toast.LENGTH_SHORT).show();
            return;
        }

        playbackSettingsDialog.dismiss();

        Log.d(TAG, "Launching VR Player for students at: " + startFromTime);

        if(LeadMeMain.isMiUiV9()) {
            //setting the playback video controller
            setupVideoPreview(controllerVideoView);
            controllerVideoView.setVideoPath(main.vrVideoPath);
        } else {
            //setting the playback video controller
            setupVideoPreview(controllerVideoView);
            controllerVideoView.setVideoURI(main.vrVideoURI);
        }

        //LAUNCH THE APPLICATION FROM HERE
        main.getAppManager().launchApp(
                packageName,
                appName,
                false,
                "false",
                true,
                main.getNearbyManager().getSelectedPeerIDsOrAll());

        showPushConfirmed();

        //Set the source for the peers device
        setVideoSource(startFromTime);
    }

    //TIME SETTINGS
    private void setTotalTime(int value) {
        Log.d("SetTotalTime", "Value: " + value);
        if (value > 0) {
            totalTime = value;
            main.runOnUiThread(() -> totalTimeText.setText(intToTime(totalTime)));
        }
    }

    private String intToTime(int duration) {
        return DateUtils.formatElapsedTime(duration);
    }

    private void setCurrentTime(String value) {
        Log.d(TAG, "[GUIDE] Video time is now: " + value + " // " + totalTime);

        //TODO if needed
        int tmpCurr = Integer.parseInt(value);
        if (tmpCurr > -1) {
            currentTime = tmpCurr;
        }
        main.runOnUiThread(() -> {
            elapsedTimeText.setText(intToTime((int) currentTime));
            int progress = Math.round((currentTime / totalTime) * 100);
            progressBar.setProgress(progress);
        });
    }

    private void setNewTime(int newTime) {
        if (totalTime == -1) {
            return;
        }

        //ensure new time is sensible
        if (newTime < 0) {
            newTime = 0;
        } else if (newTime > totalTime && totalTime > 0) {
            newTime = totalTime;
        }

        startFromTime = newTime;

        //update player and local view
        main.runOnUiThread(() -> {
            //set the video at new time (needs to be in ms)
            if(startFromTime == 0) {
                //show the first frame
                controllerVideoView.seekTo(1);
                vrplayerPreviewVideoView.seekTo(1);
            } else {
                controllerVideoView.seekTo(startFromTime * 1000);
                vrplayerPreviewVideoView.seekTo(startFromTime * 1000);
            }
        });

        setCurrentTime("" + newTime);
    }

    //CONTROL FUNCTIONS
    //content://com.android.providers.media.documents/document/video%3A25275 - example URI
    //TODO change appName to video name - set within AppManager
    //Used when repushing the application as the appName will already be set
    public void showPlaybackPreview() {
        openPreview(appName);
    }

    public void showPlaybackPreview(String title) {
        openPreview(title);
    }

    private void openPreview(String title) {
        if(main.vrVideoURI != null || main.vrVideoPath != null) {
            if(LeadMeMain.isMiUiV9()) {
                //setting the playback video controller
                setupVideoPreview(vrplayerPreviewVideoView);
                controllerVideoView.setVideoPath(main.vrVideoPath);
            } else {
                //setting the playback video controller
                setupVideoPreview(vrplayerPreviewVideoView);
                controllerVideoView.setVideoURI(main.vrVideoURI);
            }
        }

        main.runOnUiThread(() -> {
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

        noVideoChosen(fileName == null);

        if(firstOpen) {
            disableSetSourceBtn(fileName != null);
        } else {
            disableSetSourceBtn(firstOpen);
        }

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
            main.getDialogManager().hideConfirmPushDialog();
            showVideoController();
        });
    }

    /**
     * Disables or enables the push button depending on if the user has selected a video.
     * Hides or shows the no chosen video element. Tells the user that nothing has been selected.
     * @param show A boolean representing if the no video selected layout should be shown.
     */
    private void noVideoChosen(boolean show) {
        vrplayerPreviewPushBtn.setVisibility(show ? View.GONE : View.VISIBLE);
        vrplayerNoVideo.setVisibility(show ? View.VISIBLE : View.GONE);
        vrplayerVideoControls.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Launches the custom VR Player.
     * @param peerSet A set of strings representing the learner ID's to send the action to.
     */
    public void launchVR(Set<String> peerSet) {
        main.getAppManager().launchApp(packageName, appName, false, "false", true, peerSet);
    }

    /**
     * Relaunches the last VR experience with the selected video source.
     * @param peerSet A set of strings representing the learner ID's to send the action to.
     */
    public void relaunchVR(Set<String> peerSet) {
        main.getAppManager().launchApp(packageName, appName, false, "false", true, peerSet);
        setVideoSource(startFromTime);
    }

    private void setupGuideVideoControllerButtons() {
        videoControllerDialogView.findViewById(R.id.push_again_btn).setOnClickListener(v ->
            relaunchVR(main.getNearbyManager().getSelectedPeerIDsOrAll())
        );

        videoControllerDialogView.findViewById(R.id.new_video_btn).setOnClickListener(v -> {
            resetControllerState();
            videoControlDialog.dismiss();
            this.fileName = null;
            firstOpen = true;
            showPlaybackPreview(appName);
            selectSource(); //go straight to file picker
        });

        videoControllerDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v -> {
            resetControllerState();
            hideVideoController();
            firstOpen = false;
        });

        videoControllerDialogView.findViewById(R.id.play_btn).setOnClickListener(v ->
            playVideo()
        );

        videoControllerDialogView.findViewById(R.id.pause_btn).setOnClickListener(v ->
            pauseVideo()
        );

        videoControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v ->
            main.muteLeaners()
        );

        videoControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v ->
            main.unmuteLearners()
        );
    }

    /**
     * Opens the video controller for the custom VR player. Only available if the video path has
     * already been set/saved in the LeadMe main.
     */
    public void openVideoController() {
        controllerVideoView.seekTo(1); //to not show a black screen
        showVideoController();
    }

    private void showVideoController() {
        main.runOnUiThread(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(videoControllerDialogView)
                    .create();

            videoControlDialog.setCancelable(false);

            videoControlDialog.setOnDismissListener(dialog -> {
                main.hideSystemUI();
                //resetControllerState(); //reset here?
            });
        }
        //viewModeToggle.setChecked(true);

        Log.d(TAG, "Attempting to show video controller for VR player");
        videoControlDialog.show();
    }

    private void hideVideoController() {
        main.runOnUiThread(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });
//        pauseVideo();
        videoControlDialog.dismiss();
    }

    private void resetControllerState() {
        stopVideo();

        currentTime = 0;
        totalTime = -1;
        startFromTime = 0;
        progressBar.setProgress(0);
        playFromTime.setText(R.string.zero_seconds);
        elapsedTimeText.setText(R.string.zero_seconds);

        buttonHighlights(VRAccessibilityManager.CUE_PAUSE);
    }

    //VR Player Controls
    //changes the highlights of the buttons, controls both the local video and
    //sends an action to the connected peers.
    //Enhancement - sync time will peers each button press??
    private void setVideoSource(int startTime) {
        //Send action to peers to play
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_SET_SOURCE + ":" + fileName + ":" + startTime,
                main.getNearbyManager().getSelectedPeerIDsOrAll());
    }

    private void playVideo() {
        //Play local video
        controllerVideoView.start();
        buttonHighlights(VRAccessibilityManager.CUE_PLAY);

        //Send action to peers to play
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_PLAY,
                main.getNearbyManager().getSelectedPeerIDsOrAll());
    }

    private void pauseVideo() {
        //Play local video
        controllerVideoView.pause();
        buttonHighlights(VRAccessibilityManager.CUE_PAUSE);

        //Send action to peers to pause
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_PAUSE,
                main.getNearbyManager().getSelectedPeerIDsOrAll());
    }

    //BELOW NOT IMPLEMENTED YET
    private void stopVideo() {
        //Stop local video
        vrplayerPreviewVideoView.stopPlayback();
        controllerVideoView.stopPlayback();

//        buttonHighlights(VRAccessibilityManager.CUE_STOP);
//        //stop();
//
//        //Send action to peers to stop
//        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
//                LeadMeMain.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_STOP,
//                main.getNearbyManager().getSelectedPeerIDsOrAll());
    }

    private void fwdVideo() {
        //Fastforward local video
        buttonHighlights(VRAccessibilityManager.CUE_FWD);
        //fwd();

        //Send action to peers to fastforward
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_FWD,
                main.getNearbyManager().getSelectedPeerIDsOrAll());
    }

    private void rwdVideo() {
        //Rewind local video
        buttonHighlights(VRAccessibilityManager.CUE_RWD);
        //rwd();

        //Send action to peers to rewind
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_RWD,
                main.getNearbyManager().getSelectedPeerIDsOrAll());
    }

    //change video control icon colour
    private void buttonHighlights(int state) {
        switch(state) {
            case VRAccessibilityManager.CUE_PLAY:
                playBtn.setImageResource(R.drawable.vid_play_highlight);
                pauseBtn.setImageResource(R.drawable.vid_pause);
                break;
            case VRAccessibilityManager.CUE_PAUSE:
                playBtn.setImageResource(R.drawable.vid_play);
                pauseBtn.setImageResource(R.drawable.vid_pause_highlight);
                break;
            case VRAccessibilityManager.CUE_STOP:
                //change the highlights
                Log.d(TAG, "Stopping video");
                break;
            case VRAccessibilityManager.CUE_FWD:
                //change the highlights
                Log.d(TAG, "Forwarding video");
                break;
            case VRAccessibilityManager.CUE_RWD:
                //change the highlights
                Log.d(TAG, "Rewinding video");
                break;
            default:
                Log.d(TAG, "Unknown video state");
                break;
        }
    }
}

