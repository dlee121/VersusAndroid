package com.vs.bcd.versus.fragment;

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
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.UserAction;
import com.vs.bcd.versus.model.VSCNode;
import com.vs.bcd.versus.model.VSComment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private Hashtable<String, VSCNode> nodeTable;
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
    private UserAction currentUserAction;
    private boolean applyActions = true;
    private boolean redIncrementedLast, blackIncrementedLast;
    private Map<String, String> actionMap;
    private Map<String, String> actionHistoryMap;
    private int origRedCount, origBlackCount;
    private String lastSubmittedVote = "none";

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
                        //actionMap.remove(topCardContent.getComment_id());   //TODO: instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote
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
                    topCardHeartCount.setText( Integer.toString(topCardContent.getUpvotes() - topCardContent.getDownvotes()) );
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
                        //actionMap.remove(topCardContent.getComment_id());   //TODO: instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote
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
                    topCardHeartCount.setText( Integer.toString(topCardContent.getUpvotes() - topCardContent.getDownvotes()) );
                }
            }
        });

        return rootView;
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
            ((MainContainer)getActivity()).setToolbarTitleTextForCP();
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
        vsComments.clear();
    }

    public void enableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            if( !(childViews.get(i) instanceof FloatingActionButton && ppfabActive == false) ){
                childViews.get(i).setEnabled(true);
                childViews.get(i).setClickable(true);
                childViews.get(i).setLayoutParams(LPStore.get(i));
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
    public void setPostCard(Post post){
        ((TextView)(rootView.findViewById(R.id.post_page_question))).setText(post.getTopic());
        ((TextView)(rootView.findViewById(R.id.post_page_redname))).setText(post.getRedname());
        ((TextView)(rootView.findViewById(R.id.post_page_blackname))).setText(post.getBlackname());
        ((TextView)(rootView.findViewById(R.id.post_page_redcount))).setText(Integer.toString(post.getRedcount()));
        ((TextView)(rootView.findViewById(R.id.post_page_blackcount))).setText(Integer.toString(post.getBlackcount()));

        RV.createVi

    }
*/
    public void setContent(final PostSkeleton post, final boolean downloadImages){

        Log.d("Debug", "setContent called");
        postID = post.getPost_id();

        postTopic = post.getQuestion();
        postX = post.getRedname();
        postY = post.getBlackname();
        this.post = post;
        origRedCount = post.getRedcount();
        origBlackCount = post.getBlackcount();
        redIncrementedLast = false;
        blackIncrementedLast = false;
        Runnable runnable = new Runnable() {
            public void run() {
                if(currentUserAction == null || !currentUserAction.getPostID().equals(postID)){
                    currentUserAction = activity.getMapper().load(UserAction.class, sessionManager.getCurrentUsername(), postID);   //TODO: catch exception for this query
                    //Log.d("DB", "download attempt for UserAction");
                }
                applyActions = true;
                if(currentUserAction == null){
                    currentUserAction = new UserAction(sessionManager.getCurrentUsername(), postID);
                    applyActions = false;
                }
                lastSubmittedVote = currentUserAction.getVotedSide();
                actionMap = currentUserAction.getActionRecord();
                deepCopyToActionHistoryMap(actionMap);

                VSComment vscommentToQuery = new VSComment();
                vscommentToQuery.setPost_id(postID);

                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression().withHashKeyValues(vscommentToQuery);
                PaginatedQueryList<VSComment> result = ((MainContainer)getActivity()).getMapper().query(VSComment.class, queryExpression);
                result.loadAllResults();

                Iterator<VSComment> it = result.iterator();


                //below, we form the comment structure. each comment is a node in doubly linked list. So we only need the root comment that is at the top to traverse the comment tree to display all the comments.
                VSCNode firstParentNode = null; //holds the first parent node, which holds the comment that appears at the top of the hierarchy.
                VSCNode latestParentNode = null;  //holds the latest parent node we worked with. Used for assigning sibling order for parent nodes (root comments)
                nodeTable = new Hashtable(result.size());    //Hashtable to assist in assigning children/siblings.
                //TODO: Hashtable should be big enough to prevent collision as that fucks up the algorithm below. Right? Test if that's the case.
                VSCNode currNode, pNode;
                while (it.hasNext()) {

                    currNode = new VSCNode(it.next());
                    pNode = null;   //temporary node holder

                    //TODO: figure out how to add siblings, child, parent and all that most efficiently
                    if(currNode.isRoot()){  //this is a parent node, AKA a root comment node
                        if(latestParentNode == null) {    //this is the first parent node to be worked with here
                            currentRootLevelNode = currNode;
                            firstRoot = currNode;
                            firstParentNode = currNode;
                            latestParentNode = currNode;
                        }
                        else{
                            latestParentNode.setTailSibling(currNode);
                            currNode.setHeadSibling(latestParentNode);
                            latestParentNode = currNode;
                        }
                        //nodeTable.put(currNode.getCommentID(), currNode); //since this is a parent node, KEY = Comment_ID
                    }
                    else { //this is a child node, AKA reply node
                        pNode = nodeTable.put(currNode.getParentID(), currNode);    //pNode holds whatever value was mapped for this key, if any, that is now overwritten

                        if(pNode.getParentID().trim().equals(currNode.getParentID().trim())) { //same parents => siblings
                            //currNode is not a first_child, so we need to assign some siblings here.
                            // pNode currently holds the Head Sibling for currNode. Therefore currNode is Tail Sibling of pNode
                            //head_sibling holds comment that is displayed immediately above the node, and tail_sibling holds comment that is displayed immediately below the node.
                            //TODO: Implement the following: this sibling order is determined by upvotes-downvotes score, and then timestamp as tie breaker (and in the rare occasion of a tie again, use username String lexical comparison)
                            //TODO: For now just use timestamp (default sort order of query result) to assign sibling order. Eventually this ordering has to reflect vote score and aforementioned tiebreakers.
                            pNode.setTailSibling(currNode);
                            currNode.setHeadSibling(pNode);
                        }
                        else{   //different parents => parent-child relationship detected => first child. so this would happen for the first bucket collision, where the first item in there would have been the parent node hashed under its own comment_id as per this line: nodeTable.put(currNode.getCommentID(), currNode) at the bottom of this while loop, and the second item causing the collision would be the child comment hashed under its parent_id
                            pNode.setFirstChild(currNode);  //set currNode as first_child of its parentNode
                            currNode.setParent(pNode);
                        }
                    }

                    nodeTable.put(currNode.getCommentID(), currNode);   //add this node to the hash table so that its children, if any, can find it in the table.

                }

                //TODO: vscomment table in ddb is sorted by timestamp. So a parent comment would always come before a reply comment, so sorting the list is not necessary. Confirm this, and think of any case where a reply may come before parent and cause an error while setting its parent due to parent node not yet existing because it was placed after the reply in the list.

                //TODO: obtain a list of comments we want to display here (root comments and upto their grandchildren). while making the list, set VSComment.nestedLevel to 0, 1, or 2 accordingly, for indent.
                //TODO: then create the recycler view, following Tab1Newsfeed.java's example

            //Below is a debugging algorithm to see if comment structure is correctly built. with indentation to indicate nested level
            //printNode(firstParentNode, 0);

            //iterate through root nodes and populate the vsComments list for use by recycler view (PostPageAdapter)
            if(firstParentNode != null){
                setCommentList(firstParentNode);
                while(firstParentNode.hasTailSibling()){
                    firstParentNode = firstParentNode.getTailSibling();
                    setCommentList(firstParentNode);
                }
            }


            //run UI updates on UI Thread
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //find view by id and attaching adapter for the RecyclerView
                    RV.setLayoutManager(new LinearLayoutManager(getActivity()));
                    vsComments.add(0, post);
                    //insert post card into the adapter

                    if(applyActions){
                        applyUserActions();
                    }

                    PPAdapter = new PostPageAdapter(RV, vsComments, post, (MainContainer)getActivity(), downloadImages, true);
                    RV.setAdapter(PPAdapter);
                    activity.setPostInDownload(postID, "done");

                    //set load more listener for the RecyclerView adapter
                    PPAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                        @Override
                        public void onLoadMore() {

                            if (vsComments.size() <= 3) {
                    /*
                    posts.add(null);
                    PPAdapter.notifyItemInserted(posts.size() - 1);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            posts.remove(posts.size() - 1);
                            PPAdapter.notifyItemRemoved(posts.size());

                            //Generating more data
                            int index = posts.size();
                            int end = index + 10;
                            for (int i = index; i < end; i++) {
                                Contact contact = new Contact();
                                contact.setEmail("DevExchanges" + i + "@gmail.com");
                                posts.add(contact);
                            }
                            PPAdapter.notifyDataSetChanged();
                            PPAdapter.setLoaded();
                        }
                    }, 1500);
                    */
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
        mythread.start();
    }

    //TODO: currently just prints string representation in Logcat. Modify to insert TextView representation into PostPage layout
    public void printNode(VSCNode node, int level){
        String indent = "";
        for(int i=0; i<level; i++){
            indent += "\t";
        }
        Log.d("NODE", indent + node.getNodeContent().getContent());
        if(node.getFirstChild() != null) {
            printNode(node.getFirstChild(), level + 1);
        }
        if(node.getTailSibling() != null) {
            printNode(node.getTailSibling(), level);

        }
    }

    public void setCommentList(VSCNode rootNode){
        VSCNode tempChildNode, tempGCNode;

        vsComments.add(rootNode.setNestedLevelandGetComment(0)); //root node
        if(rootNode.hasChild()){    //first child
            tempChildNode = rootNode.getFirstChild();
            vsComments.add(tempChildNode.setNestedLevelandGetComment(1));

            if(tempChildNode.hasChild()){   //first child's first child
                tempGCNode = tempChildNode.getFirstChild();
                vsComments.add(tempGCNode.setNestedLevelandGetComment(2));

                if(tempGCNode.hasTailSibling()){    //first child's second child
                    vsComments.add((tempGCNode.getTailSibling()).setNestedLevelandGetComment(2));
                }
            }

            if(tempChildNode.hasTailSibling()){ //second child
                tempChildNode = tempChildNode.getTailSibling();
                vsComments.add(tempChildNode.setNestedLevelandGetComment(1));

                if(tempChildNode.hasChild()){   //second child's first child
                    tempGCNode = tempChildNode.getFirstChild();
                    vsComments.add(tempGCNode.setNestedLevelandGetComment(2));

                    if(tempGCNode.hasTailSibling()){    //second child's second child
                        vsComments.add((tempGCNode.getTailSibling()).setNestedLevelandGetComment(2));
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
        heartCount.setText(Integer.toString(clickedComment.getUpvotes() - clickedComment.getDownvotes()));

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
            lp.bottomMargin = 154;
            topCard.setLayoutParams(lp);
            topCardActive = true;
        }

    }

    public void hideTopCard(){
        topCardActive = false;
        topCard.findViewById(R.id.replybuttontc).setEnabled(false);
        topCard.setEnabled(false);
        topCard.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        showPostPageFAB();
    }

    //used when descending into nested levels, so when pageNestedLevel > 0
    public void setCommentsPage(VSComment clickedComment){

        Log.d("comment", "processing comment click");



        setUpTopCard(clickedComment);

        //note that VSCNode child is not necessarily a child of clickedComment, it could be the clickedComment itself if the comment currently has no child, that's how the nodeTable is used here
        VSCNode child = nodeTable.get(clickedComment.getComment_id()); //last child of clicked comment. or, if clicked comment currently does not have a child, it would be the node of the clicked comment itself.

        Log.d("comment", "clickedCommentID: " + clickedComment.getComment_id());
        Log.d("comment", "nodeCommentID: " + child.getCommentID());

        boolean commentCurrentlyChildless = child.getCommentID().equals(clickedComment.getComment_id());

        if(child != null){
            clearList();
            pageNestedLevel++;
            currentRootLevelNode = child;   //in case of currently childless comment, we need to fix this because currentRootLevelNode should hold clickedComment's child and not itself; we fix this by assigning a dummy child node inside the 'else' condition to the below 'if' statement which is reached when clickedComment is found to be currently childless

            if(!commentCurrentlyChildless){  //if child is not null && child.commentID does not equal clickedComment.commentID (if equal, it would indicate that clickedComment is currently childless, so no need to set up comment card views with PostPageAdapter)
                //get the first child
                while(child.hasHeadSibling()){
                    child = child.getHeadSibling();
                }
                currentRootLevelNode = child;

                setCommentList(child);

                //populate childList with the comments we want for the comment section, children and grandchildren of the clicked comment
                //in order
                while(child.hasTailSibling()){
                    child = child.getTailSibling();
                    setCommentList(child);
                }

                applyUserActions();

                //find view by id and attaching adapter for the RecyclerView
                RV.setLayoutManager(new LinearLayoutManager(getActivity()));
                PPAdapter = new PostPageAdapter(RV, vsComments, post, (MainContainer)getActivity(), false, false);
                RV.setAdapter(PPAdapter);

                //set load more listener for the RecyclerView adapter
                PPAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                    @Override
                    public void onLoadMore() {

                        if (vsComments.size() <= 3) {
                        /*
                        posts.add(null);
                        PPAdapter.notifyItemInserted(posts.size() - 1);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                posts.remove(posts.size() - 1);
                                PPAdapter.notifyItemRemoved(posts.size());

                                //Generating more data
                                int index = posts.size();
                                int end = index + 10;
                                for (int i = index; i < end; i++) {
                                    Contact contact = new Contact();
                                    contact.setEmail("DevExchanges" + i + "@gmail.com");
                                    posts.add(contact);
                                }
                                PPAdapter.notifyDataSetChanged();
                                PPAdapter.setLoaded();
                            }
                        }, 1500);
                        */
                        } else {
                            //Toast.makeText(getActivity(), "Loading data completed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            else {  //we get here if clickedComment is currently childless, skipping the above 'if' block which sets up card views with the PostPageAdapter.
                //like we said above, we fix the currentRootLevelNode to hold a dummy child node, to make navigation work (which requires that this variable holds clickedComment's first child and not clickedComment itself which is the case here)
                Log.d("comment", "dummy assignment");
                currentRootLevelNode = new VSCNode(null, currentRootLevelNode); //this only

            }

        }

    }

    public void setCommentsPageWithCurrentNode (){
        VSCNode node = currentRootLevelNode;

        if(node != null){
            pageNestedLevel--;
            clearList();

            if(isRootLevel()) {
                hideTopCard();
                vsComments.add(0, post);
            }
            else{
                setUpTopCard(currentRootLevelNode.getParent().getNodeContent());
            }

            setCommentList(node);
            //populate childList with the comments we want for the comment section, children and grandchildren of the clicked comment
            //in order
            while(node.hasTailSibling()){
                node = node.getTailSibling();
                setCommentList(node);
            }

            applyUserActions();
            //find view by id and attaching adapter for the RecyclerView
            RV.setLayoutManager(new LinearLayoutManager(getActivity()));
            PPAdapter = new PostPageAdapter(RV, vsComments, post, (MainContainer)getActivity(), false, isRootLevel());
            RV.setAdapter(PPAdapter);

            //set load more listener for the RecyclerView adapter
            PPAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore() {

                    if (vsComments.size() <= 3) {
                    /*
                    posts.add(null);
                    PPAdapter.notifyItemInserted(posts.size() - 1);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            posts.remove(posts.size() - 1);
                            PPAdapter.notifyItemRemoved(posts.size());

                            //Generating more data
                            int index = posts.size();
                            int end = index + 10;
                            for (int i = index; i < end; i++) {
                                Contact contact = new Contact();
                                contact.setEmail("DevExchanges" + i + "@gmail.com");
                                posts.add(contact);
                            }
                            PPAdapter.notifyDataSetChanged();
                            PPAdapter.setLoaded();
                        }
                    }, 1500);
                    */
                    } else {
                        //Toast.makeText(getActivity(), "Loading data completed", Toast.LENGTH_SHORT).show();
                    }

                }
            });

        }
    }

    //only called when we're not on pageNestedLevel 0, so when ascending up nested levels towards root level. so not called in root level (in root level we simply exit out of PostPage on UpButton click)
    public void backToParentPage(){

        currentRootLevelNode = currentRootLevelNode.getParent();

        while(currentRootLevelNode.hasHeadSibling()){
            currentRootLevelNode = currentRootLevelNode.getHeadSibling();
        }
        setCommentsPageWithCurrentNode();

    }

    public boolean isRootLevel(){
        return currentRootLevelNode == null || !currentRootLevelNode.hasParent();
    }

    public void refreshThenSetUpPage(final VSComment current){
        Runnable runnable = new Runnable() {
            public void run() {
                VSComment vscommentToQuery = new VSComment();
                vscommentToQuery.setPost_id(postID);

                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression().withHashKeyValues(vscommentToQuery);
                PaginatedQueryList<VSComment> result = ((MainContainer)getActivity()).getMapper().query(VSComment.class, queryExpression);
                result.loadAllResults();

                Iterator<VSComment> it = result.iterator();


                //below, we form the comment structure. each comment is a node in doubly linked list. So we only need to root comment that is at the top to traverse the comment tree to display all the comments.
                VSCNode firstParentNode = null; //holds the first parent node, which holds the comment that appears at the top of the hierarchy.
                VSCNode latestParentNode = null;  //holds the latest parent node we worked with. Used for assigning sibling order for parent nodes (root comments)
                nodeTable = new Hashtable(result.size());    //Hashtable to assist in assigning children/siblings.
                //TODO: Hashtable should be big enough to prevent collision as that fucks up the algorithm below. Right? Test if that's the case.
                while (it.hasNext()) {
                    VSCNode currNode = new VSCNode(it.next());
                    VSCNode pNode = null;   //temporary node holder
                    //TODO: figure out how to add siblings, child, parent and all that most efficiently
                    if(currNode.isRoot()){  //this is a parent node, AKA a root comment node
                        if(latestParentNode == null) {    //this is the first parent node to be worked with here
                            currentRootLevelNode = currNode;
                            firstRoot = currNode;
                            firstParentNode = currNode;
                            latestParentNode = currNode;
                        }
                        else{
                            latestParentNode.setTailSibling(currNode);
                            currNode.setHeadSibling(latestParentNode);
                            latestParentNode = currNode;
                        }
                        //nodeTable.put(currNode.getCommentID(), currNode); //since this is a parent node, KEY = Comment_ID
                    }
                    else { //this is a child node, AKA reply node
                        pNode = nodeTable.put(currNode.getParentID(), currNode);    //pNode holds whatever value was mapped for this key, if any, that is now overwritten

                        if(pNode.getParentID().trim().equals(currNode.getParentID().trim())) { //same parents => siblings
                            //currNode is not a first_child, so we need to assign some siblings here.
                            // pNode currently holds the Head Sibling for currNode. Therefore currNode is Tail Sibling of pNode
                            //head_sibling holds comment that is displayed immediately above the node, and tail_sibling holds comment that is displayed immediately below the node.
                            //TODO: Implement the following: this sibling order is determined by upvotes-downvotes score, and then timestamp as tie breaker (and in the rare occasion of a tie again, use username String lexical comparison)
                            //TODO: For now just use timestamp (default sort order of query result) to assign sibling order. Eventually this ordering has to reflect vote score and aforementioned tiebreakers.
                            pNode.setTailSibling(currNode);
                            currNode.setHeadSibling(pNode);
                        }
                        else{   //different parents => parent-child relationship detected => first child
                            pNode.setFirstChild(currNode);  //set currNode as first_child of its parentNode
                            currNode.setParent(pNode);
                        }
                    }
                    nodeTable.put(currNode.getCommentID(), currNode);   //add this node to the hash table so that its children, if any, can find it in the table
                }
                //TODO: vscomment table in ddb is sorted by timestamp. So a parent comment would always come before a reply comment, so sorting the list is not necessary. Confirm this, and think of any case where a reply may come before parent and cause an error while setting its parent due to parent node not yet existing because it was placed after the reply in the list.

                //TODO: obtain a list of comments we want to display here (root comments and upto their grandchildren). while making the list, set VSComment.nestedLevel to 0, 1, or 2 accordingly, for indent.
                //TODO: then create the recycler view, following Tab1Newsfeed.java's example

                //Below is a debugging algorithm to see if comment structure is correctly built. with indentation to indicate nested level
                //printNode(firstParentNode, 0);

                //iterate through root nodes and populate the vsComments list for use by recycler view (PostPageAdapter)
                if(firstParentNode != null){
                    setCommentList(firstParentNode);
                    while(firstParentNode.hasTailSibling()){
                        firstParentNode = firstParentNode.getTailSibling();
                        setCommentList(firstParentNode);
                    }
                }

                //run UI updates on UI Thread
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setCommentsPage(current);
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public Map<String, String> getActionMap(){
        return actionMap;
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
    public void applyUserActions(){
        VSCNode temp;
        for(Map.Entry<String, String> entry : actionMap.entrySet()){
            temp = nodeTable.get(entry.getKey());
            if(!temp.getCommentID().equals(entry.getKey())){ //this means we retrieved a child of the node we are looking for
                temp = getParent(temp); //now temp is the node we are looking for
            }
            if(temp == null){
                Log.d("DEBUG", "This is unexpected, this is a bug. PostPage.java line 801");
                return;
            }
            //now temp should be the node we want to work with

            switch(entry.getValue()){
                //TODO: we record and handle "N" case which is when user initially voted and then clicked it again to cancel the vote, keep the N until we decrement past vote and then it can be removed from actionmap after the decrement for DB is performed
                case "U":
                    temp.getNodeContent().initialSetUservote(UPVOTE);
                    break;
                case "D":
                    temp.getNodeContent().initialSetUservote(DOWNVOTE);
                    break;
            }
        }
    }

    //TODO:this is where we do user action synchronization I believe it would be called
    //Try to use this function for all action synchronization updates, because we do some stuff to keep track of synchronization
    public void writeActionsToDB(){
        if(currentUserAction != null) {
            Runnable runnable = new Runnable() {
                public void run() {
                    List<String> markedForRemoval = new ArrayList<>();


                    String tempString;
                    VSCNode tempNode;
                    boolean updateForNRemoval = false;

                    for (Map.Entry<String, String> entry : actionMap.entrySet()) {
                        tempString = actionHistoryMap.get(entry.getKey());

                        if (tempString == null) {
                            //just increment
                            HashMap<String, AttributeValue> keyMap =
                                    new HashMap<>();
                            keyMap.put("post_id", new AttributeValue().withS(postID));  //partition key
                            tempNode = nodeTable.get(entry.getKey());
                            if (!tempNode.getCommentID().equals(entry.getKey())) {
                                tempNode = getParent(tempNode);
                            }
                            keyMap.put("timestamp", new AttributeValue().withS(tempNode.getTimestamp()));   //sort key

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
                    else if(!tempString.equals("N")){
                        Log.d("debug", "so this happens");
                    }
                    */
                        else if (!tempString.equals(entry.getValue())) {
                            if (tempString.equals("N")) {
                                Log.d("DB update", "this is the source of error. we don't decrement opposite in this case");
                            }
                            //increment current and decrement past
                            HashMap<String, AttributeValue> keyMap =
                                    new HashMap<>();
                            keyMap.put("post_id", new AttributeValue().withS(postID));  //partition key
                            tempNode = nodeTable.get(entry.getKey());
                            if (!tempNode.getCommentID().equals(entry.getKey())) {
                                tempNode = getParent(tempNode);
                            }
                            keyMap.put("timestamp", new AttributeValue().withS(tempNode.getTimestamp()));   //sort key //TODO:sort key which we'll eventually change

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

                    if (!lastSubmittedVote.equals(currentUserAction.getVotedSide())) {
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

                        UpdateItemRequest request = new UpdateItemRequest()
                                .withTableName("post")
                                .withKey(keyMap)
                                .withAttributeUpdates(updates);

                        activity.getDDBClient().updateItem(request);

                        //update lastSubmittedVote
                        lastSubmittedVote = currentUserAction.getVotedSide();

                    }

                    //clean up stray "N" marks
                    for (String key : markedForRemoval) {
                        Log.d("DB Update", key + "removed from actionMap");
                        actionMap.remove(key);
                    }

                    if (!actionMap.isEmpty() || redIncrementedLast != blackIncrementedLast || updateForNRemoval) { //user made comment action(s) OR voted for a side in the post
                        activity.getMapper().save(currentUserAction, new DynamoDBMapperConfig(DynamoDBMapperConfig.SaveBehavior.CLOBBER));
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
        if(currentUserAction.getVotedSide().equals("BLK")){
            post.decrementBlackCount();
        }
        post.incrementRedCount();
        currentUserAction.setVotedSide("RED");
        redIncrementedLast = true;
        blackIncrementedLast = false;
    }

    public void blackVotePressed(){
        if(currentUserAction.getVotedSide().equals("RED")){
            post.decrementRedCount();
        }
        post.incrementBlackCount();
        currentUserAction.setVotedSide("BLK");
        blackIncrementedLast = true;
        redIncrementedLast = false;
    }

    public UserAction getUserAction(){
        return currentUserAction;
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
}
