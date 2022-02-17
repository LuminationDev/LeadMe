package com.lumination.leadme;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.view.View;

import com.google.android.gms.nearby.connection.Payload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.nearby.connection.Payload.fromBytes;

class studentThread {
    Thread t;
    int ID;
}

class client {
    String name;
    int ID;
    int pingCycle = 1;
    ArrayList<msg> messageQueue = new ArrayList<>();//fifo structure
}

class msg {
    String message;
    String type;
}

public class NetworkAdapter {
    Context mContext;
    NetworkAdapter netAdapt;
    NearbyPeersManager nearbyPeersManager;
    LeadMeMain main;

    NsdManager mNsdManager;
    NsdServiceInfo mService;
    NsdManager.DiscoveryListener mDiscoveryListener = null;
    NsdManager.RegistrationListener mRegistrationListener = null;

    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String TAG = "NetworkAdapter";
    public String mServiceName = "LeadMe";
    public String Name;

    public ServerSocket mServerSocket = null; //server socket for server
    Socket clientsServerSocket = null;//server socket for client

    Future<?> Server = null;
    Future<?> receiveInput = null;

    public int localport = -1;
    public boolean allowConnections = true;
    public boolean allowInput = true;
    boolean pingName = true;
    int clientID = 0;
    int connectionIsActive = 0;
    int timeOut = 2;
    int PORT = 54321; //port used for connection

    /**
     * Keep track of the student threads that are currently being managed by the leader device.
     */
    public HashMap<Integer, studentThread> studentThreadArray;
    /**
     * Keep Track of the Client ID as the key and student socket as the value. It can then be used
     * to determine if a student is reconnecting or is a new user.
     */
    HashMap<Integer, Map.Entry<Socket, Boolean>> clientSocketArray;

    // Acquire multicast lock
    WifiManager.MulticastLock multicastLock;

    public ArrayList<client> currentClients = new ArrayList<>();
    public ArrayList<NsdServiceInfo> discoveredLeaders = new ArrayList<>();

    public ExecutorService executorService = Executors.newFixedThreadPool(2);
    private ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1);


    public NetworkAdapter(Context context, LeadMeMain main, NearbyPeersManager nearbyPeersManager) {
        this.nearbyPeersManager = nearbyPeersManager;
        this.main = main;
        Name = nearbyPeersManager.getName();
        mContext = context;
        netAdapt = this;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        studentThreadArray = new HashMap<>();
        clientSocketArray = new HashMap<>();
        setMulticastLock(); //should be set for learners and leaders?
    }

    /**
     * Acquire multicast lock - required for pre API 11 and some devices.
     */
    private void setMulticastLock() {
        WifiManager wifi = (WifiManager) main.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();
    }

    /*
    Listens for services on the network and will send any that end in #Teacher through to the resolve listener
     */
    public void initializeDiscoveryListener() {
        if (mDiscoveryListener == null) {
            mDiscoveryListener = new NsdManager.DiscoveryListener() {

                @Override
                public void onDiscoveryStarted(String regType) {
                    Log.d(TAG, "Service discovery started");
                    main.runOnUiThread(() -> {
                        ArrayList<ConnectedPeer> temp = new ArrayList<>();
                        main.getLeaderSelectAdapter().setLeaderList(temp);
                        main.showLeaderWaitMsg(true);
                    });
                }

                @Override
                public void onServiceFound(NsdServiceInfo service) {
                    Log.d(TAG, "Service discovery success " + service);

                    if (!service.getServiceType().equals(SERVICE_TYPE)) {
                        Log.d(TAG, "Unknown Service Type: " + service.getServiceType());

                    } else if (service.getServiceName().equals(Name)) {
                        Log.d(TAG, "Same machine: " + Name);

                    } else if (service.getServiceName().contains("#Teacher")) {
                        Log.d(TAG, "onServiceFound: attempting to resolve " + service.getServiceName());

                        //fixes the resolve 3 error
                        try {
                            executorService.submit(() -> mNsdManager.resolveService(service, new resListener()));
                            Thread.currentThread();
                            Thread.sleep(200); //purposely block
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onServiceLost(NsdServiceInfo service) {
                    Log.e(TAG, "service lost" + service);
                    if (mService == service) {
                        mService = null;
                    }
                    //clear the list and then scan again
                    main.runOnUiThread(() -> {
                        ArrayList<ConnectedPeer> temp = new ArrayList<>();
                        main.getLeaderSelectAdapter().setLeaderList(temp);
                        main.showLeaderWaitMsg(true);
                    });
                    startDiscovery();
                }

                @Override
                public void onDiscoveryStopped(String serviceType) {
                    Log.i(TAG, "Discovery stopped: " + serviceType);
                }

                @Override
                public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                }

                @Override
                public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                }
            };
        }
    }


    /*
    Resolves services found by the discovery listener, will check if is same machine
    if not will return and info object that contains IP address and port
     */
    public class resListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

            //stop it from detecting its own service, not relevant for LeadMe as it is either in discovery or advertising mode
            if (serviceInfo.getServiceName().equals(Name)) {
                Log.d(TAG, "Same IP.");
                return;
            }

            Log.d(TAG, "onServiceResolved: " + serviceInfo);

            executorService.submit(() -> {
                resolveSingleService(serviceInfo);
            });
        }
    }

    /**
     * Add the details of the new service to the leader list.
     * @param serviceInfo An NsdServiceInfo object relating to the newly found service.
     */
    private void resolveSingleService(NsdServiceInfo serviceInfo) {
        if (!discoveredLeaders.contains(serviceInfo)) {
            Log.d(TAG, "run: added leader");
            InetAddress hardCoded = null, hardCoded_alt = null;

            //Get the IP address from the name
            List<String> alt = Arrays.asList(serviceInfo.getServiceName().split("#"));

            try {
                //Get the hard coded IP address set across in the Service info attributes
                hardCoded = InetAddress.getByName(new String(serviceInfo.getAttributes().get("IP")));
                hardCoded_alt = InetAddress.getByName(alt.get(2)); //Used as a backup and for comparison
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            //Override the captured IP address with the hardcoded one
            if(hardCoded != null) {
                serviceInfo.setHost(hardCoded);
            } else if (hardCoded_alt != null) {
                serviceInfo.setHost(hardCoded_alt);
            }

            serviceInfo.setServiceName(alt.get(0) + "#Teacher");
            discoveredLeaders.add(serviceInfo);
        }

        addToLeaderList(serviceInfo);
    }

    /**
     * Add a leader to the UI leader list for learner devices to connect to.
     */
    private void addToLeaderList(NsdServiceInfo serviceInfo) {
        List<String> leader = Arrays.asList(serviceInfo.getServiceName().split("#"));

        if(!main.sessionManual) {
            main.runOnUiThread(() -> {
                main.getLeaderSelectAdapter().addLeader(new ConnectedPeer(leader.get(0), serviceInfo.getHost().toString()));
                main.showLeaderWaitMsg(false);
            });
        }
    }

    /*
    Initialises the registration listener allowing us to register the service
     */
    public void initializeRegistrationListener() {
        if (mRegistrationListener == null) {
            mRegistrationListener = new NsdManager.RegistrationListener() {

                @Override
                public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                    mServiceName = NsdServiceInfo.getServiceName();
                    Log.d(TAG, "Service registered: " + mServiceName);
                }

                @Override
                public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                    Log.d(TAG, "Service registration failed: " + arg1);
                }

                @Override
                public void onServiceUnregistered(NsdServiceInfo arg0) {
                    Log.d(TAG, "Service unregistered: " + arg0.getServiceName());
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.d(TAG, "Service unregistration failed: " + errorCode);
                }
            };
        }
    }

    int tries = 0; //counter for connection attempts to server

    /*
    When teacher is found the info can be handed over to this to initialise the socket
    - socket will return true to isConnected if it has ever had a successful connection
     */
    public boolean isConnected() {
        if (clientsServerSocket != null) {
            return clientsServerSocket.isConnected();
        }
        return false;
    }

    public boolean isServerRunning() {
        if (mServerSocket != null) {
            return !mServerSocket.isClosed();
        }
        return false;
    }

    /**
     * Using a supplied NsdService connect to a server using the details provided.
     * @param serviceInfo An NsdServiceInfo object containing the details about the selected leader.
     */
    public void connectToServer(NsdServiceInfo serviceInfo) {
        Log.d(TAG, "connectToServer: attempting to connect to " + serviceInfo.getHost() + ":" + serviceInfo.getPort());
        Log.e(TAG, serviceInfo.toString());

        mService = serviceInfo;

        try {
            clientsServerSocket = new Socket(mService.getHost(), mService.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (clientsServerSocket == null) {
                tryReconnect();
            }
        }

        if (clientsServerSocket != null) {
            if (clientsServerSocket.isConnected()) {
                connectionIsActive = 20;
                allowInput = true;
                Log.d(TAG, "connectToServer: connection successful");
                Name = nearbyPeersManager.getName();
                sendToServer(Name, "NAME"); //sends the student name to the teacher for a record
                stopDiscovery();
                main.runOnUiThread(() -> {
                    main.findViewById(R.id.client_main).setVisibility(View.VISIBLE);
                    List<String> inputList = Arrays.asList(mService.getServiceName().split("#"));
                    main.setLeaderName(inputList.get(0));
                });
            }

            startConnectionCheck();
        }
    }

    /**
     * If the initial connection attempt fails try again before sending the learner back to the
     * splash screen.
     */
    private void tryReconnect() {
        if (!clientsServerSocket.isConnected()) {
            Log.d(TAG, "connectToServer: Socket disconnected attempting to reconnect");
            clientsServerSocket = null;
            if (tries <= 10) {
                tries++;
                connectToServer(mService);
            } else {
                Log.d(TAG, "connectToServer: reconnection unsuccessful");
                main.runOnUiThread(() -> main.setUIDisconnected());
            }
        }
    }

    /**
     * Start an executor to continually check if the leader has disconnected or is still active. If
     * not then move the learner from a logged in state to the splash screen.
     */
    private void startConnectionCheck() {
        if(clientsServerSocket != null) {
            scheduledExecutor.shutdown();
            scheduledExecutor = new ScheduledThreadPoolExecutor(1);
            scheduledExecutor.scheduleAtFixedRate(() -> {
                if (clientsServerSocket != null) {
                    if (clientsServerSocket.isConnected()) {
                        checkConnection();
                    }
                }

                if (clientsServerSocket != null && pingName) {
                    if (clientsServerSocket.isConnected()) {
                        Name = nearbyPeersManager.getName();
                        sendToServer(Name, "NAME");
                    }
                }
            }, 1, 8000, TimeUnit.MILLISECONDS);
        }
    }


    private void checkConnection() {
        if (connectionIsActive > 0) {
            connectionIsActive--;
            Log.d(TAG, "checkConnection: connection " + connectionIsActive);
        } else if (connectionIsActive == 0) {
            Log.d(TAG, "checkConnection: connection " + connectionIsActive);
            main.runOnUiThread(() -> {
                try {
                    clientsServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientsServerSocket = null;
                Log.d(TAG, "checkConnection: connection timed out");
                main.setUIDisconnected();
                connectionIsActive--;
            });
        }
    }

    /*
    Sends message from student to Teacher
     */
    public void sendToServer(String message, String type) {
        if (clientsServerSocket != null) {
            if (clientsServerSocket.isConnected()) {
                executorService.submit(() -> {
                    PrintWriter out;
                    try {
                        out = new PrintWriter(clientsServerSocket.getOutputStream(), true);
                        out.println(type + "," + message.replace("\n", "_").replace("\r", "|"));
                        Log.d(TAG, "sendToServer: message sent, type: " + type + " message: " + message);
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                return;
            }
        }
        Log.e(TAG, "FAILED! Tried to send from " + main.getNearbyManager().getName() + " to server >> " + message + ", " + type);
    }

    /**
     * Disconnect a student from the leaders side. Sends an action to the client to know they
     * have been disconnected.
     * @param id An int representing the ID of the learner who is being disconnected.
     */
    public void removeClient(int id) {
        ArrayList<Integer> selected = new ArrayList<>();
        selected.add(id);
        sendToSelectedClients("disconnect", "DISCONNECT", selected);
        Log.d(TAG, "removeClient: client successfully removed");
        for (int i = 0; i < currentClients.size(); i++) {
            if (currentClients.get(i).ID == id) {
                //currentClients.remove(i);
                if (currentClients.size() == 0) {
                    main.waitingForLearners.setVisibility(View.VISIBLE);
                }
                return;
            }
        }
    }

//    int testing = 20;
    /**
     * Discovers services, is not continuous so will need to be called in a runnable to implement a scan
     */
    public void startDiscovery() {
        Log.d(TAG, "startDiscovery: ");
        discoveredLeaders = new ArrayList<>();
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();

        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        //Updates parent with the name, this acts as a ping mechanism.
        //on the fly name changes are supported, client is identified by assigned ID

        //new Thread so server messages can be read from a while loop without impacting the UI
        if(receiveInput == null) {
            receiveInput = main.serverThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    while (allowInput) {
                        if (clientsServerSocket != null) {
                            if (!clientsServerSocket.isClosed() && !clientsServerSocket.isInputShutdown()) {

                                BufferedReader in;
                                String input = "";
                                try {
                                    InputStreamReader inStream = new InputStreamReader(clientsServerSocket.getInputStream());
                                    in = new BufferedReader(inStream);

                                    try {
                                        //Comments below are for testing purposes
                                        //Only need the input = in.readLine() for production
//                                        Log.e(TAG, "TESTING COUNTDOWN: " + testing);
//                                        if (testing == 0) {
//                                            Log.e(TAG, "Throwing exception");
//                                            testing = 20;
//
//                                            throw new SocketException();
//                                        } else {
//                                            testing--;
                                            input = in.readLine();
//                                        }
                                    } catch (SocketException e) {
                                        if (clientsServerSocket != null) {
                                            clientsServerSocket.close();
                                            clientsServerSocket = null;
                                        }

                                        e.printStackTrace();
                                        Log.e(TAG, "FAILED! {1}");
                                        Log.e(TAG, "Attempting to reconnect");

                                        connectToServer(mService);
                                    }

                                    if (input != null) {
                                        if (input.length() > 0) {
                                            Log.d(TAG, "allowInput is active");

                                            if (main.destroying) {
                                                if (clientsServerSocket != null) {
                                                    clientsServerSocket.close();
                                                    clientsServerSocket = null;
                                                }

                                                scheduledExecutor.shutdown();
                                                allowInput = false;
                                                return;
                                            }

                                            netAdapt.messageReceivedFromServer(input);
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "FAILED! {2}");
                                }
                            } else {
                                clientsServerSocket = null;
                                Log.e(TAG, "FAILED! {3}");
                                Log.e(TAG, "Attempting to reconnect");

                                connectToServer(mService);
                            }
                        }
                    }
                    Log.e(TAG, "FAILED! {4}");
                }
            });
        }
    }

    //stops any discovery processes that are in progress
    public void stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
                mDiscoveryListener = null;
            }
        }
        Log.d(TAG, "stopDiscovery: has been deprecated");
    }

    /*
    splits message over the comma into type and message use as per below:
    COMMUNICATION: simply prints the message on the client is used for nothing else
    ACTION: used for controlling the client with different actions, app launches, etc
    PING: used to let the client know it is still receiving data from the server and helps keep the connection alive
     */
    public void messageReceivedFromServer(String input) {
        Log.d(TAG, "messageReceivedFromServer: " + input);
        List<String> inputList = Arrays.asList(input.split(","));
        switch (inputList.get(0)) {
            case "COMMUNICATION":
                receivedCommunication(inputList.get(1));
                break;

            case "ACTION":
                receivedAction(inputList.get(1));
                break;

            case "PING":
                receivedPing(inputList.get(1));
                break;

            case "FILE":
                receivedFile(inputList.get(1));
                break;

            case "DISCONNECT":
                receivedDisconnect();
                break;

            default:
                Log.d(TAG, "messageReceivedFromServer: Invalid message type");
                break;
        }
    }

    /**
     * Decipher what communication has been sent from the server.
     * @param input A string representing from the communication from the server.
     */
    private void receivedCommunication(String input) {
        Log.d(TAG, "messageReceivedFromServer: [COMM] " + input);
        if (input.length() > 6 && input.contains("Thanks")) {
            main.closeDialogController(true);
            pingName = false;
        }
    }

    /**
     * Decide what action should be taken depending on what is contained within the input string.
     * @param input A string containing the action to be handled by the learner device.
     */
    private void receivedAction(String input) {
        byte[] bytes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bytes = Base64.getDecoder().decode(input.getBytes());
        } else {
            bytes = android.util.Base64.decode(input, android.util.Base64.DEFAULT);
        }
        //extract the parcel and package it into a payload to integrate with the pre existing functions
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        Payload payload = fromBytes(bytes);

        final String timestamp = System.currentTimeMillis() + "MS";
        Log.d(TAG, timestamp + "]] messageReceivedFromServer: [ACTION] " + p.readString() + ", " + payload);
        //main.getHandler().removeCallbacks(null);
        main.getHandler().postAtFrontOfQueue(() -> {
            Log.d(TAG, timestamp + "]] messageReceivedFromServer: [ACTION] INSIDE MAIN THREAD");
            main.handlePayload(payload.asBytes());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, timestamp + "]] done!");
        });
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        p.recycle();
    }

    /**
     * Ignore the ping from the server and reset the timeOut limit for the learners socket
     * connection.
     * @param input A string containing the ID of the device being pinged.
     */
    private void receivedPing(String input) {
        nearbyPeersManager.myID = input;
        connectionIsActive = timeOut;
        Log.d(TAG, "messageReceivedFromServer: PING!!");
        pingName = false;
        Log.d(TAG, "messageReceivedFromServer: received ping and subsequently ignoring it");
    }

    /**
     * Connect to the file server to receive the file from the leader.
     * @param input A String containing the PORT of the file server.
     */
    private void receivedFile(String input) {
        List<String> inputList2 = Arrays.asList(input.split(":"));
        Log.e(TAG, clientsServerSocket.getInetAddress() + " : " + inputList2);
        if(main.fileTransferEnabled) {
            FileTransfer.setFileType(inputList2.get(2));
            main.getFileTransfer().receivingFile(clientsServerSocket.getInetAddress(), Integer.parseInt(inputList2.get(1)));
        } else {
            main.permissionDenied(LeadMeMain.FILE_TRANSFER);
        }
    }

    /**
     * If the device is not connected as a guide, disconnect from all endpoints.
     */
    private void receivedDisconnect() {
        Log.w(TAG, "Disconnect. Guide? " + main.isGuide);
        try {
            clientsServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientsServerSocket = null;
        nearbyPeersManager.disconnectFromEndpoint("");
    }


    /**
     * Getter for the serviceInfo, only useful for student to teacher connections.
     * @return An instance of the current service info.
     */
    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }


    /*
    Server Functions Below:
     */

    //necessary to be on a separate thread as it runs an eternal server allowing any connections that come in
    class ServerThread implements Runnable {

        @Override
        public void run() {

            try {
                // Since discovery will happen via Nsd, we don't need to care which port is
                // used.  Just grab an available one and advertise it via Nsd.
                mServerSocket = new ServerSocket();
                mServerSocket.setReuseAddress(true);
                mServerSocket.bind(new InetSocketAddress(PORT));

                localport = mServerSocket.getLocalPort();

                while (true) {
                    Log.d(TAG, "ServerSocket Created, awaiting connection");
                    Socket clientSocket = null;

                    try {
                        clientSocket = mServerSocket.accept();//blocks the thread until client is accepted
                    } catch (IOException e) {
                        if (!allowConnections) {
                            Log.d(TAG, "Server: server stopped");
                            return;
                        }
                        throw new RuntimeException("Error creating client", e);
                    }

                    Log.d(TAG, "run: client connected");

                    studentThreadManager(clientSocket);

                    Log.d(TAG, "Connected.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating ServerSocket: ", e);
                e.printStackTrace();
            }
        }
    }

    //starts a new server thread as seen below
    public void startServer() {
        Server = main.backgroundExecutor.submit(new ServerThread());
    }

    /**
     * Stop the leaders socket server. This will drop all currently connected clients.
     */
    public void stopServer() {
        if(Server != null) {
            Log.e(TAG, "Server cancel");
            Server.cancel(true);
        }

        if(mServerSocket != null) {
            try {
                mServerSocket.close();
                Log.e("CLOSING PORT", "Server closing is bound: " + mServerSocket.isBound());
                if (multicastLock != null) {
                    multicastLock.release();
                    multicastLock = null;
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Error when closing server socket.");
            }
        }
    }

    public void startAdvertising() {
        Name = nearbyPeersManager.getName();
        stopAdvertising();  // Cancel any previous registration request
        initializeRegistrationListener();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        //While using hardcoded PORT this should never be an issue... should
        if (localport < 0) {
            main.getDialogManager().showWarningDialog("Connection Issue",
                    "Invalid Port Number, please restart the application.");
            Log.d(TAG, "startAdvertising: ERROR - Server not started");
        }

        String hardIpAddress = null;
        try {
            hardIpAddress = InetAddress.getByAddress(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(main.wifiManager.getConnectionInfo().getIpAddress())
                            .array()
            ).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        serviceInfo.setPort(PORT); //Use the hard coded port
        serviceInfo.setServiceName(Name + "#Teacher#" + hardIpAddress);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setAttribute("IP", hardIpAddress);
        serviceInfo.setHost(mServerSocket.getInetAddress());

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    /**
     * Only stops the Nsd service from being discoverable and should not drop current connections.
     * This should only be called on logout as student should be able to connect at any point during
     * a session.
     */
    public void stopAdvertising() {
        if (mRegistrationListener != null) {
            try {
                Log.d(TAG, "Unregister nsd service");
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
                mRegistrationListener = null;
            }
        }
    }

    /**
     * called by the TCP clients to update the parent thread with information
     * types defined as such:
     * NAME: client server pings name through on first use it will update the parent with the clients name,
     *      if its changed then subsequent run throughs will rectify that. Also acts as a pinging, once name
     *      is received a response will be sent letting the student know they are still connected.
     * @param message A string containing information relevant to the switch case functions.
     * @param clientID An int representing which learner this update relates to.
     * @param type A string to determine what action is being taken.
     */
    public void updateParent(String message, int clientID, String type) {
        switch (type) {
            case "NAME":
                parentUpdateName(message, clientID);
                break;

            case "DISCONNECT":
                parentUpdateDisconnect(clientID);
                break;

            case "LOST":
                parentUpdateLost(clientID);
                break;

            case "ACTION":
                parentUpdateAction(message);
                break;

            default:
                Log.d(TAG, "updateParent: invalid type: " + type + " message: " + message);
                break;
        }
    }

    /**
     * Detect if the most recent connection is a reconnect or a new learner, handle any name changes
     * or client creation appropriately.
     * @param message A string containing the name of the learner and the IP address of the device,
     *                separated by a ':'.
     * @param clientID An integer representing the clients ID saved within the student class on a
     *                 leaders device.
     */
    private void parentUpdateName(String message, int clientID) {
        boolean exists = false;
        ArrayList<Integer> selected = new ArrayList<>();

        for (int i = 0; i < currentClients.size(); i++) {
            if (currentClients.get(i).ID == clientID) {
                if (!currentClients.get(i).name.equals(message)) {
                    Log.d(TAG, "updateParent: " + currentClients.get(i).name + " has changed to " + message);
                    currentClients.get(i).name = message;
                    String[] spilt = message.split(":");
                    main.getConnectedLearnersAdapter().getMatchingPeer(String.valueOf(clientID)).setName(spilt[0]);
                }
                exists = true;
                currentClients.get(i).pingCycle = 1;
                Log.d(TAG, "updateParent: ID:" + clientID + " name: " + message + " is active");
            }
        }

        if (!exists) {
            Log.d(TAG, "updateParent: creating new client: ID:" + clientID + " name is: " + message);
            client temp = new client();
            temp.name = message;
            temp.ID = clientID;
            temp.pingCycle = 1;
            currentClients.add(temp);
            NearbyPeersManager.Endpoint endpoint = new NearbyPeersManager.Endpoint();
            endpoint.name = message;
            endpoint.Id = String.valueOf(clientID);
            ConnectedPeer thisPeer = new ConnectedPeer(endpoint);
            main.runOnUiThread(() -> {
                main.getConnectedLearnersAdapter().addStudent(thisPeer);
                main.showConnectedStudents(true);
            });
        }

        selected.add(clientID);
        sendToSelectedClients("Thanks " + message, "COMMUNICATION", selected);//lets client know their name has been saved
    }

    /**
     * A learner has disconnected, remove the client from the clients array list and display the
     * waiting text if there are no longer any learners connected.
     * @param clientID An integer representing the clients ID saved within the student class on a
     *                 leaders device.
     */
    private void parentUpdateDisconnect(int clientID) {
        for (int i = 0; i < currentClients.size(); i++) {
            if (currentClients.get(i).ID == clientID) {
                Log.d(TAG, "updateParent: student has been disconnected: " + clientID);
                currentClients.remove(i);
                if (currentClients.size() == 0) {
                    main.runOnUiThread(() -> main.waitingForLearners.setVisibility(View.VISIBLE));
                } else {
                    Log.d(TAG, "updateParent: " + currentClients.size() + " remaining students");
                }
                return;
            }
        }
    }

    /**
     * A leader has lost connection with a learner device, determine if the learner is trying to
     * reconnect or if they have disconnected entirely.
     * @param clientID An integer representing the clients ID saved within the student class on a
     *                 leaders device.
     */
    private void parentUpdateLost(int clientID) {
        if(clientSocketArray.get(clientID).getValue()) {
            Log.d(TAG, "updateParent: client: " + clientID + " is reconnecting");
            clientSocketArray.get(clientID).setValue(false);
        } else {
            Log.d(TAG, "updateParent: client: " + clientID + " has lost connection");
            Objects.requireNonNull(studentThreadArray.get(clientID)).t.interrupt();
            currentClients.remove(clientID);
            main.getXrayManager().removePeerFromMap(String.valueOf(clientID));

            main.runOnUiThread(() -> {
                if (main.getConnectedLearnersAdapter().getMatchingPeer(String.valueOf(clientID)) != null) {
                    if (main.getConnectedLearnersAdapter().getMatchingPeer(String.valueOf(clientID)).getStatus() != ConnectedPeer.STATUS_ERROR) {
                        main.getConnectedLearnersAdapter().updateStatus(String.valueOf(clientID), ConnectedPeer.STATUS_ERROR);
                    }
                }
            });
        }
    }

    /**
     * Determine what action has been taken by a learner device and respond appropriately.
     * @param message A string representing the action a learner has taken.
     */
    private void parentUpdateAction(String message) {
        byte[] bytes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bytes = Base64.getDecoder().decode(message);
        } else {
            bytes = android.util.Base64.decode(message, android.util.Base64.DEFAULT);
        }
        //extract the parcel and package it into a payload to integrate with the pre existing functions
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        Log.d(TAG, "messageReceivedFromServer: " + p.readString());
        Payload payload = fromBytes(bytes);
        main.runOnUiThread(() -> {
            main.handlePayload(payload.asBytes());
        });
        p.recycle();
    }

    /**
     * Add messages to the message queue which is then checked by each student thread.
     */
    public void sendToSelectedClients(String message, String type, ArrayList<Integer> selectedClientIDs) {
        Log.d(TAG, "sendToSelectedClients: " + selectedClientIDs + " " + currentClients.size());
        msg Msg = new msg();
        Msg.message = message;
        Msg.type = type;

        for (int i = 0; i < currentClients.size(); i++) {
            Iterator siterator = selectedClientIDs.iterator();
            while (siterator.hasNext()) {
                int selected = (Integer) siterator.next();
                if (selected == currentClients.get(i).ID) {
                    currentClients.get(i).messageQueue.add(Msg);
                }
            }
        }

    }

    /**
     * Cycle through the student thread container. Interrupt each student thread running in
     * the background and remove it from the array. To be used on logout or session end to stop
     * hanging connections on the student devices.
     * Reset the client ID count on logout otherwise ID and xray ID do not match and client ID
     * consecutively grows
     */
    public void resetClientIDs() {
        for(Map.Entry<Integer, studentThread> entry : studentThreadArray.entrySet()) {
            entry.getValue().t.interrupt();
        }

        studentThreadArray.clear();
        clientSocketArray.clear();
        clientID = 0;
    }

    public void startMonitoring(int ID, int localPort) {
        ArrayList<Integer> selected = new ArrayList<>();
        selected.add(ID);
        sendToSelectedClients("START:" + localPort, "MONITOR", selected);
    }

    public void stopMonitoring(int ID) {
        ArrayList<Integer> selected = new ArrayList<>();
        selected.add(ID);
        sendToSelectedClients("STOP", "MONITOR", selected);
    }

    /**
     * File transfer case.
     * @param ID An int representing the client that the file is being sent to.
     * @param localPort An int representing the port to use for the transfer server.
     * @param fileType A string representing the type of file that is being transferred.
     */
    public void sendFile(int ID, int localPort, String fileType) {
        ArrayList<Integer> selected = new ArrayList<>();
        selected.add(ID);
        sendToSelectedClients("SEND:" + localPort + ":" + fileType,"FILE", selected);
    }

    /**
     * Manages what Client IDs are currently in use. Gets the first index equaling null and
     * assigned the a student thread to it.
     * @param clientSocket A socket object of the newly connected user.
     */
    public void studentThreadManager(Socket clientSocket) {
        Log.e(TAG, clientSocket.toString());

        ClientResult result = manageClientID(clientSocket);
        int ID = result.getID();
        boolean reconnect = result.getReconnect();

        Log.e(TAG, "Connecting Student: " + ID);

        TcpClient tcp = new TcpClient(clientSocket, netAdapt, ID);
        Thread client = new Thread(tcp); //new thread for every client
        studentThread st = new studentThread();
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
    public ClientResult manageClientID(Socket clientSocket) {
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

