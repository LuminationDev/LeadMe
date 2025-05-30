package com.lumination.leadme.managers;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.LumiSpinnerAdapter;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.linkpreview.LinkPreviewCallback;
import com.lumination.leadme.linkpreview.SourceContent;
import com.lumination.leadme.linkpreview.TextCrawler;
import com.lumination.leadme.players.YouTubeEmbedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebManager {

    //tag for debugging
    private static final String TAG = "WebManager";
    private final TextCrawler textCrawler = new TextCrawler();

    private AlertDialog websiteLaunchDialog, previewDialog;
    private final View websiteLaunchDialogView;
    private final View previewDialogView;
    public boolean lastWasGuideView = false;
    private boolean launch = false;

    private ImageView previewImage;
    private TextView previewTitle;
    private TextView previewMessage;
    private ProgressBar previewProgress;
    private String pushURL = "";
    private String pushTitle = "";

    private final Button previewPushBtn;

    private final LeadMeMain main;
    public final SearchManager searchManager;
    public final FavouritesManager favouritesManager;
    private final DialogManager dialogManager;
    private final CheckBox favCheckbox;

    private final Spinner lockSpinner;
    private final YouTubeEmbedPlayer youTubeEmbedPlayer;

    //this entire thing is in progress
    public WebManager(LeadMeMain main) {
        Log.d(TAG, "WebManager: ");
        this.main = main;
        this.dialogManager = Controller.getInstance().getDialogManager();

        websiteLaunchDialogView = View.inflate(main, R.layout.d__enter_url, null);
        previewDialogView = View.inflate(main, R.layout.e__preview_url_push, null);
        previewPushBtn = previewDialogView.findViewById(R.id.push_btn);

        youTubeEmbedPlayer = new YouTubeEmbedPlayer(main, this);

        //set up lock spinner
        lockSpinner = (Spinner) previewDialogView.findViewById(R.id.push_spinner);
        String[] lockSpinnerItems = new String[2];
        lockSpinnerItems[0] = "View only";
        lockSpinnerItems[1] = "Free play";
        Integer[] push_imgs = {R.drawable.controls_view, R.drawable.controls_play};
        LumiSpinnerAdapter push_adapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, lockSpinnerItems, push_imgs);
        lockSpinner.setAdapter(push_adapter);
        lockSpinner.setSelection(1); //default to locked
        favCheckbox = previewDialogView.findViewById(R.id.fav_checkbox);

        setupPreviewDialog();
        setupWebLaunchDialog();
        buildPreviewDialog();

        //Must be after preview dialog setup
        searchManager = new SearchManager(main, websiteLaunchDialogView, previewDialog, this::showPreview);
        favouritesManager = Controller.getInstance().getFavouritesManager();
    }

    private boolean error = false;
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
                searchManager.setErrorPreview(searchTerm);
            } else {
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

                        favouritesManager.getUrlFavouritesAdapter().updateTitle(url, previewTitle.getText().toString());

                        if (loadedBitmap != null) {
                            previewImage.setVisibility(View.VISIBLE); //show image
                            favouritesManager.getUrlFavouritesAdapter().updatePreview(url, previewImage.getDrawable());

                        } else {
                            Log.d(TAG, "onPos: ERROR URL not valid");
                            previewMessage.setVisibility(View.VISIBLE); //show error
                        }
                    });

                    favouritesManager.getUrlFavouritesAdapter().notifyDataSetChanged();
                    favouritesManager.getYouTubeFavouritesAdapter().notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e(TAG, "Error launching URL: " + e.getMessage());
                    e.printStackTrace();
                    dialogManager.showWarningDialog(main.getResources().getString(R.string.warning_couldnt_launch_url));
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

    private void setupPreviewDialog() {
        Log.d(TAG, "setupPreviewDialog: ");
        previewImage = previewDialogView.findViewById(R.id.preview_image);
        previewTitle = previewDialogView.findViewById(R.id.popup_title);
        previewMessage = previewDialogView.findViewById(R.id.preview_message);
        previewProgress = previewDialogView.findViewById(R.id.preview_progress);

        final CheckBox saveWebToFav = previewDialogView.findViewById(R.id.fav_checkbox);
        Controller.getInstance().getDialogManager().setupPushToggle(previewDialogView, false);

        previewPushBtn.setOnClickListener(v -> {
            //save to favourites if needed
            if (/*FavouritesManager.adding_to_fav ||*/ saveWebToFav.isChecked()) {
                favouritesManager.getUrlFavouritesAdapter().addCurrentPreviewToFavourites(getPushURL(), getPreviewTitle(), getPreviewImage());
            }

            //if we're not only saving to favourites, push it to learners
            if (!FavouritesManager.adding_to_fav) {
                //retrieve appropriate list of receivers
                if (websiteLaunchDialog != null) {
                    websiteLaunchDialog.dismiss();
                }
                if (launch) {
                    pushURL(pushURL, pushTitle);
                    Controller.getInstance().getDialogManager().showConfirmPushDialog(false, FavouritesManager.adding_to_fav);
                } else {
                    Controller.getInstance().getDialogManager().createContentLaunchChoiceDialog(
                            "URL Launch",
                            pushURL,
                            LeadMeMain.isGuide
                    );
                }
            }

            //clean up dialogs
            hidePreviewDialog();
            FavouritesManager.adding_to_fav = false;

            //reset
            pushURL = "";
            pushTitle = "";
            previewTitle.setText("");
            previewImage.setImageDrawable(null);
        });

        previewDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> {
            hidePreviewDialog();
            FavouritesManager.adding_to_fav = false;
            showWebLaunchDialog(false);
        });
    }

    public ImageView getPreviewImageView() {
        return previewImage;
    }

    public Drawable getPreviewImage() {
        return previewImage.getDrawable();
    }

    public String getPreviewTitle() {
        return previewTitle.getText().toString().trim();
    }

    public String getPushURL() {
        return pushURL;
    }

    public void reset() {
        pushURL = "";
    }

    public void pushYouTube(String url, String urlTitle, boolean locked, boolean selectedOnly) {
        Log.d(TAG, "pushYouTube: ");
        pushURL = url;
        pushTitle = urlTitle;

        if (urlTitle.isEmpty()) {
            urlTitle = " ";
        }

        if(selectedOnly) {
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.UNLOCK_TAG, NearbyPeersManager.getSelectedPeerIDsOrAll());
        }else{
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.UNLOCK_TAG, NearbyPeersManager.getAllPeerIDs());
        }

        //push the right instruction to the receivers
        if(selectedOnly) {
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.LAUNCH_YT + url + ":::" + urlTitle + ":::",
                    NearbyPeersManager.getSelectedPeerIDsOrAll());
        }else{
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.LAUNCH_YT + url + ":::" + urlTitle + ":::",
                    NearbyPeersManager.getAllPeerIDs());
        }
    }


    public void pushURL(String url, String urlTitle) {
        Log.d(TAG, "pushURL: ");
        //unlocked if selected
        if(LeadMeMain.selectedOnly) {
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.UNLOCK_TAG, NearbyPeersManager.getSelectedPeerIDsOrAll());
        }else{
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.UNLOCK_TAG, NearbyPeersManager.getAllPeerIDs());
        }

        //push the right instruction to the receivers
        if(LeadMeMain.selectedOnly) {
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.LAUNCH_URL + url + ":::" + urlTitle, NearbyPeersManager.getSelectedPeerIDsOrAll());
        }else{
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.LAUNCH_URL + url + ":::" + urlTitle, NearbyPeersManager.getAllPeerIDs());
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

    public void launchWebsite(String url, String urlTitle, boolean updateCurrentTask) {
        Log.d(TAG, "launchWebsite: ");
        pushTitle = urlTitle;
        pushURL = url;
        lastWasGuideView = false;

        main.backgroundExecutor.submit(() -> {
            if (!Controller.getInstance().getPermissionsManager().isInternetConnectionAvailable()) {
                Log.w(TAG, "No internet connection in LaunchWebsite");

                LeadMeMain.UIHandler.post(() -> {
                    dialogManager.showWarningDialog("No Internet Connection",
                            "Internet based functions are unavailable at this time. " +
                                    "Please check your WiFi connection and try again.");
                    DispatchManager.alertGuidePermissionGranted(Controller.STUDENT_NO_INTERNET, false);
                    hideWebsiteLaunchDialog();
                });
            }
        });

        DispatchManager.alertGuidePermissionGranted(Controller.STUDENT_NO_INTERNET, true); //reset it

        //check it's a minimally sensible url
        if (url == null || url.length() < 3 || !url.contains(".")) {
            LeadMeMain.runOnUI(() -> Toast.makeText(main.getApplicationContext(), "Invalid URL", Toast.LENGTH_SHORT).show());
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                    Controller.AUTO_INSTALL_FAILED + "Invalid URL:" + NearbyPeersManager.getID(),
                    NearbyPeersManager.getSelectedPeerIDsOrAll());
            return;
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
                ApplicationInfo appInfo = ai.applicationInfo;
                String name = pm.getApplicationLabel(appInfo).toString();
                Log.w(TAG, "Selecting browser:  " + ai + " for " + uri.getHost() + ", " + ai.name + ", " + name);

                scheduleActivityLaunch(intent, updateCurrentTask, ai.packageName, name, "Website", url, urlTitle);
                DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                        Controller.LAUNCH_SUCCESS + uri.getHost() + ":" + NearbyPeersManager.getID() + ":" + ai.packageName, NearbyPeersManager.getSelectedPeerIDsOrAll());
                //success!
                return;
            }
        }

        // no browser from preferred list, so find default
        Intent browserIntent = getBrowserIntent(url);

        if (browserIntent != null) {
            scheduleActivityLaunch(browserIntent, updateCurrentTask, browserIntent.getStringExtra("packageName"), "Default Browser", "Website", url, urlTitle);
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                    Controller.LAUNCH_SUCCESS + uri.getHost() + ":" + NearbyPeersManager.getID() + ":" + browserIntent.getStringExtra("packageName"), NearbyPeersManager.getSelectedPeerIDsOrAll());
            //success!

        } else {
            LeadMeMain.runOnUI(() -> Toast.makeText(main.getApplicationContext(), "No browser available", Toast.LENGTH_SHORT).show());
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                    Controller.AUTO_INSTALL_FAILED + "No browser:" + NearbyPeersManager.getID(), NearbyPeersManager.getSelectedPeerIDsOrAll());
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
    public static String getYouTubeID(String youTubeUrl) {
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
        //first position is 'locked' - default for YouTube
        lockSpinner.setSelection(1);
        buildAndShowPreviewDialog(url);
    }

    private void showWebsitePreview(String url) {
        Log.d(TAG, "showWebsitePreview: ");
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

        lockSpinner.setSelection(1); //default to unlocked
        pushTitle = favouritesManager.getUrlFavouritesAdapter().getTitle(url);
        previewDialogView.findViewById(R.id.preview_web).setVisibility(View.VISIBLE);
        previewDialogView.findViewById(R.id.preview_youtube).setVisibility(View.GONE);

        if (pushTitle == null && !previewTitle.getText().toString().equals("Website title")) {
            pushTitle = previewTitle.getText().toString();
        }

        Controller.getInstance().getDialogManager().toggleSelectedView(previewDialogView);

        //set up preview to appear correctly
        if (FavouritesManager.adding_to_fav) {
            LeadMeMain.runOnUI(() -> previewPushBtn.setText(main.getResources().getString(R.string.add_this_app_to_favourites)));
            favCheckbox.setChecked(true);
            favCheckbox.setVisibility(View.GONE);
        } else {
            LeadMeMain.runOnUI(() -> previewPushBtn.setText(main.getResources().getString(R.string.push_this_to_everyone)));
            favCheckbox.setChecked(favouritesManager.getUrlFavouritesAdapter().isInFavourites(url));
            favCheckbox.setVisibility(View.VISIBLE);
        }

        if (previewDialog == null) {
            buildPreviewDialog();
        }

        previewDialog.show();
    }

    private void buildPreviewDialog() {
        previewDialog = new AlertDialog.Builder(main)
                .setView(previewDialogView)
                .create();
        previewDialog.setOnDismissListener(dialog -> main.hideSystemUI());
    }

    public void showWebLaunchDialog(boolean isYT, boolean add_fav_mode) {
        Log.d(TAG, "showWebLaunchDialog: ");
        showWebLaunchDialog(add_fav_mode);
    }

    public void showWebLaunchDialog(boolean add_fav_mode) {
        Log.d(TAG, "showWebLaunchDialog: ");

        if (websiteLaunchDialog == null) {
            websiteLaunchDialog = new AlertDialog.Builder(main)
                    .setView(websiteLaunchDialogView)
                    .create();
            websiteLaunchDialog.setOnDismissListener(dialog -> main.hideSystemUI());
        }

        websiteLaunchDialog.show();
        websiteLaunchDialogView.findViewById(R.id.url_input_field).requestFocus();
        main.openKeyboard();
    }

    private void setupWebLaunchDialog() {
        Log.d(TAG, "setupWebLaunchDialog: ");

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
            favouritesManager.launchUrlYtFavourites(FavouritesManager.LAUNCHTYPE_WEB);
        });
    }

    public void showPreview(String url) {
        showPreview(url, false);
    }


    public void showPreview(String url, boolean launch) {
        this.launch = launch;
        Log.d(TAG, "showPreview: ");
        Log.d(TAG, "showPreview: ");
        main.closeKeyboard();
        main.hideSystemUI();

        main.backgroundExecutor.submit(() -> {
            if (!Controller.getInstance().getPermissionsManager().isInternetConnectionAvailable()) {
                Log.w(TAG, "No internet connection in showPreview");
                LeadMeMain.UIHandler.post(() ->
                    dialogManager.showWarningDialog("No Internet Connection",
                            "Internet based functions are unavailable at this time. " +
                                    "Please check your WiFi connection and try again.")
                );
            }
        });

        url = assistWithUrl(url);

        if (!URLUtil.isValidUrl(url)) {
            if (url.contains(".")) {
                if (!url.startsWith("http")) {
                    url = "http://" + url; //append a protocol
                }
                url = URLUtil.guessUrl(url); //do a bit more checking/auto-fixing
            }
            if (!URLUtil.isValidUrl(url)) {
                hidePreviewDialog();
                String searchTerm = url.replace("http://", "").replace("https://", "").replace("www.", "").replace(".com/", "");
                Log.d(TAG, "UnknownHostHandler: search: " + searchTerm);
                searchManager.setErrorPreview(searchTerm);
                return;
            }
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

        Drawable preview;
        String title;
        title = favouritesManager.getUrlFavouritesAdapter().getTitle(url);
        preview = favouritesManager.getUrlFavouritesAdapter().getPreview(url);

        if (preview == null || title == null) {
            //generate correct information
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

        hideWebsiteLaunchDialog();
        showWebsitePreview(url);
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

    private void hideWebsiteLaunchDialog() {
        Log.d(TAG, "hideWebsiteLaunchDialog: ");
        main.closeKeyboard();
        main.hideSystemUI();
        if (websiteLaunchDialog != null) {
            websiteLaunchDialog.dismiss();
        }
    }

    public static String cleanYouTubeURL(String url) {
        Log.d(TAG, "cleanYouTubeURL: ");
        String id = getYouTubeID(url);
        //Log.i(TAG, "YouTube ID = " + id + " from " + url);
        if (id.isEmpty()) {
            return "";
        }
        String startSubstring;
        if (url.contains("&start=")) {
            int startIndex = url.indexOf("&start=") + 7;
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
        String finalURL = "https://www.youtube.com/watch/" + id + FavouritesManager.getSuffix() + startSubstring;
        Log.d(TAG, "Final URL: " + finalURL);
        return finalURL;
    }

    public void launchYouTube(String url, String urlTitle, boolean updateTask) {
        Log.w(TAG, "Launching: " + url + ", " + urlTitle);
        String cleanURL = cleanYouTubeURL(url);
        if (cleanURL.equals(pushURL)) {
            Log.e(TAG, "This is what we're already playing - ignoring it!");
            return;
        }

        pushURL = cleanURL;
        pushTitle = urlTitle;

        main.backgroundExecutor.submit(() -> {
            if (!Controller.getInstance().getPermissionsManager().isInternetConnectionAvailable()) {
                Log.w(TAG, "No internet connection in launchYouTube");
                LeadMeMain.UIHandler.post(() -> {
                    dialogManager.showWarningDialog("No Internet Connection",
                            "Internet based functions are unavailable at this time. " +
                                    "Please check your WiFi connection and try again.");
                    DispatchManager.alertGuidePermissionGranted(Controller.STUDENT_NO_INTERNET, false);
                    hideWebsiteLaunchDialog();
                });
            }
        });

        final String youTubePackageName = AppManager.youtubePackage;

        Log.w(TAG, "CLEAN YOUTUBE: " + pushURL + " || ");

        if (pushURL.isEmpty()) {
            //TODO not sure if this is the right spot to exit on fail
            //could cause other URLs to fail instead of being launched as websites
            Log.e(TAG, "No URL to push!");
            return;
        }

        textCrawler.makePreview(linkPreviewCallback, pushURL);

        main.unMuteAudio(); //turn sound back on, in case muted earlier
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pushURL));
        appIntent.setPackage(youTubePackageName);

        boolean youTubeExists = true;
        //test if the YouTube app exists
        ActivityInfo ai = appIntent.resolveActivityInfo(main.getPackageManager(), 0);
        if (ai == null) {
            //the YouTube app doesn't exist
            Log.d(TAG, "YOUTUBE APP DOESN'T EXIST?! " + youTubePackageName);
            youTubeExists = false;
        }

        //YouTube app doesn't exist on this device
        if (!youTubeExists) {
            launchWebsite(url, urlTitle, true); //fall back
            return;
        }

        try {
            //schedule this to run as soon as remote brings this to the front
            main.activityManager.killBackgroundProcesses(AppManager.youtubePackage);
            scheduleActivityLaunch(appIntent, updateTask, youTubePackageName, "YouTube", "VR Video", pushURL, urlTitle);

            //alert other peers as needed
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                    Controller.LAUNCH_SUCCESS + "YT id=" + getYouTubeID(url) + ":" + NearbyPeersManager.getID() + ":" + youTubePackageName, NearbyPeersManager.getSelectedPeerIDsOrAll());


        } catch (Exception ex) {
            launchWebsite(url, urlTitle, true); //fall back
        }
    }

    private void scheduleActivityLaunch(Intent appIntent) {
        Log.d(TAG, "scheduleActivityLaunch: ");
        if (!main.isAppVisibleInForeground()) {
            Log.w(TAG, "Need focus, scheduling for later " + appIntent + ", " + main + ", " + main.getLifecycle().getCurrentState());
            LeadMeMain.appIntentOnFocus = appIntent;
            main.getLumiAccessibilityConnector().bringMainToFront();
        } else {
            main.verifyOverlay();
            Log.w(TAG, "Has focus! Run it now. " + appIntent + ", " + main + ", " + main.getLifecycle().getCurrentState());
            AppManager.lastApp = appIntent.getPackage();
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
        textCrawler.cancel();

        if (websiteLaunchDialog != null)
            websiteLaunchDialog.dismiss();
        if (previewDialog != null)
            previewDialog.dismiss();
        if (favouritesManager.warningDialog != null)
            favouritesManager.warningDialog.dismiss();

        youTubeEmbedPlayer.dismissDialogs();
    }
}
