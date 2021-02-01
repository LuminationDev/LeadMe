package com.lumination.leadme;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static boolean waitingForStateChange = false;
    private static final String modeVR = "Enter virtual reality mode";
    private static final String modeFS = "Full screen";
    private static final String moreOptions = "More options";
    private static final String watchVR = "Watch in VR";

    //these are not case sensitive, and will return partial matches
    private static String[] keyYouTubePhrases = {
            "Play video",  //also "Pause video"
            modeVR,
            "More options",
            "Watch in VR",
            "Continue",
            "Dismiss",
            "Skip trial",
            "Skip ad",
            "Skip ads",
            "No Thanks",
            "Cancel auto",
            "Autoplay is on",
            //"Move device to explore video",
            "Enter fullscreen",
            //"Exit fullscreen"
            //"More options"
            //"Action menu"
    };

    private int[] getWordyMatches(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);

        int[] vals = new int[6]; //max we'll need
        int val_count = 0;
        while (matcher.find()) {
            vals[val_count] = Integer.parseInt(matcher.group(0));
            val_count++;
        }
        if (val_count == 0) {
            return new int[0];
        }
        return Arrays.copyOfRange(vals, 0, val_count);
    }

    private boolean hasExactMatch(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        boolean foundMatch = matcher.find();
        String match = "";
        if (foundMatch) {
            match = matcher.group(0);
        }
        //if there's a match and the match is the same length as the input string
        //we must have an exact match
        return foundMatch && (match.length() == str.length()); //matcher.find();
    }

    private String durationRegex = "\\d{0,2}:?\\d{1,2}:\\d{2}(?:\\s)*/(?:\\s)*\\d{0,2}:?\\d{1,2}:\\d{2}";
    private String wordyDurationRegex = "(\\d{1,3})(?= hour| second| minute)";
    //"(\\d{1,3} hour(s)* )?(\\d{1,3} minute(s)* )?\\d{1,3} second(s)* of (\\d{1,3} hour(s)* )?(\\d{1,3} minute(s)* )?\\d{1,3} second(s)*";

    private ArrayList<AccessibilityNodeInfo> collectChildren(AccessibilityNodeInfo nodeInfo, String phrase, int level) {
        String[] tmp = {phrase};
        return collectChildren(nodeInfo, tmp, level);
    }

    private ArrayList<AccessibilityNodeInfo> collectChildren(AccessibilityNodeInfo nodeInfo, String[] phrases, int level) {
        ArrayList<AccessibilityNodeInfo> infoArrayList = new ArrayList<>();
        if (nodeInfo == null) {
            return infoArrayList;
        }

        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo v1 = nodeInfo.getChild(i);
            if (v1 == null) {
                continue;
            }
            if (v1.getChildCount() > 0)
                infoArrayList.addAll(collectChildren(v1, phrases, (level + 1)));

            if (v1.getText() == null && v1.getContentDescription() == null) {
                continue; //nothing useful here, move to the next
            }

            String testForDur = "" + v1.getText();
            String testForDurDesc = "" + v1.getContentDescription();
            String searchStr = testForDur + " // " + testForDurDesc;
            Log.d(TAG, level + " || " + searchStr + " || " + v1.isFocused() + ", " + v1.isEnabled() + ", " + v1.isClickable());

            for (String phrase : phrases) {
                if (searchStr.contains(phrase)) {
                    Log.d(TAG, "\tContains " + phrase + "!");
                    infoArrayList.add(v1); //contains at least one desired phrase, collect it
                    break;
                }
            }

            //we want elapsed time, not remaining time
            if (hasExactMatch(testForDur, durationRegex) || (!testForDurDesc.contains("remaining") && getWordyMatches(testForDurDesc, wordyDurationRegex).length > 0)) {
                Log.d(TAG, "\tContains time info! " + testForDur + " // " + testForDurDesc);
                //infoArrayList.add(v1);
                break;
            }

        }
        return infoArrayList;
    }

    private void clickNode(AccessibilityNodeInfo thisNode) {
        boolean success = thisNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!success) {
            Rect bounds = new Rect();
            thisNode.getBoundsInScreen(bounds);
            Log.e(TAG, "CLICK FAILED FOR " + thisNode.getText() + " // " + thisNode.getContentDescription() + "! Trying something new. " + bounds);
            main.tapBounds(bounds);
        } else {
            Log.e(TAG, "CLICK SUCCESS FOR " + thisNode.getText() + " // " + thisNode.getContentDescription() + "!");
        }
    }

    private void manageYouTubeAccess(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {
        Log.d(TAG, "----");
        if (main.getWebManager().isFreshPlay() &&
                (main.getWebManager().getLaunchTitle() == null || rootInActiveWindow.findAccessibilityNodeInfosByText(main.getWebManager().getLaunchTitle()).size() == 0)) {
            Log.d(TAG, "Give the newest video time to load! " + main.getWebManager().getLaunchTitle());
            ArrayList<AccessibilityNodeInfo> pauseNodes = collectChildren(rootInActiveWindow, "back", 0);
            for (AccessibilityNodeInfo thisInfo : pauseNodes) {
                clickNode(thisInfo);
                Log.d(TAG, "EXIT LAST MODE!");
            }
            //return;
        } else if (main.getWebManager().isFreshPlay()) {
            //requesting to play this video again, so hit replay
            ArrayList<AccessibilityNodeInfo> pauseNodes = collectChildren(rootInActiveWindow, "Replay", 0);
            for (AccessibilityNodeInfo thisInfo : pauseNodes) {
                clickNode(thisInfo);
                Log.d(TAG, "REPLAYING VIDEO!");
            }
        } else if (!main.getWebManager().isFreshPlay()) {
            //we've finished this video
//            ArrayList<AccessibilityNodeInfo> pauseNodes = collectChildren(rootInActiveWindow, "Replay", 0);
//            if (pauseNodes.size() > 0) {
//                //then return to LeadMe
//                main.updateFollowerCurrentTaskToLeadMe();
//                main.recallToLeadMe();
//                return;
//            }
        }

        String[] testNodes = {"Move device to explore video", "Cancel"};
        ArrayList<AccessibilityNodeInfo> moveDeviceNodes = collectChildren(rootInActiveWindow, testNodes, 0);
        if (moveDeviceNodes.size() >= 2) { //need at least one of each
            Log.e(TAG, "MOVE DEVICE! Focus issue");
            for (AccessibilityNodeInfo thisNode : moveDeviceNodes) {
                String testStr = thisNode.getText() + " " + thisNode.getContentDescription();
                if (!(testStr.contains("Video player") && main.getWebManager().getLaunchTitle() != null)) {
                    clickNode(thisNode);
                }
            }
        }


        ArrayList<AccessibilityNodeInfo> foundNodes = collectChildren(rootInActiveWindow, keyYouTubePhrases, 0);
        Log.d(TAG, "GOT " + foundNodes.size() + " found nodes!!");

        if (foundNodes.size() > 0) {
            main.getWebManager().setFreshPlay(false);
            for (AccessibilityNodeInfo node : foundNodes) {
                String testStr = "" + node.getText();
                String testStrDesc = "" + node.getContentDescription();

                boolean videoFinished = calculateVideoElapsedTime(testStr, testStrDesc);

                //if the video is not finished yet, we know this is in progress
                //so it's not a fresh launch
                if (!videoFinished) {
                    main.getWebManager().setFreshPlay(false); //time has now elapsed
                }

                if (main.getWebManager().isFreshPlay()) {
                    ArrayList<AccessibilityNodeInfo> exitNodes = collectChildren(rootInActiveWindow, "Replay video", 0);
                    for (AccessibilityNodeInfo thisInfo : exitNodes) {
                        clickNode(thisInfo);
                        Log.d(TAG, "REPLAYING VID!");
                    }
                } else if (videoFinished) {
                    //if the video is done, attempt to pause it
                    ArrayList<AccessibilityNodeInfo> exitNodes = collectChildren(rootInActiveWindow, "back", 0);
                    for (AccessibilityNodeInfo thisInfo : exitNodes) {
                        clickNode(thisInfo);
                        Log.d(TAG, "EXITING VR!");
                    }

                    ArrayList<AccessibilityNodeInfo> pauseNodes = collectChildren(rootInActiveWindow, "Pause", 0);
                    for (AccessibilityNodeInfo thisInfo : pauseNodes) {
                        clickNode(thisInfo);
                        Log.d(TAG, "PAUSING VIDEO!");
                    }
                    //then return to LeadMe
                    main.updateFollowerCurrentTaskToLeadMe();
                    main.recallToLeadMe();
                    return; //return here so we don't press play!

                } else {
                    clickNode(node);
                }
            }
            return;
        }

        //click all the buttons!
        //Log.d(TAG, "Clicking all the buttons! " + foundNodes.size());
        for (
                AccessibilityNodeInfo accessibilityNodeInfo : foundNodes) {
            //if(accessibilityNodeInfo.getClassName() != null && accessibilityNodeInfo.getText() != null) {
            if (accessibilityNodeInfo.getText() != null
                    && accessibilityNodeInfo.getText().toString().equals("You can skip ad in 0s")) {
                continue; //we don't need to click this one
            }

            Log.d(TAG, "VR MODE: " + main.getWebManager().enteredVR + ", " + main.getWebManager().launchingVR + " // " + accessibilityNodeInfo.getContentDescription() + " vs " + modeVR);

            //manage VR mode
            if (main.getWebManager().launchingVR && accessibilityNodeInfo.getContentDescription() != null && accessibilityNodeInfo.getContentDescription().toString().equals(modeVR)) {
                main.getWebManager().enteredVR = true;
                clickNode(accessibilityNodeInfo); //do click, we want VR mode on
                Log.w(TAG, "Clicked 'Enter VR mode' " + main.getWebManager().enteredVR);
                continue;

            } else if (!main.getWebManager().launchingVR && accessibilityNodeInfo.getContentDescription() != null
                    && accessibilityNodeInfo.getContentDescription().toString().equals(modeVR)) {
                main.getWebManager().enteredVR = true;
                continue; //don't click, we don't want to enter VR mode

            } else if (!main.getWebManager().enteredVR && accessibilityNodeInfo.getContentDescription() != null && accessibilityNodeInfo.getContentDescription().toString().equals(modeFS)) {
                //modeFS
                clickNode(accessibilityNodeInfo); //do click, we want VR mode on
                Log.w(TAG, "Clicked 'Full Screen");
                continue;
            }

            //manage everything else
            Log.w(TAG, "Clicked " + accessibilityNodeInfo.getText() + " // " + accessibilityNodeInfo.getContentDescription());
            clickNode(accessibilityNodeInfo);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

//        //nothing else interesting happened, but we should be entering VR mode
//        if (!main.getWebManager().enteredVR && main.getWebManager().launchingVR && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
//            Log.d(TAG, "TAP: " + main.getWebManager().enteredVR + ", " + main.getWebManager().launchingVR + ", " + rootInActiveWindow.getChildCount());
//            main.tapScreenMiddle();
//
//        }

    }


    public void bringMainToFront() {
        main.getHandler().post(() -> {
            if (main != null && !main.appHasFocus) {
                main.recallToLeadMe();
            }
        });
    }

    private boolean calculateVideoElapsedTime(String text, String contentDesc) {
        int[] wordyMatches = getWordyMatches(contentDesc, wordyDurationRegex);
        int totalDurationA = 0;
        int totalDurationB = 0;

        Log.d(TAG, "GOT NOW -- " + wordyMatches.length + " for " + contentDesc + " (or this " + text + ")");
        if (wordyMatches.length == 6) {
            totalDurationA = (wordyMatches[0] * 60 * 60) +    //hours
                    (wordyMatches[1] * 60) +         //minutes
                    wordyMatches[2];                //seconds

            totalDurationB = (wordyMatches[3] * 60 * 60) +    //hours
                    (wordyMatches[4] * 60) +         //minutes
                    wordyMatches[5];                //seconds

        } else if (wordyMatches.length == 4) {
            totalDurationA = (wordyMatches[0] * 60) +         //minutes
                    wordyMatches[1];                //seconds

            totalDurationB = (wordyMatches[2] * 60) +         //minutes
                    wordyMatches[3];                //seconds

        } else if (text.contains("/") && hasExactMatch(text, durationRegex)) {
            String[] durSplit = text.split("/");
            String[] timeA = durSplit[0].split(":");
            String[] timeB = durSplit[1].split(":");

            if (timeA.length == 3) { //includes hours
                totalDurationA = (Integer.parseInt(timeA[0]) * 60 * 60) +    //hours
                        (Integer.parseInt(timeA[1]) * 60) +         //minutes
                        Integer.parseInt(timeA[2]);                 //seconds

                totalDurationB = (Integer.parseInt(timeB[0]) * 60 * 60) +    //hours
                        (Integer.parseInt(timeB[1]) * 60) +         //minutes
                        Integer.parseInt(timeB[2]);                 //seconds

            } else if (timeA.length == 2) { //includes minutes
                totalDurationA = (Integer.parseInt(timeA[0]) * 60) +         //minutes
                        Integer.parseInt(timeA[1]);                 //seconds

                totalDurationB = (Integer.parseInt(timeB[0]) * 60) +         //minutes
                        Integer.parseInt(timeB[1]);                 //seconds
            }
        }

        boolean videoFinished = false;
        if (totalDurationA > 0) {
            int threshold = 1; //how many seconds to the end do we count as finished
            videoFinished = totalDurationA > 0 && totalDurationA >= (totalDurationB - threshold);
        }

        Log.d(TAG, "ELAPSED TIME: " + totalDurationA + "s of " + totalDurationB + "s (" + videoFinished + ", " + main.getWebManager().isFreshPlay() + ")");
        return videoFinished;

    }

    boolean showDebugMsg = false;

    private AccessibilityEvent lastEvent = null;
    private AccessibilityNodeInfo lastInfo = null;

    public boolean manageAccessibilityEvent(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {

        if (main == null) {
            return false;
        }

        if (main.getNearbyManager().isConnectedAsGuide()) {
            //check details for video controller
//            Log.w(TAG, "GUIDE]] Managing received AccessibilityEvent: " + event);
//            if(event != null && event.getSource() != null && event.getSource().getPackageName().toString().contains("youtube"))
//            {
//                manageYouTubeAccess(event, rootInActiveWindow);
//            }
            return true;
        }


        if (event == null && rootInActiveWindow == null) {
            if (lastEvent != null && lastInfo != null) {
                event = lastEvent;
                rootInActiveWindow = lastInfo;

            } else {
                return false; //nothing we can do!
            }
        } else {
            lastEvent = event;
            lastInfo = rootInActiveWindow;
        }

        //after this point is for client-specific behaviours only
        if (main.isGuide || !main.getNearbyManager().isConnectedAsFollower()) {
            return false;
        }

        try {
            if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                //if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                Log.e(TAG, "SOMETHING! " + event.getPackageName() + ", " + event.getClassName() + ", " + event.getText() + ", " + event.getAction());
                Log.e(TAG, "SOURCE! >>>  " + event.getSource());

                if (event.getText().contains("back") && main.getWebManager().launchingVR) {
                    main.getWebManager().enteredVR = false; //reset this
                }

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

    protected void triageReceivedIntent(Intent intent) {
        //Log.i(TAG, "Triaging the intent! " + intent + ", " + intent.getStringExtra(LumiAccessibilityService.INFO_TAG) + ", have main? " + main + ", " + main.getLifecycle().getCurrentState());

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
