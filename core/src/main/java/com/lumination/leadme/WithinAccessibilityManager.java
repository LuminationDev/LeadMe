package com.lumination.leadme;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

public class WithinAccessibilityManager {

    private static final String TAG = "WithinAccess";

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

    String[] sequence = {"menu", "All", "Iceland is Melting", "Play"};
    int progress = 0;
    //ArrayList<AccessibilityNodeInfo> cuedTaps = new ArrayList<>();

    private LumiAccessibilityConnector connector;
    private LeadMeMain main;

    public WithinAccessibilityManager(LeadMeMain main, LumiAccessibilityConnector connector) {
        this.connector = connector;
        this.main = main;
    }

    public void manageWithinAccess(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {
        return;

        //recursiveCollect(rootInActiveWindow, 0);

//        ArrayList<AccessibilityNodeInfo> menuNodes = connector.collectChildren(event.getSource(), menuPhrases, 0);
//        if (menuNodes.isEmpty()) {
//            Log.w(TAG, "WITHIN: No nodes for " + sequence[progress] + "!");
////            if(progress < sequence.length) {
////                progress++;
////                manageWithinAccess(event, rootInActiveWindow);
////            }
//        }
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
