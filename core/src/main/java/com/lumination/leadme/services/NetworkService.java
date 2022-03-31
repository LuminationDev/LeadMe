package com.lumination.leadme.services;

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

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.connections.TcpClient;
import com.lumination.leadme.connections.StudentThread;
import com.lumination.leadme.managers.NetworkManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Responsible for handling network connection between a leader and a learner.
 */
@TargetApi(29)
public class NetworkService extends Service {
    private static final String TAG = "NetworkService";
    private static final String CHANNEL_ID = "network_service";
    private static final String CHANNEL_NAME = "Network_Service";

    private static Future<?> server = null;
    private static ServerSocket mServerSocket = null; //server socket for server
    private static final int PORT = 54321;
    private static int clientID = 0;
    public static boolean isRunning = false;

    /**
     * Keep track of the student threads that are currently being managed by the leader device.
     */
    public static HashMap<Integer, StudentThread> studentThreadArray;

    /**
     * Keep Track of the Client ID as the key and student socket as the value. It can then be used
     * to determine if a student is reconnecting or is a new user.
     */
    public static HashMap<Integer, Map.Entry<Socket, Boolean>> clientSocketArray;

    /**
     * Specific executor just for the server, has not automatic cut off period like the
     * CachedThreadPool.
     */
    private static ThreadPoolExecutor serverThreadPool;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    /**
     * Class used for the client Binder.  We know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public NetworkService getService() {
            // Return this instance of NetworkService so clients can call public methods
            return NetworkService.this;
        }
    }

    /**
     * Each time the server is started (after logout or end session) reset the thread pool
     * and client arrays.
     */
    private static void setupService() {
        isRunning = true;
        serverThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        setArrays();
    }

    //Server thread here
    //Runs an eternal server allowing any connections that come in
    public static void startLeaderServer() {
        setupService();
        Log.e(TAG, "Starting server");
        if(server == null) {
            server = serverThreadPool.submit(() -> {
                try {
                    mServerSocket = new ServerSocket();
                    mServerSocket.setReuseAddress(true);
                    mServerSocket.bind(new InetSocketAddress(PORT));

                    while (true) {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        Socket clientSocket;

                        try {
                            clientSocket = mServerSocket.accept();//blocks the thread until client is accepted
                        } catch (IOException e) {
                            //Exits the server loop on session end
                            e.printStackTrace();
                            return;
                        }

                        //Supporting functions in network manager or network adapter? don't want to be too cluttered
                        studentThreadManager(clientSocket);
                        Log.d(TAG, "run: client connected");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating ServerSocket: ", e);
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Check if the server is running on a leader's device.
     * @return A boolean representing if the server socket is open.
     */
    public static boolean isServerRunning() {
        if (mServerSocket != null) {
            return !mServerSocket.isClosed();
        }
        return false;
    }

    /**
     * Stop the server from running and cancel the future task related to the server.
     */
    public static void stopServer() {
        if(server != null) {
            Log.d(TAG, "Server cancel");
            server.cancel(true);
            server = null;
        }

        if(mServerSocket != null) {
            try {
                mServerSocket.close();
                Log.d("CLOSING PORT", "Server closing is bound: " + mServerSocket.isBound());
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.e(TAG, "Error when closing server socket.");
            }
        }
    }

    /**
     * Get the instance of the server socket running on the leader's device.
     * @return A ServerSocket representing the server the leader is associated with.
     */
    public static ServerSocket getServerSocket() {
        return mServerSocket;
    }

    /**
     * Manages what Client IDs are currently in use. Gets the first index equaling null and
     * assigned the a student thread to it.
     * @param clientSocket A socket object of the newly connected user.
     */
    public static void studentThreadManager(Socket clientSocket) {
        ClientResult result = manageClientID(clientSocket);
        int ID = result.getID();
        boolean reconnect = result.getReconnect();

        TcpClient tcp = new TcpClient(clientSocket, ID);
        Thread client = new Thread(tcp); //new thread for every client
        StudentThread st = new StudentThread();
        st.t = client;
        st.ID = ID;

        AbstractMap.SimpleEntry<Socket, Boolean> entry = new AbstractMap.SimpleEntry<>(clientSocket, reconnect);

        clientSocketArray.put(ID, entry);
        studentThreadArray.put(ID, st);
        Objects.requireNonNull(studentThreadArray.get(ID)).t.start();
    }

    /**
     * Find the correct Client ID to pass to the new user depending on if they are reconnecting or
     * are a new user.
     * @param clientSocket A socket object of the newly connected user.
     */
    public static ClientResult manageClientID(Socket clientSocket) {
        final int[] tempID = {-1};
        //Scan through the set to find any matching IP addresses and get the client ID ('key')
        clientSocketArray.entrySet().stream().forEach(e -> {
            if(e.getValue().getKey().getInetAddress().getHostAddress().equals(clientSocket.getInetAddress().getHostAddress())) {
                Log.d(TAG, "Is user reconnecting: " + true);
                Log.d(TAG, "User connecting as ID: " + e.getKey());
                tempID[0] = e.getKey();
            }
        });

        if(tempID[0] != -1) {
            Log.d(TAG, "Reconnecting Student: " + tempID[0]);
            Objects.requireNonNull(studentThreadArray.get(tempID[0])).t.interrupt();
            studentThreadArray.remove(tempID[0]);
            return new ClientResult(tempID[0], true);
        } else {
            Log.d(TAG, "New Student: " + clientID);
            return new ClientResult(clientID++, false);
        }
    }

    /**
     * Cycle through the student thread container. Interrupt each student thread running in
     * the background and remove it from the array. To be used on logout or session end to stop
     * hanging connections on the student devices.
     * Reset the client ID count on logout otherwise ID and xray ID do not match and client ID
     * consecutively grows
     */
    public static void resetClientIDs() {
        isRunning = false;
        studentThreadArray.clear();
        clientSocketArray.clear();
        clientID = 0;
    }

    public static void destroyLearnerConnection() {
        NetworkManager.cleanUpInput();
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

    private static void setArrays() {
        clientSocketArray = new HashMap<>();
        studentThreadArray = new HashMap<>();
    }

    @Override
    public void onDestroy() {
        destroyLearnerConnection();
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
                .setSmallIcon(R.drawable.core_xray)
                .setContentTitle("NetworkService is running in the foreground")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(notificationId, notification);
    }

    public void endForeground() {
        if(LeadMeMain.isGuide) {stopAllFunction();}
        stopSelf();
        stopForeground(true);
    }

    /**
     * Close the server socket, shut down the thread pool and interupt any TCP clients so the
     * server is not running in the background if the application is closed.
     */
    private void stopAllFunction() {
        //TODO Send disconnection message here??
        resetClientIDs();
        stopServer();
        serverThreadPool.shutdown();
    }
}

final class ClientResult {
    private final int ID;
    private final boolean reconnect;

    public ClientResult(int ID, boolean reconnect) {
        this.ID = ID;
        this.reconnect = reconnect;
    }

    public int getID() {
        return ID;
    }

    public boolean getReconnect() {
        return reconnect;
    }
}
