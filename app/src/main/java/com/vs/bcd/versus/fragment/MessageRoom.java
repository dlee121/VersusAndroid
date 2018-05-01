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
import com.firebase.ui.common.ChangeEventType;
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

    public static class EventMessageViewHolder extends RecyclerView.ViewHolder {
        TextView eventMessageText;

        public EventMessageViewHolder(View v) {
            super(v);
            eventMessageText = itemView.findViewById(R.id.event_message_text);
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
    private FirebaseRecyclerAdapter<MessageObject, RecyclerView.ViewHolder> mFirebaseAdapter;
    private FirebaseAnalytics mFirebaseAnalytics;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private Button mSendButton;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;
    private String mUsername = "";
    private String userMKey = "";
    private boolean firstMessage = false;
    private String roomNum = "";
    private ArrayList<String> newRoomInviteList;
    private ArrayList<String> existingRoomUsersList;
    private ChildEventListener roomObjListener;
    private boolean initialRoomInfoLoaded = false;
    private String currentRoomTitle = "";
    private boolean roomVisible = false;
    private boolean specialSend = false;
    private String oldRoomNum = "";
    private boolean roomIsDM = true;
    private int VIEW_TYPE_EVENT = 0;
    private int VIEW_TYPE_MESSAGE = 1;
    private boolean defaultRoomName = true;
    private String adapterRNum = "";

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
                                mFirebaseUser = mFirebaseAuth.getCurrentUser();
                                //setUpMessenger();
                            }
                        }
                    });
        }
        else {
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
        if(adapterRNum.equals(roomNum) && mFirebaseAdapter != null){
            setRoomObjListener(roomNum);
            //mFirebaseAdapter.startListening();
            mMessageRecyclerView.setAdapter(mFirebaseAdapter);
        }
        /*
        else{
            if(roomNum.length() > 1 && activity != null && activity.isInMessageRoom()){
                setRoomObjListner(roomNum);
                setUpRecyclerView(roomNum);
            }
        }
        */
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

    public boolean firstMessageSent(){
        return !firstMessage;
    }

    public void setUpNewRoom(final ArrayList<String> invitedUsers){
        defaultRoomName = true;

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
                roomTitle = invitedUsers.get(0);
                break;

            case 2:
                roomTitle = mUsername + ", " + invitedUsers.get(0) + ", and " + invitedUsers.get(1);
                break;

            default:
                roomTitle = mUsername + ", " + invitedUsers.get(0) + ", and " + Integer.toString(invitedUsers.size() - 1) + " more";
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
                MessageObject messageObject = new MessageObject(content, mUsername, null);

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

                    for(final String invitedUsername : invitedUsers){
                        String usernameFinal = invitedUsername;
                        if(!isDM){
                            if(invitedUsername.indexOf('*') > 0){
                                int numberCodeIndex = invitedUsername.indexOf('*');
                                int numberCode = Integer.parseInt(invitedUsername.substring(numberCodeIndex+1));
                                if(numberCode == 0 || numberCode == 2 || numberCode == 4){
                                    continue;
                                }
                                usernameFinal = invitedUsername.substring(0, numberCodeIndex);
                            }
                        }

                        if(!(isDM && activity.getMessengerFragment().blockedFromUser(usernameFinal))){
                            String WRITE_PATH = getUsernameHash(usernameFinal) + "/" + usernameFinal + "/messages/" + rnum;
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

    //this is only used for DM
    public void setUpNewRoom(final ArrayList<String> invitedUsers, final String roomNumInput){
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
                roomTitle = invitedUsers.get(0);
                break;

            case 2:
                roomTitle = invitedUsers.get(0) + " and " + invitedUsers.get(1);
                break;

            default:
                roomTitle = invitedUsers.get(0) + ", " + invitedUsers.get(1) + ", and " + Integer.toString(invitedUsers.size() - 2) + " more";
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
                final MessageObject messageObject = new MessageObject(content, mUsername, null);

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
                    //TODO: this is only used for DMs so no need to account for numberCode here.
                    //TODO: if we do use this for group chat, then we need to account for numberCode here to make sure people who left the group don't get messages for the group without first getting re-invited

                    for(final String invitedUsername : invitedUsers){
                        if(!(activity.getMessengerFragment().blockedFromUser(invitedUsername) && isDM)){
                            String WRITE_PATH = getUsernameHash(invitedUsername) + "/" + invitedUsername + "/messages/" + roomNumInput;
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
    //so this is only for that special dm case
    public void setUpRoomInDBSpecial(final String roomNumInput, String preview, final MessageObject messageObject, final Uri uri){
        activity.getMessengerFragment().setClickedRoomNum(roomNumInput);
        Log.d("badgeInc", "clickedRoomNum set: " + roomNumInput);

        //modified version of setUpRoomInDB()
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + roomNumInput;
        final ArrayList<String> roomUsersHolderList;
        if(newRoomInviteList != null){
            roomUsersHolderList = new ArrayList<>();
            for(String invitedUsername : newRoomInviteList){
                roomUsersHolderList.add(invitedUsername);
            }
        }
        else {
            roomUsersHolderList = existingRoomUsersList; //just safety, don't think this line is necessary under normal circumstances
        }

        if(roomUsersHolderList.size() == 1){
            String dmTarget = roomUsersHolderList.get(0);
            //this is a DM, so add it to the dm list in firebase
            mFirebaseDatabaseReference.child(activity.getUserPath() + "dm/"+dmTarget).setValue(roomNumInput);
        }

        roomUsersHolderList.add(0, mUsername);

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
            }
        });

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
        //TODO: this is for DM so don't need to account for numberCode, but if that changes and this is also used for group chat then we'll have to account for numberCode here
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
                MessageObject messageObject = new MessageObject(content, mUsername, null);

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
                    String pureUsername = mName;

                    if(!isDM){
                        if(mName.indexOf('*') > 0){
                            int index = mName.indexOf('*');
                            int numberCode = Integer.parseInt(mName.substring(index + 1));
                            if(numberCode == 0 || numberCode == 2 || numberCode == 4){//first, second, or third leave, so don't send to this username
                                Log.d("skipsend", "send skipped for user " + mName);
                                continue;
                            }
                            pureUsername = mName.substring(0, index);
                        }
                    }

                    if(!(pureUsername.equals(mUsername))){ //this condition check is necessary for when going from CreateMessage to existing room
                        if(!(isDM && activity.getMessengerFragment().blockedFromUser(pureUsername))){
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

                    final MessageObject tempMessage = new MessageObject(null, mUsername, LOADING_IMAGE_URL);

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
                                    new MessageObject(null, mUsername, task.getResult().getMetadata().getDownloadUrl().toString());
                            mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(key)
                                    .setValue(messageObject);

                            if(newRoomInviteList != null){

                                boolean isDM = newRoomInviteList.size() == 1;

                                for(final String invitedUsername : newRoomInviteList){

                                    if(!(activity.getMessengerFragment().blockedFromUser(invitedUsername) && isDM)){
                                        String WRITE_PATH = getUsernameHash(invitedUsername) + "/" + invitedUsername + "/messages/" + roomNum;
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

    public boolean isRoomAdmin(){
        int numberCode;
        boolean isRoomAdmin = false;
        for(String username : getUsersList()){
            if(username.indexOf('*') > 0){
                numberCode = Integer.parseInt(username.substring(username.indexOf('*') + 1));
                if(numberCode == 1 || numberCode == 3){
                    if(username.substring(0, username.indexOf('*')).equals(mUsername)){
                        isRoomAdmin = true;
                    }
                    break;
                }
            }
            else{
                if(username.equals(mUsername)){
                    isRoomAdmin = true;
                }
                break;
            }
        }

        return isRoomAdmin;
    }

    public void editRoomTitle(){

    }

    public void roomTitleClick(){
        if(getUsersList().size() == 2){
            if(getUsersList().get(0).equals(mUsername)){
                activity.goToProfile(getUsersList().get(1), true);

            }
            else{
                activity.goToProfile(getUsersList().get(0), true);
            }
        }
        else{
            //show group members list
            activity.getViewPager().setCurrentItem(13);

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
        adapterRNum = rnum;
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + rnum;

        Query query = mFirebaseDatabaseReference.child(MESSAGES_CHILD).limitToLast(15); //TODO: increase the limit number

        FirebaseRecyclerOptions<MessageObject> options =
                new FirebaseRecyclerOptions.Builder<MessageObject>()
                        .setLifecycleOwner(this)
                        .setQuery(query, MessageObject.class)
                        .build();


        mFirebaseAdapter = new FirebaseRecyclerAdapter<MessageObject, RecyclerView.ViewHolder>(options) {

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                if(viewType == VIEW_TYPE_MESSAGE){
                    View view = LayoutInflater.from(activity).inflate(R.layout.message_item_view, parent, false);
                    return new MessageViewHolder(view);
                }
                else{
                    View view = LayoutInflater.from(activity).inflate(R.layout.room_event_message, parent, false);
                    return new EventMessageViewHolder(view);
                }


            }

            @Override
            public int getItemViewType(int position) {
                MessageObject messageObject = getItem(position);
                if (messageObject.getName() != null) {
                    return VIEW_TYPE_MESSAGE;
                }
                else {
                    return  VIEW_TYPE_EVENT;
                }
            }

            @Override
            protected void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, MessageObject messageObject) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

                if (viewHolder instanceof MessageViewHolder){ //this is a chat message item
                    final MessageViewHolder messageViewHolder = (MessageViewHolder) viewHolder;
                    if(messageObject.getName() == null){
                        //this is for safety since mesageObject.name is now nullable. Just in case a room event message object somehow makes it here.
                        messageObject.setName("");
                    }

                    if(messageObject.getName().equals(mUsername)){
                        RelativeLayout.LayoutParams messageContentWrapperLP = (RelativeLayout.LayoutParams) messageViewHolder.messageContentWrapper.getLayoutParams();
                        messageContentWrapperLP.removeRule(RelativeLayout.ALIGN_PARENT_START);
                        messageContentWrapperLP.addRule(RelativeLayout.ALIGN_PARENT_END);
                        //viewHolder.messageContentWrapper.setLayoutParams(messageContentWrapperLP);
                        messageViewHolder.messageContentWrapper.setGravity(Gravity.RIGHT);
                        messageViewHolder.usernameTextView.setVisibility(View.GONE);
                        GradientDrawable bgShape = (GradientDrawable)messageViewHolder.messageContent.getBackground();
                        bgShape.setColor(ContextCompat.getColor(activity, R.color.messageGreen));
                        bgShape.setStroke(0, Color.WHITE);

                        LinearLayout.LayoutParams messageContentLP = (LinearLayout.LayoutParams) messageViewHolder.messageContent.getLayoutParams();
                        messageContentLP.gravity = Gravity.RIGHT;
                        messageViewHolder.messageContent.setLayoutParams(messageContentLP);
                    }
                    else{
                        RelativeLayout.LayoutParams messageContentWrapperLP = (RelativeLayout.LayoutParams) messageViewHolder.messageContentWrapper.getLayoutParams();
                        messageContentWrapperLP.removeRule(RelativeLayout.ALIGN_PARENT_END);
                        messageContentWrapperLP.addRule(RelativeLayout.ALIGN_PARENT_START);
                        //viewHolder.messageContentWrapper.setLayoutParams(messageContentWrapperLP);
                        messageViewHolder.messageContentWrapper.setGravity(Gravity.LEFT);

                        if(roomIsDM){
                            messageViewHolder.usernameTextView.setVisibility(View.GONE);
                        }
                        else{
                            messageViewHolder.usernameTextView.setVisibility(View.VISIBLE);
                        }
                        GradientDrawable bgShape = (GradientDrawable)messageViewHolder.messageContent.getBackground();
                        bgShape.setColor(Color.WHITE);
                        bgShape.setStroke(2, ContextCompat.getColor(activity, R.color.highlightGrey));
                        LinearLayout.LayoutParams messageContentLP = (LinearLayout.LayoutParams) messageViewHolder.messageContent.getLayoutParams();
                        messageContentLP.gravity = Gravity.LEFT;
                        messageViewHolder.messageContent.setLayoutParams(messageContentLP);
                    }

                    if (messageObject.getText() != null) {
                        messageViewHolder.messageTextView.setText(messageObject.getText());
                        messageViewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                        messageViewHolder.messageImageView.setVisibility(ImageView.GONE);

                    } else {
                        String imageUrl = messageObject.getImageUrl();
                        if (imageUrl.startsWith("gs://")) {
                            StorageReference storageReference = FirebaseStorage.getInstance()
                                    .getReferenceFromUrl(imageUrl);
                            storageReference.getDownloadUrl().addOnCompleteListener(
                                    new OnCompleteListener<Uri>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Uri> task) {
                                            if (task.isSuccessful()) {
                                                String downloadUrl = task.getResult().toString();
                                                Glide.with(messageViewHolder.messageImageView.getContext())
                                                        .load(downloadUrl)
                                                        .into(messageViewHolder.messageImageView);
                                            } else {
                                                Log.w(TAG, "Getting download url was not successful.",
                                                        task.getException());
                                            }
                                        }
                                    });
                        } else {
                            Glide.with(messageViewHolder.messageImageView.getContext())
                                    .load(messageObject.getImageUrl())
                                    .into(messageViewHolder.messageImageView);
                        }
                        messageViewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                        messageViewHolder.messageTextView.setVisibility(TextView.GONE);
                    }

                    messageViewHolder.usernameTextView.setText(messageObject.getName());
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
                else {
                    //this is a room event message item
                    ((EventMessageViewHolder) viewHolder).eventMessageText.setText(messageObject.getText());
                }
            }

            @Override
            public void onChildChanged(@NonNull ChangeEventType type,
                                       @NonNull DataSnapshot snapshot,
                                       int newIndex,
                                       int oldIndex) {
                switch (type) {
                    case ADDED:
                        notifyItemInserted(newIndex);
                        if(getItem(newIndex).getName() == null){

                            String[] parts = getItem(newIndex).getText().split(" ");
                            if(parts[0].equals(mUsername)){
                                break;
                            }
                            if(parts.length == 2 && parts[1].equals("left")){ //[username] [left]
                                updateUserLeave(parts[0]);
                            }
                            else if(parts.length >= 3){
                                //String subject = parts[0];
                                String verb = parts[1];

                                if(verb.equals("removed")){
                                    for(int i = 2; i<parts.length; i++){
                                        if(!parts[i].equals("and")){ //"and" is a reserved word so no users can use that username
                                            String username = parts[i];
                                            char lastChar = username.charAt(username.length()-1);
                                            if(lastChar == ',' || lastChar == '.' || lastChar == ' ' || lastChar == '!'){
                                                username = username.substring(0, username.length() - 2);
                                            }
                                            updateUserLeave(username);
                                        }
                                    }
                                }
                                else if(verb.equals("invited")){
                                    for(int i = 2; i<parts.length; i++){
                                        if(!parts[i].equals("and")){ //"and" is a reserved word so no users can use that username
                                            String username = parts[i];
                                            char lastChar = username.charAt(username.length()-1);
                                            if(lastChar == ',' || lastChar == '.' || lastChar == ' ' || lastChar == '!'){
                                                username = username.substring(0, username.length() - 2);
                                            }
                                            updateUserInvite(username);
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case CHANGED:
                        notifyItemChanged(newIndex);
                        break;
                    case REMOVED:
                        notifyItemRemoved(newIndex);
                        break;
                    case MOVED:
                        notifyItemMoved(oldIndex, newIndex);
                        break;
                    default:
                        throw new IllegalStateException("Incomplete case statement");
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

    private void sendRoomEventMessage(String messageText, String rnum) {
        if (existingRoomUsersList != null) {
            MessageObject messageObject = new MessageObject(messageText, null, null);

            for (String username : existingRoomUsersList) {
                if (username.indexOf('*') > 0) {
                    int numberCode = Integer.parseInt(username.substring(username.indexOf('*') + 1));
                    if (numberCode == 1 || numberCode == 3) {
                        String pureUsername = username.substring(0, username.indexOf('*'));

                        String WRITE_PATH = getUsernameHash(pureUsername) + "/" + pureUsername + "/messages/" + rnum;
                        //final String username = usi.getUsername();
                        mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                    }
                } else {
                    String WRITE_PATH = getUsernameHash(username) + "/" + username + "/messages/" + rnum;
                    //final String username = usi.getUsername();
                    mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                }
            }
        }
    }

    private void updateUserInvite(String username){
        String usernameFinal = username;
        boolean inviteUpdated = false;

        if(existingRoomUsersList != null){
            for(int i=0; i<existingRoomUsersList.size(); i++){
                int numberCodeIndex = existingRoomUsersList.get(i).indexOf('*');
                if(numberCodeIndex > 0){
                    if(!existingRoomUsersList.get(i).substring(0, numberCodeIndex).equals(username)){
                        continue;
                    }

                    int numberCode = Integer.parseInt(existingRoomUsersList.get(i).substring(numberCodeIndex+1));
                    if(numberCode == 0){
                        usernameFinal = username + "*1";
                    }
                    else if(numberCode == 2){
                        usernameFinal = username + "*3";
                    }
                }
                else{
                    if(!existingRoomUsersList.get(i).equals(username)){
                        continue;
                    }
                    usernameFinal = username + "*0";
                }

                existingRoomUsersList.set(i, usernameFinal);
                inviteUpdated = true;
                break;
            }
        }
        else if(newRoomInviteList != null){
            for(int i = 0; i < newRoomInviteList.size(); i++){
                int numberCodeIndex = newRoomInviteList.get(i).indexOf('*');
                if(numberCodeIndex > 0){
                    if(!newRoomInviteList.get(i).substring(0, numberCodeIndex).equals(username)){
                        continue;
                    }

                    int numberCode = Integer.parseInt(newRoomInviteList.get(i).substring(numberCodeIndex+1));
                    if(numberCode == 0){
                        usernameFinal = username + "*1";
                    }
                    else if(numberCode == 2){
                        usernameFinal = username + "*3";
                    }
                }
                else{
                    if(!newRoomInviteList.get(i).equals(username)){
                        continue;
                    }
                    usernameFinal = username + "*0";
                }
                newRoomInviteList.set(i, usernameFinal);
                inviteUpdated = true;
                break;
            }
        }

        if(!inviteUpdated){ //this is a new user to the group
            if(existingRoomUsersList != null){
                existingRoomUsersList.add(username);
            }
            else if(newRoomInviteList != null){
                newRoomInviteList.add(username);
            }
        }
    }

    private void updateUserLeave(String username){
        String usernameFinal = username;

        if(existingRoomUsersList != null){
            for(int i=0; i<existingRoomUsersList.size(); i++){
                int numberCodeIndex = existingRoomUsersList.get(i).indexOf('*');
                if(numberCodeIndex > 0){
                    if(!existingRoomUsersList.get(i).substring(0, numberCodeIndex).equals(username)){
                        continue;
                    }

                    int numberCode = Integer.parseInt(existingRoomUsersList.get(i).substring(numberCodeIndex+1));
                    if(numberCode == 1){
                        usernameFinal = username + "*2";
                    }
                    else if(numberCode == 3){
                        usernameFinal = username + "*4";
                    }
                }
                else{
                    if(!existingRoomUsersList.get(i).equals(username)){
                        continue;
                    }
                    usernameFinal = username + "*0";
                }
                existingRoomUsersList.set(i, usernameFinal);
                break;
            }
        }
        else if(newRoomInviteList != null){
            for(int i = 0; i < newRoomInviteList.size(); i++){
                int numberCodeIndex = newRoomInviteList.get(i).indexOf('*');
                if(numberCodeIndex > 0){
                    if(!newRoomInviteList.get(i).substring(0, numberCodeIndex).equals(username)){
                        continue;
                    }

                    int numberCode = Integer.parseInt(newRoomInviteList.get(i).substring(numberCodeIndex+1));
                    if(numberCode == 1){
                        usernameFinal = username + "*2";
                    }
                    else if(numberCode == 3){
                        usernameFinal = username + "*4";
                    }
                }
                else{
                    if(!newRoomInviteList.get(i).equals(username)){
                        continue;
                    }
                    usernameFinal = username + "*0";
                }
                newRoomInviteList.set(i, usernameFinal);
                break;
            }
        }
    }

    private void setUpRoomInDB(String preview, final MessageObject messageObject, final Uri uri){
        activity.getMessengerFragment().setClickedRoomNum(roomNum);
        MESSAGES_CHILD = MESSAGES_CHILD_BODY + roomNum;
        final ArrayList<String> roomUsersHolderList;
        if(newRoomInviteList != null){
            roomUsersHolderList = new ArrayList<>();
            for(String invitedUsername : newRoomInviteList){
                roomUsersHolderList.add(invitedUsername);
            }
        }
        else {
            roomUsersHolderList = existingRoomUsersList; //just safety, don't think this line is necessary under normal circumstances
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

        roomUsersHolderList.add(0, mUsername);

        final boolean isDM = roomUsersHolderList.size() == 2;
        String groupChatOpening;
        final MessageObject eventMessageObject;
        if(!isDM){
            String head;
            if(defaultRoomName){
                head = mUsername + " created a group chat with ";
            }
            else{
                head = mUsername + " created " + currentRoomTitle + " with ";
            }
            StringBuilder tail = new StringBuilder();

            for (int i = 1; i<roomUsersHolderList.size(); i++){ //since last item in this list is mUsername, we don't have to iterate the last item for this
                String username = roomUsersHolderList.get(i);
                if(i == 1){
                    tail.append(username);
                }
                else if(i + 1 == roomUsersHolderList.size()) {
                    tail.append(", and " + username + "!");
                }
                else {
                    tail.append(", " + username);
                }
            }
            groupChatOpening = head + tail.toString();
            eventMessageObject = new MessageObject(groupChatOpening, null,null);
        }
        else{
            eventMessageObject = null;
        }

        final RoomObject roomObject = new RoomObject(currentRoomTitle, System.currentTimeMillis(), preview, roomUsersHolderList);
        String userRoomPath = activity.getUserPath() + "r/" + roomNum;
        mFirebaseDatabaseReference.child(userRoomPath).setValue(roomObject).addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(eventMessageObject != null){
                    mFirebaseDatabaseReference.child(activity.getUserPath()+"messages/"+roomNum).push().setValue(eventMessageObject);
                }

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
            }
        });

        if(activity.getMessengerFragment().isEmpty()){
            activity.getMessengerFragment().initializeFragmentAfterFirstRoomCreation();
        }

        if(roomObject.getUsers() != null && roomObject.getUsers().size() == 2){
            roomObject.setName(mUsername);
        }

        setRoomObjListener(roomNum);

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
                if(!(isDM && activity.getMessengerFragment().blockedFromUser(mName))){
                    mFirebaseDatabaseReference.child(roomPath).setValue(roomObject).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(eventMessageObject != null){
                                String eventMessagePath = getUsernameHash(mName) + "/" + mName + "/messages/" + roomNum;
                                mFirebaseDatabaseReference.child(eventMessagePath).push().setValue(eventMessageObject);
                            }

                            String WRITE_PATH = usernameHash + "/" + mName + "/messages/" + roomNum;
                            //final String username = usi.getUsername();
                            mFirebaseDatabaseReference.child(WRITE_PATH).push().setValue(messageObject);
                        }
                    });
                }
            }
        }

    }

    public ArrayList<String> getUsersList(){
        if(newRoomInviteList != null){
            String pureUsername = newRoomInviteList.get(0);
            if(pureUsername.indexOf('*')>0){
                pureUsername = pureUsername.substring(0, pureUsername.indexOf('*'));
            }
            if(pureUsername.equals(mUsername)){
                return newRoomInviteList;
            }
            else{
                ArrayList<String> returnList = new ArrayList<>(newRoomInviteList);
                returnList.add(0, mUsername);
                return returnList;
            }
        }
        else{
            return existingRoomUsersList;
        }
    }

    public String getCurrentRoomTitle(){
        return currentRoomTitle;
    }

    public String getAdapterRNum(){
        return adapterRNum;
    }

    public void setRoomObjListener(String roomNum){
        if(roomObjListener != null){
            return;
        }
        String rPath = activity.getUserPath()+"r/" + roomNum;
        Log.d("ROLT", "rolt added to roomNum: " + roomNum);
        initialRoomInfoLoaded = false;
        activity.getMessengerFragment().setClickedRoomNum(roomNum);

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
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //if user is removed from the room
                activity.getViewPager().setCurrentItem(4);
            }

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
                    if(activity.getViewPager().getCurrentItem() == 11){
                        activity.showOverflowMenu();
                        activity.showRightChevron();
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
            Log.d("ROLT", "ROLT removed for roomNum: " + roomNum);
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
        adapterRNum = "";
    }

    public boolean isRoomDM(){
        return roomIsDM;
    }
}