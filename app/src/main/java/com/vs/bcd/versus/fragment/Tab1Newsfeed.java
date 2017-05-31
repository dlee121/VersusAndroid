package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.Post;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab1Newsfeed extends Fragment{

    private List<Post> posts;
    private MyAdapter myAdapter;
    private boolean fragmentSelected = false;
    private View rootView;
    private MainContainer mHostActivity;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab1newsfeed, container, false);
        //TODO: need to add categories. maybe a separate categories table where post IDs have rows of categories they are linked with
        //TODO: create, at the right location, list of constant enumeration to represent categories. probably at post creation page, which is for now replaced by sample data creation below
        mHostActivity.setToolbarTitleText("Newsfeed");


        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //save the activity to a member of this fragment
        mHostActivity = (MainContainer)context;
    }



    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            if(fragmentSelected) {
                mHostActivity.setToolbarTitleText("Newsfeed");
            }
            else{
                fragmentSelected = true;

                Runnable runnable = new Runnable() {
                    public void run() {

                        //DynamoDB calls go here
                        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
                        DynamoDBMapperConfig config = new DynamoDBMapperConfig(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING);
                        PaginatedScanList<Post> result = mHostActivity.getMapper().scan(Post.class, scanExpression, config);
                        result.loadAllResults();
                        posts = new ArrayList<>(result.size());
                        Iterator<Post> it = result.iterator();
                        while (it.hasNext()) {
                            Post element = it.next();
                            posts.add(element);
                        }
                        //run UI updates on UI Thread
                        mHostActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //find view by id and attaching adapter for the RecyclerView
                                RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

                                recyclerView.setLayoutManager(new LinearLayoutManager(mHostActivity));
                                //this is where the list is passed on to adapter
                                myAdapter = new MyAdapter(recyclerView, posts, mHostActivity);
                                recyclerView.setAdapter(myAdapter);

                                //set load more listener for the RecyclerView adapter
                                myAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                                    @Override
                                    public void onLoadMore() {

                                        if (posts.size() <= 3) {
                        /*
                        posts.add(null);
                        myAdapter.notifyItemInserted(posts.size() - 1);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                posts.remove(posts.size() - 1);
                                myAdapter.notifyItemRemoved(posts.size());

                                //Generating more data
                                int index = posts.size();
                                int end = index + 10;
                                for (int i = index; i < end; i++) {
                                    Contact contact = new Contact();
                                    contact.setEmail("DevExchanges" + i + "@gmail.com");
                                    posts.add(contact);
                                }
                                myAdapter.notifyDataSetChanged();
                                myAdapter.setLoaded();
                            }
                        }, 1500);
                        */
                                        } else {
                                            Toast.makeText(mHostActivity, "Loading data completed", Toast.LENGTH_SHORT).show();
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
        }
    }
}