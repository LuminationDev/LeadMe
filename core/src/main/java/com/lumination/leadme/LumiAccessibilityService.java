package com.lumination.leadme;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

public class LumiAccessibilityService extends android.accessibilityservice.AccessibilityService {

    private final static String TAG = "LumiAccessService";

    protected final static String BROADCAST_ACTION = "com.lumination.leadme.BROADCAST_ACTION";
    protected final static String INFO_TAG = "LumiBroadcastInfo";
    protected final static String INFO_CONFIG = "CONFIG_CHANGE";
    protected final static String INFO_CONNECTED = "SERVICE_CONNECTED";
    protected final static String EVENT_RECEIVED = "ACCESS_EVENT";
    protected final static String EVENT_OBJ = "EVENT_OBJ";
    protected final static String EVENT_ROOT = "EVENT_ROOT";
    protected static final String REFRESH_ACTION = "REFRESH_ACTION";

    private String leadmePackageName = "";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Accessibility Service CREATED!");
        leadmePackageName = getPackageName();
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Interrupted!");
        ///disableSelf(); //close down the accessibility service
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroyed!");
        super.onDestroy();
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        Log.w(TAG, "System service: " + name);
        return super.getSystemService(name);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.w(TAG, "Unbind! " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.w(TAG, "And rebind! " + intent);
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ClearFromRecentService", "Service Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("ClearFromRecentService", "END");
        //Code here
        stopSelf();
    }

    @Override
    protected void onServiceConnected() {
        Log.d(TAG, "Accessibility Service CONNECTED!");
        sendInfoBroadcast(LumiAccessibilityService.INFO_CONNECTED);
        LeadMeMain.setLumiAccessibilityService(this);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        sendInfoBroadcast(LumiAccessibilityService.INFO_CONFIG);
    }


    public void sendInfoBroadcast(String tagInfo) {
        //Log.w(TAG, "Sending INFO broadcast");
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        //intent.setComponent(new ComponentName(getPackageName(), LumiAccessibilityReceiver.class.getName()));
        intent.setComponent(new ComponentName(getPackageName(), LumiAccessibilityReceiver.class.getName()));
        Bundle data = new Bundle();
        //data.putString(LumiAccessibilityService.INFO_TAG, tagInfo);
        data.putString(INFO_TAG, tagInfo);
        intent.putExtras(data);
        sendBroadcast(intent);
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
//        AccessibilityNodeInfo ani = event.getSource();
//        Log.w(TAG, "A1] " + ani + " // " + getRootInActiveWindow());
//        if (ani != null) {
//            ani.refresh(); // to fix issue with viewIdResName = null on Android 6+
//        }
//        Log.w(TAG, "A2] " + ani + " // " + getRootInActiveWindow());
//
//        AccessibilityNodeInfo source = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
//        Log.w(TAG, "A3] " + source + " // " + getRootInActiveWindow());

        if (getApplicationContext() == null) {
            //app is not currently running, can't do anything right now
            return;
        }

        AccessibilityNodeInfo rootInActiveWindow = null;

        //we're currently in LeadMe, we don't need to broadcast
        if (event.getPackageName() == null || event.getPackageName().equals(leadmePackageName)) {
            //This happens in v10 due to new 'TYPE_WINDOWS_CHANGED' event with no package
            //Log.d(TAG, "ERROR! Can't do anything with this event.");
            return;
        }

        //we never need this for System UI calls - save the effort for 3rd party apps
        if (!event.getPackageName().equals("com.android.systemui")) {
            rootInActiveWindow = getRootInActiveWindow();
        }

        if (rootInActiveWindow == null) {
            rootInActiveWindow = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        }

        if (rootInActiveWindow == null) {
            rootInActiveWindow = event.getSource();
        }

        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        //intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        //intent.setComponent(new ComponentName("com.lumination.leadme", "com.lumination.leadme.LumiAccessibilityReceiver"));
        intent.setComponent(new ComponentName(getPackageName(), LumiAccessibilityReceiver.class.getName()));
        Bundle data = new Bundle();
        data.putString(INFO_TAG, LumiAccessibilityService.EVENT_RECEIVED);
        data.putParcelable(EVENT_OBJ, event);
        data.putParcelable(EVENT_ROOT, rootInActiveWindow);
        intent.putExtras(data);

        try {
            //Log.w(TAG, "BROADCASTING: " + event+" // "+rootInActiveWindow);
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to broadcast from accessibility: " + event + ", " + getRootInActiveWindow());
            e.printStackTrace();
        }
    }
    public void updateOnBoard(LeadMeMain main){
        main.setStudentOnBoard(1);
    }

}
