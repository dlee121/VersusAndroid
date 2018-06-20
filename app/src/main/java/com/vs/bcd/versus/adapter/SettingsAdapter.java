package com.vs.bcd.versus.adapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.apigateway.ApiClientException;
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.SettingObject;

import java.util.List;
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

            case "About":
                settingObjectViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
                        DialogFragment newFragment = AboutFragment.newInstance();
                        newFragment.show(ft, "dialog");
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

                                    final ProgressDialog progressDialog = new ProgressDialog(activity);
                                    progressDialog.setTitle("Setting up account recovery");
                                    progressDialog.show();

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
                                                                                            progressDialog.dismiss();
                                                                                            alertDialog.dismiss();
                                                                                            if(mToast != null){
                                                                                                mToast.cancel();
                                                                                            }
                                                                                            mToast = Toast.makeText(activity, "Account recovery was set up successfully!", Toast.LENGTH_SHORT);
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

                                                                                            progressDialog.dismiss();
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
                                                                        progressDialog.dismiss();
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
                                                            progressDialog.dismiss();
                                                            if(mToast != null){
                                                                mToast.cancel();
                                                            }
                                                            mToast = Toast.makeText(activity, "Something went wrong. Please check your network connection and try again.", Toast.LENGTH_SHORT);
                                                            mToast.show();
                                                            alertDialog.dismiss();
                                                        }
                                                    }
                                                    else {
                                                        progressDialog.dismiss();
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

    private boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public static class AboutFragment extends DialogFragment {

        static AboutFragment newInstance() {
            AboutFragment f = new AboutFragment();
            return f;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View v = inflater.inflate(R.layout.about_fragment, container, false);

            ImageButton exitButton = v.findViewById(R.id.frag_exit_button);
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getDialog().dismiss();
                }
            });

            TextView aboutTV1 = v.findViewById(R.id.about_tv1);

            SpannableString ss = new SpannableString("Terms and Conditions\nPrivacy Policy\nEULA");
            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.versusdaily.com/terms-and-conditions"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, 0, 20, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.versusdaily.com/privacy-policy"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, 21, 35, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.versusdaily.com/eula"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, 36, 40, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            aboutTV1.setText(ss);
            aboutTV1.setMovementMethod(LinkMovementMethod.getInstance());
            aboutTV1.setHighlightColor(Color.TRANSPARENT);

            TextView aboutTV2 = v.findViewById(R.id.about_tv2);
            String secondString = "Acknowledgements:\n\t\tCircleImageView,\n\t\tGlide \n\n"+
                    "\t\tIcons from the following authors:\n"+
                    "\t\t\t\tGregor Cresna,\n"+
                    "\t\t\t\tFreepik,\n"+
                    "\t\t\t\tChanut,\n"+
                    "\t\t\t\tEleanor Wang,\n"+
                    "\t\t\t\tPixel Perfect,\n"+
                    "\t\t\t\tIcomoon,\n"+
                    "\t\t\t\tVectors Market,\n"+
                    "\t\t\t\tAnton Saputro,\n"+
                    "\t\t\t\tHanan,\n"+
                    "\t\t\t\tPavel Kozlov,\n"+
                    "\t\t\t\tGoogle Material Design,\n"+
                    "\t\t\t\tAnd custom icons from Andrew Cho, Aryaman Kochar, Francisca Caviedes, Teddy DeMask, Nikki Mantis, and Alexia Jackson.";
            SpannableString ss2 = new SpannableString(secondString);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hdodenhof/CircleImageView/blob/master/LICENSE.txt"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("CircleImageView"), secondString.indexOf("CircleImageView")+15, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bumptech/glide/blob/master/LICENSE"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Glide"), secondString.indexOf("Glide")+5, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/gregor-cresnar"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Gregor Cresna"), secondString.indexOf("Gregor Cresna")+13, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/freepik"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Freepik"), secondString.indexOf("Freepik")+7, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/chanut"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Chanut"), secondString.indexOf("Chanut")+6, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/eleonor-wang"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Eleanor Wang"), secondString.indexOf("Eleanor Wang")+12, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/pixel-perfect"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Pixel Perfect"), secondString.indexOf("Pixel Perfect")+13, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/icomoon"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Icomoon"), secondString.indexOf("Icomoon")+7, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/vectors-market"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Vectors Market"), secondString.indexOf("Vectors Market")+14, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/anton-saputro"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Anton Saputro"), secondString.indexOf("Anton Saputro")+13, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/hanan"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Hanan"), secondString.indexOf("Hanan")+5, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.flaticon.com/authors/pavel-kozlov"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Pavel Kozlov"), secondString.indexOf("Pavel Kozlov")+12, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss2.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/google/material-design-icons"));
                    startActivity(browserIntent);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(ContextCompat.getColor(getActivity(), R.color.vsBlue));

                }
            }, secondString.indexOf("Google Material Design"), secondString.indexOf("Google Material Design")+22, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            aboutTV2.setText(ss2);
            aboutTV2.setMovementMethod(LinkMovementMethod.getInstance());
            aboutTV2.setHighlightColor(Color.TRANSPARENT);


            return v;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);

            // request a window without the title
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            return dialog;
        }

        @Override
        public void onStart()
        {
            super.onStart();
            Dialog dialog = getDialog();
            if (dialog != null)
            {
                int width = ViewGroup.LayoutParams.MATCH_PARENT;
                int height = ViewGroup.LayoutParams.MATCH_PARENT;
                dialog.getWindow().setLayout(width, height);
            }
        }

    }

}