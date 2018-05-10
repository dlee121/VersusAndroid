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
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.Post;

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
                /*
                if(accessKey == null || accessKey.equals("")){
                    accessKey = activity.getCred().getAWSAccessKeyId();
                }
                if(secretKey == null || secretKey.equals("")){
                    secretKey = activity.getCred().getAWSSecretKey();
                }
                */
                //TODO: get accesskey and secretkey

                String query = "/post/_search";
                String payload = "{\"from\":"+Integer.toString(fromIndex)+",\"size\":"+Integer.toString(retrievalSize)+",\"sort\":[{\"ps\":{\"order\":\"desc\"}}],\"query\":{\"match_all\":{}}}";

                String url = "https://" + host + query;

                TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
                awsHeaders.put("host", host);

                AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                        .regionName(region)
                        .serviceName("es") // es - elastic search. use your service name
                        .httpMethodName("POST") //GET, PUT, POST, DELETE, etc...
                        .canonicalURI(query) //end point
                        .queryParametes(null) //query parameters if any
                        .awsHeaders(awsHeaders) //aws header parameters
                        .payload(payload) // payload if any
                        .debug() // turn on the debug mode
                        .build();

                HttpPost httpPost = new HttpPost(url);
                StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);

		        /* Get header calculated for request */
                Map<String, String> header = aWSV4Auth.getHeaders();
                for (Map.Entry<String, String> entrySet : header.entrySet()) {
                    String key = entrySet.getKey();
                    String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

                    httpPost.addHeader(key, value);
                }
                httpPostRequest(httpPost);

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

        //TODO: also ad ads as we did with the ddb Newsfeed query
    }

    public void httpPostRequest(HttpPost httpPost) {
		/* Create object of CloseableHttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

		/* Response handler for after request execution */
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				/* Get status code */
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
					/* Convert response to String */
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        try {
			/* Execute URL and attach after execution response handler */
            String strResponse = httpClient.execute(httpPost, responseHandler);
            if(posts == null){
                posts = new ArrayList<>();
                myAdapter = new MyAdapter(posts, mHostActivity, profileImgVersions, 0);
                recyclerView.setAdapter(myAdapter);
            }

            JSONObject obj = new JSONObject(strResponse);
            JSONArray hits = obj.getJSONObject("hits").getJSONArray("hits");
            if(hits.length() == 0){
                Log.d("loadmore", "end reached, disabling loadMore");
                mHostActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
                return;
            }
            int j = 0;
            StringBuilder strBuilder = new StringBuilder((56*hits.length()) - 1);
            for(int i = 0; i < hits.length(); i++) {
                JSONObject item = hits.getJSONObject(i).getJSONObject("_source");
                posts.add(new Post(item, false));
                currPostsIndex++;
                if (currPostsIndex % adFrequency == 0) {
                    Post adSkeleton = new Post();
                    NativeAd nextAd = mHostActivity.getNextAd();
                    if (nextAd != null) {
                        Log.d("adscheck", "ads loaded");
                        if (nextAd instanceof NativeAppInstallAd) {
                            adSkeleton.setCategory(NATIVE_APP_INSTALL_AD);
                            adSkeleton.setNAI((NativeAppInstallAd) nextAd);
                            posts.add(adSkeleton);
                            adCount++;
                        } else if (nextAd instanceof NativeContentAd) {
                            adSkeleton.setCategory(NATIVE_CONTENT_AD);
                            adSkeleton.setNC((NativeContentAd) nextAd);
                            posts.add(adSkeleton);
                            adCount++;
                        }
                    } else {
                        Log.d("adscheck", "ads not loaded");
                    }
                }
                if(!item.getString("a").equals("[deleted]")){
                    //add username to parameter string, then at loop finish we do multiget of those users and create hashmap of username:profileImgVersion
                    if(j == 0){
                        strBuilder.append("{\"_id\":\""+item.getString("a")+"\",\"_source\":\"pi\"}");
                    }
                    else{
                        strBuilder.append(",{\"_id\":\""+item.getString("a")+"\",\"_source\":\"pi\"}");
                    }
                    j++;
                }
            }

            if(strBuilder.length() > 0){
                String payload = "{\"docs\":["+strBuilder.toString()+"]}";
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

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removePostFromList(int index, String postID){
        if(posts != null && !posts.isEmpty() && myAdapter != null && index >= 0){
            if(posts.get(index).getPost_id().equals(postID)){
                Post deletedPost = posts.get(index);
                deletedPost.setAuthor("[deleted]");
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
        String query = "/user/user_type/_mget";
        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);
        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(region)
                .serviceName("es") // es - elastic search. use your service name
                .httpMethodName("POST") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(query) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .payload(payload) // payload if any
                .debug() // turn on the debug mode
                .build();

        String url = "https://" + host + query;

        HttpPost httpPost = new HttpPost(url);
        StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);

		        /* Get header calculated for request */
        Map<String, String> header = aWSV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

            httpPost.addHeader(key, value);
        }

        /* Create object of CloseableHttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

		/* Response handler for after request execution */
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				/* Get status code */
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
					/* Convert response to String */
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        try {
			/* Execute URL and attach after execution response handler */

            String strResponse = httpClient.execute(httpPost, responseHandler);
            Log.d("hahahai", strResponse);

            //iterate through hits and put the info in postInfoMap
            JSONObject obj = new JSONObject(strResponse);
            JSONArray hits = obj.getJSONArray("docs");
            for(int i = 0; i<hits.length(); i++){
                JSONObject item = hits.getJSONObject(i);
                JSONObject src = item.getJSONObject("_source");
                profileImgVersions.put(item.getString("_id"), src.getInt("pi"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public MyAdapter getMyAdapter() {
        return myAdapter;
    }

}