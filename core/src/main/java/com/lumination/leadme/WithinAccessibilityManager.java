package com.lumination.leadme;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class WithinAccessibilityManager {

    private static final String TAG = "WithinAccess";

    private static final int MODE_STREAM = 0;
    private static final int MODE_DOWNLOAD = 1;
    private static final int MODE_DEFAULT = 0; //stream by default

    private static final int STATE_UNSTARTED = 0;
    private static final int STATE_READY = 1;
    private static final int STATE_PLAYING = 2;

    private static final int VIEW_VR = 0;
    private static final int VIEW_PHONE = 1;
    private static final int VIEW_DEFAULT = 0; //vr by default

    private int currentState = STATE_UNSTARTED;
    private int currentMode = MODE_DEFAULT;
    private int currentView = VIEW_DEFAULT;

    String[] menuPhrases = {
            "Featured",
            "Animated",
            "Music",
            "Documentary",
            "Horror",
            "Experimental",
            "The Archives",
            "All",
            "Downloaded"
    };


    private LumiAccessibilityConnector connector;
    private LeadMeMain main;

    public WithinAccessibilityManager(LeadMeMain main, LumiAccessibilityConnector connector) {
        this.connector = connector;
        this.main = main;
    }

    private boolean screenContainsPhrases(AccessibilityNodeInfo rootInActiveWindow, String[] targetPhrases, String[] exclusionPhrases) {
        boolean containsPhrases = false;
        ArrayList<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
        for (String phrase : targetPhrases) {
            foundNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText(phrase));
        }

        ArrayList<AccessibilityNodeInfo> iteratorList = new ArrayList<>();
        iteratorList.addAll(foundNodes); //make a copy
        for (AccessibilityNodeInfo thisNode : iteratorList) {
            String searchStr = (thisNode.getText() + " // " + thisNode.getContentDescription()).toLowerCase();
            Log.d(TAG, "Testing " + searchStr);
            for (String exc : exclusionPhrases) {
                if (searchStr.contains(exc.toLowerCase())) {
                    Log.d(TAG, "Nope, remove this one! " + searchStr);
                    foundNodes.remove(thisNode);
                    continue;
                }
            }
        }

        return !foundNodes.isEmpty();
    }

    Rect leftRect = new Rect(285, 1309, 505, 1529); //play
    Rect rightRect = new Rect(550, 1309, 750, 1529); //downloads

    // Rect(297, 1401 - 507, 1611) //LEFT tapped
    // Rect(573, 1401 - 783, 1611); //RIGHT tapped

    //      L     T     R    B
    // Rect(365, 3637 - 439, 2214) //LEFT label
    // Rect(590, 3637 - 766, 2214) //RIGHT label

    //WHOLE SCREEN: Rect(0, 0 - 1080, 2214)

    //2236 diff T
    //603 diff B

    private void extractTapLocations(AccessibilityNodeInfo rootInActiveWindow) {
        List<AccessibilityNodeInfo> playNodes = rootInActiveWindow.findAccessibilityNodeInfosByText("Play");
        List<AccessibilityNodeInfo> streamNodes = rootInActiveWindow.findAccessibilityNodeInfosByText("Stream");
        if (!playNodes.isEmpty()) {
            List<AccessibilityNodeInfo> deleteNodes = rootInActiveWindow.findAccessibilityNodeInfosByText("Delete");
            Log.w(TAG, "[1] Found " + playNodes.size() + " and " + deleteNodes.size());
            playNodes.get(0).getBoundsInScreen(leftRect);
            deleteNodes.get(0).getBoundsInScreen(rightRect);

            // this view is a whole screen length below the main view, so compensate by subtracting rect.bottom
            // then compensate for us finding the LABEL but not the CLICKABLE IMAGE and move it another 200
            leftRect.top -= (leftRect.bottom + 200);
            rightRect.top -= (rightRect.bottom + 200);
            leftRect.bottom = leftRect.top + 200;
            rightRect.bottom = rightRect.top + 200;

            initTapLocations = true;
            return;

        } else if (!streamNodes.isEmpty()) {
            List<AccessibilityNodeInfo> downloadNodes = rootInActiveWindow.findAccessibilityNodeInfosByText("Download");
            Log.w(TAG, "[2] Found " + streamNodes.size() + " and " + downloadNodes.size());
            streamNodes.get(0).getBoundsInScreen(leftRect);
            downloadNodes.get(0).getBoundsInScreen(rightRect);

            // this view is a whole screen length below the main view, so compensate by subtracting rect.bottom
            // then compensate for us finding the LABEL but not the CLICKABLE IMAGE and move it another 200
            leftRect.top -= (leftRect.bottom + 200);
            rightRect.top -= (rightRect.bottom + 200);
            leftRect.bottom = leftRect.top + 200;
            rightRect.bottom = rightRect.top + 200;

            initTapLocations = true;
        }
    }

    boolean initTapLocations = false;

    public void manageWithinAccess(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {

        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            Log.e(TAG, "WITHIN] " + event.getSource() + ", " + rootInActiveWindow);
        } else {
            Log.d(TAG, "WITHIN] " + event + ", " + rootInActiveWindow);
        }

        Log.d(TAG, "Current state is: " + currentState);

        if (main.getAppManager().getIsWithinStreaming()) {
            currentMode = MODE_STREAM;
        } else {
            currentMode = MODE_DOWNLOAD;
        }

        if (main.getAppManager().getIsWithinVRMode()) {
            currentView = VIEW_VR;
        } else {
            currentView = VIEW_PHONE;
        }

        String[] targetPhrasesA = {"Play"};
        String[] targetPhrasesB = {"Replay"};
        String[] exclusionPhrases0 = {};
        if (/*currentView == VIEW_VR &&*/ screenContainsPhrases(rootInActiveWindow, targetPhrasesA, exclusionPhrases0) && screenContainsPhrases(rootInActiveWindow, targetPhrasesB, exclusionPhrases0)) {
            Log.w(TAG, "Contains PLAY and REPLAY -- must be end of video!");
            connector.bringMainToFront();
        }

        if (!initTapLocations) {
            extractTapLocations(rootInActiveWindow);
        }

        List<AccessibilityNodeInfo> collectedInfo = rootInActiveWindow.findAccessibilityNodeInfosByText(":");
        Log.e(TAG, "Got " + collectedInfo.size() + " with : in them");
        for (AccessibilityNodeInfo info : collectedInfo) {
            Log.e(TAG, "\t" + info.getText() + " // " + info.getContentDescription());
        }

        //check for this first, before checking for stream/download
        String[] targetPhrases1 = {"Play"};
        String[] exclusionPhrases1 = {};
        if (screenContainsPhrases(rootInActiveWindow, targetPhrases1, exclusionPhrases1)) {
            Log.w(TAG, "[PLAY] Phrases are present! " + currentView);
            main.tapBounds(leftRect.centerX(), leftRect.centerY());
            main.getAppManager().videoInit = true;
            return;
        }

        String[] targetPhrases = {"Stream", "Download"};
        String[] exclusionPhrases = {"YOU HAVEN'T DOWNLOADED", "Streaming", "Preparing"};
        if (screenContainsPhrases(rootInActiveWindow, targetPhrases, exclusionPhrases)) {
            Log.w(TAG, "[STREAM/DOWNLOAD] Phrases are present! " + currentMode);
            if (currentMode == MODE_DOWNLOAD) {
                main.tapBounds(rightRect.centerX(), rightRect.centerY());
            } else {
                main.tapBounds(leftRect.centerX(), leftRect.centerY());
            }
            main.getAppManager().videoInit = true;
            return;
        }

        String[] targetPhrases2 = {"VIEW IN VR"};
        String[] exclusionPhrases2 = {};
        if (main.getAppManager().videoInit && screenContainsPhrases(rootInActiveWindow, targetPhrases2, exclusionPhrases2)) {
            Log.w(TAG, "[MODE] Phrases are present! " + currentView);
            if (currentView == VIEW_PHONE) {
                main.tapBounds(rightRect.centerX(), rightRect.centerY());
            } else {
                main.tapBounds(leftRect.centerX(), leftRect.centerY());
            }
        }
    }


    boolean hasTapped = false;
    boolean hasTexted = false;

    private ArrayList<AccessibilityNodeInfo> recursiveCollect(AccessibilityNodeInfo nodeInfo, int level) {
        ArrayList<AccessibilityNodeInfo> infoArrayList = new ArrayList<>();
        if (nodeInfo == null) {
            return infoArrayList;
        }

        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo v1 = nodeInfo.getChild(i);
            if (v1 == null) {
                continue;
            }

            infoArrayList.add(v1);

            if (v1.getClassName().toString().contains("ImageView")) {
                Rect r = new Rect();
                v1.getBoundsInParent(r);
                Log.w(TAG, "Where am I? " + r + ", " + r.top + " " + r.left + " " + r.bottom + ", " + r.right);

                if (r.top == 0 && r.left == 0 && r.bottom <= 91 && r.right <= 91) {
                    if (!hasTapped) { //TODO think this gets activated before we're ready
                        connector.gestureTapNode(v1);
                        hasTapped = true;
                    } else if (!hasTexted) {
                        // ENTER TEXT IN FIELD
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Iceland is Melting");
                        v1.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT.getId(), arguments);
                        hasTexted = true;
                    }
                }
            }

            if (v1.getChildCount() > 0)
                infoArrayList.addAll(recursiveCollect(v1, (level + 1)));
            if (v1.getText() == null && v1.getContentDescription() == null) {
                continue; //nothing useful to print here, move to the next
            }

            String testForDur = "" + v1.getText();
            String testForDurDesc = "" + v1.getContentDescription();
            String searchStr = testForDur + " // " + testForDurDesc;
            Log.d(TAG, level + " || " + v1.getClassName() + " || " + searchStr);

        }
        Log.d(TAG, "---------------------");
        return infoArrayList;
    }


}
