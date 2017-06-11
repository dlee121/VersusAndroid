package com.vs.bcd.versus.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.Post;

/**
 * Created by dlee on 5/19/17.
 */

public class CreatePost extends Fragment {

    private EditText rednameET;
    private EditText blacknameET;
    private EditText questionET;
    private EditText categoryET;
    private String redStr;
    private String blackStr;
    private String questiongStr;
    private String catStr;
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;

    private SessionManager sessionManager;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.create_post, container, false);
        rednameET = (EditText)rootView.findViewById(R.id.redname_in);
        blacknameET = (EditText)rootView.findViewById(R.id.blackname_in);
        questionET = (EditText)rootView.findViewById(R.id.question_in);
        categoryET = (EditText)rootView.findViewById(R.id.category_in);
        sessionManager = new SessionManager(getActivity());
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

    public void createButtonPressed(View view){
        //this is where you validate data and, if valid, write to database
        //TODO: validate submission here
        redStr = rednameET.getText().toString();
        blackStr = blacknameET.getText().toString();
        questiongStr = questionET.getText().toString();
        catStr = categoryET.getText().toString();

        Runnable runnable = new Runnable() {
            public void run() {
                Post post = new Post();
                post.setCategory(catStr);
                /*
                //time is now set in the constructor. refer to Post.java

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                post.setTime(df.format(new Date()));
                */
                post.setAuthor(sessionManager.getUserDetails().get(SessionManager.KEY_USERNAME));
                post.setRedname(redStr);
                post.setBlackname(blackStr);
                post.setQuestion(questiongStr);
                ((MainContainer)getActivity()).getMapper().save(post);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainContainer) getActivity()).getViewPager().setCurrentItem(1);   //once create post submits, we move back to MainActivity (tabs activity)
                        Intent intent = new Intent(getActivity(), MainContainer.class);
                        startActivity(intent);  //go on to the next activity, MainContainer
                        getActivity().overridePendingTransition(0, 0);
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "CREATE POST VISIBLE");
            if(rootView != null)
                enableChildViews();
        }
        else {
            Log.d("VISIBLE", "CREATE POST GONE");
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
}
