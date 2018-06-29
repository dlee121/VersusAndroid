package com.vs.bcd.versus.fragment;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.apigateway.ApiClientException;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.SignUp;
import com.vs.bcd.versus.model.FormValidator;

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
                nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
                nextButton.setEnabled(false);
                validated = false;
                usernameLength = text.trim().length();
                usernameVersion++;
                if(usernameLength > 0){
                    char[] chars = text.trim().toCharArray();
                    boolean invalidCharacterPresent = false;
                    //iterate over characters
                    for (int i = 0; i < chars.length; i++) {
                        char c = chars[i];
                        //check if the character is alphanumeric
                        if (!isLetterOrDigit(c)) {
                            if(c != '-' && c != '_' && c != '~' && c != '%'){
                                invalidCharacterPresent = true;
                            }
                        }
                    }
                    if(invalidCharacterPresent){
                        etWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                        etWarning.setText("Can only contain letters, numbers, and the following special characters: '-', '_', '~', and '%'");
                    }
                    else {
                        ignoreAsync = false;
                        etWarning.setTextColor(Color.GRAY);
                        etWarning.setText("Checking username...");
                        showProgressBar();
                        checkUsername(text.trim(), usernameVersion);
                    }
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
                if(validated) {
                    activity.setUsername(editText.getText().toString().trim());
                    activity.getViewPager().setCurrentItem(2);
                }
            }
        });
        hideProgressBar();
        disableChildViews();

        return rootView;
    }

    private boolean isLetterOrDigit(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9');
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


    private void checkUsername(final String username, final int thisVersion) { //TODO: do this using Elasticsearch instead (so first we need to set up API gateway for that)
        if(username.equals("deleted")){
            hideProgressBar();
            etWarning.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
            etWarning.setText(username + " is already taken!");
            nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
            nextButton.setEnabled(false);
            validated = false;
            return;
        }

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
                            try{
                                activity.getClient().userHead("uc", username.toLowerCase());
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
                            catch(ApiClientException e){
                                if(e.getStatusCode() == 404){ //404 means username doesn't exist in ES yet, which means it's available for new user
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(!ignoreAsync && thisVersion >= usernameVersion){
                                                hideProgressBar();
                                                etWarning.setTextColor(Color.GRAY);
                                                etWarning.setText("Username available");
                                                nextButton.setEnabled(true);
                                                validated = true;
                                                nextButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsRed));
                                            }
                                        }
                                    });

                                }
                                else {
                                    //clear cached credentials, set up credentials again, refresh, then retry
                                    hideProgressBar();
                                    etWarning.setText("");
                                    nextButton.setBackgroundColor(Color.rgb(238, 238, 238));
                                    nextButton.setEnabled(false);
                                    validated = false;

                                    activity.handleUnauthException();

                                }
                            }
                            catch(Exception e){
                                Log.d("heyheyheyya", "da fuck");
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
