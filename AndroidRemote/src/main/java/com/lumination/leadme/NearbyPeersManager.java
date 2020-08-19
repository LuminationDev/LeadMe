package com.lumination.leadme;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
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
public class NearbyPeersManager implements SensorEventListener {

    private static final String TAG = "NearbyManager";

    private CheckBox checkBox;
    private View checkBoxView;

    private TextView textBox;
    private View textBoxView;
    private final String teachercode = "1990"; //hard coded for now

    private static int AUTO_DISCONNECT = -1; //-1 = unknown, 1 = yes, 0 = no

    //This service id lets us find other nearby devices that are interested in the same thing.
    private static final String SERVICE_INSTANCE = "com.lumination.leadme.LumiNearby_110";

    // The connection strategy we'll use for Nearby Connections. In this case, we've decided on
    // P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    //private static final Strategy STRATEGY = Strategy.P2P_STAR;

    /**
     * These permissions are required before connecting to Nearby Connections. Only {@link
     * Manifest.permission#ACCESS_COARSE_LOCATION} is considered dangerous, so the others should be
     * granted just by having them in our AndroidManifest.xml
     */
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    // Our handler to Nearby Connections
    private ConnectionsClient mConnectionsClient;

    //tracks the state of discovered endpoints
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();

    //tracks connection states
    private boolean mIsConnecting = false;
    private boolean mIsDiscovering = false;
    private boolean mIsAdvertising = false;


    // The SensorManager gives us access to sensors on the device.
    private SensorManager mSensorManager;
    // The accelerometer sensor allows us to detect device movement for shake-to-advertise.
    private Sensor mAccelerometer;

    // Acceleration required to detect a shake. In multiples of Earth's gravity.
    private static final float SHAKE_THRESHOLD_GRAVITY = 2;

    // Advertise for 30 seconds before going back to discovering. If a client connects, we'll continue
    // to advertise indefinitely so others can still connect.
    private static final long ADVERTISING_DURATION = 30000;

    // The state of the app. As the app changes states, the UI will update and advertising/discovery will start/stop.
    private State mState = State.UNKNOWN;

    //A Handler that allows us to post back on to the UI thread. We use this to resume discovery after an uneventful bout of advertising.
    private final Handler mUiHandler;
    private final MainActivity main;

    private String myName;
    private String myId;

    /**
     * Starts discovery. Used in a postDelayed manor with {@link #mUiHandler}.
     */
    private final Runnable mDiscoverRunnable = new Runnable() {
        @Override
        public void run() {
            if (main.isReadyToConnect && getState() != State.CONNECTED) {
                Log.i(TAG, "In runnable - " + main.isGuide + " in " + getState());
                setState(State.DISCOVERING);
            }
        }
    };

    public NearbyPeersManager(MainActivity main) {
        Log.d(TAG, "Created NearbyManager!");

        this.main = main;
        mUiHandler = main.getHandler();
        mConnectionsClient = Nearby.getConnectionsClient(main);

        mSensorManager = (SensorManager) main.getSystemService(MainActivity.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void onStart() {
        //if it's already connected, leave it alone!
        if (main.isReadyToConnect && getState() != State.CONNECTED) {

            Log.i(TAG, "In onStart - " + main.isGuide);
            setState(State.DISCOVERING);
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        Log.d(TAG, "onStart() State is: " + getState());
    }

    public void onStop() {
        //if it's already connected, leave it alone!
        if (getState() != State.CONNECTED) {
            setState(State.UNKNOWN);
            mSensorManager.unregisterListener(this);
            mUiHandler.removeCallbacksAndMessages(null);
        }
        Log.d(TAG, "onStop() State is: " + getState());
    }

    public void onBackPressed() {
        //TODO I'm not sure about these states?
        if (main.isReadyToConnect && (getState() == State.CONNECTED || getState() == State.ADVERTISING)) {
            Log.i(TAG, "In back pressed - " + main.isGuide);
            setState(State.DISCOVERING);
            return;
        }
    }

    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        if (!isConnecting()) {
            connectToEndpoint(endpoint);
        }
    }

    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // A connection to another device has been initiated! We'll accept the connection immediately.
        acceptConnection(endpoint);
    }

    protected void onEndpointConnected(final Endpoint endpoint) {
        Toast.makeText(main, "Connected to " + endpoint.getName(), Toast.LENGTH_SHORT).show();
        setState(State.CONNECTED, endpoint);

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                //NOTE: this must be done on main thread
                Log.d(TAG, "Am I the guide? " + main.isGuide);
                if (main.isGuide) {
                    LumiPeer thisPeer = new LumiPeer(endpoint);
                    main.getConnectedStudentsFragment().addStudent(thisPeer);
                    //send the ID back to the student
                    main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.YOUR_ID_IS + thisPeer.getID() + ":" + thisPeer.getDisplayName());
                }
            }
        });
    }

    protected void onEndpointDisconnected(final Endpoint endpoint) {
        Toast.makeText(main, "Disconnected from " + endpoint.getName(), Toast.LENGTH_SHORT).show();
        performDisconnection(endpoint);

        // If we lost all our endpoints, then we should reset the state of our app and go back
        // to our initial state (discovering).
        if (main.isReadyToConnect && getConnectedEndpoints().isEmpty()) {

            Log.i(TAG, "In onEndPointDisconnected - " + main.isGuide);
            setState(State.DISCOVERING);
        }
    }

    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
        }
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
        switch (newState) {
            case DISCOVERING:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                disconnectFromAllEndpoints();
                startDiscovering();
                break;
            case ADVERTISING:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpoints();
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
                stopAllEndpoints();
                break;
            default:
                // no-op
                break;
        }
    }

    private void performDisconnection(final Endpoint endpoint) {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                //NOTE: this must be done on main thread
                main.nameView.setEnabled(true);

                if (main.isGuide) {
                    //remove student and refresh view
                    main.getConnectedStudentsFragment().removeStudent(endpoint.getId());
                    main.getConnectedStudentsFragment().refresh();

                    if (!main.getConnectedStudentsFragment().hasConnectedStudents()) {

                        if (AUTO_DISCONNECT == 1) {
                            main.setUIDisconnected();

                        } else if (AUTO_DISCONNECT == -1) {
                            //prompt

                            if (checkBoxView == null) {
                                checkBoxView = View.inflate(main, R.layout.alert_checkbox, null);
                                checkBox = (CheckBox) checkBoxView.findViewById(R.id.alertCheckBox);
                            }

                            if (main.hasWindowFocus()) {
                                AlertDialog disconnectPrompt = new AlertDialog.Builder(main)
                                        .setTitle("Disconnect")
                                        .setMessage("All followers have disconnected.\nDo you want to stop guiding now?")
                                        .setView(checkBoxView)

                                        // Specifying a listener allows you to take an action before dismissing the dialog.
                                        // The dialog is automatically dismissed when a dialog button is clicked.
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (checkBox.isChecked()) {
                                                    AUTO_DISCONNECT = 1;
                                                }
                                                main.setUIDisconnected();
                                            }
                                        })

                                        // A null listener allows the button to dismiss the dialog and take no further action.
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (checkBox.isChecked()) {
                                                    AUTO_DISCONNECT = 0;
                                                }
                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            } else {
                                main.returnToAppAction();
                                //TODO and prompt to disconnect
                            }
                        }
                    }

                } else {
                    main.setUIDisconnected();
                }
            }
        });
    }

    private void performConnection(final Endpoint endpoint) {
        // visual updates for GUIDE vs FOLLOWER
        main.nameView.setEnabled(false);

        main.progress.setVisibility(View.INVISIBLE);
        main.statusContainer.setVisibility(View.GONE);

        if (main.isGuide) {
            /* show relevant things */
            if (main.gestureCaptureView != null) {
                main.gestureCaptureView.setGestureVisible(true);
            }

            final ConnectedStudentsFragment connectedStudentsFragment = main.getConnectedStudentsFragment();
            if (connectedStudentsFragment != null) {
                main.findViewById(R.id.follower_layout).setVisibility(View.VISIBLE);
            } else {
                Log.w(TAG, "Connected students is NULL!!");
            }

            View controlBtns = main.findViewById(R.id.controlBtns);
            controlBtns.setVisibility(View.VISIBLE);


            /* show app list for guide */
            View appList = main.findViewById(R.id.frag_appList);
            appList.setVisibility(View.VISIBLE);
            main.readyBtn.setText(R.string.guide_connected_label);


        } else {
            /* hide unnecessary stuff */
            if (main.gestureCaptureView != null) {
                main.gestureCaptureView.setGestureVisible(false);
            }

            /* show client view */
            TextView clientTitle = main.findViewById(R.id.client_text_state);
            clientTitle.setText(main.getResources().getString(R.string.client_connected_message_state) + " " + endpoint.name + "!");

            View clientMain = main.findViewById(R.id.client_main);
            clientMain.setVisibility(View.VISIBLE);
            main.readyBtn.setEnabled(false); //students can't disconnect themselves - host must do this
            main.readyBtn.setText(R.string.learner_connected_label);
            main.progress.setVisibility(View.INVISIBLE);

            //main.startLockTask();
        }

        main.setStudentMenuVisibility(false);
    }

    /**
     * The device has moved. We need to decide if it was intentional or not.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY && getState() == State.DISCOVERING) {
            enactShake();
        }
    }

    boolean dialogOpen = false;
    private AlertDialog codePrompt;

    public void enactShake() {
        Log.d(TAG, "Device shaken");
        if (!dialogOpen) {
            dialogOpen = true;

            if (textBoxView == null) {
                textBoxView = View.inflate(main, R.layout.alert_textbox, null);
                textBox = (TextView) textBoxView.findViewById(R.id.alertTextBox);
            }

            //TODO change this to a saved code rather than hardcoded value
            if (!main.isGuide && codePrompt == null) {
                codePrompt = new AlertDialog.Builder(main)
                        .setTitle("Teacher Lock")
                        .setView(textBoxView)

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialogOpen = false;
                                if (textBox != null && textBox.getText().toString().equals(teachercode)) {
                                    setAsGuide();
                                } else {
                                    wrongCode();
                                }
                            }
                        })

                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //no action
                                dialogOpen = false;
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            } else if (!main.isGuide) {
                codePrompt.show();
            }
        }
    }

    private void setAsGuide() {
        main.isGuide = true; //update this
        main.hostSwitch.setChecked(true);
        setState(State.ADVERTISING);
        postDelayed(mDiscoverRunnable, ADVERTISING_DURATION);
    }

    private void wrongCode() {
        AlertDialog codePrompt = new AlertDialog.Builder(main)
                .setTitle("Wrong Code")
                .setMessage("Sorry, wrong code!")
                .show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.STREAM) {
            Log.d(TAG, "Got payload with a stream!");

        } else if (payload.getType() == Payload.Type.BYTES) {
            Log.d(TAG, "Got payload with bytes!");
            main.handlePayload(payload.asBytes());

        } else {
            Log.d(TAG, "Got some kinda payload! " + payload.getType());
        }

    }

    public boolean isConnectedAsFollower() {
        return !main.isGuide && getState() == State.CONNECTED;
    }

    public boolean isConnectedAsGuide() {
        return main.isGuide && getState() == State.CONNECTED;
    }

    public boolean write(byte[] bytes) {
        try {
            main.nearbyManager.send(Payload.fromBytes(bytes));
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    public String getName() {
        if (main.nameView != null) {
            myName = main.nameView.getText().toString();
        }

        if (myName == null || myName.length() == 0) {
            myName = "Buddy_" + (int) (Math.random() * 1000);
            main.nameView.setText(myName);
        }
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

    @SuppressWarnings("unchecked")
    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
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
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Log.d(TAG, String.format("onConnectionInitiated(endpointId=%s, endpointName=%s)", endpointId, connectionInfo.getEndpointName()));
            Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
            mPendingConnections.put(endpointId, endpoint);
            acceptConnection(endpoint);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
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
        public void onDisconnected(String endpointId) {
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
        public void onPayloadReceived(String endpointId, Payload payload) {
            Log.d(TAG, String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
            onReceive(mEstablishedConnections.get(endpointId), payload);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            Log.d(TAG, String.format("onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
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
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                Log.v(TAG, "Now advertising endpoint " + localEndpointName);
                                onAdvertisingStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                Log.w(TAG, "startAdvertising() failed.", e);
                                onAdvertisingFailed();
                            }
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
    }

    /**
     * Called when advertising fails to start. Override this method to act on the event.
     */
    protected void onAdvertisingFailed() {
    }

    /**
     * Accepts a connection request.
     */
    protected void acceptConnection(final Endpoint endpoint) {
        mConnectionsClient
                .acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "acceptConnection() failed.", e);
                            }
                        });
    }

    /**
     * Rejects a connection request.
     */
    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "rejectConnection() failed.", e);
                            }
                        });
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * {@link #onDiscoveryStarted()} or {@link #onDiscoveryFailed()} will be called once we've found
     * out if we successfully entered this mode.
     */
    protected void startDiscovering() {
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(getStrategy());
        mConnectionsClient
                .startDiscovery(
                        getServiceId(),
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                                Log.d(TAG, String.format(
                                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.getServiceId(), info.getEndpointName()));

                                if (getServiceId().equals(info.getServiceId())) {
                                    Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                                    mDiscoveredEndpoints.put(endpointId, endpoint);
                                    onEndpointDiscovered(endpoint);
                                }
                            }

                            @Override
                            public void onEndpointLost(String endpointId) {
                                Log.d(TAG, String.format("onEndpointLost(endpointId=%s)", endpointId));
                            }
                        },
                        discoveryOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                onDiscoveryStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                Log.w(TAG, "startDiscovering() failed.", e);
                                onDiscoveryFailed();
                            }
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
    }

    /**
     * Disconnects from all currently connected endpoints.
     */
    protected void disconnectFromAllEndpoints() {
        mConnectionsClient.stopAllEndpoints();
        for (Endpoint endpoint : mEstablishedConnections.values()) {
//            mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
//            mEstablishedConnections.remove(endpoint.getId());
            performDisconnection(endpoint); //does UI updates
        }
        mEstablishedConnections.clear();

        Log.i(TAG, "In disconnectFromAllEndpoints - " + main.isGuide);
        setState(State.DISCOVERING);
    }

    /**
     * Resets and clears all state in Nearby Connections.
     */
    protected void stopAllEndpoints() {
        mConnectionsClient.stopAllEndpoints();
        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    /**
     * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
     * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
     * if we successfully reached the device.
     */
    protected void connectToEndpoint(final Endpoint endpoint) {
        Log.v(TAG, "Sending a connection request to endpoint " + endpoint);
        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true;

        // Ask to connect
        mConnectionsClient
                .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "requestConnection() failed.", e);
                                mIsConnecting = false;
                                onConnectionFailed(endpoint);
                            }
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

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        Log.d(TAG, String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
        if (!main.isGuide) {
            main.isReadyToConnect = false; //need button press
        }
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

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    protected void send(Payload payload) {
        send(payload, mEstablishedConnections.keySet());
    }

    private void send(Payload payload, Set<String> endpoints) {
        mConnectionsClient
                .sendPayload(new ArrayList<>(endpoints), payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "sendPayload() failed.", e);
                            }
                        });
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
        private final String id;
        @NonNull
        private final String name;
        @NonNull
        private boolean isGuide;

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

        @NonNull
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
