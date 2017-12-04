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
import android.widget.Button;
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
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.UserSearchItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static android.R.string.no;

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
    private Button newNotificationsButton;
    private RelativeLayout.LayoutParams nnbLP;
    private LinearLayoutManager mLayoutManager;

    private DatabaseReference mFirebaseDatabaseReference;
    private ChildEventListener uListener, cListener, vListener, rListener, fListener, mListener;
    private String uPath, cPath, vPath, rPath, fPath, mPath;
    private HashMap<String, Long> newFollowers;
    private int gnew, snew, bnew;
    private HashMap<String, String> medalComments;
    private NotificationItem fNotification, gNotification, sNotification, bNotification;
    private boolean initialFLoaded = false;
    private boolean fNotificationAdded = false;
    private boolean initialMLoaded = false;
    private boolean gAdded, sAdded, bAdded;
    private long fTime, gTime, sTime, bTime;
    private boolean initialULoaded, initialVLoaded, initialCLoaded, initialRLoaded;
    private boolean topUnread = false;

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
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mLayoutManager.findLastVisibleItemPosition() == mLayoutManager.getItemCount() - 1) {
                    hideNNB();
                }
            }
        });

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        newNotificationsButton = (Button) rootView.findViewById(R.id.new_notifications);
        nnbLP = (RelativeLayout.LayoutParams) newNotificationsButton.getLayoutParams();

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
        for(int i = 1; i<childViews.size(); i++){   //skip i == 0, that's the newNotificationsButton, which is enabled separately as needed
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

        fTime = 0;
        initialFLoaded = false;
        gTime = 0;
        sTime = 0;
        bTime = 0;

        initialULoaded = false;
        initialCLoaded = false;
        initialVLoaded = false;
        initialRLoaded = false;

        topUnread = false;

        mFirebaseDatabaseReference.child(uPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                initialULoaded = true;
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

                if(initialULoaded && mNotificationsAdapter != null){
                    mNotificationsAdapter.notifyDataSetChanged();
                    checkAndSetTopButton();
                }
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
                    NotificationItem temp = notificationItemsMap.get("U:"+commentID);
                    notificationItems.remove(temp);
                    temp.setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItems.add(temp);
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
                checkAndSetTopButton();

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
                initialCLoaded = true;
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

                if(initialCLoaded && mNotificationsAdapter != null){
                    mNotificationsAdapter.notifyDataSetChanged();
                    checkAndSetTopButton();
                }
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
                    NotificationItem temp = notificationItemsMap.get("C:"+commentID);
                    notificationItems.remove(temp);
                    temp.setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItems.add(temp);
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
                checkAndSetTopButton();

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
                initialVLoaded = true;
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

                if(initialVLoaded && mNotificationsAdapter != null){
                    mNotificationsAdapter.notifyDataSetChanged();
                    checkAndSetTopButton();
                }
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
                    NotificationItem temp = notificationItemsMap.get("V:"+postID);
                    notificationItems.remove(temp);
                    temp.setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItems.add(temp);
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
                checkAndSetTopButton();

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
                initialRLoaded = true;
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

                if(initialRLoaded && mNotificationsAdapter != null){
                    mNotificationsAdapter.notifyDataSetChanged();
                    checkAndSetTopButton();
                }
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
                    NotificationItem temp = notificationItemsMap.get("R:"+postID);
                    notificationItems.remove(temp);
                    temp.setBody(body).setTimestamp(dataSnapshot.getChildren().iterator().next().getValue(Long.class));
                    notificationItems.add(temp);
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
                checkAndSetTopButton();

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
                //Log.d("POSPOS", Integer.toString(mLayoutManager.findFirstVisibleItemPosition()) + " and " + Integer.toString(mLayoutManager.findLastVisibleItemPosition()));
                newFollowers.put(dataSnapshot.getKey(), dataSnapshot.getValue(Long.class));
                if(dataSnapshot.getValue(Long.class) > fTime){
                    fTime = dataSnapshot.getValue(Long.class);
                }
                if(initialFLoaded){
                    String body = "You have " + Integer.toString(newFollowers.size()) + " new followers!";

                    if(newFollowers.size() == 1){
                        body = "You have a new follower!";
                    }
                    Log.d("deet", Long.toString(fTime)+" is long");
                    fNotification.setBody(body).setTimestamp(fTime);
                    if(!fNotificationAdded){
                        notificationItems.add(fNotification);
                        fNotificationAdded = true;
                    }
                    else{
                        notificationItems.remove(fNotification);
                        notificationItems.add(fNotification);
                    }
                    mNotificationsAdapter.notifyDataSetChanged();
                    checkAndSetTopButton();
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
                    String body = "You won " + Integer.toString(gnew) + " new gold medals!";
                    gNotification = new NotificationItem(body, TYPE_M, gTime);
                    notificationItems.add(gNotification);
                    gAdded = true;
                }

                if(snew > 0){
                    String body = "You won " + Integer.toString(snew) + " new silver medals!";
                    sNotification = new NotificationItem(body, TYPE_M, sTime);
                    notificationItems.add(sNotification);
                    sAdded = true;
                }

                if(bnew > 0){
                    String body = "You won " + Integer.toString(bnew) + " new bronze medals!";
                    bNotification = new NotificationItem(body, TYPE_M, bTime);
                    notificationItems.add(bNotification);
                    bAdded = true;
                }

                //sort the assembledResults by popularity velocity
                Collections.sort(notificationItems, new Comparator<NotificationItem>() {
                    //TODO: confirm that this sorts dates where most recent is at top. If not then just flip around o1 and o2: change to o2.getDate().compareTo(o1.getDate())
                    @Override
                    public int compare(NotificationItem o1, NotificationItem o2) {
                        return (int)(o1.getTimestamp() - o2.getTimestamp());
                    }
                });

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
                String[] args = dataSnapshot.getValue(String.class).split(":",2);
                String medalType = args[0];
                long timeValue = Long.parseLong(args[1]) / 1000;

                medalComments.put(dataSnapshot.getKey(), medalType);

                switch (medalType){
                    case "g":
                        gnew++;
                        if(timeValue > gTime){
                            gTime = timeValue;
                        }
                        if(initialMLoaded){
                            String body = "You won a new gold medal!";
                            if(gnew > 1){
                                body = "You won " + Integer.toString(gnew) + " new gold medals!";
                            }
                            if(!gAdded){
                                gNotification = new NotificationItem(body, TYPE_M, gTime);
                                notificationItems.add(gNotification);
                                gAdded = true;
                            }
                            else{
                                notificationItems.remove(gNotification);
                                gNotification.setBody(body);
                                notificationItems.add(gNotification);
                            }
                            mNotificationsAdapter.notifyDataSetChanged();
                            checkAndSetTopButton();
                        }
                        break;

                    case "s":
                        snew++;
                        if(timeValue > sTime){
                            sTime = timeValue;
                        }
                        if(initialMLoaded){
                            String body = "You won a new silver medal!";
                            if(snew > 1){
                                body = "You won " + Integer.toString(snew) + " new silver medals!";
                            }
                            if(!sAdded){
                                sNotification = new NotificationItem(body, TYPE_M, sTime);
                                notificationItems.add(sNotification);
                                sAdded = true;
                            }
                            else{
                                notificationItems.remove(sNotification);
                                sNotification.setBody(body);
                                notificationItems.add(sNotification);
                            }
                            mNotificationsAdapter.notifyDataSetChanged();
                            checkAndSetTopButton();
                        }
                        break;

                    case "b":
                        bnew++;
                        if(timeValue > bTime){
                            bTime = timeValue;
                        }
                        if(initialMLoaded){
                            String body = "You won a new bronze medal!";
                            if(bnew > 1){
                                body = "You won " + Integer.toString(bnew) + " new bronze medals!";
                            }
                            if(!bAdded){
                                bNotification = new NotificationItem(body, TYPE_M, bTime);
                                notificationItems.add(bNotification);
                                bAdded = true;
                            }
                            else{
                                notificationItems.remove(bNotification);
                                bNotification.setBody(body);
                                notificationItems.add(bNotification);
                            }
                            mNotificationsAdapter.notifyDataSetChanged();
                            checkAndSetTopButton();
                        }
                        break;
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

                String[] args = dataSnapshot.getValue(String.class).split(":",2);
                String medalType = args[0];
                long timeValue = Long.parseLong(args[1]) / 1000;

                if(medalComments.containsKey(dataSnapshot.getKey())){
                    switch (medalComments.get(dataSnapshot.getKey())){
                        case "g":
                            gnew--;
                            if(gNotification != null){
                                switch (gnew){
                                    case 0:
                                        notificationItems.remove(gNotification);
                                        gAdded = false;
                                        break;
                                    case 1:
                                        gNotification.setBody("You won a new gold medal!");
                                        if(!gAdded){
                                            notificationItems.add(gNotification);
                                            gAdded = true;
                                        }
                                        break;
                                    default:
                                        gNotification.setBody("You won " + Integer.toString(gnew) + " new gold medals!");
                                        if(!gAdded){
                                            notificationItems.add(gNotification);
                                            gAdded = true;
                                        }
                                        break;
                                }
                            }
                            break;

                        case "s":
                            snew--;
                            if(sNotification != null){
                                switch (snew){
                                    case 0:
                                        notificationItems.remove(sNotification);
                                        sAdded = false;
                                        break;
                                    case 1:
                                        sNotification.setBody("You won a new silver medal!");
                                        if(!sAdded){
                                            notificationItems.add(sNotification);
                                            sAdded = true;
                                        }
                                        break;
                                    default:
                                        sNotification.setBody("You won " + Integer.toString(snew) + " new silver medals!");
                                        if(!sAdded){
                                            notificationItems.add(sNotification);
                                            sAdded = true;
                                        }
                                        break;
                                }
                            }
                            break;

                        case "b":
                            bnew--;
                            if(bNotification != null){
                                switch (bnew){
                                    case 0:
                                        notificationItems.remove(bNotification);
                                        bAdded = false;
                                        break;
                                    case 1:
                                        bNotification.setBody("You won a new bronze medal!");
                                        if(!bAdded){
                                            notificationItems.add(bNotification);
                                            bAdded = true;
                                        }
                                        break;
                                    default:
                                        bNotification.setBody("You won " + Integer.toString(bnew) + " new bronze medals!");
                                        if(!bAdded){
                                            notificationItems.add(bNotification);
                                            bAdded = true;
                                        }
                                        break;
                                }
                            }
                            break;
                    }

                    medalComments.put(dataSnapshot.getKey(), medalType);

                    switch (medalType){
                        case "g":
                            gnew++;
                            if(gNotification != null){
                                switch (gnew){
                                    case 1:
                                        gNotification.setBody("You won a new gold medal!").setTimestamp(timeValue);
                                        if(!gAdded){
                                            notificationItems.add(gNotification);
                                            gAdded = true;
                                        }
                                        else{
                                            notificationItems.remove(gNotification);
                                            notificationItems.add(gNotification);
                                        }
                                        break;
                                    default:
                                        gNotification.setBody("You won " + Integer.toString(gnew) + " new gold medals!").setTimestamp(timeValue);
                                        if(!gAdded){
                                            notificationItems.add(gNotification);
                                            gAdded = true;
                                        }
                                        else{
                                            notificationItems.remove(gNotification);
                                            notificationItems.add(gNotification);
                                        }
                                        break;
                                }
                            }
                            break;

                        case "s":
                            snew++;
                            if(sNotification != null){
                                switch (snew){
                                    case 1:
                                        sNotification.setBody("You won a new silver medal!").setTimestamp(timeValue);
                                        if(!sAdded){
                                            notificationItems.add(sNotification);
                                            sAdded = true;
                                        }
                                        else{
                                            notificationItems.remove(sNotification);
                                            notificationItems.add(sNotification);
                                        }
                                        break;
                                    default:
                                        sNotification.setBody("You won " + Integer.toString(snew) + " new silver medals!").setTimestamp(timeValue);
                                        if(!sAdded){
                                            notificationItems.add(sNotification);
                                            sAdded = true;
                                        }
                                        else{
                                            notificationItems.remove(sNotification);
                                            notificationItems.add(sNotification);
                                        }
                                        break;
                                }
                            }
                            break;

                        case "b":
                            bnew++;
                            if(bNotification != null){
                                switch (bnew){
                                    case 1:
                                        bNotification.setBody("You won a new bronze medal!").setTimestamp(timeValue);
                                        if(!bAdded){
                                            notificationItems.add(bNotification);
                                            bAdded = true;
                                        }
                                        else{
                                            notificationItems.remove(bNotification);
                                            notificationItems.add(bNotification);
                                        }
                                        break;
                                    default:
                                        bNotification.setBody("You won " + Integer.toString(bnew) + " new bronze medals!").setTimestamp(timeValue);
                                        if(!bAdded){
                                            notificationItems.add(bNotification);
                                            bAdded = true;
                                        }
                                        else{
                                            notificationItems.remove(bNotification);
                                            notificationItems.add(bNotification);
                                        }
                                        break;
                                }
                            }
                            break;
                    }

                    mNotificationsAdapter.notifyDataSetChanged();
                    checkAndSetTopButton();
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

    public void showNNB(){
        newNotificationsButton.setEnabled(true);
        newNotificationsButton.setClickable(true);
        newNotificationsButton.setLayoutParams(nnbLP);
    }

    public void hideNNB(){
        newNotificationsButton.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        newNotificationsButton.setClickable(false);
        newNotificationsButton.setEnabled(false);
    }

    private void checkAndSetTopButton(){ //called after notifyDataSetChanged is called
        if(mLayoutManager != null){
            if(mLayoutManager.findLastVisibleItemPosition() < mLayoutManager.getItemCount() - 1){
                showNNB();
            }
        }
    }

}
