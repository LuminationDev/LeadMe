package com.lumination.leadme;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public static ArrayList<CuratedContentItem> curatedContentList;
    public static ArrayList<CuratedContentItem> filteredCuratedContentList;
    public static ViewDataBinding curatedContentBinding;
    public static CuratedContentAdapter curatedContentAdapter;

    public static View curatedContentScreen;

    public static void setupCuratedContent (LeadMeMain main) {
        // set up data binding and the list view for curated content
        CuratedContentManager.curatedContentBinding = DataBindingUtil.bind(CuratedContentManager.curatedContentScreen);
        CuratedContentManager.curatedContentBinding.setLifecycleOwner(main);
        CuratedContentManager.curatedContentBinding.setVariable(BR.curatedContentList, CuratedContentManager.filteredCuratedContentList);
        ListView curatedContentListView = CuratedContentManager.curatedContentScreen.findViewById(R.id.curated_content_list);
        CuratedContentManager.curatedContentAdapter = new CuratedContentAdapter(main, curatedContentScreen.findViewById(R.id.curated_content_list));
        curatedContentListView.setAdapter(CuratedContentManager.curatedContentAdapter);
        CuratedContentManager.curatedContentAdapter.curatedContentList = CuratedContentManager.filteredCuratedContentList;

        // handle clicking on a curated content item, if this starts to get more complicated we'll want to split it out
        curatedContentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CuratedContentItem current = CuratedContentManager.filteredCuratedContentList.get(i);
                WebManager webManager = new WebManager(main);
                webManager.showPreview(current.link);
            }
        });

        curatedContentScreen.findViewById(R.id.filter_button).setOnClickListener(view -> {
            CuratedContentManager.showFilters(main);
        });
    }

    private static void initializeCuratedContent(ArrayList<CuratedContentItem> curatedContentList) {
        CuratedContentManager.curatedContentList = (ArrayList<CuratedContentItem>) curatedContentList.clone();
        CuratedContentManager.filteredCuratedContentList = (ArrayList<CuratedContentItem>) curatedContentList.clone();
        CuratedContentManager.curatedContentBinding.setVariable(BR.curatedContentList, CuratedContentManager.filteredCuratedContentList);
        CuratedContentManager.curatedContentAdapter.curatedContentList = CuratedContentManager.filteredCuratedContentList;

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
    }

    private static void showFilters (LeadMeMain main) {
        final BottomSheetDialog filterSheetDialog = new BottomSheetDialog(main, R.style.BottomSheetDialogTransparentBackground);
        filterSheetDialog.setContentView(R.layout.filter_sheet_layout);

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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(main, android.R.layout.simple_spinner_item, (String[]) CuratedContentManager.curatedContentSubjects.toArray(new String[0]));
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
            curatedContentBinding.setVariable(BR.curatedContentList, CuratedContentManager.filteredCuratedContentList);
            curatedContentAdapter.curatedContentList = CuratedContentManager.filteredCuratedContentList;
            curatedContentAdapter.notifyDataSetChanged();
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
        });

        filterSheetDialog.show();
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

    public static void filterCuratedContentByYear (ArrayList<CuratedContentItem> curatedContent, int lower, int upper) {
        curatedContent.removeIf(curatedContentItem -> {
            String[] ccYears = curatedContentItem.years.split(",");
            for (int i = 0; i < ccYears.length; i++) {
                int currentValue = ccYears[i].equals("R") ? 0 : Integer.parseInt(ccYears[i]);
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
        String curatedContentAPIKey = "AIzaSyDQTgiS6UZ0BCZkpYnevn8QgBi7BVzUOvk"; // API key is public anyway and locked down to sheets API and this app only
        String curatedContentSpreadsheetId = "1rcQF2vmFQW5LmMPBXSu9qZ5N3fuqXFXjvHlDq-Qhm1Y";
        String url = String.format("https://sheets.googleapis.com/v4/spreadsheets/%s/values/A1:G100?key=%s", curatedContentSpreadsheetId, curatedContentAPIKey);

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

                            // validate that it is a supported CuratedContentType
                            try {
                                CuratedContentType.valueOf(curatedContentJson.getString(2));
                            } catch (IllegalArgumentException e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                                continue;
                            }

                            // only include if it is marked as 'live'
                            if (curatedContentJson.getString(6).equals("YES")) {
                                processedCuratedContent.add(new CuratedContentItem(
                                        i,
                                        curatedContentJson.getString(0),
                                        CuratedContentType.valueOf(curatedContentJson.getString(2)),
                                        curatedContentJson.getString(3),
                                        curatedContentJson.getString(1),
                                        curatedContentJson.getString(4),
                                        curatedContentJson.getString(5)
                                ));
                            }
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
}
