package com.lumination.leadme.players;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.net.http.SslError;
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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.managers.NearbyPeersManager;
import com.lumination.leadme.managers.WebManager;
import com.lumination.leadme.adapters.LumiSpinnerAdapter;

import java.io.InputStream;
import java.util.Scanner;

public class YouTubeEmbedPlayer {

    private final static String TAG = "embedPlayerYT";

    //static variables
    private static final int UNSTARTED = -1;
    private static final int PLAYING = 1;

    private static int videoCurrentPlayState = UNSTARTED;

    private String controllerTitle = "";
    private String attemptedURL = "";
    private AlertDialog videoControlDialog;
    private final View videoControllerDialogView, videoControls;
    private final WebView controllerWebView;
    private boolean firstPlay = true;

    private boolean firstTouch; //track if the guide has started the video
    private boolean adsFinished; //track if students are still watching ads

    //track the peers for ad control
    int peersAdControl = 0;

    private final TextView internetUnavailableMsg;
    private TextView youtubePreviewTitle;
    private Button youtubePreviewPushBtn;
    private TextView repushBtn;
    private WebView youtubePreviewWebView;
    private View youtubeSettingsDialogView;
    private CheckBox favCheck;
    private View youtubeInternetUnavailableMsg;
    private View youtubeVideoControls;

    private Spinner lockSpinner;
    private String[] lockSpinnerItems;
    private AlertDialog playbackSettingsDialog;

    private WebView activeWebView = null;

    private final WebManager webManager;
    private final LeadMeMain main;

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

    boolean pageLoaded = false;

    @SuppressLint("ClickableViewAccessibility")
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

        controllerWebView.setOnTouchListener((v, event) -> {
            //after ads finish initial play guide uses the buttons
            if(firstTouch) {
                return peerWaitingForAds(); //check if ads have finished
            }

            return !firstTouch;
        });
    }

    /**
     * Notify the leader if any learners are still watching ads on Youtube. If the number of peers
     * ready is greater or equal to the number of connected peers then the video can be controlled,
     * this is incase a learner leaves while on youtube so the guide does not get stuck.
     * @return A boolean representing if the ads have finished.
     */
    private boolean peerWaitingForAds() {
//        if(peersAdControl >= main.getConnectedLearnersAdapter().mData.size()) {
//            main.getDialogManager().showWarningDialog("Waiting for Ads","Student devices are still \n" +
//                    "waiting for ads to finish.");
//        } else {
//            adsFinished = true;
//        }
//
        //Above is disabled for now, if a single student is not signed in to youtube the guide
        //will not be able to start the video.

        adsFinished = true;
        return !adsFinished;
    }

    private void setupGuideVideoControllerButtons() {
        //set up standard dialog buttons
        //new_video New video button!
        videoControllerDialogView.findViewById(R.id.push_btn).setOnClickListener(v -> {
            //clean any launch on focus items
            LeadMeMain.appIntentOnFocus = null;
            DispatchManager.launchAppOnFocus = null;

            Controller.getInstance().getWebManager().reset();
            webManager.lastWasGuideView = false; //reset
            videoControlDialog.dismiss();
            webManager.showWebLaunchDialog(false);
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.RETURN_TAG, NearbyPeersManager.getSelectedPeerIDsOrAll());
        });

        repushBtn = videoControllerDialogView.findViewById(R.id.push_again_btn);
        repushBtn.setOnClickListener(v -> {
            webManager.pushYouTube(attemptedURL, controllerTitle, false, LeadMeMain.selectedOnly);
            syncNewStudentsWithCurrentState();
        });

        videoControllerDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v -> {
            resetControllerState();
            hideVideoController();
        });


        videoControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v -> {
            main.muteAudio(); //this is managed by the main activity
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.VID_MUTE_TAG,
                    NearbyPeersManager.getSelectedPeerIDsOrAll());
        });

        videoControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v -> {
            main.unMuteAudio(); //this is managed by the main activity
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.VID_UNMUTE_TAG,
                    NearbyPeersManager.getSelectedPeerIDsOrAll());
        });

    }

    private void syncNewStudentsWithCurrentState() {


    }


    @JavascriptInterface
    public void playVideo() {
        if (firstPlay) {
            firstPlay = false;
        }
    }

    @JavascriptInterface
    public void pauseVideo() {
    }

    private void stopVideo() {
    }

    public String getiFrameForURL(String url) {
        String embedID = WebManager.getYouTubeID(url);

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
        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        activeWebView = controllerWebView;
        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(videoControllerDialogView)
                    .create();

            videoControlDialog.setOnDismissListener(dialog -> main.hideSystemUI());
        }
        pageLoaded = false; //reset flag
        Log.d(TAG, "Attempting to show video controller for " + attemptedURL);
        loadVideoGuideURL(attemptedURL);
        videoControlDialog.setCancelable(false);
        videoControlDialog.show();
        webManager.lastWasGuideView = true;
    }

    private void loadVideoGuideURL(String url) {
        Log.d(TAG, "loadVideoGuideURL: 1");
        attemptedURL = embedYouTubeURL(url);
        if (!webManager.lastWasGuideView) {
            resetControllerState(); //this needs to happen after extracting the URL, to retain playfrom time
        }
        if (Controller.getInstance().getPermissionsManager().isInternetConnectionAvailable()) {
            internetUnavailableMsg.setVisibility(View.GONE);
            videoControls.setVisibility(View.VISIBLE);
            Log.w(TAG, "Loading webview for: " + attemptedURL + ", " + activeWebView.getTag());
            activeWebView.loadDataWithBaseURL(null, getiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
        } else {
            internetUnavailableMsg.setVisibility(View.VISIBLE);
            videoControls.setVisibility(View.GONE);
        }
        Log.d(TAG, "loadVideoGuideURL: 2");
    }

    private void resetControllerState() {
        Log.e(TAG, "Resetting controller!! " + webManager.lastWasGuideView + " vs " + attemptedURL);
        currentTime = -1;
        totalTime = -1;
        firstTouch = true;
        adsFinished = false;
        peersAdControl = 0;
    }

    public void dismissDialogs() {
        LeadMeMain.runOnUI(() -> {
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
        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        //TODO Can open the player back up at the assigned video instead of pausing?
        pauseVideo();
        videoControlDialog.dismiss();
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


    float currentTime = 0;

    int totalTime = -1;

    //////////////////

    private void createPlaybackSettingsPopup() {
        youtubeSettingsDialogView = View.inflate(main, R.layout.f__playback_settings_youtube, null);
        youtubePreviewPushBtn = youtubeSettingsDialogView.findViewById(R.id.push_btn);
        youtubePreviewTitle = youtubeSettingsDialogView.findViewById(R.id.preview_title);
        youtubeInternetUnavailableMsg = youtubeSettingsDialogView.findViewById(R.id.no_internet);
        youtubeVideoControls = youtubeSettingsDialogView.findViewById(R.id.video_controls);
        favCheck = youtubeSettingsDialogView.findViewById(R.id.fav_checkbox);
        youtubePreviewWebView = youtubeSettingsDialogView.findViewById(R.id.video_stream_webview);
        youtubePreviewWebView.setTag("PREVIEW/PLAYBACK SETTINGS");
        Controller.getInstance().getDialogManager().setupPushToggle(youtubeSettingsDialogView, false);
        youtubeSettingsDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v ->
                playbackSettingsDialog.dismiss()
        );

        youtubePreviewPushBtn.setOnClickListener(v -> LeadMeMain.UIHandler.post(() -> {

            playbackSettingsDialog.dismiss();
            lastLockState = lockSpinner.getSelectedItem().toString().startsWith("View");
            webManager.pushYouTube(attemptedURL, controllerTitle, lastLockState, LeadMeMain.selectedOnly);
            if (favCheck.isChecked()) {
                webManager.favouritesManager.getYouTubeFavouritesAdapter().addCurrentPreviewToFavourites(webManager.getPushURL(), webManager.getPreviewTitle(), webManager.getPreviewImage());
            }

            main.showLeaderScreen();
            showPushConfirmed();
        }));

        youtubePreviewWebView.setWebChromeClient(new WebChromeClient());
        youtubePreviewWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        youtubePreviewWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        youtubePreviewWebView.addJavascriptInterface(this, "Android");
        lockSpinner = (Spinner) youtubeSettingsDialogView.findViewById(R.id.push_spinner);
        lockSpinnerItems = new String[2];
        lockSpinnerItems[0] = "View only";
        lockSpinnerItems[1] = "Free play";
        Integer[] push_imgs = {R.drawable.controls_view, R.drawable.controls_play};
        LumiSpinnerAdapter push_adapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, lockSpinnerItems, push_imgs);
        lockSpinner.setAdapter(push_adapter);
        lockSpinner.setSelection(0); //default to locked
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
            showVideoController();
        });
    }

    public void updateTitle(String title) {
        youtubePreviewTitle.setText(title);
    }

    boolean lastLockState = true;

    public void showPlaybackPreview(String url, String title) {
        Log.d(TAG, "showPlaybackPreview: 1");
        LeadMeMain.runOnUI(() -> {
            main.closeKeyboard();
            main.hideSystemUI();
        });

        Controller.getInstance().getDialogManager().toggleSelectedView(youtubeSettingsDialogView);

        activeWebView = youtubePreviewWebView;
        attemptedURL = embedYouTubeURL(url);
        if (!webManager.lastWasGuideView) {
            resetControllerState(); //this needs to happen after extracting the URL, to retain playfrom time
        }
        youtubePreviewTitle.setText(title);
        if (Controller.getInstance().getPermissionsManager().isInternetConnectionAvailable()) {
            youtubeInternetUnavailableMsg.setVisibility(View.GONE);
            youtubeVideoControls.setVisibility(View.VISIBLE);
            Log.w(TAG, "Loading preview playback controls for: " + attemptedURL + ", " + activeWebView.getTag());
            activeWebView.loadDataWithBaseURL(null, getiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
        } else {
            youtubeInternetUnavailableMsg.setVisibility(View.VISIBLE);
            youtubeVideoControls.setVisibility(View.GONE);
        }

        favCheck.setChecked(Controller.getInstance().getFavouritesManager().getYouTubeFavouritesAdapter().isInFavourites(url));

        if (playbackSettingsDialog == null) {
            playbackSettingsDialog = new AlertDialog.Builder(main)
                    .setView(youtubeSettingsDialogView)
                    .show();

            playbackSettingsDialog.setOnDismissListener(dialog -> main.hideSystemUI());
        } else {
            playbackSettingsDialog.show();
        }
        Log.d(TAG, "showPlaybackPreview: 2");
    }
}
