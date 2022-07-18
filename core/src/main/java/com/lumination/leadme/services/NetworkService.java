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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.connections.TcpClient;
import com.lumination.leadme.models.Learner;
import com.lumination.leadme.managers.NetworkManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private static InetAddress leaderIPAddress;
    public static boolean isGuide = false;

    /**
     * Keep track of the student threads that are currently being managed by the leader device.
     */
    public static HashMap<String, Learner> studentThreadArray = new HashMap<>();

    /**
     * Keep Track of the Client ID as the key and student socket as the value. It can then be used
     * to determine if a student is reconnecting or is a new user.
     */
    public static HashMap<String, String> clientSocketArray = new HashMap<>();

    /**
     * Only purpose is to provide a reverse look up in comparison to the clientSocketArray for
     * when receiving a message from learners. Quickly able to get their ID by their address.
     */
    public static HashMap<String, String> addressSocketArray = new HashMap<>();

    /**
     * Specific executor just for the server, has not automatic cut off period like the
     * CachedThreadPool.
     */
    private static ThreadPoolExecutor serverThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    private static ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1);

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

    public static void receiveMessage(String message) {
        if (message.length() == 0) {
            return;
        }
        NetworkManager.messageReceivedFromServer(message);
    }

    //LEARNER NETWORK FUNCTIONS
    /**
     * Set the IP address of the leader a learner is trying to connect to.
     * @param ipAddress An InetAddress representing the device a learner is connecting to.
     */
    public static void setLeaderIPAddress(InetAddress ipAddress) {
        Log.d(TAG, "Setting IP address.");
        leaderIPAddress = ipAddress;
    }

    public static InetAddress getLeaderIPAddress() {
        return leaderIPAddress;
    }

    public static void sendToServer(String message, String type) {
        if (getLeaderIPAddress() != null) {
            sendLeaderMessage(type + "," +
                    message.replace("\n", "_").replace("\r", "|"));
        }
    }

    /**
     * Send a message to the leader. If the leader cannot be reach the learner disconnects themselves
     * to avoid a hanging connection.
     * @param message A string containing the message for the leader and the type.
     */
    public static void sendLeaderMessage(String message) {
        Log.d(TAG, "Attempting to send: " + message);
        LeadMeMain.learnerReference.child("currentMessage").setValue(message);
    }

    //LEADER NETWORK FUNCTIONS
    public static void sendToClient(String learnerID, String message, String type) {
        Log.d(TAG, "sendToClient: "+message+" Type: "+type);

        sendLearnerMessage(learnerID, type + "," + message);
    }

    public static void sendToAllClients(String message, String type) {
        Log.d(TAG, "sendToAllClients: "+message+" Type: "+type);

        sendAllLearnerMessage(type + "," + message);
    }

    public static void sendAllLearnerMessage(String message) {
        Log.d(TAG, "Attempting to send: " + message);
        LeadMeMain.messagesReference.child("currentMessage").setValue(message);
    }

    public static void sendLearnerMessage(String ipAddress, String message) {
        Log.d(TAG, "Attempting to send: " + message);

        LeadMeMain.messagesReference.child("learners").child(ipAddress.replace(".", "_")).child("leaderMessage").setValue(message);
    }

    /**
     * Find the learner that a message is directed towards and pass the message onto the appropriate
     * TCP handler. If a learner is not found, create a new entry.
     * @param clientAddress An InetAddress representing the recent connection and a learner in the
     *                      studentThreadArray.
     * @param message A string containing the action that has/or needs to be performed.
     */
    public static void determineAction(String clientAddress, String message) {
        clientAddress = clientAddress.replace(".", "_");
        String tempID = addressSocketArray.get(clientAddress);
        Learner learner = studentThreadArray.get(tempID);

        if(learner != null) {
            learner.tcp.inputReceived(message);
        } else {
            Log.d(TAG, "Learner not found: " + clientAddress +
                    " Message:" + message);

            //Only create a new connecting if a Name is being sent through
            if(message.contains("NAME")) {
                Log.d(TAG, "Adding new learner");
                determineAction(clientAddress, message);
            }
        }

        if(message.equals("DISCONNECT,No connection") && tempID != null) {
            removeStudent(clientAddress);
        }
    }

    /**
     * Check if the server is running on a leader's device.
     * @return A boolean representing if the server socket is open.
     */
    public static boolean isServerRunning() {
        return LeadMeMain.roomReference != null;
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
     * Manages what Client IDs are currently in use. Gets the first index equaling null and
     * assigned the a student thread to it.
     * @param clientAddress An InetAddress object of the newly connected user.
     */
    public static void learnerManager(String clientAddress) {
        String id = clientAddress.replace(".", "_");

        TcpClient tcpClient = new TcpClient(id, clientAddress);
        Learner learner = new Learner();
        learner.tcp = tcpClient;

        clientSocketArray.put(id, id);
        studentThreadArray.put(id, learner);
        addressSocketArray.put(id, id);
    }

    /**
     * Cycle through the student thread container. Interrupt each student thread running in
     * the background and remove it from the array. To be used on logout or session end to stop
     * hanging connections on the student devices.
     * Reset the client ID count on logout otherwise ID and xray ID do not match and client ID
     * consecutively grows
     */
    public static void resetClientIDs() {
        studentThreadArray.clear();
        clientSocketArray.clear();
        addressSocketArray.clear();
    }

    private static void disconnection() {
        clientSocketArray.forEach((key, value) -> sendLearnerMessage(value, "DISCONNECT" + ","));
    }

    /**
     * Remove a single student from all hashmaps. Use this when manually disconnecting a single
     * student or if there is a crash on a device.
     * @param clientID An integer representing the ID of the learner to remove.
     */
    public static void removeStudent(String clientID) {
        if(studentThreadArray.get(clientID) != null) {
            //Objects.requireNonNull(studentThreadArray.get(clientID)).tcp.shutdownTCP();
            studentThreadArray.remove(clientID);
        }

        clientSocketArray.remove(clientID);

        LeadMeMain.messagesReference.child("learners").child(clientID).child("currentMessage").removeEventListener(LeadMeMain.leaderReceivingLearnerMessageListener);
        LeadMeMain.messagesReference.child("learners").child(clientID).removeValue();
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
        if(isGuide) {
            disconnection();
            isGuide = false;
        }
        endForeground();
        leaderIPAddress = null;
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
        stopAllFunction();
        stopSelf();
        stopForeground(true);
    }

    /**
     * Close the server socket, shut down the thread pool and interupt any TCP clients so the
     * server is not running in the background if the application is closed.
     */
    private void stopAllFunction() {
        if(isGuide) {
            shutdownScheduler();
            resetClientIDs();
        }
        stopServer();
        serverThreadPool.shutdown();
    }

    /**
     * Shutdown the scheduler associated with the Pinging of learner devices.
     */
    public void shutdownScheduler() {
        scheduledExecutor.shutdown();
    }
}
