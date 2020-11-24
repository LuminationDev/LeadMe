package com.lumination.leadme;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;

public class RemoteDispatcherService extends AccessibilityService {
    //handler for executing on the main thread
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private static MainActivity main;
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

//        OrientationEventListener mOrientationListener = new OrientationEventListener(this) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//                if (orientation == 0 || orientation == 180) {
//                    Log.d(TAG, "portrait");
//                } else if (orientation == 90 || orientation == 270) {
//                    Log.d(TAG, "landscape");
//                }
//                mainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        main.overlayView.invalidate();
//                        main.overlayView.requestLayout();
//                    }
//                });
//            }
//        };
//
//        if (mOrientationListener.canDetectOrientation()) {
//            mOrientationListener.enable();
//        }
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

        if (main != null) {
            main.returnToAppFromSettings();
        }


        ///////////////////

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        FrameLayout layout = new FrameLayout(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;


        if (windowManager != null) {
            windowManager.addView(layout, params);
            layout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getY() == 0) {
                        Log.d(TAG, "STATUS OR NAV TOUCH!! " + event.getAction());
                        main.collapseStatusNow();
                        //main.getForegroundActivity();

                        //hide status bar
                        //check if target app still in front
                        //IF NOT alert teacher and bring target app back to the front

                        return true; //consume event
                    }

                    return false; //pass event on
                }
            });
        }

        ///////////////////
    }

    public void prepareToInstall(String packageName, String appName) {
        INSTANCE.lastAppName = appName;
        INSTANCE.lastPackageName = packageName;
        Log.d(TAG, "PREPARING TO INSTALL " + INSTANCE.lastAppName);
    }

    //returns true if it has finished searching, NOT necessarily that it found the button
    public boolean findAndClickVRMode(AccessibilityNodeInfo nodeInfo, int depth) {
        Log.d(TAG, "In FindAndClickVRMode: " + main.launchingVR);
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
        if (main == null) {
            return;
        }

        //check if we're trying to launch a VR YouTube video and respond appropriately
        //Log.i(TAG, "Launching VR? "+main.launchingVR+", "+event.getEventType()+", "+event.getSource());

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
        main.setStudentLock(interactionBlocked);
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                //we never want to block someone if they're disconnected from the guide
                //and we never want to block the guide
                main.hideSystemUI();
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
        if (main.nearbyManager.isConnectedAsGuide() ||
                action.startsWith(MainActivity.RETURN_TAG) ||
                action.startsWith(MainActivity.AUTO_INSTALL_FAILED) || action.startsWith(MainActivity.AUTO_INSTALL_ATTEMPT) ||
                action.startsWith(MainActivity.LAUNCH_SUCCESS)) {
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
                    main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + "Lead Me" + ":" + main.nearbyManager.getID() + ":" + main.getApplicationContext().getPackageName());
                    main.returnToAppAction();
                    break;

                case MainActivity.EXIT_TAG:
                    Toast exitToast = Toast.makeText(main.getApplicationContext(), "Session ended by guide", Toast.LENGTH_SHORT);
                    exitToast.show();
                    main.exitByGuide();
                    break;

                default:
                    if (action.startsWith(MainActivity.YOUR_ID_IS)) {
                        String[] split = action.split(":");
                        if (split.length == 3) {
                            //Now I know my ID! Store it.
                            if (split[2].equals(main.nearbyManager.getName())) {
                                Log.d(TAG, "The Guide tells me my ID is " + split[1]);
                                main.nearbyManager.setID(split[1]);
                            }
                        }
                        break;
                    } else if (action.startsWith(MainActivity.RETURN_CHOSEN_TAG)) {
                        String[] split = action.split(":");
                        if (split.length == 2 && split[1].contains(main.nearbyManager.getID())) {
                            //I've been selected to return to app
                            main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + "Lead Me" + ":" + main.nearbyManager.getID() + ":" + main.getApplicationContext().getPackageName());
                            main.returnToAppAction();
                        }
                        break;

                    } else if (action.startsWith(MainActivity.LOCK_CHOSEN_TAG)) {
                        String[] split = action.split(":");
                        if (split.length == 2 && split[1].contains(main.nearbyManager.getID())) {
                            //I've been selected to toggle student lock
                            main.blackout(false);
                            disableInteraction(true);
                            main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + "LOCKON" + ":" + main.nearbyManager.getID() + ":" + main.getApplicationContext().getPackageName());
                        }
                        break;

                    } else if (action.startsWith(MainActivity.UNLOCK_CHOSEN_TAG)) {
                        String[] split = action.split(":");
                        if (split.length == 2 && split[1].contains(main.nearbyManager.getID())) {
                            //I've been selected to toggle student lock
                            main.blackout(false);
                            disableInteraction(false);
                            main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + "LOCKOFF" + ":" + main.nearbyManager.getID() + ":" + main.getApplicationContext().getPackageName());
                        }
                        break;

                    } else if (action.startsWith(MainActivity.BLACKOUT_CHOSEN_TAG)) {
                        String[] split = action.split(":");
                        if (split.length == 2 && split[1].contains(main.nearbyManager.getID())) {
                            //I've been selected to toggle student lock
                            main.blackout(true);
                            disableInteraction(true);
                            main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + "BLACKOUT" + ":" + main.nearbyManager.getID() + ":" + main.getApplicationContext().getPackageName());
                        }
                        break;

                    } else if (action.startsWith(MainActivity.AUTO_INSTALL_FAILED)) {
                        Log.i(TAG, "FAILED");
                        String[] split = action.split(":");
                        main.getConnectedStudentsFragment().updateStatus(split[2], ConnectedStudentsFragment.LUMI_FAIL, split[1]);
                        break;

                    } else if (action.startsWith(MainActivity.AUTO_INSTALL_ATTEMPT)) {
                        String[] split = action.split(":");
                        main.getConnectedStudentsFragment().updateStatus(split[2], ConnectedStudentsFragment.LUMI_OTHER, split[1]);
                        break;

                    } else if (action.startsWith(MainActivity.LAUNCH_SUCCESS)) {
                        Log.i(TAG, "SUCCEEDED - " + action);
                        String[] split = action.split(":");
                        if (split[1].equals("LOCKON")) {
                            main.getConnectedStudentsFragment().updateLockStatus(split[2], LumiPeer.LOCKED);

                        } else if (split[1].equals("LOCKOFF")) {
                            main.getConnectedStudentsFragment().updateLockStatus(split[2], LumiPeer.UNLOCKED);

                        } else if (split[1].equals("BLACKOUT")) {
                            main.getConnectedStudentsFragment().updateLockStatus(split[2], LumiPeer.BLACKOUT);

                        } else {
                            main.getConnectedStudentsFragment().updateStatus(split[2], ConnectedStudentsFragment.LUMI_SUCCESS, split[1]);
                            main.getConnectedStudentsFragment().updateIcon(split[2], main.getAppLauncher().getAppIcon(split[3]));
                        }
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

    public void launchDelayedApp() {
        if (launchAppOnFocus != null) {
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
}
