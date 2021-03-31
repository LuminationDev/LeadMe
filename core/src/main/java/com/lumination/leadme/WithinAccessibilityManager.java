package com.lumination.leadme;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

public class WithinAccessibilityManager {

    private static final String TAG = "WithinAccess";

    private static final int MODE_STREAM = 0;
    private static final int MODE_DOWNLOAD = 1;
    private static final int MODE_DEFAULT = 0; //stream by default

    private static final int VIEW_VR = 0;
    private static final int VIEW_PHONE = 1;
    private static final int VIEW_DEFAULT = 0; //vr by default

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


    boolean initTapLocations = false;

    ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    public int firstTime = -1, finishTime = -1;
    boolean checkAgain = false;
    boolean scheduled = false;
    boolean checkingForFinish = false;
    ScheduledFuture schedTask;

    AccessibilityEvent lastEvent;


    public WithinAccessibilityManager(LeadMeMain main, LumiAccessibilityConnector connector) {
        Log.d(TAG, "WithinAccessibilityManager: ");
        this.connector = connector;
        this.main = main;
        scheduledExecutor.setRemoveOnCancelPolicy(true);
    }

    private boolean screenContainsPhrases(AccessibilityNodeInfo rootInActiveWindow, String[] targetPhrases, String[] exclusionPhrases) {
        Log.d(TAG, "screenContainsPhrases: ");
        ArrayList<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
        for (String phrase : targetPhrases) {
            foundNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText(phrase));
        }

        ArrayList<AccessibilityNodeInfo> iteratorList = new ArrayList<>();
        iteratorList.addAll(foundNodes); //make a copy
        for (AccessibilityNodeInfo thisNode : iteratorList) {
            String searchStr = (thisNode.getText() + " // " + thisNode.getContentDescription()).toLowerCase();
            Log.d(TAG, "Testing " + searchStr);
            //firstTime=-1;
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
    //Rect(313, 2880 - 451, 2142) and Rect(599, 2744 - 796, 2142)
    //actual center is Y = 1310

    //2236 diff T
    //603 diff B

    //REDMI         Rect(3787 - 2214) and Rect(3787 - 2214)         // ATTEMPTING TAP! 401, 1473 // 1510 // 1573
    //Lineage       Rect(3227 - 2142) and Rect(3227 - 2142)         // ATTEMPTING TAP! 382, 985  // 1330 // 1085 (base - 800)

    private void extractTapLocations(AccessibilityNodeInfo rootInActiveWindow) {
        Log.d(TAG, "extractTapLocations: ");
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG, "extractTapLocations: thread won't sleep");
        }
        ArrayList<AccessibilityNodeInfo> playNodes = new ArrayList<>();
        playNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("Play"));
        playNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("Stream"));

        ArrayList<AccessibilityNodeInfo> downloadNodes = new ArrayList<>();
        downloadNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("Download"));
        downloadNodes.addAll(rootInActiveWindow.findAccessibilityNodeInfosByText("Delete"));
        Log.e(TAG, "Searching for tap locations! " + playNodes.size() + ", " + downloadNodes.size());

        if (!playNodes.isEmpty()) {
            playNodes.get(0).getBoundsInScreen(leftRect);
            downloadNodes.get(0).getBoundsInScreen(rightRect);
            Log.w(TAG, "[1] Found " + leftRect + " and " + rightRect);

            // this view is a whole screen length below the main view, so compensate by subtracting rect.bottom
            // then compensate for us finding the LABEL but not the CLICKABLE IMAGE and move it another 200
            Log.d(TAG, "extractTapLocations: changing " + leftRect.top + " and " + leftRect.bottom + " to " + (leftRect.top + 100) + " and " + (leftRect.bottom - 780));
//            if(leftRect.bottom>2000) {
            //leftRect.top = leftRect.bottom - 780;
            leftRect.top = leftRect.bottom - 400;
            leftRect.bottom = leftRect.top + 400;
            rightRect.top = leftRect.top; //same height
            rightRect.bottom = leftRect.bottom; //same height
//            }else{
//                
//            }

            initTapLocations = true;
            return;
        }
    }

    int lastDelay = -1;
    int concurrentDelays = 0;

    public void manageWithinAccess(AccessibilityEvent event, AccessibilityNodeInfo rootInActiveWindow) {
        Log.d(TAG, "manageWithinAccess: " + currentView + " == " + VIEW_VR + ", " + checkingForFinish);
        lastEvent = event;

        if (main.getLumiAccessibilityConnector().gestureInProgress) {
            Log.i(TAG, "WAITING FOR LAST GESTURE");
            return;
        }

        if (event != null) {
            Log.e(TAG, "WITHIN A] " + AccessibilityEvent.eventTypeToString(event.getEventType()) + ", " + event.getClassName() + " // " + rootInActiveWindow.getClassName() + ", " + event.getAction() + "\n"
                    + event.getSource() + "\n"
                    + rootInActiveWindow);
        } else {
            Log.d(TAG, "WITHIN B] " + AccessibilityEvent.eventTypeToString(event.getEventType()) + ", " + event.getClassName() + " // " + rootInActiveWindow.getClassName() + ", " + event.getAction() + "\n"
                    + event + "\n " + rootInActiveWindow);
        }

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

        String[] exclusionPhrasesEmpty = {};

        String[] targetPhrasesA = {"Play"};
        String[] targetPhrasesB = {"Replay"};
        String[] exclusionPhrases1 = {"Play"};
        //this should indicate end of video in VR MODE ONLY
        if (screenContainsPhrases(rootInActiveWindow, targetPhrasesA, exclusionPhrasesEmpty) && screenContainsPhrases(rootInActiveWindow, targetPhrasesB, exclusionPhrases1)) {
            endOfVideo();
            return;
        }

        if (!initTapLocations) {
            extractTapLocations(rootInActiveWindow);
        }

        List<AccessibilityNodeInfo> collectedInfo = rootInActiveWindow.findAccessibilityNodeInfosByText(":");

        //do alternate search if first failed
        if (collectedInfo.isEmpty() && event != null && event.getSource() != null) {
            collectedInfo.addAll(event.getSource().findAccessibilityNodeInfosByText(":"));
        }

        Log.i(TAG, "Got " + collectedInfo.size() + " with : in them");

        if (!collectedInfo.isEmpty()) {
            firstTime = -1; //reset each time we collect times

            for (AccessibilityNodeInfo info : collectedInfo) {
                //Log.i(TAG, "\t" + info.getText() + " // " + info.getContentDescription());
                int tmp = -1;
                try {
                    List<String> time = Arrays.asList(info.getText().toString().split(":"));
                    for (int i = time.size() - 1; i >= 0; i--) {
                        if (i == time.size() - 1) {
                            tmp = Integer.parseInt(time.get(i));
                        } else if (i == time.size() - 2) {
                            tmp += Integer.parseInt(time.get(i)) * 60;
                        } else if (i == time.size() - 3) {
                            tmp += Integer.parseInt(time.get(i)) * 60 * 60;
                        } else if (i == time.size() - 4) {
                            tmp += Integer.parseInt(time.get(i)) * 60 * 60 * 24;
                        } else if (i == time.size() - 5) {
                            tmp += Integer.parseInt(time.get(i)) * 60 * 60 * 24 * 7;
                        }
                    }

                    if (finishTime < tmp && tmp < 9000) {
                        finishTime = tmp;
                    }

                    if (firstTime == -1 && tmp < 9000) {
                        //Log.d(TAG, "manageWithinAccess: setting firstTime to: " + tmp);
                        firstTime = tmp;
                        continue; //got a time, now move to next entry to compare
                    }

                    //Log.d(TAG, "manageWithinAccess: got times: " + firstTime + " vs " + finishTime);

                    //necessary for managing video end times in VR MODE
                    if (currentView == VIEW_VR && firstTime > 0 && finishTime >= firstTime) {
                        scheduled = true;
                        int delay = finishTime - firstTime;
                        if (lastDelay == delay) {
                            concurrentDelays++;
                        } else {
                            concurrentDelays = 0;
                            lastDelay = delay;
                        }

                        if (schedTask != null) {
                            Log.w(TAG, "Cancelling scheduled VR exit.");
                            //cancel previous task, ready for updated time
                            schedTask.cancel(true);
                            scheduledExecutor.purge();
                        }

                        if (delay > 0 && concurrentDelays > 4) {
                            Log.w(TAG, "Might be paused! Not scheduling. Concurrent: " + concurrentDelays);
                            scheduled = false;
                            tapAndSchedule();

                        } else {
                            Log.w(TAG, "Scheduling! Will run in " + delay + " milliseconds. Concurrent: " + concurrentDelays);
                            scheduledExecutor.setCorePoolSize(1);
                            schedTask = scheduledExecutor.schedule(() -> {
                                // do a thing
                                exitVRvideo();
                            }, delay, SECONDS);
                            Log.w(TAG, "Is it scheduled? ");//+schedTask.isCancelled()+", "+schedTask.isDone()+", "+schedTask.getDelay(SECONDS));
                        }
                    }

                    //necessary for managing video end times in PHONE MODE
                    if (tmp > -1 && tmp >= firstTime - 2 && tmp <= firstTime + 2) {
                        //Log.d(TAG, "manageWithinAccess: tmp: " + tmp + " firstTime: " + firstTime + ", finish: " + finishTime);
                        //the video is finished!
                        endOfVideo();
                    }

                } catch (Exception e) {
                    //not the time string we're looking for
                    continue;
                }
            }
        }

        //check for this first, before checking for stream/download
        String[] targetPhrases1 = {"Play"};
        String[] exclusionPhrases0 = {"Replay"};
        if (screenContainsPhrases(rootInActiveWindow, targetPhrases1, exclusionPhrases0)) {
            Log.w(TAG, "[PLAY] Phrases are present! " + currentView);
            main.tapBounds(leftRect.centerX(), leftRect.centerY());
            main.getAppManager().videoInit = true;

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            finishTime = -1;
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
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finishTime = -1;
            return;
        }

        String[] targetPhrases2 = {"VIEW IN VR"};
        if (main.getAppManager().videoInit && screenContainsPhrases(rootInActiveWindow, targetPhrases2, exclusionPhrasesEmpty)) {
            Log.w(TAG, "[MODE] Phrases are present! " + currentView);
            if (currentView == VIEW_PHONE) {
                main.tapBounds(rightRect.centerX(), rightRect.centerY());
            } else {
                main.tapBounds(leftRect.centerX(), leftRect.centerY());
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tapAndSchedule();

            finishTime = -1;
        }
    }


    boolean hasTapped = false;
    boolean hasTexted = false;

    private ArrayList<AccessibilityNodeInfo> recursiveCollect(AccessibilityNodeInfo nodeInfo, int level) {
        Log.d(TAG, "recursiveCollect: ");
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

            if (v1.getChildCount() > 0) {
                infoArrayList.addAll(recursiveCollect(v1, (level + 1)));
            }


//            if (v1.getText() == null && v1.getContentDescription() == null) {
//                continue; //nothing useful to print here, move to the next
//            }


            String testForDur = "" + v1.getText();
            String testForDurDesc = "" + v1.getContentDescription();
            String searchStr = testForDur + " // " + testForDurDesc;
            Log.d(TAG, level + " || " + v1.getClassName() + " || " + searchStr);

        }
        Log.d(TAG, "---------------------");
        return infoArrayList;
    }

    private void exitVRvideo() {
        AccessibilityService service = main.getAccessibilityService();
        Log.w(TAG, "CHECK VR FINISHED?? " + service.getRootInActiveWindow().getClassName() + " == " + lastEvent.getClassName() + ", " + checkingForFinish + ", " + scheduled);
        endOfVideo();
        checkingForFinish = false; //prepare to check again
        scheduled = false; //if not found, might need to schedule again
    }

    private void endOfVideo() {
        Log.w(TAG, "The video is done!");
        cleanUpVideo();
        main.runOnUiThread(() -> main.updateFollowerCurrentTaskToLeadMe());
        connector.bringMainToFront();
    }

    public void cleanUpVideo() {
        if (schedTapTask != null) {
            schedTapTask.cancel(true);
        }

        if (schedTask != null) {
            schedTask.cancel(true);
        }

        scheduledExecutor.purge();
    }

    // every 15 seconds, tap the screen and check again
    // if not already scheduled to exit at end of video
    ScheduledFuture schedTapTask;

    private void tapAndSchedule() {
        Log.w(TAG, "TAP AND SCHED! " + scheduled);
        if (!scheduled) {
            main.tapBounds(250, 250);
            if (schedTapTask != null) {
                Log.w(TAG, "Cancelling scheduled tap.");
                //cancel previous task, ready for updated time
                schedTapTask.cancel(true);
                scheduledExecutor.purge();
            }
//            scheduledExecutor.setCorePoolSize(1);
//            Log.w(TAG, "Scheduling next tap.");
//            schedTapTask = scheduledExecutor.schedule(() -> {
//                tapAndSchedule();
//            }, 15, SECONDS);
        }
    }

}
