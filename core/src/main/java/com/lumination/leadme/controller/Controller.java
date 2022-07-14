package com.lumination.leadme.controller;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.BoardiesITSolutions.FileDirectoryPicker.DirectoryPicker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.accessibility.VRAccessibilityManager;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.adapters.LeaderSelectAdapter;
import com.lumination.leadme.managers.AppManager;
import com.lumination.leadme.managers.AuthenticationManager;
import com.lumination.leadme.managers.DialogManager;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.managers.FileTransferManager;
import com.lumination.leadme.managers.FirebaseManager;
import com.lumination.leadme.managers.NearbyPeersManager;
import com.lumination.leadme.managers.NetworkManager;
import com.lumination.leadme.managers.PermissionManager;
import com.lumination.leadme.managers.ScreenSharingManager;
import com.lumination.leadme.managers.WebManager;
import com.lumination.leadme.managers.XrayManager;
import com.lumination.leadme.players.VREmbedPhotoPlayer;
import com.lumination.leadme.players.VREmbedVideoPlayer;
import com.lumination.leadme.utilities.AppInstaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Class responsible for creating and accessing managers as well as static tags for
 * communications.
 */
public class Controller {
    private static final String TAG = "Controller";

    //Singleton pattern
    private static Controller controllerInstance;
    public static Controller getInstance()
    {
        return controllerInstance;
    }

    //tags to indicate what incoming messages holds
    public static final String LOGOUT_TAG = "LumiLogout";
    public static final String ACTION_TAG = "LumiAction";
    public static final String APP_TAG = "LumiAppLaunch";
    public static final String EXIT_TAG = "LumiExit";
    public static final String RETURN_TAG = "LumiReturnToApp";
    public static final String YOUR_ID_IS = "LumiYourID:";

    public static final String LOCK_TAG = "LumiLock";
    public static final String UNLOCK_TAG = "LumiUnlock";
    public static final String BLACKOUT_TAG = "LumiBlackout";

    public static final String DISCONNECTION = "LumiDisconnect";
    public static final String TRANSFER_ERROR = "LumiTransferError";
    public static final String VR_PLAYER_TAG = "LumiVRPlayer";
    public static final String FILE_REQUEST_TAG = "LumiFileRequest";
    public static final String STUDENT_FINISH_ADS = "LumiAdsFinished";
    public static final String VID_MUTE_TAG = "LumiVidMute";
    public static final String VID_UNMUTE_TAG = "LumiVidUnmute";
    public static final String VID_ACTION_TAG = "LumiVid:";

    public static final String LAUNCH_URL = "LumiLaunch:::";
    public static final String LAUNCH_YT = "LumiYT:::";
    public static final String LAUNCH_ACCESS = "LumiLaunchAccess";

    public static final String PERMISSION_DENIED = "LumiPermissionDenied";
    public static final String FILE_TRANSFER = "LumiFileTransfer";
    public static final String UPDATE_DEVICE_MESSAGE = "LumiUpdateDeviceMessage";

    public static final String MULTI_INSTALL = "LumiMultiInstall";
    public static final String AUTO_INSTALL = "LumiAutoInstalling";
    public static final String AUTO_INSTALL_FAILED = "LumiAutoInstallFail:";
    public static final String AUTO_INSTALL_ATTEMPT = "LumiAutoInstallAttempt:";
    public static final String APP_NOT_INSTALLED = "LumiAppNotInstalled";
    public static final String COLLECT_APPS = "LumiCollectApps";
    public static final String APP_COLLECTION = "LumiPeerAppCollection";
    public static final String AUTO_UNINSTALL = "LumiAutoUninstall";

    public static final String STUDENT_OFF_TASK_ALERT = "LumiOffTask:";
    public static final String STUDENT_NO_OVERLAY = "LumiOverlay:";
    public static final String STUDENT_NO_ACCESSIBILITY = "LumiAccess:";
    public static final String STUDENT_NO_INTERNET = "LumiInternet:";
    public static final String STUDENT_NO_XRAY = "LumiXray:";
    public static final String PERMISSION_TRANSFER_DENIED = "LumiTransfer:";
    public static final String PERMISSION_AUTOINSTALL_DENIED = "LumiAutoInstaller:";
    public static final String LAUNCH_SUCCESS = "LumiSuccess:";

    public static final String SESSION_UUID_TAG = "SessionUUID";
    public static final String SESSION_VR_TAG = "SessionFirstVR";

    public static final String XRAY_REQUEST = "LumiXrayRequest";
    public static final String XRAY_ON = "LumiXrayOn";
    public static final String XRAY_OFF = "LumiXrayOff";

    public static final String NAME_CHANGE = "NameChange:";
    public static final String NAME_REQUEST = "NameRequest:";

    public static final String PING_TAG = "LumiPing";
    public static final String PING_ACTION = "StillAlive";

    //Managers and players
    private final VREmbedPhotoPlayer vrEmbedPhotoPlayer;
    private final VREmbedVideoPlayer vrEmbedVideoPlayer;
    private final VRAccessibilityManager vrAccessibilityManager;

    private final FirebaseManager firebaseManager;
    private final NetworkManager networkManager;
    private AppUpdateManager appUpdateManager;
    private final FileTransferManager fileTransferManager;
    private final PermissionManager permissionManager;
    private final AuthenticationManager authenticationManager;
    private final NearbyPeersManager nearbyManager;
    private final WebManager webManager;
    private final DialogManager dialogManager;
    private final AppManager appLaunchAdapter;
    private final ConnectedLearnersAdapter connectedLearnersAdapter;
    private final LeaderSelectAdapter leaderSelectAdapter;
    private final DispatchManager dispatcher;
    private final XrayManager xrayManager;
    private final AppInstaller lumiAppInstaller;
    private final ScreenSharingManager screenSharingManager;

    /**
     *
     */
    public Controller() {
        controllerInstance = this;
        LeadMeMain main = LeadMeMain.getInstance();

        screenSharingManager = new ScreenSharingManager(main);
        firebaseManager = new FirebaseManager(main);
        networkManager = new NetworkManager();
        permissionManager = new PermissionManager(main);
        authenticationManager = new AuthenticationManager(main);
        dialogManager = new DialogManager(main);
        nearbyManager = new NearbyPeersManager();
        dispatcher = new DispatchManager(main);
        webManager = new WebManager(main);
        leaderSelectAdapter = new LeaderSelectAdapter(main);
        vrAccessibilityManager = new VRAccessibilityManager(main);
        vrEmbedPhotoPlayer = new VREmbedPhotoPlayer(main);
        vrEmbedVideoPlayer = new VREmbedVideoPlayer(main);
        appLaunchAdapter = new AppManager(main);
        xrayManager = new XrayManager(main, LeadMeMain.getInstance().xrayScreen);
        fileTransferManager = new FileTransferManager(main);
        lumiAppInstaller = new AppInstaller(main);
        connectedLearnersAdapter = new ConnectedLearnersAdapter(main, new ArrayList<>(), dialogManager.alertsAdapter);
    }

    //Activity Results Functions Area

    /**
     * Determine if the overlay permission has been turned on when returning to LeadMe.
     * @param resultCode An integer representing if the result was successful or not.
     *
     */
    public void overlyOn(int resultCode) {
        Log.d(TAG, "Returning from OVERLAY ON with " + resultCode);

        if (getPermissionsManager().isOverlayPermissionGranted()) {
            if(getFirebaseManager().getServerIP().length()>0){
                LeadMeMain.getInstance().setandDisplayStudentOnBoard(3);
            } else {
                LeadMeMain.getInstance().setandDisplayStudentOnBoard(2);
            }
        } else {
            LeadMeMain.getInstance().setandDisplayStudentOnBoard(1);
        }
    }

    /**
     * Check if the accessibility settings have been turned on.
     * @param resultCode An integer representing if the result was successful or not.
     */
    public void accessibilityOn(int resultCode) {
        Log.d(TAG, "Returning from ACCESS ON with " + resultCode + " (" + LeadMeMain.isGuide + ")");
        PermissionManager.waitingForPermission = false;
        if (getPermissionsManager().isAccessibilityGranted()) {
            LeadMeMain.getInstance().setandDisplayStudentOnBoard(1);
        } else {
            LeadMeMain.getInstance().setandDisplayStudentOnBoard(0);
        }
        //permissionManager.requestBatteryOptimisation();
    }

    /**
     * Handle the screen capture starting data and result.
     * @param resultCode An integer representing if the result was successful or not.
     * @param data An intent representing the screen capture details.
     */
    public void screenCapture(int resultCode, Intent data) {
        screenSharingManager.handleResultReturn(resultCode, data);
    }

    /**
     * Sign in to LeadMe using the google sign in method.
     * @param data An intent representing the google sign in intent details.
     */
    public void googleSignIn(Intent data) {
        // The Task returned from this call is always completed, no need to attach
        // a listener.
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            getAuthenticationManager().handleSignInResult(account);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the file choice for the custom VR player.
     * @param resultCode An integer representing if the result was successful or not.
     * @param data An intent representing the chosen file details.
     */
    public void vrFileChoice(int resultCode, Intent data) {
        Log.d(TAG, "DATA: " + data);
        if (resultCode == Activity.RESULT_OK ) {
            if(data != null)  {
                if(isMiUiV9()) {
                    LeadMeMain.vrPath = data.getStringExtra(DirectoryPicker.BUNDLE_SELECTED_FILE);

                    if(LeadMeMain.defaultVideo) {
                        getVrEmbedVideoPlayer().setFilepath(LeadMeMain.vrPath);
                    } else {
                        getVrEmbedPhotoPlayer().setFilepath(LeadMeMain.vrPath);
                    }
                } else {
                    LeadMeMain.vrURI = data.getData();

                    if(LeadMeMain.defaultVideo) {
                        getVrEmbedVideoPlayer().setFilepath(LeadMeMain.vrURI);
                    } else {
                        getVrEmbedPhotoPlayer().setFilepath(LeadMeMain.vrURI);
                    }
                }
            }
        }
    }

    /**
     * Set the file choice for the file transfer protocol.
     * @param resultCode An integer representing if the result was successful or not.
     * @param data An intent representing the chosen file details.
     */
    public void transferFileChoice(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(data != null)  {
                FileTransferManager.setFileType("File");

                if(isMiUiV9()) {
                    String selectedFile = data.getStringExtra(DirectoryPicker.BUNDLE_SELECTED_FILE);
                    Log.e(TAG, "Selected file: " + selectedFile);
                    getFileTransferManager().startFileServer(selectedFile, false);
                } else {
                    getFileTransferManager().startFileServer(data.getData(), false);
                }
            }
        }
    }

    /**
     * Detect what version of MIUI is running on a Xiaomi device. As version below 10 carry an inherent
     * OS processing issue relating to launching file picker intents.
     * @return A boolean representing if the MIUI version is 9.5 or below.
     */
    public static boolean isMiUiV9() {
        String version = getSystemProperty();

        //If empty then it is a different type of phone
        if(TextUtils.isEmpty(version)) {
            return false;
        }

        //Version number comes through as integers or doubles depending on the version
        //ranging from 8, 9.5, 11.5, 12 etc..
        //All Xiaomi devices above SE8, i.e Note 8/9 can update to MIUI 12. So only MIUI versions
        //at or below 10 need to use the alternate file picker.
        String[] num = new String[0];
        if (version != null) {
            num = version.split("V");
        }
        int versionNum = Integer.parseInt(num[1]);

        return versionNum <= 10;
    }

    private static String getSystemProperty() {
        String propName = "ro.miui.ui.version.name";
        String line;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d(TAG, "MIUI version: " + line);
        return line;
    }

    //Accessors for Managers
    public DispatchManager getDispatcher() {
        return dispatcher;
    }
    public ConnectedLearnersAdapter getConnectedLearnersAdapter() {
        return connectedLearnersAdapter;
    }
    public ScreenSharingManager getScreenSharingManager() { return screenSharingManager; }
    public VRAccessibilityManager getVRAccessibilityManager() {
        return vrAccessibilityManager;
    }
    public FileTransferManager getFileTransferManager() { return fileTransferManager; }
    public VREmbedVideoPlayer getVrEmbedVideoPlayer() { return vrEmbedVideoPlayer; }
    public VREmbedPhotoPlayer getVrEmbedPhotoPlayer() { return vrEmbedPhotoPlayer; }
    public PermissionManager getPermissionsManager() {
        return permissionManager;
    }
    public FirebaseManager getFirebaseManager() { return firebaseManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }
    public NearbyPeersManager getNearbyManager() {
        return nearbyManager;
    }
    public LeaderSelectAdapter getLeaderSelectAdapter() {
        return leaderSelectAdapter;
    }
    public AppManager getAppManager() {
        return appLaunchAdapter;
    }
    public WebManager getWebManager() {
        return webManager;
    }
    public DialogManager getDialogManager() {
        return dialogManager;
    }
    public XrayManager getXrayManager() { return xrayManager; }
    public AppInstaller getLumiAppInstaller() {
        return lumiAppInstaller;
    }
}
