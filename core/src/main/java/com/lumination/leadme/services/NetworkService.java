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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
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
    private static InetAddress leaderIPAddress;
    public static final int leaderPORT = 54321;
    private static final int learnerPORT = 54320;
    private static int clientID = 0;
    public static boolean isRunning = false;
    public static boolean isGuide = false;

    /**
     * Keep track of the student threads that are currently being managed by the leader device.
     */
    public static HashMap<Integer, Learner> studentThreadArray = new HashMap<>();

    /**
     * Keep Track of the Client ID as the key and student socket as the value. It can then be used
     * to determine if a student is reconnecting or is a new user.
     */
    public static HashMap<Integer, Map.Entry<InetAddress, Boolean>> clientSocketArray = new HashMap<>();

    /**
     * Only purpose is to provide a reverse look up in comparison to the clientSocketArray for
     * when receiving a message from learners. Quickly able to get their ID by their address.
     */
    public static HashMap<InetAddress, Integer> addressSocketArray = new HashMap<>();

    /**
     * Specific executor just for the server, has not automatic cut off period like the
     * CachedThreadPool.
     */
    private static ThreadPoolExecutor serverThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    private static ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

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
        backgroundExecutor = Executors.newCachedThreadPool();

        if(LeadMeMain.isGuide) {
            isGuide = true;
            setArrays();
        }
    }

    /**
     * Runs an eternal server allowing any connections that come in. Uses a different port number
     * depending on if the user is a leader or learner.
     */
    public static void startServer() {
        setupService();
        Log.e(TAG, "Starting server");
        if(server == null) {
            server = serverThreadPool.submit(() -> {
                try {
                    mServerSocket = new ServerSocket();
                    mServerSocket.setReuseAddress(true);

                    if(isGuide) {
                        mServerSocket.bind(new InetSocketAddress(leaderPORT));
                    } else {
                        mServerSocket.bind(new InetSocketAddress(learnerPORT));
                    }

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
                        backgroundExecutor.submit(() -> receiveMessage(clientSocket));
                        Log.d(TAG, "run: client connected");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating ServerSocket: ", e);
                    e.printStackTrace();
                }
            });
        }
    }

    private static void receiveMessage(Socket clientSocket) {
        try {
            // get the input stream from the connected socket
            InputStream inputStream = clientSocket.getInputStream();
            // read from the stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] content = new byte[ 2048 ];
            int bytesRead;

            while( ( bytesRead = inputStream.read( content ) ) != -1 ) {
                baos.write( content, 0, bytesRead );
            }

            String message = baos.toString();

            //Get the IP address used to determine who has just connected.
            InetAddress ipAddress = clientSocket.getInetAddress();

            //Message has been received close the socket
            clientSocket.close();

            if(isGuide) {
                determineAction(ipAddress, message);
            } else {
                NetworkManager.messageReceivedFromServer(message);
            }

            Log.d(TAG, "Message: " + message + " IpAddress:" + ipAddress.getHostAddress());

        } catch (IOException e) {
            Log.e(TAG, "Unable to process client request");
            e.printStackTrace();
        }
    }

    /**
     * An interface for sending a message to a destination.
     * @param message A string representing the communication that is being sent.
     * @param ipAddress An InetAddress of the destination.
     * @param destPORT A int representing the destinations port.
     * @throws IOException Throws an error if the socket was unable to be connected.
     */
    private static void sendMessage(String message, InetAddress ipAddress, int destPORT) throws IOException {
        Socket soc = new Socket(ipAddress, destPORT);

        OutputStream toServer = soc.getOutputStream();
        PrintWriter output = new PrintWriter(toServer);
        output.println(message);
        DataOutputStream out = new DataOutputStream(toServer);
        out.writeBytes(message);

        toServer.close();
        output.close();
        out.close();
        soc.close();
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
            backgroundExecutor.submit(() -> sendLeaderMessage(type + "," +
                    message.replace("\n", "_").replace("\r", "|")));
        }
    }

    /**
     * Send a message to the leader. If the leader cannot be reach the learner disconnects themselves
     * to avoid a hanging connection.
     * @param message A string containing the message for the leader and the type.
     */
    public static void sendLeaderMessage(String message) {
        Log.d(TAG, "Attempting to send: " + message);

        try {
            sendMessage(message, leaderIPAddress, leaderPORT);
            Log.d(TAG, "Message sent closing socket");
        } catch (IOException e) {
            //Disconnect if no connection is achieved
            NetworkManager.messageReceivedFromServer("DISCONNECT,");
            e.printStackTrace();
        }
    }

    //LEADER NETWORK FUNCTIONS
    public static void sendToClient(int learnerID, String message, String type) {
        Log.d(TAG, "sendToClient: "+message+" Type: "+type);

        Map.Entry<InetAddress, Boolean> entry = clientSocketArray.get(learnerID);
        if (entry != null) {
            InetAddress ipAddress = entry.getKey();
            backgroundExecutor.submit(() -> sendLearnerMessage(learnerID, ipAddress, type + "," + message));
        }
    }

    public static void sendLearnerMessage(int learnerID, InetAddress ipAddress, String message) {
        Log.d(TAG, "Attempting to send: " + message);

        try {
            sendMessage(message, ipAddress, learnerPORT);
            Log.d(TAG, "Message sent closing socket");
        } catch (IOException e) {
            backgroundExecutor.submit(() -> determineAction(ipAddress, "DISCONNECT,No connection"));
            removeStudent(learnerID);
            e.printStackTrace();
        }
    }

    /**
     * Find the learner that a message is directed towards and pass the message onto the appropriate
     * TCP handler. If a learner is not found, create a new entry.
     * @param clientAddress An InetAddress representing the recent connection and a learner in the
     *                      studentThreadArray.
     * @param message A string containing the action that has/or needs to be performed.
     */
    private static void determineAction(InetAddress clientAddress, String message) {
        Integer tempID = addressSocketArray.get(clientAddress);
        Learner learner = studentThreadArray.get(tempID);

        if(learner != null) {
            learner.tcp.inputReceived(message);
        } else {
            Log.d(TAG, "Learner not found: " + clientAddress.getHostAddress() +
                    " Message:" + message);
            Log.d(TAG, "Adding new learner");

            //Only create a new connecting if a Name is being sent through
            if(message.contains("NAME")) {
                learnerManager(clientAddress);
                determineAction(clientAddress, message);
            }
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
     * @param clientAddress An InetAddress object of the newly connected user.
     */
    public static void learnerManager(InetAddress clientAddress) {
        ClientResult result = manageClientID(clientAddress);
        int ID = result.getID();
        boolean reconnect = result.getReconnect();

        TcpClient tcpClient = new TcpClient(clientAddress, ID);
        Learner learner = new Learner();
        learner.tcp = tcpClient;
        learner.ID = ID;

        AbstractMap.SimpleEntry<InetAddress, Boolean> entry = new AbstractMap.SimpleEntry<>(clientAddress, reconnect);

        clientSocketArray.put(ID, entry);
        studentThreadArray.put(ID, learner);
        addressSocketArray.put(clientAddress, ID);
    }

    /**
     * Find the correct Client ID to pass to the new user depending on if they are reconnecting or
     * are a new user.
     * @param clientAddress A socket object of the newly connected user.
     */
    public static ClientResult manageClientID(InetAddress clientAddress) {
        int tempID = -1;

        //find any matching IP addresses if they have previously connected
        Integer ID = addressSocketArray.get(clientAddress);
        if(ID != null) {
            tempID = ID;
        }

        if(tempID != -1) {
            Log.d(TAG, "Reconnecting Student: " + tempID);
            Log.d(TAG, "Is user reconnecting: " + true);
            studentThreadArray.remove(tempID);
            return new ClientResult(tempID, true);
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
        addressSocketArray.clear();
        clientID = 0;
    }

    private static void setArrays() {
        clientSocketArray = new HashMap<>();
        studentThreadArray = new HashMap<>();
        addressSocketArray = new HashMap<>();
    }

    private static void disconnection() {
        clientSocketArray.forEach((key, value) -> backgroundExecutor.submit(() ->
                sendLearnerMessage(key, value.getKey(), "DISCONNECT" + ","))
        );
    }

    /**
     * Remove a single student from all hashmaps. Use this when manually disconnecting a single
     * student or if there is a crash on a device.
     * @param clientID An integer representing the ID of the learner to remove.
     */
    public static void removeStudent(int clientID) {
        if(studentThreadArray.get(clientID) != null) {
            Objects.requireNonNull(studentThreadArray.get(clientID)).tcp.shutdownTCP();
            studentThreadArray.remove(clientID);
        }

        clientSocketArray.remove(clientID);
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
        if(isGuide) {resetClientIDs();}
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
