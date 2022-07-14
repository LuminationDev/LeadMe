package com.lumination.leadme.managers;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.view.View;

import androidx.collection.ArraySet;

import com.google.android.gms.nearby.connection.Payload;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.services.NetworkService;
import com.lumination.leadme.models.Client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NearbyPeersManager {
    private static final String TAG = "NearbyPeersManager";
    public static ConnectedPeer selectedLeader;
    public static String myID;
    public static String myName;

    private static NsdServiceInfo manInfo = null;
    private boolean discovering;
    private static int tryConnect = 0;

    /**
     * Constructor which initiates the nsdManager class.
     */
    public NearbyPeersManager() {
        //In case the server was not closed down
        NetworkManager.stopServer();
    }

    /**
    * Resets the connection information for a manual connection, used when swapping from server discovery
    * back to normal connection mode.
     */
    public void resetManualInfo() {
        Log.d(TAG, "Resetting manual connection details");
        manInfo = null;
        if(!LeadMeMain.isGuide) { //do not want the guide to start searching for services
            discoverLeaders();
        }
    }

    public void discoverLeaders() {
        discovering = true;
    }

    public void setSelectedLeader(ConnectedPeer peer) {
        selectedLeader = peer;
    }

    public void cancelConnection() {
        NetworkManager.receivedDisconnect();
    }

    public void onStop() {
        Log.d(TAG, "onStop: deprecated");
        disconnectFromAllEndpoints();
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
        DatabaseReference database = FirebaseDatabase.getInstance("https://leafy-rope-301003-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        LeadMeMain.roomReference = database.child(LeadMeMain.publicIpAddress.replace(".", "_")).child("rooms").child(NetworkService.getLeaderIPAddress().getHostAddress().replace(".", "_"));
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                NetworkService.receiveMessage(dataSnapshot.getValue().toString());

                Log.e("firebase", dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.e(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };

        LeadMeMain.roomReference.child("learners").child(LeadMeMain.localIpAddress.replace(".", "_")).child("currentMessage").setValue("");
        LeadMeMain.learnerReference = LeadMeMain.roomReference.child("learners").child(LeadMeMain.localIpAddress.replace(".", "_"));
        LeadMeMain.learnerReference.child("leaderMessage").setValue("");


        LeadMeMain.roomReference.child("currentMessage").addValueEventListener(postListener);
        LeadMeMain.learnerReference.child("leaderMessage").addValueEventListener(postListener);

        NetworkService.sendToServer(getName(), "NAME");
        return;
//        Log.d(TAG, "connectToSelectedLeader: no leader found with the name " + Name + ". Trying" +
//                "again.");
//
//        //In case the device is trying to connect manually when not in manual mode
//        if((!LeadMeMain.sessionManual || !LeadMeMain.directConnection) && manInfo != null) {
//            manInfo = null;
//        }
//
//        //Try connection again after a set time
//        if(tryConnect < 10) {
//            tryConnect++;
//            connectToSelectedLeader();
//        } else {
//            tryConnect = 0;
//            Log.d(TAG, "connectToSelectedLeader: unable to find leader.");
//        }
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
        return LeadMeMain.isGuide;
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
        return myID;
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

    public boolean isDiscovering() {
        return discovering;
    }

    protected void disconnectFromAllEndpoints() {
        if (LeadMeMain.isGuide) {
            NetworkManager.stopServer();
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
