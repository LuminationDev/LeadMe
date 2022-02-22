package com.lumination.leadme;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LumiAccessibilityConnector {

    private static final String TAG = "LumiAccessConnector";

    public static final String PROPAGATE_ACTION = "com.lumination.leadme.PROPAGATED_ACTION";

    private final LeadMeMain main;

    //handler for executing on the main thread
    private final DispatchManager dispatcher;
    private static boolean waitingForStateChange = false;

    public boolean gestureInProgress = false;

    YouTubeAccessibilityManager ytManager;
    WithinAccessibilityManager withinManager;

    public LumiAccessibilityConnector(LeadMeMain main) {
        Log.d(TAG, "LumiAccessibilityConnector: ");
        this.main = main;
        dispatcher = main.getDispatcher();
        ytManager = new YouTubeAccessibilityManager(main, this);
        withinManager = new WithinAccessibilityManager(main, this);
    }

    public void resetState() {
        ytManager.adFinished = false;
        withinManager.resetTapInit();
//        Log.d(TAG, "resetState: ");
//        ytManager.resetState();
//        withinManager.cleanUpVideo();
    }

    public void cueYouTubeAction(String actionStr) {
        Log.d(TAG, "cueYouTubeAction: ");
        if (main.getNearbyManager().isConnectedAsGuide()) {
            return; //guides manage YT their own way
        }
        //delegate to the right manager
        ytManager.cueYouTubeAction(actionStr);
    }

    /**
     * Print out all the accessibility nodes on a given page, able to quickly test and produce
     * the nodes needed to improve functionality.
     * @param rootWindow An AccessibilityNodeInfo representing the currently active window.
     */
    public void displayAllChildren(AccessibilityNodeInfo rootWindow) {
        List<CharSequence> nodes = getAllChildNodeText(rootWindow);

        for(CharSequence node : nodes) {
            Log.e(TAG, "Node: " + node);
        }
    }

    private List<CharSequence> getAllChildNodeText(AccessibilityNodeInfo infoCompat) {
        List<CharSequence> contents = new ArrayList<>();
        if (infoCompat == null)
            return contents;
        if (infoCompat.getContentDescription() != null) {
            contents.add(infoCompat.getContentDescription().toString().isEmpty() ? "unlabelled" : infoCompat.getContentDescription());
        } else if (infoCompat.getText() != null) {
            contents.add(infoCompat.getText().toString().isEmpty() ? "unlabelled" : infoCompat.getText());
        } else {
            getTextInChildren(infoCompat, contents);
        }
        if (infoCompat.isClickable()) {
            if (infoCompat.getClassName().toString().contains(Button.class.getSimpleName())) {
                if (contents.size() == 0) {
                    contents.add("Unlabelled button");
                } else {
                    contents.add("button");
                }
            }
            contents.add("Double tap to activate");
        }
        return contents;
    }


    private void getTextInChildren(AccessibilityNodeInfo nodeInfoCompat, List<CharSequence> contents) {
        if (nodeInfoCompat == null)
            return;
        if (!nodeInfoCompat.isScrollable()) {
            if (nodeInfoCompat.getContentDescription() != null) {
                contents.add(nodeInfoCompat.getContentDescription());
            } else if (nodeInfoCompat.getText() != null) {
                contents.add(nodeInfoCompat.getText());
            }
            if (nodeInfoCompat.getChildCount() > 0) {
                for (int i = 0; i < nodeInfoCompat.getChildCount(); i++) {
                    if (nodeInfoCompat.getChild(i) != null) {
                        getTextInChildren(nodeInfoCompat.getChild(i), contents);
                    }
                }
            }
        }
    }

    ArrayList<AccessibilityNodeInfo> collectChildren(AccessibilityNodeInfo nodeInfo, String phrase, int level) {
        displayAllChildren(nodeInfo);

        //Log.d(TAG, "collectChildren: ");
        String[] tmp = {phrase};
        return collectChildren(nodeInfo, tmp, level);
    }

    //recursively search for child nodes of the given parent that contain
    //any of the specified phrases in their text or content description
    ArrayList<AccessibilityNodeInfo> collectChildren(AccessibilityNodeInfo nodeInfo, String[] phrases, int level) {
        //Log.d(TAG, "collectChildren: ");
        //Log.e(TAG, "SEEKING: " + Arrays.toString(phrases));
        Set<AccessibilityNodeInfo> infoArrayList = new HashSet<>();
        if (nodeInfo == null) {
            return new ArrayList<>();
        }

        //do the standard search for each phrase
        for (String phrase : phrases) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(phrase);
            if (!list.isEmpty()) {
                infoArrayList.addAll(list);
            }
        }

        ArrayList<AccessibilityNodeInfo> finalList = new ArrayList<>();
        //do some filtering
        for (AccessibilityNodeInfo info : infoArrayList) {
            String searchStr = info.getText() + " // " + info.getContentDescription();
            if (!searchStr.contains("You can skip ad in 0s") && !searchStr.contains("Ad · 0:00") && !searchStr.contains("Download")) {
                Log.d(TAG, level + " || " + searchStr);
                finalList.add(info);
            }
        }

        return finalList;
    }


    //click a given node, ideally using performClick but with a secondary attempt via accessibility gesture
    boolean accessibilityClickNode(AccessibilityNodeInfo thisNode) {
        Log.w(TAG, "CLICKNODE: " + thisNode.getText() + ", " + thisNode.getContentDescription());
        boolean success = thisNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if ((thisNode.getText() != null && thisNode.getText().equals("back"))
                || thisNode.getContentDescription() != null && thisNode.getContentDescription().equals("back")) {
            Log.d(TAG, "Clicking BACK again!");
            //when exiting VR mode, back needs to be clicked TWICE
            //once to activate, once to actually go back
            success |= thisNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        if (!success) {
//            Rect bounds = new Rect();
//            thisNode.getBoundsInScreen(bounds);
//            main.tapBounds(bounds.centerX(), bounds.centerY());
            Log.e(TAG, "CLICK FAILED FOR " + thisNode.getText() + " // " + thisNode.getContentDescription());// + "! Also trying a manual tap at this location: " + bounds);
            return false;
        } else {
            Log.e(TAG, "CLICK SUCCESS FOR " + thisNode.getText() + " // " + thisNode.getContentDescription() + "!");
            return true;
        }
    }

    public void gestureTapNode(AccessibilityNodeInfo thisNode) {
        Rect bounds = new Rect();
        thisNode.getBoundsInScreen(bounds);
        main.tapBounds(bounds.centerX(), bounds.centerY());
    }


    public void bringMainToFront() {
        Log.d(TAG, "bringMainToFront: ");
        main.getHandler().post(() -> {
            if (main != null && !main.isAppVisibleInForeground()) {
                main.recallToLeadMe();
            }
        });
    }

    boolean showDebugMsg = false;
    AccessibilityEvent lastEvent = null;
    AccessibilityNodeInfo lastInfo = null;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    public boolean manageAccessibilityEvent(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {

        //TODO I believe this does the random tap, in here somewhere
        //Log.d(TAG, "manageAccessibilityEvent: ");
        boolean appInForeground = main.isAppVisibleInForeground();
        if (main == null || !main.getNearbyManager().isConnectedAsFollower()) {
            return false;
        }

        if (event == null && rootInActiveWindow == null) { //lastEvent != null) {
            Log.w(TAG, "Revisiting previous event..." + lastEvent + " " + lastInfo);
            event = lastEvent;
            rootInActiveWindow = lastInfo;
//            lastInfo = null; //we probably don't want to revisit these too many times
//            lastEvent = null; //we probably don't want to revisit these too many times

//       }
//
//        if (event == null && rootInActiveWindow == null) {
//            Log.e(TAG, "No events here to act on");
//            return false;

        } else {
            lastEvent = event;
            //if null youtube embed gestures stop working
            if(rootInActiveWindow != null) {
                lastInfo = rootInActiveWindow;
            }
        }

        if (event == null) {
            return false;
        }

        try {
            Log.w(TAG, "Current event..." + lastEvent + " " + lastInfo);
            //if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                Rect bounds = new Rect();
                if (event != null && event.getSource() != null) {
                    event.getSource().getBoundsInScreen(new Rect());
                }
                Log.e(TAG, "SOMETHING! " + AccessibilityEvent.eventTypeToString(event.getEventType()) + ", " + event.getPackageName() + ", " + event.getClassName() + ", " + event.getText() + ", " + event.getAction() + ", " + bounds);
                Log.e(TAG, "SOURCE! >>>  " + event.getSource());
            }

            AccessibilityEvent finalEvent = event;
            AccessibilityNodeInfo finalRootInActiveWindow = rootInActiveWindow;

            if (!appInForeground && event.getSource() != null && event.getSource().getPackageName().toString().contains(main.getAppManager().withinPackage)) {
                Log.e(TAG, "SOMETHING!");
                executor.execute(() -> {
                    withinManager.manageWithinAccess(finalEvent, finalRootInActiveWindow);
                });

            } else if (!appInForeground && event.getSource() != null && event.getSource().getPackageName().toString().contains(main.getAppManager().youtubePackage)) {
                executor.execute(() -> {
                    Log.e(TAG, "Youtube: " + finalEvent);
                    //non-UI work here
                    ytManager.manageYouTubeAccess(finalEvent, finalRootInActiveWindow);
                });

            } else if (appInForeground && dispatcher.launchAppOnFocus == null
                    && (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED && event.getPackageName().toString().equals("com.android.systemui")
                    /* || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && event.getPackageName().toString().equals("com.android.systemui")*/)) {
                //likely pulled down notifications while in main app
                Log.i(TAG, "User VIEWED status bar in LeadMe! " + event.toString());
                //main.recallToLeadMe();
                main.collapseStatus();
            } else if (appInForeground && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName().toString().equals("com.lumination.leadme")) {
                //if (showDebugMsg)
                Log.i(TAG, "User RETURNED TO LEADME! [" + appInForeground + "] " + event.toString());
                //don't need to do anything really, perhaps alert guide?
                dispatcher.alertGuideStudentOffTask();
                waitingForStateChange = false; //reset

            } else if ((waitingForStateChange && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) ||
                    //works for Redmi Note 7
                    (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName().toString().equals("com.android.launcher3")) ||

                    //works for MI 8 SE
                    (event.getPackageName().toString().equals("com.android.systemui") && event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED
                            /*&& (event.getContentDescription()) != null && (event.getContentDescription().equals("Overview") || event.getContentDescription().equals("Home"))*/)) {
                //either pulled down or clicked notification, setting or nav while in launched 3rd party app
                // if (showDebugMsg)
                Log.i(TAG, "User clicked some kind of SYSTEM button! " + event.toString());
                dispatcher.alertGuideStudentOffTask();
//                if (!main.studentLockOn) {
//                    if (showDebugMsg) Log.i(TAG, "It's OK, user is in free play mode");
//                    return true;
//                }

                waitingForStateChange = false;
                if (!appInForeground) {//!main.getAppLaunchAdapter().lastApp.equals(packageName)) {
                    dispatcher.launchAppOnFocus = new String[2];
                    dispatcher.launchAppOnFocus[0] = main.currentTaskPackageName;
                    dispatcher.launchAppOnFocus[1] = main.currentTaskName;
                    // if (showDebugMsg)
                    Log.d(TAG, "NEED FOCUS! " + dispatcher.launchAppOnFocus);
                    bringMainToFront();

                } else {
                    //if (showDebugMsg)
                    Log.d(TAG, "HAVE FOCUS!");
                    dispatcher.launchAppOnFocus = null; //reset
                    main.getAppManager().relaunchLast();
                }

            } else if (event.getPackageName().toString().equals("com.android.systemui")
                    && event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED
                /*&& event.getContentDescription() != null && event.getContentDescription() == null && event.getContentDescription().equals("Back")*/) {
                //if (showDebugMsg)
                Log.i(TAG, "User clicked SYSTEM WINDOW! " + event.toString());
                //don't need to do anything unless window state changes
                waitingForStateChange = true;
                dispatcher.alertGuideStudentOffTask();
                main.collapseStatus();


            } else if (
                //this works for MI 8 SE
                    (event.getPackageName().toString().equals("com.android.systemui") && event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) ||
                            //this works for Redmi Note 7
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE))) {
                if (showDebugMsg)
                    Log.i(TAG, "User VIEWED status bar or other SYSTEM interface! " + event.toString());
                main.collapseStatus();

            }

            //check if we're trying to install something, respond appropriately
            if ((AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED == event.getEventType()
                    || (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType()))) {
                //run the install event
                main.autoInstall(event);
            }

        } catch (Exception e) {
            //giant try-catch because we'd prefer to fail managing a single accessibility
            //action than crash the program outright
            e.printStackTrace();
        }

        try {
            Thread.sleep(200); //make sure we don't end up with endless looping
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected void triageReceivedIntent(Intent intent) {
        //TODO or this?
        //Log.d(TAG, "triageReceivedIntent!");
        //only students need to respond to these events
        if (intent.hasExtra(LumiAccessibilityService.INFO_TAG)) {
            //Log.d(TAG, "triageReceivedIntent: " + intent.getStringExtra(LumiAccessibilityService.INFO_TAG));
            switch (intent.getStringExtra(LumiAccessibilityService.INFO_TAG)) {
                case LumiAccessibilityService.REFRESH_ACTION:
                    Log.w(TAG, "REFRESHING ACCESSIBILITY STATE");
                    //trigger function to re-run on last received event
                    manageAccessibilityEvent(null, null);

                case LumiAccessibilityService.INFO_CONFIG:
                    if (main.getNearbyManager().isConnectedAsFollower()) {
                        main.runOnUiThread(() -> main.refreshOverlay());
                    }
                    break;

                case LumiAccessibilityService.INFO_CONNECTED:
                    Log.d(TAG, "Recalling from settings to LeadMe");
                    main.recallToLeadMe();
                    break;

                case LumiAccessibilityService.EVENT_RECEIVED:

                    if (main.getNearbyManager().isConnectedAsFollower() || main.getNearbyManager().isConnectedAsGuide()) {
                        Bundle data = intent.getExtras();
                        Object one = data.getString(LumiAccessibilityService.INFO_TAG);
                        Object two = data.getParcelable(LumiAccessibilityService.EVENT_OBJ);
                        Object three = data.getParcelable(LumiAccessibilityService.EVENT_ROOT);

                        AccessibilityEvent evt = (AccessibilityEvent) two;
                        AccessibilityNodeInfo root = (AccessibilityNodeInfo) three;
                        manageAccessibilityEvent(evt, root);
                    }
                    break;
            }
        }
    }
}
