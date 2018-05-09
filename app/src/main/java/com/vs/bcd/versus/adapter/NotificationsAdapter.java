package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.NotificationsTab;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.LeaderboardEntry;
import com.vs.bcd.versus.model.NotificationItem;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.User;

import java.util.HashSet;
import java.util.List;


public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_U = 0; //new comment upvote notification
    private final int TYPE_C = 1; //new comment reply notification
    private final int TYPE_V = 2; //new post vote notification
    private final int TYPE_R = 3; //new post root comment notification
    private final int TYPE_F = 4; //new follower notification
    private final int TYPE_M = 5; //new medal notification

    private final int VIEW_TYPE_HIDE = 0;
    private final int VIEW_TYPE_SHOW = 1;

    private Activity activity;
    private List<NotificationItem> nItems;
    private SparseIntArray mostRecentTimeValue = null;
    private NotificationsTab notificationsTab;

    public NotificationsAdapter(List<NotificationItem> nItems, NotificationsTab notificationsTab, Activity activity) {
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
            View view = LayoutInflater.from(activity).inflate(R.layout.notification_hidden, parent, false);
            return new HiddenNotificationViewHolder(view);
        }
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof NotificationViewHolder){
            NotificationItem notificationItem = nItems.get(position);
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
}