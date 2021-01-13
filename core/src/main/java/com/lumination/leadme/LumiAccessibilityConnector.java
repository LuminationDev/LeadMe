package com.lumination.leadme;

import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class LumiAccessibilityConnector {

    private static final String TAG = "LumiAccessConnector";

    public static final String PROPAGATE_ACTION = "com.lumination.leadme.PROPAGATED_ACTION";

    private LeadMeMain main;
    private String lastAppName, lastPackageName;

    //handler for executing on the main thread
    //private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DispatchManager dispatcher;

    public LumiAccessibilityConnector(LeadMeMain main) {
        this.main = main;
        dispatcher = main.getDispatcher();
    }


    public void prepareToInstall(String packageName, String appName) {
        lastAppName = appName;
        lastPackageName = packageName;
        Log.d(TAG, "PREPARING TO INSTALL " + lastAppName);
    }

    //returns true if it has finished searching, NOT necessarily that it found the button
    boolean debugVRClick = true;
    boolean autoEnterVR = true;
    boolean inFindAndClick = false;
    int vrClickAttempts = 0;
    AccessibilityNodeInfo originalNodeInfo;

    private static boolean waitingForStateChange = false;

    private static String[] keyYouTubePhrases = {
            "Enter virtual reality mode",
            "Continue",
            "Dismiss",
            "Skip Trial",
            "No Thanks",
            "NO THANKS"
    };

    private void manageYouTubeAccess(AccessibilityNodeInfo rootInActiveWindow) {
        ArrayList<AccessibilityNodeInfo> foundNodes = new ArrayList<>();

        //find nodes matching all desired phrases
        for (String phrase : keyYouTubePhrases) {
            foundNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText(phrase));
        }

        //click all the buttons!
        for (AccessibilityNodeInfo accessibilityNodeInfo : foundNodes) {
            accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.w(TAG, "Clicked " + accessibilityNodeInfo.getViewIdResourceName() + ", " + accessibilityNodeInfo.getText() + ", " + accessibilityNodeInfo.getContentDescription());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void bringMainToFront() {
        main.getHandler().post(() -> {
            if (main != null && !main.appHasFocus) {
                main.recallToLeadMe();
            }
        });
    }


    public boolean manageAccessibilityEvent(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {
        if (main == null) {
            return false;
        }

        //after this point is for client-specific behaviours only
        if (main.isGuide || !main.getNearbyManager().isConnectedAsFollower()) {
            return false;
        }

        Log.w(TAG, "Managing received AccessibilityEvent: " + event);

        if (!main.appHasFocus && main.getWebManager().launchingVR && event.getSource() != null && event.getSource().getPackageName().toString().contains("youtube")) {
            manageYouTubeAccess(rootInActiveWindow);

        } else if (main.appHasFocus && dispatcher.launchAppOnFocus == null && event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED && event.getPackageName().toString().equals("com.android.systemui")) {
            //likely pulled down notifications while in main app
            Log.i(TAG, "User VIEWED status bar in LeadMe! " + event.toString());
            //main.recallToLeadMe();
            main.collapseStatus();

        } else if (main.appHasFocus && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName().toString().equals("com.lumination.leadme")) {
            Log.i(TAG, "User RETURNED TO LEADME! [" + main.appHasFocus + "] " + event.toString());
            //don't need to do anything really, perhaps alert guide?
            waitingForStateChange = false; //reset

            //update learner UI and let leader know
            //main.updateFollowerCurrentTask(main.leadMePackageName, main.leadMeAppName, "Application", "");
            if (!main.getDispatcher().hasDelayedLaunchContent()) {
                Log.i(TAG, "No delayed content!");
                dispatcher.sendActionToSelected(LeadMeMain.ACTION_TAG,
                        LeadMeMain.LAUNCH_SUCCESS + main.leadMeAppName + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName, main.getNearbyManager().getAllPeerIDs());
            }


        } else if ((waitingForStateChange && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) ||

                //works for Redmi Note 7
                (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName().toString().equals("com.android.launcher3")) ||

                //works for MI 8 SE
                (event.getPackageName().toString().equals("com.android.systemui") && event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED &&
                        (event.getContentDescription()) != null && (event.getContentDescription().equals("Overview") || event.getContentDescription().equals("Home")))) {
            //either pulled down or clicked notification, setting or nav while in launched 3rd party app
            Log.i(TAG, "User clicked HOME or OVERVIEW! " + event.toString());
            if (!main.studentLockOn) {
                Log.i(TAG, "It's OK, user is in free play mode");
                return true;
            }
            waitingForStateChange = false;
            if (!main.appHasFocus && main.studentLockOn) {//!main.getAppLaunchAdapter().lastApp.equals(packageName)) {
                dispatcher.launchAppOnFocus = new String[2];
                dispatcher.launchAppOnFocus[0] = main.currentTaskPackageName;
                dispatcher.launchAppOnFocus[1] = main.currentTaskName;
                Log.d(TAG, "NEED FOCUS! " + dispatcher.launchAppOnFocus);
                bringMainToFront();

            } else if (main.studentLockOn) {
                Log.d(TAG, "HAVE FOCUS!");
                dispatcher.launchAppOnFocus = null; //reset
                main.getAppManager().relaunchLast();

            }
        } else if (event.getPackageName().toString().equals("com.android.systemui")
                && event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED
                && event.getContentDescription() == null && event.getContentDescription().equals("Back")) {
            Log.i(TAG, "User clicked BACK! " + event.toString());
            //don't need to do anything unless window state changes
            waitingForStateChange = true;


        } else if (
            //this works for MI 8 SE
                (event.getPackageName().toString().equals("com.android.systemui") && event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) ||
                        //this works for Redmi Note 7
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE))) {
            Log.i(TAG, "User VIEWED status bar or other SYSTEM interface! " + event.toString());
            main.collapseStatus();


        } else if (main.getNearbyManager().isConnectedAsFollower()) {
            //Log.d(TAG, "onAccessibilityEvent " + event + "\n");
        }


        //check if we're trying to install something and respond appropriately
        if ((AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED == event.getEventType()
                || (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType()))
                && (lastAppName != null) && (lastAppName.length() > 0)) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) {
                return false;
            }

            List<AccessibilityNodeInfo> installNodes = nodeInfo.findAccessibilityNodeInfosByText("Install");
            List<AccessibilityNodeInfo> acceptNodes = nodeInfo.findAccessibilityNodeInfosByText("ACCEPT"); //is this case sensitive?
            List<AccessibilityNodeInfo> openNodes = nodeInfo.findAccessibilityNodeInfosByText("Open");

            String needsAccessTxt = "needs access to";
            List<AccessibilityNodeInfo> accessToTextNodes = nodeInfo.findAccessibilityNodeInfosByText(needsAccessTxt);
            List<AccessibilityNodeInfo> textNodes = nodeInfo.findAccessibilityNodeInfosByText(lastAppName);

            AccessibilityNodeInfo acceptButton = null, installerButton = null, openButton = null;
            boolean foundTitle = false, foundNeedsAccess = false;

            //TODO also check for 'Needs access to'
            for (AccessibilityNodeInfo node : acceptNodes) {
                if (node.getText() != null && node.getText().equals("ACCEPT")) {
                    Log.i(TAG, "ACC::onAccessibilityEvent: Accept (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                    acceptButton = node;
                    break;
                }
            }

            for (AccessibilityNodeInfo node : installNodes) {
                if (node.getText() != null && node.getText().equals("Install")) {
                    Log.i(TAG, "ACC::onAccessibilityEvent: Install (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                    installerButton = node;
                    break;
                }
            }

            for (AccessibilityNodeInfo node : openNodes) {
                if (node.getText() != null && node.getText().equals("Open")) {
                    Log.i(TAG, "ACC::onAccessibilityEvent: Open (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                    openButton = node;
                    break;
                }
            }

            for (AccessibilityNodeInfo node : textNodes) {
                if (node.getText() != null && node.getText().equals(lastAppName)) {
                    //found one, that's enough
                    foundTitle = true;
                    Log.i(TAG, "ACC::onAccessibilityEvent: Title (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                    break;
                }
            }

            for (AccessibilityNodeInfo node : accessToTextNodes) {
                if (node.getText() != null && node.getText().equals(needsAccessTxt)) {
                    //found it once, that's enough
                    foundNeedsAccess = true;
                    Log.i(TAG, "ACC::onAccessibilityEvent: Needs Access (" + needsAccessTxt + ") " + node.getPackageName() + " for " + node.getText());
                    break;
                }
            }

            //if we can open it, it's already installed
            if (openButton != null && foundTitle) {
                Log.i(TAG, ">> Now let's launch it! " + openNodes.size() + ", " + openButton + ", " + openButton.isClickable());

                if (openButton.isClickable()) {
                    boolean success = openButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if (!success) {
                        Log.i(TAG, ">> Second try opening it... " + openNodes.size() + ", " + openButton);
                        success = openNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    if (!success) {
                        Log.i(TAG, ">> Third try opening it... " + openNodes.size() + ", " + openButton);
                        main.getAppManager().launchLocalApp(lastPackageName, lastAppName, true);
                    }
                    lastAppName = null; //reset, we're done
                    return true; //all done, can exit
                } else {
                    Log.d(TAG, "Not clickable yet...");
                    return false;
                }
            }

            //otherwise we need to install it
            if (installerButton != null && foundTitle) {
                Log.i(TAG, ">> Let's try to install that app! " + installNodes.size());
                installerButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

            //we might need to accept permissions
            if (acceptButton != null && foundNeedsAccess) {
                Log.i(TAG, ">> Let's accept it's permissions! " + acceptNodes.size());
                acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        return false;
    }

    protected void triageReceivedIntent(Intent intent) {
        //Log.i(TAG, "Triaging the intent! " + intent + ", " + intent.getStringExtra(LumiAccessibilityService.INFO_TAG) + ", have main? " + main + ", " + main.getLifecycle().getCurrentState());

        if (intent.hasExtra(LumiAccessibilityService.INFO_TAG)) {

            switch (intent.getStringExtra(LumiAccessibilityService.INFO_TAG)) {
                case LumiAccessibilityService.INFO_CONFIG:
                    main.runOnUiThread(() -> main.refreshOverlay());
                    break;

                case LumiAccessibilityService.INFO_CONNECTED:
                    main.recallToLeadMe();
                    break;

                case LumiAccessibilityService.EVENT_RECEIVED:
                    AccessibilityEvent evt = intent.getParcelableExtra(LumiAccessibilityService.EVENT_OBJ);
                    AccessibilityNodeInfo root = intent.getParcelableExtra(LumiAccessibilityService.EVENT_ROOT);
                    manageAccessibilityEvent(evt, root);
                    break;
            }

        }
    }
}
