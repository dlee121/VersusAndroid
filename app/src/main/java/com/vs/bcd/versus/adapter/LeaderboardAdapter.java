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
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.User;

import java.util.List;


public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity activity;
    private List<LeaderboardEntry> leaders;

    private final int TYPE_G = 0; //first place
    private final int TYPE_S = 1; //second place
    private final int TYPE_B = 2; //third place
    private final int TYPE_R = 3; //all other ranked users

    public LeaderboardAdapter(List<LeaderboardEntry> leaders, Activity activity) {
        this.leaders = leaders;
        this.activity = activity;
    }

    @Override
    public int getItemViewType(int position) {

        switch (leaders.size() - 1 - position){ //since the list is actually in ascending order, we count from the tail-end
            case 0:
                return TYPE_G;
            case 1:
                return TYPE_S;
            case 2:
                return TYPE_B;
            default:
                return TYPE_R;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType){
            case TYPE_G:
                view = LayoutInflater.from(activity).inflate(R.layout.leaderboard_gold, parent, false);
                return new GoldViewHolder(view);

            case TYPE_S:
                view = LayoutInflater.from(activity).inflate(R.layout.leaderboard_silver, parent, false);
                return new SilverViewHolder(view);

            case TYPE_B:
                view = LayoutInflater.from(activity).inflate(R.layout.leaderboard_bronze, parent, false);
                return new BronzeViewHolder(view);

            case TYPE_R:
                view = LayoutInflater.from(activity).inflate(R.layout.leaderboard_item, parent, false);
                return new LeaderViewHolder(view);

            default:
                view = LayoutInflater.from(activity).inflate(R.layout.leaderboard_item, parent, false);
                return new LeaderViewHolder(view);
        }
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof LeaderViewHolder){

            LeaderboardEntry leaderboardEntry = leaders.get(position);
            LeaderViewHolder leaderViewHolder = (LeaderViewHolder) holder;

            leaderViewHolder.username.setText(leaderboardEntry.getUsername());
            leaderViewHolder.points.setText(Integer.toString(leaderboardEntry.getPoints())+" influence");
        }
        else if(holder instanceof GoldViewHolder){
            LeaderboardEntry leaderboardEntry = leaders.get(position);
            GoldViewHolder goldViewHolder = (GoldViewHolder) holder;

            goldViewHolder.username.setText(leaderboardEntry.getUsername());
            goldViewHolder.points.setText(Integer.toString(leaderboardEntry.getPoints())+" influence");

            if(leaderboardEntry.getG() > 0){
                goldViewHolder.gcount.setText(Integer.toString(leaderboardEntry.getG()));
            }
            else{
                goldViewHolder.gcount.setText("0");
            }

            if(leaderboardEntry.getS() > 0){
                goldViewHolder.scount.setText(Integer.toString(leaderboardEntry.getS()));
            }
            else{
                goldViewHolder.scount.setText("0");
            }

            if(leaderboardEntry.getB() > 0){
                goldViewHolder.bcount.setText(Integer.toString(leaderboardEntry.getB()));
            }
            else{
                goldViewHolder.bcount.setText("0");
            }

        }
        else if(holder instanceof SilverViewHolder){
            LeaderboardEntry leaderboardEntry = leaders.get(position);
            SilverViewHolder silverViewHolder = (SilverViewHolder) holder;

            silverViewHolder.username.setText(leaderboardEntry.getUsername());
            silverViewHolder.points.setText(Integer.toString(leaderboardEntry.getPoints())+" influence");

            if(leaderboardEntry.getG() > 0){
                silverViewHolder.gcount.setText(Integer.toString(leaderboardEntry.getG()));
            }
            else{
                silverViewHolder.gcount.setText("0");
            }

            if(leaderboardEntry.getS() > 0){
                silverViewHolder.scount.setText(Integer.toString(leaderboardEntry.getS()));
            }
            else{
                silverViewHolder.scount.setText("0");
            }

            if(leaderboardEntry.getB() > 0){
                silverViewHolder.bcount.setText(Integer.toString(leaderboardEntry.getB()));
            }
            else{
                silverViewHolder.bcount.setText("0");
            }

        }
        else if(holder instanceof BronzeViewHolder){
            LeaderboardEntry leaderboardEntry = leaders.get(position);
            BronzeViewHolder bronzeViewHolder = (BronzeViewHolder) holder;

            bronzeViewHolder.username.setText(leaderboardEntry.getUsername());
            bronzeViewHolder.points.setText(Integer.toString(leaderboardEntry.getPoints())+" influence");

            if(leaderboardEntry.getG() > 0){
                bronzeViewHolder.gcount.setText(Integer.toString(leaderboardEntry.getG()));
            }
            else{
                bronzeViewHolder.gcount.setText("0");
            }

            if(leaderboardEntry.getS() > 0){
                bronzeViewHolder.scount.setText(Integer.toString(leaderboardEntry.getS()));
            }
            else{
                bronzeViewHolder.scount.setText("0");
            }

            if(leaderboardEntry.getB() > 0){
                bronzeViewHolder.bcount.setText(Integer.toString(leaderboardEntry.getB()));
            }
            else{
                bronzeViewHolder.bcount.setText("0");
            }

        }

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

    private class GoldViewHolder extends RecyclerView.ViewHolder {

        public TextView username, points, gcount, scount, bcount;

        public GoldViewHolder(View view){
            super(view);
            username = view.findViewById(R.id.gm_username);
            points = view.findViewById(R.id.gm_points);
            gcount = view.findViewById(R.id.gmc_goldmedal_count);
            scount = view.findViewById(R.id.gmc_silvermedal_count);
            bcount = view.findViewById(R.id.gmc_bronzemedal_count);
        }
    }

    private class SilverViewHolder extends RecyclerView.ViewHolder {

        public TextView username, points, gcount, scount, bcount;

        public SilverViewHolder(View view){
            super(view);
            username = view.findViewById(R.id.sm_username);
            points = view.findViewById(R.id.sm_points);
            gcount = view.findViewById(R.id.smc_goldmedal_count);
            scount = view.findViewById(R.id.smc_silvermedal_count);
            bcount = view.findViewById(R.id.smc_bronzemedal_count);
        }
    }

    private class BronzeViewHolder extends RecyclerView.ViewHolder {

        public TextView username, points, gcount, scount, bcount;

        public BronzeViewHolder(View view){
            super(view);
            username = view.findViewById(R.id.bm_username);
            points = view.findViewById(R.id.bm_points);
            gcount = view.findViewById(R.id.bmc_goldmedal_count);
            scount = view.findViewById(R.id.bmc_silvermedal_count);
            bcount = view.findViewById(R.id.bmc_bronzemedal_count);
        }
    }
}