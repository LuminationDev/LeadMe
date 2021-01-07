package com.lumination.leadme;

import android.app.AlertDialog;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Our primary NearbyConnections class. This has 4 {@link State}s.
 *
 * <p>{@link State#UNKNOWN}: We cannot do anything while we're in this state. The app is likely in the background.
 *
 * <p>{@link State#DISCOVERING}: Our default state (after we've connected). We constantly listen for a device to advertise near us.
 *
 * <p>{@link State#ADVERTISING}: If a user shakes their device, they enter this state. We advertise our device so that others nearby can discover us.
 *
 * <p>{@link State#CONNECTED}: We've connected to another device. We'll continue to advertise (if we were already advertising) so that more people can connect to us.
 */
public class NearbyPeersManager {

    private static final String TAG = "NearbyManager";

    private AlertDialog disconnectPrompt;
    private View everyoneDisconnectedView;

    //This service id lets us find other nearby devices that are interested in the same thing.
    private static final String SERVICE_INSTANCE = "com.lumination.leadme.LumiLeadMe";

    // The connection strategy we'll use for Nearby Connections.
    // P2P_STAR = which is a combination of Bluetooth Classic and WiFi Hotspots.
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    //private static final Strategy STRATEGY = Strategy.P2P_STAR;

    // Our handler to Nearby Connections
    private final ConnectionsClient mConnectionsClient;

    //tracks the state of discovered endpoints
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    //tracks connection states
    private boolean mIsConnecting = false;
    private boolean mIsDiscovering = false;
    private boolean mIsAdvertising = false;

    // Advertise for 30 seconds before going back to discovering. If a client connects, we'll continue
    // to advertise indefinitely so others can still connect.
    private static final long ADVERTISING_DURATION = 30000;

    // The state of the app. As the app changes states, the UI will update and advertising/discovery will start/stop.
    private State mState = State.UNKNOWN;

    //A Handler that allows us to post back on to the UI thread. We use this to resume discovery after an uneventful bout of advertising.
    private final Handler mUiHandler;
    private final LeadMeMain main;

    private String myName;
    private String myId;

    /**
     * Starts discovery. Used in a postDelayed manor with {@link #mUiHandler}.
     * Allows the guide to accept learner connections even once leading has begun
     */
    private final Runnable mDiscoverRunnable = new Runnable() {
        @Override
        public void run() {
            if (main.isGuide && getState() == State.ADVERTISING) {
                Log.i(TAG, "In runnable - " + main.isGuide + " in " + getState());

            } else if (main.isReadyToConnect && getState() == State.UNKNOWN) {
                Log.i(TAG, "In runnable - " + main.isGuide + " in " + getState());
                setState(State.DISCOVERING);
            }
        }
    };

    public NearbyPeersManager(LeadMeMain main) {
        this.main = main;
        mUiHandler = main.getHandler();
        mConnectionsClient = Nearby.getConnectionsClient(main);
        Log.d(TAG, "NEW NEARBY PEERS MANAGER");
        disconnectFromAllEndpoints(); //clear any old connections
    }


    //TODO
    protected void discoverLeaders() {
        Log.d(TAG, "DISCOVER LEADERS: " + main.isReadyToConnect + ", " + mState + " --> " + mIsDiscovering + ", " + mIsAdvertising + ", " + mEstablishedConnections + ", " + mDiscoveredEndpoints);
        if (main.isReadyToConnect) {
            setState(State.DISCOVERING);
        }
    }

    private ConnectedPeer chosenLeader = null;

    protected void setSelectedLeader(ConnectedPeer peer) {
        chosenLeader = peer;
    }

    protected void cancelConnection() {
        stopDiscovering();
        stopAdvertising();

        Log.d(TAG, "CANCEL CONNECTION");
        disconnectFromAllEndpoints();
    }


    public void onStart() {
        //if it's already connected, leave it alone!
        if (main.isReadyToConnect && getState() != State.CONNECTED) {
            setState(State.DISCOVERING);
        }
    }

    public void onStop() {
        //if it's already connected, leave it alone!
        if (getState() != State.CONNECTED) {
            Log.d(TAG, "Uh oh! Stop! " + getState());
            setState(State.UNKNOWN);
            main.stopShakeDetection();
            mUiHandler.removeCallbacksAndMessages(null);
        }
    }

    public void onBackPressed() {
        if (main.isReadyToConnect && (getState() == State.CONNECTED || getState() == State.ADVERTISING)) {
            setState(State.DISCOVERING);
        }
    }

    protected void connectToSelectedLeader() {
        connectToEndpoint(chosenLeader.getMyEndpoint());
    }

    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        if (!isConnecting()) {
            Log.d(TAG, "Adding to leader adapter!");
            main.getLeaderSelectAdapter().addLeader(new ConnectedPeer(endpoint));
            main.showLeaderWaitMsg(false);
        }
    }


    protected void onEndpointConnected(final Endpoint endpoint) {
        Toast.makeText(main, "Connected to " + endpoint.getName(), Toast.LENGTH_SHORT).show();
        setState(State.CONNECTED, endpoint);
        main.closeWaitingDialog(true);

        mUiHandler.post(() -> {
            //NOTE: this must be done on main thread
            Log.d(TAG, "Am I the guide? " + main.isGuide);
            if (main.isGuide) {
                ConnectedPeer thisPeer = new ConnectedPeer(endpoint);
                main.getConnectedLearnersAdapter().addStudent(thisPeer);
                //send the ID back to the student
                main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.YOUR_ID_IS + thisPeer.getID() + ":" + thisPeer.getDisplayName(),
                        getAllPeerIDs());
            } else {
                main.findViewById(R.id.client_main).setVisibility(View.VISIBLE);
                main.setLeaderName(endpoint.getName());
            }
        });
    }

    protected void onEndpointDisconnected(final Endpoint endpoint) {
        Toast.makeText(main, "Disconnected from " + endpoint.getName(), Toast.LENGTH_SHORT).show();

        if (main.overlayView != null) {
            main.overlayView.setVisibility(View.INVISIBLE); //hide the overlay so we don't get stuck
        }

        performDisconnection(endpoint);

        // If we lost all our endpoints, then we should reset the state of our app and go back
        // to our initial state (discovering).
        if (main.isReadyToConnect && getConnectedEndpoints().isEmpty()) {
            main.recallToLeadMe();
            Log.i(TAG, "In onEndPointDisconnected - " + main.isGuide);

            //setState(State.DISCOVERING);
            if (!main.isGuide) {
                Runnable myRunnable = main::setUIDisconnected;
                main.getHandler().post(myRunnable); //needs to run on main thread
            }
        }
    }

    protected void writeCurrentStatus() {
        Log.d(TAG, mState + " --> " + mIsDiscovering + ", " + mIsAdvertising + ", " + mEstablishedConnections + ", " + mDiscoveredEndpoints);
    }

    protected void onConnectionFailed(Endpoint endpoint) {
        disconnectedFromEndpoint(endpoint);
        main.closeWaitingDialog(false);
        main.showWarningDialog("Connection failed");
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state) {
        if (mState == state) {
            Log.w(TAG, "State set to " + state + " but already in that state");
            return;
        }

        Log.d(TAG, "State set to " + state);
        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state, null);
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state, Endpoint endpoint) {
        if (mState == state) {
            Log.w(TAG, "State set to " + state + " but already in that state");
            return;
        }

        Log.d(TAG, "State set to " + state);
        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state, endpoint);
    }

    /**
     * @return The current state.
     */
    private State getState() {
        return mState;
    }

    /**
     * State has changed.
     *
     * @param oldState The previous state we were in. Clean up anything related to this state.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private void onStateChanged(State oldState, State newState, Endpoint endpoint) {
        // Update Nearby Connections to the new state.

        Log.d(TAG, "STATE CHANGE -- " + newState);
        switch (newState) {
            case DISCOVERING:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                disconnectFromAllEndpoints();
                startDiscovering();
                break;
            case ADVERTISING:
//                if(isAdvertising()){
//                    return; //already doing it
//                }
                if (isDiscovering()) {
                    stopDiscovering();
                }
                //disconnectFromAllEndpoints();
                startAdvertising();
                break;
            case CONNECTED:
                if (isDiscovering()) {
                    stopDiscovering();

                } else if (isAdvertising()) {
                    // Continue to advertise, so others can still connect,
                    // but clear the discover runnable.
                    main.isReadyToConnect = false; //connected
                    removeCallbacks(mDiscoverRunnable);
                }
                performConnection(endpoint);
                break;
            case UNKNOWN:
                disconnectFromAllEndpoints();
                break;
            default:
                // no-op
                break;
        }
    }

    private void performDisconnection(final Endpoint endpoint) {
        mUiHandler.post(() -> {
            //NOTE: this must be done on main thread
            main.nameView.setEnabled(true);

            if (main.isGuide) {
                //remove student and refresh view
                //main.getConnectedLearnersAdapter().removeStudent(endpoint.getId());

                main.getConnectedLearnersAdapter().alertStudentDisconnect(endpoint.getId());
                main.getConnectedLearnersAdapter().refresh();
                mEstablishedConnections.remove(endpoint.getId());


                if (!main.getConnectedLearnersAdapter().hasConnectedStudents()) {

                    if (everyoneDisconnectedView == null) {
                        everyoneDisconnectedView = View.inflate(main, R.layout.e__all_disconnected_popup, null);
                        Button ok_btn = everyoneDisconnectedView.findViewById(R.id.ok_btn);
                        Button back_btn = everyoneDisconnectedView.findViewById(R.id.back_btn);

                        ok_btn.setOnClickListener(v -> main.setUIDisconnected());

                        back_btn.setOnClickListener(v -> disconnectPrompt.hide());
                    }

                    if (main.hasWindowFocus() && main.isReadyToConnect) {
                        if (disconnectPrompt == null) {
                            disconnectPrompt = new AlertDialog.Builder(main)
                                    .setView(everyoneDisconnectedView)
                                    .show();
                        } else {
                            disconnectPrompt.show();
                        }

                    } else {
                        main.recallToLeadMe();
                    }

                }

            } else {
                main.getLeaderSelectAdapter().removeLeader(endpoint.getId());
                main.setUIDisconnected();
            }
        });
    }

    private void performConnection(final Endpoint endpoint) {
        // visual updates for GUIDE vs FOLLOWER
        main.nameView.setEnabled(false);

        if (main.isGuide) {
            main.showConnectedStudents(true);

        } else {
            /* set up and show learner view */
            //guide name, learner name
            main.displayLearnerMain(endpoint.name);

            main.getHandler().post(() -> {
                View clientMain = main.findViewById(R.id.client_main);
                clientMain.setVisibility(View.VISIBLE);
                main.readyBtn.setEnabled(false); //students can't disconnect themselves - guide must do this
                main.readyBtn.setText(R.string.learner_connected_label);
                if (main.isGuide) {
                    main.waitingForLearners.setVisibility(View.GONE);
                }
            });
        }
    }

    public void setAsGuide() {
        main.isGuide = true; //update this
        setState(State.ADVERTISING);
        postDelayed(mDiscoverRunnable, ADVERTISING_DURATION);
    }


    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.STREAM) {
            Log.d(TAG, "Got payload with a stream! -- TODO");

        } else if (payload.getType() == Payload.Type.BYTES) {
            //Log.d(TAG, "Got payload with bytes!");
            main.handlePayload(payload.asBytes());

        } else {
            Log.d(TAG, "Got some kinda payload! " + payload.getType() + " -- TODO");
        }

    }

    public boolean isConnectedAsFollower() {
        return !main.isGuide && getState() == State.CONNECTED;
    }

    public boolean isConnectedAsGuide() {
        return main.isGuide && getState() == State.CONNECTED;
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    public String getName() {
        if (main.nameView != null) {
            myName = main.nameView.getText().toString().trim();
        }

//        if (myName == null || myName.length() == 0) {
//            myName = "Buddy_" + (int) (Math.random() * 1000);
//            main.nameView.setText(myName);
//        }
        return myName;
    }

    public String getID() {
        return myId;
    }

    public void setID(String id) {
        if (myId != null) {
            Log.e(TAG, "My ID is already " + myId + ", can't set it to " + id + "!");
            return;
        }
        myId = id;
    }

    /**
     * {@see ConnectionsActivity#getServiceId()}
     */
    public String getServiceId() {
        return SERVICE_INSTANCE;
    }

    /**
     * {@see ConnectionsActivity#getStrategy()}
     */
    public Strategy getStrategy() {
        return STRATEGY;
    }

    /**
     * {@see Handler#postDelayed(Runnable, long)}
     */
    protected void postDelayed(Runnable r, long duration) {
        mUiHandler.postDelayed(r, duration);
    }

    /**
     * {@see Handler#removeCallbacks(Runnable)}
     */
    protected void removeCallbacks(Runnable r) {
        mUiHandler.removeCallbacks(r);
    }


    /**
     * States that the UI goes through.
     */
    public enum State {
        UNKNOWN,
        DISCOVERING,
        ADVERTISING,
        CONNECTED
    }

    /**
     * Callbacks for connections to other devices.
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
            Log.d(TAG, String.format("onConnectionInitiated(endpointId=%s, endpointName=%s)", endpointId, connectionInfo.getEndpointName()));
            Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
            mPendingConnections.put(endpointId, endpoint);
            acceptConnection(endpoint);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            Log.d(TAG, String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

            // We're no longer connecting
            mIsConnecting = false;

            if (!result.getStatus().isSuccess()) {
                Log.w(TAG, String.format("Connection failed. Received status %s.", "" + result.getStatus()));
                onConnectionFailed(mPendingConnections.remove(endpointId));
                return;
            }
            connectedToEndpoint(mPendingConnections.remove(endpointId));
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            if (!mEstablishedConnections.containsKey(endpointId)) {
                Log.w(TAG, "Unexpected disconnection from endpoint " + endpointId);
                return;
            }
            disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
        }
    };

    /**
     * Callbacks for payloads (bytes of data) sent from another device to us.
     */
    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            //Log.d(TAG, String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
            onReceive(mEstablishedConnections.get(endpointId), payload);
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
            //Log.d(TAG, String.format("onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
        }
    };

    /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either {@link #onAdvertisingStarted()} or {@link #onAdvertisingFailed()} will be called once
     * we've found out if we successfully entered this mode.
     */
    protected void startAdvertising() {
        mIsAdvertising = true;
        final String localEndpointName = getName() + "#" + main.isGuide;

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(getStrategy());

        mConnectionsClient
                .startAdvertising(
                        localEndpointName,
                        getServiceId(),
                        mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(
                        unusedResult -> {
                            Log.v(TAG, "Now advertising endpoint " + localEndpointName + ", " + getState());
                            onAdvertisingStarted();
                        })
                .addOnFailureListener(
                        e -> {
                            mIsAdvertising = false;
                            Log.w(TAG, "startAdvertising() failed.", e);
                            onAdvertisingFailed();
                        });
    }

    /**
     * Stops advertising.
     */
    protected void stopAdvertising() {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
        Log.v(TAG, "Stopping advertising " + getName() + "#" + main.isGuide);
    }

    /**
     * Returns {@code true} if currently advertising.
     */
    protected boolean isAdvertising() {
        return mIsAdvertising;
    }

    /**
     * Called when advertising successfully starts. Override this method to act on the event.
     */
    protected void onAdvertisingStarted() {
        setState(State.ADVERTISING);
    }

    /**
     * Called when advertising fails to start. Override this method to act on the event.
     */
    protected void onAdvertisingFailed() {
        setState(State.UNKNOWN);
    }

    /**
     * Accepts a connection request.
     */
    protected void acceptConnection(final Endpoint endpoint) {
        mConnectionsClient
                .acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnFailureListener(
                        e -> Log.w(TAG, "acceptConnection() failed.", e));
    }

    /**
     * Rejects a connection request.
     */
    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        e -> Log.w(TAG, "rejectConnection() failed.", e));
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * {@link #onDiscoveryStarted()} or {@link #onDiscoveryFailed()} will be called once we've found
     * out if we successfully entered this mode.
     */
    protected void startDiscovering() {
        if (mIsDiscovering) {
            return; //already discovering!
        }
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();

        discoveryOptions.setStrategy(getStrategy());
        mConnectionsClient
                .startDiscovery(
                        getServiceId(),
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                                Log.d(TAG, String.format("onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.getServiceId(), info.getEndpointName()));

                                if (getServiceId().equals(info.getServiceId()) && info.getEndpointName().split("#").length == 2) {
                                    Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                                    mDiscoveredEndpoints.put(endpointId, endpoint);
                                    onEndpointDiscovered(endpoint);
                                }
                            }

                            @Override
                            public void onEndpointLost(@NonNull String endpointId) {
                                Log.d(TAG, String.format("onEndpointLost(endpointId=%s)", endpointId));
                                main.getLeaderSelectAdapter().removeLeader(endpointId);
                            }
                        },
                        discoveryOptions.build())
                .addOnSuccessListener(
                        unusedResult -> onDiscoveryStarted())
                .addOnFailureListener(
                        e -> {
                            mIsDiscovering = false;
                            Log.w(TAG, "startDiscovering() failed.", e);
                            onDiscoveryFailed();
                        });

    }

    /**
     * Stops discovery.
     */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }

    /**
     * Returns {@code true} if currently discovering.
     */
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    /**
     * Called when discovery successfully starts.
     */
    protected void onDiscoveryStarted() {
    }

    /**
     * Called when discovery fails to start.
     */
    protected void onDiscoveryFailed() {
        Log.d(TAG, "Discovery failed. " + getState());
        setState(State.UNKNOWN);
        stopDiscovering();
    }

    /**
     * Disconnects from all currently connected endpoints.
     */
    protected void disconnectFromAllEndpoints() {
        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;
        mConnectionsClient.stopAllEndpoints();
        Log.d(TAG, "Disconnecting from all endpoints - " + getState());
        setState(State.UNKNOWN);
        for (Endpoint endpoint : mEstablishedConnections.values()) {
            performDisconnection(endpoint); //does UI updates
        }
        for (Endpoint endpoint : mDiscoveredEndpoints.values()) {
            performDisconnection(endpoint); //does UI updates
        }
        for (Endpoint endpoint : mPendingConnections.values()) {
            performDisconnection(endpoint); //does UI updates
        }
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
        main.getConnectedLearnersAdapter().removeAllStudents();

    }

    /**
     * Sends a connection request to the endpoint.
     */
    protected void connectToEndpoint(final Endpoint endpoint) {
        Log.v(TAG, "Sending a connection request to endpoint " + endpoint + " (" + main.isGuide + ")");
        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true;

        // Ask to connect
        mConnectionsClient
                .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(
                        e -> {
                            Log.w(TAG, "requestConnection() failed.", e);
                            mIsConnecting = false;
                            onConnectionFailed(endpoint);
                        });
    }

    /**
     * Returns {@code true} if we're currently attempting to connect to another device.
     */
    protected final boolean isConnecting() {
        return mIsConnecting;
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        Log.d(TAG, String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    protected void disconnectedFromEndpoint(Endpoint endpoint) {
        Log.d(TAG, String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }


    /**
     * Returns a list of currently connected endpoints.
     */
    protected Set<Endpoint> getDiscoveredEndpoints() {
        return new HashSet<>(mDiscoveredEndpoints.values());
    }

    /**
     * Returns a list of currently connected endpoints.
     */
    protected Set<Endpoint> getConnectedEndpoints() {
        return new HashSet<>(mEstablishedConnections.values());
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
            return mEstablishedConnections.keySet();
        }
    }

    public Set<String> getAllPeerIDs() {
        return mEstablishedConnections.keySet();
    }

    void sendToSelected(Payload payload, Set<String> endpoints) {
        Log.d(TAG, "Sending to: " + endpoints.size() + " endpoints.");
        Log.d(TAG, String.valueOf(endpoints));

        if (endpoints.size() > 0) {
            mConnectionsClient.sendPayload(new ArrayList<>(endpoints), payload)
                    .addOnFailureListener(e -> Log.w(TAG, "sendPayload() failed.", e));
        }
    }

    /**
     * Transforms a {@link Status} into a English-readable message for logging.
     *
     * @param status The current status
     * @return A readable String. eg. [404]File not found.
     */
    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }


    /**
     * Represents a device we can talk to.
     */
    public static class Endpoint {
        @NonNull
        private final String id, name;
        private final boolean isGuide;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name.split("#")[0];
            this.isGuide = name.endsWith("#true");
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public boolean isGuide() {
            return isGuide;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s, guide? %b}", id, name, isGuide);
        }
    }
}
