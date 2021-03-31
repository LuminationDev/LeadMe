package com.lumination.leadme;

import android.app.AlertDialog;
import android.net.http.SslError;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Scanner;

public class YouTubeEmbedPlayer {

    private final static String TAG = "embedPlayerYT";


    //static variables
    private static final int UNSTARTED = -1;
    private static final int ENDED = 0;
    private static final int PLAYING = 1;
    private static final int PAUSED = 2;
    private static final int BUFFERING = 3;
    private static final int VIDEO_CUED = 5;

    private static final int VR_MODE = 1;
    private static final int FULLSCRN_MODE = 0;

    //variables to store what the latest request was
    private static boolean showCaptions = false;
    private static int videoCurrentPlayState = UNSTARTED;
    private static int videoCurrentDisplayMode = FULLSCRN_MODE; //VR, FS, STD

    //private String controllerURL = "";
    private String controllerTitle = "";
    private String attemptedURL = "";
    private AlertDialog videoControlDialog;
    private final View videoControllerDialogView, videoControls;
    private final WebView controllerWebView;
    private Switch vrModeBtn;
    private boolean firstPlay = true;

    private TextView youtubePreviewTitle;
    private Button youtubePreviewPushBtn;
    private TextView repushBtn;
    private WebView youtubePreviewWebView;
    private View youtubeSettingsDialogView;
    private CheckBox favCheck;
    private View youtubeInternetUnavailableMsg;
    private View youtubeVideoControls;

    private View lockSpinnerParent;
    private Spinner lockSpinner;
    private String[] lockSpinnerItems;
    private AlertDialog playbackSettingsDialog;
    private TextView playFromTime;

    private WebView activeWebView = null;

    private TextView totalTimeText, elapsedTimeText;
    private SeekBar progressBar;

    private WebManager webManager;
    private LeadMeMain main;

    /**
     * USEFUL LINKS
     * <p>
     * https://stackoverflow.com/questions/3298597/how-to-get-return-value-from-javascript-in-webview-of-android
     * https://developers.google.com/youtube/iframe_api_reference
     */

    public YouTubeEmbedPlayer(LeadMeMain main, WebManager webManager) {
        this.main = main;
        this.webManager = webManager;

        videoControllerDialogView = View.inflate(main, R.layout.f__playback_control_youtube, null);
        videoControls = videoControllerDialogView.findViewById(R.id.video_controls);
        internetUnavailableMsg = videoControllerDialogView.findViewById(R.id.no_internet);
        internetUnavailableMsg.setOnClickListener(v -> loadVideoGuideURL(attemptedURL));
        controllerWebView = videoControllerDialogView.findViewById(R.id.video_stream_webview);
        controllerWebView.setTag("CONTROLLER");

        createPlaybackSettingsPopup();

        controllerWebView.setWebChromeClient(new WebChromeClient());
        controllerWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        controllerWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        controllerWebView.addJavascriptInterface(this, "Android");

        setupGuideVideoControllerWebClient();
        setupGuideVideoControllerButtons();
    }

    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(main, toast, Toast.LENGTH_LONG).show();
    }

    private boolean init = false;

    @JavascriptInterface
    public void updateState(int state) {
        Log.d(TAG, "[GUIDE] Video state is now: " + state + " // " + currentTime);
        videoCurrentPlayState = state;
        //make sure student state is updated too
        if (!init && (state == VIDEO_CUED || state == PLAYING)) {
            String str = extractTime(attemptedURL);
            if (!str.isEmpty()) {
                lastStartFrom = Integer.parseInt(str);
            } else {
                lastStartFrom = 1;
            }
            setNewTime(lastStartFrom);
            init = true;
        }

        if (state == PLAYING) {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PLAY,
                    main.getNearbyManager().getSelectedPeerIDsOrAll());

        } else if (state == PAUSED) {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PAUSE,
                    main.getNearbyManager().getSelectedPeerIDsOrAll());
        }
    }

    @JavascriptInterface
    public void captionsLoaded() {
        //Toast.makeText(main, "Captions loaded / API change", Toast.LENGTH_SHORT).show();
        //turn them back on to stay in sync with students
        //controllerWebView.loadUrl("javascript:hideCaptions()");
    }

    boolean pageLoaded = false;

    private void setupGuideVideoControllerWebClient() {
        controllerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //Log.d(TAG, videoCurrentPlayState + ", " + request.getUrl() + " // " + request.getMethod() + " // " + request.getRequestHeaders());
                //we shouldn't be navigating away once a video is loaded, so block this
                if (pageLoaded) {
                    //this will pause the video, so if it should
                    //be playing, play it again
                    if (videoCurrentPlayState == PLAYING) {
                        playVideo();
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            public void onLoadResource(WebView view, String url) {
                //Log.d(TAG, "VIDEO GUIDE] onLoadResource: " + url + " (" + attemptedURL + ")");
            }

            public void onPageFinished(WebView view, String url) {
                firstPlay = true;
                pageLoaded = true;
                stopVideo(); //stop it cleanly
                //Log.d(TAG, "VIDEO GUIDE] onPageFinished: " + url + " (" + attemptedURL + ")");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                //Log.d(TAG, "VIDEO GUIDE] Received error: " + error.toString());
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                //Log.d(TAG, "VIDEO GUIDE] Received HTTP error: " + errorResponse.toString());
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                //Log.d(TAG, "VIDEO GUIDE] Received SSL error: " + error.toString());
            }
        });
    }

    private void setupGuideVideoControllerButtons() {
        //set up standard dialog buttons
        //new_video New video button!
        videoControllerDialogView.findViewById(R.id.push_btn).setOnClickListener(v -> {

            //clean any launch on focus items
            main.appIntentOnFocus = null;
            main.getDispatcher().launchAppOnFocus = null;

            main.getWebManager().reset();
            webManager.lastWasGuideView = false; //reset
            videoControlDialog.hide();
            webManager.showWebLaunchDialog(false);
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.RETURN_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

        repushBtn = videoControllerDialogView.findViewById(R.id.push_again_btn);
        repushBtn.setOnClickListener(v -> {
            webManager.pushYouTube(attemptedURL, controllerTitle, lastStartFrom, lastLockState, isVROn());
            syncNewStudentsWithCurrentState();
        });

        videoControllerDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v ->
                hideVideoController()
        );


        //set up basic controls
        View basicControls = videoControllerDialogView.findViewById(R.id.basic_controls);

        ImageView vrIcon = videoControllerDialogView.findViewById(R.id.vr_mode_icon);
        vrModeBtn = (Switch) videoControllerDialogView.findViewById(R.id.vr_mode_toggle);
        ((Switch) videoControllerDialogView.findViewById(R.id.vr_mode_toggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    videoCurrentDisplayMode = VR_MODE;
                    vrModeBtn.setText("VR Mode ON");
                    vrModeBtn.setChecked(true);

                    vrIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.task_vr_icon, null));
                    playVideo(); //entering VR mode automatically plays the video for students, so replicate that here

                    main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                            LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_VR_ON,
                            main.getNearbyManager().getSelectedPeerIDsOrAll());
                } else {
                    videoCurrentDisplayMode = FULLSCRN_MODE;
                    vrModeBtn.setText("VR Mode OFF");
                    vrModeBtn.setChecked(false);

                    vrIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.task_vr_icon_disabled, null));

                    main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                            LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_VR_OFF,
                            main.getNearbyManager().getSelectedPeerIDsOrAll());
                }
            }
        });


        videoControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v -> {
            main.muteAudio(); //this is managed by the main activity
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_MUTE_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

        videoControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v -> {
            main.unMuteAudio(); //this is managed by the main activity
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_UNMUTE_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

//        videoControllerDialogView.findViewById(R.id.play_btn).setOnClickListener(v -> {
//            if (videoCurrentDisplayMode == VR_MODE) {
//                showToast("Cannot play in VR mode. Exit VR mode and try again.");
//                return;
//            }
//            playVideo();
//            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
//                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PLAY,
//                    main.getNearbyManager().getSelectedPeerIDsOrAll());
//        });
//
//        videoControllerDialogView.findViewById(R.id.pause_btn).setOnClickListener(v -> {
//            if (videoCurrentDisplayMode == VR_MODE) {
//                showToast("Cannot pause in VR mode. Exit VR mode and try again.");
//                return;
//            }
//            pauseVideo();
//            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
//                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PAUSE,
//                    main.getNearbyManager().getSelectedPeerIDsOrAll());
//        });

    }

    private void syncNewStudentsWithCurrentState() {

        Log.w(TAG, "Syncing new! " + videoCurrentPlayState + ", " + videoCurrentDisplayMode + ", " + main.isMuted);
        //update on play/pause
        if (videoCurrentPlayState == PLAYING) {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PLAY,
                    main.getNearbyManager().getSelectedPeerIDsOrAll());
        } else {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PAUSE,
                    main.getNearbyManager().getSelectedPeerIDsOrAll());
        }

        //update on VR on/off
        if (videoCurrentDisplayMode == VR_MODE) {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_VR_ON,
                    main.getNearbyManager().getSelectedPeerIDsOrAll());
        } else {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_VR_OFF,
                    main.getNearbyManager().getSelectedPeerIDsOrAll());
        }

        //update on mute on/off
        if (main.isMuted) {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_MUTE_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        } else {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_UNMUTE_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        }
    }

    private void setNewTime(int newTime) {
        if (totalTime == -1) {
            Log.e(TAG, "Nope. No good.");
            return;
        }
        //TODO if needed
        //ensure new time is sensible
        if (newTime < 0) {
            newTime = 0;
        } else if (newTime > totalTime && totalTime > 0) {
            newTime = totalTime;
        }
        final int finalData = newTime;

        //update player and local view
        main.runOnUiThread(() -> {
            activeWebView.loadUrl("javascript:seekTo(\"" + attemptedURL + "\", " + finalData + ")");
        });
        setCurrentTime("" + newTime);
    }


    @JavascriptInterface
    public void playVideo() {
        if (firstPlay) {
            firstPlay = false;
        }
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PLAY,
                main.getNearbyManager().getSelectedPeerIDsOrAll());

//        if (firstPlay) {
//            firstPlay = false;
//            //touch the screen, we're ready
//            Point p = new Point();
//            main.getWindowManager().getDefaultDisplay().getRealSize(p);
//            main.tapBounds(518, 927);
//            Log.w(TAG, "TAP TAP! " + (p.x / 2) + ", " + (p.y / 2.5) + " vs hardcoded 518, 927");
//        } else {
//            activeWebView.loadUrl("javascript:playVideo()");
//        }
    }

    @JavascriptInterface
    public void pauseVideo() {
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PAUSE,
                main.getNearbyManager().getSelectedPeerIDsOrAll());

        //activeWebView.loadUrl("javascript:pauseVideo()");
    }

    private void stopVideo() {
        //activeWebView.loadUrl("javascript:stopVideo()");
    }

    public String getiFrameForURL(String url) {
        String embedID = webManager.getYouTubeID(url);

        InputStream htmlTemplate = main.getResources().openRawResource(R.raw.embed_yt_player);
        Scanner scanner = new Scanner(htmlTemplate);
        String output = "";
        while (scanner.hasNext()) {
            output += scanner.nextLine() + "\n";
        }
        String startTime = extractTime(url);
        if (startTime.isEmpty()) {
            startTime = "0";
        }
        output = output.replace("PLACEHOLDER_ID", embedID);
        output = output.replace("PLACEHOLDER_START", startTime);
        //Log.d(TAG, output);
        return output;

    }

    public void showVideoController() {
        main.runOnUiThread(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        activeWebView = controllerWebView;
        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(videoControllerDialogView)
                    .create();
        }
        pageLoaded = false; //reset flag
        Log.d(TAG, "Attempting to show video controller for " + attemptedURL);
        loadVideoGuideURL(attemptedURL);
        videoControlDialog.show();
        webManager.lastWasGuideView = true;
    }

    private TextView internetUnavailableMsg;

    private void loadVideoGuideURL(String url) {
        init = false;
        attemptedURL = embedYouTubeURL(url);
        if (!webManager.lastWasGuideView) {
            resetControllerState(); //this needs to happen after extracting the URL, to retain playfrom time
        }
        if (main.getPermissionsManager().isInternetConnectionAvailable()) {
            internetUnavailableMsg.setVisibility(View.GONE);
            videoControls.setVisibility(View.VISIBLE);
            Log.w(TAG, "Loading webview for: " + attemptedURL + ", " + activeWebView.getTag());
            activeWebView.loadDataWithBaseURL(null, getiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
        } else {
            internetUnavailableMsg.setVisibility(View.VISIBLE);
            videoControls.setVisibility(View.GONE);
        }
    }

    private void resetControllerState() {
        Log.e(TAG, "Resetting controller!! " + webManager.lastWasGuideView + " vs " + attemptedURL);
        showCaptions = false;
        videoCurrentDisplayMode = FULLSCRN_MODE;
        currentTime = -1;
        totalTime = -1;
        progressBar.setProgress(0);
        playFromTime.setText("00:00");
        elapsedTimeText.setText("00:00");

        if (vrModeBtn != null && vrModeBtn.isChecked()) {
            vrModeBtn.setChecked(false); //toggle it
        }
    }

    public void dismissDialogs() {
        main.runOnUiThread(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        if (videoControlDialog != null) {
            videoControlDialog.dismiss();
        }

        if (playbackSettingsDialog != null) {
            playbackSettingsDialog.dismiss();
        }
    }

    private void hideVideoController() {
        main.runOnUiThread(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        pauseVideo();
        videoControlDialog.hide();
    }

    private String extractTime(String url) {
        int startIndex = url.indexOf("start=", 0);

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
        String id = webManager.getYouTubeID(url);
        //Log.w(TAG, "YouTube ID = " + id + " from " + url);
        if (id.isEmpty()) {
            return "";
        }
        String startSubstring = "";
        if (url.contains("&start=")) {
            String val = extractTime(url);
            startSubstring = "?start=" + val + "&t=" + val;

        } else if (currentTime > 0) {
            startSubstring = "?start=" + (int) currentTime + "&t=" + (int) currentTime;

        } else {
            startSubstring = "?t=1";
        }
        String finalURL = "https://www.youtube.com/embed/" + id + startSubstring;
        //Log.d(TAG, "Final URL: " + finalURL + "(from " + url + ", " + currentTime + ")");
        return finalURL;
    }


    float currentTime = 0;

    @JavascriptInterface
    public void setCurrentTime(String value) {
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

    private String intToTime(int duration) {
        return DateUtils.formatElapsedTime(duration);
    }

    int totalTime = -1;

    @JavascriptInterface
    public void setTotalTime(String value) {
        int tmpTotal = Integer.parseInt(value);
        if (tmpTotal > 0) {
            //Log.d(TAG, "[GUIDE] TOTAL time is now: " + value + " // " + attemptedURL);// + ", " + extractedTime);
            totalTime = tmpTotal;
            main.runOnUiThread(() -> {
                totalTimeText.setText(intToTime(totalTime));
            });
        }
    }


    //////////////////

    private void createPlaybackSettingsPopup() {
        youtubeSettingsDialogView = View.inflate(main, R.layout.f__playback_settings_youtube, null);
        youtubePreviewPushBtn = youtubeSettingsDialogView.findViewById(R.id.yt_push_btn);
        youtubePreviewTitle = youtubeSettingsDialogView.findViewById(R.id.preview_title);
        youtubeInternetUnavailableMsg = youtubeSettingsDialogView.findViewById(R.id.no_internet);
        youtubeVideoControls = youtubeSettingsDialogView.findViewById(R.id.video_controls);
        progressBar = youtubeSettingsDialogView.findViewById(R.id.progressBar);
        playFromTime = youtubeSettingsDialogView.findViewById(R.id.video_play_from_input);
        elapsedTimeText = youtubeSettingsDialogView.findViewById(R.id.elapsedTimeText);
        totalTimeText = youtubeSettingsDialogView.findViewById(R.id.totalTimeText);
        favCheck = youtubeSettingsDialogView.findViewById(R.id.fav_checkbox);
        youtubePreviewWebView = youtubeSettingsDialogView.findViewById(R.id.video_stream_webview);
        youtubePreviewWebView.setTag("PREVIEW/PLAYBACK SETTINGS");

        youtubeSettingsDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v ->
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

        youtubePreviewPushBtn.setOnClickListener(v -> main.getHandler().post(() -> {
            playbackSettingsDialog.dismiss();
            int durationCalc = (int) ((progressBar.getProgress() / 100.0) * totalTime);
            lastLockState = lockSpinner.getSelectedItem().toString().startsWith("View");
            webManager.pushYouTube(attemptedURL, controllerTitle, durationCalc, lastLockState, isVROn());
            if (favCheck.isChecked()) {
                webManager.getYouTubeFavouritesManager().addCurrentPreviewToFavourites();
            }
            showVideoController();
        }));

        youtubePreviewWebView.setWebChromeClient(new WebChromeClient());
        youtubePreviewWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        youtubePreviewWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        youtubePreviewWebView.addJavascriptInterface(this, "Android");

        lockSpinnerParent = youtubeSettingsDialogView.findViewById(R.id.lock_spinner);
        lockSpinner = (Spinner) youtubeSettingsDialogView.findViewById(R.id.push_spinner);
        lockSpinnerItems = new String[2];
        lockSpinnerItems[0] = "View only";
        lockSpinnerItems[1] = "Free play";
        Integer[] push_imgs = {R.drawable.controls_view, R.drawable.controls_play};
        LumiSpinnerAdapter push_adapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, lockSpinnerItems, push_imgs);
        lockSpinner.setAdapter(push_adapter);
        lockSpinner.setSelection(0); //default to locked
    }

    public void updateTitle(String title) {
        youtubePreviewTitle.setText(title);
    }

    public boolean isVROn() {
        return vrModeBtn.isChecked();
    }

    int lastStartFrom = 1;
    boolean lastLockState = true;

    public void showPlaybackPreview(String url, String title) {
        main.runOnUiThread(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        init = false;
        activeWebView = youtubePreviewWebView;
        attemptedURL = embedYouTubeURL(url);
        if (!webManager.lastWasGuideView) {
            resetControllerState(); //this needs to happen after extracting the URL, to retain playfrom time
        } else if (vrModeBtn.isChecked()) {
            playVideo();
        }
        youtubePreviewTitle.setText(title);
        if (main.getPermissionsManager().isInternetConnectionAvailable()) {
            youtubeInternetUnavailableMsg.setVisibility(View.GONE);
            youtubeVideoControls.setVisibility(View.VISIBLE);
            Log.w(TAG, "Loading preview playback controls for: " + attemptedURL + ", " + activeWebView.getTag());
            activeWebView.loadDataWithBaseURL(null, getiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
        } else {
            youtubeInternetUnavailableMsg.setVisibility(View.VISIBLE);
            youtubeVideoControls.setVisibility(View.GONE);
        }

        favCheck.setChecked(main.getWebManager().getYouTubeFavouritesManager().isInFavourites(url));

        if (playbackSettingsDialog == null) {
            playbackSettingsDialog = new AlertDialog.Builder(main)
                    .setView(youtubeSettingsDialogView)
                    .show();
        } else {
            playbackSettingsDialog.show();
        }
    }


}
