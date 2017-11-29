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
import com.vs.bcd.versus.model.User;

import java.util.List;


public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity activity;
    private List<LeaderboardEntry> leaders;

    public LeaderboardAdapter(List<LeaderboardEntry> leaders, Activity activity) {
        this.leaders = leaders;
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.leaderboard_item, parent, false);
        return new LeaderViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        //TODO:this is where values are put into the layout, from the post object
        final LeaderboardEntry leaderboardEntry = leaders.get(position);
        LeaderViewHolder leaderViewHolder = (LeaderViewHolder) holder;

        //TODO: add onclick listener to profile pic and username that navigates user to clicked user's profile page

        leaderViewHolder.username.setText(leaderboardEntry.getUsername());
        leaderViewHolder.points.setText(Integer.toString(leaderboardEntry.getPoints()));


    }

    @Override
    public int getItemCount() {
        return leaders == null ? 0 : leaders.size();
    }

    private class LeaderViewHolder extends RecyclerView.ViewHolder {

        public TextView username;   //maybe switch to circular colorful icons
        public TextView points;

        public LeaderViewHolder(View view) {
            super(view);
            username = (TextView) view.findViewById(R.id.lb_username);
            points = (TextView) view.findViewById(R.id.lb_points);
        }
    }
}