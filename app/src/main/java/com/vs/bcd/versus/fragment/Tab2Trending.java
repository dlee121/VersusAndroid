package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.vs.api.vs2.model.ProfileImageViews;
import com.vs.api.vs2.model.ProfileImageViewsDocsItem;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostResults;
import com.vs.bcd.versus.model.PostResultsHitsHitsItem;
import com.vs.bcd.versus.model.PostResultsHitsHitsItemSource;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab2Trending extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<Post> posts;
    private MyAdapter myAdapter;
    private boolean fragmentSelected = false; //marks if initial loading for this fragment was already done (as in, fragment was already selected once before if true). Used so that we don't load content every time the tab gets selected.
    private View rootView;
    private MainContainer mHostActivity;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    private boolean displayResults = false;
    private boolean nowLoading = false;
    private RecyclerView recyclerView;
    SwipeRefreshLayout mSwipeRefreshLayout;

    private int loadThreshold = 8;
    private int adFrequency = 18; //place native ad after every 18 posts
    private int adCount = 0;
    private int retrievalSize = 16;

    private int NATIVE_APP_INSTALL_AD = 42069;
    private int NATIVE_CONTENT_AD = 69420;

    private int currPostsIndex = 0;

    private String host, region;

    private HashMap<String, Integer> profileImgVersions = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab2trending, container, false);
        //mHostActivity.setToolbarTitleTextForTabs("Trending");

        host = mHostActivity.getESHost();
        region = mHostActivity.getESRegion();

        posts = new ArrayList<>();

        recyclerView = rootView.findViewById(R.id.recycler_view2);

        recyclerView.setLayoutManager(new LinearLayoutManager(mHostActivity));
        //this is where the list is passed on to adapter
        myAdapter = new MyAdapter(posts, mHostActivity, profileImgVersions, 0);
        recyclerView.setAdapter(myAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //only if postSearchResults.size()%retrievalSize == 0, meaning it's possible there's more matching documents for this search
                if(posts != null && !posts.isEmpty() && currPostsIndex%retrievalSize == 0) {
                    LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                    int lastVisible = layoutManager.findLastVisibleItemPosition();

                    boolean endHasBeenReached = lastVisible + loadThreshold >= currPostsIndex;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                    if (currPostsIndex > 0 && endHasBeenReached) {
                        //you have reached to the bottom of your recycler view
                        if (!nowLoading) {
                            nowLoading = true;
                            Log.d("loadmore", "now loading more");
                            trendingESQuery(currPostsIndex);
                        }
                    }
                }
            }
        });

        //recyclerview preloader setup
        ListPreloader.PreloadSizeProvider sizeProvider =
                new FixedPreloadSizeProvider(mHostActivity.getImageWidthPixels(), mHostActivity.getImageHeightPixels());
        RecyclerViewPreloader<Post> preloader =
                new RecyclerViewPreloader<>(Glide.with(mHostActivity), myAdapter, sizeProvider, 10);
        recyclerView.addOnScrollListener(preloader);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_container_tab2);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if(getUserVisibleHint()){
            trendingESQuery(0);
        }

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
    }

    /**
     * This method is called when swipe refresh is pulled down
     */
    @Override
    public void onRefresh() {
        // Fetching data from server
        adCount = 0;
        Log.d("Refresh", "Now Refreshing");

        posts.clear();
        profileImgVersions.clear();
        trendingESQuery(0);

        Log.d("Refresh", "Now posts has " + Integer.toString(posts.size()) + " items");
    }


    public void trendingESQuery(final int fromIndex) {
        if(fromIndex == 0){
            mSwipeRefreshLayout.setRefreshing(true);
            currPostsIndex = 0;
            nowLoading = false;
        }

        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    /* Execute URL and attach after execution response handler */
                    if(posts == null){
                        posts = new ArrayList<>();
                        myAdapter = new MyAdapter(posts, mHostActivity, profileImgVersions, 0);
                        recyclerView.setAdapter(myAdapter);
                    }


                    PostResults results = mHostActivity.getClient1().vSLambdaGet(null, null, "tr", Integer.toString(fromIndex));
                    if(results != null){
                        List<PostResultsHitsHitsItem> hits = results.getHits().getHits();
                        if(hits != null && !hits.isEmpty()){
                            int i = 0;
                            StringBuilder strBuilder = new StringBuilder((56*hits.size()) - 1);
                            for(PostResultsHitsHitsItem item : hits){
                                PostResultsHitsHitsItemSource source = item.getSource();
                                String id = item.getId();
                                posts.add(new Post(source, id, false));
                                currPostsIndex++;

                                if(currPostsIndex%adFrequency == 0){
                                    Post adSkeleton = new Post();
                                    NativeAd nextAd = mHostActivity.getNextAd();
                                    if(nextAd != null){
                                        Log.d("adscheck", "ads loaded");
                                        if(nextAd instanceof NativeAppInstallAd){
                                            adSkeleton.setCategory(NATIVE_APP_INSTALL_AD);
                                            adSkeleton.setNAI((NativeAppInstallAd) nextAd);
                                            posts.add(adSkeleton);
                                            adCount++;
                                        }
                                        else if(nextAd instanceof NativeContentAd){
                                            adSkeleton.setCategory(NATIVE_CONTENT_AD);
                                            adSkeleton.setNC((NativeContentAd) nextAd);
                                            posts.add(adSkeleton);
                                            adCount++;
                                        }
                                    }
                                    else{
                                        Log.d("adscheck", "ads not loaded");
                                    }
                                }

                                //add username to parameter string, then at loop finish we do multiget of those users and create hashmap of username:profileImgVersion
                                if(i == 0){
                                    strBuilder.append("\""+source.getA()+"\"");
                                }
                                else{
                                    strBuilder.append(",\""+source.getA()+"\"");
                                }
                                i++;
                            }

                            if(strBuilder.length() > 0){
                                String payload = "{\"ids\":["+strBuilder.toString()+"]}";
                                getProfileImgVersions(payload);
                            }

                            mHostActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    if(nowLoading){
                                        nowLoading = false;
                                    }
                                    if(posts != null && !posts.isEmpty()){
                                        myAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                        else{
                            mHostActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("loadmore", "end reached, disabling loadMore");
                                    nowLoading = true;
                                    mSwipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        }
                    }
                    else{
                        mHostActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("loadmore", "end reached, disabling loadMore");
                                nowLoading = true;
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    }

                    //System.out.println("Response: " + strResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void removePostFromList(int index, String postID){
        if(posts != null && !posts.isEmpty() && myAdapter != null && index >= 0){
            if(posts.get(index).getPost_id().equals(postID)){
                Post deletedPost = posts.get(index);
                deletedPost.setAuthor("deleted");
                posts.set(index, deletedPost);
                myAdapter.notifyItemChanged(index);
            }
        }
    }

    public boolean postsLoaded(){
        return posts != null && !posts.isEmpty();
    }

    public void editedPostRefresh(int index, Post editedPost){
        if(!posts.isEmpty() && posts.get(index) != null && index >= 0){
            if(posts.get(index).getPost_id().equals(editedPost.getPost_id())){
                posts.set(index, editedPost);
                myAdapter.notifyItemChanged(index);
            }
        }

    }

    private void getProfileImgVersions(String payload){

        try {
            ProfileImageViews pivResult = mHostActivity.getClient2().vSLambdaGet("pis", payload);

            List<ProfileImageViewsDocsItem> pivList = pivResult.getDocs();
            if(pivList != null && !pivList.isEmpty()){
                for(ProfileImageViewsDocsItem item : pivList){
                    profileImgVersions.put(item.getId(), item.getSource().getPi().intValue());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public MyAdapter getMyAdapter() {
        return myAdapter;
    }

}