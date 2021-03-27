package com.lumination.leadme;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.view.View;

import com.google.android.gms.nearby.connection.Payload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    NsdManager mNsdManager = null;
    NsdServiceInfo mService;
    NsdManager.DiscoveryListener mDiscoveryListener = null;
    NsdManager.RegistrationListener mRegistrationListener = null;


    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String TAG = "NetworkAdapter";
    public String mServiceName = "LeadMe";
    public String Name = "Temp";


    ServerSocket mServerSocket = null; //server socket for server
    Socket socket = null;//server socket for client
    Thread mThread = null;
    public int localport = -1;
    public boolean allowConnections = true;
    public boolean allowInput = true;
    int clientID = 0;
    boolean pingName = true;
    boolean closeSocket = false;
    int connectionisActive = 0;


    public ArrayList<studentThread> clientThreadList = new ArrayList<>();
    public ArrayList<client> currentClients = new ArrayList<>();
    public ArrayList<NsdServiceInfo> discoveredLeaders = new ArrayList<>();

    public ExecutorService executorService = Executors.newFixedThreadPool(1);
    ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1);


    public NetworkAdapter(Context context, LeadMeMain main, NearbyPeersManager nearbyPeersManager) {
        closeSocket = false;
        Name = nearbyPeersManager.getName();
        mContext = context;
        netAdapt = this;
        this.nearbyPeersManager = nearbyPeersManager;
        this.main = main;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        //
        startServer();
        //main.startServer();
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
                }

                @Override
                public void onServiceFound(NsdServiceInfo service) {
                    Log.d(TAG, "Service discovery success" + service);
                    if (!service.getServiceType().equals(SERVICE_TYPE)) {
                        Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                    } else if (service.getServiceName().equals(Name)) {
                        Log.d(TAG, "Same machine: " + Name);
                    } else if (service.getServiceName().contains("#Teacher")) {
                        Log.d(TAG, "onServiceFound: attempting to resolve " + service.getServiceName());
                        mNsdManager.resolveService(service, new resListener());
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
            if (errorCode == 3) {
                mNsdManager.resolveService(serviceInfo, new resListener());
            }
            return;
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
            mService = serviceInfo;
            if (!discoveredLeaders.contains(serviceInfo)) {
                discoveredLeaders.add(serviceInfo);
            }
            List<String> leader = Arrays.asList(serviceInfo.getServiceName().split("#"));

            //add to the leaders list
            main.runOnUiThread(() -> {
                main.getLeaderSelectAdapter().addLeader(new ConnectedPeer(leader.get(0), serviceInfo.getHost().toString()));
                main.showLeaderWaitMsg(false);
            });
            return;
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
        if (socket != null) {
            return socket.isConnected();
        } else {
            return false;
        }
    }

    public void connectToServer(NsdServiceInfo serviceInfo) {
        if (socket == null) {
            try {
                Log.d(TAG, "connectToServer: attempting to connect to " + serviceInfo.getHost() + ":" + serviceInfo.getPort());
                socket = new Socket(serviceInfo.getHost(), serviceInfo.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (socket != null) {
                if (socket.isConnected()) {
                    connectionisActive = 20;
                    allowInput = true;
                    Log.d(TAG, "connectToServer: connection successful");
                    Name = nearbyPeersManager.getName();
                    sendToServer(Name, "NAME"); //sends the student name to the teacher for a record

                    main.runOnUiThread(() -> {
                        main.findViewById(R.id.client_main).setVisibility(View.VISIBLE);
                        List<String> inputList = Arrays.asList(serviceInfo.getServiceName().split("#"));
                        main.setLeaderName(inputList.get(0));
                        //main.startServer();
                    });
                }
            }
        } else if (!socket.isConnected()) {
            Log.d(TAG, "connectToServer: Socket disconnected attempting to reconnect");
            socket = null;
            if (tries <= 10) {
                tries++;
                connectToServer(serviceInfo);
            }
            Log.d(TAG, "connectToServer: reconnection unsuccessful");
        } else if (socket.isConnected()) {
            if (socket.getInetAddress().equals(serviceInfo.getHost()) && socket.getPort() == serviceInfo.getPort()) {
                Log.d(TAG, "connectToServer: socket already connected");
                try {
                    socket = new Socket(serviceInfo.getHost(), serviceInfo.getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (socket != null) {
                    if (socket.isConnected()) {
                        connectionisActive = 20;
                        Log.d(TAG, "connectToServer: connection successful");
                        Name = nearbyPeersManager.getName();
                        sendToServer(Name, "NAME"); //sends the student name to the teacher for a record

                        main.runOnUiThread(() -> {
                            main.findViewById(R.id.client_main).setVisibility(View.VISIBLE);
                            List<String> inputList = Arrays.asList(serviceInfo.getServiceName().split("#"));
                            main.setLeaderName(inputList.get(0));
                            //main.startServer();
                        });
                    }
                }
            } else {
                Log.d(TAG, "connectToServer: connected to : " + socket.getInetAddress() + ":" + socket.getPort());
                Log.d(TAG, "connectToServer: connecting to :" + serviceInfo.getHost() + ":" + serviceInfo.getPort());
                try {
                    socket = new Socket(serviceInfo.getHost(), serviceInfo.getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.d(TAG, "connectToServer: not really sure how we ended up here");
        }
        scheduledExecutor.shutdown();
        scheduledExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    if (socket.isConnected()) {
                        checkConnection();
                    }
                }
                if (socket != null && pingName) {
                    if (socket.isConnected()) {
                        //Log.d(TAG, "run: pinging parent");
                        Name = nearbyPeersManager.getName();
                        sendToServer(Name, "NAME");
                    }
                }
            }
        }, 0, 8000, TimeUnit.MILLISECONDS);
    }

    /*
    Sends message from student to Teacher
     */
    public void sendToServer(String message, String type) {
        if (socket != null) {
            if (socket.isConnected()) {
//                Thread thread = new Thread() {//no network on main thread
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        PrintWriter out;
                        try {
                            out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(type + "," + message.replace("\n", "_").replace("\r", "|"));
                            Log.d(TAG, "sendToServer: message sent, type: " + type + " message: " + message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            Thread.currentThread().sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
//                @Override
//                    public void run() {
//
//                    }
//                };
//                thread.start();
            }
        }
    }

    public void removeClient(int id) {
        for (int i = 0; i < clientThreadList.size(); i++) {
            if (clientThreadList.get(i).ID == id) {
                ArrayList<Integer> selected = new ArrayList<>();
                selected.add(id);
                sendToSelectedClients("disconnect", "DISCONNECT", selected);
                clientThreadList.get(i).t.interrupt();
                clientThreadList.remove(i);
                Log.d(TAG, "removeClient: client successfully removed");
                break;
            }
        }
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

    private void checkConnection() {
        //Log.d(TAG, "checkConnection: checking connection");
        if (connectionisActive > 0) {
            connectionisActive--;
        } else if (connectionisActive == 0) {
            main.runOnUiThread(() -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
                Log.d(TAG, "checkConnection: connection timed out");
                main.setUIDisconnected();
                connectionisActive--;
            });
        }
    }

    //discovers services, is not continuous so will need to be called in a runnable to implement a scan
    public void startDiscovery() {
        discoveredLeaders = new ArrayList<>();
        //Name = text.getText().toString(); //TODO swap with actual name in Leadme
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();

        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        //Updates parent with the name, this acts as a ping mechanism.
        //on the fly name changes are supported, client is identified by assigned ID


        //new Thread so server messages can be read from a while loop without impacting the UI
        Thread thread = new Thread() {
            @Override
            public void run() {

                while (allowInput) {
                    if (socket != null) {
                        if (!socket.isClosed() && !socket.isInputShutdown()) {
                            BufferedReader in;
                            String input = "";
                            try {
                                InputStreamReader inStream = new InputStreamReader(socket.getInputStream());
                                in = new BufferedReader(inStream);
                                try {
                                    input = in.readLine();
                                } catch (SocketException e) {
                                    if (socket != null) {
                                        socket.close();
                                    }
                                    e.printStackTrace();
                                    return;
                                }
//                            if(inStream.read()==-1){
//                                Log.d(TAG, "Disconnected: The teacher has disconnected");
//                            }else {
                                if (input != null) {
                                    if (input.length() > 0) {
                                        //Log.d(TAG, "run: server said: " + input);
                                        netAdapt.messageRecievedFromServer(input);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            socket = null;
                            main.runOnUiThread(() -> {
                                main.setUIDisconnected();
                            });
                        }
                    }
                }
            }
        };
        thread.start();

    }

    //stops any discovery processes that are inprogress
    public void stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
            }
            mDiscoveryListener = null;
        }
        Log.d(TAG, "stopDiscovery: has been deprecated");
    }

    /*
    splits message over the comma into type and message use as per below:
    COMMUNICATION: simply prints the message on the client is used for nothing else
    ACTION: used for controlling the client with different actions, app launches, etc
    PING: used to let the client know it is still recieving data from the server and helps keep the connection alive
     */
    public void messageRecievedFromServer(String input) {
        List<String> inputList = Arrays.asList(input.split(","));
        switch (inputList.get(0)) {
            case "COMMUNICATION":
                Log.d(TAG, "messageRecievedFromServer: " + inputList.get(1));
                if (inputList.get(1).length() > 6 && inputList.get(1).contains("Thanks")) {
                    main.closeWaitingDialog(true);
                    pingName = false;
                }

                //pingName=false;
                break;
            case "ACTION":
                byte[] bytes = new byte[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bytes = Base64.getDecoder().decode(inputList.get(1).getBytes());
                } else {
                    bytes = android.util.Base64.decode(inputList.get(1), android.util.Base64.DEFAULT);
                }
                //extract the parcel and package it into a payload to integrate with the pre existing functions
                Parcel p = Parcel.obtain();
                p.unmarshall(bytes, 0, bytes.length);
                p.setDataPosition(0);
                Log.d(TAG, "messageRecievedFromServer: " + p.readString());
                Payload payload = fromBytes(bytes);
                main.runOnUiThread(() -> {
                    main.handlePayload(payload.asBytes());
                });

                break;
            case "DISCONNECT":
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
                nearbyPeersManager.disconnectFromEndpoint("");

                break;
            case "PING":
                nearbyPeersManager.myID = inputList.get(1);
                connectionisActive = 3;
                if (main.waitingDialog.isShowing()) {
                    main.closeWaitingDialog(true);
                }
                pingName = false;
                //Log.d(TAG, "messageRecievedFromServer: recieved ping and subsequently ignoring it");
                break;
            case "MONITOR":
                if (inputList.get(1).contains(":")) {
                    List<String> inputList2 = Arrays.asList(inputList.get(1).split(":"));
                    if (inputList2.get(0).equals("START")) {
                        main.runOnUiThread(() -> {
                            main.startServer();
                            //main.monitorInProgress=true;
                            //main.takeScreenshots=true;
                            //main.startImageClient(String.valueOf(clientID));
                            main.startScreenshotRunnable(socket.getInetAddress(), Integer.parseInt(inputList2.get(1)));

                        });
                    }
                    Log.d(TAG, "messageRecievedFromServer: " + inputList.get(1));
                } else {
                    if (inputList.get(1).equals("STOP")) {
                        //main.takeScreenshots=false;
                        main.stopServer();
                        main.stopScreenshotRunnable();
                    } else {
                        main.setScreenshotRate(Integer.parseInt(inputList.get(1)));
                    }
                }
                break;
            default:
                Log.d(TAG, "messageRecievedFromServer: Invalid message type");
                break;
        }
    }


    //getter for the serviceinfo, only useful for student to teacher connections
    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }


    /*
    Server Functions Below:
     */

    //necessary to be on a seperate thread as it runs an eternal server allowing any connections that come in
    class ServerThread implements Runnable {

        @Override
        public void run() {

            try {
                // Since discovery will happen via Nsd, we don't need to care which port is
                // used.  Just grab an available one  and advertise it via Nsd.
                mServerSocket = new ServerSocket(0);
                localport = mServerSocket.getLocalPort();

                //while (!Thread.currentThread().isInterrupted()) {
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
                    TcpClient tcp = new TcpClient(clientSocket, netAdapt, clientID);
                    Thread client = new Thread(tcp); //new thread for every client
                    studentThread st = new studentThread();
                    st.t = client;
                    st.ID = clientID;
                    clientID++;
                    clientThreadList.add(st); //threads are saved in an array incase they need to be accessed for any reason
                    clientThreadList.get(clientThreadList.size() - 1).t.start();
                    Log.d(TAG, "Connected.");
                }
                //}
            } catch (IOException e) {
                Log.e(TAG, "Error creating ServerSocket: ", e);
                e.printStackTrace();
            }
        }


    }

    public void startAdvertising() {
        Name = nearbyPeersManager.getName();
        stopAdvertising();  // Cancel any previous registration request
        initializeRegistrationListener();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        if (localport < 0) {
            Log.d(TAG, "startAdvertising: ERROR - Server not started");
        }
        serviceInfo.setPort(localport);
        serviceInfo.setServiceName(Name + "#Teacher");
        serviceInfo.setServiceType(SERVICE_TYPE);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        //startServer();

    }

    //only stops the service from being discoverable and should not drop current connections
    public void stopAdvertising() {
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
    }

    //starts a new server thread as seen below
    public void startServer() {
        mThread = new Thread(new ServerThread());
        mThread.start();
    }

    /*
    called by the TCP clients to update the parent thread with information
    types defined as such:
    NAME: client server pings name through on first use it will update the parent with the clients name,
          if its changed then subsequent run throughs will rectify that. Also acts as a pinging, once name
          is recieved a response will be sent letting the student know they are still connected.
     */
    boolean imgInProgress = false;

    public void updateParent(String message, int clientID, String type) {
        Log.d(TAG, "updateParent: ");
        switch (type) {
            case "NAME":
                boolean exists = false;
                for (int i = 0; i < currentClients.size(); i++) {
                    if (currentClients.get(i).ID == clientID) {
                        if (!currentClients.get(i).name.equals(message)) {
                            Log.d(TAG, "updateParent: " + currentClients.get(i).name + " has changed to " + message);
                            currentClients.get(i).name = message;
                        }
                        exists = true;
                        currentClients.get(i).pingCycle = 1;
                        Log.d(TAG, "updateParent: ID:" + clientID + " name: " + message + " is active");
                        ArrayList<Integer> selected = new ArrayList<>();
                        selected.add(clientID);
                        sendToSelectedClients("Thanks " + message, "COMMUNICATION", selected);//lets client know their name has been saved
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
                break;
            case "DISCONNECT":
                //todo fix waiting on clients message
                Log.d(TAG, "updateParent: student has been disconnected");
                for (int i = 0; i < currentClients.size(); i++) {
                    Log.d(TAG, "updateParent: ");
                    if (currentClients.get(i).ID == clientID) {
                        Log.d(TAG, "updateParent: ");
                        currentClients.remove(i);
                        if (currentClients.size() == 0) {
                            main.runOnUiThread(() -> {
                                main.waitingForLearners.setVisibility(View.VISIBLE);
                            });
                        } else {
                            Log.d(TAG, "updateParent: " + currentClients.size() + " remaining students");
                        }
                        return;
                    }
                }
                break;
            case "LOST":
                //todo use shared preferences to try and reconnect students to the same server
                Log.d(TAG, "updateParent: client:" + clientID + " has lost connection");
                main.runOnUiThread(() -> {
                    if (main.getConnectedLearnersAdapter().getMatchingPeer(String.valueOf(clientID)) != null) {
                        if (main.getConnectedLearnersAdapter().getMatchingPeer(String.valueOf(clientID)).getStatus() != ConnectedPeer.STATUS_ERROR) {
                            main.getConnectedLearnersAdapter().updateStatus(String.valueOf(clientID), ConnectedPeer.STATUS_ERROR);
                        }
                    }
                });
                break;
            case "ACTION":
                byte[] bytes = new byte[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bytes = Base64.getDecoder().decode(message);
                } else {
                    bytes = android.util.Base64.decode(message, android.util.Base64.DEFAULT);
                }
                //extract the parcel and package it into a payload to integrate with the pre existing functions
                Parcel p = Parcel.obtain();
                p.unmarshall(bytes, 0, bytes.length);
                p.setDataPosition(0);
                Log.d(TAG, "messageRecievedFromServer: " + p.readString());
                Payload payload = fromBytes(bytes);
                main.runOnUiThread(() -> {
                    main.handlePayload(payload.asBytes());
                });
                break;
            default:
                Log.d(TAG, "updateParent: invalid type: " + type + " message: " + message);
                break;
        }
    }

    //will drop all currently connected clients
    public void stopServer() {
        mThread.interrupt();
        try {
            mServerSocket.close();
        } catch (IOException ioe) {
            Log.e(TAG, "Error when closing server socket.");
        }
    }

    /*
    both functions add messages to the message queue which is then checked by each student thread
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
}
