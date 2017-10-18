package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.SessionManager;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private FirebaseUser mFirebaseUser;
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab4messenger, container, false);

        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        mRoomRecyclerView = (RecyclerView) rootView.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setStackFromEnd(true);
        mRoomRecyclerView.setLayoutManager(mLinearLayoutManager);

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
        SessionManager sessionManager = new SessionManager(context);
        mUsername = sessionManager.getCurrentUsername();
        ROOMS_CHILD = sessionManager.getBday() + "/" + sessionManager.getCurrentUsername() + "/rooms";
    }

    private void setUpMessenger(){
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<RoomObject,
                RoomViewHolder>(
                RoomObject.class,
                R.layout.room_item_view,
                RoomViewHolder.class,
                mFirebaseDatabaseReference.child(ROOMS_CHILD)) {

            @Override
            protected void populateViewHolder(final RoomViewHolder viewHolder,
                                              RoomObject roomObject, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (roomObject.getName() != null) {
                    viewHolder.roomTitleTV.setText(roomObject.getName());
                    viewHolder.roomTimeTV.setText(df.format(new Date(roomObject.getTime())));
                    viewHolder.roomPreviewTV.setText(roomObject.getPreview());
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

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
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

    public void firebaseSignOut(){
        mFirebaseAuth.signOut();
    }

}
