package com.lumination.leadme;

import android.os.Parcel;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;

import java.util.Set;

public class DispatchManager {
    private final String TAG = "Dispatcher";
    private LeadMeMain main;
    protected String[] launchAppOnFocus = null;

    public DispatchManager(LeadMeMain main) {
        this.main = main;
    }

    private void disableInteraction(final int status) {
        final boolean interactionBlocked = (status == ConnectedPeer.STATUS_BLACKOUT || status == ConnectedPeer.STATUS_LOCK);
        Log.d(TAG, "Is interaction blocked? " + interactionBlocked + ", " + main.getNearbyManager().isConnectedAsFollower() + ", " + main.isGuide + ", " + main.getPermissionsManager().isOverlayPermissionGranted() + ", " + main.overlayView);
        main.setStudentLock(status);
        Runnable myRunnable = () -> {
            //we never want to block someone if they're disconnected from the guide
            //and we never want to block the guide
            main.hideSystemUI();
            if (!main.verifyOverlay()) {
                return;
            }
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
            main.getWindowManager().updateViewLayout(main.overlayView, main.overlayParams);
        };
        main.runOnUiThread(myRunnable);
    }

    private void writeMessageToSelected(byte[] bytes, Set<String> selectedPeerIDs) {
        if (main.getNearbyManager().isConnectedAsGuide()) {
            main.getNearbyManager().sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
        } else {
            Log.i(TAG, "Sorry, you can't send messages!");
        }
    }

    public void requestRemoteAppOpen(String tag, String packageName, String appName, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(tag);
        p.writeString(packageName);
        p.writeString(appName);
        bytes = p.marshall();
        p.recycle();

        writeMessageToSelected(bytes, selectedPeerIDs);
    }

    public synchronized void alertLogout() {
        sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOGOUT_TAG + ":" + main.getNearbyManager().getID(), main.getNearbyManager().getAllPeerIDs());
    }

    public synchronized void sendActionToSelected(String actionTag, String action, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(actionTag);
        p.writeString(action);
        bytes = p.marshall();
        p.recycle();

        //auto-install tags and success tag are exempt so students can alert teacher to their status
        if (main.getNearbyManager().isConnectedAsGuide() ||
                action.startsWith(LeadMeMain.YOUR_ID_IS) ||
                action.startsWith(LeadMeMain.RETURN_TAG) ||
                action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED) ||
                action.startsWith(LeadMeMain.AUTO_INSTALL_ATTEMPT) ||
                action.startsWith(LeadMeMain.STUDENT_NO_OVERLAY) ||
                action.startsWith(LeadMeMain.STUDENT_NO_INTERNET) ||
                action.startsWith(LeadMeMain.STUDENT_NO_ACCESSIBILITY) ||
                action.startsWith(LeadMeMain.STUDENT_OFF_TASK_ALERT) ||
                action.startsWith(LeadMeMain.LAUNCH_SUCCESS)) {
            main.getNearbyManager().sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
        } else {
            Log.i(TAG, "Sorry, you can't send actions!");
        }
    }

    public synchronized void sendBoolToSelected(String actionTag, String action, boolean value, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes;
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
                    Log.d(TAG, "Trying to return to " + main.leadMeAppName);
                    sendActionToSelected(LeadMeMain.ACTION_TAG,
                            LeadMeMain.LAUNCH_SUCCESS + main.leadMeAppName + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
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
                                Log.d(TAG, "My peer tells me my ID is " + split[1] + " -- " + action + ", " + main.getNearbyManager().getName());
                                main.getNearbyManager().setID(split[1]);
                            }
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.LOGOUT_TAG)) {
                        String id = action.split(":")[1];
                        Log.d(TAG, "Guide " + id + " has logged out!");
                        main.getNearbyManager().disconnectFromEndpoint(id);

                    } else if (action.startsWith(LeadMeMain.RETURN_TAG)) {
                        Log.d(TAG, "Leader told me to return!");
                        sendActionToSelected(LeadMeMain.ACTION_TAG,
                                LeadMeMain.LAUNCH_SUCCESS + main.leadMeAppName + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
                                main.getNearbyManager().getAllPeerIDs());
                        main.recallToLeadMe();
                        break;

                    } else if (action.startsWith(LeadMeMain.LOCK_TAG)) {
                        //I've been selected to toggle student lock
                        main.blackout(false);
                        disableInteraction(ConnectedPeer.STATUS_LOCK);
                        sendActionToSelected(LeadMeMain.ACTION_TAG,
                                LeadMeMain.LAUNCH_SUCCESS + "LOCKON" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
                                main.getNearbyManager().getAllPeerIDs());
                        break;

                    } else if (action.startsWith(LeadMeMain.UNLOCK_TAG)) {
                        //I've been selected to toggle student lock
                        main.blackout(false);
                        disableInteraction(ConnectedPeer.STATUS_UNLOCK);
                        sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "LOCKOFF" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
                                main.getNearbyManager().getAllPeerIDs());
                        break;

                    } else if (action.startsWith(LeadMeMain.BLACKOUT_TAG)) {
                        //I've been selected to toggle student lock
                        main.blackout(true);
                        disableInteraction(ConnectedPeer.STATUS_BLACKOUT);
                        sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "BLACKOUT" + ":" + main.getNearbyManager().getID() + ":" + main.getApplicationContext().getPackageName(),
                                main.getNearbyManager().getAllPeerIDs());
                        break;
                    } else if (action.startsWith(LeadMeMain.STUDENT_NO_OVERLAY)) {
                        String[] split = action.split(":");
                        Log.i(TAG, "STUDENT HAS OVERLAY? " + split[1].equals("OK"));
                        //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_ERROR, split[1]);
                        if (split[1].equalsIgnoreCase("OK")) {
                            //clear previous flag
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_OVERLAY);
                        } else {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_OVERLAY);
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.STUDENT_NO_INTERNET)) {
                        String[] split = action.split(":");
                        Log.i(TAG, "STUDENT HAS INTERNET? " + split[1].equals("OK"));
                        //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_ERROR, split[1]);
                        if (split[1].equalsIgnoreCase("OK")) {
                            //clear previous flag
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_INTERNET);
                        } else {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_INTERNET);
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.STUDENT_NO_ACCESSIBILITY)) {
                        String[] split = action.split(":");
                        Log.i(TAG, "STUDENT HAS ACCESS? " + split[1].equals("OK"));
                        //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_ERROR, split[1]);
                        if (split[1].equalsIgnoreCase("OK")) {
                            //clear previous flag
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                        } else {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED)) {
                        Log.i(TAG, "FAILED");
                        String[] split = action.split(":");
                        main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.AUTO_INSTALL_FAILED);
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
                            Log.d(TAG, "Updating icon to " + split[3]);
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

    public boolean hasDelayedLaunchContent() {
        return launchAppOnFocus != null || main.appIntentOnFocus != null;
    }

    public void launchDelayedApp() {
        main.verifyOverlay();
        Log.d(TAG, "Have something to launch? " + launchAppOnFocus);
        if (launchAppOnFocus != null) {
            final String[] tmp = launchAppOnFocus;
            launchAppOnFocus = null; //reset

            main.getHandler().post(() -> main.getAppManager().launchLocalApp(tmp[0], tmp[1], true));
        }
    }

    public boolean openApp(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        String tag = p.readString();
        final String packageName = p.readString();
        final String appName = p.readString();
        p.recycle();

        Log.d(TAG, "Received in OpenApp: " + tag + ", " + packageName + ", " + appName + " vs " + main.getAppManager().lastApp);
        if (tag != null && tag.equals(LeadMeMain.APP_TAG)) {
            if (!main.appHasFocus) {//!main.getAppLaunchAdapter().lastApp.equals(packageName)) {
                Log.d(TAG, "NEED FOCUS!");
                //only needed if it's not what we've already got open
                //TODO make this more robust, check if it's actually running
                launchAppOnFocus = new String[2];
                launchAppOnFocus[0] = packageName;
                launchAppOnFocus[1] = appName;
                main.getLumiAccessibilityConnector().bringMainToFront();
            } else {
                Log.d(TAG, "HAVE FOCUS!");
                launchAppOnFocus = null; //reset
                main.getHandler().post(() -> main.getAppManager().launchLocalApp(packageName, appName, true));
            }
            return true;
        } else {
            Log.d(TAG, "Nope, not an app request");
            return false;
        }
    }


    protected void alertGuidePermissionGranted(String whichPermission, boolean isGranted) {

        if (isGranted) {
            sendActionToSelected(LeadMeMain.ACTION_TAG, whichPermission + "OK:" + main.getNearbyManager().getID(), main.getNearbyManager().getAllPeerIDs());
            return;
        }

        String warningMessage = "";
        switch (whichPermission) {
            case LeadMeMain.STUDENT_NO_ACCESSIBILITY:
                warningMessage = "Permission error (accessibility)";
                break;
            case LeadMeMain.STUDENT_NO_OVERLAY:
                warningMessage = "Permission error (overlay)";
                break;
            case LeadMeMain.STUDENT_NO_INTERNET:
                warningMessage = "Permission error (no internet)";
                break;
            case LeadMeMain.STUDENT_OFF_TASK_ALERT:
                warningMessage = "Off task";
                break;
            case LeadMeMain.AUTO_INSTALL_FAILED:
                warningMessage = "Couldn't launch";
        }

        //send correct update to guide
        sendActionToSelected(LeadMeMain.ACTION_TAG, whichPermission + warningMessage +
                ":" + main.getNearbyManager().getID(), main.getNearbyManager().getAllPeerIDs());
    }

}
