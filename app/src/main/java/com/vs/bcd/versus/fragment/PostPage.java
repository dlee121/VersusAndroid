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

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.VSComment;

import java.util.ArrayList;

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
            //TODO: populate page with post info here



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
    }


}
