package com.vs.bcd.versus;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhoneOrEmail extends AppCompatActivity {

    public static final String EXTRA_PE = "com.example.myfirstapp.WYU";
    private String fullnameBdayUsernamePasswordPE;
    private DynamoDBMapper mapper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_or_email);

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
                this.getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);


        Intent intent = new Intent(this, MainContainer.class);
        EditText emailText = (EditText) findViewById(R.id.editText6);
        fullnameBdayUsernamePasswordPE = fullnameBdayUsernamePasswordPE + "%" + emailText.getText().toString();

        Runnable runnable = new Runnable() {
            public void run() {
                User newUser = new User(fullnameBdayUsernamePasswordPE);
                mapper.save(newUser);
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

        //intent.putExtra(EXTRA_PE, fullnameBdayUsernamePasswordPE);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
