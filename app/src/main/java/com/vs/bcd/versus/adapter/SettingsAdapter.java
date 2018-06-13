package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.apigateway.ApiClientException;
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.internal.api.FirebaseNoSignedInUserException;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.activity.StartScreen;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.SettingObject;
import com.vs.bcd.versus.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private MainContainer activity;
    private List<SettingObject> settingObjects;
    private Toast mToast;

    public SettingsAdapter(List<SettingObject> settingObjects, MainContainer activity) {
        this.settingObjects = settingObjects;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.setting_object, parent, false);
        return new SettingObjectViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final SettingObject settingObject = settingObjects.get(position);
        SettingObjectViewHolder settingObjectViewHolder = (SettingObjectViewHolder) holder;

        //settingObjectViewHolder.settingIcon.setImageResource(settingObject.getIconResID());
        settingObjectViewHolder.settingName.setText(settingObject.getSettingName());

        switch(settingObject.getSettingName()){
            case "Log Out":
                settingObjectViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.sessionLogOut();
                    }
                });
                break;

            case "Set Up Email for Account Recovery":
                settingObjectViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        //builder.setTitle("Set Up Email for Account Recovery");

                        LinearLayout layout = new LinearLayout(activity);
                        layout.setOrientation(LinearLayout.VERTICAL);

                        TextView titleView = new TextView(activity);
                        titleView.setText("Set Up Email for Account Recovery");
                        int eightDP = activity.getResources().getDimensionPixelSize(R.dimen.eight);
                        int fourDP = activity.getResources().getDimensionPixelSize(R.dimen.four);
                        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                        titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        tlp.setMargins(0, eightDP, 0, 0);
                        titleView.setLayoutParams(tlp);
                        layout.addView(titleView);

                        // Set up the input
                        final EditText input = new EditText(activity);
                        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                        input.setBackground(ContextCompat.getDrawable(activity, R.drawable.edit_text_smooth_boy));
                        input.setTextColor(Color.parseColor("#000000"));
                        input.setHint("Enter your email");

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, eightDP*6);
                        lp.setMargins(fourDP, eightDP, fourDP, 0);
                        input.setLayoutParams(lp);
                        layout.addView(input); // Another add method

                        // Set up the input
                        TextInputLayout textInputLayout = new TextInputLayout(activity);
                        textInputLayout.setPasswordVisibilityToggleEnabled(true);

                        final TextInputEditText pwin = new TextInputEditText(activity);
                        pwin.setInputType(InputType.TYPE_CLASS_TEXT |InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        pwin.setHint("Enter your password");
                        textInputLayout.addView(pwin);


                        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp2.setMargins(fourDP, 0, fourDP, 0);
                        textInputLayout.setLayoutParams(lp2);

                        layout.addView(textInputLayout); // Another add method


                        builder.setView(layout);

                        // Set up the buttons
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //we set it up below, to override default click handler that automatically closes dialog on click
                            }
                        });

                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        final AlertDialog alertDialog = builder.show();
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if(pwin.getText().toString().isEmpty()){
                                    if(mToast != null){
                                        mToast.cancel();
                                    }
                                    mToast = Toast.makeText(activity, "Please enter your password.", Toast.LENGTH_SHORT);
                                    mToast.show();
                                }
                                else{
                                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); //to prevent repeat clicks
                                    if(mToast != null){
                                        mToast.cancel();
                                    }
                                    mToast = Toast.makeText(activity, "Setting up account recovery...", Toast.LENGTH_SHORT);
                                    mToast.show();

                                    String currEmail = activity.getUserEmail();
                                    if(currEmail.equals("0")){
                                        currEmail = activity.getUsername()+"@versusbcd.com";
                                    }

                                    final String newEmail = input.getText().toString();

                                    FirebaseAuth.getInstance().signInWithEmailAndPassword(currEmail, pwin.getText().toString())
                                            .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    if (task.isSuccessful()) {

                                                        final FirebaseUser firebaseUser= FirebaseAuth.getInstance().getCurrentUser();

                                                        if(firebaseUser != null){
                                                            firebaseUser.updateEmail(newEmail).addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        Runnable runnable = new Runnable() {
                                                                            public void run() {

                                                                                try{
                                                                                    activity.getClient().setemailGet(newEmail, "sem", activity.getUsername()); //testing. set a's email

                                                                                    activity.setUserEmail(newEmail);

                                                                                    activity.runOnUiThread(new Runnable() {
                                                                                        @Override
                                                                                        public void run() {
                                                                                            alertDialog.dismiss();
                                                                                            if(mToast != null){
                                                                                                mToast.cancel();
                                                                                            }
                                                                                            mToast = Toast.makeText(activity, "Account recovery was set up successfully!.", Toast.LENGTH_SHORT);
                                                                                            mToast.show();
                                                                                        }
                                                                                    });

                                                                                }
                                                                                catch (ApiClientException | NotAuthorizedException e){
                                                                                    activity.runOnUiThread(new Runnable() {
                                                                                        @Override
                                                                                        public void run() {

                                                                                            if(mToast != null){
                                                                                                mToast.cancel();
                                                                                            }
                                                                                            mToast = Toast.makeText(activity, "Something went wrong. Please check your network connection and try again.", Toast.LENGTH_SHORT);
                                                                                            mToast.show();

                                                                                            alertDialog.dismiss();

                                                                                            activity.handleNotAuthorizedException();

                                                                                        }
                                                                                    });

                                                                                }
                                                                            }
                                                                        };
                                                                        Thread mythread = new Thread(runnable);
                                                                        mythread.start();
                                                                    }
                                                                    else{
                                                                        if(mToast != null){
                                                                            mToast.cancel();
                                                                        }
                                                                        mToast = Toast.makeText(activity, "This email address is already in use", Toast.LENGTH_SHORT);
                                                                        mToast.show();
                                                                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else{
                                                            if(mToast != null){
                                                                mToast.cancel();
                                                            }
                                                            mToast = Toast.makeText(activity, "Something went wrong. Please check your network connection and try again.", Toast.LENGTH_SHORT);
                                                            mToast.show();
                                                            alertDialog.dismiss();
                                                        }
                                                    }
                                                    else {

                                                        if(mToast != null){
                                                            mToast.cancel();
                                                        }
                                                        mToast = Toast.makeText(activity, "Please check your password", Toast.LENGTH_SHORT);
                                                        mToast.show();
                                                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                                    }
                                                }
                                            });

                                }



                            }
                        });

                        final Button positive= alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        positive.setEnabled(false);


                        input.addTextChangedListener(new FormValidator(input) {
                            @Override
                            public void validate(TextView textView, String text) {
                                positive.setEnabled(false);
                                if(text.trim().length() > 0 && isEmailValid(text) && !text.substring(text.indexOf('@')).equals("@versusbcd.com")){
                                    positive.setEnabled(true);
                                }
                            }
                        });
                    }
                });

                break;
            /*
            case "Add 10 Posts":
                settingObjectViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Runnable runnable = new Runnable() {
                            public void run() {
                                for(int i = 0; i<10; i++){
                                    Post post = new Post();
                                    post.setCategory(5);
                                    post.setAuthor("Deeks");
                                    post.setRedname("red");
                                    post.setBlackname("blue");
                                    post.setQuestion("question?");
                                    post.setRedimg("default");
                                    post.setBlackimg("default");
                                    ((MainContainer)activity).getMapper().save(post);
                                }
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();

                    }
                });
                break;
            */
        }

    }

    @Override
    public int getItemCount() {
        return settingObjects == null ? 0 : settingObjects.size();
    }

    private class SettingObjectViewHolder extends RecyclerView.ViewHolder {

        //public ImageView settingIcon;
        public TextView settingName;


        public SettingObjectViewHolder(View view) {
            super(view);
            //settingIcon = (ImageView) view.findViewById(R.id.setting_icon);
            settingName = (TextView) view.findViewById(R.id.setting_name);
        }
    }

    public void updateList(List<SettingObject> list){
        settingObjects = list;
        notifyDataSetChanged();
    }

    public static boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

}