package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.ActivePost;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.ThreadCounter;
import com.vs.bcd.versus.model.VSComment;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab2Trending extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<PostSkeleton> posts;
    private MyAdapter myAdapter;
    private boolean fragmentSelected = false; //marks if initial loading for this fragment was already done (as in, fragment was already selected once before if true). Used so that we don't load content every time the tab gets selected.
    private View rootView;
    private MainContainer mHostActivity;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    private boolean displayResults = false;
    private boolean nowLoading = false;
    private HashMap<Integer, Map<String,AttributeValue>> lastEvaluatedKeysMap = new HashMap<>();
    private RecyclerView recyclerView;
    private final Tab2Trending thisTab = this;
    private int numCategoriesToQuery = 24;  //this may change if user preference or Premium user option dictates removal of certain categories from getting queried and added to Newsfeed
    private int retrievalLimit = 10;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private int vcMultiplier = 1;
    private int ccMultiplier = 1;
    //the two booleans below are two-way dependency thing where, if xml loads first, we trigger initial loading animation in setUserVisibleHint (which is triggered when tab becomes visible)
    //and if setUserVisibleHint(true) is triggered before xml loads, then we mark that initial loading in progress and trigger initial loading animation during xml loading in onCreateView
    private boolean initialLoadInProgress = false;
    private boolean xmlLoaded = false;

    private int loadThreshold = 3;
    private int adFrequency = 25; //place interstitial ad after every 25 posts
    private int adCount = 0;

    private int NATIVE_APP_INSTALL_AD = 42069;
    private int NATIVE_CONTENT_AD = 69420;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab2trending, container, false);
        //TODO: need to add categories. maybe a separate categories table where post IDs have rows of categories they are linked with
        //TODO: create, at the right location, list of constant enumeration to represent categories. probably at post creation page, which is for now replaced by sample data creation below
        //mHostActivity.setToolbarTitleTextForTabs("Trending");

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container_tab2);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if(initialLoadInProgress){
            mSwipeRefreshLayout.setRefreshing(true);
        }
        xmlLoaded = true;

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
                //mHostActivity.setToolbarTitleTextForTabs("Trending");
                //toolbar title text now set in MainActivity addOnTabSelectedListener section
            }
            else{
                fragmentSelected = true;

                initialLoadInProgress = true;
                if(xmlLoaded){
                    mSwipeRefreshLayout.setRefreshing(true);
                }

                Runnable runnable = new Runnable() {
                    public void run() {

                        posts = trendingQuery();

                        Log.d("Query on Category: ", "posts set with " + Integer.toString(posts.size()) + " items");


                        //run UI updates on UI Thread
                        mHostActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //find view by id and attaching adapter for the RecyclerView
                                recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view2);

                                recyclerView.setLayoutManager(new LinearLayoutManager(mHostActivity));
                                //this is where the list is passed on to adapter
                                myAdapter = new MyAdapter(recyclerView, posts, mHostActivity, 0);
                                recyclerView.setAdapter(myAdapter);
                                initialLoadInProgress = false;
                                mSwipeRefreshLayout.setRefreshing(false);

                                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                    @Override
                                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                        LinearLayoutManager layoutManager=LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                                        int totalItemCount = layoutManager.getItemCount();
                                        int lastVisible = layoutManager.findLastVisibleItemPosition();

                                        boolean endHasBeenReached = lastVisible + loadThreshold >= totalItemCount;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                                        if (totalItemCount > 0 && endHasBeenReached) {
                                            //you have reached to the bottom of your recycler view
                                            if(!nowLoading){
                                                nowLoading = true;
                                                Log.d("Load", "Now Loadin More");
                                                loadMorePosts();
                                            }
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

    //returns newsfeed query result, sorted by date
    private ArrayList<PostSkeleton> trendingQuery(){
        final ArrayList<PostSkeleton> assembledResults = new ArrayList<>();
        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("-1"));

        final ThreadCounter threadCounter = new ThreadCounter(0, numCategoriesToQuery, this);
        for(int i = 0; i <  numCategoriesToQuery; i++){
            final int index = i;
            final double currTime = ((System.currentTimeMillis()/1000)/60);    //current time in minutes from epoch

            Runnable runnable = new Runnable() {
                public void run() {
                    ActivePost queryTemplate = new ActivePost();
                    queryTemplate.setCategory(index);
                    //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                    DynamoDBQueryExpression queryExpression =
                            new DynamoDBQueryExpression()
                                    .withIndexName("category-votecount-index")
                                    .withHashKeyValues(queryTemplate)
                                    .withRangeKeyCondition("votecount", rangeKeyCondition)
                                    .withScanIndexForward(false)
                                    //.withConsistentRead(true)
                                    .withLimit(retrievalLimit);

                    Log.d("Query on Category: ", Integer.toString(queryTemplate.getCategory()));
                    /*
                    PaginatedQueryList<ActivePost> queryResults = ((MainContainer)getActivity()).getMapper().query(ActivePost.class, queryExpression);
                    queryResults.loadAllResults();
                    assembledResults.addAll(queryResults);
                    */
                    QueryResultPage queryResultPage = ((MainContainer)getActivity()).getMapper().queryPage(ActivePost.class, queryExpression);
                    ArrayList<ActivePost> queryResults = new ArrayList<ActivePost>(queryResultPage.getResults());
                    assembledResults.addAll(queryResults);
                    //bookmark for the range key for loading more when user scrolls down far enough and triggers "load more action"
                    /*
                    for(int i = 0; i<queryResults.size(); i++){
                        Log.d("order", "category: " + queryResults.get(i).getCategoryString() + " votecount: " + Integer.toString(queryResults.get(i).getVotecount()));
                    }
                    */

                    if(queryResults.size() < retrievalLimit){
                        lastEvaluatedKeysMap.put(new Integer(index), null);
                    }
                    else{
                        //Log.d("Load: ", "retrieved " + Integer.toString(queryResults.size()) + " more items");
                        lastEvaluatedKeysMap.put(new Integer(index), queryResultPage.getLastEvaluatedKey());
                    }

                    //calculate and set the popularityVelocity for each retrieved ActivePost object
                    if(!queryResults.isEmpty()){
                        for (ActivePost ap : queryResults){
                            ap.setPopularityVelocity(
                                    ((ap.getVotecount() * vcMultiplier + ap.getCommentcount() * ccMultiplier))
                                            / (currTime - (double)(((ap.getDate().getTime())/1000)/60))
                            );
                        }
                    }

                    threadCounter.increment();
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();
        }

        long end = System.currentTimeMillis() + 10*1000; // 10 seconds * 1000 ms/sec
        //automatic timeout at 10 seconds to prevent infinite loop
        while(!displayResults && System.currentTimeMillis() < end){

        }
        if(displayResults){
            Log.d("Query on Category: ", "got through");
        }
        else{
            Log.d("Query on Category: ", "loop timeout");
        }

        threadCounter.terminateCounter();
        displayResults = false;

        //sort the assembledResults by popularity velocity
        Collections.sort(assembledResults, new Comparator<PostSkeleton>() {
            //TODO: confirm that this sorts dates where most recent is at top. If not then just flip around o1 and o2: change to o2.getDate().compareTo(o1.getDate())
            @Override
            public int compare(PostSkeleton o1, PostSkeleton o2) {
                double result = o2.getPopularityVelocity() - o1.getPopularityVelocity();
                if(result > 0){
                    return (int)Math.ceil(result);
                }
                if(result < 0){
                    return (int)Math.floor(result);
                }
                return 0;
            }
        });

        return assembledResults;
    }

    public void yesDisplayResults(){
        Log.d("Load", "displayResults set to true");

        displayResults = true;
    }

    private void loadMorePosts() {

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
                mSwipeRefreshLayout.setRefreshing(true);

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                try {
                    final ArrayList<PostSkeleton> assembledResults = new ArrayList<>();

                    final ThreadCounter threadCounter = new ThreadCounter(0, numCategoriesToQuery, thisTab);
                    final double currTime = ((System.currentTimeMillis()/1000)/60);    //current time in minutes from epoch

                    for(int i = 0; i <  numCategoriesToQuery; i++){
                        final Map<String,AttributeValue> leKey = lastEvaluatedKeysMap.get(new Integer(i));
                        if(leKey != null){
                            final Condition rangeKeyCondition = new Condition()
                                    .withComparisonOperator(ComparisonOperator.GT.toString())
                                    .withAttributeValueList(new AttributeValue().withN("-1"));

                            final int index = i;

                            Runnable runnable = new Runnable() {
                                public void run() {
                                    ActivePost queryTemplate = new ActivePost();
                                    queryTemplate.setCategory(index);
                                    //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                                    DynamoDBQueryExpression queryExpression =
                                            new DynamoDBQueryExpression()
                                                    .withIndexName("category-votecount-index")
                                                    .withHashKeyValues(queryTemplate)
                                                    .withRangeKeyCondition("votecount", rangeKeyCondition)
                                                    .withScanIndexForward(false)
                                                    //.withConsistentRead(true)
                                                    .withLimit(retrievalLimit)
                                                    .withExclusiveStartKey(leKey);

                                    Log.d("Query on Category: ", Integer.toString(queryTemplate.getCategory()));

                                    QueryResultPage queryResultPage = ((MainContainer)getActivity()).getMapper().queryPage(ActivePost.class, queryExpression);
                                    ArrayList<ActivePost> queryResults = new ArrayList<ActivePost>(queryResultPage.getResults());
                                    assembledResults.addAll(queryResults);
                                    //bookmark for the range key for loading more when user scrolls down far enough and triggers "load more action"

                                    if(queryResults.size() < retrievalLimit){
                                        lastEvaluatedKeysMap.put(new Integer(index), null);
                                    }
                                    else{
                                        //Log.d("Load: ", "retrieved " + Integer.toString(queryResults.size()) + " more items");
                                        lastEvaluatedKeysMap.put(new Integer(index), queryResultPage.getLastEvaluatedKey());
                                    }

                                    //calculate and set the popularityVelocity for each retrieved ActivePost object
                                    if(!queryResults.isEmpty()){
                                        for (ActivePost ap : queryResults){
                                            ap.setPopularityVelocity(
                                                    ((ap.getVotecount() * vcMultiplier + ap.getCommentcount() * ccMultiplier))
                                                            / (currTime - (double)(((ap.getDate().getTime())/1000)/60))
                                            );
                                        }
                                    }

                                    //}
                                    threadCounter.increment();
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }
                        else {
                            threadCounter.increment();
                        }
                    }

                    long end = System.currentTimeMillis() + 10*1000; // 10 seconds * 1000 ms/sec
                    //automatic timeout at 10 seconds to prevent infinite loop
                    while(!displayResults && System.currentTimeMillis() < end){

                    }
                    if(displayResults){
                        Log.d("Query on Category: ", "got through");
                    }
                    else{
                        Log.d("Query on Category: ", "loop timeout");
                    }

                    threadCounter.terminateCounter();
                    displayResults = false;

                    //sort the assembledResults by popularity velocity
                    Collections.sort(assembledResults, new Comparator<PostSkeleton>() {
                        //TODO: confirm that this sorts dates where most recent is at top. If not then just flip around o1 and o2: change to o2.getDate().compareTo(o1.getDate())
                        @Override
                        public int compare(PostSkeleton o1, PostSkeleton o2) {
                            double result = o2.getPopularityVelocity() - o1.getPopularityVelocity();
                            if(result > 0){
                                return (int)Math.ceil(result);
                            }
                            if(result < 0){
                                return (int)Math.floor(result);
                            }
                            return 0;
                        }
                    });

                    //if we don't add new materials from the loading operation, prevent future loading by setting nowLoading to true
                    //otherwise we set nowLoading false so that we can load more posts when conditions are met
                    if(!assembledResults.isEmpty()){
                        posts.addAll(assembledResults);
                        if(posts.size() / adFrequency > adCount){
                            PostSkeleton adSkeleton = new PostSkeleton();
                            NativeAd nextAd = mHostActivity.getNextAd();
                            if(nextAd != null){
                                if(nextAd instanceof NativeAppInstallAd){
                                    adSkeleton.setCategory(NATIVE_APP_INSTALL_AD);
                                    posts.add(adSkeleton);
                                    adCount++;
                                }
                                else if(nextAd instanceof NativeContentAd){
                                    adSkeleton.setCategory(NATIVE_CONTENT_AD);
                                    posts.add(adSkeleton);
                                    adCount++;
                                }
                            }
                        }
                        nowLoading = false;
                    }
                    else{
                        nowLoading = true;
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
            @Override
            protected void onProgressUpdate(String... values) {
                // TODO Auto-generated method stub
                super.onProgressUpdate(values);
                System.out.println("Progress : "  + values);
            }

            @Override
            protected void onPostExecute(String result)
            {
                recyclerView.getAdapter().notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        };
        _Task.execute((String[]) null);
    }

    /**
     * This method is called when swipe refresh is pulled down
     */
    @Override
    public void onRefresh() {

        // Fetching data from server
        refreshTrending();
    }

    private void refreshTrending(){
        adCount = 0;
        Log.d("Refresh", "Now Refreshing");
        mSwipeRefreshLayout.setRefreshing(true);
        lastEvaluatedKeysMap.clear();
        posts.clear();

        Runnable runnable = new Runnable() {
            public void run() {
                posts.addAll(trendingQuery());
                Log.d("Refresh", "Now posts has " + Integer.toString(posts.size()) + " items");

                mHostActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.getAdapter().notifyDataSetChanged();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });

            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();

        //lastRetrievedTime = assembledResults.get(assembledResults.size()-1).getTime();
        nowLoading = false;

    }


}