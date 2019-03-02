package com.example.jaff.reiziger;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.Toast;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.facebook.FacebookSdk;

import java.util.Objects;

/***CONTEXT************************************************************************************************
 *   Connexion -->
 *   class allowing the display of the connection means to the first download for data recording,
 *   management of the terms and conditions of use and loading of the Maps API for the next activity.
 ***************************************************************************************************/

public class Connexion extends AppCompatActivity{
    private static final String TAG = "Connexion";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int RC_SIGN_IN = 1;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private CallbackManager mCallbackManager;
    CardView cardviewSignInGoogle, cardviewSignInFacebook;
    private CheckBox checkboxConditions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //FacebookSdk.sdkInitialize(this.getApplicationContext());
        requestWindowFeature(Window.FEATURE_NO_TITLE); //Fullscreen activity
        setContentView(R.layout.activity_connexion);
        Objects.requireNonNull(getSupportActionBar()).hide(); //remove title bar
        overridePendingTransition(R.anim.fade_in_longtime, R.anim.fade_out_longtime); // Transition fade

        mAuth = FirebaseAuth.getInstance();     // Initialize Firebase Auth

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() != null){
                    if(isServicesOK()) {
                        finish();
                        startActivity(new Intent(Connexion.this, MainActivity.class));
                    }
                }
            }
        };

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1011035156707-fdp1k9j2lq8v4biba7eh7eccu69mef0d.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener(){
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){
                        Toast.makeText(Connexion.this,"Error",Toast.LENGTH_LONG).show();
                    }
                }).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();

        //-----------------------------------------------------------------------------------------------------------
        // Initialize checkbox for terms and conditions
        checkboxConditions = findViewById(R.id.checkBox);

        // Initialize Google Button
        cardviewSignInGoogle = findViewById(R.id.CardViewSignInGoogle);
        cardviewSignInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkboxConditions.isChecked()){
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                } else{
                    Toast.makeText(getApplicationContext(),"Error message : Terms and condition are not accepted"
                            ,Toast.LENGTH_LONG).show();
                }
            }
        });

        // Initialize Facebook Button
        mCallbackManager = CallbackManager.Factory.create();
        cardviewSignInFacebook = findViewById(R.id.CardViewSignInFacebook);
        cardviewSignInFacebook.setVisibility(View.GONE);                            // This module is not yet validated so no display
        cardviewSignInFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkboxConditions.isChecked()){
                    /*List<String> permissionNeeds= Arrays.asList("email", "public_profile");
                    LoginManager.getInstance().logInWithReadPermissions(Connexion.this, permissionNeeds);
                    LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            Log.d(TAG, "facebook:onSuccess:" + loginResult);
                            handleFacebookAccessToken(loginResult.getAccessToken());
                        }
                        @Override
                        public void onCancel() {
                            Log.d(TAG, "facebook:onCancel");
                            Toast.makeText(getApplicationContext(),"Login Cancelled", Toast.LENGTH_LONG).show();
                        }
                        @Override
                        public void onError(FacebookException error) {
                            Log.d(TAG, "facebook:onError", error);
                            Toast.makeText(getApplicationContext(),"Login Error", Toast.LENGTH_LONG).show();
                        }
                    });*/
                } else{
                    Toast.makeText(getApplicationContext(),"Error message : Terms and condition are not accepted"
                            ,Toast.LENGTH_LONG).show();
                }
            }
        });

    }
    //--------------------------------------------------------------------------------------------------------------


    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null){
            updateUI();
        }else{
            mAuth.addAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                // ...
            }
        }else{
            //If not request code is RC_SIGN_IN it must be facebook
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithCredential:success");
                        } else {
                            // If sign in fails
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(Connexion.this,"Authentication failed",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(Connexion.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void updateUI(){
        Intent intent = new Intent(Connexion.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(Connexion.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(Connexion.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        FirebaseAuth.getInstance().signOut();
                        Connexion.this.finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }
}
