package com.lumination.leadme;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DispatchManager {
    private final String TAG = "Dispatcher";
    private final LeadMeMain main;
    protected String[] launchAppOnFocus = null;
    String tagRepush;
    String packageNameRepush;
    String appNameRepush;
    String lockTagRepush;
    String extraRepush;
    boolean streamingRepush;
    boolean vrModeRepush;
    String mActionTag=null, mAction=null;
    int lastEvent =0;

    Actions dispatchAction = new Actions();

    public DispatchManager(LeadMeMain main) {
        this.main = main;
    }

    public void repushApp(Set<String> selectedPeerIDs) {
        Log.w(TAG, "REPUSH: " + tagRepush + ", " + packageNameRepush + ", " + appNameRepush + ", " + lockTagRepush + ", " + extraRepush);
        main.recallToLeadMe();
        switch(lastEvent){
            case 1:
                requestRemoteWithinLaunch(tagRepush, packageNameRepush, appNameRepush, lockTagRepush, extraRepush, streamingRepush, vrModeRepush, selectedPeerIDs);
                break;
            case 2:
                requestRemoteAppOpen(tagRepush, packageNameRepush, appNameRepush, lockTagRepush, "false", selectedPeerIDs);
                break;
            case 3:
                sendActionToSelected(mActionTag,mAction,selectedPeerIDs);
                break;
        }
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

    /**
     * Launch a within experience on the learner's devices with a set video source to stream or download.
     * @param tag A string ....
     * @param packageName A string representing if the package is within.
     * @param appName A string representing the name of the application 'Within'.
     * @param lockTag A string representing if the learner devices should be in lock or play mode.
     * @param extra A string representing the URL of the video to launch.
     * @param streaming A boolean determining if the video is to be streamed or downloaded.
     * @param vrMode A boolean determining if launching in VR mode.
     * @param selectedPeerIDs A set of strings that represents the selected peers.
     */
    public void requestRemoteWithinLaunch(String tag, String packageName, String appName, String lockTag, String extra, boolean streaming, boolean vrMode, Set<String> selectedPeerIDs) {
        Log.d(TAG, "requestRemoteWithinLaunch: " + extra);
        lastEvent=1;
        tagRepush = tag;
        packageNameRepush = packageName;
        appNameRepush = appName;
        lockTagRepush = lockTag;
        extraRepush = extra;
        streamingRepush = streaming;
        vrModeRepush = vrMode;
        Parcel p = Parcel.obtain();
        byte[] bytes;
        p.writeString(tag);
        p.writeString(packageName);
        p.writeString(appName);
        p.writeString(lockTag);
        p.writeString(extra);
        p.writeString(streaming + "");
        p.writeString(vrMode + "");
        bytes = p.marshall();
        p.recycle();
        writeMessageToSelected(bytes, selectedPeerIDs);
        main.updateLastTask(main.getAppManager().getAppIcon(packageName), main.getAppManager().getAppName(packageName), packageName, lockTag);
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
    public void requestRemoteAppOpen(String tag, String packageName, String appName, String lockTag, String install, Set<String> selectedPeerIDs) {
        main.setProgressTimer(2000);
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

        main.updateLastTask(main.getAppManager().getAppIcon(packageName), main.getAppManager().getAppName(packageName), packageName, lockTag);

        if (!packageName.equals(main.getAppManager().withinPackage)) {
            main.getAppManager().getWithinPlayer().foundURL = "";
        }
    }

    public synchronized void alertLogout() {
        //sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOGOUT_TAG + ":" + main.getNearbyManager().getID(), main.getNearbyManager().getAllPeerIDs());

        ArrayList<Integer> selected = new ArrayList<>();
        Iterator<String> iterator = main.getNearbyManager().getAllPeerIDs().iterator();
        while (iterator.hasNext()) {
            String peer = (String) iterator.next();
            Log.d(TAG, "sendToSelected: " + peer);
            selected.add(Integer.parseInt(peer));
        }
        main.getNearbyManager().networkAdapter.sendToSelectedClients("DISCONNECT", "DISCONNECT", selected);
    }

    public synchronized void sendActionToSelected(String actionTag, String action, Set<String> selectedPeerIDs) {
        main.setProgressTimer(2000);
        if(action.contains(LeadMeMain.LAUNCH_URL) || action.contains(LeadMeMain.LAUNCH_YT)) {
            Log.d(TAG, "sendActionToSelected: ");
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
        if (main.getNearbyManager().isConnectedAsGuide() ||
                action.startsWith(LeadMeMain.YOUR_ID_IS) ||
                action.startsWith(LeadMeMain.RETURN_TAG) ||
                action.startsWith(LeadMeMain.PERMISSION_DENIED) ||
                action.startsWith(LeadMeMain.TRANSFER_ERROR) ||
                action.startsWith(LeadMeMain.FILE_REQUEST_TAG) ||
                action.startsWith(LeadMeMain.APP_NOT_INSTALLED) ||
                action.startsWith(LeadMeMain.AUTO_INSTALL_ATTEMPT) ||
                action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED) ||
                action.startsWith(LeadMeMain.STUDENT_NO_OVERLAY) ||
                action.startsWith(LeadMeMain.STUDENT_NO_INTERNET) ||
                action.startsWith(LeadMeMain.STUDENT_NO_ACCESSIBILITY) ||
                action.startsWith(LeadMeMain.STUDENT_OFF_TASK_ALERT) ||
                action.startsWith(LeadMeMain.STUDENT_FINISH_ADS) ||
                action.startsWith(LeadMeMain.PING_TAG) ||
                action.startsWith(LeadMeMain.LAUNCH_SUCCESS) ||
                action.startsWith(LeadMeMain.STUDENT_NO_XRAY) ||
                action.startsWith(LeadMeMain.DISCONNECTION) ||
                action.startsWith(LeadMeMain.NAME_REQUEST)) {
            main.getNearbyManager().sendToSelected(Payload.fromBytes(bytes), selectedPeerIDs);
        } else {
            Log.i(TAG, "Sorry, you can't send actions!");
        }
    }

    public synchronized void sendBoolToSelected(String actionTag, String action, boolean value, Set<String> selectedPeerIDs) {
        main.setProgressTimer(2000);
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

        if (action == null) {
            return false;
        }

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

        if (actionTag != null && actionTag.equals(LeadMeMain.ACTION_TAG)) {

            switch (action) {
                case LeadMeMain.XRAY_ON:
                    dispatchAction.turnOnXray();
                    break;

                case LeadMeMain.XRAY_OFF:
                    dispatchAction.turnOffXray();
                    break;

                case LeadMeMain.XRAY_REQUEST:
                    dispatchAction.requestXray();
                    break;

                case LeadMeMain.PING_ACTION:
                    dispatchAction.stillAlivePing(action);
                    break;

                case LeadMeMain.RETURN_TAG:
                    dispatchAction.returnToLeadMe();
                    break;

                case LeadMeMain.EXIT_TAG:
                    dispatchAction.endSession();
                    break;

                case LeadMeMain.LAUNCH_ACCESS:
                    dispatchAction.requestAccessibility();
                    break;

                case LeadMeMain.NAME_REQUEST:
                    dispatchAction.issueNameChange();
                    break;

                default:
                    if (action.startsWith(LeadMeMain.YOUR_ID_IS)) {
                        dispatchAction.setPeerID(action);

                    } else if(action.startsWith(LeadMeMain.NAME_CHANGE)) {
                        dispatchAction.nameChange(action);

                    } else if(action.startsWith(LeadMeMain.NAME_REQUEST)){
                        dispatchAction.requestNameChange(action);

                    } else if (action.startsWith(LeadMeMain.VID_MUTE_TAG)) {
                        dispatchAction.muteLearner();

                    } else if (action.startsWith(LeadMeMain.VID_UNMUTE_TAG)) {
                        dispatchAction.unMuteLearner();

                    } else if (action.startsWith(LeadMeMain.VID_ACTION_TAG)) {
                        dispatchAction.youtubeAction(action);

                    } else if (action.startsWith(LeadMeMain.VR_PLAYER_TAG)) {
                        dispatchAction.vrPlayerAction(action);

                    } else if(action.startsWith(LeadMeMain.FILE_REQUEST_TAG)) {
                        dispatchAction.requestFile(action);

                    } else if (action.startsWith(LeadMeMain.STUDENT_FINISH_ADS)) {
                        dispatchAction.adsFinished();

                    } else if (action.startsWith(LeadMeMain.LOGOUT_TAG)) {
                        dispatchAction.logout(action);

                    } else if (action.startsWith(LeadMeMain.LOCK_TAG)) {
                        dispatchAction.lockDevice();

                    } else if (action.startsWith(LeadMeMain.UNLOCK_TAG)) {
                        dispatchAction.unlockDevice();

                    } else if (action.startsWith(LeadMeMain.BLACKOUT_TAG)) {
                        dispatchAction.blackout();

                    } else if (action.startsWith(LeadMeMain.TRANSFER_ERROR)) {
                        dispatchAction.transferError(action);

                    } else if(action.startsWith(LeadMeMain.UPDATE_DEVICE_MESSAGE)) {
                        dispatchAction.updateDeviceMessage(action);

                    } else if(action.startsWith(LeadMeMain.MULTI_INSTALL)) {
                        dispatchAction.multiInstall(action);

                    } else if(action.startsWith(LeadMeMain.APP_NOT_INSTALLED)) {
                        dispatchAction.applicationNotInstalled(action);

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED)) {
                        dispatchAction.autoInstallFail(action);

                    } else if(action.startsWith(LeadMeMain.COLLECT_APPS)) {
                        dispatchAction.collectApplications();

                    } else if(action.startsWith(LeadMeMain.APP_COLLECTION)) {
                        dispatchAction.applicationCollection(action);

                    } else if(action.startsWith(LeadMeMain.AUTO_UNINSTALL)) {
                        dispatchAction.uninstallApplication(action);

                    } else if (action.startsWith(LeadMeMain.DISCONNECTION)) {
                        dispatchAction.disconnectLearner(action);

                    } else if (action.startsWith(LeadMeMain.LAUNCH_URL)) {
                        dispatchAction.launchURL(action);

                    } else if (action.startsWith(LeadMeMain.LAUNCH_YT)) {
                        dispatchAction.launchYoutube(action);

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

    public boolean hasDelayedLaunchContent() {
        return launchAppOnFocus != null || main.appIntentOnFocus != null;
    }

    public void launchDelayedApp() {
        main.verifyOverlay();
        Log.d(TAG, "[XX] Have something to launch? " + launchAppOnFocus);
        if (launchAppOnFocus != null) {
            final String[] tmp = launchAppOnFocus;
            launchAppOnFocus = null; //reset
            main.getAppManager().launchLocalApp(tmp[0], tmp[1], true, false, "false", null);
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

        Log.d(TAG, "Received in OpenApp!: " + tag + ", " + packageName + ", " + appName + ", " + lockTag + ", " + extra + ", " + streaming + ", " + vrMode + " vs " + main.getAppManager().lastApp);
        if (tag != null && tag.equals(LeadMeMain.APP_TAG)) {
            if (lockTag.equals(LeadMeMain.LOCK_TAG)) {
                dispatchAction.lockDevice();

            } else if (lockTag.equals(LeadMeMain.UNLOCK_TAG)) {
                //I've been selected to toggle student lock
//                main.blackout(false);
//                disableInteraction(ConnectedPeer.STATUS_UNLOCK);
//                sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "LOCKOFF" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
//                        main.getNearbyManager().getAllPeerIDs());

                //TODO this is slightly different so test before thoroughly before fully implementing
                dispatchAction.unlockDevice();
            }

            boolean appInForeground = main.isAppVisibleInForeground();
            if (packageName.equals(main.getAppManager().withinPackage)) {
                if (!extra.isEmpty()) {
                    // save all the info for a Within launch
                    Uri thisURI = Uri.parse(extra);

                    if (main.getAppManager().withinURI != null && main.getAppManager().withinURI.equals(thisURI) && !main.isAppVisibleInForeground()) {
                        Log.w(TAG, "We're already playing " + thisURI.toString() + "! Ignoring...");
                        return true; //successfully extracted the details, no action needed

                    } else {
                        Log.e(TAG, String.valueOf(thisURI));
                        main.getAppManager().withinURI = thisURI;
                        main.getAppManager().isStreaming = Boolean.parseBoolean(streaming);
                        main.getAppManager().isVR = Boolean.parseBoolean(vrMode);
                        Log.d(TAG, "Setting streaming status, vrMode and URL for WITHIN VR " + extra + ", " + streaming + ", " + vrMode + ", (" + appInForeground + ")");

                    }
                } else {
                    Log.w(TAG, "[1] No URI, reset state!");
                    //no URL was specified, so clear any previous info
                    main.getAppManager().withinURI = null;
                    main.getLumiAccessibilityConnector().resetState();

                }
            } else {
                Log.w(TAG, "[2] No URI, reset state!");
                //no URL was specified, so clear any previous info
                main.getAppManager().withinURI = null;
                main.getLumiAccessibilityConnector().resetState();
            }

            if (!appInForeground) {//!main.getAppLaunchAdapter().lastApp.equals(packageName)) {
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
                main.getHandler().post(() -> main.getAppManager().launchLocalApp(packageName, appName, true, false, extra, null));

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

    /**
     * Action functions associated with the dispatch manager class.
     */
    private class Actions {
        /**
         * Detect if the screen capture service for a learner device is granted permission. Ask for
         * permission if not, otherwise connect to the server and start sending images.
         */
        private void turnOnXray() {
            if(!main.screenCap.permissionGranted){
                main.screenCap.startService(true);
                return;
            }
            if(main.screenCap.clientToServerSocket==null){
                main.screenCap.connectToServer();
            }

            main.screenCap.sendImages=true;
        }

        /**
         * Stop sending images to the screenCap server.
         */
        private void turnOffXray() {
            main.screenCap.sendImages=false;
        }

        /**
         * Ping a device ID to establish if the socket connection is still alive.
         * @param action A string of the incoming action, it contains the ID of the peer relating
         *               to the ping.
         */
        private void stillAlivePing(String action) {
            Set<String> peerSet = new HashSet<>();
            peerSet.add(action.split(":")[1]);
            //send a response back to the ID that pinged me
            sendActionToSelected(LeadMeMain.PING_TAG, LeadMeMain.PING_ACTION + ":" + main.getNearbyManager().getID(), peerSet);
        }

        /**
         * Send a signal to the selected learners (or all) to return to the LeadMe home page.
         */
        private void returnToLeadMe() {
            Log.d(TAG, "Trying to return to " + main.leadMeAppName);
            main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + main.leadMePackageName
                    + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName, main.getNearbyManager().getAllPeerIDs());
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
            main.getPermissionsManager().requestAccessibilitySettingsOn();
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
                    main.getNearbyManager().myName = name;
                    main.changeStudentName(name);
                    sendActionToSelected(LeadMeMain.ACTION_TAG,LeadMeMain.NAME_REQUEST+name+":"+main.getNearbyManager().getID(),main.getNearbyManager().getAllPeerIDs());
                    studentNameChangeRequest.dismiss();
                }
            });
        }

        /**
         * Sets the ID of a connected peer as assigned by a guide.
         * @param action A string of the incoming action, it contains the new ID for the peer.
         */
        private void setPeerID(String action) {
            String[] split = action.split(":");
            if (split.length == 3) {
                //Now I know my ID! Store it.
                Log.d(TAG, ">>> INCOMING: " + action + " vs " + main.getNearbyManager().getName() + " // " + main.getNearbyManager().getID());
                if (split[2].equals(main.getUUID())) {
                    Log.d(TAG, "My peer tells me my ID is " + split[1] + " -- " + action + ", " + main.getNearbyManager().getName() + "/" + main.getUUID());
                    main.getNearbyManager().setID(split[1]);
                }
            }
        }

        /**
         * Change the name on a learner's device header.
         * @param action A string of the incoming action, it contains the new name for the selected peer.
         */
        private void nameChange(String action) {
            String[] Strsplit = action.split(":");
            main.getNearbyManager().myName=Strsplit[1];
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

            ConnectedPeer peer = main.getConnectedLearnersAdapter().getMatchingPeer(Strsplit[2]);
            String oldName = peer.getDisplayName();
            peer.setName(Strsplit[1]);
            main.getConnectedLearnersAdapter().refresh();
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
            }

            main.getVRAccessibilityManager().videoPlayerAction(Integer.parseInt(split[1]), additionalInfo);
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
            Log.e("FILE REQUEST", "Requesting: " + main.vrVideoURI + " from: " + ID + " status: " + split[2] + " type: " + split[3]);

            //Set the file type for handling when complete
            FileTransfer.setFileType(split[3]);

            //If false, then the learner needs the file, otherwise the transfer is complete - relaunch the app
            if(split[2].equals("false")) {
                main.fileRequests.add(Integer.parseInt(ID));
                main.getDialogManager().showRequestDialog(5);
            } else {
                Set<String> peer = new HashSet<>();
                peer.add(ID);

                //TODO change into a switch case if more request types are handled later
                if(FileTransfer.getFileType().equals("VRVideo")) {
                    main.getVrEmbedPlayer().relaunchVR(peer);
                }
            }
        }

        /**
         * Alerts the guide that a peer's ads have finished on youtube.
         */
        private void adsFinished() {
            main.getWebManager().getYouTubeEmbedPlayer().addPeerReady();
        }

        /**
         * Logs out .... ?
         * @param action A string of the incoming action, it contains action to be taken.
         */
        private void logout(String action) {
            String id = action.split(":")[1];
            Log.d(TAG, "Guide " + id + " has logged out!");
            main.getNearbyManager().disconnectFromEndpoint(id);
        }

        /**
         * Locks a learners device, placing it in the view only mode.
         */
        private void lockDevice() {
            //I've been selected to toggle student lock
            main.blackout(false);
            disableInteraction(ConnectedPeer.STATUS_LOCK);
            sendActionToSelected(LeadMeMain.ACTION_TAG,
                    LeadMeMain.LAUNCH_SUCCESS + "LOCKON" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
                    main.getNearbyManager().getAllPeerIDs());
        }

        /**
         * Unlocks a learners device, placing it in the play mode.
         */
        private void unlockDevice() {
            //I've been selected to toggle student lock
            main.blackout(false);
            disableInteraction(ConnectedPeer.STATUS_UNLOCK);
            main.getLumiAccessibilityConnector().manageAccessibilityEvent(null,null);
            sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "LOCKOFF" + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName,
                    main.getNearbyManager().getAllPeerIDs());
        }

        /**
         * Blacks out a learners device, making it so the screen cannot be interacted with.
         */
        private void blackout() {
            main.blackout(true);
            disableInteraction(ConnectedPeer.STATUS_BLACKOUT);
            sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "BLACKOUT" + ":" + main.getNearbyManager().getID() + ":" + main.getApplicationContext().getPackageName(),
                    main.getNearbyManager().getAllPeerIDs());
        }

        /**
         * Update the guide in the case there is a error when transferring a file.
         * @param action A string of the incoming action, it contains the type of error that has occurred.
         */
        private void transferError(String action) {
            String[] split = action.split(":");
            main.getFileTransfer().removePeer(split[1], split[2]);
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
            main.getLumiAppInstaller().autoInstall(firstApp[0], firstApp[1], split[1], appArray);
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

            Toast.makeText(main.getApplicationContext(), "Application needed on peer: " + split[3], Toast.LENGTH_SHORT).show();

            //TODO AUTO INSTALLER - uncomment below to enable auto installer
            main.getLumiAppInstaller().peersToInstall.add(split[3]);

            //open a dialog to confirm if wanting to install apps
            main.getLumiAppInstaller().applicationsToInstallWarning(split[1], split[2], false); //should auto update number of devices need as the action come in
        }

        /**
         * Notify the guide that an installation error has occurred and update the peers icon
         * accordingly.
         * @param action A string of the incoming action, it contains the peer ID of the device that
         *               has failed and the package name that wasn't installed.
         */
        private void autoInstallFail(String action) {
            String[] split = action.split(":");
            main.getConnectedLearnersAdapter().appLaunchFail(split[2], split[1]);

            //in this case, student will be back in LeadMe, so update icon too
            main.getConnectedLearnersAdapter().updateIcon(split[2], main.getAppManager().getAppIcon(main.leadMePackageName));
        }

        /**
         * Collect all applications (package name & app name) installed on a connected device
         */
        private void collectApplications() {
            List<String> applicationInfo = new ArrayList<String>(main.getAppManager().refreshAppList());

            //send back to the leader - placeholder for now
            sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.APP_COLLECTION + ":" + applicationInfo,
                    main.getNearbyManager().getSelectedPeerIDs());
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

            Collections.addAll(main.getLumiAppInstaller().peerApplications, appArray);
            main.getLumiAppInstaller().populateUninstall();
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
            Collections.addAll(main.getLumiAppInstaller().appsToManage, appArray);

            main.getLumiAppInstaller().runUninstaller();
        }

        /**
         * The guide is recieving a message that a learner has just abruptly disconnected.
         * @param action A string of the incoming action, it contains the ID of the disconnecting
         *               peer.
         */
        private void disconnectLearner(String action) {
            String[] split = action.split(":"); //get the peer ID

            //TODO disconnect this student
            Log.e(TAG, split[1] + " has just disconnected");
            main.getNearbyManager().networkAdapter.updateParent("Blah", Integer.parseInt(split[1]), "LOST");
        }

        /**
         * Launch a URL using the default web browser (set to chrome)
         * @param action A string of the incoming action, it contains the URL of the site to be
         *               launched.
         */
        private void launchURL(String action) {
            String[] split = action.split(":::", 3);
            main.getWebManager().launchWebsite(split[1], split[2], true);
//            main.url_overlay.setVisibility(View.VISIBLE);
        }

        /**
         * Launch a Youtube experience with the URL supplied.
         * @param action A string of the incoming action, it contains the URL of the video to be
         *               launched.
         */
        private void launchYoutube(String action) {
            String[] split = action.split(":::", 4);
            main.getWebManager().launchYouTube(split[1], split[2], split[3].equals("true"), true);
            Log.w(TAG, action + "||" + split[1] + ", " + split[2] + ", " + split[3] + "|");
        }

        /**
         * Ask learners to turn on a specific permission.
         * @param action A string of the incoming action, it contains the permission that is requested.
         */
        private void askPermission(String action) {
            String[] split = action.split(":");

            if(action.startsWith(LeadMeMain.FILE_TRANSFER)) {
                main.askForPeerPermission(LeadMeMain.FILE_TRANSFER, Boolean.parseBoolean(split[1]));

            } else if(action.startsWith(LeadMeMain.AUTO_INSTALL)) {
                main.askForPeerPermission(LeadMeMain.AUTO_INSTALL, Boolean.parseBoolean(split[1]));
            }
        }

        /**
         * Ask a learner to turn on the screen sharing service. Is only activated if the service is
         * not accepted initially or the service is terminated during a session through the
         * student alerts area.
         */
        private void requestXray() {
            main.screenCap.startService(false);
        }

        /**
         * Update the learner icons on the guides device to reflect any changes to the status of
         * that peer.
         * @param action A string of the incoming action, it contains the update status and peer
         */
        private void updatePeerStatus(String action) {
            String[] split = action.split(":");

            if (action.startsWith(LeadMeMain.STUDENT_NO_OVERLAY)) {
                if (split[1].equalsIgnoreCase("OK")) {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_OVERLAY);
                } else {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_OVERLAY);
                }

            } else if (action.startsWith(LeadMeMain.STUDENT_OFF_TASK_ALERT)) {
                main.updatePeerStatus(split[1], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_OFF_TASK_ALERT);

            } else if (action.startsWith(LeadMeMain.STUDENT_NO_INTERNET)) {
                if (split[1].equalsIgnoreCase("OK")) {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_INTERNET);
                } else {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_INTERNET);
                }

            } else if(action.startsWith(LeadMeMain.STUDENT_NO_XRAY)){
                if (split[1].equalsIgnoreCase("OK")) {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_XRAY);
                } else {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_XRAY);
                }

            } else if (action.startsWith(LeadMeMain.STUDENT_NO_ACCESSIBILITY)) {
                if (split[1].equalsIgnoreCase("OK")) {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                } else {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                }

            } else if (action.startsWith(LeadMeMain.PERMISSION_DENIED)) {
                if (split[3].equals(LeadMeMain.FILE_TRANSFER)) {
                    if (split[1].equalsIgnoreCase("OK")) {
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.PERMISSION_TRANSFER_DENIED);
                    } else {
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.PERMISSION_TRANSFER_DENIED);
                    }
                } else if (split[3].equals(LeadMeMain.AUTO_INSTALL)) {
                    if (split[1].equalsIgnoreCase("OK")) {
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.PERMISSION_AUTOINSTALL_DENIED);
                    } else {
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.PERMISSION_AUTOINSTALL_DENIED);
                    }
                }

                Log.d(TAG, "Peer: " + split[2] + " has denied permission: " + split[3]);

            } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_ATTEMPT)) {
                    main.updatePeerStatus(split[2], ConnectedPeer.STATUS_INSTALLING, null);

            } else if (action.startsWith(LeadMeMain.LAUNCH_SUCCESS)) {

                switch (split[1]) {
                    case "LOCKON":
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_LOCK, null);
                        break;

                    case "LOCKOFF":
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_UNLOCK, null);
                        break;

                    case "BLACKOUT":
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_BLACKOUT, null);
                        break;

                    default:
                        if (split[1].equals("INSTALLED")) { //remove the downloading icon from the peer
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_INSTALLED, null);
                        }
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_OFF_TASK_ALERT);
                        main.getConnectedLearnersAdapter().appLaunchSuccess(split[2], split[1]);
                        main.getConnectedLearnersAdapter().updateIcon(split[2], main.getAppManager().getAppIcon(split[3]));
                }
            }
        }
    }
}
