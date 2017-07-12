package com.vs.bcd.versus.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

public class PhoneOrEmail extends AppCompatActivity {

    public static final String EXTRA_PE = "com.example.myfirstapp.WYU";
    private String fullnameBdayUsernamePasswordPE;
    private DynamoDBMapper mapper;
    private boolean signUpSuccessful = true;
    SessionManager sessionManager;
    private Button signupButton;
    private ProgressBar signupPB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        setContentView(R.layout.activity_phone_or_email);
        signupButton = (Button)findViewById(R.id.signupbutton);
        signupPB = (ProgressBar)findViewById(R.id.signuppb);
        Intent intent = getIntent();
        fullnameBdayUsernamePasswordPE = intent.getStringExtra(WhatsYourPassword.EXTRA_WYP);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);
    }

    public void PhoneOrEmailNext(View view){
        //TODO: implement actual phone / email verification


        //validate, write to db (which completes registration), write session data to SharedPref (same as login) then move on to MainContainer


        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);


        Intent intent = new Intent(this, MainContainer.class);
        EditText emailText = (EditText) findViewById(R.id.editText6);
        fullnameBdayUsernamePasswordPE = fullnameBdayUsernamePasswordPE + "%" + emailText.getText().toString();
        final User newUser = new User(fullnameBdayUsernamePasswordPE);
        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    mapper.save(newUser);
                }
                catch (Throwable t){
                    signUpSuccessful = false;   //if ddb save operation (sign up query) doesn't go through, indicate it through signUpSuccessful
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
        try {
            mythread.join();    //wait for registration to go through
        }
        catch(InterruptedException ex) {
            System.err.println("An InterruptedException was caught: " + ex.getMessage());
        }
        if(signUpSuccessful){
            sessionManager.createLoginSession(newUser);
            //intent.putExtra(EXTRA_PE, fullnameBdayUsernamePasswordPE);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
        else {
            displayProgressBar(false);
            Toast.makeText(this, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
        }

    }

    public void displayProgressBar(boolean display){
        if(display){
            signupButton.setEnabled(false);
            signupButton.setVisibility(View.INVISIBLE);
            signupPB.setEnabled(true);
            signupPB.setVisibility(View.VISIBLE);
        }
        else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //loginThreadRunning = false;
                    signupButton.setEnabled(true);
                    signupButton.setVisibility(View.VISIBLE);
                    signupPB.setEnabled(false);
                    signupPB.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

}
