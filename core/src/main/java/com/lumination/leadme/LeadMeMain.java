
package com.lumination.leadme;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.ViewSwitcher;

import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Set;


public class LeadMeMain extends FragmentActivity implements Handler.Callback, SensorEventListener {

    //tag for debugging
    static final String TAG = "LeadMe";
    Drawable leadmeIcon;

    private String teachercode = "1990";
    private boolean nameEntered = false;
    private boolean codeEntered = false;
    private boolean selectEveryone = false;

    //tag to indicate what incoming message holds
    static final String ACTION_TAG = "LumiAction";
    static final String APP_TAG = "LumiAppLaunch";
    static final String EXIT_TAG = "LumiExit";
    static final String RETURN_TAG = "LumiReturnToApp";
    static final String YOUR_ID_IS = "LumiYourID:";

    static final String LOCK_TAG = "LumiLock";
    static final String UNLOCK_TAG = "LumiUnlock";
    static final String BLACKOUT_TAG = "LumiBlackout";

    static final String AUTO_INSTALL = "LumiAutoInstall";

    static final String LAUNCH_URL = "LumiLaunch:";
    static final String LAUNCH_YT = "LumiYT:";
    static final String AUTO_INSTALL_FAILED = "LumiAutoInstallFail:";
    static final String AUTO_INSTALL_ATTEMPT = "LumiAutoInstallAttempt:";

    static final String LAUNCH_SUCCESS = "LumiSuccess:";

    public final int OVERLAY_ON = 0;
    public final int ACCESSIBILITY_ON = 1;
    public final int BLUETOOTH_ON = 2;
    public final int FINE_LOC_ON = 3;

    // The SensorManager gives us access to sensors on the device.
    public SensorManager mSensorManager;
    // The accelerometer sensor allows us to detect device movement for shake-to-advertise.
    private Sensor mAccelerometer;

    // Acceleration required to detect a shake. In multiples of Earth's gravity.
    private static final float SHAKE_THRESHOLD_GRAVITY = 2;

    protected WindowManager windowManager;
    private InputMethodManager imm;
    private EditText code1, code2, code3, code4;
    protected WindowManager.LayoutParams overlayParams;
    private RemoteDispatcherService remoteDispatcherService;
    protected View overlayView;

    //local variables for exitApp function only
    private long lastTap = 0;
    private Toast exitToast;

    public boolean studentLockOn = true; //students start locked
    public boolean autoInstallApps = false; //if true, missing apps on student devices get installed automatically

    //states to control set up sequence
    private boolean permissionsInitialised = false;

    //details about me to send to peers
    public boolean isGuide = false;
    public boolean isReadyToConnect = false;
    public boolean dialogShowing = false;

    private Handler handler = new Handler(this);
    private ViewAnimator leadmeAnimator;
    private ViewSwitcher leaderLearnerSwitcher;
    private boolean loggingInAsLeader = true;
    public String leaderName = "Leader";

    private int SWITCH_LEADER_INDEX = 0;
    private int SWITCH_LEARNER_INDEX = 1;

    private int ANIM_SPLASH_INDEX = 0;
    private int ANIM_START_SWITCH_INDEX = 1;
    private int ANIM_LEARNER_INDEX = 2;
    private int ANIM_LEADER_INDEX = 3;
    private int ANIM_APP_LAUNCH_INDEX = 4;
    private int ANIM_OPTIONS_INDEX = 5;

    AlertDialog warningDialog, loginDialog, appPushDialog;
    private AlertDialog confirmPushDialog;
    View waitingForLearners, appLauncherScreen;
    private View loginDialogView, confirmPushDialogView, appPushDialogView;
    private View mainLearner, mainLeader, optionsScreen;
    private TextView warningDialogMessage, learnerWaitingText;

    private GridView connectedStudentsView;

    public Context context;
    private ActivityManager activityManager;
    private PermissionManager permissionManager;
    private NearbyPeersManager nearbyManager;
    private WebManager webManager;
    private AppManager appLaunchAdapter;
    private FavouritesManager favouritesManager; //TODO shift to app manager
    private ConnectedLearnersAdapter connectedLearnersAdapter;
    private LeaderSelectAdapter leaderSelectAdapter;

    public TextView nameView;
    public Button readyBtn;

    ImageView currentTaskIcon;
    TextView currentTaskTitle;
    TextView currentTaskDescription;
    Button currentTaskLaunchBtn;
    String currentTaskPackageName;
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

    public final Point size = new Point();

    private long appLaunchTime = -1;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OVERLAY_ON:
                Log.d(TAG, "Returning from OVERLAY ON with " + resultCode);
                break;
            case ACCESSIBILITY_ON:
                Log.d(TAG, "Returning from ACCESS ON with " + resultCode);
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
            default:
                Log.d(TAG, "RETURNED FROM ?? " + resultCode);
                break;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.w(TAG, "In onStop");
        super.onStop();
        getNearbyManager().onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getNearbyManager().onStart();
    }


    public boolean handleMessage(Message msg) {
        Log.d(TAG, "Got a message! " + msg.what + ", " + msg.obj);
        return true;
    }

    public boolean handlePayload(byte[] payloadBytes) {

        //if it's an action, execute it
        if (remoteDispatcherService.readAction(payloadBytes)) {
            Log.d(TAG, "Incoming message was an action!");
            return true; //it was an action, we're done!
        }

        if (remoteDispatcherService.readBool(payloadBytes)) {
            Log.d(TAG, "Incoming message was a boolean request!");
            return true; //it was a boolean, we're done!
        }

        //if it's an app launch request, deploy it
        if (remoteDispatcherService.openApp(payloadBytes)) {
            Log.d(TAG, "Incoming message was an app launch request!");
            return true; //it was an app launch request, we're done!
        }

        return true;
    }

    public ConnectedLearnersAdapter getConnectedLearnersAdapter() {
        return connectedLearnersAdapter;
    }

    public NearbyPeersManager getNearbyManager() {
        if (nearbyManager == null) {
            nearbyManager = new NearbyPeersManager(this);
        }
        return nearbyManager;
    }

    public LeaderSelectAdapter getLeaderSelectAdapter() {
        if (leaderSelectAdapter == null) {
            leaderSelectAdapter = new LeaderSelectAdapter(this);
        }
        return leaderSelectAdapter;
    }

    public AppManager getAppManager() {
        //this exists in the main layout, need to get a reference to it
        if (appLaunchAdapter == null) {
            appLaunchAdapter = new AppManager(this);
        }
        return appLaunchAdapter;
    }

    public FavouritesManager getFavouritesManager() {
        if (favouritesManager == null) {
            favouritesManager = new FavouritesManager(this, null, FavouritesManager.FAVTYPE_APP, 4);
        }
        return favouritesManager;
    }

    public WebManager getWebManager() {
        if (webManager == null) {
            webManager = new WebManager(this);
        }
        return webManager;
    }

    @Override
    public void onResume() {
        super.onResume();

        closeKeyboard();
        hideSystemUI();

        if (!permissionsInitialised || !permissionManager.workThroughPermissions()) {
            permissionsInitialised = true;

        } else if (permissionManager.workThroughPermissions()) {
            startOverlayAndServices();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (overlayView != null && getNearbyManager().isConnectedAsFollower() && studentLockOn) {
            overlayView.setVisibility(View.VISIBLE);
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
        }

        if (!getConnectedLearnersAdapter().someoneIsSelected()) {
            //if no-one is selected, prompt to push to everyone
            appPushBtn.setText(getResources().getString(R.string.push_this_to_everyone));
        } else {
            //if someone is selected, prompt to push to selected
            appPushBtn.setText(getResources().getString(R.string.push_this_to_selected));
        }

        //display push
        if (appPushDialog == null) {
            appPushDialog = new AlertDialog.Builder(this)
                    .setView(appPushDialogView)
                    .show();
        } else {
            appPushDialog.show();
        }
    }

    private void hideAppPushDialogView() {
        if (appPushDialog != null) {
            appPushDialog.hide();
        }
    }

    private void setupLoginDialog() {
        if (code1 == null) {
            code1 = loginDialogView.findViewById(R.id.codeInput1);
            code2 = loginDialogView.findViewById(R.id.codeInput2);
            code3 = loginDialogView.findViewById(R.id.codeInput3);
            code4 = loginDialogView.findViewById(R.id.codeInput4);
            readyBtn = loginDialogView.findViewById(R.id.connect_btn);
        }

        loginDialogView.findViewById(R.id.close_login_alert_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!codeEntered) {
                    code1.getText().clear();
                    code2.getText().clear();
                    code3.getText().clear();
                    code4.getText().clear();
                }
                if (nameView.getText().toString().trim().length() == 0) {
                    nameView.requestFocus();
                } else {
                    code1.requestFocus();
                }
                loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.VISIBLE);
                loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.GONE);
                openKeyboard();
                hideSystemUI();
            }
        });

        if (readyBtn != null) {
            readyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
                        initiateLeaderAdvertising();
                        hideLoginDialog();
                    } else {
                        getNearbyManager().connectToSelectedLeader();
                        hideLoginDialog();
                        showWaitingForConnectDialog();
                    }
                }
            });
        }


        View.OnKeyListener codeKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //View focus = null;
                if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_BACK) {
                    code1.getText().clear();
                    code2.getText().clear();
                    code3.getText().clear();
                    code4.getText().clear();
                    code1.requestFocus();
                }
//                Log.d(TAG, "Focus? " + focus);
//                if (focus != null) {
//                    focus.requestFocus();
//                }
                return false; //true if event consumed, false otherwise
            }
        };


        code1.setOnKeyListener(codeKeyListener);
        code2.setOnKeyListener(codeKeyListener);
        code3.setOnKeyListener(codeKeyListener);
        code4.setOnKeyListener(codeKeyListener);

        code1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() == 1) {
                    code2.requestFocus();
                } else if (s != null && s.length() == 0) {
                    nameView.requestFocus();
                }
            }
        });

        code2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() == 1) {
                    code3.requestFocus();

                } else if (s != null && s.length() == 0) {
                    code1.requestFocus();
                }
            }
        });

        code3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() == 1) {
                    code4.requestFocus();

                } else if (s != null && s.length() == 0) {
                    code2.requestFocus();
                }
            }
        });

    }

    void showLoginDialog() {
        Log.d(TAG, "Showing login dialog");
        stopShakeDetection();

        //set appropriate mode
        if (leaderLearnerSwitcher.getDisplayedChild() == SWITCH_LEADER_INDEX) {
            //leader
            loginDialogView.findViewById(R.id.code_entry_view).setVisibility(View.VISIBLE);
        } else {
            //learner
            loginDialogView.findViewById(R.id.code_entry_view).setVisibility(View.GONE);
        }

        if (loginDialog == null) {
            loginDialog = new AlertDialog.Builder(this)
                    .setView(loginDialogView)
                    .create();
        }

        if (loginDialog == null) {
            return;
        }
        loginDialog.show();
        nameView.requestFocus();
        openKeyboard();
        hideSystemUI();
    }

    private void hideLoginDialog() {
        Log.d(TAG, "Hiding dialog box");
        closeKeyboard();
        hideSystemUI();
        if (loginDialog != null) {
            loginDialog.hide();
            startShakeDetection();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not needed
    }

    private void moveAwayFromSplashScreen() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEADER_INDEX);
                leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);
                init = true;
                startShakeDetection();
            }
        }, 2000);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && !init) {
            //TODO call     mSensorManager.unregisterListener(this);
            moveAwayFromSplashScreen();
        }

        hideSystemUI();
        collapseStatusNow();

        if (hasFocus && appToast != null) {
            appToast.cancel();
            appToast = null;
        }

        //if there's an app to launch, do it
        if (hasFocus) {
            Log.d(TAG, "Focus is back! Launching delayed stuff.");

            //sometimes delayed things are stored here
            getRemoteDispatchService().launchDelayedApp();

            //sometimes they're here, so check both
            if (appIntentOnFocus != null) {
                startActivity(appIntentOnFocus);
                appIntentOnFocus = null;
            }
        }
    }

    public RemoteDispatcherService getRemoteDispatchService() {
        if (remoteDispatcherService == null) {
            remoteDispatcherService = RemoteDispatcherService.getInstance(this);
            Log.d(TAG, "Dispatch service: " + remoteDispatcherService);
        }
        return remoteDispatcherService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "In onDestroy");

        //clean up nearby connections
        getNearbyManager().onStop();
        getNearbyManager().stopAdvertising();
        getNearbyManager().disconnectFromAllEndpoints();

        //remove the overlay if necessary
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }

        //clean up link preview assets
        getWebManager().textCrawler.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //returns here after requesting location permission
        if (permissionManager.workThroughPermissions()) {
            Log.d(TAG, "Setting P2P and Overlay from PermissionResult");
            startOverlayAndServices();
        } else {
            Log.d(TAG, "RETURNED! Got: " + requestCode + ", " + permissions + ", " + grantResults);
        }
    }

    public void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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

        collapseStatusNow();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar); //switches from splash screen to main
        //https://android.jlelse.eu/the-complete-android-splash-screen-guide-c7db82bce565

        super.onCreate(savedInstanceState);
        appLaunchTime = System.currentTimeMillis();

        try {
            leadmeIcon = getPackageManager().getApplicationIcon(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //killAllBackgroundProcesses(); //start with a fresh slate

        //set up shake detection
        mSensorManager = (SensorManager) getSystemService(LeadMeMain.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        permissionManager = new PermissionManager(this);

        context = getApplicationContext();
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
        View startLeader = View.inflate(context, R.layout.b__start_leader, null);
        View startLearner = View.inflate(context, R.layout.b__start_learner, null);
        mainLearner = View.inflate(context, R.layout.c__learner_main, null);
        mainLeader = View.inflate(context, R.layout.c__leader_main, null);
        optionsScreen = View.inflate(context, R.layout.d__options_menu, null);
        appLauncherScreen = View.inflate(context, R.layout.d__app_list, null);
        learnerWaitingText = startLearner.findViewById(R.id.waiting_text);

        //add adapters for lists and grids
        getRemoteDispatchService();
        ((GridView) appLauncherScreen.findViewById(R.id.app_list_grid)).setAdapter(getAppManager());
        ((GridView) appLauncherScreen.findViewById(R.id.fav_list_grid)).setAdapter(getFavouritesManager());
        ((ListView) startLearner.findViewById(R.id.leader_list_view)).setAdapter(getLeaderSelectAdapter());

        ((Button) mainLeader.findViewById(R.id.url_core_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWebManager().showWebLaunchDialog(false);
            }
        });

        ((Button) mainLeader.findViewById(R.id.yt_core_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWebManager().showWebLaunchDialog(false);
            }
        });

        Button app_btn = ((Button) mainLeader.findViewById(R.id.app_core_btn));
        app_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppLaunchScreen();
            }
        });

        connectedLearnersAdapter = new ConnectedLearnersAdapter(this, new ArrayList<ConnectedPeer>());
        connectedStudentsView = (GridView) mainLeader.findViewById(R.id.studentListView);
        connectedStudentsView.setAdapter(connectedLearnersAdapter);

        //set up return to app button
        mainLeader.findViewById(R.id.leadme_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGuide) {
                    showRecallDialog();
                }
            }
        });

        //set up select/deselect all button
        mainLeader.findViewById(R.id.toggle_all_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //select or deselect all buddies
                selectEveryone = !selectEveryone;
                connectedLearnersAdapter.selectAllPeers(selectEveryone);
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

        leadmeAnimator.setDisplayedChild(ANIM_SPLASH_INDEX);

        setContentView(leadmeAnimator);

        //set up menu buttons
        View.OnClickListener menuListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leadmeAnimator.setDisplayedChild(ANIM_OPTIONS_INDEX);
            }
        };

        startLeader.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        startLearner.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        mainLeader.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        mainLearner.findViewById(R.id.menu_btn).setOnClickListener(menuListener);
        appLauncherScreen.findViewById(R.id.menu_btn).setOnClickListener(menuListener);

        //set up back buttons
        appLauncherScreen.findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
            }
        });

        //set up options screen
        optionsScreen.findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGuide) {
                    leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
                } else {
                    leadmeAnimator.setDisplayedChild(ANIM_LEARNER_INDEX);
                }
            }
        });

        optionsScreen.findViewById(R.id.how_to_use_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        optionsScreen.findViewById(R.id.help_support_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        optionsScreen.findViewById(R.id.logout_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGuide || !getNearbyManager().isConnectedAsFollower()) {
                    isGuide = false;
                    getNearbyManager().stopDiscovering();
                    getNearbyManager().stopAdvertising();
                    getNearbyManager().disconnectFromAllEndpoints(); //disconnect everyone
                    getLeaderSelectAdapter().setLeaderList(new ArrayList<ConnectedPeer>()); //empty the list
                    leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEADER_INDEX);
                    leadmeAnimator.setDisplayedChild(ANIM_SPLASH_INDEX);
                    moveAwayFromSplashScreen();

                    //look for people to connect to
                    getNearbyManager().startDiscovering();
                } else {
                    Toast.makeText(getApplicationContext(), "Logout is unavailable.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //prepare elements for connection dialog
        final Button leader_toggle = switcherView.findViewById(R.id.leader_btn);
        final Button learner_toggle = switcherView.findViewById(R.id.learner_btn);

        leader_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loggingInAsLeader = true;
                leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEADER_INDEX);
                leader_toggle.setBackground(getResources().getDrawable(R.drawable.bg_active_right_leader, null));
                learner_toggle.setBackground(getResources().getDrawable(R.drawable.bg_passive_left_white, null));
            }
        });

        learner_toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loggingInAsLeader = false;
                leaderLearnerSwitcher.setDisplayedChild(SWITCH_LEARNER_INDEX);
                leader_toggle.setBackground(getResources().getDrawable(R.drawable.bg_passive_right_white, null));
                learner_toggle.setBackground(getResources().getDrawable(R.drawable.bg_active_left_learner, null));
            }
        });

        //prepare elements for login dialog
        loginDialogView = View.inflate(context, R.layout.b__login_popup, null);
        nameView = loginDialogView.findViewById(R.id.name_input_field);

        ((Button) startLeader.findViewById(R.id.app_login)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });

        ((Button) loginDialogView.findViewById(R.id.back_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideLoginDialog();
            }
        });

        ((Button) loginDialogView.findViewById(R.id.connect_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateLeaderAdvertising();
            }
        });

        setupLoginDialog();

        //prepare elements for app push dialog
        appPushDialogView = View.inflate(context, R.layout.e__preview_app_push, null);

        ((Button) appPushDialogView.findViewById(R.id.push_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appLaunchAdapter.launchApp(appPushPackageName, appPushTitle, false);
                Log.d(TAG, "LAUNCHING! " + appPushPackageName);
                hideAppPushDialogView();
                showConfirmPushDialog(true, false);
            }
        });

        ((Button) appPushDialogView.findViewById(R.id.back_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideAppPushDialogView();
            }
        });

        //prepare elements for leader main view
        waitingForLearners = mainLeader.findViewById(R.id.no_students_connected);
        appLauncherScreen = View.inflate(context, R.layout.d__app_list, null);
        confirmPushDialogView = View.inflate(context, R.layout.e__confirm_popup, null);

        ((Button) confirmPushDialogView.findViewById(R.id.ok_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideConfirmPushDialog();
            }
        });


        //set up options screen
        CheckBox autoinstall_checkbox = (CheckBox) optionsScreen.findViewById(R.id.auto_install_checkbox);
        autoinstall_checkbox.setChecked(autoInstallApps); //toggle the checkbox
        autoinstall_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoInstallApps = isChecked;
                Log.d(TAG, "Changed detected! Auto install is now " + autoInstallApps);
                Toast toast;
                if (autoInstallApps) {
                    toast = Toast.makeText(getApplicationContext(), "Missing apps will be installed automatically on student devices.", Toast.LENGTH_SHORT);
                } else {
                    toast = Toast.makeText(getApplicationContext(), "Missing apps will NOT be installed on student devices.", Toast.LENGTH_SHORT);
                }
                toast.show();
                getRemoteDispatchService().sendBoolToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL, autoInstallApps, getNearbyManager().getSelectedPeerIDs());
            }
        });

        //check that all required permissions have been granted
        if (permissionManager.workThroughPermissions()) {
            //all settings are on!
            Log.d(TAG, "Setting P2P and Overlay from WorkThroughPermissions");
            startOverlayAndServices();
        }

        setUpControlButtons();

        //start this
        isReadyToConnect = true;
        getForegroundActivity();
        initiateLeaderDiscovery();

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
     *
     * @param isApp
     */
    public void showConfirmPushDialog(boolean isApp, boolean isSavedOnly) {
        //TODO include display a message if errors occur

        if (confirmPushDialog == null) {
            confirmPushDialog = new AlertDialog.Builder(this)
                    .setView(confirmPushDialogView)
                    .show();
        } else {
            confirmPushDialog.show();
        }

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
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideConfirmPushDialog();
            }
        }, 1500);

    }

    public void openKeyboard() {
        View view = this.getCurrentFocus();
        //Log.d(TAG, "Open keyboard! " + view);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        //Log.d(TAG, "Close keyboard! " + view);

        // Check if no view has focus:
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    public void hideConfirmPushDialog() {
        if (confirmPushDialog != null) {
            confirmPushDialog.hide();
        }
        //return to main screen
        leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);
    }

    public void showWarningDialog(String message) {
        if (warningDialog == null) {
            setupWarningDialog();
        }
        warningDialogMessage.setText(message);
        warningDialogMessage.setVisibility(View.VISIBLE);
        warningDialog.show();
        hideSystemUI();
        collapseStatusNow();
    }

    protected void closeWaitingDialog() {
        if (waitingDialog != null) {
            waitingDialog.hide();
        }
    }

    private AlertDialog waitingDialog = null;

    private void showWaitingForConnectDialog() {
        if (waitingDialog == null) {
            View waitingDialogView = View.inflate(context, R.layout.e__waiting_to_connect, null);
            Button backBtn = waitingDialogView.findViewById(R.id.back_btn);
            backBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    waitingDialog.hide();
                    getNearbyManager().cancelConnection();
                }
            });

            waitingDialog = new AlertDialog.Builder(this)
                    .setView(waitingDialogView)
                    .create();
        }

        waitingDialog.show();
    }

    private void setupWarningDialog() {
        View warningDialogView = View.inflate(context, R.layout.e__warning_popup, null);
        warningDialogMessage = warningDialogView.findViewById(R.id.warning_comment);
        Button okBtn = warningDialogView.findViewById(R.id.ok_btn);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                warningDialog.hide();
                warningDialogMessage.setVisibility(View.GONE);
            }
        });

        warningDialog = new AlertDialog.Builder(this)
                .setView(warningDialogView)
                .create();
    }

    public void showLoginAlertMessage() {
        loginDialogView.findViewById(R.id.name_code_entry_view).setVisibility(View.GONE);
        loginDialogView.findViewById(R.id.wrong_code_view).setVisibility(View.VISIBLE);
        closeKeyboard();
        hideSystemUI();
    }

    public void initiateLeaderDiscovery() {
        getNearbyManager().discoverLeaders();
    }

    public void initiateLeaderAdvertising() {
        //reset error messages
        loginDialogView.findViewById(R.id.no_name_message).setVisibility(View.GONE);
        loginDialogView.findViewById(R.id.wrong_code_message).setVisibility(View.GONE);

        //check that a name has been entered
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
            String code = "" + code1.getText() + code2.getText() + code3.getText() + code4.getText();
            Log.d(TAG, "Code entered: " + code);
            if (code.equals(teachercode)) { //correct code
                codeEntered = true;
            } else { //incorrect code
                codeEntered = false;
                loginDialogView.findViewById(R.id.wrong_code_message).setVisibility(View.VISIBLE);
            }
        } else {
            codeEntered = true; //mark as true, since we don't need one
        }

        if (!nameEntered || !codeEntered) {
            //alert to errors and exit
            showLoginAlertMessage();
            return;

        } else if (loggingInAsLeader) {
            getNearbyManager().setAsGuide();
        }

        loginAction();
    }

    private void loginAction() {
        hideLoginDialog();

        //re-check permissions
        permissionManager.workThroughPermissions();

        String name = getNearbyManager().getName();
        Log.d(TAG, "Your name is " + name + ", are you a guide? " + isGuide);

        ((TextView) optionsScreen.findViewById(R.id.student_name)).setText(name);
        if (isGuide) {
            //display main guide view
            leadmeAnimator.setDisplayedChild(ANIM_LEADER_INDEX);

            //update options
            optionsScreen.findViewById(R.id.settings_title).setVisibility(View.VISIBLE);
            optionsScreen.findViewById(R.id.auto_install_checkbox).setVisibility(View.VISIBLE);
            ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.light, null));
            TextView title = ((TextView) leadmeAnimator.getCurrentView().findViewById(R.id.leader_title));
            title.setText(name);
            ((TextView) optionsScreen.findViewById(R.id.connected_as_learner)).setText(getResources().getText(R.string.leader));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_learner)).setTextColor(getResources().getColor(R.color.accent, null));

        } else {
            //display main student view
            leadmeAnimator.setDisplayedChild(ANIM_LEARNER_INDEX);

            //update options
            optionsScreen.findViewById(R.id.settings_title).setVisibility(View.GONE);
            optionsScreen.findViewById(R.id.auto_install_checkbox).setVisibility(View.GONE);
            ((TextView) optionsScreen.findViewById(R.id.logout_btn)).setTextColor(getResources().getColor(R.color.leadme_medium_grey, null));
            TextView title = ((TextView) leadmeAnimator.getCurrentView().findViewById(R.id.learner_title));
            title.setText(name);
            ((TextView) optionsScreen.findViewById(R.id.connected_as_learner)).setText(getResources().getText(R.string.learner));
            ((TextView) optionsScreen.findViewById(R.id.connected_as_learner)).setTextColor(getResources().getColor(R.color.medium, null));
        }

        //getNearbyManager().onStart();

    }

    public void getForegroundActivity() {
        String packageName = "";
        String className = "";
        long _begTime = appLaunchTime;
        long _endTime = System.currentTimeMillis();
        appLaunchTime = _endTime; //update so we're only looking at history since last check

        UsageStatsManager usageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);

        if (usageStatsManager != null) {
            UsageEvents queryEvents = usageStatsManager.queryEvents(_begTime, _endTime);

            if (queryEvents != null) {
                UsageEvents.Event event = new UsageEvents.Event();

                Log.d(TAG, queryEvents.toString());
                Log.d(TAG, event.toString());

                while (queryEvents.hasNextEvent()) {
                    UsageEvents.Event eventAux = new UsageEvents.Event();
                    queryEvents.getNextEvent(eventAux);

                    Log.d(TAG, "EV: " + eventAux.toString());
                    if (eventAux.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        event = eventAux;
                    }
                }

                packageName = event.getPackageName();
                className = event.getClassName();
                Log.i(TAG, "CURRENTLY: " + packageName + ", " + className);
            }
        }
    }

    private void setUpControlButtons() {
        //TODO reimplement this as needed

        mainLeader.findViewById(R.id.unlock_selected_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //toggle and set
                unlockFromMainAction();
            }
        });

        mainLeader.findViewById(R.id.lock_selected_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //toggle and set
                lockFromMainAction();
            }
        });

        mainLeader.findViewById(R.id.block_selected_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //toggle and set
                blackoutFromMainAction();
            }
        });
    }

    private void startOverlayAndServices() {
        if (!permissionsInitialised) {
            //haven't finished setting up yet
            return;
        }

        initialiseBlockingOverlay();
        overlayView.setVisibility(View.INVISIBLE); //default is hidden behind main app
    }


    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
    }


    private void initialiseBlockingOverlay() {
        //overlay is already running
        if (overlayView != null) {
            refreshOverlay();
            return;
        }

        //initialise window manager for shared use
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        //retrieve a reference to the overlay view in the main layout
        LinearLayout parentLayout = findViewById(R.id.c__leader_main);
        overlayView = LayoutInflater.from(context).inflate(R.layout.transparent_overlay, parentLayout, false);
        overlayView.findViewById(R.id.blocking_view).setVisibility(View.GONE); //default is this should be hidden

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if (getNearbyManager().isConnectedAsFollower()) {
                            hideSystemUI(); //hide immediately
                        } else {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    hideSystemUI(); //hide after short delay
                                }
                            }, 1500);
                        }
                    }
                });


        //prepare layout parameters so overlay fills whole screen
        calcParams();

        //add overlay to the window manager
        windowManager.addView(overlayView, overlayParams);
        windowManager.updateViewLayout(overlayView, overlayParams);
    }

    public void refreshOverlay() {
        calcParams();
        if (overlayView != null) {
            windowManager.updateViewLayout(overlayView, overlayParams);
        }
    }

    private void calcParams() {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // APPLICATION_OVERLAY FOR ANDROID 26+ AS THE PREVIOUS VERSION RAISES ERRORS
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // FOR PREVIOUS VERSIONS USE TYPE_PHONE AS THE NEW VERSION IS NOT SUPPORTED
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        Display display = getWindowManager().getDefaultDisplay();
        display.getRealSize(size);

        int OTHER_FLAGS = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //OTHER_FLAGS += WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE; //passes touch through - non-blocking

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                OTHER_FLAGS,
                PixelFormat.TRANSLUCENT);

        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.x = 0;
        overlayParams.y = 0;
        overlayParams.width = size.x;
        overlayParams.height = size.y;

    }

    public void MuteAudio() {
        AudioManager mAudioManager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        } else {
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }

    public void UnMuteAudio() {
        AudioManager mAudioManager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
        } else {
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        }
    }

    public void blackout(boolean on) {
        if (overlayView == null) {
            initialiseBlockingOverlay();
        }

        View blocking_view = overlayView.findViewById(R.id.blocking_view);

        if (on) {
            blocking_view.setVisibility(View.VISIBLE);
            MuteAudio();
        } else {
            blocking_view.setVisibility(View.GONE);
            UnMuteAudio();
        }
    }

    public void setStudentLock(int status) {
        studentLockOn = (status == ConnectedPeer.STATUS_BLACKOUT || status == ConnectedPeer.STATUS_LOCK);

        Log.d(TAG, "Is locked? " + studentLockOn);
        String statusMsg = "";

        if (isGuide) {
            statusMsg = "Students are now in ";
        } else {
            statusMsg = "You are now in ";
        }

        switch (status) {
            case ConnectedPeer.STATUS_LOCK:
                overlayView.setVisibility(View.VISIBLE);
                statusMsg += "FOLLOW mode.";
                break;
            case ConnectedPeer.STATUS_BLACKOUT:
                overlayView.setVisibility(View.VISIBLE);
                statusMsg += "BLOCKED mode.";
                break;
            case ConnectedPeer.STATUS_UNLOCK:
                overlayView.setVisibility(View.INVISIBLE);
                statusMsg += "FREE PLAY mode.";
                break;
            default:
                Log.e(TAG, "Invalid status: " + status);
                return; //invalid status
        }

        Toast studentStatus = Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT);
        studentStatus.show();
    }

    public void updateFollowerCurrentTask(String packageName, String appName, String taskType, String url) {
        try {
            //initialise everything
            if (currentTaskIcon == null) {
                currentTaskIcon = mainLearner.findViewById(R.id.current_task_icon);
                currentTaskTitle = mainLearner.findViewById(R.id.current_task_name);
                currentTaskDescription = mainLearner.findViewById(R.id.current_task_desc);
                currentTaskLaunchBtn = mainLearner.findViewById(R.id.launch_btn);
                currentTaskIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));

                currentTaskLaunchBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Clicking launch! " + currentTaskPackageName);
                        try {
                            appLaunchAdapter.relaunchLast(currentTaskPackageName, currentTaskName, currentTaskType, currentTaskURL);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            currentTaskPackageName = packageName;
            currentTaskName = appName;
            currentTaskURL = url;
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

        if (recallPrompt == null) {
            View recallView = View.inflate(context, R.layout.e__recall_confirm_popup, null);
            recallMessage = recallView.findViewById(R.id.recall_comment);
            toggleBtnView = recallView.findViewById(R.id.toggleBtnView);
            selectedBtn = recallView.findViewById(R.id.selected_btn);
            everyoneBtn = recallView.findViewById(R.id.everyone_btn);

            recallView.findViewById(R.id.ok_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    returnToAppFromMainAction(returnEveryone);
                    recallPrompt.hide();
                }
            });

            recallView.findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recallPrompt.hide();
                }
            });

            recallView.findViewById(R.id.selected_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makeSelectedBtnActive();
                }
            });

            recallView.findViewById(R.id.everyone_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    makeEveryoneBtnActive();
                }
            });

            recallPrompt = new AlertDialog.Builder(this)
                    .setView(recallView)
                    .create();
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
    }

    private void makeSelectedBtnActive() {
        returnEveryone = false;
        selectedBtn.setBackground(getResources().getDrawable(R.drawable.bg_active_right, null));
        everyoneBtn.setBackground(getResources().getDrawable(R.drawable.bg_passive_left, null));
        selectedBtn.setTextColor(getResources().getColor(R.color.leadme_light_grey, null));
        everyoneBtn.setTextColor(getResources().getColor(R.color.light, null));
    }

    private void makeEveryoneBtnActive() {
        returnEveryone = true;
        selectedBtn.setBackground(getResources().getDrawable(R.drawable.bg_passive_right, null));
        everyoneBtn.setBackground(getResources().getDrawable(R.drawable.bg_active_left, null));

        everyoneBtn.setTextColor(getResources().getColor(R.color.leadme_light_grey, null));
        selectedBtn.setTextColor(getResources().getColor(R.color.light, null));
    }

//    public void returnToAppAction() {
//        Log.d(TAG, "Current focus is: " + getCurrentFocus());
//        if (!hasWindowFocus()) {
//            Log.d(TAG, "Returning to app!");
//            //TODO if calling this after launching a 3rd party app from within Lead Me, it works perfectly
//            //TODO if calling this after minimising Lead Me, it can take multiple taps to return to the app - weird behaviour?
//            recallToLeadMe();
//        }
//    }

    public void recallToLeadMe() {
        collapseStatusNow();
        //activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
        if (hasWindowFocus()) {
            Log.d(TAG, "Already in LeadMe!");
            return;
        }
        Log.d(TAG, "Recalling to LeadMe!");
        getRemoteDispatchService().bringMainToFront();

        Intent intent = new Intent(this, LeadMeMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);

        if (!isGuide) {
            updateFollowerCurrentTask(getPackageName(), getResources().getString(R.string.app_title), "Application", "");
        }

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
        getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LOCK_TAG, chosen);
    }

    public void unlockFromMainAction() {
        Set<String> chosen;
        if (getConnectedLearnersAdapter().someoneIsSelected()) {
            chosen = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosen = getNearbyManager().getAllPeerIDs();
        }
        getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.UNLOCK_TAG, chosen);
    }

    public void blackoutFromMainAction() {
        Set<String> chosen;
        if (getConnectedLearnersAdapter().someoneIsSelected()) {
            chosen = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosen = getNearbyManager().getAllPeerIDs();
        }
        getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.BLACKOUT_TAG, chosen);
    }

    //helper function, returns only selected learners
//    public void returnToAppFromMainAction() {
//        returnToAppFromMainAction(false);
//    }

    //main function, can return everyone or only selected learners
    public void returnToAppFromMainAction(boolean returnEveryone) {
        Log.d(TAG, "Returning to app from MAIN! " + hasWindowFocus());
        //String connections = getNearbyManager().getSelectedPeerIDsAsString();

        Set<String> chosenPeers;
        if (!returnEveryone && getConnectedLearnersAdapter().someoneIsSelected()) {
            chosenPeers = getNearbyManager().getSelectedPeerIDs();
        } else {
            chosenPeers = getNearbyManager().getAllPeerIDs();
        }
        Log.i(TAG, "Returning " + chosenPeers.size() + " learners to LeadMe (" + chosenPeers.toString() + ") vs me: " + getNearbyManager().getID());
        getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.RETURN_TAG, chosenPeers);

        if (appToast == null) {
            returningToApp = true;
            appToast = Toast.makeText(context, "Returning selected followers to Lumination Lead Me app", Toast.LENGTH_SHORT);
            appToast.show();
        }
    }

//    public void returnToAppFromSettings() {
//        Log.d(TAG, "Returning to app from SETTINGS! "+hasWindowFocus());
////        if (!hasWindowFocus()) {
////            handler.post(new Runnable() {
////                @Override
////                public void run() {
////                    recallToLeadMe();
////                }
////            });
////        }
//
//        Intent intent = new Intent(context, LeadMeMain.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//        intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//    }

    public void displayLearnerMain(String leaderName) {
        TextView leaderTitle = mainLearner.findViewById(R.id.leader_name);
        leaderTitle.setText(leaderName);

        TextView learnerTitle = mainLearner.findViewById(R.id.learner_title);
        learnerTitle.setText(getNearbyManager().getName());
    }

    public void setUIDisconnected() {
        //reset state
        isReadyToConnect = false; //need to press button first

        //reset views
        showConnectedStudents(false);

        if (overlayView != null) {
            overlayView.setVisibility(View.INVISIBLE);
        }

        readyBtn.setEnabled(true);
        if (isGuide) {
            waitingForLearners.setVisibility(View.GONE);
        }

        //make sure everything is stopped
        getNearbyManager().onStop();
        getNearbyManager().disconnectFromAllEndpoints();
        getNearbyManager().stopAdvertising();

        leadmeAnimator.setDisplayedChild(ANIM_START_SWITCH_INDEX);
    }

    public void collapseStatusNow() {
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(closeDialog);
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
        if (android.os.Build.VERSION.SDK_INT >= 19 && !isTaskRoot() && mIsRestoredToTop) {
            // 4.4.2 platform issues for FLAG_ACTIVITY_REORDER_TO_FRONT,
            // reordered activity back press will go to home unexpectedly,
            // Workaround: move reordered activity current task to front when it's finished.
            ActivityManager tasksManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            tasksManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }


    private boolean layoutTouchInit = false;

    protected void setupLayoutTouchListener() {
        if (layoutTouchInit) {
            return;
        }

        Log.d(TAG, getRemoteDispatchService() + "" + getRemoteDispatchService().getBaseContext());
        FrameLayout layout = new FrameLayout(this); //we want to detect things via the service

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;

        if (windowManager != null) {
            layoutTouchInit = true;
            layout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "TEST: " + event.getY());
                    if (getNearbyManager().isConnectedAsFollower() && event.getY() == 0) {
                        Log.d(TAG, "STATUS OR NAV TOUCH!! " + event.getAction());
                        collapseStatusNow();

                        //main.getForegroundActivity();

                        //hide status bar
                        //check if target app still in front
                        //IF NOT alert teacher and bring target app back to the front

                        return true; //consume event
                    }

                    return false; //pass event on
                }

            });
        }
    }
}
