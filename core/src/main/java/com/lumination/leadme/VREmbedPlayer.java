package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.BoardiesITSolutions.FileDirectoryPicker.OpenFilePicker;

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
    public final static String packageName = "com.Edward.VRPlayer";

    private String appName;
    private String fileName;

    private AlertDialog videoControlDialog, playbackSettingsDialog;

    private final View videoControllerDialogView; //, videoControls
    private final VideoView controllerVideoView;

    private TextView vrplayerPreviewTitle;
    private Button vrplayerPreviewPushBtn, vrplayerSetSourceBtn;
    private VideoView vrplayerPreviewVideoView;
    private View vrplayerSettingsDialogView;
    private View vrplayerVideoControls;

    private TextView playFromTime;

    private TextView totalTimeText, elapsedTimeText;
    private int totalTime = -1;
    private float currentTime = 0;
    private int startFromTime = 0;
    private SeekBar progressBar;

    private ImageView playBtn, pauseBtn;

    boolean selectedOnly = false;

    Switch viewModeToggle;

    private LeadMeMain main;

    public VREmbedPlayer(LeadMeMain main) {
        this.main = main;

        videoControllerDialogView = View.inflate(main, R.layout.f__playback_control_vrplayer, null);
//        videoControls = videoControllerDialogView.findViewById(R.id.video_controls);

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
        File f = new File(path);
        Log.e(TAG,"File name is: " + f.getName());

        this.fileName = f.getName(); //get file name

        //For testing purposes
        Log.e(TAG, path);
        Log.e(TAG, this.fileName);

        //In case a file is not chosen or there is an error
        if(path == null || this.fileName == null) {
            Log.e(TAG, "File is missing or path is incorrect");
            return;
        }

        vrplayerPreviewVideoView.setVideoPath(path);

        //setting the preview video
        setupVideoPreview(vrplayerPreviewVideoView);
    }

    /**
     * Takes a URI and sets the video preview for the EmbedPlayer, sets one for the preview and
     * one for the control. Separated for ease of use and customisation in the future.
     * @param path A URI of the content that is going to be played.
     */
    public void setFilepath(Uri path) {
        this.fileName = getFileName(path);

        //For testing purposes
        Log.e(TAG, String.valueOf(path));
        Log.e(TAG, this.fileName);

        //In case a file is not chosen or there is an error
        if(path == null || this.fileName == null) {
            Log.e(TAG, "File is missing or path is incorrect");
            return;
        }

        vrplayerPreviewVideoView.setVideoURI(path);

        //setting the preview video
        setupVideoPreview(vrplayerPreviewVideoView);
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

        viewModeToggle = videoControllerDialogView.findViewById(R.id.view_mode_toggle);
        viewModeToggle.setChecked(true);
        viewModeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    viewModeToggle.setText("View Mode ON");
                    ImageViewCompat.setImageTintList(videoControllerDialogView.findViewById(R.id.view_mode_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_blue)));
                    main.lockFromMainAction();
                }else{
                    ImageViewCompat.setImageTintList(videoControllerDialogView.findViewById(R.id.view_mode_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_medium_grey)));
                    viewModeToggle.setText("View Mode OFF");
                    main.unlockFromMainAction();
                }
            }
        });
    }

    //Sets up the UI for selecting where to start the video from.
    private void createPlaybackSettingsPopup() {
        vrplayerSettingsDialogView = View.inflate(main, R.layout.f__playback_settings_vrplayer, null);
        vrplayerSetSourceBtn = vrplayerSettingsDialogView.findViewById(R.id.set_source_btn);
        vrplayerPreviewPushBtn = vrplayerSettingsDialogView.findViewById(R.id.vr_push_btn);
        vrplayerPreviewTitle = vrplayerSettingsDialogView.findViewById(R.id.preview_title);
        vrplayerVideoControls = vrplayerSettingsDialogView.findViewById(R.id.video_controls);
        progressBar = vrplayerSettingsDialogView.findViewById(R.id.progressBar);
        playFromTime = vrplayerSettingsDialogView.findViewById(R.id.video_play_from_input);
        elapsedTimeText = vrplayerSettingsDialogView.findViewById(R.id.elapsedTimeText);
        totalTimeText = vrplayerSettingsDialogView.findViewById(R.id.totalTimeText);
        vrplayerPreviewVideoView = vrplayerSettingsDialogView.findViewById(R.id.video_stream_videoview);
        vrplayerPreviewVideoView.setTag("PREVIEW/PLAYBACK SETTINGS");
        setupPushToggle();
        vrplayerSettingsDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v ->
                playbackSettingsDialog.dismiss()
        );


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

        vrplayerSetSourceBtn.setOnClickListener(v -> {
            Log.d(TAG, "FileUtilities: picking a file");
            if(LeadMeMain.isMiUiV9()) {
                main.alternateFileChoice(LeadMeMain.VR_FILE_CHOICE);
            } else {
                FileUtilities.browseFiles(main, LeadMeMain.VR_FILE_CHOICE);
            }
        });

        vrplayerPreviewPushBtn.setOnClickListener(v -> main.getHandler().post(() -> {
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
            main.getAppManager().launchApp(packageName, appName, false, "false", true, main.getNearbyManager().getSelectedPeerIDsOrAll());
            showPushConfirmed();

            //Set the source for the peers device
            setVideoSource(startFromTime);
        }));
    }

    //TIME SETTINGS
    private void setTotalTime(int value) {
        Log.e("SetTotalTime", "Value: " + value);
        if (value > 0) {
            totalTime = value;
            main.runOnUiThread(() -> {
                totalTimeText.setText(intToTime(totalTime));
            });
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
            Log.e(TAG, "Nope. No good.");
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
        appName = title;
        openPreview(appName);
    }

    private void openPreview(String title) {
        if(main.vrVideoURI != null) {
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

        Log.d(TAG, "showPlaybackPreview: " + title);

        main.runOnUiThread(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        vrplayerPreviewTitle.setText(title);
        vrplayerVideoControls.setVisibility(View.VISIBLE);

        if (playbackSettingsDialog == null) {
            playbackSettingsDialog = new AlertDialog.Builder(main)
                    .setView(vrplayerSettingsDialogView)
                    .show();
            playbackSettingsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });
        } else {
            playbackSettingsDialog.show();
        }
    }

    private void showPushConfirmed() {
        playbackSettingsDialog.dismiss();
        View confirmPushDialogView = View.inflate(main, R.layout.e__confirm_popup, null);
        AlertDialog confirmPopup = new AlertDialog.Builder(main)
                .setView(confirmPushDialogView)
                .show();
        ((TextView)confirmPushDialogView.findViewById(R.id.push_success_comment)).setText("Your video was successfully launched.");
        Button ok = confirmPushDialogView.findViewById(R.id.ok_btn);
        ok.setOnClickListener(v -> {
            confirmPopup.dismiss();
            //return to main screen
            main.getDialogManager().hideConfirmPushDialog();
            showVideoController();
        });
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
        videoControllerDialogView.findViewById(R.id.push_again_btn).setOnClickListener(v -> {
            relaunchVR(main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

        videoControllerDialogView.findViewById(R.id.push_btn).setOnClickListener(v -> {
            resetControllerState();
            showPlaybackPreview(this.appName);
        });

        videoControllerDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v -> {
            resetControllerState();
            hideVideoController();
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
            videoControlDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                    //resetControllerState(); //reset here?
                }
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

    private void setupPushToggle() {
        Button leftToggle = vrplayerSettingsDialogView.findViewById(R.id.selected_btn);
        Button rightToggle = vrplayerSettingsDialogView.findViewById(R.id.everyone_btn);
        leftToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedOnly=true;
                vrplayerSettingsDialogView.findViewById(R.id.everyone_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left_white, null));
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.everyone_btn)).setElevation(Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 5,main.getResources().getDisplayMetrics())));
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.selected_btn)).setElevation(0);
                vrplayerSettingsDialogView.findViewById(R.id.selected_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right, null));
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);
                vrplayerPreviewPushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));

            }
        });
        rightToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedOnly=false;
                vrplayerSettingsDialogView.findViewById(R.id.everyone_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left, null));
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.selected_btn)).setElevation(Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 5,main.getResources().getDisplayMetrics())));

                vrplayerSettingsDialogView.findViewById(R.id.selected_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right_white, null));
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                vrplayerPreviewPushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone));
                ((Button) vrplayerSettingsDialogView.findViewById(R.id.everyone_btn)).setElevation(0);
            }
        });
        rightToggle.callOnClick();
    }

    private void resetControllerState() {
        Log.e(TAG, "Resetting controller!!");
        stopVideo();

        currentTime = 0;
        totalTime = -1;
        startFromTime = 0;
        progressBar.setProgress(0);
        playFromTime.setText("00:00");
        elapsedTimeText.setText("00:00");

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
                break;
            case VRAccessibilityManager.CUE_FWD:
                //change the highlights
                break;
            case VRAccessibilityManager.CUE_RWD:
                //change the highlights
                break;
            default:
                Log.e(TAG, "Unknown video state");
                break;
        }
    }
}

