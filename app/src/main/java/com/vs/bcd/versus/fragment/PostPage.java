package com.vs.bcd.versus.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.loopj.android.http.HttpGet;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.ArrayAdapterWithIcon;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.CustomEditText;
import com.vs.bcd.versus.model.FormValidator;
import com.vs.bcd.versus.model.MedalUpdateRequest;
import com.vs.bcd.versus.model.NotificationItem;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.TopCardObject;
import com.vs.bcd.versus.model.UserAction;
import com.vs.bcd.versus.model.VSCNode;
import com.vs.bcd.versus.model.VSComment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
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

import static com.vs.bcd.versus.adapter.PostPageAdapter.DOWNVOTE;
import static com.vs.bcd.versus.adapter.PostPageAdapter.UPVOTE;


/**
 * Created by dlee on 6/7/17.
 */

public class PostPage extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private EditText commentInput;
    private RelativeLayout mRelativeLayout;
    private PostPageAdapter PPAdapter;
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private String postID = "";
    private String postTopic;
    private String postX;
    private String postY;
    private Post post;
    private SessionManager sessionManager;
    private List<Object> vsComments = new ArrayList<>(); //ArrayList of VSCNode
    private ViewGroup.LayoutParams RVLayoutParams;
    private RecyclerView RV;
    private LayoutInflater layoutInflater;
    private VSCNode prevRootLevelNode = null;
    private VSCNode firstRoot = null; //first comment in root level, as in parent_id = "0"
    private MainContainer activity;
    private boolean topCardActive = false;
    private VSComment topCardContent = null;
    private UserAction userAction;
    private boolean redIncrementedLast, blackIncrementedLast;
    private Map<String, String> actionMap;
    private Map<String, String> actionHistoryMap; //used to store previous user action on a comment, if any, for comparing with current user action, e.g. if user chose upvote and previously chose downvote, then we need to do both increment upvote and decrement downvote
    private int origRedCount, origBlackCount;
    private String lastSubmittedVote = "none";
    private int retrievalLimit = 25;
    private Map<String,AttributeValue> lastEvaluatedKey;
    private PostPage thisPage;
    private boolean exitLoop = false;
    private Button topcardSortTypeSelector;
    private LinearLayout topcardSortTypeSelectorBackground;
    final HashMap<String, VSCNode> nodeMap = new HashMap<>();
    private HashMap<String, VSComment> parentCache = new HashMap<>();
    private HashMap<String, Pair<Integer, Integer>> freshlyVotedComments = new HashMap<>();
    private String freshlyVotedCommentsListPostID = "";
    private boolean atRootLevel = true;
    private long queryThreadID = 0;
    private int sortType = 1; //0 = New, 1 = Popular
    private final int MOST_RECENT = 0;
    private final int POPULAR = 1;
    private final int CHRONOLOGICAL = 2;
    private boolean nowLoading = false;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean initialLoadInProgress = false;
    private boolean xmlLoaded = false;
    private volatile boolean dbWriteComplete = false;
    private int minUpvotes = 5;
    private int loadThreshold = 5;

    private int goldPoints = 30;
    private int silverPoints = 15;
    private int bronzePoints = 5;

    private double votePSI = 2.0; //ps increment per vote
    private int currCommentsIndex = 0;
    private int childrenCount = 0;
    private int retrievalSize = 30;

    private DatabaseReference mFirebaseDatabaseReference;
    private String host, region;

    private int pageLevel = 0;
    private int topCardSortType = POPULAR;

    private Toast mToast;

    private ImageButton sendButton;
    private CustomEditText pageCommentInput;
    private RelativeLayout pageCommentInputContainer;
    private VSComment replyTarget;
    private TextView replyingTo, replyTV;
    private int replyTargetIndex;
    private int trueReplyTargetIndex;
    private VSComment trueReplyTarget;
    private int editIndex;
    private VSComment editTarget;

    private Stack<List<Object>> masterListStack = new Stack<>();
    private Stack<Integer> scrollPositionStack = new Stack<>();
    private int viewTreeTriggerCount = 0;

    private InputMethodManager imm;
    private HashMap<String, Integer> medalWinnersList = new HashMap<>();

    private double commentPSI = 3.0; //ps increment per comment
    private String postRefreshCode = ""; //r == increment red, b == increment blue, rb == increment red, decrement blue, br == increment blue, decrement red

    private PostPage thisFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.post_page, container, false);
        layoutInflater = inflater;

        host = activity.getESHost();
        region = activity.getESRegion();
        thisFragment = this;

        imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        pageCommentInputContainer = rootView.findViewById(R.id.page_comment_input_container);

        sendButton = rootView.findViewById(R.id.comment_send_button);
        pageCommentInput = rootView.findViewById(R.id.page_comment_input);
        replyingTo = rootView.findViewById(R.id.replying_to);
        replyTV = rootView.findViewById(R.id.reply_target_tv);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            public void onGlobalLayout(){
                if((replyTarget != null || editTarget != null) && viewTreeTriggerCount == 0 && ((float)rootView.getHeight())/((float)rootView.getRootView().getHeight()) < 0.6){
                    ((LinearLayoutManager)RV.getLayoutManager()).scrollToPositionWithOffset(trueReplyTargetIndex, 0);
                    viewTreeTriggerCount++;
                }

            }
        });

        pageCommentInput.addTextChangedListener(new FormValidator(pageCommentInput) {
            @Override
            public void validate(TextView textView, String text) {
                if (text.length() > 0) {
                    sendButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_send_blue));
                    String prefix = pageCommentInput.getPrefix();
                    if(prefix != null){
                        if(!text.startsWith(prefix)){
                            String newText = prefix + text.substring(prefix.length()-1);
                            pageCommentInput.setText(newText);

                            Selection.setSelection(pageCommentInput.getText(), prefix.length());
                        }
                    }

                } else {
                    sendButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_send_grey));
                }
            }
        });


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String input = pageCommentInput.getText().toString().trim();


                Runnable runnable = new Runnable() {
                    public void run() {

                        if(input.length() > 0){
                            if(editTarget != null){

                                if(editTarget.getContent().equals(input)){
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            editTarget.setIsHighlighted(false);
                                            PPAdapter.notifyItemChanged(editIndex);
                                            pageCommentInput.setText("");
                                            hideCommentInputCursor();
                                            imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                                        }
                                    });
                                    return;
                                }

                                //update comment content through ddb update request
                                HashMap<String, AttributeValue> keyMap = new HashMap<>();
                                keyMap.put("i", new AttributeValue().withS(editTarget.getComment_id()));

                                HashMap<String, AttributeValueUpdate> updates = new HashMap<>();

                                AttributeValueUpdate ct = new AttributeValueUpdate()
                                        .withValue(new AttributeValue().withS(input))
                                        .withAction(AttributeAction.PUT);
                                updates.put("ct", ct);

                                UpdateItemRequest request = new UpdateItemRequest()
                                        .withTableName("vscomment")
                                        .withKey(keyMap)
                                        .withAttributeUpdates(updates);
                                activity.getDDBClient().updateItem(request);

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //update content in local copy of the comment, first in nodemap, then in masterlist
                                        editCommentLocal(editIndex, input, editTarget.getComment_id());
                                        editTarget.setIsHighlighted(false);
                                        PPAdapter.notifyItemChanged(editIndex);
                                        pageCommentInput.setText("");
                                        hideCommentInputCursor();
                                        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                                    }
                                });

                            }
                            else{

                                String targetID = "";
                                int targetChildCount = 0;
                                int targetNestedLevel = 0;
                                int targetIndex = 0;

                                boolean insertReplyInCurrentPage = false;
                                final VSComment vsc = new VSComment();

                                if(replyTarget == null){
                                    if(pageLevel == 0){
                                        vsc.setParent_id(postID);
                                    }
                                    else{
                                        vsc.setParent_id(topCardContent.getComment_id());
                                    }

                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            PPAdapter.clearList();
                                            mSwipeRefreshLayout.setRefreshing(true);
                                        }
                                    });
                                }
                                else{
                                    targetID = replyTarget.getComment_id();
                                    targetChildCount = replyTarget.getChild_count();
                                    targetNestedLevel = replyTarget.getNestedLevel();
                                    targetIndex = replyTargetIndex;

                                    vsc.setParent_id(targetID);

                                    if(pageLevel < 2 && targetChildCount < 2){
                                        //insert comment into the existing tree in the page
                                        insertReplyInCurrentPage = true;
                                    }
                                    else{

                                        if(PPAdapter != null) {
                                            List<Object> masterList = PPAdapter.getMasterList();

                                            if(!masterList.isEmpty()){
                                                List<Object> stackEntry = new ArrayList<>();
                                                stackEntry.addAll(masterList);

                                                masterListStack.push(stackEntry);

                                                scrollPositionStack.push(((LinearLayoutManager) RV.getLayoutManager()).findFirstVisibleItemPosition());

                                                if(stackEntry.get(0) instanceof Post && replyTarget.getNestedLevel() > 0){ //pageLevel is already incremented at this point so we check for pageLevel == 1
                                                    scrollPositionStack.push(-1);
                                                }
                                            }
                                        }

                                        //add comment in a new page with replyTarget as top card
                                        if(targetNestedLevel == 2){
                                            pageLevel = 2;
                                        }
                                        else{
                                            pageLevel += targetNestedLevel + 1;
                                        }

                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                PPAdapter.clearList();
                                                mSwipeRefreshLayout.setRefreshing(true);
                                            }
                                        });

                                    }
                                }

                                vsc.setPost_id(postID);
                                vsc.setAuthor(activity.getUsername());
                                vsc.setContent(input);
                                vsc.setIsNew(true); //sets it to be highlighted

                                activity.getMapper().save(vsc);

                                //send appropriate notification
                                if(replyTarget == null){ //if root comment
                                    if(pageLevel == 0 && post != null && !post.getAuthor().equals("[deleted]") && !post.getAuthor().equals(activity.getUsername())){
                                        String nKey = postID+":"+sanitizeContentForURL(post.getRedname())+":"+sanitizeContentForURL(post.getBlackname());
                                        String postAuthorPath = getUsernameHash(post.getAuthor()) + "/" + post.getAuthor() + "/n/r/" + nKey;
                                        mFirebaseDatabaseReference.child(postAuthorPath).child(activity.getUsername()).setValue(System.currentTimeMillis()/1000);  //set value = timestamp as seconds from epoch
                                    }
                                    else if(topCardContent != null && !topCardContent.getAuthor().equals("[deleted]") && !topCardContent.getAuthor().equals(activity.getUsername())){
                                        String payloadContent = sanitizeContentForURL(topCardContent.getContent());
                                        String subjectAuthorPath = getUsernameHash(topCardContent.getAuthor()) + "/" + topCardContent.getAuthor() + "/n/c/"
                                                + topCardContent.getComment_id() + ":" + payloadContent;
                                        mFirebaseDatabaseReference.child(subjectAuthorPath).child(activity.getUsername()).setValue(System.currentTimeMillis()/1000);
                                    }
                                }
                                else if(trueReplyTarget != null && !trueReplyTarget.getAuthor().equals("[deleted]") && !trueReplyTarget.getAuthor().equals(activity.getUsername())){ //reply to a comment
                                    String payloadContent = sanitizeContentForURL(trueReplyTarget.getContent());
                                    String subjectAuthorPath = getUsernameHash(trueReplyTarget.getAuthor()) + "/" + trueReplyTarget.getAuthor() + "/n/c/"
                                            + trueReplyTarget.getComment_id() + ":" + payloadContent;
                                    mFirebaseDatabaseReference.child(subjectAuthorPath).child(activity.getUsername()).setValue(System.currentTimeMillis()/1000);
                                }


                                HashMap<String, AttributeValue> keyMap = new HashMap<>();
                                keyMap.put("i", new AttributeValue().withS(postID));   //sort key

                                HashMap<String, AttributeValueUpdate> updates = new HashMap<>();

                                //update pt and increment ps
                                int currPt = (int)((System.currentTimeMillis()/1000)/60);
                                AttributeValueUpdate ptu = new AttributeValueUpdate()
                                        .withValue(new AttributeValue().withN(Integer.toString(currPt)))
                                        .withAction(AttributeAction.PUT);
                                updates.put("pt", ptu);

                                int timeDiff;
                                if(post == null){
                                    timeDiff = currPt - activity.getCurrentPost().getPt();
                                }
                                else{
                                    timeDiff = currPt - post.getPt();
                                }
                                if(timeDiff <= 0){ //if timeDiff is negative due to some bug or if timeDiff is zero, we just make it equal 1.
                                    timeDiff = 1;
                                }

                                double psIncrement = commentPSI/timeDiff;
                                AttributeValueUpdate psu = new AttributeValueUpdate()
                                        .withValue(new AttributeValue().withN(Double.toString(psIncrement)))
                                        .withAction(AttributeAction.ADD);
                                updates.put("ps", psu);

                                UpdateItemRequest request = new UpdateItemRequest()
                                        .withTableName("post")
                                        .withKey(keyMap)
                                        .withAttributeUpdates(updates);
                                activity.getDDBClient().updateItem(request);

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pageCommentInput.setText("");
                                        hideCommentInputCursor();
                                        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                                    }
                                });

                                if(insertReplyInCurrentPage){

                                    vsc.setNestedLevel(targetNestedLevel + 1);
                                    VSCNode parentNode = nodeMap.get(targetID);
                                    VSCNode thisNode = new VSCNode(vsc);
                                    int offset = 0;

                                    if(targetChildCount > 0){
                                        VSCNode firstChild = parentNode.getFirstChild();
                                        thisNode.setHeadSibling(parentNode.getFirstChild());
                                        firstChild.setTailSibling(thisNode);
                                        offset += 2;

                                        if(pageLevel == 0 && targetNestedLevel == 0){
                                            offset += firstChild.getNodeContent().getChild_count();
                                        }
                                    }
                                    else{
                                        thisNode.setParent(parentNode);
                                        parentNode.setFirstChild(thisNode);
                                        offset++;
                                    }

                                    final int insertionIndex = targetIndex + offset;
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            PPAdapter.insertItem(vsc, insertionIndex);
                                        }
                                    });

                                    nodeMap.put(vsc.getComment_id(), thisNode);
                                }
                                else{
                                    commentSubmissionRefresh(vsc);
                                }
                                if(!(targetID.equals(""))){
                                    nodeMap.get(targetID).getNodeContent().incrementChild_Count();
                                }
                            }
                        }
                    }
                };
                Thread mythread = new Thread(runnable);
                mythread.start();
            }
        });

        mRelativeLayout =  rootView.findViewById(R.id.post_page_layout);
        RV = rootView.findViewById(R.id.recycler_view_cs);
        RV.setLayoutManager(new LinearLayoutManager(activity));
        RVLayoutParams = RV.getLayoutParams();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        hideTopCard();

        sessionManager = new SessionManager(getActivity());

        //TODO: look into cheap synching scheme to keep comments updated realtime

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }
        disableChildViews();

        // SwipeRefreshLayout
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_container_postpage);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if(initialLoadInProgress){
            mSwipeRefreshLayout.setRefreshing(true);
        }
        xmlLoaded = true;


        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //save the activity to a member of this fragment
        activity = (MainContainer)context;
        thisPage = this;
    }

    public void hideCommentInputCursor(){
        pageCommentInputContainer.requestFocusFromTouch();
    }

    /**
     * This method is called when swipe refresh is pulled down
     */
    @Override
    public void onRefresh() {
        PPAdapter.setLockButtons(true);
        nowLoading = false;

        dbWriteComplete = false;

        final boolean writingPostVoteToDB = (!lastSubmittedVote.equals(userAction.getVotedSide()));

        if (redIncrementedLast) {
            postRefreshCode = "r";
            if (lastSubmittedVote.equals("BLK")) {
                postRefreshCode = "rb";
            }
        } else {
            postRefreshCode = "b";
            if (lastSubmittedVote.equals("RED")) {
                postRefreshCode = "br";
            }
        }

        writeActionsToDB();

        mSwipeRefreshLayout.setRefreshing(true);

        Runnable runnable = new Runnable() {
            public void run() {
                try{
                    long end = System.currentTimeMillis() + 8*1000; // 8 seconds * 1000 ms/sec

                    while(!dbWriteComplete && System.currentTimeMillis() < end){

                    }
                    if(!dbWriteComplete){
                        Log.d("PostPageRefresh", "user actions dbWrite timeout");
                        dbWriteComplete = true;
                    }
                    else{
                        Log.d("PostPageRefresh", "user actions dbWrite successful");
                    }

                    //update post card if atRootLevel, else update top card
                    if(atRootLevel){
                        post = getPost(postID, writingPostVoteToDB);
                        postTopic = post.getQuestion();
                        postX = post.getRedname();
                        postY = post.getBlackname();
                        origRedCount = post.getRedcount();
                        origBlackCount = post.getBlackcount();
                        redIncrementedLast = false;
                        blackIncrementedLast = false;
                    }
                    else{
                        final VSComment updatedTopCardContent = getComment(topCardContent.getComment_id());
                        nodeMap.get(topCardContent.getComment_id()).setNodeContent(updatedTopCardContent);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String actionEntry = actionMap.get(updatedTopCardContent.getComment_id());
                                if(actionEntry != null){
                                    switch (actionEntry){
                                        case "U":
                                            updatedTopCardContent.initialSetUservote(UPVOTE);
                                            break;

                                        case "D":
                                            updatedTopCardContent.initialSetUservote(DOWNVOTE);
                                            break;
                                        //we ignore case "N" because uservote is 0 by default so we don't need to set it here
                                    }
                                }
                                parentCache.put(updatedTopCardContent.getComment_id(), updatedTopCardContent);
                                setUpTopCard(parentCache.get(updatedTopCardContent.getComment_id()));
                            }
                        });
                    }

                    switch (sortType){
                        case POPULAR:
                            refreshCommentUpvotesQuery();
                            break;
                        case MOST_RECENT:
                            refreshCommentTimestampQuery();
                            break;
                        case CHRONOLOGICAL:
                            refreshCommentChronologicalQuery();
                            break;
                        default:
                            break;
                    }


                }catch (Throwable t){

                }

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }


    private void refreshCommentUpvotesQuery(){

        //lastEvaluatedKey = null;
        //clearList();

        if(atRootLevel){
            commentsQuery(postID, "u");
        }
        else {
            commentsQuery(topCardContent.getComment_id(), "u");
        }


        nowLoading = false;

    }

    private void refreshCommentTimestampQuery(){

        //lastEvaluatedKey = null;
        //clearList();

        if(atRootLevel){
            commentsQuery(postID, "t");
        }
        else {
            commentsQuery(topCardContent.getComment_id(), "t");
        }

        nowLoading = false;
    }

    private void refreshCommentChronologicalQuery(){
        if(atRootLevel){
            commentsQuery(postID, "c");
        }
        else {
            commentsQuery(topCardContent.getComment_id(), "c");
        }

        nowLoading = false;
    }



    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null) {
                enableChildViews();
                rootView.findViewById(R.id.recycler_view_cs).setLayoutParams(RVLayoutParams);
                RVLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                RVLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                RV.setLayoutParams(RVLayoutParams);
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

        }
        else {
            if (rootView != null) {
                disableChildViews();
                pageCommentInput.setText("");
                RVLayoutParams.height = 0;
                RVLayoutParams.width = 0;
                RV.setLayoutParams(RVLayoutParams);
            }
        }
    }

    public void clearList(){
        nodeMap.clear();
        vsComments.clear();
        topCardContent = null;
    }

    public void enableChildViews(){
        if(topCardContent != null){//for re-entry from CommentEnterFragment. Without this setUpTopCard, enableChildViews hides top card from view
            setUpTopCard(topCardContent);
        }
        for(int i = 0; i<childViews.size(); i++){
            if( !(childViews.get(i) instanceof FloatingActionButton) ){

                childViews.get(i).setEnabled(true);
                childViews.get(i).setClickable(true);
                childViews.get(i).setLayoutParams(LPStore.get(i));
            }
        }
    }

    public void disableChildViews() {
        for (int i = 0; i < childViews.size(); i++) {
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0, 0));
        }
    }

    public void setPageLevel(int target){
        pageLevel = target;
    }

    private void submitMedalUpdate(int currentMedal, VSComment medalWinner){

        int pointsIncrement = 0;

        String mUsername = medalWinner.getAuthor();
        int usernameHash;
        if(mUsername.length() < 5){
            usernameHash = mUsername.hashCode();
        }
        else{
            String hashIn = "" + mUsername.charAt(0) + mUsername.charAt(mUsername.length() - 2) + mUsername.charAt(1) + mUsername.charAt(mUsername.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        String incrementKey = null;

        switch(currentMedal){   //set updates for user num_g/num_s/num_b increment and comment topmedal
            case 3: //gold medal won
                incrementKey = "g";
                pointsIncrement = goldPoints;
                break;

            case 2: //silver medal won
                incrementKey = "s";
                pointsIncrement = silverPoints;
                break;

            case 1: //bronze medal won
                incrementKey = "b";
                pointsIncrement = bronzePoints;
                break;
        }

        if(incrementKey != null){
            String decrementKey = "";

            switch(medalWinner.getTopmedal()){ //set updates for user num_s/num_b decrement and points
                case 0: //went from no medal to currentMedal
                    break;

                case 1: //went from bronze to currentMedal
                    decrementKey = "b";
                    pointsIncrement -= bronzePoints;
                    break;

                case 2: //went from silver to currentMedal
                    decrementKey = "s";
                    pointsIncrement -= silverPoints;
                    break;

                //no case 3 if it was already gold then it's not a upgrade event

            }
            String medalType = incrementKey + decrementKey;
            int timeValueSecs = (int) (System.currentTimeMillis() / 1000);
            int timeValue = ((timeValueSecs / 60 )/ 60 )/ 24; //now timeValue is in days since epoch
            //submit update request to firebase updates path, the first submission will trigger Cloud Functions operation to update user medals and points
            String updateRequest = "updates/" + Integer.toString(timeValue) + "/" + Integer.toString(usernameHash)  + "/" + mUsername + "/" + medalWinner.getComment_id() + "/" + medalType;
            MedalUpdateRequest medalUpdateRequest = new MedalUpdateRequest(pointsIncrement, timeValueSecs, sanitizeContentForURL(medalWinner.getContent()));
            mFirebaseDatabaseReference.child(updateRequest).setValue(medalUpdateRequest);

        }

    }

    private void setMedals(){
        if(medalWinnersList == null){
            medalWinnersList = new HashMap<>();
        }
        else{
            medalWinnersList.clear();
        }
        Runnable runnable = new Runnable() {
            public void run() {
                String query = "/vscomment/_search";
                String payload = "{\"size\":15,\"sort\":[{\"u\":{\"order\":\"desc\"}}],\"query\":{\"match\":{\"pt\":\""+postID+"\"}}}";

                String url = "https://" + host + query;

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

                    JSONObject obj = new JSONObject(strResponse);
                    JSONArray hits = obj.getJSONObject("hits").getJSONArray("hits");

                    if(hits.length() == 0){
                        return;
                    }
                    ArrayList<VSComment> medalWinners = new ArrayList<>();
                    for(int i = 0; i < hits.length(); i++){
                        JSONObject item = hits.getJSONObject(i).getJSONObject("_source");
                        //VSComment medalWinner = new VSComment(item);
                        medalWinners.add(new VSComment(item));
                    }

                    Collections.sort(medalWinners, new Comparator<VSComment>() {
                        @Override
                        public int compare(VSComment o1, VSComment o2) {
                            int diff = o2.getUpvotes() - o1.getUpvotes();
                            if(diff != 0){
                                return diff;
                            }
                            else {
                                return o2.getDownvotes() - o1.getDownvotes();
                            }
                        }
                    });
                    //TODO: eventually, modify the ES query so that we only grab comments with upvote >= 10

                    int currentMedal = 3;
                    int lastWinnerUpvotes = 0;
                    int lastWinnerDownvotes = 0;

                    for(int i = 0; i<medalWinners.size(); i++){
                        VSComment medalWinner = medalWinners.get(i);
                        if(lastWinnerUpvotes > medalWinner.getUpvotes()){
                            currentMedal--;
                        }
                        else if(lastWinnerUpvotes == medalWinner.getUpvotes() && lastWinnerDownvotes > medalWinner.getDownvotes()){
                            currentMedal--;
                        }
                        if(currentMedal < 1 || medalWinner.getUpvotes() < 10){
                            break;
                        }
                        if(medalWinner.getTopmedal() < currentMedal){
                            submitMedalUpdate(currentMedal, medalWinner);
                        }
                        medalWinnersList.put(medalWinner.getComment_id(), currentMedal);
                        lastWinnerUpvotes = medalWinner.getUpvotes();
                        lastWinnerDownvotes = medalWinner.getDownvotes();
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(medalWinnersList.size() > 0){
                                if(PPAdapter != null){
                                    PPAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }



            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }



    /*
    grab n roots. n = retrievalLimit.
        then iterate through each roots
            for each root, get two children
                for each children, get two children
            then we will have two children and four grandchildren per root, and n such roots.
    LoadMore loads more roots and does same thing.
    */

    //automatically includes Post Card in the vsComments list for recycler view if rootParentId equals postID,
    // so use it for all PostPage set up cases where we query by upvotes
    private void commentsQuery(final String rootParentID, final String uORt){
        setMedals();

        if(pageLevel != 2) { //"g" denotes grandchild-only query for level 2
            final ArrayList<VSComment> rootComments = new ArrayList<>();
            final ArrayList<VSComment> childComments = new ArrayList<>();
            final ArrayList<VSComment> grandchildComments = new ArrayList<>();

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });

            Runnable runnable = new Runnable() {
                public void run() {
                    long thisThreadID = Thread.currentThread().getId();
                    final List<Object> masterList = new ArrayList<>();

                    getRootComments(0, rootComments, rootParentID, uORt);

                    chunkSorter(rootComments);

                    VSCNode prevNode = null;

                    final HashMap<Integer, VSComment> medalUpgradeMap = new HashMap<>();
                    final HashSet<String> medalWinners = new HashSet<>();
                    exitLoop = false;

                    if (!rootComments.isEmpty()) {
                        int currMedalNumber = 3; //starting with 3, which is gold medal
                        int prevUpvotes = rootComments.get(0).getUpvotes();
                        VSComment currComment;

                        //set up nodeMap with root comments
                        for (int i = 0; i < rootComments.size(); i++) {
                            if (thisThreadID != queryThreadID) {
                                Log.d("wow", "broke out of old query thread");
                                return;
                            }
                            currComment = rootComments.get(i);
                            VSCNode cNode = new VSCNode(currComment);
                            final String commentID = currComment.getComment_id();

                            Log.d("wow", "child query, parentID to query: " + commentID);


                            cNode.setNestedLevel(0);

                            if (prevNode != null) {
                                prevNode.setTailSibling(cNode);
                                cNode.setHeadSibling(prevNode);
                            }

                            nodeMap.put(commentID, cNode);
                            prevNode = cNode;
                        }
                        getChildComments(rootComments, childComments);
                    }

                    if (!medalUpgradeMap.isEmpty()) {
                        //set update duty
                        //if no update duty then set update duty true if user authored one of the comments
                        //update DB is update duty true.
                        //medalsUpdateDB(medalUpgradeMap, medalWinners);
                    }

                    if (!childComments.isEmpty()) {
                        //set up nodeMap with child comments
                        for (int i = 0; i < childComments.size(); i++) {
                            if (thisThreadID != queryThreadID) {
                                Log.d("wow", "broke out of old query thread");
                                return;
                            }

                            VSCNode cNode = new VSCNode(childComments.get(i));
                            final String commentID = cNode.getCommentID();

                            cNode.setNestedLevel(1);
                            VSCNode parentNode = nodeMap.get(cNode.getParentID());
                            if (parentNode != null) {
                                if (!parentNode.hasChild()) {
                                    parentNode.setFirstChild(cNode);
                                    cNode.setParent(parentNode);
                                } else {
                                    VSCNode sibling = parentNode.getFirstChild();
                                    if (sibling.getUpvotes() == cNode.getUpvotes() && sibling.getVotecount() < cNode.getVotecount()) {
                                        sibling.setParent(null);
                                        cNode.setParent(parentNode);
                                        parentNode.setFirstChild(cNode);
                                        cNode.setTailSibling(sibling);
                                        sibling.setHeadSibling(cNode);
                                    } else {
                                        sibling.setTailSibling(cNode);
                                        cNode.setHeadSibling(sibling);
                                    }
                                }
                            }

                            nodeMap.put(commentID, cNode);
                        }

                        getChildComments(childComments, grandchildComments);
                    }

                    //set up nodeMap with grandchild comments
                    if (!grandchildComments.isEmpty()) {
                        //TODO: run chunkSorter on grandchildComments here

                        for (int i = 0; i < grandchildComments.size(); i++) {

                            if (thisThreadID != queryThreadID) {
                                Log.d("wow", "broke out of old query thread");
                                return;
                            }

                            VSCNode cNode = new VSCNode(grandchildComments.get(i));
                            cNode.setNestedLevel(2);

                            VSCNode parentNode = nodeMap.get(cNode.getParentID());
                            if (parentNode != null) {
                                if (!parentNode.hasChild()) {
                                    parentNode.setFirstChild(cNode);
                                    cNode.setParent(parentNode);
                                } else {
                                    VSCNode sibling = parentNode.getFirstChild();
                                    if (sibling.getUpvotes() == cNode.getUpvotes() && sibling.getVotecount() < cNode.getVotecount()) {
                                        sibling.setParent(null);
                                        cNode.setParent(parentNode);
                                        parentNode.setFirstChild(cNode);
                                        cNode.setTailSibling(sibling);
                                        sibling.setHeadSibling(cNode);
                                    } else {
                                        sibling.setTailSibling(cNode);
                                        cNode.setHeadSibling(sibling);
                                    }
                                }
                            }

                            nodeMap.put(cNode.getCommentID(), cNode);
                        }

                    }


                    //set up comments list
                    VSCNode temp;
                    for (int i = 0; i < rootComments.size(); i++) {
                        if (thisThreadID != queryThreadID) {
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }

                        temp = nodeMap.get(rootComments.get(i).getComment_id());
                        if (temp != null) {
                            setCommentList(temp, masterList);
                        }
                    }

                    //run UI updates on UI Thread
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            applyUserActions(masterList);

                            //Make sure to do this after applyUserActions because applyUserActions doesn't expect post object in the list
                            if (rootParentID.equals(postID)) {
                                //vsComments.add(0, post);
                                masterList.add(0, post);
                            }
                            else{
                                if(topCardContent != null){
                                    masterList.add(0, new TopCardObject(topCardContent));
                                }
                            }

                            //find view by id and attaching adapter for the RecyclerView
                            //RV.setLayoutManager(new LinearLayoutManager(activity));

                            //if true then we got the root comments whose parentID is postID ("rootest roots"), so include the post card for the PostPage view
                            //this if condition also determines the boolean parameter at the end of PostPageAdapter constructor to notify adapter if it should set up Post Card
                            if (rootParentID.equals(postID)) {
                                atRootLevel = true;
                                PPAdapter = new PostPageAdapter(masterList, post, activity, pageLevel, thisFragment);
                            } else {
                                atRootLevel = false;
                                PPAdapter = new PostPageAdapter(masterList, post, activity, pageLevel, thisFragment);
                            }

                            RV.setAdapter(PPAdapter);
                            activity.setPostInDownload(postID, "done");
                            mSwipeRefreshLayout.setRefreshing(false);

                            RV.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                @Override
                                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                    LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                                    int lastVisible = layoutManager.findLastVisibleItemPosition() - childrenCount;

                                    boolean endHasBeenReached = lastVisible + loadThreshold >= currCommentsIndex;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                                    if (currCommentsIndex > 0 && endHasBeenReached) {
                                        //you have reached to the bottom of your recycler view
                                        if (!nowLoading) {
                                            nowLoading = true;
                                            Log.d("Load", "Now Loadin More");
                                            loadMoreComments(uORt);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            };
            Thread mythread = new Thread(runnable);
            queryThreadID = mythread.getId();
            mythread.start();
        }
        else{
            final ArrayList<VSComment> grootComments = new ArrayList<>(); //grandchildren page's root comments, so grandchildren comments

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });

            Runnable runnable = new Runnable() {
                public void run() {
                    long thisThreadID = Thread.currentThread().getId();
                    final List<Object> masterList = new ArrayList<>();

                    getRootComments(0, grootComments, rootParentID, uORt);

                    if(!grootComments.isEmpty()){
                        VSCNode prevNode = null;
                        VSComment currComment;

                        //set up nodeMap with root comments
                        for(int i = 0; i < grootComments.size(); i++){
                            if(thisThreadID != queryThreadID){
                                Log.d("wow", "broke out of old query thread");
                                return;
                            }
                            currComment = grootComments.get(i);
                            VSCNode cNode = new VSCNode(currComment);
                            final String commentID = currComment.getComment_id();

                            Log.d("wow", "child query, parentID to query: " + commentID);

                            cNode.setNestedLevel(0);

                            if(prevNode != null){
                                prevNode.setTailSibling(cNode);
                                cNode.setHeadSibling(prevNode);
                            }

                            nodeMap.put(commentID, cNode);
                            prevNode = cNode;
                        }
                    }

                    //set up comments list
                    VSCNode temp;
                    for(int i = 0; i<grootComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }

                        temp = nodeMap.get(grootComments.get(i).getComment_id());
                        if(temp != null){
                            setCommentList(temp, masterList);
                        }
                    }

                    //run UI updates on UI Thread
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            applyUserActions(masterList);

                            if(topCardContent != null){
                                masterList.add(0, new TopCardObject(topCardContent));
                            }

                            //find view by id and attaching adapter for the RecyclerView
                            //RV.setLayoutManager(new LinearLayoutManager(activity));

                            //if true then we got the root comments whose parentID is postID ("rootest roots"), so include the post card for the PostPage view
                            //this if condition also determines the boolean parameter at the end of PostPageAdapter constructor to notify adapter if it should set up Post Card
                            atRootLevel = false;
                            PPAdapter = new PostPageAdapter(masterList, post, activity, pageLevel, thisFragment);

                            RV.setAdapter(PPAdapter);
                            mSwipeRefreshLayout.setRefreshing(false);

                            RV.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                @Override
                                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                    LinearLayoutManager layoutManager=LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                                    int lastVisible = layoutManager.findLastVisibleItemPosition() - childrenCount;

                                    boolean endHasBeenReached = lastVisible + loadThreshold >= currCommentsIndex;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                                    if (currCommentsIndex > 0 && endHasBeenReached) {
                                        //you have reached to the bottom of your recycler view
                                        if(!nowLoading){
                                            nowLoading = true;
                                            Log.d("Load", "Now Loadin More");
                                            loadMoreComments(uORt);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            };
            Thread mythread = new Thread(runnable);
            queryThreadID = mythread.getId();
            mythread.start();

        }


    }

    public void setContent(final Post post){  //downloadImages signifies initial post page set up
        pageCommentInput.setHint("Join the discussion!");
        this.post = post;
        postID = post.getPost_id();

        if(freshlyVotedComments != null && !(freshlyVotedCommentsListPostID.equals(postID))){ //only clearing if this case is true prevents upvotes/downvotes counter error if user votes on a comment, immediately exits and then re-enters the post
            freshlyVotedComments.clear();
        }

        sortType = POPULAR;
        nowLoading = false;
        pageLevel = 0;

        postTopic = post.getQuestion();
        postX = post.getRedname();
        postY = post.getBlackname();
        origRedCount = post.getRedcount();
        origBlackCount = post.getBlackcount();
        redIncrementedLast = false;
        blackIncrementedLast = false;

        parentCache.clear();

        if(RV != null && RV.getAdapter() != null){
            ((PostPageAdapter)(RV.getAdapter())).clearList();
        }

        initialLoadInProgress = true;
        if(xmlLoaded){
            mSwipeRefreshLayout.setRefreshing(true);
        }

        Runnable runnable = new Runnable() {
            public void run() {
                if(userAction == null || !userAction.getPostID().equals(postID)){
                    userAction = activity.getMapper().load(UserAction.class, sessionManager.getCurrentUsername(), postID);   //TODO: catch exception for this query
                    //Log.d("DB", "download attempt for UserAction");
                }

                if(userAction == null){
                    userAction = new UserAction(sessionManager.getCurrentUsername(), postID);
                }
                lastSubmittedVote = userAction.getVotedSide();
                actionMap = userAction.getActionRecord();
                deepCopyToActionHistoryMap();

                commentsQuery(postID, "u");
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }

    public void setCommentList(VSCNode rootNode, List<Object> mList){
        VSCNode tempChildNode, tempGCNode;
        if(rootNode != null){
            mList.add(rootNode.getNodeContent()); //root node
            if(rootNode.hasChild()){    //first child
                tempChildNode = rootNode.getFirstChild();
                mList.add(tempChildNode.getNodeContent());

                if(tempChildNode.hasChild()){   //first child's first child
                    tempGCNode = tempChildNode.getFirstChild();
                    mList.add(tempGCNode.getNodeContent());

                    if(tempGCNode.hasTailSibling()){    //first child's second child
                        mList.add((tempGCNode.getTailSibling()).getNodeContent());
                    }
                }

                if(tempChildNode.hasTailSibling()){ //second child
                    tempChildNode = tempChildNode.getTailSibling();
                    mList.add(tempChildNode.getNodeContent());

                    if(tempChildNode.hasChild()){   //second child's first child
                        tempGCNode = tempChildNode.getFirstChild();
                        mList.add(tempGCNode.getNodeContent());

                        if(tempGCNode.hasTailSibling()){    //second child's second child
                            mList.add((tempGCNode.getTailSibling()).getNodeContent());
                        }
                    }
                }
            }
        }
    }

    public PostPageAdapter getPPAdapter(){
        return PPAdapter;
    }

    //now this is more of a legacy function to keep other functions working,
    // the real TopCard setup happens in query functions where we add a TopCard Object into the masterList that goes into PostPageAdapter
    public void setUpTopCard(VSComment clickedComment){

        topCardContent = clickedComment;
        atRootLevel = false;

        setCommentCardSortTypeHint();
    }

    public void hideTopCard(){
        topCardContent = null;
        atRootLevel = true;
    }

    //used when expanding into nested levels, so when pageNestedLevel > 0
    public void setCommentsPage(VSComment subjectComment){
        pageCommentInput.setHint("Enter a reply!");

        if(PPAdapter != null && PPAdapter.getPostID().equals(subjectComment.getPost_id())) {
            List<Object> masterList = PPAdapter.getMasterList();

            if(!masterList.isEmpty()){
                List<Object> stackEntry = new ArrayList<>();
                stackEntry.addAll(masterList);

                masterListStack.push(stackEntry);

                scrollPositionStack.push(((LinearLayoutManager) RV.getLayoutManager()).findFirstVisibleItemPosition());

                if(stackEntry.get(0) instanceof Post && subjectComment.getNestedLevel() > 0){ //pageLevel is already incremented at this point so we check for pageLevel == 1
                    scrollPositionStack.push(-1);
                }
            }
        }


        mSwipeRefreshLayout.setRefreshing(true);
        if(pageLevel != 2){
            sortType = POPULAR;
        }
        else{
            sortType = CHRONOLOGICAL;
        }
        if(RV != null && RV.getAdapter() != null){
            ((PostPageAdapter)(RV.getAdapter())).clearList();
        }
        setUpTopCard(subjectComment);

        if(pageLevel != 2) {
            commentsQuery(subjectComment.getComment_id(), "u");
        }
        else{
            commentsQuery(subjectComment.getComment_id(), "c");
        }

    }

    //only called when we're not on pageNestedLevel 0, so when ascending up nested levels towards root level. so not called in root level (in root level we simply exit out of PostPage on UpButton click)
    public void backToParentPage(){
        pageLevel--;

        mSwipeRefreshLayout.setRefreshing(true);

        if(PPAdapter != null){
            PPAdapter.clearList();
        }

        sortType = POPULAR;

        String tempParentID = topCardContent.getParent_id();

        if(tempParentID.equals(postID)){
            pageCommentInput.setHint("Join the discussion!");
            hideTopCard();
        }
        else{
            setUpTopCard(parentCache.get(tempParentID));
        }

        if(!scrollPositionStack.isEmpty()){
            int scrollPosition = scrollPositionStack.pop();
            if(scrollPosition >= 0){
                PPAdapter = new PostPageAdapter(masterListStack.pop(), post, activity, pageLevel, thisFragment);
                RV.setAdapter(PPAdapter);
                mSwipeRefreshLayout.setRefreshing(false);
                RV.getLayoutManager().scrollToPosition(scrollPosition);
                return;
            }
        }

        commentsQuery(tempParentID, "u");
    }

    public boolean isRootLevel(){
        return atRootLevel;
    }

    //only call this after nodeTable and vsComments have been set up, but before passing vsComments into a PPAdapter instance
    //sets up VSComment.uservote for comments that user upvoted or downvoted
    //this is only used after downloading for initial uservote setup
    public void applyUserActions(List<Object> commentsList){
        VSCNode temp;
        if(!actionMap.isEmpty()){
            for(int i = 0; i<commentsList.size(); i++){
                temp = nodeMap.get(((VSComment)commentsList.get(i)).getComment_id());

                if(temp == null) {
                    Log.d("DEBUG", "This is unexpected, this is a bug. PostPage.java line 801");
                    return;
                }

                String currentValue = actionMap.get(temp.getCommentID());
                String historyValue = actionHistoryMap.get(temp.getCommentID());
                if(currentValue != null){
                    switch(currentValue){
                        //we record and handle "N" case which is when user initially voted and then clicked it again to cancel the vote,
                        //keep the N until we decrement past vote
                        // and then it is removed from actionmap after the decrement for DB is performed
                        case "U":
                            if(historyValue == null){
                                temp.updateUservote(UPVOTE);
                                Log.d("history", "history value is always null");
                            }
                            else{
                                if(historyValue.equals("U")){
                                    temp.getNodeContent().initialSetUservote(UPVOTE);
                                }
                                else{
                                    temp.updateUservoteAndDecrement(UPVOTE);
                                }
                            }
                            break;
                        case "D":
                            if(historyValue == null){
                                temp.updateUservote(DOWNVOTE);
                            }
                            else{
                                if(historyValue.equals("D")){
                                    temp.getNodeContent().initialSetUservote(DOWNVOTE);
                                }
                                else{
                                    temp.updateUservoteAndDecrement(DOWNVOTE);
                                }
                            }
                            break;
                        case "N":   //"N" doesn't get written to DB because we remove it before uploading actionMap through UserAction object
                            //so this is going to be a current session action that needs to be manually applied
                            if(historyValue != null){
                                if(historyValue.equals("U")){
                                    temp.getNodeContent().decrementAndSetN(UPVOTE);
                                }
                                else {  //since DB doesn't store "N", we can safely assume this is "D"
                                    temp.getNodeContent().decrementAndSetN(DOWNVOTE);
                                }
                            }
                            break;
                    }
                }
            }
        }

        //overrides downloaded values of upvotes and downvotes for comments that were recently voted on, to prevent bugs related to discrepancy between local and database values
        for (HashMap.Entry<String, Pair<Integer, Integer>> entry : freshlyVotedComments.entrySet()) {
            String commentID = entry.getKey();
            Pair<Integer, Integer> votes = entry.getValue();
            VSCNode currentNode = nodeMap.get(commentID);
            if(currentNode != null) {
                currentNode.setUpvotesAndDownvotes(votes);
            }
        }
        freshlyVotedComments.clear();
    }

    //TODO:this is where we do user action synchronization I believe it would be called
    //Try to use this function for all action synchronization updates, because we do some stuff to keep track of synchronization
    public void writeActionsToDB(){
        if(userAction != null) {
            Runnable runnable = new Runnable() {
                public void run() {
                    List<String> markedForRemoval = new ArrayList<>();


                    String actionHistoryEntryValue;
                    VSCNode tempNode;
                    boolean updateForNRemoval = false;

                    for (Map.Entry<String, String> entry : actionMap.entrySet()) {
                        actionHistoryEntryValue = actionHistoryMap.get(entry.getKey());

                        if (actionHistoryEntryValue == null) { //first record of user action on this comment, or if vote record's been cleared (like cancel a downvote)

                            tempNode = nodeMap.get(entry.getKey());
                            String author = tempNode.getNodeContent().getAuthor();

                            //just increment
                            HashMap<String, AttributeValue> keyMap =
                                    new HashMap<>();
                            keyMap.put("i", new AttributeValue().withS(entry.getKey()));

                            HashMap<String, AttributeValueUpdate> updates =
                                    new HashMap<>();

                            AttributeValueUpdate avu;
                            switch (entry.getValue()) {
                                case "U":
                                    Log.d("DB update", "upvote increment");
                                    avu = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("u", avu);

                                    UpdateItemRequest request = new UpdateItemRequest()
                                            .withTableName("vscomment")
                                            .withKey(keyMap)
                                            .withAttributeUpdates(updates);

                                    activity.getDDBClient().updateItem(request);

                                    //update activityHistoryMap
                                    actionHistoryMap.put(entry.getKey(), entry.getValue());

                                    if(!(author.equals("[deleted]"))){
                                        //increment influence points on Firebase
                                        mFirebaseDatabaseReference.child(getUsernameHash(author)+"/"+author+"/p").runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                Integer value = mutableData.getValue(Integer.class);
                                                if (value == null) {
                                                    mutableData.setValue(1);
                                                }
                                                else {
                                                    mutableData.setValue(value + 1);
                                                }

                                                return Transaction.success(mutableData);
                                            }

                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b,
                                                                   DataSnapshot dataSnapshot) {
                                                Log.d("Firebase", "transaction:onComplete:" + databaseError);
                                            }
                                        });

                                        sendCommentUpvoteNotification(tempNode.getNodeContent());
                                    }

                                    break;

                                case "D":
                                    Log.d("DB update", "downvote increment");
                                    avu = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("d", avu);

                                    request = new UpdateItemRequest()
                                            .withTableName("vscomment")
                                            .withKey(keyMap)
                                            .withAttributeUpdates(updates);

                                    activity.getDDBClient().updateItem(request);

                                    boolean incrementInfluence = (tempNode.getUpvotes() == 0 && tempNode.getDownvotes() + 1 <= 10) || (tempNode.getUpvotes() * 10 >= tempNode.getDownvotes() + 1);
                                    
                                    if(incrementInfluence && !(author.equals("[deleted]"))){ //as long as downvotes are less than 10*upvotes, we increase user's influence

                                        //increment influence points on Firebase
                                        mFirebaseDatabaseReference.child(getUsernameHash(author)+"/"+author+"/p").runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                Integer value = mutableData.getValue(Integer.class);
                                                if (value == null) {
                                                    mutableData.setValue(1);
                                                }
                                                else {
                                                    mutableData.setValue(value + 1);
                                                }

                                                return Transaction.success(mutableData);
                                            }

                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b,
                                                                   DataSnapshot dataSnapshot) {
                                                Log.d("Firebase", "transaction:onComplete:" + databaseError);
                                            }
                                        });

                                    }

                                    //update activityHistoryMap
                                    actionHistoryMap.put(entry.getKey(), entry.getValue());
                                    break;

                                default:    //this happens when we have a new UesrAction (none found in DB) locally and we cancel a vote and record "N". We don't need to do DB operation in this case.
                                    markedForRemoval.add(entry.getKey());
                                    break;
                            }

                        }

                        else if (!actionHistoryEntryValue.equals(entry.getValue())) {   //modifying existing record of user action on this comment

                            //increment current and decrement past
                            HashMap<String, AttributeValue> keyMap =
                                    new HashMap<>();
                            tempNode = nodeMap.get(entry.getKey());
                            keyMap.put("i", new AttributeValue().withS(entry.getKey()));

                            HashMap<String, AttributeValueUpdate> updates =
                                    new HashMap<>();
                            AttributeValueUpdate avu, avd;
                            UpdateItemRequest request;

                            switch (entry.getValue()) {

                                case "U":
                                    avu = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("u", avu);
                                    avd = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("-1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("d", avd);
                                    request = new UpdateItemRequest()
                                            .withTableName("vscomment")
                                            .withKey(keyMap)
                                            .withAttributeUpdates(updates);

                                    activity.getDDBClient().updateItem(request);
                                    //update activityHistoryMap
                                    actionHistoryMap.put(entry.getKey(), entry.getValue());

                                    sendCommentUpvoteNotification(tempNode.getNodeContent());
                                    break;

                                case "D":
                                    avu = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("d", avu);
                                    avd = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("-1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("u", avd);
                                    request = new UpdateItemRequest()
                                            .withTableName("vscomment")
                                            .withKey(keyMap)
                                            .withAttributeUpdates(updates);

                                    activity.getDDBClient().updateItem(request);
                                    //update activityHistoryMap
                                    actionHistoryMap.put(entry.getKey(), entry.getValue());

                                    break;

                                case "N":

                                    if (actionHistoryMap.get(entry.getKey()).equals("U")) {
                                        Log.d("DB update", "upvote decrement");
                                        avd = new AttributeValueUpdate()
                                                .withValue(new AttributeValue().withN("-1"))
                                                .withAction(AttributeAction.ADD);
                                        updates.put("u", avd);
                                    } else {   //it's either "U" or "D", because if it was "N" (the only other option) then it wouldn't arrive to this switch case since history and current are equal
                                        Log.d("DB update", "downvote decrement");
                                        avd = new AttributeValueUpdate()
                                                .withValue(new AttributeValue().withN("-1"))
                                                .withAction(AttributeAction.ADD);
                                        updates.put("d", avd);
                                    }

                                    request = new UpdateItemRequest()
                                            .withTableName("vscomment")
                                            .withKey(keyMap)
                                            .withAttributeUpdates(updates);

                                    activity.getDDBClient().updateItem(request);

                                    actionHistoryMap.remove(entry.getKey());
                                    updateForNRemoval = true;
                                    markedForRemoval.add(entry.getKey()); //mark this comment's entry in actionMap for removal. we mark it and do it after this for-loop to avoid ConcurrentModificationException
                                    //we remove the "N" record because it only serves as marker for decrement, now decrement is executed so we're removing the marker, resetting record on that comment

                                    //decrement influence points on Firebase to prevent duplicate point increment to same comment from same user
                                    String author = tempNode.getNodeContent().getAuthor();
                                    if(!author.equals("[deleted]")){
                                        mFirebaseDatabaseReference.child(getUsernameHash(author)+"/"+author+"/p").runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                Integer value = mutableData.getValue(Integer.class);
                                                if (value == null || value <= 0) {
                                                    mutableData.setValue(0);
                                                }
                                                else {
                                                    mutableData.setValue(value - 1);
                                                }

                                                return Transaction.success(mutableData);
                                            }

                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b,
                                                                   DataSnapshot dataSnapshot) {
                                                Log.d("Firebase", "transaction:onComplete:" + databaseError);
                                            }
                                        });
                                    }

                                    break;

                                default:
                                    break;
                            }
                        }
                    }

                    if (!lastSubmittedVote.equals(userAction.getVotedSide())) {
                        String redOrBlack;
                        boolean decrement = false;
                        if (redIncrementedLast) {
                            redOrBlack = "rc";
                            if (lastSubmittedVote.equals("BLK")) {
                                decrement = true;
                            }
                        } else {
                            redOrBlack = "bc";
                            if (lastSubmittedVote.equals("RED")) {
                                decrement = true;
                            }
                        }

                        HashMap<String, AttributeValue> keyMap =
                                new HashMap<>();
                        keyMap.put("i", new AttributeValue().withS(postID));

                        HashMap<String, AttributeValueUpdate> updates =
                                new HashMap<>();

                        AttributeValueUpdate avu = new AttributeValueUpdate()
                                .withValue(new AttributeValue().withN("1"))
                                .withAction(AttributeAction.ADD);
                        updates.put(redOrBlack, avu);

                        if (decrement) {
                            String voteToDecrement = "rc";
                            if (redOrBlack.equals("rc")) {
                                voteToDecrement = "bc";
                            }
                            AttributeValueUpdate avd = new AttributeValueUpdate()
                                    .withValue(new AttributeValue().withN("-1"))
                                    .withAction(AttributeAction.ADD);
                            updates.put(voteToDecrement, avd);
                        }

                        else {
                            //update pt and increment ps
                            int currPt = (int)((System.currentTimeMillis()/1000)/60);
                            AttributeValueUpdate ptu = new AttributeValueUpdate()
                                    .withValue(new AttributeValue().withN(Integer.toString(currPt)))
                                    .withAction(AttributeAction.PUT);
                            updates.put("pt", ptu);

                            int timeDiff = currPt - post.getPt();
                            Log.d("timeDiff", "timeDiff = "+Integer.toString(timeDiff));
                            if(timeDiff <= 0){ //if timeDiff is negative due to some bug or if timeDiff is zero, we just make it equal 1.
                                timeDiff = 1;
                            }

                            double psIncrement = votePSI/timeDiff;
                            AttributeValueUpdate psu = new AttributeValueUpdate()
                                    .withValue(new AttributeValue().withN(Double.toString(psIncrement)))
                                    .withAction(AttributeAction.ADD);
                            updates.put("ps", psu);

                            sendPostVoteNotification();

                            activity.updateTargetVotecount();
                        }


                        UpdateItemRequest request = new UpdateItemRequest()
                                .withTableName("post")
                                .withKey(keyMap)
                                .withAttributeUpdates(updates);

                        activity.getDDBClient().updateItem(request);

                        //update lastSubmittedVote
                        lastSubmittedVote = userAction.getVotedSide();

                    }

                    //clean up stray "N" marks
                    for (String key : markedForRemoval) {
                        Log.d("DB Update", key + "removed from actionMap");
                        actionMap.remove(key);
                    }

                    if (!actionMap.isEmpty() || redIncrementedLast != blackIncrementedLast || updateForNRemoval) { //user made comment action(s) OR voted for a side in the post
                        activity.getMapper().save(userAction, new DynamoDBMapperConfig(DynamoDBMapperConfig.SaveBehavior.CLOBBER));
                        //TODO: the below line to deep copy action history is currently commented out because this code currently only executes on PostPage exit. However, once we need to write UserAction to DB while user is still inside PostPage, then we should uncomment the line below to keep actionHistoryMap up to date with current version of actionMap in the DB that has just been updated by the line above.
                        //if(exiting post page)
                        // deepCopyToActionHistoryMap(actionMap);

                    }

                    dbWriteComplete = true;
                }
            };

            Thread mythread = new Thread(runnable);
            mythread.start();
        }
    }

    public void redVotePressed(){
        if(userAction.getVotedSide().equals("BLK")){
            post.decrementBlackCount();
        }
        post.incrementRedCount();
        userAction.setVotedSide("RED");
        redIncrementedLast = true;
        blackIncrementedLast = false;
    }

    public void blackVotePressed(){
        if(userAction.getVotedSide().equals("RED")){
            post.decrementRedCount();
        }
        post.incrementBlackCount();
        userAction.setVotedSide("BLK");
        blackIncrementedLast = true;
        redIncrementedLast = false;
    }

    public UserAction getUserAction(){
        return userAction;
    }

    public String getPostPagePostID(){
        return postID;
    }

    private void deepCopyToActionHistoryMap(){
        actionHistoryMap = new HashMap<>();
        for(Map.Entry<String, String> entry : actionMap.entrySet()){
            if(entry.getValue().equals("N")){
                Log.d("DB update", "N put into actionMap");
            }
            actionHistoryMap.put(entry.getKey(), entry.getValue());
        }
    }

    public String getCatNumString(){
        return post.getCategoryIntAsString();
    }

    public void yesExitLoop(){
        exitLoop = true;
    }

    //String grandchildsParentsID is the parentID of the clicked grandchild comment,
    // hence the parentID of grandchildsPaarent would be commentID of the grandchild we're adding to the parentCache
    public void addGrandParentToCache(String grandchildParentID){
        //temp is the grandparent node of the clicked grandchild comment
        VSCNode temp = nodeMap.get( nodeMap.get(grandchildParentID).getParentID() );
        parentCache.put(temp.getCommentID(), temp.getNodeContent());
        Log.d("grandparentcache", "content: " + temp.getNodeContent().getContent() + "\ngrandparentCacheSize: " + Integer.toString(parentCache.size()));

    }

    public void addParentToCache(String parentID){
        if(!parentID.equals(postID)){
            VSCNode temp = nodeMap.get(parentID);
            parentCache.put(temp.getCommentID(), temp.getNodeContent());
            Log.d("parentcache", "content: " + temp.getNodeContent().getContent() + "\nparentCacheSize: " + Integer.toString(parentCache.size()));
        }

    }

    public void addThisToCache(String commentID){
        VSCNode temp = nodeMap.get(commentID);
        parentCache.put(temp.getCommentID(), temp.getNodeContent());
    }

    //called with query results before nodeMap setup
    private void chunkSorter(List<VSComment> commentsList){

        if(commentsList.size() > 1){
            ArrayList<ArrayList<VSComment>> chunksList = new ArrayList<>();   //chunks are added in upvote order since nodes are already sorted by upvotes from the query

            int chunkListIndex = 0;
            int currentChunkUpvotes = commentsList.get(0).getUpvotes();

            ArrayList<VSComment> firstChunkEntry = new ArrayList<>();
            firstChunkEntry.add(commentsList.get(0));
            chunksList.add(firstChunkEntry);

            for(int i = 1; i<commentsList.size(); i++){
                VSComment commentItem = commentsList.get(i);
                if(commentItem.getUpvotes() == currentChunkUpvotes){
                    chunksList.get(chunkListIndex).add(commentItem);
                }
                else {
                    chunkListIndex++;
                    currentChunkUpvotes = commentItem.getUpvotes();
                    ArrayList<VSComment> freshChunkEntry = new ArrayList<>();
                    freshChunkEntry.add(commentItem);
                    chunksList.add(freshChunkEntry);
                }
            }

            //go through each chunk and sort as needed, and if we sort, then write it over the corresponding section of commentsList
            int chunkInsertionIndex = 0;

            for(ArrayList<VSComment> chunk : chunksList){
                if(chunk.size() == 1){   //a chunk with only single item means no sorting occured in it, and chunks have minimum size of 1.
                    chunkInsertionIndex += 1;
                }
                else {

                    boolean chunkSorted = false;
                    VSComment chunkItem;
                    for(int right = 1; right< chunk.size(); right++){ //iterate over this chunk
                        chunkItem = chunk.get(right);
                        int insertionIndex = right;
                        for(int left = right-1; left >= 0; left--){
                            if(chunkItem.heartsTotal() > chunk.get(left).heartsTotal()){
                                insertionIndex = left;
                            }
                        }

                        if(insertionIndex < right){
                            VSComment temp = chunkItem;
                            chunk.set(right, chunk.get(insertionIndex));
                            chunk.set(insertionIndex, temp);
                            chunkSorted = true;
                        }
                    }

                    if(chunkSorted){
                        for(int chunkIndex = 0; chunkIndex<chunk.size(); chunkIndex++){
                            commentsList.set(chunkInsertionIndex, chunk.get(chunkIndex));
                            chunkInsertionIndex++;
                        }
                    }
                    else {
                        chunkInsertionIndex += chunk.size();
                    }

                }

            }
        }

    }

    public void selectSortType(final String pORc){
        final String [] items = new String[] {"Most Recent", "Popular", "Chronological"};
        final Integer[] icons = new Integer[] {R.drawable.ic_new_releases, R.drawable.ic_thumb_up, R.drawable.ic_chrono}; //TODO: change these icons to actual ones
        ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(), items, icons);
        new AlertDialog.Builder(getActivity()).setTitle("Sort by")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item ) {
                        switch(item){

                            case MOST_RECENT: //Sort by Most Recent
                                sortType = MOST_RECENT;
                                mSwipeRefreshLayout.setRefreshing(true);
                                refreshCommentTimestampQuery();
                                break;

                            case POPULAR: //Sort by Popular
                                sortType = POPULAR;
                                mSwipeRefreshLayout.setRefreshing(true);
                                refreshCommentUpvotesQuery();
                                break;

                            case 2: //Sort by Chronological
                                sortType = CHRONOLOGICAL;
                                mSwipeRefreshLayout.setRefreshing(true);
                                refreshCommentChronologicalQuery();
                                break;
                        }
                        if(pORc.equals("p")){
                            setPostCardSortTypeHint();
                        }
                        else{
                            setCommentCardSortTypeHint();
                        }
                    }
                }).show();

    }

    public int getSortType(){
        return sortType;
    }

    private void setPostCardSortTypeHint(){
        if(PPAdapter != null){
            //PPAdapter.setSortTypeHint(sortType);
        }
    }

    private void setCommentCardSortTypeHint(){
        if(PPAdapter != null){
            PPAdapter.setTopCardSortTypeHint(sortType);
        }
        /*
        switch (sortType){
            case MOST_RECENT:
                topcardSortTypeSelector.setText("MOST RECENT");
                topcardSortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                break;

            case POPULAR:
                topcardSortTypeSelector.setText("POPULAR");
                topcardSortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                break;

            case CHRONOLOGICAL:
                topcardSortTypeSelector.setText("CHRONOLOGICAL");
                topcardSortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_chrono_20small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                break;

        }
        */
    }


    private void loadMoreComments(final String uORt){
        if(pageLevel == 2){
            loadMoreGComments(uORt); //little faster, for grandchildren page where there are only root comments
            return;
        }

        Runnable runnable = new Runnable() {
            public void run() {
                try{

                    long thisThreadID = Thread.currentThread().getId();

                    final List<Object> masterList = new ArrayList<>();

                    final ArrayList<VSComment> rootComments = new ArrayList<>();
                    final ArrayList<VSComment> childComments = new ArrayList<>();
                    final ArrayList<VSComment> grandchildComments = new ArrayList<>();

                    String queryParentID;
                    if(atRootLevel){
                        queryParentID = postID;
                    }
                    else{
                        queryParentID = topCardContent.getComment_id();
                    }

                    getRootComments(currCommentsIndex, rootComments, queryParentID, uORt);

                    chunkSorter(rootComments);

                    VSCNode prevNode = null;
                    exitLoop = false;
                    if(!rootComments.isEmpty()){
                        //set up nodeMap with root comments
                        for(int i = 0; i < rootComments.size(); i++){
                            if(thisThreadID != queryThreadID){
                                Log.d("wow", "broke out of old query thread");
                                nowLoading = false;
                                return;
                            }
                            VSCNode cNode = new VSCNode(rootComments.get(i));
                            final String commentID = cNode.getCommentID();

                            Log.d("wow", "child query, parentID to query: " + commentID);


                            cNode.setNestedLevel(0);

                            if(prevNode != null){
                                prevNode.setTailSibling(cNode);
                                cNode.setHeadSibling(prevNode);
                            }

                            nodeMap.put(commentID, cNode);
                            prevNode = cNode;

                            getChildComments(rootComments, childComments);
                        }

                    }
                    else{
                        nowLoading = true; //no more loading until refresh
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                    Log.d("wow", "child query result size at the end: " + childComments.size());
                    exitLoop = false;
                    if(!childComments.isEmpty()){

                        //set up nodeMap with child comments
                        for(int i = 0; i < childComments.size(); i++){
                            if(thisThreadID != queryThreadID){
                                Log.d("wow", "broke out of old query thread");
                                nowLoading = false;
                                return;
                            }

                            VSCNode cNode = new VSCNode(childComments.get(i));
                            final String commentID = cNode.getCommentID();

                            cNode.setNestedLevel(1);
                            VSCNode parentNode = nodeMap.get(cNode.getParentID());
                            if(parentNode != null) {
                                if(!parentNode.hasChild()){
                                    parentNode.setFirstChild(cNode);
                                    cNode.setParent(parentNode);
                                }
                                else{
                                    VSCNode sibling = parentNode.getFirstChild();
                                    if(sibling.getUpvotes() == cNode.getUpvotes() && sibling.getVotecount() < cNode.getVotecount()){
                                        sibling.setParent(null);
                                        cNode.setParent(parentNode);
                                        parentNode.setFirstChild(cNode);
                                        cNode.setTailSibling(sibling);
                                        sibling.setHeadSibling(cNode);
                                    }
                                    else{
                                        sibling.setTailSibling(cNode);
                                        cNode.setHeadSibling(sibling);
                                    }
                                }
                            }

                            nodeMap.put(commentID, cNode);

                            getChildComments(childComments, grandchildComments);
                        }
                    }

                    //set up nodeMap with grandchild comments
                    if(!grandchildComments.isEmpty()){
                        //TODO: run chunkSorter on grandchildComments here

                        for (int i = 0; i < grandchildComments.size(); i++){

                            if(thisThreadID != queryThreadID){
                                Log.d("wow", "broke out of old query thread");
                                nowLoading = false;
                                return;
                            }

                            VSCNode cNode = new VSCNode(grandchildComments.get(i));
                            cNode.setNestedLevel(2);

                            VSCNode parentNode = nodeMap.get(cNode.getParentID());
                            if(parentNode != null) {
                                if(!parentNode.hasChild()){
                                    parentNode.setFirstChild(cNode);
                                    cNode.setParent(parentNode);
                                }
                                else{
                                    VSCNode sibling = parentNode.getFirstChild();
                                    if(sibling.getUpvotes() == cNode.getUpvotes() && sibling.getVotecount() < cNode.getVotecount()){
                                        sibling.setParent(null);
                                        cNode.setParent(parentNode);
                                        parentNode.setFirstChild(cNode);
                                        cNode.setTailSibling(sibling);
                                        sibling.setHeadSibling(cNode);
                                    }
                                    else{
                                        sibling.setTailSibling(cNode);
                                        cNode.setHeadSibling(sibling);
                                    }
                                }
                            }

                            nodeMap.put(cNode.getCommentID(), cNode);
                        }

                    }


                    //set up comments list
                    VSCNode temp;
                    for(int i = 0; i<rootComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            nowLoading = false;
                            return;
                        }

                        temp = nodeMap.get(rootComments.get(i).getComment_id());
                        if(temp != null){
                            setCommentList(temp, masterList);
                        }
                    }

                    if(!rootComments.isEmpty()){
                        nowLoading = false;
                    }
                    else {
                        nowLoading = true;  //to stop triggering loadMore
                    }

                    //run UI updates on UI Thread
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            applyUserActions(masterList);

                            ((PostPageAdapter)RV.getAdapter()).appendToList(masterList);

                            mSwipeRefreshLayout.setRefreshing(false);

                        }
                    });

                }catch (Throwable t){
                    Log.d("commentloading", t.getLocalizedMessage());

                }

            }
        };
        Thread mythread = new Thread(runnable);
        queryThreadID = mythread.getId();
        mythread.start();
    }

    private void loadMoreGComments(final String uORt){
        //Log.d("gcomments", "loading more g comments");
        Runnable runnable = new Runnable() {
            public void run() {
                try{

                    long thisThreadID = Thread.currentThread().getId();

                    final List<Object> masterList = new ArrayList<>();

                    final ArrayList<VSComment> grootComments = new ArrayList<>();

                    String queryParentID = topCardContent.getComment_id();

                    getRootComments(currCommentsIndex, grootComments, queryParentID, uORt);

                    VSCNode prevNode = null;
                    if(!grootComments.isEmpty()){
                        //set up nodeMap with root comments
                        for(int i = 0; i < grootComments.size(); i++){
                            if(thisThreadID != queryThreadID){
                                Log.d("wow", "broke out of old query thread");
                                nowLoading = false;
                                return;
                            }
                            VSCNode cNode = new VSCNode(grootComments.get(i));
                            final String commentID = cNode.getCommentID();

                            Log.d("wow", "child query, parentID to query: " + commentID);


                            cNode.setNestedLevel(0);

                            if(prevNode != null){
                                prevNode.setTailSibling(cNode);
                                cNode.setHeadSibling(prevNode);
                            }

                            nodeMap.put(commentID, cNode);
                            prevNode = cNode;
                        }

                    }
                    else{
                        nowLoading = true; //no more loading until refresh
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }

                    //set up comments list
                    VSCNode temp;
                    for(int i = 0; i<grootComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            nowLoading = false;
                            return;
                        }

                        temp = nodeMap.get(grootComments.get(i).getComment_id());
                        if(temp != null){
                            setCommentList(temp, masterList);
                        }
                    }

                    /*
                    if(!grootComments.isEmpty()){
                        nowLoading = false;
                    }
                    else {
                        nowLoading = true;  //to stop triggering loadMore
                    }
                    */

                    //run UI updates on UI Thread
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            applyUserActions(masterList);

                            ((PostPageAdapter)RV.getAdapter()).appendToList(masterList);

                            mSwipeRefreshLayout.setRefreshing(false);

                        }
                    });

                }catch (Throwable t){
                    Log.d("commentloading", t.getLocalizedMessage());

                }

            }
        };
        Thread mythread = new Thread(runnable);
        queryThreadID = mythread.getId();
        mythread.start();
    }

    private void medalsUpdateDB(final HashMap<Integer, VSComment> upgradeMap, HashSet<String> newMedalists){

        /*
        //****** not using "timecode" anymore ******
        //user gets updateDuty if user.timecode matches the one given at query time, or if user authored the post, or if user won a medal in this upgrade event
        //so if none of those conditions are met then exit the function with a return.
        if( ! (activity.getUserTimecode() == (int)(System.currentTimeMillis()%10) || activity.getUsername().equals(post.getAuthor()) || newMedalists.contains(activity.getUsername())) ){
            return;
        }
        */

        Runnable runnable = new Runnable() {
            public void run() {

                try {

                    int pointsIncrement = 0;

                    for(HashMap.Entry<Integer, VSComment> entry : upgradeMap.entrySet()){
                        //update user medal count, decrementing previous medal's count
                        //update vscomment topmedal
                        //update user points, incrementing by appropriate increment amount, not overlapping with points from previous medals, determined by difference between comment.topMedal and the medal that was won
                        int currentMedal = entry.getKey();

                        HashMap<String, AttributeValueUpdate> vscUpdates = new HashMap<>();

                        //avc = comment topmedal update
                        AttributeValueUpdate avc;

                        String mUsername = entry.getValue().getAuthor();
                        int usernameHash;
                        if(mUsername.length() < 5){
                            usernameHash = mUsername.hashCode();
                        }
                        else{
                            String hashIn = "" + mUsername.charAt(0) + mUsername.charAt(mUsername.length() - 2) + mUsername.charAt(1) + mUsername.charAt(mUsername.length() - 1);
                            usernameHash = hashIn.hashCode();
                        }

                        String incrementKey = null;

                        switch(currentMedal){   //set updates for user num_g/num_s/num_b increment and comment topmedal
                            case 3: //gold medal won
                                incrementKey = "g";
                                pointsIncrement = goldPoints;
                                break;

                            case 2: //silver medal won
                                incrementKey = "s";
                                pointsIncrement = silverPoints;
                                break;

                            case 1: //bronze medal won
                                incrementKey = "b";
                                pointsIncrement = bronzePoints;
                                break;
                        }

                        if(incrementKey != null){
                            String decrementKey = "";

                            switch(entry.getValue().getTopmedal()){ //set updates for user num_s/num_b decrement and points
                                case 0: //went from no medal to currentMedal
                                    break;

                                case 1: //went from bronze to currentMedal
                                    decrementKey = "b";
                                    pointsIncrement -= bronzePoints;
                                    break;

                                case 2: //went from silver to currentMedal
                                    decrementKey = "s";
                                    pointsIncrement -= silverPoints;
                                    break;

                                //no case 3 if it was already gold then it's not a upgrade event

                            }
                            String medalType = incrementKey + decrementKey;
                            int timeValueSecs = (int) (System.currentTimeMillis() / 1000);
                            int timeValue = ((timeValueSecs / 60 )/ 60 )/ 24; //now timeValue is in days since epoch
                            //submit update request to firebase updates path, the first submission will trigger Cloud Functions operation to update user medals and points
                            String updateRequest = "updates/" + Integer.toString(timeValue) + "/" + Integer.toString(usernameHash)  + "/" + mUsername + "/" + entry.getValue().getComment_id() + "/" + medalType;
                            MedalUpdateRequest medalUpdateRequest = new MedalUpdateRequest(pointsIncrement, timeValueSecs, sanitizeContentForURL(entry.getValue().getContent()));
                            mFirebaseDatabaseReference.child(updateRequest).setValue(medalUpdateRequest);

                            //update topmedal on local comment object in nodeMap
                            //nodeMap.get(entry.getValue()).setTopMedal(currentMedal); I think the below version would work fine, I believe it maps to same object that is also linked to VSCNode objects in nodeMap
                            entry.getValue().setTopmedal(currentMedal);
                            //the below debug statement is to confirm the statement above, delete it eventually
                            // Log.d("topmedal", "currentMedal = " + Integer.toString(currentMedal) + "\nnodeMap updated topmedal: " + Integer.toString(nodeMap.get(entry.getValue().getComment_id()).getNodeContent().getTopmedal()));
                        /*
                            //update vscomment topmedal
                            avc = new AttributeValueUpdate()
                                    .withValue(new AttributeValue().withN(Integer.toString(currentMedal)))
                                    .withAction(AttributeAction.PUT);

                            vscUpdates.put("topmedal", avc);

                            UpdateItemRequest vscUpdateRequest = new UpdateItemRequest()
                                    .withTableName("vscomment")
                                    .withKey(vscommentKeyMap)
                                    .withAttributeUpdates(vscUpdates);

                            activity.getDDBClient().updateItem(vscUpdateRequest);
                        */
                        }
                    }
                }
                catch (Throwable t){

                }

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    private void sendCommentUpvoteNotification(VSComment content){
        if(!content.getAuthor().equals("[deleted]") && !content.getAuthor().equals(activity.getUsername())){
            String payloadContent = sanitizeContentForURL(content.getContent());
            String commentAuthorPath = getUsernameHash(content.getAuthor()) + "/" + content.getAuthor() + "/n/u/" +content.getComment_id() + ":" + payloadContent;
            mFirebaseDatabaseReference.child(commentAuthorPath).child(activity.getUsername()).setValue(System.currentTimeMillis()/1000);
        }
    }

    private void sendPostVoteNotification(){
        if(!post.getAuthor().equals("[deleted]") && !post.getAuthor().equals(activity.getUsername())){
            String nKey = postID+":"+sanitizeContentForURL(post.getRedname())+":"+sanitizeContentForURL(post.getBlackname())+":"+sanitizeContentForURL(post.getQuestion());
            String postAuthorPath = getUsernameHash(post.getAuthor()) + "/" + post.getAuthor() + "/n/v/" + nKey;
            mFirebaseDatabaseReference.child(postAuthorPath).child(activity.getUsername()).setValue(System.currentTimeMillis()/1000);
        }
    }

    private String getUsernameHash(String usernameIn){
        int usernameHash;
        if(usernameIn.length() < 5){
            usernameHash = usernameIn.hashCode();
        }
        else{
            String hashIn = "" + usernameIn.charAt(0) + usernameIn.charAt(usernameIn.length() - 2) + usernameIn.charAt(1) + usernameIn.charAt(usernameIn.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        return Integer.toString(usernameHash);
    }

    public String getR(){
        return post.getRedname();
    }

    public String getB(){
        return post.getBlackname();
    }

    public String getQ(){
        return post.getQuestion();
    }

    public String sanitizeContentForURL(String url){
        String strIn = url.trim();
        if(strIn.length()>26){
            strIn.substring(0,26);
        }
        return strIn.trim().replaceAll("[ /\\\\.\\\\$\\[\\]\\\\#]", "^").replaceAll(":", ";");
    }

    public Post getCurrentPost(){
        return post;
    }

    private void getRootComments(final int fromIndex, ArrayList<VSComment> results, String prIn, String uORt) {

        if(fromIndex == 0){
            currCommentsIndex = 0;
            childrenCount = 0;
            nowLoading = false;
        }

        Log.d("commentloading", "from: " + Integer.toString(fromIndex));

        String sortBy, ascORdesc;
        if(uORt.equals("c")){
            sortBy = "t"; //c stands for chronological, but still uses the t parameter for time, just in ascending order instead of descending order as when uORt is t.
            ascORdesc = "asc";
        }
        else{
            sortBy = uORt;
            ascORdesc = "desc";
        }

        String query = "/vscomment/_search";
        String payload = "{\"from\":"+Integer.toString(fromIndex)+",\"size\":"+Integer.toString(retrievalSize)+",\"sort\":[{\""+sortBy+"\":{\"order\":\""+ascORdesc+"\"}}],\"query\":{\"match\":{\"pr\":\""+prIn+"\"}}}";

        String url = "https://" + host + query;

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
            long startTime = System.currentTimeMillis();

            String strResponse = httpClient.execute(httpPost, responseHandler);

            long elapsedTime = System.currentTimeMillis() - startTime;
            Log.d("httpElapsedTime", "root query elapsed time in milliseconds: " + elapsedTime);

            JSONObject obj = new JSONObject(strResponse);
            JSONArray hits = obj.getJSONObject("hits").getJSONArray("hits");
            //Log.d("idformat", hits.getJSONObject(0).getString("_id"));
            if(hits.length() == 0){
                Log.d("loadmore", "end reached, disabling loadMore");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
                return;
            }
            for(int i = 0; i < hits.length(); i++){
                JSONObject item = hits.getJSONObject(i).getJSONObject("_source");
                results.add(new VSComment(item));
                currCommentsIndex++;
            }
            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void getChildComments(ArrayList<VSComment> commentParents, ArrayList<VSComment> results) {
        String query = "/vscomment/_msearch";
        StringBuilder strBuilder = new StringBuilder(100*commentParents.size());

        //strBuilder.append("{}\n{\"query\":{\"match\":{\"pr\":\""+commentParents.get(0).getComment_id()+"\"}},\"size\":2}");
        for(int n = 0; n<commentParents.size(); n++){
            strBuilder.append("{}\n{\"from\":0,\"size\":2,\"sort\":[{\"u\":{\"order\":\"desc\"}}],\"query\":{\"match\":{\"pr\":\"" + commentParents.get(n).getComment_id() + "\"}}}\n");
        }
        String payload = strBuilder.toString();

        //Log.d("childCommentsQuery", payload);

        String url = "https://" + host + query;

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

            //Log.d("childCommentsQuery", strResponse);
            JSONObject responseObj = new JSONObject(strResponse);
            JSONArray responseArray = responseObj.getJSONArray("responses");
            for(int r = 0; r<responseArray.length(); r++){

                JSONObject hitsObject = responseArray.getJSONObject(r).getJSONObject("hits");

                //set the child_count on the parent comment
                int childCount = hitsObject.getInt("total");
                VSCNode parentNode = nodeMap.get(commentParents.get(r).getComment_id());
                if(parentNode != null){
                    parentNode.getNodeContent().setChild_count(childCount);
                }

                JSONArray hArray = hitsObject.getJSONArray("hits");
                for(int h = 0; h<hArray.length(); h++){
                    results.add(new VSComment(hArray.getJSONObject(h).getJSONObject("_source")));
                    childrenCount++;
                }

            }

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VSComment getComment(String comment_id){

        String query = "/vscomment/vscomment_type/"+comment_id;
        String url = "https://" + host + query;

        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);

        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(region)
                .serviceName("es") // es - elastic search. use your service name
                .httpMethodName("GET") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(query) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .debug() // turn on the debug mode
                .build();

        HttpGet httpGet = new HttpGet(url);

		        /* Get header calculated for request */
        Map<String, String> header = aWSV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

            httpGet.addHeader(key, value);
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
            long startTime = System.currentTimeMillis();

            String strResponse = httpClient.execute(httpGet, responseHandler);

            JSONObject obj = new JSONObject(strResponse);
            JSONObject item = obj.getJSONObject("_source");
            return new VSComment(item);

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //if the ES GET fails, then return old topCardContent
        return topCardContent;
    }

    private Post getPost(String post_id, final boolean writingPostVoteToDB){

        String query = "/post/post_type/"+post_id;
        String url = "https://" + host + query;

        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);

        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(region)
                .serviceName("es") // es - elastic search. use your service name
                .httpMethodName("GET") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(query) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .debug() // turn on the debug mode
                .build();

        HttpGet httpGet = new HttpGet(url);

        /* Get header calculated for request */
        Map<String, String> header = aWSV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();

            /* Attach header in your request */
            /* Simple get request */

            httpGet.addHeader(key, value);
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
            long startTime = System.currentTimeMillis();

            String strResponse = httpClient.execute(httpGet, responseHandler);

            JSONObject obj = new JSONObject(strResponse);
            JSONObject item = obj.getJSONObject("_source");

            final Post refreshedPost = new Post(item, false);

            if(post != null){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.applyPostRefreshToMyAdapter(refreshedPost, writingPostVoteToDB);
                    }
                });
            }

            return refreshedPost;

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //if the ES GET fails, then return old topCardContent
        return post;
    }


    public void commentSubmissionRefresh(VSComment submittedComment){
        //first handle writing user actions to db and refreshing post card or top card
        nowLoading = false;
        dbWriteComplete = false;
        final boolean writingPostVoteToDB = (!lastSubmittedVote.equals(userAction.getVotedSide()));

        if (redIncrementedLast) {
            postRefreshCode = "r";
            if (lastSubmittedVote.equals("BLK")) {
                postRefreshCode = "rb";
            }
        } else {
            postRefreshCode = "b";
            if (lastSubmittedVote.equals("RED")) {
                postRefreshCode = "br";
            }
        }

        writeActionsToDB();
        try{
            long end = System.currentTimeMillis() + 8*1000; // 8 seconds * 1000 ms/sec

            while(!dbWriteComplete && System.currentTimeMillis() < end){

            }
            if(!dbWriteComplete){
                Log.d("PostPageRefresh", "user actions dbWrite timeout");
                dbWriteComplete = true;
            }
            else{
                Log.d("PostPageRefresh", "user actions dbWrite successful");
            }

            //update post card if atRootLevel, else update top card
            if(atRootLevel){
                post = getPost(postID, writingPostVoteToDB);
                postTopic = post.getQuestion();
                postX = post.getRedname();
                postY = post.getBlackname();
                origRedCount = post.getRedcount();
                origBlackCount = post.getBlackcount();
                redIncrementedLast = false;
                blackIncrementedLast = false;
            }
            else{
                final VSComment updatedTopCardContent = getComment(topCardContent.getComment_id());
                nodeMap.get(topCardContent.getComment_id()).setNodeContent(updatedTopCardContent);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String actionEntry = actionMap.get(updatedTopCardContent.getComment_id());
                        if(actionEntry != null){
                            switch (actionEntry){
                                case "U":
                                    updatedTopCardContent.initialSetUservote(UPVOTE);
                                    break;

                                case "D":
                                    updatedTopCardContent.initialSetUservote(DOWNVOTE);
                                    break;
                                //we ignore case "N" because uservote is 0 by default so we don't need to set it here
                            }
                        }
                        parentCache.put(updatedTopCardContent.getComment_id(), updatedTopCardContent);
                        setUpTopCard(parentCache.get(updatedTopCardContent.getComment_id()));
                    }
                });
            }


        }catch (Throwable t){

        }
        //finished handle writing user actions to db and refreshing post card or top card

        Log.d("pageLevel", "CSR: level: " + Integer.toString(pageLevel));
        final ArrayList<VSComment> rootComments = new ArrayList<>();
        final ArrayList<VSComment> childComments = new ArrayList<>();
        final ArrayList<VSComment> grandchildComments = new ArrayList<>();
        final String rootParentID = submittedComment.getParent_id();
        sortType = MOST_RECENT;


        final List<Object> masterList = new ArrayList<>();

        getRootComments(0, rootComments, rootParentID, "t");

        chunkSorter(rootComments);

        VSCNode prevNode = null;

        final HashMap<Integer, VSComment> medalUpgradeMap = new HashMap<>();
        final HashSet<String> medalWinners = new HashSet<>();
        exitLoop = false;

        if(!rootComments.isEmpty()){
            int currMedalNumber = 3; //starting with 3, which is gold medal
            int prevUpvotes = rootComments.get(0).getUpvotes();
            VSComment currComment;

            //set up nodeMap with root comments
            for(int i = 0; i < rootComments.size(); i++){
                currComment = rootComments.get(i);
                final String commentID = currComment.getComment_id();
                if(commentID.equals(submittedComment.getComment_id())){
                    rootComments.remove(i);
                    currCommentsIndex--;
                    continue;
                }

                VSCNode cNode = new VSCNode(currComment);

                cNode.setNestedLevel(0);

                if(prevNode != null){
                    prevNode.setTailSibling(cNode);
                    cNode.setHeadSibling(prevNode);
                }

                nodeMap.put(commentID, cNode);
                prevNode = cNode;
            }
            if(!rootComments.isEmpty()){ //after removing possible duplicate submittedComment in rootComments, we need to check again if rootComments is empty or not, before calling getChildComments with it
                getChildComments(rootComments, childComments);
            }
        }
        else if(pageLevel > 0){

            VSCNode cNode = new VSCNode(submittedComment);
            cNode.setNestedLevel(0);
            nodeMap.put(submittedComment.getComment_id(), cNode);
            masterList.add(0, submittedComment);
            currCommentsIndex++;

            masterList.add(0, new TopCardObject(parentCache.get(rootParentID)));

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    atRootLevel = false;
                    PPAdapter = new PostPageAdapter(masterList, post, activity, pageLevel, thisFragment);
                    RV.setAdapter(PPAdapter);
                    activity.setPostInDownload(postID, "done");
                    setUpTopCard(parentCache.get(rootParentID));
                    mSwipeRefreshLayout.setRefreshing(false);

                    RV.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            LinearLayoutManager layoutManager=LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                            int lastVisible = layoutManager.findLastVisibleItemPosition() - childrenCount;

                            boolean endHasBeenReached = lastVisible + loadThreshold >= currCommentsIndex;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                            if (currCommentsIndex > 0 && endHasBeenReached) {
                                //you have reached to the bottom of your recycler view
                                if(!nowLoading){
                                    nowLoading = true;
                                    Log.d("Load", "Now Loadin More");
                                    loadMoreComments("t");
                                }
                            }
                        }
                    });
                    //activity.hideToolbarProgressbar();
                    //activity.getViewPager().setCurrentItem(3);
                }
            });
            return;
        }

        if(!medalUpgradeMap.isEmpty()) {
            //set update duty
            //if no update duty then set update duty true if user authored one of the comments
            //update DB is update duty true.
            //medalsUpdateDB(medalUpgradeMap, medalWinners);
        }

        if(!childComments.isEmpty()){
            //set up nodeMap with child comments
            for(int i = 0; i < childComments.size(); i++){

                VSCNode cNode = new VSCNode(childComments.get(i));
                final String commentID = cNode.getCommentID();

                cNode.setNestedLevel(1);
                VSCNode parentNode = nodeMap.get(cNode.getParentID());
                if(parentNode != null) {
                    if(!parentNode.hasChild()){
                        parentNode.setFirstChild(cNode);
                        cNode.setParent(parentNode);
                    }
                    else{
                        VSCNode sibling = parentNode.getFirstChild();
                        if(sibling.getUpvotes() == cNode.getUpvotes() && sibling.getVotecount() < cNode.getVotecount()){
                            sibling.setParent(null);
                            cNode.setParent(parentNode);
                            parentNode.setFirstChild(cNode);
                            cNode.setTailSibling(sibling);
                            sibling.setHeadSibling(cNode);
                        }
                        else{
                            sibling.setTailSibling(cNode);
                            cNode.setHeadSibling(sibling);
                        }
                    }
                }

                nodeMap.put(commentID, cNode);
            }

            getChildComments(childComments, grandchildComments);
        }

        //set up nodeMap with grandchild comments
        if(!grandchildComments.isEmpty()){
            //TODO: run chunkSorter on grandchildComments here

            for (int i = 0; i < grandchildComments.size(); i++){

                VSCNode cNode = new VSCNode(grandchildComments.get(i));
                cNode.setNestedLevel(2);

                VSCNode parentNode = nodeMap.get(cNode.getParentID());
                if(parentNode != null) {
                    if(!parentNode.hasChild()){
                        parentNode.setFirstChild(cNode);
                        cNode.setParent(parentNode);
                    }
                    else{
                        VSCNode sibling = parentNode.getFirstChild();
                        if(sibling.getUpvotes() == cNode.getUpvotes() && sibling.getVotecount() < cNode.getVotecount()){
                            sibling.setParent(null);
                            cNode.setParent(parentNode);
                            parentNode.setFirstChild(cNode);
                            cNode.setTailSibling(sibling);
                            sibling.setHeadSibling(cNode);
                        }
                        else{
                            sibling.setTailSibling(cNode);
                            cNode.setHeadSibling(sibling);
                        }
                    }
                }

                nodeMap.put(cNode.getCommentID(), cNode);
            }

        }


        //set up comments list
        VSCNode temp;
        for(int i = 0; i<rootComments.size(); i++) {

            temp = nodeMap.get(rootComments.get(i).getComment_id());
            if (temp != null) {
                setCommentList(temp, masterList);
            }
        }

        VSCNode sNode = new VSCNode(submittedComment);
        sNode.setNestedLevel(0);
        if(!rootComments.isEmpty()){
            VSCNode rNode = nodeMap.get(rootComments.get(0).getComment_id());
            sNode.setTailSibling(rNode);
            rNode.setHeadSibling(sNode);
        }
        nodeMap.put(submittedComment.getComment_id(), sNode);

        masterList.add(0, submittedComment);
        currCommentsIndex++;

        if(pageLevel > 0){
            masterList.add(0, new TopCardObject(parentCache.get(rootParentID)));
        }

        //run UI updates on UI Thread
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                applyUserActions(masterList);

                mSwipeRefreshLayout.setRefreshing(false);

                //Make sure to do this after applyUserActions because applyUserActions doesn't expect post object in the list
                if(pageLevel == 0) {
                    //vsComments.add(0, post);
                    masterList.add(0,post);
                }
                else{
                    setUpTopCard(parentCache.get(rootParentID));
                }

                //find view by id and attaching adapter for the RecyclerView
                //RV.setLayoutManager(new LinearLayoutManager(activity));

                //if true then we got the root comments whose parentID is postID ("rootest roots"), so include the post card for the PostPage view
                //this if condition also determines the boolean parameter at the end of PostPageAdapter constructor to notify adapter if it should set up Post Card
                if(rootParentID.equals(postID)){
                    atRootLevel = true;
                    PPAdapter = new PostPageAdapter(masterList, post, activity, pageLevel, thisFragment);
                }
                else{
                    atRootLevel = false;
                    PPAdapter = new PostPageAdapter(masterList, post, activity, pageLevel, thisFragment);
                }

                RV.setAdapter(PPAdapter);
                activity.setPostInDownload(postID, "done");

                RV.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        LinearLayoutManager layoutManager=LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                        int lastVisible = layoutManager.findLastVisibleItemPosition() - childrenCount;

                        boolean endHasBeenReached = lastVisible + loadThreshold >= currCommentsIndex;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                        if (currCommentsIndex > 0 && endHasBeenReached) {
                            //you have reached to the bottom of your recycler view
                            if(!nowLoading){
                                nowLoading = true;
                                Log.d("Load", "Now Loadin More");
                                loadMoreComments("t");
                            }
                        }
                    }
                });
                //activity.hideToolbarProgressbar();
                //activity.getViewPager().setCurrentItem(3);
            }
        });
    }

    public boolean pageCommentInputInUse(){
        return pageCommentInput.isInUse();
    }

    public void clearReplyingTo(){
        replyingTo.getLayoutParams().height = 0;
        if(trueReplyTarget != null){
            trueReplyTarget.setIsHighlighted(false);
            if(PPAdapter != null){
                PPAdapter.notifyItemChanged(trueReplyTargetIndex);
            }
        }
        if(editTarget != null){
            editTarget.setIsHighlighted(false);
            if(PPAdapter != null){
                PPAdapter.notifyItemChanged(editIndex);
            }
        }
        replyTarget = null;
        trueReplyTarget = null;
        pageCommentInput.setText("");
        pageCommentInput.setPrefix(null);
        replyTV.getLayoutParams().width = 0;
        editTarget = null;
        editIndex = 0;
        replyTargetIndex = 0;
    }

    public void itemViewClickHelper(VSComment clickedComment){

        int nestedLevel = clickedComment.getNestedLevel();
        switch (pageLevel) {
            case 0: //root page
                if(nestedLevel == 2){ //grandchild comment
                    addGrandParentToCache(clickedComment.getParent_id()); //pass in parent's id, then the function will get that parent's parent, the grandparent, and add it to the parentCache
                    addParentToCache(clickedComment.getParent_id());
                    //addThisToCache(clickedComment.getComment_id());
                    pageLevel = 2;
                    setCommentsPage(nodeMap.get(clickedComment.getParent_id()).getNodeContent());
                }
                else{ //root comment or child comment
                    if(nestedLevel == 1){ //child comment
                        addParentToCache(clickedComment.getParent_id());
                        pageLevel = 2;
                    }
                    else{
                        pageLevel = 1;
                    }
                    addThisToCache(clickedComment.getComment_id());
                    setCommentsPage(clickedComment);
                }

                break;

            case 1: //children page, where clicked root comment is the topCard and children comments are at nestedLevel == 0
                if(clickedComment.getNestedLevel() == 1){ //grandchild comment
                    addParentToCache(clickedComment.getParent_id());
                    //addThisToCache(clickedComment.getComment_id());
                    pageLevel = 2;
                    setCommentsPage(nodeMap.get(clickedComment.getParent_id()).getNodeContent());
                }
                else{ //child comment
                    //addParentToCache(clickedComment.getParent_id());
                    addThisToCache(clickedComment.getComment_id());
                    pageLevel = 2;
                    setCommentsPage(clickedComment);
                }

                break;

            //case 2 == grandchildren page, no itemView click events for that level
        }

    }

    public void itemReplyClickHelper(final VSComment clickedComment, final int index){
        if(replyTarget != null && replyTarget.getComment_id().equals(clickedComment)){
            return;
        }

        if(pageCommentInput.isInUse()){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked

                            pageCommentInput.setPrefix(null);
                            pageCommentInput.setText("");
                            if(replyTarget != null){
                                trueReplyTarget.setIsHighlighted(false);
                                PPAdapter.notifyItemChanged(trueReplyTargetIndex);
                                replyTarget = null;
                            }
                            else if(editTarget != null){
                                editTarget.setIsHighlighted(false);
                                PPAdapter.notifyItemChanged(editIndex);
                            }

                            itemReplyClickHelper(clickedComment, index);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked

                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("Are you sure? The text you entered will be discarded.").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

            return;
        }
        else{
            pageCommentInput.setPrefix(null);
            pageCommentInput.setText("");
            if(trueReplyTarget != null){
                trueReplyTarget.setIsHighlighted(false);
                PPAdapter.notifyItemChanged(trueReplyTargetIndex);
            }
        }

        editTarget = null;
        editIndex = 0;

        clickedComment.setIsHighlighted(true);
        PPAdapter.notifyItemChanged(index);
        replyTarget = clickedComment;
        trueReplyTarget = clickedComment;
        replyTargetIndex = index;
        trueReplyTargetIndex = index;
        replyingTo.setText("Replying to: " + clickedComment.getAuthor());
        replyingTo.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        viewTreeTriggerCount = 0;


        if(!pageCommentInput.hasFocus()){
            pageCommentInput.requestFocus();
            imm.showSoftInput(pageCommentInput, 0);
        }

        int nestedLevel = clickedComment.getNestedLevel();

        switch (pageLevel) {
            case 0: //root page
                if (nestedLevel == 2) { //grandchild comment
                    addGrandParentToCache(clickedComment.getParent_id()); //pass in parent's id, then the function will get that parent's parent, the grandparent, and add it to the parentCache
                    addParentToCache(clickedComment.getParent_id());
                } else { //root comment or child comment
                    if (nestedLevel == 1) { //child comment
                        addParentToCache(clickedComment.getParent_id());
                    }
                    addThisToCache(clickedComment.getComment_id());
                }

                break;

            case 1: //children page, where clicked root comment is the topCard and children comments are at nestedLevel == 0
                if (clickedComment.getNestedLevel() == 1) { //grandchild comment
                    addParentToCache(clickedComment.getParent_id());
                } else { //child comment
                    addThisToCache(clickedComment.getComment_id());
                }
                break;
        }

        if((pageLevel == 0 && nestedLevel == 2) || (pageLevel == 1 && nestedLevel == 1) || (pageLevel == 2 && nestedLevel == 0)){
            String prefix = "@"+replyTarget.getAuthor() +" ";
            pageCommentInput.setText(prefix);
            pageCommentInput.setPrefix(prefix);

            //this is a reply to a grandchild comment, so we set the replyTarget to its parent
            replyTarget = nodeMap.get(clickedComment.getParent_id()).getNodeContent();
            if(nodeMap.get(clickedComment.getComment_id()).getHeadSibling() == null){ //first displayed grandchild
                replyTargetIndex--;
            }
            else{ //second displayed grandchild
                replyTargetIndex -= 2;
            }
        }
    }

    public void editComment(final VSComment commentToEdit, final int index){
        //TODO: dialog if pageCommentInput is in use, same as in reply helper

        if(pageCommentInput.isInUse()){
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            pageCommentInput.setPrefix(null);
                            pageCommentInput.setText("");
                            if(replyTarget != null){
                                trueReplyTarget.setIsHighlighted(false);
                                PPAdapter.notifyItemChanged(trueReplyTargetIndex);
                                replyTarget = null;
                            }
                            else if(editTarget != null){
                                editTarget.setIsHighlighted(false);
                                PPAdapter.notifyItemChanged(editIndex);
                            }
                            editComment(commentToEdit, index);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked

                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("Are you sure? The text you entered will be discarded.").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

            return;
        }

        if(replyTarget != null){
            trueReplyTarget.setIsHighlighted(false);
            PPAdapter.notifyItemChanged(trueReplyTargetIndex);
            replyTarget = null;
        }
        else if(editTarget != null){
            editTarget.setIsHighlighted(false);
            PPAdapter.notifyItemChanged(editIndex);
        }

        editTarget = commentToEdit;
        editIndex = index;
        commentToEdit.setIsHighlighted(true);
        PPAdapter.notifyItemChanged(index);
        replyingTo.setText("Editing");
        replyingTo.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        viewTreeTriggerCount = 0;
        trueReplyTargetIndex = editIndex; //for scrolling to target
        pageCommentInput.setText(commentToEdit.getContent());
        if(!pageCommentInput.hasFocus()){
            pageCommentInput.requestFocus();
            imm.showSoftInput(pageCommentInput, 0);
        }
        if(!(commentToEdit.getParent_id().equals(postID))) {
            VSCNode parentNode = nodeMap.get(commentToEdit.getParent_id());
            if (parentNode != null) {
                String prefix = "@" + parentNode.getNodeContent().getAuthor() + " ";
                if (commentToEdit.getContent().startsWith(prefix)) {
                    pageCommentInput.setPrefix(prefix);
                }
            }
        }

    }

    public int getCurrentCommentCount(){
        return currCommentsIndex;
    }

    public boolean overflowMenuIsOpen(){
        if(PPAdapter != null){
            return PPAdapter.overflowMenuIsOpen();
        }
        else{
            return false;
        }
    }

    public void closeOverflowMenu(){
        if(PPAdapter != null) {
            PPAdapter.closeOverflowMenu();
        }
    }

    public void childOrGrandchildHistoryItemClicked(final VSComment clickedComment, final boolean fromProfile, final String key){
        freshlyVotedComments.clear();
        pageLevel  = 2;
        clearList();
        if(PPAdapter != null) {
            PPAdapter.clearList();
        }
        parentCache.put(clickedComment.getComment_id(), clickedComment);

        activity.getViewPager().setCurrentItem(3);
        mSwipeRefreshLayout.setRefreshing(true);

        Runnable runnable = new Runnable() {
            public void run() {
                final Post subjectPost = getPost(clickedComment.getPost_id(), false);

                if(fromProfile){
                    activity.commentHistoryClickHelper(clickedComment.getAuthor());
                }
                else{ //from Notifications
                    activity.notificationsCommentClickHelper(key);
                }

                sortType = POPULAR;
                nowLoading = false;

                post = subjectPost;

                postID = post.getPost_id();

                postTopic = post.getQuestion();
                postX = post.getRedname();
                postY = post.getBlackname();
                origRedCount = post.getRedcount();
                origBlackCount = post.getBlackcount();
                redIncrementedLast = false;
                blackIncrementedLast = false;

                parentCache.clear();

                initialLoadInProgress = true;

                if(userAction == null || !userAction.getPostID().equals(postID)){
                    userAction = activity.getMapper().load(UserAction.class, sessionManager.getCurrentUsername(), postID);   //TODO: catch exception for this query
                    //Log.d("DB", "download attempt for UserAction");
                }

                if(userAction == null){
                    userAction = new UserAction(sessionManager.getCurrentUsername(), postID);
                }
                lastSubmittedVote = userAction.getVotedSide();
                actionMap = userAction.getActionRecord();
                deepCopyToActionHistoryMap();

                //first get parent and add it to cache
                final VSComment parentComment = getComment(clickedComment.getParent_id());
                if(parentComment == null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mToast != null){
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    });
                    return;
                }
                parentCache.put(parentComment.getComment_id(), parentComment);

                if(!parentComment.getParent_id().equals(parentComment.getPost_id())) {
                    //turns out the parent is also child, so get the grandparent and add it to cache
                    VSComment grandParentComment = getComment(parentComment.getParent_id());
                    if (grandParentComment == null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mToast != null) {
                                    mToast.cancel();
                                }
                                mToast = Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT);
                                mToast.show();
                            }
                        });
                        return;
                    }
                    parentCache.put(grandParentComment.getComment_id(), grandParentComment);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setCommentsPage(parentComment);
                        }
                    });

                }
                else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setCommentsPage(clickedComment);
                        }
                    });
                }

            }
        };
        final Thread mythread = new Thread(runnable);
        mythread.start();

    }

    public void rootCommentHistoryItemClicked(final VSComment clickedRootComment, final boolean fromProfile, final String key){
        freshlyVotedComments.clear();
        clearList();
        if(PPAdapter != null) {
            PPAdapter.clearList();
        }
        activity.getViewPager().setCurrentItem(3);
        mSwipeRefreshLayout.setRefreshing(true);

        Runnable runnable = new Runnable() {
            public void run() {
                final Post subjectPost = getPost(clickedRootComment.getPost_id(), false);

                if(fromProfile){
                    activity.commentHistoryClickHelper(clickedRootComment.getAuthor());
                }
                else{ //from Notifications
                    activity.notificationsCommentClickHelper(key);
                }

                sortType = POPULAR;
                nowLoading = false;
                pageLevel = 1;

                post = subjectPost;

                postID = post.getPost_id();

                postTopic = post.getQuestion();
                postX = post.getRedname();
                postY = post.getBlackname();
                origRedCount = post.getRedcount();
                origBlackCount = post.getBlackcount();
                redIncrementedLast = false;
                blackIncrementedLast = false;

                parentCache.clear();

                initialLoadInProgress = true;

                if(userAction == null || !userAction.getPostID().equals(postID)){
                    userAction = activity.getMapper().load(UserAction.class, sessionManager.getCurrentUsername(), postID);   //TODO: catch exception for this query
                    //Log.d("DB", "download attempt for UserAction");
                }

                if(userAction == null){
                    userAction = new UserAction(sessionManager.getCurrentUsername(), postID);
                }
                lastSubmittedVote = userAction.getVotedSide();
                actionMap = userAction.getActionRecord();
                deepCopyToActionHistoryMap();

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setCommentsPage(clickedRootComment);
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    private String getTimeString(String timeStamp){
        int timeFormat = 0;

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        Date myDate = null;
        try {
            myDate = df.parse(timeStamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //TODO: test all possible cases to make sure date format conversion works correctly, for seconds, for all time format constants (secs, mins, ... , years), singulars / plurals
        long timediff = ((new Date()).getTime() - myDate.getTime()) / 1000;  //time elapsed since post creation, in seconds

        //time format constants: 0 = seconds, 1 = minutes, 2 = hours, 3 = days , 4 = weeks, 5 = months, 6 = years
        if(timediff >= 60) {  //if 60 seconds or more, convert to minutes
            timediff /= 60;
            timeFormat = 1;
            if(timediff >= 60) { //if 60 minutes or more, convert to hours
                timediff /= 60;
                timeFormat = 2;
                if(timediff >= 24) { //if 24 hours or more, convert to days
                    timediff /= 24;
                    timeFormat = 3;

                    if(timediff >= 365) { //if 365 days or more, convert to years
                        timediff /= 365;
                        timeFormat = 6;
                    }

                    else if (timeFormat < 6 && timediff >= 30) { //if 30 days or more and not yet converted to years, convert to months
                        timediff /= 30;
                        timeFormat = 5;
                    }

                    else if(timeFormat < 5 && timediff >= 7) { //if 7 days or more and not yet converted to months or years, convert to weeks
                        timediff /= 7;
                        timeFormat = 4;
                    }

                }
            }
        }


        if(timediff > 1) //if timediff is not a singular value
            timeFormat += 7;

        switch (timeFormat) {
            //plural
            case 7:
                return String.valueOf(timediff) + " seconds ago";
            case 8:
                return String.valueOf(timediff) + " minutes ago";
            case 9:
                return String.valueOf(timediff) + " hours ago";
            case 10:
                return String.valueOf(timediff) + " days ago";
            case 11:
                return String.valueOf(timediff) + " weeks ago";
            case 12:
                return String.valueOf(timediff) + " months ago";
            case 13:
                return String.valueOf(timediff) + " years ago";

            //singular
            case 0:
                return String.valueOf(timediff) + " second ago";
            case 1:
                return String.valueOf(timediff) + " minute ago";
            case 2:
                return String.valueOf(timediff) + " hour ago";
            case 3:
                return String.valueOf(timediff) + " day ago";
            case 4:
                return String.valueOf(timediff) + " week ago";
            case 5:
                return String.valueOf(timediff) + " month ago";
            case 6:
                return String.valueOf(timediff) + " year ago";

            default:
                return "";
        }
    }

    public void addFreshlyVotedComment(String commentID, Pair<Integer, Integer> votes){
        //Log.d("freshCommentVoteStatus", "Upvotes: " + votes.first.toString() + ", Downvotes: " + votes.second.toString());
        freshlyVotedComments.put(commentID, votes);
        freshlyVotedCommentsListPostID = postID;
    }

    public void editCommentLocal(int index, String text, String commentID){
        if(nodeMap.get(commentID) != null){
            Log.d("editComment", "node content updated");
            nodeMap.get(commentID).getNodeContent().setContent(text);
        }
        PPAdapter.editCommentLocal(index, text, commentID);
        Log.d("editComment", "adapter content updated");
    }

    public String getPostRefreshCode(){
        return postRefreshCode;
    }

    public int checkMedalWinnersList(String commentID){
        if(medalWinnersList == null || !medalWinnersList.containsKey(commentID)){
            return 0;
        }
        return medalWinnersList.get(commentID);
    }

}