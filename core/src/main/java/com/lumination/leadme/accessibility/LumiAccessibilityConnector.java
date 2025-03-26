package com.lumination.leadme.accessibility;

import android.util.Log;

import com.lumination.leadme.LeadMeMain;

public class LumiAccessibilityConnector {

    private static final String TAG = "LumiAccessConnector";
    private final LeadMeMain main;

    public LumiAccessibilityConnector(LeadMeMain main) {
        Log.d(TAG, "LumiAccessibilityConnector: ");
        this.main = main;
    }


    public void bringMainToFront() {
        Log.d(TAG, "bringMainToFront: ");
        LeadMeMain.UIHandler.post(() -> {
            if (main != null && !main.isAppVisibleInForeground()) {
                main.recallToLeadMe();
            }
        });
    }

}
