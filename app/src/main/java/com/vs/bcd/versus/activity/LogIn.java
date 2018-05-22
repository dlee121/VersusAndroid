package com.vs.bcd.versus.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Handler;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

import java.util.HashMap;
import java.util.Map;

public class LogIn extends AppCompatActivity {

    private DynamoDBMapper mapper;
    private SessionManager sessionManager;
    String usernameIn;
    private User user;
    String passwordIn;
    private boolean loginThreadRunning = false;
    private Activity thisActivity;
    private Button loginButton;
    private ProgressBar loginPB;
    private EditText usernameET;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");
        thisActivity = this;
        mFirebaseAuth = FirebaseAuth.getInstance();
        loginButton = (Button)findViewById(R.id.loginbutton);
        loginPB = (ProgressBar)findViewById(R.id.indeterminate_login_pb);
        usernameET = (EditText)findViewById(R.id.editText6);
        usernameET.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        displayProgressBar(false);
    }

    public void logInSubmitted(View view){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow( getCurrentFocus().getWindowToken(), 0);
        //TODO: better way to prevent multiple login submission from tapping the button rapidly multiple times?

        if(!loginThreadRunning){
            loginThreadRunning = true;
            displayProgressBar(true);

            // Initialize the Amazon Cognito credentials provider
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                    Regions.US_EAST_1 // Region
            );
            AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
            mapper = new DynamoDBMapper(ddbClient);

            usernameIn = ((EditText) findViewById(R.id.editText6)).getText().toString();
            passwordIn = ((TextInputEditText) findViewById(R.id.editTextPWIN)).getText().toString();

            //TODO: form validation here

            mFirebaseAuth.signInWithEmailAndPassword(usernameIn + "@versusbcd.com", passwordIn)
                    .addOnCompleteListener(LogIn.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("authsuccess", "aye success");
                                FirebaseUser firebaseUser= mFirebaseAuth.getCurrentUser();

                                if(firebaseUser != null){

                                    firebaseUser.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                                        @Override
                                        public void onSuccess(GetTokenResult getTokenResult) {
                                            Map<String, String> logins = new HashMap<>();
                                            final String token = getTokenResult.getToken();
                                            logins.put("securetoken.google.com/bcd-versus", token);
                                            credentialsProvider.setLogins(logins);

                                            Runnable runnable = new Runnable() {
                                                public void run() {
                                                    credentialsProvider.refresh();
                                                    user = mapper.load(User.class, usernameIn); //TODO: replace with ES user GET

                                                    thisActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sessionManager = new SessionManager(thisActivity);
                                                            sessionManager.createLoginSession(user);    //store login session data in Shared Preferences

                                                            Intent intent = new Intent(thisActivity, MainContainer.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            intent.putExtra("oitk", token);
                                                            loginThreadRunning = false;
                                                            startActivity(intent);  //go on to the next activity, MainContainer
                                                            overridePendingTransition(0, 0);
                                                        }
                                                    });
                                                }
                                            };
                                            Thread mythread = new Thread(runnable);
                                            mythread.start();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(thisActivity, "There was a problem logging in. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                else{
                                    Toast.makeText(thisActivity, "There was a problem logging in. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                displayProgressBar(false);
                                loginThreadRunning = false;
                                Toast.makeText(LogIn.this, "Check your username or password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });






        }
    }

    public void displayProgressBar(boolean display){
        if(display){
            loginButton.setEnabled(false);
            loginButton.setVisibility(View.INVISIBLE);
            loginPB.setEnabled(true);
            loginPB.setVisibility(View.VISIBLE);
        }
        else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loginThreadRunning = false;
                    loginButton.setEnabled(true);
                    loginButton.setVisibility(View.VISIBLE);
                    loginPB.setEnabled(false);
                    loginPB.setVisibility(View.INVISIBLE);
                }
            });
        }
    }
}
