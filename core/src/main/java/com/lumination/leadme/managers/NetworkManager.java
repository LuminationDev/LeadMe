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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.services.FileTransferService;
import com.lumination.leadme.services.NetworkService;
import com.lumination.leadme.models.Client;
import com.lumination.leadme.models.Endpoint;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private static WifiManager.MulticastLock multicastLock; // Acquire multicast lock
    private static boolean init = false; //check if connection has been initialised

    public static ArrayList<Client> currentClients = new ArrayList<>();

    public static ExecutorService executorService = Executors.newCachedThreadPool();

    public NetworkManager() { }

    /**
     * Start the server socket on a device.
     */
    public void startService() {
        Log.d(TAG, "startService: ");
        Intent network_intent = new Intent(LeadMeMain.getInstance().getApplicationContext(), NetworkService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LeadMeMain.getInstance().startForegroundService(network_intent);
        } else {
            LeadMeMain.getInstance().startService(network_intent);
        }
    }

    public static void stopService() {
        Intent stop_network_intent = new Intent(LeadMeMain.getInstance().getApplicationContext(), NetworkService.class);
        LeadMeMain.getInstance().stopService(stop_network_intent);
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
     * Disconnect a student from the leaders side. Sends an action to the client to know they
     * have been disconnected.
     * @param id An int representing the ID of the learner who is being disconnected.
     */
    public void removeClient(String id) {
        ArrayList<String> selected = new ArrayList<>();
        selected.add(id);
        sendToSelectedClients("disconnect", "DISCONNECT", selected);
        Log.d(TAG, "removeClient: client successfully removed");
        if (currentClients.size() == 0) {
            LeadMeMain.getInstance().waitingForLearners.setVisibility(View.VISIBLE);
        }
    }

    /*
    splits message over the comma into type and message use as per below:
    COMMUNICATION: simply prints the message on the client is used for nothing else
    ACTION: used for controlling the client with different actions, app launches, etc
    PING: used to let the client know it is still receiving data from the server and helps keep the connection alive
     */
    public static void messageReceivedFromServer(String input) {
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
    private static void receivedCommunication(String input) {
        Log.d(TAG, "messageReceivedFromServer: [COMM] " + input);
        if (input.contains("Thanks")) {
            LeadMeMain.getInstance().closeDialogController(true);
            init = true;
        }
    }

    /**
     * Decide what action should be taken depending on what is contained within the input string.
     *
     * @param input A string containing the action to be handled by the learner device.
     */
    private static void receivedAction(String input) {
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

        LeadMeMain.UIHandler.postAtFrontOfQueue(() -> {
            Log.d(TAG, timestamp + "]] messageReceivedFromServer: [ACTION] INSIDE MAIN THREAD");
            LeadMeMain.getInstance().handlePayload(payload.asBytes());
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
    private static void receivedPing(String input) {
        NearbyPeersManager.myID = input;
        Log.d(TAG, "messageReceivedFromServer: received ping and subsequently ignoring it");

        if (!init) {
            LeadMeMain.getInstance().closeDialogController(true);
            init = true;
        }
    }

    /**
     * Connect to the file server to receive the file from the leader.
     * @param input A String containing the PORT of the file server.
     */
    private static void receivedFile(String input) {
        List<String> inputList2 = Arrays.asList(input.split(":"));
        if (LeadMeMain.fileTransferEnabled) {
            FileTransferManager.setFileType(inputList2.get(2));
            Controller.getInstance().getFileTransferManager().receivingFile(NetworkService.getLeaderIPAddress(), Integer.parseInt(inputList2.get(1)));
        }
    }

    /**
     * If the device is not connected as a guide, disconnect from all endpoints.
     */
    public static void receivedDisconnect() {
        Log.w(TAG, "Disconnect. Guide? " + LeadMeMain.isGuide);
        NearbyPeersManager.disconnectFromEndpoint("");
        stopService();
    }

    /**
     * Called by the TCP clients to update the parent thread with information
     * types defined as such:
     * NAME: client server pings name through on first use it will update the parent with the clients name,
     * if its changed then subsequent runs will rectify that. Also acts as a pinging, once name
     * is received a response will be sent letting the student know they are still connected.
     *
     * @param message  A string containing information relevant to the switch case functions.
     * @param clientID An int representing which learner this update relates to.
     * @param type     A string to determine what action is being taken.
     */
    public static void updateParent(String message, String clientID, String type) {
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
    public static void parentUpdateName(String message, String clientID) {
        boolean exists = false;
        ArrayList<String> selected = new ArrayList<>();

        for (int i = 0; i < currentClients.size(); i++) {
            if (currentClients.get(i).ID.equals(clientID)) {
                if (!currentClients.get(i).name.equals(message)) {
                    Log.d(TAG, "updateParent: " + currentClients.get(i).name + " has changed to " + message);
                    currentClients.get(i).name = message;
                    String[] spilt = message.split(":");
                    ConnectedLearnersAdapter.getMatchingPeer(String.valueOf(clientID)).setName(spilt[0]);
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
            endpoint.name = message + ":" + clientID;
            endpoint.Id = String.valueOf(clientID);
            ConnectedPeer thisPeer = new ConnectedPeer(endpoint);
            LeadMeMain.runOnUI(() -> {
                Controller.getInstance().getConnectedLearnersAdapter().addStudent(thisPeer);
                LeadMeMain.getInstance().showConnectedStudents(true);
            });

            ValueEventListener postListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Get Post object and use the values to update the UI
                    Object messageObj = dataSnapshot.getValue();
                    Log.e("firebase", dataSnapshot.toString());
                    if (messageObj != null) {
                        String message = messageObj.toString();
                        Log.e("firebase", message);
                        Log.e("firebase", dataSnapshot.getKey());
                        if (message != null && message.length() > 0) {
                            NetworkService.determineAction(clientID, message);
                        }
                    }

                    // ..
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting Post failed, log a message
                    Log.e(TAG, "loadPost:onCancelled", databaseError.toException());
                }
            };
            LeadMeMain.roomReference.child("learners").child(clientID).child("currentMessage").addValueEventListener(postListener);
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
    private static void parentUpdateDisconnect(String clientID) {
        for (int i = 0; i < currentClients.size(); i++) {
            if (currentClients.get(i).ID.equals(clientID)) {
                Log.d(TAG, "updateParent: student has been disconnected: " + clientID);
                cleanUpTransfer(clientID);
                NetworkService.removeStudent(clientID);
                currentClients.remove(i);
                if (currentClients.size() == 0) {
                    LeadMeMain.runOnUI(() -> LeadMeMain.getInstance().waitingForLearners.setVisibility(View.VISIBLE));
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
    private static void parentUpdateLost(String clientID) {
        Log.d(TAG, "updateParent: client: " + clientID + " has lost connection");
        cleanUpTransfer(clientID);
        currentClients.remove(clientID);
        Controller.getInstance().getXrayManager().removePeerFromMap(String.valueOf(clientID));
        LeadMeMain.runOnUI(() -> {
            if (ConnectedLearnersAdapter.getMatchingPeer(String.valueOf(clientID)) != null) {
                if (ConnectedLearnersAdapter.getMatchingPeer(String.valueOf(clientID)).getStatus() != ConnectedPeer.STATUS_ERROR) {
                    LeadMeMain.getInstance().updatePeerStatus(String.valueOf(clientID), ConnectedPeer.STATUS_ERROR, null);
                }
            }
        });
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
        LeadMeMain.runOnUI(() -> LeadMeMain.getInstance().handlePayload(payload.asBytes()));
        p.recycle();
    }

    /**
     * Add messages to the message queue which is then checked by each student thread.
     */
    public static void sendToSelectedClients(String message, String type, ArrayList<String> selectedClientIDs) {
        Log.d(TAG, "sendToSelectedClients: " + selectedClientIDs + " " + currentClients.size());

        if(currentClients.size() == 0) {
            return;
        }
        if (currentClients.size() == selectedClientIDs.size()) {
            NetworkService.sendToAllClients(message, type);
            return;
        }

        for (int i = 0; i < currentClients.size(); i++) {
            for (String selected : selectedClientIDs) {
                if (selected.equals(currentClients.get(i).ID)) {
                    NetworkService.sendToClient(selected, message, type);

                    if(type.equals("DISCONNECT")) {
                        currentClients.remove(i);
                    }
                }
            }
        }
    }

    /**
     * Remove any devices that may have disconnected while file transfer was active.
     * @param ID An ID representing a learner.
     */
    private static void cleanUpTransfer(String ID) {
        if(FileTransferManager.selected != null && FileTransferManager.transfers != null) {
            FileTransferManager.selected.remove(ID);
            FileTransferManager.transfers.remove(ID);
            FileTransferService.removeRequest(ID);
        }
    }

    /**
     * Stop the screenSharingService from sending images to the guide.
     * @param ID An int representing the learner that needs to stop sending images.
     */
    public void stopMonitoring(String ID) {
        ArrayList<String> selected = new ArrayList<>();
        selected.add(ID);
        sendToSelectedClients("STOP", "MONITOR", selected);
    }

    /**
     * File transfer case.
     * @param ID        An int representing the client that the file is being sent to.
     * @param localPort An int representing the port to use for the transfer server.
     * @param fileType  A string representing the type of file that is being transferred.
     */
    public static void sendFile(String ID, int localPort, String fileType) {
        Log.e(TAG, "Sending file to: " + ID);
        ArrayList<String> selected = new ArrayList<>();
        selected.add(ID);
        sendToSelectedClients("SEND:" + localPort + ":" + fileType, "FILE", selected);
    }
}
