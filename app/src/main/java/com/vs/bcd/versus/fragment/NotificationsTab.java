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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.InvitedUserAdapter;
import com.vs.bcd.versus.adapter.NotificationsAdapter;
import com.vs.bcd.versus.adapter.UserSearchAdapter;
import com.vs.bcd.versus.model.NotificationItem;
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dlee on 8/6/17.
 */



public class NotificationsTab extends Fragment {

    private final int TYPE_U = 0; //new comment upvote notification
    private final int TYPE_C = 1; //new comment reply notification
    private final int TYPE_V = 2; //new post vote notification
    private final int TYPE_R = 3; //new post root comment notification
    private final int TYPE_F = 4; //new follower notification
    private final int TYPE_M = 5; //new medal notification

    private View rootView;
    private ArrayList<NotificationItem> notificationItems;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private MainContainer activity;
    private RecyclerView recyclerView;
    private NotificationsAdapter mNotificationsAdapter;
    private HashMap<String, NotificationItem> notificationItemsMap;

    private DatabaseReference mFirebaseDatabaseReference;
    private ChildEventListener uListener, cListener, vListener, rListener, fListener, mListener;
    private String uPath, cPath, vPath, rPath, fPath, mPath;
    private HashMap<String, Long> newFollowers;
    private int gnew, snew, bnew;
    private HashMap<String, String> medalComments;
    private Long fTime = (long) 0;
    private NotificationItem fNotification, gNotification, sNotification, bNotification;
    private boolean initialFLoaded = false;
    private boolean fNotificationAdded = false;
    private boolean initialMLoaded = false;
    private boolean gAdded, sAdded, bAdded;

    String userNotificationsPath = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.notifications, container, false);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        userNotificationsPath = activity.getUserPath() + "n/";
        uPath = userNotificationsPath + "u";
        cPath = userNotificationsPath + "c";
        vPath = userNotificationsPath + "v";
        rPath = userNotificationsPath + "r";
        fPath = userNotificationsPath + "f";
        mPath = userNotificationsPath + "m";

        notificationItems = new ArrayList<>();
        notificationItemsMap = new HashMap<>();
        newFollowers = new HashMap<>();
        medalComments = new HashMap<>();
        gnew = 0;
        snew = 0;
        bnew = 0;

        recyclerView = (RecyclerView) rootView.findViewById(R.id.notifications_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        disableChildViews();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer)context;
    }

    @Override
    public void onResume(){
        super.onResume();
        fetchUNotifications();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mNotificationsAdapter != null){
            mNotificationsAdapter.clearAllItems();
        }
        if(uListener != null){
            mFirebaseDatabaseReference.child(uPath).removeEventListener(uListener);
        }
        if(cListener != null){
            mFirebaseDatabaseReference.child(cPath).removeEventListener(cListener);
        }
        if(vListener != null){
            mFirebaseDatabaseReference.child(vPath).removeEventListener(vListener);
        }
        if(rListener != null){
            mFirebaseDatabaseReference.child(rPath).removeEventListener(rListener);
        }
        if(fListener != null){
            mFirebaseDatabaseReference.child(fPath).removeEventListener(fListener);
        }
        if(mListener != null){
            mFirebaseDatabaseReference.child(mPath).removeEventListener(mListener);
        }
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


    private void fetchUNotifications(){
        if(notificationItems != null && notificationItemsMap != null && newFollowers != null && medalComments != null){
            notificationItems.clear();
            notificationItemsMap.clear();
            newFollowers.clear();
            medalComments.clear();
        }
        else{
            notificationItems = new ArrayList<>();
            notificationItemsMap = new HashMap<>();
            newFollowers = new HashMap<>();
            medalComments = new HashMap<>();
        }
        gnew = 0;
        snew = 0;
        bnew = 0;

        fTime = (long) 0;
        initialFLoaded = false;

        mFirebaseDatabaseReference.child(uPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fetchCNotifications();  //called after initial onChildAdded calls finish for uListener below
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        uListener = mFirebaseDatabaseReference.child(uPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                String parentID, commentID, content;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 3);
                parentID = parsed[0];
                commentID = parsed[1];
                content = parsed[2];

                long childrenCount = dataSnapshot.getChildrenCount();
                String heading;
                if(childrenCount == 1){
                    heading = "1 user";
                }
                else{
                    heading = Long.toString(childrenCount)+" users";
                }
                String body = heading + " upvoted your comment: \"" + content + "\".";
                NotificationItem n = new NotificationItem(body, TYPE_U, parentID+":"+commentID, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                notificationItemsMap.put("U:"+commentID, n);
                notificationItems.add(n);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

                String parentID, commentID, content;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 3);
                parentID = parsed[0];
                commentID = parsed[1];
                content = parsed[2];

                if(notificationItemsMap.containsKey("U:"+commentID)){
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " upvoted your comment: \"" + content + "\".";
                    notificationItemsMap.get("U:"+commentID).setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                }
                else{
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " upvoted your comment: \"" + content + "\".";
                    NotificationItem n = new NotificationItem(body, TYPE_U, parentID+":"+commentID, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItemsMap.put("U:"+commentID, n);
                    notificationItems.add(n);
                }
                mNotificationsAdapter.notifyDataSetChanged();

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void fetchCNotifications(){

        mFirebaseDatabaseReference.child(cPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fetchVNotifications();  //called after initial onChildAdded calls finish for cListener below
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        cListener = mFirebaseDatabaseReference.child(cPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                String parentID, commentID, content;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 3);
                parentID = parsed[0];
                commentID = parsed[1];
                content = parsed[2];

                long childrenCount = dataSnapshot.getChildrenCount();
                String heading;
                if(childrenCount == 1){
                    heading = "1 user";
                }
                else{
                    heading = Long.toString(childrenCount)+" users";
                }
                String body = heading + " replied to your comment: \"" + content + "\".";
                NotificationItem n = new NotificationItem(body, TYPE_C, parentID+":"+commentID, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                notificationItemsMap.put("C:"+commentID, n);
                notificationItems.add(n);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

                String parentID, commentID, content;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 3);
                parentID = parsed[0];
                commentID = parsed[1];
                content = parsed[2];

                if(notificationItemsMap.containsKey("C:"+commentID)){
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " replied to your comment: \"" + content + "\".";
                    notificationItemsMap.get("C:"+commentID).setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));

                }
                else{
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " replied to your comment: \"" + content + "\".";
                    NotificationItem n = new NotificationItem(body, TYPE_C, parentID+":"+commentID, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItemsMap.put("C:"+commentID, n);
                    notificationItems.add(n);
                }
                mNotificationsAdapter.notifyDataSetChanged();

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void fetchVNotifications(){
        mFirebaseDatabaseReference.child(vPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fetchRNotifications();  //called after initial onChildAdded calls finish for vListener below
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        vListener = mFirebaseDatabaseReference.child(vPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                String postID, redName, blackName, question;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 4);
                postID = parsed[0];
                redName = parsed[1];
                blackName = parsed[2];
                question = parsed[3];

                long childrenCount = dataSnapshot.getChildrenCount();
                String heading;
                if(childrenCount == 1){
                    heading = "1 user";
                }
                else{
                    heading = Long.toString(childrenCount)+" users";
                }
                String body = heading + " voted in your post: \"" + redName + " VS " + blackName + ", " + question + "\".";
                NotificationItem n = new NotificationItem(body, TYPE_V, redName+":"+blackName+":"+question, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                notificationItemsMap.put("V:"+postID, n);
                notificationItems.add(n);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

                String postID, redName, blackName, question;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 4);
                postID = parsed[0];
                redName = parsed[1];
                blackName = parsed[2];
                question = parsed[3];

                if(notificationItemsMap.containsKey("V:"+postID)){
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " voted in your post: \"" + redName + " VS " + blackName + ", " + question + "\".";
                    notificationItemsMap.get("V:"+postID).setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));

                }
                else{
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " voted in your post: \"" + redName + " VS " + blackName + ", " + question + "\".";
                    NotificationItem n = new NotificationItem(body, TYPE_V, redName+":"+blackName+":"+question, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItemsMap.put("V:"+postID, n);
                    notificationItems.add(n);
                }
                mNotificationsAdapter.notifyDataSetChanged();

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void fetchRNotifications(){
        mFirebaseDatabaseReference.child(rPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fetchFNotifications();  //called after initial onChildAdded calls finish for rListener below
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        rListener = mFirebaseDatabaseReference.child(rPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                String postID, redName, blackName, question;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 4);
                postID = parsed[0];
                redName = parsed[1];
                blackName = parsed[2];
                question = parsed[3];

                long childrenCount = dataSnapshot.getChildrenCount();
                String heading;
                if(childrenCount == 1){
                    heading = "1 user";
                }
                else{
                    heading = Long.toString(childrenCount)+" users";
                }
                String body = heading + " commented in your post: \"" + redName + " VS " + blackName + ", " + question + "\".";
                NotificationItem n = new NotificationItem(body, TYPE_R, redName+":"+blackName+":"+question, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                notificationItemsMap.put("R:"+postID, n);
                notificationItems.add(n);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

                String postID, redName, blackName, question;
                String[] parsed = dataSnapshot.getKey().trim().split(":", 4);
                postID = parsed[0];
                redName = parsed[1];
                blackName = parsed[2];
                question = parsed[3];

                if(notificationItemsMap.containsKey("R:"+postID)){
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " commented in your post: \"" + redName + " VS " + blackName + ", " + question + "\".";
                    notificationItemsMap.get("R:"+postID).setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                }
                else{
                    long childrenCount = dataSnapshot.getChildrenCount();
                    String heading;
                    if(childrenCount == 1){
                        heading = "1 user";
                    }
                    else{
                        heading = Long.toString(childrenCount)+" users";
                    }
                    String body = heading + " commented in your post: \"" + redName + " VS " + blackName + ", " + question + "\".";
                    NotificationItem n = new NotificationItem(body, TYPE_R, redName+":"+blackName+":"+question, dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItemsMap.put("R:"+postID, n);
                    notificationItems.add(n);
                }
                mNotificationsAdapter.notifyDataSetChanged();

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void fetchFNotifications(){

        fNotification = new NotificationItem("You have followers!", TYPE_F, 0);
        initialFLoaded = false;
        fNotificationAdded = false;

        mFirebaseDatabaseReference.child(fPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                initialFLoaded = true;
                if(!newFollowers.isEmpty()){
                    String body = "You have " + Integer.toString(newFollowers.size()) + " new followers!";
                    if(newFollowers.size() == 1){
                        body = "You have a new follower!";
                    }
                    fNotification.setBody(body).setTimestamp(fTime);
                    notificationItems.add(fNotification);
                    fNotificationAdded = true;
                }

                fetchMNotifications();  //called after initial onChildAdded calls finish for fListener below
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        fListener = mFirebaseDatabaseReference.child(fPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                newFollowers.put(dataSnapshot.getKey(), dataSnapshot.getValue(Long.class));
                if(dataSnapshot.getValue(Long.class) > fTime){
                    fTime = dataSnapshot.getValue(Long.class);
                }
                if(initialFLoaded){
                    String body = "You have " + Integer.toString(newFollowers.size()) + " new followers!";

                    if(newFollowers.size() == 1){
                        body = "You have a new follower!";
                    }
                    fNotification.setBody(body).setTimestamp(fTime);
                    if(!fNotificationAdded){
                        notificationItems.add(fNotification);
                        fNotificationAdded = true;
                    }
                    mNotificationsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void fetchMNotifications(){
        initialMLoaded = false;
        gAdded = false;
        sAdded = false;
        bAdded = false;

        mFirebaseDatabaseReference.child(mPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                initialMLoaded = true;
                if(gnew > 0){
                    String body = "You won " + Integer.toString(gnew) + " new gold medals";
                    gNotification = new NotificationItem(body, TYPE_M, 0);
                    notificationItems.add(gNotification);
                    gAdded = true;
                }

                if(snew > 0){
                    String body = "You won " + Integer.toString(snew) + " new silver medals";
                    sNotification = new NotificationItem(body, TYPE_M, 0);
                    notificationItems.add(sNotification);
                    sAdded = true;
                }

                if(bnew > 0){
                    String body = "You won " + Integer.toString(bnew) + " new bronze medals";
                    bNotification = new NotificationItem(body, TYPE_M, 0);
                    notificationItems.add(bNotification);
                    bAdded = true;
                }

                mNotificationsAdapter = new NotificationsAdapter(notificationItems, activity);
                recyclerView.setAdapter(mNotificationsAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mListener = mFirebaseDatabaseReference.child(mPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                medalComments.put(dataSnapshot.getKey(), dataSnapshot.getValue(String.class));
                switch (dataSnapshot.getValue(String.class)){
                    case "g":
                        gnew++;
                        if(initialMLoaded){
                            String body = "You won " + Integer.toString(gnew) + " new gold medals";
                            gNotification.setBody(body);
                            if(!gAdded){
                                notificationItems.add(gNotification);
                                gAdded = true;
                            }
                            mNotificationsAdapter.notifyDataSetChanged();
                        }
                        break;

                    case "s":
                        snew++;
                        if(initialMLoaded){
                            String body = "You won " + Integer.toString(snew) + " new silver medals";
                            sNotification.setBody(body);
                            if(!sAdded){
                                notificationItems.add(sNotification);
                                sAdded = true;
                            }
                            mNotificationsAdapter.notifyDataSetChanged();
                        }
                        break;

                    case "b":
                        bnew++;
                        if(initialMLoaded){
                            String body = "You won " + Integer.toString(bnew) + " new bronze medals";
                            bNotification.setBody(body);
                            if(!bAdded){
                                notificationItems.add(bNotification);
                                bAdded = true;
                            }
                            mNotificationsAdapter.notifyDataSetChanged();
                        }
                        break;
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                if(medalComments.containsKey(dataSnapshot.getKey())){
                    switch (medalComments.get(dataSnapshot.getKey())){
                        case "g":
                            gnew--;
                            gNotification.setBody("You won " + Integer.toString(gnew) + " new gold medals");
                            if(gnew == 0 && gNotification != null){ //just in case
                                notificationItems.remove(gNotification);
                            }
                            break;

                        case "s":
                            snew--;
                            sNotification.setBody("You won " + Integer.toString(snew) + " new silver medals");
                            if(snew == 0 && sNotification != null){
                                notificationItems.remove(sNotification);
                            }
                            break;

                        case "b":
                            bnew--;
                            bNotification.setBody("You won " + Integer.toString(bnew) + " new bronze medals");
                            if(bnew == 0 && bNotification != null){
                                notificationItems.remove(bNotification);
                            }
                            break;
                    }

                    medalComments.put(dataSnapshot.getKey(), dataSnapshot.getValue(String.class));

                    switch (dataSnapshot.getValue(String.class)){
                        case "g":
                            gnew++;
                            gNotification.setBody("You won " + Integer.toString(gnew) + " new gold medals");
                            break;

                        case "s":
                            snew++;
                            sNotification.setBody("You won " + Integer.toString(snew) + " new silver medals");
                            break;

                        case "b":
                            bnew++;
                            bNotification.setBody("You won " + Integer.toString(bnew) + " new bronze medals");
                            break;
                    }

                    mNotificationsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });


    }

}
