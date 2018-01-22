package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CommentHistoryAdapter;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.VSComment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dlee on 4/29/17.
 */

public class ProfileTab extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private MainContainer activity;
    private TextView usernameTV, goldTV, silverTV, bronzeTV, pointsTV, followingTextTV, followerCountTV, followingCountTV;
    private Button followButton;
    private ProgressBar progressBar;
    private LinearLayout mainCase, followCase, medalCase;
    private TabLayout tabLayout;
    private RelativeLayout.LayoutParams mainCaseLP, followCaseLP, medalCaseLP, progressbarLP, swipeLayoutLP, tabsLP;
    private LinearLayout.LayoutParams followingtextLP, followbuttonLP;
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView recyclerView;
    private int postRetrievalLimit = 20;
    private int commentRetrievalLimit = 20;
    private int commentsORposts = 0;    //0 = comments, 1 = posts
    private final int COMMENTS = 0;
    private final int POSTS = 1;
    private String profileUsername = null; //username of the user for the profile page, not necessarily the current logged-in user
    private DatabaseReference mFirebaseDatabaseReference;
    private long followingCount = 0;
    private long followerCount = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.profile, container, false);

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressbar_pt);
        progressbarLP = (RelativeLayout.LayoutParams) progressBar.getLayoutParams();

        mainCase = (LinearLayout) rootView.findViewById(R.id.maincase);
        followCase = (LinearLayout) rootView.findViewById(R.id.follow_case);
        medalCase = (LinearLayout) rootView.findViewById(R.id.medal_case);

        mainCaseLP = (RelativeLayout.LayoutParams) mainCase.getLayoutParams();
        followCaseLP = (RelativeLayout.LayoutParams) followCase.getLayoutParams();
        medalCaseLP = (RelativeLayout.LayoutParams) medalCase.getLayoutParams();

        usernameTV = (TextView) rootView.findViewById(R.id.username_pt);
        goldTV = (TextView) rootView.findViewById(R.id.gmedal_pt);
        silverTV = (TextView) rootView.findViewById(R.id.smedal_pt);
        bronzeTV = (TextView) rootView.findViewById(R.id.bmedal_pt);
        pointsTV = (TextView) rootView.findViewById(R.id.points_pt);

        followerCountTV = (TextView) rootView.findViewById(R.id.num_followers);
        followingCountTV = (TextView) rootView.findViewById(R.id.num_following);

        followingTextTV = (TextView) rootView.findViewById(R.id.followingtext);
        followingtextLP = (LinearLayout.LayoutParams) followingTextTV.getLayoutParams();

        followButton = (Button) rootView.findViewById(R.id.followbutton);
        followbuttonLP = (LinearLayout.LayoutParams) followButton.getLayoutParams();
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                followThisUser();
            }
        });


        tabLayout = (TabLayout) rootView.findViewById(R.id.tabs_profile);
        tabLayout.addTab(tabLayout.newTab().setText("COMMENTS"), true);
        tabLayout.addTab(tabLayout.newTab().setText("POSTS"));
        tabsLP = (RelativeLayout.LayoutParams) tabLayout.getLayoutParams();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: //comments history
                        if(commentsORposts == POSTS && profileUsername != null){
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    setUpComments(profileUsername);
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }
                        commentsORposts = COMMENTS;
                        break;
                    case 1: //posts history
                        if(commentsORposts == COMMENTS && profileUsername != null){
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    setUpPosts(profileUsername);
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }
                        commentsORposts = POSTS;
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container_profile);
        swipeLayoutLP = (RelativeLayout.LayoutParams) mSwipeRefreshLayout.getLayoutParams();
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_profile);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        disableChildViews();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer)context;
    }

    @Override
    public void onRefresh() {
        //TODO: do a refresh operation here
    }

    //for accessing another user's profile page
    public void setUpProfile(final String username, boolean myProfile){

        /* don't need to delete because we hide layout and show layout when it's ready with updated info
        if(recyclerView != null && recyclerView.getAdapter() != null){
            switch (commentsORposts){
                case COMMENTS:
                    ((PostPageAdapter)recyclerView.getAdapter()).clearList();
                    break;
                case POSTS:
                    ((MyAdapter)recyclerView.getAdapter()).clearList();
                    break;
            }
        }
        */

        profileUsername = username;

        displayLoadingScreen(); //hide all page content and show refresh animation during loading, no other UI element

        if(myProfile){
            //this is setting up the profile page for the logged-in user, as in "Me" page
            //disable toolbarButtonLeft
            //use projection attribute to reduce network traffic; get posts list and comments list from SharedPref
                //so only grab: num_g, num_s, num_b, points

            Log.d("ptab", "setting up my profile");

            hideFollowUI();

            Runnable runnable = new Runnable() {
                public void run() {
                    setUpComments(username);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getGSBP(activity.getUserPath());
                            usernameTV.setText(username);
                            followingCountTV.setText(Integer.toString(activity.getFollowingNum()) + "\nFollowing");
                            followerCountTV.setText(Integer.toString(activity.getFollowerNum()) + "\nFollowers");
                            displayMyProfile();
                        }
                    });
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();

        }
        else{
            //this is setting up the profile page for another user that the logged-in user clicked on
            //enable toolbarButtonLeft and set it to "x" or "<" and set it to go back to the page that user came from
            //use projection attribute to exclude private info.
                //so only grab: comments list, posts list, first name, last name, num_g, num_s, num_b, points
            Log.d("ptab", "setting up another user's profile");

            Log.d("ptab", "setting up my profile");

            if(activity.isFollowing(username)){
                showFollowingText();
            }
            else{
                showFollowButton();
            }

            Runnable runnable = new Runnable() {
                public void run() {

                    setUpComments(username);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int usernameHash;
                            if(username.length() < 5){
                                usernameHash = username.hashCode();
                            }
                            else{
                                String hashIn = "" + username.charAt(0) + username.charAt(username.length() - 2) + username.charAt(1) + username.charAt(username.length() - 1);
                                usernameHash = hashIn.hashCode();
                            }
                            String userPath = Integer.toString(usernameHash) + "/" + username + "/";
                            getGSBP(userPath);
                            usernameTV.setText(username);
                            getFGHCounts();
                        }
                    });
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();
        }
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isVisibleToUser) { //we don't care about case of isVisibleToUser==true because that case is handled by setUpProfile()
            if(rootView != null) {
                commentsORposts = COMMENTS;
                tabLayout.getTabAt(0).select();
                disableChildViews();
            }
        }
    }


    private void displayLoadingScreen(){
        progressBar.setEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setLayoutParams(progressbarLP);
        mainCase.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        followCase.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        medalCase.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        tabLayout.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        mSwipeRefreshLayout.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }

    private void displayMyProfile(){
        progressBar.setEnabled(false);
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        mainCase.setLayoutParams(mainCaseLP);
        followCase.setLayoutParams(followCaseLP);
        medalCase.setLayoutParams(medalCaseLP);

        tabLayout.setEnabled(true);
        tabLayout.setClickable(true);
        tabLayout.setLayoutParams(tabsLP);

        mSwipeRefreshLayout.setLayoutParams(swipeLayoutLP);
        mSwipeRefreshLayout.setEnabled(true);
        //TODO: does mSwipeRefreshLayout need setClickable(true) as well?
        //activate ME specific UI

    }

    private void displayOtherProfile(){
        progressBar.setEnabled(false);
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        mainCase.setLayoutParams(mainCaseLP);
        followCase.setLayoutParams(followCaseLP);
        medalCase.setLayoutParams(medalCaseLP);

        tabLayout.setEnabled(true);
        tabLayout.setClickable(true);
        tabLayout.setLayoutParams(tabsLP);

        mSwipeRefreshLayout.setLayoutParams(swipeLayoutLP);
        mSwipeRefreshLayout.setEnabled(true);
        //TODO: does mSwipeRefreshLayout need setClickable(true) as well?
        //activate Other User Profile specific UI

    }

    private void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    //restore my profile
    public void restoreUI(){
        activity.meClickTrue();

        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(LPStore.get(i));
        }
    }

    private void setUpPosts(String username){

        Log.d("profilesetup", "setUpPosts called");

        commentsORposts = POSTS;

        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("-1"));

        Post queryTemplate = new Post();
        queryTemplate.setAuthor(username);


        DynamoDBQueryExpression queryExpression =
                new DynamoDBQueryExpression()
                        .withIndexName("author-votecount-index")
                        .withHashKeyValues(queryTemplate)
                        .withRangeKeyCondition("votecount", rangeKeyCondition)
                        .withScanIndexForward(false)
                        .withConsistentRead(false)
                        .withLimit(postRetrievalLimit);

        List<Post> posts = activity.getMapper().query(Post.class, queryExpression);

        final MyAdapter postsAdapter = new MyAdapter(recyclerView, posts, activity, 9);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(postsAdapter);
            }
        });


    }

    private void setUpComments(String username){
        commentsORposts = COMMENTS;

        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("-1"));

        VSComment queryTemplate = new VSComment();
        queryTemplate.setAuthor(username);


        DynamoDBQueryExpression queryExpression =
                new DynamoDBQueryExpression()
                        .withIndexName("author-upvotes-index")
                        .withHashKeyValues(queryTemplate)
                        .withRangeKeyCondition("upvotes", rangeKeyCondition)
                        .withScanIndexForward(false)
                        .withConsistentRead(false)
                        .withLimit(commentRetrievalLimit);

        final List<VSComment> comments = activity.getMapper().query(VSComment.class, queryExpression);

        //final PostPageAdapter commentsAdapter = new PostPageAdapter(recyclerView, comments, new Post(), activity, false, false);   //the Post item here is just a dummy, there to prevent null-related errors just in case
        final CommentHistoryAdapter commentsAdapter = new CommentHistoryAdapter(comments, activity);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(commentsAdapter);
            }
        });

    }


    private void showFollowingText(){
        followingTextTV.setEnabled(true);
        followingTextTV.setVisibility(View.VISIBLE);
        followingTextTV.setLayoutParams(followingtextLP);

        followButton.setEnabled(false);
        followButton.setClickable(false);
        followButton.setVisibility(View.INVISIBLE);
        followButton.setLayoutParams(new LinearLayout.LayoutParams(0,0));
    }

    private void showFollowButton(){
        followingTextTV.setEnabled(false);
        followingTextTV.setVisibility(View.INVISIBLE);
        followingTextTV.setLayoutParams(new LinearLayout.LayoutParams(0,0));

        followButton.setEnabled(true);
        followButton.setClickable(true);
        followButton.setVisibility(View.VISIBLE);
        followButton.setLayoutParams(followbuttonLP);
    }

    private void hideFollowUI(){
        followingTextTV.setEnabled(false);
        followingTextTV.setVisibility(View.INVISIBLE);
        followingTextTV.setLayoutParams(new LinearLayout.LayoutParams(0,0));

        followButton.setEnabled(false);
        followButton.setClickable(false);
        followButton.setVisibility(View.INVISIBLE);
        followButton.setLayoutParams(new LinearLayout.LayoutParams(0,0));
    }

    private void followThisUser(){
        if(profileUsername != null){
            if(activity.followedBy(profileUsername)) {   //add to h

                //add to current user's h list
                String userHPath = activity.getUserPath() + "h";
                mFirebaseDatabaseReference.child(userHPath).child(profileUsername)
                        .setValue(true, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Log.w("MESSENGER", "Unable to update followings list in Firebase.");
                                }
                            }
                        });

                //remove old entry in f list now that we have it in h list
                String fPath = activity.getUserPath() + "f";
                mFirebaseDatabaseReference.child(fPath).child(profileUsername).removeValue();

                //update the followed user's h list
                int usernameHash;
                if(profileUsername.length() < 5){
                    usernameHash = profileUsername.hashCode();
                }
                else{
                    String hashIn = "" + profileUsername.charAt(0) + profileUsername.charAt(profileUsername.length() - 2) + profileUsername.charAt(1) + profileUsername.charAt(profileUsername.length() - 1);
                    usernameHash = hashIn.hashCode();
                }
                String hPath = Integer.toString(usernameHash) + "/" + profileUsername + "/h";
                mFirebaseDatabaseReference.child(hPath).child(activity.getUsername())
                        .setValue(true, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError == null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendFollowNotification(profileUsername);
                                            showFollowingText();
                                            updateProfileInfo();
                                        }
                                    });
                                } else {
                                    Log.w("MESSENGER", "Unable to update followers list in Firebase.");
                                }
                            }
                        });

                //remove old entry in g list now that we have it in h list
                String gPath = Integer.toString(usernameHash) + "/" + profileUsername + "/g";
                mFirebaseDatabaseReference.child(gPath).child(activity.getUsername()).removeValue();

            }
            else{   //add to f and g

                //update the current user's following list in Firebase
                String followingsPath = activity.getUserPath() + "g";
                mFirebaseDatabaseReference.child(followingsPath).child(profileUsername)
                        .setValue(true, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Log.w("MESSENGER", "Unable to update followings list in Firebase.");
                                }
                            }
                        });


                //update the followed user's follower list in Firebase
                int usernameHash;
                if(profileUsername.length() < 5){
                    usernameHash = profileUsername.hashCode();
                }
                else{
                    String hashIn = "" + profileUsername.charAt(0) + profileUsername.charAt(profileUsername.length() - 2) + profileUsername.charAt(1) + profileUsername.charAt(profileUsername.length() - 1);
                    usernameHash = hashIn.hashCode();
                }
                String followersPath = Integer.toString(usernameHash) + "/" + profileUsername + "/f";
                mFirebaseDatabaseReference.child(followersPath).child(activity.getUsername())
                        .setValue(true, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError == null) {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendFollowNotification(profileUsername);
                                            showFollowingText();
                                            updateProfileInfo();
                                        }
                                    });
                                } else {
                                    Log.w("MESSENGER", "Unable to update followers list in Firebase.");
                                }
                            }
                        });

            }
        }
    }

    //only use for updating points, following count, and follower count for other users' profile. not for use with myProfile
    private void updateProfileInfo(){
        //only called when we follow another user, meaning the profile page is displaying another user when this function is called
        //that means we can use getFGHCounts(), which uses displayOtherProfile() and not displayMyProfile(), to update the follower count and following count
        Runnable runnable = new Runnable() {
            public void run() {

                if(profileUsername == null){
                    return;
                }
                HashMap<String, AttributeValue> keyMap =
                        new HashMap<>();
                keyMap.put("username", new AttributeValue().withS(profileUsername));  //partition key

                GetItemRequest request = new GetItemRequest()
                        .withTableName("user")
                        .withKey(keyMap)
                        .withProjectionExpression("points");
                GetItemResult result = activity.getDDBClient().getItem(request);

                final Map<String, AttributeValue> resultMap = result.getItem();

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        usernameTV.setText(profileUsername);

                        for (Map.Entry<String, AttributeValue> entry : resultMap.entrySet()) {
                            String attrName = entry.getKey();
                            if(attrName.equals("points")){
                                pointsTV.setText(entry.getValue().getN());
                            }
                        }

                        getFGHCounts();

                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }

    //get counts for following and follower
    private void getFGHCounts(){

        followingCount = 0;
        followerCount = 0;

        int usernameHash;
        if(profileUsername.length() < 5){
            usernameHash = profileUsername.hashCode();
        }
        else{
            String hashIn = "" + profileUsername.charAt(0) + profileUsername.charAt(profileUsername.length() - 2) + profileUsername.charAt(1) + profileUsername.charAt(profileUsername.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        final String fPath = usernameHash + "/" + profileUsername + "/f";
        final String gPath = usernameHash + "/" + profileUsername + "/g";
        final String hPath = usernameHash + "/" + profileUsername + "/h";

        mFirebaseDatabaseReference.child(fPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                followerCount += dataSnapshot.getChildrenCount();

                mFirebaseDatabaseReference.child(gPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        followingCount += dataSnapshot.getChildrenCount();

                        mFirebaseDatabaseReference.child(hPath).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                followerCount += dataSnapshot.getChildrenCount();
                                followingCount += dataSnapshot.getChildrenCount();

                                followerCountTV.setText(Long.toString(followerCount) + "\nFollowers");
                                followingCountTV.setText(Long.toString(followingCount) + "\nFollowing");

                                displayOtherProfile();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    //get medal counts and points
    private void getGSBP(String userPath){

        final String medalsPath = userPath + "w";
        final String pointsPath = userPath + "p";

        mFirebaseDatabaseReference.child(medalsPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("g")){
                    goldTV.setText(dataSnapshot.child("g").getValue(Integer.class).toString());
                }
                else{
                    goldTV.setText(Integer.toString(0));
                }

                if(dataSnapshot.hasChild("s")){
                    silverTV.setText(dataSnapshot.child("s").getValue(Integer.class).toString());
                }
                else{
                    silverTV.setText(Integer.toString(0));
                }

                if(dataSnapshot.hasChild("b")){
                    bronzeTV.setText(dataSnapshot.child("b").getValue(Integer.class).toString());
                }
                else{
                    bronzeTV.setText(Integer.toString(0));
                }

                mFirebaseDatabaseReference.child(pointsPath).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String strIn;
                        if(dataSnapshot.getValue() != null){
                            strIn = dataSnapshot.getValue(Integer.class).toString() + " points";
                            pointsTV.setText(strIn);
                        }
                        else{
                            strIn = Integer.toString(0) + " points";
                            pointsTV.setText(strIn);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void clearProfilePage(){
        goldTV.setText("");
        silverTV.setText("");
        bronzeTV.setText("");
        pointsTV.setText("");
    }

    private void sendFollowNotification(String fUsername){
        int usernameHash;
        if(fUsername.length() < 5){
            usernameHash = fUsername.hashCode();
        }
        else{
            String hashIn = "" + fUsername.charAt(0) + fUsername.charAt(fUsername.length() - 2) + fUsername.charAt(1) + fUsername.charAt(fUsername.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        String fUserNFPath = Integer.toString(usernameHash) + "/" + fUsername + "/n/f/" + activity.getUsername();
        mFirebaseDatabaseReference.child(fUserNFPath).setValue(System.currentTimeMillis()/1000);    //set value as timestamp as seconds from epoch

    }


}

