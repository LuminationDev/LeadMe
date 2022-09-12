package com.lumination.leadme.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.linkpreview.LinkPreviewCallback;
import com.lumination.leadme.linkpreview.SourceContent;
import com.lumination.leadme.linkpreview.TextCrawler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FavouritesManager extends BaseAdapter {

    private final String TAG = "FavManager";

    public static final int FAVTYPE_URL = 0;
    public static final int FAVTYPE_YT = 1;
    public static final int FAVTYPE_APP = 2;

    private TextView favMsgView, favTitleView;
    private ImageView favImgView;
    private Button favOKBtn;
    private AlertDialog favouritesDialog;

    private String favPackageName = "";
    private boolean favAdding = true;

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private final ArrayList<String> actualItems = new ArrayList<>();
    private final ArrayList<String> contentList = new ArrayList<>();
    private final ArrayList<String> titleList = new ArrayList<>();
    private final ArrayList<Drawable> iconList = new ArrayList<>();

    private final HashMap<String, Drawable> previewStorage = new HashMap<>();
    private final Drawable activeBg;
    private final Drawable emptyBg;
    private final Drawable placeholder;
    private final LayoutInflater inflater;

    private final int favType;
    private final int maxLimit;

    private String favPrefix;

    private final LeadMeMain main;
    private final WebManager webManager;

    public static View FavouritesScreen;

    public FavouritesManager(LeadMeMain main, WebManager webManager, int favType, int maxLimit) {
        this.main = main;
        this.webManager = webManager;
        this.favType = favType;
        this.maxLimit = maxLimit;

        inflater = LayoutInflater.from(main);
        activeBg = ResourcesCompat.getDrawable(main.getResources(), R.drawable.add_favourite_active, null);
        emptyBg = ResourcesCompat.getDrawable(main.getResources(), R.drawable.add_favourite, null);
        placeholder = ResourcesCompat.getDrawable(main.getResources(), R.drawable.web_no_preview, null);

        sharedPreferences = main.getSharedPreferences(main.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        switch (favType) {
            case FAVTYPE_APP:
                favPrefix = main.getResources().getString(R.string.preference_fav_app_prefix);
                break;
            case FAVTYPE_URL:
                favPrefix = main.getResources().getString(R.string.preference_fav_url_prefix);
                break;
            case FAVTYPE_YT:
                favPrefix = main.getResources().getString(R.string.preference_fav_yt_prefix);
                break;
        }

        populateFavouritesFromPreferences(); //initialise list
        setupFavouritesDialog();
    }

    private void updateSizes(int itemCount, GridView view) {
        //this is only applicable for scrolling views
        //the app favourites is a static view
        if (favType != FAVTYPE_APP) {
            //get dp conversion
            DisplayMetrics displayMetrics = main.getResources().getDisplayMetrics();
            int itemWidth = (int) (160 * displayMetrics.density);
            int calculatedWidth = itemWidth * itemCount;
            Log.w(TAG, "Calculated width for parent is: " + calculatedWidth + ", " + actualItems + ", " + contentList + ", " + itemCount);

            if (calculatedWidth < displayMetrics.widthPixels - 145) {
                calculatedWidth = displayMetrics.widthPixels - 145;
            }
            view.setNumColumns(itemCount);
            view.getLayoutParams().width = calculatedWidth;
        }
    }

    public void clearFavourites() {
        editor.remove(favPrefix);     //TODO work this out
        editor.commit();

        actualItems.clear();
        contentList.clear();
        titleList.clear();
        iconList.clear();

        formatAndSavePrefs();

        notifyDataSetChanged();
        updateListVisibilities();
    }

    private void populateFavouritesFromPreferences() {
        //retrieve content and put into lists
        Set<String> tmpContent = sharedPreferences.getStringSet(favPrefix, new HashSet<>());
        Object[] content = tmpContent.toArray();
        for (Object o : content) {
            if (o != null && o.toString().replace("::::", "").trim().length() > 0) {
                String[] tmp = o.toString().split(breaker);
                actualItems.add(tmp[0]);
                contentList.add(tmp[0]);

                if(tmp.length > 1) {
                    titleList.add(tmp[1]);
                } else {
                    titleList.add(tmp[0]);
                }

                iconList.add(null);
            }
        }

        //if web favourites, add in any preview or placeholder thumbnails as needed
        if (favType != FAVTYPE_APP) {
            for (int i = 0; i < contentList.size(); i++) {
                String url = contentList.get(i);
                if (!url.isEmpty() && !previewStorage.containsKey(url)) {
                    iconList.add(placeholder); //placeholder
                } else {
                    iconList.add(previewStorage.get(url));
                }
            }
            updateListVisibilities();

        }

        //if app favourites, fill with empty content so we get placeholder images
        if (favType == FAVTYPE_APP) {
            for (int i = contentList.size(); i < maxLimit; i++) {
                contentList.add("");
                titleList.add("");
                iconList.add(null);
            }
        }
        Log.d(TAG, "SHARED PREFs: " + sharedPreferences.getAll().toString());
        Log.d(TAG, "FAV LIST: " + contentList + " for type " + favType + " with " + titleList);
    }

    LinkPreviewCallback linkPreviewCallback = new LinkPreviewCallback() {
        @Override
        public void onPre() {
            // Any work that needs to be done before generating the preview. Usually inflate
            // your custom preview layout here.
        }

        @Override
        public void onPos(final SourceContent sourceContent, boolean b) {
            String urlStr = sourceContent.getUrl();
            Log.d(TAG, "PREVIEW RETURNED! " + urlStr + ", " + sourceContent.isSuccess());
            gettingPreviews.remove(urlStr); //whatever the outcome, we're done with this one
            refreshAllPreviews();

            for (int i = 0; i < contentList.size(); i++) {

                try {
                    URL favUrl = new URL(contentList.get(i));
                    URL newUrl = new URL(sourceContent.getUrl());

                    if (contentList.get(i).contains(sourceContent.getUrl()) || favUrl.sameFile(newUrl)) {
                        //update if new information is returned
                        //otherwise stick with what was last saved
                        if (!sourceContent.getTitle().isEmpty()) {
                            titleList.set(i, sourceContent.getTitle());
                            formatAndSavePrefs();
                        }

                        final int prevIndex = i;
                        String img = "";
                        if (!sourceContent.getImages().isEmpty()) {
                            img = sourceContent.getImages().get(0);
                        }
                        if (sourceContent.isSuccess()) {
                            ImageView tmpView = new ImageView(main);
                            UrlImageViewHelper.setUrlDrawable(tmpView, img, (imageView, loadedBitmap, url, loadedFromCache) -> {
                                Drawable drawable = imageView.getDrawable();
                                iconList.set(prevIndex, drawable);
                                previewStorage.put(sourceContent.getFinalUrl(), drawable);

                                if (webManager.getPreviewTitle() == sourceContent.getTitle()) {
                                    Log.d(TAG, "This is the one we're trying to show!");
                                    webManager.getPreviewImageView().setImageDrawable(drawable);
                                } else {
                                    Log.d(TAG, "This is another we're loading in the background");
                                }
                            });
                        } else {
                            Drawable drawable = ResourcesCompat.getDrawable(main.getResources(), R.drawable.placeholder_broken_img, null);
                            iconList.set(prevIndex, drawable);
                            previewStorage.put(sourceContent.getFinalUrl(), drawable);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing preview: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            notifyDataSetChanged();
        }

    };

    public void addCurrentPreviewToFavourites() {
        //update local/working variables
        String url = webManager.getPushURL();
        String title = webManager.getPreviewTitle();
        Drawable icon = webManager.getPreviewImage();

        Log.d(TAG, "addCurrentPreviewToFavourites: "+url);
        if (title.isEmpty()) {
            title = "Loading...";
        }

        if (icon == null) {
            icon = placeholder;
        }

        //if (url.contains("/embed/")) {
        //do this to keep everything consistent and avoid doubling up URLs
        //update(jake) was a nice thought but unfortunately just wiped the url from existence
        //url = webManager.cleanYouTubeURLWithoutStart(url);
        Log.d(TAG, "FAV! " + url);
        //}

        addToFavourites(url, title, icon);
    }

    private void updateListVisibilities() {
        int itemCount = contentList.size();
        if (itemCount > 0) {
            if (favType == FAVTYPE_YT) {
                webManager.getWebYouTubeFavView().findViewById(R.id.yt_no_favs).setVisibility(View.GONE);
                GridView view = webManager.getWebYouTubeFavView().findViewById(R.id.yt_favourites);
                view.setVisibility(View.VISIBLE);
                updateSizes(itemCount, view);

            }
           if (favType == FAVTYPE_URL) {
                webManager.getWebYouTubeFavView().findViewById(R.id.url_no_favs).setVisibility(View.GONE);
                GridView view = webManager.getWebYouTubeFavView().findViewById(R.id.url_favourites);
                view.setVisibility(View.VISIBLE);
                updateSizes(itemCount, view);
            }
           if (favType == FAVTYPE_APP) {
                webManager.getWebYouTubeFavView().findViewById(R.id.app_no_favs).setVisibility(View.GONE);
                GridView view = webManager.getWebYouTubeFavView().findViewById(R.id.app_favourites);
                view.setVisibility(View.VISIBLE);
                updateSizes(itemCount, view);
            }
        }

        else {
            if (favType == FAVTYPE_YT) {
                webManager.getWebYouTubeFavView().findViewById(R.id.yt_no_favs).setVisibility(View.VISIBLE);
                webManager.getWebYouTubeFavView().findViewById(R.id.yt_favourites).setVisibility(View.GONE);

            }
            if (favType == FAVTYPE_URL) {
                webManager.getWebYouTubeFavView().findViewById(R.id.url_no_favs).setVisibility(View.VISIBLE);
                webManager.getWebYouTubeFavView().findViewById(R.id.url_favourites).setVisibility(View.GONE);
            }
            if (favType == FAVTYPE_APP) {
                webManager.getWebYouTubeFavView().findViewById(R.id.app_no_favs).setVisibility(View.VISIBLE);
                webManager.getWebYouTubeFavView().findViewById(R.id.app_favourites).setVisibility(View.GONE);
            }
        }
    }

    //TODO pretty sure this is a dumb way to do it
    String breaker = "::::";

    //update the local lists, then call this to store the result
    private void formatAndSavePrefs() {
        ArraySet<String> tmpContent = new ArraySet<>();
        for (int i = 0; i < contentList.size(); i++) {
            if (contentList.get(i).trim().length() > 0) {
                tmpContent.add(contentList.get(i) + breaker + titleList.get(i));
            }
        }
        editor.putStringSet(favPrefix, tmpContent);
        editor.commit();
        notifyDataSetChanged();
        updateListVisibilities();

        Log.d(TAG, contentList.toString());
        Log.d(TAG, sharedPreferences.getAll().toString());
    }

    public boolean isInFavourites(String content) {
        Log.e(TAG, "Looking for " + content + " in " + contentList + " >> " + contentList.contains(content));
        return contentList.contains(content);
    }

    public void addToFavourites(String content, String title) {
        addToFavourites(content, title, placeholder);
    }

    //content is URL or packageName
    public void addToFavourites(String content, String title, Drawable icon) {
        Log.d(TAG, "addToFavourites: "+content);

        //get the youtube video id as different links can add the same video
        String pattern = "(?<=watch\\?v=|/videos/|embed/)[^#&?]*";
        String youtubeId = content;

        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(content);

        if(matcher.find()){
            youtubeId = matcher.group();
        }

        for(int x=0; x < contentList.size(); x++) {
            if(contentList.get(x).contains(youtubeId)) {
                Log.d(TAG, "Youtube ID duplicate found");
                return;
            }
        }

        //update local/working variables
        int thisIndex = getNextFavIndex();
        //Log.d(TAG, "Next index: " + thisIndex + ", putting " + content + " in " + contentList);

        if (thisIndex == -1) { //add
            showFullAlertFavDialog(content);

        } else if (thisIndex >= contentList.size()) { //add a new one
            actualItems.add(content);
            contentList.add(content);
            titleList.add(title);
            iconList.add(icon);

        } else { //update an existing one
            if (thisIndex < actualItems.size()) {
                actualItems.set(thisIndex, content);
            } else {
                actualItems.add(content);
            }
            contentList.set(thisIndex, content);
            titleList.set(thisIndex, title);
            iconList.set(thisIndex, icon);
        }

        formatAndSavePrefs();
    }

    public void deleteFromFavourites(String packageName) {
        int thisIndex = contentList.indexOf(packageName);

        //Log.d(TAG, thisIndex + ", " + actualItems + ", " + contentList + ", " + titleList + ", " + iconList);

        actualItems.remove(packageName);

        //for app favs (because of placeholder icons), replace with empty string
        if (favType == FAVTYPE_APP && thisIndex < contentList.size()) {
            contentList.set(thisIndex, "");
            titleList.set(thisIndex, "");
            iconList.set(thisIndex, null);

            //otherwise just remove it
        } else {
            contentList.remove(thisIndex);
            titleList.remove(thisIndex);
            iconList.remove(thisIndex);
        }

        formatAndSavePrefs();
    }

    private void setupFavouritesDialog() {
        View favouritesView = View.inflate(main, R.layout.e__edit_favourites, null);
        favImgView = favouritesView.findViewById(R.id.fav_app_icon);
        favMsgView = favouritesView.findViewById(R.id.fav_app_confirm_txt);
        favTitleView = favouritesView.findViewById(R.id.fav_app_title);
        favOKBtn = favouritesView.findViewById(R.id.ok_btn);
        Button favBackBtn = favouritesView.findViewById(R.id.back_btn);

        favOKBtn.setOnClickListener(v -> {
            if (favAdding) {
                addToFavourites(favPackageName, favTitleView.getText().toString(), null);
                favAdding = false; //reset
                if (favType != FAVTYPE_APP) {
                    webManager.adding_to_fav = false; //reset
                }
            } else {
                deleteFromFavourites(favPackageName);
            }
            favouritesDialog.dismiss();
        });

        favBackBtn.setOnClickListener(v -> favouritesDialog.dismiss());

        favouritesDialog = new AlertDialog.Builder(main).setView(favouritesView).create();
        favouritesDialog.setOnDismissListener(dialog -> main.hideSystemUI());
    }

    private void showAddFavDialog(String packageName) {
        favAdding = true; //adding
        favPackageName = packageName; //assign so we can use this in dialogs/buttons
        final String title = AppManager.getAppName(packageName);
        final Drawable icon = AppManager.getAppIcon(packageName);
        favMsgView.setText(main.getResources().getString(R.string.add_this_app_to_favourites));
        favOKBtn.setVisibility(View.VISIBLE);
        favImgView.setImageDrawable(icon);
        favTitleView.setText(title);
        favouritesDialog.show();
    }

    private void showDeleteFavDialog(String packageName) {
        favAdding = false; //deleting
        favPackageName = packageName; //assign so we can use this in dialogs/buttons

        if (favType == FAVTYPE_APP) {
            favImgView.setImageDrawable(AppManager.getAppIcon(packageName));
            favTitleView.setText(AppManager.getAppName(packageName));
        } else {
            int thisIndex = contentList.indexOf(packageName);
            favImgView.setImageDrawable(getPreview(packageName));
            favTitleView.setText(titleList.get(thisIndex));
        }

        favMsgView.setText(main.getResources().getString(R.string.delete_this_app_from_favourites));
        favOKBtn.setVisibility(View.VISIBLE);
        favouritesDialog.show();
    }

    private void showFullAlertFavDialog(String packageName) {
        favPackageName = packageName; //assign so we can use this in dialogs/buttons
        final String title = AppManager.getAppName(packageName);
        final Drawable icon = AppManager.getAppIcon(packageName);
        favMsgView.setText(main.getResources().getString(R.string.all_your_favourites_are_full));
        favOKBtn.setVisibility(View.GONE);
        favImgView.setImageDrawable(icon);
        favTitleView.setText(title);
        favouritesDialog.show();
    }

    private void showAlreadyFavDialog(String packageName) {
        favPackageName = packageName; //assign so we can use this in dialogs/buttons
        final String title = AppManager.getAppName(packageName);
        final Drawable icon = AppManager.getAppIcon(packageName);
        favMsgView.setText(main.getResources().getString(R.string.already_in_fav));
        favOKBtn.setVisibility(View.GONE);
        favImgView.setImageDrawable(icon);
        favTitleView.setText(title);
        favouritesDialog.show();
    }

    /**
     * @return -1 if the list is full, next empty index otherwise
     */
    private int getNextFavIndex() {

        if (contentList.isEmpty()) {
            return 0;
        }

        int lastChecked = 0;
        for (int i = 0; i < contentList.size(); i++) {
            lastChecked = i;
            String tmp = contentList.get(i).trim();
            if (tmp.length() == 0) {
                //an empty one!
                return i;
            } else if (i == contentList.size() - 1) {
                //last item in list, and the item contains content
                break;
            }
        }

        if (lastChecked < maxLimit) {
            return (lastChecked + 1); //we're under the limit, add another!
        } else {
            return -1; //full!
        }
    }

    public void manageFavouritesEntry(String packageName) {
        //if it contains the package we're checking, assume we want to delete it
        if (contentList.contains(packageName)) {
            showAlreadyFavDialog(packageName);

        } else {
            //if it has 4 things in it, it's full!
            if (actualItems.size() >= maxLimit) {
                //show full app favs popup
                showFullAlertFavDialog(packageName);

            } else {
                //show add popup
                showAddFavDialog(packageName);
            }
        }
    }

    @Override
    public int getCount() {
        return contentList.size();
    }

    @Override
    public Object getItem(int position) {
        return contentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (favType == FAVTYPE_APP) {
            final String content = contentList.get(position);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_fav_app, null); // parent, false);
                final ViewHolder viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }
            updateAppFavView(convertView, content);

        } else {
            String url = contentList.get(position);

            String title = titleList.get(position);
            Drawable icon = iconList.get(position);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_fav_url, null);//parent, false);
                final ViewHolder viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }
            updateWebFavView(convertView, url, title, icon);
        }

        return convertView;
    }

    public Drawable getPreview(String url) {
        int index = contentList.indexOf(url);
        if (index != -1) {
            return iconList.get(index);
        } else {
            return null;
        }
    }

    public String getTitle(String url) {
        int index = contentList.indexOf(url);
        if (index != -1) {
            return titleList.get(index);
        } else {
            return null;
        }
    }

    public void updateTitle(String url, String title) {
        int index = contentList.indexOf(url);
        if (index != -1) {
            titleList.set(index, title);
            formatAndSavePrefs();
        }
    }

    public void updatePreview(String url, Drawable icon) {
        int index = contentList.indexOf(url);
        if (index != -1) {
            iconList.set(index, icon);
        }
    }

    public void clearPreviews() {
        gettingPreviews.clear();
    }

    private final HashMap<String, TextCrawler> crawlers = new HashMap<>();
    private TextCrawler tmpCrawler;

    public void refreshPreview(String url) {
        String tmpUrl = url.replace(webManager.getSuffix(), ""); //clean the URL
        Log.d(TAG, "Trying to retrieve preview for " + tmpUrl + ", " + gettingPreviews);

        if (!gettingPreviews.contains(tmpUrl)) {

            tmpCrawler = crawlers.get(tmpUrl);
            if (tmpCrawler != null) {
                tmpCrawler.cancel(); //cancel the old
            }
            tmpCrawler = new TextCrawler(webManager); //make a freshie


            gettingPreviews.add(tmpUrl); //storing it here means we only try once per url
            crawlers.put(tmpUrl, tmpCrawler);

            main.backgroundExecutor.submit(() -> tmpCrawler.makePreview(linkPreviewCallback, tmpUrl));
        }
    }

    private void refreshAllPreviews() {
        for (int i = 0; i < contentList.size(); i++) {
            //if current icon is null or placeholder, then try again to load preview
            if (iconList.get(i) == null || iconList.get(i) == placeholder) {
                refreshPreview(contentList.get(i));
            }
        }
    }

    private final ArrayList<String> gettingPreviews = new ArrayList<>();

    private void updateWebFavView(View convertView, final String url, final String title, Drawable icon) {
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        //check if the icon needs updating
        if ((icon == null || icon == placeholder) && !url.isEmpty()) {
            Drawable storedIcon = getPreview(url);
            if (storedIcon != null && storedIcon != placeholder) {
                Log.d(TAG, "Getting preview for " + url + " from storage");
                icon = getPreview(url);

            } else {
                Log.d(TAG, "Setting placeholder as preview for " + url);
                viewHolder.favouriteIcon.setImageDrawable(placeholder);
                refreshPreview(url);
            }
        }

        //only update content if needed
        viewHolder.favouriteName.setText(title);
        viewHolder.favouriteIcon.setImageDrawable(icon);
        convertView.setOnClickListener(v -> {
            if (favType != FAVTYPE_APP) {
                Log.d(TAG, "Showing preview" + url);
                webManager.adding_to_fav = false;
                webManager.showPreview(url);
            }
            webManager.hideFavDialog();
        });

        convertView.setLongClickable(true);
        convertView.setOnLongClickListener(v -> {
            if (favType != FAVTYPE_APP) {
                if (url == null || url.trim().isEmpty()) {
                    return false;
                }
                showDeleteFavDialog(url);
            }
            return true;
        });
    }


    private void updateAppFavView(View convertView, final String favPackage) {
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        if (favPackage.isEmpty()) { //placeholder
            viewHolder.favouriteName.setText("");
            viewHolder.favouriteIcon.setImageDrawable(null);
            viewHolder.favouriteIcon.setBackground(emptyBg);
            viewHolder.favouriteIcon.setElevation(10);

            convertView.setClickable(false);
            convertView.setLongClickable(false);

        } else { //actual favourite
            final String appName = AppManager.getAppName(favPackage);
            final Drawable appIcon = AppManager.getAppIcon(favPackage);
            viewHolder.favouriteName.setText(appName);
            viewHolder.favouriteIcon.setImageDrawable(appIcon);
            viewHolder.favouriteIcon.setBackground(activeBg);
            viewHolder.favouriteIcon.setElevation(10);

            convertView.setClickable(true);
            convertView.setOnClickListener(v -> {
                favAdding = false;
                Controller.getInstance().getDialogManager().showAppPushDialog(appName, appIcon, favPackage);
            });

            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(v -> {
                if (favPackage == null || favPackage.trim().isEmpty()) {
                    return false;
                }
                showDeleteFavDialog(favPackage);
                return true;
            });
        }
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder {
        final TextView favouriteName;
        final ImageView favouriteIcon;

        ViewHolder(View itemView) {
            favouriteName = itemView.findViewById(R.id.fav_name);
            favouriteIcon = itemView.findViewById(R.id.fav_icon);
        }
    }
}
