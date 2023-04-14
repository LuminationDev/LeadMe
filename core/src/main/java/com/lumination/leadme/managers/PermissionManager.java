package com.lumination.leadme.managers;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    private static final String TAG = "LumiPermissions";
    private final LeadMeMain main;
//    private PermissionListener overlayPermissionListener;
    private final PermissionListener nearbyPermissionListener;
    private final PermissionListener storagePermissionListener;
    private final PermissionListener miscPermissionListener;
    private final ArrayList<String> rejectedPermissions = new ArrayList<>();


    private boolean overlayPermissionGranted = false, nearbyPermissionsGranted = false, storagePermissionsGranted = false, usageGranted = false;
    public static boolean waitingForPermission = false;

    public PermissionManager(LeadMeMain main) {
        super();
        this.main = main;
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

        storagePermissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                storagePermissionsGranted = main.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        main.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                waitingForPermission = false; //no longer waiting

                if (storagePermissionsGranted) {
                    main.performNextAction();
                }
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                storagePermissionsGranted = false; //not all granted
                waitingForPermission = false; //no longer waiting
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
        overlayPermissionGranted = Settings.canDrawOverlays(main);

        Log.e(TAG, "Overlay Permission: " + overlayPermissionGranted);

        if (!overlayPermissionGranted && NearbyPeersManager.isConnectedAsFollower()) {
            //alert the teacher that the student may not be lockable
            Log.e(TAG, "WARNING! I may be off task - overlay can't be shown. " + NearbyPeersManager.getID());
            DispatchManager.alertGuidePermissionGranted(Controller.STUDENT_NO_OVERLAY, false);

        } else if (overlayPermissionGranted && NearbyPeersManager.isConnectedAsFollower()) {
            DispatchManager.alertGuidePermissionGranted(Controller.STUDENT_NO_OVERLAY, true);
            if(!main.overlayInitialised){
                main.initialiseOverlayView();
            }
        }

        return overlayPermissionGranted;
    }

    public void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        main.startActivity(intent);
    }

    public boolean isUsageStatsPermissionGranted() {
        AppOpsManager appOps = (AppOpsManager) LeadMeMain.getInstance().getBaseContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), "com.lumination.leadme");
        usageGranted = mode == AppOpsManager.MODE_ALLOWED;
        return usageGranted;
    }

    public boolean isNearbyPermissionsGranted() {
        nearbyPermissionsGranted = main.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                main.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        //Log.d(TAG, "IsNearbyPermissionGranted? " + nearbyPermissionsGranted);
        return nearbyPermissionsGranted;
    }

    public boolean isStoragePermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storagePermissionsGranted =  Environment.isExternalStorageManager();
        } else {
            storagePermissionsGranted = main.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    main.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return storagePermissionsGranted;
    }

    public void checkOverlayPermissions() {
        Log.d(TAG, "Checking Overlay Permissions. Currently " + overlayPermissionGranted + ", " + rejectedPermissions);
        if (!isOverlayPermissionGranted()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + main.getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            main.startActivityForResult(intent, LeadMeMain.OVERLAY_ON);
        }
    }

    //these permissions don't need user prompting, but CAN be turned off
    public void checkMiscPermissions() {
        //Log.d(TAG, "Checking Misc Permissions.");
        waitingForPermission = true;

        TedPermission.with(main)
                .setPermissionListener(miscPermissionListener)
                .setPermissions(Manifest.permission.INTERNET, Manifest.permission.REORDER_TASKS,
                       // Manifest.permission.NFC,
                        Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.EXPAND_STATUS_BAR) //only learners/students need a system alert window
                .check();
    }

    /**
     * Checking if the device has ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission on. This
     * is for finding nearby guides on a local network.
     */
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

    /**
     * Checking if the device has READ_EXTERNAL storage permission on . This is used for the file transfer
     * function as well as the VR controller.
     */
    public void getStoragePermissions() {
        waitingForPermission = true;
        String rationaleMsg = "Please enable Storage permissions so Guides can select and transfer videos.";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            main.startActivity(intent);
        } else {
            TedPermission.with(main)
                    .setPermissionListener(storagePermissionListener)
                    .setDeniedMessage(rationaleMsg)
                    .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .check();
        }
    }

    private boolean successfulPing = false;

    public boolean isInternetConnectionAvailable() {
        String url = "google.com"; //some things don't ping well, so just test this for now
        ConnectivityManager connectivityManager = (ConnectivityManager) main.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        if (connected) {
            try {
                String host = Uri.parse(url).getHost();

                main.backgroundExecutor.submit(() -> {
                    try {
                        successfulPing = InetAddress.getByName(host).isReachable(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        successfulPing = false;
                    }
                });

                //wait for isReachable to return
                Thread.sleep(1200);
                connected = successfulPing;

            } catch (Exception e) {
                e.printStackTrace();
                connected = false;
            }
        }
        return connected;
    }

    /**
     * Check whether a device is connected to the internet, either through WIFI or CELLULAR.
     * @param context The context of the application.
     * @return A boolean representing if the device is connected to the internet.
     */
    public static boolean isInternetAvailable(Context context) {
        boolean result = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    result = true;
                } else {
                    result = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            }
        }

        return result;
    }
}

