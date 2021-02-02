package com.lumination.leadme;

import android.app.AlertDialog;
import android.graphics.Point;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Scanner;

public class YouTubeEmbedPlayer {

    private final static String TAG = "embedPlayerYT";


    //static variables
    public static final int UNSTARTED = -1;
    public static final int ENDED = 0;
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;
    public static final int BUFFERING = 3;
    public static final int VIDEO_CUED = 5;

    public static final int VR_MODE = 1;
    public static final int FULLSCRN_MODE = 0;

    //variables that accessibility service can inspect
    //to see if a change has occurred
    public static boolean playStateChanged = false;
    public static boolean displayModeChanged = false;
    public static boolean playFromChanged = false;
    public static boolean showCaptions = false;

    //variables to store what the latest request was
    public static int videoCurrentPlayState = UNSTARTED;
    public static int videoCurrentDisplayMode = FULLSCRN_MODE; //VR, FS, STD
    public static int playFromInSeconds = -1;

    private String controllerURL = "";
    private AlertDialog videoControlDialog;
    private View videoControllerDialogView;
    private WebView controllerWebView;
    private String attemptedURL = "";
    private boolean firstPlay = true;

    private String embedSuffix = "";//?t=1&fs=1&rel=0&controls=0&modestbranding=1&feature=oembed&enablejsapi=1";//&t=1&rel=0"; //"?fs=1&feature=oembed"

    WebManager webManager;
    LeadMeMain main;

    /**
     * USEFUL LINKS
     * <p>
     * https://stackoverflow.com/questions/3298597/how-to-get-return-value-from-javascript-in-webview-of-android
     * https://developers.google.com/youtube/iframe_api_reference
     */

    private boolean blockingTouch = true;

    public YouTubeEmbedPlayer(LeadMeMain main, WebManager webManager) {
        this.main = main;
        this.webManager = webManager;

        videoControllerDialogView = View.inflate(main, R.layout.e__currently_streaming_popup, null);
        controllerWebView = videoControllerDialogView.findViewById(R.id.video_stream_webview);

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

    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(main, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void onData(String value) {
        //.. do something with the data
        Log.d(TAG, "Data is currently: " + value);
    }

    @JavascriptInterface
    public void updateState(int state) {
        Log.d(TAG, "Video state is now: " + state);
        videoCurrentPlayState = state;
    }

    @JavascriptInterface
    public void optionsChanged(String options) {
        Log.d(TAG, "Got options: " + options);
    }


    boolean pageLoaded = false;

    private void setupGuideVideoControllerWebClient() {
        controllerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, videoCurrentPlayState + ", " + request.getUrl() + " // " + request.getMethod() + " // " + request.getRequestHeaders());
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

        //set up basic controls
        TextView vrModeBtn = (TextView) videoControllerDialogView.findViewById(R.id.vr_mode_btn);
        videoControllerDialogView.findViewById(R.id.vr_mode_btn).setOnClickListener(v -> {
            if (videoCurrentDisplayMode != VR_MODE) {
                videoCurrentDisplayMode = VR_MODE;
                vrModeBtn.setText("VR mode on");
                vrModeBtn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.task_vr_icon, 0, 0);
            } else {
                videoCurrentDisplayMode = FULLSCRN_MODE;
                vrModeBtn.setText("VR mode off");
                vrModeBtn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.task_vr_icon_disabled, 0, 0);
            }
            displayModeChanged = true;
            //TODO determine what to send to students and how to action it at their end
        });


        videoControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v -> {
            webManager.muteVideo();
        });

        videoControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v -> {
            webManager.unmuteVideo();
        });

    }


    private void playVideo() {
        if (firstPlay) {
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
    }

    private void pauseVideo() {
        controllerWebView.loadUrl("javascript:pauseVideo()");
    }

    private void stopVideo() {
        controllerWebView.loadUrl("javascript:(stopVideo())()");
    }
//
//    private void triggerAccessibilityUpdate() {
//        if (main.getNearbyManager().isConnectedAsFollower()) {
//            //trigger the accessibility service to run an update
//            Intent intent = new Intent(LumiAccessibilityService.BROADCAST_ACTION);
//            intent.setComponent(new ComponentName("com.lumination.leadme", "com.lumination.leadme.LumiAccessibilityReceiver"));
//            Bundle data = new Bundle();
//            data.putString(LumiAccessibilityService.INFO_TAG, LumiAccessibilityService.REFRESH_ACTION);
//            intent.putExtras(data);
//            main.sendBroadcast(intent);
//        }
//    }


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


    void showVideoController(String url) {
        if (url != null) {
            controllerURL = url;
        }
        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(videoControllerDialogView)
                    .create();
        }
        pageLoaded = false; //reset flag
        Log.d(TAG, "Attempting to show video controller for " + controllerURL);
        loadVideoGuideURL(controllerURL);
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
