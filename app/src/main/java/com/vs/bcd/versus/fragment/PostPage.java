package com.vs.bcd.versus.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.Condition;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.HttpGet;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.ArrayAdapterWithIcon;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.MedalUpdateRequest;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.ThreadCounter;
import com.vs.bcd.versus.model.UserAction;
import com.vs.bcd.versus.model.VSCNode;
import com.vs.bcd.versus.model.VSComment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
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

import static com.vs.bcd.versus.adapter.PostPageAdapter.DOWNVOTE;
import static com.vs.bcd.versus.adapter.PostPageAdapter.NOVOTE;
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
    private int pageNestedLevel = 0;    //increase / decrease as we get click into and out of comments
    private MainContainer activity;
    private boolean topCardActive = false;
    private RelativeLayout topCard;
    private boolean ppfabActive = true;
    private FloatingActionButton postPageFAB;
    private RelativeLayout.LayoutParams fabLP;
    private VSComment topCardContent = null;
    private UserAction userAction;
    private boolean applyActions = true;
    private boolean redIncrementedLast, blackIncrementedLast;
    private Map<String, String> actionMap;
    private Map<String, String> actionHistoryMap; //used to store previous user action on a comment, if any, for comparing with current user action, e.g. if user chose upvote and previously chose downvote, then we need to do both increment upvote and decrement downvote
    private int origRedCount, origBlackCount;
    private String lastSubmittedVote = "none";
    private int retrievalLimit = 25;
    private Map<String,AttributeValue> lastEvaluatedKey;
    private PostPage thisPage;
    private boolean exitLoop = false;
    final HashMap<String, VSCNode> nodeMap = new HashMap<>();
    private HashMap<String, VSComment> parentCache = new HashMap<>();
    private boolean atRootLevel = true;
    private long queryThreadID = 0;
    private int sortType = 1; //0 = New, 1 = Popular
    private final int NEW = 0;
    private final int POPULAR = 1;
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = (MainContainer)getActivity();
        rootView = inflater.inflate(R.layout.post_page, container, false);
        layoutInflater = inflater;

        host = activity.getESHost();
        region = activity.getESRegion();

        //commentInput = (EditText) rootView.findViewById(R.id.commentInput);
        mRelativeLayout =  rootView.findViewById(R.id.post_page_layout);
        postPageFAB = rootView.findViewById(R.id.postpage_fab);
        fabLP = (RelativeLayout.LayoutParams)postPageFAB.getLayoutParams();
        topCard = rootView.findViewById(R.id.topCard);
        RV = rootView.findViewById(R.id.recycler_view_cs);
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

        postPageFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: implement a version where we reply to comments and have this function choose between that and root comment version
                //depending if we're at isRootLevel or not
                ((MainContainer)getActivity()).getCommentEnterFragment().setContentReplyToPost(post);
                ((MainContainer)getActivity()).getViewPager().setCurrentItem(4);
            }
        });

        topCard.findViewById(R.id.replybuttontc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.getCommentEnterFragment().setContentReplyToComment(topCardContent);
                activity.getViewPager().setCurrentItem(4);
            }
        });

        topCard.findViewById(R.id.sort_type_selector_topcard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectSortType();
            }
        });

        final ImageButton topCardUpvoteButton = (ImageButton)rootView.findViewById(R.id.heartbuttontc);
        final ImageButton topCardDownvoteButton = (ImageButton)rootView.findViewById(R.id.broken_heart_button_tc);
        final TextView topCardHeartCount = (TextView)rootView.findViewById(R.id.heartCounttc);

        topCardUpvoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(topCardContent != null){
                    int userVote = topCardContent.getUservote();
                    if(userVote == UPVOTE){
                        topCardUpvoteButton.setImageResource(R.drawable.ic_heart);
                        topCardContent.setUservote(NOVOTE);
                        actionMap.put(topCardContent.getComment_id(), "N");
                        //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                    }
                    else if(userVote == DOWNVOTE){
                        topCardDownvoteButton.setImageResource(R.drawable.ic_heart_broken);
                        topCardUpvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                        topCardContent.setUservote(UPVOTE);
                        actionMap.put(topCardContent.getComment_id(), "U");
                    }
                    else if(userVote == NOVOTE){
                        topCardUpvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                        topCardContent.setUservote(UPVOTE);
                        actionMap.put(topCardContent.getComment_id(), "U");
                    }
                    Log.d("topcardrefresh", "topCardContent hearts total: " + Integer.toString(parentCache.get(topCardContent.getComment_id()).heartsTotal()));
                    topCardHeartCount.setText( Integer.toString(topCardContent.heartsTotal()) );
                }
            }
        });

        topCardDownvoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(topCardContent != null){
                    int userVote = topCardContent.getUservote();
                    if(userVote == DOWNVOTE){
                        topCardDownvoteButton.setImageResource(R.drawable.ic_heart_broken);
                        topCardContent.setUservote(NOVOTE);
                        actionMap.put(topCardContent.getComment_id(), "N");
                        //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                    }
                    else if(userVote == UPVOTE){
                        topCardUpvoteButton.setImageResource(R.drawable.ic_heart);
                        topCardDownvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                        topCardContent.setUservote(DOWNVOTE);
                        actionMap.put(topCardContent.getComment_id(), "D");
                    }
                    else if(userVote == NOVOTE){
                        topCardDownvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                        topCardContent.setUservote(DOWNVOTE);
                        actionMap.put(topCardContent.getComment_id(), "D");
                    }
                    topCardHeartCount.setText( Integer.toString(topCardContent.heartsTotal()) );
                }
            }
        });

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

    public void hidePostPageFAB(){
        Log.d("debug", "hppfab called");
        postPageFAB.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        postPageFAB.setEnabled(false);
        postPageFAB.setClickable(false);
        ppfabActive = false;
    }

    public void showPostPageFAB(){
        Log.d("debug", "showppfab called");
        postPageFAB.setEnabled(true);
        postPageFAB.setLayoutParams(fabLP);
        postPageFAB.setClickable(true);
        ppfabActive = true;
    }

    public void setSortType(int sortType){
        switch (sortType){
            case NEW:
                this.sortType = NEW;
                break;
            case POPULAR:
                this.sortType = POPULAR;
                break;
        }
    }

    /**
     * This method is called when swipe refresh is pulled down
     */
    @Override
    public void onRefresh() {

        nowLoading = false;

        dbWriteComplete = false;

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
                        post = getPost(postID);
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
                        case NEW:
                            refreshCommentTimestampQuery();
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

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            //((MainContainer)getActivity()).setToolbarTitleTextForCP();
            //TODO: get comments from DB, create the comment structure and display it here. Actually we're doing that in setContent() right now

            if(rootView != null) {
                enableChildViews();
                rootView.findViewById(R.id.recycler_view_cs).setLayoutParams(RVLayoutParams);
                RVLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                RVLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                RV.setLayoutParams(RVLayoutParams);
            }

        }
        else {
            if (rootView != null) {
                disableChildViews();
                RVLayoutParams.height = 0;
                RVLayoutParams.width = 0;
                RV.setLayoutParams(RVLayoutParams);
                topCardActive = false;
            }
        }
    }

    public void clearList(){
        nodeMap.clear();
        vsComments.clear();
        topCardContent = null;
    }

    public void enableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            if( !(childViews.get(i) instanceof FloatingActionButton && ppfabActive == false) ){

                childViews.get(i).setEnabled(true);
                childViews.get(i).setClickable(true);
                if(childViews.get(i).getId() == R.id.topCard && topCardContent != null){//for re-entry from CommentEnterFragment. Without this setUpTopCard, enableChildViews hides top card from view
                    setUpTopCard(topCardContent);
                }
                else{
                    childViews.get(i).setLayoutParams(LPStore.get(i));
                }
            }
        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
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


        final ArrayList<VSComment> rootComments = new ArrayList<>();
        final ArrayList<VSComment> childComments = new ArrayList<>();
        final ArrayList<VSComment> grandchildComments = new ArrayList<>();

        mSwipeRefreshLayout.setRefreshing(true);

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

                if(!rootComments.isEmpty()){
                    int currMedalNumber = 3; //starting with 3, which is gold medal
                    int prevUpvotes = rootComments.get(0).getUpvotes();
                    VSComment currComment;

                    //set up nodeMap with root comments
                    for(int i = 0; i < rootComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }
                        currComment = rootComments.get(i);
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

                        //medal handling
                        if(atRootLevel && currMedalNumber > 0){
                            if(currComment.getUpvotes() >= minUpvotes){ //need to meet upvotes minimum to be cosidered for medals
                                if(currComment.getUpvotes() < prevUpvotes){
                                    currMedalNumber--;
                                    prevUpvotes = currComment.getUpvotes();
                                }
                                if(currMedalNumber > 0){
                                    if(currComment.getTopmedal() < currMedalNumber){ //upgrade event detected
                                        medalUpgradeMap.put(new Integer(currMedalNumber), currComment);
                                        medalWinners.add(currComment.getAuthor());
                                    }
                                    cNode.setCurrentMedal(currMedalNumber);
                                }
                            }
                            else{ //if current comment doesn't meet minimum upvotes requirement for medals, that means subsequent comments won't either, so no more need to handle medals.
                                currMedalNumber = 0; //this will stop subsequent medal handling for this query
                            }
                        }
                    }
                    getChildComments(rootComments, childComments);
                }
                else if(!rootParentID.equals(postID)){
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    return;
                }

                if(!medalUpgradeMap.isEmpty()) {
                    //set update duty
                    //if no update duty then set update duty true if user authored one of the comments
                    //update DB is update duty true.
                    medalsUpdateDB(medalUpgradeMap, medalWinners);
                }

                if(!childComments.isEmpty()){
                    //set up nodeMap with child comments
                    for(int i = 0; i < childComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
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
                    }

                    getChildComments(childComments, grandchildComments);
                }

                //set up nodeMap with grandchild comments
                if(!grandchildComments.isEmpty()){
                    //TODO: run chunkSorter on grandchildComments here

                    for (int i = 0; i < grandchildComments.size(); i++){

                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
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
                        return;
                    }

                    temp = nodeMap.get(rootComments.get(i).getComment_id());
                    if(temp != null){
                        setCommentList(temp, masterList);
                    }
                }

                //run UI updates on UI Thread
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if(applyActions){
                            applyUserActions(masterList);
                        }
                        else{
                            Log.d("wow", "applyActions is false");
                        }

                        //Make sure to do this after applyUserActions because applyUserActions doesn't expect post object in the list
                        if(rootParentID.equals(postID)) {
                            //vsComments.add(0, post);
                            masterList.add(0,post);
                        }

                        //find view by id and attaching adapter for the RecyclerView
                        RV.setLayoutManager(new LinearLayoutManager(getActivity()));

                        //if true then we got the root comments whose parentID is postID ("rootest roots"), so include the post card for the PostPage view
                        //this if condition also determines the boolean parameter at the end of PostPageAdapter constructor to notify adapter if it should set up Post Card
                        if(rootParentID.equals(postID)){
                            atRootLevel = true;
                            PPAdapter = new PostPageAdapter(masterList, post, activity, true);
                        }
                        else{
                            atRootLevel = false;
                            PPAdapter = new PostPageAdapter(masterList, post, activity, false);
                        }

                        RV.setAdapter(PPAdapter);
                        activity.setPostInDownload(postID, "done");
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





    public void setContent(final Post post){  //downloadImages signifies initial post page set up

        nowLoading = false;

        this.post = post;

        postID = post.getPost_id();

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
                applyActions = true;
                if(userAction == null){
                    userAction = new UserAction(sessionManager.getCurrentUsername(), postID);
                    applyActions = false;
                }
                lastSubmittedVote = userAction.getVotedSide();
                actionMap = userAction.getActionRecord();
                deepCopyToActionHistoryMap(actionMap);

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

    //used to set up the card view at the top to show the clicked comment which won't scroll with the recycler view
    //topcard always exists, it's just hidden when not needed. this shows it if it's not already shown and we want it to be shown
    public void setUpTopCard(VSComment clickedComment){

        topCardContent = clickedComment;
        atRootLevel = false;

        if(ppfabActive){
            hidePostPageFAB();
        }

        CircleImageView circView = (CircleImageView)topCard.findViewById(R.id.profile_image_tc);
        TextView timestamp = (TextView)topCard.findViewById(R.id.timetvtc);
        TextView author = (TextView)topCard.findViewById(R.id.usernametvtc);
        TextView content = (TextView)topCard.findViewById(R.id.usercommenttc);
        TextView heartCount = (TextView)topCard.findViewById(R.id.heartCounttc);

        circView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                PPAdapter.profileClicked(v);
            }
        });

        timestamp.setText(PPAdapter.getTimeString(clickedComment.getTime()));
        author.setText(clickedComment.getAuthor());
        content.setText(clickedComment.getContent());
        heartCount.setText(Integer.toString(clickedComment.heartsTotal()));

        switch (clickedComment.getUservote()){
            case 0: //NOVOTE
                ((ImageButton)topCard.findViewById(R.id.heartbuttontc)).setImageResource(R.drawable.ic_heart);
                ((ImageButton)topCard.findViewById(R.id.broken_heart_button_tc)).setImageResource(R.drawable.ic_heart_broken);
                break;

            case 1: //UPVOTE
                ((ImageButton)topCard.findViewById(R.id.heartbuttontc)).setImageResource(R.drawable.ic_heart_highlighted);
                ((ImageButton)topCard.findViewById(R.id.broken_heart_button_tc)).setImageResource(R.drawable.ic_heart_broken);
                break;

            case 2: //DOWNVOTE
                ((ImageButton)topCard.findViewById(R.id.heartbuttontc)).setImageResource(R.drawable.ic_heart);
                ((ImageButton)topCard.findViewById(R.id.broken_heart_button_tc)).setImageResource(R.drawable.ic_heart_broken_highlighted);
                break;
            default:
                break;
        }

        if(!topCardActive){
            topCard.setEnabled(true);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            topCard.findViewById(R.id.replybuttontc).setEnabled(true);
            //lp.bottomMargin = 154;
            topCard.setLayoutParams(lp);
            topCardActive = true;
        }
    }

    public void hideTopCard(){
        topCardActive = false;
        topCardContent = null;
        topCard.findViewById(R.id.replybuttontc).setEnabled(false);
        topCard.setEnabled(false);
        topCard.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        showPostPageFAB();
        atRootLevel = true;
    }

    //used when expanding into nested levels, so when pageNestedLevel > 0
    public void setCommentsPage(VSComment subjectComment){
        mSwipeRefreshLayout.setRefreshing(true);
        if(RV != null && RV.getAdapter() != null){
            ((PostPageAdapter)(RV.getAdapter())).clearList();
        }
        setUpTopCard(subjectComment);

        sortType = POPULAR;
        commentsQuery(subjectComment.getComment_id(), "u");


    }

    //only called when we're not on pageNestedLevel 0, so when ascending up nested levels towards root level. so not called in root level (in root level we simply exit out of PostPage on UpButton click)
    public void backToParentPage(){
        mSwipeRefreshLayout.setRefreshing(true);

        if(RV != null && RV.getAdapter() != null){
            ((PostPageAdapter)(RV.getAdapter())).clearList();
        }

        String tempParentID = topCardContent.getParent_id();

        if(tempParentID.equals(postID)){
            hideTopCard();
        }
        else{
            setUpTopCard(parentCache.get(tempParentID));
        }

        /* for now comment this out and just enforce default sort order POPULAR between nested comment page navigation, no sortType storing.
        switch (sortType){
            case POPULAR:
                commentUpvotesQuery(tempParentID, false);
                break;

            case NEW:
                commentTimeQuery(tempParentID, false);
                break;
        }
        */
        sortType = POPULAR;
        commentsQuery(tempParentID, "u");


    }

    public boolean isRootLevel(){
        return atRootLevel;
    }

    public VSCNode getParent(VSCNode childnode){
        while(childnode.hasHeadSibling()){
            childnode = childnode.getHeadSibling();
        }
        return childnode.getParent();
    }

    //only call this after nodeTable and vsComments have been set up, but before passing vsComments into a PPAdapter instance
    //sets up VSComment.uservote for comments that user upvoted or downvoted
    //this is only used after downloading for initial uservote setup
    public void applyUserActions(List<Object> commentsList){
        VSCNode temp;

        for(int i = 0; i<commentsList.size(); i++){
            temp = nodeMap.get(((VSComment)commentsList.get(i)).getComment_id());

            if(temp == null){
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

                    for (Map.Entry<String, String> entry : actionMap.entrySet()) {  //first record of user action on this comment
                        actionHistoryEntryValue = actionHistoryMap.get(entry.getKey());

                        if (actionHistoryEntryValue == null) {
                            //just increment
                            HashMap<String, AttributeValue> keyMap =
                                    new HashMap<>();
                            tempNode = nodeMap.get(entry.getKey());
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

                                    sendCommentUpvoteNotification(tempNode.getNodeContent());
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

                                    //update activityHistoryMap
                                    actionHistoryMap.put(entry.getKey(), entry.getValue());
                                    break;

                                default:    //this happens when we have a new UesrAction (none found in DB) locally and we cancel a vote and record "N". We don't need to do DB operation in this case.
                                    markedForRemoval.add(entry.getKey());
                                    break;
                            }

                        }
                    /*
                    else if(!actionHistoryEntryValue.equals("N")){
                        Log.d("debug", "so this happens");
                    }
                    */
                        else if (!actionHistoryEntryValue.equals(entry.getValue())) {   //modifying exisiting record of user action on this comment
                            if (actionHistoryEntryValue.equals("N")) {
                                Log.d("DB update", "this is the source of error. we don't decrement opposite in this case");
                            }
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
                                    Log.d("DB update", "upvote increment");
                                    Log.d("DB update", "downvote decrement");
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
                                    Log.d("DB update", "downvote increment");
                                    Log.d("DB update", "upvote decrement");
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
                                    } else {   //it's either "U" or "D", because if it was "N" (the only other option) then it wouldn't arrive to this switch case since history and current are equal //TODO: test if that's true
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
                                    //we remove the "N" record because it only serves as marker for decrement, now decrement is getting executed so we're removing the marker, essentially resetting record on that comment
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

    private void deepCopyToActionHistoryMap(Map<String, String> actionRecord){
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

    public void selectSortType(){
        final String [] items = new String[] {"Popular", "New"};
        final Integer[] icons = new Integer[] {R.drawable.ic_thumb_up, R.drawable.ic_new_releases}; //TODO: change these icons to actual ones
        ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(), items, icons);
        new AlertDialog.Builder(getActivity()).setTitle("Sort by")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item ) {
                        switch(item){
                            case 0: //Sort by Popular; parent_id-upvotes-index query.
                                Log.d("SortType", "sort by upvotes");
                                sortType = POPULAR;
                                mSwipeRefreshLayout.setRefreshing(true);
                                refreshCommentUpvotesQuery();
                                break;

                            case 1: //Sort by New; parent_id-time-index query.
                                Log.d("SortType", "sort by time");
                                sortType = NEW;
                                mSwipeRefreshLayout.setRefreshing(true);
                                refreshCommentTimestampQuery();
                                break;
                        }
                    }
                }).show();

    }



    private void loadMoreComments(final String uORt){
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
                        lastEvaluatedKey = null;
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

                            if(applyActions){
                                applyUserActions(masterList);
                            }
                            else{
                                Log.d("wow", "applyActions is false");
                            }

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

        //user gets updateDuty if user.timecode matches the one given at query time, or if user authored the post, or if user won a medal in this upgrade event
        //so if none of those conditions are met then exit the function with a return.
        if( ! (activity.getUserTimecode() == (int)(System.currentTimeMillis()%10) || activity.getUsername().equals(post.getAuthor()) || newMedalists.contains(activity.getUsername())) ){
            return;
        }

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

                        HashMap<String, AttributeValue> vscommentKeyMap = new HashMap<>();

                        vscommentKeyMap.put("parent_id", new AttributeValue().withS(entry.getValue().getParent_id()));
                        vscommentKeyMap.put("comment_id", new AttributeValue().withS(entry.getValue().getComment_id()));

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
                            MedalUpdateRequest medalUpdateRequest = new MedalUpdateRequest(pointsIncrement, entry.getValue().getParent_id(), timeValueSecs);
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
        String payloadContent = sanitizeContentForURL(content.getContent());
        String commentAuthorPath = getUsernameHash(content.getAuthor()) + "/" + content.getAuthor() + "/n/u/" + content.getParent_id() + ":" +content.getComment_id() + ":" + payloadContent;
        mFirebaseDatabaseReference.child(commentAuthorPath).push().setValue(System.currentTimeMillis()/1000);
    }

    private void sendPostVoteNotification(){
        String nKey = postID+":"+sanitizeContentForURL(post.getRedname())+":"+sanitizeContentForURL(post.getBlackname())+":"+sanitizeContentForURL(post.getQuestion());
        String postAuthorPath = getUsernameHash(post.getAuthor()) + "/" + post.getAuthor() + "/n/v/" + nKey;
        mFirebaseDatabaseReference.child(postAuthorPath).push().setValue(System.currentTimeMillis()/1000);
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

    private String sanitizeContentForURL(String url){
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

        String query = "/vscomment/_search";
        String payload = "{\"from\":"+Integer.toString(fromIndex)+",\"size\":"+Integer.toString(retrievalSize)+",\"sort\":[{\""+uORt+"\":{\"order\":\"desc\"}}],\"query\":{\"match\":{\"pr\":\""+prIn+"\"}}}";

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
            long startTime = System.currentTimeMillis();

            String strResponse = httpClient.execute(httpPost, responseHandler);

            long elapsedTime = System.currentTimeMillis() - startTime;
            Log.d("httpElapsedTime", "child query elapsed time in milliseconds: " + elapsedTime);

            //Log.d("childCommentsQuery", strResponse);
            JSONObject responseObj = new JSONObject(strResponse);
            JSONArray responseArray = responseObj.getJSONArray("responses");
            for(int r = 0; r<responseArray.length(); r++){
                JSONArray hArray = responseArray.getJSONObject(r).getJSONObject("hits").getJSONArray("hits");
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

    private Post getPost(String post_id){

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
            return new Post(item, false);

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //if the ES GET fails, then return old topCardContent
        return post;
    }


    public void commentSubmissionRefresh(final VSComment submittedComment){


        final ArrayList<VSComment> rootComments = new ArrayList<>();
        final ArrayList<VSComment> childComments = new ArrayList<>();
        final ArrayList<VSComment> grandchildComments = new ArrayList<>();
        final String rootParentID = submittedComment.getParent_id();
        sortType = NEW;


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

                //medal handling
                if(atRootLevel && currMedalNumber > 0){
                    if(currComment.getUpvotes() >= minUpvotes){ //need to meet upvotes minimum to be cosidered for medals
                        if(currComment.getUpvotes() < prevUpvotes){
                            currMedalNumber--;
                            prevUpvotes = currComment.getUpvotes();
                        }
                        if(currMedalNumber > 0){
                            if(currComment.getTopmedal() < currMedalNumber){ //upgrade event detected
                                medalUpgradeMap.put(new Integer(currMedalNumber), currComment);
                                medalWinners.add(currComment.getAuthor());
                            }
                            cNode.setCurrentMedal(currMedalNumber);
                        }
                    }
                    else{ //if current comment doesn't meet minimum upvotes requirement for medals, that means subsequent comments won't either, so no more need to handle medals.
                        currMedalNumber = 0; //this will stop subsequent medal handling for this query
                    }
                }
            }
            if(!rootComments.isEmpty()){ //after removing possible duplicate submittedComment in rootComments, we need to check again if rootComments is empty or not, before calling getChildComments with it
                getChildComments(rootComments, childComments);
            }
        }
        else if(!rootParentID.equals(postID)){

            VSCNode cNode = new VSCNode(submittedComment);
            cNode.setNestedLevel(0);
            nodeMap.put(submittedComment.getComment_id(), cNode);
            masterList.add(0, submittedComment);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    atRootLevel = false;
                    PPAdapter = new PostPageAdapter(masterList, post, activity, false);
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
                    activity.getViewPager().setCurrentItem(3);
                }
            });
            return;
        }

        if(!medalUpgradeMap.isEmpty()) {
            //set update duty
            //if no update duty then set update duty true if user authored one of the comments
            //update DB is update duty true.
            medalsUpdateDB(medalUpgradeMap, medalWinners);
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
        VSCNode rNode = nodeMap.get(rootComments.get(0).getComment_id());
        sNode.setNestedLevel(0);
        sNode.setTailSibling(rNode);
        rNode.setHeadSibling(sNode);
        nodeMap.put(submittedComment.getComment_id(), sNode);
        masterList.add(0, submittedComment);

        //run UI updates on UI Thread
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(applyActions){
                    applyUserActions(masterList);
                }
                else{
                    Log.d("wow", "applyActions is false");
                }

                //Make sure to do this after applyUserActions because applyUserActions doesn't expect post object in the list
                if(rootParentID.equals(postID)) {
                    //vsComments.add(0, post);
                    masterList.add(0,post);
                }

                //find view by id and attaching adapter for the RecyclerView
                RV.setLayoutManager(new LinearLayoutManager(getActivity()));

                //if true then we got the root comments whose parentID is postID ("rootest roots"), so include the post card for the PostPage view
                //this if condition also determines the boolean parameter at the end of PostPageAdapter constructor to notify adapter if it should set up Post Card
                if(rootParentID.equals(postID)){
                    atRootLevel = true;
                    PPAdapter = new PostPageAdapter(masterList, post, activity, true);
                }
                else{
                    atRootLevel = false;
                    PPAdapter = new PostPageAdapter(masterList, post, activity, false);
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
                activity.getViewPager().setCurrentItem(3);
            }
        });
    }

}