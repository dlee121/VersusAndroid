package com.vs.bcd.versus.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.PostPageAdapter;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.VSComment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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
    private Button submitButton;
    private RelativeLayout postRef, commentRef;
    private RelativeLayout.LayoutParams postRefLP, commentRefLP;
    private PostSkeleton post = null;
    private VSComment subjectComment = null;
    private String parentID = "0";
    private String postID;
    private String categoryInt;
    private long currSTL;
    private MainContainer activity;
    private String r = "";
    private String b = "";
    private String q = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.comment_enter_fragment , container, false);
        questionTV = (TextView)rootView.findViewById(R.id.postquestion);
        vsX = (TextView)rootView.findViewById(R.id.vsx);
        vsY = (TextView)rootView.findViewById(R.id.vsy);
        submitButton = (Button)rootView.findViewById(R.id.submitButton);
        postRef = (RelativeLayout)rootView.findViewById(R.id.postref);
        commentRef = (RelativeLayout)rootView.findViewById(R.id.commentref);
        postRefLP = (RelativeLayout.LayoutParams)postRef.getLayoutParams();
        commentRefLP = (RelativeLayout.LayoutParams)commentRef.getLayoutParams();
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: form validation (although most strings should be acceptable as comments anyway)
                Runnable runnable = new Runnable() {
                    public void run() {
                        final VSComment vsc = new VSComment();
                        vsc.setParent_id(parentID);  //TODO: for root/reply check, which would be more efficient, checking if parent_id == "0" or checking parent_id.length() == 1?
                        vsc.setPost_id(postID);
                        vsc.setAuthor(((MainContainer)getActivity()).getSessionManager().getCurrentUsername());
                        vsc.setContent(((TextView)(rootView.findViewById(R.id.commentInput))).getText().toString().trim());
                        vsc.setR(r);
                        vsc.setB(b);
                        vsc.setQ(q);

                        activity.getMapper().save(vsc);

                        //increment commentcount
                        HashMap<String, AttributeValue> keyMap = new HashMap<>();
                        keyMap.put("category", new AttributeValue().withN(categoryInt));  //partition key
                        keyMap.put("post_id", new AttributeValue().withS(postID));   //sort key

                        HashMap<String, AttributeValueUpdate> updates = new HashMap<>();

                        AttributeValueUpdate avu;

                        avu = new AttributeValueUpdate().withValue(new AttributeValue().withN("1")).withAction(AttributeAction.ADD);
                        updates.put("commentcount", avu);

                        UpdateItemRequest request = new UpdateItemRequest()
                                .withTableName("post")
                                .withKey(keyMap)
                                .withAttributeUpdates(updates);
                        activity.getDDBClient().updateItem(request);

                        //if current post's STL is not expired (STL is still in future), then update commentcount for active_post counterpart as well
                        if(System.currentTimeMillis()/1000 < currSTL){
                            //Log.d("STL", Long.toString(currSTL));
                            request = new UpdateItemRequest()
                                    .withTableName("active_post")
                                    .withKey(keyMap)
                                    .withAttributeUpdates(updates);
                            activity.getDDBClient().updateItem(request);
                        }

                        //update DB User.posts list with the new postID String
                        /*  retired the attribute but keeping this code as reference for list_append
                        UpdateItemRequest commentsListUpdateRequest = new UpdateItemRequest();
                        commentsListUpdateRequest.withTableName("user")
                                .withKey(Collections.singletonMap("username",
                                        new AttributeValue().withS(activity.getUsername())))
                                .withUpdateExpression("SET comments = list_append(comments, :val)")
                                .withExpressionAttributeValues(
                                        Collections.singletonMap(":val",
                                                new AttributeValue().withL(new AttributeValue().withS(vsc.getComment_id()))
                                        )
                                );
                        activity.getDDBClient().updateItem(commentsListUpdateRequest);
                        */

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
                                ((MainContainer)getActivity()).getViewPager().setCurrentItem(3);    //3 -> PostPage

                            }
                        });
                    }
                };
                Thread mythread = new Thread(runnable);
                mythread.start();
                Log.d("VSCOMMENT", "VSComment submitted");
            }
        });

        activity = (MainContainer)getActivity();

        disableChildViews();
        return rootView;
    }

    public void setContentReplyToPost(String question, String x, String y, PostSkeleton post){
        questionTV.setText(question);
        vsX.setText(x);
        vsY.setText(y);
        this.post = post;
        postID = post.getPost_id();
        parentID = postID;
        categoryInt = post.getCategoryIntAsString();
        currSTL = post.getStl();
        subjectComment = null;
        showPostRef();
        r = x;
        b = y;
        q = question;
    }

    public void setContentReplyToComment(VSComment replySubject){
        //TODO: start setting up user profile pic whereever it appears
        ((TextView)commentRef.findViewById(R.id.usernameref)).setText(replySubject.getAuthor());
        ((TextView)commentRef.findViewById(R.id.timetvref)).setText(getTimeString(replySubject.getTime()));
        ((TextView)commentRef.findViewById(R.id.usercommentref)).setText(replySubject.getContent());
        parentID = replySubject.getComment_id();
        subjectComment = replySubject;
        post = null;
        PostPage postPage = activity.getPostPage();
        categoryInt = postPage.getCatNumString();
        currSTL = postPage.getCurrPostSTL();
        postID = postPage.getPostPagePostID();
        r = postPage.getR();
        b = postPage.getB();
        q = postPage.getQ();

        showCommentRef();
    }

    public void showCommentRef(){
        commentRef.setEnabled(true);
        commentRef.setLayoutParams(commentRefLP);
        postRef.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        postRef.setEnabled(false);
    }

    public void showPostRef(){
        postRef.setEnabled(true);
        postRef.setLayoutParams(postRefLP);
        commentRef.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        commentRef.setEnabled(false);
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




    public String getTimeString(String timeStamp){
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
}


