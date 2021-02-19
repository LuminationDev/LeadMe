package com.lumination.leadme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NearbyPeersManager {
    String TAG = "NearbyPeersManager";
    LeadMeMain main;
    public NetworkAdapter networkAdapter;
    ConnectedPeer selectedLeader;

    String myID;
    boolean discovering;

    public String myName;



    public NearbyPeersManager(LeadMeMain main) {
       this.main=main;
       networkAdapter=new NetworkAdapter(main.context,main,this);
    }

    protected void startPingThread() {
        Log.d(TAG, "startPingThread: ping is now handled by the DNS-SD protocols");
    }

    protected void discoverLeaders() {
        discovering=true;
        networkAdapter.stopAdvertising();
        networkAdapter.startDiscovery();
    }



    protected void setSelectedLeader(ConnectedPeer peer) {
        selectedLeader=peer;
    }

    protected void cancelConnection() {
        networkAdapter.stopServer();
    }

    public void onStop() {
        Log.d(TAG, "onStop: deprecated");
        networkAdapter.closeSocket=true;

    }

    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: deprecated");

    }

    protected void connectToSelectedLeader() {
        String Name = selectedLeader.getDisplayName();
        ArrayList<NsdServiceInfo> discoveredLeaders=networkAdapter.discoveredLeaders ;
        Iterator iterator = discoveredLeaders.iterator();
        while (iterator.hasNext()){
            NsdServiceInfo info = (NsdServiceInfo) iterator.next();
            Log.d(TAG, "connectToSelectedLeader: "+info.getServiceName());
            if(info.getServiceName().equals(Name+"#Teacher")){
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
        main.isGuide=true;
        networkAdapter.stopDiscovery();
        networkAdapter.startAdvertising();
    }


    public boolean isConnectedAsFollower() {
        return !main.isGuide && networkAdapter.isConnected();
    }

    public boolean isConnectedAsGuide() {
       return main.isGuide && networkAdapter.clientThreadList.size()>0;//TODO maybe replace this with current clients once implemented
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
        if(main.isGuide){
            disconnectStudent(endpointId);
        }else {
            try {
                networkAdapter.socket.close();
                main.setUIDisconnected();
            } catch (IOException e) {
                Log.d(TAG, "disconnectFromEndpoint: unable to close socket");
                e.printStackTrace();
            }
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
        if(main.isGuide){
            networkAdapter.stopServer();
        }else{
//            try {
//                networkAdapter.socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }


    protected final boolean isConnecting() {
        if(networkAdapter.socket!=null) {
            return networkAdapter.socket.isConnected();
        }else{
            return false;
        }
    }

    public Set<String> getSelectedPeerIDs() {

        Set<String> something = new Set<String>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(@Nullable Object o) {
                return false;
            }

            @NonNull
            @Override
            public Iterator<String> iterator() {
                return null;
            }

            @NonNull
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @NonNull
            @Override
            public <T> T[] toArray(@NonNull T[] a) {
                return null;
            }

            @Override
            public boolean add(String s) {
                return false;
            }

            @Override
            public boolean remove(@Nullable Object o) {
                return false;
            }

            @Override
            public boolean containsAll(@NonNull Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(@NonNull Collection<? extends String> c) {
                return false;
            }

            @Override
            public boolean retainAll(@NonNull Collection<?> c) {
                return false;
            }

            @Override
            public boolean removeAll(@NonNull Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }
        };
        return something;
    }

    public Set<String> getAllPeerIDs() {
        Set<String> something = new HashSet(networkAdapter.currentClients);
        return something;
    }

    void sendToSelected(Payload payload, Set<String> endpoints) {
        Parcel p = Parcel.obtain();
        p.unmarshall(payload.asBytes(), 0, payload.asBytes().length);
        p.setDataPosition(0);
        byte[] b = p.marshall();
        String test = null;
        ArrayList<String> selectedString = new ArrayList<>(endpoints);
        ArrayList<Integer> selected = new ArrayList<>();
        Iterator iterator = selectedString.iterator();
        while(iterator.hasNext()){
            selected.add(Integer.parseInt((String) iterator.next()));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String encoded = Base64.getEncoder().encodeToString(b);
            networkAdapter.sendToSelectedClients(encoded,"ACTION",selected);
        }

    }

//    public void sendScreenShot(Bitmap bitmap) {
//        if(networkAdapter!=null){
//            //networkAdapter.sendToServer(encodeToBase64(bitmap,Bitmap.CompressFormat.JPEG,70),"IMAGE");
//            Log.d(TAG, "sendScreenShot: "+encodeToBase64(bitmap,Bitmap.CompressFormat.JPEG,70).length());
//        }else{
//            Log.d(TAG, "sendScreenShot: networkAdapter is null");
//        }
//    }
    public String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality) {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return android.util.Base64.encodeToString(byteArrayOS.toByteArray(), android.util.Base64.DEFAULT);
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
