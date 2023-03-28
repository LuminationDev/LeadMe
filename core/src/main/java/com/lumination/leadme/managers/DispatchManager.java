package com.lumination.leadme.managers;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.accessibility.VRAccessibilityManager;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DispatchManager {
    private static final String TAG = "Dispatcher";
    private final LeadMeMain main;
    public static String[] launchAppOnFocus = null;
    public static String tagRepush;
    public static String packageNameRepush;
    public static String appNameRepush;
    public static String lockTagRepush;
    public static String extraRepush;
    public static boolean streamingRepush;
    public static boolean vrModeRepush;
    public static String mActionTag=null, mAction=null;
    public static int lastEvent =0;

    private final Actions dispatchAction = new Actions();

    public DispatchManager(LeadMeMain main) {
        this.main = main;
    }

    public static void repushApp(Set<String> selectedPeerIDs) {
        Log.w(TAG, "REPUSH: " + tagRepush + ", " + packageNameRepush + ", " + appNameRepush + ", " + lockTagRepush + ", " + extraRepush);
        LeadMeMain.getInstance().recallToLeadMe();
        switch(lastEvent){
            case 2:
                requestRemoteAppOpen(tagRepush, packageNameRepush, appNameRepush, lockTagRepush, "false", selectedPeerIDs);
                break;
            case 3:
                sendActionToSelected(mActionTag,mAction,selectedPeerIDs);
                break;
        }
    }

    public void disableInteraction(final int status) {
        final boolean interactionBlocked = (status == ConnectedPeer.STATUS_BLACKOUT || status == ConnectedPeer.STATUS_LOCK);
        main.setStudentLock(status);
        Runnable myRunnable = () -> {
            //we never want to block someone if they're disconnected from the guide
            //and we never want to block the guide
            main.hideSystemUI();
            if (!main.verifyOverlay()) {
                return;
            }
            if (NearbyPeersManager.isConnectedAsFollower() && interactionBlocked) {
                Log.d(TAG, "BLOCKING!");
                LeadMeMain.overlayParams.flags = LeadMeMain.CORE_FLAGS;
            } else {
                Log.d(TAG, "NOT BLOCKING!");
                LeadMeMain.overlayParams.flags = LeadMeMain.CORE_FLAGS + WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            if (main.overlayView.isAttachedToWindow()) {
                main.getWindowManager().updateViewLayout(main.overlayView, LeadMeMain.overlayParams);
            } else {
                Log.e(TAG, "ERROR: Overlay not attached to window yet! Might need to accept permission first");
            }
        };

        LeadMeMain.runOnUI(myRunnable);
    }

    private static void writeMessageToSelected(byte[] bytes, Set<String> selectedPeerIDs) {
//        if (NearbyPeersManager.isConnectedAsGuide()) {
        Log.d(TAG, "writeMessageToSelected");
            NearbyPeersManager.sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
//        } else {
//            Log.i(TAG, "Sorry, you can't send messages!");
//        }
    }

    /**
     * Launch an application on the learner's device.
     * @param tag A string ....
     * @param packageName A string representing if the package is within.
     * @param appName A string representing the name of the application 'Within'.
     * @param lockTag A string representing if the learner devices should be in lock or play mode.
     * @param install A string representing if the application needs to be installed on a device.
     * @param selectedPeerIDs A set of strings that represents the selected peers.
     */
    public static void requestRemoteAppOpen(String tag, String packageName, String appName, String lockTag, String install, Set<String> selectedPeerIDs) {
        LeadMeMain.getInstance().setProgressTimer(2000);
        Log.d(TAG, "requestRemoteAppOpen: ");
        lastEvent=2;
        tagRepush = tag;
        packageNameRepush = packageName;
        appNameRepush = appName;
        lockTagRepush = lockTag;
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(tag);
        p.writeString(packageName);
        p.writeString(appName);
        p.writeString(lockTag);
        p.writeString(install);
        p.writeString("");
        p.writeString("");
        bytes = p.marshall();
        p.recycle();
        writeMessageToSelected(bytes, selectedPeerIDs);

        LeadMeMain.getInstance().updateLastTask(AppManager.getAppIcon(packageName), AppManager.getAppName(packageName), packageName, lockTag);
    }

    public synchronized void alertLogout() {
        ArrayList<String> selected = new ArrayList<>();
        for (String peer : NearbyPeersManager.getAllPeerIDs()) {
            selected.add(peer);
        }
        NetworkManager.sendToSelectedClients("DISCONNECT", "DISCONNECT", selected);
    }

    public static synchronized void sendActionToSelected(String actionTag, String action, Set<String> selectedPeerIDs) {
        Log.d(TAG, "sendActionToSelected: " + actionTag + " " + action);
        LeadMeMain.getInstance().setProgressTimer(2000);
        if(action.contains(Controller.LAUNCH_URL) || action.contains(Controller.LAUNCH_YT)) {
            lastEvent = 3;
            mActionTag = actionTag;
            mAction = action;
        }
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(actionTag);
        p.writeString(action);
        bytes = p.marshall();
        p.recycle();

        //auto-install, request, notification tags and success tag are exempt so students can alert teacher to their status
        NearbyPeersManager.sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
    }

    public static String encodeMessage(String actionTag, String action) {
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(actionTag);
        p.writeString(action);
        bytes = p.marshall();

        p.unmarshall(bytes, 0, Objects.requireNonNull(bytes).length);
        p.setDataPosition(0);
        byte[] b = p.marshall();
        p.recycle();

        return Base64.getEncoder().encodeToString(b);
    }

    public synchronized boolean readAction(byte[] bytes) {
        if(main.setProgressTimer(-1)!=null){
            main.setProgressTimer(-1).setVisibility(View.INVISIBLE);
        }
        Parcel p = Parcel.obtain();

        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        String actionTag = p.readString();
        String action = p.readString();
        p.recycle();

        if (action == null) {
            return false;
        }

        Log.d(TAG, "Received action: " + actionTag + ", " + action);

        if (actionTag != null && actionTag.equals(Controller.ACTION_TAG)) {

            switch (action) {
                case Controller.PING_ACTION:
                    dispatchAction.stillAlivePing(action);
                    break;

                case Controller.RETURN_TAG:
                    dispatchAction.returnToLeadMe();
                    break;

                case Controller.EXIT_TAG:
                    dispatchAction.endSession();
                    break;

                case Controller.LAUNCH_ACCESS:
                    dispatchAction.requestAccessibility();
                    break;

                case Controller.NAME_REQUEST:
                    dispatchAction.issueNameChange();
                    break;

                default:
                    if (action.startsWith(Controller.YOUR_ID_IS)) {
//                        dispatchAction.setPeerID(action); todo - unneeded now, but don't want to break things by removing this if statement right now

                    } else if(action.startsWith(Controller.NAME_CHANGE)) {
                        dispatchAction.nameChange(action);

                    } else if(action.startsWith(Controller.NAME_REQUEST)){
                        dispatchAction.requestNameChange(action);

                    } else if (action.startsWith(Controller.VID_MUTE_TAG)) {
                        dispatchAction.muteLearner();

                    } else if (action.startsWith(Controller.VID_UNMUTE_TAG)) {
                        dispatchAction.unMuteLearner();

                    } else if (action.startsWith(Controller.VID_ACTION_TAG)) {
                        dispatchAction.youtubeAction(action);

                    } else if (action.startsWith(Controller.VR_PLAYER_TAG)) {
                        dispatchAction.vrPlayerAction(action);

                    } else if(action.startsWith(Controller.FILE_REQUEST_TAG)) {
                        dispatchAction.requestFile(action);

                    } else if (action.startsWith(Controller.STUDENT_FINISH_ADS)) {
                        dispatchAction.adsFinished();

                    } else if (action.startsWith(Controller.LOGOUT_TAG)) {
                        dispatchAction.logout(action);

                    } else if (action.startsWith(Controller.LOCK_TAG)) {
                        dispatchAction.lockDevice();

                    } else if (action.startsWith(Controller.UNLOCK_TAG)) {
                        dispatchAction.unlockDevice();

                    } else if (action.startsWith(Controller.BLACKOUT_TAG)) {
                        dispatchAction.blackout();

                    } else if (action.startsWith(Controller.TRANSFER_ERROR)) {
                        dispatchAction.transferError(action);

                    } else if(action.startsWith(Controller.UPDATE_DEVICE_MESSAGE)) {
                        dispatchAction.updateDeviceMessage(action);

                    } else if(action.startsWith(Controller.MULTI_INSTALL)) {
                        dispatchAction.multiInstall(action);

                    } else if(action.startsWith(Controller.APP_NOT_INSTALLED)) {
                        dispatchAction.applicationNotInstalled(action);

                    } else if (action.startsWith(Controller.AUTO_INSTALL_FAILED)) {
                        dispatchAction.autoInstallFail(action);

                    } else if(action.startsWith(Controller.COLLECT_APPS)) {
                        dispatchAction.collectApplications();

                    } else if(action.startsWith(Controller.APP_COLLECTION)) {
                        dispatchAction.applicationCollection(action);

                    } else if(action.startsWith(Controller.AUTO_UNINSTALL)) {
                        dispatchAction.uninstallApplication(action);

                    } else if (action.startsWith(Controller.DISCONNECTION)) {
                        dispatchAction.disconnectLearner(action);

                    } else if (action.startsWith(Controller.LAUNCH_URL)) {
                        dispatchAction.launchURL(action);

                    } else if (action.startsWith(Controller.LAUNCH_YT)) {
                        dispatchAction.launchYoutube(action);

                    } else if (action.startsWith(Controller.OPEN_CURATED_CONTENT)) {
                        dispatchAction.openCuratedContent();
                    } else {
                        dispatchAction.askPermission(action);
                        dispatchAction.updatePeerStatus(action);
                    }
            }
            return true;

        } else {
            return false;
        }

    }

    public static boolean hasDelayedLaunchContent() {
        return launchAppOnFocus != null || LeadMeMain.appIntentOnFocus != null;
    }

    public static void launchDelayedApp() {
        LeadMeMain.getInstance().verifyOverlay();
        Log.d(TAG, "[XX] Have something to launch? " + Arrays.toString(launchAppOnFocus));
        if (launchAppOnFocus != null) {
            final String[] tmp = launchAppOnFocus;
            launchAppOnFocus = null; //reset
            Controller.getInstance().getAppManager().launchLocalApp(tmp[0], tmp[1], true, false, "false", null);
        }
    }

    public boolean openApp(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        final String tag = p.readString();
        final String packageName = p.readString();
        final String appName = p.readString();
        final String lockTag = p.readString();
        final String extra = p.readString(); //represents the Within URI or the Install function
        final String streaming = p.readString();
        final String vrMode = p.readString();

        p.recycle();

        Log.d(TAG, "Received in OpenApp!: " + tag + ", " + packageName + ", " + appName + ", " + lockTag + ", " + extra + ", " + streaming + ", " + vrMode + " vs " + AppManager.lastApp);
        if (tag != null && tag.equals(Controller.APP_TAG)) {
            if (lockTag.equals(Controller.LOCK_TAG)) {
                dispatchAction.lockDevice();

            } else if (lockTag.equals(Controller.UNLOCK_TAG)) {
                //I've been selected to toggle student lock
//                main.blackout(false);
//                disableInteraction(ConnectedPeer.STATUS_UNLOCK);
//                sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "LOCKOFF" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
//                        main.getNearbyManager().getAllPeerIDs());

                //TODO this is slightly different so test before thoroughly before fully implementing
                dispatchAction.unlockDevice();
            }

            boolean appInForeground = main.isAppVisibleInForeground();
            Log.w(TAG, "[2] No URI, reset state!");
            main.getLumiAccessibilityConnector().resetState();

            if (!appInForeground) {
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
                LeadMeMain.UIHandler.post(() -> Controller.getInstance().getAppManager().launchLocalApp(packageName, appName, true, false, extra, null));

            }
            return true;
        } else {
            Log.d(TAG, "Nope, not an app request");
            return false;
        }
    }


    public static void alertGuidePermissionGranted(String whichPermission, boolean isGranted) {
        if (isGranted) {
            sendActionToSelected(Controller.ACTION_TAG, whichPermission + "OK:" + NearbyPeersManager.getID(), NearbyPeersManager.getAllPeerIDs());
        } else {
            sendActionToSelected(Controller.ACTION_TAG, whichPermission + "FAIL:" + NearbyPeersManager.getID(), NearbyPeersManager.getAllPeerIDs());
        }
    }

    public static void alertGuideStudentOffTask() {
        sendActionToSelected(Controller.ACTION_TAG, Controller.STUDENT_OFF_TASK_ALERT + NearbyPeersManager.getID(),
                NearbyPeersManager.getAllPeerIDs());
    }

    /**
     * Creates a dialog asking for a peer to allow a certain permission, no dialog is present for disabling
     * the permission. Used for auto installing applications and transferring files between devices.
     * @param permission A string representing what permission wanting to be allowed.
     * @param enable A boolean representing if the permission is being turned on or off.
     */
    public static void askForPeerPermission(String permission, Boolean enable) {
        String msg;

        switch(permission) {
            case Controller.FILE_TRANSFER:
                if(enable) {
                    msg = "Guide wants to enable \nfile transfer services.";
                } else {
                    LeadMeMain.fileTransferEnabled = false;
                    return;
                }

                if(LeadMeMain.fileTransferEnabled) {
                    return;
                }

                break;

            case Controller.AUTO_INSTALL:
                if(enable) {
                    msg = "Guide wants to enable \nauto installing of applications.";
                } else {
                    LeadMeMain.autoInstallApps = false;
                    return;
                }

                if(LeadMeMain.autoInstallApps) {
                    return;
                }

                break;

            default:
                Log.e(TAG, "askForPeerPermission: Permission is not defined.");
                return;
        }

        Controller.getInstance().getDialogManager().showPermissionDialog(msg, permission);
    }

    /**
     * Determines if a permission has been allowed or denied, enables the permission if allowed.
     * @param permission A string representing what permission has been requested.
     * @param allowed A boolean representing if the peer has allowed or denied the request.
     */
    public static void permissionAllowed(String permission, Boolean allowed) {
        if(allowed) {
            switch (permission) {
                case Controller.FILE_TRANSFER:
                    LeadMeMain.fileTransferEnabled = true;
                    break;

                case Controller.AUTO_INSTALL:
                    LeadMeMain.autoInstallApps = true;
                    break;

                default:
                    break;
            }

            permissionGranted(permission);
        } else {
            permissionDenied(permission);
        }
    }

    /**
     * Sends an action back to the guide if the permission has been granted.
     * @param permission A string representing what permission has been granted.
     */
    public static void permissionGranted(String permission) {
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.PERMISSION_DENIED + ":" + "OK" + ":" + NearbyPeersManager.getID() + ":" + permission,
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }

    /**
     * Sends an action back to the guide if the permission has been denied.
     * @param permission A string representing what permission has been denied.
     */
    public static void permissionDenied(String permission) {
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                Controller.PERMISSION_DENIED + ":" + "BAD" + ":" + NearbyPeersManager.getID() + ":" + permission,
                NearbyPeersManager.getSelectedPeerIDsOrAll());
    }

    /**
     * Action functions associated with the dispatch manager class.
     */
    private class Actions {
        /**
         * Ping a device ID to establish if the socket connection is still alive.
         * @param action A string of the incoming action, it contains the ID of the peer relating
         *               to the ping.
         */
        private void stillAlivePing(String action) {
            Set<String> peerSet = new HashSet<>();
            peerSet.add(action.split(":")[1]);
            //send a response back to the ID that pinged me
            sendActionToSelected(Controller.PING_TAG, Controller.PING_ACTION + ":" + NearbyPeersManager.getID(), peerSet);
        }

        /**
         * Send a signal to the selected learners (or all) to return to the LeadMe home page.
         */
        private void returnToLeadMe() {
            Log.d(TAG, "Trying to return to " + LeadMeMain.leadMeAppName);
            sendActionToSelected(Controller.ACTION_TAG, Controller.LAUNCH_SUCCESS + LeadMeMain.leadMePackageName
                    + ":" + NearbyPeersManager.getID() + ":" + LeadMeMain.leadMePackageName, NearbyPeersManager.getAllPeerIDs());
            main.updateFollowerCurrentTaskToLeadMe();
            main.recallToLeadMe();
        }

        /**
         * Alert any connected devices that the session has been ended by the guide.
         */
        private void endSession() {
            Toast exitToast = Toast.makeText(main.getApplicationContext(), "Session ended by guide", Toast.LENGTH_SHORT);
            exitToast.show();
            main.exitByGuide();
        }

        /**
         * Request that a connected learner device turn on their accessibility settings.
         */
        private void requestAccessibility() {
            Controller.getInstance().getPermissionsManager().requestAccessibilitySettingsOn();
        }

        /**
         * Initiates a name change for a connected peer by the guide. Renaming the learner on the
         * guides device as well as the learners.
         */
        private void issueNameChange() {
            View NameChangeRequest = View.inflate(main, R.layout.d__name_change_request, null);
            AlertDialog studentNameChangeRequest = new AlertDialog.Builder(main)
                    .setView(NameChangeRequest)
                    .show();
            studentNameChangeRequest.setCancelable(false);
            studentNameChangeRequest.setCanceledOnTouchOutside(false);

            TextView newName = NameChangeRequest.findViewById(R.id.name_request_newname);
            Button confirm = NameChangeRequest.findViewById(R.id.name_request_confirm);
            confirm.setOnClickListener(v -> {
                String name = newName.getText().toString();
                if(newName.getText() != null && name.length() > 0){
                    NearbyPeersManager.myName = name;
                    main.changeStudentName(name);
                    sendActionToSelected(Controller.ACTION_TAG,
                            Controller.NAME_REQUEST + name
                                    + ":" + NearbyPeersManager.getID(),
                            NearbyPeersManager.getAllPeerIDs());
                    studentNameChangeRequest.dismiss();
                }
            });
        }

        /**
         * Change the name on a learner's device header.
         * @param action A string of the incoming action, it contains the new name for the selected peer.
         */
        private void nameChange(String action) {
            String[] Strsplit = action.split(":");
            NearbyPeersManager.myName = Strsplit[1];
            main.changeStudentName(Strsplit[1]);
        }

        /**
         * Change a learners name to the new one that has been picked by the learner. Guide has
         * previously sent a message asking them to input a new one.
         * @param action A string of the incoming action, it contains the new name chosen by the
         *              selected peer.
         */
        private void requestNameChange(String action) {
            //teachers handler for name change
            String[] Strsplit = action.split(":");

            ConnectedPeer peer = ConnectedLearnersAdapter.getMatchingPeer(Strsplit[2]);
            String oldName = peer.getDisplayName();
            peer.setName(Strsplit[1]);
            Controller.getInstance().getConnectedLearnersAdapter().refresh();
            View NameChangedConfirm = View.inflate(main, R.layout.f__name_push_confirm, null);
            AlertDialog studentNameConfirm = new AlertDialog.Builder(main)
                    .setView(NameChangedConfirm)
                    .show();
            LinearLayout changed = NameChangedConfirm.findViewById(R.id.name_changed_view);
            LinearLayout requested = NameChangedConfirm.findViewById(R.id.name_request_view);
            changed.setVisibility(View.VISIBLE);
            requested.setVisibility(View.GONE);
            TextView nameChangedText = NameChangedConfirm.findViewById(R.id.name_changed_text);
            nameChangedText.setText(oldName+"'s name was changed to "+Strsplit[1]);
            Button confirmName = NameChangedConfirm.findViewById(R.id.name_changed_confirm);
            confirmName.setOnClickListener(v -> studentNameConfirm.dismiss());
            studentNameConfirm.setOnDismissListener(dialog -> main.hideSystemUI());
        }

        /**
         * Mute a learners device.
         */
        private void muteLearner() {
            Log.d(TAG, "GOT MUTE");
            main.muteAudio();
        }

        /**
         * Unmute a learners device.
         */
        private void unMuteLearner() {
            Log.d(TAG, "GOT UNMUTE");
            main.unMuteAudio();
        }

        /**
         * Cue up an accessibility action for the youtube controller sent by the guide.
         * @param action A string of the incoming action, it contains action to be taken.
         */
        private void youtubeAction(String action) {
            Log.d(TAG, "GOT SOMETHING VID RELATED");
            String[] split = action.split(":");
            main.getLumiAccessibilityConnector().cueYouTubeAction(split[1]);
        }

        /**
         * Cue up an accessibility action for the custom VR player controller sent by the guide.
         * @param action A string of the incoming action, it contains action to be taken.
         */
        private void vrPlayerAction(String action) {
            Log.d(TAG, "VR PLAYER - " + packageNameRepush);
            String[] split = action.split(":");

            //Used to set the source but can be used in the future for projection changes etc.
            String additionalInfo = "";
            if(Integer.parseInt(split[1]) == VRAccessibilityManager.CUE_SET_SOURCE) {
                additionalInfo = split[2]; //video source
                additionalInfo += ":" + split[3]; //start time
                additionalInfo += ":" + split[4]; //content type
            }

            if(Integer.parseInt(split[1]) == VRAccessibilityManager.CUE_PROJECTION) {
                additionalInfo = split[2]; //projection type
            }
            Controller.getInstance().getVRAccessibilityManager().vrPlayerAction(Integer.parseInt(split[1]), additionalInfo);
        }

        /**
         * Notifies the guide that a file has been requested by a learner or that the file request
         * has finished transferring.
         * @param action A string of the incoming action, it contains the transfer status (true if finished
         *              or false if requesting) and the file type.
         */
        private void requestFile(String action) {
            String[] split = action.split(":");
            String ID = split[1];
            Log.e("FILE REQUEST", "Requesting: " + LeadMeMain.vrURI + " from: " + ID + " status: " + split[2] + " type: " + split[3]);

            //Set the file type for handling when complete
            FileTransferManager.setFileType(split[3]);

            //If false, then the learner needs the file, otherwise the transfer is complete - relaunch the app
            if(split[2].equals("false")) {
                LeadMeMain.fileRequests.add(ID);
                Controller.getInstance().getDialogManager().showRequestDialog(5);
            } else {
                Set<String> peer = new HashSet<>();
                peer.add(ID);

                //TODO change into a switch case if more request types are handled later
                if(FileTransferManager.getFileType().equals("Video")) {
                    Controller.getInstance().getVrEmbedVideoPlayer().relaunchVR(peer);
                }
                if(FileTransferManager.getFileType().equals("Photo")) {
                    Controller.getInstance().getVrEmbedPhotoPlayer().relaunchVR(peer);
                }
            }
        }

        /**
         * Alerts the guide that a peer's ads have finished on youtube.
         */
        private void adsFinished() {
            Controller.getInstance().getWebManager().getYouTubeEmbedPlayer().addPeerReady();
        }

        /**
         * Logs out .... ?
         * @param action A string of the incoming action, it contains action to be taken.
         */
        private void logout(String action) {
            String id = action.split(":")[1];
            Log.d(TAG, "Guide " + id + " has logged out!");
            NearbyPeersManager.disconnectFromEndpoint(id);
        }

        /**
         * Locks a learners device, placing it in the view only mode.
         */
        private void lockDevice() {
            //I've been selected to toggle student lock
            main.blackout(false);
            disableInteraction(ConnectedPeer.STATUS_LOCK);
            sendActionToSelected(Controller.ACTION_TAG,
                    Controller.LAUNCH_SUCCESS + "LOCKON" + ":" + NearbyPeersManager.getID() + ":" + LeadMeMain.leadMePackageName,
                    NearbyPeersManager.getAllPeerIDs());
        }

        /**
         * Unlocks a learners device, placing it in the play mode.
         */
        private void unlockDevice() {
            //I've been selected to toggle student lock
            main.blackout(false);
            disableInteraction(ConnectedPeer.STATUS_UNLOCK);
            main.getLumiAccessibilityConnector().manageAccessibilityEvent(null,null);
            sendActionToSelected(Controller.ACTION_TAG,
                    Controller.LAUNCH_SUCCESS + "LOCKOFF"
                            + ":" + NearbyPeersManager.getID()
                            + ":" + LeadMeMain.leadMePackageName,
                    NearbyPeersManager.getAllPeerIDs());
        }

        /**
         * Blacks out a learners device, making it so the screen cannot be interacted with.
         */
        private void blackout() {
            main.blackout(true);
            disableInteraction(ConnectedPeer.STATUS_BLACKOUT);
            sendActionToSelected(Controller.ACTION_TAG,
                    Controller.LAUNCH_SUCCESS + "BLACKOUT"
                            + ":" + NearbyPeersManager.getID()
                            + ":" + main.getApplicationContext().getPackageName(),
                    NearbyPeersManager.getAllPeerIDs());
        }

        /**
         * Update the guide in the case there is a error when transferring a file.
         * @param action A string of the incoming action, it contains the type of error that has occurred.
         */
        private void transferError(String action) {
            String[] split = action.split(":");
            Controller.getInstance().getFileTransferManager().removePeer(split[1], split[2]);
        }

        /**
         * Updates the message displayed on the home page of learner devices.
         * @param action A string of the incoming action, it contains the new message.
         */
        private void updateDeviceMessage(String action) {
            String[] split = action.split(":");
            main.setDeviceStatusMessage(Integer.parseInt(split[1]));
        }

        /**
         * Start the auto installer function with the supplied application package names.
         * @param action A string of the incoming action, it contains an array of applications to
         *               install.
         */
        private void multiInstall(String action) {
            String[] split = action.split(":"); //get the instructions
            String applications = split[2]; //get the array of apps currently in string form
            //change the string array into an array
            String[] appArray = applications.replace("[","").replace("]","").split(",");
            String[] firstApp = appArray[0].split("//");
            Controller.getInstance().getLumiAppInstaller().autoInstall(firstApp[0], firstApp[1], split[1], appArray);
        }

        /**
         * Notifies the guide that a pushed application is not on the learner devices. Adds that learner
         * to the peers to install array and asks if the guide wants to install the applications.
         * @param action A string of the incoming action, it contains the missing apps name, package
         *               name and the peer ID that send the action.
         */
        private void applicationNotInstalled(String action) {
            String[] split = action.split(":");
            //get the student id add it to the need to install array.
            Log.d(TAG, "Application needed on peer: " + split[3]);

            Controller.getInstance().getConnectedLearnersAdapter().appLaunchFail(split[3], appNameRepush);

            if(LeadMeMain.FLAG_INSTALLER) {
                Controller.getInstance().getLumiAppInstaller().peersToInstall.add(split[3]);

                //open a dialog to confirm if wanting to install apps
                Controller.getInstance().getLumiAppInstaller().applicationsToInstallWarning(split[1], split[2], false); //should auto update number of devices need as the action come in
            }
        }

        /**
         * Notify the guide that an installation error has occurred and update the peers icon
         * accordingly.
         * @param action A string of the incoming action, it contains the peer ID of the device that
         *               has failed and the package name that wasn't installed.
         */
        private void autoInstallFail(String action) {
            String[] split = action.split(":");
            Controller.getInstance().getConnectedLearnersAdapter().appLaunchFail(split[2], split[1]);

            //in this case, student will be back in LeadMe, so update icon too
            Controller.getInstance().getConnectedLearnersAdapter().updateIcon(split[2], AppManager.getAppIcon(LeadMeMain.leadMePackageName));
        }

        /**
         * Collect all applications (package name & app name) installed on a connected device
         */
        private void collectApplications() {
            List<String> applicationInfo = new ArrayList<>(Controller.getInstance().getAppManager().refreshAppList());

            //send back to the leader - placeholder for now
            sendActionToSelected(Controller.ACTION_TAG, Controller.APP_COLLECTION + ":" + applicationInfo,
                    NearbyPeersManager.getSelectedPeerIDs());
        }

        /**
         * Guide is receiving a list of applications that a connected learner has.
         * @param action A string of the incoming action, it contains an array of all the applications
         *               installed.
         */
        private void applicationCollection(String action) {
            String[] split = action.split(":"); //get the app array
            String applications = split[1]; //get the array of apps currently in string form
            //change the string array into an array
            String[] appArray = applications.replace("[", "").replace("]", "").split(",");

            Collections.addAll(Controller.getInstance().getLumiAppInstaller().peerApplications, appArray);
            Controller.getInstance().getLumiAppInstaller().populateUninstall();
        }

        /**
         * Uninstalls the applications that have been received in the action on a device.
         * Note: Currently not achievable as there is a Android UI button that has to manually be
         * pressed within the Play Store.
         * @param action A string of the incoming action, it contains an array of all the applications
         *               that are to be uninstalled.
         */
        private void uninstallApplication(String action) {
            String[] split = action.split(":"); //get the instructions
            String applications = split[2]; //get the array of apps currently in string form
            //change the string array into an array
            String[] appArray = applications.replace("[","").replace("]","").split(",");
            Collections.addAll(Controller.getInstance().getLumiAppInstaller().appsToManage, appArray);

            Controller.getInstance().getLumiAppInstaller().runUninstaller();
        }

        /**
         * The guide is receiving a message that a learner has just abruptly disconnected.
         * @param action A string of the incoming action, it contains the ID of the disconnecting
         *               peer.
         */
        private void disconnectLearner(String action) {
            String[] split = action.split(":"); //get the peer ID

            Log.e(TAG, split[1] + " has just disconnected");
            NetworkManager.updateParent(split[1] + " has disconnected", split[1], "LOST");
        }

        /**
         * Launch a URL using the default web browser (set to chrome)
         * @param action A string of the incoming action, it contains the URL of the site to be
         *               launched.
         */
        private void launchURL(String action) {
            String[] split = action.split(":::", 3);
            Controller.getInstance().getWebManager().launchWebsite(split[1], split[2], true);
//            main.url_overlay.setVisibility(View.VISIBLE);
        }

        /**
         * Launch a Youtube experience with the URL supplied.
         * @param action A string of the incoming action, it contains the URL of the video to be
         *               launched.
         */
        private void launchYoutube(String action) {
            String[] split = action.split(":::", 4);
            Controller.getInstance().getWebManager().launchYouTube(split[1], split[2], split[3].equals("true"), true);
            Log.w(TAG, action + "||" + split[1] + ", " + split[2] + ", " + split[3] + "|");
        }

        private void openCuratedContent() {
            CuratedContentManager.setupCuratedContent(main);
            main.showCuratedContentScreen();
            main.appLauncherScreen.findViewById(R.id.app_scroll_view).scrollTo(0, 0);
        }

        /**
         * Ask learners to turn on a specific permission.
         * @param action A string of the incoming action, it contains the permission that is requested.
         */
        private void askPermission(String action) {
            String[] split = action.split(":");

            if(action.startsWith(Controller.FILE_TRANSFER)) {
                askForPeerPermission(Controller.FILE_TRANSFER, Boolean.parseBoolean(split[1]));

            } else if(action.startsWith(Controller.AUTO_INSTALL)) {
                askForPeerPermission(Controller.AUTO_INSTALL, Boolean.parseBoolean(split[1]));
            }
        }

        /**
         * Update the learner icons on the guides device to reflect any changes to the status of
         * that peer.
         * @param action A string of the incoming action, it contains the update status and peer
         */
        private void updatePeerStatus(String action) {
            String[] split = action.split(":");

            if (action.startsWith(Controller.STUDENT_NO_OVERLAY)) {
                if (split[1].equalsIgnoreCase("OK")) {
                    LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, Controller.STUDENT_NO_OVERLAY);
                } else {
                    LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, Controller.STUDENT_NO_OVERLAY);
                }

            } else if (action.startsWith(Controller.STUDENT_OFF_TASK_ALERT)) {
                LeadMeMain.updatePeerStatus(split[1], ConnectedPeer.STATUS_WARNING, Controller.STUDENT_OFF_TASK_ALERT);

            } else if (action.startsWith(Controller.STUDENT_NO_INTERNET)) {
                if (split[1].equalsIgnoreCase("OK")) {
                    LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, Controller.STUDENT_NO_INTERNET);
                } else {
                    LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, Controller.STUDENT_NO_INTERNET);
                }

            } else if (action.startsWith(Controller.STUDENT_NO_ACCESSIBILITY)) {
                if (split[1].equalsIgnoreCase("OK")) {
                    LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, Controller.STUDENT_NO_ACCESSIBILITY);
                } else {
                    LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, Controller.STUDENT_NO_ACCESSIBILITY);
                }

            } else if (action.startsWith(Controller.PERMISSION_DENIED)) {
                if (split[3].equals(Controller.FILE_TRANSFER)) {
                    if (split[1].equalsIgnoreCase("OK")) {
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, Controller.PERMISSION_TRANSFER_DENIED);
                    } else {
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_INSTALLED, null);
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, Controller.PERMISSION_TRANSFER_DENIED);
                    }
                } else if (split[3].equals(Controller.AUTO_INSTALL)) {
                    if (split[1].equalsIgnoreCase("OK")) {
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, Controller.PERMISSION_AUTOINSTALL_DENIED);
                    } else {
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_INSTALLED, null);
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, Controller.PERMISSION_AUTOINSTALL_DENIED);
                    }
                }

                Log.d(TAG, "Peer: " + split[2] + " has denied permission: " + split[3]);

            } else if (action.startsWith(Controller.AUTO_INSTALL_ATTEMPT)) {
                LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_INSTALLING, null);

            } else if (action.startsWith(Controller.LAUNCH_SUCCESS)) {

                switch (split[1]) {
                    case "LOCKON":
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_LOCK, null);
                        break;

                    case "LOCKOFF":
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_UNLOCK, null);
                        break;

                    case "BLACKOUT":
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_BLACKOUT, null);
                        break;

                    default:
                        if (split[1].equals("INSTALLED")) { //remove the downloading icon from the peer
                            LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_INSTALLED, null);
                        }
                        LeadMeMain.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, Controller.STUDENT_OFF_TASK_ALERT);
                        Controller.getInstance().getConnectedLearnersAdapter().appLaunchSuccess(split[2], split[1]);
                        Controller.getInstance().getConnectedLearnersAdapter().updateIcon(split[2], AppManager.getAppIcon(split[3]));
                }
            }
        }
    }
}
