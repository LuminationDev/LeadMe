package com.lumination.leadme.managers;

import static com.google.android.gms.nearby.connection.Payload.fromBytes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.view.View;

import com.google.android.gms.nearby.connection.Payload;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.services.NetworkService;
import com.lumination.leadme.connections.Client;
import com.lumination.leadme.connections.Endpoint;
import com.lumination.leadme.connections.Msg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for starting and stopping the network service holding the leaders server.
 * Manages the learners connection to the server.
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";

    private static LeadMeMain main;
    private static WifiManager.MulticastLock multicastLock; // Acquire multicast lock
    private static Socket clientsServerSocket = null;//server socket for client
    private int tries = 0; //counter for connection attempts to server
    private final int timeOut = 2;
    private boolean init = false; //check if connection has been initialised
    public static boolean pingName = true;
    public static boolean allowInput = true;
    public static int connectionIsActive = 0;

    public static ArrayList<Client> currentClients = new ArrayList<>();

    private static ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    private static ThreadPoolExecutor socketThreadPool;
    private static final ThreadPoolExecutor connectionThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    public NetworkManager(LeadMeMain main) {
        NetworkManager.main = main;
    }

    /**
     * Start the server socket on a guides device.
     */
    public void startService() {
        Log.d(TAG, "startService: ");
        Intent network_intent = new Intent(main.getApplicationContext(), NetworkService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            main.startForegroundService(network_intent);
        } else {
            main.startService(network_intent);
        }
    }

    public void stopService() {
        Intent stop_network_intent = new Intent(main.getApplicationContext(), NetworkService.class);
        main.stopService(stop_network_intent);
    }

    /**
     * Acquire multicast lock - required for pre API 11 and some devices.
     */
    public static void setMulticastLock(Activity main) {
        WifiManager wifi = (WifiManager) main.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();
    }

    /**
     * Stop the leaders socket server. This will drop all currently connected clients.
     */
    public static void stopServer() {
        NetworkService.stopServer();

        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
    }

    /**
     * Get the name set for the current device
     */
    public static String getName() {
        return main.getNearbyManager().getName();
    }

    //CONNECTING TO A SERVER BELOW

    /**
     * Using a supplied NsdService connect to a server using the details provided.
     *
     * @param serviceInfo An NsdServiceInfo object containing the details about the selected leader.
     */
    public void connectToServer(NsdServiceInfo serviceInfo) {
        connectionThreadPool.submit(() -> {
            Log.d(TAG, "connectToServer: attempting to connect to " + serviceInfo.getHost() + ":" + serviceInfo.getPort());

            NSDManager.mService = serviceInfo;

            try {
                if (clientsServerSocket != null) {
                    clientsServerSocket.close();
                    clientsServerSocket = null;
                }

                clientsServerSocket = new Socket(serviceInfo.getHost(), serviceInfo.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (clientsServerSocket == null) {
                    tryReconnect();
                } else {
                    clientSetup();
                }
            }
        });
    }

    /**
     * If the initial connection attempt fails try again before sending the learner back to the
     * splash screen.
     */
    private void tryReconnect() {
        Log.d(TAG, "connectToServer: Socket disconnected attempting to reconnect");
        if (tries <= 10) {
            tries++;
            connectToServer(NSDManager.getChosenServiceInfo());
        } else {
            Log.d(TAG, "connectToServer: reconnection unsuccessful");
            main.runOnUiThread(() -> main.getNearbyManager().disconnectFromEndpoint(""));
        }
    }

    /**
     * If the socket is connected send the learners name back to the server to start the TCP client
     * and then begin monitoring the connection.
     */
    private void clientSetup() {
        if (clientsServerSocket.isConnected()) {
            Log.d(TAG, "connectToServer: connection successful");
            pingName = true; //Allows learner to send name again on reconnection
            connectionIsActive = 20;
            allowInput = true;
            startClientInputListener();
            main.getNearbyManager().nsdManager.stopDiscovery();

            main.runOnUiThread(() -> {
                main.findViewById(R.id.client_main).setVisibility(View.VISIBLE);
                List<String> inputList = Arrays.asList(NSDManager.getChosenServiceInfo().getServiceName().split("#"));
                main.setLeaderName(inputList.get(0));
            });

            startConnectionCheck();
        } else {
            connectToServer(NSDManager.getChosenServiceInfo());
        }
    }

    /**
     * Start an executor to continually check if the leader has disconnected or is still active. If
     * not then move the learner from a logged in state to the splash screen.
     */
    public void startConnectionCheck() {
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
                    sendToServer(getName(), "NAME");
                    pingName = false;
                }
            }
        }, 1, 8000, TimeUnit.MILLISECONDS);
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
                tryReconnect();
            });
        }
    }

    //CLIENT SOCKET CONNECTIONS
    //Kept for future testing purposes
    int testing = 10;

    /**
     * Starts a client socket listener that receives messages from the leader server. Handles
     * reconnection if a socket is closed prematurely.
     */
    public void startClientInputListener() {
        if(socketThreadPool != null) {
            socketThreadPool.shutdown();
        }

        socketThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        socketThreadPool.submit(() -> {
            while (true) {
                if (clientsServerSocket != null) {
                    if (!clientsServerSocket.isClosed() && !clientsServerSocket.isInputShutdown()) {
                        BufferedReader in;
                        String input = "";
                        try {
                            InputStreamReader inStream = new InputStreamReader(clientsServerSocket.getInputStream());
                            in = new BufferedReader(inStream);

                            try {
                                //Kept for future testing purposes
//                                    Only need the input = in.readLine() for production
//                                    Log.e(TAG, "TESTING COUNTDOWN: " + testing);
//                                    if (testing == 0) {
//                                        Log.e(TAG, "Throwing exception");
//                                        testing = 10;
//
//                                        throw new SocketException();
//                                    } else {
//                                        testing--;
//                                    }

                                input = in.readLine();
                            } catch (SocketException e) {
                                if (clientsServerSocket != null) {
                                    clientsServerSocket.close();
                                    clientsServerSocket = null;
                                }

                                e.printStackTrace();
                                Log.e(TAG, "FAILED! {1}");
                                break;
                            }

                            if (input != null) {
                                if (input.length() > 0) {
                                    Log.d(TAG, "allowInput is active");

                                    if (LeadMeMain.destroying) {
                                        if (clientsServerSocket != null) {
                                            clientsServerSocket.close();
                                            clientsServerSocket = null;
                                        }

                                        scheduledExecutor.shutdown();
                                        allowInput = false;
                                        return;
                                    } else {
                                        messageReceivedFromServer(input);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "FAILED! {2}");
                        }
                    } else {
                        clientsServerSocket = null;
                        Log.e(TAG, "FAILED! {3}");
                        break;
                    }
                }
            }

            cleanUpInput();

            if(!LeadMeMain.destroying) {
                connectToServer(NSDManager.getChosenServiceInfo());
            }
            Log.e(TAG, "FAILED! {4}");
        });
    }

    /**
     * Shutdown and destroy the thread pool and receive input future to ensure a fresh start
     * on reconnection.
     */
    public static void cleanUpInput() {
        try {
            if (clientsServerSocket != null) {
                clientsServerSocket.close();
                clientsServerSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        scheduledExecutor.shutdown();
        allowInput = false;

        socketThreadPool.shutdown();
        socketThreadPool = null;
//        receiveInput.cancel(true);
//        receiveInput = null;

        socketThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }

    /**
     * When teacher is found the info can be handed over to this to initialise the socket
     * - socket will return true to isConnected if it has ever had a successful connection
     */
    public static boolean isClientConnected() {
        if (clientsServerSocket != null) {
            return clientsServerSocket.isConnected();
        }
        return false;
    }

    /**
     * Get the instance of the client socket.
     *
     * @return A socket representing the client socket
     */
    public static Socket getClientSocket() {
        return clientsServerSocket;
    }

    /**
     * Set the client socket for connecting to the leader's server.
     *
     * @param socket A socket representing the chosen IP address and Port.
     */
    public static void setClientSocket(Socket socket) {
        clientsServerSocket = socket;
    }

    /**
     * Sends message from student to Teacher.
     */
    public void sendToServer(String message, String type) {
        if (NetworkManager.getClientSocket() != null) {
            if (NetworkManager.isClientConnected()) {
                NSDManager.executorService.submit(() -> {
                    PrintWriter out;
                    try {
                        out = new PrintWriter(NetworkManager.getClientSocket().getOutputStream(), true);
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
        if (currentClients.size() == 0) {
            main.waitingForLearners.setVisibility(View.VISIBLE);
        }
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
     *
     * @param input A string representing from the communication from the server.
     */
    private void receivedCommunication(String input) {
        Log.d(TAG, "messageReceivedFromServer: [COMM] " + input);
        if (input.contains("Thanks")) {
            main.closeDialogController(true);
            init = true;
            NetworkManager.pingName = false;
        }
    }

    /**
     * Decide what action should be taken depending on what is contained within the input string.
     *
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
        main.getNearbyManager().myID = input;
        NetworkManager.connectionIsActive = timeOut;
        Log.d(TAG, "messageReceivedFromServer: PING!!");
        NetworkManager.pingName = false;
        Log.d(TAG, "messageReceivedFromServer: received ping and subsequently ignoring it");

        if (!init) {
            main.closeDialogController(true);
            init = true;
        }
    }

    /**
     * Connect to the file server to receive the file from the leader.
     * @param input A String containing the PORT of the file server.
     */
    private void receivedFile(String input) {
        List<String> inputList2 = Arrays.asList(input.split(":"));
        if (main.fileTransferEnabled) {
            FileTransferManager.setFileType(inputList2.get(2));
            main.getFileTransferManager().receivingFile(NetworkManager.getClientSocket().getInetAddress(), Integer.parseInt(inputList2.get(1)));
        }
    }

    /**
     * If the device is not connected as a guide, disconnect from all endpoints.
     */
    public void receivedDisconnect() {
        Log.w(TAG, "Disconnect. Guide? " + LeadMeMain.isGuide);
        if (NetworkManager.getClientSocket() != null) {
            try {
                NetworkManager.getClientSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            NetworkManager.setClientSocket(null);
        }

        main.getNearbyManager().disconnectFromEndpoint("");
    }

    /**
     * called by the TCP clients to update the parent thread with information
     * types defined as such:
     * NAME: client server pings name through on first use it will update the parent with the clients name,
     * if its changed then subsequent run throughs will rectify that. Also acts as a pinging, once name
     * is received a response will be sent letting the student know they are still connected.
     *
     * @param message  A string containing information relevant to the switch case functions.
     * @param clientID An int representing which learner this update relates to.
     * @param type     A string to determine what action is being taken.
     */
    public static void updateParent(String message, int clientID, String type) {
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
     *
     * @param message  A string containing the name of the learner and the IP address of the device,
     *                 separated by a ':'.
     * @param clientID An integer representing the clients ID saved within the student class on a
     *                 leaders device.
     */
    private static void parentUpdateName(String message, int clientID) {
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
            Client temp = new Client();
            temp.name = message;
            temp.ID = clientID;
            temp.pingCycle = 1;
            currentClients.add(temp);
            Endpoint endpoint = new Endpoint();
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
     *
     * @param clientID An integer representing the clients ID saved within the student class on a
     *                 leaders device.
     */
    private static void parentUpdateDisconnect(int clientID) {
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
     *
     * @param clientID An integer representing the clients ID saved within the student class on a
     *                 leaders device.
     */
    private static void parentUpdateLost(int clientID) {
        if (NetworkService.clientSocketArray.get(clientID).getValue()) {
            Log.d(TAG, "updateParent: client: " + clientID + " is reconnecting");
            NetworkService.clientSocketArray.get(clientID).setValue(false);
        } else {
            Log.d(TAG, "updateParent: client: " + clientID + " has lost connection");
            Objects.requireNonNull(NetworkService.studentThreadArray.get(clientID)).t.interrupt();
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
     *
     * @param message A string representing the action a learner has taken.
     */
    private static void parentUpdateAction(String message) {
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
        main.runOnUiThread(() -> main.handlePayload(payload.asBytes()));
        p.recycle();
    }

    /**
     * Add messages to the message queue which is then checked by each student thread.
     */
    public static void sendToSelectedClients(String message, String type, ArrayList<Integer> selectedClientIDs) {
        Log.d(TAG, "sendToSelectedClients: " + selectedClientIDs + " " + currentClients.size());
        Msg msg = new Msg();
        msg.message = message;
        msg.type = type;

        for (int i = 0; i < currentClients.size(); i++) {
            for (int selected : selectedClientIDs) {
                if (selected == currentClients.get(i).ID) {
                    currentClients.get(i).messageQueue.add(msg);
                }
            }
        }

    }

    public void stopMonitoring(int ID) {
        ArrayList<Integer> selected = new ArrayList<>();
        selected.add(ID);
        sendToSelectedClients("STOP", "MONITOR", selected);
    }

    /**
     * File transfer case.
     *
     * @param ID        An int representing the client that the file is being sent to.
     * @param localPort An int representing the port to use for the transfer server.
     * @param fileType  A string representing the type of file that is being transferred.
     */
    public static void sendFile(int ID, int localPort, String fileType) {
        ArrayList<Integer> selected = new ArrayList<>();
        selected.add(ID);
        sendToSelectedClients("SEND:" + localPort + ":" + fileType, "FILE", selected);
    }
}
