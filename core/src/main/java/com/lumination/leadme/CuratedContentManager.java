package com.lumination.leadme;

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
            return response.body().string();
        } catch (Exception e) {
            return "heck2";
        }
    }

    public static void getCuratedContent(LeadMeMain main) {
//        todo - tidy up this method, move the url to be a bit cleaner and store the api key somewhere 11/03/22
//        todo - we don't need to worry too much about the api key, as it'll be locked down to just sheets and this android app 11/03/22
        String url = "https://sheets.googleapis.com/v4/spreadsheets/1rcQF2vmFQW5LmMPBXSu9qZ5N3fuqXFXjvHlDq-Qhm1Y/values/A1:G100?key=AIzaSyDQTgiS6UZ0BCZkpYnevn8QgBi7BVzUOvk";

        // todo handle 403s etc 11/03/22
        new Thread(new Runnable() {
            public void run() {
                String response = null;
                try {
                    response = makeHttpRequest(url);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray curatedContent = jsonObject.getJSONArray("values");
                    // todo validate the top row has the correct headings 11/03/22
                    ArrayList<CuratedContentItem> processedCuratedContent = new ArrayList<CuratedContentItem>();
                    for(int i = 1; i < curatedContent.length(); i++) {
                        JSONArray curatedContentJson = curatedContent.getJSONArray(i);
                        processedCuratedContent.add(new CuratedContentItem(
                                curatedContentJson.getString(0),
                                CuratedContentType.valueOf(curatedContentJson.getString(2)),
                                curatedContentJson.getString(3),
                                curatedContentJson.getString(1)
                        ));
                    }
                    main.initializeCuratedContent(processedCuratedContent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
