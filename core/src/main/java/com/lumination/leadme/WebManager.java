package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
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

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lumination.leadme.linkpreview.LinkPreviewCallback;
import com.lumination.leadme.linkpreview.SourceContent;
import com.lumination.leadme.linkpreview.TextCrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebManager {

    //tag for debugging
    private static final String TAG = "WebManager";
    private TextCrawler textCrawler = new TextCrawler();

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
    private String pushURL = "", pushTitle = "", controllerURL = "";

    private View webYouTubeFavView;
    private FavouritesManager urlFavouritesManager;
    private FavouritesManager youTubeFavouritesManager;

    private LeadMeMain main;
    private CheckBox favCheckbox;

    private Spinner lockSpinner, searchSpinner;
    private View lockSpinnerParent;
    private String[] searchSpinnerItems, lockSpinnerItems;

    private YouTubeEmbedPlayer youTubeEmbedPlayer;

    //this entire thing is in progress
    public WebManager(LeadMeMain main) {
        this.main = main;
        youTubeEmbedPlayer = new YouTubeEmbedPlayer(main, this);

        websiteLaunchDialogView = View.inflate(main, R.layout.d__enter_url, null);
        previewDialogView = View.inflate(main, R.layout.e__preview_url_push, null);
        searchDialogView = View.inflate(main, R.layout.e__preview_url_search, null);

        //set up lock spinner
        lockSpinnerParent = previewDialogView.findViewById(R.id.spinner_parent);
        lockSpinner = (Spinner) previewDialogView.findViewById(R.id.push_spinner);
        lockSpinnerItems = new String[2];
        lockSpinnerItems[0] = "Lock students";
        lockSpinnerItems[1] = "Unlock students";
        Integer[] push_imgs = {R.drawable.controls_lock, R.drawable.controls_unlock};
        SpinnerAdapter push_adapter = new SpinnerAdapter(main, R.layout.row_push_spinner, lockSpinnerItems, push_imgs);
        lockSpinner.setAdapter(push_adapter);

        //set up search spinner
        //TODO add Vimeo search
        searchSpinner = (Spinner) searchDialogView.findViewById(R.id.search_spinner);
        searchSpinnerItems = new String[2];
        searchSpinnerItems[0] = "Google search";
        searchSpinnerItems[1] = "YouTube search";
        Integer[] search_imgs = {R.drawable.search_google, R.drawable.search_yt};
        SpinnerAdapter search_adapter = new SpinnerAdapter(main, R.layout.row_search_spinner, searchSpinnerItems, search_imgs);
        searchSpinner.setAdapter(search_adapter);

        //set up favourites view
        webYouTubeFavView = View.inflate(main, R.layout.d__url_yt_favourites, null);
        favCheckbox = previewDialogView.findViewById(R.id.fav_checkbox);
        setupWarningDialog();

        websiteLaunchDialogView.findViewById(R.id.url_search_btn).setOnClickListener(v -> {
            hidePreviewDialog();
            buildAndShowSearchDialog();
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
            generatingPreview = false;
            // Populate your preview layout with the results of sourceContent.
            String title = sourceContent.getTitle();
            if (title.isEmpty()) {
                title = sourceContent.getFinalUrl();
            }

            //store the title
            pushTitle = title;

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

    };



    void unmuteVideo() {
        main.unMuteAudio(); //this is managed by the main activity
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_UNMUTE_TAG, main.getNearbyManager().getSelectedPeerIDs());
    }

    void muteVideo() {
        main.muteAudio(); //this is managed by main activity
        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_MUTE_TAG, main.getNearbyManager().getSelectedPeerIDs());
    }


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
        webYouTubeFavView.findViewById(R.id.yt_add_btn).setOnClickListener(v -> {
            showWebLaunchDialog(true);
            urlYtFavDialog.hide();
        });

        webYouTubeFavView.findViewById(R.id.url_add_btn).setOnClickListener(v -> {
            showWebLaunchDialog(true);
            urlYtFavDialog.hide();
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
            warningDialog.hide();
            getUrlFavouritesManager().notifyDataSetChanged();
            getYouTubeFavouritesManager().notifyDataSetChanged();
        });

        warningDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> warningDialog.hide());

        warningDialog = new AlertDialog.Builder(main)
                .setView(warningDialogView)
                .create();

    }

    private void showClearWebFavDialog(int whatToClear) {
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
        previewImage = previewDialogView.findViewById(R.id.preview_image);
        previewTitle = previewDialogView.findViewById(R.id.preview_title);
        previewMessage = previewDialogView.findViewById(R.id.preview_message);
        previewProgress = previewDialogView.findViewById(R.id.preview_progress);
        previewPushBtn = previewDialogView.findViewById(R.id.push_btn);

        final CheckBox saveWebToFav = previewDialogView.findViewById(R.id.fav_checkbox);

        previewPushBtn.setOnClickListener(v -> {
            //save to favourites if needed
            if (adding_to_fav || saveWebToFav.isChecked()) {
                if (isYouTube) {
                    getYouTubeFavouritesManager().addCurrentPreviewToFavourites();
                } else {
                    getUrlFavouritesManager().addCurrentPreviewToFavourites();
                }
            }

            //if we're not only saving to favourites, push it to learners
            if (!adding_to_fav) {
                //retrieve appropriate list of receivers
                pushYouTubeOrWeb(pushURL, pushTitle);
            }

            //clean up dialogs
            hideSearchDialog();
            hidePreviewDialog();
            main.showConfirmPushDialog(false, adding_to_fav);

            if (!adding_to_fav && isYouTube) {
                main.getHandler().postDelayed(() -> {
                    //main.hideConfirmPushDialog();
                    youTubeEmbedPlayer.showVideoController(controllerURL);
                }, 1000);
            }

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

    protected String getLaunchTitle() {
        return pushTitle;
    }

    private boolean freshPlay = true;

    protected boolean isFreshPlay() {
        return freshPlay;
    }

    protected void setFreshPlay(boolean freshPlay) {
        this.freshPlay = freshPlay;
    }


    public void pushYouTubeOrWeb(String url, String urlTitle) {
        Set<String> selectedPeers;
        if (main.getConnectedLearnersAdapter().someoneIsSelected()) {
            selectedPeers = main.getNearbyManager().getSelectedPeerIDs();
        } else {
            selectedPeers = main.getNearbyManager().getAllPeerIDs();
        }

        //update lock status
        if (lockSpinner.getSelectedItem().toString().startsWith("Lock")) {
            //locked by default
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, selectedPeers);
        } else {
            //unlocked if selected
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, selectedPeers);
        }

        //push the right instruction to the receivers
        if (isYouTube) {
            Log.d(TAG, "pushYouTubeOrWeb: pushed yt");
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_YT + url + ":::" + urlTitle, selectedPeers);
        } else {
            Log.d(TAG, "pushYouTubeOrWeb: pushed website");
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_URL + url + ":::" + urlTitle, selectedPeers);
        }
    }

    private void hidePreviewDialog() {
        main.closeKeyboard();
        main.hideSystemUI();

        if (previewDialog != null) {
            previewDialog.hide();
            error = false; //reset flag
        }
    }

    private void hideSearchDialog() {
        main.closeKeyboard();
        main.hideSystemUI();

        if (searchDialog != null) {
            searchDialog.hide();
            error = false; //reset flag
        }
    }

    public void launchWebsite(String url, String urlTitle, boolean updateCurrentTask) {
        String finalUrl = url;
        pushTitle = urlTitle;
        freshPlay = true;
        new Thread(() -> {
            if (!main.getPermissionsManager().isInternetConnectionAvailable(finalUrl)) {
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
        }).start();

        //check it's a minimally sensible url
        if (url == null || url.length() < 3 || !url.contains(".")) {
            Toast toast = Toast.makeText(main, "Invalid URL", Toast.LENGTH_SHORT);
            toast.show();
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_FAILED + "Invalid URL:" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDs());
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        PackageManager pm = main.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Uri uri = Uri.parse(url);
        intent.setData(uri);

        for (ComponentName cn : browserComponents) {
            intent.setComponent(cn);
            ActivityInfo ai = intent.resolveActivityInfo(pm, 0);
            if (ai != null) {
                Log.w(TAG, "Selecting browser:  " + ai + " for " + uri.getHost());

                scheduleActivityLaunch(intent, updateCurrentTask, ai.packageName, ai.name, "Website", url, urlTitle);
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
                    LeadMeMain.AUTO_INSTALL_FAILED + "No browser:" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDs());
            //no browser, failure
        }
    }

    Intent getBrowserIntent(String url) {
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
        if (!youTubeUrl.toLowerCase().contains("youtu.be") && !youTubeUrl.toLowerCase().contains("youtube.com") && !youTubeUrl.toLowerCase().contains("youtube-nocookie.com")) {
            Log.w(TAG, "Not a YouTube URL! " + youTubeUrl);
            return "";
        }
        //String pattern = "(?<=youtu.be/|watch?v=|/w/|/videos/|embed/)[^#&?]*";
        String pattern = "(?:http:|https:)*?\\/\\/(?:www\\.|)(?:youtube\\.com|m\\.youtube\\.com|youtu\\.|youtube-nocookie\\.com).*(?:v=|v%3D|v\\/|(?:a|p)\\/(?:a|u)\\/\\d.*\\/|watch\\?|\\/w\\/|vi(?:=|\\/)|\\/embed\\/|oembed\\?|be\\/|e\\/)([^&?%#\\/\\n]*)";
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
        isYouTube = true;
        //first position is 'locked' - default for YouTube
        lockSpinner.setSelection(0);
        buildAndShowPreviewDialog(url);
    }

    private void showWebsitePreview(String url) {
        isYouTube = false;
        //second position is 'unlocked' - default for website
        lockSpinner.setSelection(1);
        buildAndShowPreviewDialog(url);
    }

    private void buildAndShowPreviewDialog(String url) {
        if (error) {
            error = false; //reset error flag
            return;
        }

        main.closeKeyboard();

        pushURL = url;
        controllerURL = url;
        pushTitle = getYouTubeFavouritesManager().getTitle(url);
        if (pushTitle == null && !previewTitle.getText().toString().equals("Website title")) {
            pushTitle = previewTitle.getText().toString();
        }

        //set up preview to appear correctly
        if (adding_to_fav) {
            lockSpinnerParent.setVisibility(View.INVISIBLE);
            previewPushBtn.setText(main.getResources().getString(R.string.add_this_app_to_favourites));
            favCheckbox.setChecked(true);
            favCheckbox.setVisibility(View.GONE);

        } else {
            lockSpinnerParent.setVisibility(View.VISIBLE);
            if (main.getConnectedLearnersAdapter().someoneIsSelected()) {
                previewPushBtn.setText(main.getResources().getString(R.string.push_this_to_selected));
            } else {
                previewPushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone));
            }
            favCheckbox.setChecked(false);
            favCheckbox.setVisibility(View.VISIBLE);
        }

        //display correct preview information
        if (isYouTube) {
            previewDialogView.findViewById(R.id.preview_website).setVisibility(View.GONE);
            previewDialogView.findViewById(R.id.preview_youtube).setVisibility(View.VISIBLE);
        } else {
            previewDialogView.findViewById(R.id.preview_website).setVisibility(View.VISIBLE);
            previewDialogView.findViewById(R.id.preview_youtube).setVisibility(View.GONE);
        }

        if (previewDialog == null) {
            previewDialog = new AlertDialog.Builder(main)
                    .setView(previewDialogView)
                    .show();
        } else {
            previewDialog.show();
        }
    }

    boolean adding_to_fav = false;

    void showWebLaunchDialog(boolean isYT, boolean add_fav_mode) {
        isYouTube = isYT;
        showWebLaunchDialog(add_fav_mode);
    }

    void showWebLaunchDialog(boolean add_fav_mode) {
        if (lastWasGuideView) {
            youTubeEmbedPlayer.showVideoController(null);
            return;
        }

        lastWasGuideView = false;
        if (websiteLaunchDialog == null) {
            websiteLaunchDialog = new AlertDialog.Builder(main)
                    .setView(websiteLaunchDialogView)
                    .create();
        }

        adding_to_fav = add_fav_mode;

        websiteLaunchDialog.show();
        main.openKeyboard();
    }

    private void setupWebLaunchDialog() {
        ((TextView) websiteLaunchDialogView.findViewById(R.id.url_input_field)).setText("https://www.youtube.com/w/SEbqkn1TWTA"); //sample for testing

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
            websiteLaunchDialog.hide();
        });

        websiteLaunchDialogView.findViewById(R.id.open_favourites).setOnClickListener(v -> {
            main.closeKeyboard();
            main.hideSystemUI();
            websiteLaunchDialog.hide();
            launchUrlYtFavourites();
        });

    }

    protected void showPreview(String url) {
        main.closeKeyboard();
        main.hideSystemUI();


        String finalUrl = url;
        new Thread(() -> {
            if (!main.getPermissionsManager().isInternetConnectionAvailable(finalUrl)) {
                Log.w(TAG, "No internet connection in showPreview");
                main.getHandler().post(() -> {
                    //Toast.makeText(main, "Can't display preview, no Internet connection.", Toast.LENGTH_SHORT).show();
                    main.showWarningDialog("No Internet Connection",
                            "Internet based functions are unavailable at this time. " +
                                    "Please check your WiFi connection and try again.");
//                        main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_INTERNET, false);
//                        hidePreviewDialog();
                });
            }
        }).start();


        url = assistWithUrl(url);

        if (!URLUtil.isValidUrl(url)) {
            if (!url.startsWith("http")) {
                url = "http://" + url; //append a protocol
            }
            url = URLUtil.guessUrl(url); //do a bit more checking/auto-fixing
        }

        //hide preview image and title
        previewProgress.setVisibility(View.VISIBLE);
        previewMessage.setVisibility(View.GONE);
        previewImage.setVisibility(View.GONE);
        previewTitle.setVisibility(View.GONE);

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

        String youTubeId = getYouTubeID(url);

        if (youTubeId.length() > 0) {
            hideWebsiteLaunchDialog();
            showYouTubePreview(cleanYouTubeURL(url));
        } else {
            hideWebsiteLaunchDialog();
            showWebsitePreview(url);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private String assistWithUrl(String origUrl) {
        if (!origUrl.contains("edu.cospaces.io") && origUrl.contains("cospaces.io")) {
            return origUrl.replace("cospaces.io", "edu.cospaces.io");
        }
        //no known issues
        return origUrl;
    }

    void hideFavDialog() {
        main.closeKeyboard();
        main.hideSystemUI();
        urlYtFavDialog.hide();
    }

    void launchUrlYtFavourites() {
        getUrlFavouritesManager().clearPreviews();
        getUrlFavouritesManager().notifyDataSetChanged();
        getYouTubeFavouritesManager().clearPreviews();
        getYouTubeFavouritesManager().notifyDataSetChanged();

        if (urlYtFavDialog == null) {
            urlYtFavDialog = new AlertDialog.Builder(main)
                    .setView(webYouTubeFavView)
                    .create();

            webYouTubeFavView.findViewById(R.id.back_btn).setOnClickListener(v -> urlYtFavDialog.hide());
        }

        urlYtFavDialog.show();
    }

    private void hideWebsiteLaunchDialog() {
        main.closeKeyboard();
        main.hideSystemUI();
        if (websiteLaunchDialog != null) {
            websiteLaunchDialog.hide();
        }
    }


    public String cleanYouTubeURL(String url) {
        String id = getYouTubeID(url);
        Log.i(TAG, "YouTube ID = " + id + " from " + url);
        return "https://www.youtube.com/watch?v=" + id + suffix;
    }

    public YouTubeEmbedPlayer getYouTubeEmbedPlayer(){
        return youTubeEmbedPlayer;
    }

    private String suffix = "&t=1&rel=0"; //&autoplay=1&start=1&end=10&controls=0&rel=0";
    public String getSuffix() {
        return suffix;
    }

    public void launchYouTube(String url, String urlTitle, boolean updateTask) {
        freshPlay = true;
        pushTitle = urlTitle;
        String finalUrl = url;
        new Thread(() -> {
            if (!main.getPermissionsManager().isInternetConnectionAvailable(finalUrl)) {
                Log.w(TAG, "No internet connection in launchYouTube");
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
        }).start();

        launchingVR = true; //activate button pressing
        enteredVR = false;
        final String youTubePackageName = "com.google.android.youtube"; //TODO don't hardcode the package name
        String cleanURL = cleanYouTubeURL(url);
        //Uri uri = Uri.parse("vnd.youtube://" + getYouTubeID(url));
        Log.w(TAG, "CLEAN YOUTUBE: " + cleanURL);

        main.unMuteAudio(); //turn sound back on, in case muted earlier
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanURL));
        //appIntent.putExtra("force_fullscreen",true);
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
            scheduleActivityLaunch(appIntent, updateTask, youTubePackageName, "YouTube", "VR Video", cleanYouTubeURL(url), urlTitle);

            //alert other peers as needed
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.LAUNCH_SUCCESS + "YT id=" + getYouTubeID(url) + ":" + main.getNearbyManager().getID() + ":" + youTubePackageName, main.getNearbyManager().getAllPeerIDs());


        } catch (Exception ex) {
            launchWebsite(url, urlTitle, true); //fall back
        }
    }

    private void scheduleActivityLaunch(Intent appIntent) {
        freshPlay = true;
        if (!main.appHasFocus) {
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
        scheduleActivityLaunch(appIntent);

        if (updateTask) {
            main.updateFollowerCurrentTask(packageName, appName, taskType, url, urlTitle);
        }
    }

    public void cleanUp() {
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
    }

    protected TextCrawler getTextCrawler() {
        return textCrawler;
    }


    /////////////////////////////
    // JAKE'S SEARCH CODE
    /////////////////////////////

    private boolean searchYoutube = true;
    private WebView searchWebView;

    private void buildAndShowSearchDialog() {
        hideWebsiteLaunchDialog();


        //placeholder URL for testing connection
        String finalUrl = "https://google.com";
        new Thread(() -> {
            if (!main.getPermissionsManager().isInternetConnectionAvailable(finalUrl)) {
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
        }).start();


        //instantiates the search dialog popup if it does not already exist
        if (searchDialog == null) {
            searchDialog = new AlertDialog.Builder(main)
                    .setView(searchDialogView)
                    .show();

            DisplayMetrics displayMetrics = main.getResources().getDisplayMetrics();
            searchDialogView.getLayoutParams().width = displayMetrics.widthPixels - 140;

            searchDialogView.findViewById(R.id.url_search_bar).requestFocus();

            searchWebView = searchDialogView.findViewById(R.id.webview_preview);
            searchWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
            searchWebView.canGoBack();
            searchWebView.setVisibility(View.GONE);
            //web.setWebViewClient(new WebViewClient());

            final SearchView searchView = searchDialogView.findViewById(R.id.url_search_bar);
            searchView.setMaxWidth(Integer.MAX_VALUE); //ensures it fills whole space on init

            if (searchView.getQuery().length() > 0) {
                if (!isYouTube) {
                    //fixes the webpage loading in background
                    searchWebView.loadUrl("https://www.google.com/search?q=" + searchView.getQuery());
                }
            }

            searchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "Search mode: " + searchSpinnerItems[position]);

                    if (searchSpinnerItems[position].startsWith("YouTube")) {
                        ((TextView) searchDialogView.findViewById(R.id.web_search_title)).setText("Search YouTube");
                        searchYoutube = true;
                    } else if (searchSpinnerItems[position].startsWith("Google")) {
                        ((TextView) searchDialogView.findViewById(R.id.web_search_title)).setText("Search the web");
                        searchYoutube = false;
                    }

                    searchView.performClick();
                    //populateSearch();

                }


                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //no action
                }
            });

            searchDialog.findViewById(R.id.back_btn).setOnClickListener(v -> {
                searchDialog.hide();
                websiteLaunchDialog.show();
            });

        } else {
            searchDialogView.findViewById(R.id.url_search_bar).requestFocus();
            searchDialog.show();
        }

        if (isYouTube) {
            //default to YouTube search
            searchSpinner.setSelection(1);
        } else {
            //default to web search
            searchSpinner.setSelection(0);
        }
        populateSearch();
    }

    private void populateSearch() {
        final SearchView searchView = searchDialogView.findViewById(R.id.url_search_bar);
//        if (!(searchView.getQuery().length() > 0)) {
//            web.setVisibility(View.GONE);
//        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            //moved listener to end of search to avoid triggering recaptcha for rapid querys
            public boolean onQueryTextSubmit(String newText) {
                if (newText.length() > 0) {
                    searchWebView.setVisibility(View.VISIBLE);
                }
//                } else {
//                    web.setVisibility(View.GONE);
//                }

                //filters the search results
                if (searchYoutube) {
                    searchWebView.loadUrl("https://www.google.com/search?q=" + newText + "&tbm=vid&as_sitesearch=youtube.com");

                    //swap the above line with the one below to index youtube's site directly
                    //NOTE - if this is used, will need to change triggers for when to show preview
                    // (currently loads preview for anything that doesn't begin with google.com)
                    //web.loadUrl("https://www.youtube.com/results?search_query="+newText);

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
                            //searchDialog.hide();
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
                            //searchDialog.hide();
                            hideSearchDialog();
                            showPreview(URL);
                            return true;
                        }

                        return false;
                    }
                });

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //removes the webview when text is cleared
//                if (!(newText.length() > 0)) {
//                    web.setVisibility(View.GONE);
//                }
                return false;
            }
        });

    }

}
