package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.Condition;

import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.ThreadCounter;
import com.vs.bcd.versus.model.UserAction;
import com.vs.bcd.versus.model.VSCNode;
import com.vs.bcd.versus.model.VSComment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.vs.bcd.versus.adapter.PostPageAdapter.DOWNVOTE;
import static com.vs.bcd.versus.adapter.PostPageAdapter.NOVOTE;
import static com.vs.bcd.versus.adapter.PostPageAdapter.UPVOTE;


/**
 * Created by dlee on 6/7/17.
 */

public class PostPage extends Fragment {

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
    private PostSkeleton post;
    private SessionManager sessionManager;
    private List<Object> vsComments = new ArrayList<>(); //ArrayList of VSCNode
    private ViewGroup.LayoutParams RVLayoutParams;
    private RecyclerView RV;
    private LayoutInflater layoutInflater;
    private VSCNode currentRootLevelNode = null;    //This is the first child of current TopCard content. will be null at post level. we grab currentRootLevelNode.getParentNode() to go back up a level when up button is pressed in comment section view
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
    private boolean updateDuty = false;
    private int retrievalLimit = 25;
    private Map<String,AttributeValue> lastEvaluatedKey;
    private PostPage thisPage;
    private boolean exitLoop = false;
    final HashMap<String, VSCNode> nodeMap = new HashMap<>();
    private HashMap<String, VSComment> parentCache = new HashMap<>();
    private boolean atRootLevel = true;
    private long queryThreadID = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = (MainContainer)getActivity();
        rootView = inflater.inflate(R.layout.post_page, container, false);
        layoutInflater = inflater;
        //commentInput = (EditText) rootView.findViewById(R.id.commentInput);
        mRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.post_page_layout);
        postPageFAB = (FloatingActionButton)rootView.findViewById(R.id.postpage_fab);
        fabLP = (RelativeLayout.LayoutParams)postPageFAB.getLayoutParams();
        topCard = (RelativeLayout)rootView.findViewById(R.id.topCard);
        RV = (RecyclerView)rootView.findViewById(R.id.recycler_view_cs);
        RVLayoutParams = RV.getLayoutParams();

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
                ((MainContainer)getActivity()).getCommentEnterFragment().setContentReplyToPost(postTopic, postX, postY, post);
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
    private void commentUpvotesQuery(final String rootParentID, final boolean downloadImages){


        final ArrayList<VSComment> rootComments = new ArrayList<>();
        final ArrayList<VSComment> childComments = new ArrayList<>();
        final ArrayList<VSComment> grandchildComments = new ArrayList<>();

        /*
        BY THE TIME THIS FUNCTION IS CALLED, post SHOULD BE SET SO WE CAN JUST GET THE hashkey = parent_id = post_id FROM THERE
        We also put together the structure and display onto recycler view all on here too
        */
        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("-1"));

        Runnable runnable = new Runnable() {
            public void run() {
                long thisThreadID = Thread.currentThread().getId();
                final List<Object> masterList = new ArrayList<>();

                //vsComments.clear();
                //nodeMap.clear();

                VSComment queryTemplate = new VSComment();
                queryTemplate.setParent_id(rootParentID);

                DynamoDBQueryExpression queryExpression =
                        new DynamoDBQueryExpression()
                                .withHashKeyValues(queryTemplate)
                                .withRangeKeyCondition("upvotes", rangeKeyCondition)
                                .withScanIndexForward(false)
                                .withLimit(retrievalLimit);

                //get the root comments
                QueryResultPage queryResultPage = activity.getMapper().queryPage(VSComment.class, queryExpression);
                rootComments.addAll(queryResultPage.getResults());

                chunkSorter(rootComments);

                VSCNode prevNode = null;
                exitLoop = false;
                if(!rootComments.isEmpty()){
                    //get the children while setting up nodeMap with root comments
                    final ThreadCounter threadCounter = new ThreadCounter(0, rootComments.size(), thisPage);
                    for(int i = 0; i < rootComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }
                        VSCNode cNode = new VSCNode(rootComments.get(i).withPostID(postID));
                        final String commentID = cNode.getCommentID();

                        Log.d("wow", "child query, parentID to query: " + commentID);


                        cNode.setNestedLevel(0);

                        if(prevNode != null){
                            prevNode.setTailSibling(cNode);
                            cNode.setHeadSibling(prevNode);
                        }

                        nodeMap.put(commentID, cNode);
                        prevNode = cNode;

                        Runnable runnable = new Runnable() {
                            public void run() {
                                VSComment queryTemplate = new VSComment();
                                queryTemplate.setParent_id(commentID);
                                //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                                DynamoDBQueryExpression childQueryExpression =
                                        new DynamoDBQueryExpression()
                                                .withHashKeyValues(queryTemplate)
                                                .withRangeKeyCondition("upvotes", rangeKeyCondition)
                                                .withScanIndexForward(false)
                                                .withLimit(2);

                                QueryResultPage childQueryResultPage = activity.getMapper().queryPage(VSComment.class, childQueryExpression);
                                childComments.addAll(childQueryResultPage.getResults());

                                Log.d("wow", "child query result size: " + childComments.size());

                                threadCounter.increment();
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }

                    long end = System.currentTimeMillis() + 10*1000; // 10 seconds * 1000 ms/sec

                    while(!exitLoop && System.currentTimeMillis() < end){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }
                    }
                    exitLoop = false;
                }
                Log.d("wow", "child query result size at the end: " + childComments.size());
                exitLoop = false;
                if(!childComments.isEmpty()){

                    //get the grandchildren while setting up nodeMap with child comments
                    final ThreadCounter threadCounter2 = new ThreadCounter(0, childComments.size(), thisPage);
                    for(int i = 0; i < childComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }

                        VSCNode cNode = new VSCNode(childComments.get(i).withPostID(postID));
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

                        Runnable runnable = new Runnable() {
                            public void run() {
                                VSComment queryTemplate = new VSComment();
                                queryTemplate.setParent_id(commentID);
                                //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                                DynamoDBQueryExpression gChildQueryExpression =
                                        new DynamoDBQueryExpression()
                                                .withHashKeyValues(queryTemplate)
                                                .withRangeKeyCondition("upvotes", rangeKeyCondition)
                                                .withScanIndexForward(false)
                                                .withLimit(2);

                                QueryResultPage gChildQueryResultPage = activity.getMapper().queryPage(VSComment.class, gChildQueryExpression);
                                grandchildComments.addAll(gChildQueryResultPage.getResults());
                                threadCounter2.increment();
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }

                    long end2 = System.currentTimeMillis() + 10*1000; // 10 seconds * 1000 ms/sec

                    while(!exitLoop && System.currentTimeMillis() < end2){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }
                    }
                    exitLoop = false;
                }

                //set up nodeMap with grandchild comments
                if(!grandchildComments.isEmpty()){
                    //TODO: run chunkSorter on grandchildComments here

                    for (int i = 0; i < grandchildComments.size(); i++){

                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }

                        VSCNode cNode = new VSCNode(grandchildComments.get(i).withPostID(postID));
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


                //set up comments list using the first root comment node
                //set currentRootLevelNode = first root comment's node if it exists
                VSCNode temp;
                if(!rootComments.isEmpty()){
                    temp = nodeMap.get(rootComments.get(0).getComment_id());
                    if(temp != null && thisThreadID == queryThreadID){
                        currentRootLevelNode = temp;
                        setCommentList(temp, masterList);
                    }
                }
                for(int i = 1; i<rootComments.size(); i++){ //we already did i=0 above so start at i=1
                    if(thisThreadID != queryThreadID){
                        Log.d("wow", "broke out of old query thread");
                        return;
                    }

                    temp = nodeMap.get(rootComments.get(i).getComment_id());
                    if(temp != null){
                        setCommentList(temp, masterList);
                    }
                }

                if(rootComments.size() < retrievalLimit){
                    lastEvaluatedKey = null;
                }
                else{
                    //Log.d("Load: ", "retrieved " + Integer.toString(queryResults.size()) + " more items");
                    lastEvaluatedKey = queryResultPage.getLastEvaluatedKey();
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
                            PPAdapter = new PostPageAdapter(RV, masterList, post, activity, downloadImages, true);
                        }
                        else{
                            atRootLevel = false;
                            PPAdapter = new PostPageAdapter(RV, masterList, post, activity, downloadImages, false);
                        }

                        RV.setAdapter(PPAdapter);
                        activity.setPostInDownload(postID, "done");

                        //set load more listener for the RecyclerView adapter
                        PPAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                            @Override
                            public void onLoadMore() {

                                if (masterList.size() <= 3) {

                                } else {
                                    Toast.makeText(getActivity(), "Loading data completed", Toast.LENGTH_SHORT).show();
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


    private void commentTimeQuery(final String rootParentID, final boolean downloadImages){

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        final String currentTime = df.format(new Date());

        final ArrayList<VSComment> rootComments = new ArrayList<>();
        final ArrayList<VSComment> childComments = new ArrayList<>();
        final ArrayList<VSComment> grandchildComments = new ArrayList<>();

    /*
    BY THE TIME THIS FUNCTION IS CALLED, post SHOULD BE SET SO WE CAN JUST GET THE hashkey = parent_id = post_id FROM THERE
    We also put together the structure and display onto recycler view all on here too
    */
        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LT.toString())
                .withAttributeValueList(new AttributeValue().withS(currentTime));

        Runnable runnable = new Runnable() {
            public void run() {
                long thisThreadID = Thread.currentThread().getId();
                final List<Object> masterList = new ArrayList<>();

                //vsComments.clear();
                //nodeMap.clear();

                VSComment queryTemplate = new VSComment();
                queryTemplate.setParent_id(rootParentID);

                DynamoDBQueryExpression queryExpression =
                        new DynamoDBQueryExpression()
                                .withHashKeyValues(queryTemplate)
                                .withRangeKeyCondition("time", rangeKeyCondition)
                                .withScanIndexForward(false)
                                .withLimit(retrievalLimit);

                //get the root comments
                QueryResultPage queryResultPage = activity.getMapper().queryPage(VSComment.class, queryExpression);
                rootComments.addAll(queryResultPage.getResults());

                final Condition childrenRKCondition = new Condition()
                        .withComparisonOperator(ComparisonOperator.GT.toString())
                        .withAttributeValueList(new AttributeValue().withN("-1"));

                VSCNode prevNode = null;
                exitLoop = false;
                if(!rootComments.isEmpty()){
                    //get the children while setting up nodeMap with root comments
                    final ThreadCounter threadCounter = new ThreadCounter(0, rootComments.size(), thisPage);
                    for(int i = 0; i < rootComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }
                        VSCNode cNode = new VSCNode(rootComments.get(i).withPostID(postID));
                        final String commentID = cNode.getCommentID();

                        Log.d("wow", "child query, parentID to query: " + commentID);


                        cNode.setNestedLevel(0);

                        if(prevNode != null){
                            prevNode.setTailSibling(cNode);
                            cNode.setHeadSibling(prevNode);
                        }

                        nodeMap.put(commentID, cNode);
                        prevNode = cNode;

                        Runnable runnable = new Runnable() {
                            public void run() {
                                VSComment queryTemplate = new VSComment();
                                queryTemplate.setParent_id(commentID);
                                //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                                DynamoDBQueryExpression childQueryExpression =
                                        new DynamoDBQueryExpression()
                                                .withHashKeyValues(queryTemplate)
                                                .withRangeKeyCondition("upvotes", childrenRKCondition)
                                                .withScanIndexForward(false)
                                                .withLimit(2);

                                QueryResultPage childQueryResultPage = activity.getMapper().queryPage(VSComment.class, childQueryExpression);
                                childComments.addAll(childQueryResultPage.getResults());

                                Log.d("wow", "child query result size: " + childComments.size());

                                threadCounter.increment();
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }

                    long end = System.currentTimeMillis() + 10*1000; // 10 seconds * 1000 ms/sec

                    while(!exitLoop && System.currentTimeMillis() < end){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }
                    }
                    exitLoop = false;
                }
                Log.d("wow", "child query result size at the end: " + childComments.size());
                exitLoop = false;
                if(!childComments.isEmpty()){

                    //get the grandchildren while setting up nodeMap with child comments
                    final ThreadCounter threadCounter2 = new ThreadCounter(0, childComments.size(), thisPage);
                    for(int i = 0; i < childComments.size(); i++){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }

                        VSCNode cNode = new VSCNode(childComments.get(i).withPostID(postID));
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

                        Runnable runnable = new Runnable() {
                            public void run() {
                                VSComment queryTemplate = new VSComment();
                                queryTemplate.setParent_id(commentID);
                                //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                                DynamoDBQueryExpression gChildQueryExpression =
                                        new DynamoDBQueryExpression()
                                                .withHashKeyValues(queryTemplate)
                                                .withRangeKeyCondition("upvotes", childrenRKCondition)
                                                .withScanIndexForward(false)
                                                .withLimit(2);

                                QueryResultPage gChildQueryResultPage = activity.getMapper().queryPage(VSComment.class, gChildQueryExpression);
                                grandchildComments.addAll(gChildQueryResultPage.getResults());
                                threadCounter2.increment();
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }

                    long end2 = System.currentTimeMillis() + 10*1000; // 10 seconds * 1000 ms/sec

                    while(!exitLoop && System.currentTimeMillis() < end2){
                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }
                    }
                    exitLoop = false;
                }

                //set up nodeMap with grandchild comments
                if(!grandchildComments.isEmpty()){
                    //TODO: run chunkSorter on grandchildComments here

                    for (int i = 0; i < grandchildComments.size(); i++){

                        if(thisThreadID != queryThreadID){
                            Log.d("wow", "broke out of old query thread");
                            return;
                        }

                        VSCNode cNode = new VSCNode(grandchildComments.get(i).withPostID(postID));
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


                //set up comments list using the first root comment node
                //set currentRootLevelNode = first root comment's node if it exists
                VSCNode temp;
                if(!rootComments.isEmpty()){
                    temp = nodeMap.get(rootComments.get(0).getComment_id());
                    if(temp != null && thisThreadID == queryThreadID){
                        currentRootLevelNode = temp;
                        setCommentList(temp, masterList);
                    }
                }
                for(int i = 1; i<rootComments.size(); i++){ //we already did i=0 above so start at i=1
                    if(thisThreadID != queryThreadID){
                        Log.d("wow", "broke out of old query thread");
                        return;
                    }

                    temp = nodeMap.get(rootComments.get(i).getComment_id());
                    if(temp != null){
                        setCommentList(temp, masterList);
                    }
                }

                if(rootComments.size() < retrievalLimit){
                    lastEvaluatedKey = null;
                }
                else{
                    //Log.d("Load: ", "retrieved " + Integer.toString(queryResults.size()) + " more items");
                    lastEvaluatedKey = queryResultPage.getLastEvaluatedKey();
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
                            PPAdapter = new PostPageAdapter(RV, masterList, post, activity, downloadImages, true);
                        }
                        else{
                            atRootLevel = false;
                            PPAdapter = new PostPageAdapter(RV, masterList, post, activity, downloadImages, false);
                        }

                        RV.setAdapter(PPAdapter);
                        activity.setPostInDownload(postID, "done");

                        //set load more listener for the RecyclerView adapter
                        PPAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                            @Override
                            public void onLoadMore() {

                                if (masterList.size() <= 3) {

                                } else {
                                    Toast.makeText(getActivity(), "Loading data completed", Toast.LENGTH_SHORT).show();
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



    public void setContent(final PostSkeleton post, final boolean downloadImages){  //downloadImages signifies initial post page set up
        /*  Local postsList and commentsList contents check section
            //used to check local lists against DB.User version to test list setup/modification

            ArrayList<String> testPostsList = activity.getUserPostsList();
            ArrayList<String> testCommentsList = activity.getUserCommentsList();
            for(int i = 0; i<testPostsList.size();i++){
                Log.d("listtest posts", testPostsList.get(i));
            }
            for(int i = 0; i<testCommentsList.size();i++){
                Log.d("listtest comments", testCommentsList.get(i));
            }
        */

        Log.d("Debug", "setContent called");
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

        setUpdateDuty();    //determines if this user will have the update duty; authority to detect comment upgrade events and make DB updates accordingly

        currentRootLevelNode = null;

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

                commentUpvotesQuery(postID, downloadImages);
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

        timestamp.setText(PPAdapter.getTimeString(clickedComment.getTimestamp()));
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
        if(RV != null && RV.getAdapter() != null){
            ((PostPageAdapter)(RV.getAdapter())).clearList();
        }
        setUpTopCard(subjectComment);
        //parentCache.put(subjectComment.getComment_id(), subjectComment);

        commentUpvotesQuery(subjectComment.getComment_id(), false);

    }

    //only called when we're not on pageNestedLevel 0, so when ascending up nested levels towards root level. so not called in root level (in root level we simply exit out of PostPage on UpButton click)
    public void backToParentPage(){

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

        commentUpvotesQuery(tempParentID, false);

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
                switch(actionMap.get(temp.getCommentID())){
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
                            keyMap.put("parent_id", new AttributeValue().withS(tempNode.getParentID()));  //partition key
                            keyMap.put("comment_id", new AttributeValue().withS(entry.getKey()));   //sort key

                            HashMap<String, AttributeValueUpdate> updates =
                                    new HashMap<>();

                            AttributeValueUpdate avu;
                            switch (entry.getValue()) {
                                case "U":
                                    Log.d("DB update", "upvote increment");
                                    avu = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("upvotes", avu);

                                    UpdateItemRequest request = new UpdateItemRequest()
                                            .withTableName("vscomment")
                                            .withKey(keyMap)
                                            .withAttributeUpdates(updates);

                                    activity.getDDBClient().updateItem(request);

                                    //update activityHistoryMap
                                    actionHistoryMap.put(entry.getKey(), entry.getValue());
                                    break;

                                case "D":
                                    Log.d("DB update", "downvote increment");
                                    avu = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("downvotes", avu);

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
                            keyMap.put("parent_id", new AttributeValue().withS(tempNode.getParentID()));  //partition key
                            keyMap.put("comment_id", new AttributeValue().withS(entry.getKey()));   //sort key //TODO:sort key which we'll eventually change

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
                                    updates.put("upvotes", avu);
                                    avd = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("-1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("downvotes", avd);
                                    request = new UpdateItemRequest()
                                            .withTableName("vscomment")
                                            .withKey(keyMap)
                                            .withAttributeUpdates(updates);

                                    activity.getDDBClient().updateItem(request);
                                    //update activityHistoryMap
                                    actionHistoryMap.put(entry.getKey(), entry.getValue());
                                    break;

                                case "D":
                                    Log.d("DB update", "downvote increment");
                                    Log.d("DB update", "upvote decrement");
                                    avu = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("downvotes", avu);
                                    avd = new AttributeValueUpdate()
                                            .withValue(new AttributeValue().withN("-1"))
                                            .withAction(AttributeAction.ADD);
                                    updates.put("upvotes", avd);
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
                                        updates.put("upvotes", avd);
                                    } else {   //it's either "U" or "D", because if it was "N" (the only other option) then it wouldn't arrive to this switch case since history and current are equal //TODO: test if that's true
                                        Log.d("DB update", "downvote decrement");
                                        avd = new AttributeValueUpdate()
                                                .withValue(new AttributeValue().withN("-1"))
                                                .withAction(AttributeAction.ADD);
                                        updates.put("downvotes", avd);
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
                            redOrBlack = "redcount";
                            if (lastSubmittedVote.equals("BLK")) {
                                decrement = true;
                            }
                        } else {
                            redOrBlack = "blackcount";
                            if (lastSubmittedVote.equals("RED")) {
                                decrement = true;
                            }
                        }

                        HashMap<String, AttributeValue> keyMap =
                                new HashMap<>();
                        keyMap.put("category", new AttributeValue().withN(post.getCategoryIntAsString()));
                        keyMap.put("post_id", new AttributeValue().withS(postID));

                        HashMap<String, AttributeValueUpdate> updates =
                                new HashMap<>();

                        AttributeValueUpdate avu = new AttributeValueUpdate()
                                .withValue(new AttributeValue().withN("1"))
                                .withAction(AttributeAction.ADD);
                        updates.put(redOrBlack, avu);

                        if (decrement) {
                            String voteToDecrement = "redcount";
                            if (redOrBlack.equals("redcount")) {
                                voteToDecrement = "blackcount";
                            }
                            AttributeValueUpdate avd = new AttributeValueUpdate()
                                    .withValue(new AttributeValue().withN("-1"))
                                    .withAction(AttributeAction.ADD);
                            updates.put(voteToDecrement, avd);
                        }
                        else {  //decrement == false means this is a new vote, therefore increment votecount
                            AttributeValueUpdate vcu = new AttributeValueUpdate()
                                    .withValue(new AttributeValue().withN("1"))
                                    .withAction(AttributeAction.ADD);
                            updates.put("votecount", vcu);
                        }

                        UpdateItemRequest request = new UpdateItemRequest()
                                .withTableName("post")
                                .withKey(keyMap)
                                .withAttributeUpdates(updates);

                        activity.getDDBClient().updateItem(request);


                        if(System.currentTimeMillis()/1000 < post.getStl()){
                            request = new UpdateItemRequest()
                                    .withTableName("active_post")
                                    .withKey(keyMap)
                                    .withAttributeUpdates(updates);

                            activity.getDDBClient().updateItem(request);
                        }

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

    public long getCurrPostSTL(){
        return post.getStl();
    }

    private void setUpdateDuty(){
        updateDuty = ( activity.getUserTimecode() == (int)(System.currentTimeMillis()%10) || activity.getUsername().equals(post.getAuthor()) );
        if(updateDuty){
            Log.d("UPDATEDUTY", "on duty");
        }
        else{
            Log.d("UPDATEDUTY", "not on duty");
        }
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

    private void hidePostCardSortToggle(){
        //call a method on postpagefragmentAdapter that makes it make the change inside it on the postcardViewholder
    }
    private void showPostCardSortToggle(){
        //call a method on postpagefragmentAdapter that makes it make the change inside it on the postcardViewholder
    }

    private void hideTopcardSortToggle(){

    }
    private void showTopcardSortToggle(){

    }



}