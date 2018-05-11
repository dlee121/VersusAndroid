package com.vs.bcd.versus.adapter;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.GlideApp;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class GroupMembersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  implements ListPreloader.PreloadModelProvider<String>{
    private MainContainer activity;
    private List<String> membersList;
    private HashMap<String, Integer> profileImgVersions;
    private int profileImgDimension;
    private Drawable defaultProfileImage;
    private boolean followersAndFollowingsMode = false;

    public GroupMembersAdapter(List<String> membersList, MainContainer activity, HashMap<String, Integer> profileImgVersions, boolean followersAndFollowingsMode) {
        this.membersList = membersList;
        this.activity = activity;
        this.profileImgVersions = profileImgVersions;
        defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);
        profileImgDimension = activity.getResources().getDimensionPixelSize(R.dimen.comment_margin);
        this.followersAndFollowingsMode = followersAndFollowingsMode;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.user_search_item, parent, false);
        return new GroupMemberViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final String memberUsername = membersList.get(position);
        GroupMemberViewHolder groupMemberViewHolder = (GroupMemberViewHolder) holder;

        groupMemberViewHolder.memberName.setText(memberUsername);

        try{
            Integer profileImg = profileImgVersions.get(memberUsername);
            if(profileImg != null && profileImg.intValue() != 0){
                GlideApp.with(activity).load(activity.getProfileImgUrl(memberUsername, profileImg)).override(profileImgDimension, profileImgDimension).into(groupMemberViewHolder.memberProfileImg);
            }
            else{
                GlideApp.with(activity).load(defaultProfileImage).into(groupMemberViewHolder.memberProfileImg);
            }

        }catch (Throwable t){

        }

        groupMemberViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(followersAndFollowingsMode){
                    if(!activity.getFollowersAndFollowings().isFromProfile()){
                        activity.getProfileTab().profileBackStackPush();
                    }
                    activity.getFollowersAndFollowings().ffStackPush();
                    activity.hideTitleRightButton();
                    activity.goToProfile(memberUsername, true);
                }
                else{
                    activity.closeEditRoomTitle();
                    activity.hideTitleRightButton();
                    activity.goToProfile(memberUsername, true);
                }
            }
        });


    }

    public void setProfileImgVersions(HashMap<String, Integer> profileImgVersions){
        this.profileImgVersions = profileImgVersions;
    }

    @Override
    public int getItemCount() {
        return membersList == null ? 0 : membersList.size();
    }

    private class GroupMemberViewHolder extends RecyclerView.ViewHolder {
        CircleImageView memberProfileImg;
        TextView memberName;
        View divider;

        public GroupMemberViewHolder(View v) {
            super(v);
            memberProfileImg = (CircleImageView) itemView.findViewById(R.id.search_user_photo);
            memberName = (TextView) itemView.findViewById(R.id.search_username);
            divider = itemView.findViewById(R.id.user_item_divider);
        }
    }

    public void updateList(List<String> list){
        membersList = list;
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public List<String> getPreloadItems(int position) {
        try{//TODO: eventually we wanna fix the bug that's causing the exception
            return Collections.singletonList(membersList.get(position));
        }
        catch (Throwable t){
            return Collections.emptyList();
        }
    }

    @Override
    @Nullable
    public RequestBuilder getPreloadRequestBuilder(String username) {
        try{
            int profileImg;
            profileImg = profileImgVersions.get(username).intValue();
            if(profileImg == 0){
                return null;
            }
            return GlideApp.with(activity).load( activity.getProfileImgUrl(username, profileImgVersions.get(username).intValue()) ).override(profileImgDimension, profileImgDimension);

        }
        catch (Throwable t){

        }

        return null;
    }
}