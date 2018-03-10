package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CEFAdapter;
import com.vs.bcd.versus.model.CEFObject;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.VSComment;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dlee on 7/1/17.
 */

public class CommentEnterFragment extends Fragment{
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private TextView questionTV, vsX, vsY;
    private RelativeLayout postRef, commentRef;
    private RelativeLayout.LayoutParams postRefLP, commentRefLP;
    private Post post = null;
    private VSComment subjectComment = null;
    private String parentID = "0";
    private String postID;
    private String categoryInt;
    private long currSTL;
    private MainContainer activity;
    private String r = "";
    private String b = "";
    private String q = "";
    private double commentPSI = 3.0; //ps increment per comment
    private EditText textInput;
    private RecyclerView recyclerView;
    private CEFAdapter cefAdapter;
    private ArrayList<CEFObject> cefObjectList;
    private Toast mToast;
    private String prefix = "";
    private int pageLevelTarget = 0;

    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.comment_enter_fragment , container, false);

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        cefObjectList = new ArrayList<>();
        recyclerView = rootView.findViewById(R.id.recycler_view_ch);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        cefAdapter = new CEFAdapter(cefObjectList, activity);
        recyclerView.setAdapter(cefAdapter);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        disableChildViews();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
    }

    public void setContentReplyToPost(Post post){
        prefix = "";
        pageLevelTarget = 0;
        cefObjectList.clear();
        cefObjectList.add(new CEFObject(post));
        cefObjectList.add(new CEFObject()); //add text input card view
        cefAdapter.notifyDataSetChanged();

        this.post = post;
        postID = post.getPost_id();
        subjectComment = null;
        parentID = postID;
    }

    public void setContentReplyToComment(VSComment replySubject, int pageLevelTarget){
        prefix = "";
        this.pageLevelTarget = pageLevelTarget;
        cefObjectList.clear();
        cefObjectList.add(new CEFObject(replySubject));
        cefObjectList.add(new CEFObject()); //add text input card view
        cefAdapter.notifyDataSetChanged();

        parentID = replySubject.getComment_id();
        subjectComment = replySubject;
        post = null;

        postID = replySubject.getPost_id();
    }

    public void setContentReplyToComment(VSComment replySubject, int pageLevelIncrement, String parentID, String prefix){
        this.prefix = prefix;
        cefObjectList.clear();
        cefObjectList.add(new CEFObject(replySubject));
        cefObjectList.add(new CEFObject()); //add text input card view
        cefAdapter.notifyDataSetChanged();

        this.parentID = parentID;
        subjectComment = replySubject;
        post = null;

        postID = replySubject.getPost_id();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "SEARCH VISIBLE");
            if(rootView != null){
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                enableChildViews();
            }
        }
        else {
            Log.d("VISIBLE", "SEARCH POST GONE");
            if (rootView != null){
                disableChildViews();
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

    private String sanitizeContentForURL(String url){
        String strIn = url.trim();
        if(strIn.length()>26){
            strIn.substring(0,26);
        }
        return strIn.trim().replaceAll("[ /\\\\.\\\\$\\[\\]\\\\#]", "^").replaceAll(":", ";");
    }

    public boolean submitButtonPressed(){

        final String inputString = cefAdapter.getTextInput();

        if(inputString != null && inputString.length() > 0){
            activity.showToolbarProgressbar();

            Runnable runnable = new Runnable() {
                public void run() {

                    final VSComment vsc = new VSComment();
                    vsc.setParent_id(parentID);  //TODO: for root/reply check, which would be more efficient, checking if parent_id == "0" or checking parent_id.length() == 1?
                    vsc.setPost_id(postID);
                    vsc.setAuthor(activity.getUsername());
                    vsc.setContent(prefix + inputString);
                    vsc.setIsNew(true); //sets it to be highlighted

                    activity.getMapper().save(vsc);

                    //send appropriate notification
                    if(post != null && !post.getAuthor().equals("[deleted]")){    //if root comment
                        String nKey = postID+":"+sanitizeContentForURL(post.getRedname())+":"+sanitizeContentForURL(post.getBlackname())+":"+sanitizeContentForURL(post.getQuestion());
                        String postAuthorPath = getUsernameHash(post.getAuthor()) + "/" + post.getAuthor() + "/n/r/" + nKey;
                        mFirebaseDatabaseReference.child(postAuthorPath).push().setValue(System.currentTimeMillis()/1000);  //set value = timestamp as seconds from epoch
                    }
                    else if(subjectComment != null && !subjectComment.getAuthor().equals("[deleted]")){   //else this is a reply to a comment
                        String payloadContent = sanitizeContentForURL(subjectComment.getContent());

                        String subjectAuthorPath = getUsernameHash(subjectComment.getAuthor()) + "/" + subjectComment.getAuthor() + "/n/c/"
                                + subjectComment.getParent_id() + ":" + subjectComment.getComment_id() + ":" + payloadContent;
                        mFirebaseDatabaseReference.child(subjectAuthorPath).push().setValue(System.currentTimeMillis()/1000);

                    }

                    //increment commentcount
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

                    //update DB User.posts list with the new postID String



                    PostPage postPage = activity.getPostPage();
                    if(pageLevelTarget > 0){
                        postPage.setPageLevel(pageLevelTarget);
                    }

                    postPage.commentSubmissionRefresh(vsc);

                    //UI refresh. two options, one for setting up with post card and one for setting up with comment top card
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //hiding progress bar called in commentSubmissionRefresh
                            cefAdapter.clearTextInput();
                        }
                    });

                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();
        }
        else{
            if(mToast != null){
                mToast.cancel();
            }
            if(subjectComment != null){
                mToast = Toast.makeText(activity, "Please enter a reply", Toast.LENGTH_SHORT);
            }
            else{
                mToast = Toast.makeText(activity, "Please enter a comment", Toast.LENGTH_SHORT);
            }
            mToast.show();
        }



        return inputString != null && inputString.length() > 0;
    }

    public void clearTextInput(){
        if(cefAdapter != null){
            cefAdapter.clearTextInput();
        }
    }
}

