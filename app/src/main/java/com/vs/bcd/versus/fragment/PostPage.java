package com.vs.bcd.versus.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.VSCNode;
import com.vs.bcd.versus.model.VSComment;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import static com.vs.bcd.versus.R.id.commentInput;
import static com.vs.bcd.versus.R.id.submitCommentButton;


/**
 * Created by dlee on 6/7/17.
 */

public class PostPage extends Fragment {

    private EditText commentInput;
    private RelativeLayout mRelativeLayout;
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private String postID = "";
    private SessionManager sessionManager;
    private List<VSCNode> vscNodes = new ArrayList<>(); //ArrayList of VSCNode

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.post_page, container, false);
        commentInput = (EditText) rootView.findViewById(R.id.commentInput);
        mRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.post_page_layout);

        sessionManager = new SessionManager(getActivity());

        Button commentSubmitButton = (Button) rootView.findViewById(R.id.submitCommentButton);
        commentSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: this whole thing on the bottom is bullshit because we need to submit the text to DB, not just simply display it. write to db, then refresh to display the comment.
                //TODO: look into perioding cheap synching scheme to keep comments updated realtime
                /*
                TextView tv = new TextView(getActivity());
                tv.setText(commentInput.getText());
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                params.leftMargin = 5; //5dp for root comment. replies get +5dp per level
                mRelativeLayout.addView(tv, params);
                */
            }
        });

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            if(childViews.get(i) instanceof EditText){
                LPStore.add(childViews.get(i).getLayoutParams());
            }
            else{
                LPStore.add(null);
            }
        }
        disableChildViews();

        //root comment submission function to execute when submit button is pressed
        rootView.findViewById(R.id.submitCommentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: form validation (although most strings should be acceptable as comments anyway)
                Runnable runnable = new Runnable() {
                    public void run() {
                        VSComment vsc = new VSComment();
                        vsc.setPost_id(postID);
                        vsc.setParent_id("0");  //TODO: for root/reply check, which would be more efficient, checking if parent_id == "0" or checking parent_id.length() == 1?
                        vsc.setAuthor(sessionManager.getCurrentUsername());
                        vsc.setContent(((TextView)(rootView.findViewById(R.id.commentInput))).getText().toString().trim());
                        ((MainContainer)getActivity()).getMapper().save(vsc);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //TODO: refresh comments. Eventually make it efficient. For now, just grabbing every comments belonging to currently displayed post

                            }
                        });
                    }
                };
                Thread mythread = new Thread(runnable);
                mythread.start();
                Log.d("VSCOMMENT", "VSComment submitted");
            }
        });



        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "SEARCH VISIBLE");
            //TODO: get comments from DB, create the comment structure and display it here. Actually we're doing that in setContent() right now



            if(rootView != null)
                enableChildViews();
        }
        else {
            Log.d("VISIBLE", "SEARCH POST GONE");
            if (rootView != null)
                disableChildViews();
        }
    }

    public void enableChildViews(){
        for(int i = 0; i<childViews.size(); i++){

            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            if(childViews.get(i) instanceof EditText){
                childViews.get(i).setLayoutParams(LPStore.get(i));
            }

        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            if(childViews.get(i) instanceof EditText){
                childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
            }

        }
    }

    public void setContent(Post post){
        ((TextView)(rootView.findViewById(R.id.post_page_question))).setText(post.getQuestion());
        ((TextView)(rootView.findViewById(R.id.post_page_redname))).setText(post.getRedname());
        ((TextView)(rootView.findViewById(R.id.post_page_blackname))).setText(post.getBlackname());
        ((TextView)(rootView.findViewById(R.id.post_page_redcount))).setText(Integer.toString(post.getRedcount()));
        ((TextView)(rootView.findViewById(R.id.post_page_blackcount))).setText(Integer.toString(post.getBlackcount()));
        postID = post.getPost_id();

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
                Hashtable<String, VSCNode> nodeTable = new Hashtable(result.size());    //Hashtable to assist in assigning children/siblings.
                //TODO: Hashtable should be big enough to prevent collision as that fucks up the algorithm below. Right? Test if that's the case.
                while (it.hasNext()) {
                    VSCNode currNode = new VSCNode(it.next());
                    VSCNode pNode = null;   //temporary node holder
                    //TODO: figure out how to add siblings, child, parent and all that most efficiently
                    if(currNode.isRoot()){  //this is a parent node, AKA a root comment node
                        if(latestParentNode == null) {    //this is the first parent node to be worked with here
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

            //Below is a debugging algorithm to see if comment structure is correctly built. with indentation to indicate nested level
            printNode(firstParentNode, 0);
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


}
