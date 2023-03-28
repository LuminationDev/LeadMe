package com.lumination.leadme.accessibility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LumiAccessibilityReceiver extends BroadcastReceiver {

    private static final String TAG = "LumiAccessReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(LumiAccessibilityConnector.PROPAGATE_ACTION);
        i.putExtras(intent.getExtras());
        context.sendBroadcast(i);
    }
}
