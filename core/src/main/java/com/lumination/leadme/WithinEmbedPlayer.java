package com.lumination.leadme;

import android.app.AlertDialog;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Scanner;

public class WithinEmbedPlayer {

    private final static String TAG = "embedPlayerYT";


    //static variables
    private static final int UNSTARTED = -1;
    private static final int PLAYING = 1;
    private static final int PAUSED = 2;

    private static final int VR_MODE = 1;
    private static final int FULLSCRN_MODE = 0;

    //variables to store what the latest request was
    private static boolean showCaptions = false;
    private static int videoCurrentPlayState = UNSTARTED;
    private static int videoCurrentDisplayMode = FULLSCRN_MODE; //VR, FS, STD

    private String controllerURL = "", controllerTitle = "";
    private AlertDialog videoControlDialog;
    View videoControllerDialogView;
    private WebView controllerWebView;
    private TextView vrModeBtn, captionsBtn, expander;
    private String attemptedURL = "";
    private boolean firstPlay = true;
    boolean pageLoaded = false;


    LeadMeMain main;

    /**
     * USEFUL LINKS
     * <p>
     * https://stackoverflow.com/questions/3298597/how-to-get-return-value-from-javascript-in-webview-of-android
     * https://developers.google.com/youtube/iframe_api_reference
     */

    public WithinEmbedPlayer(LeadMeMain main) {
        this.main = main;
        videoControllerDialogView = View.inflate(main, R.layout.e__within_popup, null);
        controllerWebView = videoControllerDialogView.findViewById(R.id.within_webview);
        internetUnavailableMsg = videoControllerDialogView.findViewById(R.id.no_internet);
        internetUnavailableMsg.setOnClickListener(v -> loadVideoGuideURL(controllerURL));

        controllerWebView.setWebChromeClient(new WebChromeClient());
        controllerWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        controllerWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        controllerWebView.canGoBack();
        controllerWebView.canGoForward();
        controllerWebView.getSettings().setDomStorageEnabled(true);
        controllerWebView.addJavascriptInterface(this, "Android");
//        String ua = controllerWebView.getSettings().getUserAgentString();
//        Log.w(TAG, "User agent? " +ua+" vs "+System.getProperty("http.agent"));
//        controllerWebView.getSettings().setUserAgentString(ua);

        controllerWebView.getSettings().setAllowFileAccess(true);
        controllerWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        controllerWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        controllerWebView.getSettings().setAllowContentAccess(true);
        controllerWebView.getSettings().setLoadsImagesAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            controllerWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        }
        controllerWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);


        //controllerWebView.getSettings().setUserAgentString("");

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
        //Log.d(TAG, "Data is currently: " + value);
    }

    @JavascriptInterface
    public void updateState(int state) {
        Log.d(TAG, "[GUIDE] Video state is now: " + state);
        videoCurrentPlayState = state;
        //make sure student state is updated too
//        if (state == PLAYING) {
//            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
//                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PLAY,
//                    main.getNearbyManager().getSelectedPeerIDsOrAll());
//
//        } else if (state == PAUSED) {
//            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
//                    LeadMeMain.VID_ACTION_TAG + YouTubeAccessibilityManager.CUE_PAUSE,
//                    main.getNearbyManager().getSelectedPeerIDsOrAll());
//        }
    }

    private final String urlPrefix = "https://with.in/watch/";
    private final String foundPrefix = "https://cms.with.in/v1/content/";
    private final String foundSuffix = "?platform=webplayer&list=Main-Web";
    private String foundURL = "";
    private String foundTitle = "";

    private void setupGuideVideoControllerWebClient() {
        controllerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "WITHIN GUIDE] OVERRIDE? " + videoCurrentPlayState + ", " + request.getUrl() + " // " + request.getMethod() + " // " + request.getRequestHeaders());
                //we shouldn't be navigating away from within
                //and this only gets triggered when we try to, so block it every time
                return true;
            }

            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "WITHIN GUIDE] onLoadResource: " + url + " (" + attemptedURL + ")");
                if (url.startsWith(foundPrefix) && url.endsWith(foundSuffix)) {
                    foundTitle = url.replace(foundPrefix, "").replace(foundSuffix, "");
                    foundURL = urlPrefix + foundTitle;
                    Log.w(TAG, "EXTRACTED! " + foundURL);
                } else if (url.startsWith("https://cms.with.in/v1/category/all?page=")) {
                    view.stopLoading();
                }
            }

            public void onPageFinished(WebView view, String url) {
                firstPlay = true;
                pageLoaded = true;
                //stopVideo(); //stop it cleanly
                Log.d(TAG, "WITHIN GUIDE] onPageFinished: " + url + " (" + attemptedURL + ")");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.d(TAG, "WITHIN GUIDE] Received error: " + error.toString());
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.d(TAG, "WITHIN GUIDE] Received HTTP error: " + errorResponse.getReasonPhrase() + ", " + errorResponse.getData());
                Log.d(TAG, "WITHIN GUIDE] ER " + request.toString() + ", " + request.getMethod() + ", " + request.getRequestHeaders());
            }

//            @Override
//            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                super.onReceivedSslError(view, handler, error);
//                Log.d(TAG, "WITHIN GUIDE] Received SSL error: " + error.toString());
//                handler.proceed();
//            }
        });
    }

    private void setupGuideVideoControllerButtons() {
        //set up standard dialog buttons
        videoControllerDialogView.findViewById(R.id.web_back_btn).setOnClickListener(v -> controllerWebView.goBack());

        videoControllerDialogView.findViewById(R.id.stream_within).setOnClickListener(v -> {
                    if (foundURL.isEmpty()) {
                        showToast("No experience selected!");
                        return;
                    }
                    main.getAppManager().launchWithin(foundURL, true);
                }
        );

        videoControllerDialogView.findViewById(R.id.download_within).setOnClickListener(v -> {
                    if (foundURL.isEmpty()) {
                        showToast("No experience selected!");
                        return;
                    }
                    main.getAppManager().launchWithin(foundURL, false);
                }
        );

        videoControllerDialogView.findViewById(R.id.within_back).setOnClickListener(v -> {
                    foundURL = "";
                    hideVideoController();
                }
        );

    }


    void showGuideController() {
        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(videoControllerDialogView)
                    .create();
        }
        pageLoaded = false; //reset flag
        loadVideoGuideURL(controllerURL);
        foundURL = ""; //clean this
        videoControlDialog.show();
        //return to main screen
        main.hideConfirmPushDialog();
    }

    TextView internetUnavailableMsg;

    private void loadVideoGuideURL(String url) {
        resetControllerState();
        attemptedURL = url;
        if (main.getPermissionsManager().isInternetConnectionAvailable(attemptedURL)) {
            internetUnavailableMsg.setVisibility(View.GONE);
            controllerWebView.setVisibility(View.VISIBLE);
            //controllerWebView.loadUrl("https://get.webgl.org/");
            controllerWebView.loadDataWithBaseURL(null, getiFrameData(), "text/html", "UTF-8", null);
        } else {
            internetUnavailableMsg.setVisibility(View.VISIBLE);
            controllerWebView.setVisibility(View.GONE);
        }
    }

    public String getiFrameData() {
        InputStream htmlTemplate = main.getResources().openRawResource(R.raw.embed_within_player);
        Scanner scanner = new Scanner(htmlTemplate);
        String output = "";
        while (scanner.hasNext()) {
            output += scanner.nextLine() + "\n";
        }
        return output;
    }

    private void resetControllerState() {
        showCaptions = false;
        videoCurrentDisplayMode = FULLSCRN_MODE;
        controllerURL = "";
        controllerTitle = "";
        attemptedURL = "";
    }

    private void hideVideoController() {
        videoControlDialog.dismiss();
    }


}
