package com.lumination.leadme.managers;

import android.content.pm.PackageManager;
import android.util.Log;

import com.google.common.net.InetAddresses;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.adapters.ConnectedLearnersAdapter;
import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.services.FirebaseService;
import com.lumination.leadme.services.NetworkService;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Class create specifically to interact with Firebase, not to handle authentication but to make
 * calls for manual connection and handle the Firebase service.
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static String serverIP = ""; //needs to be "" so that the learner can see check against
    private static String publicIP;
    private static String localIpAddress;
    public static DatabaseReference roomsReference;
    public static DatabaseReference roomReference;
    public static DatabaseReference learnerReference;
    public static DatabaseReference messagesReference;
    public static ValueEventListener roomsListener;
    public static ValueEventListener learnerReceiveMessageListener;
    public static ValueEventListener leaderReceivingLearnerMessageListener;
    public static ValueEventListener addLearnerListener;

    private static DatabaseReference getDatabase()
    {
        return FirebaseDatabase.getInstance("https://leafy-rope-301003-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
    }

    public static void sendAllLearnerMessage(String message)
    {
        messagesReference.child("currentMessage").setValue(message);
    }

    public static void sendLearnerMessage(String ipAddress, String message)
    {
        messagesReference.child("learners").child(ipAddress.replace(".", "_")).child("leaderMessage").setValue(message);
    }

    public static void sendLeaderMessage(String message)
    {
        learnerReference.child("currentMessage").setValue(message);
    }

    public static void connectToLeader()
    {
        DatabaseReference database = getDatabase();
        roomReference = database.child(waitForPublic().replace(".", "_")).child("rooms").child(NetworkService.getLeaderIPAddress().getHostAddress().replace(".", "_"));
        messagesReference = database.child(waitForPublic().replace(".", "_")).child("messages").child(NetworkService.getLeaderIPAddress().getHostAddress().replace(".", "_"));

        learnerReceiveMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                if (dataSnapshot.getValue() != null && dataSnapshot.getValue().toString().length() > 0) {
                    NetworkService.receiveMessage(dataSnapshot.getValue().toString());

                    Log.e("firebase", dataSnapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        messagesReference.child("learners").child(getLocalIpAddress().replace(".", "_")).child("currentMessage").setValue("");
        messagesReference.child("learners").child(getLocalIpAddress().replace(".", "_")).child("currentMessage").onDisconnect().setValue("DISCONNECT," + getLocalIpAddress().replace(".", "_"));
        learnerReference = messagesReference.child("learners").child(getLocalIpAddress().replace(".", "_"));
        messagesReference.child("learners").child(getLocalIpAddress().replace(".", "_")).child("leaderMessage").setValue("");

        messagesReference.child("currentMessage").addValueEventListener(learnerReceiveMessageListener);
        messagesReference.child("learners").child(getLocalIpAddress().replace(".", "_")).child("leaderMessage").addValueEventListener(learnerReceiveMessageListener);
    }

    public static void connectAsLeader(String name)
    {
        DatabaseReference database = getDatabase();
        database.child(waitForPublic().replace(".", "_")).child("rooms").child(getLocalIpAddress().replace(".", "_")).setValue("roomCreated");
        roomReference = database.child(waitForPublic().replace(".", "_")).child("rooms").child(getLocalIpAddress().replace(".", "_"));
        messagesReference = database.child(waitForPublic().replace(".", "_")).child("messages").child(getLocalIpAddress().replace(".", "_"));
        messagesReference.child("learners").setValue("emptyLearners");
        messagesReference.child("currentMessage").setValue("");
        roomReference.child("leaderName").setValue(name);

        roomReference.onDisconnect().removeValue();
        messagesReference.onDisconnect().removeValue();

        addLearnerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                Log.e("firebase", dataSnapshot.toString());
                if (dataSnapshot.getChildrenCount() != ConnectedLearnersAdapter.mData.size()) {
                    for (DataSnapshot data:dataSnapshot.getChildren()) {
                        if (data.child("leaderMessage").getValue() == null || !data.child("leaderMessage").getValue().toString().startsWith("DISCONNECT")) {
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

    public static void handleDisconnect() {
        if (messagesReference != null) {
            if (learnerReceiveMessageListener != null) {
                messagesReference.child("currentMessage").removeEventListener(learnerReceiveMessageListener);
                messagesReference.child("learners").child(getLocalIpAddress().replace(".", "_")).child("leaderMessage").removeEventListener(learnerReceiveMessageListener);
            }
            if (addLearnerListener != null) {
                messagesReference.child("learners").removeEventListener(addLearnerListener);
            }
            messagesReference.removeValue();
            messagesReference = null;
        }
        if (roomReference != null) {
            roomReference.removeValue();
            roomReference = null;
        }
    }

    /**
     * A call to firebase to retrieve any leaders registered to a peer's publicIP address. If there
     * are no records the function will repeat every x seconds where x is an integer (waitForGuide).
     * Will only return leaders who have been active within a certain period of time (inactiveUser).
     * If the public IP address is present but the certain leader hasn't logged on yet, a listener is
     * attached to wait for any chances in the firebase.
     * */
    public static void retrieveLeaders() {
        waitForPublic();

        Log.d(TAG, "retrieveLeaders: " + publicIP);

        if (publicIP == null || publicIP.length() == 0) {
            Log.d(TAG, "PublicIP address not found");
            Controller.getInstance().getDialogManager().showWarningDialog("Public IP", "Public IP address not found" +
                    "\n firewall may be blocking the query.");
            return;
        } else if (!InetAddresses.isInetAddress(publicIP)) {
            Log.d(TAG, "PublicIP address not valid");
            publicIP = null;
            Controller.getInstance().getDialogManager().showWarningDialog("Public IP", "Public IP address not valid" +
                    "\n firewall may be blocking the query.");
            return;
        }

        //if a user has logged in as a guide then this function should not be triggered anymore
        if(LeadMeMain.isGuide) {
            return;
        }

        DatabaseReference database = FirebaseDatabase.getInstance("https://leafy-rope-301003-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        roomsReference = database.child(waitForPublic().replace(".", "_")).child("rooms");
        if (roomsListener != null) {
            FirebaseManager.roomsReference.removeEventListener(FirebaseManager.roomsListener);
        }
        roomsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                Log.e("firebase", dataSnapshot.toString());
                Controller.getInstance().getLeaderSelectAdapter().setLeaderList(new ArrayList<>());
                if (dataSnapshot.getChildrenCount() != ConnectedLearnersAdapter.mData.size()) {
                    for (DataSnapshot data:dataSnapshot.getChildren()) {
                        if (data.child("leaderName").getValue() != null) {
                            LeadMeMain.runOnUI(() -> {
                                Controller.getInstance().getLeaderSelectAdapter().addLeader(
                                        new ConnectedPeer(
                                                data.child("leaderName").getValue().toString(),
                                                data.getKey().replace("_", ".")));

                                LeadMeMain.getInstance().showLeaderWaitMsg(false);
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.e(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        roomsReference.addValueEventListener(roomsListener);
    }

    /**
     * Attempt to get the public IP address of the current device (router address)
     */
    private static String waitForPublic() {
        if (publicIP != null) {
            return  publicIP;
        }
        Thread getPublic = new Thread(() -> {
            publicIP = getPublicIP();
        });

        getPublic.start();

        try {
            getPublic.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return publicIP;
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

    //Call https://api.ipify.org to simply get the public IP address
    private static String getPublicIP() {
        String internalPublicIp = "";
        try  {
            java.util.Scanner s = new java.util.Scanner(
                    new java.net.URL(
                            "https://api.ipify.org")
                            .openStream(), "UTF-8")
                    .useDelimiter("\\A");
            internalPublicIp = s.next();
            Log.d(TAG, "getPublicIP: got public");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Log.d(TAG, "getPublicIP: didn't get public");
        }

        return internalPublicIp;
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
}
