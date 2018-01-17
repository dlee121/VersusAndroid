package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.ArrayAdapterWithIcon;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostSkeleton;

/**
 * Created by dlee on 9/8/17.
 */

public class CategoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<PostSkeleton> posts = new ArrayList<>();
    private MyAdapter myAdapter;
    private View rootView;
    private MainContainer mHostActivity;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    private boolean nowLoading = false;
    private HashMap<Integer, Map<String,AttributeValue>> lastEvaluatedKeysMap = new HashMap<>();
    private Map<String,AttributeValue> lastEvaluatedKey;
    private RecyclerView recyclerView;
    private int retrievalLimit = 10;    //TODO: play with this number through testing with usecases. for best UX while minimizing cost.
    private SwipeRefreshLayout mSwipeRefreshLayout;
    //the two booleans below are two-way dependency thing where, if xml loads first, we trigger initial loading animation in setUserVisibleHint (which is triggered when tab becomes visible)
    //and if setUserVisibleHint(true) is triggered before xml loads, then we mark that initial loading in progress and trigger initial loading animation during xml loading in onCreateView
    private boolean initialLoadInProgress = false;
    private boolean xmlLoaded = false;
    private int currCategoryInt = 0;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private int sortType = 0; //0 = New, 1 = Popular
    private final int NEW = 0;
    private final int POPULAR = 1;

    private int loadThreshold = 3;
    private int adFrequency = 25; //place interstitial ad after every 25 posts
    private int adCount = 0;

    private int NATIVE_APP_INSTALL_AD = 42069;
    private int NATIVE_CONTENT_AD = 69420;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.category_fragment, container, false);
        //TODO: need to add categories. maybe a separate categories table where post IDs have rows of categories they are linked with
        //TODO: create, at the right location, list of constant enumeration to represent categories. probably at post creation page, which is for now replaced by sample data creation below
        //mHostActivity.setToolbarTitleTextForTabs("Trending");

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container_catfrag);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if(initialLoadInProgress){
            mSwipeRefreshLayout.setRefreshing(true);
        }
        xmlLoaded = true;

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fabcf);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHostActivity.getCreatePostFragment().resetCatSelection();
                mHostActivity.getViewPager().setCurrentItem(2);
                mHostActivity.getToolbarTitleText().setText("Create Post");
                mHostActivity.getToolbarButtonLeft().setImageResource(R.drawable.ic_left_chevron);
            }
        });


        Button sortTypeSelector = (Button) rootView.findViewById(R.id.sort_type_selector);
        sortTypeSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String [] items = new String[] {"Popular", "New"};
                final Integer[] icons = new Integer[] {R.drawable.goldmedal, R.drawable.goldmedal}; //TODO: change these icons to actual ones
                ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(), items, icons);

                new AlertDialog.Builder(getActivity()).setTitle("Sort by")
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item ) {
                                switch(item){
                                    case 0: //Sort by Popular; category-votecount-index query.
                                        Log.d("SortType", "sort by votecount");
                                        sortType = POPULAR;
                                        refreshCategoryVotecountQuery();
                                        break;

                                    case 1: //Sort by New; category-time-index query.
                                        Log.d("SortType", "sort by time");
                                        sortType = NEW;
                                        refreshCategoryTimeQuery();
                                        break;
                                }
                            }
                        }).show();
            }
        });


        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }
        disableChildViews();

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

    //grabs newest posts in the category sorted by time
    public void categoryTimeQuery(final int categoryInt){
        //Log.d("CATFRAG", "category pressed for catfrag");
        sortType = NEW;

        currCategoryInt = categoryInt;

        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.LT.toString())
                .withAttributeValueList(new AttributeValue().withS(df.format(new Date())));

        initialLoadInProgress = true;
        if(xmlLoaded){
            mSwipeRefreshLayout.setRefreshing(true);
        }

        Runnable runnable = new Runnable() {
            public void run() {
                Post queryTemplate = new Post();
                queryTemplate.setCategory(currCategoryInt);
                //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                DynamoDBQueryExpression queryExpression =
                        new DynamoDBQueryExpression()
                                .withIndexName("category-time-index")
                                .withHashKeyValues(queryTemplate)
                                .withRangeKeyCondition("time", rangeKeyCondition)
                                .withScanIndexForward(false)
                                //.withConsistentRead(true)
                                .withLimit(retrievalLimit);

                QueryResultPage queryResultPage = ((MainContainer)getActivity()).getMapper().queryPage(Post.class, queryExpression);
                ArrayList<Post> queryResults = new ArrayList<>(queryResultPage.getResults());

                if(queryResults.size() < retrievalLimit){
                    lastEvaluatedKey = null;
                }
                else{
                    lastEvaluatedKey = queryResultPage.getLastEvaluatedKey();
                }

                if(!queryResults.isEmpty()){
                    //sort the assembledResults where posts are sorted from more recent to less recent
                    Collections.sort(queryResults, new Comparator<Post>() {
                        //TODO: confirm that this sorts dates where most recent is at top. If not then just flip around o1 and o2: change to o2.getDate().compareTo(o1.getDate())
                        @Override
                        public int compare(Post o1, Post o2) {
                            return o2.getDate().compareTo(o1.getDate());
                        }
                    });

                    posts.addAll(queryResults);

                    mHostActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //find view by id and attaching adapter for the RecyclerView
                            recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_cf);

                            recyclerView.setLayoutManager(new LinearLayoutManager(mHostActivity));
                            //this is where the list is passed on to adapter
                            myAdapter = new MyAdapter(recyclerView, posts, mHostActivity, 6);
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
                                            loadMorePostsByTime();
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
                else{
                    mHostActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    //grabs newest posts in the category sorted by time
    public void categoryVotecountQuery(final int categoryInt){
        //Log.d("CATFRAG", "category pressed for catfrag");
        sortType = POPULAR;

        currCategoryInt = categoryInt;

        final Condition rangeKeyCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withN("-1"));

        initialLoadInProgress = true;
        if(xmlLoaded){
            mSwipeRefreshLayout.setRefreshing(true);
        }

        Runnable runnable = new Runnable() {
            public void run() {
                Post queryTemplate = new Post();
                queryTemplate.setCategory(currCategoryInt);
                //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                DynamoDBQueryExpression queryExpression =
                        new DynamoDBQueryExpression()
                                .withIndexName("category-votecount-index")
                                .withHashKeyValues(queryTemplate)
                                .withRangeKeyCondition("votecount", rangeKeyCondition)
                                .withScanIndexForward(false)
                                //.withConsistentRead(true)
                                .withLimit(retrievalLimit);

                QueryResultPage queryResultPage = ((MainContainer)getActivity()).getMapper().queryPage(Post.class, queryExpression);
                ArrayList<Post> queryResults = new ArrayList<>(queryResultPage.getResults());

                if(queryResults.size() < retrievalLimit){
                    lastEvaluatedKey = null;
                }
                else{
                    lastEvaluatedKey = queryResultPage.getLastEvaluatedKey();
                }

                if(!queryResults.isEmpty()){
                    //sort the assembledResults where posts are sorted from more recent to less recent
                    Collections.sort(queryResults, new Comparator<Post>() {
                        @Override
                        public int compare(Post o1, Post o2) {
                            return o2.getVotecount() - o1.getVotecount();
                        }
                    });

                    posts.addAll(queryResults);

                    mHostActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //find view by id and attaching adapter for the RecyclerView
                            recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_cf);

                            recyclerView.setLayoutManager(new LinearLayoutManager(mHostActivity));
                            //this is where the list is passed on to adapter
                            myAdapter = new MyAdapter(recyclerView, posts, mHostActivity, 6);
                            recyclerView.setAdapter(myAdapter);
                            initialLoadInProgress = false;
                            mSwipeRefreshLayout.setRefreshing(false);

                            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                @Override
                                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                    LinearLayoutManager layoutManager=LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                                    int totalItemCount = layoutManager.getItemCount();
                                    int lastVisible = layoutManager.findLastVisibleItemPosition();

                                    boolean endHasBeenReached = lastVisible + 3 >= totalItemCount;  //TODO: increase the integer (loading trigger threshold) as we get more posts, but capping it at 5 is probably sufficient
                                    if (totalItemCount > 0 && endHasBeenReached) {
                                        //you have reached to the bottom of your recycler view
                                        if(!nowLoading){
                                            nowLoading = true;
                                            Log.d("Load", "Now Loadin More");
                                            loadMorePostsByVotecount();
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
                else{
                    mHostActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    private void loadMorePostsByTime() {

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
                mSwipeRefreshLayout.setRefreshing(true);

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                try {

                    if(lastEvaluatedKey != null){
                        final Condition rangeKeyCondition = new Condition()
                                .withComparisonOperator(ComparisonOperator.LT.toString())
                                .withAttributeValueList(new AttributeValue().withS(df.format(new Date())));

                        Post queryTemplate = new Post();
                        queryTemplate.setCategory(currCategoryInt);
                        //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                        DynamoDBQueryExpression queryExpression =
                                new DynamoDBQueryExpression()
                                        .withIndexName("category-time-index")
                                        .withHashKeyValues(queryTemplate)
                                        .withRangeKeyCondition("time", rangeKeyCondition)
                                        .withScanIndexForward(false)
                                        //.withConsistentRead(true)
                                        .withLimit(retrievalLimit)
                                        .withExclusiveStartKey(lastEvaluatedKey);

                        //Log.d("Query on Category: ", Integer.toString(queryTemplate.getCategory()));

                        QueryResultPage queryResultPage = ((MainContainer)getActivity()).getMapper().queryPage(Post.class, queryExpression);
                        ArrayList<Post> queryResults = new ArrayList<>(queryResultPage.getResults());
                        //bookmark for the range key for loading more when user scrolls down far enough and triggers "load more action"

                        if(queryResults.size() < retrievalLimit){
                            lastEvaluatedKey = null;
                        }
                        else{
                            //Log.d("Load: ", "retrieved " + Integer.toString(queryResults.size()) + " more items");
                            lastEvaluatedKey = queryResultPage.getLastEvaluatedKey();
                        }

                        if(!queryResults.isEmpty()){
                            //sort the assembledResults where posts are sorted from more recent to less recent
                            Collections.sort(queryResults, new Comparator<Post>() {
                                //TODO: confirm that this sorts dates where most recent is at top. If not then just flip around o1 and o2: change to o2.getDate().compareTo(o1.getDate())
                                @Override
                                public int compare(Post o1, Post o2) {
                                    return o2.getDate().compareTo(o1.getDate());
                                }
                            });
                            posts.addAll(queryResults);
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
                        else {
                            nowLoading = true;
                        }

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
                //recyclerView.getAdapter().notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        };
        _Task.execute((String[]) null);
    }

    private void loadMorePostsByVotecount() {

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
                mSwipeRefreshLayout.setRefreshing(true);

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                try {

                    if(lastEvaluatedKey != null){
                        final Condition rangeKeyCondition = new Condition()
                                .withComparisonOperator(ComparisonOperator.GT.toString())
                                .withAttributeValueList(new AttributeValue().withN("-1"));

                        Post queryTemplate = new Post();
                        queryTemplate.setCategory(currCategoryInt);
                        //Query the category for rangekey timestamp <= maxTimestamp, Limit to retrieving 10 results
                        DynamoDBQueryExpression queryExpression =
                                new DynamoDBQueryExpression()
                                        .withIndexName("category-votecount-index")
                                        .withHashKeyValues(queryTemplate)
                                        .withRangeKeyCondition("votecount", rangeKeyCondition)
                                        .withScanIndexForward(false)
                                        //.withConsistentRead(true)
                                        .withLimit(retrievalLimit)
                                        .withExclusiveStartKey(lastEvaluatedKey);

                        //Log.d("Query on Category: ", Integer.toString(queryTemplate.getCategory()));

                        QueryResultPage queryResultPage = ((MainContainer)getActivity()).getMapper().queryPage(Post.class, queryExpression);
                        ArrayList<Post> queryResults = new ArrayList<>(queryResultPage.getResults());
                        //bookmark for the range key for loading more when user scrolls down far enough and triggers "load more action"

                        if(queryResults.size() < retrievalLimit){
                            lastEvaluatedKey = null;
                        }
                        else{
                            //Log.d("Load: ", "retrieved " + Integer.toString(queryResults.size()) + " more items");
                            lastEvaluatedKey = queryResultPage.getLastEvaluatedKey();
                        }

                        if(!queryResults.isEmpty()){
                            //sort the assembledResults where posts are sorted from more recent to less recent
                            Collections.sort(queryResults, new Comparator<Post>() {
                                //TODO: confirm that this sorts dates where most recent is at top. If not then just flip around o1 and o2: change to o2.getDate().compareTo(o1.getDate())
                                @Override
                                public int compare(Post o1, Post o2) {
                                    return o2.getVotecount() - o1.getVotecount();
                                }
                            });
                            posts.addAll(queryResults);

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
                        else {
                            nowLoading = true;
                        }
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
                //recyclerView.getAdapter().notifyDataSetChanged();
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
        switch (sortType){
            case NEW:
                refreshCategoryTimeQuery();
                break;
            case POPULAR:
                refreshCategoryVotecountQuery();
                break;
            default:
                break;
        }
    }

    private void refreshCategoryTimeQuery(){
        adCount = 0;
        Log.d("Refresh", "Now Refreshing");
        //mSwipeRefreshLayout.setRefreshing(true);
        lastEvaluatedKey = null;
        posts.clear();

        categoryTimeQuery(currCategoryInt);
        recyclerView.getAdapter().notifyDataSetChanged();

        nowLoading = false;

    }

    private void refreshCategoryVotecountQuery(){
        adCount = 0;
        lastEvaluatedKey = null;
        posts.clear();

        categoryVotecountQuery(currCategoryInt);
        recyclerView.getAdapter().notifyDataSetChanged();

        nowLoading = false;
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

    public void clearPosts(){
        posts.clear();
        if(recyclerView != null){
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }


}