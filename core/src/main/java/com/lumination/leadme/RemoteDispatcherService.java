package com.lumination.leadme;

import android.accessibilityservice.AccessibilityService;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.connection.Payload;

import java.util.List;
import java.util.Set;

public class RemoteDispatcherService extends AccessibilityService {
    //handler for executing on the main thread
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private static LeadMeMain main;
    private String lastAppName, lastPackageName;

    private final String TAG = "RemoteDispatch";

    private static RemoteDispatcherService INSTANCE = new RemoteDispatcherService();

    private void attachMain(LeadMeMain m) {
        main = m;
    }

    public static RemoteDispatcherService getInstance(LeadMeMain m) {
        main = m;
        INSTANCE.attachMain(m);
        return INSTANCE;
    }

    private boolean init = false;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service CREATED! " + main);
        init = true;

        OrientationEventListener mOrientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
//                if (orientation == 0 || orientation == 180) {
//                    Log.d(TAG, "portrait");
//                } else if (orientation == 90 || orientation == 270) {
//                    Log.d(TAG, "landscape");
//                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (main != null && main.overlayView != null) {
                            main.overlayView.invalidate();
                            //main.overlayView.requestLayout();
                        }
                    }
                });
            }
        };

        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "CHANGE!");
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                main.refreshOverlay();
            }
        });
    }

    @Override
    public void onInterrupt() {
        //disableSelf();
    }

    @Override
    protected void onServiceConnected() {
        Log.d(TAG, "Service CONNECTED!");
        INSTANCE = this;

        main.setupLayoutTouchListener();

    }

    public void prepareToInstall(String packageName, String appName) {
        INSTANCE.lastAppName = appName;
        INSTANCE.lastPackageName = packageName;
        Log.d(TAG, "PREPARING TO INSTALL " + INSTANCE.lastAppName);
    }

    //returns true if it has finished searching, NOT necessarily that it found the button
    boolean debugVRClick = true;
    int vrClickAttempts = 0;
    AccessibilityNodeInfo originalNodeInfo;

    public boolean findAndClickVRMode(AccessibilityNodeInfo nodeInfo, int depth) {
        originalNodeInfo = nodeInfo;
        if (debugVRClick)
            Log.w(TAG, "\t\tIn FindAndClickVRMode: " + main.getWebManager().launchingVR + ", " + nodeInfo);
        if (nodeInfo == null) {
            main.getWebManager().launchingVR = false; //done!
            return true; //finished searching
        }

        if (debugVRClick)
            Log.w(TAG, "\t\tVR SEARCH: " + nodeInfo.getViewIdResourceName() + ", " + nodeInfo.getContentDescription());
        if (nodeInfo.getContentDescription() != null && nodeInfo.getContentDescription().toString().equals("Enter virtual reality mode")) {
            if (debugVRClick) Log.w(TAG, "\t\tFound and clicked!");
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            main.getWebManager().launchingVR = false; //done!
            return true; //finished searching
        }

        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            boolean success = findAndClickVRMode(nodeInfo.getChild(i), depth + 1);
            if (success) {
                return true; //finished searching
            }
        }
        vrClickAttempts++;
        if (vrClickAttempts <= 2) {
            Log.w(TAG, "---- TRYING AGAIN (" + vrClickAttempts + ") ----");
            findAndClickVRMode(originalNodeInfo, 0); //keep trying!
        } else {
            vrClickAttempts = 0; //reset and quit
        }
        return false;
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        if (main == null) {
            return;
        }

        //check if we're trying to launch a VR YouTube video and respond appropriately
        //Log.i(TAG, "Launching VR? "+main.launchingVR+", "+event.getEventType()+", "+event.getSource());

        if (main.getWebManager().launchingVR && (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType())) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) {
                return;
            }
            findAndClickVRMode(getRootInActiveWindow(), 0);
        }

        //after this point is for client-specific behaviours only
        if (main.isGuide) {
            return;
        }

        //check if we're trying to install something and respond appropriately
        if ((AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED == event.getEventType() || (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType())) && (INSTANCE.lastAppName != null) && (INSTANCE.lastAppName.length() > 0)) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) {
                return;
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
                    INSTANCE.lastAppName = null; //reset, we're done
                    return; //all done, can exit
                } else {
                    Log.d(TAG, "Not clickable yet...");
                    return;
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
    }

    public void bringMainToFront() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
//                Intent intent = new Intent(main, LeadMeMain.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                main.startActivity(intent);
//
//                Log.d(TAG, "Bringing main to front. " + main.hasWindowFocus());
                if (!main.hasWindowFocus()) {
                    main.recallToLeadMe();
                    //main.returnToAppAction();
                    //main.returnToAppFromSettings();
                }
            }
        });
    }

    private void disableInteraction(final int status) {
        final boolean interactionBlocked = (status == ConnectedPeer.STATUS_BLACKOUT || status == ConnectedPeer.STATUS_LOCK);
        Log.d(TAG, "Is interaction blocked? " + interactionBlocked + ", " + main.getNearbyManager().isConnectedAsFollower() + ", " + main.isGuide);
        main.setStudentLock(status);
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                //we never want to block someone if they're disconnected from the guide
                //and we never want to block the guide
                main.hideSystemUI();
                if (main.getNearbyManager().isConnectedAsFollower() && interactionBlocked) {
                    Log.d(TAG, "BLOCKING!");
                    main.overlayParams.flags -= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    main.overlayParams.width = main.size.x;
                    main.overlayParams.height = main.size.y;
                } else {
                    Log.d(TAG, "NOT BLOCKING!");
                    main.overlayParams.flags += WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                    main.overlayParams.width = 0;
                    main.overlayParams.height = 0;
                }
                main.windowManager.updateViewLayout(main.overlayView, main.overlayParams);
            }
        };
        mainHandler.post(myRunnable);
    }

    private void writeMessageToSelected(byte[] bytes, Set<String> selectedPeerIDs) {
        if (main.getNearbyManager().isConnectedAsGuide()) {
            main.getNearbyManager().sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
        } else {
            Log.i(TAG, "Sorry, you can't send messages!");
        }
    }

    public byte[] requestRemoteAppOpen(String tag, String packageName, String appName, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes = null;
        p.writeString(tag);
        p.writeString(packageName);
        p.writeString(appName);
        bytes = p.marshall();
        p.recycle();

        writeMessageToSelected(bytes, selectedPeerIDs);

        return bytes;
    }

    public synchronized void sendActionToSelected(String actionTag, String action, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes = null;
        p.writeString(actionTag);
        p.writeString(action);
        bytes = p.marshall();
        p.recycle();

        //auto-install tags and success tag are exempt so students can alert teacher to their status
        if (main.getNearbyManager().isConnectedAsGuide() ||
                action.startsWith(LeadMeMain.RETURN_TAG) ||
                action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED) || action.startsWith(LeadMeMain.AUTO_INSTALL_ATTEMPT) ||
                action.startsWith(LeadMeMain.LAUNCH_SUCCESS)) {
            main.getNearbyManager().sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
        } else {
            Log.i(TAG, "Sorry, you can't send actions!");
        }
    }


    public synchronized void sendBoolToSelected(String actionTag, String action, boolean value, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes = null;
        p.writeString(actionTag);
        p.writeString(action);
        p.writeString(value + "");
        bytes = p.marshall();
        p.recycle();

        if (main.getNearbyManager().isConnectedAsGuide()) {
            main.getNearbyManager().sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
        } else {
            Log.i(TAG, "Sorry, you can't send booleans!");
        }
    }

    public synchronized boolean readBool(byte[] bytes) {
        Parcel p = Parcel.obtain();

        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        String actionTag = p.readString();
        String action = p.readString();
        String value = p.readString();
        boolean boolVal = Boolean.parseBoolean(value);
        p.recycle();

        Log.d(TAG, "Received boolean: " + actionTag + ", " + action + "=" + value);

        switch (action) {
//            case LeadMeMain.STUDENT_LOCK:
//                main.setStudentLock(ConnectedPeer.STATUS_LOCK);
//                disableInteraction(ConnectedPeer.STATUS_LOCK);
//                break;

            case LeadMeMain.AUTO_INSTALL:
                main.autoInstallApps = boolVal;
                break;

            default:
                //no match
                return false;
        }
        return true;

    }

    public synchronized boolean readAction(byte[] bytes) {
        Parcel p = Parcel.obtain();

        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        String actionTag = p.readString();
        String action = p.readString();
        p.recycle();

        Log.d(TAG, "Received action: " + actionTag + ", " + action);

        if (actionTag != null && actionTag.equals(LeadMeMain.ACTION_TAG)) {

            switch (action) {
                case LeadMeMain.RETURN_TAG:
                    Log.d(TAG, "Trying to return to " + main.getApplicationContext().getPackageName());
                    main.getRemoteDispatchService()
                            .sendActionToSelected(LeadMeMain.ACTION_TAG,
                                    LeadMeMain.LAUNCH_SUCCESS + "Lead Me" + ":" + main.getNearbyManager().getID() + ":" + main.getApplicationContext().getPackageName(),
                                    main.getNearbyManager().getAllPeerIDs());
                    main.recallToLeadMe();
                    break;

                case LeadMeMain.EXIT_TAG:
                    Toast exitToast = Toast.makeText(main.getApplicationContext(), "Session ended by guide", Toast.LENGTH_SHORT);
                    exitToast.show();
                    main.exitByGuide();
                    break;

                default:
                    if (action.startsWith(LeadMeMain.YOUR_ID_IS)) {
                        String[] split = action.split(":");
                        if (split.length == 3) {
                            //Now I know my ID! Store it.
                            if (split[2].equals(main.getNearbyManager().getName())) {
                                Log.d(TAG, "The Guide tells me my ID is " + split[1]);
                                main.getNearbyManager().setID(split[1]);
                            }
                        }
                        break;
                    } else if (action.startsWith(LeadMeMain.RETURN_TAG)) {
                        Log.d(TAG, "Leader told me to return!");
                        main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG,
                                LeadMeMain.LAUNCH_SUCCESS + "Lead Me" + ":" + main.getNearbyManager().getID() + ":" + main.getApplicationContext().getPackageName(),
                                main.getNearbyManager().getAllPeerIDs());
                        main.recallToLeadMe();
                        break;

                    } else if (action.startsWith(LeadMeMain.LOCK_TAG)) {
                        //I've been selected to toggle student lock
                        main.blackout(false);
                        disableInteraction(ConnectedPeer.STATUS_LOCK);
                        main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG,
                                LeadMeMain.LAUNCH_SUCCESS + "LOCKON" + ":" + main.getNearbyManager().getID() + ":" + main.getApplicationContext().getPackageName(),
                                main.getNearbyManager().getAllPeerIDs());
                        break;

                    } else if (action.startsWith(LeadMeMain.UNLOCK_TAG)) {
                        //I've been selected to toggle student lock
                        main.blackout(false);
                        disableInteraction(ConnectedPeer.STATUS_UNLOCK);
                        main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "LOCKOFF" + ":" + main.getNearbyManager().getID() + ":" + main.getApplicationContext().getPackageName(),
                                main.getNearbyManager().getAllPeerIDs());
                        break;

                    } else if (action.startsWith(LeadMeMain.BLACKOUT_TAG)) {
                        //I've been selected to toggle student lock
                        main.blackout(true);
                        disableInteraction(ConnectedPeer.STATUS_BLACKOUT);
                        main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "BLACKOUT" + ":" + main.getNearbyManager().getID() + ":" + main.getApplicationContext().getPackageName(),
                                main.getNearbyManager().getAllPeerIDs());
                        break;

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED)) {
                        Log.i(TAG, "FAILED");
                        String[] split = action.split(":");
                        //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_ERROR, split[1]);
                        main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_OFF_TASK_ALERT);
                        break;

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_ATTEMPT)) {
                        String[] split = action.split(":");
                        main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_INSTALLING);
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_SUCCESS)) {
                        Log.i(TAG, "SUCCEEDED - " + action);
                        String[] split = action.split(":");
                        if (split[1].equals("LOCKON")) {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_LOCK);

                        } else if (split[1].equals("LOCKOFF")) {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_UNLOCK);

                        } else if (split[1].equals("BLACKOUT")) {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_BLACKOUT);

                        } else {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS);
                            main.getConnectedLearnersAdapter().updateIcon(split[2], main.getAppManager().getAppIcon(split[3]));
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_URL)) {
                        String[] split = action.split(":", 2);
                        main.getWebManager().launchWebsite(split[1], true);
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_YT)) {
                        String[] split = action.split(":", 2);
                        main.getWebManager().launchYouTube(split[1], true);
                        break;

                    } else {
                        return false;
                    }
            }
            return true;

        } else {
            return false;
        }

    }

    public void launchDelayedApp() {
        if (launchAppOnFocus != null) {
            final String[] tmp = launchAppOnFocus;
            launchAppOnFocus = null; //reset

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    main.getAppManager().launchLocalApp(tmp[0], tmp[1], true);
                }
            });
        }
    }

    private String[] launchAppOnFocus = null;

    public boolean openApp(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        String tag = p.readString();
        final String packageName = p.readString();
        final String appName = p.readString();

        Log.d(TAG, "Received in OpenApp: " + tag + ", " + packageName + ", " + appName + " vs " + main.getAppManager().lastApp);
        if (tag != null && tag.equals(LeadMeMain.APP_TAG)) {
            if (!main.hasWindowFocus()) {//!main.getAppLaunchAdapter().lastApp.equals(packageName)) {
                Log.d(TAG, "NEED FOCUS!");
                //only needed if it's not what we've already got open
                //TODO make this more robust, check if it's actually running
                launchAppOnFocus = new String[2];
                launchAppOnFocus[0] = packageName;
                launchAppOnFocus[1] = appName;
                bringMainToFront();
            } else {
                Log.d(TAG, "HAVE FOCUS!");
                launchAppOnFocus = null; //reset
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        main.getAppManager().launchLocalApp(packageName, appName, true);
                    }
                });
            }
            return true;
        } else {
            Log.d(TAG, "Nope, not an app request");
            return false;
        }
    }

}
