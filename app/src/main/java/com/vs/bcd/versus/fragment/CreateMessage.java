package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.InvitedUserAdapter;
import com.vs.bcd.versus.adapter.UserSearchAdapter;
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

    private String ROOMS_CHILD = "";
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

        userMKey = ((MainContainer)getActivity()).getUserMKey();
        //mUsername = ((MainContainer)getActivity()).getUsername();
        if(mFirebaseUser == null){
            mFirebaseAuth.signInWithEmailAndPassword(userMKey + mUsername.replaceAll("[^A-Za-z0-9]", "v") + "@versusbcd.com", userMKey + "vsbcd121")
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w("firebasechat", "sign in failed");
                                Toast.makeText(getActivity(), "Authentication failed.", Toast.LENGTH_SHORT).show();

                            } else {
                                //TODO: photoURL should be a link to the user's profile picture stored in firebase. If no pic then should be blank, and in the UI if photoURL is blank then use default image in-app
                                mPhotoUrl = "https://firebasestorage.googleapis.com/v0/b/bcd-versus.appspot.com/o/vs_shadow_w_tag.png?alt=media&token=76f50800-a388-4be7-b802-bff78fe0d07d";
                                mFirebaseUser = mFirebaseAuth.getCurrentUser();
                                setUpMessenger();
                            }
                        }
                    });
        }
        else {
            mPhotoUrl = "https://firebasestorage.googleapis.com/v0/b/bcd-versus.appspot.com/o/vs_shadow_w_tag.png?alt=media&token=76f50800-a388-4be7-b802-bff78fe0d07d";
            setUpMessenger();
        }

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
        SessionManager sessionManager = new SessionManager(context);
        mUsername = sessionManager.getCurrentUsername();
        ROOMS_CHILD = sessionManager.getBday() + "/" + sessionManager.getCurrentUsername() + "/rooms";
    }

    private void setUpMessenger(){
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        following = new ArrayList<>(activity.getLocalFns());
        Log.d("MESSENGER", "following list size: "+Integer.toString(following.size()));

        //TODO: get fresh followers list from firebase
        followers = new ArrayList<>();  //temp empty list
        //followers = new ArrayList<>(***get realtime instance of followers list in firebase***);

        messageContacts = new ArrayList<>();
        for(String fws : following){
            messageContacts.add(new UserSearchItem(fws));
        }
        for(String frs : followers){
            messageContacts.add(new UserSearchItem(frs));
        }

        invitedUsers = new ArrayList<>();

        invitedUserAdapter = new InvitedUserAdapter(invitedUsers, getActivity(), this);
        invitedUsersRV.setAdapter(invitedUserAdapter);

        userSearchAdapter = new UserSearchAdapter(messageContacts, getActivity(), this);
        userSearchRV.setAdapter(userSearchAdapter);

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null)
                enableChildViews();
        }
        else {
            if (rootView != null)
                disableChildViews();
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
