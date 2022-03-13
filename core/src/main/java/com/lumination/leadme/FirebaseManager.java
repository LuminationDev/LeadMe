package com.lumination.leadme;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.google.common.net.InetAddresses;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class create specifically to interact with Firebase, not to handle authentication but to make
 * calls for manual connection and handle the Firebase service.
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private final LeadMeMain main;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collRef = null;
    private ListenerRegistration manualUserListener;

    private Timer timestampUpdater;
    private final int inactiveUser = 30; //cut off for hiding inactive leaders (mins)
    private final int waitForGuide = 10000; //how long to wait before peer re-query's firestore
    private String serverIP = ""; //needs to be "" so that the learner can see check against
    private String publicIP;
    private final HashMap<String, Object> manualConnectionDetails = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    public FirebaseManager(LeadMeMain main) {
        this.main = main;
    }

    public void startService() {
        Log.d(TAG, "startService: ");
        Intent firebase_intent = new Intent(main.getApplicationContext(), FirebaseService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            main.startForegroundService(firebase_intent);
        } else {
            main.startService(firebase_intent);
        }
    }

    public void stopService() {
        if(timestampUpdater != null) {
            timestampUpdater.cancel();
        }
        Intent stop_firebase_intent = new Intent(main.getApplicationContext(), FirebaseService.class);
        main.stopService(stop_firebase_intent);
    }

    /**
     * A call to firebase to retrieve any leaders registered to a peer's publicIP address. If there
     * are no records the function will repeat every x seconds where x is an integer (waitForGuide).
     * Will only return leaders who have been active within a certain period of time (inactiveUser).
     * If the public IP address is present but the certain leader hasn't logged on yet, a listener is
     * attached to wait for any chances in the firebase.
     * */
    public void retrieveLeaders() {
        if(publicIP == null) {
            waitForPublic();
        }

        Log.d(TAG, "retrieveLeaders: " + publicIP);

        if (publicIP == null || publicIP.length() == 0) {
            Log.d(TAG, "PublicIP address not found");
            main.getDialogManager().showWarningDialog("Public IP", "Public IP address not found" +
                    "\n firewall may be blocking the query.");
            return;
        } else if (!InetAddresses.isInetAddress(publicIP)) {
            Log.d(TAG, "PublicIP address not valid");
            publicIP = null;
            main.getDialogManager().showWarningDialog("Public IP", "Public IP address not valid" +
                    "\n firewall may be blocking the query.");
            return;
        }

        //if a user has logged in as a guide then this function should not be triggered anymore
        if(LeadMeMain.isGuide) {
            return;
        }

        collRef = db.collection("addresses").document(publicIP).collection("Leaders");

        collRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                //if no one has registered on the public IP yet, wait sometime and try again.
                try {
                    if (Objects.requireNonNull(task.getResult()).size() == 0) {
                        //In case the user switches back to auto
                        if (main.sessionManual && !main.directConnection) {
                            scheduledExecutorService.schedule(this::retrieveLeaders, waitForGuide, TimeUnit.MILLISECONDS);
                        }
                    } else {
                        //add listeners to track if leader hasn't logged in but publicIP exists (multiple leaders on network)
                        trackCollection(collRef);
                    }
                } catch (NullPointerException e) {
                    Log.d(TAG, "onComplete: " + e);
                }
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    //add a listener to the Leader collection to wait for log in
    private void trackCollection(CollectionReference collRef) {
        if(manualUserListener != null){
            manualUserListener.remove();
        }

        if(!main.getNearbyManager().isConnectedAsFollower()) {
            Log.d(TAG, "trackCollection: listener added");

            manualUserListener = collRef.addSnapshotListener((value, error) -> {
                Log.d(TAG, "onEvent: ip listener fired");
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }

                if (value != null) {
                    for (QueryDocumentSnapshot document : value) {
                        Date leaderTimeStamp = Objects.requireNonNull(document.getTimestamp("TimeStamp")).toDate();

                        if (checkTimeDifference(leaderTimeStamp) >= inactiveUser) {
                            return;
                        }

                        if (document.get("Username") != null) {
                            main.manuallyAddLeader(document.get("Username").toString(), document.get("ServerIP").toString());
                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            });
        }
    }

    /**
     * Removes the user entry listener attached to the firebase collection that looks for new
     * leaders signing in.
     */
    public void removeUserListener() {
        if (manualUserListener != null) {
            Log.d(TAG, "loginAction: listener removed");
            manualUserListener.remove();
            manualUserListener = null;
        }
    }

    /**
     * calculate the difference between the firebase timestamp and the current time
     * return the minutes, can also work out other units if necessary.
     */
    private int checkTimeDifference(Date leaderTimeStamp) {
        Date now = new Date();

        long difference_In_Time = now.getTime() - leaderTimeStamp.getTime();
        long difference_In_Minutes = (difference_In_Time / (1000 * 60)) % 60;
        long difference_In_Hours = (difference_In_Time / (1000 * 60 * 60)) % 24;

        /*max difference can be 24 * 60 (mins), server clears firestore once or twice a day
        so anything more is not necessary at this point.*/
        return (int) (difference_In_Hours * 60) + (int) difference_In_Minutes;
    }

    /**
     * Create a new entry in firebase for a Guide, allowing peers to connect manually instead of
     * by discovery. Updates the timestamp every set period while leader is logged in, separate
     * code clears the database every few hours.
     * @param ipAddress A String representing the local IPAddress of the Guide's device.
     */
    public void createManualConnection(String ipAddress) {
        //initiate public ip track and firebase
        waitForPublic();

        serverIP = ipAddress;
        FirebaseService.setServerIP(serverIP);
        manualConnectionDetails.put("Email", main.getAuthenticationManager().getCurrentAuthEmail());
        manualConnectionDetails.put("Username", main.getAuthenticationManager().getCurrentAuthUserName());
        manualConnectionDetails.put("ServerIP", serverIP);
        manualConnectionDetails.put("TimeStamp", FieldValue.serverTimestamp());

        //update firebase and start a continuous timer for updating the timestamp
        timestampUpdater = new Timer();
        TimerTask updateTimestamp = new TimerTask() {
            @Override
            public void run() {
                updateAddress();
            }
        };

        //update the leaders timestamp on firebase (mins)
        int leaderTimestampUpdate = 15;
        timestampUpdater.scheduleAtFixedRate(updateTimestamp, 0L, leaderTimestampUpdate * (60 * 1000));
    }

    /**
     * If the public IP is not null check for duplicate entries in the database.
     */
    private void updateAddress() {
        if(publicIP.length() == 0) {
            return;
        }

        //Check the database for any duplicate usernames and delete them.
        deleteDuplicates();
    }

    /**
     * Collect any previous data entries for the same email and delete to avoid duplicate discovered leaders.
     */
    private void deleteDuplicates() {
        Query query = db.collection("addresses").document(publicIP)
                .collection("Leaders").whereEqualTo("Email", manualConnectionDetails.get("Email"));

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Documents found.");

                for (DocumentSnapshot document : task.getResult()) {
                    db.collection("addresses").document(publicIP)
                            .collection("Leaders").document(document.getId()).delete();
                }

                //Register the new address.
                setNewAddress();
            } else {
                Log.d(TAG, "Error getting documents: ", task.getException());
            }
        });
    }

    /**
     * Register the login details with PublicIP address as the documentID
     * create a new document of the publicIP address if does not exist
     * create a new collection, Leaders if it does not exist in case multiple Leaders are online
     * create a new document of the ServerIP address with username, ServerIP, PublicIP and timestamp fields
     */
    private void setNewAddress() {
        db.collection("addresses").document(publicIP)
                .collection("Leaders").document(serverIP).set(manualConnectionDetails, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));
    }

    /**
     * Attempt to get the public IP address of the current device (router address)
     */
    private void waitForPublic() {
        Thread getPublic = new Thread(() -> {
            publicIP = getPublicIP();
            FirebaseService.setPublicIP(publicIP);
            manualConnectionDetails.put("PublicIP", publicIP); //store as reference for the clean up server
        });

        getPublic.start();

        try {
            getPublic.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Call https://api.ipify.org to simply get the public IP address
    private String getPublicIP() {
        String publicIPlocal = "";
        try  {
            java.util.Scanner s = new java.util.Scanner(
                    new java.net.URL(
                            "https://api.ipify.org")
                            .openStream(), "UTF-8")
                    .useDelimiter("\\A");
            publicIPlocal = s.next();
            Log.d(TAG, "getPublicIP: got public");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Log.d(TAG, "getPublicIP: didn't get public");
        }

        return publicIPlocal;
    }

    /**
     * Set the server IP
     * @param ip A String representing the public IP address the guide is connected to.
     */
    public void setServerIP(String ip) {
        this.serverIP = ip;
    }

    /**
     * Get the current server IP.
     * @return A String representing the public IP address the guide is connected to.
     */
    public String getServerIP() {
        return this.serverIP;
    }

    /**
     * Call the firebase to get the current version of the application that is on the play store.
     */
    public void checkCurrentVersion() {
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
    private void compareVersions(String productionVersion) {
        String runningVersion = "";

        Log.d(TAG, "Production version: " + productionVersion);

        if (!productionVersion.equals("")) {
            try {
                runningVersion = main.context.getPackageManager()
                        .getPackageInfo(main.context.getPackageName(), 0)
                        .versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Running version: " + runningVersion);

            if(!runningVersion.equals(productionVersion)) {
                main.getDialogManager().showWarningDialog("Update",
                        "There is a new version of LeadMe \n" +
                                "available on the play store.");
            }
        }
    }
}
