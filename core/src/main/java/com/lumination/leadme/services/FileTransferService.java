package com.lumination.leadme.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.utilities.ClientTransferTask;
import com.lumination.leadme.utilities.FileUtilities;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TargetApi(29)
public class FileTransferService extends Service {
    private static final String TAG = "FileTransferService";

    private static final String CHANNEL_ID = "file_transfer";
    private static final String CHANNEL_NAME = "File_Transfer";

    private static Thread serverThread;
    private static ServerSocket serverSocket = null;

    public static boolean isRunning = true;
    public static final int PORT = 54323;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    /**
     * Class used for the client Binder.  We know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public FileTransferService getService() {
            // Return this instance of ScreensharingService so clients can call public methods
            return FileTransferService.this;
        }
    }

    /**
     * Start the transfer server, continue accepting connections until the last transfer has been
     * completed.
     */
    public static void startFileServer() {
        stopFileServer();

        //TODO test with 10+ phones in case the server shuts down before finishing
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

        Runnable serverTask = () -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));

                Log.d(TAG, "Waiting for clients to connect...");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    clientProcessingPool.submit(new ClientTransferTask(clientSocket));
                }

                Log.d(TAG, "Transfer server shutdown...");
                isRunning = true;

            } catch (IOException e) {
                Log.e(TAG, "Unable to start or continue transfer server");
                e.printStackTrace();
            }
        };

        serverThread = new Thread(serverTask);
        serverThread.start();
    }

    /**
     * Stop any current or old instances of the server thread and socket.
     */
    public static void stopFileServer() {
        if(serverThread != null) {
            serverThread.interrupt();
            Log.d(TAG, "Old server thread interrupted");
        }

        if(serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
                Log.d(TAG, "Old server socket interrupted");
            } catch (IOException e) {
                Log.e(TAG, "Unable to stop old server socket");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
    }

    @Override
    public void onDestroy() {
        stopFileServer();
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
                .setSmallIcon(R.drawable.core_url_icon)
                .setContentTitle("FileTransferService is running in the foreground")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(notificationId, notification);
    }

    public void endForeground() {
        stopFileServer();
        stopSelf();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}

