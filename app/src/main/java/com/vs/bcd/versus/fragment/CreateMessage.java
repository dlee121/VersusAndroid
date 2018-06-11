package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.vs.bcd.api.VersusAPIClient;
import com.vs.bcd.api.model.PIVModel;
import com.vs.bcd.api.model.PIVModelDocsItem;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.InvitedUserAdapter;
import com.vs.bcd.versus.adapter.ContactsListAdapter;
import com.vs.bcd.versus.model.MessageObject;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.SessionManager;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
    private HashMap<String, Integer> profileImgVersions = new HashMap<>();
    private RoomObject inviteTargetRoom = null;
    private String inviteTargetRoomNum = null;
    private RoomObject removalTargetRoom = null;
    private String removalTargetRoomNum = null;


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
            if(activity != null){
                invitedUserAdapter = new InvitedUserAdapter(invitedUsers, activity, thisFragment, profileImgVersions);
            }
            else{
                invitedUserAdapter = new InvitedUserAdapter(invitedUsers, (MainContainer) getActivity(), thisFragment, profileImgVersions);
            }

            invitedUsersRV.setAdapter(invitedUserAdapter);
        }
        if(messageContacts == null){
            messageContacts = new ArrayList<>();
            setupInitialContactsList();
            if(activity != null){
                contactsListAdapter = new ContactsListAdapter(messageContacts, activity, thisFragment, profileImgVersions);
                //recyclerview preloader setup
                ListPreloader.PreloadSizeProvider sizeProvider =
                        new FixedPreloadSizeProvider(activity.getResources().getDimensionPixelSize(R.dimen.messenger_profile_image), activity.getResources().getDimensionPixelSize(R.dimen.messenger_profile_image));
                RecyclerViewPreloader<String> preloader =
                        new RecyclerViewPreloader<>(Glide.with(activity), contactsListAdapter, sizeProvider, 20);
                userSearchRV.addOnScrollListener(preloader);
            }

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
                userSearchET.setText("");
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

    public void setUpRemovePage(ArrayList<String> removalCandidates){
        nowLoading = true; //since we have all the users we need to list, we can disable onScroll loading

        StringBuilder strBuilder = new StringBuilder();
        int i = 0;
        for(String username: removalCandidates){
            if(profileImgVersions.get(username) == null){
                if(i == 0){
                    strBuilder.append("\""+username+"\"");
                }
                else{
                    strBuilder.append(",\""+username+"\"");
                }
                i++;
            }
        }

        if(strBuilder.length() > 0){
            final String payload = "{\"ids\":["+strBuilder.toString()+"]}";
            Runnable runnable = new Runnable() {
                public void run() {
                    getProfileImgVersions(payload);
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();
        }

        contactsListAdapter = new ContactsListAdapter(removalCandidates, activity, thisFragment, profileImgVersions);
        userSearchRV.setAdapter(contactsListAdapter);

    }


    void filter(final String text){

        currentFilterText = text;


        if(text == null || text.equals("")){
            contactsListAdapter.updateList(messageContacts);
            nowLoading = false; //enable onScroll loading
        }
        else{
            nowLoading = true; //disable onScroll loading
            mFirebaseDatabaseReference.child(activity.getUserPath()+"contacts").orderByKey().startAt(text).endAt(text+"\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(text.equals(currentFilterText)){
                        filteredList = new ArrayList<>();
                        if(dataSnapshot.hasChildren()){
                            StringBuilder strBuilder = new StringBuilder((56*(int)dataSnapshot.getChildrenCount()) - 1);
                            int i = 0;
                            for(DataSnapshot child: dataSnapshot.getChildren()){
                                filteredList.add(child.getKey());
                                //messageContacts.add(child.getKey());
                                localContactsSet.add(child.getKey());
                                if(i == 0){
                                    strBuilder.append("\""+child.getKey()+"\"");
                                }
                                else{
                                    strBuilder.append(",\""+child.getKey()+"\"");
                                }
                                i++;
                            }

                            if(strBuilder.length() > 0){
                                final String payload = "{\"ids\":["+strBuilder.toString()+"]}";
                                Runnable runnable = new Runnable() {
                                    public void run() {
                                        getProfileImgVersions(payload);
                                    }
                                };
                                Thread mythread = new Thread(runnable);
                                mythread.start();
                            }
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
        if(invitedUsers.size() > 8){
            invitedUsersRV.scrollToPosition(invitedUsers.size()-1);
        }
    }

    public void setInviteTargetRoom(RoomObject targetRoom, String roomNum){
        inviteTargetRoom = targetRoom;
        inviteTargetRoomNum = roomNum;
    }

    public void notifyDataSetChanged(){
        if(contactsListAdapter != null){
            contactsListAdapter.notifyDataSetChanged();
        }
    }

    public RoomObject getInviteTargetRoom(){
        return inviteTargetRoom;
    }

    public void setInviteTargetUsersListNull(){
        if(contactsListAdapter != null){
            contactsListAdapter.setInviteTargetUsersListNull();
        }
    }

    public HashMap<String, Integer> getInviteTargetRoomUsersList(){
        HashMap<String, Integer> inviteTargetRoomUsersMap = new HashMap<>();
        for(String usernameAndNumberCode : inviteTargetRoom.getUsers()){
            Log.d("thisgroup", usernameAndNumberCode);
            int numberCodeIndex = usernameAndNumberCode.indexOf('*');
            if(numberCodeIndex > 0){
                inviteTargetRoomUsersMap.put(usernameAndNumberCode
                        .substring(0, numberCodeIndex), Integer.parseInt(usernameAndNumberCode.substring(numberCodeIndex+1)));
            }
            else{
                inviteTargetRoomUsersMap.put(usernameAndNumberCode, -1); //no numberCode
            }
        }
        return inviteTargetRoomUsersMap;
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

    private int getUsernameHash(String username){
        int usernameHash;
        if(username.length() < 5){
            usernameHash = username.hashCode();
        }
        else {
            String hashIn = "" + username.charAt(0) + username.charAt(username.length() - 2) + username.charAt(1) + username.charAt(username.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        return usernameHash;
    }

    public void setInviteText(){
        invitedTV.setTextColor(ContextCompat.getColor(activity, R.color.vsBlue));
        invitedTV.setText("Invite");
    }

    public void setRemoveText(){
        invitedTV.setTextColor(ContextCompat.getColor(activity, R.color.vsRed));
        invitedTV.setText("Remove");
    }

    public void setRemovalTargetRoom(RoomObject targetRoom, String targetRoomNum){
        removalTargetRoom = targetRoom;
        removalTargetRoomNum = targetRoomNum;
    }

    public void removeFromGroupSubmit(){
        if(invitedUsers != null && !invitedUsers.isEmpty()){
            HashSet<String> removalCandidates = new HashSet<>(invitedUsers);
            ArrayList<String> secondTimeLeaving = new ArrayList<>();
            ArrayList<String> thirdTimeLeaving = new ArrayList<>();
            int secondCount = 0;
            int thirdCount = 0;

            for(int i = 0; i<removalTargetRoom.getUsers().size(); i++){
                String username = removalTargetRoom.getUsers().get(i);
                if(username.indexOf('*') > 0){
                    String pureUsername = username.substring(0, username.indexOf('*'));
                    if(removalCandidates.contains(pureUsername)){
                        int numberCode = Integer.parseInt(username.substring(username.indexOf('*')+1));
                        removalTargetRoom.getUsers().set(i, pureUsername+"*"+Integer.toString(numberCode + 1));
                        switch (numberCode){
                            case 1:
                                secondTimeLeaving.add(pureUsername);
                                secondCount++;
                                break;

                            case 3:
                                thirdTimeLeaving.add(pureUsername);
                                thirdCount++;
                                break;
                        }
                    }
                }
                else if(removalCandidates.contains(username)){
                    removalTargetRoom.getUsers().set(i, username+"*0");
                }
            }

            String firstString, secondString, thirdString;

            StringBuilder stringBuilder1 = new StringBuilder();
            if(invitedUsers.size() == 1){
                firstString = mUsername + " removed " + invitedUsers.get(0);
            }
            else if(invitedUsers.size() == 2){
                firstString = mUsername + " removed " + invitedUsers.get(0) + " and " + invitedUsers.get(1);
            }
            else{
                for(int i = 0; i<invitedUsers.size() - 1; i++){
                    stringBuilder1.append(invitedUsers.get(i)).append(", ");
                }
                stringBuilder1.append("and ").append(invitedUsers.get(invitedUsers.size()-1));
                firstString = mUsername + " removed " + stringBuilder1.toString();
            }

            if(!secondTimeLeaving.isEmpty()){
                if(secondCount == 1){
                    secondString = secondTimeLeaving.get(0) + " has one invite left";
                }
                else if(secondCount == 2){
                    secondString = secondTimeLeaving.get(0) + " and " + secondTimeLeaving.get(1) + " have one invite left";
                }
                else{
                    StringBuilder stringBuilder2 = new StringBuilder();
                    for(int i=0; i<secondTimeLeaving.size()-1; i++){
                        stringBuilder2.append(secondTimeLeaving.get(i)).append(", ");
                    }
                    stringBuilder2.append("and ").append(secondTimeLeaving.get(secondCount-1));
                    secondString = stringBuilder2.toString() + " have one invite left";
                }
            }
            else{
                secondString = null;
            }

            if(!thirdTimeLeaving.isEmpty()){
                if(thirdCount == 1){
                    thirdString = thirdTimeLeaving.get(0) + " has been removed indefinitely";
                }
                else if(thirdCount == 2){
                    thirdString = thirdTimeLeaving.get(0) + " and " + thirdTimeLeaving.get(1) + " have been removed indefinitely";
                }
                else{
                    StringBuilder stringBuilder3 = new StringBuilder();
                    for(int i=0; i<thirdTimeLeaving.size()-1; i++){
                        stringBuilder3.append(thirdTimeLeaving.get(i)).append(", ");
                    }
                    stringBuilder3.append("and ").append(thirdTimeLeaving.get(thirdCount-1));
                    thirdString = stringBuilder3.toString() + " have been removed indefinitely";
                }
            }
            else{
                thirdString = null;
            }


            String finalString;
            if(secondString != null && thirdString != null){
                finalString = firstString+"\n\n"+secondString+"\n\n"+thirdString;
            }
            else if(secondString != null){
                finalString = firstString+"\n\n"+secondString;
            }
            else if(thirdString != null){
                finalString = firstString+"\n\n"+thirdString;
            }
            else{
                finalString = firstString;
            }

            final MessageObject eventMessage = new MessageObject(finalString, null, null);
            activity.getMessengerFragment().setClickedRoomNum(removalTargetRoomNum);
            for(String username : removalTargetRoom.getUsers()){
                if(username.indexOf('*') > 0){
                    int numberCode = Integer.parseInt(username.substring(username.indexOf('*')+1));
                    if(numberCode == 1 || numberCode == 3){
                        String pureUsername = username.substring(0, username.indexOf('*'));
                        final String writePath = getUsernameHash(pureUsername)+"/"+pureUsername;
                        mFirebaseDatabaseReference.child(writePath+"/r/"+removalTargetRoomNum).setValue(removalTargetRoom).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mFirebaseDatabaseReference.child(writePath+"/messages/"+removalTargetRoomNum).push().setValue(eventMessage);
                            }
                        });
                    }
                }
                else{
                    final String writePath = getUsernameHash(username)+"/"+username;
                    mFirebaseDatabaseReference.child(writePath+"/r/"+removalTargetRoomNum).setValue(removalTargetRoom).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mFirebaseDatabaseReference.child(writePath+"/messages/"+removalTargetRoomNum).push().setValue(eventMessage);
                        }
                    });
                }
            }

            for(String username : removalCandidates){
                final String writePath = getUsernameHash(username)+"/"+username;
                mFirebaseDatabaseReference.child(writePath+"/r/"+removalTargetRoomNum).removeValue();
                mFirebaseDatabaseReference.child(writePath+"/messages/"+removalTargetRoomNum).removeValue();
                mFirebaseDatabaseReference.child(writePath+"/unread/"+removalTargetRoomNum).removeValue();
                mFirebaseDatabaseReference.child(writePath+"/push/m/"+removalTargetRoomNum).removeValue();
            }

            activity.getMessageRoom().setUpRoom(removalTargetRoomNum, removalTargetRoom.getUsers(), removalTargetRoom.getName());
            activity.getViewPager().setCurrentItem(11);
        }
        else{
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(activity, "No user selected.", Toast.LENGTH_SHORT);
            mToast.show();
        }
        //setInviteText();
    }


    public void inviteToGroupSubmit(HashSet<String> numberCodeIncrementList){
        if(invitedUsers != null && !invitedUsers.isEmpty()){
            HashMap<String, String> usernameToUsernameWithNumberCode = new HashMap<>();
            ArrayList<String> newUsersList = new ArrayList<>();
            for(String username : inviteTargetRoom.getUsers()){
                if(username.indexOf('*') > 0){
                    String pureUsername = username.substring(0, username.indexOf('*'));
                    if(numberCodeIncrementList.contains(pureUsername)){
                        int numberCode = Integer.parseInt(username.substring(username.indexOf('*')+1));
                        //inviteTargetRoom.getUsers().set(i, pureUsername+"*"+Integer.toString(numberCode+1));
                        usernameToUsernameWithNumberCode.put(pureUsername, pureUsername+"*"+Integer.toString(numberCode+1));
                    }
                    else{
                        newUsersList.add(username);
                    }
                }
                else{
                    newUsersList.add(username);
                }
            }

            for(String invitedUsername:invitedUsers){
                if(!numberCodeIncrementList.contains(invitedUsername)){ //skip users in this hash set because they're already in the usersList
                    newUsersList.add(invitedUsername);
                }
                else{
                    newUsersList.add(usernameToUsernameWithNumberCode.get(invitedUsername));
                }
            }

            inviteTargetRoom.setUsers(newUsersList);

            //final String targetRoomNum = inviteTargetRoomNum;
            String eventMessageString;
            if(invitedUsers.size() == 1){
                eventMessageString = mUsername + " invited " + invitedUsers.get(0);
            }
            else if(invitedUsers.size() == 2){
                eventMessageString = mUsername + " invited " + invitedUsers.get(0) + " and " + invitedUsers.get(1);
            }
            else{
                StringBuilder strBuilder = new StringBuilder();
                for(int i = 0; i<invitedUsers.size()-1; i++){
                    strBuilder.append(invitedUsers.get(i)).append(", ");
                }
                strBuilder.append("and ").append(invitedUsers.get(invitedUsers.size()-1)).append("!");
                eventMessageString = mUsername + " invited " + strBuilder.toString();
            }

            inviteTargetRoom.setPreview(eventMessageString);
            inviteTargetRoom.setTime(System.currentTimeMillis());

            //TODO: this can be further optimized by instead of sending a whole room object, we only update the users list for existing users and only set up new room for new user.
            //TODO: also the preview and time would be set twice since we're also sending an event message after setting up the room. So getting rid of that intersection would further optimize the process, if it is done safely and reliably.
            final MessageObject eventMessage = new MessageObject(eventMessageString, null, null);
            activity.getMessengerFragment().setClickedRoomNum(inviteTargetRoomNum);
            for(final String username : inviteTargetRoom.getUsers()){
                if(username.indexOf('*') > 0){
                    final String pureUsername = username.substring(0, username.indexOf('*'));
                    String roomPath = Integer.toString(getUsernameHash(pureUsername))+"/"+pureUsername+"/r/"+inviteTargetRoomNum;

                    mFirebaseDatabaseReference.child(roomPath).setValue(inviteTargetRoom).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            String messagePath = Integer.toString(getUsernameHash(pureUsername))+"/"+pureUsername+"/messages/"+inviteTargetRoomNum;
                            mFirebaseDatabaseReference.child(messagePath).push().setValue(eventMessage);
                        }
                    });
                }
                else{
                    String roomPath = Integer.toString(getUsernameHash(username))+"/"+username+"/r/"+inviteTargetRoomNum;

                    mFirebaseDatabaseReference.child(roomPath).setValue(inviteTargetRoom).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            String messagePath = Integer.toString(getUsernameHash(username))+"/"+username+"/messages/"+inviteTargetRoomNum;
                            mFirebaseDatabaseReference.child(messagePath).push().setValue(eventMessage);
                        }
                    });
                }
            }

            activity.getMessageRoom().setUpRoom(inviteTargetRoomNum, inviteTargetRoom.getUsers(), inviteTargetRoom.getName());
            activity.getViewPager().setCurrentItem(11);
        } else{
            if(mToast != null){
                mToast.cancel();
            }
            mToast = Toast.makeText(activity, "No user selected.", Toast.LENGTH_SHORT);
            mToast.show();
        }
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
            mToast = Toast.makeText(activity, "No user selected.", Toast.LENGTH_SHORT);
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
        setInviteText();
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
                if(dataSnapshot != null && dataSnapshot.getChildrenCount() > 0){
                    StringBuilder strBuilder = new StringBuilder((56*(int)dataSnapshot.getChildrenCount()) - 1);
                    int i = 0;
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        messageContacts.add(child.getKey());
                        localContactsSet.add(child.getKey());
                        if(i == 0){
                            strBuilder.append("\""+child.getKey()+"\"");
                        }
                        else{
                            strBuilder.append(",\""+child.getKey()+"\"");
                        }
                        i++;
                    }
                    if(strBuilder.length() > 0){
                        final String payload = "{\"ids\":["+strBuilder.toString()+"]}";
                        Runnable runnable = new Runnable() {
                            public void run() {
                                getProfileImgVersions(payload);
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
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
                    if(dataSnapshot != null && dataSnapshot.getChildrenCount() > 0){
                        StringBuilder strBuilder = new StringBuilder((56*(int)dataSnapshot.getChildrenCount()) - 1);
                        int i = 0;
                        for(DataSnapshot child : dataSnapshot.getChildren()){

                            if(i == 0){
                                strBuilder.append("\""+child.getKey()+"\"");
                            }
                            else{
                                strBuilder.append(",\""+child.getKey()+"\"");
                            }
                            i++;

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

                        if(strBuilder.length() > 0){
                            final String payload = "{\"ids\":["+strBuilder.toString()+"]}";

                            Runnable runnable = new Runnable() {
                                public void run() {
                                    getProfileImgVersions(payload);
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
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

    private void getProfileImgVersions(String payload){

        PIVModel pivResult = activity.getClient().pivGet("pis", payload);

        List<PIVModelDocsItem> pivList = pivResult.getDocs();
        if(pivList != null && !pivList.isEmpty()){
            for(PIVModelDocsItem item : pivList){
                profileImgVersions.put(item.getId(), item.getSource().getPi().intValue());
            }
        }

        if(contactsListAdapter != null){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    contactsListAdapter.notifyDataSetChanged();
                }
            });
        }

    }

}
