package com.vs.bcd.versus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Handler;
import android.widget.Toast;
import android.app.Activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.*;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab1Newsfeed extends Fragment{

    private List<Post> posts;
    private MyAdapter myAdapter;
    private boolean dataLoaded = false;
    private View rootView;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab1newsfeed, container, false);
        //TODO: need to add categories. maybe a separate categories table where post IDs have rows of categories they are linked with
        //TODO: this is where I need to get data from database and construct list of Post objects
        //TODO: create, at the right location, list of constant enumeration to represent categories. probably at post creation page, which is for now replaced by sample data creation below
        //for now, create sample data

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity().getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        final DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

        Runnable runnable = new Runnable() {
            public void run() {
                //DynamoDB calls go here
                DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
                DynamoDBMapperConfig config = new DynamoDBMapperConfig(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING);
                PaginatedScanList<Post> result = mapper.scan(Post.class, scanExpression, config);
                result.loadAllResults();
                posts = new ArrayList<>(result.size());
                Iterator<Post> it = result.iterator();
                while (it.hasNext()){
                    Post element = it.next();
                    posts.add(element);
                }
                //run UI updates on UI Thread
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //find view by id and attaching adapter for the RecyclerView
                        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

                        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                        //this is where the list is passed on to adapter
                        myAdapter = new MyAdapter(recyclerView, posts, getActivity());
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
        return rootView;
    }
}