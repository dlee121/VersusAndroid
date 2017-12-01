package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.LeaderboardEntry;
import com.vs.bcd.versus.model.NotificationItem;
import com.vs.bcd.versus.model.User;

import java.util.List;


public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_U = 0; //new comment upvote notification
    private final int TYPE_C = 1; //new comment reply notification
    private final int TYPE_V = 2; //new post vote notification
    private final int TYPE_R = 3; //new post root comment notification
    private final int TYPE_F = 4; //new follower notification
    private final int TYPE_M = 5; //new medal notification

    private Activity activity;
    private List<NotificationItem> nItems;

    public NotificationsAdapter(List<NotificationItem> nItems, Activity activity) {
        this.nItems = nItems;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        //TODO:this is where values are put into the layout, from the post object
        NotificationItem notificationItem = nItems.get(position);
        NotificationViewHolder notificationViewHolder = (NotificationViewHolder) holder;

        switch (notificationItem.getType()){
            case TYPE_U:
                notificationViewHolder.icon.setImageResource(R.drawable.vs_shadow_w_tag);   //TODO: use type specific icon instead of default VS icon
                break;

            case TYPE_C:
                notificationViewHolder.icon.setImageResource(R.drawable.vs_shadow_w_tag);   //TODO: use type specific icon instead of default VS icon
                break;

            case TYPE_V:
                notificationViewHolder.icon.setImageResource(R.drawable.vs_shadow_w_tag);   //TODO: use type specific icon instead of default VS icon
                break;

            case TYPE_R:
                notificationViewHolder.icon.setImageResource(R.drawable.vs_shadow_w_tag);   //TODO: use type specific icon instead of default VS icon
                break;

            case TYPE_F:
                notificationViewHolder.icon.setImageResource(R.drawable.vs_shadow_w_tag);   //TODO: use type specific icon instead of default VS icon
                break;

            case TYPE_M:
                notificationViewHolder.icon.setImageResource(R.drawable.vs_shadow_w_tag);   //TODO: use type specific icon instead of default VS icon
                break;
        }

        notificationViewHolder.body.setText(notificationItem.getBody());
    }

    @Override
    public int getItemCount() {
        return nItems == null ? 0 : nItems.size();
    }

    private class NotificationViewHolder extends RecyclerView.ViewHolder {

        public ImageView icon;   //maybe switch to circular colorful icons
        public TextView body;

        public NotificationViewHolder(View view) {
            super(view);
            icon = (ImageView) view.findViewById(R.id.notification_icon);
            body = (TextView) view.findViewById(R.id.notification_body);
        }
    }

    public void clearAllItems(){
        nItems.clear();
        notifyDataSetChanged();
    }
}