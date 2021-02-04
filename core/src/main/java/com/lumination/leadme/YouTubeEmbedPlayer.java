package com.lumination.leadme;

import android.app.AlertDialog;
import android.graphics.Point;
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
import android.widget.SeekBar;
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
    private TextView totalTimeText, elapsedTimeText;
    private SeekBar progressBar;
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

    public YouTubeEmbedPlayer(LeadMeMain main, WebManager webManager) {
        this.main = main;
        this.webManager = webManager;

        videoControllerDialogView = View.inflate(main, R.layout.e__currently_streaming_popup, null);
        controllerWebView = videoControllerDialogView.findViewById(R.id.video_stream_webview);
        internetUnavailableMsg = videoControllerDialogView.findViewById(R.id.no_internet);
        totalTimeText = videoControllerDialogView.findViewById(R.id.totalTimeText);
        elapsedTimeText = videoControllerDialogView.findViewById(R.id.elapsedTimeText);
        progressBar = videoControllerDialogView.findViewById(R.id.progressBar);

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
                setNewTime(durationCalc);
            }
        });

        internetUnavailableMsg.setOnClickListener(v -> loadVideoGuideURL(controllerURL));

        controllerWebView.setWebChromeClient(new WebChromeClient());
        controllerWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        controllerWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        //controllerWebView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

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
    public void captionsLoaded() {
        Toast.makeText(main, "Captions loaded / API change", Toast.LENGTH_SHORT).show();
        //turn them back on to stay in sync with students
        //controllerWebView.loadUrl("javascript:hideCaptions()");
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
        //set up advanced controls toggle behaviour
        final View advancedControls = videoControllerDialogView.findViewById(R.id.advanced_controls);
        advancedControls.setVisibility(View.GONE); //hidden by default

        TextView expander = videoControllerDialogView.findViewById(R.id.advanced_controls_expander);
        expander.setOnClickListener(v -> {
            if (advancedControls.getVisibility() == View.GONE) {
                advancedControls.setVisibility(View.VISIBLE);
                expander.setText("Less");
                expander.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.vid_less, 0, 0);
            } else {
                advancedControls.setVisibility(View.GONE);
                expander.setText("More");
                expander.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.vid_more);
            }
        });

        TextView captionsBtn = (TextView) videoControllerDialogView.findViewById(R.id.captions_btn);
        videoControllerDialogView.findViewById(R.id.captions_btn).setOnClickListener(v -> {
            if (videoCurrentPlayState != PLAYING) {
                Toast.makeText(main, "Captions only available when video is playing", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!showCaptions) {
                showCaptions = true;
                captionsBtn.setText("Captions on");
                captionsBtn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.vid_captions, 0, 0);
                controllerWebView.loadUrl("javascript:showCaptions()");
            } else {
                showCaptions = false;
                captionsBtn.setText("Captions off");
                captionsBtn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.vid_captions_disabled, 0, 0);
                controllerWebView.loadUrl("javascript:hideCaptions()");
            }
            displayModeChanged = true;
        });


        //set up basic controls
        TextView vrModeBtn = (TextView) videoControllerDialogView.findViewById(R.id.vr_mode_btn);
        videoControllerDialogView.findViewById(R.id.vr_mode_btn).setOnClickListener(v -> {
            if (videoCurrentDisplayMode != VR_MODE) {
                videoCurrentDisplayMode = VR_MODE;
                vrModeBtn.setText("VR on");
                vrModeBtn.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.task_vr_icon, 0, 0);
            } else {
                videoCurrentDisplayMode = FULLSCRN_MODE;
                vrModeBtn.setText("VR off");
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

        videoControllerDialogView.findViewById(R.id.play_btn).setOnClickListener(v -> {
            playVideo();
        });

        videoControllerDialogView.findViewById(R.id.pause_btn).setOnClickListener(v -> {
            pauseVideo();
        });

        videoControllerDialogView.findViewById(R.id.rewind_btn).setOnClickListener(v -> {
            int rwdTime = ((int) currentTime - 10);
            setNewTime(rwdTime);
        });

        videoControllerDialogView.findViewById(R.id.fastforward_btn).setOnClickListener(v -> {
            int ffwdTime = ((int) currentTime + 10);
            setNewTime(ffwdTime);
        });

    }

    private void setNewTime(int newTime) {
        //ensure new time is sensible
        if (newTime < 0) {
            newTime = 0;
        } else if (newTime > totalTime) {
            newTime = totalTime;
        }
        //update player and local view
        controllerWebView.loadUrl("javascript:seekTo(\"" + attemptedURL + "\", " + newTime + ")");
        setCurrentTime("" + newTime);
    }


    private void playVideo() {
        if (firstPlay) {
            firstPlay = false;
            //touch the screen, we're ready
            Point p = new Point();
            main.getWindowManager().getDefaultDisplay().getRealSize(p);
            main.tapBounds(518, 927);
            Log.w(TAG, "TAP TAP! " + (p.x / 2) + ", " + (p.y / 3) + " vs hardcoded 518, 927");
        } else {
            controllerWebView.loadUrl("javascript:playVideo()");
        }
    }

    private void pauseVideo() {
        controllerWebView.loadUrl("javascript:pauseVideo()");
    }

    private void stopVideo() {
        controllerWebView.loadUrl("javascript:stopVideo()");
    }

    public String getiFrameForURL(String url) {
        Log.d(TAG, "Attempting to show " + url);
        String embedID = webManager.getYouTubeID(url);

        InputStream htmlTemplate = main.getResources().openRawResource(R.raw.embed_player);
        Scanner scanner = new Scanner(htmlTemplate);
        String output = "";
        while (scanner.hasNext()) {
            output += scanner.nextLine() + "\n";
        }
        output = output.replace("PLACEHOLDER_ID", embedID);
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

    TextView internetUnavailableMsg;
    private void loadVideoGuideURL(String url) {
        attemptedURL = convertYouTubeToEmbed(url);
        if (main.getPermissionsManager().isInternetConnectionAvailable(attemptedURL)) {
            internetUnavailableMsg.setVisibility(View.GONE);
            controllerWebView.setVisibility(View.VISIBLE);
            controllerWebView.loadDataWithBaseURL(null, getiFrameForURL(attemptedURL), "text/html", "UTF-8", null);
        } else {
            internetUnavailableMsg.setVisibility(View.VISIBLE);
            controllerWebView.setVisibility(View.GONE);
        }
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

    float currentTime = -1;

    @JavascriptInterface
    public void setCurrentTime(String value) {
        currentTime = Integer.parseInt(value);
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
        totalTime = Integer.parseInt(value);
        main.runOnUiThread(() -> totalTimeText.setText(intToTime(totalTime)));
    }


}
