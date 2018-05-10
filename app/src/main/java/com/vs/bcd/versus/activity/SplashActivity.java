package com.vs.bcd.versus.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import  android.content.Intent;
import android.util.Log;
import android.view.View;

import com.vs.bcd.versus.model.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //for messenger notification click, get rnum from intent extra
        String notificationType = null;
        if(getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().get("type") != null){
            notificationType = getIntent().getExtras().get("type").toString();
        }

        SessionManager sessionManager = new SessionManager(getApplicationContext());
        Intent intent;

        if(sessionManager.isLoggedIn()) {
            intent = new Intent(this, MainContainer.class);
            if(notificationType != null){
                intent.putExtra("type", notificationType);
            }
        }
        else {
            intent = new Intent(this, StartScreen.class);

        }

        startActivity(intent);
        finish();
    }
}
