package com.lumination.leadme;

import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;

import androidx.collection.ArraySet;

import com.google.android.gms.nearby.connection.Payload;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NearbyPeersManager {
    String TAG = "NearbyPeersManager";
    public LeadMeMain main;
    public NetworkAdapter networkAdapter;
    ConnectedPeer selectedLeader;

    String myID;
    boolean discovering;

    public String myName;


    public NearbyPeersManager(LeadMeMain main) {
        this.main = main;
        networkAdapter = new NetworkAdapter(main.context, main, this);
    }

    protected void startPingThread() {
        Log.d(TAG, "startPingThread: ping is now handled by the DNS-SD protocols");
    }

    protected void discoverLeaders() {
        discovering = true;
        networkAdapter.stopAdvertising();
        networkAdapter.startDiscovery();
    }


    protected void setSelectedLeader(ConnectedPeer peer) {
        selectedLeader = peer;
    }

    protected void cancelConnection() {
        networkAdapter.stopServer();
    }

    public void onStop() {
        Log.d(TAG, "onStop: deprecated");
        networkAdapter.closeSocket = true;

    }

    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: deprecated");

    }

    protected void connectToSelectedLeader() {
        String Name = selectedLeader.getDisplayName();
        ArrayList<NsdServiceInfo> discoveredLeaders = networkAdapter.discoveredLeaders;
        Iterator iterator = discoveredLeaders.iterator();
        while (iterator.hasNext()) {
            NsdServiceInfo info = (NsdServiceInfo) iterator.next();
            Log.d(TAG, "connectToSelectedLeader: " + info.getServiceName());
            if (info.getServiceName().equals(Name + "#Teacher")) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        networkAdapter.connectToServer(info);
                        return;
                    }
                });
                t.start();

            }
        }
        Log.d(TAG, "connectToSelectedLeader: no leader found with the name " + Name);
    }


    public void setAsGuide() {
        main.isGuide = true;
        networkAdapter.stopDiscovery();
        networkAdapter.startAdvertising();
    }


    public boolean isConnectedAsFollower() {
        return !main.isGuide && networkAdapter.isConnected();
    }

    public boolean isConnectedAsGuide() {
        return main.isGuide && networkAdapter.currentClients.size() > 0;//TODO maybe replace this with current clients once implemented
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    public String getName() {
        if (main.getNameView() != null) {
            myName = main.getNameView().getText().toString().trim();
        }
        return myName;
    }

    public String getID() {
        return myID;
    }

    public void setID(String id) {
        if (id != null && id.length() > 0) {
            myID = id;
            Log.i(TAG, "My ID is now " + myID);
        } else {
            Log.e(TAG, "Something wrong with the new id! " + id);
        }
    }


    public void disconnectStudent(String endpointId) {
        networkAdapter.removeClient(Integer.parseInt(endpointId));
    }

    public void disconnectFromEndpoint(String endpointId) {
        if (main.isGuide) {
            main.getConnectedLearnersAdapter().alertStudentDisconnect(endpointId);
            main.getConnectedLearnersAdapter().refresh();
            disconnectStudent(endpointId);

        } else {
//            try {
            main.runOnUiThread(() -> {
                ArrayList<ConnectedPeer> temp = new ArrayList<>();
                main.getLeaderSelectAdapter().setLeaderList(temp);
                main.showLeaderWaitMsg(true);
                networkAdapter.startDiscovery();
                main.setUIDisconnected();
            });
            //networkAdapter.socket.close();
//            } catch (IOException e) {
//                Log.d(TAG, "disconnectFromEndpoint: unable to close socket");
//                e.printStackTrace();
//            }
        }
    }

    /**
     * Stops advertising.
     */
    protected void stopAdvertising() {
        networkAdapter.stopAdvertising();
    }

    protected boolean isDiscovering() {
        return discovering;

    }

    protected void disconnectFromAllEndpoints() {
        if (main.isGuide) {
            networkAdapter.stopServer();
        } else {
//            try {
//                networkAdapter.socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }


    protected final boolean isConnecting() {
        if (networkAdapter.socket != null) {
            return networkAdapter.socket.isConnected();
        } else {
            return false;
        }
    }

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

    public Set<String> getAllPeerIDs() {
        Set<String> something = new HashSet();
        Iterator it = networkAdapter.currentClients.iterator();
        while (it.hasNext()) {
            client id = (client) it.next();
            something.add(String.valueOf(id.ID));
        }
        if (!main.isGuide) {
            something.add("-1");
        }
        return something;
    }

    void sendToSelected(Payload payload, Set<String> endpoints) {
        Parcel p = Parcel.obtain();
        p.unmarshall(payload.asBytes(), 0, payload.asBytes().length);
        p.setDataPosition(0);
        byte[] b = p.marshall();
        String test = null;
        ArrayList<String> selectedString = new ArrayList<>(endpoints);
        if (selectedString.size() == 0 && !main.isGuide) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                networkAdapter.sendToServer(encoded, "ACTION");
            }
            return;
        }
        if (!selectedString.isEmpty() && selectedString.get(0).equals("-1") && !main.isGuide) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                networkAdapter.sendToServer(encoded, "ACTION");
            }
        } else {
            ArrayList<Integer> selected = new ArrayList<>();
            Iterator iterator = selectedString.iterator();
            while (iterator.hasNext()) {
                String peer = (String) iterator.next();
                Log.d(TAG, "sendToSelected: " + peer);
                selected.add(Integer.parseInt(peer));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String encoded = Base64.getEncoder().encodeToString(b);
                networkAdapter.sendToSelectedClients(encoded, "ACTION", selected);
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
