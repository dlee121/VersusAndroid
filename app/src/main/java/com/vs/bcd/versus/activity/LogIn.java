package com.vs.bcd.versus.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

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
    private Context thisContext;
    private boolean loginThreadRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        thisContext = getApplicationContext();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void logInSubmitted(View view){
        //TODO: better way to prevent multiple login submission from tapping the button rapidly multiple times?
        if(!loginThreadRunning){
            loginThreadRunning = true;
            // Initialize the Amazon Cognito credentials provider
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    this.getApplicationContext(),
                    "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                    Regions.US_EAST_1 // Region
            );
            AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
            mapper = new DynamoDBMapper(ddbClient);

            usernameIn = ((EditText) findViewById(R.id.editText6)).getText().toString();
            passwordIn = ((TextInputEditText) findViewById(R.id.editTextPWIN)).getText().toString();


            Runnable runnable = new Runnable() {
                public void run() {
                    user = mapper.load(User.class, usernameIn);
                    if(user == null){
                        //this username does not exist in database.
                        //TODO: popup dialog saying wrong username or password (that's the most secure thing to say as opposed to "that username doesn't exist" right?) <- or is that even a necessary precaution?
                        Log.d("LOGIN", "incorrect username: " + usernameIn);
                    }
                    else{
                        //username exists. now check password.
                        if(user.getPassword().equals(passwordIn)){
                            //password matches what we have on file for the user with the username. log the user in.
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sessionManager = new SessionManager(thisContext);
                                    sessionManager.createLoginSession(user);    //store login session data in Shared Preferences
                                    Intent intent = new Intent(thisContext, MainContainer.class);
                                    startActivity(intent);  //go on to the next activity, MainContainer
                                    overridePendingTransition(0, 0);
                                }
                            });
                        }
                        else{
                            //incorrect password
                            Log.d("LOGIN", "incorrect password");
                        }

                    }
                    loginThreadRunning = false;

                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();
        }
    }
}
