package com.lumination.leadme.managers;

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

import com.alimuzaffar.lib.pin.PinEntryEditText;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.himanshurawat.hasher.HashType;
import com.himanshurawat.hasher.Hasher;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Specific class for handling the authentication of users. Manages login/logout functions through
 * the firebase database as well as third party methods.
 * */
public class AuthenticationManager {
    private final String TAG = "AuthenticationManager";

    private final LeadMeMain main;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser = null;
    private GoogleSignInClient mGoogleSignInClient;
    private String regoCode = "";
    private boolean hasScrolled = false;
    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    /**
     * A basic constructor that sets up the dialog objects.
     * @param main A reference to the LeadMe main class.
     */
    public AuthenticationManager(LeadMeMain main) {
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
                send.setText(R.string.done);
                send.setOnClickListener(v17 -> forgottenDialog.dismiss());
                cancel.setOnClickListener(v16 -> {
                    forgotten.setVisibility(View.VISIBLE);
                    email_sent.setVisibility(View.GONE);
                    send.setText(R.string.send);
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
                            send.setText(R.string.done);
                            send.setOnClickListener(v14 -> forgottenDialog.dismiss());
                            cancel.setOnClickListener(v13 -> {
                                forgotten.setVisibility(View.VISIBLE);
                                email_sent.setVisibility(View.GONE);
                                send.setText(R.string.send);
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

    //REGISTERING USERS / MANAGING LOGIN FUNCTIONS
    private String loginEmail = "";
    private String loginPassword = "";
    private String Name = "";
    private Boolean Marketing = false;

    /**
     * Build the sign up page for a new user or a user that is partially through a registration,
     * sign in verification is set to false by default.
     * @param page An integer representing which stage the user is up to.
     **/
    public void buildloginsignup(int page) {
        buildloginsignup(page, false);
    }

    /**
     * Build the sign up page for a new user or a user that is partially through a registration.
     * @param page An integer representing which stage the user is up to.
     * @param signinVerif A boolean representing whether or not the sign in has been verified already.
     */
    public void buildloginsignup(int page, boolean signinVerif) {
        showSystemUI();
        View loginView = View.inflate(main, R.layout.b__login_signup, null);
        LinearLayout[] layoutPages = {loginView.findViewById(R.id.rego_code), loginView.findViewById(R.id.terms_of_use), loginView.findViewById(R.id.signup_page)
                , loginView.findViewById(R.id.email_verification)
                , loginView.findViewById(R.id.set_pin), loginView.findViewById(R.id.account_created)};
        Button next = loginView.findViewById(R.id.signup_enter);
        Button back = loginView.findViewById(R.id.signup_back);
        ProgressBar progressBar = loginView.findViewById(R.id.signup_indeterminate);
        progressBar.setVisibility(View.GONE);
        TextView support = loginView.findViewById(R.id.rego_contact_support);
        
        support.setOnClickListener(v -> {
            String[] email = {"dev@lumination.com.au"};
            //TODO perhaps change this later?
            main.composeEmail(email,"LeadMe Support: Signup Issue");
        });

        //page 0
        EditText loginCode = loginView.findViewById(R.id.rego_code_box);
        TextView regoLost = loginView.findViewById(R.id.rego_lost_code);
        regoLost.setOnClickListener(v -> {
            String[] email = {"dev@lumination.com.au"};
            main.composeEmail(email,"LeadMe Support: Signup Code Request");
        });
        TextView regoError = loginView.findViewById(R.id.rego_code_error);
        //page 2
        TextView signupError = loginView.findViewById(R.id.signup_error);
        EditText signupName = loginView.findViewById(R.id.signup_name);
        EditText signupEmail = loginView.findViewById(R.id.signup_email);
        EditText signupPass = loginView.findViewById(R.id.signup_password);
        EditText signupConPass = loginView.findViewById(R.id.signup_confirmpass);
        CheckBox marketingCheck = loginView.findViewById(R.id.signup_marketing);
        signupEmail.setText(loginEmail);
        signupPass.setText(loginPassword);
        signupName.setText(Name);
        marketingCheck.setChecked(Marketing);
        //page 1
        TextView errorText = loginView.findViewById(R.id.tou_readtext);
        ScrollView touScroll = loginView.findViewById(R.id.tou_scrollView);
        TextView terms = loginView.findViewById(R.id.tou_terms);
        CheckBox touAgree = loginView.findViewById(R.id.tou_check);
        //page 3
        VideoView animation = loginView.findViewById(R.id.email_animation);
        //page 4
        TextView pinError = loginView.findViewById(R.id.pin_error_text);
        ImageView pinErrorImg = loginView.findViewById(R.id.pin_error_image);
        //page 5
        TextView accountText = loginView.findViewById(R.id.account_createdtext);
        for (int i = 0; i < layoutPages.length; i++) {
            if (i != page) {
                layoutPages[i].setVisibility(View.GONE);
            } else {
                layoutPages[i].setVisibility(View.VISIBLE);
            }
        }

        main.setContentView(loginView);

        loginView.setOnClickListener(v -> main.getDialogManager().hideSoftKeyboard(v));

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
                hideSystemUI();
                main.getHandler().postDelayed(this::hideSystemUI, 500);
                errorText.setVisibility(View.GONE);
                hasScrolled = true;
                WebView TOF = loginView.findViewById(R.id.tof_webview);
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
                main.getHandler().postDelayed(this::hideSystemUI, 500);

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

                FirebaseAuth.AuthStateListener mAuthListener = firebaseAuth -> {
                    //Setup the firebase again, if this doesn't work then there is no internet connection?
                    if(mAuth == null) {
                        setupFirebase();
                        return;
                    }

                    //TODO crash occurs here - getCurrentUser() or mAuth or isEmailVerified is null?
                    if (!Objects.requireNonNull(mAuth.getCurrentUser()).isEmailVerified()) {
                        Log.d(TAG, "buildloginsignup: email verification sent");
                        mAuth.getCurrentUser().sendEmailVerification().addOnSuccessListener(aVoid ->
                                scheduledExecutorService.scheduleAtFixedRate(() -> mAuth.addAuthStateListener(firebaseAuth1 -> {
                                    Log.d(TAG, "run: checking user verification");
                                    if (!mAuth.getCurrentUser().isEmailVerified()) {
                                        mAuth.getCurrentUser().reload();
                                    } else {
                                        currentUser = mAuth.getCurrentUser();
                                        scheduledExecutorService.shutdown();
                                        main.runOnUiThread(() -> buildloginsignup(4));
                                    }
                                }), 100, 100, TimeUnit.MILLISECONDS));
                    } else {
                        Log.d(TAG, "buildloginsignup: user is already verified");
                    }
                };

                mAuth.addAuthStateListener(mAuthListener);
                Log.d(TAG, "buildloginsignup: and here");

                /*If not they can exit the app and verify whenever they want. Otherwise open to here
                *until the verify.
                * */
                back.setOnClickListener(v -> {
                    scheduledExecutorService.shutdown();
                    mAuth.removeAuthStateListener(mAuthListener);
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
                        if(pinEntry.getText().length() != 4) {
                            pinError.setText("Pin is not long enough");
                            pinError.setTextColor(main.getColor(R.color.leadme_red));
                            pinErrorImg.setImageResource(R.drawable.alert_error);
                        } else {
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
                        }
                    } else {
                        pinError.setText("The pin's do not match");
                        pinError.setTextColor(main.getColor(R.color.leadme_red));
                        pinErrorImg.setImageResource(R.drawable.alert_error);
                        progressBar.setVisibility(View.GONE);
                    }
                });

                /*Return to the main screen, email is verified just have to set code
                *before first login. Have to logout so the pin entry does no come up upon retry.
                * */
                back.setOnClickListener(v -> {
                    logoutAction();
                    main.animatorAsContentView();
                    hideSystemUI();
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
     * Handles sign in requests for the google client sign in.
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
    public void FirebaseEmailSignIn(String email, String password, TextView errorText) {
        Log.d(TAG, "FirebaseEmailSignIn: ");

        if (email != null && password != null) {
            if (email.length() > 0 && password.length() > 0) {
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(main, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        currentUser = task.getResult().getUser();

                        if (!currentUser.isEmailVerified()) {
                            //not clearing
                            main.cleanDialogs();
                            buildloginsignup(3, false);
                        } else {
                            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                                    .addOnCompleteListener((OnCompleteListener<DocumentSnapshot>) task1 -> {
                                Log.d(TAG, "onComplete: ");
                                main.setIndeterminateBar(View.GONE);

                                if (task1.isSuccessful()) {
                                    main.setIndeterminateBar(View.GONE);
                                    if (task1.getResult().get("pin") == null ) {
                                        main.cleanDialogs();
                                        buildloginsignup(4, false);
                                        return;
                                    }

                                    main.setUserName((String) task1.getResult().get("name"), false);
                                    Log.d(TAG, "onComplete: name found: " + task1.getResult().get("name"));
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
     * Get the email of the account holder of the currently logged in user.
     * @return A String representing the current user's email.
     */
    public String getCurrentAuthEmail() {
        return mAuth.getCurrentUser().getEmail();
    }

    /**
     * Get this classes instance of the google sign in client.
     * @return An instance of the google sign in client.
     */
    public GoogleSignInClient getGoogleSignInClient() {
        return this.mGoogleSignInClient;
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
