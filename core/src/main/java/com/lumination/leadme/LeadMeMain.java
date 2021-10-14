
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
import android.graphics.drawable.ColorDrawable;
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
import android.os.Message;
import android.os.PowerManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eu.bolt.screenshotty.ScreenshotManagerBuilder;

/*
    LeadMe Main:

    • Handles most UI related events
    • Initilises main classes
    • Handles login/signup and authentication
 */

public class LeadMeMain extends FragmentActivity implements Handler.Callback, SensorEventListener, LifecycleObserver, ComponentCallbacks2 {

    //tag for debugging
    static final String TAG = "LeadMe";
    Drawable leadmeIcon;
    protected PowerManager powerManager;

    final private String teacherCode = "1234";
    protected String leadMeAppName = "";
    protected String leadMePackageName = "";
    private boolean codeEntered = false;
    private boolean selectEveryone = false;

    //tag to indicate what incoming message holds
    static final String LOGOUT_TAG = "LumiLogout";
    static final String ACTION_TAG = "LumiAction";
    static final String APP_TAG = "LumiAppLaunch";
    static final String EXIT_TAG = "LumiExit";
    static final String RETURN_TAG = "LumiReturnToApp";
    static final String YOUR_ID_IS = "LumiYourID:";

    static final String LOCK_TAG = "LumiLock";
    static final String UNLOCK_TAG = "LumiUnlock";
    static final String BLACKOUT_TAG = "LumiBlackout";
    static final String APP_LOCK_TAG = "LumiWakeLock";

    static final String VID_MUTE_TAG = "LumiVidMute";
    static final String VID_UNMUTE_TAG = "LumiVidUnmute";
    static final String VID_ACTION_TAG = "LumiVid:";

    static final String AUTO_INSTALL = "LumiAutoInstall";

    static final String LAUNCH_URL = "LumiLaunch:::";
    static final String LAUNCH_YT = "LumiYT:::";
    static final String LAUNCH_ACCESS = "LumiLaunchAccess";

    static final String AUTO_INSTALL_FAILED = "LumiAutoInstallFail:";
    static final String AUTO_INSTALL_ATTEMPT = "LumiAutoInstallAttempt:";
    static final String STUDENT_OFF_TASK_ALERT = "LumiOffTask:";
    static final String STUDENT_NO_OVERLAY = "LumiOverlay:";
    static final String STUDENT_NO_ACCESSIBILITY = "LumiAccess:";
    static final String STUDENT_NO_INTERNET = "LumiInternet:";
    static final String STUDENT_NO_XRAY = "LumiXray:";
    static final String LAUNCH_SUCCESS = "LumiSuccess:";
    final static String SESSION_UUID_TAG = "SessionUUID";
    final static String SESSION_MANUAL_TAG = "SessionManual";

    static final String XRAY_ON = "LumiXrayOn";
    static final String XRAY_OFF = "LumiXrayOff";

    static final String NAME_CHANGE = "NameChange:";
    static final String NAME_REQUEST = "NameRequest:";

    public final int OVERLAY_ON = 0;
    public final int ACCESSIBILITY_ON = 1;
    public final int BLUETOOTH_ON = 2;
    public final int FINE_LOC_ON = 3;
    public final int RC_SIGN_IN = 4;


    //for testing if a connection is still live
    static final String PING_TAG = "LumiPing";
    static final String PING_ACTION = "StillAlive";

    // The SensorManager gives us access to sensors on the device.
    public SensorManager mSensorManager;
    // The accelerometer sensor allows us to detect device movement for shake-to-advertise.
    private Sensor mAccelerometer;

    // Acceleration required to detect a shake. In multiples of Earth's gravity.
    private static final float SHAKE_THRESHOLD_GRAVITY = 2;

    //sessionUUID to enable disconnect/reconnect as same user
    private String sessionUUID = null;
    public Boolean sessionManual = null;

    protected WindowManager windowManager;
    private InputMethodManager imm;
    protected WindowManager.LayoutParams overlayParams;
    private LumiAccessibilityConnector lumiAccessibilityConnector;
    private BroadcastReceiver accessibilityReceiver;
    protected View overlayView;

    public boolean studentLockOn = true; //students start locked
    public String lastLockState = LOCK_TAG;
    public String lastAppID;
    public boolean autoInstallApps = false; //if true, missing apps on student devices get installed automatically

    //details about me to send to peers
    public boolean isGuide = false;
    public boolean isReadyToConnect = false;
    public boolean dialogShowing = false;

    private final Handler handler = new Handler(this);
    public ViewAnimator leadmeAnimator;
    private ViewSwitcher leaderLearnerSwitcher;
    protected boolean loggingInAsLeader = true;
    public String leaderName = "Leader";

    private final int SWITCH_LEADER_INDEX = 0;
    private final int SWITCH_LEARNER_INDEX = 1;

    private final int ANIM_SPLASH_INDEX = 0;
    public final int ANIM_START_SWITCH_INDEX = 1;
    private final int ANIM_LEARNER_INDEX = 2;
    private final int ANIM_LEADER_INDEX = 3;
    private final int ANIM_APP_LAUNCH_INDEX = 4;
    private final int ANIM_OPTIONS_INDEX = 5;
    private final int ANIM_XRAY_INDEX = 6;

    AlertDialog warningDialog, loginDialog, appPushDialog;
    private AlertDialog confirmPushDialog, studentAlertsDialog;
    public View waitingForLearners, appLauncherScreen, appPushDialogView;
    private View loginDialogView, confirmPushDialogView, studentAlertsView;
    private View mainLearner;
    private View mainLeader;
    private View optionsScreen;
    View xrayScreen;
    private TextView warningDialogTitle, warningDialogMessage, learnerWaitingText;
    private Button leader_toggle, learner_toggle;
    ImageView logo;
    ImageView studentImg;

    private GridView connectedStudentsView;

    public Context context;
    public ActivityManager activityManager;
    private PermissionManager permissionManager;
    private NearbyPeersManager nearbyManager;
    private WebManager webManager;
    private AppManager appLaunchAdapter;
    private FavouritesManager favouritesManager; //TODO shift to app manager
    private ConnectedLearnersAdapter connectedLearnersAdapter;
    private LeaderSelectAdapter leaderSelectAdapter;
    private static DispatchManager dispatcher;

    private TextView nameView;
    public Button readyBtn;

    ImageView currentTaskIcon;
    TextView currentTaskTitle;
    TextView currentTaskDescription;
    Button currentTaskLaunchBtn;
    String currentTaskPackageName;
    String currentTaskURLTitle;
    String currentTaskName;
    String currentTaskURL;
    String currentTaskType;

    Intent appIntentOnFocus = null;
    Toast appToast = null;
    boolean returningToApp = false;

    AlertDialog recallPrompt;
    TextView recallMessage;
    Button selectedBtn, everyoneBtn;
    View toggleBtnView;
    boolean returnEveryone = true;


    String appPushPackageName, appPushTitle;
    TextView appPushMessageView;
    Button appPushBtn;

    private boolean init = false;

    XrayManager xrayManager;

    SeekBar seekBar;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ListenerRegistration manualUserListener;
    GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser = null;
    int pinCodeInd = 0;
    String regoCode = "";
    boolean hasScrolled = false;
    boolean allowHide = false;
    ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    public ExecutorService backgroundExecutor = Executors.newCachedThreadPool();
    public ScreenCap screenCap;

    AlertDialog manual;
    View manView;
    String ServerIP="";

    //Manual connection
    private final int leaderTimestampUpdate = 15; //update the leaders timestamp on firebase (mins)
    private final int inactiveUser = 30; //cut off for hiding inactive leaders (mins)
    private final int waitForGuide = 10000; //how long to wait before peer re-querys firestore
    private String publicIP;
    private HashMap<String, Object> manualConnectionDetails = new HashMap<String, Object>();

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

    protected boolean canAskForAccessibility = true;

    public final int SCREEN_CAPTURE = 999;
    private static final int REQUEST_SCREENSHOT_PERMISSION = 1234;

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OVERLAY_ON:
                Log.d(TAG, "Returning from OVERLAY ON with " + resultCode);
                if (getPermissionsManager().isOverlayPermissionGranted()) {
                    if(ServerIP.length()>0){
                        setandDisplayStudentOnBoard(3);
                    }else {
                        setandDisplayStudentOnBoard(2);
                    }
                } else {
                    setandDisplayStudentOnBoard(1);
                }
                break;
            case ACCESSIBILITY_ON:
                Log.d(TAG, "Returning from ACCESS ON with " + resultCode + " (" + isGuide + ")");
                permissionManager.waitingForPermission = false;
                if (getPermissionsManager().isAccessibilityGranted()) {
                    setandDisplayStudentOnBoard(1);
                } else {
                    setandDisplayStudentOnBoard(0);
                }
                //permissionManager.requestBatteryOptimisation();
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
            //added------------
            case SCREEN_CAPTURE:
               // xrayManager.manageResultsReturn(requestCode, resultCode, data);
                screenCap.handleResultReturn(resultCode, data);
                break;

            case RC_SIGN_IN:
                // The Task returned from this call is always completed, no need to attach
                // a listener.
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    handleSignInResult(account);
                } catch (ApiException e) {
                    e.printStackTrace();
                }
                break;

            //added------------
            default:
                Log.d(TAG, "RETURNED FROM ?? with " + resultCode);
                break;
        }
        xrayManager.screenshotManager.onActivityResult(requestCode, resultCode, data);
    }

    public boolean handleMessage(Message msg) {
        Log.d(TAG, "Got a message! " + msg.what + ", " + msg.obj);
        return true;
    }

    public boolean handlePayload(byte[] payloadBytes) {
        if (payloadBytes == null) {
            Log.e(TAG, "Payload is EMPTY!");
            return false;
        }

        //if it's an action, execute it
        if (getDispatcher().readAction(payloadBytes)) {
            Log.d(TAG, "Incoming message was an action!");
            return true; //it was an action, we're done!
        }

        if (getDispatcher().readBool(payloadBytes)) {
            Log.d(TAG, "Incoming message was a boolean request!");
            return true; //it was a boolean, we're done!
        }

        //if it's an app launch request, deploy it
        if (getDispatcher().openApp(payloadBytes)) {
            Log.d(TAG, "Incoming message was an app launch request!");
            return true; //it was an app launch request, we're done!
        }

        Log.e(TAG, "Couldn't find a match for " + payloadBytes);
        return false;
    }


    protected DispatchManager getDispatcher() {
        return dispatcher;
    }

    public LumiAccessibilityConnector getLumiAccessibilityConnector() {
        return lumiAccessibilityConnector;
    }

    public ConnectedLearnersAdapter getConnectedLearnersAdapter() {
        return connectedLearnersAdapter;
    }

    public PermissionManager getPermissionsManager() {
        return permissionManager;
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

    public FavouritesManager getFavouritesManager() {
        return favouritesManager;
    }

    public WebManager getWebManager() {
        return webManager;
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

        //can't go any further
        if (!canAskForAccessibility && !permissionManager.isAccessibilityGranted()) {
            showWarningDialog("Cannot connect to other LeadMe users until Accessibility permission is granted.");
            return;
        }

//        if (canAskForAccessibility && !permissionManager.isAccessibilityGranted()) {
//            Log.d(TAG, "Permission return - request accessibility");
//            permissionManager.requestAccessibilitySettingsOn();
//            return;
//        }

        if (canAskForAccessibility && permissionManager.isAccessibilityGranted() && !permissionManager.isMyServiceRunning(AccessibilityService.class)) {
            Log.d(TAG, "Permission return - accessibility permission granted, but service not running");
            Intent accessibilityIntent = new Intent(this, LumiAccessibilityService.class);
            startService(accessibilityIntent);
            return;
        }

        //make sure we're not just restarting
        if (init && !getNearbyManager().isConnectedAsFollower() && !nearbyManager.isConnectedAsGuide()) {
            //loginAction(); //disabled due to calling onBoarding
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getNearbyManager().onBackPressed();
        Log.i(TAG, "On BACK!");
    }

    /**
     * The device has moved. We need to decide if it was intentional or not.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //already connected, or not ready to connect
        if (getNearbyManager().isConnecting() || getNearbyManager().isConnectedAsGuide() || getNearbyManager().isConnectedAsFollower()) {
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

    private void showAppLaunchScreen() {
        leadmeAnimator.setDisplayedChild(ANIM_APP_LAUNCH_INDEX);
    }

    private void setSelectedOrEveryoneBtn(boolean selected) {
        currentlySelectedOnly = selected;
        if (!selected) {
            appPushDialogView.findViewById(R.id.everyone_btn).setBackground(getResources().getDrawable(R.drawable.bg_passive_left, null));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);

            appPushDialogView.findViewById(R.id.selected_btn).setBackground(getResources().getDrawable(R.drawable.bg_passive_right_white, null));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            appPushBtn.setText(getResources().getString(R.string.push_this_to_everyone));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setElevation(Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics())));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setElevation(0);


        } else {
            appPushDialogView.findViewById(R.id.everyone_btn).setBackground(getResources().getDrawable(R.drawable.bg_passive_left_white, null));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

            appPushDialogView.findViewById(R.id.selected_btn).setBackground(getResources().getDrawable(R.drawable.bg_passive_right, null));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_fav_star_check, 0, 0, 0);
            appPushBtn.setText(getResources().getString(R.string.push_this_to_selected));
            ((Button) appPushDialogView.findViewById(R.id.everyone_btn)).setElevation(Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics())));
            ((Button) appPushDialogView.findViewById(R.id.selected_btn)).setElevation(0);
        }
    }

    boolean currentlySelectedOnly = false;

    public void showAppPushDialog(String title, Drawable icon, String packageName) {
        //TODO include display a message if errors occur
        appPushPackageName = packageName; //keep track of what should launch
        appPushTitle = title;

        //update appearance
        ((TextView) appPushDialogView.findViewById(R.id.push_app_title)).setText(title);
        ((ImageView) appPushDialogView.findViewById(R.id.push_app_icon)).setImageDrawable(icon);

        if (appPushMessageView == null) {
            appPushMessageView = appPushDialogView.findViewById(R.id.push_confirm_txt);
            appPushBtn = appPushDialogView.findViewById(R.id.push_btn);

            appPushDialogView.findViewById(R.id.everyone_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedOrEveryoneBtn(false);
                }
            });

            appPushDialogView.findViewById(R.id.selected_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedOrEveryoneBtn(true);
                }
            });
            setSelectedOrEveryoneBtn(true);
        }

        //selected_btn
        //everyone_btn

//        if (!getConnectedLearnersAdapter().someoneIsSelected()) {
//            //if no-one is selected, prompt to push to everyone
//            setSelectedOrEveryoneBtn(true);
//        } else {
//            //if someone is selected, prompt to push to selected
//            setSelectedOrEveryoneBtn(false);
//        }

        //display push
        if (appPushDialog == null) {
            appPushDialog = new AlertDialog.Builder(this)
                    .setView(appPushDialogView)
                    .show();
            appPushDialog.setOnDismissListener(dialog -> hideSystemUI());
        } else {
            appPushDialog.show();
        }
        dialogShowing = true;
    }

    private void hideAppPushDialogView() {
        if (appPushDialog != null) {
            dialogShowing = false;
            appPushDialog.dismiss();

        }
    }


    private void setupLoginDialog() {
        readyBtn = loginDialogView.findViewById(R.id.connect_btn);
        loginDialogView.findViewById(R.id.close_login_alert_btn).setOnClickListener(v -> {
            if (nameView.getText().toString().trim().length() == 0) {
                nameView.requestFocus();
            } else {
                loginDialogView.findViewById(R.id.login_pin_entry).requestFocus();
            }
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
            openKeyboard();
            hideSystemUI();
        });

        if (readyBtn != null) {
            readyBtn.setOnClickListener(v -> {
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

                } else {
                    //don't need to wait, so just login
                    initPermissions = true; //only prompt once here
                    loginAction(false);
                }

            });
        }

    }

    protected void showLoginDialog() {
        Log.d(TAG, "Showing login dialog");
        if (destroying) {
            return;
        }
        stopShakeDetection();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        //todo if null then show account popup
        if (mAuth.getCurrentUser() == null) {
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.GONE);

        } else {
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);

            getNearbyManager().myName = mAuth.getCurrentUser().getDisplayName();
            getNameView().setText(mAuth.getCurrentUser().getDisplayName());


        }
        //set appropriate mode
        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
            //leader
            Log.d(TAG, "showLoginDialog: teacher");
            loginDialogView.findViewById(R.id.code_entry_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.student_teacher_view).setVisibility(View.GONE);
            TextView forgotPin = loginDialogView.findViewById(R.id.login_forgot_pin);
            forgotPin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setAndDisplayPinReset(0);
                }
            });
        } else {
            //learner
            Log.d(TAG, "showLoginDialog: learner");
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.code_entry_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.student_teacher_view).setVisibility(View.VISIBLE);
            if (getNearbyManager().selectedLeader == null) {
                leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEADER_INDEX);
                showLoginDialog();
                return;
            }
            ((TextView) loginDialogView.findViewById(R.id.teacher_name)).setText(getNearbyManager().selectedLeader.getDisplayName());
        }

        if (loginDialog == null) {
            loginDialog = new AlertDialog.Builder(this)
                    .setView(loginDialogView)
                    .create();
        }
        loginDialog.setOnDismissListener(dialog -> hideSystemUI());
        loginDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText email = loginDialogView.findViewById(R.id.login_email);
        EditText password = loginDialogView.findViewById(R.id.login_password);
        TextView forgotPassword = loginDialogView.findViewById(R.id.login_forgotten);
        TextView errorText = loginDialogView.findViewById(R.id.error_text);
        LinearLayout googleSignin = loginDialogView.findViewById(R.id.login_google);
        Button enterBtn = loginDialogView.findViewById(R.id.login_enter);
        Button backBtn = loginDialogView.findViewById(R.id.login_back);
        TextView signup = loginDialogView.findViewById(R.id.login_signup);

        password.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
            }
        });


        googleSignin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        enterBtn.setOnClickListener(v -> {
            if(password.getText().toString().length()>0 && email.getText().toString().length()>0) {
                setProgressSpinner(3000, loginDialogView.findViewById(R.id.indeterminateBar));
                FirebaseEmailSignIn(email.getText().toString(), password.getText().toString(), errorText);
            }else {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Please check you have entered your details correctly.");
            }
        });

        signup.setOnClickListener(v -> {
            loginDialog.dismiss();
            buildloginsignup(0);
        });

        backBtn.setOnClickListener(v -> loginDialog.dismiss());
        forgotPassword.setOnClickListener(v -> {
            showForgottenPassword(loginDialog);
        });
        //hideSystemUI();
        initPermissions = false; //reset this to ask once more
        dialogShowing = true;
        loginDialog.show();
        nameView.requestFocus();
        openKeyboard();
    }

    private void showForgottenPassword(AlertDialog previous) {
        boolean prevShow=previous.isShowing();
        previous.dismiss();
        View forgotten_view = View.inflate(context, R.layout.c__forgot_password, null);
        AlertDialog forgottenDialog = new AlertDialog.Builder(this)
                .setView(forgotten_view)
                .create();
        LinearLayout forgotten = forgotten_view.findViewById(R.id.forgot_layout);
        LinearLayout email_sent = forgotten_view.findViewById(R.id.email_sent);
        EditText email = forgotten_view.findViewById(R.id.forgot_email);
        Button send = forgotten_view.findViewById(R.id.forgot_enter);
        Button cancel = forgotten_view.findViewById(R.id.forgot_back);
        forgotten.setVisibility(View.VISIBLE);
        forgottenDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        forgottenDialog.show();
        send.setOnClickListener(v -> {
            if (email.getText().toString().length() > 0) {
                mAuth.sendPasswordResetEmail(email.getText().toString())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Email sent.");


                            }
                        });
                forgotten.setVisibility(View.GONE);
                email_sent.setVisibility(View.VISIBLE);
                send.setText("Done");
                send.setOnClickListener(v17 -> forgottenDialog.dismiss());
                cancel.setOnClickListener(v16 -> {
                    forgotten.setVisibility(View.VISIBLE);
                    email_sent.setVisibility(View.GONE);
                    send.setText("Send");
                    send.setOnClickListener(v15 -> {
                        if (email.getText().toString().length() > 0) {
                            mAuth.sendPasswordResetEmail(email.getText().toString())
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Email sent.");


                                        }
                                    });
                            forgotten.setVisibility(View.GONE);
                            email_sent.setVisibility(View.VISIBLE);
                            send.setText("Done");
                            send.setOnClickListener(v14 -> forgottenDialog.dismiss());
                            cancel.setOnClickListener(v13 -> {
                                forgotten.setVisibility(View.VISIBLE);
                                email_sent.setVisibility(View.GONE);
                                send.setText("Send");
                                cancel.setOnClickListener(v12 -> {
                                    forgottenDialog.dismiss();
                                    previous.show();
                                });
                            });
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(), "Please enter your email first", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });
                    cancel.setOnClickListener(v1 -> {
                        forgottenDialog.dismiss();
                        previous.show();
                    });
                });
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please enter your email first", Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        cancel.setOnClickListener(v -> {
            forgottenDialog.dismiss();
            if(prevShow) {
                previous.show();
            }
        });
    }

    private void hideLoginDialog(boolean cancelled) {
        Log.d(TAG, "Hiding dialog box");
        closeKeyboard();
        hideSystemUI();
        if (loginDialog != null) {
            dialogShowing = false;
            loginDialog.dismiss();
            if (cancelled) {
                startShakeDetection();
            }
        }

    }


    protected void showAlertsDialog() {
        if (destroying) {
            return;
        }

        if (studentAlertsDialog == null) {
            studentAlertsDialog = new AlertDialog.Builder(this)
                    .setView(studentAlertsView)
                    .create();
            studentAlertsDialog.setOnDismissListener(dialog -> hideSystemUI());
        }

        hideSystemUI();
        dialogShowing = true;
        getConnectedLearnersAdapter().refreshAlertsView();
        studentAlertsDialog.show();
    }

    private void hideAlertsDialog() {
        closeKeyboard();
        hideSystemUI();
        if (studentAlertsDialog != null) {
            dialogShowing = false;
            studentAlertsDialog.dismiss();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not needed
    }

    private void showSplashScreen() {
        logo.setImageResource(R.mipmap.lumination_logo_reverse);
        leadmeAnimator.setDisplayedChild(ANIM_SPLASH_INDEX);
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
        //Toast.makeText(this, "LC Pause", Toast.LENGTH_LONG).show();
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
            } else {
                //need to allow students to accept permissions
                overlayView.setVisibility(View.INVISIBLE);
            }

        } else if (!studentLockOn) {
            setStudentLock(ConnectedPeer.STATUS_UNLOCK);
            overlayView.setVisibility(View.INVISIBLE);
        }
    }

    private static LumiAccessibilityService accessibilityService;

    boolean debugging = false;
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
//                scheduledExecutor.shutdownNow();
//                scheduledExecutor = new ScheduledThreadPoolExecutor(1);
//                scheduledExecutor.schedule(overlayBack,100, TimeUnit.MILLISECONDS);
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
//            swipePath.lineTo(x + 200, y);
//            swipePath.lineTo(x - 200, y);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 200)); //50 was too short for Within
            GestureDescription swipe = gestureBuilder.build();

            handler.postAtFrontOfQueue(() -> {
                //change overlay so taps can temporarily pass through
                if (overlayView.isAttachedToWindow()) {
                    overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    getWindowManager().updateViewLayout(overlayView, overlayParams);

                    //handler.post(() -> {
                    //new Thread(() -> {
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
                    //}).start();
                }

            });
        });
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onLifecycleResume() {
        if (OnBoardStudentInProgress) {
            if (getPermissionsManager().isAccessibilityGranted() && !getPermissionsManager().isOverlayPermissionGranted()) {
                setandDisplayStudentOnBoard(1);
            } else if (getPermissionsManager().isAccessibilityGranted() && getPermissionsManager().isOverlayPermissionGranted()) {
                if(ServerIP.length()>0){
                    setandDisplayStudentOnBoard(3);
                }else {
                    setandDisplayStudentOnBoard(2);
                }
            }
        }

        //Toast.makeText(this, "LC Resume", Toast.LENGTH_LONG).show();
        Log.w(TAG, "LC Resume // " + getDispatcher().hasDelayedLaunchContent());
        appHasFocus = true;
        getLumiAccessibilityConnector().resetState(); //reset

        manageFocus();


        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEARNER_INDEX) {
            if (permissionManager.isNearbyPermissionsGranted()) {
                displayLearnerStartToggle();
            }
        }

        if (getNearbyManager().isConnectedAsFollower()) {
            //do a delayed check to give Android OS time
            //to catch up from a permission being set
            if (!overlayInitialised) {
                getHandler().postDelayed(() -> {
                    if (permissionManager.isOverlayPermissionGranted()) {
                        getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_OVERLAY, true);
                    } else {
                        getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_OVERLAY, false);
                    }
                }, 1000);
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
        //Toast.makeText(this, "LC Start", Toast.LENGTH_LONG).show();
        Log.w(TAG, "LC Start");
        appHasFocus = true;
        manageFocus();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onLifecycleStop() {
        //Toast.makeText(this, "LC Stop", Toast.LENGTH_LONG).show();
        Log.w(TAG, "LC Stop");
        appHasFocus = false;
        if (!permissionManager.waitingForPermission
                && currentTaskPackageName != null && currentTaskPackageName.equals(leadMePackageName)
                && getNearbyManager().isConnectedAsFollower()) {
            dispatcher.alertGuideStudentOffTask();
            recallToLeadMe();
        } else {
            manageFocus();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onLifecycleDestroy() {
        //Toast.makeText(this, "LC Destroy", Toast.LENGTH_LONG).show();
        Log.d(TAG, "LC Destroy");
        appHasFocus = false;
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

        if (init && loginDialog != null && loginDialog.isShowing()) {
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
                studentLockOn=false;
                //if we're in lock mode and we should be in something other than LeadMe, relaunch it
                if (studentLockOn && currentTaskPackageName != null && currentTaskPackageName != leadMePackageName) {
                    Log.e(TAG, "RELAUNCH?? " + currentTaskPackageName);

                    if (currentTaskPackageName.equals(getAppManager().withinPackage) || currentTaskPackageName.equals(getAppManager().youtubePackage)) {
                        getAppManager().relaunchLast(currentTaskPackageName, currentTaskName, currentTaskType, currentTaskURL, currentTaskURLTitle);
                    } else {
                        getAppManager().relaunchLast();
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
        }
    }

    boolean destroying = false;

    @Override
    public void onDestroy() {
        if (getAppManager().getWithinPlayer().controllerWebView != null)
            getAppManager().getWithinPlayer().controllerWebView.destroy();
        super.onDestroy();
        Log.w(TAG, "In onDestroy");
        backgroundExecutor.shutdownNow();
        //subscription.dispose();
        destroyAndReset();
        //xrayManager.screenShot = false;
    }

    private void destroyAndReset() {
        destroying = true;
        init = false;
//        if (wakeLock.isHeld()) {
//                wakeLock.release(); //release this when the app is destroyed
//            }
        //getLumiAccessibilityService().disableSelf(); //manually turn off the Accessibility Service

        if (accessibilityReceiver != null) {
            try {
                unregisterReceiver(accessibilityReceiver); //stop listening for re-broadcast intents
            } catch (Exception e) {
                Log.e(TAG, "Accessibility receiver was not registered!");
            }
        }

//        xrayManager.stopScreenshotRunnable();
//        xrayManager.stopServer();

        cleanUpDialogs();

        //clean up nearby connections
        isGuide = false;
        isReadyToConnect = false;
        getNearbyManager().onStop();
        getNearbyManager().stopAdvertising();
        getNearbyManager().disconnectFromAllEndpoints();

        //remove the overlay if necessary
        if (overlayView != null && overlayView.isAttachedToWindow()) {
            windowManager.removeView(overlayView);
        }

        //clean up link preview assets
        getWebManager().cleanUp();
    }

    protected void cleanUpDialogs() {
        if (loginDialog != null)
            loginDialog.dismiss();
        if (waitingDialog != null)
            waitingDialog.dismiss();
        if (warningDialog != null)
            warningDialog.dismiss();
        if (appPushDialog != null)
            appPushDialog.dismiss();
        if (confirmPushDialog != null)
            confirmPushDialog.dismiss();
        if (recallPrompt != null)
            recallPrompt.dismiss();

        if (webManager != null) {
            webManager.cleanUp();
        }
    }

    public void hideSystemUIStudent() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
//    private void showSystemUI() {
////        View decorView = getWindow().getDecorView();
////        decorView.setSystemUiVisibility(
////                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
////                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
////                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//    }

    public void hideSystemUI() {
        if (getNearbyManager().isConnectedAsFollower()) {
            return;
        }
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        int newUiOptions = uiOptions;
        newUiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(newUiOptions);
    }

    public void showSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        int newUiOptions = uiOptions;
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
        screenCap = new ScreenCap(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        currentUser = mAuth.getCurrentUser();


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.w(TAG, "On create! " + init);

        //store a random UUID for this phone in shared preferences
        //will be unique and anonymous and will stay stable for a
        //good period of time to allow re-connections during a session
        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        //if a UUID exists, retrieve it
        if (sharedPreferences.contains(SESSION_UUID_TAG)) {
            sessionUUID = sharedPreferences.getString(SESSION_UUID_TAG, null);
        }

        //if none exists, make one and store it
        if (sessionUUID == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            sessionUUID = UUID.randomUUID().toString();
            editor.putString(SESSION_UUID_TAG, sessionUUID);
            editor.apply();
        }

        //TODO add manual toggle selection
        if (sharedPreferences.contains(SESSION_MANUAL_TAG)) {
            sessionManual = sharedPreferences.getBoolean(SESSION_MANUAL_TAG, false);
        }

        if (sessionManual == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            sessionManual = false;
            editor.putBoolean(SESSION_MANUAL_TAG, sessionManual);
            editor.apply();
        }

        context = getApplicationContext();

        //add observer to respond to lifecycle events
        getLifecycle().addObserver(this);

        //create adapters
        permissionManager = new PermissionManager(this);
        nearbyManager = new NearbyPeersManager(this);
        dispatcher = new DispatchManager(this);
        webManager = new WebManager(this);
        favouritesManager = new FavouritesManager(this, null, FavouritesManager.FAVTYPE_APP, 4);
        leaderSelectAdapter = new LeaderSelectAdapter(this);
        lumiAccessibilityConnector = new LumiAccessibilityConnector(this);

        appPushDialogView = View.inflate(context, R.layout.e__preview_app_push, null);
        appLaunchAdapter = new AppManager(this);

        //set up a receiver to capture the re-broadcast intent
        accessibilityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "Received rebroadcast intent: " + intent);
                getLumiAccessibilityConnector().triageReceivedIntent(intent);
            }
        };
        Log.w(TAG, "Registering receiver");
        registerReceiver(accessibilityReceiver, new IntentFilter(LumiAccessibilityConnector.PROPAGATE_ACTION));

        //details about me to send to peers
        studentLockOn = false; //students start unlocked
        isGuide = false;
        isReadyToConnect = false;
        loggingInAsLeader = true;

        if (nearbyManager != null) {
            nearbyManager.setID(null);
        }
        //}

        initPermissions = false; //reset

        leadMeAppName = getResources().getString(R.string.app_title);
        leadMePackageName = getPackageName();

        setTheme(R.style.Theme_AppCompat_Light_NoActionBar); //switches from splash screen to main
        //https://android.jlelse.eu/the-complete-android-splash-screen-guide-c7db82bce565

        //listen for UI visibility changes
        Log.d(TAG, "Adding System Visibility listener to window/decor");
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                visibility -> {
                    Log.d(TAG, "DECOR VIEW! " + getNearbyManager().isConnectedAsFollower() + ", " + dialogShowing);
                    if (getNearbyManager().isConnectedAsFollower() || OnBoardStudentInProgress) {
                        if (allowHide) {
                            handler.postDelayed(this::hideSystemUIStudent, 0);
                        }
                    } else {
                        //hide after short delay
//                        if(allowHide) {
//                            handler.postDelayed(this::hideSystemUI, 1500);
//                        }
                    }
                });

        try {
            leadmeIcon = getPackageManager().getApplicationIcon(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //killAllBackgroundProcesses(); //start with a fresh slate

        //set up shake detection
        mSensorManager = (SensorManager) getSystemService(LeadMeMain.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        hideSystemUI();
        imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        //set up the view animator with all key views
        leadmeAnimator = (ViewAnimator) View.inflate(context, R.layout.a__viewanimator, null);
        Log.d(TAG, "Got animator: " + leadmeAnimator);

        View splashscreen = View.inflate(context, R.layout.a__splash_screen, null);
        logo = splashscreen.findViewById(R.id.lumi_logo);
        logo.setImageResource(R.mipmap.lumination_logo_reverse);
        View startLeader = View.inflate(context, R.layout.b__start_leader, null);
        View startLearner = View.inflate(context, R.layout.b__start_learner, null);
        mainLearner = View.inflate(context, R.layout.c__learner_main, null);
        mainLeader = View.inflate(context, R.layout.c__leader_main, null);
        optionsScreen = View.inflate(context, R.layout.d__options_menu, null);
        xrayScreen = View.inflate(context, R.layout.d__xray_view, null);
        appLauncherScreen = View.inflate(context, R.layout.d__app_list, null);
        learnerWaitingText = startLearner.findViewById(R.id.waiting_text);
        xrayManager = new XrayManager(this, xrayScreen);

        //set up main page search

        Button searchBtn = mainLeader.findViewById(R.id.search_btn);
        SearchView searchView = mainLeader.findViewById(R.id.search_bar);
        searchBtn.setVisibility(View.GONE);
        searchBtn.setOnClickListener(v -> {
            //TODO enable this when search function implemented
//            if(searchView.getVisibility() == View.GONE){
//                //searchBtn.setBackground(getResources().getDrawable(R.drawable.close, null));
//                searchView.setVisibility(View.VISIBLE);
//            } else {
//                //searchBtn.setBackground(getResources().getDrawable(R.drawable.search_icon, null));
//                searchView.setVisibility(View.GONE);
//            }
        });

        alertsBtn = mainLeader.findViewById(R.id.alerts_button);
        alertsBtn.setOnClickListener(v -> showAlertsDialog());
        //alertsBtn.setVisibility(View.GONE); //by default, hide this

        //initialise window manager for shared use
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        //SET UP OVERLAY (don't add it to the window manager until permission granted)
        buildOverlay();

        //add adapters for lists and grids
        //getLumiAccessibilityService();
        GridView appGrid = ((GridView) appLauncherScreen.findViewById(R.id.app_list_grid));
        appGrid.setAdapter(getAppManager());
        ViewGroup.LayoutParams layoutParams = appGrid.getLayoutParams();
        layoutParams.height = appGrid.getMeasuredHeight(); //this is in pixels
        appGrid.setLayoutParams(layoutParams);
        ((GridView) appLauncherScreen.findViewById(R.id.fav_list_grid)).setAdapter(getFavouritesManager());
        ((LinearLayout) appLauncherScreen.findViewById(R.id.current_task_layout)).setVisibility(View.GONE);
        ((TextView) appLauncherScreen.findViewById(R.id.text_current_task)).setVisibility(View.GONE);
        ((ListView) startLearner.findViewById(R.id.leader_list_view)).setAdapter(getLeaderSelectAdapter());

        ((ImageView) appLauncherScreen.findViewById(R.id.repush_btn)).setOnClickListener((View.OnClickListener) v -> {
            if (lastLockState != null && lastLockState.equals(LOCK_TAG)) {
                getDispatcher().requestRemoteAppOpen(APP_TAG, lastAppID, String.valueOf(((TextView) appLauncherScreen.findViewById(R.id.text_current_task)).getText()), LOCK_TAG, getNearbyManager().getSelectedPeerIDsOrAll());
            } else {
                getDispatcher().requestRemoteAppOpen(APP_TAG, lastAppID, String.valueOf(((TextView) appLauncherScreen.findViewById(R.id.text_current_task)).getText()), UNLOCK_TAG, getNearbyManager().getSelectedPeerIDsOrAll());
            }
            showConfirmPushDialog(true, false);
        });

        mainLeader.findViewById(R.id.url_core_btn).setOnClickListener(v -> getWebManager().showWebLaunchDialog(false, false));

        mainLeader.findViewById(R.id.vr_core_btn).setOnClickListener(v -> {
            getAppManager().getWithinPlayer().showWithin(); //launch within search
            //getWebManager().showWebLaunchDialog(true, false)
        });


        Button app_btn = mainLeader.findViewById(R.id.app_core_btn);
        //app_btn.setOnClickListener(v -> showAppLaunchScreen());
        app_btn.setOnClickListener((View.OnClickListener) v -> {
            showAppLaunchScreen();
            ((ScrollView) appLauncherScreen.findViewById(R.id.app_scroll_view)).scrollTo(0, 0);
        });


        mainLeader.findViewById(R.id.xray_core_btn).setOnClickListener(v -> {
            if (getConnectedLearnersAdapter().getCount() > 0) {
                xrayManager.showXrayView("");
            } else {
                Toast.makeText(getApplicationContext(), "No students connected.", Toast.LENGTH_SHORT).show();
            }
        });

        studentAlertsView = View.inflate(context, R.layout.d__alerts_list, null);
        ListView studentAlerts = studentAlertsView.findViewById(R.id.current_alerts_list);

        View no_alerts_view = studentAlertsView.findViewById(R.id.no_alerts_message);
        View alerts_list = studentAlertsView.findViewById(R.id.current_alerts_list);

        StudentAlertsAdapter alertsAdapter = new StudentAlertsAdapter(this, alerts_list, no_alerts_view);
        studentAlerts.setAdapter(alertsAdapter);

        studentAlertsView.findViewById(R.id.confirm_btn).setOnClickListener(v -> hideAlertsDialog());

        studentAlertsView.findViewById(R.id.clear_alerts_btn).setOnClickListener(v -> alertsAdapter.hideCurrentAlerts());

        connectedLearnersAdapter = new ConnectedLearnersAdapter(this, new ArrayList<>(), alertsAdapter);
        connectedStudentsView = mainLeader.findViewById(R.id.studentListView);
        connectedStudentsView.setAdapter(connectedLearnersAdapter);

        //set up return to app button
        mainLeader.findViewById(R.id.leadme_icon).setOnClickListener(v -> {
            if (isGuide) {
                showRecallDialog();
            } else {
                //test my connection
                if (!getNearbyManager().isConnectedAsFollower() && !getNearbyManager().isConnectedAsGuide()) {
                    Log.e(TAG, "No longer connected!");
                    logoutAction();
                } else {
                    Log.d(TAG, "Going to test my connection: " + getNearbyManager().isConnectedAsFollower() + ", " + getNearbyManager().isConnectedAsGuide());
                    getNearbyManager().startPingThread();
                }
            }
        });


        //set up start switcher and main animator
        final View switcherView = View.inflate(context, R.layout.a__viewswitcher, null);
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

        showSplashScreen();

        setContentView(leadmeAnimator);

        //set up menu buttons
        View.OnClickListener menuListener = v -> {
            lastDisplayedIndex = leadmeAnimator.getDisplayedChild();
            leadmeAnimator.setDisplayedChild(ANIM_OPTIONS_INDEX);
            if (getNearbyManager().isConnectedAsFollower() || getNearbyManager().isConnectedAsGuide()) {
                optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.GONE);
                optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.GONE);
            } else {

                if (mAuth.getCurrentUser() != null) {
                    optionsScreen.findViewById(R.id.options_teacher).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.GONE);
                    optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.GONE);
                    ((TextView) optionsScreen.findViewById(R.id.options_signed_name)).setText(mAuth.getCurrentUser().getDisplayName());
                } else {
                    optionsScreen.findViewById(R.id.options_loginBtn).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_notsigned).setVisibility(View.VISIBLE);
                    optionsScreen.findViewById(R.id.options_teacher).setVisibility(View.GONE);
                }
            }
            if (getNearbyManager().isConnectedAsGuide()) {
                optionsScreen.findViewById(R.id.options_teacher).setVisibility(View.VISIBLE);
                optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.VISIBLE);
                ((TextView) optionsScreen.findViewById(R.id.options_signed_name)).setText(mAuth.getCurrentUser().getDisplayName());
            } else if (getNearbyManager().isConnectedAsFollower()) {
                optionsScreen.findViewById(R.id.options_endSess).setVisibility(View.GONE);
                optionsScreen.findViewById(R.id.options_teacher).setVisibility(View.GONE);
            }
        };


        startLeader.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        startLearner.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        mainLeader.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        mainLearner.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        appLauncherScreen.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        xrayScreen.findViewById(R.id.menu_btn).setOnClickListener(menuListener);

        //set up back buttons
        appLauncherScreen.findViewById(R.id.back_btn).setOnClickListener(v -> leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX));

        //prepare elements for app push dialog
        appPushDialogView.findViewById(R.id.push_btn).setOnClickListener(v -> {
            appLaunchAdapter.launchApp(appPushPackageName, appPushTitle, false);
            Log.d(TAG, "LAUNCHING! " + appPushPackageName);
            hideAppPushDialogView();
            showConfirmPushDialog(true, false);
        });

        appPushDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> hideAppPushDialogView());

        //set up options screen
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
        optionsScreen.findViewById(R.id.options_loginBtn).setOnClickListener(view -> showLoginDialog());
        optionsScreen.findViewById(R.id.options_notsigned).setOnClickListener(view -> buildloginsignup(0));
        optionsScreen.findViewById(R.id.how_to_use_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
                startActivity(browserIntent);
            }
        });

        optionsScreen.findViewById(R.id.help_support_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email[] = {"dev@lumination.com.au"};
                composeEmail(email,"LeadMe Support");
            }
        });

        manView = View.inflate(context, R.layout.e__manual_popup, null);
        manual = new AlertDialog.Builder(this)
                .setView(manView)
                .create();

        //change the shared preferences, do the rest on login for guide or learner button select
        Switch ManualToggle = optionsScreen.findViewById(R.id.server_discovery);
        ManualToggle.setChecked(sessionManual);
        ManualToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Learner cannot switch while logged in
                switchManualPreference(sharedPreferences, isChecked);
            }
        });

        //direct ip input connection
        optionsScreen.findViewById(R.id.manual_connect).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                manual.show();
                Button back = manView.findViewById(R.id.manual_back);
                back.setOnClickListener(v1 -> manual.dismiss());
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                if(isGuide){
                    manView.findViewById(R.id.manual_teacher_view).setVisibility(View.VISIBLE);
                    manView.findViewById(R.id.manual_learner_view).setVisibility(View.GONE);
                    manView.findViewById(R.id.manual_ok).setVisibility(View.GONE);
                    TextView IpAddress = manView.findViewById(R.id.manual_ip);
                    IpAddress.setText(ipAddress);
                }else{
                    if(getNearbyManager().isConnectedAsFollower()){
                        manual.dismiss();
                        Toast.makeText(getApplicationContext(), "You are already connected to a leader", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    manView.findViewById(R.id.manual_learner_view).setVisibility(View.VISIBLE);
                    manView.findViewById(R.id.manual_ok).setVisibility(View.VISIBLE);
                    manView.findViewById(R.id.manual_teacher_view).setVisibility(View.GONE);
                    EditText IpEnter = manView.findViewById(R.id.manual_enterIP);
                    EditText ManName = manView.findViewById(R.id.manual_name);
                    Button connect = manView.findViewById(R.id.manual_ok);
                    IpEnter.setText(ipAddress.substring(0, ipAddress .lastIndexOf(".")+1)   );
                    IpEnter.setSelection(IpEnter.getText().length());
                    //add to the leaders list

                    connect.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(IpEnter!=null && ManName!=null &&ManName.getText().toString().length()>0 && IpEnter.getText().toString().length()>0){
                                Log.d(TAG, "onClick: "+IpEnter.getText().toString());
                                getNameView().setText(ManName.getText().toString());
                                getNearbyManager().myName = ManName.getText().toString();
                                runOnUiThread(() -> {
                                    getLeaderSelectAdapter().addLeader(new ConnectedPeer("key", IpEnter.getText().toString()));
                                    //showLeaderWaitMsg(false);
                                });
                                manual.dismiss();
                                ServerIP = IpEnter.getText().toString();
                                isGuide=false;
                                loginAction(true);
//                                getNearbyManager().connectToManualLeader(IpEnter.getText().toString());
//                                leadmeAnimator.setDisplayedChild(ANIM_LEARNER_INDEX);
//                                screenCap.startService(false);
                            }
                        }
                    });
                }
            }
        });

        optionsScreen.findViewById(R.id.logout_btn).setOnClickListener(v -> {
            if (isGuide || !getNearbyManager().isConnectedAsFollower()) {
                mAuth.signOut();
                currentUser = mAuth.getCurrentUser();
                optionsScreen.findViewById(R.id.options_teacher).setVisibility(View.GONE);
                logoutAction();
            } else {
                Toast.makeText(getApplicationContext(), "Logout is unavailable.", Toast.LENGTH_SHORT).show();
            }

        });
        optionsScreen.findViewById(R.id.options_endSess).setOnClickListener(view -> {
            if (isGuide || !getNearbyManager().isConnectedAsFollower()) {
                logoutAction();
            } else {
                Toast.makeText(getApplicationContext(), "Logout is unavailable.", Toast.LENGTH_SHORT).show();
            }
        });


        optionsScreen.findViewById(R.id.connected_only_view).setVisibility(View.GONE);
        optionsScreen.findViewById(R.id.auto_install_checkbox).setVisibility(View.GONE);

        //prepare elements for connection dialog
        leader_toggle = switcherView.findViewById(R.id.leader_btn);
        learner_toggle = switcherView.findViewById(R.id.learner_btn);

        leader_toggle.setOnClickListener(v -> {
            displayLeaderStartToggle();
            if (mAuth.getCurrentUser() == null) {
                loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.VISIBLE);
                loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
                loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.GONE);
            }
        });
        if (mAuth.getCurrentUser() == null) {
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.GONE);
        } else {
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
        }
        learner_toggle.setOnClickListener(v -> {
            displayLearnerStartToggle();
            if (mAuth.getCurrentUser() == null) {
                loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.GONE);
                loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
                loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
            }
        });

        //prepare elements for login dialog
        getNameView();

        startLeader.findViewById(R.id.app_login).setOnClickListener(v -> showLoginDialog());

        loginDialogView.findViewById(R.id.back_btn).setOnClickListener(v -> hideLoginDialog(true));

        loginDialogView.findViewById(R.id.connect_btn).setOnClickListener(v -> initiateLeaderAdvertising());

        setupLoginDialog();


        //prepare elements for leader main view
        waitingForLearners = mainLeader.findViewById(R.id.no_students_connected);
        //appLauncherScreen = View.inflate(context, R.layout.d__app_list, null);
        confirmPushDialogView = View.inflate(context, R.layout.e__confirm_popup, null);

        confirmPushDialogView.findViewById(R.id.ok_btn).setOnClickListener(v -> hideConfirmPushDialog());


        //set up options screen
        //auto install of missing apps on student devices
        CheckBox auto_install_checkbox = optionsScreen.findViewById(R.id.auto_install_checkbox);
        auto_install_checkbox.setChecked(autoInstallApps); //toggle the checkbox
        auto_install_checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoInstallApps = isChecked;
            Log.d(TAG, "Changed detected! Auto install is now " + autoInstallApps);
            Toast toast;
            if (autoInstallApps) {
                toast = Toast.makeText(getApplicationContext(), "Missing apps will be installed automatically on student devices.", Toast.LENGTH_SHORT);
            } else {
                toast = Toast.makeText(getApplicationContext(), "Missing apps will NOT be installed on student devices.", Toast.LENGTH_SHORT);
            }
            toast.show();
            getDispatcher().sendBoolToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL, autoInstallApps, getNearbyManager().getSelectedPeerIDs());
        });

        setUpControlButtons();

        initPermissions = true;
        permissionManager.checkMiscPermissions();

        if (!permissionManager.isNearbyPermissionsGranted()) {
            permissionManager.checkNearbyPermissions();
        }
//        currentTaskIcon = mainLearner.findViewById(R.id.current_task_icon);
//        currentTaskIcon.setImageResource(R.color.transparent);

        xrayManager.screenshotManager = new ScreenshotManagerBuilder(this).withPermissionRequestCode(REQUEST_SCREENSHOT_PERMISSION) //optional, 888 is the default
                .build();
        //start this
        //getForegroundActivity();
        seekBar = (SeekBar) findViewById(R.id.screen_capture_rate);
        seekBar.setProgress(20); //default value that seems to work with slowish phones
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int rate;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rate = progress;
                //Toast.makeText(getApplicationContext(),"seekbar progress: " + progress, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(),"seekbar touch started!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                xrayManager.screenshotRate = 1000 / rate * 10;
                Toast.makeText(getApplicationContext(), "Capture rate: " + rate + " fps", Toast.LENGTH_SHORT).show();
            }
        });


        mainLeader.findViewById(R.id.select_bar_back).setOnClickListener(v -> {
            getConnectedLearnersAdapter().selectAllPeers(false);
        });
        CheckBox checkBox = mainLeader.findViewById(R.id.select_bar_selectall);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                getConnectedLearnersAdapter().selectAllPeers(true);
            } else {
                getConnectedLearnersAdapter().selectAllPeers(false);
            }
        });
        mainLeader.findViewById(R.id.select_bar_repush).setOnClickListener(v -> {
            getDispatcher().repushApp(getNearbyManager().getSelectedPeerIDsOrAll());
            getConnectedLearnersAdapter().selectAllPeers(false);
        });


//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll()
//                    .penaltyLog().build());
//
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
//                            .penaltyLog().build());
//        }

//        if a UUID exists, retrieve it
        displayGuidePrompt(sharedPreferences);

    }

    //MANUAL CONNECTION FUNCTIONS START
    public void switchManualPreference(SharedPreferences sharedPreferences, boolean isManual) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        sessionManual = isManual;
        editor.putBoolean(SESSION_MANUAL_TAG, sessionManual);
        editor.apply();

        //TODO if logged in as learner skip
        if(isManual && isGuide) {
            createManualConnection();
        } else {
            //remove listeners for followers
        }
    }

    private void createManualConnection() {
        //initiate public ip track and firebase
        waitForPublic();
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        ServerIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        manualConnectionDetails.put("Username", mAuth.getCurrentUser().getDisplayName());
        manualConnectionDetails.put("ServerIP", ServerIP);
        manualConnectionDetails.put("TimeStamp", FieldValue.serverTimestamp());

        //update firebase and start a continuous timer for updating the timestamp
        Timer timestamp = new Timer();
        TimerTask updateTimestamp = new TimerTask() {
            @Override
            public void run() {
                updateAddress();
            }
        };

        timestamp.scheduleAtFixedRate(updateTimestamp, 0L, leaderTimestampUpdate * (60 * 1000));
    }

    /*register the login details with PublicIP address as the documentID
    create a new document of the publicIP address if does not exist
    create a new collection, Leaders if it does not exist in case multiple Leaders are online
    create a new document of the ServerIP address with username, ServerIP, PublicIP and timestamp fields*/
    private void updateAddress() {
        if(publicIP.length()==0){
            return;
        }
        db.collection("addresses").document(publicIP)
            .collection("Leaders").document(ServerIP).set(manualConnectionDetails, SetOptions.merge())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }

    private void waitForPublic() {
        Thread getPublic = new Thread(() -> {
            Log.d(TAG, "waitForPublic: this1");
            publicIP = getPublicIP();
            manualConnectionDetails.put("PublicIP", publicIP); //store as reference for the clean up server
        });

        getPublic.start();

        try {
            getPublic.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "waitForPublic: this2");
    }

    private String getPublicIP() {
//        if(publicIP!=null && publicIP.length()>0){
//            return publicIP;
//        }
//        Log.d(TAG, "getPublicIP: here");
        String publicIPlocal = "";
        try  {
            java.util.Scanner s = new java.util.Scanner(
                    new java.net.URL(
                            "https://api.ipify.org")
                            .openStream(), "UTF-8")
                    .useDelimiter("\\A");
            publicIPlocal = s.next();
            Log.d(TAG, "getPublicIP: got public");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Log.d(TAG, "getPublicIP: didn't get public");
        }

        return publicIPlocal;
    }
    //MANUAL CONNECTION FUNCTIONS END

    public void composeEmail(String[] addresses, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void displayGuidePrompt(SharedPreferences sharedPreferences) {
        if (!sharedPreferences.contains("FIRST")) {
            View firstDialog = View.inflate(context, R.layout.a__first_time, null);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("FIRST", true);
            editor.apply();
            AlertDialog alert = new AlertDialog.Builder(this)
                    .setView(firstDialog)
                    .show();
            firstDialog.findViewById(R.id.open_guide).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
                            startActivity(browserIntent);
                    //todo link to guide
                }
            });
            firstDialog.findViewById(R.id.skip_guide).setOnClickListener(v -> alert.dismiss());
        }
    }

    public void exitXrayView() {
        leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
    }

    public void displayXrayView() {
        leadmeAnimator.setDisplayedChild(ANIM_XRAY_INDEX);
    }

    Button alertsBtn;

    public ProgressBar setProgressTimer(int Time) {
        ProgressBar indeterminate = mainLeader.findViewById(R.id.leader_loading);
        if (indeterminate != null) {
            if (Time > 0) {
                runOnUiThread(() -> indeterminate.setVisibility(View.VISIBLE));
                scheduledExecutorService.schedule((Runnable) () -> runOnUiThread(() -> indeterminate.setVisibility(View.INVISIBLE)), (long) Time, TimeUnit.MILLISECONDS);
            }
        }
        return indeterminate;
    }

    public void setAlertsBtnVisibility(int visibility) {
        alertsBtn.setVisibility(View.VISIBLE);
    }

    protected void buildOverlay() {
        LinearLayout parentLayout = findViewById(R.id.c__leader_main);
        overlayView = LayoutInflater.from(context).inflate(R.layout.transparent_overlay, parentLayout, false);
        overlayView.findViewById(R.id.blocking_view).setVisibility(View.GONE); //default is this should be hidden
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

        overlayInitialised = true; //must set this before calling disable interaction

        //set default state
        getDispatcher().disableInteraction(ConnectedPeer.STATUS_UNLOCK);

    }

    public TextView getNameView() {
        if (loginDialogView == null || nameView == null) {
            loginDialogView = View.inflate(context, R.layout.b__login_popup, null);
            nameView = loginDialogView.findViewById(R.id.name_input_field);

            //TODO temporary code to allow me to skip login while testing
            loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
            loginDialogView.findViewById(R.id.login_signup_view).setVisibility(View.GONE);
        }
        return nameView;
    }

    private void prepLoginSwitcher() {
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
        if (!permissionManager.isNearbyPermissionsGranted()) {
            learnerWaitingText.setText(getResources().getString(R.string.enable_location_to_connect));
            permissionManager.checkNearbyPermissions();
        } else {
            if(sessionManual) {
                initiateManualLeaderDiscovery();
            }
            else {
                initiateLeaderDiscovery();
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
    public void showConfirmPushDialog(boolean isApp, boolean isSavedOnly) {
        //TODO include display a message if errors occur

        if (confirmPushDialog == null) {
            confirmPushDialog = new AlertDialog.Builder(this)
                    .setView(confirmPushDialogView)
                    .show();
            confirmPushDialog.setOnDismissListener(dialog -> hideSystemUI());
        } else {
            confirmPushDialog.show();
        }
        dialogShowing = true;

        if (isSavedOnly) {
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_comment)).setText(R.string.fav_save_success);
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_title)).setText(R.string.save_success_title);
        } else if (isApp) {
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_comment)).setText(R.string.app_push_success);
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_title)).setText(R.string.push_success_title);
        } else { //isLink
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_comment)).setText(R.string.link_push_success);
            ((TextView) confirmPushDialog.findViewById(R.id.push_success_title)).setText(R.string.push_success_title);
        }

        closeKeyboard();

        //auto close after 1.5 seconds
        if (isSavedOnly) {
            handler.postDelayed(() -> {
                hideConfirmPushDialog();
                getWebManager().launchUrlYtFavourites();
            }, 1500);
        } else {
            handler.postDelayed(this::hideConfirmPushDialog, 1500);
        }

    }

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


    public void hideConfirmPushDialog() {
        if (confirmPushDialog != null) {
            dialogShowing = false;
            confirmPushDialog.dismiss();
        }
        //return to main screen
        leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
    }

    public void showWarningDialog(String title, String message) {
        if (destroying) {
            return;
        }
        if (warningDialog == null) {
            setupWarningDialog();
        }
        warningDialogTitle.setText(title);
        warningDialogMessage.setText(message);
        warningDialogMessage.setVisibility(View.VISIBLE);
        warningDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }

    public void showWarningDialog(String message) {
        if (destroying) {
            return;
        }
        if (warningDialog == null) {
            setupWarningDialog();
        }
        warningDialogTitle.setText(getResources().getString(R.string.oops_something_went_wrong));
        warningDialogMessage.setText(message);
        warningDialogMessage.setVisibility(View.VISIBLE);
        warningDialog.show();
        hideSystemUI();
        dialogShowing = true;
    }

    protected void closeWaitingDialog(boolean success) {
        Log.d(TAG, "Closing waiting dialog! " + success + ", " + waitingDialog + ", " + loginDialog);
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
        }
        if (loginDialog != null && loginDialog.isShowing()) {
            loginDialog.dismiss();
        }
        dialogShowing = (waitingDialog != null && waitingDialog.isShowing()) || (loginDialog != null && loginDialog.isShowing());
        Log.d(TAG, "Are they showing now?? " + (waitingDialog != null && waitingDialog.isShowing()) + " || " + (loginDialog != null && loginDialog.isShowing()));

        //wait for dialogs to close
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        if (dialogShowing) { //try again until they're both gone
//            closeWaitingDialog(success);
//            return;
//        }

        if (!success) {
            //failed to login, so show login screen again
            prepLoginSwitcher();
            leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);

        } else if (!isGuide) {
            //only need this if we're a follower
            if (!permissionManager.isOverlayPermissionGranted()) {
                //permissionManager.checkOverlayPermissions();
            } else {
                initialiseOverlayView();
            }
        }
    }

    public AlertDialog waitingDialog = null;

    private void showWaitingForConnectDialog() {
        if (waitingDialog == null) {
            View waitingDialogView = View.inflate(context, R.layout.e__waiting_to_connect, null);
            Button backBtn = waitingDialogView.findViewById(R.id.back_btn);
            backBtn.setOnClickListener(v -> {
                waitingDialog.dismiss();
                dialogShowing = false;
                getNearbyManager().cancelConnection();
            });

            waitingDialog = new AlertDialog.Builder(this)
                    .setView(waitingDialogView)
                    .create();
            waitingDialog.setOnDismissListener(dialog -> hideSystemUI());
        }

        loginDialog.dismiss();
        waitingDialog.show();
        dialogShowing = true;
    }


    private void setupWarningDialog() {
        View warningDialogView = View.inflate(context, R.layout.e__warning_popup, null);
        warningDialogTitle = warningDialogView.findViewById(R.id.warning_title);
        warningDialogMessage = warningDialogView.findViewById(R.id.warning_comment);
        Button okBtn = warningDialogView.findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(v -> {
            warningDialog.dismiss();
            dialogShowing = false;
            warningDialogMessage.setVisibility(View.GONE);
        });

        warningDialog = new AlertDialog.Builder(this)
                .setView(warningDialogView)
                .create();
        warningDialog.setOnDismissListener(dialog -> hideSystemUI());
    }

    public void showLoginAlertMessage() {
        loginDialogView.findViewById(R.id.indeterminateBar).setVisibility(View.GONE);
        loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.GONE);
        loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.VISIBLE);
        closeKeyboard();
        hideSystemUI();
    }

    public void initiateLeaderDiscovery() {
        Log.d(TAG, "Initiating Leader Discovery");
        isReadyToConnect = true;
        getNearbyManager().discoverLeaders();
    }

    //MANUAL CONNECTION FOR LEARNERS START
    public void initiateManualLeaderDiscovery() {
        Log.d(TAG, "Initiating Leader Discovery");
        isReadyToConnect = true;
        getNearbyManager().networkAdapter.startDiscovery();
        retrieveLeaders();
    }

    //call to firebase to retrieve any leaders registered to that publicIP address
    private void retrieveLeaders() {
        waitForPublic();
        Log.d(TAG, "retrieveLeaders: "+publicIP);
        if (publicIP == null|| publicIP.length()==0) {
            return;
        }
        CollectionReference collRef = db.collection("addresses").document(publicIP).collection("Leaders");

        collRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    //if no one has registered on the public IP yet, wait sometime and try again.
                    try {
                        if (Objects.requireNonNull(task.getResult()).size() == 0) {
                            scheduledExecutorService.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(() -> {
                                        retrieveLeaders();
                                    });
                                }
                            }, waitForGuide, TimeUnit.MILLISECONDS);
//                        try {
//                            Thread.sleep(waitForGuide);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }

                        } else {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                Date leaderTimeStamp = Objects.requireNonNull(document.getTimestamp("TimeStamp")).toDate();

                                if (checkTimeDifference(leaderTimeStamp) >= inactiveUser) {
                                    return;
                                }

                                //add to the leaders list
                                runOnUiThread(() -> {
                                    getLeaderSelectAdapter().addLeader(new ConnectedPeer(document.get("Username").toString(), document.get("ServerIP").toString()));
                                    showLeaderWaitMsg(false);
                                });
                            }

                            //add listeners to track if leader hasn't logged in but publicIP exists (multiple leaders on network)
                            //TODO trackCollection works fine, haven't create code to stop the listeners when students log in.
                            trackCollection(collRef);
                        }
                    }catch(NullPointerException e){
                        Log.d(TAG, "onComplete: "+e);
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });
    }

    //add a listener to the Leader collection to wait for log in
    private void trackCollection(CollectionReference collRef) {
        //checkArray(document);
        //adapter.notifyDataSetChanged();
        if(manualUserListener!=null){
            manualUserListener.remove();
        }
        if(!getNearbyManager().isConnectedAsFollower()) {
            Log.d(TAG, "trackCollection: listener added");
            manualUserListener = collRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    Log.d(TAG, "onEvent: ip listener fired");
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            if (document.get("Username") != null) {
                                //checkArray(document);
                                runOnUiThread(() -> {
                                    getLeaderSelectAdapter().addLeader(new ConnectedPeer(document.get("Username").toString(), document.get("ServerIP").toString()));
                                    showLeaderWaitMsg(false);
                                });
                            }
                        }
                        //adapter.notifyDataSetChanged();
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                }
            });
        }
        //TODO stop listener when leader is selected
    }

    /*calculate the difference between the firebase timestamp and the current time
    return the minutes, can also work out other units if necessary.*/
    private int checkTimeDifference(Date leaderTimeStamp) {
        Date now = new Date();

        long difference_In_Time = now.getTime() - leaderTimeStamp.getTime();
        long difference_In_Minutes = (difference_In_Time / (1000 * 60)) % 60;
        long difference_In_Hours = (difference_In_Time / (1000 * 60 * 60)) % 24;

        /*max difference can be 24 * 60 (mins), server clears firestore once or twice a day
        so anything more is not necessary at this point.*/
        return (int) (difference_In_Hours * 60) + (int) difference_In_Minutes;
    }

    //MANUAL CONNECTION FOR LEARNERS END

    public boolean checkLoginDetails() {
        //reset error messages
        loginDialogView.findViewById(R.id.no_name_message).setVisibility(View.GONE);
        loginDialogView.findViewById(R.id.wrong_code_message).setVisibility(View.GONE);
        final PinEntryEditText pinEntry = loginDialogView.findViewById(R.id.login_pin_entry);

        //check that a name has been entered
        boolean nameEntered;
        if (nameView.getText().toString().trim().length() == 0) { //no name entered
            nameEntered = false;
            loginDialogView.findViewById(R.id.no_name_message).setVisibility(View.VISIBLE);
        } else { //name entered
            nameEntered = true;
            String name = getNearbyManager().getName();
            Log.d(TAG, "Your name is now " + name);
        }

        //if appropriate, check if the correct code has been entered
        if (loggingInAsLeader) {
            //check teacher code
            String code = pinEntry.getText().toString();
            pinEntry.setText("");
            Log.d(TAG, "Code entered: " + code);
            // For showing
            ProgressBar progressBar = loginDialogView.findViewById(R.id.indeterminateBar);
            progressBar.setVisibility(View.VISIBLE);

            // For hiding

            db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (Hasher.Companion.hash(code, HashType.SHA_256).equals(task.getResult().getString("pin"))) {
                        codeEntered = true;

                        progressBar.setVisibility(View.GONE);
                        loginAction(false);
                    } else {
                        codeEntered = false;
                        showLoginAlertMessage();
                        loginDialogView.findViewById(R.id.wrong_code_message).setVisibility(View.VISIBLE);
                    }

                }
            });
//            if (code.equals(teacherCode)) { //correct code
//                codeEntered = true;
//            } else { //incorrect code
//                codeEntered = false;
//            loginDialogView.findViewById(R.id.wrong_code_message).setVisibility(View.VISIBLE);
//            }
        } else {
            codeEntered = true; //mark as true, since we don't need one
            if (!nameEntered) {
                //alert to errors and exit
                showLoginAlertMessage();
                return false; //failed
            } else {
                return true; //succeeded
            }
        }
        return false;
//        if (!nameEntered || !codeEntered) {
//            //alert to errors and exit
//            showLoginAlertMessage();
//            return false; //failed
//        } else {
//            return true; //succeeded
//        }
    }

    public void initiateLeaderAdvertising() {
        if (loggingInAsLeader) {
            getNearbyManager().setAsGuide();
        }
    }

    // TODO need to reset peer id setting?
    void logoutAction() {
        getDispatcher().alertLogout(); //need to send this before resetting 'isGuide'
        isGuide = false;
        getNearbyManager().networkAdapter.resetClientIDs();
        getConnectedLearnersAdapter().resetOnLogout();
        //xrayManager.monitorInProgress = false; //break connection loop in clientStream
        getNearbyManager().onStop();
        getNearbyManager().stopAdvertising();
        getNearbyManager().disconnectFromAllEndpoints(); //disconnect everyone
        getLeaderSelectAdapter().setLeaderList(new ArrayList<>()); //empty the list
        setUIDisconnected();
        showSplashScreen();
        moveAwayFromSplashScreen();
    }

    boolean loginAttemptInAction = false;

    // protected PowerManager.WakeLock wakeLock;

    void loginAction(boolean isManual) {
        Log.w(TAG, "LOGGING IN " + nearbyManager.getName());
        //   wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":" + APP_LOCK_TAG);
        //  wakeLock.acquire(); //this is to keep the device alive while logged in to LeadMe
        loginAttemptInAction = true;
        //if all permissions are already granted, just continue
        if(isManual){
            leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEARNER_INDEX);
        }
        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
            initiateLeaderAdvertising();
            loginAttemptInAction = false;

        } else {
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

        hideLoginDialog(false);

        String name = getNearbyManager().getName();
        if(isManual) {
            isGuide=false;
        }
        Log.d(TAG, "Your name is " + name + ", are you a guide? " + isGuide);

        ((TextView) optionsScreen.findViewById(R.id.student_name)).setText(name);
        optionsScreen.findViewById(R.id.connected_only_view).setVisibility(View.VISIBLE);
        if (isGuide) {
            optionsScreen.findViewById(R.id.capture_rate_display).setVisibility(View.GONE);
        } else {
            optionsScreen.findViewById(R.id.capture_rate_display).setVisibility(View.VISIBLE);
        }

        if (isGuide) {
            //display main guide view
            leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);


            //update options
            //TODO re-implement these AUTO-INSTALL features later
//            optionsScreen.findViewById(R.id.auto_install_checkbox).setVisibility(View.VISIBLE);
            ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.light, null));
            TextView title = leadmeAnimator.getCurrentView().findViewById(R.id.leader_title);
            title.setText(name);
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setText(getResources().getText(R.string.leader));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setTextColor(getResources().getColor(R.color.accent, null));

            SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
//            if a UUID exists, retrieve it
            if (!sharedPreferences.contains("ONBOARD")) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("ONBOARD", true);
                editor.apply();
                buildAndDisplayOnBoard();
            }

            //if it is a manual connection session, create a firebase lookup entry
            if(sessionManual) {
                createManualConnection();
            }
        } else {
            //display main student view
            leadmeAnimator.setDisplayedChild(ANIM_LEARNER_INDEX);
            allowHide = true;
            handler.postDelayed(() -> hideSystemUIStudent(), 1000);
            //update options
            ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.leadme_medium_grey, null));
            TextView title = leadmeAnimator.getCurrentView().findViewById(R.id.learner_title);
            title.setText(name);
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setText(getResources().getText(R.string.learner));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_role)).setTextColor(getResources().getColor(R.color.medium, null));
            //refresh overlay
            // verifyOverlay();
            if(manualUserListener!=null){
                Log.d(TAG, "loginAction: listener removed");
                manualUserListener.remove();
                manualUserListener=null;
                getNearbyManager().networkAdapter.stopDiscovery();
            }

        }
    }

    private void setUpControlButtons() {
        //TODO reimplement this as needed
        mainLeader.findViewById(R.id.unlock_selected_btn).setOnClickListener(v -> {
            unlockFromMainAction();
        });

        mainLeader.findViewById(R.id.lock_selected_btn).setOnClickListener(v -> {
            lockFromMainAction();
        });

        mainLeader.findViewById(R.id.block_selected_btn).setOnClickListener(v -> {
            blackoutFromMainAction();

        });
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
    }

    public void refreshOverlay() {
        calcParams();
        if (overlayInitialised && overlayView != null) {
            windowManager.updateViewLayout(overlayView, overlayParams);
        }

        if (appHasFocus) {
            overlayView.setVisibility(View.INVISIBLE); //NEVER want this over LeadMe
        }
    }

    int CORE_FLAGS;

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

                currentTaskLaunchBtn.setOnClickListener(v -> {
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

    private void showRecallDialog() {
        dialogShowing = true;
        Log.w(TAG, "Showing recall dialog");

        if (recallPrompt == null) {
            View recallView = View.inflate(context, R.layout.e__recall_confirm_popup, null);
            recallMessage = recallView.findViewById(R.id.recall_comment);
            toggleBtnView = recallView.findViewById(R.id.toggleBtnView);
            selectedBtn = recallView.findViewById(R.id.selected_btn);
            everyoneBtn = recallView.findViewById(R.id.everyone_btn);

            recallView.findViewById(R.id.ok_btn).setOnClickListener(v -> {
                returnToAppFromMainAction(returnEveryone);
                dialogShowing = false;
                recallPrompt.dismiss();
            });

            recallView.findViewById(R.id.back_btn).setOnClickListener(v -> {
                dialogShowing = false;
                recallPrompt.dismiss();
            });

            recallView.findViewById(R.id.selected_btn).setOnClickListener(v -> makeSelectedBtnActive());

            recallView.findViewById(R.id.everyone_btn).setOnClickListener(v -> makeEveryoneBtnActive());

            recallPrompt = new AlertDialog.Builder(this)
                    .setView(recallView)
                    .create();
            recallPrompt.setOnDismissListener(dialog -> hideSystemUI());
        }

        if (getConnectedLearnersAdapter().someoneIsSelected() && (getNearbyManager().getSelectedPeerIDs().size() < getNearbyManager().getAllPeerIDs().size())) {
            recallMessage.setText(getResources().getString(R.string.recall_comment_selected));
            toggleBtnView.setVisibility(View.VISIBLE);
            makeSelectedBtnActive();
        } else {
            recallMessage.setText(getResources().getString(R.string.recall_comment_all));
            toggleBtnView.setVisibility(View.GONE);
        }

        recallPrompt.show();
        dialogShowing = true;
    }

    private void makeSelectedBtnActive() {
        returnEveryone = false;
        selectedBtn.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_active_right, null));
        everyoneBtn.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_passive_left, null));
        selectedBtn.setTextColor(getResources().getColor(R.color.leadme_light_grey, null));
        everyoneBtn.setTextColor(getResources().getColor(R.color.light, null));
    }

    private void makeEveryoneBtnActive() {
        returnEveryone = true;
        selectedBtn.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_passive_right, null));
        everyoneBtn.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_active_left, null));
        everyoneBtn.setTextColor(getResources().getColor(R.color.leadme_light_grey, null));
        selectedBtn.setTextColor(getResources().getColor(R.color.light, null));
    }


    public void recallToLeadMe() {
        if(leadmeAnimator.getDisplayedChild()==ANIM_START_SWITCH_INDEX){
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

        Log.d(TAG, "Recalling to LeadMe! " + dialogShowing + ", " + appHasFocus + ", " + hasWindowFocus() + ", " + getLifecycle().getCurrentState());
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
            returningToApp = true;
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
    public void returnToAppFromMainAction(boolean returnEveryone) {
        Log.d(TAG, "Returning to app from MAIN! " + appHasFocus + ", " + hasWindowFocus());
        //String connections = getNearbyManager().getSelectedPeerIDsAsString();

        Set<String> chosenPeers;
        if (!returnEveryone && getConnectedLearnersAdapter().someoneIsSelected()) {
            chosenPeers = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosenPeers = getNearbyManager().getAllPeerIDs();
        }
        Log.i(TAG, "Returning " + chosenPeers.size() + " learners to LeadMe (" + chosenPeers.toString() + ") vs me: " + getNearbyManager().getID());
        getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.RETURN_TAG, chosenPeers);

        if (appToast == null) {
            returningToApp = true;
            appToast = Toast.makeText(context, "Returning selected followers to Lumination Lead Me app", Toast.LENGTH_SHORT);
            appToast.show();
        }
    }

    public void displayLearnerMain(String leaderName) {
        TextView leaderTitle = mainLearner.findViewById(R.id.leader_name);

        studentImg.setImageResource(R.drawable.connected_student);
        leaderTitle.setText(leaderName);

        TextView learnerTitle = mainLearner.findViewById(R.id.learner_title);
        learnerTitle.setText(getNearbyManager().getName());
    }

    //UPDATE OPTIONS PAGE
    public void setUIDisconnected() {
        //reset state
        isReadyToConnect = false; //need to press button first
//        if (wakeLock.isHeld()) {
//            wakeLock.release(); //release the wakeLock when disconnected
//        }
        getLumiAccessibilityConnector().resetState();

        cleanUpDialogs();

        //reset views
        showConnectedStudents(false);

        if (overlayView != null) {
            overlayView.setVisibility(View.INVISIBLE);
        }

        readyBtn.setEnabled(true);
        readyBtn.setText(R.string.connect_label);
        if (isGuide) {
            waitingForLearners.setVisibility(View.GONE);
        }

        Log.d(TAG, "SET UI DISCONNECTED");

        //update options screen
        optionsScreen.findViewById(R.id.connected_only_view).setVisibility(View.GONE);
        ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.light, null));


        //display login view
        prepLoginSwitcher();
        leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);

        initiateLeaderDiscovery();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkable = menu.findItem(R.id.auto_install);
        checkable.setChecked(autoInstallApps);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
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

    public void setLeaderName(String name) {
        leaderName = name;
        ((TextView) mainLearner.findViewById(R.id.leader_name)).setText(name);
        mainLearner.findViewById(R.id.connected_icon).setVisibility(View.VISIBLE);
        mainLearner.findViewById(R.id.connected_txt).setVisibility(View.VISIBLE);
    }

    ///////////////////////

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
        return appHasFocus && screenOn && (!init || leadmeAnimator.isShown());
    }


    public void updateLastTask(Drawable icon, String Name, String appID, String lock) {
        lastAppID = appID;
        lastLockState = lock;
        Log.d(TAG, "updateLastTask: " + Name);
        ((LinearLayout) appLauncherScreen.findViewById(R.id.current_task_layout)).setVisibility(View.VISIBLE);
        ((TextView) appLauncherScreen.findViewById(R.id.text_current_task)).setVisibility(View.VISIBLE);
        ((ImageView) appLauncherScreen.findViewById(R.id.current_icon)).setImageDrawable(icon);
        ((TextView) appLauncherScreen.findViewById(R.id.current_app_name)).setText(Name);
        appLauncherScreen.invalidate();
        updateLastOffTask();
    }

    public void updateLastOffTask() {
        int offTask = getConnectedLearnersAdapter().alertsAdapter.getCount();
        //String.valueOf(offTask)
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
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            if (isChecked) {
                getConnectedLearnersAdapter().selectAllPeers(true);
            } else {
                getConnectedLearnersAdapter().selectAllPeers(false);
            }
        };
        CheckBox checkBox = mainLeader.findViewById(R.id.select_bar_selectall);
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(numSelected == getConnectedLearnersAdapter().getCount());
        checkBox.setOnCheckedChangeListener(listener);


    }

    public int onBoardPage = 0;
    public View OnBoard;

    @SuppressLint("ClickableViewAccessibility")
    private void buildAndDisplayOnBoard() {
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
        //TextSwitcher onBoardContent = OnBoard.findViewById(R.id.onBoard_content);
        OnBoard.setOnTouchListener((v, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }
            return false;
        });

        ImageView[] buttons = {OnBoard.findViewById(R.id.oboard_btn_1), OnBoard.findViewById(R.id.oboard_btn_2), OnBoard.findViewById(R.id.oboard_btn_3), OnBoard.findViewById(R.id.oboard_btn_4), OnBoard.findViewById(R.id.oboard_btn_5)};
        for (int i = 0; i < buttons.length; i++) {
            int ind = i;
            buttons[i].setOnClickListener(new View.OnClickListener() {
                int index = ind;

                @Override
                public void onClick(View v) {
                    setOnboardCurrent(index);
                }
            });
        }
        ImageView nextButton = OnBoard.findViewById(R.id.next_button);
        nextButton.setOnClickListener(v -> setOnboardCurrent(onBoardPage + 1));
        TextView skipIntro = OnBoard.findViewById(R.id.skip_intro);
        skipIntro.setOnClickListener(v -> setContentView(leadmeAnimator));
        setOnboardCurrent(0);
        this.setContentView(OnBoard);
    }

    public void setOnboardCurrent(int current) {
        Log.d(TAG, "setOnboardCurrent: ");

        ImageView[] buttons = {
                OnBoard.findViewById(R.id.oboard_btn_1),
                OnBoard.findViewById(R.id.oboard_btn_2),
                OnBoard.findViewById(R.id.oboard_btn_3),
                OnBoard.findViewById(R.id.oboard_btn_4),
                OnBoard.findViewById(R.id.oboard_btn_5)};

        String titleToShow[] = {
                getResources().getString(R.string.onboard_title_1),
                getResources().getString(R.string.onboard_title_2),
                getResources().getString(R.string.onboard_title_3),
                getResources().getString(R.string.onboard_title_4),
                getResources().getString(R.string.onboard_title_5)};

        String textToShow[] = {
                getResources().getString(R.string.onboard_1),
                getResources().getString(R.string.onboard_2),
                getResources().getString(R.string.onboard_3),
                getResources().getString(R.string.onboard_4),
                getResources().getString(R.string.onboard_5)};
//
//        Uri[] videos = {Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.welcome), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.push_app),
//                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.play_view_block), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.select),
//                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.recall)};


        Uri[] videos = {Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.welcome), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.push_app),
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.block), Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.manage),
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.recall)};

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
        ImageView nextButton = OnBoard.findViewById(R.id.next_button);
        LinearLayout buttons_layout = OnBoard.findViewById(R.id.onboard_buttons);
        buttons_layout.setVisibility(View.GONE);
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
        if (onBoardPage <= 4) {
            onBoardTitle.setText(titleToShow[onBoardPage]);
            onBoardContent.setText(textToShow[onBoardPage]);
            video.setVideoURI(videos[onBoardPage]);
            video.requestFocus();
            video.start();
            //  pageHolder.setImageResource(imageToDraw[index]);
            if (onBoardPage == 4) {
                skipIntro.setVisibility(View.GONE);
                nextButton.setVisibility(View.GONE);
                buttons_layout.setVisibility(View.VISIBLE);
                OnBoard.findViewById(R.id.onboard_ok_btn).setOnClickListener(v1 -> {
                    video.setBackgroundColor(Color.WHITE);
                    handler.postDelayed(() -> setContentView(leadmeAnimator), 50);
                    video.suspend();
                    OnBoard = null;


                });
                OnBoard.findViewById(R.id.onboard_moreinfo_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1GGU7GeR4Ibq60-6bcc2F_bd698CKRFvZ/view"));
                        startActivity(browserIntent);
                    }
                });
            } else {
                skipIntro.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.VISIBLE);
                buttons_layout.setVisibility(View.GONE);
            }
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
            support.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
                    startActivity(browserIntent);
                }
            });
            cancelPermission.setOnClickListener(v -> {
                leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);
                this.setContentView(leadmeAnimator);
            });
            this.setContentView(OnBoardPerm);
        } else if (page == 1) {
            View OnBoardPerm = View.inflate(this, R.layout.c__onboarding_student_2, null);
            TextView support = OnBoardPerm.findViewById(R.id.onboardperm_support);
            support.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1LrbQ5I1jlf-OQyIgr2q3Tg3sCo00x5lu/view"));
                    startActivity(browserIntent);
                }
            });
            Button okPermission = OnBoardPerm.findViewById(R.id.onboardperm_ok_btn);
            Button cancelPermission = OnBoardPerm.findViewById(R.id.onboardperm_cancel_btn);

            okPermission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "setStudentOnBoard: checking");
                    scheduledCheck = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                        @Override
                        public void run() {
                            if (getPermissionsManager().isOverlayPermissionGranted()) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(ServerIP.length()>0){
                                            setandDisplayStudentOnBoard(3);
                                        }else{
                                            setandDisplayStudentOnBoard(2);
                                        }
                                    }
                                });

                            }
                        }
                    },100,100,TimeUnit.MILLISECONDS);
                    if (getPermissionsManager().isOverlayPermissionGranted()) {
                        setandDisplayStudentOnBoard(2);
                    }
                    getPermissionsManager().checkOverlayPermissions();
                }
            });
            cancelPermission.setOnClickListener(v -> {
                leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);
                this.setContentView(leadmeAnimator);
            });
            this.setContentView(OnBoardPerm);
        } else {
            if(scheduledCheck!=null){
                scheduledCheck.cancel(true);
            }
            setContentView(leadmeAnimator);
            OnBoardStudentInProgress = false;
            updateFollowerCurrentTaskToLeadMe();
            loginAttemptInAction = false;

            if(page==2 && !sessionManual) {
                getNearbyManager().connectToSelectedLeader();
                showWaitingForConnectDialog();
            }else if(sessionManual){
                if(ServerIP.equals("")) {
                    ServerIP = getNearbyManager().selectedLeader.getID();
                }
                getNearbyManager().connectToManualLeader(ServerIP);
            }

            screenCap.startService(false);
        }
    }

    public void buildloginsignup(int page) {
        buildloginsignup(page, false);
    }
    private String loginEmail = "";
    private String loginPassword = "";
    private String Name = "";
    private Boolean Marketing =false;

    public void buildloginsignup(int page, boolean signinVerif) {
        showSystemUI();
        View Login = View.inflate(this, R.layout.b__login_signup, null);
        LinearLayout[] layoutPages = {Login.findViewById(R.id.rego_code), Login.findViewById(R.id.terms_of_use), Login.findViewById(R.id.signup_page)
                , Login.findViewById(R.id.email_verification)
                , Login.findViewById(R.id.set_pin), Login.findViewById(R.id.account_created)};
        Button next = Login.findViewById(R.id.signup_enter);
        Button back = Login.findViewById(R.id.signup_back);
        ProgressBar progressBar = Login.findViewById(R.id.signup_indeterminate);
        progressBar.setVisibility(View.GONE);
        TextView support = Login.findViewById(R.id.rego_contact_support);
        support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email[] = {"dev@lumination.com.au"};
                composeEmail(email,"LeadMe Support: Signup Issue");
            }
        });
        //page 0
        EditText loginCode = Login.findViewById(R.id.rego_code_box);
        TextView regoLost = Login.findViewById(R.id.rego_lost_code);
        regoLost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email[] = {"dev@lumination.com.au"};
                composeEmail(email,"LeadMe Support: Signup Code Request");
            }
        });
        TextView regoError = Login.findViewById(R.id.rego_code_error);
        //page 2
        TextView signupError = Login.findViewById(R.id.signup_error);
        EditText signupName = Login.findViewById(R.id.signup_name);
        EditText signupEmail = Login.findViewById(R.id.signup_email);
        EditText signupPass = Login.findViewById(R.id.signup_password);
        EditText signupConPass = Login.findViewById(R.id.signup_confirmpass);
        CheckBox marketingCheck = Login.findViewById(R.id.signup_marketing);
        signupEmail.setText(loginEmail);
        signupPass.setText(loginPassword);
        signupName.setText(Name);
        marketingCheck.setChecked(Marketing);
        //page 1
        TextView errorText = Login.findViewById(R.id.tou_readtext);
        ScrollView touScroll = Login.findViewById(R.id.tou_scrollView);
        TextView terms = Login.findViewById(R.id.tou_terms);
        CheckBox touAgree = Login.findViewById(R.id.tou_check);
        //page 3
        VideoView animation = Login.findViewById(R.id.email_animation);
        //page 4
        TextView pinError = Login.findViewById(R.id.pin_error_text);
        ImageView pinErrorImg = Login.findViewById(R.id.pin_error_image);
        //page 5
        TextView accountText = Login.findViewById(R.id.account_createdtext);
        for (int i = 0; i < layoutPages.length; i++) {
            if (i != page) {
                layoutPages[i].setVisibility(View.GONE);
            } else {
                layoutPages[i].setVisibility(View.VISIBLE);
            }
        }
        setContentView(Login);
        switch (page) {
            case 0:
                showSystemUI();
                regoError.setVisibility(View.GONE);
                //loginCode.requestFocus();
                loginCode.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        showSystemUI();
                    }
                });
                loginCode.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        showSystemUI();
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        Log.d(TAG, "onTextChanged: " + count);
                        if (s.length() == 6) {
                            closeKeyboard();
                        }
//                        else if(s.length()>6){
//                            loginCode.setText(s.subSequence(0,7));
//                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                next.setOnClickListener(v -> {
                    if (loginCode.getText().length() == 6) {
                        setProgressSpinner(3000, progressBar);
                        db.collection("signin_codes").document(loginCode.getText().toString())
                                .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "buildloginsignup: database accessed");
                                if (task.getResult().exists()) {
                                    //todo add email under signup code
//                                        if(task.getResult().get)
                                    regoCode = loginCode.getText().toString();
                                    buildloginsignup(1);
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    regoError.setText("I'm sorry that code doesn't exist.");
                                    regoError.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Log.d(TAG, "buildloginsignup: unable to access database");
                            }
                        });
                    }else{
                        regoError.setVisibility(View.VISIBLE);
                        regoError.setText("Please check you have entered the code correctly.");
                    }
                });
                back.setOnClickListener(v -> {
                    setContentView(leadmeAnimator);
                    hideSystemUI();
                });
                break;
                case 1:
                hideSystemUI();
                handler.postDelayed(() -> hideSystemUI(), 500);
                errorText.setVisibility(View.GONE);
//                terms.setOnScrollChangeListener(new View.OnScrollChangeListener() {
//                    boolean scrolled = false;
//                    @Override
//                    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                        if(scrollY==v.scr)
//                    }
//                });
                hasScrolled = true;
                WebView TOF = Login.findViewById(R.id.tof_webview);
                TOF.getSettings().setJavaScriptEnabled(true);
                String pdf = "https://github.com/LuminationDev/public/raw/main/LeadMeEdu-TermsAndConditions.pdf";
                TOF.loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=" + pdf);


                        touAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked && !hasScrolled) {
                                touAgree.setChecked(false);
                                errorText.setVisibility(View.VISIBLE);
                                errorText.setText("Please read all of the terms of use");
                            }
                        });

                next.setOnClickListener(v -> {
                    if (touAgree.isChecked()) {
                        buildloginsignup(2);
                    } else {
                        errorText.setVisibility(View.VISIBLE);
                    }
                });
                back.setOnClickListener(v -> buildloginsignup(1));
                break;
            case 2:
                showSystemUI();
                signupError.setVisibility(View.GONE);
                marketingCheck.setOnCheckedChangeListener((buttonView, isChecked) -> closeKeyboard());
                next.setOnClickListener(v -> {
                    if (signupName.getText().toString().length() == 0) {
                        signupError.setText("Please enter a name");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (signupEmail.getText().toString().length() == 0) {
                        signupError.setText("Please enter an email");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (signupPass.getText().toString().length() == 0) {
                        signupError.setText("Please enter a password");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (signupPass.getText().toString().equals(signupConPass.getText().toString())) {
                        signupError.setVisibility(View.GONE);
                        setProgressSpinner(3000, progressBar);
                        loginEmail = signupEmail.getText().toString();
                        loginPassword = signupPass.getText().toString();
                        Name = signupName.getText().toString();
                        Marketing = marketingCheck.isChecked();
                        FirebaseEmailSignUp(signupEmail.getText().toString(), signupPass.getText().toString(), signupName.getText().toString(), marketingCheck.isChecked(), regoCode, signupError, progressBar);
                        hideSystemUI();
                    } else {
                        signupError.setText("Passwords do not match");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                });
                back.setOnClickListener(v -> {
                    buildloginsignup(0);
                    hideSystemUI();
                });
                break;


            case 3:
                handler.postDelayed(() -> hideSystemUI(), 500);
                next.setVisibility(View.GONE);
                Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.email_sent);
                animation.setVideoURI(uri);
                animation.setBackgroundColor(Color.WHITE);
                Log.d(TAG, "buildloginsignup: here");
                animation.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    animation.start();
                    handler.postDelayed(() -> animation.setBackgroundColor(Color.TRANSPARENT), 100);
                });
                FirebaseAuth.AuthStateListener mAuthListener= new FirebaseAuth.AuthStateListener() {
                    @Override
                    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                        if (!mAuth.getCurrentUser().isEmailVerified()) {
                            Log.d(TAG, "buildloginsignup: email verification sent");
                            mAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    scheduledExecutorService.scheduleAtFixedRate(() -> mAuth.addAuthStateListener(firebaseAuth1 -> {
                                        Log.d(TAG, "run: checking user verification");
                                        if (!mAuth.getCurrentUser().isEmailVerified()) {
                                            mAuth.getCurrentUser().reload();
                                        } else {
                                            currentUser = mAuth.getCurrentUser();
                                            scheduledExecutorService.shutdown();
                                            runOnUiThread(() -> {
                                                buildloginsignup(4);
                                            });
                                        }
                                    }), 100, 100, TimeUnit.MILLISECONDS);
                                }
                            });
                        } else {
                            Log.d(TAG, "buildloginsignup: user is already verified");
                        }
                    }
                };
                mAuth.addAuthStateListener(mAuthListener);
                Log.d(TAG, "buildloginsignup: and here");
                back.setOnClickListener(v -> {
                    scheduledExecutorService.shutdown();
                    mAuth.removeAuthStateListener(mAuthListener);
                    firebaseRemoveUser(mAuth.getCurrentUser());
                    mAuth.getCurrentUser().delete();
                    buildloginsignup(2);
//                    setContentView(leadmeAnimator);
                });
                break;
            case 4:
                next.setVisibility(View.VISIBLE);
                pinError.setText("Your email has been verified");
                pinError.setTextColor(getColor(R.color.leadme_black));
                pinErrorImg.setImageResource(R.drawable.icon_fav_star_check);
                if (signinVerif) {
                    setProgressSpinner(3000, progressBar);
                    db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                        if (task.getResult().exists()) {
                            if (task.getResult().getString("pin").length() > 0) {
                                progressBar.setVisibility(View.GONE);
                                getNearbyManager().myName = currentUser.getDisplayName();
                                getNameView().setText(currentUser.getDisplayName());
                                setContentView(leadmeAnimator);
                                loginAction(false);
                            }
                        }
                    });

                }
                showSystemUI();
                next.setOnClickListener(v -> {
                    final PinEntryEditText pinEntry = (PinEntryEditText) findViewById(R.id.signup_pin_entry);
                    final PinEntryEditText pinEntryConfirm = (PinEntryEditText) findViewById(R.id.signup_pin_confirm);
                    if (pinEntry != null && pinEntryConfirm!=null && pinEntry.getText().toString().equals(pinEntryConfirm.getText().toString())) {
                        Map<String, Object> userDet = new HashMap<>();
                        setProgressSpinner(3000, progressBar);
                        userDet.put("pin", Hasher.Companion.hash(pinEntry.getText().toString(), HashType.SHA_256));
                        db.collection("users").document(mAuth.getCurrentUser().getUid()).update(userDet).addOnSuccessListener(aVoid -> Log.d(TAG, "onSuccess: pin saved to account"));
                        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.GONE);
                                getNearbyManager().myName = task.getResult().getString("name");
                                getNameView().setText(task.getResult().getString("name"));
                                setContentView(leadmeAnimator);
                                loginAction(false);
                                loginPassword="";
                                Name="";
                                loginEmail="";
                                Marketing=false;
                            }
                        });
                    } else {
                        pinError.setText("The pin's do not match");
                        pinError.setTextColor(getColor(R.color.leadme_red));
                        pinErrorImg.setImageResource(R.drawable.alert_error);
                        progressBar.setVisibility(View.GONE);
                    }
                });
                break;
            default:
                break;
        }

    }

    private void firebaseRemoveUser(FirebaseUser currentUser) {
        db.collection("users").document(currentUser.getUid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    currentUser.delete();
                }
            }
        });
    }

    //handles signin requests for the google signin
    private void handleSignInResult(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser = mAuth.getCurrentUser();
                mAuth.addAuthStateListener(firebaseAuth -> {
                    if (currentUser != null) {
                        db.collection("users").document(currentUser.getUid())
                                .get().addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                if (task1.getResult().exists()) {
                                    Log.d(TAG, "handleSignInResult: user found");
                                    getNearbyManager().myName = account.getGivenName();
                                    getNameView().setText(account.getGivenName());
                                    loginAction(false);
                                } else {
                                    Log.d(TAG, "handleSignInResult: new user");
                                    Map<String, Object> userDet = new HashMap<>();
                                    userDet.put("name", account.getGivenName() + " " + account.getFamilyName());
                                    userDet.put("email", currentUser.getEmail());
                                    db.collection("users").document(currentUser.getUid()).set(userDet)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "handleSignInResult: new user created");
                                                getNearbyManager().myName = account.getGivenName();
                                                getNameView().setText(account.getGivenName());
                                                hideSystemUI();
                                                loginAction(false);
                                            })
                                            .addOnFailureListener(e -> Log.d(TAG, "handleSignInResult: failed to create new user please check internet"));

                                }
                            }
                        });
                    } else {
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        startActivityForResult(signInIntent, RC_SIGN_IN);
                    }
                });
            } else {
                Log.d(TAG, "handleSignInResult: failed to sign in");
            }
        });

    }

    private void FirebaseEmailSignUp(String email, String password, String name, boolean marketing, String regoCode, TextView errorText, ProgressBar progressBar) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(LeadMeMain.this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        currentUser = task.getResult().getUser();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        currentUser.updateProfile(profileUpdates);
                        Map<String, Object> userDet = new HashMap<>();
                        userDet.put("name", name);
                        userDet.put("email", email);
                        userDet.put("marketing", marketing);
                        userDet.put("rego_code", regoCode);
                        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task1 -> {
                            if (task1.getResult().exists()) {
                                Log.d(TAG, "user data exists but user is deleted, updating user info");
                            }
                            db.collection("users").document(mAuth.getCurrentUser().getUid()).set(userDet)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "buildloginsignup: new user created");
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    errorText.setVisibility(View.VISIBLE);
                                    errorText.setText("Error failed to save account details");
                                    Log.d(TAG, "buildloginsignup: failed to create new user please check internet");
                                });
                        });
                        buildloginsignup(3);
                    } else {
                        // If sign in fails, display a message to the user.
                        progressBar.setVisibility(View.GONE);
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText(task.getException().getMessage());
                    }
                });


    }

    private void FirebaseEmailSignIn(String email, String password, TextView errorText ) {
        Log.d(TAG, "FirebaseEmailSignIn: ");
        if (email != null && password != null) {
            if (email.length() > 0 && password.length() > 0) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithEmail:success");
                                currentUser = task.getResult().getUser();
                                if (!currentUser.isEmailVerified()) {
                                    buildloginsignup(3, true);
                                } else {
                                    db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener((OnCompleteListener<DocumentSnapshot>) task1 -> {
                                        Log.d(TAG, "onComplete: ");
                                        loginDialogView.findViewById(R.id.indeterminateBar).setVisibility(View.GONE);
                                        if (task1.isSuccessful()) {
                                            loginDialogView.findViewById(R.id.indeterminateBar).setVisibility(View.GONE);
                                            if (task1.getResult().get("pin") == null ) {
                                                buildloginsignup(4, false);
                                                return;
                                            }
                                            getNearbyManager().myName = (String) task1.getResult().get("name");
                                            getNameView().setText((String) task1.getResult().get("name"));
                                            Log.d(TAG, "onComplete: name found: " + (String) task1.getResult().get("name"));
                                            setContentView(leadmeAnimator);
                                            loginAction(false);
                                        }
                                    });
                                }

                            } else {
                                loginDialogView.findViewById(R.id.indeterminateBar).setVisibility(View.GONE);
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                errorText.setVisibility(View.VISIBLE);
                                errorText.setText(task.getException().getMessage());
                            }
                        });
            }
        }
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
                scheduledExecutorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if(indeterminate!=null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    indeterminate.setVisibility(View.INVISIBLE);
                                }
                            });

                        }
                    }
                }, (long) Time, TimeUnit.MILLISECONDS);
            }
        }
    }

    int savedViewIndex=-1;
    public void setAndDisplayPinReset(int page){
        if(loginDialog!=null &&loginDialog.isShowing()) {
            loginDialog.dismiss();
        }
        View resetPinView = View.inflate(this, R.layout.c__forgot_pin, null);
        if(savedViewIndex==-1){
            savedViewIndex= leadmeAnimator.getDisplayedChild();
        }
        leadmeAnimator.addView(resetPinView);
        leadmeAnimator.setDisplayedChild(leadmeAnimator.getChildCount()-1);
        Button confirm = resetPinView.findViewById(R.id.pin_reset_confirm);
        Button cancel = resetPinView.findViewById(R.id.pin_reset_cancel);
        View pages[] = {resetPinView.findViewById(R.id.pin_reset_pass_view),resetPinView.findViewById(R.id.set_pin),resetPinView.findViewById(R.id.pin_reset_finish_view)};
        ProgressBar pBar = resetPinView.findViewById(R.id.pin_reset_spinner);
        pBar.setVisibility(View.INVISIBLE);
        switch(page){
            case 0:
                pages[0].setVisibility(View.VISIBLE);
                pages[1].setVisibility(View.GONE);
                pages[2].setVisibility(View.GONE);
                TextView error = resetPinView.findViewById(R.id.pin_reset_error);
                EditText Pass = resetPinView.findViewById(R.id.pin_reset_password);
                TextView forgotPass = resetPinView.findViewById(R.id.pin_reset_forgot_password);
                forgotPass.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showForgottenPassword(loginDialog);
                    }
                });
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Pass != null && Pass.getText().toString().length() > 0) {
                            setProgressSpinner(5000, pBar);
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), Pass.getText().toString());
                            user.reauthenticate(credential)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                setAndDisplayPinReset(1);
                                            } else {
                                                error.setVisibility(View.VISIBLE);
                                                pBar.setVisibility(View.INVISIBLE);
                                            }
                                        }
                                    });
                        }
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pBar.setVisibility(View.INVISIBLE);
                        leadmeAnimator.setDisplayedChild(savedViewIndex);
                        savedViewIndex=-1;
                        leadmeAnimator.removeView(resetPinView);
                    }
                });
                break;
            case 1:
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
                for (int i = 0; i < codes.length; i++) {
                    codes[i].addTextChangedListener(pinWatcher);
                    codes[i].setOnKeyListener(codeKeyListener);
                }
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        closeKeyboard();
                        String pin = "";
                        String confirmPin = "";
                        for (int i = 0; i < 8; i++) {
                            if (i < 4) {
                                pin += codes[i].getText().toString();
                            } else {
                                confirmPin += codes[i].getText().toString();
                            }
                        }
                        if(pin.equals(confirmPin)){
                            setProgressSpinner(5000, pBar);
                            Map<String, Object> userDet = new HashMap<>();
                            //progressBar.setVisibility(View.VISIBLE);
                            userDet.put("pin", Hasher.Companion.hash(pin, HashType.SHA_256));
                            db.collection("users").document(mAuth.getCurrentUser().getUid()).update(userDet).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        setAndDisplayPinReset(2);
                                    }else{
                                        pBar.setVisibility(View.INVISIBLE);
                                    }
                                }
                            });
                        }
                    }
                });
                cancel.setText("Close");
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        leadmeAnimator.setDisplayedChild(savedViewIndex);
                        savedViewIndex=-1;
                        leadmeAnimator.removeView(resetPinView);
                    }
                });
                break;
            case 2:
                pages[2].setVisibility(View.VISIBLE);
                pages[1].setVisibility(View.GONE);
                pages[0].setVisibility(View.GONE);
                confirm.setText("Finish");
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        leadmeAnimator.setDisplayedChild(savedViewIndex);
                        savedViewIndex=-1;
                        leadmeAnimator.removeView(resetPinView);
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        leadmeAnimator.setDisplayedChild(savedViewIndex);
                        savedViewIndex=-1;
                        leadmeAnimator.removeView(resetPinView);
                    }
                });
                break;
            default:

                leadmeAnimator.setDisplayedChild(savedViewIndex);
                savedViewIndex=-1;
                leadmeAnimator.removeView(resetPinView);
                break;
        }
    }


}

