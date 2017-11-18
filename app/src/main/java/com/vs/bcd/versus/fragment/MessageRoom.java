package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.MessageObject;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.UserSearchItem;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.R.attr.data;
import static android.content.ContentValues.TAG;

/**
 * Created by dlee on 8/6/17.
 */



public class MessageRoom extends Fragment {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView usernameTextView;
        CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            usernameTextView = (TextView) itemView.findViewById(R.id.usernameTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

    private String MESSAGES_CHILD = "";
    private String MESSAGES_CHILD_BODY = "";
    private final int REQUEST_IMAGE = 2;
    private final int RESULT_OK = -1;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";    //TODO: replace with our own

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private MainContainer activity;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<MessageObject, MessageViewHolder> mFirebaseAdapter;
    private FirebaseAnalytics mFirebaseAnalytics;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private Button mSendButton;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;
    private String mUsername = "";
    private String mPhotoUrl = "";
    private String userMKey = "";
    private boolean firstMessage = false;
    private String roomNum = "";
    private ArrayList<UserSearchItem> roomUsersList;
    private ArrayList<String> roomUsersStringList;
    private ChildEventListener roomObjListener;
    private boolean initialRoomInfoLoaded = false;
    private String currentRoomTitle = "";
    private boolean roomVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.message_room, container, false);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) rootView.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        if(roomVisible){
            enableChildViews();
        }
        else{
            disableChildViews();
        }

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
                                mPhotoUrl = "https://firebasestorage.googleapis.com/v0/b/bcd-versus.appspot.com/o/vs_shadow_w_tag.png?alt=media&token=d9bda511-cd3c-4fed-8b1b-ea2dd72ca479";
                                mFirebaseUser = mFirebaseAuth.getCurrentUser();
                                //setUpMessenger();
                            }
                        }
                    });
        }
        else {
            mPhotoUrl = "https://firebasestorage.googleapis.com/v0/b/bcd-versus.appspot.com/o/vs_shadow_w_tag.png?alt=media&token=d9bda511-cd3c-4fed-8b1b-ea2dd72ca479";
            //setUpMessenger();
        }

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

        MESSAGES_CHILD_BODY = Integer.toString(usernameHash) + "/" + sessionManager.getCurrentUsername() + "/messages/";
        MESSAGES_CHILD =  MESSAGES_CHILD_BODY;
    }

    @Override
    public void onResume(){
        super.onResume();
        if(roomNum.length() > 1 && activity != null && activity.isInMessageRoom()){
            setRoomObjListener(roomNum);
            setUpRecyclerView(roomNum);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mFirebaseAdapter != null){
            mFirebaseAdapter.cleanup();
            closeRoomObjListener(roomNum);
            Log.d("ORDER", "MessageRoom FirebaseRecyclerAdapter cleanup done");
        }
    }

    public void setUpNewRoom(final ArrayList<UserSearchItem> invitedUsers){
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        firstMessage = true;
        final String rnum = UUID.randomUUID().toString();
        roomNum = rnum;
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + rnum;
        roomUsersList = invitedUsers;
        roomUsersStringList = null;

        final String roomTitle;

        switch(invitedUsers.size()){
            case 1:
                roomTitle = invitedUsers.get(0).getUsername();
                break;

            case 2:
                roomTitle = invitedUsers.get(0).getUsername() + " and " + invitedUsers.get(1).getUsername();
                break;

            default:
                roomTitle = invitedUsers.get(0).getUsername() + ", " + invitedUsers.get(1).getUsername() + ", and " + Integer.toString(invitedUsers.size() - 2) + " more";
                break;
        }

        currentRoomTitle = roomTitle;
        activity.setMessageRoomTitle(currentRoomTitle);

        mMessageEditText = (EditText) rootView.findViewById(R.id.messageEditText);

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) rootView.findViewById(R.id.sendButton);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);

                final String content = mMessageEditText.getText().toString().trim();
                MessageObject messageObject = new MessageObject(content, mUsername, mPhotoUrl, null);

                int usernameHash;
                if(mUsername.length() < 5){
                    usernameHash = mUsername.hashCode();
                }
                else{
                    String hashIn = "" + mUsername.charAt(0) + mUsername.charAt(mUsername.length() - 2) + mUsername.charAt(1) + mUsername.charAt(mUsername.length() - 1);
                    usernameHash = hashIn.hashCode();
                }

                String USER_PATH = Integer.toString(usernameHash) + "/" + mUsername + "/messages/" + rnum;

                if(firstMessage){
                    mFirebaseDatabaseReference.child(USER_PATH).push().setValue(messageObject)
                        .addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                setUpRecyclerView(rnum);
                                setUpRoomInDB(content);
                            }
                        });
                    firstMessage = false;
                }
                else{
                    mFirebaseDatabaseReference.child(USER_PATH).push().setValue(messageObject);
                }

                mMessageEditText.setText("");

                final boolean isDM;
                if(invitedUsers.size() == 1){
                    isDM = true;
                }
                else{
                    isDM = false;
                }

                final String preview;
                if(content.trim().length() > 15){
                    preview = content.trim().substring(0,12) + "...";
                }
                else{
                    preview = content.trim();
                }

                for(final UserSearchItem usi : invitedUsers){
                    String WRITE_PATH = usi.getHash() + "/" + usi.getUsername() + "/messages/" + rnum;
                    final String username = usi.getUsername();
                    mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);

                }
            }
        });

        mAddMessageImageView = (ImageView) rootView.findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
    }

    public void setUpRoom(final String rnum, final ArrayList<String> usersList, String roomTitle){
        mProgressBar.setVisibility(ProgressBar.VISIBLE);

        roomNum = rnum;
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + rnum;
        roomUsersStringList = usersList;
        roomUsersList = null;
        currentRoomTitle = roomTitle;
        activity.setMessageRoomTitle(currentRoomTitle);

        setRoomObjListener(roomNum);
        setUpRecyclerView(rnum);

        mMessageEditText = (EditText) rootView.findViewById(R.id.messageEditText);
        /*
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        */
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) rootView.findViewById(R.id.sendButton);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);

                final String content = mMessageEditText.getText().toString().trim();
                MessageObject messageObject = new MessageObject(content, mUsername, mPhotoUrl, null);

                int usernameHash;
                if (mUsername.length() < 5) {
                    usernameHash = mUsername.hashCode();
                } else {
                    String hashIn = "" + mUsername.charAt(0) + mUsername.charAt(mUsername.length() - 2) + mUsername.charAt(1) + mUsername.charAt(mUsername.length() - 1);
                    usernameHash = hashIn.hashCode();
                }

                String USER_PATH = Integer.toString(usernameHash) + "/" + mUsername + "/messages/" + rnum;

                mFirebaseDatabaseReference.child(USER_PATH).push().setValue(messageObject);

                mMessageEditText.setText("");

                final boolean isDM;
                if (usersList.size() == 1) {
                    isDM = true;
                } else {
                    isDM = false;
                }

                final String preview;
                if (content.trim().length() > 15) {
                    preview = content.trim().substring(0, 12) + "...";
                } else {
                    preview = content.trim();
                }

                for (final String mName : usersList) {
                    if(!(mName.equals(mUsername))){ //this condition check is necessary for when going from CreateMessage to existing room
                        int nameHash;
                        if (mName.length() < 5) {
                            usernameHash = mName.hashCode();
                        } else {
                            String hashIn = "" + mName.charAt(0) + mName.charAt(mName.length() - 2) + mName.charAt(1) + mName.charAt(mName.length() - 1);
                            usernameHash = hashIn.hashCode();
                        }

                        String WRITE_PATH = Integer.toString(usernameHash) + "/" + mName + "/messages/" + rnum;
                        mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                    }
                }
            }
        });

        mAddMessageImageView = (ImageView) rootView.findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    MessageObject tempMessage = new MessageObject(null, mUsername, mPhotoUrl,
                            LOADING_IMAGE_URL);


                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push()
                        .setValue(tempMessage, new DatabaseReference.CompletionListener() { //sends message with temp image holder to self. Currently, not broadcasting temp message to the room and sending just to self.
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError == null) {
                                    if(firstMessage){
                                        setUpRecyclerView(roomNum);
                                        setUpRoomInDB("");
                                        firstMessage = false;
                                    }

                                    String key = databaseReference.getKey();
                                    StorageReference storageReference =
                                            FirebaseStorage.getInstance()
                                                    .getReference(mFirebaseUser.getUid())
                                                    .child(key)
                                                    .child(uri.getLastPathSegment());

                                    putImageInStorage(storageReference, uri, key);
                                } else {
                                    Log.w(TAG, "Unable to write message to database.",
                                            databaseError.toException());
                                }
                            }
                        });

                    //TODO: for now, not broadcasting placeholder message. We'll broadcast the real message with the actual image in putImageInStorage once image upload is done.
                    //TODO: see if that's sufficient, if it feels slow or less optimal by any means we'll switch to broadcasting placeholder message

                }
            }
        }
        else {
            //a custom analytics log submission
            Bundle payload = new Bundle();
            payload.putString(FirebaseAnalytics.Param.VALUE, "custom analytics, let's get some useful usage info with this");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, payload);

        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(getActivity(),
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            MessageObject messageObject =
                                    new MessageObject(null, mUsername, mPhotoUrl,
                                            task.getResult().getMetadata().getDownloadUrl()
                                                    .toString());
                            mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(key)
                                    .setValue(messageObject);

                            if(roomUsersList != null){
                                for(final UserSearchItem usi : roomUsersList){

                                    final boolean isDM;
                                    if (roomUsersList.size() == 1) {
                                        isDM = true;
                                    } else {
                                        isDM = false;
                                    }

                                    final String preview = mUsername + " sent an image.";


                                    String WRITE_PATH = usi.getHash() + "/" + usi.getUsername() + "/messages/" + roomNum;
                                    mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                                }

                            }
                            else if(roomUsersStringList != null){

                                final boolean isDM;
                                if (roomUsersStringList.size() == 1) {
                                    isDM = true;
                                } else {
                                    isDM = false;
                                }

                                final String preview = mUsername + " sent an image.";

                                for(final String mName : roomUsersStringList){

                                    int usernameHash;
                                    if(mName.length() < 5){
                                        usernameHash = mName.hashCode();
                                    }
                                    else{
                                        String hashIn = "" + mName.charAt(0) + mName.charAt(mName.length() - 2) + mName.charAt(1) + mName.charAt(mName.length() - 1);
                                        usernameHash = hashIn.hashCode();
                                    }

                                    String WRITE_PATH = usernameHash + "/" + mName + "/messages/" + roomNum;
                                    mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                                }
                            }

                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        roomVisible = isVisibleToUser;
        if (isVisibleToUser) {
            if(rootView != null){
                //the great piece of code that prevents keyboard from pushing toolbar up
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                enableChildViews();
            }
        }
        else {
            if (rootView != null){
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                disableChildViews();
                if(roomObjListener != null && roomNum != null && roomNum.length() > 1){
                    closeRoomObjListener(roomNum);
                }
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

    private void setUpRecyclerView(String rnum){
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + rnum;

        mFirebaseAdapter = new FirebaseRecyclerAdapter<MessageObject,
                MessageViewHolder>(
                MessageObject.class,
                R.layout.message_item_view,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(MESSAGES_CHILD)) {

            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              MessageObject friendlyMessage, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null) {
                    viewHolder.messageTextView.setText(friendlyMessage.getText());
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);

                } else {
                    String imageUrl = friendlyMessage.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.messageImageView.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.messageImageView);
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(friendlyMessage.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.messageTextView.setVisibility(TextView.GONE);
                }

                viewHolder.usernameTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                            R.drawable.vs_shadow_w_tag));
                } else {
                    Glide.with(getActivity())
                            .load(friendlyMessage.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

    private void setUpRoomInDB(String preview){
        final ArrayList<String> roomUsersHolderList;
        if(roomUsersList != null){
            roomUsersHolderList = new ArrayList<>();
            for(UserSearchItem usi : roomUsersList){
                roomUsersHolderList.add(usi.getUsername());
            }
        }
        else {
            roomUsersHolderList = roomUsersStringList;
        }
        roomUsersHolderList.add(mUsername);

        final RoomObject roomObject = new RoomObject(currentRoomTitle, System.currentTimeMillis(), preview, roomUsersHolderList);
        String userRoomPath = activity.getUserPath() + "r/" + roomNum;
        mFirebaseDatabaseReference.child(userRoomPath).setValue(roomObject).addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                setRoomObjListener(roomNum);

                for(String mName : roomUsersHolderList){
                    int usernameHash;
                    if(mName.length() < 5){
                        usernameHash = mName.hashCode();
                    }
                    else{
                        String hashIn = "" + mName.charAt(0) + mName.charAt(mName.length() - 2) + mName.charAt(1) + mName.charAt(mName.length() - 1);
                        usernameHash = hashIn.hashCode();
                    }

                    String roomPath = usernameHash + "/" + mName + "/" + "r/" + roomNum;
                    Log.d("ROOMCREATE", roomPath);
                    mFirebaseDatabaseReference.child(roomPath).setValue(roomObject);
                }
            }
        });

    }

    private void setRoomObjListener(String roomNum){
        String rPath = activity.getUserPath()+"r/" + roomNum;
        Log.d("ROLT", "rolt added to roomNum: " + roomNum);
        initialRoomInfoLoaded = false;

        roomObjListener = mFirebaseDatabaseReference.child(rPath).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                //this would be if we go from default room name to custom room name
                if(initialRoomInfoLoaded){
                    if(dataSnapshot.getKey().equals("name")){
                        currentRoomTitle = dataSnapshot.getValue(String.class);
                        if(activity!=null){
                            activity.setMessageRoomTitle(currentRoomTitle);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                //this would be if custom room name changed, or is users list was modified
                if(dataSnapshot.getKey().equals("name")){
                    currentRoomTitle = dataSnapshot.getValue(String.class);
                    if(activity!=null){
                        activity.setMessageRoomTitle(currentRoomTitle);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        //This just serves to filter out initial onChildAdded actions (data we already have) for the ChildEventListener above.
        mFirebaseDatabaseReference.child(rPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if(child.getKey().equals("name")){
                            currentRoomTitle = child.getValue(String.class);
                            if(activity!=null){
                                activity.setMessageRoomTitle(currentRoomTitle);
                            }
                        }
                    }
                }

                initialRoomInfoLoaded = true;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void closeRoomObjListener(String roomNum){
        if(roomObjListener != null){
            Log.d("ROL", "ROL removed for roomNum: " + roomNum);
            String rPath = activity.getUserPath()+"r/" + roomNum;
            mFirebaseDatabaseReference.child(rPath).removeEventListener(roomObjListener);
            roomObjListener = null;
            currentRoomTitle = "";
        }
    }

    public void cleanUp(){
        if(mFirebaseAdapter != null){
            mFirebaseAdapter.cleanup();
        }
        if(mMessageRecyclerView != null){
            mMessageRecyclerView.setAdapter(null);
        }
    }
}
