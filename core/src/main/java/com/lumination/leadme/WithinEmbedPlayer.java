package com.lumination.leadme;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.Scanner;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

public class WithinEmbedPlayer {

    private final static String TAG = "embedPlayerWithin";

    //static variables
    private static final int VR_MODE = 1;
    private static final int STD_MODE = 0;

    //variables to store what the latest request was
    private static int videoCurrentDisplayMode = STD_MODE; //VR, STD

    private AlertDialog videoControlDialog, videoSearchDialog;
    private final Button pushBtn;
    private final TextView repushBtn;
    private final CheckBox favCheck;
    private final View withinControllerDialogView, withinSearchDialogView;
    private final TextView internetUnavailableMsg, searchUnavailableMsg, downModeText;
    public WebView controllerWebView, searchWebView;
    private final Switch vrModeBtn, downModeBtn;
    private final ImageView vrIcon;
    private final Spinner lockSpinner;
    private final Spinner searchSpinner;

    private String attemptedURL = "";
    boolean pageLoaded = false;
    public ViewDataBinding binding;

    private boolean stream = true;
    private boolean vrMode = true;

    private final LeadMeMain main;

    /**
     * USEFUL LINKS
     * <p>
     * https://stackoverflow.com/questions/3298597/how-to-get-return-value-from-javascript-in-webview-of-android
     * https://developers.google.com/youtube/iframe_api_reference
     */

    private final ViewGroup.LayoutParams searchBackupParams, controllerBackupParams;

    public WithinEmbedPlayer(LeadMeMain main) {
        this.main = main;
        withinSearchDialogView = View.inflate(main, R.layout.f__selection_popup_within, null);
        binding = DataBindingUtil.bind(withinSearchDialogView);
        binding.setLifecycleOwner(main);
        binding.setVariable(BR.foundURL, foundURL);
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
        downModeBtn = withinControllerDialogView.findViewById(R.id.down_mode_toggle);
        downModeText = withinControllerDialogView.findViewById(R.id.predowload_text);
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
        main.getDialogManager().setupPushToggle(withinControllerDialogView, false);

        withinSearchDialogView.findViewById(R.id.open_favourites).setOnClickListener(v -> {
            main.closeKeyboard();
            main.hideSystemUI();
            videoSearchDialog.dismiss();
            main.getWebManager().launchUrlYtFavourites();
        });

        searchSpinner = (Spinner) withinSearchDialogView.findViewById(R.id.search_spinner);
        String[] searchSpinnerItems = new String[3];
        searchSpinnerItems[0] = "Default";
        searchSpinnerItems[1] = "Within search";
        searchSpinnerItems[2] = "YouTube search";
        Integer[] search_imgs = {R.drawable.search_icon_larger, R.drawable.search_within, R.drawable.search_yt};
        LumiSpinnerAdapter search_adapter = new LumiSpinnerAdapter(main, R.layout.row_search_spinner, searchSpinnerItems, search_imgs);
        searchSpinner.setAdapter(search_adapter);

        searchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 2: //youtube search
                        main.closeKeyboard();
                        main.hideSystemUI();
                        videoSearchDialog.dismiss();
                        main.getWebManager().buildAndShowSearchDialog(1);
                        break;
                    case 1:
                        main.closeKeyboard();
                        main.hideSystemUI();
                        videoSearchDialog.dismiss();
                        main.getWebManager().buildAndShowSearchDialog(2);
                        break;
                    default: //default / within
                        //do nothing, we're already here?
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
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

        // chromium, enable hardware acceleration
        tmpWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

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
    public String foundURL = "";
    private String foundTitle = "";

    private String setFoundURL (String value) {
        foundURL = value;
        binding.setVariable(BR.foundURL, foundURL);
        return foundURL;
    }

    private void setupWebClient(WebView tmpWebView, boolean searchView) {
        tmpWebView.setWebContentsDebuggingEnabled(true);
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
                if (searchView && url.startsWith(foundPrefix) && url.endsWith(foundSuffix)) {
                    foundTitle = url.replace(foundPrefix, "").replace(foundSuffix, "");
                    setFoundURL(urlPrefix + foundTitle);
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
        });
    }

    private void setupWithinSearchButtons() {
        Drawable disabledBg = main.getResources().getDrawable(R.drawable.bg_disabled, null);
        Button withinBackBtn = withinSearchDialogView.findViewById(R.id.within_back_btn);

        //set up standard dialog buttons
        withinBackBtn.setOnClickListener(v -> {
            searchWebView.goBack();
            setFoundURL("");
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

    private void showPushConfirmed() {
        videoControlDialog.hide();
        main.hideSystemUI();
        View confirmPushDialogView = View.inflate(main, R.layout.e__confirm_popup, null);
        AlertDialog confirmPopup = new AlertDialog.Builder(main)
                .setView(confirmPushDialogView)
                .show();
        ((TextView)confirmPushDialogView.findViewById(R.id.push_success_comment)).setText("Your video was successfully launched.");

        Button ok = confirmPushDialogView.findViewById(R.id.ok_btn);
        ok.setOnClickListener(v -> {
            confirmPopup.dismiss();
            updateControllerUI(true);
        });
    }

    private void updateControllerUI(boolean isPlaybackController) {
        if (isPlaybackController) {
            videoControlDialog.show();

            withinControllerDialogView.findViewById(R.id.view_mode_controls).setVisibility(View.VISIBLE);
            withinControllerDialogView.findViewById(R.id.basic_controls).setVisibility(View.VISIBLE);
            withinControllerDialogView.findViewById(R.id.playback_btns).setVisibility(View.VISIBLE);
            withinControllerDialogView.findViewById(R.id.vr_status_bar).setVisibility(View.VISIBLE);

            withinControllerDialogView.findViewById(R.id.vr_selection).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.download_buttons).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.within_select_btns).setVisibility(View.GONE);

            repushBtn.setVisibility(View.VISIBLE);
            ((TextView) withinControllerDialogView.findViewById(R.id.title)).setText(main.getResources().getText(R.string.playback_controls_title));

        } else {
            withinControllerDialogView.findViewById(R.id.view_mode_controls).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.basic_controls).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.vr_status_bar).setVisibility(View.GONE);
            withinControllerDialogView.findViewById(R.id.playback_btns).setVisibility(View.GONE);

            withinControllerDialogView.findViewById(R.id.vr_selection).setVisibility(View.VISIBLE);
            withinControllerDialogView.findViewById(R.id.download_buttons).setVisibility(View.VISIBLE);
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
            videoControlDialog.hide();
            main.hideSystemUI();
        });

        withinControllerDialogView.findViewById(R.id.within_back).setOnClickListener(v -> {
            videoControlDialog.hide();
            main.hideSystemUI();
        });

        withinControllerDialogView.findViewById(R.id.mute_btn).setOnClickListener(v -> {
            main.muteAudio(); //this is managed by the main activity
            main.muteLeaners();
        });

        withinControllerDialogView.findViewById(R.id.unmute_btn).setOnClickListener(v -> {
            main.unMuteAudio(); //this is managed by the main activity
            main.unmuteLearners();
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

        downModeBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                downModeBtn.setText("Predownload ON");
                ImageViewCompat.setImageTintList(withinControllerDialogView.findViewById(R.id.download_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_blue)));
                downModeText.setText(R.string.will_download_via_internet);

                stream=false;
            } else {
                downModeBtn.setText("Predownload OFF");
                ImageViewCompat.setImageTintList(withinControllerDialogView.findViewById(R.id.download_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_medium_grey)));
                downModeText.setText(R.string.will_stream_via_internet);

                stream=true;
            }
        });

        Switch viewModeToggle = withinControllerDialogView.findViewById(R.id.view_mode_toggle);
        viewModeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                viewModeToggle.setText("View Mode ON");
                ImageViewCompat.setImageTintList(withinControllerDialogView.findViewById(R.id.view_mode_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_blue)));
                main.lockFromMainAction();
            }else{
                ImageViewCompat.setImageTintList(withinControllerDialogView.findViewById(R.id.view_mode_icon), ColorStateList.valueOf(ContextCompat.getColor(main, R.color.leadme_medium_grey)));
                viewModeToggle.setText("View Mode OFF");
                main.unlockFromMainAction();
            }
        });
    }

    private void pushWithin() {
        attemptedURL = foundURL;
        Log.d(TAG, "Launching WithinVR for students: " + attemptedURL + ", [STR] " + stream + ", [VR] " + vrMode);
        main.getAppManager().launchWithin(attemptedURL, stream, vrMode, main.getSelectedOnly());
        main.updateFollowerCurrentTask(main.getAppManager().withinPackage, "Within VR", "VR Video", attemptedURL, foundTitle);

        //update UI
        showPushConfirmed();

        //add to favourites
        if (favCheck.isChecked()) {
            main.getWebManager().getYouTubeFavouritesManager().addToFavourites(foundURL, foundTitle, null);
        }

        withinControllerDialogView.findViewById(R.id.vr_mode).setVisibility(vrMode ? View.VISIBLE : View.GONE);
        withinControllerDialogView.findViewById(R.id.phone_mode).setVisibility(vrMode ? View.GONE : View.VISIBLE);
    }

    public void showWithin() {
        Log.w(TAG, "Showing WITHIN: " + attemptedURL + ", " + foundURL + ", " + main.getAppManager().withinURI);
        if (attemptedURL.isEmpty()) {
            showWithinSearch();
        } else {
            showGuideController(false);
        }
    }

    private void showWithinSearch() {
        setFoundURL("");
        foundTitle = "";
        attemptedURL = "";
        searchSpinner.setSelection(0);
        if (videoSearchDialog == null) {
            videoSearchDialog = new AlertDialog.Builder(main)
                    .setView(withinSearchDialogView)
                    .create();
            videoSearchDialog.setOnDismissListener(dialog -> main.hideSystemUI());
        }

        main.getDialogManager().toggleSelectedView(withinControllerDialogView);

        loadSearchView();
        videoSearchDialog.show();
    }

    //for web manager to call when URL contains with.in/watch
    //could be entered directly or from favourites
    public void showController(String url) {
        setFoundURL(url);
        Log.d(TAG, "showController: " + foundURL);
        showGuideController(true);
    }

    private void showGuideController(boolean isFresh) {
        updateControllerUI(!isFresh);
        if (videoControlDialog == null) {
            videoControlDialog = new AlertDialog.Builder(main)
                    .setView(withinControllerDialogView)
                    .create();
            videoControlDialog.setOnDismissListener(dialog -> main.hideSystemUI());
        }

        if (isFresh) {
            downModeBtn.setChecked(false);

            vrMode = true;
            toggleVRBtn();

            pageLoaded = false; //reset flag
            setFoundURL(foundURL.replace("/watch/", "/embed/"));
            setFoundURL(foundURL.replace("https", "http"));
            loadVideoGuideURL(foundURL); //display embedded version;
        }

        videoControlDialog.show();
        //return to main screen
        main.getDialogManager().hideConfirmPushDialog();
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
        setFoundURL("");
        foundTitle = "";
        attemptedURL = "";
    }

    public void onDestroy() {
        controllerWebView.clearHistory();
        controllerWebView.clearCache(true);
        controllerWebView.loadUrl("about:blank");
        controllerWebView.onPause();
        controllerWebView.removeAllViews();
        controllerWebView.destroyDrawingCache();
        controllerWebView.pauseTimers();
        controllerWebView.destroy();
        controllerWebView = null;

        searchWebView.clearHistory();
        searchWebView.clearCache(true);
        searchWebView.loadUrl("about:blank");
        searchWebView.onPause();
        searchWebView.removeAllViews();
        searchWebView.destroyDrawingCache();
        searchWebView.pauseTimers();
        searchWebView.destroy();
        searchWebView = null;
    }
}
