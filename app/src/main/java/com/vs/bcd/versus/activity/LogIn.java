package com.vs.bcd.versus.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Handler;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        thisActivity = this;
        loginButton = (Button)findViewById(R.id.loginbutton);
        loginPB = (ProgressBar)findViewById(R.id.indeterminate_login_pb);
        displayProgressBar(false);
    }

    public void logInSubmitted(View view){
        //TODO: better way to prevent multiple login submission from tapping the button rapidly multiple times?

        if(!loginThreadRunning){
            loginThreadRunning = true;
            displayProgressBar(true);

            // Initialize the Amazon Cognito credentials provider
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                    Regions.US_EAST_1 // Region
            );
            AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
            mapper = new DynamoDBMapper(ddbClient);

            usernameIn = ((EditText) findViewById(R.id.editText6)).getText().toString();
            passwordIn = ((TextInputEditText) findViewById(R.id.editTextPWIN)).getText().toString();

            //TODO: form validation here


            Runnable runnable = new Runnable() {
                public void run() {
                    try{
                        user = mapper.load(User.class, usernameIn);

                        if(user == null){
                            //this username does not exist in database.
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(thisActivity, "Invalid username", Toast.LENGTH_SHORT).show();
                                    displayProgressBar(false);
                                }
                            });
                        }
                        else{
                            //username exists. now check password.
                            if(user.getPassword().equals(passwordIn)){
                                //password matches what we have on file for the user with the username. log the user in.
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sessionManager = new SessionManager(thisActivity);
                                        sessionManager.createLoginSession(user);    //store login session data in Shared Preferences
                                        Intent intent = new Intent(thisActivity, MainContainer.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        loginThreadRunning = false;
                                        startActivity(intent);  //go on to the next activity, MainContainer
                                        overridePendingTransition(0, 0);
                                    }
                                });
                            }
                            else{
                                //incorrect password
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(thisActivity, "Incorrect password", Toast.LENGTH_SHORT).show();
                                        displayProgressBar(false);
                                    }
                                });

                            }

                        }

                    } catch (Throwable t) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(thisActivity, "There was a problem logging in. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                                displayProgressBar(false);
                            }
                        });
                    }
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();


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
