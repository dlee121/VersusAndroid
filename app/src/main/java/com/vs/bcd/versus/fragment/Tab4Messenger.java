package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.SessionManager;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by dlee on 8/6/17.
 */



public class Tab4Messenger extends Fragment {

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomTitleTV;
        TextView roomTimeTV;
        TextView roomPreviewTV;

        //CircleImageView roomImageView;    //TODO: implement circular image view for rooms

        public RoomViewHolder(View v) {
            super(v);
            roomTitleTV = (TextView) itemView.findViewById(R.id.roomNameTV);
            roomTimeTV = (TextView) itemView.findViewById(R.id.roomTimeTV);
            roomPreviewTV = (TextView) itemView.findViewById(R.id.roomPreviewTV);
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
    private String mPhotoUrl = "";
    private String userMKey = "";
    private SimpleDateFormat df;
    private MainContainer activity;
    private FloatingActionButton fabNewMsg;
    private ChildEventListener roomsListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab4messenger, container, false);

        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.roomsProgressBar);
        mRoomRecyclerView = (RecyclerView) rootView.findViewById(R.id.roomsRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setStackFromEnd(true);
        mRoomRecyclerView.setLayoutManager(mLinearLayoutManager);

        userMKey = ((MainContainer)getActivity()).getUserMKey();
        mPhotoUrl = activity.getProfileImageURL();

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        disableChildViews();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

        fabNewMsg = (FloatingActionButton) rootView.findViewById(R.id.fab_new_msg);
        fabNewMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: open Send New Message UI (so also implement that UI in XML) and set up related stuff
                activity.getViewPager().setCurrentItem(13);
            }
        });

        return rootView;
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

        // Initialize Firebase Auth
        //mFirebaseAuth = FirebaseAuth.getInstance();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<RoomObject,
                RoomViewHolder>(
                RoomObject.class,
                R.layout.room_item_view,
                RoomViewHolder.class,
                mFirebaseDatabaseReference.child(ROOMS_CHILD)) {

            @Override
            protected void populateViewHolder(final RoomViewHolder viewHolder,
                                              final RoomObject roomObject, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (roomObject.getName() != null) {
                    viewHolder.roomTitleTV.setText(roomObject.getName());
                    viewHolder.roomTimeTV.setText(df.format(new Date(roomObject.getTime())));
                    viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            String roomNum = roomObject.getRnum();
                            ArrayList<String> usersMap = roomObject.getUsers();

                            usersMap.remove(mUsername); //remove logged-in user from the room users map to prevent duplicate sends,
                            // since we handle logged-in user's message transfer separate from message transfer of other room users

                            if(roomNum != null && usersMap != null){
                                activity.setUpAndOpenMessageRoom(roomNum, usersMap);
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
        });

        mRoomRecyclerView.setAdapter(mFirebaseAdapter);


    }

    @Override
    public void onPause(){
        super.onPause();
        if(mFirebaseAdapter != null){
            mFirebaseAdapter.cleanup();
            Log.d("ORDER", "Tab4Messenger FirebaseRecyclerAdapter cleanup done");
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

}
