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
import com.vs.bcd.versus.adapter.NotificationsAdapter;
import com.vs.bcd.versus.model.NotificationItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private boolean fragmentVisible = false;

    String userNotificationsPath = "";


    private ChildEventListener nListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            switch (dataSnapshot.getKey()){
                case "c": //comment reply notification
                    /*
                    Log.d("fbkeys", "Comment Reply Notifications:");
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        String[] args = child.getKey().split(":",2);
                        String commentID = args[0];
                        String commentContent = args[1];
                        Log.d("fbkeys", "CommentID: " + commentID);
                        Log.d("fbkeys", "CommentContent: " + commentContent);
                        for(DataSnapshot grandchildren : child.getChildren()){
                            String username = grandchildren.getKey();
                            long gTimeValue = grandchildren.getValue(Long.class);
                            Log.d("fbkeys", "username: " + username);
                            Log.d("fbkeys", "TimeValue: " + gTimeValue);
                        }
                    }
                    */

                    break;
                case "f": //follower notification
                    /*
                    Log.d("fbkeys", "Follower Notifications:");
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        String followerUsername = child.getKey();
                        long timeValue = child.getValue(Long.class);
                        Log.d("fbkeys", "Username: " + followerUsername);
                        Log.d("fbkeys", "TimeValue: " + timeValue);
                    }
                    */

                    break;
                case "m": //medal notification
                    /*
                    Log.d("fbkeys", "Medal Notifications:");
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        String commentID = child.getKey();
                        String[] args = child.getValue(String.class).split(":",2);
                        String medalType = args[0];
                        long timeValue = Long.parseLong(args[1]);
                        Log.d("fbkeys", "commentID: " + commentID);
                        Log.d("fbkeys", "medalType: " + medalType);
                        Log.d("fbkeys", "timeValue: " + timeValue);
                    }
                    */

                    break;
                case "r": //root comment (comment to post) notification
                    /*
                    Log.d("fbkeys", "Root Comment Notifications:");
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        String[] args = child.getKey().split(":",4);

                        String postID = args[0];
                        String redName = args[1];
                        String blueName = args[2];
                        String question = args[3];

                        for(DataSnapshot grandChild : child.getChildren()){
                            String voterUsername = grandChild.getKey();
                            long timeValue = grandChild.getValue(Long.class);
                            Log.d("fbkeys", "PostID: " + postID);
                            Log.d("fbkeys", "redName: " + redName);
                            Log.d("fbkeys", "blueName: " + blueName);
                            Log.d("fbkeys", "question: " + question);
                            Log.d("fbkeys", "Username: " + voterUsername);
                            Log.d("fbkeys", "TimeValue: " + timeValue);
                        }
                    }
                    */

                    break;
                case "u": //hearts notification (comment upvote)
                    /*
                    Log.d("fbkeys", "Hearts Notifications:");
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        String[] args = child.getKey().split(":",2);
                        String commentID = args[0];
                        String commentContent = args[1];
                        Log.d("fbkeys", "CommentID: " + commentID);
                        Log.d("fbkeys", "CommentContent: " + commentContent);
                        for(DataSnapshot grandchildren : child.getChildren()){
                            String username = grandchildren.getKey();
                            long gTimeValue = grandchildren.getValue(Long.class);
                            Log.d("fbkeys", "username: " + username);
                            Log.d("fbkeys", "TimeValue: " + gTimeValue);
                        }
                    }
                    */


                    break;
                case "v": //post vote notification
                    /*
                    Log.d("fbkeys", "Post Vote Notifications:");
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        String[] args = child.getKey().split(":",4);

                        String postID = args[0];
                        String redName = args[1];
                        String blueName = args[2];
                        String question = args[3];

                        for(DataSnapshot grandChild : child.getChildren()){
                            String voterUsername = grandChild.getKey();
                            long timeValue = grandChild.getValue(Long.class);
                            Log.d("fbkeys", "PostID: " + postID);
                            Log.d("fbkeys", "redName: " + redName);
                            Log.d("fbkeys", "blueName: " + blueName);
                            Log.d("fbkeys", "question: " + question);
                            Log.d("fbkeys", "Username: " + voterUsername);
                            Log.d("fbkeys", "TimeValue: " + timeValue);
                        }
                    }
                    */

                    break;
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            for(DataSnapshot child : dataSnapshot.getChildren()){
                Log.d("key", child.getKey());


            }

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };



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

        recyclerView = rootView.findViewById(R.id.notifications_rv);
        mLayoutManager = new LinearLayoutManager(activity);
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

        newNotificationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayoutManager.scrollToPosition(mLayoutManager.getItemCount() - 1);
            }
        });

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
        mFirebaseDatabaseReference.child(userNotificationsPath).addChildEventListener(nListener);
    }

    @Override
    public void onPause(){
        super.onPause();
        mFirebaseDatabaseReference.child(userNotificationsPath).removeEventListener(nListener);
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                fragmentVisible = true;
                enableChildViews();
            }
        }
        else {
            if (rootView != null){
                fragmentVisible = false;
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
        if(topUnread){
            showNNB();
        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    public void showNNB(){
        if(fragmentVisible){
            newNotificationsButton.setEnabled(true);
            newNotificationsButton.setClickable(true);
            newNotificationsButton.setLayoutParams(nnbLP);
        }

        topUnread = true;
    }

    public void hideNNB(){
        newNotificationsButton.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        newNotificationsButton.setClickable(false);
        newNotificationsButton.setEnabled(false);
        topUnread = false;
    }

    private void checkAndSetTopButton(){ //called after notifyDataSetChanged is called
        if(mLayoutManager != null){
            if(mLayoutManager.findLastVisibleItemPosition() < mLayoutManager.getItemCount() - 1){
                showNNB();
            }
        }
    }

}
