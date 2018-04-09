package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.InvitedUserAdapter;
import com.vs.bcd.versus.adapter.UserSearchAdapter;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Created by dlee on 8/6/17.
 */



public class CreateMessage extends Fragment {

    private String FOLLOWERS_CHILD = "";
    private String FOLLOWING_CHILD = "";
    private String H_CHILD = "";
    private final int REQUEST_IMAGE = 2;
    private final int RESULT_OK = -1;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";    //TODO: replace with our own

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAnalytics mFirebaseAnalytics;
    private LinearLayoutManager invitedUsersLLM, userSearchLLM;
    private String mUsername = "";
    private int mPhotoUrl = 0;
    private String userMKey = "";
    private SimpleDateFormat df;
    private MainContainer activity;
    private FloatingActionButton fabNewMsg;
    private ChildEventListener followingListener, followerListener, hListener;

    private RecyclerView invitedUsersRV, userSearchRV;
    private EditText userSearchET;
    private HashSet<String> following, followers, hList, messageContactsCheckSet;
    private ArrayList<UserSearchItem> messageContacts, invitedUsers;
    private InvitedUserAdapter invitedUserAdapter;
    private UserSearchAdapter userSearchAdapter;
    private TextView invitedTV;
    private boolean initialFollowersLoaded = false;
    private boolean initialFollowingLoaded = false;
    private boolean initialHLoaded = false;
    private String currentFilterText = "";
    private CreateMessage thisFragment;

    private Toast mToast;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.create_message, container, false);

        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

        userSearchET = (EditText) rootView.findViewById(R.id.user_search_edittext);
        userSearchET.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
                //you can use runnable postDelayed like 500 ms to delay search text
            }
        });

        invitedTV = (TextView) rootView.findViewById(R.id.invited_tv);

        invitedUsersRV = (RecyclerView) rootView.findViewById(R.id.invited_users_rv);
        invitedUsersLLM = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        invitedUsersLLM.setStackFromEnd(false);
        invitedUsersRV.setLayoutManager(invitedUsersLLM);

        userSearchRV = (RecyclerView) rootView.findViewById(R.id.user_search_rv);
        userSearchLLM = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        userSearchLLM.setStackFromEnd(true);
        userSearchRV.setLayoutManager(userSearchLLM);

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        disableChildViews();

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser(); //TODO: handle possible null object reference error

        userMKey = activity.getUserMKey();
        mPhotoUrl = activity.getProfileImage();

        thisFragment = this;

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        Log.d("ORDER", "CreateMessage OnCreate finished");

        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        messageContacts = new ArrayList<>();
        messageContactsCheckSet = new HashSet<>();
        addHListener();
        //addHListener() finishes and calls addFollowerListener, which finishes and calls addFollowingListener

        Log.d("ORDER", "CreateMessage OnResume called");
    }

    @Override
    public void onPause(){
        super.onPause();
        initialFollowingLoaded = false;
        initialFollowersLoaded = false;
        initialHLoaded = false;

        if(followerListener != null){
            mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).removeEventListener(followerListener);
        }
        if(followingListener != null){
            mFirebaseDatabaseReference.child(FOLLOWING_CHILD).removeEventListener(followingListener);
        }
        if(hListener != null){
            mFirebaseDatabaseReference.child(H_CHILD).removeEventListener(hListener);
        }

        Log.d("ORDER", "CreateMessage Firebase Listeners removed");

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
        SessionManager sessionManager = new SessionManager(context);
        mUsername = sessionManager.getCurrentUsername();

        int usernameHash;
        if(mUsername.length() < 5){
            usernameHash = mUsername.hashCode();
        }
        else{
            String hashIn = "" + mUsername.charAt(0) + mUsername.charAt(mUsername.length() - 2) + mUsername.charAt(1) + mUsername.charAt(mUsername.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        String userPath = Integer.toString(usernameHash) + "/" + sessionManager.getCurrentUsername();
        FOLLOWERS_CHILD = userPath + "/f";
        FOLLOWING_CHILD = userPath + "/g";
        H_CHILD = userPath + "/h";
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                enableChildViews();
            }
        }
        else {
            if (rootView != null){
                disableChildViews();
                if(invitedUsers != null){
                    invitedUsers.clear();
                    userSearchAdapter.clearCheckedItems();
                }
            }
        }
    }

    public void enableChildViews(){
        for(int i = 1; i<childViews.size(); i++){ //start at i = 1, works because invitedTV is the first child of this layout and we skip it in this for-loop
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));
        }
        if(invitedUsers != null && !invitedUsers.isEmpty()){
            showInvitedTV();
        }
        else{
            hideInvitedTV();
        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    void filter(String text){
        currentFilterText = text;

        List<UserSearchItem> tempH = new ArrayList<>();
        for(String mName: hList){
            if(mName.toLowerCase().contains(text.toLowerCase())){
                tempH.add(new UserSearchItem(mName));
            }

        }

        List<UserSearchItem> tempFollowing = new ArrayList<>();
        for(String mName: following){
            if(mName.toLowerCase().contains(text.toLowerCase())){
                tempFollowing.add(new UserSearchItem(mName));
            }
        }

        List<UserSearchItem> tempFollowers = new ArrayList<>();
        for(String mName: followers){
            if(mName.toLowerCase().contains(text.toLowerCase())){
                tempFollowers.add(new UserSearchItem(mName));
            }
        }

        messageContacts = new ArrayList<>();
        for(UserSearchItem h : tempH){
            messageContacts.add(h);
            messageContactsCheckSet.add(h.getUsername());
        }
        for(UserSearchItem g : tempFollowing){
            messageContacts.add(g);
            messageContactsCheckSet.add(g.getUsername());
        }
        for(UserSearchItem f : tempFollowers){
            messageContacts.add(f);
            messageContactsCheckSet.add(f.getUsername());
        }

        //update recyclerview
        userSearchAdapter.updateList(messageContacts);
    }

    public void addToInvitedList(UserSearchItem usi){
        invitedUsers.add(usi);
        invitedUserAdapter.notifyDataSetChanged();
        if(!invitedUsers.isEmpty()){
            showInvitedTV();
        }
    }

    public void removeFromInvitedList(UserSearchItem usi){
        invitedUsers.remove(usi);
        invitedUserAdapter.notifyDataSetChanged();
        if(invitedUsers.isEmpty()){
            hideInvitedTV();
        }
    }

    public void removeFromCheckedItems(String username){
        userSearchAdapter.removeFromCheckedItems(username);
        userSearchAdapter.notifyDataSetChanged();
    }

    private void hideInvitedTV(){
        invitedTV.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }

    private void showInvitedTV(){
        invitedTV.setLayoutParams(LPStore.get(0));  //works because invitedTV is the first child of this layout
    }

    public void createMessageRoom(){
        if(invitedUsers != null && !invitedUsers.isEmpty()){
            //set up a new message room and go into it. actual message room in DB is created with the sending of its first message.
            activity.getMessageRoom().setUpNewRoom(new ArrayList<UserSearchItem>(invitedUsers));
            activity.getViewPager().setCurrentItem(11);
        } else{
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(activity, "Please add recipient(s).", Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    private void addFollowingListener(){

        following = new HashSet<>();

        followingListener = mFirebaseDatabaseReference.child(FOLLOWING_CHILD).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                if(initialFollowingLoaded){ //so this is only for new updates to following list
                    if(!followers.contains(dataSnapshot.getKey())){  //don't add if it's already in Following list
                        following.add(dataSnapshot.getKey());
                        if(userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText)){
                            messageContacts.add(new UserSearchItem(dataSnapshot.getKey()));
                            messageContactsCheckSet.add(dataSnapshot.getKey());
                            userSearchAdapter.updateList(messageContacts);
                            //TODO: test that this case works (when new follower is added and their username passes current text filter, then display them in the recycler view)
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                following.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("MESSENGER", "follower query cancelled");
            }
        });

        mFirebaseDatabaseReference.child(FOLLOWING_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        if(hList.contains(child.getKey())){
                            child.getRef().removeValue();
                        }
                        else if(followers.contains(child.getKey())){
                            mFirebaseDatabaseReference.child(H_CHILD).child(child.getKey()).setValue(child.getValue());
                            child.getRef().removeValue();
                            mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).child(child.getKey()).removeValue();
                        }
                        else {
                            following.add(child.getKey());
                            messageContacts.add(new UserSearchItem(child.getKey()));
                            messageContactsCheckSet.add(child.getKey());
                        }
                    }
                }

                initialFollowingLoaded = true;

                invitedUsers = new ArrayList<>();

                invitedUserAdapter = new InvitedUserAdapter(invitedUsers, getActivity(), thisFragment);
                invitedUsersRV.setAdapter(invitedUserAdapter);

                userSearchAdapter = new UserSearchAdapter(messageContacts, getActivity(), thisFragment);
                userSearchRV.setAdapter(userSearchAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void addFollowerListener(){

        followers = new HashSet<>();  //temp empty list
        //followers = new ArrayList<>(***get realtime instance of followers list in firebase***);

        followerListener = mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                if(initialFollowersLoaded){ //so this is only for new updates to followers list
                    if(!following.contains(dataSnapshot.getKey())){  //don't add if it's already in Following list
                        followers.add(dataSnapshot.getKey());
                        if(userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText)){
                            messageContacts.add(new UserSearchItem(dataSnapshot.getKey()));
                            messageContactsCheckSet.add(dataSnapshot.getKey());
                            userSearchAdapter.updateList(messageContacts);
                            //TODO: test that this case works (when new follower is added and their username passes current text filter, then display them in the recycler view)
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                followers.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("MESSENGER", "follower query cancelled");
            }
        });

        mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChildren()){
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        if(hList.contains(child.getKey())){
                            child.getRef().removeValue();
                        }
                        else{
                            followers.add(child.getKey());
                            messageContacts.add(new UserSearchItem(child.getKey()));
                            messageContactsCheckSet.add(child.getKey());
                        }
                    }
                }

                initialFollowersLoaded = true;
                addFollowingListener();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //HListener listenes to list "h" in firebase, which is a list of users with whom the user has a two-way relationship (following and follower)
    private void addHListener(){
        hList = new HashSet<>();  //temp empty list

        hListener = mFirebaseDatabaseReference.child(H_CHILD).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                if(initialHLoaded){ //so this is only for new updates to followers list
                    hList.add(dataSnapshot.getKey());

                    if(followers.contains(dataSnapshot.getKey())){
                        followers.remove(dataSnapshot.getKey());
                    }

                    if(following.contains(dataSnapshot.getKey())){
                        following.remove(dataSnapshot.getKey());
                    }

                    if(!(messageContactsCheckSet.contains(dataSnapshot.getKey())) && (userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText))){
                        messageContacts.add(new UserSearchItem(dataSnapshot.getKey()));
                        messageContactsCheckSet.add(dataSnapshot.getKey());
                        userSearchAdapter.updateList(messageContacts);
                        //TODO: test that this case works (when new follower is added and their username passes current text filter, then display them in the recycler view)
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                hList.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("MESSENGER", "h query cancelled");
            }
        });

        mFirebaseDatabaseReference.child(H_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChildren()){
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        hList.add(child.getKey());
                        messageContacts.add(new UserSearchItem(child.getKey()));
                        messageContactsCheckSet.add(child.getKey());
                    }
                }

                initialHLoaded = true;
                addFollowerListener();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    public int getFollowingCount(){
        int count = 0;
        if(following != null){
            count += following.size();
        }
        if(hList != null){
            count += hList.size();
        }

        return count;
    }

    public int getFollowerCount(){
        int count = 0;
        if(followers != null){
            count += followers.size();
        }
        if(hList != null){
            count += hList.size();
        }

        return  count;
    }

    public boolean followedBy(String username){
        if(followers != null && hList != null){
            return followers.contains(username) || hList.contains(username);
        }
        Log.d("FOLLOW", "followedBy detected uninitialized followers and/or hList");
        return false;
    }

    public boolean isFollowing(String username){
        if(following != null && hList != null){
            return following.contains(username) || hList.contains(username);
        }
        return false;
    }

    //returns empty string if not DM
    public String getDMTarget(){
        if(invitedUsers != null && invitedUsers.size() == 1){
            return invitedUsers.get(0).getUsername();
        }
        return "";
    }

}
