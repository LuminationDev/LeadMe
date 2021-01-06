package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lumination.leadme.linkpreview.LinkPreviewCallback;
import com.lumination.leadme.linkpreview.SourceContent;
import com.lumination.leadme.linkpreview.TextCrawler;
import com.lumination.leadme.twowaygrid.TwoWayGridView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebManager {

    //tag for debugging
    static final String TAG = "WebManager";
    protected TextCrawler textCrawler = new TextCrawler();

    AlertDialog websiteLaunchDialog, previewDialog, urlYtFavDialog;
    private View websiteLaunchDialogView, previewDialogView;
    View webYouTubeFavView;
    public boolean launchingVR = false;

    ImageView previewImage;
    TextView previewTitle;
    TextView previewMessage;
    ProgressBar previewProgress;
    Button previewPushBtn;
    boolean isYouTube = false;
    String pushURL = "";

    private FavouritesManager urlFavouritesManager;
    private FavouritesManager youTubeFavouritesManager;

    private LeadMeMain main;

    CheckBox favCheckbox;

    public WebManager(final LeadMeMain main) {
        this.main = main;
        websiteLaunchDialogView = View.inflate(main, R.layout.d__enter_url, null);
        previewDialogView = View.inflate(main, R.layout.e__preview_url_push, null);

        //set up favourites view
        webYouTubeFavView = View.inflate(main, R.layout.d__url_yt_favourites, null);
        favCheckbox = previewDialogView.findViewById(R.id.fav_checkbox);
        setupWarningDialog();
        Log.d(TAG, "Got: " + favCheckbox);

        ((TwoWayGridView) webYouTubeFavView.findViewById(R.id.yt_favourites)).setAdapter(getYouTubeFavouritesManager());
        ((TwoWayGridView) webYouTubeFavView.findViewById(R.id.url_favourites)).setAdapter(getUrlFavouritesManager());

        webYouTubeFavView.findViewById(R.id.clear_fav_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearWebFavDialog(CLEAR_ALL);
                getYouTubeFavouritesManager().clearFavourites();
                getUrlFavouritesManager().clearFavourites();
            }
        });

        setupViews();
        setupPreviewDialog();
        setupWebLaunchDialog();
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
            // Populate your preview layout with the results of sourceContent.
            String title = sourceContent.getTitle();
            if (title.isEmpty()) {
                title = sourceContent.getFinalUrl();
            }

            previewTitle.setText(title);
            previewTitle.setVisibility(View.VISIBLE);

            Log.d(TAG, sourceContent.toString() + ", " + sourceContent.getTitle() + ", " + sourceContent.getUrl() + ", " + sourceContent.getDescription() + ", " + sourceContent.getFinalUrl());
            Log.d(TAG, sourceContent.getMetaTags().toString());
            //Log.d(TAG, sourceContent.getHtmlCode());
            String icon;
            if (!sourceContent.getImages().isEmpty()) {
                icon = sourceContent.getImages().get(0);
            } else {
                icon = "";
            }

            try {
                UrlImageViewHelper.setUrlDrawable(previewImage, icon, new UrlImageViewCallback() {
                    @Override
                    public void onLoaded(ImageView imageView, Bitmap loadedBitmap, String url, boolean loadedFromCache) {
                        if (loadedBitmap != null) {
                            previewImage.setVisibility(View.VISIBLE); //show image
                        } else {
                            previewMessage.setVisibility(View.VISIBLE); //show error
                        }
                        previewProgress.setVisibility(View.GONE);
                    }
                });


            } catch (Exception e) {
                Log.e(TAG, "Error launching URL: " + e.getMessage());
                e.printStackTrace();
                main.showWarningDialog(main.getResources().getString(R.string.warning_couldnt_launch_url));
                error = true; //set error flag
                hidePreviewDialog();
            }
        }

    };


    private final static List<ComponentName> browserComponents = new ArrayList<ComponentName>() {{
        add(new ComponentName("com.android.chrome", "com.google.android.apps.chrome.IntentDispatcher")); //preferred browser
        add(new ComponentName("com.google.android.browser", "com.google.android.browser.BrowserActivity"));
        add(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
    }};


    public FavouritesManager getUrlFavouritesManager() {
        if (urlFavouritesManager == null) {
            urlFavouritesManager = new FavouritesManager(main, this, FavouritesManager.FAVTYPE_URL, 10);
        }
        return urlFavouritesManager;
    }

    public FavouritesManager getYouTubeFavouritesManager() {
        if (youTubeFavouritesManager == null) {
            youTubeFavouritesManager = new FavouritesManager(main, this, FavouritesManager.FAVTYPE_YT, 10);
        }
        return youTubeFavouritesManager;
    }

    private void setupViews() {
        ((Button) webYouTubeFavView.findViewById(R.id.yt_add_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWebLaunchDialog(true);
                urlYtFavDialog.hide();
            }
        });

        ((Button) webYouTubeFavView.findViewById(R.id.url_add_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWebLaunchDialog(true);
                urlYtFavDialog.hide();
            }
        });

        ((Button) webYouTubeFavView.findViewById(R.id.yt_del_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearWebFavDialog(CLEAR_VID);
            }
        });

        ((Button) webYouTubeFavView.findViewById(R.id.url_del_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearWebFavDialog(CLEAR_URL);
            }
        });

    }


    final private static int CLEAR_ALL = 0;
    final private static int CLEAR_VID = 1;
    final private static int CLEAR_URL = 2;
    private int whatToClear = -1;
    TextView warningTextView; //need to be able to change text
    AlertDialog warningDialog;

    private void setupWarningDialog() {
        View warningDialogView = View.inflate(main, R.layout.e__fav_clear_confirmation_popup, null);
        warningTextView = (TextView) warningDialogView.findViewById(R.id.favclear_comment);

        ((Button) warningDialogView.findViewById(R.id.ok_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        ((Button) warningDialogView.findViewById(R.id.back_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                warningDialog.hide();
            }
        });

        warningDialog = new AlertDialog.Builder(main)
                .setView(warningDialogView)
                .create();

    }

    private void showClearWebFavDialog(int whatToClear) {
        String message = "";

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
        previewImage = (ImageView) previewDialogView.findViewById(R.id.preview_image);
        previewTitle = (TextView) previewDialogView.findViewById(R.id.preview_title);
        previewMessage = (TextView) previewDialogView.findViewById(R.id.preview_message);
        previewProgress = (ProgressBar) previewDialogView.findViewById(R.id.preview_progress);
        previewPushBtn = previewDialogView.findViewById(R.id.push_btn);

        final CheckBox saveWebToFav = previewDialogView.findViewById(R.id.fav_checkbox);

        previewPushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    pushYouTubeOrWeb(pushURL);
                }

                //clean up dialogs
                hidePreviewDialog();
                main.showConfirmPushDialog(false, adding_to_fav);

                //reset
                pushURL = "";
                previewTitle.setText("");
                previewImage.setImageDrawable(null);

            }
        });

        ((Button) previewDialogView.findViewById(R.id.back_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePreviewDialog();
                showWebLaunchDialog(adding_to_fav);
            }
        });
    }

    public void pushYouTubeOrWeb(String url) {
        Set<String> selectedPeers;
        if (main.getConnectedLearnersAdapter().someoneIsSelected()) {
            selectedPeers = main.getNearbyManager().getSelectedPeerIDs();
        } else {
            selectedPeers = main.getNearbyManager().getAllPeerIDs();
        }

        //push the right instruction to the receivers
        if (isYouTube) {
            main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_YT + url, selectedPeers);
        } else {
            main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_URL + url, selectedPeers);
        }
    }

    private void hidePreviewDialog() {
        Log.d(TAG, "Hiding dialog box");
        main.closeKeyboard();
        main.hideSystemUI();

        if (previewDialog != null) {
            previewDialog.hide();
            error = false; //reset flag
        }
    }

    public boolean launchWebsite(String url, boolean updateCurrentTask) {
        //check it's a minimally sensible url
        if (url == null || url.length() < 3 || !url.contains(".")) {
            Toast toast = Toast.makeText(main, "Invalid URL", Toast.LENGTH_SHORT);
            toast.show();
            main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_FAILED + "Invalid URL:" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDs());
            return false;
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

                scheduleActivityLaunch(intent, updateCurrentTask, ai.packageName, ai.name, "Website", url);
                main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG,
                        LeadMeMain.LAUNCH_SUCCESS + uri.getHost() + ":" + main.getNearbyManager().getID() + ":" + ai.packageName, main.getNearbyManager().getAllPeerIDs());
                return true; //success!
            }
        }

        // no browser from preferred list, so find default
        Intent browserIntent = getBrowserIntent(url);

        if (browserIntent != null) {
            scheduleActivityLaunch(browserIntent, updateCurrentTask, browserIntent.getStringExtra("packageName"), browserIntent.getStringExtra("label"), "Website", url);
            main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.LAUNCH_SUCCESS + uri.getHost() + ":" + main.getNearbyManager().getID() + ":" + browserIntent.getStringExtra("packageName"), main.getNearbyManager().getAllPeerIDs());
            return true; //success!

        } else {
            Toast toast = Toast.makeText(main, "No browser available", Toast.LENGTH_SHORT);
            toast.show();
            main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.AUTO_INSTALL_FAILED + "No browser:" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDs());
            return false; //no browser, failure
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

    private String getYouTubeID(String youTubeUrl) {
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

        if (res.length() > 0) {
            Log.d(TAG, "YouTube ID = " + res);
            return res;
        } else {
            Log.d(TAG, "Couldn't extract YouTube ID");
            return "";
        }
    }

    private void showYouTubePreview(String url) {
        isYouTube = true;
        buildAndShowPreviewDialog(url);
    }

    private void showWebsitePreview(String url) {
        isYouTube = false;
        buildAndShowPreviewDialog(url);
    }

    private void buildAndShowPreviewDialog(String url) {
        if (error) {
            error = false; //reset error flag
            return;
        }

        main.closeKeyboard();

        pushURL = url;

        //set up preview to appear correctly
        if (adding_to_fav) {
            previewPushBtn.setText(main.getResources().getString(R.string.add_this_app_to_favourites));
            favCheckbox.setChecked(true);
            favCheckbox.setVisibility(View.GONE);

        } else {
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

    void showWebLaunchDialog(boolean add_fav_mode) {
        if (websiteLaunchDialog == null) {
            websiteLaunchDialog = new AlertDialog.Builder(main)
                    .setView(websiteLaunchDialogView)
                    .create();
        }

        Log.d(TAG, "Adding to fav? " + add_fav_mode);
        adding_to_fav = add_fav_mode;

        websiteLaunchDialog.show();
        main.openKeyboard();
    }

    private void setupWebLaunchDialog() {
        ((TextView) websiteLaunchDialogView.findViewById(R.id.url_input_field)).setText("https://www.youtube.com/w/SEbqkn1TWTA"); //sample for testing

        websiteLaunchDialogView.findViewById(R.id.paste_from_clipboard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.closeKeyboard();
                main.hideSystemUI();
                ClipboardManager clipboard = (ClipboardManager) main.getSystemService(Context.CLIPBOARD_SERVICE);
                CharSequence pasteData = "";

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
            }
        });

        websiteLaunchDialogView.findViewById(R.id.confirm_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = ((TextView) websiteLaunchDialogView.findViewById(R.id.url_input_field)).getText().toString();
                if (url.length() == 0) {
                    return;
                }
                showPreview(url);
            }
        });

        websiteLaunchDialogView.findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.closeKeyboard();
                main.hideSystemUI();
                websiteLaunchDialog.hide();
            }
        });

        websiteLaunchDialogView.findViewById(R.id.open_favourites).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.closeKeyboard();
                main.hideSystemUI();
                websiteLaunchDialog.hide();
                launchUrlYtFavourites();
            }
        });

    }

    protected void showPreview(String url) {
        main.closeKeyboard();
        main.hideSystemUI();

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

        //generate preview
        textCrawler.makePreview(linkPreviewCallback, url);

        String youTubeId = getYouTubeID(url);
        Log.d(TAG, "What is it? " + youTubeId + " (" + youTubeId.length() + "), " + url);

        if (youTubeId.length() > 0) {//url.contains("youtu.be") || ((url.contains("youtube.")||url.contains("youtube-nocookie")) && (url.contains("?")||url.contains("embed")))) {
            hideWebsiteLaunchDialog();
            showYouTubePreview(cleanYouTubeURL(url));
        } else {
            hideWebsiteLaunchDialog();
            showWebsitePreview(url);
        }
    }

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

    private void launchUrlYtFavourites() {
        if (urlYtFavDialog == null) {
            urlYtFavDialog = new AlertDialog.Builder(main)
                    .setView(webYouTubeFavView)
                    .create();

            webYouTubeFavView.findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    urlYtFavDialog.hide();
                }
            });
        }

        urlYtFavDialog.show();
    }

    private void hideWebsiteLaunchDialog() {
        Log.d(TAG, "Hiding dialog box");
        main.closeKeyboard();
        main.hideSystemUI();
        if (websiteLaunchDialog != null) {
            websiteLaunchDialog.hide();
        }
    }

    public String cleanYouTubeURL(String url) {
        String id = getYouTubeID(url);
        Log.i(TAG, "YouTube ID = " + id + " from " + url);
        return "https://www.youtube.com/watch?v=" + id + "&t=1";
    }

    public boolean launchYouTube(String url, boolean updateTask) {
        launchingVR = true; //activate button pressing
        final String youTubePackageName = "com.google.android.youtube"; //TODO don't hardcode the package name
        //String cleanURL = cleanYouTubeURL(url);
        Uri uri = Uri.parse("vnd.youtube://" + getYouTubeID(url));
        Log.w(TAG, "YouTUBE: " + uri);

        main.UnMuteAudio(); //turn sound back on, in case muted earlier
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + getYouTubeID(url) + "?t=1"));
        appIntent.setPackage(youTubePackageName);
        //startActivity(appIntent);

        //test if the YouTube app exists
        ActivityInfo ai = appIntent.resolveActivityInfo(main.getPackageManager(), 0);
        if (ai == null) {
            //the YouTube app doesn't exist
            Log.d(TAG, "YOUTUBE APP DOESN'T EXIST?! " + youTubePackageName);
            //if installing, try that first.
            if (main.autoInstallApps) {
                main.getAppManager().autoInstall(youTubePackageName, "YouTube");
                return false;
            } else {
                return launchWebsite(url, true); //fall back
            }
        }

        try {
            //schedule this to run as soon as remote brings this to the front
            scheduleActivityLaunch(appIntent, updateTask, youTubePackageName, "YouTube", "VR Video", cleanYouTubeURL(url));

            //alert other peers as needed
            main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.LAUNCH_SUCCESS + "YT id=" + getYouTubeID(url) + ":" + main.getNearbyManager().getID() + ":" + youTubePackageName, main.getNearbyManager().getAllPeerIDs());

            return true; //assume the best if we get to here

        } catch (Exception ex) {
            return launchWebsite(url, true); //fall back
        }
    }

    private void scheduleActivityLaunch(Intent appIntent) {
        if (!main.hasWindowFocus()) {
            main.appIntentOnFocus = appIntent;
            main.getRemoteDispatchService().bringMainToFront();
        } else {
            main.startActivity(appIntent);
        }
    }

    private void scheduleActivityLaunch(Intent appIntent, boolean updateTask, String packageName, String appName, String taskType, String url) {
        scheduleActivityLaunch(appIntent);

        if (updateTask) {
            main.updateFollowerCurrentTask(packageName, appName, taskType, url);
        }
    }


}
