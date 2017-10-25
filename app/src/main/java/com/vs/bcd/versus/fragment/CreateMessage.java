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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by dlee on 8/6/17.
 */



public class CreateMessage extends Fragment {

    private String FOLLOWERS_CHILD = "";
    private String FOLLOWING_CHILD = "";
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
    private String mPhotoUrl = "";
    private String userMKey = "";
    private SimpleDateFormat df;
    private MainContainer activity;
    private FloatingActionButton fabNewMsg;

    private RecyclerView invitedUsersRV, userSearchRV;
    private EditText userSearchET;
    private HashMap<String, String> following, followers;
    private ArrayList<UserSearchItem> messageContacts, invitedUsers;
    private InvitedUserAdapter invitedUserAdapter;
    private UserSearchAdapter userSearchAdapter;
    private TextView invitedTV;
    private boolean initialFollowersLoaded = false;
    private boolean initialFollowingLoaded = false;
    private String currentFilterText = "";
    private HashSet<String> localFW;
    private CreateMessage thisFragment;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.create_message, container, false);

        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

        localFW = activity.getLocalFns();

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
        mPhotoUrl = activity.getProfileImageURL();

        thisFragment = this;

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        //TODO: get fresh followers list from firebase
        followers = new HashMap<>();  //temp empty list
        //followers = new ArrayList<>(***get realtime instance of followers list in firebase***);

        mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if(!localFW.contains(child.getKey())){  //don't add if it's already in Following list
                        followers.put(child.getKey(), child.getValue(String.class));
                    }
                }
                initialFollowersLoaded = true;
                finishSetUp();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                if(initialFollowersLoaded){ //so this is only for new updates to followers list
                    if(!following.containsKey(dataSnapshot.getKey())){  //don't add if it's already in Following list
                        followers.put(dataSnapshot.getKey(), dataSnapshot.getValue(String.class));
                        if(userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText)){
                            messageContacts.add(new UserSearchItem(dataSnapshot.getKey(), dataSnapshot.getValue(String.class)));
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

        return rootView;
    }

    private void finishSetUp(){

        messageContacts = new ArrayList<>();

        following = new HashMap<>();

        mFirebaseDatabaseReference.child(FOLLOWING_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        following.put(child.getKey(), child.getValue(String.class));
                    }
                    for(Map.Entry<String, String> entry : following.entrySet()){
                        messageContacts.add(new UserSearchItem(entry.getKey(), entry.getValue()));
                    }
                }

                initialFollowingLoaded = true;

                for(Map.Entry<String, String> entry : followers.entrySet()){
                    messageContacts.add(new UserSearchItem(entry.getKey(), entry.getValue()));
                }

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
        mFirebaseDatabaseReference.child(FOLLOWING_CHILD).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                if(initialFollowingLoaded){ //so this is only for new updates to following list
                    if(!followers.containsKey(dataSnapshot.getKey())){  //don't add if it's already in Following list
                        following.put(dataSnapshot.getKey(), dataSnapshot.getValue(String.class));
                        if(userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText)){
                            Log.d("MESSENGER", "Following New User");
                            messageContacts.add(new UserSearchItem(dataSnapshot.getKey(), dataSnapshot.getValue(String.class)));
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

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
        SessionManager sessionManager = new SessionManager(context);
        mUsername = sessionManager.getCurrentUsername();
        String userPath = sessionManager.getBday() + "/" + sessionManager.getCurrentUsername();
        FOLLOWERS_CHILD = userPath + "/f";
        FOLLOWING_CHILD = userPath + "/g";
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
        List<UserSearchItem> tempFollowing = new ArrayList<>();
        for(Map.Entry<String, String> entry: following.entrySet()){
            if(entry.getKey().toLowerCase().contains(text.toLowerCase())){
                tempFollowing.add(new UserSearchItem(entry.getKey(), entry.getValue()));
            }
        }

        List<UserSearchItem> tempFollowers = new ArrayList<>();
        for(Map.Entry<String, String> entry: followers.entrySet()){
            if(entry.getKey().toLowerCase().contains(text.toLowerCase())){
                tempFollowers.add(new UserSearchItem(entry.getKey(), entry.getValue()));
            }
        }

        messageContacts = new ArrayList<>();
        for(UserSearchItem g : tempFollowing){
            messageContacts.add(g);
        }
        for(UserSearchItem f : tempFollowers){
            messageContacts.add(f);
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
        //set up a new message room and go into it. actual message room in DB is created with the sending of its first message.
        activity.getMessageRoom().setUpNewRoom(invitedUsers);
        activity.getViewPager().setCurrentItem(11);
    }

}
