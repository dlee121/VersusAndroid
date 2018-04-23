package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.CreateMessage;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.vs.bcd.versus.R.string.username;


public class InvitedUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private MainContainer activity;
    private List<String> usernameList;
    private CreateMessage thisFragment;
    private int profileImgDimension;
    private Drawable defaultProfileImage;
    private HashMap<String, Integer> profileImgVersions;

    public InvitedUserAdapter(List<String> usernameList, MainContainer activity, CreateMessage thisFragment, HashMap<String, Integer> profileImgVersions) {
        this.usernameList = usernameList;
        this.activity = activity;
        this.thisFragment = thisFragment;
        profileImgDimension = activity.getResources().getDimensionPixelSize(R.dimen.comment_margin);
        defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);
        this.profileImgVersions = profileImgVersions;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.invited_user_item, parent, false);
        return new InvitedUserViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final String invitedUsername = usernameList.get(position);
        InvitedUserViewHolder invitedUserViewHolder = (InvitedUserViewHolder) holder;

        //invitedUserViewHolder.invitedUserPhoto.setImageResource( ***image goes here*** ); //TODO: set user profile image downloaded from S3
        invitedUserViewHolder.invitedUsername.setText(invitedUsername);

        try{
            Integer profileImg = profileImgVersions.get(invitedUsername);
            if(profileImg != null && profileImg.intValue() != 0){
                GlideApp.with(activity).load(activity.getProfileImgUrl(invitedUsername, profileImg)).override(profileImgDimension, profileImgDimension).into(invitedUserViewHolder.invitedUserPhoto);
            }
            else{
                GlideApp.with(activity).load(defaultProfileImage).into(invitedUserViewHolder.invitedUserPhoto);
            }

        }catch (Throwable t){

        }


        invitedUserViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thisFragment.removeFromInvitedList(invitedUsername);
                thisFragment.removeFromCheckedItems(invitedUsername);
            }
        });
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