package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.graphics.Rect;
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
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.vs.bcd.versus.adapter.ArrayAdapterWithIcon;
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
import java.util.HashSet;
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
        View unreadCircleView;
        TextView roomTitleTV;
        TextView roomTimeTV;
        TextView roomPreviewTV;
        CircleImageView circView;

        //CircleImageView roomImageView;    //TODO: implement circular image view for rooms

        public RoomViewHolder(View v) {
            super(v);
            unreadCircleView = itemView.findViewById(R.id.unread_circle);
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
    //private HashMap<String, RNumAndUList> rNameToRNumAndUListMap;
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
    private Rect rect;
    private Toast mToast;
    private ListPopupWindow listPopupWindow;
    private HashMap<String, Boolean> blockList = new HashMap<>();
    private HashMap<String, Boolean> muteList = new HashMap<>();
    private HashSet<String> unreadRooms = new HashSet<>();
    private String clickedRoomNum = "";
    private ChildEventListener unreadMessagesListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if(clickedRoomNum.equals(dataSnapshot.getKey())){
                mFirebaseDatabaseReference.child(activity.getUserPath()+"unread/"+dataSnapshot.getKey()).removeValue();
            }
            else{
                unreadRooms.add(dataSnapshot.getKey());
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            unreadRooms.remove(dataSnapshot.getKey());
            if(mFirebaseAdapter != null){
                mFirebaseAdapter.notifyDataSetChanged();
            }
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

    private void loadMoreRooms(final int retrievalMultiplier){
        mRoomRecyclerView.setAdapter(null);
        final FirebaseRecyclerAdapter oldAdapter = mFirebaseAdapter;

        previousAdapterItemCount = mFirebaseAdapter.getItemCount();
        //rNameToRNumAndUListMap = new HashMap<>();

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
                            //rNameToRNumAndUListMap.put(roomObject.getName(), new RNumAndUList(child.getKey(), roomObject.getUsers()));
                        }

                        final String payload;
                        if(strBuilder.length() > 0){
                            payload = "{\"docs\":[" + strBuilder.toString() + "]}";
                        }
                        else{
                            payload = null;
                        }

                        Runnable runnable = new Runnable() {
                            public void run() {
                                if(payload != null){
                                    getProfileImgVersions(payload);
                                }

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

                                            if(unreadRooms.contains(roomNum)){
                                                //set unread UI
                                                viewHolder.unreadCircleView.setVisibility(View.VISIBLE);
                                            }
                                            else{
                                                viewHolder.unreadCircleView.setVisibility(View.INVISIBLE);
                                            }

                                            if(usersList == null){
                                                mFirebaseDatabaseReference.child(ROOMS_CHILD).child(roomNum).removeValue();
                                            }
                                            else{
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

                                                //rNameToRNumAndUListMap.put(roomTitle, new RNumAndUList(roomNum, usersList)); //TODO: will this get updated if room is updated with modified usersList?

                                                viewHolder.roomTitleTV.setText(roomTitle);
                                                viewHolder.roomTimeTV.setText(getMessengerTimeString(roomObject.getTime()));
                                                viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                                                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {

                                                        if(roomNum != null){
                                                            clickedRoomNum = roomNum;
                                                            activity.setUpAndOpenMessageRoom(roomNum, usersList, roomTitle);
                                                        }
                                                        else{
                                                            Log.d("MESSENGER", "roomNum is null");
                                                        }
                                                    }
                                                });

                                                viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                                                    @Override
                                                    public boolean onLongClick(View view) {
                                                        roomItemLongClickMenu(viewHolder.roomTimeTV, roomNum, roomObject);
                                                        return true;
                                                    }
                                                });
                                            }

                                        }
                                        else {
                                            Log.d("roomSetUp", "title null");
                                        }

                                    }

                                    @Override
                                    @NonNull
                                    public List<RoomObject> getPreloadItems(int position) {
                                        if (getItemCount() > position) {
                                            RoomObject item = (RoomObject) getItem(position);
                                            if (item == null) {
                                                return Collections.emptyList();
                                            }
                                            return Collections.singletonList(getItem(position));
                                        }
                                        return Collections.emptyList();

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
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();

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
                            //rNameToRNumAndUListMap.put(roomObject.getName(), new RNumAndUList(child.getKey(), roomObject.getUsers()));
                        }

                        final String payload;
                        if(strBuilder.length() > 0){
                            payload = "{\"docs\":[" + strBuilder.toString() + "]}";
                        }
                        else{
                            payload = null;
                        }

                        Runnable runnable = new Runnable() {
                            public void run() {
                                if(payload != null){
                                    getProfileImgVersions(payload);
                                }

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

                                                if(unreadRooms.contains(roomNum)){
                                                    //set unread UI
                                                    viewHolder.unreadCircleView.setVisibility(View.VISIBLE);
                                                }
                                                else{
                                                    viewHolder.unreadCircleView.setVisibility(View.INVISIBLE);
                                                }

                                                if(usersList == null){
                                                    mFirebaseDatabaseReference.child(ROOMS_CHILD).child(roomNum).removeValue();
                                                }
                                                else{
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

                                                    //rNameToRNumAndUListMap.put(roomTitle, new RNumAndUList(roomNum, usersList)); //TODO: will this get updated if room is updated with modified usersList?

                                                    viewHolder.roomTitleTV.setText(roomTitle);
                                                    viewHolder.roomTimeTV.setText(getMessengerTimeString(roomObject.getTime()));
                                                    viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                                                    viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {

                                                            if (roomNum != null) {
                                                                clickedRoomNum = roomNum;
                                                                activity.setUpAndOpenMessageRoom(roomNum, usersList, roomTitle);
                                                            } else {
                                                                Log.d("MESSENGER", "roomNum is null");
                                                            }
                                                        }
                                                    });

                                                    viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                                                        @Override
                                                        public boolean onLongClick(View view) {
                                                            roomItemLongClickMenu(viewHolder.roomTimeTV, roomNum, roomObject);
                                                            return true;
                                                        }
                                                    });
                                                }

                                            } else {
                                                Log.d("roomSetUp", "title null");
                                            }

                                        }

                                        @Override
                                        @NonNull
                                        public List<RoomObject> getPreloadItems(int position) {
                                            if (getItemCount() > position) {
                                                RoomObject item = (RoomObject) getItem(position);
                                                if (item == null) {
                                                    return Collections.emptyList();
                                                }
                                                return Collections.singletonList(getItem(position));
                                            }
                                            return Collections.emptyList();

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

    public void initializeFragmentAfterFirstRoomCreation(){
        if(mFirebaseAdapter != null){
            mFirebaseAdapter.stopListening();
            mFirebaseAdapter = null;
        }
        onResume();
    }

    @Override
    public void onResume() {
        super.onResume();
        //get unread list here
        mFirebaseDatabaseReference.child(activity.getUserPath()+"unread").addChildEventListener(unreadMessagesListener);

        //rNameToRNumAndUListMap = new HashMap<>();

        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.startListening();
        } else {
            query = mFirebaseDatabaseReference.child(ROOMS_CHILD).orderByChild("time").limitToLast(initialRetrievalSize);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!(dataSnapshot.hasChildren())) {
                        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                        emptyListTV.setVisibility(TextView.VISIBLE);
                    } else {
                        totalRoomCount = (int) dataSnapshot.getChildrenCount();

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

                            //rNameToRNumAndUListMap.put(roomObject.getName(), new RNumAndUList(child.getKey(), roomObject.getUsers()));

                        }

                        final String payload;
                        if(strBuilder.length() > 0){
                            payload = "{\"docs\":[" + strBuilder.toString() + "]}";
                        }
                        else{
                            payload = null;
                        }

                        Runnable runnable = new Runnable() {
                            public void run() {

                                if(payload != null){
                                    getProfileImgVersions(payload);
                                }

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

                                        if (setPreloader) {
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

                                            if(unreadRooms.contains(roomNum)){
                                                //set unread UI
                                                viewHolder.unreadCircleView.setVisibility(View.VISIBLE);
                                            }
                                            else{
                                                viewHolder.unreadCircleView.setVisibility(View.INVISIBLE);
                                            }

                                            if(usersList == null){
                                                mFirebaseDatabaseReference.child(ROOMS_CHILD).child(roomNum).removeValue();
                                            }
                                            else{
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


                                                //rNameToRNumAndUListMap.put(roomTitle, new RNumAndUList(roomNum, usersList)); //TODO: will this get updated if room is updated with modified usersList?

                                                viewHolder.roomTitleTV.setText(roomTitle);
                                                viewHolder.roomTimeTV.setText(getMessengerTimeString(roomObject.getTime()));
                                                viewHolder.roomPreviewTV.setText(roomObject.getPreview());
                                                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {

                                                        if (roomNum != null) {
                                                            clickedRoomNum = roomNum;
                                                            activity.setUpAndOpenMessageRoom(roomNum, usersList, roomTitle);
                                                        } else {
                                                            Log.d("MESSENGER", "roomNum is null");
                                                        }
                                                    }
                                                });

                                                viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                                                    @Override
                                                    public boolean onLongClick(View view) {
                                                        roomItemLongClickMenu(viewHolder.roomTimeTV, roomNum, roomObject);
                                                        return true;
                                                    }
                                                });
                                            }

                                        } else {
                                            Log.d("roomSetUp", "title null");
                                        }

                                    }


                                    @Override
                                    @NonNull
                                    public List<RoomObject> getPreloadItems(int position) {
                                        if (getItemCount() > position) {
                                            RoomObject item = (RoomObject) getItem(position);
                                            if (item == null) {
                                                return Collections.emptyList();
                                            }
                                            return Collections.singletonList(getItem(position));
                                        }
                                        return Collections.emptyList();

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

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }

    public boolean roomIsUnread(String roomNum){
        return unreadRooms != null && unreadRooms.contains(roomNum);
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

    private void muteRoom(String roomNum){
        String target = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/mute";
        mFirebaseDatabaseReference.child(target).child(roomNum).setValue(true);
    }

    private void unmuteRoom(String roomNum){
        String target = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/mute";
        mFirebaseDatabaseReference.child(target).child(roomNum).removeValue();
    }

    private void blockUser(RoomObject roomObject){
        String username = roomObject.getUsers().get(0);
        if(username.equals(mUsername)){
            username = roomObject.getUsers().get(1);
        }

        String target = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/block";
        mFirebaseDatabaseReference.child(target).child(username).setValue(true);
    }

    private void unblockUser(RoomObject roomObject){
        String username = roomObject.getUsers().get(0);
        if(username.equals(mUsername)){
            username = roomObject.getUsers().get(1);
        }

        String target = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/block";
        mFirebaseDatabaseReference.child(target).child(username).removeValue();
    }

    private void deleteRoom(final String roomNum, final String dmTarget){
        mFirebaseDatabaseReference.child(activity.getUserPath()+"dm/"+dmTarget).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                /*
                final int targetHash;
                if(dmTarget.length() < 5){
                    targetHash = dmTarget.hashCode();
                }
                else{
                    String hashIn = "" + dmTarget.charAt(0) + dmTarget.charAt(dmTarget.length() - 2) + dmTarget.charAt(1) + dmTarget.charAt(dmTarget.length() - 1);
                    targetHash = hashIn.hashCode();
                }

                mFirebaseDatabaseReference.child(Integer.toString(targetHash) + "/" + dmTarget + "/dm/" + mUsername).setValue("*" + roomNum);
                */

                String roomTarget = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/r";
                mFirebaseDatabaseReference.child(roomTarget).child(roomNum).removeValue();

                String messagesTarget = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/messages";
                mFirebaseDatabaseReference.child(messagesTarget).child(roomNum).removeValue();
            }
        });

        mFirebaseDatabaseReference.child(Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/unread/" + roomNum).removeValue();

        //rNameToRNumAndUListMap.remove(roomName);
    }

    private void leaveRoom(String roomNum, RoomObject roomObject){
        //rNameToRNumAndUListMap.remove(roomObject.getName());

        String roomTarget = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/r";
        mFirebaseDatabaseReference.child(roomTarget).child(roomNum).removeValue();

        String messagesTarget = Integer.toString(getUsernameHash(mUsername)) + "/" + mUsername + "/messages";
        mFirebaseDatabaseReference.child(messagesTarget).child(roomNum).removeValue();

        String roomEditPath;
        ArrayList<String> newUsersList = roomObject.getUsers();
        newUsersList.remove(mUsername);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users", newUsersList);
        for (String username : roomObject.getUsers()) {
            roomEditPath = Integer.toString(getUsernameHash(username)) + "/" + username + "/r/" + roomNum;
            mFirebaseDatabaseReference.child(roomEditPath).updateChildren(childUpdates);
        }
    }

    private void roomItemLongClickMenu(View anchorPoint, final String roomNum, final RoomObject roomObject){
        final String [] items;
        final Integer[] icons;
        boolean yesEdit = false;
        final boolean editAvailable;
        final boolean muted, blocked;

        if(muteList.get(roomNum) != null){
            muted = muteList.get(roomNum);
        }
        else{
            muted = false;
        }

        if(blockList.get(roomNum) != null && blockList.get(roomNum)){
            blocked = true;
        }
        else{
            blocked = false;
        }

        if(roomObject.getUsers().size() > 2){
            if(muted){
                items = new String[]{"Unmute", "Leave"};
            }
            else{
                items = new String[]{"Mute", "Leave"};
            }

            //icons = new Integer[]{R.drawable.ic_edit, R.drawable.ic_delete};
            icons = new Integer[]{};

        }
        else{
            if(blocked){
                items = new String[]{"Unblock", "Delete"};
            }
            else if(muted){
                items = new String[]{"Unmute", "Delete", "Block"};
            }
            else{
                items = new String[]{"Mute", "Delete", "Block"};
            }

            //icons = new Integer[]{R.drawable.ic_edit, R.drawable.ic_delete};
            icons = new Integer[]{};
        }

        int width = activity.getResources().getDimensionPixelSize(R.dimen.overflow_width);


        ListAdapter adapter = new ArrayAdapterWithIcon(activity, items, icons);

        listPopupWindow = new ListPopupWindow(activity);
        listPopupWindow.setAnchorView(anchorPoint);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setWidth(width);

        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        if(blocked){
                            unblockUser(roomObject);
                            unmuteRoom(roomNum);
                        }
                        else if(muted){
                            unmuteRoom(roomNum);
                        }
                        else{
                            muteRoom(roomNum);
                        }
                        break;

                    case 1:
                        if(roomObject.getUsers().size() > 2){
                            leaveRoom(roomNum, roomObject);
                        }
                        else{
                            deleteRoom(roomNum, roomObject.getName());
                        }
                        break;

                    case 2:
                        blockUser(roomObject);
                        break;
                }

                activity.enableClicksForListPopupWindowClose();
                listPopupWindow.dismiss();
            }
        });
        listPopupWindow.show();
        activity.disableClicksForListPopupWindowOpen();
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
        //get unread list here
        mFirebaseDatabaseReference.child(activity.getUserPath()+"unread").removeEventListener(unreadMessagesListener);
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

    public boolean isEmpty(){
        return mFirebaseAdapter == null || mFirebaseAdapter.getItemCount() == 0;
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
