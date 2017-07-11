package com.vs.bcd.versus.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
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
    private boolean signUpSuccessful = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_your_password);
        sessionManager = new SessionManager(this);

        Intent intent = getIntent();
        fullnameBdayUsernamePassword = intent.getStringExtra(WhatsYourUsername.EXTRA_WYU);

        signupButton = (Button)findViewById(R.id.signupsubmit);
        signupPB = (ProgressBar)findViewById(R.id.signuppb);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);
    }


    public void SignUpSubmit(View view){
        //TODO: validate and hash/salt password
        Intent intent = new Intent(this, MainContainer.class);
        TextInputEditText editText = (TextInputEditText) findViewById(R.id.editText5);
        fullnameBdayUsernamePassword = fullnameBdayUsernamePassword + "%" + editText.getText().toString(); //fullname%bday-bmonth-byear%username%password
        intent.putExtra(EXTRA_WYP, fullnameBdayUsernamePassword);
        startActivity(intent);
        overridePendingTransition(0, 0);



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

        final User newUser = new User(fullnameBdayUsernamePassword);
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

