package com.lumination.leadme;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;

import androidx.collection.ArraySet;

import com.google.android.gms.nearby.connection.Payload;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NearbyPeersManager {
    private static final String TAG = "NearbyPeersManager";
    public LeadMeMain main;
    public NSDManager nsdManager;
    public ConnectedPeer selectedLeader;
    public String myID;
    public String myName;

    private NsdServiceInfo manInfo = null;
    private boolean discovering;
    private int tryConnect = 0;

    /**
     * Constructor which initiates the nsdManager class.
     * @param main A reference to the LeadMeMain class.
     */
    public NearbyPeersManager(LeadMeMain main) {
        this.main = main;
        nsdManager = new NSDManager(main);
        //In case the server was not closed down
        NetworkManager.stopServer();
    }

    protected void startPingThread() {
        Log.d(TAG, "startPingThread: ping is now handled by the DNS-SD protocols");
    }

    /**
    * Resets the connection information for a manual connection, used when swapping from server discovery
    * back to normal connection mode.
     */
    protected void resetManualInfo() {
        Log.d(TAG, "Resetting manual connection details");
        manInfo = null;
        if(!LeadMeMain.isGuide) { //do not want the guide to start searching for services
            discoverLeaders();
        }
    }

    protected void discoverLeaders() {
        discovering = true;
        nsdManager.stopAdvertising();
        nsdManager.startDiscovery();
    }

    protected void setSelectedLeader(ConnectedPeer peer) {
        selectedLeader = peer;
    }

    protected void cancelConnection() {
        main.getNetworkManager().receivedDisconnect();
    }

    public void onStop() {
        Log.d(TAG, "onStop: deprecated");
        stopAdvertising();
        disconnectFromAllEndpoints();
    }

    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: deprecated");
    }

    protected void connectToSelectedLeader() {
        String Name = selectedLeader.getDisplayName();

        Log.e(TAG, "Teacher: " + Name);

        if(manInfo == null) {
            ArrayList<NsdServiceInfo> discoveredLeaders = nsdManager.discoveredLeaders;
            Log.d(TAG, "Leaders array: " + discoveredLeaders.size());
            for (NsdServiceInfo info : discoveredLeaders) {
                Log.d(TAG, "connectToSelectedLeader: " + info.getServiceName());
                if (info.getServiceName().equals(Name + "#Teacher")) {
                    main.manageServerConnection(info);
                    return;
                }
            }
        } else {
            main.manageServerConnection(manInfo);
            return;
        }
        Log.d(TAG, "connectToSelectedLeader: no leader found with the name " + Name + ". Trying" +
                "again.");

        //In case the device is trying to connect manually when not in manual mode
        if((!main.sessionManual || !main.directConnection) && manInfo != null) {
            manInfo = null;
        }

        //Try connection again after a set time
        if(tryConnect < 10) {
            tryConnect++;
            connectToSelectedLeader();
        } else {
            tryConnect = 0;
            Log.d(TAG, "connectToSelectedLeader: unable to find leader.");
        }
    }

    //TODO IpAddress sometimes had a '/' preceding it
    protected void connectToManualLeader(String leaderName, String IpAddress) {
        Log.d(TAG, "connectToManualLeader: " + IpAddress);
        main.backgroundExecutor.submit(() -> {
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
            nsdManager.discoveredLeaders.add(info);
            selectedLeader = new ConnectedPeer(leaderName, IpAddress);
            main.runOnUiThread(this::connectToSelectedLeader);
        });
    }

    /**
     * Sets the user as the Guide, starts the network server and after a set delay starts
     * advertising for a connection.
     */
    public void setAsGuide() {
        Log.e(TAG, "Server starting for leader");
        LeadMeMain.isGuide = true;
        main.startServer();

        //Wait a little bit for the server to start before making the guide discoverable.
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        final Runnable runnable = () -> {
            nsdManager.stopDiscovery();
            nsdManager.startAdvertising();

            scheduler.shutdown();
        };
        scheduler.scheduleAtFixedRate(runnable, 2, 1, SECONDS);
    }

    /**
     * Query if a user is connected as a follower.
     * @return A boolean representing if they are a student.
     */
    public boolean isConnectedAsFollower() {
        return !LeadMeMain.isGuide && NetworkManager.isClientConnected();
    }

    /**
     * Query if a user is connected as a guide.
     * @return A boolean representing if they are a guide.
     */
    public boolean isConnectedAsGuide() {
        return LeadMeMain.isGuide && NetworkService.isServerRunning();
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    public String getName() {
        if (main.getNameViewController() != null) {
            myName = main.getNameViewController().getText().toString().trim();
        }
        return myName;
    }

    /**
     * Get the ID of the currently connected user.
     * @return A string that represents the ID of the current user.
     */
    public String getID() {
        return myID;
    }

    /**
     * Set the ID of a user that has just logged in.
     * @param id A string representing the new user.
     */
    public void setID(String id) {
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
    public void disconnectStudent(String endpointId) {
        main.getNetworkManager().removeClient(Integer.parseInt(endpointId));
    }

    /**
     * Removes a user from the network adapter's server.
     * @param endpointId A string representing a users device.
     */
    public void disconnectFromEndpoint(String endpointId) {
        if (LeadMeMain.isGuide) {
            main.getConnectedLearnersAdapter().alertStudentDisconnect(endpointId);
            main.getConnectedLearnersAdapter().refresh();
            disconnectStudent(endpointId);
        } else {
            main.runOnUiThread(() -> {
                main.screenCap.clientToServerSocket = null;
                main.screenCap.stopService(); //stop the screen sharing service
                ArrayList<ConnectedPeer> temp = new ArrayList<>();
                main.getLeaderSelectAdapter().setLeaderList(temp);
                main.showLeaderWaitMsg(true);
                nsdManager.startDiscovery();
                main.setUIDisconnected();
            });
        }
    }

    /**
     * Stops advertising.
     */
    protected void stopAdvertising() {
        nsdManager.stopAdvertising();
    }

    protected boolean isDiscovering() {
        return discovering;
    }

    protected void disconnectFromAllEndpoints() {
        if (LeadMeMain.isGuide) {
            NetworkManager.stopServer();
        }
    }

    protected final boolean isConnecting() {
        if (NetworkManager.getClientSocket() != null) {
            return NetworkManager.isClientConnected();
        } else {
            return false;
        }
    }

    /**
     * Get any selected peers, if no one is selected then get all peers.
     * @return An ArraySet containing all the selected peer IDs or all peers.
     */
    public Set<String> getSelectedPeerIDsOrAll() {
        Set<String> endpoints = new ArraySet<>();
        //if connected as guide, send message to specific peers
        if (isConnectedAsGuide()) {
            for (ConnectedPeer thisPeer : main.getConnectedLearnersAdapter().mData) {
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
    public Set<String> getSelectedPeerIDs() {
        Set<String> endpoints = new ArraySet<>();
        //if connected as guide, send message to specific peers
        if (isConnectedAsGuide()) {
            for (ConnectedPeer thisPeer : main.getConnectedLearnersAdapter().mData) {
                if (thisPeer.isSelected()) {
                    Log.d(TAG, "Adding " + thisPeer.getDisplayName());
                    endpoints.add(thisPeer.getID());
                }
            }
            return endpoints;
        //if connected as follower, send message back to guide
        } else {
            endpoints.add("-1");
        }
        return endpoints;
    }

    /**
     * Get all connected peers regardless of if they are selected or not.
     * @return A hashSet containing all the peer's IDs excluding the Guide's.
     */
    public Set<String> getAllPeerIDs() {
        Set<String> peerIDS = new HashSet<>();
        for (client id : NetworkManager.currentClients) {
            peerIDS.add(String.valueOf(id.ID));
        }
        if (!LeadMeMain.isGuide) {
            peerIDS.add("-1");
        }
        return peerIDS;
    }

    void sendToSelected(Payload payload, Set<String> endpoints) {
        Parcel p = Parcel.obtain();
        p.unmarshall(payload.asBytes(), 0, Objects.requireNonNull(payload.asBytes()).length);
        p.setDataPosition(0);
        byte[] b = p.marshall();
        p.recycle(); //recycle the parcel
        ArrayList<String> selectedString = new ArrayList<>(endpoints);
        if (selectedString.size() == 0 && !LeadMeMain.isGuide) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                main.getNetworkManager().sendToServer(encoded, "ACTION");
            }
            return;
        }
        if (!selectedString.isEmpty() && selectedString.get(0).equals("-1") && !LeadMeMain.isGuide) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                main.getNetworkManager().sendToServer(encoded, "ACTION");
            }
        } else {
            ArrayList<Integer> selected = new ArrayList<>();
            for (String peer : selectedString) {
                Log.d(TAG, "sendToSelected: " + peer);
                selected.add(Integer.parseInt(peer));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                NetworkManager.sendToSelectedClients(encoded, "ACTION", selected);
            }
        }

    }


    /**
     * Represents a device we can talk to.
     */
    public static class Endpoint {
        String name;
        String Id;

        public String getName() {
            return name;
        }

        public String getId() {
            return Id;
        }
    }
}
