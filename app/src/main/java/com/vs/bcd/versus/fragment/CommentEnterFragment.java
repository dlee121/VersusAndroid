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
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CEFAdapter;
import com.vs.bcd.versus.adapter.CommentHistoryAdapter;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.CEFObject;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.VSComment;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

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

        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

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
        cefObjectList.clear();
        cefObjectList.add(new CEFObject(post));
        cefObjectList.add(new CEFObject()); //add text input card view
        cefAdapter.notifyDataSetChanged();

        this.post = post;
        postID = post.getPost_id();
        subjectComment = null;
        parentID = postID;
    }

    public void setContentReplyToComment(VSComment replySubject){
        cefObjectList.clear();
        cefObjectList.add(new CEFObject(replySubject));
        cefObjectList.add(new CEFObject()); //add text input card view
        cefAdapter.notifyDataSetChanged();

        parentID = replySubject.getComment_id();
        subjectComment = replySubject;
        post = null;

        postID = replySubject.getPost_id();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "SEARCH VISIBLE");
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

    public void submitButtonPressed(){

        //TODO: form validation (although most strings should be acceptable as comments anyway)
        Runnable runnable = new Runnable() {
            public void run() {
                String inputString = cefAdapter.getTextInput();
                if(inputString != null && inputString.length() > 0){

                    final VSComment vsc = new VSComment();
                    vsc.setParent_id(parentID);  //TODO: for root/reply check, which would be more efficient, checking if parent_id == "0" or checking parent_id.length() == 1?
                    vsc.setPost_id(postID);
                    vsc.setAuthor(activity.getUsername());
                    vsc.setContent(inputString);

                    activity.getMapper().save(vsc);

                    //send appropriate notification
                    if(post != null){    //if root comment
                        String nKey = postID+":"+sanitizeContentForURL(post.getRedname())+":"+sanitizeContentForURL(post.getBlackname())+":"+sanitizeContentForURL(post.getQuestion());
                        String postAuthorPath = getUsernameHash(post.getAuthor()) + "/" + post.getAuthor() + "/n/r/" + nKey;
                        mFirebaseDatabaseReference.child(postAuthorPath).push().setValue(System.currentTimeMillis()/1000);  //set value = timestamp as seconds from epoch
                    }
                    else if(subjectComment != null){   //else this is a reply to a comment
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

                    //UI refresh. two options, one for setting up with post card and one for setting up with comment top card
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            PostPage postPage = ((MainContainer)getActivity()).getPostPage();
                            PostPageAdapter m_adapter = postPage.getPPAdapter();
                            m_adapter.clearList();
                            //m_adapter.notifyDataSetChanged(); probably unnecessary since we'll be making new adapter in post page in setContent
                            if(subjectComment == null && post != null){
                                postPage.setContent(post, false);
                            }
                            if(post == null && subjectComment != null){
                                postPage.hidePostPageFAB();
                                postPage.setCommentsPage(subjectComment);
                            }

                            //TODO: refresh comments list (but not the post info part) of the PostPage when we return to it here
                            activity.getViewPager().setCurrentItem(3);    //3 -> PostPage
                            cefAdapter.clearTextInput();

                        }
                    });

                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
        //Log.d("VSCOMMENT", "VSComment submitted");

    }

    public void clearTextInput(){
        if(cefAdapter != null){
            cefAdapter.clearTextInput();
        }
    }
}


