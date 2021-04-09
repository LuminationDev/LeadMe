package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class WithinEmbedPlayer {

    private final static String TAG = "embedPlayerWithin";


    //static variables
    private static final int VR_MODE = 1;
    private static final int STD_MODE = 0;

    //variables to store what the latest request was
    private static int videoCurrentDisplayMode = STD_MODE; //VR, STD

    private AlertDialog videoControlDialog, videoSearchDialog;
    private final Button pushBtn;
    private TextView repushBtn;
    private final CheckBox favCheck;
    private final View withinControllerDialogView, withinSearchDialogView;
    private final TextView internetUnavailableMsg;
    private final TextView searchUnavailableMsg;
    private WebView controllerWebView, searchWebView;
    private final TextView streamBtn, downloadBtn;
    private final Switch vrModeBtn;
    private final ImageView vrIcon;
    private final Spinner lockSpinner;

    private String attemptedURL = "";
    boolean pageLoaded = false;

    private boolean stream = true;
    private boolean vrMode = true;

    private final LeadMeMain main;

    /**
     * USEFUL LINKS
     * <p>
     * https://stackoverflow.com/questions/3298597/how-to-get-return-value-from-javascript-in-webview-of-android
     * https://developers.google.com/youtube/iframe_api_reference
     */

    private ViewGroup.LayoutParams searchBackupParams, controllerBackupParams;

    public WithinEmbedPlayer(LeadMeMain main) {
        this.main = main;
        withinSearchDialogView = View.inflate(main, R.layout.f__selection_popup_within, null);
        searchWebView = withinSearchDialogView.findViewById(R.id.within_webview_search);
        searchBackupParams = searchWebView.getLayoutParams();
        searchUnavailableMsg = withinSearchDialogView.findViewById(R.id.no_internet);
        searchUnavailableMsg.setOnClickListener(v -> searchWebView.reload());
        setupWebView(searchWebView);
        setupWebClient(searchWebView, true);
        setupWithinSearchButtons();

        withinControllerDialogView = View.inflate(main, R.layout.f__playback_within, null);
        favCheck = withinControllerDialogView.findViewById(R.id.fav_checkbox_within);
        vrModeBtn = withinControllerDialogView.findViewById(R.id.vr_mode_toggle);
        streamBtn = withinControllerDialogView.findViewById(R.id.stream_btn);
        downloadBtn = withinControllerDialogView.findViewById(R.id.download_btn);
        lockSpinner = (Spinner) withinControllerDialogView.findViewById(R.id.push_spinner);
        pushBtn = withinControllerDialogView.findViewById(R.id.push_btn);
        repushBtn = withinControllerDialogView.findViewById(R.id.push_again_btn);
        vrIcon = withinControllerDialogView.findViewById(R.id.vr_mode_icon);
        controllerWebView = withinControllerDialogView.findViewById(R.id.within_webview);
        controllerBackupParams = controllerWebView.getLayoutParams();
        internetUnavailableMsg = withinControllerDialogView.findViewById(R.id.no_internet);
        internetUnavailableMsg.setOnClickListener(v -> loadVideoGuideURL(foundURL));
        setupWebView(controllerWebView);
        setupWebClient(controllerWebView, false);
        setupGuideVideoControllerButtons();

        withinSearchDialogView.findViewById(R.id.open_favourites).setOnClickListener(v -> {
            main.closeKeyboard();
            main.hideSystemUI();
            videoSearchDialog.dismiss();
            main.getWebManager().launchUrlYtFavourites();
        });

        withinSearchDialogView.findViewById(R.id.select_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_active, null));
    }

    private void setupWebView(WebView tmpWebView) {
        tmpWebView.setWebChromeClient(new WebChromeClient());
        tmpWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
        tmpWebView.canGoBack();
        tmpWebView.canGoForward();
        tmpWebView.addJavascriptInterface(this, "Android");

        //speed it up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tmpWebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        } else {
            tmpWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            tmpWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            tmpWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        // this is required to show the video preview
        tmpWebView.getSettings().setDomStorageEnabled(true);

        // it's likely that not ALL of these are needed, perhaps worth testing and whittling down
        tmpWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        tmpWebView.getSettings().setAllowFileAccess(true);
        tmpWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        tmpWebView.getSettings().setPluginState(WebSettings.PluginState.ON);
        tmpWebView.getSettings().setAllowContentAccess(true);
        tmpWebView.getSettings().setLoadsImagesAutomatically(true);

        //this prevents the issue with the accelerometer values
        //not passing to webview and the 360 view thinking we're
        //always looking at the floor
        setDesktopMode(tmpWebView, true);
    }

    //String newUA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";
    private void setDesktopMode(WebView webView, boolean enabled) {
        String newUserAgent = webView.getSettings().getUserAgentString();
        if (enabled) {
            try {
                String ua = webView.getSettings().getUserAgentString();
                String androidOSString = webView.getSettings().getUserAgentString().substring(ua.indexOf("("), ua.indexOf(")") + 1);
                newUserAgent = webView.getSettings().getUserAgentString().replace(androidOSString, "(X11; Linux x86_64)");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            newUserAgent = null;
        }

        webView.getSettings().setUserAgentString(newUserAgent);
        webView.reload();
    }

    private void toggleStreamBtn() {
        if (stream) {
            streamBtn.setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right, null));
            downloadBtn.setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left_white, null));
            streamBtn.setElevation(2);
            downloadBtn.setElevation(3);

        } else {
            streamBtn.setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right_white, null));
            downloadBtn.setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left, null));
            streamBtn.setElevation(3);
            downloadBtn.setElevation(2);
        }
    }

    private void toggleVRBtn() {
        if (vrMode) {
            vrModeBtn.setText(main.getResources().getString(R.string.vr_mode_on));
            vrModeBtn.setChecked(true);
            vrIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.task_vr_icon, null));

        } else {
            vrModeBtn.setText(main.getResources().getString(R.string.vr_mode_off));
            vrModeBtn.setChecked(false);
            vrIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.task_vr_icon_disabled, null));
        }
    }

    public Spinner getLockSpinner() {
        return lockSpinner;
    }


    /**
     * Show a toast from the web page
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(main, toast, Toast.LENGTH_SHORT).show();
    }

    private final String urlPrefix = "https://with.in/watch/";
    private final String foundPrefix = "https://cms.with.in/v1/content/";
    private final String foundSuffix = "?platform=webplayer&list=Main-Web";
    String foundURL = "";
    private String foundTitle = "";

    private void setupWebClient(WebView tmpWebView, boolean searchView) {
        tmpWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "WITHIN GUIDE] OVERRIDE? " + request.getUrl() + " // " + request.getMethod() + " // " + request.getRequestHeaders());
                //we shouldn't be navigating away from within
                //and this only gets triggered when we try to, so block it every time
                return true;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                //try to work out which it is doesn't seem to be sufficient.
                //so to be safe, just kill and rebuild both
                Log.e(TAG, "The WebView rendering process crashed!");

                try {
                    resetControllerState();

                    //controller view
                    ViewGroup webViewContainer = (ViewGroup) withinControllerDialogView.findViewById(R.id.preview_view);
                    webViewContainer.removeView(controllerWebView);
                    controllerWebView.destroy();
                    controllerWebView = null;

                    //build it again
                    controllerWebView = new WebView(main);
                    controllerWebView.setLayoutParams(controllerBackupParams);
                    setupWebView(controllerWebView);
                    setupWebClient(controllerWebView, false);
                    webViewContainer.addView(controllerWebView, 0);

                    //search view
                    webViewContainer = (ViewGroup) withinSearchDialogView.findViewById(R.id.preview_view);
                    webViewContainer.removeView(searchWebView);
                    searchWebView.destroy();
                    searchWebView = null;

                    //build it again
                    searchWebView = new WebView(main);
                    searchWebView.setLayoutParams(searchBackupParams);
                    setupWebView(searchWebView);
                    setupWebClient(searchWebView, true);
                    webViewContainer.addView(searchWebView, 0);

                    // Renderer crashed because of an internal error, such as a memory access violation
                    Log.e(TAG, "The WebView rendering process crashed! >> " + view + ", " + view.getTag() + ", " + view.getId() + ", " + view.getUrl() + ", " + withinControllerDialogView.isShown() + ", " + withinSearchDialogView.isShown());

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true; //true if app can continue to function
            }

            public void onLoadResource(WebView view, String url) {
                //Log.d(TAG, "WITHIN GUIDE] onLoadResource: " + url + " (" + attemptedURL + ")");
                if (searchView && url.startsWith(foundPrefix) && url.endsWith(foundSuffix)) {
                    foundTitle = url.replace(foundPrefix, "").replace(foundSuffix, "");
                    foundURL = urlPrefix + foundTitle;
                    withinSearchDialogView.findViewById(R.id.select_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_active, null));
                    Log.w(TAG, "EXTRACTED! " + foundURL + ", " + main.getFavouritesManager().isInFavourites(foundURL));

                } else if (url.startsWith("https://cms.with.in/v1/category/all?page=")) {
                    view.stopLoading();
                }
            }

            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                Log.d(TAG, "WITHIN GUIDE] onPageFinished: " + url + " (" + foundURL + ")");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.d(TAG, "WITHIN GUIDE] Received error: " + error.getErrorCode() + ", " + error.getDescription());
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.d(TAG, "WITHIN GUIDE] Received HTTP error: " + errorResponse.getReasonPhrase() + ", " + errorResponse.getData());
                Log.d(TAG, "WITHIN GUIDE] ER " + request.toString() + ", " + request.getMethod() + ", " + request.getRequestHeaders());
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request){
                String url = request.getUrl().toString();
                if(url==null){
                    return super.shouldInterceptRequest(view,request);
                }
                //should in theory help improve load times for within
                if(url.toLowerCase().contains(".jpg") || url.toLowerCase().contains(".jpeg")){
                    Bitmap bitmap = null;
                    try {
                        bitmap = Glide.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.RESOURCE).load(url).submit().get();
                        Log.d(TAG, "shouldInterceptRequest: intercepted jpg");
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(bitmap==null){
                        return super.shouldInterceptRequest(view,request);
                    }
                    Log.d(TAG, "shouldInterceptRequest: loaded jpg");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100 , bos);
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                    return new WebResourceResponse("image/jpg", "UTF-8",bs);
                }else if(url.toLowerCase().contains(".png")){
                    Bitmap bitmap = null;
                    try {
                        bitmap = Glide.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.RESOURCE).load(url).submit().get();
                        Log.d(TAG, "shouldInterceptRequest: intercepted png");
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(bitmap==null){
                        return super.shouldInterceptRequest(view,request);
                    }
                    Log.d(TAG, "shouldInterceptRequest: loaded png");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100 , bos);
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                    return new WebResourceResponse("image/png", "UTF-8",bs);
                }else if(url.toLowerCase().contains(".webp")){
                    Bitmap bitmap = null;
                    try {
                        bitmap = Glide.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.RESOURCE).load(url).submit().get();
                        Log.d(TAG, "shouldInterceptRequest: intercepted webp");
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(bitmap==null){
                        return super.shouldInterceptRequest(view,request);
                    }
                    Log.d(TAG, "shouldInterceptRequest: loaded webp");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100 , bos);
                    }else{
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 100 , bos);
                    }
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                    return new WebResourceResponse("image/webp", "UTF-8",bs);
                }else{
                    return super.shouldInterceptRequest(view,request);
                }
            }
        });
    }

    Drawable disabledBg;

    private void setupWithinSearchButtons() {
        disabledBg = main.getResources().getDrawable(R.drawable.bg_disabled, null);
        //set up standard dialog buttons
        withinSearchDialogView.findViewById(R.id.web_back_btn).setOnClickListener(v -> {
            if (foundURL.isEmpty()) {
                //if no experience is selected, close the popup
                videoSearchDialog.dismiss();
            } else {
                //otherwise, go back
                searchWebView.goBack();
            }
        });

        withinSearchDialogView.findViewById(R.id.within_back).setOnClickListener(v -> {
            videoSearchDialog.dismiss();
        });

        withinSearchDialogView.findViewById(R.id.select_btn).setOnClickListener(v -> {
            if (!v.getBackground().equals(disabledBg) && !foundURL.trim().equals("")) {
                Log.i(TAG, "Loading " + foundTitle + " | " + foundURL);
                videoSearchDialog.dismiss();
                showGuideController(true);

            } else {
                showToast("No experience selected!");
            }
        });

    }

    private void updateControllerUI(boolean isPlaybackController) {
        if (isPlaybackController) {
            withinControllerDialogView.findViewById(R.id.basic_controls).setVisibility(View.VISIBLE);
            withinControllerDialogView.findViewById(R.id.vr_selection).setVisibility(View.GONE);

            withinControllerDialogView.findViewById(R.id.playback_btns).setVisibility(View.VISIBLE);
            withinControllerDialogView.findViewById(R.id.within_select_btns).setVisibility(View.GONE);

            repushBtn.setVisibility(View.VISIBLE);
            ((TextView) withinControllerDialogView.findViewById(R.id.title)).setText(main.getResources().getText(R.string.playback_controls_title));

        } else {
            withinControllerDialogView.findViewById(R.id.basic_controls).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.vr_selection).setVisibility(View.VISIBLE);

            withinControllerDialogView.findViewById(R.id.playback_btns).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.within_select_btns).setVisibility(View.VISIBLE);

            repushBtn.setVisibility(View.INVISIBLE);
            ((TextView) withinControllerDialogView.findViewById(R.id.title)).setText(main.getResources().getText(R.string.playback_settings_title));
        }
    }

    private void setupGuideVideoControllerButtons() {
        //set up standard dialog buttons
        withinControllerDialogView.findViewById(R.id.web_back_btn).setOnClickListener(v -> {
            controllerWebView.goBack();
        });

        withinControllerDialogView.findViewById(R.id.new_video).setOnClickListener(v -> {
            resetControllerState();
            videoControlDialog.dismiss();
            showWithinSearch();
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.RETURN_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

        withinControllerDialogView.findViewById(R.id.video_back_btn).setOnClickListener(v -> {
            videoControlDialog.dismiss();
        });

        withinControllerDialogView.findViewById(R.id.within_back).setOnClickListener(v -> {
            videoControlDialog.dismiss();
        });

        withinControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v -> {
            main.muteAudio(); //this is managed by the main activity
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_MUTE_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

        withinControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v -> {
            main.unMuteAudio(); //this is managed by the main activity
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_UNMUTE_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

        repushBtn.setOnClickListener(v -> {
            main.getDispatcher().repushApp(main.getNearbyManager().getSelectedPeerIDsOrAll());
        });

        pushBtn.setOnClickListener(v -> {
            pushWithin();
        });

        vrModeBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vrMode = isChecked;
            toggleVRBtn();
        });

        streamBtn.setOnClickListener(v -> {
            stream = true;
            toggleStreamBtn();
        });

        downloadBtn.setOnClickListener(v -> {
            stream = false;
            toggleStreamBtn();
        });

    }

    private void pushWithin() {
        attemptedURL = foundURL;
        Log.d(TAG, "Launching WithinVR for students: " + attemptedURL + ", [STR] " + stream + ", [VR] " + vrMode);
        main.getAppManager().launchWithin(attemptedURL, stream, vrMode);
        main.updateFollowerCurrentTask(main.getAppManager().withinPackage, "Within VR", "VR Video", attemptedURL, foundTitle);
        //String packageName, String appName, String taskType, String url, String urlTitle)

        //update UI
        updateControllerUI(true);

        //add to favourites
        if (favCheck.isChecked()) {
            main.getWebManager().getYouTubeFavouritesManager().addToFavourites(foundURL, foundTitle, null);
        }

        if (vrMode) {
            //TODO AUTO PLAY VIDEO
            withinControllerDialogView.findViewById(R.id.vr_mode).setVisibility(View.VISIBLE);
            withinControllerDialogView.findViewById(R.id.phone_mode).setVisibility(View.GONE);

        } else {
            withinControllerDialogView.findViewById(R.id.vr_mode).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.phone_mode).setVisibility(View.VISIBLE);
        }
    }

    public void showWithin() {
        Log.w(TAG, "Showing WITHIN: " + foundURL + ", " + main.getAppManager().withinURI);
        if (foundURL.isEmpty()) {
            showWithinSearch();
        } else {
            showGuideController(false);
        }
    }

    private void showWithinSearch() {
        foundURL = ""; //reset
        foundTitle = "";
        attemptedURL = "";
        if (videoSearchDialog == null) {
            videoSearchDialog = new AlertDialog.Builder(main)
                    .setView(withinSearchDialogView)
                    .create();
            videoSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });
        }
        loadSearchView();
        videoSearchDialog.show();
    }

    //for web manager to call when URL contains with.in/watch
    //could be entered directly or from favourites
    public void showController(String url) {
        foundURL = url;
//        foundURL = foundURL.replace("/watch/", "/embed/"); //moved this to showGuideController
        Log.d(TAG, "showController: " + foundURL);
        withinSearchDialogView.findViewById(R.id.select_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_active, null));
        showGuideController(true);
    }

    private void showGuideController(boolean isFresh) {
        updateControllerUI(!isFresh);
        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(withinControllerDialogView)
                    .create();
            videoControlDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });
        }

        if (isFresh) {
            //update buttons
            if (!main.getConnectedLearnersAdapter().someoneIsSelected()) {
                //if no-one is selected, prompt to push to everyone
                pushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone));
            } else {
                //if someone is selected, prompt to push to selected
                pushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));
            }

            stream = true;
            toggleStreamBtn();

            vrMode = true;
            toggleVRBtn();

            pageLoaded = false; //reset flag
            loadVideoGuideURL(foundURL.replace("/watch/", "/embed/")); //display embedded version
            //controllerWebView.scrollTo(0, 200);
        }

        videoControlDialog.show();
        //return to main screen
        main.hideConfirmPushDialog();
    }

    private void loadVideoGuideURL(String url) {
        if (main.getPermissionsManager().isInternetConnectionAvailable()) {
            internetUnavailableMsg.setVisibility(View.GONE);
            controllerWebView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Attempting to load " + url + " on controller");
            controllerWebView.loadDataWithBaseURL(null, getiFrameData(url), "text/html", "UTF-8", null);

            //update check if appropriate
            favCheck.setEnabled(true);
            favCheck.setChecked(main.getWebManager().getYouTubeFavouritesManager().isInFavourites(foundURL));

        } else {
            internetUnavailableMsg.setVisibility(View.VISIBLE);
            controllerWebView.setVisibility(View.GONE);
        }
    }

    private void loadSearchView() {
        resetControllerState();
        if (main.getPermissionsManager().isInternetConnectionAvailable()) {
            searchUnavailableMsg.setVisibility(View.GONE);
            searchWebView.setVisibility(View.VISIBLE);
            searchWebView.loadDataWithBaseURL(null, getiFrameData("https://www.with.in/experiences"), "text/html", "UTF-8", null);
        } else {
            searchUnavailableMsg.setVisibility(View.VISIBLE);
            searchWebView.setVisibility(View.GONE);
        }
    }

    public String getiFrameData(String url) {
        InputStream htmlTemplate = main.getResources().openRawResource(R.raw.embed_within_player);
        Scanner scanner = new Scanner(htmlTemplate);
        String output = "";
        while (scanner.hasNext()) {
            output += scanner.nextLine() + "\n";
        }
        output = output.replace("PLACEHOLDER_URL", url);
        return output;
    }

    public void resetControllerState() {
        videoCurrentDisplayMode = STD_MODE;
        foundURL = "";
        foundTitle = "";
        attemptedURL = "";

        withinSearchDialogView.findViewById(R.id.select_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_disabled, null));
    }

}
