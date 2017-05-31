package com.vs.bcd.versus.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import  android.content.Intent;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, StartScreen.class);
        startActivity(intent);
        finish();
    }
}
