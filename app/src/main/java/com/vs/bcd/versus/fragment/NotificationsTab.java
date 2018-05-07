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
import java.util.concurrent.atomic.AtomicInteger;

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
    private boolean initialLoadComplete = false;

    private int cCount, rCount, uCount, vCount;
    private AtomicInteger typeChildCount;
    String userNotificationsPath = "";


    private ValueEventListener initialListner = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            typeChildCount = new AtomicInteger((int) dataSnapshot.getChildrenCount());

            for(DataSnapshot typeChild : dataSnapshot.getChildren()){
                switch (typeChild.getKey()){
                    case "c": //comment reply notification
                        cCount = (int)typeChild.getChildrenCount();
                        if(cCount == 0){
                            if(typeChildCount.decrementAndGet() == 0){
                                finalizeList();
                            }
                        }

                        for(DataSnapshot child : typeChild.getChildren()){
                            String[] args = child.getKey().split(":",2);
                            final String commentID = args[0];
                            final String commentContent = args[1];

                            mFirebaseDatabaseReference.child(userNotificationsPath+"c/"+child.getKey()).orderByValue().limitToLast(8).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    StringBuilder usernames = new StringBuilder();
                                    int i = 0;
                                    long timeValue = System.currentTimeMillis();

                                    for(DataSnapshot grandchildren : dataSnapshot.getChildren()){
                                        usernames.insert(0, grandchildren.getKey()+", ");
                                        if(i == 0){
                                            timeValue = grandchildren.getValue(Long.class);
                                        }
                                        i++;
                                    }
                                    String usernamesString = usernames.toString();
                                    if(usernamesString.length() >= 26){
                                        usernamesString = usernamesString.substring(0, 26);
                                        usernamesString = usernamesString.substring(0, usernamesString.lastIndexOf(", "));
                                        usernamesString = usernamesString + "...";
                                    }
                                    else{
                                        usernamesString = usernamesString.substring(0, usernamesString.lastIndexOf(", "));
                                    }
                                    String body = usernamesString + "\nreplied to your comment, \"" + commentContent.replace('^', ' ') + "\"";
                                    notificationItems.add(new NotificationItem(body, TYPE_C, commentID, timeValue));

                                    cCount--;
                                    if(cCount == 0){
                                        if(typeChildCount.decrementAndGet() == 0){
                                            finalizeList();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                        }

                        break;
                    case "f": //follower notification

                        mFirebaseDatabaseReference.child(userNotificationsPath+"f/").orderByValue().limitToLast(8).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                StringBuilder usernames = new StringBuilder();
                                int i = 0;
                                long timeValue = System.currentTimeMillis();

                                for(DataSnapshot grandchildren : dataSnapshot.getChildren()){
                                    usernames.insert(0, grandchildren.getKey()+", ");
                                    if(i == 0){
                                        timeValue = grandchildren.getValue(Long.class);
                                    }
                                    i++;
                                }
                                String usernamesString = usernames.toString();
                                if(usernamesString.length() >= 26){
                                    usernamesString = usernamesString.substring(0, 26);
                                    usernamesString = usernamesString.substring(0, usernamesString.lastIndexOf(", "));
                                    usernamesString = usernamesString + "...";
                                }
                                else{
                                    usernamesString = usernamesString.substring(0, usernamesString.lastIndexOf(", "));
                                }
                                String body = usernamesString + "\nstarted following you!";
                                notificationItems.add(new NotificationItem(body, TYPE_F, timeValue));

                                if(typeChildCount.decrementAndGet() == 0){
                                    finalizeList();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        break;
                    case "m": //medal notification

                        for(DataSnapshot child : typeChild.getChildren()){
                            String commentID = child.getKey();
                            String[] args = child.getValue(String.class).split(":",3);
                            String medalType = args[0];
                            long timeValue = Long.parseLong(args[1]);
                            String commentContent = args[2];
                            String header;
                            switch (medalType){
                                case "g":
                                    header = "Congratulations! You won a Gold Medal for,";
                                    break;
                                case "s":
                                    header = "Congratulations! You won a Silver Medal for,";
                                    break;
                                case "b":
                                    header = "Congratulations! You won a Bronze Medal for,";
                                    break;
                                default:
                                    header = "Congratulations! You won a medal for,";
                                    break;
                            }

                            String body = header + "\n\""+commentContent+"\"";
                            notificationItems.add(new NotificationItem(body, TYPE_M, commentID, timeValue, medalType));
                        }
                        if(typeChildCount.decrementAndGet() == 0){
                            finalizeList();
                        }

                        break;
                    case "r": //root comment (comment to post) notification
                        rCount = (int) typeChild.getChildrenCount();

                        if(rCount == 0){
                            if(typeChildCount.decrementAndGet() == 0){
                                finalizeList();
                            }
                        }

                        for(DataSnapshot child : typeChild.getChildren()){
                            String[] args = child.getKey().split(":",3);

                            final String postID = args[0];
                            final String redName = args[1].replace('^', ' ');
                            final String blueName = args[2].replace('^', ' ');


                            mFirebaseDatabaseReference.child(userNotificationsPath+"r/"+child.getKey()).orderByValue().limitToLast(8).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    StringBuilder usernames = new StringBuilder();
                                    int i = 0;
                                    long timeValue = System.currentTimeMillis();

                                    for(DataSnapshot grandchildren : dataSnapshot.getChildren()){
                                        usernames.insert(0, grandchildren.getKey()+", ");
                                        if(i == 0){
                                            timeValue = grandchildren.getValue(Long.class);
                                        }
                                        i++;
                                    }
                                    String usernamesString = usernames.toString();
                                    if(usernamesString.length() >= 26){
                                        usernamesString = usernamesString.substring(0, 26);
                                        usernamesString = usernamesString.substring(0, usernamesString.lastIndexOf(", "));
                                        usernamesString = usernamesString + "...";
                                    }
                                    else{
                                        usernamesString = usernamesString.substring(0, usernamesString.lastIndexOf(", "));
                                    }
                                    String body = usernamesString + "\ncommented on \"" + redName + " vs. " + blueName + "\"";
                                    notificationItems.add(new NotificationItem(body, TYPE_R, postID, timeValue));

                                    rCount--;
                                    if(rCount == 0){
                                        if(typeChildCount.decrementAndGet() == 0){
                                            finalizeList();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                        }

                        break;
                    case "u": //hearts notification (comment upvote)

                        uCount = (int) typeChild.getChildrenCount();
                        if(uCount == 0){
                            if(typeChildCount.decrementAndGet() == 0){
                                finalizeList();
                            }
                        }

                        for(DataSnapshot child : typeChild.getChildren()){
                            String[] args = child.getKey().split(":",2);
                            final String commentID = args[0];
                            final String commentContent = args[1];
                            final int newHeartsCount = (int) child.getChildrenCount();

                            mFirebaseDatabaseReference.child(userNotificationsPath+"u/"+child.getKey()).orderByValue().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot mostRecent : dataSnapshot.getChildren()){
                                        long timeValue = mostRecent.getValue(Long.class);
                                        String body;
                                        if(newHeartsCount == 1) {
                                            body = "You got " + Integer.toString(newHeartsCount) + " Heart on a comment, \""
                                                    + commentContent.replace('^', ' ') + "\"";
                                        }
                                        else{
                                            body = "You got " + Integer.toString(newHeartsCount) + " Hearts on a comment, \""
                                                    + commentContent.replace('^', ' ') + "\"";
                                        }

                                        notificationItems.add(new NotificationItem(body, TYPE_U, commentID, timeValue));

                                        uCount--;
                                        if(uCount == 0){
                                            if(typeChildCount.decrementAndGet() == 0){
                                                finalizeList();
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

                        break;
                    case "v": //post vote notification
                        vCount = (int) typeChild.getChildrenCount();
                        if(vCount == 0){
                            if(typeChildCount.decrementAndGet() == 0){
                                finalizeList();
                            }
                        }

                        for(DataSnapshot child : typeChild.getChildren()){
                            String[] args = child.getKey().split(":",4);

                            final String postID = args[0];
                            final String redName = args[1].replace('^', ' ');
                            final String blueName = args[2].replace('^', ' ');
                            final String question = args[3].replace('^', ' ');
                            final int newVotesCount = (int) child.getChildrenCount();

                            mFirebaseDatabaseReference.child(userNotificationsPath+"v/"+child.getKey()).orderByValue().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for(DataSnapshot mostRecent : dataSnapshot.getChildren()){
                                        long timeValue = mostRecent.getValue(Long.class);
                                        String body;
                                        if(newVotesCount == 1){
                                            body = "You got " + Integer.toString(newVotesCount) + " New Vote on your post\n"
                                                    + question + "\n\"" + redName + " vs. " + blueName + "\"";
                                        }
                                        else{
                                            body = "You got " + Integer.toString(newVotesCount) + " New Votes on your post\n"
                                                    + question + "\n\"" + redName + " vs. " + blueName + "\"";
                                        }

                                        notificationItems.add(new NotificationItem(body, TYPE_V, postID, timeValue));
                                        vCount--;
                                        if(vCount == 0){
                                            if(typeChildCount.decrementAndGet() == 0){
                                                finalizeList();
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

                        break;
                }

            }

            initialLoadComplete = true;

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    private ChildEventListener nListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot typeChild, String s) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {


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

        mNotificationsAdapter = new NotificationsAdapter(notificationItems, activity);
        recyclerView.setAdapter(mNotificationsAdapter);

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
        notificationItems.clear();
        initialLoadComplete = false;
        mFirebaseDatabaseReference.child(userNotificationsPath).addListenerForSingleValueEvent(initialListner);
    }

    @Override
    public void onPause(){
        super.onPause();
        //mFirebaseDatabaseReference.child(userNotificationsPath).removeEventListener(nListener);
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

    private void checkAndSetTopButton() { //called after notifyDataSetChanged is called
        if (mLayoutManager != null) {
            if (mLayoutManager.findLastVisibleItemPosition() < mLayoutManager.getItemCount() - 1) {
                showNNB();
            }
        }
    }

    private void finalizeList(){
        //sort the assembledResults by time
        Collections.sort(notificationItems, new Comparator<NotificationItem>() {
            @Override
            public int compare(NotificationItem o1, NotificationItem o2) {
                return (int)(o1.getTimestamp() - o2.getTimestamp());
            }
        });
        mNotificationsAdapter.notifyDataSetChanged();
    }

}
