package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SettingObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private MainContainer activity;
    private List<SettingObject> settingObjects;

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

            case "Set Password Reset Email":
                settingObjectViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle("Enter your email");
                        // Set up the input
                        final EditText input = new EditText(activity);
                        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                        builder.setView(input);

                        // Set up the buttons
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Runnable runnable = new Runnable() {
                                    public void run() {
                                        activity.getClient().setemailGet(input.getText().toString(), "sem", "a"); //testing. set a's email

                                    }
                                };
                                Thread mythread = new Thread(runnable);
                                mythread.start();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        final AlertDialog alertDialog = builder.show();

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