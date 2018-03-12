package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.DialogInterface;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.ArrayAdapterWithIcon;
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
 * Created by dlee on 9/8/17.
 */

public class CategoryFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<Post> posts = new ArrayList<>();
    private MyAdapter myAdapter;
    private View rootView;
    private MainContainer mHostActivity;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    private boolean nowLoading = false;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean initialLoadInProgress = false;
    private int currCategoryInt = 0;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private Button sortTypeSelector;
    private int sortType = 0; //0 = Most Recent, 1 = Popular
    private final int MOST_RECENT = 0;
    private final int POPULAR = 1;

    private int loadThreshold = 8;
    private int adFrequency = 18; //place native ad after every 18 posts
    private int adCount = 0;
    private int retrievalSize = 16;

    private int NATIVE_APP_INSTALL_AD = 42069;
    private int NATIVE_CONTENT_AD = 69420;

    private int currPostsIndex = 0;

    private String host, region;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.category_fragment, container, false);
        //TODO: need to add categories. maybe a separate categories table where post IDs have rows of categories they are linked with
        //TODO: create, at the right location, list of constant enumeration to represent categories. probably at post creation page, which is for now replaced by sample data creation below
        //mHostActivity.setToolbarTitleTextForTabs("Trending");

        host = mHostActivity.getESHost();
        region = mHostActivity.getESRegion();

        posts = new ArrayList<>();

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view_cf);

        recyclerView.setLayoutManager(new LinearLayoutManager(mHostActivity));
        //this is where the list is passed on to adapter
        myAdapter = new MyAdapter(posts, mHostActivity, 6);
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

                            switch (sortType){
                                case MOST_RECENT:
                                    categoryTimeESQuery(currPostsIndex);
                                    break;
                                case POPULAR:
                                    categoryPsESQuery(currPostsIndex);
                                    break;
                                default:
                                    break;
                            }
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
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container_catfrag);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if(initialLoadInProgress) {
            mSwipeRefreshLayout.setRefreshing(true);
        }

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fabcf);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHostActivity.setOriginFragNum(2);
                mHostActivity.getViewPager().setCurrentItem(2);
                mHostActivity.getToolbarTitleText().setText("Create Post");
                mHostActivity.getToolbarButtonLeft().setImageResource(R.drawable.ic_left_chevron);
            }
        });


        sortTypeSelector = rootView.findViewById(R.id.sort_type_selector);
        sortTypeSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String [] items = new String[] {"Popular", "Most Recent"};
                final Integer[] icons = new Integer[] {R.drawable.ic_thumb_up, R.drawable.ic_new_releases}; //TODO: change these icons to actual ones
                ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(), items, icons);

                new AlertDialog.Builder(getActivity()).setTitle("Sort by")
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item ) {
                                clearPosts();
                                switch(item){
                                    case 0: //Sort by Popular; category-votecount-index query.
                                        Log.d("SortType", "sort by votecount");
                                        sortType = POPULAR;
                                        categoryPsESQuery(0);
                                        break;

                                    case 1: //Sort by New; category-time-index query.
                                        Log.d("SortType", "sort by time");
                                        sortType = MOST_RECENT;
                                        categoryTimeESQuery(0);
                                        break;
                                }
                                setSortTypeHint();
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
            if (rootView != null)
                enableChildViews();
        } else {
            Log.d("VISIBLE", "SEARCH POST GONE");
            if (rootView != null)
                disableChildViews();

        }
    }

    /**
     * This method is called when swipe refresh is pulled down
     */
    @Override
    public void onRefresh() {
        clearPosts();
        switch (sortType){
            case MOST_RECENT:
                categoryTimeESQuery(0);
                break;
            case POPULAR:
                categoryPsESQuery(0);
                break;
            default:
                break;
        }
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

    private void setSortTypeHint(){
        switch (sortType){
            case MOST_RECENT:
                sortTypeSelector.setText("MOST RECENT");
                sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                break;

            case POPULAR:
                sortTypeSelector.setText("POPULAR");
                sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                break;
        }
    }

    public void addPostToTop(Post post){
        if(posts != null && myAdapter != null){
            if(post.getCategory() == currCategoryInt){
                posts.add(0, post);
                myAdapter.notifyItemInserted(0);
            }
        }
    }

    public void categoryTimeESQuery(final int fromIndex) {
        if(sortType == POPULAR){
            sortType = MOST_RECENT; //resets sort type if coming from another category where sort type was set to POPULAR
            setSortTypeHint();
        }


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

                String query = "/_search";
                //String payload = "{\"from\":"+Integer.toString(fromIndex)+",\"size\":"+Integer.toString(retrievalSize)+",\"sort\":[{\"t\":{\"order\":\"desc\"}}],\"query\":{\"match\":{\"c\":"+Integer.toString(currCategoryInt)+"}}}";
                String payload =
                        "{\"from\":"+Integer.toString(fromIndex)+",\"size\":"+Integer.toString(retrievalSize)+",\"sort\":[{\"t\":{\"order\":\"desc\"}}],\"query\":{\"bool\":{\"must\":{\"match\":{\"c\":"+Integer.toString(currCategoryInt)+"}},\"must_not\":{\"match\":{\"a\":\"[deleted\"}}}}}";



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
    }

    public void categoryPsESQuery(final int fromIndex) {

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
                String payload = "{\"from\":"+Integer.toString(fromIndex)+",\"size\":"+Integer.toString(retrievalSize)+",\"sort\":[{\"ps\":{\"order\":\"desc\"}}],\"query\":{\"match\":{\"c\":"+Integer.toString(currCategoryInt)+"}}}";

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
                myAdapter = new MyAdapter(posts, mHostActivity, 6);
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
            for(int i = 0; i < hits.length(); i++){
                JSONObject item = hits.getJSONObject(i).getJSONObject("_source");
                posts.add(new Post(item, false));
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
                Log.d("SEARCHRESULTS", "R: " + posts.get(i).getRedname() + ", B: " + posts.get(i).getBlackname() + ", Q: " + posts.get(i).getQuestion());
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

    public CategoryFragment setCurrCategoryInt(int currCategoryInt){
        this.currCategoryInt = currCategoryInt;
        return this;
    }

    public void removePostFromList(int index, String postID){
        if(posts != null && !posts.isEmpty() && myAdapter != null && index >= 0){
            if(posts.get(index).getPost_id().equals(postID)){
                if(sortType == POPULAR){
                    Post deletedPost = posts.get(index);
                    deletedPost.setAuthor("[deleted]");
                    posts.set(index, deletedPost);
                    myAdapter.notifyItemChanged(index);
                }
                else{
                    posts.remove(index);
                    myAdapter.notifyItemRemoved(index);
                }
            }
        }
    }

    public void editedPostRefresh(int index, Post editedPost){
        if(!posts.isEmpty() && posts.get(index) != null && index >= 0){
            if(posts.get(index).getPost_id().equals(editedPost.getPost_id())){
                posts.set(index, editedPost);
                myAdapter.notifyItemChanged(index);
            }
        }

    }


}