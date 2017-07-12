package com.vs.bcd.versus.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.MenuInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.WhatsYourBirthday;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.vs.bcd.versus.model.FormValidator;

import static android.R.id.input;
import static android.R.id.message;
import static android.provider.AlarmClock.EXTRA_MESSAGE;


public class WhatsYourName extends AppCompatActivity {

    public static final String EXTRA_WYN = "com.example.myfirstapp.WYN";
    private EditText firstnameET, lastnameET;
    private TextView fetWarning, letWarning;
    private boolean firstnameValidated = false;
    private boolean lastnameValidated = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_your_name);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitleTextColor(0xFFFFFF);

        firstnameET = (EditText)findViewById(R.id.firstnameet);
        lastnameET = (EditText)findViewById(R.id.lastnameet);
        fetWarning = (TextView)findViewById(R.id.fetwarning);
        letWarning = (TextView)findViewById(R.id.letwarning);

        firstnameET.addTextChangedListener(new FormValidator(firstnameET) {
            @Override
            public void validate(TextView textView, String text) {
                String textInput = text.trim();
                if(textInput.matches(".*[<>&;/{}:\\\\].*")){ //no need to consider leading/trailing whitespace, since we trim the string before taking it
                    fetWarning.setText("This first name contains characters that aren't allowed.");
                    firstnameValidated = false;
                }
                else if(textInput.length() > 0 && textInput.length() < 50){
                    fetWarning.setText("");
                    firstnameValidated = true;
                }
            }
        });

        lastnameET.addTextChangedListener(new FormValidator(lastnameET) {
            @Override
            public void validate(TextView textView, String text) {
                String textInput = text.trim();
                if(textInput.matches(".*[<>&;/{}:\\\\].*")){ //no need to consider leading/trailing whitespace, since we trim the string before taking it
                    letWarning.setText("This last name contains characters that aren't allowed.");
                    lastnameValidated = false;
                }
                else if(textInput.length() > 0 && textInput.length() < 50){
                    letWarning.setText("");
                    lastnameValidated = true;
                }
            }
        });



    }

    public void WhatsYourNameNext(View view){
        if(firstnameValidated && lastnameValidated){
            Intent intent = new Intent(this, WhatsYourBirthday.class);
            EditText firstName = (EditText) findViewById(R.id.firstnameet);
            EditText lastName = (EditText) findViewById(R.id.lastnameet);

            String fullName = firstName.getText().toString().trim() + "%" + lastName.getText().toString().trim(); //string containing first and last name, with '%' as delimiter between first name and last name
            intent.putExtra(EXTRA_WYN, fullName);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
        else {
            Toast.makeText(this, "The name contains characters that aren't allowed.", Toast.LENGTH_SHORT).show();
        }
    }

}
