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
import android.widget.Toast;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.SignUp;

import java.util.ArrayList;
import java.util.Calendar;

public class WhatsYourBirthday extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private SignUp activity;
    private DatePicker datePicker;
    private Button nextButton;
    private boolean firstRound = true;
    private boolean validated = false;
    private Toast mToast;

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

        datePicker = rootView.findViewById(R.id.datePicker4);
        final Calendar calendar = Calendar.getInstance();
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener(){
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth){
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
                int month = datePicker.getMonth() + 1;
                int day = datePicker.getDayOfMonth();
                int year = datePicker.getYear();

                Log.d("bdaycalendar", "month picked: "+month);

                Calendar today = Calendar.getInstance();
                Log.d("bdaycalendar", "current month: "+Calendar.getInstance().get(Calendar.MONTH));
                int yearDiff = today.get(Calendar.YEAR) - year;
                boolean ageRequirementMet = false;
                if(yearDiff > 13){
                    ageRequirementMet = true;
                }
                else if(yearDiff == 13){
                    int monthDiff = today.get(Calendar.MONTH) + 1 - month;
                    if(monthDiff > 0){
                        ageRequirementMet = true;
                    }
                    else if(monthDiff == 0){
                        if(today.get(Calendar.DAY_OF_MONTH) >= day){
                            ageRequirementMet = true;
                        }
                    }
                }

                if(ageRequirementMet){
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                    String bday = Integer.toString(month) + "-" + Integer.toString(day) + "-" + Integer.toString(year);
                    activity.setBday(bday);
                    activity.getViewPager().setCurrentItem(1);
                }
                else{
                    if(mToast != null){
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(activity, "You must be at least 13 years old", Toast.LENGTH_SHORT);
                    mToast.show();
                }

            }
        });


        nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
        nextButton.setEnabled(false);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (SignUp) context;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                enableChildViews();
                try{
                    InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(nextButton.getWindowToken(), 0);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
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
