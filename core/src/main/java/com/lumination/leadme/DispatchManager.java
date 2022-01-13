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
    private LeadMeMain main;
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

        //auto-install tags and success tag are exempt so students can alert teacher to their status
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
                    if(!main.screenCap.permissionGranted){
                        main.screenCap.startService(true);
                        break;
                    }
                    if(main.screenCap.clientToServerSocket==null){
                        main.screenCap.connectToServer();
                    }

                    main.screenCap.sendImages=true;

                    break;

                case LeadMeMain.XRAY_OFF:
                    main.screenCap.sendImages=false;
                    break;

                case LeadMeMain.PING_ACTION:
                    Set<String> peerSet = new HashSet<>();
                    peerSet.add(action.split(":")[1]);
                    //send a response back to the ID that pinged me
                    sendActionToSelected(LeadMeMain.PING_TAG, LeadMeMain.PING_ACTION + ":" + main.getNearbyManager().getID(), peerSet);
                    break;

                case LeadMeMain.RETURN_TAG:
                    Log.d(TAG, "Trying to return to " + main.leadMeAppName);
                    main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + main.leadMePackageName//todo
                            + ":" + main.getNearbyManager().getID() + ":" + main.leadMePackageName, main.getNearbyManager().getAllPeerIDs());
                    main.updateFollowerCurrentTaskToLeadMe();
                    main.recallToLeadMe();
                    break;

                case LeadMeMain.EXIT_TAG:
                    Toast exitToast = Toast.makeText(main.getApplicationContext(), "Session ended by guide", Toast.LENGTH_SHORT);
                    exitToast.show();
                    main.exitByGuide();
                    break;

                case LeadMeMain.LAUNCH_ACCESS:
                    main.getPermissionsManager().requestAccessibilitySettingsOn();
                    break;

                case LeadMeMain.NAME_REQUEST:
                    View NameChangeRequest = View.inflate(main, R.layout.d__name_change_request, null);
                    AlertDialog studentNameChangeRequest = new AlertDialog.Builder(main)
                            .setView(NameChangeRequest)
                            .show();
                    studentNameChangeRequest.setCancelable(false);
                    studentNameChangeRequest.setCanceledOnTouchOutside(false);

                    TextView newName = NameChangeRequest.findViewById(R.id.name_request_newname);
                    Button confirm = NameChangeRequest.findViewById(R.id.name_request_confirm);
                    confirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(newName.getText()!=null && newName.getText().toString().length()>0){
                                TextView title = main.leadmeAnimator.getCurrentView().findViewById(R.id.learner_title);
                                title.setText(newName.getText().toString());
                                sendActionToSelected(LeadMeMain.ACTION_TAG,LeadMeMain.NAME_REQUEST+newName.getText().toString()+":"+main.getNearbyManager().getID(),main.getNearbyManager().getAllPeerIDs());
                                studentNameChangeRequest.dismiss();
                            }
                        }
                    });
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

                    } else if(action.startsWith(LeadMeMain.NAME_CHANGE)) {
                        String[] Strsplit = action.split(":");
                        main.getNearbyManager().myName=Strsplit[1];
                        TextView title = main.leadmeAnimator.getCurrentView().findViewById(R.id.learner_title);
                        title.setText(Strsplit[1]);

                    } else if(action.startsWith(LeadMeMain.NAME_REQUEST)){
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

                    } else if (action.startsWith(LeadMeMain.VR_PLAYER_TAG)) {
                        Log.d(TAG, "VR PLAYER - " + packageNameRepush);
                        String[] split = action.split(":");
                        //Used to set the source but can be used in the future for projection changes etc.
                        String additionalInfo = "";
                        if(Integer.parseInt(split[1]) == VRAccessibilityManager.CUE_SET_SOURCE) {
                            additionalInfo = split[2]; //video source
                            additionalInfo += ":" + split[3]; //start time
                        }

                        main.getVRAccessibilityManager().videoPlayerAction(Integer.parseInt(split[1]), additionalInfo);

                    } else if(action.startsWith(LeadMeMain.FILE_REQUEST_TAG)) {
                        String[] split = action.split(":");
                        String ID = split[1];
                        Log.e("FILE REQUEST", "Requesting: " + main.vrVideoPath + " from " + ID + " status: " + split[2] + "type: " + split[3]);

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

                    } else if (action.startsWith(LeadMeMain.STUDENT_FINISH_ADS)) {
                        main.getWebManager().getYouTubeEmbedPlayer().addPeerReady();

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
//                        alertGuideStudentOffTask();
                        main.getLumiAccessibilityConnector().manageAccessibilityEvent(null,null);
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
                        if (split[1].equalsIgnoreCase("OK")) {
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_OVERLAY);
                        } else {
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_OVERLAY);
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.STUDENT_OFF_TASK_ALERT)) {
                        String[] split = action.split(":");
                        main.updatePeerStatus(split[1], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_OFF_TASK_ALERT);
                        break;

                    } else if (action.startsWith(LeadMeMain.STUDENT_NO_INTERNET)) {
                        String[] split = action.split(":");
                        if (split[1].equalsIgnoreCase("OK")) {
                            //clear previous flag
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_INTERNET);
                        } else {
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_INTERNET);
                        }
                        break;

                    } else if(action.startsWith(LeadMeMain.STUDENT_NO_XRAY)){
                        String[] split = action.split(":");
                        if (split[1].equalsIgnoreCase("OK")) {
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_XRAY);
                        } else {
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_XRAY);
                        }

                    } else if (action.startsWith(LeadMeMain.STUDENT_NO_ACCESSIBILITY)) {
                        String[] split = action.split(":");
                        if (split[1].equalsIgnoreCase("OK")) {
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_SUCCESS, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                        } else {
                            main.updatePeerStatus(split[2], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_NO_ACCESSIBILITY);
                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.PERMISSION_DENIED)) {
                        String[] split = action.split(":");
                        //TODO update the message to reflect what has happened
                        main.updatePeerStatus(split[1], ConnectedPeer.STATUS_WARNING, LeadMeMain.STUDENT_OFF_TASK_ALERT);
                        Log.e("PERMISSIONS", "Peer: " + split[1] + " has denied permission: " + split[2]);
                        break;

                    } else if (action.startsWith(LeadMeMain.TRANSFER_ERROR)) {
                        String[] split = action.split(":");
                        main.getFileTransfer().removePeer(split[1], split[2]);
                        break;

                    } else if(action.startsWith(LeadMeMain.FILE_TRANSFER)) {
                        String[] split = action.split(":");
                        main.askForPeerPermission(LeadMeMain.FILE_TRANSFER, Boolean.parseBoolean(split[1]));
                        break;

                    } else if(action.startsWith(LeadMeMain.UPDATE_DEVICE_MESSAGE)) {
                        String[] split = action.split(":");
                        main.setDeviceStatusMessage(Integer.parseInt(split[1]));
                        break;

                    } else if(action.startsWith(LeadMeMain.AUTO_INSTALL)) {
                        String[] split = action.split(":");
                        main.askForPeerPermission(LeadMeMain.AUTO_INSTALL, Boolean.parseBoolean(split[1]));
                        break;

                    } else if(action.startsWith(LeadMeMain.MULTI_INSTALL)) {
                        String[] split = action.split(":"); //get the instructions
                        String applications = split[2]; //get the array of apps currently in string form
                        //change the string array into an array
                        String[] appArray = applications.replace("[","").replace("]","").split(",");
                        String[] firstApp = appArray[0].split("//");
                        main.getLumiAppInstaller().autoInstall(firstApp[0], firstApp[1], split[1], appArray);
                        break;

                    } else if(action.startsWith(LeadMeMain.APP_NOT_INSTALLED)) {
                        String[] split = action.split(":");
                        //get the student id add it to the need to install array.
                        Log.d(TAG, "Application needed on peer: " + split[3]);
                        main.getLumiAppInstaller().peersToInstall.add(split[3]);

                        //open a dialog to confirm if wanting to install apps
                        main.getLumiAppInstaller().applicationsToInstallWarning(split[1], split[2], false); //should auto update number of devices need as the action come in
                        break;

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_FAILED)) {
                        String[] split = action.split(":");
                        main.getConnectedLearnersAdapter().appLaunchFail(split[2], split[1]);

                        //in this case, student will be back in LeadMe, so update icon too
                        main.getConnectedLearnersAdapter().updateIcon(split[2], main.getAppManager().getAppIcon(main.leadMePackageName));
                        break;

                    } else if (action.startsWith(LeadMeMain.AUTO_INSTALL_ATTEMPT)) {
                        String[] split = action.split(":");
                        main.updatePeerStatus(split[2], ConnectedPeer.STATUS_INSTALLING, null);
                        break;

                    } else if(action.startsWith(LeadMeMain.COLLECT_APPS)) { //collecting apps from a learner device
                        //collect all applications(package name & app name) installed on learner device
                        List<String> applicationInfo = new ArrayList<String>(main.getAppManager().refreshAppList());

                        //send back to the leader - placeholder for now
                        sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.APP_COLLECTION + ":" + applicationInfo,
                                main.getNearbyManager().getSelectedPeerIDs());
                        break;

                    } else if(action.startsWith(LeadMeMain.APP_COLLECTION)) { //leader receiving an app collection from a learner
                        String[] split = action.split(":"); //get the app array
                        String applications = split[1]; //get the array of apps currently in string form
                        //change the string array into an array
                        String[] appArray = applications.replace("[", "").replace("]", "").split(",");

                        Collections.addAll(main.getLumiAppInstaller().peerApplications, appArray);
                        main.getLumiAppInstaller().populateUninstall();

                        break;

                    } else if(action.startsWith(LeadMeMain.AUTO_UNINSTALL)) {
                        String[] split = action.split(":"); //get the instructions
                        String applications = split[2]; //get the array of apps currently in string form
                        //change the string array into an array
                        String[] appArray = applications.replace("[","").replace("]","").split(",");
                        Collections.addAll(main.getLumiAppInstaller().appsToManage, appArray);

                        main.getLumiAppInstaller().runUninstaller();

                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_SUCCESS)) {
                        //Log.i(TAG, "SUCCEEDED - " + action);
                        String[] split = action.split(":");

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
                                break;

                        }
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_URL)) {
                        String[] split = action.split(":::", 3);
                        main.getWebManager().launchWebsite(split[1], split[2], true);
                        break;

                    } else if (action.startsWith(LeadMeMain.LAUNCH_YT)) {
                        String[] split = action.split(":::", 4);
                        main.getWebManager().launchYouTube(split[1], split[2], split[3].equals("true"), true);
                        Log.w(TAG, action + "||" + split[1] + ", " + split[2] + ", " + split[3] + "|");
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
}
