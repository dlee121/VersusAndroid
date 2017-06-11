package com.vs.bcd.versus.fragment;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.post_page, container, false);
        commentInput = (EditText) rootView.findViewById(R.id.commentInput);
        mRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.post_page_layout);

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

        return rootView;
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

    public void commentSubmitButtonPressed(){



    }


}
