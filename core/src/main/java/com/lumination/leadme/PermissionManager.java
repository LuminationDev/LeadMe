package com.lumination.leadme;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class PermissionManager {

    private String app_title = "Lumination LeadMe";
    private String TAG = "LumiPermissions";

    private final int LOCATION_ON_REQUEST = 4;
    private final int BLUETOOTH_ON_REQUEST = 3;
    private final int WIFI_ON_REQUEST = 2;
    private final int GPS_ON_REQUEST = 1;

    //required for GPS pop up
    private GoogleApiClient googleApiClient;

    private LeadMeMain main;
    public boolean permissionRequestInProgress = false;

    public PermissionManager(LeadMeMain main) {
        super();
        this.main = main;
        app_title = main.getResources().getString(R.string.app_title_with_brand);
    }

    public boolean requestLocationAndNetworkOn() {
        Log.i(TAG, "Requesting location and network permissions");

        LocationManager lm = (LocationManager) main.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        } catch (Exception ex) {
            Log.e(TAG, "Error requesting GPS: " + ex.getMessage());
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            Log.e(TAG, "Error requesting location: " + ex.getMessage());
        }

        if (main.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                main.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                main.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {

            permissionRequestInProgress = true;
            main.requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN}, LOCATION_ON_REQUEST);

            permissionRequestInProgress = false;
            return false;
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast toast = Toast.makeText(main, "Your device does not support Bluetooth", Toast.LENGTH_SHORT);
            toast.show();
            return false; // Device does not support Bluetooth

        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled :)
            permissionRequestInProgress = true;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            main.startActivityForResult(intent, main.BLUETOOTH_ON);
            permissionRequestInProgress = false;
            return false; //wait for user to complete this
        }

        if (!gps_enabled) {
            // notify user
            Log.e(TAG, "Please turn on Location Services");
            //startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ON_REQUEST);
            enableLoc(); //use Google API to show pop up
            return false;
        }

        if (!network_enabled) {
            // notify user
            Log.e(TAG, "Please turn on Wifi");
            main.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), WIFI_ON_REQUEST);
            return false;
        }

        int permissionCheck = ContextCompat.checkSelfPermission(main,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // ask permissions here using below code
            ActivityCompat.requestPermissions(main,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    main.FINE_LOC_ON);
        }

        if (gps_enabled && network_enabled) {
            Log.d(TAG, "ALL READY TO CONNECT!");
            allPermissionsGranted = true;
            return true;
        }

        Log.d(TAG, "Missing something: " + gps_enabled + ", " + network_enabled);
        return false;
    }

    private void enableLoc() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(main)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            Log.d("Location error", "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(main, GPS_ON_REQUEST);
                            //finish();
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                }
            }
        });
    }

    private boolean allPermissionsGranted = false;

    public boolean workThroughPermissions() {
        if (permissionRequestInProgress) {
            return false;
        }
        if (allPermissionsGranted) {
            return true;
        }
        Log.i(TAG, "Working through permissions!");

        if (!isAccessibilityServiceEnabled(main, RemoteDispatcherService.class)) {// if (!areAccessibilitySettingsOn()) {
            //check accessibility permission next, generally needed once per session
            permissionRequestInProgress = true;
            requestAccessibilitySettingsOn();
            permissionRequestInProgress = false;
            return false;
        }

        if (!Settings.canDrawOverlays(main)) {
            //check overlay drawing permission, generally a one-off permission
            permissionRequestInProgress = true;
            requestOverlaySettingsOn();
            permissionRequestInProgress = false;
            return false;
        }

        return requestLocationAndNetworkOn();
    }

    public void requestOverlaySettingsOn() {
        Toast toast = Toast.makeText(main, "Please turn on Floating Window permission for " + app_title, Toast.LENGTH_SHORT);
        toast.show();

        // [1] THIS WORKS ALONE BUT ISN'T CONVENIENT FOR USERS BECAUSE THEY HAVE TO FIND THE RIGHT PERMISSION
        Intent intentParent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intentParent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        main.startActivity(intentParent);

        // [2] ADD THIS AS IT'S MORE CONVENIENT, BUT CALL BACK IS INCORRECT IF [1] IS REMOVED AND LOOPS FOREVER - BOTH [1] and [2] are needed
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", main.getPackageName(), null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        main.startActivityForResult(intent, main.OVERLAY_ON);
    }


    public void requestAccessibilitySettingsOn() {
        Toast toast = Toast.makeText(main, "Please turn on Accessibility for " + app_title, Toast.LENGTH_SHORT);
        toast.show();

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        main.startActivityForResult(intent, main.ACCESSIBILITY_ON);

        pingForAccess();
    }

    private void pingForAccess() {
        main.getHandler().post(new Runnable() {
            @Override
            public void run() {
                while (!isAccessibilityServiceEnabled(main, RemoteDispatcherService.class)) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "Access granted!");
                main.recallToLeadMe();
            }
        });
    }

    public boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) {
            return false;
        }

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        main.getRemoteDispatchService(); //initialise this

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName)) {
                Log.i(TAG, "***ACCESSIBILITY IS ENABLED***");
                return true;
            }
        }

        Log.i(TAG, "***ACCESSIBILITY IS DISABLED***");
        return false;
    }
}

