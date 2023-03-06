package com.lumination.leadme.managers;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.LumiSpinnerAdapter;
import com.lumination.leadme.controller.Controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

public class SearchManager {
    //define callback interface
    public interface CallbackInterface {
        void onURLSelected(String url);
    }

    private final String TAG = "SearchManager";

    public static boolean isYouTube = false;

    private final Spinner searchSpinner;
    private final String[] searchSpinnerItems;

    private AlertDialog searchDialog;
    private final AlertDialog originalDialog;
    private final View searchDialogView;
    private final LeadMeMain main;

    final CallbackInterface callback;

    //private boolean searchYoutube = true;
    private final int SEARCH_WEB = 0;
    private final int SEARCH_YOUTUBE = 1;
    private int searchType = SEARCH_WEB;
    private WebView searchWebView;

    public SearchManager(LeadMeMain main, View originalView, AlertDialog originalDialog, CallbackInterface callback) {
        this.main = main;
        this.originalDialog = originalDialog;
        this.callback = callback;

        searchDialogView = View.inflate(main, R.layout.e__preview_url_search, null);

        originalView.findViewById(R.id.url_search_btn).setOnClickListener(v -> {
            originalDialog.dismiss();
            buildAndShowSearchDialog();
        });

        searchSpinner = (Spinner) searchDialogView.findViewById(R.id.search_spinner);
        searchSpinnerItems = new String[2];
        initialiseSpinner();
    }

    private void initialiseSpinner() {
        searchSpinnerItems[0] = "Google search";
        searchSpinnerItems[1] = "YouTube search";
        Integer[] search_imgs = {R.drawable.search_google, R.drawable.search_yt};
        LumiSpinnerAdapter search_adapter = new LumiSpinnerAdapter(main, R.layout.row_search_spinner, searchSpinnerItems, search_imgs);
        searchSpinner.setAdapter(search_adapter);
    }

    public void setErrorPreview(String searchTerm) {
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

    private void hideSearchDialog() {
        Log.d(TAG, "hideSearchDialog: ");
        main.closeKeyboard();
        main.hideSystemUI();

        if (searchDialog != null) {
            searchDialog.dismiss();
        }
    }

    public void buildAndShowSearchDialog(int type) {
        searchType=type;
        buildAndShowSearchDialog();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void buildAndShowSearchDialog() {
        Log.d(TAG, "buildAndShowSearchDialog: ");
        originalDialog.dismiss();
        searchDialogView.findViewById(R.id.web_search_title).setVisibility(View.VISIBLE);
        searchDialogView.findViewById(R.id.url_error_layout).setVisibility(View.GONE);

        main.backgroundExecutor.submit(() -> {
            if (!Controller.getInstance().getPermissionsManager().isInternetConnectionAvailable()) {
                Log.w(TAG, "No internet connection in buildAndShowSearch");
                LeadMeMain.UIHandler.post(() -> {
                    Controller.getInstance().getDialogManager().showWarningDialog("No Internet Connection",
                            "Internet based functions are unavailable at this time. " +
                                    "Please check your WiFi connection and try again.");
                    hideSearchDialog();
                });
            }
        });

        //instantiates the search dialog popup if it does not already exist
        if (searchDialog == null) {
            searchDialog = new AlertDialog.Builder(main)
                    .setView(searchDialogView)
                    .show();
            searchDialog.setOnDismissListener(dialog -> main.hideSystemUI());
            DisplayMetrics displayMetrics = main.getResources().getDisplayMetrics();
            searchDialogView.getLayoutParams().width = displayMetrics.widthPixels - 140;

            searchDialogView.findViewById(R.id.url_search_bar).requestFocus();
            searchDialogView.findViewById(R.id.web_search_title).setVisibility(View.VISIBLE);
            searchDialogView.findViewById(R.id.url_error_layout).setVisibility(View.GONE);
            searchWebView = searchDialogView.findViewById(R.id.webview_preview);
            searchWebView.getSettings().setJavaScriptEnabled(true); // enable javascript
            searchWebView.canGoBack();
            searchWebView.setVisibility(View.GONE);

            final SearchView searchView = searchDialogView.findViewById(R.id.url_search_bar);
            searchView.setMaxWidth(Integer.MAX_VALUE); //ensures it fills whole space on init

            if (searchView.getQuery().length() > 0) {
                if (!isYouTube) {
                    searchWebView.setVisibility(View.VISIBLE);
                    //fixes the webpage loading in background
                    searchWebView.loadUrl("https://www.google.com/search?q=" + searchView.getQuery());
                }
            }
            if(searchType == SEARCH_YOUTUBE){
                searchSpinner.setSelection(1);
            }
            searchSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "Search mode: " + searchSpinnerItems[position]);

                    if (searchSpinnerItems[position].startsWith("YouTube")) {
                        ((TextView) searchDialogView.findViewById(R.id.web_search_title)).setText(R.string.search_youtube);
                        searchType = SEARCH_YOUTUBE;

                    } else if (searchSpinnerItems[position].startsWith("Google")) {
                        ((TextView) searchDialogView.findViewById(R.id.web_search_title)).setText(R.string.search_web);
                        searchType = SEARCH_WEB;

                    }
                    searchText(searchView.getQuery().toString());
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
            }

            searchDialog.findViewById(R.id.back_btn).setOnClickListener(v -> {
                searchWebView.clearCache(false);
                searchDialog.dismiss();
                if(searchDialog != null) {
                    searchDialog.show();
                }
            });

        } else {
            searchDialogView.findViewById(R.id.url_search_bar).requestFocus();
            searchDialog.show();
        }


        Log.w(TAG, "Is this from YouTube? " + isYouTube);

        switch(searchType){
            case SEARCH_WEB:
                isYouTube=false;
                searchSpinner.setSelection(0);
                break;
            case SEARCH_YOUTUBE:
                isYouTube=true;
                searchSpinner.setSelection(1);
                break;
        }
        populateSearch();
    }

    private void populateSearch() {
        Log.d(TAG, "populateSearch: ");
        final SearchView searchView = searchDialogView.findViewById(R.id.url_search_bar);

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
                    for (String part : parts) {
                        if (part.startsWith("url=")) {
                            url = part.substring(4);
                        }
                    }
                    Log.d(TAG, "onLoadResource valid: " + url);
                    //searchDialog.dismiss();
                    hideSearchDialog();
                    callback.onURLSelected(url);
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
                    callback.onURLSelected(URL);

                    LeadMeMain.runOnUI(() -> {
                        main.hideSystemUI();
                        main.closeKeyboard();
                    });
                    return true;
                }

                LeadMeMain.runOnUI(() -> {
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
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(bitmap == null) {
                        Log.d(TAG, "intercepted jpg: null exception");
                        LeadMeMain.runOnUI(() -> Toast.makeText(main.getApplicationContext(), "An image was unable to be displayed.", Toast.LENGTH_SHORT).show());
                        return null;
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
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(bitmap == null) {
                        Log.d(TAG, "intercepted png: null exception");
                        LeadMeMain.runOnUI(() -> Toast.makeText(main.getApplicationContext(), "An image was unable to be displayed.", Toast.LENGTH_SHORT).show());
                        return null;
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
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(bitmap == null) {
                        Log.d(TAG, "intercepted webp: null exception");
                        LeadMeMain.runOnUI(() -> Toast.makeText(main.getApplicationContext(), "An image was unable to be displayed.", Toast.LENGTH_SHORT).show());
                        return null;
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

        LeadMeMain.runOnUI(() -> {
            main.hideSystemUI();
            main.closeKeyboard();
        });

        return false;
    }
}
