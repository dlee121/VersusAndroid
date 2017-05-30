package com.vs.bcd.versus;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

public class WhatsYourPassword extends AppCompatActivity {

    public static final String EXTRA_WYP = "com.example.myfirstapp.WYU";
    private String fullnameBdayUsernamePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_your_password);


        Intent intent = getIntent();
        fullnameBdayUsernamePassword = intent.getStringExtra(WhatsYourUsername.EXTRA_WYU);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);
    }


    public void WhatsYourPasswordNext(View view){
        //TODO: validate and hash/salt password
        Intent intent = new Intent(this, PhoneOrEmail.class);
        TextInputEditText editText = (TextInputEditText) findViewById(R.id.editText5);
        fullnameBdayUsernamePassword = fullnameBdayUsernamePassword + "%" + editText.getText().toString(); //fullname%bday-bmonth-byear%username%password
        intent.putExtra(EXTRA_WYP, fullnameBdayUsernamePassword);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

}
