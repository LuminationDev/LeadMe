package com.lumination.leadme.players;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.net.http.SslError;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.accessibility.VRAccessibilityManager;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.managers.FavouritesManager;
import com.lumination.leadme.managers.NearbyPeersManager;
import com.lumination.leadme.managers.SearchManager;
import com.lumination.leadme.managers.WebManager;

import java.io.InputStream;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
 * Operation flow
 * Paste or type in a link - select set as source
 * Display the VideoView preview screen to select a starting point
 * Dispatch an action to open the VR player
 * (Peer device) VRAccessibilityManager sends intent with the selected path to the app
 * Opens the video controller, buttons dispatch actions
 * (Peer device) sends intent to the VR app depending on the action received
 */
public class VREmbedLinkPlayer {
    private final static String TAG = "embedPlayerVR";
    //Package name of the external VR player
    public final static String packageName = "com.lumination.VRPlayer";
    private final String appName = "VRPlayer"; //Past to the app manager

    private String attemptedURL = null;

    private AlertDialog videoControlDialog, playbackSettingsDialog;

    private final View videoControllerDialogView; //, videoControls
    private final WebView controllerWebView;

    private PopupWindow popupWindow;
    private ImageView changeProjectionBtn;
    private TextView monoText, eacText, eac3dText, ouText, sbsText;
    private final TextView internetUnavailableMsg;

    private LinearLayout vrPlayerUrlArea;
    private EditText vrplayerLinkInput;
    private Button vrplayerPreviewPushBtn, vrplayerSetSourceBtn;
    private final WebView vrplayerPreviewVideoView;
    private final View vrplayerSettingsDialogView, vrLoadingBar, vrVideoControls;
    private View vrplayerVideoControls;

    private TextView playFromTime, totalTimeText, elapsedTimeText;
    private int totalTime = -1;
    private float currentTime = 0;
    private int startFromTime = 1;
    private SeekBar progressBar;
    boolean pageLoaded = false;
    boolean isYoutube = false;

    private boolean firstTouch; //track if the guide has started the video
    private final ImageView playBtn, pauseBtn;

    Switch viewModeToggle;

    private final LeadMeMain main;
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "ClickableViewAccessibility"})
    public VREmbedLinkPlayer(LeadMeMain main) {
        this.main = main;

        //Preview webView
        vrplayerSettingsDialogView = View.inflate(main, R.layout.f__playback_settings_vr_linkplayer, null);
        vrplayerPreviewVideoView = vrplayerSettingsDialogView.findViewById(R.id.video_stream_videoview);
        vrplayerPreviewVideoView.setTag("PREVIEW/PLAYBACK SETTINGS");
        vrplayerPreviewVideoView.setWebChromeClient(new WebChromeClient());
        vrplayerPreviewVideoView.getSettings().setJavaScriptEnabled(true); // enable javascript
        vrplayerPreviewVideoView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        vrplayerPreviewVideoView.addJavascriptInterface(this, "Android");

        vrplayerSettingsDialogView.findViewById(R.id.open_favourites).setOnClickListener(view ->
            Controller.getInstance().getFavouritesManager().launchUrlYtFavourites(FavouritesManager.LAUNCHTYPE_WEB)
        );

        //Web search
        createPlaybackDialog();
        new SearchManager(main, vrplayerSettingsDialogView, playbackSettingsDialog, this::openPreview);

        //Playback webView
        videoControllerDialogView = View.inflate(main, R.layout.f__playback_control_vr_linkplayer, null);
        vrLoadingBar = videoControllerDialogView.findViewById(R.id.video_loading);
        vrVideoControls = videoControllerDialogView.findViewById(R.id.video_controls);
        controllerWebView = videoControllerDialogView.findViewById(R.id.video_stream_videoview);
        controllerWebView.setTag("CONTROLLER");
        controllerWebView.setWebChromeClient(new WebChromeClient());
        controllerWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        controllerWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        controllerWebView.addJavascriptInterface(this, "Android");
        controllerWebView.setOnTouchListener((v, event) -> {
            //Clicking the player the first time will enable the buttons to work
            if(firstTouch) {
                buttonHighlights(VRAccessibilityManager.CUE_PLAY);
                playVideo();
            }
            return !firstTouch;
        });

        internetUnavailableMsg = videoControllerDialogView.findViewById(R.id.no_internet);
        internetUnavailableMsg.setOnClickListener(v -> loadVideoGuideURL(attemptedURL, vrplayerPreviewVideoView));

        playBtn = videoControllerDialogView.findViewById(R.id.play_btn);
        pauseBtn = videoControllerDialogView.findViewById(R.id.pause_btn);

        setupProjectionDropdown();

        createPlaybackSettingsPopup();

        setupGuideVideoControllerWebClient(controllerWebView);
        setupGuideVideoControllerWebClient(vrplayerPreviewVideoView);

        setupGuideVideoControllerButtons();
    }

    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(main, toast, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupGuideVideoControllerWebClient(WebView player) {
        player.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //we shouldn't be navigating away once a video is loaded, so block this
                if (pageLoaded) {
                    //this will pause the video, so if it should
                    //be playing, play it again
                    if (videoCurrentPlayState == PLAYING) {
                        buttonHighlights(VRAccessibilityManager.CUE_PLAY);
                        playVideo();
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            public void onLoadResource(WebView view, String url) {
            }

            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                pauseVideo(); //stop it cleanly
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.d(TAG, "VIDEO GUIDE] Received error: " + error.toString());
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.d(TAG, "VIDEO GUIDE] Received HTTP error: " + errorResponse.toString());
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                Log.d(TAG, "VIDEO GUIDE] Received SSL error: " + error.toString());
            }
        });
    }

    /**
     * Setup the projection dropdown by adding onclick listeners for each of the
     * projection types available.
     */
    private void setupProjectionDropdown() {
        changeProjectionBtn = videoControllerDialogView.findViewById(R.id.change_projection_btn);
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

        eacText = projectionDropdown.findViewById(R.id.eac_text);
        LinearLayout eacBtn = projectionDropdown.findViewById(R.id.eac_toggle);
        eacText.setVisibility(View.VISIBLE);
        eacBtn.setVisibility(View.VISIBLE);
        eacBtn.setOnClickListener(v -> {
            changeProjection("eac");
            changeSelectedProjection(eacText);
        });

        eac3dText = projectionDropdown.findViewById(R.id.eac3d_text);
        LinearLayout eac3dBtn = projectionDropdown.findViewById(R.id.eac3d_toggle);
        eac3dText.setVisibility(View.VISIBLE);
        eac3dBtn.setVisibility(View.VISIBLE);
        eac3dBtn.setOnClickListener(v -> {
            changeProjection("eac3d");
            changeSelectedProjection(eac3dText);
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
     * @param URL A String of the content that is going to be played.
     */
    public void setAttemptedURL(String URL) {
        attemptedURL = URL;

        //hide the choose video button if fileName is null - first time choice?
        disableSetSourceBtn(true);

        //For testing purposes
        Log.d(TAG, "Attempting to show video controller for " + attemptedURL);

        //In case a file is not chosen or there is an error
        if(attemptedURL == null) {
            Log.e(TAG, "Link is missing or path is incorrect");
            return;
        }

        //Load the link into the WebView
        loadVideoGuideURL(attemptedURL, vrplayerPreviewVideoView);

        noVideoChosen(false);

        //setting the preview video
        setupVideoPreview(vrplayerPreviewVideoView);
    }

    private void loadVideoGuideURL(String url, WebView video) {
        Log.d(TAG, "loadVideoGuideURL: 1");

        if (Controller.getInstance().getPermissionsManager().isInternetConnectionAvailable()) {
            internetUnavailableMsg.setVisibility(View.GONE);

            //Switch between streaming domains
            if(isYoutubeUrl(url)) {
                isYoutube = true;
                attemptedURL = embedYouTubeURL(url);
                video.loadDataWithBaseURL(null, getYoutubeiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
            } else {
                isYoutube = false;
                firstTouch = false; //Do not have to interact with the screen before buttons can be activated (Youtube requires this)
                attemptedURL = url;
                video.loadDataWithBaseURL(null, getVimeoiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
            }

            Log.w(TAG, "Loading webview for: " + attemptedURL + ", " + video.getTag());
        } else {
            internetUnavailableMsg.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "loadVideoGuideURL: 2");
    }

    /**
     * Determine if the supplied URL is of a youtube domain.
     * @param youTubeURl A string of the URL to be checked.
     * @return A boolean representing if the URL is of youtube origin.
     */
    private static boolean isYoutubeUrl(String youTubeURl) {
        boolean success;
        String pattern = "^(http(s)?:\\/\\/)?((w){3}.)?youtu(be|.be)?(\\.com)?\\/.+";

        // Not Valid youtube URL
        success = !youTubeURl.isEmpty() && youTubeURl.matches(pattern);
        return success;
    }

    /**
     * If the user has just selected a video for viewing hide the choose video button.
     */
    private void disableSetSourceBtn(boolean disable) {
        vrplayerSetSourceBtn.setVisibility(disable ? View.GONE : View.VISIBLE);
    }

    /**
     * Sets the video source and moves it to the top of the UI as some phones will display it behind
     * the pop up dialog.
     */
    private void setupVideoPreview(WebView video) {
        if(startFromTime != 1) {
            LeadMeMain.runOnUI(() ->
                    video.loadUrl("javascript:seekTo(\"" + attemptedURL + "\", " + startFromTime + ")")
            );
        } else {
            //display the first frame instead of black space
            LeadMeMain.runOnUI(() ->
                    video.loadUrl("javascript:seekTo(\"" + attemptedURL + "\", " + 1 + ")")
            );
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
        vrPlayerUrlArea = vrplayerSettingsDialogView.findViewById(R.id.url_entry_view);
        vrplayerLinkInput = vrplayerSettingsDialogView.findViewById(R.id.url_input_field);
        vrplayerSetSourceBtn = vrplayerSettingsDialogView.findViewById(R.id.set_source_btn);
        vrplayerPreviewPushBtn = vrplayerSettingsDialogView.findViewById(R.id.vr_push_btn);
        vrplayerVideoControls = vrplayerSettingsDialogView.findViewById(R.id.video_controls);
        progressBar = vrplayerSettingsDialogView.findViewById(R.id.progressBar);
        playFromTime = vrplayerSettingsDialogView.findViewById(R.id.video_play_from_input);
        elapsedTimeText = vrplayerSettingsDialogView.findViewById(R.id.elapsedTimeText);
        totalTimeText = vrplayerSettingsDialogView.findViewById(R.id.totalTimeText);
        Controller.getInstance().getDialogManager().setupPushToggle(vrplayerSettingsDialogView, false);

        vrplayerSettingsDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v -> {
            this.attemptedURL = null;
            playbackSettingsDialog.dismiss();
            Controller.getInstance().getDialogManager().showVRContentDialog();
        });

        if(!isYoutube) {
            playFromTime.setText("0:01");
        }

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
                setNewTime(durationCalc, vrplayerPreviewVideoView);
                playFromTime.setText(intToTime(durationCalc));
            }
        });

        //Setup the paste into text box button
        vrplayerSettingsDialogView.findViewById(R.id.paste_from_clipboard).setOnClickListener(v -> {
            main.closeKeyboard();
            main.hideSystemUI();
            ClipboardManager clipboard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence pasteData;

            //if it does contain data, test if we can handle it
            if (!clipboard.hasPrimaryClip()) { return; }

            try {
                pasteData = clipboard.getPrimaryClip().getItemAt(0).getText(); //retrieve the data
            } catch (Exception e) { return; }

            //puts the pasted data into the URL field
            vrplayerLinkInput.setText(pasteData);
        });

        vrplayerSetSourceBtn.setOnClickListener(view -> {
            String url = vrplayerLinkInput.getText().toString();
            if(url.equals("")) return;
            setAttemptedURL(url);
        });

        vrplayerPreviewPushBtn.setOnClickListener(view -> {
            firstTouch = true;

            LeadMeMain.UIHandler.post(this::pushToLearners);

            //VR initial default is always set to eac on VR player side
            changeSelectedProjection(eacText);
        });
    }

    /**
     * Check that the source is not null and push to the appropriate learners. Determines if the
     * device needs a Uri or an absolute path for the playback controller.
     */
    private void pushToLearners() {
        if (attemptedURL == null) {
            Toast.makeText(main, "A link has not been supplied", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Launching VR Player for students at: " + startFromTime);

        if(!isYoutube) {
            //Clear any timers that my be present from previous events
            WebView video = vrplayerPreviewVideoView.findViewById(R.id.video_stream_videoview);
            video.loadUrl("javascript:clearTimer();");
        }

        //setting the playback video controller
        setupVideoPreview(controllerWebView);

        //Load the URL on the playback
        loadVideoGuideURL(attemptedURL, controllerWebView);

        //LAUNCH THE APPLICATION FROM HERE
        Controller.getInstance().getAppManager().launchApp(
                packageName,
                appName,
                false,
                "false",
                true,
                NearbyPeersManager.getSelectedPeerIDsOrAll());

        resetVrScene();
        playbackSettingsDialog.dismiss();

        main.showLeaderScreen();
        showPushConfirmed();
    }

    //TIME FUNCTIONS
    /**
     * Determine if the integer supplied is a valid time and set the startFromTime
     * variable accordingly.
     * @param newTime An integer representing where the user has moved the preview slider to.
     */
    private void setNewTime(int newTime, WebView video) {
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
        final int finalData = newTime;
        startFromTime = newTime;

        //update player and local view
        LeadMeMain.runOnUI(() ->
                video.loadUrl("javascript:seekTo(\"" + attemptedURL + "\", " + finalData + ")")
        );

        setCurrentTime("" + newTime);
    }

    public String getYoutubeiFrameForURL(String url) {
        String embedID = WebManager.getYouTubeID(url);

        InputStream htmlTemplate = main.getResources().openRawResource(R.raw.embed_yt_player);
        Scanner scanner = new Scanner(htmlTemplate);
        StringBuilder output = new StringBuilder();
        while (scanner.hasNext()) {
            output.append(scanner.nextLine()).append("\n");
        }
        String startTime = extractTime(url);
        if (startTime.isEmpty()) {
            startTime = "0";
        }
        output = new StringBuilder(output.toString().replace("PLACEHOLDER_ID", embedID));
        output = new StringBuilder(output.toString().replace("PLACEHOLDER_START", startTime));

        return output.toString();
    }

    public String getVimeoiFrameForURL(String url) {
        InputStream htmlTemplate = main.getResources().openRawResource(R.raw.embed_vimeo_player);
        Scanner scanner = new Scanner(htmlTemplate);
        StringBuilder output = new StringBuilder();

        while (scanner.hasNext()) {
            output.append(scanner.nextLine()).append("\n");
        }

        output = new StringBuilder(output.toString().replace("PLACEHOLDER_SOURCE", url));

        return output.toString();
    }

    private String extractTime(String url) {
        int startIndex = url.indexOf("start=");

        if (startIndex == -1) {
            return "";
        } else {
            startIndex += 6; //for length of searched string
        }
        int endIndex = url.indexOf("&", startIndex);
        if (endIndex == -1) {
            endIndex = url.length();
        }
        return url.substring(startIndex, endIndex);
    }

    public String embedYouTubeURL(String url) {
        String id = WebManager.getYouTubeID(url);
        //Log.w(TAG, "YouTube ID = " + id + " from " + url);
        if (id.isEmpty()) {
            return "";
        }
        String startSubstring;
        if (url.contains("&start=")) {
            String val = extractTime(url);
            startSubstring = "?start=" + val + "&t=" + val;

        } else if (currentTime > 0) {
            startSubstring = "?start=" + (int) currentTime + "&t=" + (int) currentTime;

        } else {
            startSubstring = "?t=1";
        }

        //Log.d(TAG, "Final URL: " + finalURL + "(from " + url + ", " + currentTime + ")");
        return "https://www.youtube.com/embed/" + id + startSubstring;
    }

    @JavascriptInterface
    public void setCurrentTime(String value) {
        Log.d(TAG, "[GUIDE] Video time is now: " + value + " // " + totalTime);

        int tmpCurr = Integer.parseInt(value);
        if (tmpCurr > -1) {
            currentTime = tmpCurr;
        }
        LeadMeMain.runOnUI(() -> {
            elapsedTimeText.setText(intToTime((int) currentTime));
            int progress = Math.round((currentTime / totalTime) * 100);
            progressBar.setProgress(progress);
        });
    }

    private String intToTime(int duration) {
        return DateUtils.formatElapsedTime(duration);
    }

    @JavascriptInterface
    public void setTotalTime(String value) {
        int tmpTotal = Integer.parseInt(value);
        if (tmpTotal > 0) {
            //Log.d(TAG, "[GUIDE] TOTAL time is now: " + value + " // " + attemptedURL);// + ", " + extractedTime);
            totalTime = tmpTotal;
            LeadMeMain.runOnUI(() -> totalTimeText.setText(intToTime(totalTime)));
        }
    }

    //static variables
    private static final int UNSTARTED = -1;
    private static final int PLAYING = 1;

    private static int videoCurrentPlayState = UNSTARTED;


    @JavascriptInterface
    public void updateState(int state) {
        Log.d(TAG, "[GUIDE] Video state is now: " + state + " // " + currentTime);
        videoCurrentPlayState = state;

        if (state == PLAYING) {
            //if this is the first state switch guide to buttons
            if (firstTouch) {
                firstTouch = false;
                playVideo();
            }
        }
    }

    //CONTROL FUNCTIONS
    //Used when re-pushing the application as the appName will already be set
    public void showPlaybackPreview() {
        openPreview();
    }

    public void openPreview(String url) {
        setAttemptedURL(url);

        playbackSettingsDialog.show();
    }

    private void openPreview() {
        if(attemptedURL != null) {
            //setting the playback video controller
            setupVideoPreview(vrplayerPreviewVideoView);
            loadVideoGuideURL(attemptedURL, controllerWebView);
        }

        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        noVideoChosen(attemptedURL == null);

        disableSetSourceBtn(attemptedURL != null);

        Controller.getInstance().getDialogManager().toggleSelectedView(vrplayerSettingsDialogView);
        playbackSettingsDialog.show();
    }

    /**
     * Create the playback dialog popup.
     */
    private void createPlaybackDialog() {
        if (playbackSettingsDialog == null) {
            playbackSettingsDialog = new AlertDialog.Builder(main)
                    .setView(vrplayerSettingsDialogView)
                    .create();

            playbackSettingsDialog.setCancelable(false);

            playbackSettingsDialog.setOnDismissListener(dialog -> main.hideSystemUI());
        }
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

            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

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
    private void noVideoChosen(boolean show) {
        vrPlayerUrlArea.setVisibility(show ? View.VISIBLE : View.GONE);
        vrplayerPreviewPushBtn.setVisibility(show ? View.GONE : View.VISIBLE);
        vrplayerVideoControls.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Launches the custom VR Player.
     * @param peerSet A set of strings representing the learner ID's to send the action to.
     */
    public void launchVR(Set<String> peerSet) {
        Controller.getInstance().getAppManager().launchApp(packageName, appName, false, "false", true, peerSet);
    }

    /**
     * Relaunches the last VR experience with the selected video source.
     * @param peerSet A set of strings representing the learner ID's to send the action to.
     */
    public void relaunchVR(Set<String> peerSet) {
        Controller.getInstance().getAppManager().launchApp(packageName, appName, false, "false", true, peerSet);
        setVideoSource(startFromTime);
    }

    private void setupGuideVideoControllerButtons() {
        videoControllerDialogView.findViewById(R.id.new_video_btn).setOnClickListener(v -> {
            resetControllerState();
            videoControlDialog.dismiss();
            this.attemptedURL = null;
            vrplayerPreviewVideoView.setVisibility(View.INVISIBLE);
            showPlaybackPreview();
        });

        videoControllerDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v -> {
            resetControllerState();
            hideVideoController();
        });

        playBtn.setOnClickListener(v ->
                playVideo()
        );

        pauseBtn.setOnClickListener(v ->
                pauseVideo()
        );

        //TODO not anchored correctly
        changeProjectionBtn.setOnClickListener(v ->
                popupWindow.showAsDropDown(v,-200,-100)
        );

        videoControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v ->
                DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.VID_MUTE_TAG,
                        NearbyPeersManager.getSelectedPeerIDsOrAll())
        );

        videoControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v ->
                DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.VID_UNMUTE_TAG,
                        NearbyPeersManager.getSelectedPeerIDsOrAll())
        );
    }

    /**
     * Opens the video controller for the custom VR player. Only available if the video path has
     * already been set/saved in the LeadMe main.
     */
    public void openVideoController() {
        LeadMeMain.runOnUI(() ->
                controllerWebView.loadUrl("javascript:seekTo(\"" + attemptedURL + "\", " + startFromTime + ")")
        );
        showVideoController();
    }

    private void showVideoController() {
        LeadMeMain.runOnUI(() -> {
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

        //Set the source for the peers device
        setVideoSource(startFromTime);

        //viewModeToggle.setChecked(true);
        WebView video = videoControllerDialogView.findViewById(R.id.video_stream_videoview);
        LeadMeMain.runOnUI(() ->
            video.loadUrl("javascript:seekTo(\"" + attemptedURL + "\", " + startFromTime + ")")
        );

        Log.d(TAG, "Attempting to show video controller for VR player at time: " + startFromTime);
        videoControlDialog.show();

        //Create a loading spinner to wait while the learner devices reset
        setLoadingSpinner();
    }

    private void hideVideoController() {
        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        videoControlDialog.dismiss();
    }

    private void resetControllerState() {
        this.attemptedURL = null;
        currentTime = 0;
        totalTime = -1;
        startFromTime = 1;
        progressBar.setProgress(0);
        playFromTime.setText(R.string.zero_seconds);
        elapsedTimeText.setText(R.string.zero_seconds);
        firstTouch = true;

        buttonHighlights(VRAccessibilityManager.CUE_PAUSE);
    }

    private void resetVrScene() {
        //Send a message to reset the unity scene
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_RESET_SCENE,
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }

    //VR Player Controls
    //changes the highlights of the buttons, controls both the local video and
    //sends an action to the connected peers.
    //Enhancement - sync time will peers each button press??
    private void setVideoSource(int startTime) {
        //Modify the link so that we don't split it accidentally
        String safeURL = attemptedURL.replace(':', '|');

        //Send action to peers to play
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_SET_SOURCE + ":" + safeURL + ":" + startTime + ":" + "Link",
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }

    private void setLoadingSpinner() {
        if (vrLoadingBar != null) {
            LeadMeMain.runOnUI(() -> {
                vrVideoControls.setVisibility(View.GONE);
                vrLoadingBar.setVisibility(View.VISIBLE);
            });
            scheduledExecutorService.schedule(() ->
                LeadMeMain.runOnUI(() -> {
                vrVideoControls.setVisibility(View.VISIBLE);
                vrLoadingBar.setVisibility(View.GONE);
                }), 5000, TimeUnit.MILLISECONDS
            );
        }
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

    private void playVideo() {
        if(buttonMessages()) { return; }
        //Play local video
        LeadMeMain.runOnUI(() -> {
            if (isYoutube) {
                controllerWebView.loadUrl("javascript:player.playVideo();");
            } else {
                controllerWebView.loadUrl("javascript:playVideo();");
            }
        });
        buttonHighlights(VRAccessibilityManager.CUE_PLAY);

        //Send action to peers to play
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_PLAY,
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }

    private void pauseVideo() {
        if(buttonMessages()) { return; }
        //Pause local video
        LeadMeMain.runOnUI(() -> {
            if (isYoutube) {
                controllerWebView.loadUrl("javascript:player.pauseVideo();");
            } else {
                controllerWebView.loadUrl("javascript:pauseVideo();");
            }
        });
        buttonHighlights(VRAccessibilityManager.CUE_PAUSE);

        //Send action to peers to pause
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.VR_PLAYER_TAG + ":" + VRAccessibilityManager.CUE_PAUSE,
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }


    private boolean buttonMessages() {
        boolean stopFunction = false;
        if(firstTouch) {
            showToast("Tap the youtube play button to enable controls.");
            stopFunction = true;
        }
        return stopFunction;
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
            default:
                Log.d(TAG, "Unknown video state");
                break;
        }
    }
}
