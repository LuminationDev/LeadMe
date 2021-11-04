package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.VideoView;

/*
* Operation flow
* Open a file picker to select a video - returning the path or uri
* Display the videoview preview screen to select a starting point
* Dispatch an action to open the VR player
* (Peer device) VRAccessilibityManager sends intent with the selected path to the app
* Opens the video controller, buttons dispatch actions
* (Peer device) sends intent to the VR app depending on the action received
*/
public class VREmbedPlayer {
    private final static String TAG = "embedPlayerVR";

    public final static String packageName = "com.Edward.VRPlayer";
    private String appName;
    private Uri filepath;
    private String fileName;

    private AlertDialog videoControlDialog, playbackSettingsDialog;

    private final View videoControllerDialogView, videoControls;
    private final VideoView controllerVideoView;

    private TextView vrplayerPreviewTitle;
    private Button vrplayerPreviewPushBtn;
    private VideoView vrplayerPreviewVideoView;
    private View vrplayerSettingsDialogView;
    private View vrplayerVideoControls;
    private MediaPlayer mMediaPlayer;

    private View lockSpinnerParent;
    private Spinner lockSpinner;
    private String[] lockSpinnerItems;
    private TextView playFromTime;

    private TextView totalTimeText, elapsedTimeText;
    private int totalTime = -1;
    private float currentTime = 0;
    private int startFromTime = 0;
    private SeekBar progressBar;

    private ImageView playBtn, pauseBtn;

    boolean lastLockState = true;
    boolean selectedOnly = false;
    int lastStartFrom = 1;

    private LeadMeMain main;

    public VREmbedPlayer(LeadMeMain main) {
        this.main = main;

        videoControllerDialogView = View.inflate(main, R.layout.f__playback_control_vrplayer, null);
        videoControls = videoControllerDialogView.findViewById(R.id.video_controls);

        controllerVideoView = videoControllerDialogView.findViewById(R.id.video_stream_videoview);
        controllerVideoView.setTag("CONTROLLER");

        playBtn = videoControllerDialogView.findViewById(R.id.play_btn);
        pauseBtn = videoControllerDialogView.findViewById(R.id.pause_btn);

        createPlaybackSettingsPopup();
        setupGuideVideoControllerButtons();

        //TODO move this to a function which is called when new video is selected as well
        //listener to manage when a video has loaded properly
        vrplayerPreviewVideoView.setOnPreparedListener(mp -> {
            mMediaPlayer = mp;
            int duration = mp.getDuration();
            int videoDuration = vrplayerPreviewVideoView.getDuration()/1000;
            Log.d(TAG, String.format("onPrepared: (ms)duration=%d, (s)videoDuration=%d", duration,
                    videoDuration));
            setTotalTime(videoDuration);
        });
    }

    /**
     * Takes a URI and sets the video preview for the EmbedPlayer, sets one for the preview and
     * one for the control. Separated for ease of use and customisation in the future.
     * @param path A URI of the content that is going to be played.
     */
    public void setFilepath(Uri path) {
        this.filepath = path;
        this.fileName = getFileName(path);


        //For testing purposes
        Log.e(TAG, String.valueOf(filepath));
        Log.e(TAG, getFileName(path));

        //In case a file is not chosen or there is an error
        if(this.filepath == null || this.fileName == null) {
            Log.e(TAG, "File is missing or path is incorrect");
            return;
        }

        //setting the preview video
        setupVideoPreview(vrplayerPreviewVideoView);

        //setting the playback video
        setupVideoPreview(controllerVideoView);
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
        video.setVideoURI(filepath);
        video.setZOrderOnTop(true);
        //display the first frame instead of black space
        video.seekTo(1);
    }

    //Sets up the UI for selecting where to start the video from.
    private void createPlaybackSettingsPopup() {
        vrplayerSettingsDialogView = View.inflate(main, R.layout.f__playback_settings_vrplayer, null);
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

        vrplayerPreviewPushBtn.setOnClickListener(v -> main.getHandler().post(() -> {
            playbackSettingsDialog.dismiss();
            lastLockState = lockSpinner.getSelectedItem().toString().startsWith("View");

            Log.d(TAG, "Launching VR Player for students at: " + startFromTime);

            //LAUNCH THE APPLICATION FROM HERE
            main.getAppManager().launchApp(packageName, appName, false);
            showPushConfirmed();

            //TODO wait until the application is open
            //Set the source for the peers device
            setVideoSource(startFromTime);
        }));

        lockSpinnerParent = vrplayerSettingsDialogView.findViewById(R.id.lock_spinner);
        lockSpinner = (Spinner) vrplayerSettingsDialogView.findViewById(R.id.push_spinner);
        lockSpinnerItems = new String[2];
        lockSpinnerItems[0] = "View only";
        lockSpinnerItems[1] = "Free play";
        Integer[] push_imgs = {R.drawable.controls_view, R.drawable.controls_play};
        LumiSpinnerAdapter push_adapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, lockSpinnerItems, push_imgs);
        lockSpinner.setAdapter(push_adapter);
        lockSpinner.setSelection(0); //default to locked
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
        //Put a uri for the video memory here later
        Log.d(TAG, "FileUtilities: picking a file");
        main.getFileUtilities().browseFiles(LeadMeMain.VR_FILE_CHOICE);

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
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPopup.dismiss();
                //return to main screen
                main.getDialogManager().hideConfirmPushDialog();
                showVideoController();
            }
        });
    }

    private void setupGuideVideoControllerButtons() {
        videoControllerDialogView.findViewById(R.id.push_again_btn).setOnClickListener(v -> {
            main.getAppManager().launchApp(packageName, appName, false);
            //TODO wait until the application is open
            setVideoSource(startFromTime);
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

//        if (vrModeBtn != null && vrModeBtn.isChecked()) {
//            vrModeBtn.setChecked(false); //toggle it
//        }
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

