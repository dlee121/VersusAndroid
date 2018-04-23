package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.CreateMessage;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.GlideUrlCustom;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class ContactsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  implements ListPreloader.PreloadModelProvider<String>{
    private MainContainer activity;
    private List<String> contactsList;
    private CreateMessage thisFragment;
    private HashSet<String> checkedItems;
    private HashMap<String, Integer> profileImgVersions;
    private int profileImgDimension;
    private Drawable defaultProfileImage;
    private RoomObject inviteTargetRoom = null;
    private HashMap<String, Integer> inviteTargetUsersList = null;

    public ContactsListAdapter(List<String> contactsList, MainContainer activity, CreateMessage thisFragment, HashMap<String, Integer> profileImgVersions) {
        this.contactsList = contactsList;
        this.activity = activity;
        this.thisFragment = thisFragment;
        checkedItems = new HashSet<>();
        this.profileImgVersions = profileImgVersions;
        defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);
        profileImgDimension = activity.getResources().getDimensionPixelSize(R.dimen.comment_margin);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.user_search_item, parent, false);
        return new UserSearchViewHolder(view);
    }

    public void setInviteTargetUsersListNull(){
        inviteTargetUsersList = null;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final String contactUsername = contactsList.get(position);
        final UserSearchViewHolder userSearchViewHolder = (UserSearchViewHolder) holder;


        if(activity.isInviteMode()){
            if(inviteTargetUsersList == null){
                inviteTargetUsersList = thisFragment.getInviteTargetRoomUsersList();
            }
            if(inviteTargetUsersList != null){
                Integer numberCode = inviteTargetUsersList.get(contactUsername);
                if(numberCode == null){ //this user is not yet in the room, so eligible for invite
                    userSearchViewHolder.contactName.setTextColor(Color.BLACK);
                    userSearchViewHolder.contactProfileImg.setColorFilter(null);

                    userSearchViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(!checkedItems.contains(contactUsername)){
                                userSearchViewHolder.checkMark.setVisibility(View.VISIBLE);
                                checkedItems.add(contactUsername);
                                thisFragment.addToInvitedList(contactUsername);
                            }
                            else {
                                userSearchViewHolder.checkMark.setVisibility(View.INVISIBLE);
                                checkedItems.remove(contactUsername);
                                thisFragment.removeFromInvitedList(contactUsername);
                            }
                        }
                    });

                }
                else if(numberCode.intValue() < 0 || numberCode.intValue() == 1 || numberCode.intValue() == 3){ //this user is already in the room so not eligible for invite
                    userSearchViewHolder.contactName.setTextColor(Color.GRAY);
                    userSearchViewHolder.contactProfileImg.setColorFilter(Color.argb(180, 136, 136, 136));
                    userSearchViewHolder.itemView.setOnClickListener(null);
                }
                else if(numberCode.intValue() == 0 || numberCode.intValue() == 2){ //this user left the room once or twice, so increment their numberCode once they're added to the room in DB
                    userSearchViewHolder.contactName.setTextColor(Color.BLACK);
                    userSearchViewHolder.contactProfileImg.setColorFilter(null);

                    userSearchViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(!checkedItems.contains(contactUsername)){
                                userSearchViewHolder.checkMark.setVisibility(View.VISIBLE);
                                checkedItems.add(contactUsername);
                                thisFragment.addToInvitedList(contactUsername);

                                activity.addToInviteNumberCodeUpdateList(contactUsername);
                            }
                            else {
                                userSearchViewHolder.checkMark.setVisibility(View.INVISIBLE);
                                checkedItems.remove(contactUsername);
                                thisFragment.removeFromInvitedList(contactUsername);

                                activity.removeFromInviteNumberCodeUpdateList(contactUsername);
                            }
                        }
                    });

                }
                else{ //this means numberCode == 4, meaning the user is not eligible for invite because they left the room three times
                    userSearchViewHolder.contactName.setTextColor(Color.GRAY);
                    userSearchViewHolder.contactProfileImg.setColorFilter(Color.argb(180, 136, 136, 136));
                    userSearchViewHolder.itemView.setOnClickListener(null);
                }
            }
        }
        else{
            userSearchViewHolder.contactName.setTextColor(Color.BLACK);
            userSearchViewHolder.contactProfileImg.setColorFilter(null);

            userSearchViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!checkedItems.contains(contactUsername)){
                        userSearchViewHolder.checkMark.setVisibility(View.VISIBLE);
                        checkedItems.add(contactUsername);
                        thisFragment.addToInvitedList(contactUsername);
                    }
                    else {
                        userSearchViewHolder.checkMark.setVisibility(View.INVISIBLE);
                        checkedItems.remove(contactUsername);
                        thisFragment.removeFromInvitedList(contactUsername);
                    }
                }
            });
        }

        if(thisFragment.isNewContact(contactUsername)){
            String contactString = contactUsername + "\tNEW";
            userSearchViewHolder.contactName.setText(contactString);
        }
        else{
            userSearchViewHolder.contactName.setText(contactUsername);
        }


        if(checkedItems.contains(contactUsername)){
            userSearchViewHolder.checkMark.setVisibility(View.VISIBLE);
        }
        else {
            userSearchViewHolder.checkMark.setVisibility(View.INVISIBLE);
        }

        try{
            Integer profileImg = profileImgVersions.get(contactUsername);
            if(profileImg != null && profileImg.intValue() != 0){
                GlideApp.with(activity).load(activity.getProfileImgUrl(contactUsername, profileImg)).override(profileImgDimension, profileImgDimension).into(userSearchViewHolder.contactProfileImg);
            }
            else{
                GlideApp.with(activity).load(defaultProfileImage).into(userSearchViewHolder.contactProfileImg);
            }

        }catch (Throwable t){

        }
    }

    @Override
    public int getItemCount() {
        return contactsList == null ? 0 : contactsList.size();
    }

    private class UserSearchViewHolder extends RecyclerView.ViewHolder {
        CircleImageView contactProfileImg;
        TextView contactName;
        ImageView checkMark;

        public UserSearchViewHolder(View v) {
            super(v);
            contactProfileImg = (CircleImageView) itemView.findViewById(R.id.search_user_photo);
            contactName = (TextView) itemView.findViewById(R.id.search_username);
            checkMark = (ImageView) itemView.findViewById(R.id.check_circle);
        }
    }

    public void updateList(List<String> list){
        contactsList = list;
        notifyDataSetChanged();
    }

    public void removeFromCheckedItems(String username){
        checkedItems.remove(username);
        notifyDataSetChanged();
    }

    public void clearCheckedItems(){
        checkedItems.clear();
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public List<String> getPreloadItems(int position) {
        return Collections.singletonList(contactsList.get(position));
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