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
import android.widget.RelativeLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.LeaderboardAdapter;
import com.vs.bcd.versus.model.LeaderboardEntry;

import java.util.ArrayList;

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

    private void getMedalCount(String username, final int index){
        //so in the adapter, if medal counts are -1, then we put a progress bar instead of a medal display case.
        //then when this function finishes and gets the medal count,
        // then we first update the gold/silver/bronze leader's medal count and notifyItemChanged(index),
        // which will call onBindViewHolder on that item and it'll see medal count is not -1 anymore and will display the proper counts in medal display case
        //   ^^but does it though? maybe we gotta call a function to disable the progressbar and bring out the display case


        String userPath = getUsernameHash(username) + "/" + username + "/";
        String medalPath = userPath+"w";
        mFirebaseDatabaseReference.child(medalPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LeaderboardEntry entry = leaders.get(index);
                int g = 0;
                int s = 0;
                int b = 0;

                if(dataSnapshot.hasChild("g")){
                    g = dataSnapshot.child("g").getValue(Integer.class);
                }

                if(dataSnapshot.hasChild("s")){
                    s = dataSnapshot.child("s").getValue(Integer.class);
                }

                if(dataSnapshot.hasChild("b")){
                    b = dataSnapshot.child("b").getValue(Integer.class);
                }

                entry.setMedalCount(g, s, b);

                leaders.set(index, entry);
                mLeaderboardAdapter.notifyItemChanged(index);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // ...
            }
        });
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
                    int index = 0;
                    leaders.clear();
                    for(DataSnapshot item : dataSnapshot.getChildren()){
                        Log.d("dataSnapShotCCCheck", "Count: " + Integer.toString(((int)dataSnapshot.getChildrenCount())));

                        //since we only get 100 children, we can safely cast dataSnapshot.getChildrenCount(), which returns a long, into int
                        switch (((int)dataSnapshot.getChildrenCount()) - 1 - index){ //since the list is actually in ascending order, we count from the tail-end
                            case 0:
                                getMedalCount(item.getKey(), index);
                                leaders.add(new LeaderboardEntry(item.getKey(), item.getValue(Integer.class), -1, -1, -1));
                                break;

                            case 1:
                                getMedalCount(item.getKey(), index);
                                leaders.add(new LeaderboardEntry(item.getKey(), item.getValue(Integer.class), -1, -1, -1));
                                break;

                            case 2:
                                getMedalCount(item.getKey(), index);
                                leaders.add(new LeaderboardEntry(item.getKey(), item.getValue(Integer.class), -1, -1, -1));
                                break;

                            default:
                                leaders.add(new LeaderboardEntry(item.getKey(), item.getValue(Integer.class)));
                                break;
                        }

                        index++;

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

    private String getUsernameHash(String usernameIn){
        int usernameHash;
        if(usernameIn.length() < 5){
            usernameHash = usernameIn.hashCode();
        }
        else{
            String hashIn = "" + usernameIn.charAt(0) + usernameIn.charAt(usernameIn.length() - 2) + usernameIn.charAt(1) + usernameIn.charAt(usernameIn.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        return Integer.toString(usernameHash);
    }

}
