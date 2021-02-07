package com.lumination.leadme;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LumiAccessibilityConnector {

    private static final String TAG = "LumiAccessConnector";

    public static final String PROPAGATE_ACTION = "com.lumination.leadme.PROPAGATED_ACTION";

    private LeadMeMain main;
    private String lastAppName, lastPackageName;

    //handler for executing on the main thread
    //private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DispatchManager dispatcher;
    private ArrayList<String> cuedActions = new ArrayList<>();

    public static final int CUE_PLAY = 0;
    public static final int CUE_PAUSE = 1;
    public static final int CUE_FWD = 2;
    public static final int CUE_RWD = 3;
    public static final int CUE_CAPTIONS_ON = 4;
    public static final int CUE_CAPTIONS_OFF = 5;
    public static final int CUE_VR_ON = 6;
    public static final int CUE_VR_OFF = 7;
    //don't need to schedule mute as this is managed elsewhere

    public LumiAccessibilityConnector(LeadMeMain main) {
        this.main = main;
        dispatcher = main.getDispatcher();
    }

    public void cueYouTubeAction(String actionStr) {
        if (main.getNearbyManager().isConnectedAsGuide()) {
            return; //guides manage YT their own way
        }
        int action = Integer.parseInt(actionStr);
        if (!cuedActions.isEmpty()) {
            switch (action) {
                //clean out any superseded actions
                case CUE_PLAY:
                case CUE_PAUSE:
                    cuedActions.remove(CUE_PAUSE + "");
                    cuedActions.remove(CUE_PLAY + "");
                    break;

                case CUE_CAPTIONS_ON:
                case CUE_CAPTIONS_OFF:
                    cuedActions.remove(CUE_CAPTIONS_OFF + "");
                    cuedActions.remove(CUE_CAPTIONS_ON + "");
                    break;

                case CUE_VR_ON:
                case CUE_VR_OFF:
                    cuedActions.remove(CUE_VR_OFF + "");
                    cuedActions.remove(CUE_VR_ON + "");
                    break;
            }
        }
        //add new action
        cuedActions.add(action + "");
        Log.d(TAG, "CUED ACTIONS: " + cuedActions);
        manageYouTubeAccess(lastEvent, lastInfo); //re-try last event
    }

    private static boolean waitingForStateChange = false;

    //these are not case sensitive, and will return partial matches
    private static String[] keyYouTubePhrases = {
            "Play video",
            "Pause video",
            "Enter virtual reality mode",
            "Watch in VR",
            "More options",
            "Autoplay is on",
            "Enter fullscreen",
            "Move device to explore video",
            "Cancel"
            //"Exit fullscreen"
            //"Action menu"
    };

    private static String[] popupPhrases = {
            "Continue",
            "Dismiss",
            "Skip trial",
            "No Thanks",
            "Cancel auto",
            "Hide related videos"
    };

    private static String[] skipAdsPhrases = {
            "Skip ad",
            "Skip ads",
    };

    private static String[] detectAdsPhrases = {
            "Video will play after ad",
            "Ad",
            "Up next"
    };

    private static String[] detectMiniPlayer = {
            "Close miniplayer"
    };

    private void skipAds(AccessibilityNodeInfo rootInActiveWindow) {
        ArrayList<AccessibilityNodeInfo> popupNodes = collectChildren(rootInActiveWindow, skipAdsPhrases, 0);
        for (AccessibilityNodeInfo thisInfo : popupNodes) {
            Rect bounds = new Rect();
            thisInfo.getBoundsInScreen(bounds);
            main.tapBounds(bounds.centerX(), bounds.centerY());
        }
    }

    boolean videoPlayStarted = false;
    boolean closedMini = false;

    private void closeMiniPlayer(AccessibilityNodeInfo rootInActiveWindow) {
        if (closedMini) {
            return; //don't launch it twice
        }
        ArrayList<AccessibilityNodeInfo> miniPlayerNodes = collectChildren(rootInActiveWindow, detectMiniPlayer, 0);
        if (!miniPlayerNodes.isEmpty()) {
            closedMini = true;
            //difficult to accurately tap the right button, so just relaunch the video
            String title = main.getWebManager().getLaunchTitle();
            String url = main.getWebManager().getPushURL();
            main.getWebManager().launchYouTube(url, title, false);
            cueYouTubeAction(CUE_PAUSE + "");
            cueYouTubeAction(CUE_VR_OFF + "");
        }
    }

    private void dismissPopups(AccessibilityNodeInfo rootInActiveWindow) {
        ArrayList<AccessibilityNodeInfo> popupNodes = collectChildren(rootInActiveWindow, popupPhrases, 0);
        for (AccessibilityNodeInfo thisInfo : popupNodes) {
            clickNode(thisInfo);
        }
    }

    private ArrayList<AccessibilityNodeInfo> collectChildren(AccessibilityNodeInfo nodeInfo, String phrase, int level) {
        String[] tmp = {phrase};
        return collectChildren(nodeInfo, tmp, level);
    }

    //recursively search for child nodes of the given parent that contain
    //any of the specified phrases in their text or content description
    private ArrayList<AccessibilityNodeInfo> collectChildren(AccessibilityNodeInfo nodeInfo, String[] phrases, int level) {
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


    public void clearCuedActions() {
        cuedActions.clear();
    }


    //click a given node, ideally using performClick but with a secondary attempt via accessibility gesture
    private boolean clickNode(AccessibilityNodeInfo thisNode) {
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

    public void manageYouTubeAccess(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {
        if (main.getNearbyManager().isConnectedAsGuide()) {
            return; //guides manage YT their own way
        }
        Log.d(TAG, "Managing YT: " + event);

        if (event == null && rootInActiveWindow == null && lastEvent != null && lastInfo != null) {
            event = lastEvent;
            rootInActiveWindow = lastInfo;
        } else {
            lastEvent = event;
            lastInfo = rootInActiveWindow;
        }

        closeMiniPlayer(rootInActiveWindow);
        skipAds(rootInActiveWindow);

        ArrayList<AccessibilityNodeInfo> detectAdNodes = collectChildren(rootInActiveWindow, detectAdsPhrases, 0);
        if (!detectAdNodes.isEmpty()) {
            Log.e(TAG, "WAITING FOR AD TO FINISH >> " + main.getWebManager().getLaunchTitle());

            //this indicates the video finished
            if (videoPlayStarted) {
                bringMainToFront();
                main.updateFollowerCurrentTaskToLeadMe();
                return;
            }

            for (AccessibilityNodeInfo detectNode : detectAdNodes) {
                if (detectNode.getText() != null && detectNode.getText().toString().contains("Up next in")) {
                    bringMainToFront();
                    main.updateFollowerCurrentTaskToLeadMe();
                    return;
                }
            }

            ArrayList<AccessibilityNodeInfo> playNodes = collectChildren(rootInActiveWindow, "Play video", 0);
            for (AccessibilityNodeInfo playNode : playNodes) {
                clickNode(playNode); //if we've paused the video, we actually need to play it to continue
            }


            //main.getWebManager().setFreshPlay(true); //keep resetting this until the ad is gone
            return; //don't do ANYTHING else until the ad is gone
        }

        if (!videoPlayStarted && !main.getWebManager().isFreshPlay()
                && !collectChildren(rootInActiveWindow, " / ", 0).isEmpty()
                && !collectChildren(rootInActiveWindow, main.getWebManager().getLaunchTitle(), 0).isEmpty()) {
            Log.w(TAG, "Found ELAPSED time for CURRENT video!");
            videoPlayStarted = true;
        }

        Point p = new Point();
        main.windowManager.getDefaultDisplay().getRealSize(p);

        if (main.getWebManager().isFreshPlay()) {
            Log.w(TAG, "FRESH PLAY");
            videoPlayStarted = false; //reset this here
            closedMini = false; //reset
            main.getWebManager().setFreshPlay(false); //done!
            tapVideoScreen();
            dismissPopups(rootInActiveWindow); //do this for each fresh load of the video
            cueYouTubeAction(CUE_PAUSE + "");
            cueYouTubeAction(CUE_VR_OFF + "");
        }

        if (cuedActions.isEmpty()) {
            return; //nothing to do!
        }

        String[] selectedPhrases = {};
        //make a copy of the cued actions so we can act on the array list
        //during iteration without causing a concurrent access exception
        String[] currentlyCuedActions = new String[cuedActions.size()];
        currentlyCuedActions = cuedActions.toArray(currentlyCuedActions);
        for (int i = 0; i < currentlyCuedActions.length; i++) {
            int action = Integer.parseInt(currentlyCuedActions[i]);
            switch (action) {
                //clean out any superseded actions
                case CUE_PLAY:
                    selectedPhrases = getPlayPhrases();
                    //not a fresh vid after we play it
                    //main.getWebManager().setFreshPlay(false);
                    break;

                case CUE_PAUSE:
                    selectedPhrases = getPausePhrases();
                    break;

                case CUE_VR_ON:
                    selectedPhrases = getVROnPhrases();
                    //this will also automatically play
                    //not a fresh vid after we play it
                    //main.getWebManager().setFreshPlay(false);
                    break;

                case CUE_VR_OFF:
                    selectedPhrases = getVROffPhrases();
                    break;

                case CUE_CAPTIONS_ON:
                case CUE_CAPTIONS_OFF:
                case CUE_FWD:
                case CUE_RWD:
                    //TODO all of these
                    cuedActions.remove(i);
                    break;
            }


            if (selectedPhrases.length > 0) {
                ArrayList<AccessibilityNodeInfo> actionNodes = collectChildren(rootInActiveWindow, selectedPhrases, 0);
                boolean success = false;
                if (!actionNodes.isEmpty()) {
                    for (AccessibilityNodeInfo thisInfo : actionNodes) {
                        Log.w(TAG, "Looping: " + thisInfo.getText() + " // " + thisInfo.getContentDescription());
                        tapVideoScreen();
                        success = clickNode(thisInfo);
                        if (success && cuedActions.contains(action + "")) {
                            cuedActions.remove(i); //remove this action, we've done it
                        }
                    }
                }

                //now clean up issues after attempted clicks
                if (cuedActions.contains(CUE_VR_OFF + "")) {
                    Log.i(TAG, "Problem with Full Screen mode");
                    ArrayList<AccessibilityNodeInfo> vrNodes = collectChildren(rootInActiveWindow, getVROnPhrases(), 0);
                    if (!vrNodes.isEmpty()) {
                        Log.i(TAG, "Already in FULLSCREEN mode");
                        cuedActions.remove(CUE_VR_OFF + ""); //don't need to keep looking
                    } else {
                        ArrayList<AccessibilityNodeInfo> fsNodes = collectChildren(rootInActiveWindow, main.getWebManager().getLaunchTitle(), 0);
                        Log.e(TAG, "What is there? " + fsNodes);
                        tapVideoScreen();
                    }

                } else if (cuedActions.contains(CUE_VR_ON + "")) {
                    Log.i(TAG, "Problem with Enter VR mode");
                    //can't find the button, try unlocking the screen
                    ArrayList<AccessibilityNodeInfo> vrNodes = collectChildren(rootInActiveWindow, getVROffPhrases(), 0);
                    if (!vrNodes.isEmpty()) {
                        Log.i(TAG, "Already in VR mode");
                        cuedActions.remove(CUE_VR_ON + ""); //don't need to keep looking
                    } else {
                        tapVideoScreen();
                    }

                } else if (cuedActions.contains(CUE_PAUSE + "")) {
                    Log.i(TAG, "Problem with Pause ");
                    //can't find the button, try unlocking the screen
                    ArrayList<AccessibilityNodeInfo> pauseNodes = collectChildren(rootInActiveWindow, getPlayPhrases(), 0);
                    if (!pauseNodes.isEmpty()) {
                        Log.i(TAG, "Already in PAUSE mode");
                        cuedActions.remove(CUE_PAUSE + ""); //don't need to keep looking
                    } else {
                        tapVideoScreen();
                    }

                } else if (cuedActions.contains(CUE_PLAY + "")) {
                    Log.i(TAG, "Problem with Play");
                    //can't find the button, try unlocking the screen
                    ArrayList<AccessibilityNodeInfo> pauseNodes = collectChildren(rootInActiveWindow, getPausePhrases(), 0);
                    if (!pauseNodes.isEmpty()) {
                        Log.i(TAG, "Already in PLAY mode");
                        cuedActions.remove(CUE_PLAY + ""); //don't need to keep looking
                    } else {
                        tapVideoScreen();
                    }
                }

            }
            Log.e(TAG, "CUED ACTIONS NOW: " + cuedActions);
        }
        return;

    }

    private void tapVideoScreen() {
        Point p = new Point();
        main.getWindowManager().getDefaultDisplay().getRealSize(p);
        if (p.x < p.y) {
            main.tapBounds((p.x / 2), (p.y / 3)); //portrait
            //Log.w(TAG, "TAP TAP! " + (p.x / 2) + ", " + (p.y / 3) + " vs hardcoded 518, 927");
        } else {
            main.tapBounds((p.x / 2), (p.y / 3)); //landscape
            //Log.w(TAG, "TAP TAP! " + (p.x / 2) + ", " + (p.y / 3) + " vs hardcoded 927, 518");
        }
    }

    private String[] getPlayPhrases() {
        String[] selectedPhrases = new String[1];
        selectedPhrases[0] = "Play video";
        return selectedPhrases;
    }

    private String[] getPausePhrases() {
        String[] selectedPhrases = new String[1];
        selectedPhrases[0] = "Pause";
        return selectedPhrases;
    }

    private String[] getVROnPhrases() {
        String[] selectedPhrases = new String[2];
        selectedPhrases[0] = "Enter virtual reality mode";
        selectedPhrases[1] = "Watch in VR";
        return selectedPhrases;
    }

    private String[] getVROffPhrases() {
        String[] selectedPhrases = new String[3];
        selectedPhrases[0] = "back";
        selectedPhrases[1] = "Enter full screen"; //used for MiSE8
        selectedPhrases[2] = "Enter fullscreen"; //used for Redmi7
        return selectedPhrases;
    }


    public void bringMainToFront() {
        main.getHandler().post(() -> {
            if (main != null && !main.appHasFocus) {
                main.recallToLeadMe();
            }
        });
    }

    boolean showDebugMsg = false;
    private AccessibilityEvent lastEvent = null;
    private AccessibilityNodeInfo lastInfo = null;

    private boolean manageAccessibilityEvent(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {
        if (main == null || !main.getNearbyManager().isConnectedAsFollower()) {
            return false;
        }

        try {
            //if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                Rect bounds = new Rect();
                if (event != null && event.getSource() != null) {
                    event.getSource().getBoundsInScreen(new Rect());
                }
                Log.e(TAG, "SOMETHING! " + event.getPackageName() + ", " + event.getClassName() + ", " + event.getText() + ", " + event.getAction() + ", " + bounds);
                Log.e(TAG, "SOURCE! >>>  " + event.getSource());
            }

            //if (showDebugMsg)
            //Log.w(TAG, "Managing received AccessibilityEvent: " + main.appHasFocus + ", " + main.getWebManager().launchingVR + " >>> " + event);
            if (!main.appHasFocus && event.getSource() != null && event.getSource().getPackageName().toString().contains("youtube")) {
                manageYouTubeAccess(event, rootInActiveWindow);

            } else if (main.appHasFocus && dispatcher.launchAppOnFocus == null
                    && (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED && event.getPackageName().toString().equals("com.android.systemui")
                    /* || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && event.getPackageName().toString().equals("com.android.systemui")*/)) {
                //likely pulled down notifications while in main app
                Log.i(TAG, "User VIEWED status bar in LeadMe! " + event.toString());
                //main.recallToLeadMe();
                main.collapseStatus();

            } else if (main.appHasFocus && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName().toString().equals("com.lumination.leadme")) {
                //if (showDebugMsg)
                Log.i(TAG, "User RETURNED TO LEADME! [" + main.appHasFocus + "] " + event.toString());
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
                if (!main.studentLockOn) {
                    if (showDebugMsg) Log.i(TAG, "It's OK, user is in free play mode");
                    return true;
                }
                waitingForStateChange = false;
                if (!main.appHasFocus) {//!main.getAppLaunchAdapter().lastApp.equals(packageName)) {
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


            //check if we're trying to install something and respond appropriately
            if ((AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED == event.getEventType()
                    || (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType()))
                    && (lastAppName != null) && (lastAppName.length() > 0)) {
                AccessibilityNodeInfo nodeInfo = event.getSource();
                if (nodeInfo == null) {
                    return false;
                }

                //TODO this hasn't been tested since some pretty major updates were made
                //needs thorough testing before release

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
                        if (showDebugMsg)
                            Log.i(TAG, "ACC::onAccessibilityEvent: Accept (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                        acceptButton = node;
                        break;
                    }
                }

                for (AccessibilityNodeInfo node : installNodes) {
                    if (node.getText() != null && node.getText().equals("Install")) {
                        if (showDebugMsg)
                            Log.i(TAG, "ACC::onAccessibilityEvent: Install (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                        installerButton = node;
                        break;
                    }
                }

                for (AccessibilityNodeInfo node : openNodes) {
                    if (node.getText() != null && node.getText().equals("Open")) {
                        if (showDebugMsg)
                            Log.i(TAG, "ACC::onAccessibilityEvent: Open (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                        openButton = node;
                        break;
                    }
                }

                for (AccessibilityNodeInfo node : textNodes) {
                    if (node.getText() != null && node.getText().equals(lastAppName)) {
                        //found one, that's enough
                        foundTitle = true;
                        if (showDebugMsg)
                            Log.i(TAG, "ACC::onAccessibilityEvent: Title (" + lastAppName + ") " + node.getPackageName() + " for " + node.getText());
                        break;
                    }
                }

                for (AccessibilityNodeInfo node : accessToTextNodes) {
                    if (node.getText() != null && node.getText().equals(needsAccessTxt)) {
                        //found it once, that's enough
                        foundNeedsAccess = true;
                        if (showDebugMsg)
                            Log.i(TAG, "ACC::onAccessibilityEvent: Needs Access (" + needsAccessTxt + ") " + node.getPackageName() + " for " + node.getText());
                        break;
                    }
                }

                //if we can open it, it's already installed
                if (openButton != null && foundTitle) {
                    if (showDebugMsg)
                        Log.i(TAG, ">> Now let's launch it! " + openNodes.size() + ", " + openButton + ", " + openButton.isClickable());

                    if (openButton.isClickable()) {
                        boolean success = openButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        if (!success) {
                            if (showDebugMsg)
                                Log.i(TAG, ">> Second try opening it... " + openNodes.size() + ", " + openButton);
                            success = openNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                        if (!success) {
                            if (showDebugMsg)
                                Log.i(TAG, ">> Third try opening it... " + openNodes.size() + ", " + openButton);
                            main.getAppManager().launchLocalApp(lastPackageName, lastAppName, true);
                        }
                        lastAppName = null; //reset, we're done
                        return true; //all done, can exit
                    } else {
                        if (showDebugMsg) Log.d(TAG, "Not clickable yet...");
                        return false;
                    }
                }

                //otherwise we need to install it
                if (installerButton != null && foundTitle) {
                    if (showDebugMsg)
                        Log.i(TAG, ">> Let's try to install that app! " + installNodes.size());
                    installerButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }

                //we might need to accept permissions
                if (acceptButton != null && foundNeedsAccess) {
                    if (showDebugMsg)
                        Log.i(TAG, ">> Let's accept it's permissions! " + acceptNodes.size());
                    acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }

        } catch (Exception e) {
            //giant try-catch because we'd prefer to fail managing a single accessibility
            //action than crash the program outright
            e.printStackTrace();
        }

        return false;
    }

    public void prepareToInstall(String packageName, String appName) {
        lastAppName = appName;
        lastPackageName = packageName;
        Log.d(TAG, "PREPARING TO INSTALL " + lastAppName);
    }

    protected void triageReceivedIntent(Intent intent) {
        //only students need to respond to these events
        if (intent.hasExtra(LumiAccessibilityService.INFO_TAG)) {
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
                    main.recallToLeadMe();
                    break;

                case LumiAccessibilityService.EVENT_RECEIVED:
                    if (main.getNearbyManager().isConnectedAsFollower() || main.getNearbyManager().isConnectedAsGuide()) {
                        AccessibilityEvent evt = intent.getParcelableExtra(LumiAccessibilityService.EVENT_OBJ);
                        AccessibilityNodeInfo root = intent.getParcelableExtra(LumiAccessibilityService.EVENT_ROOT);
                        manageAccessibilityEvent(evt, root);
                    }
                    break;
            }
        }
    }
}
