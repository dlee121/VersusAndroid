package com.vs.bcd.versus;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

public class WhatsYourBirthday extends AppCompatActivity {


    public static final String EXTRA_WYB = "com.example.myfirstapp.WYB";
    private String fullnameAndBirthday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_your_birthday);


        Intent intent = getIntent();
        fullnameAndBirthday = intent.getStringExtra(WhatsYourName.EXTRA_WYN);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);
    }

    public void WhatsYourBirthdayNext(View view){
        Intent intent = new Intent(this, WhatsYourUsername.class);
        DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker4);
        int month = datePicker.getMonth() + 1;
        int day = datePicker.getDayOfMonth();
        int year = datePicker.getYear();
        fullnameAndBirthday = fullnameAndBirthday + "%" + Integer.toString(year) + "-" + Integer.toString(month) + "-" + Integer.toString(day); //fullname%bday-bmonth-byear
        intent.putExtra(EXTRA_WYB, fullnameAndBirthday);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

}
