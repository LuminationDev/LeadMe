package com.lumination.leadme.managers;

import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.services.NetworkService;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.UUID;

/**
 * Class create specifically to interact with Firebase, not to handle authentication but to make
 * calls for manual connection and handle the Firebase service.
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static String serverIP = ""; //needs to be "" so that the learner can see check against
    public static String roomCode;
    private static String localIpAddress;
    private static String uuid;
    public static DatabaseReference roomReference;
    public static DatabaseReference learnerReference;
    public static DatabaseReference messagesReference;
    public static ValueEventListener learnerReceiveMessageListener;
    public static ValueEventListener leaderReceivingLearnerMessageListener;
    public static ValueEventListener addLearnerListener;

    private static DatabaseReference getDatabase()
    {
        return FirebaseDatabase.getInstance("https://leafy-rope-301003-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
    }

    public static void sendAllLearnerMessage(String message)
    {
        if (messagesReference == null) {
            return;
        }
        messagesReference.child("currentMessage").setValue(message);
    }

    public static void sendLearnerMessage(String ipAddress, String message)
    {
        if (messagesReference == null) {
            return;
        }
        messagesReference.child("learners").child(ipAddress.replace(".", "_")).child("leaderMessage").setValue(message);
    }

    public static void sendLeaderMessage(String message)
    {
        if (learnerReference == null) {
            return;
        }
        learnerReference.child("currentMessage").setValue(message);
    }

    public static void connectToLeader()
    {
        NearbyPeersManager.setID(getUuid());
        DatabaseReference database = getDatabase();
        roomReference = database.child(roomCode).child("room");
        messagesReference = database.child(roomCode).child("messages").child(NearbyPeersManager.selectedLeader.getID());

        learnerReceiveMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                if (!dataSnapshot.exists()) {
                    NetworkManager.receivedDisconnect();
                    return;
                }
                if (dataSnapshot.getValue() != null && dataSnapshot.getValue().toString().length() > 0) {
                    NetworkService.receiveMessage(dataSnapshot.getValue().toString());

                    Log.e(TAG, dataSnapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        messagesReference.child("learners").child(getUuid().replace(".", "_")).child("currentMessage").setValue("");
        messagesReference.child("learners").child(getUuid().replace(".", "_")).child("currentMessage").onDisconnect().setValue("DISCONNECT," + getUuid().replace(".", "_"));
        learnerReference = messagesReference.child("learners").child(getUuid().replace(".", "_"));
        messagesReference.child("learners").child(getUuid().replace(".", "_")).child("leaderMessage").setValue("");

        messagesReference.child("currentMessage").addValueEventListener(learnerReceiveMessageListener);
        messagesReference.child("learners").child(getUuid().replace(".", "_")).child("leaderMessage").addValueEventListener(learnerReceiveMessageListener);
    }

    public static void connectAsLeader(String name)
    {
        DatabaseReference database = getDatabase();

        String tempRoomCode = generateRoomCode(4);
        database.child(tempRoomCode).child("room").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    connectAsLeader(name);
                } else {
                    roomCode = tempRoomCode;

                    /*
                    The next two lines shouldn't be in here, but the app is already such a mess, that here they are
                     */
                    TextView title = LeadMeMain.getInstance().mainLeader.findViewById(R.id.room_code);
                    title.setText("Room code: " + roomCode);

                    database.child(roomCode).child("room").child("id").setValue(getUuid());
                    roomReference = database.child(roomCode).child("room");
                    messagesReference = database.child(roomCode).child("messages").child(getUuid().replace(".", "_"));
                    messagesReference.child("learners").child("id").setValue(getUuid());
                    messagesReference.child("currentMessage").setValue("");
                    roomReference.child("leaderName").setValue(name);
                    roomReference.child("localIpAddress").setValue(getLocalIpAddress());

                    database.child(roomCode).onDisconnect().removeValue();
                    messagesReference.child("learners").removeValue();

                    addLearnerListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // Get Post object and use the values to update the UI
                            Log.e(TAG, dataSnapshot.toString());
                            if (dataSnapshot.getChildrenCount() != ConnectedLearnersAdapter.mData.size()) {
                                for (DataSnapshot data:dataSnapshot.getChildren()) {
                                    if (!data.getKey().equals("id") &&
                                            (data.child("leaderMessage").getValue() == null || !data.child("leaderMessage").getValue().toString().startsWith("DISCONNECT"))) {
                                        NetworkManager.addLearnerIfNotExists(data.getKey());
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    };
                    messagesReference.child("learners").addValueEventListener(addLearnerListener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // todo - throw an error
            }
        });
    }

    public static void retrieveLeaderDetails(LeadMeMain main, TextView errorText)
    {
        errorText.setVisibility(View.GONE);
        DatabaseReference database = getDatabase();
        database.child(roomCode).child("room").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Leader leader = new Leader(
                            (String) snapshot.child("leaderName").getValue(),
                            (String) snapshot.child("id").getValue(),
                            (String) snapshot.child("ipAddress").getValue()
                    );
                    Controller.getInstance().getNearbyManager().setSelectedLeader(leader);
                    main.showLoginDialog();
                } else {
                    errorText.setVisibility(View.VISIBLE);
                    errorText.setText("Room not found. Try again");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AAA", "cancelled");
            }
        });
    }

    public static void handleLearnerConnectingToLeader(String clientId)
    {
        leaderReceivingLearnerMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "message received:" + dataSnapshot.toString());
                Object messageObj = dataSnapshot.getValue();
                if (messageObj != null) {
                    String message = messageObj.toString();
                    if (message.length() > 0) {
                        NetworkService.determineAction(clientId, message);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        messagesReference.child("learners").child(clientId).child("currentMessage").addValueEventListener(leaderReceivingLearnerMessageListener);
    }

    public static void removeLearner(String clientId) {
        messagesReference.child("learners").child(clientId).child("currentMessage").removeEventListener(leaderReceivingLearnerMessageListener);
        messagesReference.child("learners").child(clientId).removeValue();
    }

    public static void handleDisconnect(boolean isGuide) {
        if (messagesReference != null) {
            if (!isGuide) {
                messagesReference.child("learners").child(getUuid().replace(".", "_")).child("currentMessage").onDisconnect().cancel();
                messagesReference.child("learners").child(getUuid().replace(".", "_")).removeValue();
            }
            if (learnerReceiveMessageListener != null) {
                messagesReference.child("currentMessage").removeEventListener(learnerReceiveMessageListener);
                messagesReference.child("learners").child(getUuid().replace(".", "_")).child("leaderMessage").removeEventListener(learnerReceiveMessageListener);
            }
            if (addLearnerListener != null) {
                messagesReference.child("learners").removeEventListener(addLearnerListener);
            }
            if (isGuide) {
                messagesReference.removeValue();
            }
            messagesReference = null;
        }
        if (roomReference != null) {
            if (isGuide) {
                roomReference.removeValue();
            }
            roomReference = null;
        }
    }

    private static String generateRoomCode(int length) {
        // Define the characters that can be used to generate the string
        String characters = "0123456789";

        // Create a new Random object
        Random random = new Random();

        // Create a StringBuilder to store the random string
        StringBuilder sb = new StringBuilder();

        // Generate the random string
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            char randomChar = characters.charAt(index);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    public static String getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    /**
     * Set the server IP
     * @param ip A String representing the public IP address the guide is connected to.
     */
    public static void setServerIP(String ip) {
        serverIP = ip;
    }

    /**
     * Get the current server IP.
     * @return A String representing the public IP address the guide is connected to.
     */
    public static String getServerIP() {
        return serverIP;
    }

    /**
     * Call the firebase to get the current version of the application that is on the play store.
     */
    public static void checkCurrentVersion() {
        DocumentReference docRef = db.collection("version").document("production");

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                try {
                    String version = task.getResult().get("version_id").toString();
                    compareVersions(version);
                } catch (NullPointerException e) {
                    Log.d(TAG, "onComplete: " + e);
                }
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    /**
     * Compare the current LeadMe version against the one marked as production on the Firestore.
     * @param productionVersion A string representing the latest version as detailed by the
     *                          Firestore entry.
     */
    private static void compareVersions(String productionVersion) {
        String runningVersion = "";

        Log.d(TAG, "Production version: " + productionVersion);

        if (!productionVersion.equals("")) {
            try {
                runningVersion = LeadMeMain.getInstance().context.getPackageManager()
                        .getPackageInfo(LeadMeMain.getInstance().context.getPackageName(), 0)
                        .versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Running version: " + runningVersion);

            if(!runningVersion.equals(productionVersion)) {
                Controller.getInstance().getDialogManager().showUpdateDialog();
            }
        }
    }

    public static String getLocalIpAddress() {
        if (localIpAddress != null && localIpAddress.length() > 0) {
            return localIpAddress;
        }
        try  {
            localIpAddress = InetAddress.getByAddress(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(LeadMeMain.wifiManager.getConnectionInfo().getIpAddress())
                            .array()
            ).getHostAddress();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return localIpAddress;
    }
}
