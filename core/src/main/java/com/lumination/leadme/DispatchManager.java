package com.lumination.leadme;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;

import java.util.HashSet;
import java.util.Set;

public class DispatchManager {
    private final String TAG = "Dispatcher";
    private LeadMeMain main;
    protected String[] launchAppOnFocus = null;

    public DispatchManager(LeadMeMain main) {
        this.main = main;
    }

    protected void disableInteraction(final int status) {
        final boolean interactionBlocked = (status == ConnectedPeer.STATUS_BLACKOUT || status == ConnectedPeer.STATUS_LOCK);
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
                main.overlayParams.flags = main.CORE_FLAGS;
            } else {
                Log.d(TAG, "NOT BLOCKING!");
                main.overlayParams.flags = main.CORE_FLAGS + WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            if (main.overlayView.isAttachedToWindow()) {
                main.getWindowManager().updateViewLayout(main.overlayView, main.overlayParams);
            } else {
                Log.e(TAG, "ERROR: Overlay not attached to window yet! Might need to accept permission first");
            }
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

    public void requestRemoteAppOpenWithExtra(String tag, String packageName, String appName, String lockTag, String extra, boolean streaming, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(tag);
        p.writeString(packageName);
        p.writeString(appName);
        p.writeString(lockTag);
        p.writeString(extra);
        p.writeString(streaming + "");
        bytes = p.marshall();
        p.recycle();
        writeMessageToSelected(bytes, selectedPeerIDs);
    }

    public void requestRemoteAppOpen(String tag, String packageName, String appName, String lockTag, Set<String> selectedPeerIDs) {
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(tag);
        p.writeString(packageName);
        p.writeString(appName);
        p.writeString(lockTag);
        p.writeString("");
        p.writeString("");
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
                action.startsWith(LeadMeMain.PING_TAG) ||
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
                case LeadMeMain.PING_ACTION:
                    Set<String> peerSet = new HashSet<>();
                    peerSet.add(action.split(":")[1]);
                    //send a response back to the ID that pinged me
                    sendActionToSelected(LeadMeMain.PING_TAG, LeadMeMain.PING_ACTION + ":" + main.getNearbyManager().getID(), peerSet);
                    break;

                case LeadMeMain.RETURN_TAG:
                    Log.d(TAG, "Trying to return to " + main.leadMeAppName);
                    main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + main.leadMePackageName
                            + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName, main.getNearbyManager().getAllPeerIDs());
                    main.updateFollowerCurrentTaskToLeadMe();
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
                            Log.d(TAG, ">>> INCOMING: " + action + " vs " + main.getNearbyManager().getName() + " // " + main.getNearbyManager().getID());
                            if (split[2].equals(main.getUUID())) {
                                Log.d(TAG, "My peer tells me my ID is " + split[1] + " -- " + action + ", " + main.getNearbyManager().getName() + "/" + main.getUUID());
                                main.getNearbyManager().setID(split[1]);
                            }
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.VID_MUTE_TAG)) {
                        Log.d(TAG, "GOT MUTE");
                        main.muteAudio();

                    } else if (action.startsWith(LeadMeMain.VID_UNMUTE_TAG)) {
                        Log.d(TAG, "GOT UNMUTE");
                        main.unMuteAudio();

                    } else if (action.startsWith(LeadMeMain.VID_ACTION_TAG)) {
                        Log.d(TAG, "GOT SOMETHING VID RELATED");
                        String[] split = action.split(":");
                        main.getLumiAccessibilityConnector().cueYouTubeAction(split[1]);

                    } else if (action.startsWith(LeadMeMain.LOGOUT_TAG)) {
                        String id = action.split(":")[1];
                        Log.d(TAG, "Guide " + id + " has logged out!");
                        main.getNearbyManager().disconnectFromEndpoint(id);

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
                        //Log.i(TAG, "STUDENT HAS OVERLAY? " + split[1].equals("OK"));
                        //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_ERROR, split[1]);
                        if (split[1].equalsIgnoreCase("OK")) {
                            //clear previous flag
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_OVERLAY);
                        } else {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_OVERLAY);
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.STUDENT_OFF_TASK_ALERT)) {
                        String[] split = action.split(":");
                        //Log.i(TAG, "STUDENT OFF TASK? " + split[1]);
                        main.getConnectedLearnersAdapter().updateStatus(split[1], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_OFF_TASK_ALERT);
                        break;

                    } else if (action.startsWith(LeadMeMain.STUDENT_NO_INTERNET)) {
                        String[] split = action.split(":");
                        //Log.i(TAG, "STUDENT HAS INTERNET? " + split[1].equals("OK"));
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
                        //Log.i(TAG, "STUDENT HAS ACCESS? " + split[1].equals("OK"));
                        //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_ERROR, split[1]);
                        if (split[1].equalsIgnoreCase("OK")) {
                            //clear previous flag
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                        } else {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED)) {
                        String[] split = action.split(":");
                        //Log.i(TAG, "FAILED " + split[2]);
                        //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.AUTO_INSTALL_FAILED);
                        main.getConnectedLearnersAdapter().appLaunchFail(split[2], split[1]);

                        //in this case, student will be back in LeadMe, so update icon too
                        main.getConnectedLearnersAdapter().updateIcon(split[2], main.getAppManager().getAppIcon(main.leadMePackageName));
                        break;

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_ATTEMPT)) {
                        String[] split = action.split(":");
                        main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_INSTALLING);
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_SUCCESS)) {
                        //Log.i(TAG, "SUCCEEDED - " + action);
                        String[] split = action.split(":");
                        main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_OFF_TASK_ALERT);

                        if (split[1].equals("LOCKON")) {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_LOCK);

                        } else if (split[1].equals("LOCKOFF")) {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_UNLOCK);

                        } else if (split[1].equals("BLACKOUT")) {
                            main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_BLACKOUT);

                        } else {
                            //Log.d(TAG, "Updating icon to " + split[3]);
                            main.getConnectedLearnersAdapter().appLaunchSuccess(split[2], split[1]);
                            //main.getConnectedLearnersAdapter().updateStatus(split[2], ConnectedPeer.STATUS_SUCCESS);
                            main.getConnectedLearnersAdapter().updateIcon(split[2], main.getAppManager().getAppIcon(split[3]));
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_URL)) {
                        String[] split = action.split(":::", 3);
                        main.getWebManager().launchWebsite(split[1], split[2], true);
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_YT)) {
                        String[] split = action.split(":::", 3);
                        main.getWebManager().launchYouTube(split[1], split[2], true);
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
        Log.d(TAG, "[XX] Have something to launch? " + launchAppOnFocus);
        if (launchAppOnFocus != null) {
            final String[] tmp = launchAppOnFocus;
            launchAppOnFocus = null; //reset
            main.getAppManager().launchLocalApp(tmp[0], tmp[1], true);
        }
    }

    public boolean openApp(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        String tag = p.readString();
        final String packageName = p.readString();
        final String appName = p.readString();
        final String lockTag = p.readString();
        final String extra = p.readString();
        final String streaming = p.readString();
        p.recycle();

        Log.d(TAG, "Received in OpenApp!: " + tag + ", " + packageName + ", " + appName + ", " + lockTag + ", " + extra + ", " + streaming + " vs " + main.getAppManager().lastApp);
        if (tag != null && tag.equals(LeadMeMain.APP_TAG)) {

            if (lockTag.equals(LeadMeMain.LOCK_TAG)) {
                //I've been selected to toggle student lock
                main.blackout(false);
                disableInteraction(ConnectedPeer.STATUS_LOCK);
                sendActionToSelected(LeadMeMain.ACTION_TAG,
                        LeadMeMain.LAUNCH_SUCCESS + "LOCKON" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
                        main.getNearbyManager().getAllPeerIDs());

            } else if (lockTag.equals(LeadMeMain.UNLOCK_TAG)) {
                //I've been selected to toggle student lock
                main.blackout(false);
                disableInteraction(ConnectedPeer.STATUS_UNLOCK);
                sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "LOCKOFF" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
                        main.getNearbyManager().getAllPeerIDs());
            }

            if (!extra.isEmpty()) {
                main.getAppManager().isStreaming = Boolean.parseBoolean(streaming);
                Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(extra));
                appIntent.setPackage(packageName);

                if (!main.appHasFocus) {
                    main.appIntentOnFocus = appIntent;
                    main.getLumiAccessibilityConnector().bringMainToFront();
                } else {
                    main.startActivity(appIntent);
                }
                Log.d(TAG, "TRYING TO LAUNCH WITHIN APP FOR " + extra + ", " + streaming);

            } else if (!main.appHasFocus) {//!main.getAppLaunchAdapter().lastApp.equals(packageName)) {
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
        } else {
            sendActionToSelected(LeadMeMain.ACTION_TAG, whichPermission + "FAIL:" + main.getNearbyManager().getID(), main.getNearbyManager().getAllPeerIDs());
        }
    }

    protected void alertGuideStudentOffTask() {
        sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.STUDENT_OFF_TASK_ALERT + main.getNearbyManager().getID(),
                main.getNearbyManager().getAllPeerIDs());
    }


}
