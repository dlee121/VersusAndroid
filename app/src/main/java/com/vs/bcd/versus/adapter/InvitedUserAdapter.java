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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class InvitedUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity activity;
    private List<String> usernameList;

    public InvitedUserAdapter(List<String> usernameList, Activity activity) {
        this.usernameList = usernameList;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.invited_user_item, parent, false);
        return new InvitedUserViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final String username = usernameList.get(position);
        InvitedUserViewHolder invitedUserViewHolder = (InvitedUserViewHolder) holder;

        //invitedUserViewHolder.invitedUserPhoto.setImageResource( ***image goes here*** ); //TODO: set user profile image downloaded from S3
        invitedUserViewHolder.invitedUsername.setText(username);
    }

    @Override
    public int getItemCount() {
        return usernameList == null ? 0 : usernameList.size();
    }

    private class InvitedUserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView invitedUserPhoto;
        TextView invitedUsername;

        public InvitedUserViewHolder(View v) {
            super(v);
            invitedUserPhoto = (CircleImageView) itemView.findViewById(R.id.invited_user_photo);
            invitedUsername = (TextView) itemView.findViewById(R.id.invited_username);
        }
    }

    public void updateList(List<String> list){
        usernameList = list;
        notifyDataSetChanged();
    }
}