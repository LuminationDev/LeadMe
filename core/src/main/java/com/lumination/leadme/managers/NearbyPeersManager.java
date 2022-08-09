package com.lumination.leadme.managers;

import static com.lumination.leadme.LeadMeMain.UIHandler;

import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.view.View;

import androidx.collection.ArraySet;

import com.google.android.gms.nearby.connection.Payload;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.services.NetworkService;
import com.lumination.leadme.models.Client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class NearbyPeersManager {
    private static final String TAG = "NearbyPeersManager";
    public static ConnectedPeer selectedLeader;
    public static String myID;
    public static String myName;

    private static NsdServiceInfo manInfo = null;

    /**
     * Constructor which initiates the nsdManager class.
     */
    public NearbyPeersManager() { }

    public void setSelectedLeader(ConnectedPeer peer) {
        selectedLeader = peer;
    }

    public void cancelConnection() {
        NetworkManager.receivedDisconnect();
    }

    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: deprecated");
    }

    public static void connectToSelectedLeader() {
        String Name = selectedLeader.getDisplayName();

        Log.e(TAG, "Teacher: " + Name);

        NetworkService.setLeaderIPAddress(manInfo.getHost());
        LeadMeMain.getInstance().findViewById(R.id.client_main).setVisibility(View.VISIBLE);
        LeadMeMain.getInstance().setLeaderName(Name);

        FirebaseManager.connectToLeader();

        UIHandler.postDelayed(() ->
                NetworkService.sendToServer(getName(), "NAME"),
            500);

        FirebaseManager.roomsReference.removeEventListener(FirebaseManager.roomsListener);
        return;
    }

    public static void connectToManualLeader(String leaderName, String IpAddress) {
        Log.d(TAG, "connectToManualLeader: " + IpAddress);
        LeadMeMain.getInstance().backgroundExecutor.submit(() -> {
            NsdServiceInfo info = new NsdServiceInfo();
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getByName(IpAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            info.setHost(inetAddress);
            info.setPort(54321);
            info.setServiceName(leaderName + "#Teacher#" + IpAddress);
            info.setServiceType("_http._tcp.");
            Log.d(TAG, "run: "+info);
            manInfo = info;
            selectedLeader = new ConnectedPeer(leaderName, IpAddress);
            LeadMeMain.runOnUI(NearbyPeersManager::connectToSelectedLeader);
        });
    }

    /**
     * Sets the user as the Guide, starts the network server and after a set delay starts
     * advertising for a connection.
     */
    public void setAsGuide() {
        Log.e(TAG, "Server starting for leader");
        LeadMeMain.isGuide = true;
    }

    /**
     * Query if a user is connected as a follower.
     * @return A boolean representing if they are a student.
     */
    public static boolean isConnectedAsFollower() {
        return !LeadMeMain.isGuide && NetworkService.isServerRunning();
    }

    /**
     * Query if a user is connected as a guide.
     * @return A boolean representing if they are a guide.
     */
    public static boolean isConnectedAsGuide() {
        return LeadMeMain.isGuide && NetworkService.isServerRunning();
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    public static String getName() {
        if (LeadMeMain.getInstance().getNameViewController() != null) {
            myName = LeadMeMain.getInstance().getNameViewController().getText().toString().trim();
        }
        return myName;
    }

    /**
     * Get the ID of the currently connected user.
     * @return A string that represents the ID of the current user.
     */
    public static String getID() {
        return FirebaseManager.getLocalIpAddress().replace(".", "_");
    }

    /**
     * Set the ID of a user that has just logged in.
     * @param id A string representing the new user.
     */
    public static void setID(String id) {
        if (id != null && id.length() > 0) {
            myID = id;
            Log.i(TAG, "My ID is now " + myID);
        } else {
            Log.e(TAG, "Something wrong with the new id! " + id);
        }
    }

    /**
     * Removes a selected student from the network adapter's server.
     * @param endpointId A string representing a students device.
     */
    public static void disconnectStudent(String endpointId) {
        Controller.getInstance().getNetworkManager().removeClient(endpointId);
    }

    /**
     * Removes a user from the network adapter's server.
     * @param endpointId A string representing a users device.
     */
    public static void disconnectFromEndpoint(String endpointId) {
        if (LeadMeMain.isGuide) {
            Controller.getInstance().getConnectedLearnersAdapter().alertStudentDisconnect(endpointId);
            Controller.getInstance().getConnectedLearnersAdapter().refresh();
            disconnectStudent(endpointId);
        } else {
            LeadMeMain.runOnUI(() -> {
                Controller.getInstance().getScreenSharingManager().clientToServerSocket = null;
                Controller.getInstance().getScreenSharingManager().stopService(); //stop the screen sharing service
                ArrayList<ConnectedPeer> temp = new ArrayList<>();
                Controller.getInstance().getLeaderSelectAdapter().setLeaderList(temp);
                LeadMeMain.getInstance().showLeaderWaitMsg(true);
                LeadMeMain.getInstance().setUIDisconnected();
            });
        }
    }

    /**
     * Get any selected peers, if no one is selected then get all peers.
     * @return An ArraySet containing all the selected peer IDs or all peers.
     */
    public static Set<String> getSelectedPeerIDsOrAll() {
        Set<String> endpoints = new ArraySet<>();
        //if connected as guide, send message to specific peers
        if (isConnectedAsGuide()) {
            for (ConnectedPeer thisPeer : ConnectedLearnersAdapter.mData) {
                if (thisPeer.isSelected()) {
                    Log.d(TAG, "Adding " + thisPeer.getDisplayName());
                    endpoints.add(thisPeer.getID());
                }
            }
            if (endpoints.isEmpty()) {
                return getAllPeerIDs();
            }
            return endpoints;

            //if connected as follower, send message back to guide
        }
        return endpoints;
    }

    /**
     * Get all peers that are currently selected.
     * @return An ArraySet contain all the selected peer IDs.
     */
    public static Set<String> getSelectedPeerIDs() {
        Set<String> endpoints = new ArraySet<>();
        //if connected as guide, send message to specific peers
        for (ConnectedPeer thisPeer : ConnectedLearnersAdapter.mData) {
            if (thisPeer.isSelected()) {
                Log.d(TAG, "Adding " + thisPeer.getDisplayName());
                endpoints.add(thisPeer.getID());
            }
        }
        return endpoints;
    }

    /**
     * Get all connected peers regardless of if they are selected or not.
     * @return A hashSet containing all the peer's IDs excluding the Guide's.
     */
    public static Set<String> getAllPeerIDs() {
        Set<String> peerIDS = new HashSet<>();
        for (Client id : NetworkManager.currentClients) {
            peerIDS.add(String.valueOf(id.ID));
        }
        if (!LeadMeMain.isGuide) {
            peerIDS.add("-1");
        }
        return peerIDS;
    }

    public static void sendToSelected(Payload payload, Set<String> endpoints) {
        Parcel p = Parcel.obtain();
        p.unmarshall(payload.asBytes(), 0, Objects.requireNonNull(payload.asBytes()).length);
        p.setDataPosition(0);
        byte[] b = p.marshall();
        p.recycle(); //recycle the parcel
        ArrayList<String> selectedString = new ArrayList<>(endpoints);
        if (selectedString.size() == 0 && !LeadMeMain.isGuide) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                NetworkService.sendToServer(encoded, "ACTION");
            }
            return;
        }
        if (!selectedString.isEmpty() && selectedString.get(0).equals("-1") && !LeadMeMain.isGuide) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                NetworkService.sendToServer(encoded, "ACTION");
            }
        } else {
            ArrayList<String> selected = new ArrayList<>();
            for (String peer : selectedString) {
                Log.d(TAG, "sendToSelected: " + peer);
                selected.add(peer);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                NetworkManager.sendToSelectedClients(encoded, "ACTION", selected);
            }
        }

    }
}
