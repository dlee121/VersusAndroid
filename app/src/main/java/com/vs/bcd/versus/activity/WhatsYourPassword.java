package com.vs.bcd.versus.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.PhoneOrEmail;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

import static com.vs.bcd.versus.R.id.editText;
import static com.vs.bcd.versus.R.id.toolbar;

public class WhatsYourPassword extends AppCompatActivity {

    public static final String EXTRA_WYP = "com.example.myfirstapp.WYU";
    private String fullnameBdayUsernamePassword;
    private DynamoDBMapper mapper;
    private SessionManager sessionManager;
    private Button signupButton;
    private ProgressBar signupPB;
    private Activity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_your_password);
        sessionManager = new SessionManager(this);

        Intent intent = getIntent();
        fullnameBdayUsernamePassword = intent.getStringExtra(WhatsYourUsername.EXTRA_WYU);

        signupButton = (Button)findViewById(R.id.signupsubmit);
        signupPB = (ProgressBar)findViewById(R.id.signuppb);

        thisActivity = this;

        displayProgressBar(false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);
    }


    public void SignUpSubmit(View view){
        //TODO: validate forms, and hash/salt password

        TextInputEditText editText = (TextInputEditText) findViewById(R.id.editText5);
        fullnameBdayUsernamePassword = fullnameBdayUsernamePassword + "%" + editText.getText().toString(); //fullname%bday-bmonth-byear%username%password

        displayProgressBar(true);

        //validate, write to db (which completes registration), write session data to SharedPref (same as login) then move on to MainContainer

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);

        final User newUser = new User(fullnameBdayUsernamePassword);

        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    mapper.save(newUser);
                    //TODO: Ensure that lines below are called only if mapper.save(newUser) is successful (in other words, make sure thread waits until mapper.save(newUser) finishes its job successfully, otherwise throwing exception to exit thread before calling below lines to login the new user).
                    //TODO: So far this seems to be the case, as any exception thrown is caught and lines below don't get executed because we exit the thread when exception is thrown.
                    //TODO: Cuz if there is a case where lines below are executed despite mapper.save failure, that would probably cause some bugs.
                    //TODO: maybe do some synchronization/thread magic just to be on the safe side. We're good for now though.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sessionManager.createLoginSession(newUser);
                            Intent intent = new Intent(thisActivity, MainContainer.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   //clears back stack for navigation
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        }
                    });
                }
                catch (Throwable t){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayProgressBar(false);
                            Toast.makeText(thisActivity, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void displayProgressBar(boolean display){
        if(display){
            signupButton.setEnabled(false);
            signupButton.setVisibility(View.INVISIBLE);
            signupPB.setEnabled(true);
            signupPB.setVisibility(View.VISIBLE);
        }
        else{
            signupButton.setEnabled(true);
            signupButton.setVisibility(View.VISIBLE);
            signupPB.setEnabled(false);
            signupPB.setVisibility(View.INVISIBLE);
        }
    }

}

