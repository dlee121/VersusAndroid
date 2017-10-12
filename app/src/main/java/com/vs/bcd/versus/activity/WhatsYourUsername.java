package com.vs.bcd.versus.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.User;

import java.util.ArrayList;

public class WhatsYourUsername extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private SignUp activity;
    private EditText editText;
    private TextView etWarning;
    private boolean firstRound = true;
    private Button nextButton;
    private int usernameLength = 0;
    private int usernameVersion = 0;
    private ProgressBar usernamecheckPB;
    private boolean ignoreAsync = true;
    private boolean validated = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_signup_username, container, false);
        activity = (SignUp)getActivity();
        usernamecheckPB = (ProgressBar)rootView.findViewById(R.id.usernamecheckpb);
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        etWarning = (TextView)rootView.findViewById(R.id.etwarning);

        editText = (EditText)rootView.findViewById(R.id.unameet);

        editText.addTextChangedListener(new FormValidator(editText) {
            @Override
            public void validate(TextView textView, String text) {
                usernameLength = text.trim().length();
                if(usernameLength > 0){
                    ignoreAsync = false;
                    etWarning.setTextColor(Color.GRAY);
                    etWarning.setText("Checking username...");
                    showProgressBar();
                    checkUsername(text.trim(), ++usernameVersion);
                }
                else{
                    ignoreAsync = true;
                    hideProgressBar();
                    if(text.length() > 0){  //so text is bunch of whitespace characters
                        etWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                        etWarning.setText("Username has to have at least 1 non-whitespace character");
                    }
                    else{
                        etWarning.setText("");
                    }
                    nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
                    nextButton.setEnabled(false);
                    validated = false;
                }
            }
        });

        nextButton = (Button)rootView.findViewById(R.id.wyunbutton);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                activity.setFo(editText.getText().toString().trim());
                activity.getViewPager().setCurrentItem(3);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            }
        });
        hideProgressBar();
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







    private void checkUsername(final String username, final int thisVersion) {

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                //if (NetworkAvailablity.checkNetworkStatus(MyActivity.this))
                if(!ignoreAsync && thisVersion >= usernameVersion){

                    try {

                        //TODO:show progressbar for and "checking..." etWarning text for username checking
                        if(!ignoreAsync && thisVersion >= usernameVersion){
                            if(activity.getMapper().load(User.class, username) == null){    //if query returns null, it means username doesn't exist in db and is therefore available
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!ignoreAsync && thisVersion >= usernameVersion){
                                            hideProgressBar();
                                            etWarning.setTextColor(Color.GRAY);
                                            etWarning.setText("Username available");
                                            nextButton.setEnabled(true);
                                            validated = true;
                                            nextButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsTwo));
                                        }
                                    }
                                });
                            }

                            else {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!ignoreAsync && thisVersion >= usernameVersion){
                                            hideProgressBar();
                                            etWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                                            etWarning.setText(username + " is already taken!");
                                            nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
                                            nextButton.setEnabled(false);
                                            validated = false;
                                        }
                                    }
                                });
                            }
                        }

                    } catch (Exception e) {
                        // writing error to Log
                        e.printStackTrace();
                    }

                }
                return null;
            }
            @Override
            protected void onProgressUpdate(String... values) {
                // TODO Auto-generated method stub
                super.onProgressUpdate(values);
                System.out.println("Progress : "  + values);
            }
            @Override
            protected void onPostExecute(String result)
            {

            }
        };
        _Task.execute((String[]) null);
    }

    public void showProgressBar(){
        usernamecheckPB.setEnabled(true);
        usernamecheckPB.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar(){
        usernamecheckPB.setEnabled(false);
        usernamecheckPB.setVisibility(View.INVISIBLE);
    }




}
