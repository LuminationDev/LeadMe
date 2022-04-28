package com.lumination.leadme.managers;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.services.NetworkService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for managing NSD connections and adding leaders to the display list.
 */
public class NSDManager {
    private static final String TAG = "NSDManager";

    private final LeadMeMain main; //not a good solution work in progress
    private final NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener = null;
    private NsdManager.RegistrationListener mRegistrationListener = null;

    public static NsdServiceInfo mService;
    public static final String SERVICE_TYPE = "_http._tcp.";
    public String mServiceName = "LeadMe";

    public ArrayList<NsdServiceInfo> discoveredLeaders = new ArrayList<>();

    public NSDManager(LeadMeMain main) {
        this.main = main;
        mNsdManager = (NsdManager) main.getSystemService(Context.NSD_SERVICE);
        NetworkManager.setMulticastLock(main); //should be set for learners and leaders?
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

                    } else if (service.getServiceName().equals(NetworkManager.getName())) {
                        Log.d(TAG, "Same machine: " + NetworkManager.getName());

                    } else if (service.getServiceName().contains("#Teacher")) {
                        Log.d(TAG, "onServiceFound: attempting to resolve " + service.getServiceName());

                        //fixes the resolve 3 error
                        try {
                            NetworkManager.executorService.submit(() -> mNsdManager.resolveService(service, new resListener()));
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
            if (serviceInfo.getServiceName().equals(NetworkManager.getName())) {
                Log.d(TAG, "Same IP.");
                return;
            }

            Log.d(TAG, "onServiceResolved: " + serviceInfo);

            NetworkManager.executorService.submit(() -> resolveSingleService(serviceInfo));
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
            if (hardCoded != null) {
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

        if (!main.sessionManual && !main.directConnection) {
            main.runOnUiThread(() -> {
                main.getLeaderSelectAdapter().addLeader(new ConnectedPeer(leader.get(0), serviceInfo.getHost().toString()));
                main.showLeaderWaitMsg(false);
            });
        }
    }

    /**
     * Initialises the registration listener allowing us to register the service
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

    /**
     * Discovers services that are advertised by leaders, is not continuous so will need to be
     * called in a runnable to implement a scan
     */
    public void startDiscovery() {
        Log.d(TAG, "startDiscovery: ");
        discoveredLeaders = new ArrayList<>();
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();

        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    /**
     * Stops any discovery processes that are in progress
     */
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

    /**
     * Create a NSD service and register it with the details of a logged in leader.
     */
    public void startAdvertising() {
        stopAdvertising();  // Cancel any previous registration request
        initializeRegistrationListener();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        String hardIpAddress = null;
        try {
            hardIpAddress = InetAddress.getByAddress(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(LeadMeMain.wifiManager.getConnectionInfo().getIpAddress())
                            .array()
            ).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        serviceInfo.setPort(NetworkService.leaderPORT); //Use the hard coded port
        serviceInfo.setServiceName(NetworkManager.getName() + "#Teacher#" + hardIpAddress);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setAttribute("IP", hardIpAddress);
        serviceInfo.setHost(NetworkService.getServerSocket().getInetAddress());

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
     * Getter for the serviceInfo, only useful for student to teacher connections.
     * @return An instance of the current service info.
     */
    public static NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }
}
