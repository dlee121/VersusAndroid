package com.vs.bcd.versus.activity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.FormValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import static com.vs.bcd.versus.R.string.next;

public class WhatsYourBirthday extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private SignUp activity;
    private DatePicker datePicker;
    private Button nextButton;
    private boolean firstRound = true;
    private boolean validated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_yo_bday, container, false);
        activity = (SignUp)getActivity();
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
                nextButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsTwo));
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
                activity.setThr(bday);
                activity.getViewPager().setCurrentItem(2);

            }
        });


        nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
        nextButton.setEnabled(false);
        disableChildViews();

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
