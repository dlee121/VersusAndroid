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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CategoriesAdapter;
import com.vs.bcd.versus.adapter.LeaderboardAdapter;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.ActivePost;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.LeaderboardEntry;
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
    private ArrayList<LeaderboardEntry> leaders;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private MainContainer activity;
    private long lastRefreshTime = 0;
    private LeaderboardTab thisTab;
    RecyclerView recyclerView;
    LeaderboardAdapter mLeaderboardAdapter;

    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.leaderboard, container, false);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        leaders = new ArrayList<LeaderboardEntry>();

        recyclerView = (RecyclerView) rootView.findViewById(R.id.leaderboard_rv);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);

        mLeaderboardAdapter = new LeaderboardAdapter(leaders, activity);
        recyclerView.setAdapter(mLeaderboardAdapter);

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

        Query query = mFirebaseDatabaseReference.child("leaderboard").orderByValue().limitToLast(100);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    leaders.clear();
                    for(DataSnapshot item : dataSnapshot.getChildren()){
                        leaders.add(new LeaderboardEntry(item.getKey(), item.getValue(Integer.class)));
                        //Log.d("leaderboard", Integer.toString(i) + "\t"+item.getKey()+"\t"+Integer.toString(item.getValue(Integer.class)));
                    }
                    mLeaderboardAdapter.notifyDataSetChanged();
                }
                lastRefreshTime = System.currentTimeMillis();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
