package com.vs.bcd.versus.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.HttpGet;
import com.vs.bcd.api.model.CommentModel;
import com.vs.bcd.api.model.PostModel;
import com.vs.bcd.api.model.PostModelSource;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.NotificationsTab;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.NotificationItem;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.VSComment;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    private final int VIEW_TYPE_HIDE = 0;
    private final int VIEW_TYPE_SHOW = 1;

    private MainContainer activity;
    private List<NotificationItem> nItems;
    private SparseIntArray mostRecentTimeValue = null;
    private NotificationsTab notificationsTab;
    private String host, region;
    private Toast mToast;

    public NotificationsAdapter(List<NotificationItem> nItems, NotificationsTab notificationsTab, MainContainer activity) {
        this.nItems = nItems;
        this.activity = activity;
        this.notificationsTab = notificationsTab;
        host = activity.getESHost();
        region = activity.getESRegion();
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
                                activity.getPostPage().rootCommentHistoryItemClicked(clickedComment, false, key);
                            }
                            else{
                                activity.getPostPage().childOrGrandchildHistoryItemClicked(clickedComment, false, key);
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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return null;
    }
}