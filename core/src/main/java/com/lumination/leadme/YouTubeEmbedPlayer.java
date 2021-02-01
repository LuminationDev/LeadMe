package com.lumination.leadme;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Point;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Scanner;

public class YouTubeEmbedPlayer {

    private final static String TAG = "embedPlayerYT";


    //static variables
    public static final int PLAY = 0;
    public static final int PAUSE = 1;
    public static final int FASTFWD = 2;
    public static final int REWIND = 3;
    public static final int UNKNOWN = 4;

    public static final int STD_MODE = 0;
    public static final int VR_MODE = 1;
    public static final int FULLSCRN_MODE = 2;

    //variables that accessibility service can inspect
    //to see if a change has occurred
    public static boolean playStateChanged = false;
    public static boolean displayModeChanged = false;
    public static boolean playFromChanged = false;
    public static boolean showCaptions = false;

    //variables to store what the latest request was
    public static int videoCurrentPlayState = UNKNOWN; //PLAY, PAUSE, FWD, RWD, etc
    public static int videoCurrentDisplayMode = STD_MODE; //VR, FS, STD
    public static int playFromInSeconds = -1;

    private String controllerURL = "", controllerTitle = "", controllerVidID = "";
    private AlertDialog videoControlDialog;
    private View videoControllerDialogView;
    private WebView controllerWebView;
    private TextView totalTimeText, elapsedTimeText;
    private ProgressBar progressBar;
    private String attemptedURL = "";
    private boolean firstPlay = true;

    private String embedSuffix = "";//?t=1&fs=1&rel=0&controls=0&modestbranding=1&feature=oembed&enablejsapi=1";//&t=1&rel=0"; //"?fs=1&feature=oembed"

    WebManager webManager;
    LeadMeMain main;

    /**
     * USEFUL LINKS
     *
     * https://stackoverflow.com/questions/3298597/how-to-get-return-value-from-javascript-in-webview-of-android
     * https://developers.google.com/youtube/iframe_api_reference
     */

    private boolean blockingTouch = true;
    public YouTubeEmbedPlayer(LeadMeMain main, WebManager webManager) {
        this.main = main;
        this.webManager = webManager;

        videoControllerDialogView = View.inflate(main, R.layout.e__currently_streaming_popup, null);
        controllerWebView = videoControllerDialogView.findViewById(R.id.video_stream_webview);
        totalTimeText = videoControllerDialogView.findViewById(R.id.totalTimeText);
        elapsedTimeText = videoControllerDialogView.findViewById(R.id.elapsedTimeText);
        progressBar = videoControllerDialogView.findViewById(R.id.progressBar);

        View webBlockingView = videoControllerDialogView.findViewById(R.id.web_blocking_view);
        webBlockingView.setOnTouchListener((v, event) -> false); //consume all touch events

        controllerWebView.setWebChromeClient(new WebChromeClient());
        controllerWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        controllerWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        controllerWebView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        //controllerWebView.canGoBack();

        //controllerWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        //web1.getSettings().setDefaultFontSize(18);
        //controllerWebView.loadData(htmldata,"text/html; charset=utf-8",null);

        controllerWebView.addJavascriptInterface(this, "Android");

        setupGuideVideoControllerWebClient();
        setupGuideVideoControllerButtons();
    }

    public void setVideoDetails(String url, String title){
        controllerURL = url;
        controllerTitle = title;
        controllerVidID = webManager.getYouTubeID(url);
    }

    public void refreshView(String url, String title) {
        String testID1 = webManager.getYouTubeID(controllerURL);
        String testID2 = webManager.getYouTubeID(url);

        //update controller title
        if (title != null && testID1.equals(testID2)) {
            controllerTitle = title;
            ((TextView) videoControllerDialogView.findViewById(R.id.video_title)).setText(title);
        }
        Log.d(TAG, "!!! " + title + ", " + testID1 + " vs " + testID2);
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(main, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void onData(String value) {
        //.. do something with the data
        Log.d(TAG, "Data is currently: "+value);
    }

    float currentTime = -1;
    @JavascriptInterface
    public void setCurrentTime(String value){
        currentTime = Integer.parseInt(value);
        main.runOnUiThread(() -> {
            elapsedTimeText.setText(intToTime((int)currentTime));

            Log.d(TAG, "WHAT? "+currentTime+"/"+totalTime+" = "+(currentTime/totalTime));
            int progress = Math.round((currentTime/totalTime)*100);
            progressBar.setProgress(progress);
        });
    }

    private String intToTime(int duration){
        return DateUtils.formatElapsedTime(duration);
    }

    int totalTime = -1;
    @JavascriptInterface
    public void setTotalTime(String value){
        totalTime = Integer.parseInt(value);
        main.runOnUiThread(() -> totalTimeText.setText(intToTime(totalTime)));
    }

    private void setupGuideVideoControllerWebClient() {
        controllerWebView.setWebViewClient(new WebViewClient() {
            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "VIDEO GUIDE] onLoadResource: " + url + " (" + attemptedURL + ")");
            }

            public void onPageFinished(WebView view, String url) {
                firstPlay = true;
                stopVideo();
                Log.d(TAG, "VIDEO GUIDE] onPageFinished: " + url + " (" + attemptedURL + ")");
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

    private void setupGuideVideoControllerButtons() {
        //set up standard dialog buttons
        videoControllerDialogView.findViewById(R.id.new_video).setOnClickListener(v -> {
            webManager.lastWasGuideView = false; //reset
            hideVideoController();
            webManager.showWebLaunchDialog(false);
        });

        videoControllerDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v ->
                hideVideoController()
        );

        //set up advanced controls toggle behaviour
        final View advancedControls = videoControllerDialogView.findViewById(R.id.advanced_controls);
        advancedControls.setVisibility(View.GONE); //hidden by default

        videoControllerDialogView.findViewById(R.id.advanced_controls_expander).setOnClickListener(v -> {
            if (advancedControls.getVisibility() == View.GONE) {
                advancedControls.setVisibility(View.VISIBLE);
            } else {
                advancedControls.setVisibility(View.GONE);
            }
        });

        //set up basic controls
        videoControllerDialogView.findViewById(R.id.vr_mode_btn).setOnClickListener(v -> {
            if (videoCurrentDisplayMode != VR_MODE) {
                videoCurrentDisplayMode = VR_MODE;
                ((ImageView) videoControllerDialogView.findViewById(R.id.vr_mode_btn)).setImageDrawable(main.getResources().getDrawable(R.drawable.task_vr_icon, null));
            } else {
                videoCurrentDisplayMode = FULLSCRN_MODE;
                ((ImageView) videoControllerDialogView.findViewById(R.id.vr_mode_btn)).setImageDrawable(main.getResources().getDrawable(R.drawable.task_vr_icon_disabled, null));
            }
            displayModeChanged = true;
            triggerAccessibilityUpdate(); //this needs the accessibility service to action it
            //TODO determine what to send to students and how to action it at their end
        });

        videoControllerDialogView.findViewById(R.id.play_btn).setOnClickListener(v -> {
            playVideo();
        });

        videoControllerDialogView.findViewById(R.id.pause_btn).setOnClickListener(v -> {
            pauseVideo();
        });

        videoControllerDialogView.findViewById(R.id.rewind_btn).setOnClickListener(v -> {
            videoCurrentPlayState = REWIND;
            playStateChanged = true;
            triggerAccessibilityUpdate(); //this needs the accessibility service to action it
            //TODO determine what to send to students and how to action it at their end
        });

        videoControllerDialogView.findViewById(R.id.fastforward_btn).setOnClickListener(v -> {
            videoCurrentPlayState = FASTFWD;
            playStateChanged = true;
            triggerAccessibilityUpdate(); //this needs the accessibility service to action it
            //TODO determine what to send to students and how to action it at their end
        });

        //set up advanced controls
        videoControllerDialogView.findViewById(R.id.play_from_btn).setOnClickListener(v -> {
            playFromChanged = true;
            playFromInSeconds = 0; //TODO retrieve current video location and send to all students
            triggerAccessibilityUpdate(); //this needs the accessibility service to action it
            //TODO determine what to send to students and how to action it at their end
        });

        videoControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v -> {
            webManager.muteVideo();
        });

        videoControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v -> {
            webManager.unmuteVideo();
        });

        videoControllerDialogView.findViewById(R.id.captions_btn).setOnClickListener(v -> {
            showCaptions = !showCaptions; //toggle this
            triggerAccessibilityUpdate(); //this needs the accessibility service to action it
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_CAPTIONS_TAG + ":" + showCaptions, main.getNearbyManager().getSelectedPeerIDs());
        });

    }

    private void playVideo() {
        videoCurrentPlayState = PLAY;
        playStateChanged = true;

        if(firstPlay) {
            firstPlay = false;
            //touch the screen, we're ready
            int[] location = new int[2];
            int[] size = new int[2];
            Point p = new Point();
            main.getWindowManager().getDefaultDisplay().getRealSize(p);
            Log.w(TAG, "TAP TAP! " + (p.x / 2) + ", " + (p.y / 3));
            blockingTouch = false;
            main.tapBounds(518, 927);
        } else {
            controllerWebView.loadUrl("javascript:playVideo()");
        }

        triggerAccessibilityUpdate(); //this needs the accessibility service to action it
        //TODO determine what to send to students and how to action it at their end
    }

    private void pauseVideo() {
        videoCurrentPlayState = PAUSE;
        playStateChanged = true;
        controllerWebView.loadUrl("javascript:pauseVideo()");
        triggerAccessibilityUpdate(); //this needs the accessibility service to action it
        //TODO determine what to send to students and how to action it at their end
    }

    private void stopVideo() {
        videoCurrentPlayState = PAUSE;
        playStateChanged = true;
        controllerWebView.loadUrl("javascript:(stopVideo())()");
        triggerAccessibilityUpdate(); //this needs the accessibility service to action it
        //TODO determine what to send to students and how to action it at their end
    }

    private void triggerAccessibilityUpdate() {
        if (main.getNearbyManager().isConnectedAsFollower()) {
            //trigger the accessibility service to run an update
            Intent intent = new Intent(LumiAccessibilityService.BROADCAST_ACTION);
            intent.setComponent(new ComponentName("com.lumination.leadme", "com.lumination.leadme.LumiAccessibilityReceiver"));
            Bundle data = new Bundle();
            data.putString(LumiAccessibilityService.INFO_TAG, LumiAccessibilityService.REFRESH_ACTION);
            intent.putExtras(data);
            main.sendBroadcast(intent);
        }
    }

    public void showCaptions(boolean captionsOn) {
        triggerAccessibilityUpdate();
        showCaptions = captionsOn;
    }

    public void blockWebTouch(boolean blockingTouch){
        this.blockingTouch = blockingTouch;
    }


    public String getCurrentURL() {
        return controllerURL;
    }

    public String getCurrentTitle() {
        return controllerTitle;
    }

    public String getiFrameForURL(String url) {
        Log.d(TAG, "Attempting to show " + url);
        String embedID = webManager.getYouTubeID(url);

//        //http://www.youtube.com/embed/olC42gO-Ln4?fs=1&amp;feature=oembed
//        String content = "<body style=\"margin: 0; padding: 0\"><iframe width=\"100%\" height=\"100%\" " +
//                "frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\" src=\"" + url + "\" frameborder=\"0\"></iframe></body>";

        InputStream htmlTemplate = main.getResources().openRawResource(R.raw.embed_player);
        Scanner scanner = new Scanner(htmlTemplate);
        String output = "";
        while (scanner.hasNext()) {
            output += scanner.nextLine() + "\n";
        }
        output = output.replace("PLACEHOLDER_ID", embedID);
        Log.d(TAG, "Read file! " + output);
        return output;

    }


    void showVideoController() {
        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(videoControllerDialogView)
                    .create();
        }

        Log.d(TAG, "Attempting to show video controller for " + controllerTitle + ", " + controllerURL);
        if (controllerTitle != null) {
            ((TextView) videoControllerDialogView.findViewById(R.id.video_title)).setText(controllerTitle);
        }
        if (controllerURL != null) {
            loadVideoGuideURL(controllerURL);
        }
        videoControlDialog.show();
        webManager.lastWasGuideView = true;
    }

    private void loadVideoGuideURL(String url) {
        attemptedURL = convertYouTubeToEmbed(url);
        controllerWebView.loadDataWithBaseURL(null, getiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
    }

    private void hideVideoController() {
        pauseVideo();
        videoControlDialog.dismiss();
    }

    public String getEmbedSuffixSuffix() {
        return embedSuffix;
    }

    public String convertYouTubeToEmbed(String url) {
        String id = webManager.getYouTubeID(url);
        String finalURL = "https://www.youtube.com/embed/" + id + getEmbedSuffixSuffix();
        Log.i(TAG, "Returning embedded YT: " + finalURL);
        return finalURL;
    }


}
