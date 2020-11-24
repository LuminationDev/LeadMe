
package com.lumination.leadme;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends FragmentActivity implements Handler.Callback {

    //tag for debugging
    static final String TAG = "LuminationRemote";

    //tag to indicate what incoming message holds
    static final String ACTION_TAG = "LumiAction";
    static final String APP_TAG = "LumiAppLaunch";
    static final String EXIT_TAG = "LumiExit";
    static final String RETURN_TAG = "LumiReturnToApp";
    static final String RETURN_CHOSEN_TAG = "LumiReturnToAppChosen:";
    static final String LOCK_CHOSEN_TAG = "LumiStudentLockChosen:";
    static final String UNLOCK_CHOSEN_TAG = "LumiStudentUnlockChosen:";
    static final String BLACKOUT_CHOSEN_TAG = "LumiStudentBlackoutChosen:";
    static final String YOUR_ID_IS = "LumiYourID:";

    static final String STUDENT_LOCK = "LumiStudentLock";
    static final String STUDENT_MENU = "LumiStudentMenu";
    static final String AUTO_INSTALL = "LumiAutoInstall";

    static final String LAUNCH_URL = "LumiLaunch:";
    static final String LAUNCH_YT = "LumiYT:";
    static final String AUTO_INSTALL_FAILED = "LumiAutoInstallFail:";
    static final String AUTO_INSTALL_ATTEMPT = "LumiAutoInstallAttempt:";

    static final String LAUNCH_SUCCESS = "LumiSuccess:";

    public final int OVERLAY_ON = 0;
    public final int ACCESSIBILITY_ON = 1;
    public final int BLUETOOTH_ON = 2;

    private RemoteAppLauncher appLauncher;
    private ConnectedStudentsFragment connectedStudentsFragment;

    protected WindowManager windowManager;
    private InputMethodManager imm;
    protected WindowManager.LayoutParams overlayParams;
    private RemoteDispatcherService remoteDispatcherService;
    protected View overlayView;

    //main app equivalents
    ImageView lockBtnMainView;
    ImageView unlockBtnMainView;
    ImageView returnBtnMainView;
    ImageView blackoutBtnMainView;
    ImageView whoBtn;

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
    public ProgressBar progress;

    public boolean launchingVR = false;

    public Context context;
    private ActivityManager activityManager;
    private PermissionManager permissionManager;
    public NearbyPeersManager nearbyManager;

    public TextView statusText;
    public View statusContainer;

    public Switch hostSwitch;
    public TextView nameView;
    public Button readyBtn;

    public final Point size = new Point();

    private long appLaunchTime = -1;

    public Handler getHandler() {
        return handler;
    }

    //////////
    // for status bar handling
    boolean currentFocus; // To keep track of activity's window focus
    boolean isPaused;  // To keep track of activity's foreground/background status
    Handler collapseNotificationHandler;
    /////////

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
            default:
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
        nearbyManager.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        nearbyManager.onStart();
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

    public ConnectedStudentsFragment getConnectedStudentsFragment() {
        //this is inserted into a blank spot in the main layout, so need to create it first
        if (connectedStudentsFragment == null) {
            connectedStudentsFragment = (ConnectedStudentsFragment) getSupportFragmentManager().findFragmentById(R.id.follower_layout);
            //connectedStudentsFragment = new ConnectedStudentsFragment();
            //getSupportFragmentManager().beginTransaction().add(R.id.container_root, connectedStudentsFragment, "connectedStudentsFrag").commit();

            Log.d(TAG, "Found connected students! " + connectedStudentsFragment);
        }
        return connectedStudentsFragment;
    }

    public RemoteAppLauncher getAppLauncher() {
        //this exists in the main layout, need to get a reference to it
        if (appLauncher == null) {
            appLauncher = (RemoteAppLauncher) getSupportFragmentManager().findFragmentById(R.id.frag_appList);
        }
        return appLauncher;
    }

    @Override
    public void onResume() {
        super.onResume();

        //hide keyboard
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        if (!permissionsInitialised || !permissionManager.workThroughPermissions()) {
            permissionsInitialised = true;

        } else if (permissionManager.workThroughPermissions()) {
            startOverlayAndServices();
        }

        //if there's an app to launch, do it
        if (hasWindowFocus()) {
            getRemoteDispatchService().launchDelayedApp();
        }


    }

    @Override
    public void onPause() {
        super.onPause();

        if (overlayView != null && nearbyManager.isConnectedAsFollower()) {
            //if (overlayView != null && (nearbyManager.isConnectedAsFollower() || nearbyManager.isConnectedAsGuide())) {
            overlayView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        nearbyManager.onBackPressed();
        Log.i(TAG, "On BACK!");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.i(TAG, "FOCUS CHANGE: " + hasFocus);

        hideSystemUI();
        collapseStatusNow();

        if (hasFocus && appToast != null) {
            appToast.cancel();
            appToast = null;
        }

        if (hasFocus && returningToApp) {
            returningToApp = false; //returned!
            if (isGuide) {
                showWhoToReturnDialog();
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
        nearbyManager.onStop();
        nearbyManager.stopAdvertising();
        nearbyManager.disconnectFromAllEndpoints();

        //remove the overlay if necessary
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //returns here after requesting location permission
        if (permissionManager.workThroughPermissions()) {
            Log.d(TAG, "Setting P2P and Overlay from PermissionResult");
            startOverlayAndServices();
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
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appLaunchTime = System.currentTimeMillis();

        //killAllBackgroundProcesses(); //start with a fresh slate

        nearbyManager = new NearbyPeersManager(this);
        permissionManager = new PermissionManager(this);

        context = getApplicationContext();
        activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();

        imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        try {
            setContentView(R.layout.main);

            findViewById(R.id.mainView).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "Touch!");
                    hideSystemUI();
                    return false;
                }
            });

            findViewById(R.id.follower_layout).setVisibility(View.GONE);

            /* hide views we don't want to see until connection is made */
            View appList = findViewById(R.id.frag_appList);
            View clientMain = findViewById(R.id.client_main);

            statusText = findViewById(R.id.statusView);
            statusContainer = findViewById(R.id.status_container);
            progress = (ProgressBar) findViewById(R.id.progressBar);

            progress.setVisibility(View.INVISIBLE);

            appList.setVisibility(View.GONE);
            clientMain.setVisibility(View.GONE);

            nameView = findViewById(R.id.nameInput);
            nameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {

                        //update the name and store it
                        String name = nearbyManager.getName();
                        Log.d(TAG, "Your name is now " + name);

                        //stop the cursor flashing
                        nameView.clearFocus();

                        //hide keyboard
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        return true;
                    }
                    return false;
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error at WiFiDirectActivity: " + e.getLocalizedMessage());
        }

        ImageView mainMenuBtn = findViewById(R.id.menuBtn);
        mainMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "MENU PRESS!");
                if (nearbyManager.isConnectedAsFollower()) {
                    Log.d(TAG, "Sorry, you're a student in follow mode. You can't access the menu right now.");
                    return;
                }
                PopupMenu popup = new PopupMenu(context, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.main_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch ((item.getItemId())) {

                            case (R.id.settings):
                                Log.d(TAG, "SETTINGS PRESS!");
                                //TODO
                                return true;

                            case (R.id.auto_install):
                                Log.d(TAG, "AUTO INSTALL PRESS!");

                                final CheckBox autoInstallApp = findViewById(R.id.auto_install);
                                autoInstallApp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                        Log.d(TAG, "Changed detected! Auto install is now " + isChecked);
                                        autoInstallApps = isChecked;

                                        Toast toast;
                                        if (autoInstallApps) {
                                            toast = Toast.makeText(getApplicationContext(), "Missing apps will be installed automatically on student devices.", Toast.LENGTH_SHORT);

                                        } else {
                                            toast = Toast.makeText(getApplicationContext(), "Missing apps will NOT be installed on student devices.", Toast.LENGTH_SHORT);
                                        }
                                        toast.show();

                                        getRemoteDispatchService().sendBool(MainActivity.ACTION_TAG, MainActivity.AUTO_INSTALL, isChecked);
                                    }
                                });


                                //TODO
                                return true;

                            case (R.id.exit_app):
                                Log.d(TAG, "EXIT PRESS!");
                                exitAppImmediately();
                                return true;

                            default: //nothing clicked
                                return false;
                        }
                    }
                });

                popup.show();
            }
        });


        readyBtn = findViewById(R.id.readyBtn);
        readyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReadyBtnAction();
            }
        });

        hostSwitch = findViewById(R.id.hostSwitch);
        hostSwitch.setClickable(false);
//        hostSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                Log.d(TAG, "Am I the GUIDE? " + isChecked);
//                isGuide = isChecked;
//            }
//        });


        //https://www.youtube.com/embed/SEbqkn1TWTA
        final TextView urlField = findViewById(R.id.urlForLaunch);
        View launchBtn = findViewById(R.id.launchUrlBtn);
        launchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlField.getText().toString();
                if (url.length() == 0) {
                    url = "https://www.youtube.com/w/SEbqkn1TWTA"; //sample for testing
                }

                //stop the cursor flashing
                urlField.clearFocus();

                //hide keyboard
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                if (url.contains("youtube.") || url.contains("youtu.be")) {

                    if (isGuide) {
                        getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_YT + url);
                    } else {
                        launchYouTube(url);
                    }

                } else {
                    if (isGuide) {
                        getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_URL + url);
                    } else {
                        launchWebsite(url);
                    }
                }
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
        getForegroundActivity();

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

    private boolean selectEveryone = false;

    private void setUpControlButtons() {
        whoBtn = findViewById(R.id.whoBtnMain);
        returnBtnMainView = findViewById(R.id.returnToAppMain);
        lockBtnMainView = findViewById(R.id.lockBtnMain);
        unlockBtnMainView = findViewById(R.id.unlockBtnMain);
        blackoutBtnMainView = findViewById(R.id.blackoutBtnMain);

        whoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectEveryone = !selectEveryone;
                //select or deselect all buddies
                connectedStudentsFragment.selectAllPeers(selectEveryone);
            }
        });

        returnBtnMainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToAppFromMainAction();
            }
        });

        lockBtnMainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockFromMainAction();
            }
        });

        unlockBtnMainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlockFromMainAction();
            }
        });

        blackoutBtnMainView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        getAppLauncher();
        getConnectedStudentsFragment();
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
        LinearLayout parentLayout = findViewById(R.id.mainView);
        overlayView = LayoutInflater.from(context).inflate(R.layout.transparent_overlay, parentLayout, false);
        overlayView.findViewById(R.id.blockingView).setVisibility(View.GONE); //default is this should be hidden
        //overlayView.setOnApplyWindowInsetsListener();


        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        Log.d(TAG, "SYSTEM VIS: " + visibility);
                        if (nearbyManager.isConnectedAsFollower()) {
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
        windowManager.updateViewLayout(overlayView, overlayParams);
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

    public void blackout(boolean on) {
        Log.d(TAG, "BLACKOUT: " + on);
        if (on) {
            overlayView.findViewById(R.id.blockingView).setVisibility(View.VISIBLE);
        } else {
            overlayView.findViewById(R.id.blockingView).setVisibility(View.GONE);
        }
    }

    public void setStudentLock(boolean lock) {
        studentLockOn = lock;

        Log.d(TAG, "Is locked? " + studentLockOn);
        String status = "";
        if (studentLockOn) {
            if (isGuide) {
                status = "Students are now in FOLLOW mode";
            } else {
                status = "You are now in FOLLOW mode";
            }

        } else {
            if (isGuide) {
                status = "Students are now in FREE PLAY mode";
            } else {
                status = "You are now in FREE PLAY mode";
            }
        }
        Toast studentStatus = Toast.makeText(context, status, Toast.LENGTH_SHORT);
        studentStatus.show();
    }

    private final static List<ComponentName> browserComponents = new ArrayList<ComponentName>() {{
        add(new ComponentName("com.android.chrome", "com.google.android.apps.chrome.IntentDispatcher")); //preferred browser
        add(new ComponentName("com.google.android.browser", "com.google.android.browser.BrowserActivity"));
        add(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
    }};

    public boolean launchWebsite(String url) {
        //check it's a minimally sensible url
        if (url == null || url.length() < 3 || !url.contains(".")) {
            Toast toast = Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT);
            toast.show();
            getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.AUTO_INSTALL_FAILED + "Invalid URL:" + nearbyManager.getID());
            return false;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        Uri uri = Uri.parse(url);
        intent.setData(uri);

        for (ComponentName cn : browserComponents) {
            intent.setComponent(cn);
            ActivityInfo ai = intent.resolveActivityInfo(pm, 0);
            if (ai != null) {
                Log.w(TAG, "Selecting browser:  " + ai + " for " + uri.getHost());
                startActivity(intent);
                getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + uri.getHost() + ":" + nearbyManager.getID() + ":" + ai.packageName);
                return true; //success!
            }
        }

        // no native browser
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        if (list.size() > 0) {
            context.startActivity(intent);
            getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + uri.getHost() + ":" + nearbyManager.getID() + ":" + "unknown");
            return true; //success!
        } else {
            Toast toast = Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT);
            toast.show();
            getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.AUTO_INSTALL_FAILED + "No browser:" + nearbyManager.getID());
            return false; //no browser, failure
        }
    }

    private String getYouTubeID(String youTubeUrl) {
        String pattern = "(?<=youtu.be/|watch\\?v=|/w/|/videos/|embed\\/)[^#\\&\\?]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(youTubeUrl);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "error";
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


    public boolean launchYouTube(String url) {
        launchingVR = true; //activate button pressing
        String id = getYouTubeID(url);
        Log.i(TAG, "YouTube ID = " + id + " from " + url);
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + id + "&t=1"));

        //doing the below doesn't allow VR mode unfortunately,
        //Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id +"&t=1"))
        //appIntent.putExtra("force_fullscreen", true);
        //appIntent.putExtra("finish_on_ended", true);

        try {
            startActivity(appIntent);
            //TODO don't hardcode the package name
            getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + "YT id=" + id + ":" + nearbyManager.getID() + ":" + "com.google.android.youtube");
            return true; //assume the best if we get to here

        } catch (Exception ex) {
            return launchWebsite(url); //fall back
        }
    }

    private void exitAppImmediately() {
        if (isGuide) {
            remoteDispatcherService.sendAction(ACTION_TAG, EXIT_TAG);
        }
        setResult(0);
        finish();
    }

    public synchronized void exitApp() {
        Log.d(TAG, "Exiting app from floating button press");
        long thisTap = System.currentTimeMillis();

        if ((thisTap - lastTap) <= 2000) { //if two taps within 2 secs
            remoteDispatcherService.sendAction(ACTION_TAG, EXIT_TAG);

            exitToast.cancel();
            setResult(0);
            finish();

        } else {
            lastTap = thisTap;
            exitToast = Toast.makeText(context, "Tap again to exit", Toast.LENGTH_SHORT);
            exitToast.show();
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

    private void showWhoToReturnDialog() {
        dialogShowing = true;
        AlertDialog returnPrompt = new AlertDialog.Builder(this)
                .setTitle("Return to Lumination Lead Me")
                .setMessage("Who should return?")

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setPositiveButton(R.string.app_return_me, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogShowing = false;
                    }
                })

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setNegativeButton(R.string.app_return_everyone, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //send message for everyone to return
                        remoteDispatcherService.sendAction(ACTION_TAG, RETURN_TAG);
                        dialogShowing = false;
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Performs the return to app functionality
     */

    Toast appToast = null;
    boolean returningToApp = false;

    public void returnToAppAction() {
        collapseStatusNow();
        if (!hasWindowFocus()) {
            Log.d(TAG, "Returning to app!");
            //TODO if calling this after launching a 3rd party app from within Lead Me, it works perfectly
            //TODO if calling this after minimising Lead Me, it can take multiple taps to return to the app - weird behaviour?
            getRemoteDispatchService().bringMainToFront();
            activityManager.moveTaskToFront(getTaskId(), 0);
            if (appToast == null) {
                returningToApp = true;
                appToast = Toast.makeText(context, "Returning to Lumination Lead Me app", Toast.LENGTH_SHORT);
                appToast.show();
            }
        }
    }

    public void lockFromMainAction() {
        String connections = getConnectedStudentsFragment().getSelectedPeerIDList();
        getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LOCK_CHOSEN_TAG + connections);
    }

    public void unlockFromMainAction() {
        String connections = getConnectedStudentsFragment().getSelectedPeerIDList();
        getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.UNLOCK_CHOSEN_TAG + connections);
    }

    public void blackoutFromMainAction() {
        String connections = getConnectedStudentsFragment().getSelectedPeerIDList();
        getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.BLACKOUT_CHOSEN_TAG + connections);
    }

    public void returnToAppFromMainAction() {
        String connections = getConnectedStudentsFragment().getSelectedPeerIDList();
        getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.RETURN_CHOSEN_TAG + connections);

        if (appToast == null) {
            returningToApp = true;
            appToast = Toast.makeText(context, "Returning selected followers to Lumination Lead Me app", Toast.LENGTH_SHORT);
            appToast.show();
        }
    }

    public void returnToAppFromSettings() {
        Log.d(TAG, "Returning to app from settings!");
        if (!hasWindowFocus()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
                }
            });
        }
    }

    public void setUIDisconnected() {
        //reset state
        isReadyToConnect = false; //need to press button first
        //isGuide = false;
        //hostSwitch.setChecked(false);

        //reset views
        statusContainer.setVisibility(View.VISIBLE);
        findViewById(R.id.frag_appList).setVisibility(View.GONE);
        findViewById(R.id.follower_layout).setVisibility(View.GONE);
        findViewById(R.id.controlBtns).setVisibility(View.GONE);

        //TODO work this one out
        findViewById(R.id.client_main).setVisibility(View.INVISIBLE);

        if (overlayView != null) {
            overlayView.setVisibility(View.INVISIBLE);
        }

        //reset button
        readyBtn.setText(R.string.ready_label);
        statusText.setText(R.string.clickLetsGo);
        readyBtn.setEnabled(true);
        progress.setVisibility(View.INVISIBLE);

        //make sure everything is stopped
        nearbyManager.onStop();
        nearbyManager.disconnectFromAllEndpoints();
        nearbyManager.stopAdvertising();

    }

    public void onReadyBtnAction() {
        Log.d(TAG, "READY PRESS!");
        isReadyToConnect = !isReadyToConnect; //toggle
        if (isReadyToConnect) {

            //re-check permissions
            permissionManager.workThroughPermissions();

            readyBtn.setText(R.string.waiting_label);
            statusText.setText(R.string.waiting_for_peers);
            progress.setVisibility(View.VISIBLE);
            nearbyManager.getName();
            nearbyManager.onStart();

            if (isGuide) {
                nearbyManager.enactShake();
            }

        } else {
            setUIDisconnected();
        }
        //don't let people change their name after connecting
        nameView.setEnabled(!isReadyToConnect);
    }

    public void collapseStatusNow() {
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(closeDialog);
    }

}
