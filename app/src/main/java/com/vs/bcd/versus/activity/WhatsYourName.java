package com.vs.bcd.versus.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vs.bcd.versus.R;

import com.vs.bcd.versus.model.FormValidator;

import java.util.ArrayList;


public class WhatsYourName extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private EditText firstnameET, lastnameET;
    private SignUp activity;
    private int firstLength = 0;
    private int lastLength = 0;
    private Button nextButton;
    private boolean firstRound = true;
    private boolean validated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_signup_name, container, false);
        activity = (SignUp)getActivity();
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        nextButton = (Button)rootView.findViewById(R.id.wynbutton);
        nextButton.setEnabled(false);

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        firstnameET = (EditText) rootView.findViewById(R.id.firstnameet);
        lastnameET = (EditText) rootView.findViewById(R.id.lastnameet);


        firstnameET.addTextChangedListener(new FormValidator(firstnameET) {
            @Override
            public void validate(TextView textView, String text) {
                firstLength = text.trim().length();
                if(firstLength + lastLength > 0){
                    nextButton.setEnabled(true);
                    validated = true;
                    nextButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsRed));
                }
                else{
                    nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
                    nextButton.setEnabled(false);
                    validated = false;
                }
            }
        });

        lastnameET.addTextChangedListener(new FormValidator(lastnameET) {
            @Override
            public void validate(TextView textView, String text) {
                lastLength = text.trim().length();
                if(firstLength + lastLength > 0){
                    nextButton.setEnabled(true);
                    validated = true;
                    nextButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsRed));
                }
                else{
                    nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
                    nextButton.setEnabled(false);
                    validated = false;
                }
            }
        });

        //assign on-click function for NEXT button
        rootView.findViewById(R.id.wynbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                String firstIn = firstnameET.getText().toString().trim();
                String lastIn = lastnameET.getText().toString().trim();
                if(firstLength == 0){
                    firstIn = " ";
                }
                if(lastLength == 0){
                    lastIn = " ";
                }
                activity.setTwo(firstIn, lastIn);
                activity.getViewPager().setCurrentItem(1);
            }
        });

        return rootView;
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
        nextButton.setEnabled(validated);
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }






}
