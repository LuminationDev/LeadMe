package com.lumination.leadme.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.lumination.leadme.R;

/**
 * Create specifically to handle when an application crashes, sends a last message to firebase
 * to delete any records of the currently logged in user to stop duplicates occuring.
 */
public class FirebaseService extends Service {
    private static final String TAG = "FirebaseService";
    private static final String CHANNEL_ID = "firebase_communication";
    private static final String CHANNEL_NAME = "Firebase_Communication";

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
        stopForeground(true);
        stopSelf();
    }
}
