package com.lumination.leadme;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

public class YouTubeAccessibilityManager {

    private static final String TAG = "YTAccess";

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


    private LumiAccessibilityConnector connector;
    private LeadMeMain main;

    public YouTubeAccessibilityManager(LeadMeMain main, LumiAccessibilityConnector connector) {
        this.connector = connector;
        this.main = main;
    }


    public void manageYouTubeAccess(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {
        if (main.getNearbyManager().isConnectedAsGuide()) {
            return; //guides manage YT their own way
        }
        Log.d(TAG, "Managing YT: " + event);

        if (event == null && rootInActiveWindow == null && connector.lastEvent != null && connector.lastInfo != null) {
            event = connector.lastEvent;
            rootInActiveWindow = connector.lastInfo;
        } else {
            connector.lastEvent = event;
            connector.lastInfo = rootInActiveWindow;
        }

        closeMiniPlayer(rootInActiveWindow);
        skipAds(rootInActiveWindow);

        ArrayList<AccessibilityNodeInfo> detectAdNodes = connector.collectChildren(rootInActiveWindow, detectAdsPhrases, 0);
        if (!detectAdNodes.isEmpty()) {
            Log.e(TAG, "WAITING FOR AD TO FINISH >> " + main.getWebManager().getLaunchTitle());

            //this indicates the video finished
            if (videoPlayStarted) {
                connector.bringMainToFront();
                main.updateFollowerCurrentTaskToLeadMe();
                return;
            }

            for (AccessibilityNodeInfo detectNode : detectAdNodes) {
                if (detectNode.getText() != null && detectNode.getText().toString().contains("Up next in")) {
                    connector.bringMainToFront();
                    main.updateFollowerCurrentTaskToLeadMe();
                    return;
                }
            }

            ArrayList<AccessibilityNodeInfo> playNodes = connector.collectChildren(rootInActiveWindow, "Play video", 0);
            for (AccessibilityNodeInfo playNode : playNodes) {
                connector.accessibilityClickNode(playNode); //if we've paused the video, we actually need to play it to continue
            }


            //main.getWebManager().setFreshPlay(true); //keep resetting this until the ad is gone
            return; //don't do ANYTHING else until the ad is gone
        }

        if (!videoPlayStarted && !main.getWebManager().isFreshPlay()
                && !connector.collectChildren(rootInActiveWindow, " / ", 0).isEmpty()
                && !connector.collectChildren(rootInActiveWindow, main.getWebManager().getLaunchTitle(), 0).isEmpty()) {
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
                ArrayList<AccessibilityNodeInfo> actionNodes = connector.collectChildren(rootInActiveWindow, selectedPhrases, 0);
                boolean success = false;
                if (!actionNodes.isEmpty()) {
                    for (AccessibilityNodeInfo thisInfo : actionNodes) {
                        Log.w(TAG, "Looping: " + thisInfo.getText() + " // " + thisInfo.getContentDescription());
                        tapVideoScreen();
                        success = connector.accessibilityClickNode(thisInfo);
                        if (success && cuedActions.contains(action + "")) {
                            cuedActions.remove(i); //remove this action, we've done it
                        }
                    }
                }

                //now clean up issues after attempted clicks
                if (cuedActions.contains(CUE_VR_OFF + "")) {
                    Log.i(TAG, "Problem with Full Screen mode");
                    ArrayList<AccessibilityNodeInfo> vrNodes = connector.collectChildren(rootInActiveWindow, getVROnPhrases(), 0);
                    if (!vrNodes.isEmpty()) {
                        Log.i(TAG, "Already in FULLSCREEN mode");
                        cuedActions.remove(CUE_VR_OFF + ""); //don't need to keep looking
                    } else {
                        ArrayList<AccessibilityNodeInfo> fsNodes = connector.collectChildren(rootInActiveWindow, main.getWebManager().getLaunchTitle(), 0);
                        Log.e(TAG, "What is there? " + fsNodes);
                        tapVideoScreen();
                    }

                } else if (cuedActions.contains(CUE_VR_ON + "")) {
                    Log.i(TAG, "Problem with Enter VR mode");
                    //can't find the button, try unlocking the screen
                    ArrayList<AccessibilityNodeInfo> vrNodes = connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0);
                    if (!vrNodes.isEmpty()) {
                        Log.i(TAG, "Already in VR mode");
                        cuedActions.remove(CUE_VR_ON + ""); //don't need to keep looking
                    } else {
                        tapVideoScreen();
                    }

                } else if (cuedActions.contains(CUE_PAUSE + "")) {
                    Log.i(TAG, "Problem with Pause ");
                    //can't find the button, try unlocking the screen
                    ArrayList<AccessibilityNodeInfo> pauseNodes = connector.collectChildren(rootInActiveWindow, getPlayPhrases(), 0);
                    if (!pauseNodes.isEmpty()) {
                        Log.i(TAG, "Already in PAUSE mode");
                        cuedActions.remove(CUE_PAUSE + ""); //don't need to keep looking
                    } else {
                        tapVideoScreen();
                    }

                } else if (cuedActions.contains(CUE_PLAY + "")) {
                    Log.i(TAG, "Problem with Play");
                    //can't find the button, try unlocking the screen
                    ArrayList<AccessibilityNodeInfo> pauseNodes = connector.collectChildren(rootInActiveWindow, getPausePhrases(), 0);
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

    public void clearCuedActions() {
        cuedActions.clear();
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
        manageYouTubeAccess(connector.lastEvent, connector.lastInfo); //re-try last event
    }

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
            "Skip survey",
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
            "Up next in"
    };

    private static String[] detectMiniPlayer = {
            "Close miniplayer"
    };

    private void skipAds(AccessibilityNodeInfo rootInActiveWindow) {
        ArrayList<AccessibilityNodeInfo> popupNodes = connector.collectChildren(rootInActiveWindow, skipAdsPhrases, 0);
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
        ArrayList<AccessibilityNodeInfo> miniPlayerNodes = connector.collectChildren(rootInActiveWindow, detectMiniPlayer, 0);
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
        ArrayList<AccessibilityNodeInfo> popupNodes = connector.collectChildren(rootInActiveWindow, popupPhrases, 0);
        for (AccessibilityNodeInfo thisInfo : popupNodes) {
            connector.accessibilityClickNode(thisInfo);
        }
    }
}
