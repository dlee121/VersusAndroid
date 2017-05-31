package com.vs.bcd.versus.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.WhatsYourPassword;

public class WhatsYourUsername extends AppCompatActivity {

    public static final String EXTRA_WYU = "com.example.myfirstapp.WYU";
    private String fullnameBdayUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_your_username);

        Intent intent = getIntent();
        fullnameBdayUsername = intent.getStringExtra(WhatsYourBirthday.EXTRA_WYB);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);
    }

    public void WhatsYourUsernameNext(View view){
        //TODO: validate username and only proceed with startActivity if valid (no duplicates). validate/proceed similarly for all other signup fields.
        Intent intent = new Intent(this, WhatsYourPassword.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        fullnameBdayUsername = fullnameBdayUsername + "%" + editText.getText().toString(); //fullname%bday-bmonth-byear%username
        intent.putExtra(EXTRA_WYU, fullnameBdayUsername);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
