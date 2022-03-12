package com.lumination.leadme;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Create specifically to handle when an application crashes, sends a last message to firebase
 * to delete any records of the currently logged in user to stop duplicates occuring.
 */
@TargetApi(29)
public class FirebaseService extends Service {
    private static final String TAG = "FirebaseService";
    private static final String CHANNEL_ID = "firebase_communication";
    private static final String CHANNEL_NAME = "Firebase_Communication";

    private static String publicIP;
    private static String serverIP;

    // Binder given to clients
    private final IBinder binder = new FirebaseService.LocalBinder();

    /**
     * Class used for the client Binder.  We know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public FirebaseService getService() {
            // Return this instance of FirebaseService so clients can call public methods
            return FirebaseService.this;
        }
    }

    public static void setPublicIP(String IP) {
        publicIP = IP;
    }

    public static void setServerIP(String IP) {
        serverIP = IP;
    }

    /**
     * Remove the login details from firebase if the leader ends session or logs out. Uses the
     * publicIP and serverIP address to find the data entry. Only executes if server discovery
     * was used to connect initially. Also stops the time stamp updater from running.
     */
    public static void removeAddress() {
        if(publicIP == null) {
            return;
        }

        if(publicIP.length() == 0) {
            return;
        }

        Log.e(TAG, "FIREBASE PublicIP: " + publicIP + " Server:" + serverIP);

        FirebaseFirestore.getInstance().collection("addresses").document(publicIP)
                .collection("Leaders").document(serverIP).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
    }

    @Override
    public void onDestroy() {
        endForeground();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    public void startForeground() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(channel);

        final int notificationId = (int) System.currentTimeMillis();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.leadme_icon)
                .setContentTitle("FirebaseService is running in the foreground")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(notificationId, notification);
    }

    public void endForeground() {
        removeAddress();
        stopForeground(true);
        stopSelf();
    }
}
