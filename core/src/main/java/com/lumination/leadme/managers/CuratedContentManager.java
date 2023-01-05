package com.lumination.leadme.managers;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import com.google.api.Distribution;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lumination.leadme.BR;
import com.lumination.leadme.adapters.CuratedContentAdapter;
import com.lumination.leadme.adapters.FavouritesAdapter;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.linkpreview.LinkPreviewCallback;
import com.lumination.leadme.linkpreview.SourceContent;
import com.lumination.leadme.linkpreview.TextCrawler;
import com.lumination.leadme.models.CuratedContentItem;
import com.lumination.leadme.models.CuratedContentType;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CuratedContentManager {
    private final String TAG = "CuratedContentManager";

    public static List<Float> currentYearSelection = null;
    public static int currentRadioSelection = -1;
    public static String currentSubjectSelection = null;
    public static ArrayList<String> curatedContentSubjects;


    public static ArrayList<CuratedContentItem> curatedContentList = new ArrayList<>();
    public static ArrayList<CuratedContentItem> filteredCuratedContentList;
    public static ViewDataBinding curatedContentBinding;
    public static ViewDataBinding curatedContentSingleBinding;
    public static CuratedContentAdapter curatedContentAdapter;

    public static CuratedContentAdapter curatedContentAdapterSearch;

    private static FavouritesAdapter urlFavouritesManager;
    private static FavouritesAdapter videoFavouritesManager;
    private static LeadMeMain main;

    public static View curatedContentScreen;
    public static View curatedContentScreenSingle;
    public static boolean hasDoneSetup = false;

    public static void showCuratedContentSingle (LeadMeMain main, CuratedContentItem curatedContentItem, View listItem) {
        CuratedContentManager.curatedContentSingleBinding = DataBindingUtil.bind(CuratedContentManager.curatedContentScreenSingle);
        CuratedContentManager.curatedContentSingleBinding.setLifecycleOwner(main);
        CuratedContentManager.curatedContentSingleBinding.setVariable(BR.curatedContentItem, curatedContentItem);
        ImageView imageView = curatedContentScreenSingle.findViewById(R.id.img_view);
        if (curatedContentItem.img_url != null) {
            UrlImageViewHelper.setUrlDrawable(imageView, curatedContentItem.img_url);
        }
        Button selectItem = curatedContentScreenSingle.findViewById(R.id.select_item);
        selectItem.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                WebManager webManager = new WebManager(main);
                webManager.showPreview(curatedContentItem.link);
              }
          }
        );

        View.OnClickListener back = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main.showCuratedContentScreen();
            }
        };

        Button backButton = curatedContentScreenSingle.findViewById(R.id.close_curated_content_single);
        backButton.setOnClickListener(back);

        ImageView curatedContentTypeIcon = curatedContentScreenSingle.findViewById(R.id.curated_content_type_icon);
        switch (curatedContentItem.type) {
            case WITHIN:
                curatedContentTypeIcon.setBackground(ResourcesCompat.getDrawable(main.getResources(), R.drawable.search_within, null));
                break;
            case YOUTUBE:
                curatedContentTypeIcon.setBackground(ResourcesCompat.getDrawable(main.getResources(), R.drawable.core_yt_icon, null));
                break;
            case LINK:
                curatedContentTypeIcon.setBackground(ResourcesCompat.getDrawable(main.getResources(), R.drawable.task_website_icon, null));
                break;
        }

        CheckBox checkBox = curatedContentScreenSingle.findViewById(R.id.fav_checkbox_curated_content);
        checkBox.setChecked(isInFavourites(curatedContentItem.link, curatedContentItem.type));
        checkBox.setOnCheckedChangeListener((buttonView, checked) -> {
            CheckBox fav = listItem.findViewById(R.id.fav_checkbox_curated_content);
            fav.setChecked(checked);
        });

        main.showCuratedContentSingleScreen();
    }

    public static void setupCuratedContent (LeadMeMain main) {
        if (CuratedContentManager.hasDoneSetup) {
            return;
        }
        // set up data binding and the list view for curated content
        CuratedContentManager.curatedContentBinding = DataBindingUtil.bind(CuratedContentManager.curatedContentScreen);
        CuratedContentManager.curatedContentBinding.setLifecycleOwner(main);
        CuratedContentManager.curatedContentBinding.setVariable(BR.curatedContentList, CuratedContentManager.filteredCuratedContentList);
        ListView curatedContentListView = CuratedContentManager.curatedContentScreen.findViewById(R.id.curated_content_list);
        CuratedContentManager.curatedContentAdapter = new CuratedContentAdapter(main, curatedContentScreen.findViewById(R.id.curated_content_list));
        curatedContentListView.setAdapter(CuratedContentManager.curatedContentAdapter);
        CuratedContentManager.curatedContentAdapter.curatedContentList = CuratedContentManager.filteredCuratedContentList;
        CuratedContentManager.curatedContentAdapter.notifyDataSetChanged();

        // handle clicking on a curated content item, if this starts to get more complicated we'll want to split it out
        curatedContentListView.setItemsCanFocus(false);
        curatedContentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CuratedContentItem current = CuratedContentManager.filteredCuratedContentList.get(i);
                CuratedContentManager.showCuratedContentSingle(main, current, view);
            }
        });

        curatedContentScreen.findViewById(R.id.filter_button).setOnClickListener(view -> {
            CuratedContentManager.showFilters(main);
        });
        curatedContentScreen.findViewById(R.id.search_button).setOnClickListener(view -> {
            CuratedContentManager.showSearch(main);
        });
        CuratedContentManager.hasDoneSetup = true;
    }

    public static void addToFavourites(String link, String title, CuratedContentType type, Boolean checked) {
        if (checked) {
            if (type == CuratedContentType.LINK) {
                CuratedContentManager.urlFavouritesManager.addToFavourites(link, title);
                CuratedContentManager.urlFavouritesManager.refreshPreview(link);
            } else {
                CuratedContentManager.videoFavouritesManager.addToFavourites(link, title);
                CuratedContentManager.urlFavouritesManager.refreshPreview(link);
            }
        } else {
            if (type == CuratedContentType.LINK) {
                CuratedContentManager.urlFavouritesManager.deleteFromFavourites(link);
            } else {
                CuratedContentManager.videoFavouritesManager.deleteFromFavourites(link);
            }
        }
    }

    public static boolean isInFavourites(String link, CuratedContentType type) {
        if (type == CuratedContentType.LINK) {
            return CuratedContentManager.urlFavouritesManager.isInFavourites(link);
        } else {
            return CuratedContentManager.videoFavouritesManager.isInFavourites(link);
        }
    }

    private static void initializeCuratedContent(ArrayList<CuratedContentItem> curatedContentList) {
        CuratedContentManager.curatedContentList = (ArrayList<CuratedContentItem>) curatedContentList.clone();
        CuratedContentManager.filteredCuratedContentList = (ArrayList<CuratedContentItem>) curatedContentList.clone();

        Set<String> curatedContentSubjectsSet = new TreeSet<>();
        for (CuratedContentItem curatedContent:curatedContentList) {
            if (!curatedContent.subject.isEmpty()) {
                String[] subjects = curatedContent.subject.trim().split(",");
                for(int i = 0; i < subjects.length; i++) {
                    subjects[i] = subjects[i].trim();
                }
                curatedContentSubjectsSet.addAll(Arrays.asList(subjects));
            }
        }
        ArrayList<String> ccSubjects = new ArrayList<>(curatedContentSubjectsSet);
        ccSubjects.add(0, "Please select");
        CuratedContentManager.curatedContentSubjects = ccSubjects;

        CuratedContentManager.urlFavouritesManager = Controller.getInstance().getFavouritesManager().getUrlFavouritesAdapter();
        CuratedContentManager.videoFavouritesManager = Controller.getInstance().getFavouritesManager().getYouTubeFavouritesAdapter();
        CuratedContentManager.main = LeadMeMain.getInstance();
        if (CuratedContentManager.curatedContentAdapter != null) {
            LeadMeMain.runOnUI(() -> {
                CuratedContentManager.curatedContentAdapter.curatedContentList = CuratedContentManager.filteredCuratedContentList;
                CuratedContentManager.curatedContentAdapter.notifyDataSetChanged();
            });
        }
    }

    private static void showFilters (LeadMeMain main) {
        final BottomSheetDialog filterSheetDialog = new BottomSheetDialog(main, R.style.BottomSheetDialogTransparentBackground);
        filterSheetDialog.setContentView(R.layout.filter_sheet_layout);
        LinearLayout filterInfo = curatedContentScreen.findViewById(R.id.no_results_info);
        // set up the years slider
        RangeSlider yearsSlider = filterSheetDialog.findViewById(R.id.years_slider);
        yearsSlider.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                if (value == 0.0) {
                    return "R";
                }
                return String.valueOf(Math.round(value));
            }
        });

        // build subject selection spinner
        Spinner subjects = filterSheetDialog.findViewById(R.id.subjects);
        String[] ccSubjects = CuratedContentManager.curatedContentSubjects != null ? (String[]) CuratedContentManager.curatedContentSubjects.toArray(new String[0]) : new String[0];
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(main, R.layout.spinner_item, ccSubjects);
        subjects.setAdapter(adapter);

        RadioGroup videoType = filterSheetDialog.findViewById(R.id.video_type_radio);

        // store existing filters for when we reopen the filters
        if (CuratedContentManager.currentYearSelection != null) {
            yearsSlider.setValues(CuratedContentManager.currentYearSelection);
        }
        if (CuratedContentManager.currentRadioSelection > -1) {
            RadioButton selectedType = filterSheetDialog.findViewById(CuratedContentManager.currentRadioSelection);
            selectedType.setChecked(true);
        }
        if (CuratedContentManager.currentSubjectSelection != null) {
            subjects.setSelection(adapter.getPosition(CuratedContentManager.currentSubjectSelection));
        }

        // handle apply button
        Button applyFilters = filterSheetDialog.findViewById(R.id.apply_filters);
        applyFilters.setOnClickListener(button -> {
            RadioButton selectedType = filterSheetDialog.findViewById(videoType.getCheckedRadioButtonId());
            String radioSelection = selectedType != null ? (String) selectedType.getText() : null;
            String subjectSelection = (String) subjects.getSelectedItem();
            List<Float> yearsSelections = yearsSlider.getValues();

            CuratedContentManager.currentYearSelection = yearsSelections;
            CuratedContentManager.currentRadioSelection = videoType.getCheckedRadioButtonId();
            CuratedContentManager.currentSubjectSelection = subjectSelection;

            CuratedContentManager.filteredCuratedContentList = (ArrayList<CuratedContentItem>) CuratedContentManager.curatedContentList.clone();
            if (!subjectSelection.equals("Please select")) {
                CuratedContentManager.filterCuratedContentBySubject(CuratedContentManager.filteredCuratedContentList, subjectSelection);
            }
            if (radioSelection != null) {
                switch (radioSelection) {
                    case "Youtube":
                        CuratedContentManager.filterCuratedContentByType(CuratedContentManager.filteredCuratedContentList, CuratedContentType.YOUTUBE);
                        break;
                    case "Within":
                        CuratedContentManager.filterCuratedContentByType(CuratedContentManager.filteredCuratedContentList, CuratedContentType.WITHIN);
                        break;
                    case "Website":
                        CuratedContentManager.filterCuratedContentByType(CuratedContentManager.filteredCuratedContentList, CuratedContentType.LINK);
                        break;
                }
            }
            CuratedContentManager.filterCuratedContentByYear(CuratedContentManager.filteredCuratedContentList, Math.round(yearsSelections.get(0)), Math.round(yearsSelections.get(1)));
            CuratedContentManager.curatedContentBinding.setVariable(BR.curatedContentList, CuratedContentManager.filteredCuratedContentList);
            CuratedContentManager.curatedContentAdapter.curatedContentList = CuratedContentManager.filteredCuratedContentList;
            CuratedContentManager.curatedContentAdapter.notifyDataSetChanged();
            TextView filterHeading = curatedContentScreen.findViewById(R.id.filter_heading);
            TextView filterSubheading = curatedContentScreen.findViewById(R.id.filter_subheading);
            //display of results
            if (CuratedContentManager.filteredCuratedContentList.size() > 0) {
                filterInfo.setVisibility(View.GONE);
                curatedContentScreen.findViewById(R.id.curated_content_list);
            }
            //no results found
            else {
               if (curatedContentList.size() > 0) {
                   filterInfo.setVisibility(View.VISIBLE);
                   filterHeading.setText("No Results Found!");
                   filterSubheading.setText("Sorry, that filter combination has no results. Please try different criteria.");
               }
                else {
                    filterInfo.setVisibility(View.GONE);
                    filterHeading.setText("What are you searching for?");
                    filterSubheading.setText("Search through our curated content for classrooms!");
               }
            }

            filterSheetDialog.hide();
        });

        // handle reset button
        Button resetFilters = filterSheetDialog.findViewById(R.id.reset_filters);
        resetFilters.setOnClickListener(button -> {
            CuratedContentManager.filteredCuratedContentList = (ArrayList<CuratedContentItem>) CuratedContentManager.curatedContentList.clone();
            curatedContentBinding.setVariable(BR.curatedContentList, CuratedContentManager.filteredCuratedContentList);
            curatedContentAdapter.curatedContentList = CuratedContentManager.filteredCuratedContentList;
            curatedContentAdapter.notifyDataSetChanged();
            CuratedContentManager.currentYearSelection = null;
            CuratedContentManager.currentRadioSelection = -1;
            CuratedContentManager.currentSubjectSelection = null;
            filterSheetDialog.hide();
            filterInfo.setVisibility(View.GONE);

        });

        filterSheetDialog.show();
    }

    private static void showSearch (LeadMeMain main) {
        final BottomSheetDialog searchSheetDialog = new BottomSheetDialog(main, R.style.BottomSheetDialogTransparentBackground);
        searchSheetDialog.setContentView(R.layout.search_sheet_layout);
        EditText searchInput = searchSheetDialog.findViewById(R.id.search_input);
        TextView searchHeading = searchSheetDialog.findViewById(R.id.search_heading);
        TextView searchSubheading = searchSheetDialog.findViewById(R.id.search_subheading);
        ImageButton searchClear = searchSheetDialog.findViewById(R.id.search_clear);
        LinearLayout searchInfo = searchSheetDialog.findViewById(R.id.search_info);
        ListView curatedContentListSearch = searchSheetDialog.findViewById(R.id.curated_content_list);
        searchClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchInput.setText("");
                searchInfo.setVisibility(View.VISIBLE);
                searchHeading.setText("What are you searching for?");
                searchSubheading.setText("Search through our curated content for classrooms!");
                curatedContentListSearch.setVisibility(View.GONE);
            };

        });

        CuratedContentManager.curatedContentAdapterSearch = new CuratedContentAdapter(main, searchSheetDialog.findViewById(R.id.curated_content_list));

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    ArrayList<CuratedContentItem> curatedContent = (ArrayList<CuratedContentItem>) curatedContentList.clone();
                    CuratedContentManager.filterCuratedContentBySearch(curatedContent, searchInput.getText().toString().toLowerCase(Locale.ROOT));
                    curatedContentListSearch.setAdapter(curatedContentAdapterSearch);
                    curatedContentAdapterSearch.curatedContentList = curatedContent;
                    curatedContentAdapterSearch.notifyDataSetChanged();
                    if (textView.getText().length() > 0 && curatedContent.size() > 0) {
                        searchInfo.setVisibility(View.GONE);
                        searchHeading.setText("What are you searching for?");
                        searchSubheading.setText("Search through our curated content for classrooms!");
                        curatedContentListSearch.setVisibility(View.VISIBLE);
                    }

                    else if (textView.getText().length() > 0 && curatedContent.size() == 0){
                        searchInfo.setVisibility(View.VISIBLE);
                        searchHeading.setText("No Results Found!");
                        searchSubheading.setText("No results for " + textView.getText() + " found in curated content");
                        curatedContentListSearch.setVisibility(View.VISIBLE);
                    }

                    else if (textView.getText().length() == 0){
                        searchInfo.setVisibility(View.VISIBLE);
                        searchHeading.setText("What are you searching for?");
                        searchSubheading.setText("Search through our curated content for classrooms!");
                        curatedContentListSearch.setVisibility(View.GONE);
                    }

                    else{
                        curatedContentListSearch.setVisibility(View.VISIBLE);
                    }
                    // handle clicking on a curated content item, if this starts to get more complicated we'll want to split it out
                    curatedContentListSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            CuratedContentItem current = curatedContent.get(i);
                            CuratedContentManager.showCuratedContentSingle(main, current, view);
                            searchSheetDialog.hide();
                        }
                    });
                }
                return false;
            }
        });
        searchSheetDialog.show();
    }

    private static String makeHttpRequest(String url) throws IOException {
        final OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int responseCode = response.code();
            if (responseCode > 200) {
                FirebaseCrashlytics.getInstance().log("Response code for curated content was greater than 200. Code: " + String.valueOf(responseCode));
                return null;
            } else {
                return response.body().string();
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }

    public static void filterCuratedContentBySearch (ArrayList<CuratedContentItem> curatedContent, String search) {
        curatedContent.removeIf(curatedContentItem -> !(curatedContentItem.title.toLowerCase(Locale.ROOT).contains(search.trim()) || curatedContentItem.topics.toLowerCase(Locale.ROOT).contains(search.trim())));
    }

    public static void filterCuratedContentByYear (ArrayList<CuratedContentItem> curatedContent, int lower, int upper) {
        curatedContent.removeIf(curatedContentItem -> {
            String[] ccYears = curatedContentItem.years.split(",");
            for (int i = 0; i < ccYears.length; i++) {
                int currentValue = ccYears[i].equals("R") ? 0 : Integer.parseInt(ccYears[i].trim());
                if (currentValue >= lower && currentValue <= upper) {
                    return false;
                }
            }
            return true;
        });
    }

    public static void filterCuratedContentByType (ArrayList<CuratedContentItem> curatedContent, CuratedContentType type) {
        curatedContent.removeIf(curatedContentItem -> !curatedContentItem.type.equals(type));
    }

    public static void filterCuratedContentBySubject (ArrayList<CuratedContentItem> curatedContent, String subject) {
        curatedContent.removeIf(curatedContentItem -> !curatedContentItem.subject.contains(subject));
    }

    public static void getCuratedContent(LeadMeMain main) {
        String curatedContentAPIKey = "AIzaSyD1ey4a7bo1xqEwhSDy0UQ4bnCEd12srP4"; // API key is public anyway and locked down to sheets API and this app only
        String curatedContentSpreadsheetId = "1qAmBZyXIHGaRIR1ZdV4r0JC7lRhYOXuAxS23mVvzqgg";
        String url = String.format("https://sheets.googleapis.com/v4/spreadsheets/%s/values/A1:H100?key=%s", curatedContentSpreadsheetId, curatedContentAPIKey);

        new Thread(new Runnable() {
            public void run() {
                String response = null;
                try {
                    response = makeHttpRequest(url);
                } catch (IOException e) {
                    e.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                if (response != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray curatedContent = jsonObject.getJSONArray("values");
                        JSONArray curatedContentHeadings = curatedContent.getJSONArray(0);
                        String[] expectedHeadings = {
                                "Title",
                                "Description",
                                "Type",
                                "Link",
                                "Years",
                                "Subjects",
                                "Topics",
                                "Live"
                        };
                        for (int i = 0; i < expectedHeadings.length; i++) {
                            if (!curatedContentHeadings.getString(i).equals(expectedHeadings[i])) {
                                return;
                            }
                        }
                        ArrayList<CuratedContentItem> processedCuratedContent = new ArrayList<CuratedContentItem>();
                        for(int i = 1; i < curatedContent.length(); i++) {
                            JSONArray curatedContentJson = curatedContent.getJSONArray(i);
                            if (!validateCuratedContentRow(curatedContentJson)) {
                                continue;
                            }
                            processedCuratedContent.add(new CuratedContentItem(
                                    i,
                                    curatedContentJson.getString(0),
                                    CuratedContentType.valueOf(curatedContentJson.getString(2)),
                                    curatedContentJson.getString(3),
                                    curatedContentJson.getString(1),
                                    curatedContentJson.getString(4),
                                    curatedContentJson.getString(5),
                                    curatedContentJson.getString(6)
                            ));
                            getPreviewImages(main, curatedContentJson.getString(3));
                        }
                        CuratedContentManager.initializeCuratedContent(processedCuratedContent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }
            }
        }).start();
    }

    private static boolean validateCuratedContentRow (JSONArray curatedContentJson) throws JSONException {
        try {
            if (curatedContentJson.length() < 8) {
                return false;
            }
            for (int i = 0; i <= 7; i++) {
                if (curatedContentJson.getString(i).equals("") || curatedContentJson.getString(i) == null) {
                    return false;
                }
            }
            // check if live
            if (!curatedContentJson.getString(7).equals("YES")) {
                return false;
            }

            // validate that it is a supported CuratedContentType
            try {
                CuratedContentType.valueOf(curatedContentJson.getString(2));
            } catch (IllegalArgumentException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                return false;
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }
    }

    private static LinkPreviewCallback previewImageCallback = new LinkPreviewCallback() {
        @Override
        public void onPre() {

        }

        @Override
        public void onPos(SourceContent sourceContent, boolean isNull) {
            String img = "";
            if (!sourceContent.getImages().isEmpty()) {
                img = sourceContent.getImages().get(0);
            }
            if (img != null) {
                String finalImg = img;
                CuratedContentManager.curatedContentList.forEach(curatedContentItem -> {
                    if (curatedContentItem.link.equals(sourceContent.getUrl())) {
                        curatedContentItem.img_url = finalImg;
                    }
                });
                String finalImg1 = img;
                CuratedContentManager.filteredCuratedContentList.forEach(curatedContentItem -> {
                    if (curatedContentItem.link.equals(sourceContent.getUrl())) {
                        curatedContentItem.img_url = finalImg1;
                        if (CuratedContentManager.curatedContentAdapter != null) {
                            CuratedContentManager.curatedContentAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }
    };

    public static void getPreviewImages (LeadMeMain main, String url) {
        TextCrawler textCrawler = new TextCrawler(Controller.getInstance().getWebManager());
        textCrawler.makePreview(previewImageCallback, url);
    }
}
