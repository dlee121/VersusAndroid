package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
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
import com.vs.bcd.versus.adapter.CustomFirebaseRecyclerAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.RNumAndUList;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.SessionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by dlee on 8/6/17.
 */



public class MessengerFragment extends Fragment {

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomTitleTV;
        TextView roomTimeTV;
        TextView roomPreviewTV;
        CircleImageView circView;

        //CircleImageView roomImageView;    //TODO: implement circular image view for rooms

        public RoomViewHolder(View v) {
            super(v);
            roomTitleTV = itemView.findViewById(R.id.roomNameTV);
            roomTimeTV = itemView.findViewById(R.id.roomTimeTV);
            roomPreviewTV = itemView.findViewById(R.id.roomPreviewTV);
            circView = itemView.findViewById(R.id.room_item_profile_img);
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
    private CustomFirebaseRecyclerAdapter<RoomObject, RoomViewHolder> mFirebaseAdapter;
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
    private boolean firstOnBindVieHolderCall = false;
    private int previousAdapterItemCount = 0;
    private int previousAdapterScrollPosition = 0;
    private int totalRoomCount = Integer.MAX_VALUE;
    private HashMap<String, Integer> profileImgVersions = new HashMap<>();
    private boolean setPreloader = true;
    private Drawable defaultProfileImage;
    private MessengerFragment thisFragment;

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
                                loadMoreRooms(retrievalMultiplier);
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

    private void getProfileImgVersions(){
        Runnable runnable = new Runnable() {
            public void run() {
                //assass



            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }

    private void loadMoreRooms(final int retrievalMultiplier){
        mRoomRecyclerView.setAdapter(null);
        final FirebaseRecyclerAdapter oldAdapter = mFirebaseAdapter;

        previousAdapterItemCount = mFirebaseAdapter.getItemCount();
        rNameToRNum = new HashMap<>();

        if(retrievalMultiplier == -1){

            query = mFirebaseDatabaseReference.child(ROOMS_CHILD).orderByChild("time");
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    if(!(dataSnapshot.hasChildren())){
                        emptyListTV.setVisibility(TextView.VISIBLE);
                    }
                    else{
                        totalRoomCount = (int) dataSnapshot.getChildrenCount();

                        StringBuilder strBuilder = new StringBuilder(((int)dataSnapshot.getChildrenCount() * 56) - 1);
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            RoomObject roomObject = child.getValue(RoomObject.class);
                            if (roomObject.getUsers().size() == 2){ //if DM
                                String username = roomObject.getUsers().get(0);
                                if(username.equals(mUsername)){
                                    username = roomObject.getUsers().get(1);
                                }
                                //add username to parameter string, then at loop finish we do multiget of those users and create hashmap of username:profileImgVersion
                                if(strBuilder.length() == 0){
                                    strBuilder.append("{\"_id\":\""+username+"\",\"_source\":\"pi\"}");
                                }
                                else{
                                    strBuilder.append(",{\"_id\":\""+username+"\",\"_source\":\"pi\"}");
                                }
                            }
                        }
                        if(strBuilder.length() > 0){
                            final String payload = "{\"docs\":["+strBuilder.toString()+"]}";
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    getProfileImgVersions(payload);

                                    firstOnBindVieHolderCall = true;

                                    FirebaseRecyclerOptions<RoomObject> options =
                                            new FirebaseRecyclerOptions.Builder<RoomObject>()
                                                    .setLifecycleOwner(thisFragment)
                                                    .setQuery(query, RoomObject.class)
                                                    .build();

                                    final CustomFirebaseRecyclerAdapter freshFirebaseAdapter = new CustomFirebaseRecyclerAdapter<RoomObject, RoomViewHolder>(options) {

                                        @Override
                                        public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                                            View view = LayoutInflater.from(activity).inflate(R.layout.room_item_view, parent, false);
                                            return new RoomViewHolder(view);
                                        }

                                        @Override
                                        protected void onBindViewHolder(final RoomViewHolder viewHolder, final int position, final RoomObject roomObject) {

                                            if(firstOnBindVieHolderCall){
                                                firstOnBindVieHolderCall = false;
                                                new Handler().postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mRoomRecyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - previousAdapterItemCount + 2);
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

                                                if(usersList.size() == 2){
                                                    try{
                                                        String username = usersList.get(0);
                                                        if(username.equals(mUsername)){
                                                            username = usersList.get(1);
                                                        }

                                                        int profileImg = profileImgVersions.get(username).intValue();

                                                        if(profileImg == 0){
                                                            GlideApp.with(activity).load(defaultProfileImage).into(viewHolder.circView);
                                                        }
                                                        else{
                                                            GlideApp.with(activity).load(activity.getProfileImgUrl(username, profileImg)).into(viewHolder.circView);
                                                        }

                                                    }catch (Throwable t){

                                                    }
                                                }
                                                else{
                                                    GlideApp.with(activity).load(defaultProfileImage).into(viewHolder.circView);
                                                }

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

                                        @Override
                                        @NonNull
                                        public List<RoomObject> getPreloadItems(int position) {
                                            RoomObject item = (RoomObject)getItem(position);
                                            if(item == null || item.getUsers().size() > 2){
                                                return Collections.emptyList();
                                            }
                                            return Collections.singletonList((RoomObject)getItem(position));
                                        }

                                        @Override
                                        @Nullable
                                        public RequestBuilder getPreloadRequestBuilder(RoomObject roomObject) {
                                            try{
                                                int profileImg;
                                                String username = roomObject.getUsers().get(0);
                                                if(username.equals(mUsername)){
                                                    username = roomObject.getUsers().get(1);
                                                }
                                                profileImg = profileImgVersions.get(username).intValue();
                                                if(profileImg == 0){
                                                    return null;
                                                }
                                                return GlideApp.with(activity).load( activity.getProfileImgUrl(username,profileImgVersions.get(username).intValue()) );

                                            }
                                            catch (Throwable t){

                                            }

                                            return null;
                                        }
                                    };

                                    mFirebaseAdapter = freshFirebaseAdapter;
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRoomRecyclerView.setAdapter(mFirebaseAdapter);
                                            setFirebaseObserver();
                                        }
                                    });

                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else{
            mProgressBar.setVisibility(View.VISIBLE);

            query = mFirebaseDatabaseReference.child(ROOMS_CHILD).orderByChild("time").limitToLast(initialRetrievalSize + retrievalSize * retrievalMultiplier);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    if(!(dataSnapshot.hasChildren())){
                        emptyListTV.setVisibility(TextView.VISIBLE);
                    }
                    else {
                        totalRoomCount = (int) dataSnapshot.getChildrenCount();
                        if(totalRoomCount%retrievalSize != 0){
                            loadMoreRooms(-1);
                            nowLoading = true;
                            return;
                        }

                        StringBuilder strBuilder = new StringBuilder(((int) dataSnapshot.getChildrenCount() * 56) - 1);
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            RoomObject roomObject = child.getValue(RoomObject.class);
                            if (roomObject.getUsers().size() == 2) { //if DM
                                String username = roomObject.getUsers().get(0);
                                if (username.equals(mUsername)) {
                                    username = roomObject.getUsers().get(1);
                                }
                                //add username to parameter string, then at loop finish we do multiget of those users and create hashmap of username:profileImgVersion
                                if (strBuilder.length() == 0) {
                                    strBuilder.append("{\"_id\":\"" + username + "\",\"_source\":\"pi\"}");
                                } else {
                                    strBuilder.append(",{\"_id\":\"" + username + "\",\"_source\":\"pi\"}");
                                }
                            }
                        }
                        if (strBuilder.length() > 0) {
                            final String payload = "{\"docs\":[" + strBuilder.toString() + "]}";
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    getProfileImgVersions(payload);

                                    if (retrievalMultiplier > 0) {
                                        firstOnBindVieHolderCall = true;

                                        FirebaseRecyclerOptions<RoomObject> options =
                                                new FirebaseRecyclerOptions.Builder<RoomObject>()
                                                        .setLifecycleOwner(thisFragment)
                                                        .setQuery(query, RoomObject.class)
                                                        .build();

                                        final CustomFirebaseRecyclerAdapter freshFirebaseAdapter = new CustomFirebaseRecyclerAdapter<RoomObject, RoomViewHolder>(options) {

                                            @Override
                                            public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                                                View view = LayoutInflater.from(activity).inflate(R.layout.room_item_view, parent, false);
                                                return new RoomViewHolder(view);
                                            }

                                            @Override
                                            protected void onBindViewHolder(final RoomViewHolder viewHolder, final int position, final RoomObject roomObject) {
                                                if (firstOnBindVieHolderCall) {
                                                    firstOnBindVieHolderCall = false;
                                                    nowLoading = false;
                                                    new Handler().postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mRoomRecyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - previousAdapterItemCount + 2);
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

                                                    if (usersList.size() == 2) {
                                                        try {
                                                            String username = usersList.get(0);
                                                            if (username.equals(mUsername)) {
                                                                username = usersList.get(1);
                                                            }

                                                            int profileImg = profileImgVersions.get(username).intValue();

                                                            if (profileImg == 0) {
                                                                GlideApp.with(activity).load(defaultProfileImage).into(viewHolder.circView);
                                                            } else {
                                                                GlideApp.with(activity).load(activity.getProfileImgUrl(username, profileImg)).into(viewHolder.circView);
                                                            }

                                                        } catch (Throwable t) {

                                                        }
                                                    } else {
                                                        GlideApp.with(activity).load(defaultProfileImage).into(viewHolder.circView);
                                                    }

                                                    rNameToRNum.put(roomTitle, new RNumAndUList(roomNum, usersList)); //TODO: will this get updated if room is updated with modified usersList?

                                                    viewHolder.roomTitleTV.setText(roomTitle);
                                                    viewHolder.roomTimeTV.setText(getMessengerTimeString(roomObject.getTime()));
                                                    viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                                                    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {

                                                            usersList.remove(mUsername); //remove logged-in user from the room users map to prevent duplicate sends,
                                                            // since we handle logged-in user's message transfer separate from message transfer of other room users

                                                            if (roomNum != null && usersList != null) {
                                                                activity.setUpAndOpenMessageRoom(roomNum, usersList, roomTitle);
                                                            } else {
                                                                Log.d("MESSENGER", "roomNum is null");
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    Log.d("roomSetUp", "title null");
                                                }

                                            }

                                            @Override
                                            @NonNull
                                            public List<RoomObject> getPreloadItems(int position) {
                                                RoomObject item = (RoomObject) getItem(position);
                                                if (item == null || item.getUsers().size() > 2) {
                                                    return Collections.emptyList();
                                                }
                                                return Collections.singletonList((RoomObject) getItem(position));
                                            }

                                            @Override
                                            @Nullable
                                            public RequestBuilder getPreloadRequestBuilder(RoomObject roomObject) {
                                                try {
                                                    int profileImg;
                                                    String username = roomObject.getUsers().get(0);
                                                    if (username.equals(mUsername)) {
                                                        username = roomObject.getUsers().get(1);
                                                    }
                                                    profileImg = profileImgVersions.get(username).intValue();
                                                    if (profileImg == 0) {
                                                        return null;
                                                    }
                                                    return GlideApp.with(activity).load(activity.getProfileImgUrl(username, profileImgVersions.get(username).intValue()));

                                                } catch (Throwable t) {

                                                }

                                                return null;
                                            }
                                        };

                                        mFirebaseAdapter = freshFirebaseAdapter;
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mRoomRecyclerView.setAdapter(mFirebaseAdapter);
                                                setFirebaseObserver();
                                            }
                                        });
                                    }


                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
        thisFragment = this;
        SessionManager sessionManager = new SessionManager(context);
        mUsername = sessionManager.getCurrentUsername();
        defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);

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
            query = mFirebaseDatabaseReference.child(ROOMS_CHILD).orderByChild("time").limitToLast(initialRetrievalSize);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    if(!(dataSnapshot.hasChildren())){
                        emptyListTV.setVisibility(TextView.VISIBLE);
                    }
                    else{
                        totalRoomCount = (int) dataSnapshot.getChildrenCount();

                        StringBuilder strBuilder = new StringBuilder(((int)dataSnapshot.getChildrenCount() * 56) - 1);
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            RoomObject roomObject = child.getValue(RoomObject.class);
                            if (roomObject.getUsers().size() == 2){ //if DM
                                String username = roomObject.getUsers().get(0);
                                if(username.equals(mUsername)){
                                    username = roomObject.getUsers().get(1);
                                }
                                //add username to parameter string, then at loop finish we do multiget of those users and create hashmap of username:profileImgVersion
                                if(strBuilder.length() == 0){
                                    strBuilder.append("{\"_id\":\""+username+"\",\"_source\":\"pi\"}");
                                }
                                else{
                                    strBuilder.append(",{\"_id\":\""+username+"\",\"_source\":\"pi\"}");
                                }
                            }
                        }
                        if(strBuilder.length() > 0){
                            final String payload = "{\"docs\":["+strBuilder.toString()+"]}";
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    getProfileImgVersions(payload);

                                    rNameToRNum = new HashMap<>();

                                    FirebaseRecyclerOptions<RoomObject> options =
                                            new FirebaseRecyclerOptions.Builder<RoomObject>()
                                                    .setLifecycleOwner(thisFragment)
                                                    .setQuery(query, RoomObject.class)
                                                    .build();

                                    mFirebaseAdapter = new CustomFirebaseRecyclerAdapter<RoomObject, RoomViewHolder>(options) {

                                        @Override
                                        public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                                            View view = LayoutInflater.from(activity).inflate(R.layout.room_item_view, parent, false);
                                            return new RoomViewHolder(view);
                                        }

                                        @Override
                                        protected void onBindViewHolder(final RoomViewHolder viewHolder, final int position, final RoomObject roomObject) {
                                            if(setPreloader){
                                                //recyclerview preloader setup
                                                ListPreloader.PreloadSizeProvider sizeProvider =
                                                        new FixedPreloadSizeProvider(activity.getImageWidthPixels(), activity.getImageHeightPixels());
                                                RecyclerViewPreloader<RoomObject> preloader = new RecyclerViewPreloader<RoomObject>(Glide.with(activity), mFirebaseAdapter, sizeProvider, 10);
                                                mRoomRecyclerView.addOnScrollListener(preloader);
                                                setPreloader = false;
                                            }

                                            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                                            emptyListTV.setVisibility(TextView.INVISIBLE);

                                            if (roomObject != null) {

                                                final ArrayList<String> usersList = roomObject.getUsers();
                                                final String roomTitle = roomObject.getName();
                                                final String roomNum = getRef(position).getKey();

                                                if(usersList.size() == 2){
                                                    try{
                                                        String username = usersList.get(0);
                                                        if(username.equals(mUsername)){
                                                            username = usersList.get(1);
                                                        }

                                                        int profileImg = profileImgVersions.get(username).intValue();

                                                        if(profileImg == 0){
                                                            GlideApp.with(activity).load(defaultProfileImage).into(viewHolder.circView);
                                                        }
                                                        else{
                                                            GlideApp.with(activity).load(activity.getProfileImgUrl(username, profileImg)).into(viewHolder.circView);
                                                        }

                                                    }catch (Throwable t){

                                                    }
                                                }
                                                else{
                                                    GlideApp.with(activity).load(defaultProfileImage).into(viewHolder.circView);
                                                }


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


                                        @Override
                                        @NonNull
                                        public List<RoomObject> getPreloadItems(int position) {
                                            if(getItemCount() > position){
                                                RoomObject item = (RoomObject)getItem(position);
                                                if(item == null){
                                                    return Collections.emptyList();
                                                }
                                                return Collections.singletonList(getItem(position));
                                            }
                                            return Collections.emptyList();

                                        }

                                        @Override
                                        @Nullable
                                        public RequestBuilder getPreloadRequestBuilder(RoomObject roomObject) {
                                            try{
                                                int profileImg;
                                                String username = roomObject.getUsers().get(0);
                                                if(username.equals(mUsername)){
                                                    username = roomObject.getUsers().get(1);
                                                }
                                                profileImg = profileImgVersions.get(username).intValue();
                                                if(profileImg == 0){
                                                    return null;
                                                }
                                                return GlideApp.with(activity).load( activity.getProfileImgUrl(username,profileImgVersions.get(username).intValue()) );

                                            }
                                            catch (Throwable t){

                                            }

                                            return null;
                                        }

                                    };
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRoomRecyclerView.setAdapter(mFirebaseAdapter);
                                            setFirebaseObserver();
                                        }
                                    });

                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }

    private void getProfileImgVersions(String payload){
        String host = ((MainContainer)getActivity()).getESHost();
        String region = ((MainContainer)getActivity()).getESRegion();
        String query = "/user/user_type/_mget";
        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);
        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(region)
                .serviceName("es") // es - elastic search. use your service name
                .httpMethodName("POST") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(query) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(payload) // payload if any
                .debug() // turn on the debug mode
                .build();

        String url = "https://" + host + query;

        HttpPost httpPost = new HttpPost(url);
        StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);

		        /* Get header calculated for request */
        Map<String, String> header = aWSV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

            httpPost.addHeader(key, value);
        }

        /* Create object of CloseableHttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

		/* Response handler for after request execution */
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				/* Get status code */
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
					/* Convert response to String */
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        try {
			/* Execute URL and attach after execution response handler */

            String strResponse = httpClient.execute(httpPost, responseHandler);

            //iterate through hits and put the info in postInfoMap
            JSONObject obj = new JSONObject(strResponse);
            JSONArray hits = obj.getJSONArray("docs");
            for(int i = 0; i<hits.length(); i++){
                JSONObject item = hits.getJSONObject(i);
                JSONObject src = item.getJSONObject("_source");
                profileImgVersions.put(item.getString("_id"), src.getInt("pi"));
            }

            if(!profileImgVersions.isEmpty() && mFirebaseAdapter != null){
                /*
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFirebaseAdapter.notifyDataSetChanged();
                    }
                });
                */
            }


        } catch (Exception e) {
            e.printStackTrace();
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
