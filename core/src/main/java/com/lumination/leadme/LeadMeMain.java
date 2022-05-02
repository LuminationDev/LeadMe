package com.lumination.leadme;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.BoardiesITSolutions.FileDirectoryPicker.DirectoryPicker;
import com.BoardiesITSolutions.FileDirectoryPicker.OpenFilePicker;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;
import com.lumination.leadme.adapters.CuratedContentManager;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.accessibility.LumiAccessibilityConnector;
import com.lumination.leadme.accessibility.VRAccessibilityManager;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.adapters.LeaderSelectAdapter;
import com.lumination.leadme.managers.AppManager;
import com.lumination.leadme.managers.AuthenticationManager;
import com.lumination.leadme.managers.DialogManager;
import com.lumination.leadme.managers.FileTransferManager;
import com.lumination.leadme.managers.FirebaseManager;
import com.lumination.leadme.managers.NearbyPeersManager;
import com.lumination.leadme.managers.NetworkManager;
import com.lumination.leadme.managers.PermissionManager;
import com.lumination.leadme.managers.ScreenSharingManager;
import com.lumination.leadme.managers.WebManager;
import com.lumination.leadme.managers.XrayManager;
import com.lumination.leadme.players.VREmbedPlayer;
import com.lumination.leadme.services.LumiAccessibilityService;
import com.lumination.leadme.services.NetworkService;
import com.lumination.leadme.utilities.AppInstaller;
import com.lumination.leadme.utilities.FileUtilities;
import com.lumination.leadme.utilities.OnboardingGestureDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.bolt.screenshotty.ScreenshotManagerBuilder;

/*
    LeadMe Main:
    • Handles most UI related events
    • Initialises main classes
 */
public class LeadMeMain extends FragmentActivity implements Handler.Callback, SensorEventListener, LifecycleObserver, ComponentCallbacks2 {
    static final String TAG = "LeadMe"; //tag for debugging
    final private String teacherCode = "1234";

    //Turn the updated content on or off - still need to manually switch the layout in c__leader_main.xml
    public static final boolean FLAG_UPDATES = true;
    public static final boolean FLAG_INSTALLER = false;

    public Drawable leadmeIcon;
    protected PowerManager powerManager;

    public static String leadMeAppName = "";
    public static String leadMePackageName = "";

    //tag to indicate what incoming message holds
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
    public static final String SESSION_MANUAL_TAG = "SessionManual";
    public static final String SESSION_VR_TAG = "SessionFirstVR";

    public static final String XRAY_REQUEST = "LumiXrayRequest";
    public static final String XRAY_ON = "LumiXrayOn";
    public static final String XRAY_OFF = "LumiXrayOff";

    public static final String NAME_CHANGE = "NameChange:";
    public static final String NAME_REQUEST = "NameRequest:";

    public final int OVERLAY_ON = 0;
    public final int ACCESSIBILITY_ON = 1;
    public final int BLUETOOTH_ON = 2;
    public final int FINE_LOC_ON = 3;
    public final int RC_SIGN_IN = 4;
    public static final int VR_FILE_CHOICE = 5;
    public static final int TRANSFER_FILE_CHOICE = 6;

    //for testing if a connection is still live
    public static final String PING_TAG = "LumiPing";
    public static final String PING_ACTION = "StillAlive";

    // The SensorManager gives us access to sensors on the device.
    public SensorManager mSensorManager;
    // The accelerometer sensor allows us to detect device movement for shake-to-advertise.
    private Sensor mAccelerometer;

    // Acceleration required to detect a shake. In multiples of Earth's gravity.
    private static final float SHAKE_THRESHOLD_GRAVITY = 2;

    private SharedPreferences sharedPreferences;
    /**
     * sessionUUID to enable disconnect/reconnect as same user
     */
    private String sessionUUID = null;
    /**
     * Used to determine if server discovery is on
     */
    public Boolean sessionManual = null;
    /**
     * Used to determine if the user has used the VR player before
     */
    public Boolean vrFirstTime = null;
    /**
     * Used to determine if connecting through direct IP input
     */
    public Boolean directConnection = false;

    private InputMethodManager imm;
    private LumiAccessibilityConnector lumiAccessibilityConnector;
    private BroadcastReceiver accessibilityReceiver;

    protected ViewGroup.LayoutParams layoutParams;

    public static WindowManager windowManager;
    public static WindowManager.LayoutParams overlayParams, url_overlayParams;
    public View overlayView, url_overlay;

    //VR PLayer
    private VREmbedPlayer vrEmbedPlayer;
    private VRAccessibilityManager vrAccessibilityManager;
    /**
     * A Uri representing the last source pushed by the leader to a learner,
     * saved in case a learners requests a file transfer.
     */
    public Uri vrVideoURI;

    /**
     * A String representing the last source pushed by the leader to a learner,
     * saved in case a learners requests a file transfer. Used for devices with MIUI version 9.5
     * and below.
     */
    public String vrVideoPath;

    //details about me to send to peers
    public static boolean isGuide = false;
    public boolean isReadyToConnect = false;
    public boolean studentLockOn = true; //students start locked
    private boolean selectedOnly = false; //sending to all learners or just selected

    public String lastLockState = LOCK_TAG;
    public String lastAppID;

    private final Handler handler = new Handler(this);
    public ViewAnimator leadmeAnimator;
    private ViewSwitcher leaderLearnerSwitcher;
    protected boolean loggingInAsLeader = true;
    public String leaderName = "Leader";

    /**
     * Determine if a using is trying to login as a leader or a learner.
     * Toggled on the splash page by the leader and learner buttons.
     */
    public String loginActor = "leader";

    private final int SWITCH_LEADER_INDEX = 0;
    private final int SWITCH_LEARNER_INDEX = 1;

    private final int ANIM_SPLASH_INDEX = 0;
    private final int ANIM_START_SWITCH_INDEX = 1;
    private final int ANIM_LEARNER_INDEX = 2;
    private final int ANIM_LEADER_INDEX = 3;
    private final int ANIM_APP_LAUNCH_INDEX = 4;
    private final int ANIM_OPTIONS_INDEX = 5;
    private final int ANIM_XRAY_INDEX = 6;
    protected final int ANIM_MULTI_INDEX = 7;
    public final int ANIM_CURATED_CONTENT_LAUNCH_INDEX = 8;
    private final int ANIM_CURATED_CONTENT_SINGLE_LAUNCH_INDEX = 9;

    public View waitingForLearners, appLauncherScreen;
    public View splashscreen, startLearner, mainLearner, startLeader, mainLeader, optionsScreen, switcherView, xrayScreen;
    public View multiAppManager;
    private TextView learnerWaitingText;
    public Button alertsBtn;
    private Button leader_toggle, learner_toggle;
    private ImageView logo;
    private GridView connectedStudentsView;

    //Checking for updates on the Play Store
    private final int UPDATE_REQUEST_CODE = 100;

    public Context context;
    public ActivityManager activityManager;
    private FirebaseManager firebaseManager;
    private NetworkManager networkManager;
    private AppUpdateManager appUpdateManager;
    private FileTransferManager fileTransferManager;
    private PermissionManager permissionManager;
    private AuthenticationManager authenticationManager;
    private NearbyPeersManager nearbyManager;
    private WebManager webManager;
    private DialogManager dialogManager;
    private AppManager appLaunchAdapter;
    private ConnectedLearnersAdapter connectedLearnersAdapter;
    private LeaderSelectAdapter leaderSelectAdapter;
    private static DispatchManager dispatcher;
    public static WifiManager wifiManager;
    private XrayManager xrayManager;
    private AppInstaller lumiAppInstaller;

    private Switch serverDiscoveryToggle = null;

    //File transfer
    public Boolean fileTransferEnabled = false; //hard coded so have to enable each session
    public Switch transferToggle = null;
    public static ArrayList<Integer> fileRequests = new ArrayList<>(); //array to hold learner ID's that are requesting a file

    //Auto app installer
    public Boolean autoInstallApps = false; //if true, missing apps on student devices get installed automatically
    public Boolean managingAutoInstaller = false; //track if installing applications so recall can be skipped
    public Boolean installingApps = null; //track if installing or uninstalling application
    public Switch autoToggle = null;

    ImageView currentTaskIcon;
    TextView currentTaskTitle, currentTaskDescription;
    Button currentTaskLaunchBtn;
    public static String currentTaskPackageName, currentTaskURLTitle, currentTaskName, currentTaskURL, currentTaskType;

    public static Intent appIntentOnFocus = null;
    Toast appToast = null;

    private boolean init = false;

    boolean allowHide = false;
    ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    public ExecutorService backgroundExecutor = Executors.newCachedThreadPool();
    /**
     * Used exclusively for handling messages from a server on learner devices
     */
    public ThreadPoolExecutor serverThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    public ScreenSharingManager screenSharingManager;

    public Handler getHandler() {
        return handler;
    }

    public void stopShakeDetection() {
        mSensorManager.unregisterListener(this);
    }

    public void startShakeDetection() {
        Log.d(TAG, "Listening for shakes!");
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public static boolean canAskForAccessibility = true;

    public final int SCREEN_CAPTURE = 999;
    private static final int REQUEST_SCREENSHOT_PERMISSION = 1234;

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OVERLAY_ON:
                overlyOn(resultCode);
                break;

            case ACCESSIBILITY_ON:
                accessibilityOn(resultCode);
                break;

            case BLUETOOTH_ON:
                Log.d(TAG, "Returning from BLUETOOTH ON with " + resultCode);
                break;

            case FINE_LOC_ON:
                Log.d(TAG, "Returning from FINE LOC ON with " + resultCode);
                break;

            case 99:
                Log.d(TAG, "RETURNED RESULT FROM YOUTUBE! " + resultCode + ", " + data);
                break;

            case SCREEN_CAPTURE:
                screenCapture(resultCode, data);
                break;

            case RC_SIGN_IN:
                googleSignIn(data);
                break;

            case VR_FILE_CHOICE:
                vrFileChoice(resultCode, data);
                break;

            case TRANSFER_FILE_CHOICE:
                transferFileChoice(resultCode, data);
                break;

            default:
                Log.d(TAG, "RETURNED FROM ?? with " + resultCode);
                break;
        }
        xrayManager.screenshotManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Determine if the overlay permission has been turned on when returning to LeadMe.
     * @param resultCode An integer representing if the result was successful or not.
     *
     */
    private void overlyOn(int resultCode) {
        Log.d(TAG, "Returning from OVERLAY ON with " + resultCode);

        if (getPermissionsManager().isOverlayPermissionGranted()) {
            if(getFirebaseManager().getServerIP().length()>0){
                setandDisplayStudentOnBoard(3);
            } else {
                setandDisplayStudentOnBoard(2);
            }
        } else {
            setandDisplayStudentOnBoard(1);
        }
    }

    /**
     * Check if the accessibility settings have been turned on.
     * @param resultCode An integer representing if the result was successful or not.
     */
    private void accessibilityOn(int resultCode) {
        Log.d(TAG, "Returning from ACCESS ON with " + resultCode + " (" + isGuide + ")");
        permissionManager.waitingForPermission = false;
        if (getPermissionsManager().isAccessibilityGranted()) {
            setandDisplayStudentOnBoard(1);
        } else {
            setandDisplayStudentOnBoard(0);
        }
        //permissionManager.requestBatteryOptimisation();
    }

    /**
     * Handle the screen capture starting data and result.
     * @param resultCode An integer representing if the result was successful or not.
     * @param data An intent representing the screen capture details.
     */
    private void screenCapture(int resultCode, Intent data) {
        screenSharingManager.handleResultReturn(resultCode, data);
    }

    /**
     * Sign in to LeadMe using the google sign in method.
     * @param data An intent representing the google sign in intent details.
     */
    private void googleSignIn(Intent data) {
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
    private void vrFileChoice(int resultCode, Intent data) {
        Log.d(TAG, "DATA: " + data);
        if (resultCode == Activity.RESULT_OK ) {
            if(data != null)  {
                if(isMiUiV9()) {
                    vrVideoPath = data.getStringExtra(DirectoryPicker.BUNDLE_SELECTED_FILE);
                    getVrEmbedPlayer().setFilepath(vrVideoPath);
                } else {
                    vrVideoURI = data.getData();
                    getVrEmbedPlayer().setFilepath(vrVideoURI);
                }
            }
        }
    }

    /**
     * Set the file choice for the file transfer protocol.
     * @param resultCode An integer representing if the result was successful or not.
     * @param data An intent representing the chosen file details.
     */
    private void transferFileChoice(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if(data != null)  {
                FileTransferManager.setFileType("File");

                if(isMiUiV9()) {
                    String selectedFile = data.getStringExtra(DirectoryPicker.BUNDLE_SELECTED_FILE);
                    Log.e(TAG, "Selected file: " + selectedFile);
                    fileTransferManager.startFileServer(selectedFile, false);
                } else {
                    fileTransferManager.startFileServer(data.getData(), false);
                }
            }
        }
    }

    public boolean handleMessage(Message msg) {
        Log.d(TAG, "Got a message! " + msg.what + ", " + msg.obj);
        return true;
    }

    public void handlePayload(byte[] payloadBytes) {
        if (payloadBytes == null) {
            Log.e(TAG, "Payload is EMPTY!");
            return;
        }

        //if it's an action, execute it
        if (getDispatcher().readAction(payloadBytes)) {
            Log.d(TAG, "Incoming message was an action!");
            return; //it was an action, we're done!
        }

        //if it's an app launch request, deploy it
        if (getDispatcher().openApp(payloadBytes)) {
            Log.d(TAG, "Incoming message was an app launch request!");
            return; //it was an app launch request, we're done!
        }

        Log.e(TAG, "Couldn't find a match for " + Arrays.toString(payloadBytes));
    }


    public DispatchManager getDispatcher() {
        return dispatcher;
    }

    public LumiAccessibilityConnector getLumiAccessibilityConnector() {
        return lumiAccessibilityConnector;
    }

    public ConnectedLearnersAdapter getConnectedLearnersAdapter() {
        return connectedLearnersAdapter;
    }

    //VR Player
    public VRAccessibilityManager getVRAccessibilityManager() {
        return vrAccessibilityManager;
    }

    public FileTransferManager getFileTransferManager() { return fileTransferManager; }

    public VREmbedPlayer getVrEmbedPlayer() { return vrEmbedPlayer; }

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

    private boolean initPermissions = false;

    public void performNextAction() {
        Log.d(TAG, "PerformNextAction? " + initPermissions + ", " + permissionManager.isNearbyPermissionsGranted());

        permissionManager.waitingForPermission = false; //no longer waiting
        closeKeyboard();

        if (!initPermissions) {
            return; //not ready to proceed
        }

        //deal with location permission related stuff
        if (!permissionManager.isNearbyPermissionsGranted()) {
            Log.d(TAG, "Permission return - request nearby");
            permissionManager.checkNearbyPermissions();
            return;

        } else if (!nearbyManager.isDiscovering()) {
            Log.d(TAG, "Permission return - search for leaders");
            initiateLeaderDiscovery();
        }

        if (!permissionManager.isStoragePermissionsGranted()) {
            Log.d(TAG, "Permission return - request storage");
            permissionManager.checkStoragePermission();
            return;
        }

        //can't go any further
        if (!canAskForAccessibility && !permissionManager.isAccessibilityGranted()) {
            dialogManager.showWarningDialog("Cannot connect to other LeadMe users until Accessibility permission is granted.");
            return;
        }

        if (canAskForAccessibility && permissionManager.isAccessibilityGranted() && !permissionManager.isMyServiceRunning(AccessibilityService.class)) {
            Log.d(TAG, "Permission return - accessibility permission granted, but service not running");
            Intent accessibilityIntent = new Intent(getApplicationContext(), LumiAccessibilityService.class);
            startService(accessibilityIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if (leadmeAnimator.getDisplayedChild() == ANIM_CURATED_CONTENT_SINGLE_LAUNCH_INDEX) {
            leadmeAnimator.setDisplayedChild(ANIM_CURATED_CONTENT_LAUNCH_INDEX);
        } else {
            super.onBackPressed();
            getNearbyManager().onBackPressed();
        }
        Log.i(TAG, "On BACK!");
    }

    /**
     * The device has moved. We need to decide if it was intentional or not.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //already connected, or not ready to connect
        if (getNearbyManager().isConnectedAsGuide() || getNearbyManager().isConnectedAsFollower()) {
            return;
        }
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            showLoginDialog();
        }
    }

    public void showLoginDialog() {
        Log.d(TAG, "Showing login dialog");
        if (destroying) {
            return;
        }
        stopShakeDetection();

        //Not sure if needed yet?
        //GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        FirebaseUser user = getAuthenticationManager().getCurrentAuthUser();

        if (user == null) {
            dialogManager.changeLoginViewOptions(View.VISIBLE, View.GONE, View.GONE);
        } else {
            dialogManager.changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
            getNearbyManager().myName = user.getDisplayName();
            getNameViewController().setText(user.getDisplayName());
        }

        //set appropriate mode
        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
            Log.d(TAG, "showLoginDialog: leader"); //leader
            dialogManager.changeLeaderLoginViewOptions(View.VISIBLE, View.GONE);

        } else {
            Log.d(TAG, "showLoginDialog: learner"); //learner
            dialogManager.changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
            dialogManager.changeLeaderLoginViewOptions(View.GONE, View.VISIBLE);

            if (getNearbyManager().selectedLeader == null) {
                leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEADER_INDEX);
                showLoginDialog();
                return;
            }

            dialogManager.setLeaderName(getNearbyManager().selectedLeader.getDisplayName());
        }

        //Check if the leader has internet access
        if(!PermissionManager.isInternetAvailable(getApplicationContext())) {
            dialogManager.showWarningDialog("Currently Offline", "No internet access detected. Please connect to continue.");
            return;
        }

        initPermissions = false; //reset this to ask once more
        dialogManager.dialogShowing = true;
        dialogManager.getLoginDialog().show();

        getNameViewController().requestFocus();
        openKeyboard();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not needed
    }

    private void moveAwayFromSplashScreen() {
        handler.postDelayed(() -> {
            logo.setImageResource(android.R.color.transparent);
            leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);

            init = true;
            startShakeDetection();
        }, 2000);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onLifecyclePause() {
        super.onPause();
        Log.w(TAG, "LC Pause");
        appHasFocus = false;
        Log.d(TAG, "onLifecyclePause: " + overlayInitialised + " " + permissionManager.isOverlayPermissionGranted());
        if (!overlayInitialised && permissionManager.isOverlayPermissionGranted()) {
            Log.d(TAG, "onLifecyclePause: 1");
            initialiseOverlayView();
        }

        refreshOverlay();

        if (overlayView == null) {
            return;
        }
        //update status and visibilities for overlay
        if (!getNearbyManager().isConnectedAsFollower()) {
            Log.d(TAG, "onLifecyclePause: this");
            overlayView.setVisibility(View.INVISIBLE);

        } else if (studentLockOn) {
            setStudentLock(ConnectedPeer.STATUS_LOCK);

            if (!getPermissionsManager().waitingForPermission) {
                overlayView.setVisibility(View.VISIBLE);
                url_overlay.setVisibility(View.INVISIBLE);
            } else {
                //need to allow students to accept permissions
                overlayView.setVisibility(View.INVISIBLE);
            }

        } else {
            setStudentLock(ConnectedPeer.STATUS_UNLOCK);
            overlayView.setVisibility(View.INVISIBLE);
        }
    }

    private static LumiAccessibilityService accessibilityService;

    // callback invoked either when the gesture has been completed or cancelled
    final AccessibilityService.GestureResultCallback gestureResultCallback = new AccessibilityService.GestureResultCallback() {
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            super.onCompleted(gestureDescription);

            //reset overlay
            if (overlayView.isAttachedToWindow()) {
                overlayParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                getWindowManager().updateViewLayout(overlayView, overlayParams);
            }
            handler.post(() -> {
                //wait until layout update is actioned before trying to gesture
                //while (currentTaskPackageName.equals(getAppManager().withinPackage) && overlayView.isLayoutRequested()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //}

                Log.w(TAG, "gesture completed");
                //activate the event once the tap completes
                getLumiAccessibilityConnector().gestureInProgress = false;
                getLumiAccessibilityConnector().manageAccessibilityEvent(null, null);
            });
        }

        @Override
        public void onCancelled(GestureDescription gestureDescription) {
            super.onCancelled(gestureDescription);

            //reset overlay
            if (overlayView.isAttachedToWindow()) {
                overlayParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                getWindowManager().updateViewLayout(overlayView, overlayParams);
            }

            handler.post(() -> {
                //wait until layout update is actioned before trying to gesture
                while (currentTaskPackageName.equals(getAppManager().withinPackage) && overlayView.isLayoutRequested()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.w(TAG, "gesture cancelled");
                getLumiAccessibilityConnector().gestureInProgress = false;
                //activate the event once the tap completes
                getLumiAccessibilityConnector().manageAccessibilityEvent(null, null);
            });
        }
    };

    public static void setAccessibilityService(LumiAccessibilityService service) {
        accessibilityService = service;
    }

    public static AccessibilityService getAccessibilityService() {
        return accessibilityService;
    }

    public void tapBounds(int x, int y) {
        getLumiAccessibilityConnector().gestureInProgress = true;
        Log.e(TAG, "ATTEMPTING TAP! " + x + ", " + y);
        if (accessibilityService == null) {
            return;
        }
        runOnUiThread(() -> {
            Path swipePath = new Path();
            swipePath.setLastPoint(x, y);
            swipePath.moveTo(x, y);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 200)); //50 was too short for Within
            GestureDescription swipe = gestureBuilder.build();

            handler.postAtFrontOfQueue(() -> {
                //change overlay so taps can temporarily pass through
                if (overlayView.isAttachedToWindow()) {
                    overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    getWindowManager().updateViewLayout(overlayView, overlayParams);

                    backgroundExecutor.submit(() -> {
                        //wait until layout update is actioned before trying to gesture --> needs to be NON-UI thread or blocks
                        do {
                            try {
                                Log.e(TAG, "Waiting... " + overlayView.isLayoutRequested());
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } while (currentTaskPackageName.equals(getAppManager().withinPackage) && overlayView.isLayoutRequested());

                        runOnUiThread(() -> { //must be UI thread
                            boolean success = accessibilityService.dispatchGesture(swipe, gestureResultCallback, getHandler());
                            Log.e(TAG, "Did I dispatch " + swipe + " to " + accessibilityService + "? " + success + " // " + overlayView.isAttachedToWindow() + " // " + overlayView.isLayoutRequested());
                        });
                    });
                }
            });
        });
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onLifecycleResume() {
        super.onResume();
        if (OnBoardStudentInProgress) {
            if (getPermissionsManager().isAccessibilityGranted() && !getPermissionsManager().isOverlayPermissionGranted()) {
                setandDisplayStudentOnBoard(1);
            } else if (getPermissionsManager().isAccessibilityGranted() && getPermissionsManager().isOverlayPermissionGranted()) {
                if(getFirebaseManager().getServerIP().length() > 0){
                    setandDisplayStudentOnBoard(3);
                }else {
                    setandDisplayStudentOnBoard(2);
                }
            }
        }

        if (OnBoard != null) {
            VideoView video = OnBoard.findViewById(R.id.animation_view);
            video.start();
        }

        Log.w(TAG, "LC Resume // " + getDispatcher().hasDelayedLaunchContent());
        appHasFocus = true;
        getLumiAccessibilityConnector().resetState(); //reset

        manageFocus();

        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEARNER_INDEX) {
            if (permissionManager.isNearbyPermissionsGranted() && !getNearbyManager().isConnectedAsFollower()) {
                displayLearnerStartToggle();
            }
        }

        if (getNearbyManager().isConnectedAsFollower()) {
            //do a delayed check to give Android OS time
            //to catch up from a permission being set
            if (!overlayInitialised) {
                getHandler().postDelayed(() ->
                        getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_OVERLAY, permissionManager.isOverlayPermissionGranted()),
                        1000);
            }

            //check it again
            if (!overlayInitialised && permissionManager.isOverlayPermissionGranted()) {
                initialiseOverlayView();
            }

            if (overlayView != null && getNearbyManager().isConnectedAsFollower() && studentLockOn) {
                setStudentLock(ConnectedPeer.STATUS_LOCK);

            } else if (overlayView != null && getNearbyManager().isConnectedAsFollower() && !studentLockOn) {
                setStudentLock(ConnectedPeer.STATUS_UNLOCK);
            }

            if (overlayView != null) {
                overlayView.setVisibility(View.INVISIBLE);
                url_overlay.setVisibility(View.INVISIBLE);
            }
        }

        if (accessibilityReceiver == null) {
            //set up a receiver to capture the re-broadcast intent
            accessibilityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Received rebroadcast intent: " + intent);
                    getLumiAccessibilityConnector().triageReceivedIntent(intent);
                }
            };
            Log.w(TAG, "Registering receiver");
            registerReceiver(accessibilityReceiver, new IntentFilter(LumiAccessibilityConnector.PROPAGATE_ACTION));
        }

        closeKeyboard();
        hideSystemUI();
    }

    //maintains lifecycle state for us in other classes
    boolean appHasFocus = true;

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onLifecycleStart() {
        Log.w(TAG, "LC Start");
        appHasFocus = true;
        manageFocus();
    }

    //TODO This is called and then there is no app recall?
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onLifecycleStop() {
        Log.w(TAG, "LC Stop");
        Log.d(TAG, "LeadMe: " + leadMePackageName + " Current package:" + currentTaskPackageName);
        appHasFocus = false;
        if (!permissionManager.waitingForPermission
                && currentTaskPackageName != null && currentTaskPackageName.equals(leadMePackageName)
                && getNearbyManager().isConnectedAsFollower()) {
            if(!managingAutoInstaller) {
                dispatcher.alertGuideStudentOffTask();
                recallToLeadMe();
            }
        } else {
            manageFocus();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onLifecycleDestroy() {
        Log.d(TAG, "LC Destroy");
        appHasFocus = false;
        isGuide = false;
        Log.d(TAG, "onLifecycleDestroy: "+Build.MODEL);
        if(Build.MODEL.equals("MI 8 SE")){
            if(getAccessibilityService()!=null) {
                getAccessibilityService().disableSelf();
            }
        }
        destroyAndReset();
    }

    private void manageFocus() {
        if (appHasFocus && !init) {
            //TODO call     mSensorManager.unregisterListener(this);
            Log.d(TAG, "Moving away from splash...");
            showSplashScreen();
            moveAwayFromSplashScreen();
        }

        hideSystemUI();

        AlertDialog login = dialogManager.getLoginDialog();

        if (init && login != null && login.isShowing()) {
            openKeyboard();
        }

        if (appHasFocus && appToast != null) {
            appToast.cancel();
            appToast = null;
        }

        //if there's an app to launch, do it
        if (appHasFocus && getNearbyManager().isConnectedAsFollower()) {
            //if we've got no delayed content, we're properly returning to LeadMe
            if (!getDispatcher().hasDelayedLaunchContent()) {
                //Don't disengage the lock if using the VR player
                if(!currentTaskPackageName.equals(VREmbedPlayer.packageName)) {
                    studentLockOn=false;
                }

                //if we're in lock mode and we should be in something other than LeadMe, relaunch it
                if (studentLockOn && currentTaskPackageName != null && !currentTaskPackageName.equals(leadMePackageName)) {
                    Log.e(TAG, "RELAUNCH?? " + currentTaskPackageName);

                    if (currentTaskPackageName.equals(getAppManager().withinPackage) || currentTaskPackageName.equals(getAppManager().youtubePackage)) {
                        getAppManager().relaunchLast(currentTaskPackageName, currentTaskName, currentTaskType, currentTaskURL, currentTaskURLTitle);
                    } else {
                        if(!currentTaskPackageName.equals(VREmbedPlayer.packageName)) {
                            getAppManager().relaunchLast();
                        }
                    }

                //if we have launched at least one thing previously, we might want to reset the task icon to LeadMe
                } else if (currentTaskPackageName != null) {
                    Log.e(TAG, "IS NOW A GOOD TIME TO UPDATE TO TASK ICON??");
                    getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + currentTaskPackageName + ":" + getNearbyManager().getID() + ":" + currentTaskPackageName, getNearbyManager().getAllPeerIDs());
                }
            }

            Log.d(TAG, "Focus is back! Launching delayed stuff.");

            //sometimes delayed things are stored here
            getDispatcher().launchDelayedApp();

            //sometimes they're here, so check both
            if (appIntentOnFocus != null) {
                Log.d(TAG, "[XX] Launching directly");
                verifyOverlay();
                startActivity(appIntentOnFocus);
                getAppManager().lastApp = appIntentOnFocus.getPackage();
                appIntentOnFocus = null;
                getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                        LeadMeMain.LAUNCH_SUCCESS + currentTaskName + ":" + getNearbyManager().getID() + ":" + getAppManager().lastApp, getNearbyManager().getAllPeerIDs());
            }
        }

        if (appHasFocus) {
            overlayView.setVisibility(View.INVISIBLE); //NEVER want this over LeadMe
            url_overlay.setVisibility(View.INVISIBLE);
        }
    }

    public static boolean destroying = false;

    @Override
    public void onDestroy() {
        if (getAppManager().getWithinPlayer().controllerWebView != null)
            getAppManager().getWithinPlayer().controllerWebView.destroy();
        Log.w(TAG, "In onDestroy");
        if(isGuide) {
            logoutResetController();
        }
        backgroundExecutor.shutdownNow();
        serverThreadPool.shutdownNow();
        //subscription.dispose();
        destroyAndReset();
        super.onDestroy();
    }

    private void destroyAndReset() {
        destroying = true;
        init = false;

        if (accessibilityReceiver != null) {
            try {
                unregisterReceiver(accessibilityReceiver); //stop listening for re-broadcast intents
            } catch (Exception e) {
                Log.e(TAG, "Accessibility receiver was not registered!");
            }
        }

        cleanDialogs();

        //clean up nearby connections
        isGuide = false;
        isReadyToConnect = false;
        getNearbyManager().onStop();

        //remove the overlay if necessary
        if (overlayView != null && overlayView.isAttachedToWindow()) {
            windowManager.removeView(overlayView);
            windowManager.removeView(url_overlay);
        }

        //clean up link preview assets
        getWebManager().cleanUp();

        destroying = false;
    }

    /**
     * Enables regular immersive mode.
     * For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
     * Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
     */
    public void hideSystemUIStudent() {
        Log.d(TAG, "hideSystemUI: ");
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        collapseStatus();
    }

    /**
     * Hides the UI of the operating system from interfering with the running of the application.
     */
    public void hideSystemUI() {
        if (getNearbyManager().isConnectedAsFollower()) {
            return;
        }
        View decorView = getWindow().getDecorView();
        int newUiOptions = decorView.getSystemUiVisibility();
        newUiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(newUiOptions);
    }

    public void showSystemUI() {
        View decorView = getWindow().getDecorView();
        int newUiOptions = decorView.getSystemUiVisibility();
        newUiOptions &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(newUiOptions);
    }

    public String getUUID() {
        return sessionUUID;
    }

    private int lastDisplayedIndex = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //onCreate can get called when device rotated, keyboard opened/shut, etc
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        Log.w(TAG, "On create! " + init);

        //add observer to respond to lifecycle events
        getLifecycle().addObserver(this);

        //TODO needs to be signed into play store - have to add exception handling
//        checkForUpdates();
        checkSharedPreferences();
        inflateViews();
        createAdapters();
        setupReceiver();
        setupInitialDetails();
        UIListener();
        shakeDetection();
        setFlags();
        hideSystemUI();
        setupSecondaryButtons();
        buildOverlay(); //SET UP OVERLAY (don't add it to the window manager until permission granted)
        listGridAdapters();
        setupActionButtons();
        setupAnimator();
        showSplashScreen();
        setContentView(leadmeAnimator);
        setupMenuButtons();
        setupOptionsScreen();
        prepareConnectionElements();
        checkAppVersion();

        xrayManager.screenshotManager = new ScreenshotManagerBuilder(this).withPermissionRequestCode(REQUEST_SCREENSHOT_PERMISSION) //optional, 888 is the default
                .build();

        firstTimeUser();

        CuratedContentManager.getCuratedContent(this);
    }

    /**
     * Check the firebase for the most current version number.
     */
    private void checkAppVersion() {
        getFirebaseManager().checkCurrentVersion();
    }

    /**
     * Set up the initial details a user needs that other users might have to interact with.
     */
    private void setupInitialDetails() {
        studentLockOn = false; //students start unlocked
        isGuide = false;
        isReadyToConnect = false;
        loggingInAsLeader = true;

        if (nearbyManager != null) {
            nearbyManager.setID(null);
        }

        initPermissions = false; //reset

        leadMeAppName = getResources().getString(R.string.app_title);
        leadMePackageName = getPackageName();

        setTheme(R.style.Theme_AppCompat_Light_NoActionBar); //switches from splash screen to main
        //https://android.jlelse.eu/the-complete-android-splash-screen-guide-c7db82bce565
    }

    /**
     * Store/check a random UUID for this phone in shared preferences, it will be unique and
     * anonymous plus will stay stable for a good period of time to allow re-connections during
     * a session.
     */
    private void checkSharedPreferences() {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //if a UUID exists, retrieve it
        if (sharedPreferences.contains(SESSION_UUID_TAG)) {
            sessionUUID = sharedPreferences.getString(SESSION_UUID_TAG, null);
        }

        //if none exists, make one and store it
        if (sessionUUID == null) {
            sessionUUID = UUID.randomUUID().toString();
            editor.putString(SESSION_UUID_TAG, sessionUUID);
            editor.apply();
        }

        //Change the session connect type between Manual and Auto
        if (sharedPreferences.contains(SESSION_MANUAL_TAG)) {
            sessionManual = sharedPreferences.getBoolean(SESSION_MANUAL_TAG, false);
        }

        if (sessionManual == null) {
            sessionManual = false;
            editor.putBoolean(SESSION_MANUAL_TAG, sessionManual);
            editor.apply();
        }

        //Check if the vr player has been used before
        if(sharedPreferences.contains(SESSION_VR_TAG)) {
            vrFirstTime = sharedPreferences.getBoolean(SESSION_VR_TAG, true);
        }

        if (vrFirstTime == null) {
            vrFirstTime = true;
            editor.putBoolean(SESSION_VR_TAG, vrFirstTime);
            editor.apply();
        }
    }

    /**
     * Change the shared preferences after the initial viewing of the VR player controls. Meaning
     * that a user has seen the pop up and does not need to in the future.
     * @param change A boolean determining if the user has viewed the pop up.
     */
    public void changeVRFirstTime(boolean change) {
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SESSION_VR_TAG, change);
        editor.apply();
    }

    /**
     * Check the shared preferences for file transfer and auto installer - this will only be kept
     * in shared preferences for the guide for security reasons. Learners will have to opt-in each
     * time they connect.
     */
    private void checkAddtionalPreferences() {
        //TODO might not use shared preferences here - enabled every session for security?
        //auto install application sharedpreferences
        if (sharedPreferences.contains(AUTO_INSTALL)) {
            autoInstallApps = sharedPreferences.getBoolean(AUTO_INSTALL, false);
        }

        if (autoInstallApps == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            autoInstallApps = false;
            editor.putBoolean(AUTO_INSTALL, autoInstallApps);
            editor.apply();
        }

        //file transfer sharedpreferences
        if (sharedPreferences.contains(FILE_TRANSFER)) {
            fileTransferEnabled = sharedPreferences.getBoolean(FILE_TRANSFER, false);
        }

        if (fileTransferEnabled == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            fileTransferEnabled = false;
            editor.putBoolean(AUTO_INSTALL, fileTransferEnabled);
            editor.apply();
        }
    }

    /**
     * Inflate the initial views used for adapters and animators.
     */
    private void inflateViews() {
        splashscreen = View.inflate(context, R.layout.a__splash_screen, null);
        logo = splashscreen.findViewById(R.id.lumi_logo);
        logo.setImageResource(R.mipmap.lumination_logo_reverse);
        startLeader = View.inflate(context, R.layout.b__start_leader, null);
        startLearner = View.inflate(context, R.layout.b__start_learner, null);
        mainLearner = View.inflate(context, R.layout.c__learner_main, null);
        mainLeader = View.inflate(context, R.layout.c__leader_main, null);
        optionsScreen = View.inflate(context, R.layout.d__options_menu, null);
        xrayScreen = View.inflate(context, R.layout.d__xray_view, null);
        appLauncherScreen = View.inflate(context, R.layout.d__app_list, null);
        learnerWaitingText = startLearner.findViewById(R.id.waiting_text);
        multiAppManager = View.inflate(context, R.layout.d__app_manager_list, null);
        CuratedContentManager.curatedContentScreen = View.inflate(context, R.layout.d__curated_content_list, null);
        CuratedContentManager.curatedContentScreenSingle = View.inflate(context, R.layout.curated_content_single, null);
        CuratedContentManager.setupCuratedContent(this);
    }

    /**
     * Create the necessary system services and adapters for the application.
     */
    private void createAdapters() {
        //System services
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE); //public ipAddress used in manual connection modes
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE); //for hiding keyboard using soft input
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        //Adapters
        screenSharingManager = new ScreenSharingManager(this);
        firebaseManager = new FirebaseManager(this);
        networkManager = new NetworkManager(this);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        permissionManager = new PermissionManager(this);
        authenticationManager = new AuthenticationManager(this);
        dialogManager = new DialogManager(this);
        nearbyManager = new NearbyPeersManager(this);
        dispatcher = new DispatchManager(this);
        webManager = new WebManager(this);
        leaderSelectAdapter = new LeaderSelectAdapter(this);
        lumiAccessibilityConnector = new LumiAccessibilityConnector(this);
        vrAccessibilityManager = new VRAccessibilityManager(this);
        vrEmbedPlayer = new VREmbedPlayer(this); //VR PLAYER
        appLaunchAdapter = new AppManager(this);
        xrayManager = new XrayManager(this, xrayScreen);
        fileTransferManager = new FileTransferManager(this);
        lumiAppInstaller = new AppInstaller(this);

        connectedLearnersAdapter = new ConnectedLearnersAdapter(this, new ArrayList<>(), dialogManager.alertsAdapter);
        connectedStudentsView = mainLeader.findViewById(R.id.studentListView);
        connectedStudentsView.setAdapter(connectedLearnersAdapter);
    }

    /**
     * Set up a receiver to capture the re-broadcast intent items that are sent over the network.
     */
    private void setupReceiver() {
        accessibilityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "Received rebroadcast intent: " + intent);
                getLumiAccessibilityConnector().triageReceivedIntent(intent);
            }
        };

        Log.w(TAG, "Registering receiver");
        registerReceiver(accessibilityReceiver, new IntentFilter(LumiAccessibilityConnector.PROPAGATE_ACTION));
    }

    /**
     * Listen for UI changes on a device, close the UI if the device is connected as a learner.
     */
    private void UIListener() {
        Log.d(TAG, "Adding System Visibility listener to window/decor");
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            Log.d(TAG, "DECOR VIEW! " + getNearbyManager().isConnectedAsFollower() + ", " + dialogManager.dialogShowing);
            if (getNearbyManager().isConnectedAsFollower() || OnBoardStudentInProgress) {
                if (allowHide) {
                    handler.postDelayed(this::hideSystemUIStudent, 0);
                }
            }
        });

        try {
            leadmeIcon = getPackageManager().getApplicationIcon(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set up shake detection for logging in as a guide.
     */
    private void shakeDetection() {
        mSensorManager = (SensorManager) getSystemService(LeadMeMain.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * Add window manager flags for the device to follow while running the application.
     */
    private void setFlags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /**
     * Set up the secondary buttons on the guides home page including the show alerts and search
     * button (search not currently implemented).
     */
    private void setupSecondaryButtons() {
        alertsBtn = mainLeader.findViewById(R.id.alerts_button);
        alertsBtn.setOnClickListener(view -> dialogManager.showAlertsDialog());

        mainLeader.findViewById(R.id.select_bar_back).setOnClickListener(view -> getConnectedLearnersAdapter().selectAllPeers(false));

        CheckBox checkBox = mainLeader.findViewById(R.id.select_bar_selectall);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> getConnectedLearnersAdapter().selectAllPeers(isChecked));

        mainLeader.findViewById(R.id.select_bar_repush).setOnClickListener(v -> {
            getDispatcher().repushApp(getNearbyManager().getSelectedPeerIDsOrAll());
            getConnectedLearnersAdapter().selectAllPeers(false);
        });
    }

    /**
     * Set up the adapters for the lists and grids associated with the application launch functions
     * and learner list view.
     */
    private void listGridAdapters() {
        GridView appGrid = appLauncherScreen.findViewById(R.id.app_list_grid);
        appGrid.setAdapter(getAppManager());
        layoutParams = appGrid.getLayoutParams();
        layoutParams.height = appGrid.getMeasuredHeight(); //this is in pixels
        appGrid.setLayoutParams(layoutParams);
        ((GridView) appLauncherScreen.findViewById(R.id.fav_list_grid)).setAdapter(getAppManager().getFavouritesManager());
        (appLauncherScreen.findViewById(R.id.current_task_layout)).setVisibility(View.GONE);
        (appLauncherScreen.findViewById(R.id.text_current_task)).setVisibility(View.GONE);
        ((ListView) startLearner.findViewById(R.id.leader_list_view)).setAdapter(getLeaderSelectAdapter());

        (appLauncherScreen.findViewById(R.id.repush_btn)).setOnClickListener(v -> {
            Log.d(TAG, "Repushing " + lastAppID);
            //VR player needs to select the source before reopening, handle just like fresh start.
            if(lastAppID.equals(VREmbedPlayer.packageName)) {
                //Opens up the preview player again
                getVrEmbedPlayer().showPlaybackPreview();
            } else {
                if (lastLockState != null && lastLockState.equals(LOCK_TAG)) {
                    getDispatcher().requestRemoteAppOpen(APP_TAG, lastAppID, String.valueOf(((TextView) appLauncherScreen.findViewById(R.id.current_app_name)).getText()), LOCK_TAG, "false", getNearbyManager().getSelectedPeerIDsOrAll());
                } else {
                    getDispatcher().requestRemoteAppOpen(APP_TAG, lastAppID, String.valueOf(((TextView) appLauncherScreen.findViewById(R.id.current_app_name)).getText()), UNLOCK_TAG, "false", getNearbyManager().getSelectedPeerIDsOrAll());
                }

                dialogManager.showConfirmPushDialog(true, false);
            }
        });
    }

    /**
     * Set up the main action buttons used by the guide to control the connected peers.
     */
    private void setupActionButtons() {
        mainLeader.findViewById(R.id.url_core_btn).setOnClickListener(view -> getWebManager().showWebLaunchDialog(false, false));

        mainLeader.findViewById(R.id.within_core_btn).setOnClickListener(view -> {
            getAppManager().getWithinPlayer().showWithin(); //launch within search
        });

        mainLeader.findViewById(R.id.app_core_btn).setOnClickListener(view -> {
            showAppLaunchScreen();
            appLauncherScreen.findViewById(R.id.app_scroll_view).scrollTo(0, 0);
        });

        mainLeader.findViewById(R.id.curated_content_btn).setOnClickListener(view -> {
            showCuratedContentScreen();
            appLauncherScreen.findViewById(R.id.app_scroll_view).scrollTo(0, 0);
        });

        mainLeader.findViewById(R.id.xray_core_btn).setOnClickListener(v -> {
            if (getConnectedLearnersAdapter().getCount() > 0) {
                xrayManager.showXrayView("");
            } else {
                Toast.makeText(getApplicationContext(), "No students connected.", Toast.LENGTH_SHORT).show();
            }
        });

        checkAddtionalPreferences();

        //Code in LeadMe Main & AppManager
        if(LeadMeMain.FLAG_UPDATES) {
            //file transfer button
            mainLeader.findViewById(R.id.file_core_btn).setOnClickListener(view -> {
                if (!getConnectedLearnersAdapter().someoneIsSelected()) {
                    Toast.makeText(context, "Peers need to be selected.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (fileTransferEnabled) {
                    if (isMiUiV9()) {
                        alternateFileChoice(TRANSFER_FILE_CHOICE);
                    } else {
                        FileUtilities.browseFiles(this, TRANSFER_FILE_CHOICE);
                    }
                } else {
                    dialogManager.showWarningDialog("File Transfer", "File transfer has not been enabled.");
                }
            });

            //Custom VR button
            mainLeader.findViewById(R.id.vr_core_btn).setOnClickListener(view -> {
//            if(vrVideoPath == null) {
                getVrEmbedPlayer().showPlaybackPreview();
//            } else {
//                getVrEmbedPlayer().openVideoController();
//            }

                if (vrFirstTime) {
                    vrFirstTime = false;
                    changeVRFirstTime(vrFirstTime);

                    //Function to let leaders know what files can be picked
                    getDialogManager().showVRFirstTimeDialog();
                }
            });
        }

        //multi install button
        if(LeadMeMain.FLAG_INSTALLER) {
            LinearLayout installer = mainLeader.findViewById(R.id.installer_core_btn);
            installer.setVisibility(View.VISIBLE);
            installer.setOnClickListener(view -> {
                if (autoInstallApps) {
                    getLumiAppInstaller().showMultiInstaller(layoutParams);
                } else {
                    dialogManager.showWarningDialog("Auto Installer", "Auto installing has not been enabled.");
                }
            });
        }
    }

    /**
     * Begin the alternate file choice for old Xiaomi phones.
     * @param ActivityType An integer representing how the result should be handled.
     */
    public void alternateFileChoice(int ActivityType) {
        Intent intent = new Intent(this, OpenFilePicker.class);
        startActivityForResult(intent, ActivityType);
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

    /**
     * Set up start switcher and main animator.
     */
    private void setupAnimator() {
        leadmeAnimator = (ViewAnimator) View.inflate(context, R.layout.a__viewanimator, null);
        Log.d(TAG, "Got animator: " + leadmeAnimator);
        switcherView = View.inflate(context, R.layout.a__viewswitcher, null);

        leaderLearnerSwitcher = switcherView.findViewById(R.id.viewswitcher);
        leaderLearnerSwitcher.addView(startLeader); //leader will be index 0
        leaderLearnerSwitcher.addView(startLearner); //learner will be index 1

        leadmeAnimator.addView(splashscreen);
        leadmeAnimator.addView(switcherView);
        leadmeAnimator.addView(mainLearner);
        leadmeAnimator.addView(mainLeader);
        leadmeAnimator.addView(appLauncherScreen);
        leadmeAnimator.addView(optionsScreen);
        leadmeAnimator.addView(xrayScreen);
        leadmeAnimator.addView(multiAppManager);
        leadmeAnimator.addView(CuratedContentManager.curatedContentScreen);
        leadmeAnimator.addView(CuratedContentManager.curatedContentScreenSingle);
    }

    /**
     * Setup the menus buttons for the for the leader and learner devices along with the assoicated
     * onClick functions.
     */
    private void setupMenuButtons() {
        View.OnClickListener menuListener = view -> {
            lastDisplayedIndex = leadmeAnimator.getDisplayedChild();
            leadmeAnimator.setDisplayedChild(ANIM_OPTIONS_INDEX);
            if (getNearbyManager().isConnectedAsFollower() || getNearbyManager().isConnectedAsGuide()) {
                optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.GONE);
                optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.GONE);
            } else {

                if (getAuthenticationManager().getCurrentAuthUser() != null) {
                    optionsScreen.findViewById(R.id.options_leader).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.GONE);
                    optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.GONE);
                    ((TextView) optionsScreen.findViewById(R.id.options_signed_name)).setText(getAuthenticationManager().getCurrentAuthUserName());
                } else {
                    optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_leader).setVisibility(View.GONE);
                }
            }
            if (getNearbyManager().isConnectedAsGuide()) {
                optionsScreen.findViewById(R.id.options_leader).setVisibility(View.VISIBLE);
                optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.VISIBLE);
                ((TextView) optionsScreen.findViewById(R.id.options_signed_name)).setText(getAuthenticationManager().getCurrentAuthUserName());
            } else if (getNearbyManager().isConnectedAsFollower()) {
                optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.GONE);
                optionsScreen.findViewById(R.id.options_leader).setVisibility(View.GONE);
            }
        };

        startLeader.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        startLearner.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        mainLeader.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        mainLearner.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        appLauncherScreen.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        xrayScreen.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        multiAppManager.findViewById(R.id.menu_btn).setOnClickListener(menuListener);

        //set up back buttons
        appLauncherScreen.findViewById(R.id.back_btn).setOnClickListener(view -> leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX));
        CuratedContentManager.curatedContentScreen.findViewById(R.id.back_btn).setOnClickListener(view -> leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX));

        //multi installer screen back button
        multiAppManager.findViewById(R.id.back_btn).setOnClickListener(view -> {
            //display LeadMe main page
            leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
            //cancel the multi install
            getLumiAppInstaller().multiInstalling = false;
            //reset any selected apps
            getLumiAppInstaller().resetAppSelection();
        });

        //set up options screen
        optionsScreen.findViewById(R.id.back_btn).setOnClickListener(v -> {
            Log.e(TAG, lastDisplayedIndex + " // " + nearbyManager.isConnectedAsFollower() + " // " + nearbyManager.isConnectedAsGuide() + " // " + isGuide);
            if (lastDisplayedIndex == ANIM_START_SWITCH_INDEX) {
                //we weren't logged in yet, so refresh button colours and state
                prepLoginSwitcher();
                leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);
            } else {
                //return to where we were before
                leadmeAnimator.setDisplayedChild(lastDisplayedIndex);
            }
        });

        //return to app button.
        mainLeader.findViewById(R.id.leadme_icon).setOnClickListener(v -> {
            if (isGuide) {
                dialogManager.showRecallDialog();
            } else {
                //test my connection
                if (!getNearbyManager().isConnectedAsFollower() && !getNearbyManager().isConnectedAsGuide()) {
                    Log.e(TAG, "No longer connected!");
                    logoutResetController();
                } else {
                    Log.d(TAG, "Going to test my connection: " + getNearbyManager().isConnectedAsFollower() + ", " + getNearbyManager().isConnectedAsGuide());
                    getNearbyManager().startPingThread();
                }
            }
        });
    }

    /**
     * Prepare the options screen with links, toggles and session details.
     */
    private void setupOptionsScreen() {
        optionsScreen.findViewById(R.id.options_loginBtn).setOnClickListener(view -> showLoginDialog());
        optionsScreen.findViewById(R.id.options_notsigned).setOnClickListener(view -> {
            if(!PermissionManager.isInternetAvailable(context)) {
                dialogManager.showWarningDialog("Currently Offline", "No internet access detected. Please connect to continue.");
                return;
            }
            getAuthenticationManager().buildloginsignup(0);
        });

        optionsScreen.findViewById(R.id.on_boarding).setOnClickListener(view -> buildAndDisplayOnBoard(false));

        optionsScreen.findViewById(R.id.how_to_use_btn).setOnClickListener(view -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
            startActivity(browserIntent);
        });

        optionsScreen.findViewById(R.id.help_support_btn).setOnClickListener(view -> {
            String[] email = {"dev@lumination.com.au"};
            composeEmail(email,"LeadMe Support");
        });

        //change the shared preferences, do the rest on login for guide or learner button select
        serverDiscoveryToggle = optionsScreen.findViewById(R.id.server_discovery);
        serverDiscoveryToggle.setChecked(sessionManual);
        serverDiscoveryToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Guide or student needs internet to access firebase database
            if(!PermissionManager.isInternetAvailable(context)) {
                dialogManager.showWarningDialog("Currently Offline", "No internet access detected. Please connect to continue."
                        + "\n\n Note: Try our new manual connection feature if you're having trouble");
                serverDiscoveryToggle.setChecked(false);
                return;
            }

            //Learner cannot switch while logged in
            switchManualPreference(sharedPreferences, isChecked);
        });

        //change the shared preferences for auto installing student devices
        autoToggle = optionsScreen.findViewById(R.id.auto_install_apps);
        autoToggle.setVisibility(View.GONE); //disable until logged in
        autoToggle.setChecked(autoInstallApps);
        autoToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Tell the leader what is happening when it is turned on
            if(isChecked) {
                //showWarningDialog("Auto Install", "Tell the leader something helpful.");
                Toast.makeText(context, "Auto Installing now enabled.", Toast.LENGTH_SHORT).show();
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            autoInstallApps = isChecked;
            editor.putBoolean(AUTO_INSTALL, autoInstallApps);
            editor.apply();

            //send action to student devices to change the auto install setting
            getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL + ":"
                    + autoInstallApps, getNearbyManager().getSelectedPeerIDsOrAll());
        });

        //change the shared preferences for the file transfer setting
        transferToggle = optionsScreen.findViewById(R.id.file_transfer);
        transferToggle.setVisibility(View.GONE); //disable until logged in
        transferToggle.setChecked(fileTransferEnabled);
        transferToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Tell the leader what is happening when it is turned on
            if(isChecked) {
                //showWarningDialog("Auto Install", "Tell the leader something helpful.");
                Toast.makeText(context, "File transfer is now enabled.", Toast.LENGTH_SHORT).show();
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            fileTransferEnabled = isChecked;
            editor.putBoolean(FILE_TRANSFER, fileTransferEnabled);
            editor.apply();

            //send action to student devices to change the file transfer settings?
            getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.FILE_TRANSFER + ":"
                    + fileTransferEnabled, getNearbyManager().getSelectedPeerIDsOrAll());
        });

        //direct ip input connection
        TextView manualConnect = optionsScreen.findViewById(R.id.manual_connect);
        manualConnect.setOnClickListener(view -> {
            String ipAddress = null;
            try {
                ipAddress = InetAddress.getByAddress(
                    ByteBuffer
                        .allocate(Integer.BYTES)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(wifiManager.getConnectionInfo().getIpAddress())
                        .array()
                ).getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            dialogManager.showManualDialog(isGuide, ipAddress);
        });

        optionsScreen.findViewById(R.id.logout_btn).setOnClickListener(view -> {
            if (isGuide || !getNearbyManager().isConnectedAsFollower()) {
                getAuthenticationManager().logoutAction();
                optionsScreen.findViewById(R.id.options_leader).setVisibility(View.GONE);
                logoutResetController();
            } else {
                Toast.makeText(context, "Logout is unavailable.", Toast.LENGTH_SHORT).show();
            }

        });

        optionsScreen.findViewById(R.id.options_endSess).setOnClickListener(view -> {
            if (isGuide || !getNearbyManager().isConnectedAsFollower()) {
                logoutResetController();
            } else {
                Toast.makeText(context, "Logout is unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        optionsScreen.findViewById(R.id.connected_only_view).setVisibility(View.GONE);
    }

    /**
     * Prepare the elements used to sign in to LeadMe as a leader or a learner and enable a listener
     * for when switching between the two user types.
     */
    private void prepareConnectionElements() {
        leader_toggle = switcherView.findViewById(R.id.leader_btn);
        learner_toggle = switcherView.findViewById(R.id.learner_btn);

        leader_toggle.setOnClickListener(v -> {
            loginActor = "leader";
            displayLeaderStartToggle();
            if (getAuthenticationManager().getCurrentAuthUser() == null) {
                dialogManager.changeLoginViewOptions(View.VISIBLE, View.GONE, View.GONE);
            }
        });

        if (getAuthenticationManager().getCurrentAuthUser() == null) {
            dialogManager.changeLoginViewOptions(View.VISIBLE, View.GONE, View.GONE);
        } else {
            dialogManager.changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
        }

        learner_toggle.setOnClickListener(v -> {
            loginActor = "learner";
            if(!sessionManual) {
                initiateLeaderDiscovery();
            }
            displayLearnerStartToggle();
            if (getAuthenticationManager().getCurrentAuthUser() == null) {
                dialogManager.changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
            }
        });

        //prepare elements for login dialog
        getNameViewController();

        startLeader.findViewById(R.id.app_login).setOnClickListener(v -> showLoginDialog());

        //prepare elements for leader main view
        waitingForLearners = mainLeader.findViewById(R.id.no_students_connected);

        setUpControlButtons();

        initPermissions = true;
        permissionManager.checkMiscPermissions();

        if (!permissionManager.isNearbyPermissionsGranted()) {
            permissionManager.checkNearbyPermissions();
        }

        if (!permissionManager.isStoragePermissionsGranted()) {
            permissionManager.checkStoragePermission();
        }
    }

    /**
     * Check if using the application for the first time and display the on boarding guide.
     */
    private void firstTimeUser() {
        if (!sharedPreferences.contains("FIRST")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("FIRST", true);
            editor.apply();

            dialogManager.displayGuidePrompt();
        }
    }

    /**
     * Checks for updates on the Play Store, initiates a request for an immediate update if update
     * detected. Restarts the application on completion otherwise resumes the update on restart
     * if not finished.
     */
//    private void checkForUpdates() {
//        appUpdateManager = AppUpdateManagerFactory.create(context);
//
//        // Returns an intent object that you use to check for an update.
//        com.google.android.play.core.tasks.Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
//
//        // Checks that the platform will allow the specified type of update.
//        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
//            Log.e(TAG, "Checking Play Store for updates");
//
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
//                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
//
//                // Request the update.
//                try {
//                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, UPDATE_REQUEST_CODE);
//                } catch (IntentSender.SendIntentException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        appUpdateInfoTask.addOnFailureListener(e -> Log.e("Error", "A Failure has occurred: " + e));
//    }

    // Checks that the application update is not stalled during 'onResume()'.
    // Should execute this check at all entry points into the app.
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        Log.e(TAG, "Resuming application checks");
//
//        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
//                // If an in-app update is already running, resume the update.
//                try {
//                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE,this, UPDATE_REQUEST_CODE);
//                } catch (IntentSender.SendIntentException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        appUpdateManager.getAppUpdateInfo().addOnFailureListener(e -> Log.e("Error", "A Failure has occurred: " + e));
//    }

    //MANUAL CONNECTION FUNCTIONS START
    public void switchManualPreference(SharedPreferences sharedPreferences, boolean isManual) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        sessionManual = isManual;
        editor.putBoolean(SESSION_MANUAL_TAG, sessionManual);
        editor.apply();

        if(isManual && isGuide) {
            String ipAddress = null;
            try {
                ipAddress = InetAddress.getByAddress(
                        ByteBuffer
                                .allocate(Integer.BYTES)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .putInt(wifiManager.getConnectionInfo().getIpAddress())
                                .array()
                ).getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            getFirebaseManager().createManualConnection(ipAddress);

        } else if(!isManual) {
            //reset manual connection info (switching between session discovery and other
            nearbyManager.resetManualInfo();
            getFirebaseManager().removeUserListener();

            if (!isGuide && !nearbyManager.isDiscovering()) {
                initiateLeaderDiscovery();
            }
        }
    }

    public void composeEmail(String[] addresses, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public void showLeaderScreen() {
        leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
    }

    private void showAppLaunchScreen() {
        leadmeAnimator.setDisplayedChild(ANIM_APP_LAUNCH_INDEX);
    }

    public void showCuratedContentScreen() {
        leadmeAnimator.setDisplayedChild(ANIM_CURATED_CONTENT_LAUNCH_INDEX);
    }

    public void showCuratedContentSingleScreen() {
        leadmeAnimator.setDisplayedChild(ANIM_CURATED_CONTENT_SINGLE_LAUNCH_INDEX);
    }

    public void showMultiAppInstallerScreen() {
        leadmeAnimator.setDisplayedChild(ANIM_MULTI_INDEX);
    }

    public void exitCurrentView() {
        leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
    }

    public void displayXrayView() {
        leadmeAnimator.setDisplayedChild(ANIM_XRAY_INDEX);
    }

    public ProgressBar setProgressTimer(int Time) {
        ProgressBar indeterminate = mainLeader.findViewById(R.id.leader_loading);
        if (indeterminate != null) {
            if (Time > 0) {
                runOnUiThread(() -> indeterminate.setVisibility(View.VISIBLE));
                scheduledExecutorService.schedule(() -> runOnUiThread(() -> indeterminate.setVisibility(View.INVISIBLE)), Time, TimeUnit.MILLISECONDS);
            }
        }
        return indeterminate;
    }

    public void setAlertsBtnVisibility(int visibility) {
        alertsBtn.setVisibility(visibility);
    }

    /**
     * Prepare an invisible view to cover the entire phone screen, starts off hidden but is turned
     * on and off when needing to 'lock' the interactiveness of learner devices.
     */
    protected void buildOverlay() {
        LinearLayout parentLayout = findViewById(R.id.c__leader_main);
        overlayView = LayoutInflater.from(context).inflate(R.layout.transparent_overlay, parentLayout, false);
        overlayView.findViewById(R.id.blocking_view).setVisibility(View.GONE); //default is this should be hidden

        url_overlay = LayoutInflater.from(context).inflate(R.layout.transparent_url_overlay, parentLayout, false);
    }

    public boolean overlayInitialised = false;

    public void initialiseOverlayView() {
        Log.d(TAG, "initialiseOverlayView: ");
        if (overlayInitialised /*|| !permissionManager.isOverlayPermissionGranted()*/) {
            //   Log.d(TAG, "Not initialising right now - " + overlayInitialised + ", " + permissionManager.isOverlayPermissionGranted());
            return; //already done OR don't have permission
        }

        if (overlayView == null) {
            buildOverlay();
        }

        //prepare layout parameters so overlay fills whole screen
        calcParams();

        //add overlay to the window manager
        windowManager.addView(overlayView, overlayParams);
        windowManager.updateViewLayout(overlayView, overlayParams);
        overlayView.setVisibility(View.INVISIBLE);

        //add the url overlay to the window manager
        windowManager.addView(url_overlay, url_overlayParams);
        windowManager.updateViewLayout(url_overlay, url_overlayParams);
        url_overlay.setVisibility(View.INVISIBLE);

        overlayInitialised = true; //must set this before calling disable interaction

        //set default state
        getDispatcher().disableInteraction(ConnectedPeer.STATUS_UNLOCK);
    }

    /**
     * Get the current instance of the nameView textView within the dialog manager.
     * @return An instance of the name view text view from the dialog manager.
     */
    public TextView getNameViewController() {
        return dialogManager.getNameView();
    }

    protected void prepLoginSwitcher() {
        Log.d(TAG, "Prepping switcher! " + isGuide);
        if (loggingInAsLeader) {
            displayLeaderStartToggle();
        } else {
            displayLearnerStartToggle();
        }
    }

    private void displayLeaderStartToggle() {
        loggingInAsLeader = true;
        leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEADER_INDEX);
        leader_toggle.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_active_right_leader, null));
        learner_toggle.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_passive_left_white, null));
        getNearbyManager().nsdManager.stopDiscovery();
    }

    private void displayLearnerStartToggle() {
        loggingInAsLeader = false;
        if (!permissionManager.isNearbyPermissionsGranted()) {
            learnerWaitingText.setText(getResources().getString(R.string.enable_location_to_connect));
            permissionManager.checkNearbyPermissions();
        } else if (!permissionManager.isStoragePermissionsGranted()) {
            learnerWaitingText.setText(getResources().getString(R.string.enable_storage));
            permissionManager.checkStoragePermission();
        } else if (!getNearbyManager().isConnectedAsFollower()) {
            if(sessionManual) {
                initiateManualLeaderDiscovery();
            }
            else {
                if (!nearbyManager.isDiscovering()) {
                    initiateLeaderDiscovery();
                }
            }

            learnerWaitingText.setText(getResources().getString(R.string.waiting_for_leaders));
        }
        leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEARNER_INDEX);
        leader_toggle.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_passive_right_white, null));
        learner_toggle.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_active_left_learner, null));
    }

    public void showLeaderWaitMsg(boolean show) {
        if (show) {
            learnerWaitingText.setVisibility(View.VISIBLE);
        } else {
            learnerWaitingText.setVisibility(View.GONE);
        }
    }

    /**
     * Helper methods used by multiple managers
     */
    boolean isOpen = false;

    public void openKeyboard() {
        showSystemUI();
        //Log.d(TAG, "Open keyboard! " + isOpen);
        if (isOpen) {
            return; //not needed
        }
        isOpen = true;

        View view = this.getCurrentFocus();
        if (view == null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else {
            imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    public void closeKeyboard() {
        hideSystemUI();
        View view = this.getCurrentFocus();
        //Log.d(TAG, "Close keyboard! " + isOpen);

        if (!isOpen) {
            return; //not needed
        }
        isOpen = false;

        // Check if no view has focus:
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } else {
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        }
    }

    public void closeDialogController(boolean success) {
        dialogManager.closeWaitingDialog(success);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!success) {
            //failed to login, so show login screen again
            prepLoginSwitcher();
            leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);

        } else if (!isGuide) {
            //only need this if we're a follower
            if (!permissionManager.isOverlayPermissionGranted()) {
//                permissionManager.checkOverlayPermissions();
            } else {
                initialiseOverlayView();
            }
        }
    }

    /**
     * Empty the current leader list and start discovery of leaders using the Nsd Manager.
     */
    public void initiateLeaderDiscovery() {
        Log.d(TAG, "Initiating Leader Discovery");
        getLeaderSelectAdapter().setLeaderList(new ArrayList<>());
        isReadyToConnect = true;
        getNearbyManager().discoverLeaders();
    }

    /**
     * starts the networking service, on start up the leader server starts.
     */
    public void startServer() {
        NetworkService.startServer();
    }

    /**
     * Submit a runnable to connect to a specific service.
     * @param NSDInformation An NsdServiceInfo that holds the required information to connect
     *                          to the selected leaders service.
     */
    public void manageServerConnection(NsdServiceInfo NSDInformation) {
        NetworkService.setLeaderIPAddress(NSDInformation.getHost());
        backgroundExecutor.submit(() -> networkManager.connectToServer(NSDInformation));
    }

    //MANUAL CONNECTION FOR LEARNERS
    /**
     * Empty the current leader list and stop any Nsd discovery. Start the client socket listener
     * and retrieve any a snapshot from Firebase with any leaders that are currently active.
     */
    public void initiateManualLeaderDiscovery() {
        Log.d(TAG, "Initiating Manual Leader Discovery");
        getLeaderSelectAdapter().setLeaderList(new ArrayList<>());
        isReadyToConnect = true;
        if (nearbyManager.isDiscovering()) {
            getNearbyManager().nsdManager.stopDiscovery();
        }
        getFirebaseManager().retrieveLeaders();
    }

    public boolean checkLoginDetails() {
        AtomicBoolean codeEntered = new AtomicBoolean(false);

        //reset error messages
        dialogManager.changeLoginViewOptions(-1, View.GONE, View.GONE);

        //check that a name has been entered
        boolean nameEntered;
        if (getNameViewController().getText().toString().trim().length() == 0) { //no name entered
            nameEntered = false;

            dialogManager.changeLoginViewOptions(-1, -1, View.VISIBLE);
        } else { //name entered
            nameEntered = true;
            String name = getNearbyManager().getName();
            Log.d(TAG, "Your name is now " + name);
        }

        //if appropriate, check if the correct code has been entered
        if (loggingInAsLeader) {
            //check leader code
            String code = dialogManager.getAndClearPinEntry();

            Log.d(TAG, "Code entered: " + code);
            // For showing
            dialogManager.setIndeterminateBar(View.VISIBLE);

            // For hiding
            Task<com.google.firebase.firestore.DocumentSnapshot> firebaseAccount = getAuthenticationManager().getFirebaseAccount();

            firebaseAccount.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (Hasher.Companion.hash(code, HashType.SHA_256).equals(task.getResult().getString("pin"))) {
                        codeEntered.set(true);

                        dialogManager.setIndeterminateBar(View.GONE);
                        loginAction(false);
                    } else {
                        codeEntered.set(false);
                        dialogManager.showLoginAlertMessage();
                        dialogManager.changeLoginViewOptions(-1, View.VISIBLE, -1);
                    }

                }
            });
        } else {
            codeEntered.set(true); //mark as true, since we don't need one
            if (!nameEntered) {
                //alert to errors and exit
                dialogManager.showLoginAlertMessage();
                return false; //failed
            } else {
                return true; //succeeded
            }
        }
        return false;
    }

    public void initiateLeaderAdvertising() {
        if (loggingInAsLeader) {
            getNearbyManager().setAsGuide();
        }
    }

    /**
     * Reset the application on logout so restart has a fresh slate to being with.
     */
    private void logoutResetController() {
        Log.d(TAG, "Resetting controller");
        xrayManager.resetClientMaps(null);
        //I dont like this but sometimes sending it once doesn't work....
        getDispatcher().alertLogout(); //need to send this before resetting 'isGuide'
        getDispatcher().alertLogout(); //need to send this before resetting 'isGuide'

        //Purposely block to make sure all students receive the disconnect command
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        NetworkService.resetClientIDs();
        getConnectedLearnersAdapter().resetOnLogout();
        getNearbyManager().onStop(); //disconnect everyone
        getLeaderSelectAdapter().setLeaderList(new ArrayList<>()); //empty the list
        setUIDisconnected();
        getFirebaseManager().stopService();
        getFileTransferManager().stopService();
        NetworkManager.stopService();
        showSplashScreen();
        moveAwayFromSplashScreen();
        isGuide = false;
    }

    boolean loginAttemptInAction = false;

    void loginAction(boolean isManual) {
        Log.w(TAG, "LOGGING IN " + nearbyManager.getName());
        loginAttemptInAction = true;

        //if all permissions are already granted, just continue
        if(isManual){
            leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEARNER_INDEX);
        }
        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
            initiateLeaderAdvertising();
            loginAttemptInAction = false;

        } else {
            //getPermissionsManager().checkOverlayPermissions(); //TODO Experimental - add flag

            if (!getPermissionsManager().isAccessibilityGranted()) {
                setandDisplayStudentOnBoard(0);
            } else if (getPermissionsManager().isAccessibilityGranted() && !getPermissionsManager().isOverlayPermissionGranted()) {
                setandDisplayStudentOnBoard(1);
            } else if (getPermissionsManager().isAccessibilityGranted() && getPermissionsManager().isOverlayPermissionGranted()) {
                if(!isManual) {
                    setandDisplayStudentOnBoard(2);
                }else{
                    setandDisplayStudentOnBoard(3);
                }
            }
        }

        dialogManager.hideLoginDialog(false);

        String name = getNearbyManager().getName();
        if(isManual) {
            isGuide=false;
        }
        Log.d(TAG, "Your name is " + name + ", are you a guide? " + isGuide);

        leaderAdditionalOptions(isGuide ? View.VISIBLE : View.GONE);

        getNetworkManager().startService();

        if (isGuide) {
            getFirebaseManager().startService();

            //display main guide view
            leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);

            //update options
            TextView title = mainLeader.findViewById(R.id.leader_title);
            title.setText("Hi " + name + "!");

            optionsScreen.findViewById(R.id.connected_only_view).setVisibility(View.VISIBLE);
            ((TextView) optionsScreen.findViewById(R.id.user_name)).setText(name);
            ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.light, null));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setText(getResources().getText(R.string.leader));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setTextColor(getResources().getColor(R.color.accent, null));

            SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            //if a UUID exists, retrieve it
            if (!sharedPreferences.contains("ONBOARD")) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("ONBOARD", true);
                editor.apply();
                buildAndDisplayOnBoard(true);
            }

            //if it is a manual connection session, create a firebase lookup entry
            if(sessionManual) {
                String ipAddress = null;
                try {
                    ipAddress = InetAddress.getByAddress(
                        ByteBuffer
                            .allocate(Integer.BYTES)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(wifiManager.getConnectionInfo().getIpAddress())
                            .array()
                    ).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                getFirebaseManager().createManualConnection(ipAddress);
            }

            //remove the Firebase listener if server discovery was enabled before logging in
            getFirebaseManager().removeUserListener();
        } else {
            //display main student view
            leadmeAnimator.setDisplayedChild(ANIM_LEARNER_INDEX);
            allowHide = true;
            handler.postDelayed(this::hideSystemUIStudent, 1000);

            changeStudentName(name);

            //update options
            ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.leadme_medium_grey, null));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setText(getResources().getText(R.string.learner));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setTextColor(getResources().getColor(R.color.medium, null));
        }
    }

    /**
     * Set or change a students name on a learner device. Used in login action as well as name
     * change requests.
     * @param name A string representing the name that is to be set.
     */
    public void changeStudentName(String name) {
        ((TextView) optionsScreen.findViewById(R.id.user_name)).setText(name);
        ((TextView) mainLearner.findViewById(R.id.learner_title)).setText(name);
    }

    /**
     * Disabling or enabling the visibility of additional option menu items depending on if the
     * user is a guide or not.
     * TODO: 15/02/2022 Implement data binding to reduce the code.
     * @param enabled An int representing if the views are to be enabled or disabled.
     */
    public void leaderAdditionalOptions(int enabled) {
        optionsScreen.findViewById(R.id.on_boarding).setVisibility(enabled);
        optionsScreen.findViewById(R.id.how_to_use_btn).setVisibility(enabled);
        optionsScreen.findViewById(R.id.help_support_btn).setVisibility(enabled);

        if(LeadMeMain.FLAG_UPDATES) {
            transferToggle.setVisibility(enabled);
        }

        if(LeadMeMain.FLAG_INSTALLER) {
            autoToggle.setVisibility(enabled);
        }
    }

    /**
     * Disabling or enabling the visibility of connection options for learner devices, the connection
     * options should not be available if the learner is already signed in.
     * @param enabled An int representing if the views are to be enabled or disabled.
     */
    public void toggleConnectionOptions(int enabled) {
        optionsScreen.findViewById(R.id.help_menu).setVisibility(enabled);
    }

    /**
     * Set up the lock, block & unlock buttons for the guide. On click listeners send the appropriate
     * actions to the selected peers.
     */
    private void setUpControlButtons() {
        mainLeader.findViewById(R.id.unlock_selected_btn).setOnClickListener(v -> unlockFromMainAction());
        mainLeader.findViewById(R.id.lock_selected_btn).setOnClickListener(v -> lockFromMainAction());
        mainLeader.findViewById(R.id.block_selected_btn).setOnClickListener(v -> blackoutFromMainAction());
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
    }

    public void refreshOverlay() {
        calcParams();
        if (overlayInitialised && overlayView != null) {
            windowManager.updateViewLayout(overlayView, overlayParams);
            windowManager.updateViewLayout(url_overlay, url_overlayParams);
        }

        if (appHasFocus) {
            assert overlayView != null;
            overlayView.setVisibility(View.INVISIBLE); //NEVER want this over LeadMe
            url_overlay.setVisibility(View.INVISIBLE);
        }
    }

    public static int CORE_FLAGS;

    private void calcParams() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // APPLICATION_OVERLAY FOR ANDROID 26+ AS THE PREVIOUS VERSION RAISES ERRORS
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // FOR PREVIOUS VERSIONS USE TYPE_PHONE AS THE NEW VERSION IS NOT SUPPORTED
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealSize(size);
        CORE_FLAGS = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        overlayParams = new WindowManager.LayoutParams(
            size.x,
            size.y + 160,
            0,
            -80,
            LAYOUT_FLAG, // TYPE_SYSTEM_ALERT is denied in apiLevel >=19
            CORE_FLAGS,
            PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayView.setFitsSystemWindows(false); // allow us to draw over status bar, navigation bar

        //Purpose: Blocking the URL bar at the top of the screen when launching websites
        url_overlayParams = new WindowManager.LayoutParams(
            size.x,
            220,
            0,
            0,
            LAYOUT_FLAG, // TYPE_SYSTEM_ALERT is denied in apiLevel >=19
            CORE_FLAGS,
            PixelFormat.OPAQUE
//                PixelFormat.TRANSLUCENT
        );
        url_overlayParams.gravity = Gravity.TOP | Gravity.START;
        url_overlay.setFitsSystemWindows(false); // allow us to draw over status bar, navigation bar
    }

    public boolean isMuted = false;

    public void muteAudio() {
        isMuted = true;
        AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_ALLOW_RINGER_MODES);
        //mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        //mAudioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
    }

    public void unMuteAudio() {
        isMuted = false;
        AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_ALLOW_RINGER_MODES);
        //mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        //mAudioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
    }

    public boolean verifyOverlay() {
        if (overlayView == null && getNearbyManager().isConnectedAsFollower() && getNearbyManager().getID() != null) {
            Toast.makeText(this, "Overlay is not working. Please turn on correct permissions.", Toast.LENGTH_SHORT).show();
            getDispatcher().alertGuidePermissionGranted(STUDENT_NO_OVERLAY, false);
            permissionManager.checkOverlayPermissions();
        } else {
            //in case it hasn't been done yet
            initialiseOverlayView();
        }
        return overlayView != null;
    }

    public void blackout(boolean on) {
        if (on) {
            muteAudio();
        } else {
            unMuteAudio();
        }

        if (!verifyOverlay()) {
            return;
        }

        View blocking_view = overlayView.findViewById(R.id.blocking_view);
        if (on) {
            blocking_view.setVisibility(View.VISIBLE);
        } else {
            blocking_view.setVisibility(View.GONE);
        }
    }

    private int prevStatus = -1;

    public void setStudentLock(int status) {

        studentLockOn = (status == ConnectedPeer.STATUS_BLACKOUT || status == ConnectedPeer.STATUS_LOCK);

        Log.d(TAG, "Is locked? " + studentLockOn + ", " + prevStatus + " vs " + status);

        //don't need to alert to anything, already in this state
        if (prevStatus != status && getNearbyManager().isConnectedAsFollower()) {
            prevStatus = status;

            String statusMsg;
            if (isGuide) {
                statusMsg = "Students are now in ";
            } else {
                statusMsg = "You are now in ";
            }

            switch (status) {
                case ConnectedPeer.STATUS_LOCK:
                    statusMsg += getResources().getString(R.string.lock).toUpperCase() + " mode.";
                    break;
                case ConnectedPeer.STATUS_BLACKOUT:
                    statusMsg += getResources().getString(R.string.block).toUpperCase() + " mode.";
                    break;
                case ConnectedPeer.STATUS_UNLOCK:
                    statusMsg += getResources().getString(R.string.unlock).toUpperCase() + " mode.";
                    break;
                default:
                    Log.e(TAG, "Invalid status: " + status);
                    return; //invalid status
            }

            String finalStatusMsg = statusMsg;
            getHandler().post(() -> {
                Toast studentStatus = Toast.makeText(context, finalStatusMsg, Toast.LENGTH_SHORT);
                studentStatus.show();
            });
        }

        getHandler().post(() -> {
            if (!verifyOverlay()) {
                return;
            }

            Log.w(TAG, "In leadmemain: " + leadmeAnimator.isShown() + ", " + getPermissionsManager().waitingForPermission + ", " + status);

            if (!leadmeAnimator.isShown()) {
                switch (status) {
                    case ConnectedPeer.STATUS_LOCK:
                    case ConnectedPeer.STATUS_BLACKOUT:
                        if (!getPermissionsManager().waitingForPermission) {
                            overlayView.setVisibility(View.VISIBLE);
                            url_overlay.setVisibility(View.INVISIBLE);
                        }
                        break;
                    case ConnectedPeer.STATUS_UNLOCK:
                        overlayView.setVisibility(View.INVISIBLE);
                        break;
                    default:
                        Log.e(TAG, "Invalid status: " + status);
                }
            }
        });
    }

    public void updateFollowerCurrentTaskToLeadMe() {
        if (currentTaskPackageName != leadMePackageName) {
            activityManager.killBackgroundProcesses(currentTaskPackageName);
        }
        updateFollowerCurrentTask(leadMePackageName, leadMeAppName, "Application", "", "");
    }

    public void updateFollowerCurrentTask(String packageName, String appName, String taskType, String url, String urlTitle) {
        try {
            //initialise everything
            if (currentTaskIcon == null) {
                currentTaskIcon = mainLearner.findViewById(R.id.current_task_icon);
                currentTaskTitle = mainLearner.findViewById(R.id.current_task_name);
                currentTaskDescription = mainLearner.findViewById(R.id.current_task_desc);
                currentTaskLaunchBtn = mainLearner.findViewById(R.id.launch_btn);
                currentTaskIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));

                currentTaskLaunchBtn.setOnClickListener(view -> {
                    Log.d(TAG, "Clicking launch! " + currentTaskPackageName);
                    try {
                        appLaunchAdapter.relaunchLast(currentTaskPackageName, currentTaskName, currentTaskType, currentTaskURL, currentTaskURLTitle);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            currentTaskPackageName = packageName;
            currentTaskName = appName;
            currentTaskURL = url;
            currentTaskURLTitle = urlTitle;
            currentTaskType = taskType;

            currentTaskIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
            currentTaskTitle.setText(appName);
            currentTaskDescription.setText(taskType);
            currentTaskLaunchBtn.setEnabled(true);
            currentTaskLaunchBtn.setBackgroundResource(R.drawable.bg_active);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    private void killAllBackgroundProcesses(){
//        List<ApplicationInfo> packages;
//        PackageManager pm;
//        pm = getPackageManager();
//        //get a list of installed apps.
//        packages = pm.getInstalledApplications(0);
//
//        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//
//        for (ApplicationInfo packageInfo : packages) {
//            Log.w(TAG, "Found: "+packageInfo.packageName+", "+packageInfo.name+", "+packageInfo.uid);
//            if (((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) || packageInfo.packageName.equals(this.getPackageName())) {
//                continue;
//            }
//            mActivityManager.killBackgroundProcesses(packageInfo.packageName);
//        }
//    }


    /**
     * FOR USE BY CLIENT APP
     */
    public void exitByGuide() {
        stopLockTask();
        setResult(0);
        finish();
    }

    /**
     * Return a learner to the LeadMe application, determines if there are other apps waiting to
     * be pushed and executes.
     */
    public void recallToLeadMe() {
        Log.d(TAG,
                "Returning. State: " + getLifecycle().getCurrentState() +
                "Animator: " + leadmeAnimator.getDisplayedChild() +
                "Focus: " + appHasFocus);

        if (leadmeAnimator.getDisplayedChild() == ANIM_START_SWITCH_INDEX || isGuide){
            return;
        }
        getWebManager().reset();
        getLumiAccessibilityConnector().resetState();

        if (appHasFocus && hasWindowFocus()) {
            Log.d(TAG, "Already in LeadMe! " + appHasFocus + ", " + hasWindowFocus());
            return;
        }

        if (getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            finish();
            return;
        }

        Log.d(TAG, "Recalling to LeadMe! " + dialogManager.dialogShowing + ", " + appHasFocus + ", " + hasWindowFocus() + ", " + getLifecycle().getCurrentState());
        closeKeyboard();
        permissionManager.needsRecall = false;
        getLumiAccessibilityConnector().bringMainToFront(); //call each other until it works
        getLumiAccessibilityConnector().withinManager.cleanUpVideo(); //end video

        Intent intent = new Intent(this, LeadMeMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        getAppManager().lastApp = intent.getPackage();
        activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);

        if (appToast == null) {
            appToast = Toast.makeText(context, "Returning to " + getResources().getString(R.string.app_title), Toast.LENGTH_SHORT);
            appToast.show();
        }
    }

    public void lockFromMainAction() {
        Set<String> chosen;
        if (getConnectedLearnersAdapter().someoneIsSelected()) {
            chosen = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosen = getNearbyManager().getAllPeerIDs();
        }
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, chosen);
    }

    public void unlockFromMainAction() {
        Set<String> chosen;
        if (getConnectedLearnersAdapter().someoneIsSelected()) {
            chosen = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosen = getNearbyManager().getAllPeerIDs();
        }
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, chosen);
    }

    public void blackoutFromMainAction() {
        Set<String> chosen;
        if (getConnectedLearnersAdapter().someoneIsSelected()) {
            chosen = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosen = getNearbyManager().getAllPeerIDs();
        }
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.BLACKOUT_TAG, chosen);
    }

    //main function, can return everyone or only selected learners
    public void returnToAppFromMainAction(boolean returnSelected) {
        Log.d(TAG, "Returning to app from MAIN! " + appHasFocus + ", " + hasWindowFocus());

        Set<String> chosenPeers;
        if (returnSelected && getConnectedLearnersAdapter().someoneIsSelected()) {
            chosenPeers = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosenPeers = getNearbyManager().getAllPeerIDs();
        }
        Log.i(TAG, "Returning " + chosenPeers.size() + " learners to LeadMe (" + chosenPeers.toString() + ") vs me: " + getNearbyManager().getID());
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.RETURN_TAG, chosenPeers);

        if (appToast == null) {
            appToast = Toast.makeText(context, "Returning selected followers to Lumination Lead Me app", Toast.LENGTH_SHORT);
            appToast.show();
        }
    }

    //UPDATE OPTIONS PAGE
    public void setUIDisconnected() {
        //reset state
        isReadyToConnect = false; //need to press button first
        getLumiAccessibilityConnector().resetState();

        cleanDialogs();

        //reset views
        showConnectedStudents(false);

        if (overlayView != null) {
            overlayView.setVisibility(View.INVISIBLE);
        }

        dialogManager.readyBtn.setEnabled(true);
        dialogManager.readyBtn.setText(R.string.connect_label);
        if (isGuide) {
            waitingForLearners.setVisibility(View.GONE);
        }

        Log.d(TAG, "SET UI DISCONNECTED");

        //update options screen
        optionsScreen.findViewById(R.id.connected_only_view).setVisibility(View.GONE);
        ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.light, null));

        //Display connection options for learners
        toggleConnectionOptions(View.VISIBLE);

        //display login view
        prepLoginSwitcher();
        leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);

        if (!nearbyManager.isDiscovering()) {
            initiateLeaderDiscovery();
        }
    }

    public void collapseStatus() {
        if (/*!getNearbyManager().isConnectedAsGuide() &&*/ !getNearbyManager().isConnectedAsFollower()) {
            //only enforce this for connected students
            return;
        }
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(closeDialog);
        handler.postDelayed(() -> sendBroadcast(closeDialog), 3000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void showConnectedStudents(boolean show) {
        if (show) {
            connectedStudentsView.setVisibility(View.VISIBLE);
        } else {
            connectedStudentsView.setVisibility(View.GONE);
        }
    }

    /**
     * Set the selected leader's name at the top of the learner's home page.
     * @param name A string representing the name of the current leader.
     */
    public void setLeaderName(String name) {
        leaderName = name;
        ((TextView) mainLearner.findViewById(R.id.leader_name)).setText(name);
        mainLearner.findViewById(R.id.connected_icon).setVisibility(View.VISIBLE);
        mainLearner.findViewById(R.id.connected_txt).setVisibility(View.VISIBLE);
    }

    /**
     * Sets a message for a peer device if there is a background process occurring such as a file
     * transfer.
     * @param message An integer representing the string table value of the text to be set.
     */
    public void setDeviceStatusMessage(int message) {
        if(!fileTransferEnabled && message == R.string.waiting_for_transfer) {
            transferError("File transfer not enabled", getNearbyManager().myID);
            permissionDenied(LeadMeMain.FILE_TRANSFER);
        } else {
            TextView peerDeviceStatus = mainLearner.findViewById(R.id.connected_txt);
            runOnUiThread(() -> peerDeviceStatus.setText(getApplicationContext().getString(message)));
        }
    }

    boolean mIsRestoredToTop = false;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ((intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) > 0) {
            mIsRestoredToTop = true;
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (!isTaskRoot() && mIsRestoredToTop) {
            // 4.4.2 platform issues for FLAG_ACTIVITY_REORDER_TO_FRONT,
            // reordered activity back press will go to home unexpectedly,
            // Workaround: move reordered activity current task to front when it's finished.
            ActivityManager tasksManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            tasksManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }

    public boolean isAppVisibleInForeground() {
        boolean screenOn = false;
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() == Display.STATE_ON) {
                screenOn = true;
                break;
            }
        }
        return appHasFocus && screenOn && (!init || leadmeAnimator.isShown() || (OnBoard != null && OnBoard.isShown()));
    }

    public void updateLastTask(Drawable icon, String Name, String appID, String lock) {
        lastAppID = appID;
        lastLockState = lock;
        Log.d(TAG, "updateLastTask: " + Name);
        (appLauncherScreen.findViewById(R.id.current_task_layout)).setVisibility(View.VISIBLE);
        (appLauncherScreen.findViewById(R.id.text_current_task)).setVisibility(View.VISIBLE);
        ((ImageView) appLauncherScreen.findViewById(R.id.current_icon)).setImageDrawable(icon);
        ((TextView) appLauncherScreen.findViewById(R.id.current_app_name)).setText(Name);
        appLauncherScreen.invalidate();
        updateLastOffTask();
    }

    public void updateLastOffTask() {
        int offTask = getConnectedLearnersAdapter().alertsAdapter.getCount();
        ((TextView) appLauncherScreen.findViewById(R.id.current_offtask)).setText(Html.fromHtml("<font color=#1599F3>" + offTask + "</font><font color=#AFB7CB> may be off task</font>", Html.FROM_HTML_MODE_LEGACY));
    }

    public void displaySelectBar(int numSelected) {
        boolean show = false;
        if (numSelected > 0) {
            show = true;
        }

        LinearLayout selectBar = mainLeader.findViewById(R.id.select_bar);
        if (show) {
            selectBar.setVisibility(View.VISIBLE);
        } else {
            selectBar.setVisibility(View.GONE);
        }

        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> getConnectedLearnersAdapter().selectAllPeers(isChecked);

        CheckBox checkBox = mainLeader.findViewById(R.id.select_bar_selectall);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(numSelected == getConnectedLearnersAdapter().getCount());
        checkBox.setOnCheckedChangeListener(listener);
    }

    public int onBoardPage = 0;
    public View OnBoard;

    @SuppressLint("ClickableViewAccessibility")
    private void buildAndDisplayOnBoard(boolean firstTime) {
        Log.d(TAG, "buildAndDisplayOnBoard: ");
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.welcome);
        //check if has been displayed before
        OnBoard = View.inflate(this, R.layout.c__onboarding, null);
        VideoView video = OnBoard.findViewById(R.id.animation_view);
        video.setBackgroundColor(Color.WHITE);
        video.setVideoURI(videoUri);
        video.requestFocus();
        video.start();
        video.setOnCompletionListener(mp -> video.start());
        video.setOnPreparedListener(mp -> handler.postDelayed(() -> video.setBackgroundColor(Color.TRANSPARENT), 150));


        GestureDetector gestureDetector = new GestureDetector(this, new OnboardingGestureDetector(this));
        OnBoard.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        ImageView[] buttons = {OnBoard.findViewById(R.id.onboard_btn_1), OnBoard.findViewById(R.id.onboard_btn_2), OnBoard.findViewById(R.id.onboard_btn_3), OnBoard.findViewById(R.id.onboard_btn_4), OnBoard.findViewById(R.id.onboard_btn_5)};
        for (int i = 0; i < buttons.length; i++) {
            int ind = i;
            buttons[i].setOnClickListener(new View.OnClickListener() {
                final int index = ind;

                @Override
                public void onClick(View v) {
                    setOnboardCurrent(index);
                }
            });
        }
        ImageView nextButton = OnBoard.findViewById(R.id.next_button);
        nextButton.setOnClickListener(v -> setOnboardCurrent(onBoardPage + 1));

        Button begin = OnBoard.findViewById(R.id.onboard_ok_btn);
        if(!firstTime) {
            begin.setText(R.string.finish);
        } else {
            begin.setText(R.string.begin);
        }

        TextView skipIntro = OnBoard.findViewById(R.id.skip_intro);
        skipIntro.setOnClickListener(v -> setContentView(leadmeAnimator));
        setOnboardCurrent(0);
        this.setContentView(OnBoard);
    }

    public void setOnboardCurrent(int current) {
        Log.d(TAG, "setOnboardCurrent: ");

        ImageView[] buttons = {
                OnBoard.findViewById(R.id.onboard_btn_1),
                OnBoard.findViewById(R.id.onboard_btn_2),
                OnBoard.findViewById(R.id.onboard_btn_3),
                OnBoard.findViewById(R.id.onboard_btn_4),
                OnBoard.findViewById(R.id.onboard_btn_5),
                OnBoard.findViewById(R.id.onboard_btn_6)};

        String[] titleToShow = {
                getResources().getString(R.string.onboard_title_1),
                getResources().getString(R.string.onboard_title_2),
                getResources().getString(R.string.onboard_title_3),
                getResources().getString(R.string.onboard_title_4),
                getResources().getString(R.string.onboard_title_5),
                getResources().getString(R.string.onboard_title_6)};

        String[] textToShow = {
                getResources().getString(R.string.onboard_1),
                getResources().getString(R.string.onboard_2),
                getResources().getString(R.string.onboard_3),
                getResources().getString(R.string.onboard_4),
                getResources().getString(R.string.onboard_5),
                getResources().getString(R.string.onboard_6)};

        Uri[] videos = {Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.welcome), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.push_app),
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.block), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.manage),
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.watchvideo), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.recall)};

        Animation in = AnimationUtils.makeInAnimation(this, false);
        Animation out = AnimationUtils.makeOutAnimation(this, false);
        TextSwitcher onBoardTitle = OnBoard.findViewById(R.id.onBoard_title);
        TextSwitcher onBoardContent = OnBoard.findViewById(R.id.onBoard_content);
        onBoardContent.setInAnimation(in);
        onBoardContent.setOutAnimation(out);
        onBoardTitle.setInAnimation(in);
        onBoardTitle.setOutAnimation(out);

        VideoView video = OnBoard.findViewById(R.id.animation_view);
        TextView skipIntro = OnBoard.findViewById(R.id.skip_intro);
        Button watchVideo = OnBoard.findViewById(R.id.watch_video);
        watchVideo.setOnClickListener(view -> {
            String cleanURL = WebManager.cleanYouTubeURL("https://www.youtube.com/watch?v=m96imGHXGGM");
            final String youTubePackageName = getAppManager().youtubePackage;
            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanURL));
            appIntent.setPackage(youTubePackageName);
            startActivity(appIntent);
        });
        ImageView nextButton = OnBoard.findViewById(R.id.next_button);
        LinearLayout buttonsLayout = OnBoard.findViewById(R.id.onboard_buttons);
        LinearLayout onboardPages = OnBoard.findViewById(R.id.onboard_pages);
        buttonsLayout.setVisibility(View.GONE);
        onboardPages.setVisibility(View.VISIBLE);
        if (current < onBoardPage) {
            onBoardContent.setInAnimation(AnimationUtils.makeInAnimation(getApplicationContext(), true));
            onBoardContent.setOutAnimation(AnimationUtils.makeOutAnimation(getApplicationContext(), true));
            onBoardTitle.setInAnimation(AnimationUtils.makeInAnimation(getApplicationContext(), true));
            onBoardTitle.setOutAnimation(AnimationUtils.makeOutAnimation(getApplicationContext(), true));
        } else if (current > onBoardPage) {
            onBoardContent.setInAnimation(AnimationUtils.makeInAnimation(getApplicationContext(), false));
            onBoardContent.setOutAnimation(AnimationUtils.makeOutAnimation(getApplicationContext(), false));
            onBoardTitle.setInAnimation(AnimationUtils.makeInAnimation(getApplicationContext(), false));
            onBoardTitle.setOutAnimation(AnimationUtils.makeOutAnimation(getApplicationContext(), false));
        } else {
            return;
        }
        onBoardPage = current;
        for (int j = 0; j < buttons.length; j++) {
            if (j != onBoardPage) {
                buttons[j].setImageTintList(getResources().getColorStateList(R.color.leadme_medium_grey, null));
            } else {
                buttons[j].setImageTintList(getResources().getColorStateList(R.color.leadme_blue, null));
            }
        }

        onBoardTitle.setText(titleToShow[onBoardPage]);
        onBoardContent.setText(textToShow[onBoardPage]);
        video.setVideoURI(videos[onBoardPage]);
        video.requestFocus();
        video.start();
        if (onBoardPage < 5) {
            skipIntro.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
            onboardPages.setVisibility(View.VISIBLE);
            buttonsLayout.setVisibility(View.GONE);
            if (onBoardPage == 4) {
                watchVideo.setVisibility(View.VISIBLE);
            } else {
                watchVideo.setVisibility(View.GONE);
            }
        }
        if (onBoardPage == 5) {
            watchVideo.setVisibility(View.GONE);
            skipIntro.setVisibility(View.GONE);
            nextButton.setVisibility(View.GONE);
            buttonsLayout.setVisibility(View.VISIBLE);
            OnBoard.findViewById(R.id.onboard_ok_btn).setOnClickListener(v1 -> {
                video.setBackgroundColor(Color.WHITE);
                handler.postDelayed(() -> setContentView(leadmeAnimator), 50);
                video.suspend();
                OnBoard = null;
            });
            OnBoard.findViewById(R.id.onboard_moreinfo_btn).setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1GGU7GeR4Ibq60-6bcc2F_bd698CKRFvZ/view"));
                startActivity(browserIntent);
            });
        }
    }

    private boolean OnBoardStudentInProgress = false;

    ScheduledFuture<?> scheduledCheck=null;
    public void setandDisplayStudentOnBoard(int page) {
        Log.d(TAG, "setandDisplayStudentOnBoard: " + page);
        OnBoardStudentInProgress = true;
        hideSystemUIStudent();
        if (page == 0) {
            View OnBoardPerm = View.inflate(this, R.layout.c__onboarding_student, null);
            TextView support = OnBoardPerm.findViewById(R.id.onboardperm_support);
            Button okPermission = OnBoardPerm.findViewById(R.id.onboardperm_ok_btn);
            Button cancelPermission = OnBoardPerm.findViewById(R.id.onboardperm_cancel_btn);
            okPermission.setOnClickListener(v -> getPermissionsManager().requestAccessibilitySettingsOn());

            support.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
                startActivity(browserIntent);
            });

            cancelPermission.setOnClickListener(view -> {
                leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);
                this.setContentView(leadmeAnimator);
            });

            this.setContentView(OnBoardPerm);

        } else if (page == 1) {
            View OnBoardPerm = View.inflate(this, R.layout.c__onboarding_student_2, null);
            TextView support = OnBoardPerm.findViewById(R.id.onboardperm_support);

            support.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
                startActivity(browserIntent);
            });

            Button okPermission = OnBoardPerm.findViewById(R.id.onboardperm_ok_btn);
            Button cancelPermission = OnBoardPerm.findViewById(R.id.onboardperm_cancel_btn);

            okPermission.setOnClickListener(view -> {
                Log.d(TAG, "setStudentOnBoard: checking");
                scheduledCheck = scheduledExecutorService.scheduleAtFixedRate(() -> {
                    if (getPermissionsManager().isOverlayPermissionGranted()) {
                        runOnUiThread(() -> {
                            if(getFirebaseManager().getServerIP().length()>0){
                                setandDisplayStudentOnBoard(3);
                            }else{
                                setandDisplayStudentOnBoard(2);
                            }
                        });
                        scheduledCheck.cancel(true);
                    }
                },100,200,TimeUnit.MILLISECONDS);
                if (getPermissionsManager().isOverlayPermissionGranted()) {
                    setandDisplayStudentOnBoard(2);
                }
                getPermissionsManager().checkOverlayPermissions();
            });

            cancelPermission.setOnClickListener(view -> {
                leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);
                this.setContentView(leadmeAnimator);
            });
            this.setContentView(OnBoardPerm);

        } else {
            if(scheduledCheck != null){
                scheduledCheck.cancel(true);
            }
            setContentView(leadmeAnimator);
            OnBoardStudentInProgress = false;
            updateFollowerCurrentTaskToLeadMe();
            loginAttemptInAction = false;

            //Check if LeadMe has focus, only try to connect if in the application
            scheduledCheck = scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (appHasFocus) {
                    runOnUiThread(() -> connectOnReturn(page));
                }
            },750,1000,TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Called when overlay permission has been granted. Cancel any scheduled check and connect
     * to a guide, starting the screen sharing service in the background.
     * @param page An integer representing which on boarding page the learner is currently on.
     */
    public void connectOnReturn(int page) {
        if(scheduledCheck != null){
            scheduledCheck.cancel(true);
        }

        screenSharingManager.startService(false);

        if(page==2 && !sessionManual && !directConnection) {
            getNearbyManager().connectToSelectedLeader();
            dialogManager.showWaitingForConnectDialog();
        } else if(sessionManual || directConnection) {
            //If the serverIP address has not changed set it to the locally found guide
            if(getFirebaseManager().getServerIP().equals("")) {
                getFirebaseManager().setServerIP(getNearbyManager().selectedLeader.getID());
            }

            getNearbyManager().connectToManualLeader(getNearbyManager().selectedLeader.getDisplayName(),
                    getFirebaseManager().getServerIP());

            directConnection = false;
        }

        toggleConnectionOptions(View.GONE); //Remove connection options
        getFirebaseManager().removeUserListener(); //remove the Firebase listener if connection was manual
        getNearbyManager().nsdManager.stopDiscovery();

        optionsScreen.findViewById(R.id.connected_only_view).setVisibility(View.VISIBLE);
    }

    public void onTrimMemory(int level) {

        // Determine which lifecycle or system event was raised.
        switch (level) {

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                //screenCap.sendImages=false;
                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */

                break;

            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                recallToLeadMe();
                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */

                break;

            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }

    public void setProgressSpinner(int Time, ProgressBar indeterminate) {
        if (indeterminate != null) {
            if (Time > 0) {
                indeterminate.setVisibility(View.VISIBLE);
                if(scheduledExecutorService.isShutdown()){
                    scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
                }
                scheduledExecutorService.schedule(() -> runOnUiThread(() -> indeterminate.setVisibility(View.INVISIBLE)), Time, TimeUnit.MILLISECONDS);
            }
        }
    }

    private int pinCodeInd = 0;
    int savedViewIndex=-1;

    /**
     * Set and display the pin entered by the user.
     * @param page An int representing the stage the user is up to.
     */
    public void setAndDisplayPinReset(int page){
        AlertDialog login = dialogManager.getLoginDialog();

        if(login !=null && login.isShowing()) {
            login.dismiss();
        }

        View resetPinView = View.inflate(this, R.layout.c__forgot_pin, null);
        if(savedViewIndex==-1){
            savedViewIndex= leadmeAnimator.getDisplayedChild();
        }

        leadmeAnimator.addView(resetPinView);
        leadmeAnimator.setDisplayedChild(leadmeAnimator.getChildCount()-1);
        Button confirm = resetPinView.findViewById(R.id.pin_reset_confirm);
        Button cancel = resetPinView.findViewById(R.id.pin_reset_cancel);
        View[] pages = {resetPinView.findViewById(R.id.pin_reset_pass_view),resetPinView.findViewById(R.id.set_pin),resetPinView.findViewById(R.id.pin_reset_finish_view)};
        ProgressBar pBar = resetPinView.findViewById(R.id.pin_reset_spinner);
        pBar.setVisibility(View.INVISIBLE);

        resetPinView.setOnClickListener(v -> dialogManager.hideSoftKeyboard(v));

        switch(page){
            case 0:
                pages[0].setVisibility(View.VISIBLE);
                pages[1].setVisibility(View.GONE);
                pages[2].setVisibility(View.GONE);
                TextView error = resetPinView.findViewById(R.id.pin_reset_error);
                EditText Pass = resetPinView.findViewById(R.id.pin_reset_password);
                TextView forgotPass = resetPinView.findViewById(R.id.pin_reset_forgot_password);

                forgotPass.setOnClickListener(view -> {
                    assert login != null;
                    getAuthenticationManager().showForgottenPassword(login);
                });

                confirm.setOnClickListener(view -> {
                    if (Pass != null && Pass.getText().toString().length() > 0) {
                        setProgressSpinner(5000, pBar);

                        //move to authentication
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        assert user != null;
                        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), Pass.getText().toString());

                        user.reauthenticate(credential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                setAndDisplayPinReset(1);
                            } else {
                                error.setVisibility(View.VISIBLE);
                                pBar.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });

                cancel.setOnClickListener(view -> {
                    pBar.setVisibility(View.INVISIBLE);
                    leadmeAnimator.setDisplayedChild(savedViewIndex);
                    savedViewIndex=-1;
                    leadmeAnimator.removeView(resetPinView);
                });
                break;

            case 1:
                closeKeyboard();
                pages[1].setVisibility(View.VISIBLE);
                pages[0].setVisibility(View.GONE);
                pages[2].setVisibility(View.GONE);
                EditText[] codes = {resetPinView.findViewById(R.id.signup_pin1), resetPinView.findViewById(R.id.signup_pin2), resetPinView.findViewById(R.id.signup_pin3)
                        , resetPinView.findViewById(R.id.signup_pin4), resetPinView.findViewById(R.id.signup_pin5), resetPinView.findViewById(R.id.signup_pin6)
                        , resetPinView.findViewById(R.id.signup_pin7), resetPinView.findViewById(R.id.signup_pin8)};
                TextWatcher pinWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s != null && s.length() == 1 && pinCodeInd < 7) {
                            pinCodeInd++;
                            codes[pinCodeInd].requestFocus();

                        } else if (s != null && s.length() == 0 && pinCodeInd > 0) {
                            pinCodeInd--;
                            codes[pinCodeInd].requestFocus();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                };
                View.OnKeyListener codeKeyListener = (v, keyCode, event) -> {
                    //View focus = null;
                    if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_BACK) {
                        if (pinCodeInd > 0) {
                            pinCodeInd--;
                            codes[pinCodeInd].requestFocus();
                        }
                    }
                    return false; //true if event consumed, false otherwise
                };

                for (EditText code : codes) {
                    code.addTextChangedListener(pinWatcher);
                    code.setOnKeyListener(codeKeyListener);
                }

                confirm.setOnClickListener(view -> {
                    closeKeyboard();
                    StringBuilder pin = new StringBuilder();
                    StringBuilder confirmPin = new StringBuilder();

                    for (int i = 0; i < 8; i++) {
                        if (i < 4) {
                            pin.append(codes[i].getText().toString());
                        } else {
                            confirmPin.append(codes[i].getText().toString());
                        }
                    }

                    if(pin.toString().equals(confirmPin.toString())){
                        setProgressSpinner(5000, pBar);

                        Task<Void> setPin = getAuthenticationManager().setAccountPin(pin.toString());

                        setPin.addOnCompleteListener(task -> {
                            if(task.isSuccessful()){
                                setAndDisplayPinReset(2);
                            }else{
                                pBar.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });

                cancel.setText(R.string.close);
                cancel.setOnClickListener(view -> {
                    leadmeAnimator.setDisplayedChild(savedViewIndex);
                    savedViewIndex=-1;
                    leadmeAnimator.removeView(resetPinView);
                });
                break;

            case 2:
                pages[2].setVisibility(View.VISIBLE);
                pages[1].setVisibility(View.GONE);
                pages[0].setVisibility(View.GONE);

                confirm.setText(R.string.finish);
                confirm.setOnClickListener(view -> {
                    leadmeAnimator.setDisplayedChild(savedViewIndex);
                    savedViewIndex=-1;
                    leadmeAnimator.removeView(resetPinView);
                });

                cancel.setOnClickListener(view -> {
                    leadmeAnimator.setDisplayedChild(savedViewIndex);
                    savedViewIndex=-1;
                    leadmeAnimator.removeView(resetPinView);
                });
                break;

            default:
                leadmeAnimator.setDisplayedChild(savedViewIndex);
                savedViewIndex=-1;
                leadmeAnimator.removeView(resetPinView);
                break;
        }
    }

    /**
     * Try login a user, checking the users have entered details and validate them. Also check
     * current permissions.
     */
    public void tryLogin() {
        boolean success = checkLoginDetails();
        if (!success) {
            return; //not ready to proceed yet
        }

        Log.d(TAG, "CLICKED READY!");
        canAskForAccessibility = true; //reset
        closeKeyboard();

        // if we're still waiting on some permissions,
        // test for them and wait for the result before continuing
        if (!permissionManager.isNearbyPermissionsGranted()) {

            //re-check permissions and wait until all granted before
            //trying to connect to other LeadMe users
            permissionManager.checkNearbyPermissions();

        } else if (!permissionManager.isStoragePermissionsGranted()) {
            permissionManager.checkStoragePermission();
        } else {
            //don't need to wait, so just login
            initPermissions = true; //only prompt once here
            loginAction(false);
        }
    }

    /**
     * Display the initial splash screen for application.
     */
    public void showSplashScreen() {
        logo.setImageResource(R.mipmap.lumination_logo_reverse);
        leadmeAnimator.setDisplayedChild(ANIM_SPLASH_INDEX);
    }

    /**
     * Clean up any dialog boxes that may be present on the screen.
     */
    public void cleanDialogs() {
        dialogManager.cleanUpDialogs();
    }

    /**
     * Update the status or warning icon of the connected peer.
     * @param peerID A string representing the connected peer.
     * @param status A int representing the status to update to.
     * @param msg An optional message used for warning alerts.
     */
    public void updatePeerStatus(String peerID, int status, String msg) {
        if(msg == null) {
            connectedLearnersAdapter.updateStatus(peerID, status);
        } else {
            connectedLearnersAdapter.updateStatus(peerID, status, msg);
        }
    }

    /**
     * Update the device message on selected learner devices to reflect a change in the
     * application.
     * @param msg A string stating the message to be sent.
     * @param peerSet A set of peers that will receive the message.
     */
    public void sendDeviceMessage(int msg, Set<String> peerSet) {
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.UPDATE_DEVICE_MESSAGE + ":" + msg, peerSet);
    }

    /**
     * Creates a dialog asking for a peer to allow a certain permission, no dialog is present for disabling
     * the permission. Used for auto installing applications and transferring files between devices.
     * @param permission A string representing what permission wanting to be allowed.
     * @param enable A boolean representing if the permission is being turned on or off.
     */
    public void askForPeerPermission(String permission, Boolean enable) {
        String msg;

        switch(permission) {
            case FILE_TRANSFER:
                if(enable) {
                    msg = "Guide wants to enable \nfile transfer services.";
                } else {
                    fileTransferEnabled = false;
                    return;
                }

                if(fileTransferEnabled) {
                    return;
                }

                break;

            case AUTO_INSTALL:
                if(enable) {
                    msg = "Guide wants to enable \nauto installing of applications.";
                } else {
                    autoInstallApps = false;
                    return;
                }

                if(autoInstallApps) {
                    return;
                }

                break;

            default:
                Log.e(TAG, "askForPeerPermission: Permission is not defined.");
                return;
        }

        dialogManager.showPermissionDialog(msg, permission);
    }

    /**
     * Determines if a permission has been allowed or denied, enables the permission if allowed.
     * @param permission A string representing what permission has been requested.
     * @param allowed A boolean representing if the peer has allowed or denied the request.
     */
    public void permissionAllowed(String permission, Boolean allowed) {
        if(allowed) {
            switch (permission) {
                case FILE_TRANSFER:
                    fileTransferEnabled = true;
                    break;

                case AUTO_INSTALL:
                    autoInstallApps = true;
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
    public void permissionGranted(String permission) {
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.PERMISSION_DENIED + ":" + "OK" + ":" + nearbyManager.getID() + ":" + permission,
                getNearbyManager().getSelectedPeerIDsOrAll());
    }

    /**
     * Sends an action back to the guide if the permission has been denied.
     * @param permission A string representing what permission has been denied.
     */
    public void permissionDenied(String permission) {
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.PERMISSION_DENIED + ":" + "BAD" + ":" + nearbyManager.getID() + ":" + permission,
                getNearbyManager().getSelectedPeerIDsOrAll());
    }

    /**
     * Starts the accessibility functions in the AppInstaller to automatically install an application
     * on a peers device.
     * @param event The event to trigger an auto install.
     */
    public void autoInstall(AccessibilityEvent event) {
        if(managingAutoInstaller) {
            lumiAppInstaller.install(event);
        }
    }

    /**
     * Send an error action to the guide when a transfer has not completed properly.
     * @param error A string representing the type of error that has occurred.
     * @param peerID A string representing which peer device the error occurred on.
     */
    public void transferError(String error, String peerID) {
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.TRANSFER_ERROR + ":" + peerID + ":" + error,
                getNearbyManager().getSelectedPeerIDsOrAll());
    }

    //TODO This isn't used but can be implemented in the future.
    /**
     * Send an action to the guide on the destruction of a learners application. Notifying the guide
     * that the learner has abruptly disconnected.
     * @param peerID A string representing the ID of the disconnected peer.
     */
    public void disconnection(String peerID) {
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,
                LeadMeMain.DISCONNECTION + ":" + peerID,
                getNearbyManager().getSelectedPeerIDsOrAll());
    }

    //COMMON ACTIONS THAT ARE SENT TO LEARNERS
    /**
     * Mute the selected learners.
     */
    public void muteLeaners() {
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_MUTE_TAG,
               getNearbyManager().getSelectedPeerIDsOrAll());
    }

    /**
     * Unmute the selected learners.
     */
    public void unmuteLearners() {
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.VID_UNMUTE_TAG,
                getNearbyManager().getSelectedPeerIDsOrAll());
    }

    /**
     * Returns the user to the LeadMe main screen.
     * */
    public void returnToMain() {
        leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
    }

    /**
     * Sets the leadmeAnimator as the contect view.
     * */
    public void animatorAsContentView() {
        setContentView(leadmeAnimator);
    }

    /**
     * Calls the setIndeterminateBar function from the dialog controller.
     * @param display An int representing the visibility of the bar.
     * */
    public void setIndeterminateBar(int display) {
        dialogManager.setIndeterminateBar(display);
    }

    /**
     * Gets the input method manager assoicated with the LeadMe main activity.
     * @return An instance of InputMethodManager
     */
    public InputMethodManager getInputManager() {
        return this.imm;
    }

    //FUNCTIONS TO CONTROL THE SIGN IN PROCESS
    /**
     *
     * @param email A String representing the email of the user attempting to sign in.
     * @param password A String representing the password of the user attempting to sign in.
     * @param error A TextView that can be populate with any error messages that occur.
     */
    public void firebaseEmailSignIn(String email, String password, TextView error) {
        setProgressSpinner(3000, dialogManager.getIndeterminateBar());
        getAuthenticationManager().FirebaseEmailSignIn(email, password, error);
    }

    /**
     * Build the view for a forgotten password route.
     * @param previous An AlertDialog that represents the current view a user is on.
     */
    public void forgotPasswordController(AlertDialog previous) {
        getAuthenticationManager().showForgottenPassword(previous);
    }

    /**
     * Signing in using a google account. Get the instance of the google sign in client and start
     * the activity for result.
     */
    public void googleSignIn() {
        Intent signInIntent = getAuthenticationManager().getGoogleSignInClient().getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Show the login and sign up pages.
     * @param page An int representing the specific page to load up within the function.
     */
    public void buildLoginSignupController(int page) {
        if(!PermissionManager.isInternetAvailable(getApplicationContext())) {
            dialogManager.showWarningDialog("Currently Offline", "No internet access detected. Please connect to continue.");
            return;
        }

        getAuthenticationManager().buildloginsignup(page);
    }

    /**
     * Sets the name of the current user and calls the loginAction.
     * @param name A string representing the name of the user that is connecting.
     * @param manualLogin A boolean representing if the user is manually finding guides.
     */
    public void setUserName(String name, Boolean manualLogin) {
        getNearbyManager().myName = name;
        getNameViewController().setText(name);
        loginAction(manualLogin);
    }

    /**
     * Adding a leader to the learner display adapter manually through a firebase call.
     * @param username A String representing the name of the peer that is connecting.
     * @param serverIP A String representing the local IP address of the guide.
     * */
    public void manuallyAddLeader(String username, String serverIP) {
        runOnUiThread(() -> { //not sure if needed - check later
            getLeaderSelectAdapter().addLeader(new ConnectedPeer(username, serverIP));
            showLeaderWaitMsg(false);
        });
    }

    /**
     * Connect to a Guide manually by providing a peer name and ipAddress.
     * @param ManName A textView containing the name of the peer that is going to connect.
     * @param IpEnter A textView containing the string of the ipAddress of a Guide's device.
     */
    public void directIpConnection(TextView ManName, TextView IpEnter) {
        getNearbyManager().myName = ManName.getText().toString();
        directConnection = true;
        getFirebaseManager().setServerIP(IpEnter.getText().toString());
        loginAction(true);
    }

    /**
     * Set the value of the selected only boolean.
     * @param selected A boolean representing the new value of selectedOnly
     */
    public void setSelectedOnly(boolean selected) {
        selectedOnly = selected;
    }

    /**
     * Get the value of the selected only boolean.
     */
    public boolean getSelectedOnly() {
        Log.e(TAG, "Selected: " + selectedOnly);
        return selectedOnly;
    }
}
