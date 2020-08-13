package com.lumination.leadme;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.gesture.Gesture;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RemoteDispatcherService extends AccessibilityService {
    //handler for executing on the main thread
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    //static variables for use inside the gesture thread
    private static volatile boolean overlayInactivated = false;
    private static List<GestureDescription> gestureList;
    private static MainActivity main;
    private Context context;
    private String lastAppName, lastPackageName;

    private final String TAG = "RemoteDispatch";

    private static RemoteDispatcherService INSTANCE = new RemoteDispatcherService();

    private void attachMain(MainActivity m) {
        main = m;
    }

    public static RemoteDispatcherService getInstance(MainActivity m) {
        main = m;
        INSTANCE.attachMain(m);
        return INSTANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service CREATED!");
        context = getInstance(main);
    }

    @Override
    public void onInterrupt() {
        disableSelf();

        // main.overlayView.setVisibility(View.GONE); <-- seems to get cleaned up anyway
        //TODO maybe just hide the overlay when not in use?
    }

    @Override
    protected void onServiceConnected() {
        Log.d(TAG, "Service CONNECTED!");
        INSTANCE = this;
        //starts action listener thread
        gestureActionThread.start();

        if (main != null) {
            main.returnToAppFromSettings();
        }
    }

    public void prepareToInstall(String packageName, String appName) {
        INSTANCE.lastAppName = appName;
        INSTANCE.lastPackageName = packageName;
        Log.d(TAG, "PREPARING TO INSTALL " + INSTANCE.lastAppName);
    }

    //returns true if it has finished searching, NOT necessarily that it found the button
    public boolean findAndClickVRMode(AccessibilityNodeInfo nodeInfo, int depth) {
        if (nodeInfo == null) {
            main.launchingVR = false; //done!
            return true; //finished searching
        }

        //Log.i(TAG, "VR SEARCH: "+nodeInfo.getViewIdResourceName()+", "+nodeInfo.getContentDescription());
        if (nodeInfo.getContentDescription() != null && nodeInfo.getContentDescription().toString().equals("Enter virtual reality mode")) {
            //Log.i(TAG, "Found and clicked!");
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            main.launchingVR = false; //done!
            return true; //finished searching
        }

        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            boolean success = findAndClickVRMode(nodeInfo.getChild(i), depth + 1);
            if (success) {
                return true; //finished searching
            }
        }
        return false;
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        //check if we're trying to launch a VR YouTube video and respond appropriately
        //Log.i(TAG, "Launching VR? "+main.launchingVR);
        if (main == null) {
            return;
        }

        if (main.launchingVR && (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType())) {
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
                        main.getAppLauncher().launchLocalApp(main, lastPackageName, lastAppName);
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

    public static List<GestureDescription> getGestureList() {
        if (gestureList == null) {
            gestureList = Collections.synchronizedList(new ArrayList<GestureDescription>());
        }
        return gestureList;
    }

    Thread gestureActionThread = new Thread(new Runnable() {
        public void run() {
            while (true) {
                List<GestureDescription> tmpGestureList = getGestureList();
                if (!tmpGestureList.isEmpty()) {
                    GestureDescription desc = tmpGestureList.remove(0);
                    hideOverlay(true);
                    while (!overlayInactivated) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            Log.e(TAG, Log.getStackTraceString(ex.fillInStackTrace()));
                        }
                    }
                    overlayInactivated = false;
                    sendGesture(desc);
                    hideOverlay(false);

                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e(TAG, Log.getStackTraceString(e.fillInStackTrace()));
                    }
                }
            }
        }
    });

    public void sendGesture(GestureDescription desc) {
        boolean isDispatched = dispatchGesture(desc, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }

        }, null);
        if (!isDispatched) {
            //Log.d(TAG, "GESTURED! >> " + isDispatched);
            Log.w(TAG, ">> GESTURED FAILED TO DISPATCH!");
        }
    }

    private void hideOverlay(final boolean hide) {
        Log.d(TAG, "Hiding overlay: " + hide);
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (hide) {
                    main.overlayParams.flags += WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                } else {
                    main.overlayParams.flags -= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                }
                main.windowManager.updateViewLayout(main.overlayView, main.overlayParams);
                main.overlayView.post(new Runnable() {
                    @Override
                    public void run() {
                        overlayInactivated = hide;
                    }
                });
            }
        };
        mainHandler.post(myRunnable);
    }

    public void bringMainToFront() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(main, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                main.startActivity(intent);

                if (!main.hasWindowFocus()) {
                    main.returnToAppAction();
                }
            }
        });
    }

    private void disableInteraction(final boolean interactionBlocked) {
        Log.d(TAG, "Is interaction blocked? " + interactionBlocked + ", " + main.nearbyManager.isConnectedAsFollower() + ", " + main.isGuide);
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                //we never want to block someone if they're disconnected from the guide
                //and we never want to block the guide
                if (main.nearbyManager.isConnectedAsFollower() && interactionBlocked) {
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

    private void writeMessage(byte[] bytes) {
        if (main.nearbyManager.isConnectedAsGuide()) {
            main.nearbyManager.write(bytes);
        } else {
            Log.i(TAG, "Sorry, you can't send messages!");
        }
    }


    public byte[] gestureToBytes(String tag, Gesture g, long duration) {
        Parcel p = Parcel.obtain();
        byte[] bytes = null;
        p.writeString(tag);
        p.writeValue(g);
        p.writeLong(duration);
        bytes = p.marshall();
        p.recycle();

        writeMessage(bytes);

        return bytes;
    }

    public boolean bytesToGesture(byte[] bytes) {
        Parcel p = Parcel.obtain();
        try {
            p.unmarshall(bytes, 0, bytes.length);
            p.setDataPosition(0);
            String tag = p.readString();

            if (tag != null && tag.equals(MainActivity.GESTURE_TAG)) {
                Gesture g = (Gesture) p.readValue(Gesture.class.getClassLoader());
                long duration = p.readLong();
                p.recycle();
                Log.d(TAG, "Received in Gesture: " + tag + ", " + g + ", " + duration);

                addGesture(g.toPath(), duration);
                return true;
            } else {
                p.recycle();
                return false;
            }

        } catch (Exception e) {
            Log.w(TAG, "Failed to unpackage gesture: " + bytes.toString() + ", " + e.getLocalizedMessage());
            p.recycle();
            return false;
        }
    }

    public byte[] requestRemoteAppOpen(String tag, String packageName, String appName) {
        Parcel p = Parcel.obtain();
        byte[] bytes = null;
        p.writeString(tag);
        p.writeString(packageName);
        p.writeString(appName);
        bytes = p.marshall();
        p.recycle();

        writeMessage(bytes);

        return bytes;
    }

    public synchronized void sendAction(String actionTag, String action) {
        Parcel p = Parcel.obtain();
        byte[] bytes = null;
        p.writeString(actionTag);
        p.writeString(action);
        bytes = p.marshall();
        p.recycle();

        //auto-install tags and success tag are exempt so students can alert teacher to their status
        if (main.nearbyManager.isConnectedAsGuide() || action.startsWith(MainActivity.AUTO_INSTALL_FAILED) || action.startsWith(MainActivity.AUTO_INSTALL_ATTEMPT) || action.startsWith(MainActivity.LAUNCH_SUCCESS)) {
            main.nearbyManager.write(bytes);
        } else {
            Log.i(TAG, "Sorry, you can't send actions!");
        }
    }


    public synchronized void sendBool(String actionTag, String action, boolean value) {
        Parcel p = Parcel.obtain();
        byte[] bytes = null;
        p.writeString(actionTag);
        p.writeString(action);
        p.writeString(value + "");
        bytes = p.marshall();
        p.recycle();

        if (main.nearbyManager.isConnectedAsGuide()) {
            main.nearbyManager.write(bytes);
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
            case MainActivity.STUDENT_LOCK:
                main.setStudentLock(boolVal);
                disableInteraction(boolVal);
                break;

            case MainActivity.STUDENT_MENU:
                main.setStudentMenuVisibility(boolVal);
                break;

            case MainActivity.AUTO_INSTALL:
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

        if (actionTag != null && actionTag.equals(MainActivity.ACTION_TAG)) {

            switch (action) {
                case MainActivity.RETURN_TAG:
                    Log.d(TAG, "Trying to return to " + main.getApplicationContext().getPackageName());
                    main.returnToAppAction();
                    break;

                case MainActivity.EXIT_TAG:
                    Toast exitToast = Toast.makeText(main.getApplicationContext(), "Session ended by guide", Toast.LENGTH_SHORT);
                    exitToast.show();
                    main.exitByGuide();
                    break;

                default:
                    if (action.startsWith(MainActivity.AUTO_INSTALL_FAILED)) {
                        String[] split = action.split(":");
                        main.getConnectedStudentsFragment().updateStatus(split[2], "&#x2716; " + split[1]);
                        break;

                    } else if (action.startsWith(MainActivity.AUTO_INSTALL_ATTEMPT)) {
                        String[] split = action.split(":");
                        main.getConnectedStudentsFragment().updateStatus(split[2], "&#8718; " + split[1]);
                        break;

                    } else if (action.startsWith(MainActivity.LAUNCH_SUCCESS)) {
                        String[] split = action.split(":");
                        main.getConnectedStudentsFragment().updateStatus(split[2], "&#x2714; " + split[1]);
                        break;

                    } else if (action.startsWith(MainActivity.LAUNCH_URL)) {
                        String[] split = action.split(":", 2);
                        main.launchWebsite(split[1]);
                        break;

                    } else if (action.startsWith(MainActivity.LAUNCH_YT)) {
                        String[] split = action.split(":", 2);
                        main.launchYouTube(split[1]);
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

    public void launchDelayedApp(){
        if(launchAppOnFocus != null) {
            final String[] tmp = launchAppOnFocus;
            launchAppOnFocus = null; //reset

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    main.getAppLauncher().launchLocalApp(main, tmp[0], tmp[1]);
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

        Log.d(TAG, "Received in OpenApp: " + tag + ", " + packageName + ", " + appName);
        if (tag != null && tag.equals(MainActivity.APP_TAG)) {
            if (!main.getAppLauncher().lastApp.equals(packageName)) {
                //only needed if it's not what we've already got open
                //TODO make this more robust, check if it's actually running
                launchAppOnFocus = new String[2];
                launchAppOnFocus[0] = packageName;
                launchAppOnFocus[1] = appName;
                bringMainToFront();
            } else {
                launchAppOnFocus = null; //reset
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        main.getAppLauncher().launchLocalApp(main, packageName, appName);
                    }
                });
            }
            return true;
        } else {
            Log.d(TAG, "Nope, not an app request");
            return false;
        }
    }

    public synchronized void addGesture(Path path, long duration) {
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));
        GestureDescription desc = gestureBuilder.build();
        getGestureList().add(desc);
    }
}
