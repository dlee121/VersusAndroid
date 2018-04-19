package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
import com.google.firebase.database.Query;
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

import java.util.ArrayList;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by dlee on 8/6/17.
 */



public class MessageRoom extends Fragment {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView usernameTextView;
        LinearLayout messageContentWrapper, messageContent;
        //CircleImageView messageProfileView;

        public MessageViewHolder(View v) {
            super(v);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            messageContentWrapper = itemView.findViewById(R.id.message_content_wrapper);
            messageContent = messageContentWrapper.findViewById(R.id.message_content);
            messageTextView = messageContent.findViewById(R.id.messageTextView);
            messageImageView = messageContent.findViewById(R.id.messageImageView);
            //messageProfileView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
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
    private ArrayList<UserSearchItem> newRoomInviteList;
    private ArrayList<String> existingRoomUsersList;
    private ChildEventListener roomObjListener;
    private boolean initialRoomInfoLoaded = false;
    private String currentRoomTitle = "";
    private boolean roomVisible = false;
    private boolean specialSend = false;
    private String oldRoomNum = "";
    private boolean roomIsDM = true;

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
        if(mFirebaseAdapter != null){
            setRoomObjListener(roomNum);
            mFirebaseAdapter.startListening();
        }
        else{
            if(roomNum.length() > 1 && activity != null && activity.isInMessageRoom()){
                setRoomObjListener(roomNum);
                setUpRecyclerView(roomNum);
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mFirebaseAdapter != null){
            mFirebaseAdapter.stopListening();
            mMessageRecyclerView.setAdapter(null);
            closeRoomObjListener(roomNum);
            Log.d("ORDER", "MessageRoom FirebaseRecyclerAdapter cleanup done");
        }
    }

    public void setUpNewRoom(final ArrayList<UserSearchItem> invitedUsers){
        if(invitedUsers.size() == 1){
            roomIsDM = true;
        }
        else{
            roomIsDM = false;
        }
        specialSend = false;
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        firstMessage = true;
        final String rnum = UUID.randomUUID().toString();
        roomNum = rnum;
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + rnum;
        newRoomInviteList = invitedUsers;
        existingRoomUsersList = null;

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

        mSendButton = rootView.findViewById(R.id.sendButton);

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
                    setUpRoomInDB(content, messageObject, null);
                    firstMessage = false;
                }
                else{
                    mFirebaseDatabaseReference.child(USER_PATH).push().setValue(messageObject);

                    boolean isDM = invitedUsers.size() == 1;

                    for(final UserSearchItem usi : invitedUsers){
                        if(!(activity.getMessengerFragment().blockedFromUser(usi.getUsername()) && isDM)){
                            String WRITE_PATH = usi.getHash() + "/" + usi.getUsername() + "/messages/" + rnum;
                            //final String username = usi.getUsername();
                            mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                        }
                    }
                }

                mMessageEditText.setText("");

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

    public void setUpNewRoom(final ArrayList<UserSearchItem> invitedUsers, final String roomNumInput){
        if(invitedUsers.size() == 1){
            roomIsDM = true;
        }
        else{
            roomIsDM = false;
        }
        specialSend = true;
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        firstMessage = true;
        roomNum = roomNumInput;
        oldRoomNum = roomNumInput;
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + roomNumInput;
        newRoomInviteList = invitedUsers;
        existingRoomUsersList = null;

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

        mMessageEditText = rootView.findViewById(R.id.messageEditText);

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

        mSendButton = rootView.findViewById(R.id.sendButton);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);

                final String content = mMessageEditText.getText().toString().trim();
                final MessageObject messageObject = new MessageObject(content, mUsername, mPhotoUrl, null);

                int usernameHash;
                if(mUsername.length() < 5){
                    usernameHash = mUsername.hashCode();
                }
                else{
                    String hashIn = "" + mUsername.charAt(0) + mUsername.charAt(mUsername.length() - 2) + mUsername.charAt(1) + mUsername.charAt(mUsername.length() - 1);
                    usernameHash = hashIn.hashCode();
                }

                String USER_PATH = Integer.toString(usernameHash) + "/" + mUsername + "/messages/" + roomNumInput;

                if(firstMessage){
                    setUpRoomInDBSpecial(roomNumInput, content, messageObject, null);
                    firstMessage = false;
                }
                else{
                    mFirebaseDatabaseReference.child(USER_PATH).push().setValue(messageObject);

                    boolean isDM = invitedUsers.size() == 1;

                    for(final UserSearchItem usi : invitedUsers){
                        if(!(activity.getMessengerFragment().blockedFromUser(usi.getUsername()) && isDM)){
                            String WRITE_PATH = usi.getHash() + "/" + usi.getUsername() + "/messages/" + roomNumInput;
                            //final String username = usi.getUsername();
                            mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                        }
                    }
                }

                mMessageEditText.setText("");


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

    //for when you delete your copy of the dm room and want to dm the same person and that person hasn't deleted their copy of the dm room
    public void setUpRoomInDBSpecial(final String roomNumInput, String preview, final MessageObject messageObject, final Uri uri){

        //modified version of setUpRoomInDB()
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + roomNumInput;
        final ArrayList<String> roomUsersHolderList;
        if(newRoomInviteList != null){
            roomUsersHolderList = new ArrayList<>();
            for(UserSearchItem usi : newRoomInviteList){
                roomUsersHolderList.add(usi.getUsername());
            }
        }
        else {
            roomUsersHolderList = existingRoomUsersList;
        }

        if(roomUsersHolderList.size() == 1){
            String dmTarget = roomUsersHolderList.get(0);
            //this is a DM, so add it to the dm list in firebase
            mFirebaseDatabaseReference.child(activity.getUserPath() + "dm/"+dmTarget).setValue(roomNumInput);
        }

        roomUsersHolderList.add(mUsername);

        final RoomObject roomObject = new RoomObject(currentRoomTitle, System.currentTimeMillis(), preview, roomUsersHolderList);
        String userRoomPath = activity.getUserPath() + "r/" + roomNumInput;
        mFirebaseDatabaseReference.child(userRoomPath).setValue(roomObject).addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(messageObject.getText() == null){
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push()
                        .setValue(messageObject, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError == null) {

                                    setUpRecyclerView(roomNumInput);

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

                }
                else{
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(messageObject)
                        .addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                setUpRecyclerView(roomNumInput);
                            }
                        });

                }

                if(activity.getMessengerFragment().isEmpty()){
                    activity.getMessengerFragment().initializeFragmentAfterFirstRoomCreation();
                }

                if(roomObject.getUsers() != null && roomObject.getUsers().size() == 2){
                    roomObject.setName(mUsername);
                }

                setRoomObjListener(roomNumInput);

                boolean isDM = false;
                if(roomUsersHolderList.size() == 2){
                    isDM = true;
                }

                //setUpRoomInDBSpecial is called when the person at the other end already has the corresponding room, so we skip room setup and send message right away
                for(final String mName : roomUsersHolderList){
                    if(!mName.equals(mUsername)) {
                        if(!(activity.getMessengerFragment().blockedFromUser(mName) && isDM)){
                            String WRITE_PATH = Integer.toString(getUsernameHash(mName)) + "/" + mName + "/messages/" + roomNumInput;
                            //final String username = usi.getUsername();
                            mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                        }
                    }
                }
            }
        });
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

    public void setUpRoom(final String rnum, final ArrayList<String> usersList, String roomTitle){
        mProgressBar.setVisibility(ProgressBar.VISIBLE);

        if(usersList.size() == 2){
            roomIsDM = true;
        }
        else{
            roomIsDM = false;
        }

        roomNum = rnum;
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + rnum;
        existingRoomUsersList = usersList;
        newRoomInviteList = null;
        currentRoomTitle = roomTitle;
        activity.setMessageRoomTitle(currentRoomTitle);

        setRoomObjListener(roomNum);
        setUpRecyclerView(rnum);

        mMessageEditText = rootView.findViewById(R.id.messageEditText);
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

        mSendButton = rootView.findViewById(R.id.sendButton);

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
                if (usersList.size() == 2) {
                    isDM = true;
                } else {
                    isDM = false;
                }

                for (final String mName : usersList) {
                    String pureUsername;
                    if(mName.indexOf('*') > 0){
                        int index = mName.indexOf('*');
                        int numberCode = Integer.parseInt(mName.substring(index + 1));
                        if(numberCode == 0 || numberCode == 2 || numberCode == 4){//first, second, or third leave, so don't send to this username
                            Log.d("skipsend", "send skipped for user " + mName);
                            continue;
                        }
                        pureUsername = mName.substring(0, index);
                    }
                    else{
                        pureUsername = mName;
                    }

                    if(!(pureUsername.equals(mUsername))){ //this condition check is necessary for when going from CreateMessage to existing room
                        if(!(activity.getMessengerFragment().blockedFromUser(pureUsername) && isDM)){
                            String WRITE_PATH = Integer.toString(getUsernameHash(pureUsername)) + "/" + pureUsername + "/messages/" + rnum;
                            //final String username = usi.getUsername();
                            mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                        }
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

                    final MessageObject tempMessage = new MessageObject(null, mUsername, mPhotoUrl,
                            LOADING_IMAGE_URL);

                    if(firstMessage){
                        if(specialSend){
                            setUpRoomInDBSpecial(oldRoomNum, "", tempMessage, uri);
                        }
                        else{
                            setUpRoomInDB("", tempMessage, uri);
                        }
                        specialSend = false;
                        firstMessage = false;
                    }
                    else{
                        mFirebaseDatabaseReference.child(MESSAGES_CHILD).push()
                                .setValue(tempMessage, new DatabaseReference.CompletionListener() { //sends message with temp image holder to self. Currently, not broadcasting temp message to the room and sending just to self.
                                    @Override
                                    public void onComplete(DatabaseError databaseError,
                                                           DatabaseReference databaseReference) {
                                        if (databaseError == null) {

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
                    }

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

                            if(newRoomInviteList != null){

                                boolean isDM = newRoomInviteList.size() == 1;

                                for(final UserSearchItem usi : newRoomInviteList){

                                    if(!(activity.getMessengerFragment().blockedFromUser(usi.getUsername()) && isDM)){
                                        String WRITE_PATH = usi.getHash() + "/" + usi.getUsername() + "/messages/" + roomNum;
                                        //final String username = usi.getUsername();
                                        mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                                    }

                                }

                            }
                            else if(existingRoomUsersList != null){
                                boolean isDM = false;
                                if(existingRoomUsersList.size() == 1){
                                    isDM = true;
                                }
                                else if(existingRoomUsersList.size() == 2 && (existingRoomUsersList.get(0).equals(mUsername) || existingRoomUsersList.get(1).equals(mUsername))){
                                    isDM = true;
                                }

                                for(final String mName : existingRoomUsersList){

                                    if(!(activity.getMessengerFragment().blockedFromUser(mName) && isDM)){
                                        String WRITE_PATH = getUsernameHash(mName) + "/" + mName + "/messages/" + roomNum;
                                        //final String username = usi.getUsername();
                                        mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                                    }
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

        Query query = mFirebaseDatabaseReference.child(MESSAGES_CHILD).limitToLast(15); //TODO: increase the limit number

        FirebaseRecyclerOptions<MessageObject> options =
                new FirebaseRecyclerOptions.Builder<MessageObject>()
                        .setLifecycleOwner(this)
                        .setQuery(query, MessageObject.class)
                        .build();


        mFirebaseAdapter = new FirebaseRecyclerAdapter<MessageObject, MessageViewHolder>(options) {

            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(activity).inflate(R.layout.message_item_view, parent, false);
                return new MessageViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder, int position, MessageObject friendlyMessage) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                if(friendlyMessage.getName().equals(mUsername)){
                    RelativeLayout.LayoutParams messageContentWrapperLP = (RelativeLayout.LayoutParams) viewHolder.messageContentWrapper.getLayoutParams();
                    messageContentWrapperLP.removeRule(RelativeLayout.ALIGN_PARENT_START);
                    messageContentWrapperLP.addRule(RelativeLayout.ALIGN_PARENT_END);
                    //viewHolder.messageContentWrapper.setLayoutParams(messageContentWrapperLP);
                    viewHolder.messageContentWrapper.setGravity(Gravity.RIGHT);
                    viewHolder.usernameTextView.setVisibility(View.GONE);
                    GradientDrawable bgShape = (GradientDrawable)viewHolder.messageContent.getBackground();
                    bgShape.setColor(ContextCompat.getColor(activity, R.color.messageGreen));
                    bgShape.setStroke(0, Color.WHITE);

                    LinearLayout.LayoutParams messageContentLP = (LinearLayout.LayoutParams) viewHolder.messageContent.getLayoutParams();
                    messageContentLP.gravity = Gravity.RIGHT;
                    viewHolder.messageContent.setLayoutParams(messageContentLP);
                }
                else{
                    RelativeLayout.LayoutParams messageContentWrapperLP = (RelativeLayout.LayoutParams) viewHolder.messageContentWrapper.getLayoutParams();
                    messageContentWrapperLP.removeRule(RelativeLayout.ALIGN_PARENT_END);
                    messageContentWrapperLP.addRule(RelativeLayout.ALIGN_PARENT_START);
                    //viewHolder.messageContentWrapper.setLayoutParams(messageContentWrapperLP);
                    viewHolder.messageContentWrapper.setGravity(Gravity.LEFT);

                    if(roomIsDM){
                        viewHolder.usernameTextView.setVisibility(View.GONE);
                    }
                    else{
                        viewHolder.usernameTextView.setVisibility(View.VISIBLE);
                    }
                    GradientDrawable bgShape = (GradientDrawable)viewHolder.messageContent.getBackground();
                    bgShape.setColor(Color.WHITE);
                    bgShape.setStroke(2, ContextCompat.getColor(activity, R.color.highlightGrey));
                    LinearLayout.LayoutParams messageContentLP = (LinearLayout.LayoutParams) viewHolder.messageContent.getLayoutParams();
                    messageContentLP.gravity = Gravity.LEFT;
                    viewHolder.messageContent.setLayoutParams(messageContentLP);
                }

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
                /*
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.messageProfileView.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                            R.drawable.vs_shadow_w_tag));
                } else {
                    Glide.with(getActivity())
                            .load(friendlyMessage.getPhotoUrl())
                            .into(viewHolder.messageProfileView);
                }
                */

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

    private void setUpRoomInDB(String preview, final MessageObject messageObject, final Uri uri){
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + roomNum;
        final ArrayList<String> roomUsersHolderList;
        if(newRoomInviteList != null){
            roomUsersHolderList = new ArrayList<>();
            for(UserSearchItem usi : newRoomInviteList){
                roomUsersHolderList.add(usi.getUsername());
            }
        }
        else {
            roomUsersHolderList = existingRoomUsersList;
        }

        if(roomUsersHolderList.size() == 1){
            String dmTarget = roomUsersHolderList.get(0);
            //this is a DM, so add it to the dm list in firebase
            mFirebaseDatabaseReference.child(activity.getUserPath() + "dm/"+dmTarget).setValue(roomNum);

            final int targetHash;
            if(dmTarget.length() < 5){
                targetHash = dmTarget.hashCode();
            }
            else{
                String hashIn = "" + dmTarget.charAt(0) + dmTarget.charAt(dmTarget.length() - 2) + dmTarget.charAt(1) + dmTarget.charAt(dmTarget.length() - 1);
                targetHash = hashIn.hashCode();
            }

            mFirebaseDatabaseReference.child(Integer.toString(targetHash) + "/" + dmTarget + "/dm/" + mUsername).setValue(roomNum);
        }

        roomUsersHolderList.add(mUsername);

        final RoomObject roomObject = new RoomObject(currentRoomTitle, System.currentTimeMillis(), preview, roomUsersHolderList);
        String userRoomPath = activity.getUserPath() + "r/" + roomNum;
        mFirebaseDatabaseReference.child(userRoomPath).setValue(roomObject).addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(messageObject.getText() == null){
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push()
                        .setValue(messageObject, new DatabaseReference.CompletionListener() { //sends message with temp image holder to self. Currently, not broadcasting temp message to the room and sending just to self.
                            @Override
                            public void onComplete(DatabaseError databaseError,
                                                   DatabaseReference databaseReference) {
                                if (databaseError == null) {

                                    setUpRecyclerView(roomNum);

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

                }
                else{
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(messageObject)
                        .addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                setUpRecyclerView(roomNum);
                            }
                        });

                }

                if(activity.getMessengerFragment().isEmpty()){
                    activity.getMessengerFragment().initializeFragmentAfterFirstRoomCreation();
                }

                if(roomObject.getUsers() != null && roomObject.getUsers().size() == 2){
                    roomObject.setName(mUsername);
                }

                setRoomObjListener(roomNum);

                boolean isDM = false;
                if(roomUsersHolderList.size() == 2){
                    isDM = true;
                }

                for(final String mName : roomUsersHolderList){
                    if(!mName.equals(mUsername)) {
                        final int usernameHash;
                        if (mName.length() < 5) {
                            usernameHash = mName.hashCode();
                        } else {
                            String hashIn = "" + mName.charAt(0) + mName.charAt(mName.length() - 2) + mName.charAt(1) + mName.charAt(mName.length() - 1);
                            usernameHash = hashIn.hashCode();
                        }

                        String roomPath = usernameHash + "/" + mName + "/" + "r/" + roomNum;
                        Log.d("ROOMCREATE", roomPath);
                        if(!(activity.getMessengerFragment().blockedFromUser(mName) && isDM)){
                            mFirebaseDatabaseReference.child(roomPath).setValue(roomObject).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    String WRITE_PATH = usernameHash + "/" + mName + "/messages/" + roomNum;
                                    //final String username = usi.getUsername();
                                    mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                                }
                            });
                        }
                    }
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
            mFirebaseAdapter.stopListening();
        }
        if(mMessageRecyclerView != null){
            mMessageRecyclerView.setAdapter(null);
        }
    }
}
