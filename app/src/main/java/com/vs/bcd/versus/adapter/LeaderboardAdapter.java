package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.GlideUrlCustom;
import com.vs.bcd.versus.model.LeaderboardEntry;
import com.vs.bcd.versus.model.Post;

import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ListPreloader.PreloadModelProvider<LeaderboardEntry>{
    private MainContainer activity;
    private List<LeaderboardEntry> leaders;

    private final int TYPE_G = 0; //first place
    private final int TYPE_S = 1; //second place
    private final int TYPE_B = 2; //third place
    private final int TYPE_R = 3; //all other ranked users

    public LeaderboardAdapter(List<LeaderboardEntry> leaders, MainContainer activity) {
        this.leaders = leaders;
        this.activity = activity;
    }

    @Override
    public int getItemViewType(int position) {

        switch (position){
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
            leaderViewHolder.influence.setText(Integer.toString(leaderboardEntry.getInfluence())+" influence");
            leaderViewHolder.rank.setText(Integer.toString(position + 1));

            if(leaderboardEntry.getG() > 0){
                leaderViewHolder.gcount.setText(Integer.toString(leaderboardEntry.getG()));
            }
            else{
                leaderViewHolder.gcount.setText("0");
            }

            if(leaderboardEntry.getS() > 0){
                leaderViewHolder.scount.setText(Integer.toString(leaderboardEntry.getS()));
            }
            else{
                leaderViewHolder.scount.setText("0");
            }

            if(leaderboardEntry.getB() > 0){
                leaderViewHolder.bcount.setText(Integer.toString(leaderboardEntry.getB()));
            }
            else{
                leaderViewHolder.bcount.setText("0");
            }

            if (leaderboardEntry.getPi() != 0) {
                try {
                    Glide.with(activity).load(activity.getProfileImgUrl(leaderboardEntry.getUsername(), leaderboardEntry.getPi())).into(leaderViewHolder.profileImage);
                } catch (Exception e) {
                    leaderViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
                }
            } else {
                leaderViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
            }

        }
        else if(holder instanceof GoldViewHolder){
            LeaderboardEntry leaderboardEntry = leaders.get(position);
            GoldViewHolder goldViewHolder = (GoldViewHolder) holder;

            goldViewHolder.username.setText(leaderboardEntry.getUsername());
            goldViewHolder.influence.setText(Integer.toString(leaderboardEntry.getInfluence())+" influence");

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

            if (leaderboardEntry.getPi() != 0) {
                try {
                    Glide.with(activity).load(activity.getProfileImgUrl(leaderboardEntry.getUsername(), leaderboardEntry.getPi())).into(goldViewHolder.profileImage);
                } catch (Exception e) {
                    goldViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
                }
            } else {
                goldViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
            }

        }
        else if(holder instanceof SilverViewHolder){
            LeaderboardEntry leaderboardEntry = leaders.get(position);
            SilverViewHolder silverViewHolder = (SilverViewHolder) holder;

            silverViewHolder.username.setText(leaderboardEntry.getUsername());
            silverViewHolder.influence.setText(Integer.toString(leaderboardEntry.getInfluence())+" influence");

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

            if (leaderboardEntry.getPi() != 0) {
                try {
                    Glide.with(activity).load(activity.getProfileImgUrl(leaderboardEntry.getUsername(), leaderboardEntry.getPi())).into(silverViewHolder.profileImage);
                } catch (Exception e) {
                    silverViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
                }
            } else {
                silverViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
            }

        }
        else if(holder instanceof BronzeViewHolder){
            LeaderboardEntry leaderboardEntry = leaders.get(position);
            BronzeViewHolder bronzeViewHolder = (BronzeViewHolder) holder;

            bronzeViewHolder.username.setText(leaderboardEntry.getUsername());
            bronzeViewHolder.influence.setText(Integer.toString(leaderboardEntry.getInfluence())+" influence");

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

            if (leaderboardEntry.getPi() != 0) {
                try {
                    Glide.with(activity).load(activity.getProfileImgUrl(leaderboardEntry.getUsername(), leaderboardEntry.getPi())).into(bronzeViewHolder.profileImage);
                } catch (Exception e) {
                    bronzeViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
                }
            } else {
                bronzeViewHolder.profileImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_profile));
            }

        }

    }

    @Override
    public int getItemCount() {
        return leaders == null ? 0 : leaders.size();
    }

    private class LeaderViewHolder extends RecyclerView.ViewHolder {

        public TextView username, influence, rank, gcount, scount, bcount;
        public CircleImageView profileImage;

        public LeaderViewHolder(View view) {
            super(view);
            profileImage = view.findViewById(R.id.profile_image_lb);
            username = view.findViewById(R.id.lb_username);
            influence = view.findViewById(R.id.lb_influence);
            rank = view.findViewById(R.id.rank);
            gcount = view.findViewById(R.id.lb_goldmedal_count);
            scount = view.findViewById(R.id.lb_silvermedal_count);
            bcount = view.findViewById(R.id.lb_bronzemedal_count);
        }
    }

    private class GoldViewHolder extends RecyclerView.ViewHolder {

        public TextView username, influence, gcount, scount, bcount;
        public CircleImageView profileImage;

        public GoldViewHolder(View view){
            super(view);
            profileImage = view.findViewById(R.id.profile_image_gm);
            username = view.findViewById(R.id.gm_username);
            influence = view.findViewById(R.id.gm_influence);
            gcount = view.findViewById(R.id.gmc_goldmedal_count);
            scount = view.findViewById(R.id.gmc_silvermedal_count);
            bcount = view.findViewById(R.id.gmc_bronzemedal_count);
        }
    }

    private class SilverViewHolder extends RecyclerView.ViewHolder {

        public TextView username, influence, gcount, scount, bcount;
        public CircleImageView profileImage;

        public SilverViewHolder(View view){
            super(view);
            profileImage = view.findViewById(R.id.profile_image_sm);
            username = view.findViewById(R.id.sm_username);
            influence = view.findViewById(R.id.sm_influence);
            gcount = view.findViewById(R.id.smc_goldmedal_count);
            scount = view.findViewById(R.id.smc_silvermedal_count);
            bcount = view.findViewById(R.id.smc_bronzemedal_count);
        }
    }

    private class BronzeViewHolder extends RecyclerView.ViewHolder {

        public TextView username, influence, gcount, scount, bcount;
        public CircleImageView profileImage;

        public BronzeViewHolder(View view){
            super(view);
            profileImage = view.findViewById(R.id.profile_image_bm);
            username = view.findViewById(R.id.bm_username);
            influence = view.findViewById(R.id.bm_influence);
            gcount = view.findViewById(R.id.bmc_goldmedal_count);
            scount = view.findViewById(R.id.bmc_silvermedal_count);
            bcount = view.findViewById(R.id.bmc_bronzemedal_count);
        }
    }

    @Override
    @NonNull
    public List<LeaderboardEntry> getPreloadItems(int position) {
        try{//TODO: eventually we wanna fix the bug that's causing the exception
            return Collections.singletonList(leaders.get(position));
        }
        catch (Throwable t){
            return Collections.emptyList();
        }
    }

    @Override
    @Nullable
    public RequestBuilder getPreloadRequestBuilder(LeaderboardEntry entry) {
        try{
            int profileImg;
            profileImg = entry.getPi();
            if(profileImg == 0){
                return null;
            }
            return GlideApp.with(activity).load( activity.getProfileImgUrl(entry.getUsername(), entry.getPi()) );

        }
        catch (Throwable t){

        }

        return null;
    }
}