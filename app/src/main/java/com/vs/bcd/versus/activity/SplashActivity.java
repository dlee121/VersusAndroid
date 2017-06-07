package com.vs.bcd.versus.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import  android.content.Intent;

import com.vs.bcd.versus.model.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessionManager = new SessionManager(this);
        Intent intent;
        if(sessionManager.isLoggedIn()) {
            intent = new Intent(this, MainContainer.class);

        }
        else {
            intent = new Intent(this, StartScreen.class);

        }
        startActivity(intent);
        finish();
    }
}
