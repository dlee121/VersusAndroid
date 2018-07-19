package com.vs.bcd.versus.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.loopj.android.http.HttpGet;
import com.vs.bcd.api.model.CommentModel;
import com.vs.bcd.api.model.PostModel;
import com.vs.bcd.api.model.PostModelSource;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.NotificationsTab;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.NotificationItem;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.VSComment;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;


public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_U = 0; //new comment upvote notification
    private final int TYPE_C = 1; //new comment reply notification
    private final int TYPE_V = 2; //new post vote notification
    private final int TYPE_R = 3; //new post root comment notification
    private final int TYPE_F = 4; //new follower notification
    private final int TYPE_M = 5; //new medal notification
    private final int TYPE_EM = 6; //for password reset email setup notification

    private final int VIEW_TYPE_HIDE = 0;
    private final int VIEW_TYPE_SHOW = 1;

    private MainContainer activity;
    private List<NotificationItem> nItems;
    private SparseIntArray mostRecentTimeValue = null;
    private NotificationsTab notificationsTab;
    private Toast mToast;

    public NotificationsAdapter(List<NotificationItem> nItems, NotificationsTab notificationsTab, MainContainer activity) {
        this.nItems = nItems;
        this.activity = activity;
        this.notificationsTab = notificationsTab;
    }

    public void setMostRecentTimeValue(SparseIntArray mostRecentTimeValue){
        this.mostRecentTimeValue = mostRecentTimeValue;
    }

    @Override
    public int getItemViewType(int position) {
        NotificationItem item = nItems.get(position);
        if(mostRecentTimeValue != null && mostRecentTimeValue.get(item.hashCode()) > item.getTimestamp()){
            return VIEW_TYPE_HIDE;
        }
        else{
            return VIEW_TYPE_SHOW;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SHOW){
            View view = LayoutInflater.from(activity).inflate(R.layout.notification_item, parent, false);
            return new NotificationViewHolder(view);
        }
        else{
            View view = LayoutInflater.from(activity).inflate(R.layout.hidden_view, parent, false);
            return new HiddenNotificationViewHolder(view);
        }
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof NotificationViewHolder){
            final NotificationItem notificationItem = nItems.get(position);
            NotificationViewHolder notificationViewHolder = (NotificationViewHolder) holder;

            switch (notificationItem.getType()){
                case TYPE_U:
                    notificationViewHolder.secondaryIcon.setImageResource(R.drawable.ic_heart_highlighted);
                    notificationViewHolder.secondaryIcon.setScaleX(1.5f);
                    notificationViewHolder.secondaryIcon.setScaleY(1.5f);
                    break;

                case TYPE_M:
                    notificationViewHolder.secondaryIcon.setScaleX(1f);
                    notificationViewHolder.secondaryIcon.setScaleY(1f);
                    switch (notificationItem.getMedalType()){
                        case "g": //gold
                            notificationViewHolder.secondaryIcon.setImageResource(R.drawable.ic_gold_medal);
                            break;
                        case "s": //silver
                            notificationViewHolder.secondaryIcon.setImageResource(R.drawable.ic_silver_medal);
                            break;
                        case "b": //bronze
                            notificationViewHolder.secondaryIcon.setImageResource(R.drawable.ic_bronze_medal);
                            break;
                        default:
                            notificationViewHolder.secondaryIcon.setImageResource(android.R.color.transparent);
                            break;
                    }
                    break;

                default:
                    notificationViewHolder.secondaryIcon.setImageResource(android.R.color.transparent);
                    break;

            }

            notificationViewHolder.body.setText(notificationItem.getBody());
            notificationViewHolder.time.setText(notificationItem.getTimeString());

            notificationViewHolder.clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(notificationsTab != null){
                        notificationsTab.clearItemAtIndex(position);
                    }
                }
            });

            notificationViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (notificationItem.getType()){
                        case TYPE_C: //go to the comment
                            openPayloadComment(notificationItem.getPayload(), notificationItem.getKey());
                            break;
                        case TYPE_F: //open followers page
                            activity.getFollowersAndFollowings().setUpFollowersPage(false, activity.getUsername());
                            break;
                        case TYPE_M: //go to the comment
                            openPayloadComment(notificationItem.getPayload(), notificationItem.getKey());
                            break;
                        case TYPE_R: //go to the post
                            openPayloadPost(notificationItem.getPayload(), notificationItem.getKey(), true);
                            break;
                        case TYPE_U: //go to the comment
                            openPayloadComment(notificationItem.getPayload(), notificationItem.getKey());
                            break;
                        case TYPE_V: //go to the post
                            openPayloadPost(notificationItem.getPayload(), notificationItem.getKey(), false);
                            break;
                        case TYPE_EM:
                            emailNotificationClicked(position);
                            break;
                    }
                }
            });



        }
    }

    @Override
    public int getItemCount() {
        return nItems == null ? 0 : nItems.size();
    }

    private class NotificationViewHolder extends RecyclerView.ViewHolder {

        public ImageView secondaryIcon;   //maybe switch to circular colorful icons
        public TextView body;
        public TextView time;
        public ImageButton clearButton;

        public NotificationViewHolder(View view) {
            super(view);
            secondaryIcon = view.findViewById(R.id.secondary_icon);
            body = view.findViewById(R.id.notification_body);
            time = view.findViewById(R.id.notification_time);
            clearButton = view.findViewById(R.id.clear_button);
        }
    }

    private class HiddenNotificationViewHolder extends RecyclerView.ViewHolder {

        public HiddenNotificationViewHolder(View view) {
            super(view);
        }
    }

    public void clearAllItems(){
        nItems.clear();
        notifyDataSetChanged();
    }


    private void openPayloadComment(final String comment_id, final String key){
        Runnable runnable = new Runnable() {
            public void run() {
                final VSComment clickedComment = getComment(comment_id);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(clickedComment == null){
                            if(mToast != null){
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(activity, "Network error. Please try again.", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                        else{
                            if(clickedComment.getParent_id().equals(clickedComment.getPost_id())){ //this is a root comment
                                activity.getPostPage().rootCommentHistoryItemClicked(clickedComment, "notifications", key);
                            }
                            else{
                                activity.getPostPage().childOrGrandchildHistoryItemClicked(clickedComment, "notifications", key);
                            }
                        }
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }

    private void openPayloadPost(final String postID, final String key, final boolean fromRItem){
        Runnable runnable = new Runnable() {
            public void run() {
                final Post clickedPost = getPost(postID);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(clickedPost == null){
                            if(mToast != null){
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(activity, "Network error. Please try again.", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                        else{
                            activity.postClickedForNotificationsTab(clickedPost, key, fromRItem);
                        }
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();


    }

    //call in new thread
    private Post getPost(String post_id){

        PostModel result = activity.getClient().postGet("p", post_id);

        try {

            return new Post(result.getSource(), result.getId());

        } catch (Exception e) {
            activity.handleNotAuthorizedException();
        }

        //if the ES GET fails, then return old topCardContent
        return null;
    }


    //Call this in a new thread
    private VSComment getComment(String comment_id){

        CommentModel result = activity.getClient().commentGet("c", comment_id);

        try {

            return new VSComment(result.getSource(), result.getId());

        } catch (Exception e) {
            activity.handleNotAuthorizedException();
        }

        return null;
    }

    private void emailNotificationClicked(final int index){
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

                                                                            if(nItems.get(index).getType() == TYPE_EM){
                                                                                notificationsTab.clearItemAtIndex(index);
                                                                            }
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

    private boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}