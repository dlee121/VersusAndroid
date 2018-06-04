package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vs.bcd.api.model.LeaderboardModel;
import com.vs.bcd.api.model.LeaderboardModelHitsHitsItem;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.LeaderboardAdapter;
import com.vs.bcd.versus.model.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dlee on 8/6/17.
 */



public class LeaderboardTab extends Fragment {
    private View rootView;
    private ArrayList<LeaderboardEntry> leaders;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private MainContainer activity;
    private long lastRefreshTime = 0;
    RecyclerView recyclerView;
    LeaderboardAdapter mLeaderboardAdapter;
    private ProgressBar lbProgressBar;

    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.leaderboard, container, false);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        leaders = new ArrayList<>();

        recyclerView = rootView.findViewById(R.id.leaderboard_rv);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        mLeaderboardAdapter = new LeaderboardAdapter(leaders, activity);
        recyclerView.setAdapter(mLeaderboardAdapter);

        //recyclerview preloader setup
        ListPreloader.PreloadSizeProvider sizeProvider =
                new FixedPreloadSizeProvider(activity.getResources().getDimensionPixelSize(R.dimen.profile_img_general), activity.getResources().getDimensionPixelSize(R.dimen.profile_img_general));
        RecyclerViewPreloader<LeaderboardEntry> preloader =
                new RecyclerViewPreloader<>(Glide.with(activity), mLeaderboardAdapter, sizeProvider, 20);
        recyclerView.addOnScrollListener(preloader);


        lbProgressBar = rootView.findViewById(R.id.lb_progressbar);

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        //setUpLeaderboard();

        disableChildViews();
        Log.d("leaderboard", "lb set up complete and disablechildViews called");
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //save the activity to a member of this fragment
        activity = (MainContainer)context;
        Log.d("leaderboard", "onAttach() called");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                Log.d("leaderboard", "now visible");
                enableChildViews();
                setUpLeaderboard();
            }
        }
        else {
            if (rootView != null){
                disableChildViews();
            }
        }
    }

    public void enableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));
        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    private void setUpLeaderboard() {
        leaders.clear();
        mLeaderboardAdapter.notifyDataSetChanged();
        lbProgressBar.setVisibility(View.VISIBLE);

        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    /* Execute URL and attach after execution response handler */

                    LeaderboardModel result = activity.getClient().leaderboardGet("lb");

                    List<LeaderboardModelHitsHitsItem> hits = result.getHits().getHits();

                    for (LeaderboardModelHitsHitsItem item : hits) {
                        leaders.add(new LeaderboardEntry(item.getSource(), item.getId()));
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lbProgressBar.setVisibility(View.GONE);
                            mLeaderboardAdapter.notifyDataSetChanged();
                        }
                    });

                    //System.out.println("Response: " + strResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();


    }

}
