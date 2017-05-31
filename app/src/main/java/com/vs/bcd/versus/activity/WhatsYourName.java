package com.vs.bcd.versus.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.MenuInflater;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.WhatsYourBirthday;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import static android.R.id.message;
import static android.provider.AlarmClock.EXTRA_MESSAGE;


public class WhatsYourName extends AppCompatActivity {

    public static final String EXTRA_WYN = "com.example.myfirstapp.WYN";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_your_name);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);
    }

    public void WhatsYourNameNext(View view){
        Intent intent = new Intent(this, WhatsYourBirthday.class);
        EditText firstName = (EditText) findViewById(R.id.editText3);
        EditText lastName = (EditText) findViewById(R.id.editText4);

        String fullName = firstName.getText().toString() + "%" + lastName.getText().toString(); //string containing first and last name, with '%' as delimiter between first name and last name
        intent.putExtra(EXTRA_WYN, fullName);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

}
