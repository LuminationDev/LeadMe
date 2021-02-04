package com.lumination.leadme;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    private String app_title = "Lumination LeadMe";
    private final String TAG = "LumiPermissions";

    private LeadMeMain main;
    private boolean overlayPermissionGranted = false, nearbyPermissionsGranted = false;
    protected boolean waitingForPermission = false;
    private PermissionListener overlayPermissionListener;
    private PermissionListener nearbyPermissionListener;
    private PermissionListener miscPermissionListener;
    private ArrayList<String> rejectedPermissions = new ArrayList<>();

    public PermissionManager(LeadMeMain main) {
        super();
        this.main = main;
        app_title = main.getResources().getString(R.string.app_title_with_brand);

        overlayPermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                overlayPermissionGranted = true; //all granted
                waitingForPermission = false; //no longer waiting

                //Log.d(TAG, "Permission granted! " + main.getNearbyManager().isConnectedAsFollower() + ", " + main.getNearbyManager().isConnectedAsGuide());
                main.performNextAction();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                //Log.d(TAG, "Overlay Permission DENIED!");
                overlayPermissionGranted = false; //not all granted
                waitingForPermission = false; //no longer waiting
            }
        };

        nearbyPermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                nearbyPermissionsGranted = main.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        main.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                waitingForPermission = false; //no longer waiting

                //Log.d(TAG, "Nearby permission granted? " + nearbyPermissionsGranted + ", " + main.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) + ", " + main.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
                if (nearbyPermissionsGranted) {
                    main.performNextAction();
                }
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                nearbyPermissionsGranted = false; //not all granted
                waitingForPermission = false; //no longer waiting
                rejectedPermissions.clear();
                rejectedPermissions.addAll(deniedPermissions);
            }
        };

        miscPermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                //Log.d(TAG, "Misc permissions granted!");
                main.performNextAction();
                waitingForPermission = false; //no longer waiting
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                //Log.e(TAG, "Misc permission REJECTED! " + deniedPermissions.toString());
                waitingForPermission = false; //no longer waiting
            }
        };
    }

    public boolean isOverlayPermissionGranted() {
        overlayPermissionGranted = /*(main.checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED ||*/ Settings.canDrawOverlays(main);

//        Log.d(TAG, "IsOverlayPermissionGranted? "
//                + (main.checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED)
//                + ", " + Settings.canDrawOverlays(main) + ", " + main.overlayView);

        if (!overlayPermissionGranted && main.getNearbyManager().isConnectedAsFollower()) {
            //alert the teacher that the student may not be lockable
            Log.e(TAG, "WARNING! I may be off task - overlay can't be shown. " + main.getNearbyManager().getID());
            main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_OVERLAY, false);

        } else if (overlayPermissionGranted && main.getNearbyManager().isConnectedAsFollower()) {
            main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_OVERLAY, true);
        }

        return overlayPermissionGranted;
    }

    public boolean isNearbyPermissionsGranted() {
        nearbyPermissionsGranted = main.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                main.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        //Log.d(TAG, "IsNearbyPermissionGranted? " + nearbyPermissionsGranted);
        return nearbyPermissionsGranted;
    }

    public void checkOverlayPermissions() {
        //Log.d(TAG, "Checking Overlay Permissions. Currently " + overlayPermissionGranted + ", " + rejectedPermissions);
        waitingForPermission = true;
        if (!isOverlayPermissionGranted()) {
            main.closeKeyboard();
            TedPermission.with(main)
                    .setPermissionListener(overlayPermissionListener)
                    .setPermissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
                    .check();
        }

        if (!isAccessibilityGranted()) {
            requestAccessibilitySettingsOn();
        }
    }

    //these permissions don't need user prompting, but CAN be turned off
    public void checkMiscPermissions() {
        //Log.d(TAG, "Checking Misc Permissions.");
        waitingForPermission = true;
        String rationaleMsg = "Please turn on the following permissions to connect with a Leader and ensure LeadMe functions correctly.";

        TedPermission.with(main)
                .setPermissionListener(miscPermissionListener)
                .setPermissions(Manifest.permission.INTERNET, Manifest.permission.REORDER_TASKS,
                        Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.NFC,
                        Manifest.permission.EXPAND_STATUS_BAR) //only learners/students need a system alert window
                .check();
    }


    public void checkNearbyPermissions() {
        //Log.d(TAG, "Checking Nearby Permissions. Currently " + nearbyPermissionsGranted + ", " + rejectedPermissions);
        waitingForPermission = true;
        String rationaleMsg = "Please enable Location services to connect to other LeadMe users.";

        TedPermission.with(main)
                .setPermissionListener(nearbyPermissionListener)
                .setDeniedMessage(rationaleMsg)
                .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .check();
    }


    public void requestAccessibilitySettingsOn() {
        main.canAskForAccessibility = true;
        Toast toast = Toast.makeText(main, "Please turn on Accessibility for " + app_title, Toast.LENGTH_SHORT);
        toast.show();

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        main.startActivityForResult(intent, main.ACCESSIBILITY_ON);

        pingForAccess();
    }

    private void pingForAccess() {
        main.getHandler().post(() -> {
            while (waitingForPermission && !isAccessibilityGranted()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //once all other settings are enabled, return to LeadMe
            //Log.d(TAG, "ACCESSIBILITY search complete.");
            main.canAskForAccessibility = false;
            if (needsRecall) {
                main.recallToLeadMe();
            }
        });
    }

    protected boolean needsRecall = false;

    //this functionality is intended for testing, not control flow
    //in future, find a better way to do it for production case
    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) main.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAccessibilityGranted() {
        ComponentName expectedComponentName = new ComponentName(main, LumiAccessibilityService.class);

        String enabledServicesSetting = Settings.Secure.getString(main.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) {
            return false;
        }

        String[] services = enabledServicesSetting.split(":");
        // main.getLumiAccessibilityService(); //initialise this


        //Log.d(TAG, "Searching for: " + expectedComponentName);
        for (String componentNameString : services) {
            Log.d(TAG, "\t>> " + componentNameString);
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName)) {
                Log.i(TAG, "***ACCESSIBILITY IS ENABLED, IS IT RUNNING? (" + isMyServiceRunning(LumiAccessibilityService.class) + ") ***");
                needsRecall = true;
                waitingForPermission = false;
                main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_ACCESSIBILITY, true);
                return true;
            }
        }

        main.getDispatcher().alertGuidePermissionGranted(LeadMeMain.STUDENT_NO_ACCESSIBILITY, false);

        Log.i(TAG, "***ACCESSIBILITY IS DISABLED***");
        waitingForPermission = false;
        return false;
    }

    private boolean successfulPing = false;

    protected boolean isInternetConnectionAvailable(String url) {
        ConnectivityManager connectivityManager = (ConnectivityManager) main.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (connected) {
            try {
                String host = Uri.parse(url).getHost();

                Thread thread = new Thread(() -> {
                    try {
                        successfulPing = InetAddress.getByName(host).isReachable(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        successfulPing = false;
                    }
                });
                thread.start();

                //wait for isReachable to return
                Thread.sleep(1200);

                connected = connected && successfulPing;

            } catch (Exception e) {
                e.printStackTrace();
                connected = false;
            }
        }
        return connected;
    }

}

