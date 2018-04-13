package com.vs.bcd.versus.fragment;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.RNumAndUList;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.SessionManager;

import java.text.ParseException;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by dlee on 8/6/17.
 */



public class MessengerFragment extends Fragment {

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomTitleTV;
        TextView roomTimeTV;
        TextView roomPreviewTV;

        //CircleImageView roomImageView;    //TODO: implement circular image view for rooms

        public RoomViewHolder(View v) {
            super(v);
            roomTitleTV = itemView.findViewById(R.id.roomNameTV);
            roomTimeTV = itemView.findViewById(R.id.roomTimeTV);
            roomPreviewTV = itemView.findViewById(R.id.roomPreviewTV);
            //roomImageView
        }
    }

    private String ROOMS_CHILD = "";
    private final int REQUEST_IMAGE = 2;
    private final int RESULT_OK = -1;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";    //TODO: replace with our own

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<RoomObject, RoomViewHolder> mFirebaseAdapter;
    private FirebaseAnalytics mFirebaseAnalytics;
    private RecyclerView mRoomRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private String mUsername = "";
    private int mPhotoUrl = 0;
    private String userMKey = "";
    private MainContainer activity;
    private FloatingActionButton fabNewMsg;
    private ChildEventListener roomsListener;
    private HashMap<String, RNumAndUList> rNameToRNum;
    private TextView emptyListTV;
    private Query query;
    private int retrievalSize = 12;
    private int initialRetrievalSize = retrievalSize * 2; //has to be integer multiple of retrievalSize for current code for loadMore to work //TODO: increase this
    private int loadThreshold = 3;
    private boolean nowLoading = false;
    private boolean modifyNowLoading = false;
    private int previousAdapterItemCount = 0;
    private int previousAdapterScrollPosition = 0;
    private int totalRoomCount = Integer.MAX_VALUE;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab4messenger, container, false);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = rootView.findViewById(R.id.roomsProgressBar);
        emptyListTV = rootView.findViewById(R.id.emptyListText);
        mRoomRecyclerView = rootView.findViewById(R.id.roomsRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setStackFromEnd(true);
        mLinearLayoutManager.setReverseLayout(true);
        mRoomRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRoomRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //only if postSearchResults.size()%retrievalSize == 0, meaning it's possible there's more matching documents for this search
                if(!nowLoading && mFirebaseAdapter != null && mFirebaseAdapter.getItemCount()%retrievalSize == 0) {
                    LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                    int firstVisible = layoutManager.findFirstVisibleItemPosition(); //reversed recyclerview so we're getting first visible instead of last visible
                    if(firstVisible < 3){
                        int retrievalMultiplier = (mFirebaseAdapter.getItemCount() / retrievalSize) - 1;
                        if(!(initialRetrievalSize + retrievalSize * retrievalMultiplier < mFirebaseAdapter.getItemCount())){
                            if (mFirebaseAdapter.getItemCount() > 0) {
                                //you have reached to the bottom of your recycler view
                                nowLoading = true;
                                if(initialRetrievalSize + retrievalSize * retrievalMultiplier >= totalRoomCount){
                                    loadMoreRooms(-1);
                                }
                                else{
                                    loadMoreRooms(retrievalMultiplier);
                                }
                            }
                        }
                    }
                }
            }
        });

        userMKey = ((MainContainer)getActivity()).getUserMKey();
        mPhotoUrl = activity.getUserProfileImageVersion();

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        disableChildViews();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        fabNewMsg = rootView.findViewById(R.id.fab_new_msg);
        fabNewMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: open Send New Message UI (so also implement that UI in XML) and set up related stuff
                activity.getViewPager().setCurrentItem(13);
            }
        });

        return rootView;
    }

    private void loadMoreRooms(int retrievalMultiplier){
        mRoomRecyclerView.setAdapter(null);
        mProgressBar.setVisibility(View.VISIBLE);
        final FirebaseRecyclerAdapter oldAdapter = mFirebaseAdapter;

        Log.d("loadmorecalled", "hello");
        previousAdapterItemCount = mFirebaseAdapter.getItemCount();
        rNameToRNum = new HashMap<>();

        if(retrievalMultiplier == -1){

            Log.d("loadmorecalled", "hello2");
            query = mFirebaseDatabaseReference.child(ROOMS_CHILD).orderByChild("time");

            modifyNowLoading = true;

            FirebaseRecyclerOptions<RoomObject> options =
                    new FirebaseRecyclerOptions.Builder<RoomObject>()
                            .setLifecycleOwner(this)
                            .setQuery(query, RoomObject.class)
                            .build();

            final FirebaseRecyclerAdapter freshFirebaseAdapter = new FirebaseRecyclerAdapter<RoomObject, RoomViewHolder>(options) {

                @Override
                public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(activity).inflate(R.layout.room_item_view, parent, false);
                    return new RoomViewHolder(view);
                }

                @Override
                protected void onBindViewHolder(final RoomViewHolder viewHolder, final int position, final RoomObject roomObject) {

                    if(modifyNowLoading){
                        modifyNowLoading = false;

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mRoomRecyclerView.scrollToPosition(previousAdapterScrollPosition);
                            }
                        }, 1);
                        oldAdapter.stopListening();
                    }

                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    emptyListTV.setVisibility(TextView.INVISIBLE);
                    if (roomObject != null) {

                        final ArrayList<String> usersList = roomObject.getUsers();
                        final String roomTitle = roomObject.getName();
                        final String roomNum = getRef(position).getKey();

                        rNameToRNum.put(roomTitle, new RNumAndUList(roomNum, usersList)); //TODO: will this get updated if room is updated with modified usersList?

                        viewHolder.roomTitleTV.setText(roomTitle);
                        viewHolder.roomTimeTV.setText(getMessengerTimeString(roomObject.getTime()));
                        viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                usersList.remove(mUsername); //remove logged-in user from the room users map to prevent duplicate sends,
                                // since we handle logged-in user's message transfer separate from message transfer of other room users

                                if(roomNum != null && usersList != null){
                                    activity.setUpAndOpenMessageRoom(roomNum, usersList, roomTitle);
                                }
                                else{
                                    Log.d("MESSENGER", "roomNum is null");
                                }
                            }
                        });
                    }
                    else {
                        Log.d("roomSetUp", "title null");
                    }

                }
            };

            mFirebaseAdapter = freshFirebaseAdapter;
            mRoomRecyclerView.setAdapter(mFirebaseAdapter);
        }
        else{

            Log.d("loadmorecalled", "hello3");
            query = mFirebaseDatabaseReference.child(ROOMS_CHILD).orderByChild("time").limitToLast(initialRetrievalSize + retrievalSize * retrievalMultiplier);

            if(retrievalMultiplier > 0){
                modifyNowLoading = true;

                FirebaseRecyclerOptions<RoomObject> options =
                        new FirebaseRecyclerOptions.Builder<RoomObject>()
                                .setLifecycleOwner(this)
                                .setQuery(query, RoomObject.class)
                                .build();

                final FirebaseRecyclerAdapter freshFirebaseAdapter = new FirebaseRecyclerAdapter<RoomObject, RoomViewHolder>(options) {

                    @Override
                    public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(activity).inflate(R.layout.room_item_view, parent, false);
                        return new RoomViewHolder(view);
                    }

                    @Override
                    protected void onBindViewHolder(final RoomViewHolder viewHolder, final int position, final RoomObject roomObject) {
                        if(modifyNowLoading){
                            modifyNowLoading = false;
                            if(mFirebaseAdapter.getItemCount()-previousAdapterItemCount < retrievalSize){
                                nowLoading = true;
                                previousAdapterScrollPosition = ((LinearLayoutManager)mRoomRecyclerView.getLayoutManager()).findFirstVisibleItemPosition(); //reversed recyclerview so we're getting first visible instead of last visible
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        loadMoreRooms(-1);
                                    }
                                }, 1);
                                return;
                            }
                            else{
                                nowLoading = false;
                            }
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mRoomRecyclerView.scrollToPosition(mFirebaseAdapter.getItemCount()-previousAdapterItemCount+2);
                                }
                            }, 1);
                            oldAdapter.stopListening();
                        }

                        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                        emptyListTV.setVisibility(TextView.INVISIBLE);
                        if (roomObject != null) {

                            final ArrayList<String> usersList = roomObject.getUsers();
                            final String roomTitle = roomObject.getName();
                            final String roomNum = getRef(position).getKey();

                            rNameToRNum.put(roomTitle, new RNumAndUList(roomNum, usersList)); //TODO: will this get updated if room is updated with modified usersList?

                            viewHolder.roomTitleTV.setText(roomTitle);
                            viewHolder.roomTimeTV.setText(getMessengerTimeString(roomObject.getTime()));
                            viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                    usersList.remove(mUsername); //remove logged-in user from the room users map to prevent duplicate sends,
                                    // since we handle logged-in user's message transfer separate from message transfer of other room users

                                    if(roomNum != null && usersList != null){
                                        activity.setUpAndOpenMessageRoom(roomNum, usersList, roomTitle);
                                    }
                                    else{
                                        Log.d("MESSENGER", "roomNum is null");
                                    }
                                }
                            });
                        }
                        else {
                            Log.d("roomSetUp", "title null");
                        }

                    }
                };

                mFirebaseAdapter = freshFirebaseAdapter;
                mRoomRecyclerView.setAdapter(mFirebaseAdapter);
            }
        }

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

        ROOMS_CHILD = Integer.toString(usernameHash) + "/" + mUsername + "/r";
    }

    @Override
    public void onResume(){
        super.onResume();

        if(mFirebaseAdapter != null){
            mFirebaseAdapter.startListening();
        }
        else{
            rNameToRNum = new HashMap<>();

            query = mFirebaseDatabaseReference.child(ROOMS_CHILD).orderByChild("time").limitToLast(initialRetrievalSize);

            FirebaseRecyclerOptions<RoomObject> options =
                    new FirebaseRecyclerOptions.Builder<RoomObject>()
                            .setLifecycleOwner(this)
                            .setQuery(query, RoomObject.class)
                            .build();

            mFirebaseAdapter = new FirebaseRecyclerAdapter<RoomObject, RoomViewHolder>(options) {

                @Override
                public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(activity).inflate(R.layout.room_item_view, parent, false);
                    return new RoomViewHolder(view);
                }

                @Override
                protected void onBindViewHolder(final RoomViewHolder viewHolder, final int position, final RoomObject roomObject) {

                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    emptyListTV.setVisibility(TextView.INVISIBLE);
                    if (roomObject != null) {

                        final ArrayList<String> usersList = roomObject.getUsers();
                        final String roomTitle = roomObject.getName();
                        final String roomNum = getRef(position).getKey();

                        rNameToRNum.put(roomTitle, new RNumAndUList(roomNum, usersList)); //TODO: will this get updated if room is updated with modified usersList?

                        viewHolder.roomTitleTV.setText(roomTitle);
                        viewHolder.roomTimeTV.setText(getMessengerTimeString(roomObject.getTime()));
                        viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                usersList.remove(mUsername); //remove logged-in user from the room users map to prevent duplicate sends,
                                // since we handle logged-in user's message transfer separate from message transfer of other room users

                                if(roomNum != null && usersList != null){
                                    activity.setUpAndOpenMessageRoom(roomNum, usersList, roomTitle);
                                }
                                else{
                                    Log.d("MESSENGER", "roomNum is null");
                                }
                            }
                        });
                    }
                    else {
                        Log.d("roomSetUp", "title null");
                    }

                }
            };

            mRoomRecyclerView.setAdapter(mFirebaseAdapter);

            mFirebaseDatabaseReference.child(ROOMS_CHILD).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    if(!(dataSnapshot.hasChildren())){
                        emptyListTV.setVisibility(TextView.VISIBLE);
                    }
                    else{
                        totalRoomCount = (int) dataSnapshot.getChildrenCount();
                    }
                    setFirebaseObserver();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void setFirebaseObserver(){
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int roomCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (roomCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mRoomRecyclerView.scrollToPosition(positionStart);
                }
            }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount){
                super.onItemRangeRemoved(positionStart, itemCount);
                if(mFirebaseAdapter.getItemCount() == 0){
                    emptyListTV.setVisibility(TextView.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mFirebaseAdapter != null){
            mFirebaseAdapter.stopListening();
            //mRoomRecyclerView.setAdapter(null);
        }
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

    //returns null if entry not found, else returns the corresponding RNumAndUList object
    public RNumAndUList getRNumAndUList(String rName){
        return rNameToRNum.get(rName);
    }

    private String getMessengerTimeString(long epochTime){
        Calendar chatTime = Calendar.getInstance();
        int currentYear = chatTime.get(Calendar.YEAR);
        int currentMonth = chatTime.get(Calendar.MONTH);
        int currentDay = chatTime.get(Calendar.DAY_OF_MONTH);

        chatTime.setTimeInMillis(epochTime);
        int year = chatTime.get(Calendar.YEAR);
        int month = chatTime.get(Calendar.MONTH);
        int day = chatTime.get(Calendar.DAY_OF_MONTH);

        if(year == currentYear){
            if(month == currentMonth && day == currentDay){ //format = hh:mm
                int hour = chatTime.get(Calendar.HOUR);
                int minute = chatTime.get(Calendar.MINUTE);
                String minuteString;
                if(minute > 10) {
                    minuteString = Integer.toString(minute);
                }
                else{
                    minuteString = "0" + Integer.toString(minute);
                }

                String timeString = Integer.toString(hour)+":"+minuteString;
                if(chatTime.get(Calendar.AM_PM) == Calendar.PM){
                    timeString = timeString.concat(" pm");
                }
                else{
                    timeString = timeString.concat(" am");
                }

                return timeString;
            }
            else{ //format = mm/dd
                return Integer.toString(month+1)+"/"+Integer.toString(day); //Calendar's month starts at 0 so add 1
            }
        }
        else { //format = mm/dd/yy
            return Integer.toString(month+1)+"/"+Integer.toString(day)+"/"+Integer.toString(year); //Calendar's month starts at 0 so add 1
        }
    }

}
