package com.vs.bcd.versus.adapter;

import android.app.Activity;
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
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.HashSet;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.R.attr.checked;
import static com.vs.bcd.versus.R.string.username;


public class UserSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity activity;
    private List<UserSearchItem> usernameList;
    private CreateMessage thisFragment;
    private HashSet<String> checkedItems;

    public UserSearchAdapter(List<UserSearchItem> usernameList, Activity activity, CreateMessage thisFragment) {
        this.usernameList = usernameList;
        this.activity = activity;
        this.thisFragment = thisFragment;
        checkedItems = new HashSet<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.user_search_item, parent, false);
        return new UserSearchViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final UserSearchItem userSearchItem = usernameList.get(position);
        final UserSearchViewHolder userSearchViewHolder = (UserSearchViewHolder) holder;

        //userSearchViewHolder.searchUserPhoto.setImageResource( ***image goes here*** ); //TODO: set user profile image downloaded from S3
        userSearchViewHolder.searchUsername.setText(userSearchItem.getUsername());

        if(checkedItems.contains(userSearchItem.getUsername())){
            userSearchViewHolder.checkMark.setVisibility(View.VISIBLE);
        }
        else {
            userSearchViewHolder.checkMark.setVisibility(View.INVISIBLE);
        }


        userSearchViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!checkedItems.contains(userSearchItem.getUsername())){
                    userSearchViewHolder.checkMark.setVisibility(View.VISIBLE);
                    checkedItems.add(userSearchItem.getUsername());
                    thisFragment.addToInvitedList(userSearchItem);
                }
                else {
                    userSearchViewHolder.checkMark.setVisibility(View.INVISIBLE);
                    checkedItems.remove(userSearchItem.getUsername());
                    thisFragment.removeFromInvitedList(userSearchItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return usernameList == null ? 0 : usernameList.size();
    }

    private class UserSearchViewHolder extends RecyclerView.ViewHolder {
        CircleImageView searchUserPhoto;
        TextView searchUsername;
        ImageView checkMark;

        public UserSearchViewHolder(View v) {
            super(v);
            searchUserPhoto = (CircleImageView) itemView.findViewById(R.id.search_user_photo);
            searchUsername = (TextView) itemView.findViewById(R.id.search_username);
            checkMark = (ImageView) itemView.findViewById(R.id.check_circle);
        }
    }

    public void updateList(List<UserSearchItem> list){
        usernameList = list;
        notifyDataSetChanged();
    }

    public void removeFromCheckedItems(String username){
        checkedItems.remove(username);
        notifyDataSetChanged();
    }
}