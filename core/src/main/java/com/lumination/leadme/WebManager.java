package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lumination.leadme.linkpreview.LinkPreviewCallback;
import com.lumination.leadme.linkpreview.SourceContent;
import com.lumination.leadme.linkpreview.TextCrawler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebManager {

    //tag for debugging
    private static final String TAG = "WebManager";
    private TextCrawler textCrawler = new TextCrawler(this);

    private AlertDialog websiteLaunchDialog, previewDialog, urlYtFavDialog, searchDialog;
    private final View websiteLaunchDialogView;
    private final View previewDialogView;
    private final View searchDialogView;
    public boolean launchingVR = false, enteredVR = false;
    public boolean lastWasGuideView = false;

    private ImageView previewImage;
    private TextView previewTitle;
    private TextView previewMessage;
    private ProgressBar previewProgress;
    private Button previewPushBtn;
    private boolean isYouTube = false;
    private boolean isWithin = false;
    private String pushURL = "";
    private String pushTitle = "";
    String controllerURL = "";

    private View webYouTubeFavView;
    private FavouritesManager urlFavouritesManager;
    private FavouritesManager youTubeFavouritesManager;

    private LeadMeMain main;
    private CheckBox favCheckbox;

    private Spinner lockSpinner, searchSpinner;
    private View lockSpinnerParent;
    private String[] searchSpinnerItems, lockSpinnerItems;

    private YouTubeEmbedPlayer youTubeEmbedPlayer;

    public Thread thread;

    //this entire thing is in progress
    public WebManager(LeadMeMain main) {
        Log.d(TAG, "WebManager: ");
        this.main = main;
        thread = Thread.currentThread();
        youTubeEmbedPlayer = new YouTubeEmbedPlayer(main, this);

        websiteLaunchDialogView = View.inflate(main, R.layout.d__enter_url, null);
        previewDialogView = View.inflate(main, R.layout.e__preview_url_push, null);
        searchDialogView = View.inflate(main, R.layout.e__preview_url_search, null);

        //set up lock spinner
        lockSpinnerParent = previewDialogView.findViewById(R.id.lock_spinner);
        lockSpinner = (Spinner) previewDialogView.findViewById(R.id.push_spinner);
        lockSpinnerItems = new String[2];
        lockSpinnerItems[0] = "View only";
        lockSpinnerItems[1] = "Free play";
        Integer[] push_imgs = {R.drawable.controls_view, R.drawable.controls_play};
        LumiSpinnerAdapter push_adapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, lockSpinnerItems, push_imgs);
        lockSpinner.setAdapter(push_adapter);
        lockSpinner.setSelection(0); //default to locked

        //set up search spinner
        //TODO add Vimeo search
        searchSpinner = (Spinner) searchDialogView.findViewById(R.id.search_spinner);
        searchSpinnerItems = new String[3];
        searchSpinnerItems[0] = "Google search";
        searchSpinnerItems[1] = "YouTube search";
        searchSpinnerItems[2] = "Within search";
        Integer[] search_imgs = {R.drawable.search_google, R.drawable.search_yt, R.drawable.search_within};
        LumiSpinnerAdapter search_adapter = new LumiSpinnerAdapter(main, R.layout.row_search_spinner, searchSpinnerItems, search_imgs);
        searchSpinner.setAdapter(search_adapter);

        //set up favourites view
        webYouTubeFavView = View.inflate(main, R.layout.d__url_yt_favourites, null);
        favCheckbox = previewDialogView.findViewById(R.id.fav_checkbox);
        setupWarningDialog();


        websiteLaunchDialogView.findViewById(R.id.url_search_btn).setOnClickListener(v -> {
            hidePreviewDialog();
            buildAndShowSearchDialog(SEARCH_WEB);
        });

        urlFavouritesManager = new FavouritesManager(main, this, FavouritesManager.FAVTYPE_URL, 10);
        youTubeFavouritesManager = new FavouritesManager(main, this, FavouritesManager.FAVTYPE_YT, 10);

        ((GridView) webYouTubeFavView.findViewById(R.id.yt_favourites)).setAdapter(getYouTubeFavouritesManager());
        ((GridView) webYouTubeFavView.findViewById(R.id.url_favourites)).setAdapter(getUrlFavouritesManager());

        webYouTubeFavView.findViewById(R.id.clear_fav_btn).setOnClickListener(v -> {
            showClearWebFavDialog(CLEAR_ALL);
            getYouTubeFavouritesManager().clearFavourites();
            getUrlFavouritesManager().clearFavourites();
        });

        setupViews();
        setupPreviewDialog();
        setupWebLaunchDialog();
    }

    private void setErrorPreview(String searchTerm) {
        Log.d(TAG, "setErrorPreview: ");
        final SearchView searchView = searchDialogView.findViewById(R.id.url_search_bar);
        searchView.setQuery(searchTerm, false);
        //searchYoutube=false;
        buildAndShowSearchDialog();
        searchDialogView.findViewById(R.id.web_search_title).setVisibility(View.GONE);
        searchDialogView.findViewById(R.id.url_error_layout).setVisibility(View.VISIBLE);
        //searchYoutube=false;
        searchType = SEARCH_WEB;
        searchView.setQuery(searchTerm, true);
    }

    private boolean error = false;
    private boolean generatingPreview = false;
    // Create the callbacks to handle pre and post execution of the preview
    LinkPreviewCallback linkPreviewCallback = new LinkPreviewCallback() {

        @Override
        public void onPre() {
            // Any work that needs to be done before generating the preview. Usually inflate
            // your custom preview layout here.
            error = false; //clear error flag
        }

        @Override
        public void onPos(SourceContent sourceContent, boolean b) {
            Log.d(TAG, "onPos: ");
            if (!sourceContent.isSuccess()) {
                hidePreviewDialog();
                String searchTerm = sourceContent.getUrl().replace("http://", "").replace("https://", "").replace("www.", "").replace(".com/", "");
                Log.d(TAG, "UnknownHostHandler: search: " + searchTerm);
                isYouTube = false;
                setErrorPreview(searchTerm);
            } else {
                generatingPreview = false;
                // Populate your preview layout with the results of sourceContent.
                String title = sourceContent.getTitle();
                if (title.isEmpty()) {
                    title = sourceContent.getFinalUrl();
                }

                //store the title
                pushTitle = title;
                Log.e(TAG, "CALLBACK] Got a title! " + pushTitle);

                //update preview
                previewTitle.setText(title);
                previewTitle.setVisibility(View.VISIBLE);

                String icon;
                if (!sourceContent.getImages().isEmpty()) {
                    icon = sourceContent.getImages().get(0);
                } else {
                    icon = "";
                    previewMessage.setVisibility(View.VISIBLE);
                }

                try {
                    UrlImageViewHelper.setUrlDrawable(previewImage, icon, (imageView, loadedBitmap, url, loadedFromCache) -> {

                        if (isYouTube) {
                            youTubeFavouritesManager.updateTitle(url, previewTitle.getText().toString());
                            youTubeEmbedPlayer.updateTitle(previewTitle.getText().toString());
                        } else {
                            urlFavouritesManager.updateTitle(url, previewTitle.getText().toString());
                        }

                        if (loadedBitmap != null) {
                            previewImage.setVisibility(View.VISIBLE); //show image
                            if (isYouTube) {
                                youTubeFavouritesManager.updatePreview(url, previewImage.getDrawable());
                            } else {
                                urlFavouritesManager.updatePreview(url, previewImage.getDrawable());
                            }

                        } else {
                            Log.d(TAG, "onPos: ERROR URL not valid");
                            previewMessage.setVisibility(View.VISIBLE); //show error
                        }
                    });

                    getUrlFavouritesManager().notifyDataSetChanged();
                    getYouTubeFavouritesManager().notifyDataSetChanged();


                } catch (Exception e) {
                    Log.e(TAG, "Error launching URL: " + e.getMessage());
                    e.printStackTrace();
                    main.showWarningDialog(main.getResources().getString(R.string.warning_couldnt_launch_url));
                    error = true; //set error flag
                    hidePreviewDialog();
                }

                previewProgress.setVisibility(View.GONE); //we're done loading

            }
        }

    };

    private final static List<ComponentName> browserComponents = new ArrayList<ComponentName>() {{
        add(new ComponentName("com.android.chrome", "com.google.android.apps.chrome.IntentDispatcher")); //preferred browser
        add(new ComponentName("com.google.android.browser", "com.google.android.browser.BrowserActivity"));
        add(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
    }};


    public FavouritesManager getUrlFavouritesManager() {
        return urlFavouritesManager;
    }

    public FavouritesManager getYouTubeFavouritesManager() {
        return youTubeFavouritesManager;
    }

    private void setupViews() {
        Log.d(TAG, "setupViews: ");
        webYouTubeFavView.findViewById(R.id.yt_add_btn).setOnClickListener(v -> {
            isYouTube = true;
            Log.w(TAG, "YouTube add! " + isYouTube);
            showWebLaunchDialog(true);
            urlYtFavDialog.dismiss();
        });

        webYouTubeFavView.findViewById(R.id.url_add_btn).setOnClickListener(v -> {
            isYouTube = false;
            Log.w(TAG, "URL add! " + isYouTube);
            showWebLaunchDialog(true);
            urlYtFavDialog.dismiss();
        });

        webYouTubeFavView.findViewById(R.id.yt_del_btn).setOnClickListener(v -> showClearWebFavDialog(CLEAR_VID));
        webYouTubeFavView.findViewById(R.id.url_del_btn).setOnClickListener(v -> showClearWebFavDialog(CLEAR_URL));
    }


    final private static int CLEAR_ALL = 0;
    final private static int CLEAR_VID = 1;
    final private static int CLEAR_URL = 2;
    private int whatToClear = -1;
    private TextView warningTextView;
    private AlertDialog warningDialog;

    private void setupWarningDialog() {
        Log.d(TAG, "setupWarningDialog: ");
        View warningDialogView = View.inflate(main, R.layout.e__fav_clear_confirmation_popup, null);
        warningTextView = warningDialogView.findViewById(R.id.favclear_comment);

        warningDialogView.findViewById(R.id.ok_btn).setOnClickListener(v -> {
            switch (whatToClear) {
                case CLEAR_ALL:
                    getYouTubeFavouritesManager().clearFavourites();
                    getUrlFavouritesManager().clearFavourites();
                    break;

                case CLEAR_VID:
                    getYouTubeFavouritesManager().clearFavourites();
                    break;

                case CLEAR_URL:
                    getUrlFavouritesManager().clearFavourites();
                    break;
            }
            warningDialog.dismiss();
            getUrlFavouritesManager().notifyDataSetChanged();
            getYouTubeFavouritesManager().notifyDataSetChanged();
        });

        warningDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> warningDialog.dismiss());

        warningDialog = new AlertDialog.Builder(main)
                .setView(warningDialogView)
                .create();
        warningDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                main.hideSystemUI();
            }
        });

    }

    private void showClearWebFavDialog(int whatToClear) {
        Log.d(TAG, "showClearWebFavDialog: ");
        String message = "";
        this.whatToClear = whatToClear;
        switch (whatToClear) {
            case CLEAR_ALL:
                message = main.getResources().getString(R.string.delete_videos_and_websites_confirm);
                break;

            case CLEAR_VID:
                message = main.getResources().getString(R.string.delete_videos_confirm);
                break;

            case CLEAR_URL:
                message = main.getResources().getString(R.string.delete_websites_confirm);
                break;
        }

        warningTextView.setText(message);
        warningDialog.show();
    }

    private void setupPreviewDialog() {
        Log.d(TAG, "setupPreviewDialog: ");
        previewImage = previewDialogView.findViewById(R.id.preview_image);
        previewTitle = previewDialogView.findViewById(R.id.popup_title);
        previewMessage = previewDialogView.findViewById(R.id.preview_message);
        previewProgress = previewDialogView.findViewById(R.id.preview_progress);
        previewPushBtn = previewDialogView.findViewById(R.id.push_btn);

        final CheckBox saveWebToFav = previewDialogView.findViewById(R.id.fav_checkbox);
        setupPushToggle();
        previewPushBtn.setOnClickListener(v -> {
            //save to favourites if needed
            if (/*adding_to_fav ||*/ saveWebToFav.isChecked()) {
                if (isYouTube) {
                    getYouTubeFavouritesManager().addCurrentPreviewToFavourites();
                } else {
                    getUrlFavouritesManager().addCurrentPreviewToFavourites();
                }
            }

            //if we're not only saving to favourites, push it to learners
            if (!adding_to_fav) {
                //retrieve appropriate list of receivers
                pushURL(pushURL, pushTitle);
            }

            //clean up dialogs
            hideSearchDialog();
            hidePreviewDialog();
            main.showConfirmPushDialog(false, adding_to_fav);

            //reset
            pushURL = "";
            pushTitle = "";
            previewTitle.setText("");
            previewImage.setImageDrawable(null);

        });

        previewDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> {
            hidePreviewDialog();
            showWebLaunchDialog(adding_to_fav);
        });
    }

    protected View getWebYouTubeFavView() {
        return webYouTubeFavView;
    }

    protected ImageView getPreviewImageView() {
        return previewImage;
    }

    protected Drawable getPreviewImage() {
        return previewImage.getDrawable();
    }

    protected String getPreviewTitle() {
        return previewTitle.getText().toString().trim();
    }

    protected String getPushURL() {
        return pushURL;
    }

    protected void reset() {
        pushURL = "";
    }

    protected String getLaunchTitle() {
        return pushTitle;
    }

    private boolean freshPlay = true;

    protected boolean isFreshPlay() {
        return freshPlay;
    }

    protected void setFreshPlay(boolean freshPlay) {
        Log.d(TAG, "setFreshPlay: ");
        this.freshPlay = freshPlay;
        if (freshPlay) {
            //also reset the state
            main.getLumiAccessibilityConnector().resetState();
        }
    }

    public void pushYouTube(String url, String urlTitle, int startFrom, boolean locked, boolean vrOn, boolean selectedOnly) {
        Log.d(TAG, "pushYouTube: ");
        pushURL = url;
        pushTitle = urlTitle;
        main.getLumiAccessibilityConnector().resetState();

        if (urlTitle.isEmpty()) {
            urlTitle = " ";
        }

        //update lock status
        if (locked) {
            if(selectedOnly) {
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
            }else{
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, main.getNearbyManager().getAllPeerIDs());
            }
        } else {
            //unlocked if selected
            if(selectedOnly) {
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
            }else{
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, main.getNearbyManager().getAllPeerIDs());
            }
        }

        //push the right instruction to the receivers
        if(selectedOnly) {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_YT + url + "&start=" + startFrom + ":::" + urlTitle + ":::" + vrOn,
                    main.getNearbyManager().getSelectedPeerIDsOrAll());
        }else{
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_YT + url + "&start=" + startFrom + ":::" + urlTitle + ":::" + vrOn,
                    main.getNearbyManager().getAllPeerIDs());
        }
    }


    public void pushURL(String url, String urlTitle) {
        Log.d(TAG, "pushURL: ");
        //update lock status
        if (lockSpinner.getSelectedItem().toString().startsWith("View")) {
            //locked by default
            if(selectedOnly) {
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
            }else{
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, main.getNearbyManager().getAllPeerIDs());
            }
        } else {
            //unlocked if selected
            if(selectedOnly) {
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, main.getNearbyManager().getSelectedPeerIDsOrAll());
            }else{
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, main.getNearbyManager().getAllPeerIDs());
            }
        }

        //push the right instruction to the receivers
        if(selectedOnly) {
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_URL + url + ":::" + urlTitle, main.getNearbyManager().getSelectedPeerIDsOrAll());
        }else{
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_URL + url + ":::" + urlTitle, main.getNearbyManager().getAllPeerIDs());
        }
    }

    private void hidePreviewDialog() {
        Log.d(TAG, "hidePreviewDialog: ");
        main.closeKeyboard();
        main.hideSystemUI();

        if (previewDialog != null) {
            previewDialog.dismiss();
            error = false; //reset flag
        }
    }

    private void hideSearchDialog() {
        Log.d(TAG, "hideSearchDialog: ");
        main.closeKeyboard();
        main.hideSystemUI();

        if (searchDialog != null) {
            searchDialog.dismiss();
            error = false; //reset flag
        }
    }

    public void launchWebsite(String url, String urlTitle, boolean updateCurrentTask) {
        Log.d(TAG, "launchWebsite: ");
        String finalUrl = url;
        pushTitle = urlTitle;
        pushURL = url;
        freshPlay = true;
        lastWasGuideView = false;
        main.getLumiAccessibilityConnector().resetState();
        //new Thread(() -> {
        main.backgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (!main.getPermissionsManager().isInternetConnectionAvailable()) {
                    Log.w(TAG, "No internet connection in LaunchWebsite");
                    main.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(main, "Can't launch URL, no Internet connection.", Toast.LENGTH_SHORT).show();
                            main.showWarningDialog("No Internet Connection",
                                    "Internet based functions are unavailable at this time. " +
                                            "Please check your WiFi connection and try again.");
                            main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_INTERNET, false);
                            hideWebsiteLaunchDialog();
                        }
                    });
                }
            }
        });

//        }).start();

        main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_INTERNET, true); //reset it

        //check it's a minimally sensible url
        if (url == null || url.length() < 3 || !url.contains(".")) {
            Toast toast = Toast.makeText(main, "Invalid URL", Toast.LENGTH_SHORT);
            toast.show();
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_FAILED + "Invalid URL:" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDsOrAll());
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        PackageManager pm = main.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);

        if (url.contains("with.in/watch")) {
            intent.setPackage(main.getAppManager().withinPackage);
            Uri uri = Uri.parse(url);
            intent.setData(uri);

            if (intent.resolveActivityInfo(pm, 0) != null) {
                main.startActivity(intent);
                return;
            } else {
                intent.setPackage(null); //remove this
            }
        }

        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Uri uri = Uri.parse(url);
        intent.setData(uri);

        for (ComponentName cn : browserComponents) {
            intent.setComponent(cn);
            ActivityInfo ai = intent.resolveActivityInfo(pm, 0);
            if (ai != null) {
                ApplicationInfo appInfo = ai.applicationInfo;
                String name = pm.getApplicationLabel(appInfo).toString();
                Log.w(TAG, "Selecting browser:  " + ai + " for " + uri.getHost() + ", " + ai.name + ", " + name);

                scheduleActivityLaunch(intent, updateCurrentTask, ai.packageName, name, "Website", url, urlTitle);
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                        LeadMeMain.LAUNCH_SUCCESS + uri.getHost() + ":" + main.getNearbyManager().getID() + ":" + ai.packageName, main.getNearbyManager().getAllPeerIDs());
                //success!
                return;
            }
        }

        // no browser from preferred list, so find default
        Intent browserIntent = getBrowserIntent(url);

        if (browserIntent != null) {
            scheduleActivityLaunch(browserIntent, updateCurrentTask, browserIntent.getStringExtra("packageName"), "Default Browser", "Website", url, urlTitle);
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.LAUNCH_SUCCESS + uri.getHost() + ":" + main.getNearbyManager().getID() + ":" + browserIntent.getStringExtra("packageName"), main.getNearbyManager().getAllPeerIDs());
            //success!
            return;

        } else {
            Toast toast = Toast.makeText(main, "No browser available", Toast.LENGTH_SHORT);
            toast.show();
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.AUTO_INSTALL_FAILED + "No browser:" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDsOrAll());
            //no browser, failure
        }
    }

    Intent getBrowserIntent(String url) {
        Log.d(TAG, "getBrowserIntent: ");
        PackageManager pm = main.getPackageManager();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(url);
        browserIntent.setDataAndType(uri, "text/html");
        List<ResolveInfo> list = pm.queryIntentActivities(browserIntent, 0);
        for (ResolveInfo resolveInfo : list) {
            browserIntent = pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName);
            browserIntent.setAction(Intent.ACTION_VIEW);
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            browserIntent.putExtra("packageName", resolveInfo.activityInfo.packageName);
            browserIntent.putExtra("label", resolveInfo.activityInfo.name);
            browserIntent.setData(uri);
            return browserIntent;
        }
        return null;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public String getYouTubeID(String youTubeUrl) {
        Log.d(TAG, "getYouTubeID: ");
        if (!youTubeUrl.toLowerCase().contains("youtu.be") && !youTubeUrl.toLowerCase().contains("youtube.com") && !youTubeUrl.toLowerCase().contains("youtube-nocookie.com")) {
            Log.w(TAG, "Not a YouTube URL! " + youTubeUrl);
            return "";
        }
        //String pattern = "(?<=youtu.be/|watch?v=|/w/|/videos/|embed/)[^#&?]*";
        String pattern = "(?:http:|https:)*?//(?:www\\.|)(?:youtube.com|m.youtube.com|youtu.|youtube-nocookie.com).*(?:v=|v%3D|v/|[ap]/[au]/\\d.*/|watch/|/w/|vi[=/]|/embed/|oembed\\?|be/|e/)([^&?%#/\\n]*)";
        //String pattern = "(?:http:|https:)*?//(?:www\\.|)(?:youtube.com|m.youtube.com|youtu.|youtube-nocookie.com).*(?:v=|v%3D|v/|[ap]/[au]/\\d.*/|watch\\?|/w/|vi[=/]|/embed/|oembed\\?|be/|e/)([^&?%#/\\n]*)";
        //String pattern = "(?:http:|https:)*?\\/\\/(?:www\\.|)(?:youtube\\.com|m\\.youtube\\.com|youtu\\.|youtube-nocookie\\.com).*(?:v=|v%3D|v\\/|(?:a|p)\\/(?:a|u)\\/\\d.*\\/|watch\\?|\\/w\\/|vi(?:=|\\/)|\\/embed\\/|oembed\\?|be\\/|e\\/)([^&?%#\\/\\n]*)";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(youTubeUrl);

        String res = "";
        if (matcher.find()) {
            res = matcher.group(1);
        }

        if (res != null && res.length() > 0) {
            //Log.d(TAG, "YouTube ID = " + res);
            return res;
        } else {
            Log.w(TAG, "Couldn't extract YouTube ID");
            return "";
        }
    }

    private void showYouTubePreview(String url) {
        Log.d(TAG, "showYouTubePreview: ");
        isYouTube = true;
        //first position is 'locked' - default for YouTube
        lockSpinner.setSelection(0);
        buildAndShowPreviewDialog(url);
    }

    private void showWebsitePreview(String url) {
        Log.d(TAG, "showWebsitePreview: ");
        isYouTube = false;
        //second position is 'unlocked' - default for website
        lockSpinner.setSelection(1);
        buildAndShowPreviewDialog(url);
    }

    private void buildAndShowPreviewDialog(String url) {
        Log.d(TAG, "buildAndShowPreviewDialog: ");
        if (error) {
            error = false; //reset error flag
            return;
        }

        main.closeKeyboard();

        pushURL = url;
        controllerURL = url;

        if (isYouTube) {
            lockSpinner.setSelection(0); //default to locked
            pushTitle = getYouTubeFavouritesManager().getTitle(url);
            previewDialogView.findViewById(R.id.preview_youtube).setVisibility(View.VISIBLE);
            previewDialogView.findViewById(R.id.preview_web).setVisibility(View.GONE);
        } else {
            lockSpinner.setSelection(1); //default to unlocked
            pushTitle = getUrlFavouritesManager().getTitle(url);
            previewDialogView.findViewById(R.id.preview_web).setVisibility(View.VISIBLE);
            previewDialogView.findViewById(R.id.preview_youtube).setVisibility(View.GONE);
        }

        if (pushTitle == null && !previewTitle.getText().toString().equals("Website title")) {
            pushTitle = previewTitle.getText().toString();
        }


        //set up preview to appear correctly
        if (adding_to_fav) {
           // lockSpinnerParent.setVisibility(View.INVISIBLE);
            previewPushBtn.setText(main.getResources().getString(R.string.add_this_app_to_favourites));
            favCheckbox.setChecked(true);
            favCheckbox.setVisibility(View.GONE);

        } else if (isYouTube) {
            favCheckbox.setChecked(youTubeFavouritesManager.isInFavourites(url));
            youTubeEmbedPlayer.showPlaybackPreview(pushURL, pushTitle);
            return;

        } else {
            //lockSpinnerParent.setVisibility(View.VISIBLE);
            if (main.getConnectedLearnersAdapter().someoneIsSelected()) {
                previewPushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));
            } else {
                previewPushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone));
            }
            favCheckbox.setChecked(urlFavouritesManager.isInFavourites(url));
            favCheckbox.setVisibility(View.VISIBLE);
        }

        if (previewDialog == null) {
            previewDialog = new AlertDialog.Builder(main)
                    .setView(previewDialogView)
                    .show();
            previewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });
        } else {
            previewDialog.show();
        }
    }

    boolean adding_to_fav = false;

    void showWebLaunchDialog(boolean isYT, boolean add_fav_mode) {
        Log.d(TAG, "showWebLaunchDialog: ");
        isYouTube = isYT;
        showWebLaunchDialog(add_fav_mode);
    }

    void showWebLaunchDialog(boolean add_fav_mode) {
        Log.d(TAG, "showWebLaunchDialog: ");
        if (isYouTube && lastWasGuideView) {
            youTubeEmbedPlayer.showVideoController(); //null, null);
            return;
        }

        if (websiteLaunchDialog == null) {
            websiteLaunchDialog = new AlertDialog.Builder(main)
                    .setView(websiteLaunchDialogView)
                    .create();
            websiteLaunchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });
        }

        adding_to_fav = add_fav_mode;

        websiteLaunchDialog.show();
        main.openKeyboard();
    }

    private void setupWebLaunchDialog() {
        Log.d(TAG, "setupWebLaunchDialog: ");
        //((TextView) websiteLaunchDialogView.findViewById(R.id.url_input_field)).setText("https://www.youtube.com/watch?v=sPyAQQklc1s"); //sample for testing
        //((TextView) websiteLaunchDialogView.findViewById(R.id.url_input_field)).setText("https://www.youtube.com/w/SEbqkn1TWTA"); //sample for testing

        websiteLaunchDialogView.findViewById(R.id.paste_from_clipboard).setOnClickListener(v -> {
            main.closeKeyboard();
            main.hideSystemUI();
            ClipboardManager clipboard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence pasteData;

            //if it does contain data, test if we can handle it
            if (clipboard.hasPrimaryClip()) {
                try {
                    //retrieve the data
                    pasteData = clipboard.getPrimaryClip().getItemAt(0).getText();
                } catch (Exception e) {
                    return;
                }

                //puts the pasted data into the URL field
                ((TextView) websiteLaunchDialogView.findViewById(R.id.url_input_field)).setText(pasteData);
            }
        });

        websiteLaunchDialogView.findViewById(R.id.confirm_btn).setOnClickListener(v -> {
            String url = ((TextView) websiteLaunchDialogView.findViewById(R.id.url_input_field)).getText().toString();
            if (url.length() == 0) {
                return;
            }
            showPreview(url);
        });

        websiteLaunchDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> {
            main.closeKeyboard();
            main.hideSystemUI();
            websiteLaunchDialog.dismiss();
        });

        websiteLaunchDialogView.findViewById(R.id.open_favourites).setOnClickListener(v -> {
            main.closeKeyboard();
            main.hideSystemUI();
            websiteLaunchDialog.dismiss();
            launchUrlYtFavourites();
        });

    }

    protected void showPreview(String url) {
        Log.d(TAG, "showPreview: ");
        Log.d(TAG, "showPreview: ");
        main.closeKeyboard();
        main.hideSystemUI();

//        new Thread(() -> {
        main.backgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (!main.getPermissionsManager().isInternetConnectionAvailable()) {
                    Log.w(TAG, "No internet connection in showPreview");
                    main.getHandler().post(() -> {
                        main.showWarningDialog("No Internet Connection",
                                "Internet based functions are unavailable at this time. " +
                                        "Please check your WiFi connection and try again.");
                    });
                }
            }
        });

//        }).start();


        url = assistWithUrl(url);

        if (!URLUtil.isValidUrl(url)) {
            if (url.contains(".")) {
                if (!url.startsWith("http")) {
                    url = "http://" + url; //append a protocol
                    //url=url.replace("www.","http://www.");
                }
                url = URLUtil.guessUrl(url); //do a bit more checking/auto-fixing
            }
            if (!URLUtil.isValidUrl(url)) {
                hidePreviewDialog();
                String searchTerm = url.replace("http://", "").replace("https://", "").replace("www.", "").replace(".com/", "");
                Log.d(TAG, "UnknownHostHandler: search: " + searchTerm);
                isYouTube = false;
                setErrorPreview(searchTerm);
                return;
            }

        }


        String youTubeId = getYouTubeID(url);
        if (!youTubeId.isEmpty()) {
            isYouTube = true;
        }

        if (url.contains("with.in/watch/")) {
            Log.w(TAG, "This is a Within VR video!");
            main.getAppManager().getWithinPlayer().showController(url);
            return;
        }

        //hide preview image and title
        previewProgress.setVisibility(View.VISIBLE);
        previewMessage.setVisibility(View.GONE);
        previewImage.setVisibility(View.GONE);
        previewTitle.setVisibility(View.GONE);

        //reset these
        previewTitle.setText("");
        previewMessage.setText("");
        previewImage.setImageDrawable(null);

        Drawable preview = null;
        String title = null;
        if (isYouTube) {
            title = youTubeFavouritesManager.getTitle(url);
            preview = youTubeFavouritesManager.getPreview(url);
        } else {
            title = urlFavouritesManager.getTitle(url);
            preview = urlFavouritesManager.getPreview(url);
        }

        if (preview == null || title == null) {
            //generate correct information
            generatingPreview = true;
            textCrawler.makePreview(linkPreviewCallback, url);
        }

        if (title != null) {
            previewTitle.setText(title);
            previewTitle.setVisibility(View.VISIBLE);
        }

        if (preview != null) {
            //use stored preview
            previewImage.setImageDrawable(preview);
            previewProgress.setVisibility(View.GONE);
            previewImage.setVisibility(View.VISIBLE); //show image
            previewImage.invalidate(); //refresh
        }

        Log.w(TAG, "Is it a youtube preview? " + youTubeId);
        if (!youTubeId.isEmpty()) {
            hideWebsiteLaunchDialog();
            showYouTubePreview(url);
        } else {
            hideWebsiteLaunchDialog();
            showWebsitePreview(url);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private String assistWithUrl(String origUrl) {
        Log.d(TAG, "assistWithUrl: ");
        if (!origUrl.contains("edu.cospaces.io") && origUrl.contains("cospaces.io")) {
            return origUrl.replace("cospaces.io", "edu.cospaces.io");
        }
        //no known issues
        return origUrl;
    }

    void hideFavDialog() {
        Log.d(TAG, "hideFavDialog: ");
        main.closeKeyboard();
        main.hideSystemUI();
        urlYtFavDialog.dismiss();
    }

    void launchUrlYtFavourites() {
        Log.d(TAG, "launchUrlYtFavourites: ");
        getUrlFavouritesManager().clearPreviews();
        getUrlFavouritesManager().notifyDataSetChanged();
        getYouTubeFavouritesManager().clearPreviews();
        getYouTubeFavouritesManager().notifyDataSetChanged();

        if (urlYtFavDialog == null) {
            urlYtFavDialog = new AlertDialog.Builder(main)
                    .setView(webYouTubeFavView)
                    .create();
            urlYtFavDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });

            webYouTubeFavView.findViewById(R.id.back_btn).setOnClickListener(v -> urlYtFavDialog.dismiss());
        }

        urlYtFavDialog.show();
    }

    private void hideWebsiteLaunchDialog() {
        Log.d(TAG, "hideWebsiteLaunchDialog: ");
        main.closeKeyboard();
        main.hideSystemUI();
        if (websiteLaunchDialog != null) {
            websiteLaunchDialog.dismiss();
        }
    }

    public String cleanYouTubeURLWithoutStart(String url) {
        Log.d(TAG, "cleanYouTubeURLWithoutStart: ");
        String id = getYouTubeID(url);
        //Log.i(TAG, "YouTube ID = " + id + " from " + url);
        if (id.isEmpty()) {
            return "";
        }
        String finalURL = "https://www.youtube.com/watch/" + id + suffix;
        Log.d(TAG, "Final URL: " + finalURL);
        return finalURL;
    }

    public String cleanYouTubeURL(String url) {
        Log.d(TAG, "cleanYouTubeURL: ");
        String id = getYouTubeID(url);
        //Log.i(TAG, "YouTube ID = " + id + " from " + url);
        if (id.isEmpty()) {
            return "";
        }
        String startSubstring = "";
        if (url.contains("&start=")) {
            int startIndex = url.indexOf("&start=", 0) + 7;
            int endIndex = url.indexOf("&", startIndex);
            if (endIndex == -1) {
                endIndex = url.length();
            }
            String val = url.substring(startIndex, endIndex);
            if (val.equals("0")) {
                val = "1";
            }
            startSubstring = "&start=" + val + "&t=" + val;
            //Log.d(TAG, "[1] Found a START tag! \"" + startSubstring + "\", " + startIndex + ", " + endIndex + ", " + url.length());
        } else {
            startSubstring = "&t=1";
        }
        String finalURL = "https://www.youtube.com/watch/" + id + suffix + startSubstring;
        Log.d(TAG, "Final URL: " + finalURL);
        return finalURL;
    }

    public YouTubeEmbedPlayer getYouTubeEmbedPlayer() {
        return youTubeEmbedPlayer;
    }

    private String suffix = "?rel=0&autoplay=0"; //&autoplay=1&start=1&end=10&controls=0&rel=0";

    public String getSuffix() {
        return suffix;
    }

    public void launchYouTube(String url, String urlTitle, boolean vrOn, boolean updateTask) {
        Log.w(TAG, "Launching: " + url + ", " + urlTitle);
        String cleanURL = cleanYouTubeURL(url);
        if (cleanURL.equals(pushURL)) {
            Log.e(TAG, "This is what we're already playing - ignoring it!");
            return;
        }

        freshPlay = true;
        pushURL = cleanURL;
        pushTitle = urlTitle;
        main.getLumiAccessibilityConnector().resetState();

//        new Thread(() -> {
        main.backgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (!main.getPermissionsManager().isInternetConnectionAvailable()) {
                    Log.w(TAG, "No internet connection in launchYouTube");
                    main.getHandler().post(() -> {
                        main.showWarningDialog("No Internet Connection",
                                "Internet based functions are unavailable at this time. " +
                                        "Please check your WiFi connection and try again.");
                        main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_INTERNET, false);
                        hideWebsiteLaunchDialog();
                    });
                }
            }
        });

//        }).start();

        launchingVR = vrOn; //activate auto-VR mode
        enteredVR = false;
        final String youTubePackageName = main.getAppManager().youtubePackage;

        Log.w(TAG, "CLEAN YOUTUBE: " + pushURL + " || " + launchingVR);

        if (pushURL.isEmpty()) {
            //TODO not sure if this is the right spot to exit on fail
            //could cause other URLs to fail instead of being launched as websites
            Log.e(TAG, "No URL to push!");
            return;
        }

        textCrawler.makePreview(linkPreviewCallback, pushURL);

        main.unMuteAudio(); //turn sound back on, in case muted earlier
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pushURL));
        //appIntent.putExtra("force_fullscreen",true); //DON'T TURN THIS ON, WON'T RECALL TO LEADME
        //appIntent.putExtra("finishOnEnd", true);
        appIntent.setPackage(youTubePackageName);
        //startActivity(appIntent);

        boolean youTubeExists = true;
        //test if the YouTube app exists
        ActivityInfo ai = appIntent.resolveActivityInfo(main.getPackageManager(), 0);
        if (ai == null) {
            //the YouTube app doesn't exist
            Log.d(TAG, "YOUTUBE APP DOESN'T EXIST?! " + youTubePackageName);
            youTubeExists = false;
            //if installing, try that first.
            if (main.autoInstallApps) {
                main.getAppManager().autoInstall(youTubePackageName, "YouTube");
            }
        }

        //YouTube app doesn't exist on this device
        if (!youTubeExists) {
            launchWebsite(url, urlTitle, true); //fall back
            return;
        }

        try {
            //schedule this to run as soon as remote brings this to the front
            main.activityManager.killBackgroundProcesses(main.getAppManager().youtubePackage);
            scheduleActivityLaunch(appIntent, updateTask, youTubePackageName, "YouTube", "VR Video", pushURL, urlTitle);

            //alert other peers as needed
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.LAUNCH_SUCCESS + "YT id=" + getYouTubeID(url) + ":" + main.getNearbyManager().getID() + ":" + youTubePackageName, main.getNearbyManager().getAllPeerIDs());


        } catch (Exception ex) {
            launchWebsite(url, urlTitle, true); //fall back
        }
    }

    private void scheduleActivityLaunch(Intent appIntent) {
        Log.d(TAG, "scheduleActivityLaunch: ");
        freshPlay = true;
        if (!main.isAppVisibleInForeground()) {
            Log.w(TAG, "Need focus, scheduling for later " + appIntent + ", " + main + ", " + main.getLifecycle().getCurrentState());
            main.appIntentOnFocus = appIntent;
            main.getLumiAccessibilityConnector().bringMainToFront();
        } else {
            main.verifyOverlay();
            Log.w(TAG, "Has focus! Run it now. " + appIntent + ", " + main + ", " + main.getLifecycle().getCurrentState());
            main.getAppManager().lastApp = appIntent.getPackage();
            main.startActivity(appIntent);
        }
    }

    private void scheduleActivityLaunch(Intent appIntent, boolean updateTask, String packageName, String appName, String taskType, String url, String urlTitle) {
        Log.d(TAG, "scheduleActivityLaunch: ");
        scheduleActivityLaunch(appIntent);

        if (updateTask) {
            main.updateFollowerCurrentTask(packageName, appName, taskType, url, urlTitle);
        }
    }

    public void cleanUp() {
        Log.d(TAG, "cleanUp: ");
        if (textCrawler != null)
            textCrawler.cancel();

        if (websiteLaunchDialog != null)
            websiteLaunchDialog.dismiss();
        if (previewDialog != null)
            previewDialog.dismiss();
        if (urlYtFavDialog != null)
            urlYtFavDialog.dismiss();
        if (warningDialog != null)
            warningDialog.dismiss();

        youTubeEmbedPlayer.dismissDialogs();
    }

    protected TextCrawler getTextCrawler() {
        return textCrawler;
    }


    /////////////////////////////
    // JAKE'S SEARCH CODE
    /////////////////////////////

    //private boolean searchYoutube = true;
    private final int SEARCH_WEB = 0;
    private final int SEARCH_YOUTUBE = 1;
    private final int SEARCH_WITHIN = 2;
    private int searchType = SEARCH_WEB;
    private WebView searchWebView;

    public void buildAndShowSearchDialog(int type) {
        searchType=type;
        buildAndShowSearchDialog();
    }

    private void buildAndShowSearchDialog() {
        Log.d(TAG, "buildAndShowSearchDialog: ");
        hideWebsiteLaunchDialog();
        searchDialogView.findViewById(R.id.web_search_title).setVisibility(View.VISIBLE);
        searchDialogView.findViewById(R.id.url_error_layout).setVisibility(View.GONE);
        //placeholder URL for testing connection
        String finalUrl = "https://google.com";
//        new Thread(() -> {
        main.backgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (!main.getPermissionsManager().isInternetConnectionAvailable()) {
                    Log.w(TAG, "No internet connection in buildAndShowSearch");
                    main.getHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(main, "Can't display preview, no Internet connection.", Toast.LENGTH_SHORT).show();
                            main.showWarningDialog("No Internet Connection",
                                    "Internet based functions are unavailable at this time. " +
                                            "Please check your WiFi connection and try again.");
                            hideSearchDialog();
                        }
                    });
                }
            }
        });

//        }).start();


        //instantiates the search dialog popup if it does not already exist
        if (searchDialog == null) {
            searchDialog = new AlertDialog.Builder(main)
                    .setView(searchDialogView)
                    .show();
            searchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    main.hideSystemUI();
                }
            });
            DisplayMetrics displayMetrics = main.getResources().getDisplayMetrics();
            searchDialogView.getLayoutParams().width = displayMetrics.widthPixels - 140;

            searchDialogView.findViewById(R.id.url_search_bar).requestFocus();
            searchDialogView.findViewById(R.id.web_search_title).setVisibility(View.VISIBLE);
            searchDialogView.findViewById(R.id.url_error_layout).setVisibility(View.GONE);
            searchWebView = searchDialogView.findViewById(R.id.webview_preview);
            searchWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
            searchWebView.canGoBack();
            searchWebView.setVisibility(View.GONE);
            //web.setWebViewClient(new WebViewClient());

            final SearchView searchView = searchDialogView.findViewById(R.id.url_search_bar);
            searchView.setMaxWidth(Integer.MAX_VALUE); //ensures it fills whole space on init

            if (searchView.getQuery().length() > 0) {
                if (!isYouTube) {
                    searchWebView.setVisibility(View.VISIBLE);
                    //fixes the webpage loading in background
                    searchWebView.loadUrl("https://www.google.com/search?q=" + searchView.getQuery());
                }
            }
            if(searchType== SEARCH_YOUTUBE){
                searchSpinner.setSelection(1);
            }
            searchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "Search mode: " + searchSpinnerItems[position]);

                    if (searchSpinnerItems[position].startsWith("YouTube")) {
                        ((TextView) searchDialogView.findViewById(R.id.web_search_title)).setText("Search YouTube");
                        searchType = SEARCH_YOUTUBE;
                        //searchView.setQuery(searchView.getQuery(), true);

                    } else if (searchSpinnerItems[position].startsWith("Google")) {
                        ((TextView) searchDialogView.findViewById(R.id.web_search_title)).setText("Search the web");
                        searchType = SEARCH_WEB;
                        //searchView.setQuery(searchView.getQuery(), true);

                    } else if (searchSpinnerItems[position].startsWith("Within")) {
                        ((TextView) searchDialogView.findViewById(R.id.web_search_title)).setText("Search Within");
                        searchType = SEARCH_WITHIN;

                        /*searchWebView.clearCache(false);
                        searchDialog.dismiss();
                        main.getAppManager().getWithinPlayer().showWithin();*/

                        //searchView.setQuery(searchView.getQuery(), true);
                    }
                    searchText(searchView.getQuery().toString());
                    //searchView.performClick();
                    //populateSearch();

                }


                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //no action
                }
            });

            switch(searchType){
                case SEARCH_WEB:
                    searchSpinner.setSelection(0);
                    break;
                case SEARCH_YOUTUBE:
                    searchSpinner.setSelection(1);
                    break;
                case SEARCH_WITHIN:
                    searchSpinner.setSelection(2);
                    break;
            }

            searchDialog.findViewById(R.id.back_btn).setOnClickListener(v -> {
                searchWebView.clearCache(false);
                searchDialog.dismiss();
                if(websiteLaunchDialog!=null) {
                    websiteLaunchDialog.show();
                }
            });

        } else {
            searchDialogView.findViewById(R.id.url_search_bar).requestFocus();
            searchDialog.show();
        }


        Log.w(TAG, "Is this from YouTube? " + isYouTube);
//        if (isYouTube) {
//            //default to YouTube search
//            searchSpinner.setSelection(1);
//        } else {
//            //default to web search
//            searchSpinner.setSelection(0);
//        }
        switch(searchType){
            case SEARCH_WEB:
                isYouTube=false;
                searchSpinner.setSelection(0);
                break;
            case SEARCH_YOUTUBE:
                isYouTube=true;
                searchSpinner.setSelection(1);
                break;
            case SEARCH_WITHIN:
                isYouTube=false;
                searchSpinner.setSelection(2);
                break;
        }
        populateSearch();
    }

    private void populateSearch() {
        Log.d(TAG, "populateSearch: ");
        final SearchView searchView = searchDialogView.findViewById(R.id.url_search_bar);
//        if (!(searchView.getQuery().length() > 0)) {
//            web.setVisibility(View.GONE);
//        }
        searchDialogView.findViewById(R.id.search_btn).setOnClickListener(v -> searchText(searchView.getQuery().toString()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            //moved listener to end of search to avoid triggering recaptcha for rapid querys
            public boolean onQueryTextSubmit(String newText) {
                return searchText(newText);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }

    private boolean searchText(String newText) {
        Log.d(TAG, "searchText: " + newText + ", " + searchType);

        if (newText.length() > 0) {
            searchWebView.setVisibility(View.VISIBLE);
        }
//
        //filters the search results
        if (searchType == SEARCH_YOUTUBE) {
            searchWebView.loadUrl("https://www.google.com/search?q=" + newText + "&tbm=vid&as_sitesearch=youtube.com"); //for youtube
            //swap the above line with the one below to index youtube's site directly
            //NOTE - if this is used, will need to change triggers for when to show preview
            // (currently loads preview for anything that doesn't begin with google.com)
            //web.loadUrl("https://www.youtube.com/results?search_query="+newText);

        } else if (searchType == SEARCH_WITHIN) {
            searchWebView.loadUrl("https://www.google.com/search?q=" + newText + "&tbm=vid&as_sitesearch=with.in");

        } else {
            searchWebView.loadUrl("https://www.google.com/search?q=" + newText);
        }


        searchWebView.setWebViewClient(new WebViewClient() {
            /*
            Exists for the sole purpose of handling google's top stories news sites
            handles all resources as they load including fonts etc
             */
            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "onLoadResource: " + url);
                if (url.startsWith("https://www.google.com/gen_204") && url.contains("&url=")) { //avoid the preloaded link powered by amp
                    //find the real url hidden in the url
                    String[] parts = url.split("&");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].startsWith("url=")) {
                            url = parts[i].substring(4);
                        }
                    }
                    Log.d(TAG, "onLoadResource valid: " + url);
                    //searchDialog.dismiss();
                    hideSearchDialog();
                    showPreview(url);
                }
            }

            //
            public void onPageFinished(WebView view, String url) {
                //scrolls the page down to cut off the google rubbish at top
                if (url.startsWith("https://www.google.com")) {
                    searchWebView.scrollTo(0, 400);
                }

                Log.d(TAG, "onPageFinished: " + url);
            }

            @Override
                    /*
                    Catches the page click event and redirects it to open up our popup instead of loading the link in the browser
                     */
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //view.loadUrl(url);
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl().toString());
                String URL = request.getUrl().toString();

                //remove intent tag, let each device handle it
                //however they see fit (otherwise end up with loop of crashes)
                if (URL.startsWith("intent:")) {
                    URL = URL.substring(9);

                    //hacky, but makes the "View in VR/AR" SceneViewer things work
                    if (URL.contains("http://arvr.google.com/")) {
                        URL = URL.replace("http://arvr.google.com/", "https://arvr.google.com/");
                        if (URL.contains("&referrer=google.com")) {
                            URL = URL.split("&referrer=google.com")[0];
                        }
                    }
                }

                if (!URL.startsWith("https://www.google.com")) {
                    //searchDialog.dismiss();
                    hideSearchDialog();
                    showPreview(URL);

                    main.runOnUiThread(() -> {
                        main.hideSystemUI();
                        main.closeKeyboard();
                    });
                    return true;
                }

                main.runOnUiThread(() -> {
                    main.hideSystemUI();
                    main.closeKeyboard();
                });
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Log.d(TAG, "shouldInterceptRequest: ");
                String url = request.getUrl().toString();
                if (url == null) {
                    return super.shouldInterceptRequest(view, request);
                }

                if (url.toLowerCase().contains(".jpg") || url.toLowerCase().contains(".jpeg")) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = Glide.with(searchWebView).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                        Log.d(TAG, "shouldInterceptRequest: intercepted jpg");
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                    return new WebResourceResponse("image/jpg", "UTF-8", bs);
                } else if (url.toLowerCase().contains(".png")) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = Glide.with(searchWebView).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                        Log.d(TAG, "shouldInterceptRequest: intercepted png");
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                    return new WebResourceResponse("image/png", "UTF-8", bs);
                } else if (url.toLowerCase().contains(".webp")) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = Glide.with(searchWebView).asBitmap().diskCacheStrategy(DiskCacheStrategy.ALL).load(url).submit().get();
                        Log.d(TAG, "shouldInterceptRequest: intercepted webp");
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, bos);
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, bos);
                    }
                    byte[] bitmapdata = bos.toByteArray();
                    ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                    return new WebResourceResponse("image/webp", "UTF-8", bs);
                } else {
                    return super.shouldInterceptRequest(view, request);
                }
            }

        });

        main.runOnUiThread(() -> {
            main.hideSystemUI();
            main.closeKeyboard();
        });

        return false;
    }
    boolean selectedOnly=false;
    private void setupPushToggle() {
        Button leftToggle = previewDialogView.findViewById(R.id.selected_btn);
        Button rightToggle = previewDialogView.findViewById(R.id.everyone_btn);
        leftToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedOnly=true;
                previewDialogView.findViewById(R.id.everyone_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left_white, null));
                ((Button) previewDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                ((Button) previewDialogView.findViewById(R.id.everyone_btn)).setElevation(Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 5,main.getResources().getDisplayMetrics())));
                ((Button) previewDialogView.findViewById(R.id.selected_btn)).setElevation(0);
                previewDialogView.findViewById(R.id.selected_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right, null));
                ((Button) previewDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);
                previewPushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));
            }
        });
        rightToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedOnly=false;
                previewDialogView.findViewById(R.id.everyone_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_left, null));
                ((Button) previewDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);

                previewDialogView.findViewById(R.id.selected_btn).setBackground(main.getResources().getDrawable(R.drawable.bg_passive_right_white, null));
                ((Button) previewDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                previewPushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone));
                ((Button) previewDialogView.findViewById(R.id.selected_btn)).setElevation(Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 5,main.getResources().getDisplayMetrics())));
                ((Button) previewDialogView.findViewById(R.id.everyone_btn)).setElevation(0);
            }
        });
        rightToggle.callOnClick();
    }

}
