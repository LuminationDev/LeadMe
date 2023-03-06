package com.lumination.leadme.managers;

import android.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.FavouritesAdapter;
import com.lumination.leadme.controller.Controller;

public class FavouritesManager {
    private final String TAG = "FavManager";

    public static final int LAUNCHTYPE_WEB = 0;
    public static final int LAUNCHTYPE_VR = 1;

    public static final int FAVTYPE_APP = 0;
    public static final int FAVTYPE_URL = 1;
    public static final int FAVTYPE_YT = 2;

    private static final String suffix = "?rel=0&autoplay=0"; //&autoplay=1&start=1&end=10&controls=0&rel=0";

    private TextView warningTextView;
    public AlertDialog warningDialog;

    private AlertDialog urlYtFavDialog;
    private final View webYouTubeFavView;

    public static int currentType = 0;
    public static boolean adding_to_fav = false;

    final private static int CLEAR_ALL = 0;
    final private static int CLEAR_VID = 1;
    final private static int CLEAR_URL = 2;
    private int whatToClear = -1;

    private final LeadMeMain main;
    private final FavouritesAdapter urlFavouritesAdapter;
    private final FavouritesAdapter youTubeFavouritesAdapter;
    private final FavouritesAdapter appFavouritesAdapter;

    public FavouritesManager(LeadMeMain main) {
        this.main = main;

        webYouTubeFavView = View.inflate(main, R.layout.d__url_yt_favourites, null);
        urlFavouritesAdapter = new FavouritesAdapter(this, main, webYouTubeFavView, FavouritesManager.FAVTYPE_URL, 10);
        youTubeFavouritesAdapter = new FavouritesAdapter(this, main, webYouTubeFavView, FavouritesManager.FAVTYPE_YT, 10);
        appFavouritesAdapter = new FavouritesAdapter(this, main, webYouTubeFavView, FavouritesManager.FAVTYPE_APP, 4);

        ((GridView) webYouTubeFavView.findViewById(R.id.yt_favourites)).setAdapter(getYouTubeFavouritesAdapter());
        ((GridView) webYouTubeFavView.findViewById(R.id.url_favourites)).setAdapter(getUrlFavouritesAdapter());

        webYouTubeFavView.findViewById(R.id.clear_fav_btn).setOnClickListener(v -> {
            showClearWebFavDialog(CLEAR_ALL);
            getYouTubeFavouritesAdapter().clearFavourites();
            getUrlFavouritesAdapter().clearFavourites();
        });

        setupViews();
        setupWarningDialog();
    }

    private void setupViews() {
        Log.d(TAG, "setupViews: ");
        ((GridView) webYouTubeFavView.findViewById(R.id.yt_favourites)).setAdapter(getYouTubeFavouritesAdapter());
        ((GridView) webYouTubeFavView.findViewById(R.id.url_favourites)).setAdapter(getUrlFavouritesAdapter());

        webYouTubeFavView.findViewById(R.id.clear_fav_btn).setOnClickListener(v -> {
            showClearWebFavDialog(CLEAR_ALL);
            getYouTubeFavouritesAdapter().clearFavourites();
            getUrlFavouritesAdapter().clearFavourites();
        });

        webYouTubeFavView.findViewById(R.id.yt_add_btn).setOnClickListener(v -> {
            SearchManager.isYouTube = true;
            adding_to_fav = true;
            Log.w(TAG, "YouTube add!");
            Controller.getInstance().getWebManager().showWebLaunchDialog(adding_to_fav);
            urlYtFavDialog.dismiss();
        });

        webYouTubeFavView.findViewById(R.id.url_add_btn).setOnClickListener(v -> {
            SearchManager.isYouTube = false;
            adding_to_fav = true;
            Log.w(TAG, "URL add!");
            Controller.getInstance().getWebManager().showWebLaunchDialog(adding_to_fav);
            urlYtFavDialog.dismiss();
        });

        webYouTubeFavView.findViewById(R.id.yt_del_btn).setOnClickListener(v -> showClearWebFavDialog(CLEAR_VID));
        webYouTubeFavView.findViewById(R.id.url_del_btn).setOnClickListener(v -> showClearWebFavDialog(CLEAR_URL));
    }

    private void setupWarningDialog() {
        Log.d(TAG, "setupWarningDialog: ");
        View warningDialogView = View.inflate(main, R.layout.e__fav_clear_confirmation_popup, null);
        warningTextView = warningDialogView.findViewById(R.id.favclear_comment);

        warningDialogView.findViewById(R.id.ok_btn).setOnClickListener(v -> {
            switch (whatToClear) {
                case CLEAR_ALL:
                    getYouTubeFavouritesAdapter().clearFavourites();
                    getUrlFavouritesAdapter().clearFavourites();
                    break;

                case CLEAR_VID:
                    getYouTubeFavouritesAdapter().clearFavourites();
                    break;

                case CLEAR_URL:
                    getUrlFavouritesAdapter().clearFavourites();
                    break;
            }
            warningDialog.dismiss();
            getUrlFavouritesAdapter().notifyDataSetChanged();
            getYouTubeFavouritesAdapter().notifyDataSetChanged();
        });

        warningDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> warningDialog.dismiss());

        warningDialog = new AlertDialog.Builder(main)
                .setView(warningDialogView)
                .create();
        warningDialog.setOnDismissListener(dialog -> main.hideSystemUI());

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

    public void launchUrlYtFavourites(int type) {
        Log.d(TAG, "launchUrlYtFavourites: ");
        currentType = type;

        getUrlFavouritesAdapter().clearPreviews();
        getUrlFavouritesAdapter().notifyDataSetChanged();
        getYouTubeFavouritesAdapter().clearPreviews();
        getYouTubeFavouritesAdapter().notifyDataSetChanged();

        if (urlYtFavDialog == null) {
            urlYtFavDialog = new AlertDialog.Builder(main)
                    .setView(webYouTubeFavView)
                    .create();
            urlYtFavDialog.setOnDismissListener(dialog -> main.hideSystemUI());

            webYouTubeFavView.findViewById(R.id.back_btn).setOnClickListener(v -> urlYtFavDialog.dismiss());
        }

        urlYtFavDialog.show();
    }

    public void hideFavDialog() {
        Log.d(TAG, "hideFavDialog: ");
        main.closeKeyboard();
        main.hideSystemUI();
        urlYtFavDialog.dismiss();
    }

    public static String getSuffix() {
        return suffix;
    }

    public FavouritesAdapter getUrlFavouritesAdapter() {
        return urlFavouritesAdapter;
    }

    public FavouritesAdapter getYouTubeFavouritesAdapter() {
        return youTubeFavouritesAdapter;
    }

    public FavouritesAdapter getAppFavouritesAdapter() {
        return appFavouritesAdapter;
    }
}
