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
    public static final int CUE_FS_ONLY = 8;
    //don't need to schedule mute as this is managed elsewhere

    private LumiAccessibilityConnector connector;
    private LeadMeMain main;

    public YouTubeAccessibilityManager(LeadMeMain main, LumiAccessibilityConnector connector) {
        Log.d(TAG, "YouTubeAccessibilityManager: ");
        this.connector = connector;
        this.main = main;
    }

    public boolean adFinished = false;
    private boolean inVR = false;
    private String goalTime = "";
    private String goalTimeShort = "";

    boolean justTesting = false;

    public void manageYouTubeAccess(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {

        Log.d(TAG, "manageYouTubeAccess: ");
        if (main.getNearbyManager().isConnectedAsGuide()) {
            return; //guides manage YT their own way
        }

//        if (main.getLumiAccessibilityConnector().gestureInProgress) {
//            Log.i(TAG, "WAITING FOR LAST GESTURE");
//            return;
//        }

        AccessibilityNodeInfo eventSource = event.getSource();
        Log.e(TAG, "ROOT IN WINDOW? " + (rootInActiveWindow != null) + " vs " + (eventSource != null));// + " //PREV] " + lastYouTubeInfo);
        //we're refreshing a previous event
        if (rootInActiveWindow == null && eventSource != null) {
            Log.w(TAG, "Using event source instead");
            rootInActiveWindow = eventSource;
        }

        if (rootInActiveWindow == null) {
            tapVideoScreen(); //activate the window
            return;
        }

        closeMiniPlayer(rootInActiveWindow);
        skipAds(rootInActiveWindow);

        ArrayList<AccessibilityNodeInfo> detectAdNodes = connector.collectChildren(rootInActiveWindow, detectAdsPhrases, 0);
        if (!detectAdNodes.isEmpty()) {
            Log.e(TAG, "WAITING FOR AD TO FINISH >> " + main.getWebManager().getLaunchTitle());
            adFinished = false;

            for (AccessibilityNodeInfo detectNode : detectAdNodes) {
                if (detectNode.getText() != null && detectNode.getText().toString().contains("Up next in")) {
                    endOfVideo();
                    return;
                }
            }

            ArrayList<AccessibilityNodeInfo> playNodes = connector.collectChildren(rootInActiveWindow, "Play video", 0);
            for (AccessibilityNodeInfo playNode : playNodes) {
                //if we've paused the video, we actually need to play it to continue
                connector.accessibilityClickNode(playNode);
            }
            return; //don't do ANYTHING else until the ad is gone
        }

        //send ready status to guide if the ads have finished
        if(!adFinished) {
            main.backgroundExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    //Delay the message in case an Ad starts after a half second delay
                    try {
                        Thread.sleep(1500);

                        if(adFinished) {
                            main.getHandler().post(() -> {
                                alertGuideAdsHaveFinished();
                            });
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            //Find all the accessibility nodes on the screen.
            //Use when youtube updates or new buttons are needed to be actioned.
            //connector.displayAllChildren(rootInActiveWindow);

            adFinished = true;
        }

        Point p = new Point();
        main.windowManager.getDefaultDisplay().getRealSize(p);

        dismissPopups(rootInActiveWindow); //do this for each fresh load of the video

        ArrayList<AccessibilityNodeInfo> adNodes = null;
        if (!main.getWebManager().isFreshPlay() && !videoPlayStarted) {
            //this code assists in confirming that the ACTUAL video (not just an ad) has started
            //tapVideoScreen();
            pushTitle = main.getWebManager().getLaunchTitle().trim();
            pushURL = main.getWebManager().getPushURL();

            adNodes = connector.collectChildren(rootInActiveWindow, "Ad", 0);

            adNodes.addAll(connector.collectChildren(rootInActiveWindow, "title", 0));

            Log.d(TAG, "Has it started? " + videoPlayStarted + ", " + main.getWebManager().isFreshPlay() + ", " + adNodes.size() + " for " + pushTitle);
//            tapVideoScreen();

            if (adNodes.isEmpty()) {
                Log.e(TAG, "VIDEO STARTED!!");
                videoPlayStarted = true;
            }
//            tapVideoScreen();
        }

        if (main.getWebManager().isFreshPlay()) {
            //this typically happens before ads play,
            //but sometimes doesn't trigger until after
            Log.w(TAG, "FRESH PLAY");
            videoPlayStarted = false; //reset this here
            closedMini = false; //reset
            main.getWebManager().setFreshPlay(false); //done!
            cueYouTubeAction(CUE_PAUSE + "");
            cueYouTubeAction(CUE_FS_ONLY + "");
        } else if (!videoPlayStarted && !pushTitle.isEmpty() && adNodes == null && adNodes.isEmpty()) {
            //confirms that actual video, not just an ad, has started
            Log.w(TAG, "Ad nodes are empty for CURRENT video! Title=" + pushTitle + ", " + adNodes.size());
            videoPlayStarted = true;
        }

        if (!videoPlayStarted) {
            Log.e(TAG, "Not ready to manage this video yet! " + pushTitle + ", " + adNodes + " // " + pushURL);
            return; //not ready for the rest yet
        }

        //check if this should auto-enter VR mode
        if (main.getWebManager().launchingVR) {
            cuedActions.add(CUE_VR_ON + "");
            main.getWebManager().launchingVR = false;
        }

        if (goalTime.isEmpty()) {
            ArrayList<AccessibilityNodeInfo> timeNodes = connector.collectChildren(rootInActiveWindow, ":", 0);
            if (!timeNodes.isEmpty()) {
                for (AccessibilityNodeInfo thisInfo : timeNodes) {
                    Log.w(TAG, "Testing for time: " + thisInfo.getText());
                    if (thisInfo == null) {
                        continue;
                    }
                    String time = thisInfo.getText() + "";
                    String[] times = time.split("/");

                    if (times.length == 2) {
                        String[] goalTimeSplit = times[1].trim().split(":");
                        goalTime = times[1].trim() + "/" + times[1].trim();
                        goalTimeShort = goalTimeSplit[0] + ":" + (Integer.parseInt(goalTimeSplit[1]) - 1);//times[1].trim();

                        Log.e(TAG, "Expected end time is: " + time + ", " + goalTime + ", min: " + goalTimeShort);
                        break;
                    }
                }
            }
        } else {
            ArrayList<AccessibilityNodeInfo> timeNodes = connector.collectChildren(rootInActiveWindow, goalTime, 0);
            if (!timeNodes.isEmpty()) {
                Log.d(TAG, "TIMENODES_OG - " + timeNodes);
                //we've reached the end of the video
                endOfVideo();
                return;

            } else {
                ArrayList<AccessibilityNodeInfo> timeNodes2 = connector.collectChildren(rootInActiveWindow, goalTimeShort, 0);
                if (!timeNodes2.isEmpty()) {
                    Log.d(TAG, "TIMENODES2 - " + goalTimeShort + " vs " + timeNodes2.size() + ", " + timeNodes2);
                    for (AccessibilityNodeInfo thisInfo : timeNodes2) {
                        Log.d(TAG, thisInfo.getText().toString() + "|" + goalTimeShort);
                        if (thisInfo.getText().toString().equals(goalTimeShort)) {
                            //we've reached the end of the video
                            endOfVideo();
                            return;
                        }
                    }
                }
            }
        }

        if (cuedActions.isEmpty()) {
            return; //nothing to do!
        }

        //TODO whatever is killing the UI is AFTER this point


        //String[] selectedPhrases = {};
        //make a copy of the cued actions so we can act on the array list
        //during iteration without causing a concurrent access exception
        String[] currentlyCuedActions = new String[cuedActions.size()];
        currentlyCuedActions = cuedActions.toArray(currentlyCuedActions);
        for (int i = 0; i < currentlyCuedActions.length; i++) {
            int action = Integer.parseInt(currentlyCuedActions[i]);
            switch (action) {
                case CUE_CAPTIONS_ON:
                case CUE_CAPTIONS_OFF:
                case CUE_FWD:
                case CUE_RWD:
                    //TODO all of these
                    cuedActions.remove(i);
                    break;
            }

            if (cuedActions.contains(CUE_VR_OFF + "")) {
                Log.i(TAG, "Exiting VR mode");
                ArrayList<AccessibilityNodeInfo> vrNodes = connector.collectChildren(rootInActiveWindow, getVROnPhrases(), 0);
                ArrayList<AccessibilityNodeInfo> vrNodes2 = connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0);
                if (!vrNodes.isEmpty()||(vrNodes.isEmpty() && vrNodes2.isEmpty())) {// || !inVR) {
                    Log.i(TAG, "Already in FULLSCREEN mode");
                    inVR = false;
                    cuedActions.remove(CUE_VR_OFF + ""); //don't need to keep looking
                } else {
                    tapVideoScreen();
                    for (AccessibilityNodeInfo thisInfo : connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0)) {
                        Log.w(TAG, "-- Tapping " + thisInfo.getContentDescription() + "/" + thisInfo.getText());
                        boolean success = gestureTap(thisInfo, CUE_VR_OFF);
                        if ((thisInfo.getText() + " " + thisInfo.getContentDescription()).contains("back")) {
                            cueYouTubeAction(CUE_FS_ONLY + ""); //so we enter full screen, not just exit VR
                        }
                        if (success) {
                            inVR = false;
                        }
                    }
                    if (!connector.collectChildren(rootInActiveWindow, getVROnPhrases(), 0).isEmpty()) {
                        cuedActions.remove(CUE_VR_OFF + "");
                    }
                }
            }

            if (cuedActions.contains(CUE_FS_ONLY + "")) {
                Log.i(TAG, "Entering Full Screen mode");
                ArrayList<AccessibilityNodeInfo> vrNodes = connector.collectChildren(rootInActiveWindow, "back", 0);
                if (!vrNodes.isEmpty()) {
                    tapVideoScreen();
                    Log.i(TAG, "Need to exit VR first");
                    for (AccessibilityNodeInfo thisInfo : vrNodes) {
                        Log.w(TAG, "-- Tapping " + thisInfo.getContentDescription() + "/" + thisInfo.getText());
                        gestureTap(thisInfo, CUE_VR_OFF);
                    }
                }

                ArrayList<AccessibilityNodeInfo> fsNodes = connector.collectChildren(rootInActiveWindow, getFullscreenPhrases(), 0);
                if (fsNodes.isEmpty()) {
                    Log.i(TAG, "Already in Full Screen mode!");
                    cuedActions.remove(CUE_FS_ONLY + "");
                    inVR = false;
                } else {
                    tapVideoScreen();
                    for (AccessibilityNodeInfo thisInfo : fsNodes) {
                        Log.w(TAG, "-- Tapping " + thisInfo.getContentDescription() + "/" + thisInfo.getText());
                        boolean success = gestureTap(thisInfo, CUE_FS_ONLY);
                        if (success) {
                            inVR = false;
                        }
                    }
                }

                if (!connector.collectChildren(rootInActiveWindow, getFullscreenPhrases(), 0).isEmpty()) {
                    cuedActions.remove(CUE_FS_ONLY + "");
                }
            }

            if (cuedActions.contains(CUE_VR_ON + "")) {
                Log.i(TAG, "Entering VR mode");
                cuedActions.remove(CUE_PLAY + "");
                cuedActions.remove(CUE_PAUSE + "");
                //can't find the button, try unlocking the screen
                ArrayList<AccessibilityNodeInfo> vrNodes = connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0);
                if (!vrNodes.isEmpty()) {
                    Log.i(TAG, "Already in VR mode");
                    inVR = true;
                    cuedActions.remove(CUE_VR_ON + ""); //don't need to keep looking
                } else {
                    inVR = false;
                    tapVideoScreen();
                    for (AccessibilityNodeInfo thisInfo : connector.collectChildren(rootInActiveWindow, getVROnPhrases(), 0)) {
                        boolean success = gestureTap(thisInfo, CUE_VR_ON);
                        if (success) {
                            inVR = true;
                        }
                    }

                    if (!connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0).isEmpty()) {
                        cuedActions.remove(CUE_VR_ON + "");
                        dismissPopups(rootInActiveWindow);
                    }
                }
            }

            if (cuedActions.contains(CUE_PAUSE + "")) {
//                tapVideoScreen();
                Log.i(TAG, "Attempting Pause");
                //can't find the button, try unlocking the screen
                ArrayList<AccessibilityNodeInfo> playNodes = connector.collectChildren(rootInActiveWindow, getPlayPhrases(), 0);
                playNodes.addAll(connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0));
                if (!playNodes.isEmpty()) {// || inVR) {
                    Log.i(TAG, "Already in PAUSE or VR mode");
                    cuedActions.remove(CUE_PAUSE + ""); //don't need to keep looking
                } else {
                    tapVideoScreen();
                    ArrayList<AccessibilityNodeInfo> pauseNodes = connector.collectChildren(rootInActiveWindow, getPausePhrases(), 0);
                    Log.e(TAG, "GOT PAUSE! " + pauseNodes.size());
                    for (AccessibilityNodeInfo thisInfo : pauseNodes) {
                        gestureTap(thisInfo, CUE_PAUSE);
                    }

                    if (!connector.collectChildren(rootInActiveWindow, getPlayPhrases(), 0).isEmpty()) {
                        cuedActions.remove(CUE_PAUSE + "");
                    }
                }
            }

            if (cuedActions.contains(CUE_PLAY + "")) {
//                tapVideoScreen();
                Log.i(TAG, "Attempting Play");
                //can't find the button, try unlocking the screen
                ArrayList<AccessibilityNodeInfo> pauseNodes = connector.collectChildren(rootInActiveWindow, getPausePhrases(), 0);
                pauseNodes.addAll(connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0));
                //ArrayList<AccessibilityNodeInfo> vrNodes = connector.collectChildren(rootInActiveWindow, getVROffPhrases(), 0);
                if (!pauseNodes.isEmpty()) {// || inVR) {// || !vrNodes.isEmpty()) {
                    Log.i(TAG, "Already in PLAY or VR mode");
                    cuedActions.remove(CUE_PLAY + ""); //don't need to keep looking
                } else {
                    tapVideoScreen();
                    ArrayList<AccessibilityNodeInfo> playNodes = connector.collectChildren(rootInActiveWindow, getPlayPhrases(), 0);
                    Log.e(TAG, "GOT PLAY! " + playNodes.size());
                    for (AccessibilityNodeInfo thisInfo : playNodes) {
                        gestureTap(thisInfo, CUE_PLAY);
                    }

                    if (!connector.collectChildren(rootInActiveWindow, getPausePhrases(), 0).isEmpty()) {
                        cuedActions.remove(CUE_PLAY + "");
                    }
                }
            }
        }
        Log.e(TAG, "CUED ACTIONS NOW: " + cuedActions);// + ", in VR? " + inVR);
    }

    private void tapVideoScreen() {
        Log.d(TAG, "tapVideoScreen: Youtube");
        Point p = new Point();
        main.getWindowManager().getDefaultDisplay().getRealSize(p);

        if (justTesting) {
            Log.e(TAG, "GOT HERE!! [screenTap]");
            return;
        }
        main.tapBounds((p.x / 2), (p.y / 4));
    }

    private boolean gestureTap(AccessibilityNodeInfo thisInfo, int action) {
        if (justTesting) {
            Log.e(TAG, "GOT HERE!! [GT]");
            return false;
        }

        Log.w(TAG, "CLICK CLICK: " + thisInfo.getText() + " // " + thisInfo.getContentDescription() + " // " + cuedActions);

        boolean success = thisInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!success) {
            Log.w(TAG, "CLICK #2");
            Rect r = new Rect();
            thisInfo.getBoundsInScreen(r);
            main.tapBounds(r.centerX(), r.centerY());
        }
        return success;
    }

    private String[] getPlayPhrases() {
        String[] selectedPhrases = new String[2];
        selectedPhrases[0] = "Play";
        selectedPhrases[0] = "Play video";
        return selectedPhrases;
    }

    private String[] getPausePhrases() {
        String[] selectedPhrases = new String[2];
        selectedPhrases[0] = "Pause";
        selectedPhrases[0] = "Pause video";
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
//        selectedPhrases[0] = "Enter full screen"; //used for MiSE8
//        selectedPhrases[1] = "Enter fullscreen"; //used for Redmi7
        selectedPhrases[2] = "back";
        return selectedPhrases;
    }

    private String[] getFullscreenPhrases() {
        String[] selectedPhrases = new String[3];
        selectedPhrases[0] = "Enter full screen"; //used for MiSE8
        selectedPhrases[1] = "Enter fullscreen"; //used for Redmi7
        return selectedPhrases;
    }

    private String pushTitle = "";
    private String pushURL = "";

    public void resetState() {
        //Log.d(TAG, "resetState: ");
        Log.w(TAG, "Clearing CUED ACTIONS!");
        cuedActions.clear();
        pushURL = "";
        pushTitle = "";
        videoPlayStarted = false;
        main.getLumiAccessibilityConnector().gestureInProgress = false;
    }

    public void cueYouTubeAction(String actionStr) {
        //Log.d(TAG, "cueYouTubeAction: ");
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
                    cuedActions.remove(CUE_PLAY + ""); //VR forces autoplay
                    cuedActions.remove(CUE_PAUSE + ""); //VR forces autoplay
                case CUE_VR_OFF:
                    cuedActions.remove(CUE_VR_OFF + "");
                    cuedActions.remove(CUE_VR_ON + "");
                    break;
            }
        }
        //add new action
        cuedActions.add(action + "");
        Log.d(TAG, "CUED ACTIONS: " + cuedActions);

        Log.e(TAG, "LAST ACTIONS Event: " + connector.lastEvent + " Info: " + connector.lastInfo);

        if (connector.lastEvent != null && connector.lastInfo != null) {
            //new Thread(() -> {
            main.backgroundExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    manageYouTubeAccess(connector.lastEvent, connector.lastInfo); //re-try last event
                }
            });

            //}).start();
        }
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
            //"Expand Mini Player"
    };

    private static String[] popupPhrases = {
            "Continue",
            "Dismiss",
            "Skip trial",
            "Skip survey",
            "No Thanks",
            "Cancel auto",
            "Hide related videos",
            "Watch in " //should capture 'Watch in VIEW MASTER VR VIEWER' and similar for other viewers
    };

    private static String[] skipAdsPhrases = {
            "Skip ad",
            "Skip ads"
    };

    private static String[] detectAdsPhrases = {
            "Video will play after ad",
            "Up next in",
            "Visit advertiser"
    };

    private static String[] detectMiniPlayer = {
            "Close miniplayer"
    };

    //test if an ad has started after a video
    private boolean adTest(AccessibilityNodeInfo rootInActiveWindow) {
        ArrayList<AccessibilityNodeInfo> timeNodes = connector.collectChildren(rootInActiveWindow,
                ":", 0);

        String timeA = "";
        String timeB = "";
        //Youtube sometimes spits out a third unrelated number or string in any order
        String timeC = "";

        //before starting video
        if(timeNodes.size() < 2) {
            return false;
        }

        //pop up whilst in VR
        if(inVR) {
            String times = timeNodes.get(1).getText() + "";
            String[] timeSplit = times.split("/");
            if(timeSplit.length < 2) {
                return false;
            }
            timeA = timeSplit[0];
            timeB = timeSplit[1];
        } else {
            timeA = timeNodes.get(0).getText() + "";
            timeB = timeNodes.get(1).getText() + "";
            if(timeNodes.size() > 2) {
                timeC = timeNodes.get(2).getText() + "";
            }
            //returning from one view to another
            if(timeA.contains("Quality")) {
                timeA = "0:00";
            }
            if(timeB.contains("Quality")) {
                timeB = "0:00";
            }
            if(timeC.contains("Quality")) {
                timeC = "0:00";
            }
        }

        String[] timesA = trimToTime(timeA);
        String[] timesB = trimToTime(timeB);
        String[] timesC = trimToTime(timeC);

        //duplicate ads at the start or during
        if(timesA[0].contains("Ad") || timesB[0].contains("Ad") || timesC[0].contains("Ad")) {
            return false;
        }

        int currentTime = Integer.parseInt(timesA[0]) * 60 + Integer.parseInt(timesA[1]);
        int compareTime = Integer.parseInt(timesB[0]) * 60 + Integer.parseInt(timesB[1]);
        int time = currentTime - compareTime;

        if((goalTimeShort.equals("") && time >= -1 && time <= 1)) {
            return true;
        }

        if(timeNodes.size() > 2) {
            int optionalTime = Integer.parseInt(timesC[0]) * 60 + Integer.parseInt(timesC[1]);
            time = compareTime - optionalTime;
        }

        //compare both -1 and 1 as youtube spits timeNodes out in a semi random order
        if((goalTimeShort.equals("") && time >= -1 && time <= 1)) {
            return true;
        }

        return !goalTimeShort.equals("") && (currentTime == compareTime);
    }

    private String[] trimToTime(String time) {
        time = time.replace("/", "");
        time = time.replace(" ", "");
        return time.split(":");
    }

    private void skipAds(AccessibilityNodeInfo rootInActiveWindow) {
        Log.d(TAG, "skipAds: ");
        ArrayList<AccessibilityNodeInfo> popupNodes = connector.collectChildren(rootInActiveWindow, skipAdsPhrases, 0);

        if (videoPlayStarted && !popupNodes.isEmpty() && adTest(rootInActiveWindow)) {
            endOfVideo();
            return;
        }

        for (AccessibilityNodeInfo thisInfo : popupNodes) {
            Log.w(TAG, "Skipping add; " + thisInfo.getText() + ", " + thisInfo.getContentDescription());
            Rect bounds = new Rect();
            thisInfo.getBoundsInScreen(bounds);
            main.tapBounds(bounds.centerX(), bounds.centerY());
        }
    }

    private void alertGuideAdsHaveFinished() {
        Log.d(TAG, "Alerting Guide Ads have finished");

        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.STUDENT_FINISH_ADS + main.getNearbyManager().getID(),
                main.getNearbyManager().getAllPeerIDs());
    }

    boolean videoPlayStarted = false;
    boolean closedMini = false;

    private void closeMiniPlayer(AccessibilityNodeInfo rootInActiveWindow) {
        //Log.d(TAG, "closeMiniPlayer: ");
        if (closedMini) {
            return; //don't launch it twice
        }
        ArrayList<AccessibilityNodeInfo> miniPlayerNodes = connector.collectChildren(rootInActiveWindow, detectMiniPlayer, 0);
        if (!miniPlayerNodes.isEmpty()) {
            Log.d(TAG, "Closing Mini Player");
            closedMini = true;
            //difficult to accurately tap the right button, so just relaunch the video
            main.getWebManager().launchYouTube(pushURL, pushTitle, main.getWebManager().getYouTubeEmbedPlayer().isVROn(), false);
            if (!main.getWebManager().launchingVR) {
                cueYouTubeAction(CUE_PAUSE + "");
                cueYouTubeAction(CUE_VR_OFF + "");
            }
        }
    }

    private void dismissPopups(AccessibilityNodeInfo rootInActiveWindow) {
        //Log.d(TAG, "dismissPopups: ");
        ArrayList<AccessibilityNodeInfo> popupNodes = connector.collectChildren(rootInActiveWindow, popupPhrases, 0);
        for (AccessibilityNodeInfo thisInfo : popupNodes) {
            Log.w(TAG, "Trying to dismiss: " + thisInfo.getText() + ", " + thisInfo.getContentDescription());
            connector.accessibilityClickNode(thisInfo);
        }
    }

    private void endOfVideo() {
        main.runOnUiThread(() -> {
            connector.bringMainToFront();
            main.updateFollowerCurrentTaskToLeadMe();
        });
    }
}
