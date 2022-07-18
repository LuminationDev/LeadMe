package com.lumination.leadme;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

import androidx.collection.ArraySet;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.BoardiesITSolutions.FileDirectoryPicker.OpenFilePicker;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.managers.CuratedContentManager;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.accessibility.LumiAccessibilityConnector;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.managers.AppManager;
import com.lumination.leadme.managers.FirebaseManager;
import com.lumination.leadme.managers.NearbyPeersManager;
import com.lumination.leadme.managers.NetworkManager;
import com.lumination.leadme.managers.PermissionManager;
import com.lumination.leadme.managers.WebManager;
import com.lumination.leadme.players.VREmbedVideoPlayer;
import com.lumination.leadme.services.LumiAccessibilityService;
import com.lumination.leadme.services.NetworkService;
import com.lumination.leadme.utilities.FileUtilities;
import com.lumination.leadme.utilities.OnboardingGestureDetector;

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

    //Singleton pattern
    private static LeadMeMain leadMeInstance;
    public static LeadMeMain getInstance()
    {
        return leadMeInstance;
    }

    public static Handler UIHandler;
    static { UIHandler = new Handler(Looper.getMainLooper()); }

    /**
     * Allows runOnUIThread calls from anywhere in the program.
     */
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    //Turn the updated content on or off - still need to manually switch the layout in c__leader_main.xml
    public static final boolean FLAG_UPDATES = false;
    public static final boolean FLAG_INSTALLER = false;

    public Drawable leadMeIcon;

    public static String leadMeAppName = "";
    public static String leadMePackageName = "";

    public static final int OVERLAY_ON = 0;
    public static final int ACCESSIBILITY_ON = 1;
    public static final int BLUETOOTH_ON = 2;
    public static final int FINE_LOC_ON = 3;
    public static final int RC_SIGN_IN = 4;
    public static final int VR_FILE_CHOICE = 5;
    public static final int TRANSFER_FILE_CHOICE = 6;

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
     * Used to determine if the user has used the VR player before
     */
    public Boolean vrFirstTime = null;

    private InputMethodManager imm;
    private LumiAccessibilityConnector lumiAccessibilityConnector;
    private BroadcastReceiver accessibilityReceiver;

    protected ViewGroup.LayoutParams layoutParams;

    public static WindowManager windowManager;
    public static WindowManager.LayoutParams overlayParams, url_overlayParams;
    public View overlayView, url_overlay;

    //VR Player
    public static boolean defaultVideo = true;

    /**
     * A Uri representing the last source pushed by the leader to a learner,
     * saved in case a learners requests a file transfer.
     */
    public static Uri vrURI;

    /**
     * A String representing the last source pushed by the leader to a learner,
     * saved in case a learners requests a file transfer. Used for devices with MIUI version 9.5
     * and below.
     */
    public static String vrPath;

    //details about me to send to peers
    public static boolean isGuide = false;
    public boolean studentLockOn = true; //students start locked
    public static boolean selectedOnly = false; //sending to all learners or just selected

    public String lastLockState = Controller.LOCK_TAG;
    public String lastAppID;

    public ViewAnimator leadmeAnimator;
    private ViewSwitcher leaderLearnerSwitcher;
    protected boolean loggingInAsLeader = true;

    private final int SWITCH_LEADER_INDEX = 0;
    private final int SWITCH_LEARNER_INDEX = 1;

    private static final int ANIM_SPLASH_INDEX = 0;
    private static final int ANIM_START_SWITCH_INDEX = 1;
    private static final int ANIM_LEARNER_INDEX = 2;
    private static final int ANIM_LEADER_INDEX = 3;
    private static final int ANIM_APP_LAUNCH_INDEX = 4;
    private static final int ANIM_OPTIONS_INDEX = 5;
    private static final int ANIM_XRAY_INDEX = 6;
    protected static final int ANIM_MULTI_INDEX = 7;
    public static final int ANIM_CURATED_CONTENT_LAUNCH_INDEX = 8;
    private static final int ANIM_CURATED_CONTENT_SINGLE_LAUNCH_INDEX = 9;

    public View waitingForLearners, appLauncherScreen;
    public View splashscreen, startLearner, mainLearner, startLeader, mainLeader, optionsScreen, switcherView, xrayScreen;
    public View multiAppManager;
    private TextView learnerWaitingText;
    public Button alertsBtn;
    private Button leader_toggle, learner_toggle;
    private ImageView logo;
    private GridView connectedStudentsView;

    //Checking for updates on the Play Store
    //private final int UPDATE_REQUEST_CODE = 100;

    public Context context;
    public ActivityManager activityManager;
    public static WifiManager wifiManager;

    public static Set<ConnectedPeer> selectedLearners = new ArraySet<>();

    //File transfer
    public static Boolean fileTransferEnabled = false; //hard coded so have to enable each session
    public Switch transferToggle = null;
    public static ArrayList<String> fileRequests = new ArrayList<>(); //array to hold learner ID's that are requesting a file

    //Auto app installer
    public static Boolean autoInstallApps = false; //if true, missing apps on student devices get installed automatically
    public static Boolean managingAutoInstaller = false; //track if installing applications so recall can be skipped
    public static Boolean installingApps = null; //track if installing or uninstalling application
    public Switch autoToggle = null;

    private ImageView currentTaskIcon;
    private TextView currentTaskTitle, currentTaskDescription;
    private Button currentTaskLaunchBtn;
    public static String currentTaskPackageName = "none";
    public static String currentTaskURLTitle, currentTaskName, currentTaskURL, currentTaskType;

    public static Intent appIntentOnFocus = null;
    Toast appToast = null;

    private boolean init = false;

    boolean allowHide = false;
    private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    public ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

    public void stopShakeDetection() {
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    public void startShakeDetection() {
        Log.d(TAG, "Listening for shakes!");
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public static boolean canAskForAccessibility = true;

    public final int SCREEN_CAPTURE = 999;
    private static final int REQUEST_SCREENSHOT_PERMISSION = 1234;

    //TODO MOVE FUNCTIONS TO NEW CONTROLLER CLASS
    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OVERLAY_ON:
                Controller.getInstance().overlyOn(resultCode);
                break;

            case ACCESSIBILITY_ON:
                Controller.getInstance().accessibilityOn(resultCode);
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
                Controller.getInstance().screenCapture(resultCode, data);
                break;

            case RC_SIGN_IN:
                Controller.getInstance().googleSignIn(data);
                break;

            case VR_FILE_CHOICE:
                Controller.getInstance().vrFileChoice(resultCode, data);
                break;

            case TRANSFER_FILE_CHOICE:
                Controller.getInstance().transferFileChoice(resultCode, data);
                break;

            default:
                Log.d(TAG, "RETURNED FROM ?? with " + resultCode);
                break;
        }
        Controller.getInstance().getXrayManager().screenshotManager.onActivityResult(requestCode, resultCode, data);
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
        if (Controller.getInstance().getDispatcher().readAction(payloadBytes)) {
            Log.d(TAG, "Incoming message was an action!");
            return; //it was an action, we're done!
        }

        //if it's an app launch request, deploy it
        if (Controller.getInstance().getDispatcher().openApp(payloadBytes)) {
            Log.d(TAG, "Incoming message was an app launch request!");
            return; //it was an app launch request, we're done!
        }

        Log.e(TAG, "Couldn't find a match for " + Arrays.toString(payloadBytes));
    }

    public LumiAccessibilityConnector getLumiAccessibilityConnector() {
        return lumiAccessibilityConnector;
    }

    private boolean initPermissions = false;

    public void performNextAction() {
        Log.d(TAG, "PerformNextAction? " + initPermissions + ", " + Controller.getInstance().getPermissionsManager().isNearbyPermissionsGranted());

        PermissionManager.waitingForPermission = false; //no longer waiting
        closeKeyboard();

        if (!initPermissions) {
            return; //not ready to proceed
        }

        //deal with location permission related stuff
        if (!Controller.getInstance().getPermissionsManager().isNearbyPermissionsGranted()) {
            Log.d(TAG, "Permission return - request nearby");
            Controller.getInstance().getPermissionsManager().checkNearbyPermissions();
            return;

        }

        if (!Controller.getInstance().getPermissionsManager().isStoragePermissionsGranted()) {
            Log.d(TAG, "Permission return - request storage");
            Controller.getInstance().getPermissionsManager().checkStoragePermission();
            return;
        }

        //can't go any further
        if (!canAskForAccessibility && !Controller.getInstance().getPermissionsManager().isAccessibilityGranted()) {
            Controller.getInstance().getDialogManager().showWarningDialog("Cannot connect to other LeadMe users until Accessibility permission is granted.");
            return;
        }

        if (canAskForAccessibility && Controller.getInstance().getPermissionsManager().isAccessibilityGranted() && !Controller.getInstance().getPermissionsManager().isMyServiceRunning(AccessibilityService.class)) {
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
            Controller.getInstance().getNearbyManager().onBackPressed();
        }
        Log.i(TAG, "On BACK!");
    }

    /**
     * The device has moved. We need to decide if it was intentional or not.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //already connected, or not ready to connect
        if (NearbyPeersManager.isConnectedAsGuide() || NearbyPeersManager.isConnectedAsFollower()) {
            return;
        }
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY && !destroying) {
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

        FirebaseUser user = Controller.getInstance().getAuthenticationManager().getCurrentAuthUser();

        if (user == null) {
            Controller.getInstance().getDialogManager().changeLoginViewOptions(View.VISIBLE, View.GONE, View.GONE);
        } else {
            Controller.getInstance().getDialogManager().changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
            NearbyPeersManager.myName = user.getDisplayName();
            getNameViewController().setText(user.getDisplayName());
        }

        //set appropriate mode
        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
            Log.d(TAG, "showLoginDialog: leader"); //leader
            Controller.getInstance().getDialogManager().changeLeaderLoginViewOptions(View.VISIBLE, View.GONE);

        } else {
            Log.d(TAG, "showLoginDialog: learner"); //learner
            Controller.getInstance().getDialogManager().changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
            Controller.getInstance().getDialogManager().changeLeaderLoginViewOptions(View.GONE, View.VISIBLE);

            if (NearbyPeersManager.selectedLeader == null) {
                leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEADER_INDEX);
                showLoginDialog();
                return;
            }

            Controller.getInstance().getDialogManager().setLeaderName(NearbyPeersManager.selectedLeader.getDisplayName());
        }

        //Check if the leader has internet access
        if(!PermissionManager.isInternetAvailable(getApplicationContext())) {
            Controller.getInstance().getDialogManager().showWarningDialog("Currently Offline", "No internet access detected. Please connect to continue.");
            return;
        }

        initPermissions = false; //reset this to ask once more
        Controller.getInstance().getDialogManager().dialogShowing = true;
        Controller.getInstance().getDialogManager().getLoginDialog().show();

        getNameViewController().requestFocus();
        openKeyboard();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not needed
    }

    private void moveAwayFromSplashScreen() {
        UIHandler.postDelayed(() -> {
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
        Log.d(TAG, "onLifecyclePause: " + overlayInitialised + " " + Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted());
        if (!overlayInitialised && Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
            Log.d(TAG, "onLifecyclePause: 1");
            initialiseOverlayView();
        }

        refreshOverlay();

        if (overlayView == null) {
            return;
        }
        //update status and visibilities for overlay
        if (!NearbyPeersManager.isConnectedAsFollower()) {
            Log.d(TAG, "onLifecyclePause: this");
            overlayView.setVisibility(View.INVISIBLE);

        } else if (studentLockOn) {
            setStudentLock(ConnectedPeer.STATUS_LOCK);

            if (!PermissionManager.waitingForPermission) {
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
            runOnUI(() -> {
                //wait until layout update is actioned before trying to gesture
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

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

            runOnUI(() -> {
                //wait until layout update is actioned before trying to gesture
                while (currentTaskPackageName.equals(AppManager.withinPackage) && overlayView.isLayoutRequested()) {
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

            UIHandler.postAtFrontOfQueue(() -> {
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
                        } while (currentTaskPackageName.equals(AppManager.withinPackage) && overlayView.isLayoutRequested());

                        runOnUiThread(() -> { //must be UI thread
                            boolean success = accessibilityService.dispatchGesture(swipe, gestureResultCallback, UIHandler);
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
            if (Controller.getInstance().getPermissionsManager().isAccessibilityGranted() && !Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                setandDisplayStudentOnBoard(1);
            } else if (Controller.getInstance().getPermissionsManager().isAccessibilityGranted() && Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                if(FirebaseManager.getServerIP().length() > 0){
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

        Log.w(TAG, "LC Resume // " + DispatchManager.hasDelayedLaunchContent());
        appHasFocus = true;
        getLumiAccessibilityConnector().resetState(); //reset

        manageFocus();

        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEARNER_INDEX) {
            if (Controller.getInstance().getPermissionsManager().isNearbyPermissionsGranted() && !NearbyPeersManager.isConnectedAsFollower()) {
                displayLearnerStartToggle();
            }
        }

        if (NearbyPeersManager.isConnectedAsFollower()) {
            //do a delayed check to give Android OS time
            //to catch up from a permission being set
            if (!overlayInitialised) {
                UIHandler.postDelayed(() ->
                        DispatchManager.alertGuidePermissionGranted(Controller.STUDENT_NO_OVERLAY, Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()),
                        1000);
            }

            //check it again
            if (!overlayInitialised && Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                initialiseOverlayView();
            }

            if (overlayView != null && NearbyPeersManager.isConnectedAsFollower() && studentLockOn) {
                setStudentLock(ConnectedPeer.STATUS_LOCK);

            } else if (overlayView != null && NearbyPeersManager.isConnectedAsFollower() && !studentLockOn) {
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

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onLifecycleStop() {
        Log.w(TAG, "LC Stop");
        Log.d(TAG, "LeadMe: " + leadMePackageName + " Current package:" + currentTaskPackageName);
        appHasFocus = false;
        if (!PermissionManager.waitingForPermission
                && currentTaskPackageName != null
                && currentTaskPackageName.equals(leadMePackageName)
                && NearbyPeersManager.isConnectedAsFollower()) {
            if(!managingAutoInstaller) {
                DispatchManager.alertGuideStudentOffTask();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                boolean screenIsOn = pm.isInteractive();
                if (screenIsOn) {
                    recallToLeadMe();
                }
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
        stopShakeDetection();
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
            Log.d(TAG, "Moving away from splash...");
            showSplashScreen();
            moveAwayFromSplashScreen();
        }

        hideSystemUI();

        AlertDialog login = Controller.getInstance().getDialogManager().getLoginDialog();

        if (init && login != null && login.isShowing()) {
            openKeyboard();
        }

        if (appHasFocus && appToast != null) {
            appToast.cancel();
            appToast = null;
        }

        //if there's an app to launch, do it
        if (appHasFocus && NearbyPeersManager.isConnectedAsFollower()) {
            //if we've got no delayed content, we're properly returning to LeadMe
            if (!DispatchManager.hasDelayedLaunchContent()) {
                //Don't disengage the lock if using the VR player
                if(!currentTaskPackageName.equals(VREmbedVideoPlayer.packageName)) {
                    studentLockOn=false;
                }

                //if we're in lock mode and we should be in something other than LeadMe, relaunch it
                if (studentLockOn && currentTaskPackageName != null && !currentTaskPackageName.equals(leadMePackageName)) {
                    Log.e(TAG, "RELAUNCH?? " + currentTaskPackageName);

                    if (currentTaskPackageName.equals(AppManager.withinPackage) || currentTaskPackageName.equals(AppManager.youtubePackage)) {
                        Controller.getInstance().getAppManager().relaunchLast(currentTaskPackageName, currentTaskName, currentTaskType, currentTaskURL, currentTaskURLTitle);
                    } else {
                        if(!currentTaskPackageName.equals(VREmbedVideoPlayer.packageName)) {
                            Controller.getInstance().getAppManager().relaunchLast();
                        }
                    }

                //if we have launched at least one thing previously, we might want to reset the task icon to LeadMe
                } else if (currentTaskPackageName != null) {
                    Log.e(TAG, "IS NOW A GOOD TIME TO UPDATE TO TASK ICON??");
                    DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.LAUNCH_SUCCESS + currentTaskPackageName + ":" + NearbyPeersManager.getID() + ":" + currentTaskPackageName, NearbyPeersManager.getAllPeerIDs());
                }
            }

            Log.d(TAG, "Focus is back! Launching delayed stuff.");

            //sometimes delayed things are stored here
            DispatchManager.launchDelayedApp();

            //sometimes they're here, so check both
            if (appIntentOnFocus != null) {
                Log.d(TAG, "[XX] Launching directly");
                verifyOverlay();
                startActivity(appIntentOnFocus);
                AppManager.lastApp = appIntentOnFocus.getPackage();
                appIntentOnFocus = null;
                DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                        Controller.LAUNCH_SUCCESS + currentTaskName + ":" + NearbyPeersManager.getID() + ":" + AppManager.lastApp, NearbyPeersManager.getAllPeerIDs());
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
        if (Controller.getInstance().getAppManager().getWithinPlayer().controllerWebView != null)
            Controller.getInstance().getAppManager().getWithinPlayer().controllerWebView.destroy();
        Log.w(TAG, "In onDestroy");
        if(isGuide) {
            logoutResetController();
        }
        backgroundExecutor.shutdownNow();
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

        Controller.getInstance().getDialogManager().cleanUpDialogs();

        //clean up nearby connections
        isGuide = false;

        //remove the overlay if necessary
        if (overlayView != null && overlayView.isAttachedToWindow()) {
            windowManager.removeView(overlayView);
            windowManager.removeView(url_overlay);
        }

        //clean up link preview assets
        Controller.getInstance().getWebManager().cleanUp();

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
        if (NearbyPeersManager.isConnectedAsFollower()) {
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


        leadMeInstance = this;
        context = getApplicationContext();
        Log.w(TAG, "On create! " + init);

        //add observer to respond to lifecycle events
        getLifecycle().addObserver(this);

        //TODO needs to be signed into play store - have to add exception handling
//        checkForUpdates();
        checkSharedPreferences();
        inflateViews();
        createManagers();
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

        Controller.getInstance().getXrayManager().screenshotManager = new ScreenshotManagerBuilder(this)
                .withPermissionRequestCode(REQUEST_SCREENSHOT_PERMISSION) //optional, 888 is the default
                .build();

        firstTimeUser();
    }

    /**
     * Check the firebase for the most current version number.
     */
    private void checkAppVersion() {
        FirebaseManager.checkCurrentVersion();
    }

    /**
     * Set up the initial details a user needs that other users might have to interact with.
     */
    private void setupInitialDetails() {
        studentLockOn = false; //students start unlocked
        isGuide = false;
        loggingInAsLeader = true;

        if (Controller.getInstance().getNearbyManager() != null) {
            NearbyPeersManager.setID(null);
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
        if (sharedPreferences.contains(Controller.SESSION_UUID_TAG)) {
            sessionUUID = sharedPreferences.getString(Controller.SESSION_UUID_TAG, null);
        }

        //if none exists, make one and store it
        if (sessionUUID == null) {
            sessionUUID = UUID.randomUUID().toString();
            editor.putString(Controller.SESSION_UUID_TAG, sessionUUID);
            editor.apply();
        }

        //Check if the vr player has been used before
        if(sharedPreferences.contains(Controller.SESSION_VR_TAG)) {
            vrFirstTime = sharedPreferences.getBoolean(Controller.SESSION_VR_TAG, true);
        }

        if (vrFirstTime == null) {
            vrFirstTime = true;
            editor.putBoolean(Controller.SESSION_VR_TAG, vrFirstTime);
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
        editor.putBoolean(Controller.SESSION_VR_TAG, change);
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
        if (sharedPreferences.contains(Controller.AUTO_INSTALL)) {
            autoInstallApps = sharedPreferences.getBoolean(Controller.AUTO_INSTALL, false);
        }

        if (autoInstallApps == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            autoInstallApps = false;
            editor.putBoolean(Controller.AUTO_INSTALL, autoInstallApps);
            editor.apply();
        }

        //file transfer sharedpreferences
        if (sharedPreferences.contains(Controller.FILE_TRANSFER)) {
            fileTransferEnabled = sharedPreferences.getBoolean(Controller.FILE_TRANSFER, false);
        }

        if (fileTransferEnabled == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            fileTransferEnabled = false;
            editor.putBoolean(Controller.AUTO_INSTALL, fileTransferEnabled);
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
    }

    /**
     * Create the necessary system services and adapters for the application.
     */
    private void createManagers() {
        //System services
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE); //public ipAddress used in manual connection modes
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE); //for hiding keyboard using soft input
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);


        new Controller();
        lumiAccessibilityConnector = new LumiAccessibilityConnector(this);
        connectedStudentsView = mainLeader.findViewById(R.id.studentListView);
        connectedStudentsView.setAdapter(Controller.getInstance().getConnectedLearnersAdapter());
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
            Log.d(TAG, "DECOR VIEW! " + NearbyPeersManager.isConnectedAsFollower() + ", " + Controller.getInstance().getDialogManager().dialogShowing);
            if (NearbyPeersManager.isConnectedAsFollower() || OnBoardStudentInProgress) {
                if (allowHide) {
                    UIHandler.postDelayed(this::hideSystemUIStudent, 0);
                }
            }
        });

        try {
            leadMeIcon = getPackageManager().getApplicationIcon(getPackageName());
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
        alertsBtn.setOnClickListener(view -> Controller.getInstance().getDialogManager().showAlertsDialog());

        mainLeader.findViewById(R.id.select_bar_back).setOnClickListener(view -> Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(false));

        CheckBox checkBox = mainLeader.findViewById(R.id.select_bar_selectall);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(isChecked));

        mainLeader.findViewById(R.id.select_bar_repush).setOnClickListener(v -> {
            DispatchManager.repushApp(NearbyPeersManager.getSelectedPeerIDsOrAll());
            Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(false);
        });
    }

    /**
     * Set up the adapters for the lists and grids associated with the application launch functions
     * and learner list view.
     */
    private void listGridAdapters() {
        GridView appGrid = appLauncherScreen.findViewById(R.id.app_list_grid);
        appGrid.setAdapter(Controller.getInstance().getAppManager());
        layoutParams = appGrid.getLayoutParams();
        layoutParams.height = appGrid.getMeasuredHeight(); //this is in pixels
        appGrid.setLayoutParams(layoutParams);
        ((GridView) appLauncherScreen.findViewById(R.id.fav_list_grid)).setAdapter(Controller.getInstance().getAppManager().getFavouritesManager());
        (appLauncherScreen.findViewById(R.id.current_task_layout)).setVisibility(View.GONE);
        (appLauncherScreen.findViewById(R.id.text_current_task)).setVisibility(View.GONE);
        ((ListView) startLearner.findViewById(R.id.leader_list_view)).setAdapter(Controller.getInstance().getLeaderSelectAdapter());

        (appLauncherScreen.findViewById(R.id.repush_btn)).setOnClickListener(v -> {
            Log.d(TAG, "Repushing " + lastAppID);
            //VR player needs to select the source before reopening, handle just like fresh start.
            if(lastAppID.equals(VREmbedVideoPlayer.packageName)) {
                //Opens up the preview player again
                Controller.getInstance().getVrEmbedVideoPlayer().showPlaybackPreview();
            } else {
                if (lastLockState != null && lastLockState.equals(Controller.LOCK_TAG)) {
                    DispatchManager.requestRemoteAppOpen(Controller.APP_TAG, lastAppID,
                            String.valueOf(((TextView) appLauncherScreen.findViewById(R.id.current_app_name)).getText()),
                            Controller.LOCK_TAG, "false",
                            NearbyPeersManager.getSelectedPeerIDsOrAll());
                } else {
                    DispatchManager.requestRemoteAppOpen(Controller.APP_TAG, lastAppID,
                            String.valueOf(((TextView) appLauncherScreen.findViewById(R.id.current_app_name)).getText()),
                            Controller.UNLOCK_TAG, "false",
                            NearbyPeersManager.getSelectedPeerIDsOrAll());
                }

                Controller.getInstance().getDialogManager().showConfirmPushDialog(true, false);
            }
        });
    }

    /**
     * Set up the main action buttons used by the guide to control the connected peers.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupActionButtons() {
        View.OnTouchListener touchListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_square, null));
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_square_pressed, null));
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL){
                v.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_square, null));
            }
            return false;
        };

        mainLeader.findViewById(R.id.app_core_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.app_core_btn).setOnClickListener(view -> {
            showAppLaunchScreen();
            appLauncherScreen.findViewById(R.id.app_scroll_view).scrollTo(0, 0);
        });

        mainLeader.findViewById(R.id.curated_content_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.curated_content_btn).setOnClickListener(view -> {
            CuratedContentManager.setupCuratedContent(this);
            showCuratedContentScreen();
            appLauncherScreen.findViewById(R.id.app_scroll_view).scrollTo(0, 0);
        });

        mainLeader.findViewById(R.id.url_core_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.url_core_btn).setOnClickListener(view -> Controller.getInstance().getWebManager().showWebLaunchDialog(false, false));

        mainLeader.findViewById(R.id.within_core_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.within_core_btn).setOnClickListener(view -> {
            Controller.getInstance().getAppManager().getWithinPlayer().showWithin(); //launch within search
        });

        mainLeader.findViewById(R.id.xray_core_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.xray_core_btn).setOnClickListener(v -> {
            if (Controller.getInstance().getConnectedLearnersAdapter().getCount() > 0) {
                View confirmationView = View.inflate(LeadMeMain.getInstance(), R.layout.e__xray_experimental_confirmation, null);
                AlertDialog confirmationDialog = new AlertDialog.Builder(LeadMeMain.getInstance())
                        .setView(confirmationView)
                        .show();
                Button okButton = confirmationView.findViewById(R.id.ok_btn);
                Button backButton = confirmationView.findViewById(R.id.back_btn);
                okButton.setOnClickListener(w -> {
                    Controller.getInstance().getXrayManager().showXrayView("");
                    confirmationDialog.dismiss();
                });
                backButton.setOnClickListener(w -> {
                    confirmationDialog.dismiss();
                });
            } else {
                Toast.makeText(getApplicationContext(), "No students connected.", Toast.LENGTH_SHORT).show();
            }
        });

        checkAddtionalPreferences();

        //file transfer button
        mainLeader.findViewById(R.id.file_core_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.file_core_btn).setOnClickListener(view -> {
            if (!ConnectedLearnersAdapter.someoneIsSelected()) {
                Toast.makeText(context, "Peers need to be selected.", Toast.LENGTH_LONG).show();
                return;
            }

            if (fileTransferEnabled) {
                View confirmationView = View.inflate(LeadMeMain.getInstance(), R.layout.e__file_transfer_experimental_confirmation, null);
                AlertDialog confirmationDialog = new AlertDialog.Builder(LeadMeMain.getInstance())
                        .setView(confirmationView)
                        .show();
                Button okButton = confirmationView.findViewById(R.id.ok_btn);
                Button backButton = confirmationView.findViewById(R.id.back_btn);
                okButton.setOnClickListener(w -> {
                    if (Controller.isMiUiV9()) {
                        alternateFileChoice(TRANSFER_FILE_CHOICE);
                    } else {
                        FileUtilities.browseFiles(this, TRANSFER_FILE_CHOICE);
                    }
                    confirmationDialog.dismiss();
                });
                backButton.setOnClickListener(w -> {
                    confirmationDialog.dismiss();
                });

            } else {
                Controller.getInstance().getDialogManager().showWarningDialog("File Transfer", "File transfer has not been enabled.");
            }
        });

        //Custom VR button
        mainLeader.findViewById(R.id.vr_core_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.vr_core_btn).setOnClickListener(view -> {
            Controller.getInstance().getDialogManager().showVRContentDialog();

            if (vrFirstTime) {
                vrFirstTime = false;
                changeVRFirstTime(vrFirstTime);

                //Function to let leaders know what files can be picked
                Controller.getInstance().getDialogManager().showVRFirstTimeDialog();
            }
        });

        //End session button
        mainLeader.findViewById(R.id.end_core_btn).setOnTouchListener(touchListener);
        mainLeader.findViewById(R.id.end_core_btn).setOnClickListener(view -> {
            if (isGuide || !NearbyPeersManager.isConnectedAsFollower()) {
                logoutResetController();
            } else {
                Toast.makeText(context, "Logout is unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        //multi install button
        if(LeadMeMain.FLAG_INSTALLER) {
            LinearLayout installer = mainLeader.findViewById(R.id.installer_core_btn);
            installer.setOnTouchListener(touchListener);
            installer.setVisibility(View.VISIBLE);
            installer.setOnClickListener(view -> {
                if (autoInstallApps) {
                    Controller.getInstance().getLumiAppInstaller().showMultiInstaller(layoutParams);
                } else {
                    Controller.getInstance().getDialogManager().showWarningDialog("Auto Installer", "Auto installing has not been enabled.");
                }
            });
        }

        if(LeadMeMain.FLAG_UPDATES) {

            //Lock/Unlock button
            mainLeader.findViewById(R.id.lock_core_btn).setOnTouchListener(touchListener);

            //TODO something really wrong with the lock/unlock functionality
            mainLeader.findViewById(R.id.lock_core_btn).setOnClickListener(view -> {
                Log.e(TAG, "LOCK");
            });
        } else {
            mainLeader.findViewById(R.id.lock_core_btn).setVisibility(View.GONE);
        }
    }

    public void addSelected(ConnectedPeer peer) {
        selectedLearners.add(peer);
        checkLockStatus();
    }

    public void removeSelected(ConnectedPeer peer) {
        selectedLearners.remove(peer);
        checkLockStatus();
    }

    public void checkLockStatus() {
        TextView lockTitle = mainLeader.findViewById(R.id.lock_core_text);
        for (ConnectedPeer peer : selectedLearners) {
            if(!peer.locked) {
                lockTitle.setText("Lock");
                return;
            }
        }
        runOnUiThread(() -> lockTitle.setText("Unlock"));
    }

    /**
     * Begin the alternate file choice for old Xiaomi phones.
     * @param ActivityType An integer representing how the result should be handled.
     */
    public void alternateFileChoice(int ActivityType) {
        Intent intent = new Intent(this, OpenFilePicker.class);
        startActivityForResult(intent, ActivityType);
    }

//    /**
//     * Detect what version of MIUI is running on a Xiaomi device. As version below 10 carry an inherent
//     * OS processing issue relating to launching file picker intents.
//     * @return A boolean representing if the MIUI version is 9.5 or below.
//     */
//    public static boolean isMiUiV9() {
//        String version = getSystemProperty();
//
//        //If empty then it is a different type of phone
//        if(TextUtils.isEmpty(version)) {
//            return false;
//        }
//
//        //Version number comes through as integers or doubles depending on the version
//        //ranging from 8, 9.5, 11.5, 12 etc..
//        //All Xiaomi devices above SE8, i.e Note 8/9 can update to MIUI 12. So only MIUI versions
//        //at or below 10 need to use the alternate file picker.
//        String[] num = new String[0];
//        if (version != null) {
//            num = version.split("V");
//        }
//        int versionNum = Integer.parseInt(num[1]);
//
//        return versionNum <= 10;
//    }
//
//    private static String getSystemProperty() {
//        String propName = "ro.miui.ui.version.name";
//        String line;
//        BufferedReader input = null;
//        try {
//            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
//            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
//            line = input.readLine();
//            input.close();
//        } catch (IOException ex) {
//            return null;
//        } finally {
//            if (input != null) {
//                try {
//                    input.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        Log.d(TAG, "MIUI version: " + line);
//        return line;
//    }

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
            if (NearbyPeersManager.isConnectedAsFollower() || NearbyPeersManager.isConnectedAsGuide()) {
                optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.GONE);
                optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.GONE);
            } else {

                if (Controller.getInstance().getAuthenticationManager().getCurrentAuthUser() != null) {
                    optionsScreen.findViewById(R.id.options_leader).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.GONE);
                    optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.GONE);
                    ((TextView) optionsScreen.findViewById(R.id.options_signed_name)).setText(
                            Controller.getInstance().getAuthenticationManager().getCurrentAuthUserName());
                } else {
                    optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_leader).setVisibility(View.GONE);
                }
            }
            if (NearbyPeersManager.isConnectedAsGuide()) {
                optionsScreen.findViewById(R.id.options_leader).setVisibility(View.VISIBLE);
                optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.VISIBLE);
                ((TextView) optionsScreen.findViewById(R.id.options_signed_name)).setText(
                        Controller.getInstance().getAuthenticationManager().getCurrentAuthUserName());
            } else if (NearbyPeersManager.isConnectedAsFollower()) {
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
            Controller.getInstance().getLumiAppInstaller().multiInstalling = false;
            //reset any selected apps
            Controller.getInstance().getLumiAppInstaller().resetAppSelection();
        });

        //set up options screen
        optionsScreen.findViewById(R.id.back_btn).setOnClickListener(v -> {
            Log.e(TAG, lastDisplayedIndex + " // " + NearbyPeersManager.isConnectedAsFollower() + " // " + NearbyPeersManager.isConnectedAsGuide() + " // " + isGuide);
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
                Controller.getInstance().getDialogManager().showRecallDialog();
            } else {
                //test my connection
                if (!NearbyPeersManager.isConnectedAsFollower() && !NearbyPeersManager.isConnectedAsGuide()) {
                    Log.e(TAG, "No longer connected!");
                    logoutResetController();
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
                Controller.getInstance().getDialogManager().showWarningDialog("Currently Offline", "No internet access detected. Please connect to continue.");
                return;
            }
            Controller.getInstance().getAuthenticationManager().buildloginsignup(1);
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
            editor.putBoolean(Controller.AUTO_INSTALL, autoInstallApps);
            editor.apply();

            //send action to student devices to change the auto install setting
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.AUTO_INSTALL + ":"
                    + autoInstallApps, NearbyPeersManager.getSelectedPeerIDsOrAll());
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
            editor.putBoolean(Controller.FILE_TRANSFER, fileTransferEnabled);
            editor.apply();

            //send action to student devices to change the file transfer settings?
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.FILE_TRANSFER + ":"
                    + fileTransferEnabled, NearbyPeersManager.getSelectedPeerIDsOrAll());
        });

        //direct ip input connection
        TextView manualConnect = optionsScreen.findViewById(R.id.manual_connect);
        manualConnect.setOnClickListener(view -> {
            Controller.getInstance().getDialogManager().showManualDialog(isGuide, FirebaseManager.getLocalIpAddress());
        });

        optionsScreen.findViewById(R.id.logout_btn).setOnClickListener(view -> {
            if (isGuide || !NearbyPeersManager.isConnectedAsFollower()) {
                Controller.getInstance().getAuthenticationManager().logoutAction();
                optionsScreen.findViewById(R.id.options_leader).setVisibility(View.GONE);
                logoutResetController();
            } else {
                Toast.makeText(context, "Logout is unavailable.", Toast.LENGTH_SHORT).show();
            }

        });

        optionsScreen.findViewById(R.id.options_endSess).setOnClickListener(view -> {
            if (isGuide || !NearbyPeersManager.isConnectedAsFollower()) {
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
            displayLeaderStartToggle();
            if (Controller.getInstance().getAuthenticationManager().getCurrentAuthUser() == null) {
                Controller.getInstance().getDialogManager().changeLoginViewOptions(View.VISIBLE, View.GONE, View.GONE);
            }
        });

        if (Controller.getInstance().getAuthenticationManager().getCurrentAuthUser() == null) {
            Controller.getInstance().getDialogManager().changeLoginViewOptions(View.VISIBLE, View.GONE, View.GONE);
        } else {
            Controller.getInstance().getDialogManager().changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
        }

        learner_toggle.setOnClickListener(v -> {
            displayLearnerStartToggle();
            if (Controller.getInstance().getAuthenticationManager().getCurrentAuthUser() == null) {
                Controller.getInstance().getDialogManager().changeLoginViewOptions(View.GONE, View.GONE, View.VISIBLE);
            }
        });

        //prepare elements for login dialog
        getNameViewController();

        startLeader.findViewById(R.id.app_login).setOnClickListener(v -> showLoginDialog());

        //prepare elements for leader main view
        waitingForLearners = mainLeader.findViewById(R.id.no_students_connected);

        setUpControlButtons();

        initPermissions = true;
        Controller.getInstance().getPermissionsManager().checkMiscPermissions();

        if (!Controller.getInstance().getPermissionsManager().isNearbyPermissionsGranted()) {
            Controller.getInstance().getPermissionsManager().checkNearbyPermissions();
        }

        if (!Controller.getInstance().getPermissionsManager().isStoragePermissionsGranted()) {
            Controller.getInstance().getPermissionsManager().checkStoragePermission();
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

            Controller.getInstance().getDialogManager().displayGuidePrompt();
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
        if (overlayInitialised) {
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
        Controller.getInstance().getDispatcher().disableInteraction(ConnectedPeer.STATUS_UNLOCK);
    }

    /**
     * Get the current instance of the nameView textView within the dialog manager.
     * @return An instance of the name view text view from the dialog manager.
     */
    public TextView getNameViewController() {
        return Controller.getInstance().getDialogManager().getNameView();
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
    }

    private void displayLearnerStartToggle() {
        loggingInAsLeader = false;
        if (!Controller.getInstance().getPermissionsManager().isNearbyPermissionsGranted()) {
            learnerWaitingText.setText(getResources().getString(R.string.enable_location_to_connect));
            Controller.getInstance().getPermissionsManager().checkNearbyPermissions();
        } else if (!Controller.getInstance().getPermissionsManager().isStoragePermissionsGranted()) {
            learnerWaitingText.setText(getResources().getString(R.string.enable_storage));
            Controller.getInstance().getPermissionsManager().checkStoragePermission();
        } else if (!NearbyPeersManager.isConnectedAsFollower()) {
            initiateManualLeaderDiscovery();

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
        Controller.getInstance().getDialogManager().closeWaitingDialog(success);

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
            if (Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                initialiseOverlayView();
            }
        }
    }

    //MANUAL CONNECTION FOR LEARNERS
    /**
     * Empty the current leader list and stop any Nsd discovery. Start the client socket listener
     * and retrieve any a snapshot from Firebase with any leaders that are currently active.
     */
    public void initiateManualLeaderDiscovery() {
        Log.d(TAG, "Initiating Manual Leader Discovery");
        Controller.getInstance().getLeaderSelectAdapter().setLeaderList(new ArrayList<>());
        FirebaseManager.retrieveLeaders();
    }

    public boolean checkLoginDetails() {
        AtomicBoolean codeEntered = new AtomicBoolean(false);

        //reset error messages
        Controller.getInstance().getDialogManager().changeLoginViewOptions(-1, View.GONE, View.GONE);

        //check that a name has been entered
        boolean nameEntered;
        if (getNameViewController().getText().toString().trim().length() == 0) { //no name entered
            nameEntered = false;

            Controller.getInstance().getDialogManager().changeLoginViewOptions(-1, -1, View.VISIBLE);
        } else { //name entered
            nameEntered = true;
            String name = NearbyPeersManager.getName();
            Log.d(TAG, "Your name is now " + name);
        }

        //if appropriate, check if the correct code has been entered
        if (loggingInAsLeader) {
            //check leader code
            String code = Controller.getInstance().getDialogManager().getAndClearPinEntry();

            Log.d(TAG, "Code entered: " + code);
            // For showing
            Controller.getInstance().getDialogManager().setIndeterminateBar(View.VISIBLE);

            // For hiding
            Task<com.google.firebase.firestore.DocumentSnapshot> firebaseAccount = Controller.getInstance().getAuthenticationManager().getFirebaseAccount();

            firebaseAccount.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (Hasher.Companion.hash(code, HashType.SHA_256).equals(task.getResult().getString("pin"))) {
                        codeEntered.set(true);

                        Controller.getInstance().getDialogManager().setIndeterminateBar(View.GONE);
                        loginAction(false);
                    } else {
                        codeEntered.set(false);
                        Controller.getInstance().getDialogManager().showLoginAlertMessage();
                        Controller.getInstance().getDialogManager().changeLoginViewOptions(-1, View.VISIBLE, -1);
                    }

                }
            });
        } else {
            codeEntered.set(true); //mark as true, since we don't need one
            if (!nameEntered) {
                //alert to errors and exit
                Controller.getInstance().getDialogManager().showLoginAlertMessage();
                return false; //failed
            } else {
                return true; //succeeded
            }
        }
        return false;
    }

    public void initiateLeaderAdvertising() {
        if (loggingInAsLeader) {
            Controller.getInstance().getNearbyManager().setAsGuide();
        }
    }

    /**
     * Reset the application on logout so restart has a fresh slate to being with.
     */
    private void logoutResetController() {
        Log.d(TAG, "Resetting controller");
        Controller.getInstance().getXrayManager().resetClientMaps(null);
        //I dont like this but sometimes sending it once doesn't work....
        Controller.getInstance().getDispatcher().alertLogout(); //need to send this before resetting 'isGuide'
        Controller.getInstance().getDispatcher().alertLogout(); //need to send this before resetting 'isGuide'

        //Purposely block to make sure all students receive the disconnect command
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FirebaseManager.handleDisconnect();
        NetworkService.resetClientIDs();
        Controller.getInstance().getConnectedLearnersAdapter().resetOnLogout();
        Controller.getInstance().getLeaderSelectAdapter().setLeaderList(new ArrayList<>()); //empty the list
        setUIDisconnected();
        Controller.getInstance().getFileTransferManager().stopService();
        NetworkManager.stopService();
        showSplashScreen();
        moveAwayFromSplashScreen();
        isGuide = false;
    }

    public void loginAction(boolean isManual) {
        Log.w(TAG, "LOGGING IN " + NearbyPeersManager.getName());

        //if all permissions are already granted, just continue
        if(isManual){
            leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEARNER_INDEX);
        }
        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
            initiateLeaderAdvertising();

        } else {
            //getPermissionsManager().checkOverlayPermissions(); //TODO Experimental - add flag

            if (!Controller.getInstance().getPermissionsManager().isAccessibilityGranted()) {
                setandDisplayStudentOnBoard(0);
            } else if (Controller.getInstance().getPermissionsManager().isAccessibilityGranted() && !Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                setandDisplayStudentOnBoard(1);
            } else if (Controller.getInstance().getPermissionsManager().isAccessibilityGranted() && Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                if(!isManual) {
                    setandDisplayStudentOnBoard(2);
                }else{
                    setandDisplayStudentOnBoard(3);
                }
            }
        }

        Controller.getInstance().getDialogManager().hideLoginDialog(false);

        String name = NearbyPeersManager.getName();
        if(isManual) {
            isGuide=false;
        }
        Log.d(TAG, "Your name is " + name + ", are you a guide? " + isGuide);

        leaderAdditionalOptions(isGuide ? View.VISIBLE : View.GONE);

        Controller.getInstance().getNetworkManager().startService();

        if (isGuide) {
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

            FirebaseManager.connectAsLeader(name);

            CuratedContentManager.hasDoneSetup = false;
            CuratedContentManager.getCuratedContent(this);
        } else {
            //display main student view
            leadmeAnimator.setDisplayedChild(ANIM_LEARNER_INDEX);
            allowHide = true;
            UIHandler.postDelayed(this::hideSystemUIStudent, 1000);

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
     * @param enabled An int representing if the views are to be enabled or disabled.
     */
    public void leaderAdditionalOptions(int enabled) {
        optionsScreen.findViewById(R.id.on_boarding).setVisibility(enabled);
        optionsScreen.findViewById(R.id.how_to_use_btn).setVisibility(enabled);
        optionsScreen.findViewById(R.id.help_support_btn).setVisibility(enabled);
        transferToggle.setVisibility(enabled);

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
        if (overlayView == null && NearbyPeersManager.isConnectedAsFollower() && NearbyPeersManager.getID() != null) {
            Toast.makeText(this, "Overlay is not working. Please turn on correct permissions.", Toast.LENGTH_SHORT).show();
            DispatchManager.alertGuidePermissionGranted(Controller.STUDENT_NO_OVERLAY, false);
            Controller.getInstance().getPermissionsManager().checkOverlayPermissions();
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
        if (prevStatus != status && NearbyPeersManager.isConnectedAsFollower()) {
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
            runOnUI(() -> {
                Toast studentStatus = Toast.makeText(context, finalStatusMsg, Toast.LENGTH_SHORT);
                studentStatus.show();
            });
        }

        runOnUI(() -> {
            if (!verifyOverlay()) {
                return;
            }

            Log.w(TAG, "In leadmemain: " + leadmeAnimator.isShown() + ", " + PermissionManager.waitingForPermission + ", " + status);

            if (!leadmeAnimator.isShown()) {
                switch (status) {
                    case ConnectedPeer.STATUS_LOCK:
                    case ConnectedPeer.STATUS_BLACKOUT:
                        if (!PermissionManager.waitingForPermission) {
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
        if (!currentTaskPackageName.equals(leadMePackageName)) {
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
                        Controller.getInstance().getAppManager().relaunchLast(currentTaskPackageName, currentTaskName, currentTaskType, currentTaskURL, currentTaskURLTitle);

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

            if (!currentTaskPackageName.equals(leadMePackageName)) {
                currentTaskLaunchBtn.setVisibility(View.VISIBLE);
            } else {
                currentTaskLaunchBtn.setVisibility(View.INVISIBLE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        Controller.getInstance().getWebManager().reset();
        getLumiAccessibilityConnector().resetState();

        if (appHasFocus && hasWindowFocus()) {
            Log.d(TAG, "Already in LeadMe! " + appHasFocus + ", " + hasWindowFocus());
            return;
        }

        if (getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            finish();
            return;
        }

        Log.d(TAG, "Recalling to LeadMe! " + Controller.getInstance().getDialogManager().dialogShowing + ", " + appHasFocus + ", " + hasWindowFocus() + ", " + getLifecycle().getCurrentState());
        closeKeyboard();
        PermissionManager.needsRecall = false;
        getLumiAccessibilityConnector().bringMainToFront(); //call each other until it works
        getLumiAccessibilityConnector().withinManager.cleanUpVideo(); //end video

        Intent intent = new Intent(this, LeadMeMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        AppManager.lastApp = intent.getPackage();
        activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);

        if (appToast == null) {
            appToast = Toast.makeText(context, "Returning to " + getResources().getString(R.string.app_title), Toast.LENGTH_SHORT);
            appToast.show();
        }
    }

    public void lockFromMainAction() {
        Set<String> chosen;
        if (ConnectedLearnersAdapter.someoneIsSelected()) {
            chosen = NearbyPeersManager.getSelectedPeerIDs();
        } else {
            chosen = NearbyPeersManager.getAllPeerIDs();
        }
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.LOCK_TAG, chosen);
    }

    public void unlockFromMainAction() {
        Set<String> chosen;
        if (ConnectedLearnersAdapter.someoneIsSelected()) {
            chosen = NearbyPeersManager.getSelectedPeerIDs();
        } else {
            chosen = NearbyPeersManager.getAllPeerIDs();
        }
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.UNLOCK_TAG, chosen);
    }

    public void blackoutFromMainAction() {
        Set<String> chosen;
        if (ConnectedLearnersAdapter.someoneIsSelected()) {
            chosen = NearbyPeersManager.getSelectedPeerIDs();
        } else {
            chosen = NearbyPeersManager.getAllPeerIDs();
        }
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.BLACKOUT_TAG, chosen);
    }

    //main function, can return everyone or only selected learners
    public void returnToAppFromMainAction(boolean returnSelected) {
        Log.d(TAG, "Returning to app from MAIN! " + appHasFocus + ", " + hasWindowFocus());

        Set<String> chosenPeers;
        if (returnSelected && ConnectedLearnersAdapter.someoneIsSelected()) {
            chosenPeers = NearbyPeersManager.getSelectedPeerIDs();
        } else {
            chosenPeers = NearbyPeersManager.getAllPeerIDs();
        }
        Log.i(TAG, "Returning " + chosenPeers.size() + " learners to LeadMe (" + chosenPeers.toString() + ") vs me: " + NearbyPeersManager.getID());
        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.RETURN_TAG, chosenPeers);

        if (appToast == null) {
            appToast = Toast.makeText(context, "Returning selected followers to Lumination Lead Me app", Toast.LENGTH_SHORT);
            appToast.show();
        }
    }

    //UPDATE OPTIONS PAGE
    public void setUIDisconnected() {
        //reset state
        getLumiAccessibilityConnector().resetState();

        Controller.getInstance().getDialogManager().cleanUpDialogs();

        //reset views
        showConnectedStudents(false);

        if (overlayView != null) {
            overlayView.setVisibility(View.INVISIBLE);
        }

        Controller.getInstance().getDialogManager().readyBtn.setEnabled(true);
        Controller.getInstance().getDialogManager().readyBtn.setText(R.string.connect_label);
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

        initiateManualLeaderDiscovery();
    }

    public void collapseStatus() {
        if (/*!getNearbyManager().isConnectedAsGuide() &&*/ !NearbyPeersManager.isConnectedAsFollower()) {
            //only enforce this for connected students
            return;
        }
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(closeDialog);
        UIHandler.postDelayed(() -> sendBroadcast(closeDialog), 3000);

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
            DispatchManager.sendActionToSelected(Controller.ACTION_TAG,
                    Controller.TRANSFER_ERROR
                            + ":" + NearbyPeersManager.myID
                            + ":File transfer not enabled",
                    NearbyPeersManager.getSelectedPeerIDsOrAll());

            DispatchManager.permissionDenied(Controller.FILE_TRANSFER);
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
        int offTask = Controller.getInstance().getConnectedLearnersAdapter().alertsAdapter.getCount();
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

        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(isChecked);

        CheckBox checkBox = mainLeader.findViewById(R.id.select_bar_selectall);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(numSelected == Controller.getInstance().getConnectedLearnersAdapter().getCount());
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
        video.setOnPreparedListener(mp -> UIHandler.postDelayed(() -> video.setBackgroundColor(Color.TRANSPARENT), 150));


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
        final String intentPackageName = externalLauncher();
        if(intentPackageName != null) {
            watchVideo.setOnClickListener(view -> {
                String cleanURL = WebManager.cleanYouTubeURL("https://www.youtube.com/watch?v=m96imGHXGGM");

                Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanURL));
                appIntent.setPackage(intentPackageName);
                startActivity(appIntent);
            });
        }
        else {
            watchVideo.setText(R.string.install_youtube);
            watchVideo.setAlpha(.5f);
        }

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
                UIHandler.postDelayed(() -> setContentView(leadmeAnimator), 50);
                video.suspend();
                OnBoard = null;
            });
            OnBoard.findViewById(R.id.onboard_moreinfo_btn).setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1GGU7GeR4Ibq60-6bcc2F_bd698CKRFvZ/view"));
                startActivity(browserIntent);
            });
        }
    }

    /**
     * Work out which application to launch the introduction video with.
     * Youtube being the primary source, then chrome and lastly samsung browser.
     * @return A string representing the package name of the launcher application.
     */
    private String externalLauncher() {
        if(isPackageInstalled(AppManager.youtubePackage)) {
            return AppManager.youtubePackage;
        }
        if(isPackageInstalled("com.android.chrome")) {
            return "com.android.chrome";
        }
        if(isPackageInstalled("com.sec.android.app.sbrowser")) {
            return "com.sec.android.app.sbrowser";
        }

        return null;
    }

    /**
     * Check if a particular package is installed on a device.
     * @param packageName A string representing the package name. i.e. com.lumination.leadme
     * @return A boolean representing if the application is install or not.
     */
    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
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
            okPermission.setOnClickListener(v -> Controller.getInstance().getPermissionsManager().requestAccessibilitySettingsOn());

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
                    if (Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                        runOnUiThread(() -> {
                            if(FirebaseManager.getServerIP().length()>0){
                                setandDisplayStudentOnBoard(3);
                            }else{
                                setandDisplayStudentOnBoard(2);
                            }
                        });
                        scheduledCheck.cancel(true);
                    }
                },100,200,TimeUnit.MILLISECONDS);
                if (Controller.getInstance().getPermissionsManager().isOverlayPermissionGranted()) {
                    setandDisplayStudentOnBoard(2);
                }
                Controller.getInstance().getPermissionsManager().checkOverlayPermissions();
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

            //Check if LeadMe has focus, only try to connect if in the application
            scheduledCheck = scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (appHasFocus) {
                    runOnUiThread(() -> connectOnReturn());
                }
            },750,1000,TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Called when overlay permission has been granted. Cancel any scheduled check and connect
     * to a guide, starting the screen sharing service in the background.
     */
    public void connectOnReturn() {
        if(scheduledCheck != null){
            scheduledCheck.cancel(true);
        }

        Controller.getInstance().getScreenSharingManager().startService(false);

        //If the serverIP address has not changed set it to the locally found guide
        if(FirebaseManager.getServerIP().equals("")) {
            FirebaseManager.setServerIP(NearbyPeersManager.selectedLeader.getID());
        }

        NearbyPeersManager.connectToManualLeader(NearbyPeersManager.selectedLeader.getDisplayName(),
                FirebaseManager.getServerIP());

        toggleConnectionOptions(View.GONE); //Remove connection options

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
        AlertDialog login = Controller.getInstance().getDialogManager().getLoginDialog();

        if(login !=null && login.isShowing()) {
            login.dismiss();
        }

        View resetPinView = View.inflate(this, R.layout.c__forgot_pin, null);
        if(savedViewIndex == -1){
            savedViewIndex = leadmeAnimator.getDisplayedChild();
        }

        leadmeAnimator.addView(resetPinView);
        leadmeAnimator.setDisplayedChild(leadmeAnimator.getChildCount()-1);
        Button confirm = resetPinView.findViewById(R.id.pin_reset_confirm);
        Button cancel = resetPinView.findViewById(R.id.pin_reset_cancel);
        View[] pages = {resetPinView.findViewById(R.id.pin_reset_pass_view),resetPinView.findViewById(R.id.set_pin),resetPinView.findViewById(R.id.pin_reset_finish_view)};
        ProgressBar pBar = resetPinView.findViewById(R.id.pin_reset_spinner);
        pBar.setVisibility(View.INVISIBLE);

        resetPinView.setOnClickListener(v -> Controller.getInstance().getDialogManager().hideSoftKeyboard(v));

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
                    Controller.getInstance().getAuthenticationManager().showForgottenPassword(login);
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

                        Task<Void> setPin = Controller.getInstance().getAuthenticationManager().setAccountPin(pin.toString());

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
        if (!Controller.getInstance().getPermissionsManager().isNearbyPermissionsGranted()) {

            //re-check permissions and wait until all granted before
            //trying to connect to other LeadMe users
            Controller.getInstance().getPermissionsManager().checkNearbyPermissions();

        } else if (!Controller.getInstance().getPermissionsManager().isStoragePermissionsGranted()) {
            Controller.getInstance().getPermissionsManager().checkStoragePermission();
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
     * Update the status or warning icon of the connected peer.
     * @param peerID A string representing the connected peer.
     * @param status A int representing the status to update to.
     * @param msg An optional message used for warning alerts.
     */
    public static void updatePeerStatus(String peerID, int status, String msg) {
        if(msg == null) {
            Controller.getInstance().getConnectedLearnersAdapter().updateStatus(peerID, status);
        } else {
            Controller.getInstance().getConnectedLearnersAdapter().updateStatus(peerID, status, msg);
        }
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
        setProgressSpinner(3000, Controller.getInstance().getDialogManager().getIndeterminateBar());
        Controller.getInstance().getAuthenticationManager().FirebaseEmailSignIn(email, password, error);
    }

    /**
     * Signing in using a google account. Get the instance of the google sign in client and start
     * the activity for result.
     */
    public void googleSignIn() {
        Intent signInIntent = Controller.getInstance().getAuthenticationManager().getGoogleSignInClient().getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
}
