package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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

import com.amazonaws.mobileconnectors.apigateway.ApiClientException;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.AuthSignUp;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.activity.SignUp;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.User;

import java.util.ArrayList;

public class AuthUsernameInput extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private AuthSignUp activity;
    private EditText editText;
    private TextView etWarning;
    private boolean firstRound = true;
    private Button nextButton;
    private int usernameLength = 0;
    private int usernameVersion = 0;
    private ProgressBar usernamecheckPB;
    private boolean ignoreAsync = true;
    private boolean validated = false;
    private ProgressBar wyunPB;
    private TextView auLegalTV;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_signup_username, container, false);
        usernamecheckPB = rootView.findViewById(R.id.usernamecheckpb);
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        etWarning = rootView.findViewById(R.id.etwarning);

        editText = rootView.findViewById(R.id.unameet);

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
                        if (!Character.isLetterOrDigit(c)) {
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

        nextButton = rootView.findViewById(R.id.wyunbutton);
        final InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d("nextbuttonnext", "clicked");
                if(validated){
                    imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                    activity.setU(editText.getText().toString().trim());

                    activity.signUpUser();
                }
            }
        });

        wyunPB = rootView.findViewById(R.id.wyun_pb);

        auLegalTV = rootView.findViewById(R.id.au_legal);
        auLegalTV.setVisibility(View.VISIBLE);

        SpannableString ss = new SpannableString("By signing up, you agree to our Terms and Policies.");
        ss.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                try{
                    imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.versusdaily.com/terms-and-policies"));
                startActivity(browserIntent);
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(ContextCompat.getColor(activity, R.color.vsBlue));

            }
        }, 32, 50, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        auLegalTV.setText(ss);
        auLegalTV.setMovementMethod(LinkMovementMethod.getInstance());
        auLegalTV.setHighlightColor(Color.TRANSPARENT);

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


    public void displayProgressBar(boolean display){
        if(display){
            editText.setEnabled(false);
            nextButton.setEnabled(false);
            nextButton.setVisibility(View.INVISIBLE);
            wyunPB.setEnabled(true);
            wyunPB.setVisibility(View.VISIBLE);
        }
        else{
            editText.setEnabled(true);
            nextButton.setBackgroundColor(ContextCompat.getColor(activity,R.color.vsRed));
            nextButton.setEnabled(true);
            nextButton.setVisibility(View.VISIBLE);
            wyunPB.setEnabled(false);
            wyunPB.setVisibility(View.INVISIBLE);
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
