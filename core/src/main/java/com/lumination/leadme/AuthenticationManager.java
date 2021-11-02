package com.lumination.leadme;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Specific class for handling the authentication of users. Manages login/logout functions through
 * the firebase database as well as third party methods.
 * */
public class AuthenticationManager {
    private final String TAG = "AuthenticationManager";

    private LeadMeMain main;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser = null;
    private ListenerRegistration manualUserListener;
    private GoogleSignInClient mGoogleSignInClient;
    private String regoCode = "";
    private boolean hasScrolled = false;

    //For Manual connections
    private final int leaderTimestampUpdate = 15; //update the leaders timestamp on firebase (mins)
    private final int inactiveUser = 30; //cut off for hiding inactive leaders (mins)
    private final int waitForGuide = 10000; //how long to wait before peer re-querys firestore
    private String serverIP="";
    private String publicIP;
    private HashMap<String, Object> manualConnectionDetails = new HashMap<String, Object>();

    /**
     * A basic constructor that sets up the dialog objects.
     * @param main A reference to the LeadMe main class.
     */
    AuthenticationManager(LeadMeMain main) {
        this.main = main;
        setupThirdParty();
    }

    private void setupThirdParty() {
        setupGoogleSignIn();
        setupFirebase();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(main.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(main, gso);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    public void logoutAction() {
        mAuth.signOut();
        currentUser = mAuth.getCurrentUser();
    }

    /**
     * Show/send a forgotten password to a user.
     * @param previous An AlertDialog representing the previous screen.
     * */
    public void showForgottenPassword(AlertDialog previous) {
        boolean prevShow=previous.isShowing();
        previous.dismiss();
        View forgotten_view = View.inflate(main, R.layout.c__forgot_password, null);
        AlertDialog forgottenDialog = new AlertDialog.Builder(main)
                .setView(forgotten_view)
                .create();
        LinearLayout forgotten = forgotten_view.findViewById(R.id.forgot_layout);
        LinearLayout email_sent = forgotten_view.findViewById(R.id.email_sent);
        EditText email = forgotten_view.findViewById(R.id.forgot_email);
        Button send = forgotten_view.findViewById(R.id.forgot_enter);
        Button cancel = forgotten_view.findViewById(R.id.forgot_back);
        forgotten.setVisibility(View.VISIBLE);
        forgottenDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        forgottenDialog.show();
        send.setOnClickListener(v -> {
            if (email.getText().toString().length() > 0) {
                mAuth.sendPasswordResetEmail(email.getText().toString())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Email sent.");


                            }
                        });
                forgotten.setVisibility(View.GONE);
                email_sent.setVisibility(View.VISIBLE);
                send.setText("Done");
                send.setOnClickListener(v17 -> forgottenDialog.dismiss());
                cancel.setOnClickListener(v16 -> {
                    forgotten.setVisibility(View.VISIBLE);
                    email_sent.setVisibility(View.GONE);
                    send.setText("Send");
                    send.setOnClickListener(v15 -> {
                        if (email.getText().toString().length() > 0) {
                            mAuth.sendPasswordResetEmail(email.getText().toString())
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Email sent.");


                                        }
                                    });
                            forgotten.setVisibility(View.GONE);
                            email_sent.setVisibility(View.VISIBLE);
                            send.setText("Done");
                            send.setOnClickListener(v14 -> forgottenDialog.dismiss());
                            cancel.setOnClickListener(v13 -> {
                                forgotten.setVisibility(View.VISIBLE);
                                email_sent.setVisibility(View.GONE);
                                send.setText("Send");
                                cancel.setOnClickListener(v12 -> {
                                    forgottenDialog.dismiss();
                                    previous.show();
                                });
                            });
                        } else {
                            Toast toast = Toast.makeText(main.getApplicationContext(), "Please enter your email first", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });
                    cancel.setOnClickListener(v1 -> {
                        forgottenDialog.dismiss();
                        previous.show();
                    });
                });
            } else {
                Toast toast = Toast.makeText(main.getApplicationContext(), "Please enter your email first", Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        cancel.setOnClickListener(v -> {
            forgottenDialog.dismiss();
            if(prevShow) {
                previous.show();
            }
        });
    }

    //THE SECTION BELOW IS FOR MANUAL CONNECTIONS ONLY
    //MANUAL CONNECTION FOR LEARNERS START
    /*
     * A call to firebase to retrieve any leaders registered to a peer's publicIP address. If there
     * are no records the function will repeat every x seconds where x is an integer (waitForGuide).
     * Will only return leaders who have been active within a certain period of time (inactiveUser).
     * If the public IP address is present but the certain leader hasn't logged on yet, a listener is
     * attached to wait for any chances in the firebase.
     * */
    public void retrieveLeaders() {
        waitForPublic();
        Log.d(TAG, "retrieveLeaders: " + publicIP);
        if (publicIP == null || publicIP.length() == 0) {
            return;
        }

        CollectionReference collRef = db.collection("addresses").document(publicIP).collection("Leaders");

        collRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    //if no one has registered on the public IP yet, wait sometime and try again.
                    try {
                        if (Objects.requireNonNull(task.getResult()).size() == 0) {
                            main.scheduledExecutorService.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    //runOnUiThread(() -> {
                                        retrieveLeaders();
                                    //});
                                }
                            }, waitForGuide, TimeUnit.MILLISECONDS);
                        } else {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                Date leaderTimeStamp = Objects.requireNonNull(document.getTimestamp("TimeStamp")).toDate();

                                if (checkTimeDifference(leaderTimeStamp) >= inactiveUser) {
                                    return;
                                }

                                //add to the leaders list
                                //runOnUiThread(() -> {
                                    main.manuallyConnectLeader(document.get("Username").toString(), document.get("ServerIP").toString());
                                //});
                            }

                            //add listeners to track if leader hasn't logged in but publicIP exists (multiple leaders on network)
                            trackCollection(collRef);
                        }
                    }catch(NullPointerException e){
                        Log.d(TAG, "onComplete: "+e);
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });
    }

    //add a listener to the Leader collection to wait for log in
    private void trackCollection(CollectionReference collRef) {
        //checkArray(document);
        //adapter.notifyDataSetChanged();
        if(manualUserListener!=null){
            manualUserListener.remove();
        }

        if(!main.getNearbyManager().isConnectedAsFollower()) {
            Log.d(TAG, "trackCollection: listener added");
            manualUserListener = collRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    Log.d(TAG, "onEvent: ip listener fired");
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            if (document.get("Username") != null) {
                                //checkArray(document);
                                //runOnUiThread(() -> {
                                    main.manuallyConnectLeader(document.get("Username").toString(), document.get("ServerIP").toString());
                                //});
                            }
                        }
                        //adapter.notifyDataSetChanged();
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                }
            });
        }
    }

    /**
     * Removes the user entry listener attached to the firebase collection.
     * */
    public void removeUserListener() {
        if(manualUserListener!=null){
            Log.d(TAG, "loginAction: listener removed");
            manualUserListener.remove();
            manualUserListener=null;
        }
    }

    /*calculate the difference between the firebase timestamp and the current time
    return the minutes, can also work out other units if necessary.*/
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
     * by discovery.
     * @param ipAddress A String representing the local IPAddress of the Guide's device.
     */
    public void createManualConnection(String ipAddress) {
        //initiate public ip track and firebase
        waitForPublic();

        serverIP = ipAddress;
        manualConnectionDetails.put("Username", getCurrentAuthUserName());
        manualConnectionDetails.put("ServerIP", serverIP);
        manualConnectionDetails.put("TimeStamp", FieldValue.serverTimestamp());

        //update firebase and start a continuous timer for updating the timestamp
        Timer timestamp = new Timer();
        TimerTask updateTimestamp = new TimerTask() {
            @Override
            public void run() {
                updateAddress();
            }
        };

        timestamp.scheduleAtFixedRate(updateTimestamp, 0L, leaderTimestampUpdate * (60 * 1000));
    }

    /*register the login details with PublicIP address as the documentID
    create a new document of the publicIP address if does not exist
    create a new collection, Leaders if it does not exist in case multiple Leaders are online
    create a new document of the ServerIP address with username, ServerIP, PublicIP and timestamp fields*/
    private void updateAddress() {
        if(publicIP.length() == 0) {
            return;
        }

        db.collection("addresses").document(publicIP)
            .collection("Leaders").document(serverIP).set(manualConnectionDetails, SetOptions.merge())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }

    //Attempt to get the public IP address of the current device (router address)
    private void waitForPublic() {
        Thread getPublic = new Thread(() -> {
            publicIP = getPublicIP();
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

    //REGISTERING USERS / MANAGING LOGIN FUNCTIONS
    private String loginEmail = "";
    private String loginPassword = "";
    private String Name = "";
    private Boolean Marketing =false;

    public void buildloginsignup(int page) {
        buildloginsignup(page, false);
    }

    public void buildloginsignup(int page, boolean signinVerif) {
        showSystemUI();
        View Login = View.inflate(main, R.layout.b__login_signup, null);
        LinearLayout[] layoutPages = {Login.findViewById(R.id.rego_code), Login.findViewById(R.id.terms_of_use), Login.findViewById(R.id.signup_page)
                , Login.findViewById(R.id.email_verification)
                , Login.findViewById(R.id.set_pin), Login.findViewById(R.id.account_created)};
        Button next = Login.findViewById(R.id.signup_enter);
        Button back = Login.findViewById(R.id.signup_back);
        ProgressBar progressBar = Login.findViewById(R.id.signup_indeterminate);
        progressBar.setVisibility(View.GONE);
        TextView support = Login.findViewById(R.id.rego_contact_support);
        
        support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email[] = {"dev@lumination.com.au"};
                //TODO perhaps change this later?
                main.composeEmail(email,"LeadMe Support: Signup Issue");
            }
        });

        //page 0
        EditText loginCode = Login.findViewById(R.id.rego_code_box);
        TextView regoLost = Login.findViewById(R.id.rego_lost_code);
        regoLost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email[] = {"dev@lumination.com.au"};
                main.composeEmail(email,"LeadMe Support: Signup Code Request");
            }
        });
        TextView regoError = Login.findViewById(R.id.rego_code_error);
        //page 2
        TextView signupError = Login.findViewById(R.id.signup_error);
        EditText signupName = Login.findViewById(R.id.signup_name);
        EditText signupEmail = Login.findViewById(R.id.signup_email);
        EditText signupPass = Login.findViewById(R.id.signup_password);
        EditText signupConPass = Login.findViewById(R.id.signup_confirmpass);
        CheckBox marketingCheck = Login.findViewById(R.id.signup_marketing);
        signupEmail.setText(loginEmail);
        signupPass.setText(loginPassword);
        signupName.setText(Name);
        marketingCheck.setChecked(Marketing);
        //page 1
        TextView errorText = Login.findViewById(R.id.tou_readtext);
        ScrollView touScroll = Login.findViewById(R.id.tou_scrollView);
        TextView terms = Login.findViewById(R.id.tou_terms);
        CheckBox touAgree = Login.findViewById(R.id.tou_check);
        //page 3
        VideoView animation = Login.findViewById(R.id.email_animation);
        //page 4
        TextView pinError = Login.findViewById(R.id.pin_error_text);
        ImageView pinErrorImg = Login.findViewById(R.id.pin_error_image);
        //page 5
        TextView accountText = Login.findViewById(R.id.account_createdtext);
        for (int i = 0; i < layoutPages.length; i++) {
            if (i != page) {
                layoutPages[i].setVisibility(View.GONE);
            } else {
                layoutPages[i].setVisibility(View.VISIBLE);
            }
        }

        main.setContentView(Login);

        switch (page) {
            case 0:
                showSystemUI();
                regoError.setVisibility(View.GONE);
                //loginCode.requestFocus();
                loginCode.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        showSystemUI();
                    }
                });
                loginCode.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        showSystemUI();
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        Log.d(TAG, "onTextChanged: " + count);
                        if (s.length() == 6) {
                            closeKeyboard();
                        }
//                        else if(s.length()>6){
//                            loginCode.setText(s.subSequence(0,7));
//                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                next.setOnClickListener(v -> {
                    if (loginCode.getText().length() == 6) {
                        main.setProgressSpinner(3000, progressBar);
                        db.collection("signin_codes").document(loginCode.getText().toString())
                                .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "buildloginsignup: database accessed");
                                if (task.getResult().exists()) {
                                    //todo add email under signup code
//                                        if(task.getResult().get)
                                    regoCode = loginCode.getText().toString();
                                    buildloginsignup(1);
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    regoError.setText("I'm sorry that code doesn't exist.");
                                    regoError.setVisibility(View.VISIBLE);
                                }
                            } else {
                                Log.d(TAG, "buildloginsignup: unable to access database");
                            }
                        });
                    } else {
                        regoError.setVisibility(View.VISIBLE);
                        regoError.setText("Please check you have entered the code correctly.");
                    }
                });

                back.setOnClickListener(v -> {
                    main.animatorAsContentView();
                    hideSystemUI();
                });
                break;

            case 1:
                //TODO does not display the TOF on the first run through? only when clicking back button...
                hideSystemUI();
                main.getHandler().postDelayed(() -> hideSystemUI(), 500);
                errorText.setVisibility(View.GONE);
                hasScrolled = true;
                WebView TOF = Login.findViewById(R.id.tof_webview);
                TOF.getSettings().setJavaScriptEnabled(true);
                String pdf = "https://github.com/LuminationDev/public/raw/main/LeadMeEdu-TermsAndConditions.pdf";
                TOF.loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=" + pdf);

                touAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked && !hasScrolled) {
                        touAgree.setChecked(false);
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText("Please read all of the terms of use");
                    }
                });

                next.setOnClickListener(v -> {
                    if (touAgree.isChecked()) {
                        buildloginsignup(2);
                    } else {
                        errorText.setVisibility(View.VISIBLE);
                    }
                });

                back.setOnClickListener(v -> buildloginsignup(0));
                break;

            case 2:
                showSystemUI();
                signupError.setVisibility(View.GONE);
                marketingCheck.setOnCheckedChangeListener((buttonView, isChecked) -> closeKeyboard());
                next.setOnClickListener(v -> {
                    if (signupName.getText().toString().length() == 0) {
                        signupError.setText("Please enter a name");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (signupEmail.getText().toString().length() == 0) {
                        signupError.setText("Please enter an email");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (signupPass.getText().toString().length() == 0) {
                        signupError.setText("Please enter a password");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (signupPass.getText().toString().equals(signupConPass.getText().toString())) {
                        signupError.setVisibility(View.GONE);
                        main.setProgressSpinner(3000, progressBar);
                        loginEmail = signupEmail.getText().toString();
                        loginPassword = signupPass.getText().toString();
                        Name = signupName.getText().toString();
                        Marketing = marketingCheck.isChecked();
                        FirebaseEmailSignUp(signupEmail.getText().toString(), signupPass.getText().toString(), signupName.getText().toString(), marketingCheck.isChecked(), regoCode, signupError, progressBar);
                        hideSystemUI();
                    } else {
                        signupError.setText("Passwords do not match");
                        signupError.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                });

                back.setOnClickListener(v -> {
                    buildloginsignup(0);
                    hideSystemUI();
                });
                break;

            case 3:
                main.getHandler().postDelayed(() -> hideSystemUI(), 500);

                next.setVisibility(View.GONE);

                Uri uri = Uri.parse("android.resource://" + main.getPackageName() + "/" + R.raw.email_sent);
                animation.setVideoURI(uri);
                animation.setBackgroundColor(Color.WHITE);
                Log.d(TAG, "buildloginsignup: here");

                animation.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    animation.start();
                    main.getHandler().postDelayed(() -> animation.setBackgroundColor(Color.TRANSPARENT), 100);
                });

                FirebaseAuth.AuthStateListener mAuthListener= new FirebaseAuth.AuthStateListener() {
                    @Override
                    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                        if (!mAuth.getCurrentUser().isEmailVerified()) {
                            Log.d(TAG, "buildloginsignup: email verification sent");
                            mAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                //TODO error occurs if the user presses back before they verify email
                                @Override
                                public void onSuccess(Void aVoid) {
                                    main.scheduledExecutorService.scheduleAtFixedRate(() -> mAuth.addAuthStateListener(firebaseAuth1 -> {
                                        Log.d(TAG, "run: checking user verification");
                                        if (!mAuth.getCurrentUser().isEmailVerified()) {
                                            mAuth.getCurrentUser().reload();
                                        } else {
                                            currentUser = mAuth.getCurrentUser();
                                            main.scheduledExecutorService.shutdown();
                                            //TODO might need the UI thread - check later
                                            //runOnUiThread(() -> {
                                                buildloginsignup(4);
                                            //});
                                        }
                                    }), 100, 100, TimeUnit.MILLISECONDS);
                                }
                            });
                        } else {
                            Log.d(TAG, "buildloginsignup: user is already verified");
                        }
                    }
                };

                mAuth.addAuthStateListener(mAuthListener);
                Log.d(TAG, "buildloginsignup: and here");

                back.setOnClickListener(v -> {
                    main.scheduledExecutorService.shutdown();
                    mAuth.removeAuthStateListener(mAuthListener);
                    firebaseRemoveUser(mAuth.getCurrentUser());
                    mAuth.getCurrentUser().delete();
                    buildloginsignup(2);
                });
                break;

            case 4:
                next.setVisibility(View.VISIBLE);
                pinError.setText("Your email has been verified");
                pinError.setTextColor(main.getColor(R.color.leadme_black));
                pinErrorImg.setImageResource(R.drawable.icon_fav_star_check);

                if (signinVerif) {
                    main.setProgressSpinner(3000, progressBar);

                    db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                        if (task.getResult().exists()) {
                            if (task.getResult().getString("pin").length() > 0) {
                                progressBar.setVisibility(View.GONE);
                                main.setUserName(currentUser.getDisplayName(), false);
                                main.animatorAsContentView();
                            }
                        }
                    });

                }

                showSystemUI();

                next.setOnClickListener(v -> {
                    final PinEntryEditText pinEntry = (PinEntryEditText) main.findViewById(R.id.signup_pin_entry);
                    final PinEntryEditText pinEntryConfirm = (PinEntryEditText) main.findViewById(R.id.signup_pin_confirm);
                    if (pinEntry != null && pinEntryConfirm!=null && pinEntry.getText().toString().equals(pinEntryConfirm.getText().toString())) {
                        main.setProgressSpinner(3000, progressBar);

                        Map<String, Object> userDet = new HashMap<>();
                        userDet.put("pin", Hasher.Companion.hash(pinEntry.getText().toString(), HashType.SHA_256));

                        db.collection("users").document(mAuth.getCurrentUser().getUid()).update(userDet)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "onSuccess: pin saved to account"));

                        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.GONE);
                                main.setUserName(task.getResult().getString("name"), false);
                                main.animatorAsContentView();
                                loginPassword="";
                                Name="";
                                loginEmail="";
                                Marketing=false;
                            }
                        });
                    } else {
                        pinError.setText("The pin's do not match");
                        pinError.setTextColor(main.getColor(R.color.leadme_red));
                        pinErrorImg.setImageResource(R.drawable.alert_error);
                        progressBar.setVisibility(View.GONE);
                    }
                });
                break;

            default:
                break;
        }

    }

    private void firebaseRemoveUser(FirebaseUser currentUser) {
        db.collection("users").document(currentUser.getUid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    currentUser.delete();
                }
            }
        });
    }

    /**
     * Handles signin requests for the google client signin.
     * @param account An instance of a GoogleSignInAccount.
     */
    public void handleSignInResult(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser = mAuth.getCurrentUser();

                mAuth.addAuthStateListener(firebaseAuth -> {
                    if (currentUser != null) {
                        db.collection("users").document(currentUser.getUid())
                                .get().addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                if (task1.getResult().exists()) {
                                    Log.d(TAG, "handleSignInResult: user found");
                                    main.setUserName(account.getGivenName(), false);

                                } else {
                                    Log.d(TAG, "handleSignInResult: new user");

                                    Map<String, Object> userDet = new HashMap<>();
                                    userDet.put("name", account.getGivenName() + " " + account.getFamilyName());
                                    userDet.put("email", currentUser.getEmail());
                                    db.collection("users").document(currentUser.getUid()).set(userDet)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "handleSignInResult: new user created");
                                                main.setUserName(account.getGivenName(), false);
                                                hideSystemUI();
                                            })
                                            .addOnFailureListener(e -> Log.d(TAG, "handleSignInResult: failed to create new user please check internet"));

                                }
                            }
                        });
                    } else {
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        main.startActivityForResult(signInIntent, main.RC_SIGN_IN);
                    }
                });
            } else {
                Log.d(TAG, "handleSignInResult: failed to sign in");
            }
        });

    }

    //Sign up using an Email address and collected details.
    private void FirebaseEmailSignUp(String email, String password, String name, boolean marketing, String regoCode, TextView errorText, ProgressBar progressBar) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(main, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");

                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        currentUser = task.getResult().getUser();
                        currentUser.updateProfile(profileUpdates);

                        Map<String, Object> userDet = new HashMap<>();
                        userDet.put("name", name);
                        userDet.put("email", email);
                        userDet.put("marketing", marketing);
                        userDet.put("rego_code", regoCode);

                        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task1 -> {
                            if (task1.getResult().exists()) {
                                Log.d(TAG, "user data exists but user is deleted, updating user info");
                            }

                            db.collection("users").document(mAuth.getCurrentUser().getUid()).set(userDet)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "buildloginsignup: new user created");
                                    })
                                    .addOnFailureListener(e -> {
                                        progressBar.setVisibility(View.GONE);
                                        errorText.setVisibility(View.VISIBLE);
                                        errorText.setText("Error failed to save account details");
                                        Log.d(TAG, "buildloginsignup: failed to create new user please check internet");
                                    });
                        });

                        buildloginsignup(3);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        progressBar.setVisibility(View.GONE);
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText(task.getException().getMessage());
                    }
                });
    }

    /**
     * Query firebase and compare the user details with the supplied details.
     * @param email A String representing the email of the user attempting to sign in.
     * @param password A String representing the password of the user attempting to sign in.
     * @param errorText A TextView that can be populate with any error messages that occur.
     */
    public void FirebaseEmailSignIn(String email, String password, TextView errorText ) {
        Log.d(TAG, "FirebaseEmailSignIn: ");

        if (email != null && password != null) {
            if (email.length() > 0 && password.length() > 0) {
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(main, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        currentUser = task.getResult().getUser();

                        if (!currentUser.isEmailVerified()) {
                            buildloginsignup(3, true);
                        } else {
                            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                                    .addOnCompleteListener((OnCompleteListener<DocumentSnapshot>) task1 -> {
                                Log.d(TAG, "onComplete: ");
                                main.setIndeterminateBar(View.GONE);

                                if (task1.isSuccessful()) {
                                    main.setIndeterminateBar(View.GONE);
                                    if (task1.getResult().get("pin") == null ) {
                                        buildloginsignup(4, false);
                                        return;
                                    }

                                    main.setUserName((String) task1.getResult().get("name"), false);
                                    Log.d(TAG, "onComplete: name found: " + (String) task1.getResult().get("name"));
                                    main.animatorAsContentView();
                                }
                            });
                        }

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        main.setIndeterminateBar(View.GONE);
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText(task.getException().getMessage());
                    }
                });
            }
        }
    }

    //HELPER FUNCTIONS
    /**
     * Make a call to firebase to set the pin of a new account.
     * @param pin A string representing the chosen pin.
     * @return A Task<java.lang.Void> that can have an on complete listener attached to it.
     */
    public Task<java.lang.Void> setAccountPin(String pin) {
        Map<String, Object> userDet = new HashMap<>();
        userDet.put("pin", Hasher.Companion.hash(pin, HashType.SHA_256));

        return db.collection("users").document(mAuth.getCurrentUser().getUid()).update(userDet);
    }

    /**
     * Queries the firebase for the current user that is signing in, returning account information that
     * can be compared against.
     * @return A Task<com.google.firebase.firestore.DocumentSnapshot> that represents a User's account.
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getFirebaseAccount() {
        return db.collection("users").document(mAuth.getCurrentUser().getUid()).get();
    }

    /**
     * Get the account of the currently logged in user.
     * @return A FirebaseUser instance of the current user.
     */
    public FirebaseUser getCurrentAuthUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Get the name of the account holder of the currently logged in user.
     * @return A String representing the current user's name.
     */
    public String getCurrentAuthUserName() {
        return mAuth.getCurrentUser().getDisplayName();
    }

    /**
     * Get this classes instance of the google sign in client.
     * @return An instance of the google sign in client.
     */
    public GoogleSignInClient getGoogleSignInClient() {
        return this.mGoogleSignInClient;
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
     * Calls the showSystemUI from the LeadMe main activity.
     * */
    private void showSystemUI() {
        main.showSystemUI();
    }

    /**
     * Calls the hideSystemUI from the LeadMe main activity.
     * */
    private void hideSystemUI() {
        main.hideSystemUI();
    }

    /**
     * Calls the openKeyboard from the LeadMe main activity.
     * */
    private void openKeyboard() {
        main.openKeyboard();
    }

    /**
     * Calls the closeKeyboard from the LeadMe main activity
     * */
    private void closeKeyboard() {
        main.closeKeyboard();
    }
}
