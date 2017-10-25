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
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainActivity;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.InvitedUserAdapter;
import com.vs.bcd.versus.adapter.UserSearchAdapter;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by dlee on 8/6/17.
 */



public class CreateMessage extends Fragment {

    private String FOLLOWERS_CHILD = "";
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
    private ArrayList<String> following, followers;
    private ArrayList<UserSearchItem> messageContacts, invitedUsers;
    private InvitedUserAdapter invitedUserAdapter;
    private UserSearchAdapter userSearchAdapter;
    private TextView invitedTV;
    private boolean initialFListLoaded = false;
    private String currentFilterText = "";


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
        mPhotoUrl = activity.getProfileImageURL();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        //TODO: get fresh followers list from firebase
        followers = new ArrayList<>();  //temp empty list
        //followers = new ArrayList<>(***get realtime instance of followers list in firebase***);

        mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    followers.add(child.getKey()); //the key is the username
                }
                initialFListLoaded = true;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mFirebaseDatabaseReference.child(FOLLOWERS_CHILD).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                if(initialFListLoaded){ //so this is only for new updates to followers list
                    String newFollower = dataSnapshot.getKey();
                    followers.add(newFollower); //the key is the username
                    if(dataSnapshot.getKey().contains(currentFilterText)){
                        messageContacts.add(new UserSearchItem(newFollower));
                        userSearchAdapter.updateList(messageContacts);
                        //TODO: test that this case works (when new follower is added and their username passes current text filter, then display them in the recycler view)
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

        following = new ArrayList<>(activity.getLocalFns());

        messageContacts = new ArrayList<>();
        for(String frs : followers){
            messageContacts.add(new UserSearchItem(frs));
        }
        for(String fws : following){
            messageContacts.add(new UserSearchItem(fws));
        }

        invitedUsers = new ArrayList<>();

        invitedUserAdapter = new InvitedUserAdapter(invitedUsers, getActivity(), this);
        invitedUsersRV.setAdapter(invitedUserAdapter);

        userSearchAdapter = new UserSearchAdapter(messageContacts, getActivity(), this);
        userSearchRV.setAdapter(userSearchAdapter);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
        SessionManager sessionManager = new SessionManager(context);
        mUsername = sessionManager.getCurrentUsername();
        FOLLOWERS_CHILD = sessionManager.getBday() + "/" + sessionManager.getCurrentUsername() + "/f";
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                enableChildViews();
                if(activity != null){
                    following = new ArrayList<>(activity.getLocalFns()); //refresh "following" whenever this fragment opens, to keep code simple instead of syncing with other pages whenever "following" changes
                }
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
        List<String> tempFollowing = new ArrayList<>();
        for(String entry: following){
            if(entry.toLowerCase().contains(text.toLowerCase())){
                tempFollowing.add(entry);
            }
        }

        List<String> tempFollowers = new ArrayList<>();
        for(String entry: followers){
            if(entry.toLowerCase().contains(text.toLowerCase())){
                tempFollowers.add(entry);
            }
        }

        messageContacts = new ArrayList<>();
        for(String fws : tempFollowing){
            messageContacts.add(new UserSearchItem(fws));
        }
        for(String frs : tempFollowers){
            messageContacts.add(new UserSearchItem(frs));
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

}
