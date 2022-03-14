package com.lumination.leadme;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CuratedContentManager {
    private final String TAG = "CuratedContentManager";

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
                                        curatedContentJson.getString(0),
                                        CuratedContentType.valueOf(curatedContentJson.getString(2)),
                                        curatedContentJson.getString(3),
                                        curatedContentJson.getString(1)
                                ));
                            }
                        }
                        main.initializeCuratedContent(processedCuratedContent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }
            }
        }).start();
    }
}
