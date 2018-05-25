package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RelativeLayout;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.AuthSignUp;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.activity.SignUp;

import java.util.ArrayList;
import java.util.Calendar;

public class AuthBirthdayInput extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private AuthSignUp activity;
    private DatePicker datePicker;
    private Button nextButton;
    private boolean firstRound = true;
    private boolean validated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_signup_bday, container, false);
        nextButton = (Button)rootView.findViewById(R.id.wybbutton);
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        datePicker = (DatePicker) rootView.findViewById(R.id.datePicker4);
        final Calendar calendar = Calendar.getInstance();
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener(){
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth){
                Log.d("called", "yeah");
                if(year == calendar.get(Calendar.YEAR)){
                    if(month == calendar.get(Calendar.MONTH)){
                        if(dayOfMonth > calendar.get(Calendar.DAY_OF_MONTH)){
                            datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                        }
                    }
                    else if(month > calendar.get(Calendar.MONTH)){
                        datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), dayOfMonth);
                    }
                }
                if(year > calendar.get(Calendar.YEAR)){
                    datePicker.updateDate(calendar.get(Calendar.YEAR), month, dayOfMonth);
                }
                nextButton.setEnabled(true);
                nextButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsRed));
                validated = true;
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                int month = datePicker.getMonth() + 1;
                int day = datePicker.getDayOfMonth();
                int year = datePicker.getYear();
                String bday = Integer.toString(month) + "-" + Integer.toString(day) + "-" + Integer.toString(year);
                activity.setB(bday);
                activity.getViewPager().setCurrentItem(1);

            }
        });


        nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
        nextButton.setEnabled(false);
        //disableChildViews();

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (AuthSignUp) context;
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
