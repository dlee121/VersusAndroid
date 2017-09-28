package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CategoriesAdapter;
import com.vs.bcd.versus.adapter.LeaderboardAdapter;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.ActivePost;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.ThreadCounter;
import com.vs.bcd.versus.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by dlee on 8/6/17.
 */



public class LeaderboardTab extends Fragment {
    private View rootView;
    private ArrayList<User> leaders;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private MainContainer activity;
    private boolean displayResults = false;
    private int numTimecodes = 10;
    private int retrievalLimit = 15;
    private long lastRefreshTime = 0;
    private LeaderboardTab thisTab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.leaderboard, container, false);


        //RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.leaderboard_rv);
        //recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        //mLeaderboardAdapter = new LeaderboardAdapter(leaders, getActivity());
        //recyclerView.setAdapter(mLeaderboardAdapter);

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
        thisTab = this;
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


    private void setUpLeaderboard(){
        if(System.currentTimeMillis() < lastRefreshTime + 30 * 1000){   //if it's been less than 30 seconds since last leaderboard refresh, return instead of querying
            return;
        }

        Runnable runnable = new Runnable() {
            public void run() {

                Log.d("leaderboard", "lb setup called");

                final ArrayList<User> assembledResults = new ArrayList<>();
                final Condition rangeKeyCondition = new Condition()
                        .withComparisonOperator(ComparisonOperator.GT.toString())
                        .withAttributeValueList(new AttributeValue().withN("-1"));

                final ThreadCounter threadCounter = new ThreadCounter(0, numTimecodes, thisTab);
                for(int i = 0; i <  numTimecodes; i++){
                    final int index = i;

                    Runnable runnable = new Runnable() {
                        public void run() {
                            User queryTemplate = new User();
                            queryTemplate.setTimecode(index);
                            //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                            DynamoDBQueryExpression queryExpression =
                                    new DynamoDBQueryExpression()
                                            .withHashKeyValues(queryTemplate)
                                            .withRangeKeyCondition("points", rangeKeyCondition)
                                            .withScanIndexForward(false)
                                            .withConsistentRead(false)
                                            .withLimit(retrievalLimit);

                            ArrayList<User> queryResults = new ArrayList<>(activity.getMapper().queryPage(User.class, queryExpression).getResults());
                            assembledResults.addAll(queryResults);

                            Log.d("Query on Timecode: ", Integer.toString(queryTemplate.getTimecode()));

                            threadCounter.increment();
                        }
                    };
                    Thread mythread = new Thread(runnable);
                    mythread.start();
                }

                long end = System.currentTimeMillis() + 6*1000; // 6 seconds * 1000 ms/sec
                //automatic timeout at 10 seconds to prevent infinite loop
                while(!displayResults && System.currentTimeMillis() < end){

                }
                if(displayResults){
                    Log.d("Query on Timecode: ", "got through");
                }
                else{
                    Log.d("Query on Timecode: ", "loop timeout");
                }

                displayResults = false;

                //sort the assembledResults where posts are sorted from more recent to less recent
                Collections.sort(assembledResults, new Comparator<User>() {
                    //TODO: confirm that this sorts dates where most recent is at top. If not then just flip around o1 and o2: change to o2.getDate().compareTo(o1.getDate())
                    @Override
                    public int compare(User o1, User o2) {

                        return  o2.getPoints() - o1.getPoints();
                    }
                });


                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.leaderboard_rv);
                        leaders = new ArrayList<User>(assembledResults);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                        LeaderboardAdapter mLeaderboardAdapter = new LeaderboardAdapter(leaders, activity);
                        recyclerView.setAdapter(mLeaderboardAdapter);
                        lastRefreshTime = System.currentTimeMillis();
                    }
                });




            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }

    public void yesDisplayResults(){
        displayResults = true;
    }

}
