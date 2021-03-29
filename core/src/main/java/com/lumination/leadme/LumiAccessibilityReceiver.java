package com.lumination.leadme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LumiAccessibilityReceiver extends BroadcastReceiver {

    private static final String TAG = "LumiAccessReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received event from LumiAccessibilityService! Broadcasting it to LeadMeMain. " + intent.getExtras() + ", " + context);
        Intent i = new Intent(LumiAccessibilityConnector.PROPAGATE_ACTION);
        i.putExtras(intent.getExtras());
        context.sendBroadcast(i);
    }
}
