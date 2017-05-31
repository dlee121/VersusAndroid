package com.vs.bcd.versus.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.vs.bcd.versus.R;

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




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.create_post, container, false);
        rednameET = (EditText)rootView.findViewById(R.id.redname_in);
        blacknameET = (EditText)rootView.findViewById(R.id.blackname_in);
        questionET = (EditText)rootView.findViewById(R.id.question_in);
        categoryET = (EditText)rootView.findViewById(R.id.category_in);
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "CREATE POST VISIBLE");
        }
        else
            Log.d("VISIBLE", "CREATE POST GONE");
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
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                post.setTime(df.format(new Date()));
                post.setAuthor("DEEKS");
                post.setRedname(redStr);
                post.setBlackname(blackStr);
                post.setQuestion(questiongStr);
                ((MainContainer)getActivity()).getMapper().save(post);
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }


}
