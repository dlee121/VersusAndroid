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
import android.view.WindowManager;
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
import com.vs.bcd.versus.adapter.ContactsListAdapter;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private HashSet<String> following, followers, hList, messageContactsCheckSet, newContact, localContactsSet;
    private ArrayList<String> messageContacts, invitedUsers;
    private InvitedUserAdapter invitedUserAdapter;
    private ContactsListAdapter contactsListAdapter;
    private TextView invitedTV;
    private boolean initialFollowersLoaded = false;
    private boolean initialFollowingLoaded = false;
    private boolean initialHLoaded = false;
    private String currentFilterText = "";
    private CreateMessage thisFragment;
    private ArrayList<String> filteredList;
    private String messageContactsQueryCursor = null;
    private Toast mToast;
    private int loadThreshold = 8;
    private boolean nowLoading = false;
    private boolean settingUpInitialList = false;


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

        invitedTV = rootView.findViewById(R.id.invited_tv);

        invitedUsersRV = rootView.findViewById(R.id.invited_users_rv);
        invitedUsersLLM = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        invitedUsersLLM.setStackFromEnd(false);
        invitedUsersRV.setLayoutManager(invitedUsersLLM);

        userSearchRV = rootView.findViewById(R.id.user_search_rv);
        userSearchLLM = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        userSearchLLM.setStackFromEnd(true);
        userSearchRV.setLayoutManager(userSearchLLM);

        userSearchRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //only if postSearchResults.size()%retrievalSize == 0, meaning it's possible there's more matching documents for this search
                if(messageContacts != null && !messageContacts.isEmpty()) {
                    LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                    int lastVisible = layoutManager.findLastVisibleItemPosition();

                    boolean endHasBeenReached = lastVisible + loadThreshold >= messageContacts.size() - 1;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                    if (endHasBeenReached) {
                        //you have reached to the bottom of your recycler view
                        if (!nowLoading) {
                            nowLoading = true;
                            Log.d("loadMoreUsers", "now loading more");
                            loadMoreContacts();
                        }
                    }
                }
            }
        });

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
        mPhotoUrl = activity.getUserProfileImageVersion();

        thisFragment = this;

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        if(messageContactsCheckSet == null){
            messageContactsCheckSet = new HashSet<>();
        }
        if(newContact == null){
            newContact = new HashSet<>();
        }
        if(localContactsSet == null){
            localContactsSet = new HashSet<>();
        }
        if(invitedUsers == null){
            invitedUsers = new ArrayList<>();
            invitedUserAdapter = new InvitedUserAdapter(invitedUsers, getActivity(), thisFragment);
            invitedUsersRV.setAdapter(invitedUserAdapter);
        }
        if(messageContacts == null){
            messageContacts = new ArrayList<>();
            setupInitialContactsList();
            contactsListAdapter = new ContactsListAdapter(messageContacts, getActivity(), thisFragment);
            userSearchRV.setAdapter(contactsListAdapter);
        }

        addHListener();
        //addHListener() finishes and calls addFollowerListener, which finishes and calls addFollowingListener
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

    }

    public void setInitialLoadingFalse(){
        settingUpInitialList = false;
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
                //the great piece of code that prevents keyboard from pushing toolbar up
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
        else {
            if (rootView != null){
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                disableChildViews();
                if(invitedUsers != null){
                    invitedUsers.clear();
                    contactsListAdapter.clearCheckedItems();
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
        if(userSearchRV != null){
            userSearchRV.scrollToPosition(0);
        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    void filter(final String text){

        currentFilterText = text;


        if(text == null || text.equals("")){
            contactsListAdapter.updateList(messageContacts);
        }
        else{
            mFirebaseDatabaseReference.child(activity.getUserPath()+"contacts").orderByKey().startAt(text).endAt(text+"\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(text.equals(currentFilterText)){
                        filteredList = new ArrayList<>();
                        for(DataSnapshot child: dataSnapshot.getChildren()){
                            filteredList.add(child.getKey());
                        }
                        contactsListAdapter.updateList(filteredList);
                        userSearchRV.scrollToPosition(0);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public boolean isNewContact(String username){
        if(newContact != null){
            return newContact.contains(username);
        }
        return false;
    }

    public void addToInvitedList(String contactUsername){
        invitedUsers.add(contactUsername);
        invitedUserAdapter.notifyItemInserted(invitedUsers.size()-1);
        if(invitedUsers.size() == 1){
            showInvitedTV();
        }
        if(invitedUsers.size() > 10){
            invitedUsersRV.scrollToPosition(invitedUsers.size()-1);
        }
    }

    public void removeFromInvitedList(String contactUsername){
        invitedUsers.remove(contactUsername);
        invitedUserAdapter.notifyDataSetChanged();
        if(invitedUsers.isEmpty()){
            hideInvitedTV();
        }
    }

    public void removeFromCheckedItems(String username){
        contactsListAdapter.removeFromCheckedItems(username);
        contactsListAdapter.notifyDataSetChanged();
    }

    private void hideInvitedTV(){
        invitedTV.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }

    private void showInvitedTV(){
        int firstVisible = ((LinearLayoutManager)userSearchRV.getLayoutManager()).findFirstVisibleItemPosition();
        invitedTV.setLayoutParams(LPStore.get(0));  //works because invitedTV is the first child of this layout
        userSearchRV.scrollToPosition(firstVisible);
    }

    public void createMessageRoom(){
        if(invitedUsers != null && !invitedUsers.isEmpty()){
            //set up a new message room and go into it. actual message room in DB is created with the sending of its first message.
            ArrayList<String> invitedUsersFinal = new ArrayList<>();
            for(String invitedUsername:invitedUsers){
                invitedUsersFinal.add(invitedUsername);
            }
            activity.getMessageRoom().setUpNewRoom(invitedUsersFinal);
            activity.getViewPager().setCurrentItem(11);
        } else{
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(activity, "Please add recipient(s).", Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    //for when you delete your copy of the dm room and want to dm the same person and that person hasn't deleted their copy of the dm room
    public void createMessageRoom(String roomNum){
        if(invitedUsers != null && !invitedUsers.isEmpty()){
            //set up a new message room and go into it. actual message room in DB is created with the sending of its first message.
            ArrayList<String> invitedUsersFinal = new ArrayList<>();
            for(String invitedUsername:invitedUsers){
                invitedUsersFinal.add(invitedUsername);
            }
            activity.getMessageRoom().setUpNewRoom(invitedUsersFinal, roomNum);
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
                    following.add(dataSnapshot.getKey());
                    messageContacts.add(0, dataSnapshot.getKey()); //adds to top of contacts as new item
                    contactsListAdapter.notifyItemInserted(0);
                    newContact.add(dataSnapshot.getKey());
                    /*
                    if(userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText)){
                        //messageContacts.add(new UserSearchItem(dataSnapshot.getKey()));
                        messageContactsCheckSet.add(dataSnapshot.getKey());
                        contactsListAdapter.updateList(messageContacts);
                        //TODO: test that this case works (when new follower is added and their username passes current text filter, then display them in the recycler view)
                    }
                    */
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
                            //messageContacts.add(new UserSearchItem(child.getKey()));
                            //messageContactsCheckSet.add(child.getKey());
                        }
                    }
                }

                initialFollowingLoaded = true;
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
                        messageContacts.add(0,dataSnapshot.getKey()); //adds to top of contacts as new item
                        contactsListAdapter.notifyItemInserted(0);
                        newContact.add(dataSnapshot.getKey());
                        Log.d("NEWFOLLOWER", dataSnapshot.getKey() +" is added to the top");
                        /*
                        if(userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText)){
                            messageContacts.add(new UserSearchItem(dataSnapshot.getKey()));
                            messageContactsCheckSet.add(dataSnapshot.getKey());
                            contactsListAdapter.updateList(messageContacts);
                        }
                        */
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
                            //messageContacts.add(new UserSearchItem(child.getKey()));
                            //messageContactsCheckSet.add(child.getKey());
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

    public void setupInitialContactsList(){
        if(!settingUpInitialList && messageContacts != null && invitedUsers != null && localContactsSet != null && newContact != null){
            messageContacts.clear();
            invitedUsers.clear();
            localContactsSet.clear();
            newContact.clear();
        }
        else{
            return;
        }
        settingUpInitialList = true;

        nowLoading = false;

        mFirebaseDatabaseReference.child(activity.getUserPath()+"contacts").orderByKey().limitToFirst(39).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null){
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        messageContacts.add(child.getKey());
                        localContactsSet.add(child.getKey());
                    }
                    if(!messageContacts.isEmpty() && dataSnapshot.getChildrenCount() == 39){
                        messageContactsQueryCursor = messageContacts.get(messageContacts.size() - 1);
                    }
                    else{
                        messageContactsQueryCursor = null;
                    }
                }
                else{
                    messageContactsQueryCursor = null;
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadMoreContacts(){
        if(messageContactsQueryCursor != null){
            mFirebaseDatabaseReference.child(activity.getUserPath()+"contacts").orderByKey().startAt(messageContactsQueryCursor).limitToFirst(40).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean firstItem = true;
                    int startingPosition = messageContacts.size();
                    int newItemCount = 0;
                    if(dataSnapshot != null){
                        for(DataSnapshot child : dataSnapshot.getChildren()){
                            if(firstItem) {
                                if(!localContactsSet.contains(child.getKey())){
                                    messageContacts.add(child.getKey());
                                    localContactsSet.add(child.getKey());
                                    newItemCount++;
                                }
                                firstItem = false;
                                continue;
                            }
                            if(!localContactsSet.contains(child.getKey())){
                                messageContacts.add(child.getKey());
                                localContactsSet.add(child.getKey());
                                newItemCount++;
                            }
                        }
                        if(!messageContacts.isEmpty() && dataSnapshot.getChildrenCount() == 40){
                            messageContactsQueryCursor = messageContacts.get(messageContacts.size() - 1);
                            nowLoading = false;
                        }
                        else{
                            messageContactsQueryCursor = null;
                        }

                        if(newItemCount > 0){
                            contactsListAdapter.notifyItemRangeChanged(startingPosition, newItemCount);
                        }
                    }
                    else{
                        messageContactsQueryCursor = null;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }



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
                    /*
                    if(!(messageContactsCheckSet.contains(dataSnapshot.getKey())) && (userSearchET.getText().toString().isEmpty() || dataSnapshot.getKey().contains(currentFilterText))){
                        messageContacts.add(new UserSearchItem(dataSnapshot.getKey()));
                        messageContactsCheckSet.add(dataSnapshot.getKey());
                        contactsListAdapter.updateList(messageContacts);
                    }
                    */
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
                        //messageContacts.add(new UserSearchItem(child.getKey()));
                        //messageContactsCheckSet.add(child.getKey());
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



    public boolean followingAndFollowedBy(String username){
        if(hList != null){
            return hList.contains(username);
        }
        return false;
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
            return invitedUsers.get(0);
        }
        return "";
    }

}
