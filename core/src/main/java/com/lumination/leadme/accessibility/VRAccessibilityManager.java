package com.lumination.leadme.accessibility;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.managers.FileTransferManager;
import com.lumination.leadme.managers.NearbyPeersManager;
import com.lumination.leadme.utilities.FileUtilities;
import com.lumination.leadme.LeadMeMain;

import java.io.File;

public class VRAccessibilityManager {
    private final static String TAG = "VRAccessibilityManager";
    private final static String packageName = "com.lumination.VRPlayer";
    private final static String className = "com.lumination.receiver.ReceiverPlugin";

    public static final int CUE_PLAY = 0;
    public static final int CUE_PAUSE = 1;
    public static final int CUE_STOP = 2;
    public static final int CUE_FWD = 3;
    public static final int CUE_RWD = 4;
    public static final int CUE_PROJECTION = 5;
    public static final int CUE_SET_SOURCE = 6;

    private final LeadMeMain main;

    /**
     * Basic constructor for the VRAccessibility Manager, no additional setup required within.
     * @param main A reference to the LeadMe main activity.
     */
    public VRAccessibilityManager(LeadMeMain main) {
        this.main = main;
    }

    /**
     * Send operation intents to the VR player to control the playback.
     * NOTE: Mute and unmute use the dispatch manager to control the volume of the phone rather than
     * the volume of the application.
     * @param cue An integer that defines what action should be taken.
     * @param info A string which supplies additional information - such as the start time and/or source path
     */
    public void vrPlayerAction(int cue, String info) {
        switch(cue) {
            case CUE_PLAY:
                Log.e(TAG, "play");
                newIntent("play");
                break;

            case CUE_PAUSE:
                Log.e(TAG, "pause");
                newIntent("pause");
                break;

            case CUE_STOP:
                Log.e(TAG, "stop");
                newIntent("stop");
                break;

            case CUE_FWD:
                Log.e(TAG, "fwd");
                newIntent("fwd");
                break;

            case CUE_RWD:
                Log.e(TAG, "rwd");
                newIntent("rwd");
                break;

            case CUE_PROJECTION:
                changeProjection(info);
                break;

            case CUE_SET_SOURCE:
                setSource(info);
                break;

            default:
                Log.e(TAG, "Action unknown");
                break;
        }
    }

    private void changeProjection(String info) {
        Log.e(TAG, info);

        //Example
        String proj = "projection:" + info;

        //Send a source intent with the projection action and the type
        newIntent(proj);
    }

    /*TODO application needs to be opened once so that the user can allow file permissions otherwise
    set source will throw a storage permission denied exception.
    * */
    //Send the source filepath and start time to the external application
    private void setSource(String info) {
        Log.d(TAG, "File name: " + info);

        if(info == null) {
            return;
        }

        //Split the filename and the start time apart.
        String[] split = info.split(":");
        String fileName = split[0];
        String fileType = split[2];

        String absFilepath;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri source = FileUtilities.searchStorage(main, split[0]);

            if(source == null) {
                requestFile(split[2]);
                return;
            } else {
                //File path needed for Unity instead of Uri.
                absFilepath = FileUtilities.getPath(main, source);
            }
        } else {
            File temp = FileUtilities.findFile(Environment.getExternalStorageDirectory(), fileName);

            if(temp == null) {
                requestFile(split[2]);
                return;
            } else {
                absFilepath = temp.getPath();
            }
        }

        Log.d(TAG, "Sending source: " + absFilepath + ":" + split[1] + ":" + fileType);

        //Send a source intent with the file path and the start time
        newIntent("File path:" + absFilepath + ":" + split[1] + ":" + fileType);
    }

    //Send an action to the guide requesting that a file be transferred to this device
    private void requestFile(String fileType) {
        LeadMeMain.fileTransferEnabled = true; //hard coded for now - change to permission later

        if(LeadMeMain.fileTransferEnabled) {
            FileTransferManager.setFileType(fileType);
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.FILE_REQUEST_TAG + ":" + NearbyPeersManager.getID()
                    + ":" + "false" + ":" + FileTransferManager.getFileType(), NearbyPeersManager.getSelectedPeerIDs());
            LeadMeMain.runOnUI(() -> Controller.getInstance().getDialogManager().showWarningDialog("Missing File", "Video is transferring now."));
        } else {
            LeadMeMain.runOnUI(() ->Controller.getInstance().getDialogManager().showWarningDialog("Permission Needed", "File Transfer is not enabled " +
                    "\nThe file cannot be sent."));
        }
    }

    //Create an intent based on the action supplied and send it to the external application
    private void newIntent(String action) {
        // sendIntent is the object that will be broadcast outside our app
        Intent sendIntent = new Intent();

        // We add flags for example to work from background
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_FROM_BACKGROUND|Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        // SetAction uses a string which is an important name as it identifies the sender of the intent and that we will give to the receiver to know what to listen.
        // By convention, it's suggested to use the current package name
        sendIntent.setAction("com.lumination.leadme.IntentToUnity");

        // Set an explicit component and class to send the intent to, cannot use implicit anymore.
        sendIntent.setComponent(new ComponentName(packageName, className));

        // Here we fill the Intent with our data, here just a string with an incremented number in it.
        sendIntent.putExtra(Intent.EXTRA_TEXT, action);

        // And here it goes ! our message is sent to any other app that want to listen to it.
        main.sendBroadcast(sendIntent);
    }
}
