package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.activity.SignUp;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.SessionManager;

import java.util.ArrayList;

public class WhatsYourPassword extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private DynamoDBMapper mapper;
    private SessionManager sessionManager;
    private Button signupButton;
    private ProgressBar signupPB;
    private SignUp activity;
    private TextView petWarning;
    private EditText perkyText;
    private boolean validated = false;
    private boolean firstRound = true;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_signup_pw, container, false);
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        petWarning = (TextView)rootView.findViewById(R.id.petwarning);

        perkyText = (TextInputEditText)rootView.findViewById(R.id.editText5);


        perkyText.addTextChangedListener(new FormValidator(perkyText) {
            @Override
            public void validate(TextView textView, String text) {
                if (text.length() > 0) {
                    if(text.length() >= 6){
                        if(text.charAt(0) == ' '){
                            petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                            petWarning.setText("Password cannot start with blank space");
                            signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                            signupButton.setEnabled(false);
                            validated = false;
                        }
                        else if(text.charAt(text.length()-1) == ' '){
                            petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                            petWarning.setText("Password cannot end with blank space");
                            signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                            signupButton.setEnabled(false);
                            validated = false;
                        }
                        else{
                            passwordStrengthCheck(text);
                            signupButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsRed));
                            signupButton.setEnabled(true);
                            validated = true;
                        }
                    }
                    else{
                        petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                        petWarning.setText("Must be at least 6 characters");
                        signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                        signupButton.setEnabled(false);
                        validated = false;
                    }

                } else {
                    //petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                    petWarning.setText("");
                    signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                    signupButton.setEnabled(false);
                    validated = false;
                }
            }
        });
        signupButton = (Button)rootView.findViewById(R.id.signupsubmit);
        signupPB = (ProgressBar)rootView.findViewById(R.id.signuppb);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                try{
                    imm.hideSoftInputFromWindow(signupButton.getWindowToken(), 0);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                activity.setBiebs(perkyText.getText().toString());
                activity.signUpUser();
            }
        });

        displayProgressBar(false);

        disableChildViews();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //save the activity to a member of this fragment
        activity = (SignUp) context;
    }



    public void displayProgressBar(boolean display){
        if(display){
            perkyText.setEnabled(false);
            signupButton.setEnabled(false);
            signupButton.setVisibility(View.INVISIBLE);
            signupPB.setEnabled(true);
            signupPB.setVisibility(View.VISIBLE);
        }
        else{
            perkyText.setEnabled(true);
            signupButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsRed));
            signupButton.setEnabled(true);
            signupButton.setVisibility(View.VISIBLE);
            signupPB.setEnabled(false);
            signupPB.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null)
                enableChildViews();
        }
        else {
            if (rootView != null)
                disableChildViews();
        }
    }


    public void enableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));
        }
        signupButton.setEnabled(validated);
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    public void passwordStrengthCheck(String text){
        int strength = 0;

        if(text.length() >= 4){
            strength++;
        }
        if(text.length() >= 6){
            strength++;
        }
        if(!text.toLowerCase().equals(text)){
            strength++;
        }
        int digitCount = 0;
        for (int i = 0; i < text.length(); i++){
            if(Character.isDigit(text.charAt(i))){
                digitCount++;
            }
        }
        if(digitCount > 0 && digitCount < text.length()){
            strength++;
        }

        switch (strength){
            case 0:
                petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                petWarning.setText("Password strength: weak");
                break;
            case 1:
                petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                petWarning.setText("Password strength: weak");
                break;
            case 2:
                petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeYellow));
                petWarning.setText("Password strength: medium");
                break;
            case 3:
                petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeGreen));
                petWarning.setText("Password strength: good");
                break;
            case 4:
                petWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeGreen));
                petWarning.setText("Password strength: strong");
                break;
        }
    }

}

