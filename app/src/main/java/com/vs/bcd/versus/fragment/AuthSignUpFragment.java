package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.apigateway.ApiClientException;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.AuthSignUp;
import com.vs.bcd.versus.activity.SignUp;
import com.vs.bcd.versus.model.FormValidator;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class AuthSignUpFragment extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private AuthSignUp activity;
    private Toast mToast;

    private TextInputLayout usernameInputLayout, passwordInputLayout;
    private EditText usernameIn;
    private Button signupButton;
    private TextView usernameCheckTV, passwordCheckTV;
    private ProgressBar signupPB;


    private int usernameVersion = 0;
    private int usernameLength = 0;
    private boolean ignoreAsync = true;
    private boolean usernameValidated = false;

    private ScrollView signupScrollView;
    private String finalUsername;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.sign_up_fragment, container, false);

        usernameInputLayout = rootView.findViewById(R.id.signup_username_layout);
        usernameIn = rootView.findViewById(R.id.signup_username);


        passwordInputLayout = rootView.findViewById(R.id.pwtil);
        passwordCheckTV = rootView.findViewById(R.id.password_input_warning);
        passwordInputLayout.setVisibility(View.GONE);
        passwordInputLayout.setEnabled(false);
        passwordCheckTV.setVisibility(View.GONE);


        signupButton = rootView.findViewById(R.id.signup_button);
        usernameCheckTV = rootView.findViewById(R.id.username_input_warning);

        signupPB = rootView.findViewById(R.id.signup_pb);

        usernameIn.addTextChangedListener(new FormValidator(usernameIn) {
            @Override
            public void validate(TextView textView, String text) {
                //signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                signupButton.setEnabled(false);
                usernameValidated = false;
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
                        usernameCheckTV.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                        usernameCheckTV.setText("Can only contain letters, numbers, and the following special characters: '-', '_', '~', and '%'");
                    }
                    else {
                        ignoreAsync = false;
                        usernameCheckTV.setTextColor(Color.GRAY);
                        usernameCheckTV.setText("Checking username...");
                        checkUsername(text.trim(), usernameVersion);
                    }
                }
                else{
                    ignoreAsync = true;
                    if(text.length() > 0){  //so text is bunch of whitespace characters
                        usernameCheckTV.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                        usernameCheckTV.setText("Username has to have at least 1 non-whitespace character");
                    }
                    else{
                        usernameCheckTV.setText("");
                    }
                    //signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                    signupButton.setEnabled(false);
                    usernameValidated = false;
                }
            }
        });

        signupScrollView = rootView.findViewById(R.id.signup_scrollview);

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        KeyboardVisibilityEvent.setEventListener(
                activity,
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean isOpen) {
                        // some code depending on keyboard visiblity status
                        signupScrollView.smoothScrollTo(0,signupScrollView.getBottom());
                    }
                });

        final InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(usernameValidated && finalUsername != null) {
                    try{
                        imm.hideSoftInputFromWindow(signupButton.getWindowToken(), 0);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                    activity.setB("0");
                    activity.setU(finalUsername);

                    activity.signUpUser();

                    signupButton.setVisibility(View.INVISIBLE);
                    signupButton.setEnabled(false);
                    signupPB.setVisibility(View.VISIBLE);
                }
                else {
                    if(mToast != null){
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(activity, "Please enter a valid username", Toast.LENGTH_SHORT);
                    mToast.show();
                }

            }
        });


        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (AuthSignUp) context;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                enableChildViews();
            }
        }
        else {
            if (rootView != null)
                disableChildViews();
        }
    }


    public void enableChildViews(){
        signupScrollView.setVisibility(View.VISIBLE);
    }

    public void disableChildViews(){
        signupScrollView.setVisibility(View.GONE);
    }

    private boolean isLetterOrDigit(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9');
    }

    public void reenableSignupButton() {
        signupButton.setEnabled(true);
        signupButton.setVisibility(View.VISIBLE);
        signupPB.setVisibility(View.GONE);
    }

    private void checkUsername(final String username, final int thisVersion) { //TODO: do this using Elasticsearch instead (so first we need to set up API gateway for that)
        if(username.toLowerCase().equals("deleted") || username.toLowerCase().equals("ad")){
            usernameCheckTV.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
            usernameCheckTV.setText(username + " is already taken!");
            //signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
            signupButton.setEnabled(false);
            usernameValidated = false;
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

                        //TODO:show progressbar for and "checking..." usernameCheckTV text for username checking
                        if(!ignoreAsync && thisVersion >= usernameVersion){
                            try{
                                activity.getClient().userHead("uc", username.toLowerCase());
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!ignoreAsync && thisVersion >= usernameVersion){
                                            usernameCheckTV.setTextColor(ContextCompat.getColor(activity,R.color.noticeRed));
                                            usernameCheckTV.setText(username + " is already taken!");
                                            //signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                                            signupButton.setEnabled(false);
                                            usernameValidated = false;
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
                                                usernameCheckTV.setTextColor(Color.GRAY);
                                                usernameCheckTV.setText("Username available");
                                                usernameValidated = true;
                                                finalUsername = username;
                                                signupButton.setEnabled(true);
                                            }
                                        }
                                    });

                                }
                                else {
                                    //clear cached credentials, set up credentials again, refresh, then retry
                                    usernameCheckTV.setText("Username must ");
                                    //signupButton.setBackgroundColor(Color.rgb(238, 238, 238));
                                    signupButton.setEnabled(false);
                                    usernameValidated = false;

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


}
