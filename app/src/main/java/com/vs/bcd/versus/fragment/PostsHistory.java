package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vs.bcd.api.model.PostsListCompactModel;
import com.vs.bcd.api.model.PostsListCompactModelHitsHitsItem;
import com.vs.bcd.api.model.PostsListCompactModelHitsHitsItemSource;
import com.vs.bcd.api.model.PostsListModel;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CommentHistoryAdapter;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.VSComment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
 * Created by dlee on 5/19/17.
 */



public class PostsHistory extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private static MainContainer activity;
    private EditText searchET;
    private RecyclerView recyclerView;
    private int retrievalSize = 20;
    private int loadThreshold = 2;
    private boolean nowLoading = false;

    private ArrayList<Post> posts;
    private MyAdapter myAdapter;
    private int currPostsIndex = 0;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private String profileUsername = "";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.posts_history, container, false);

        posts = new ArrayList<>();

        recyclerView = rootView.findViewById(R.id.recycler_view_ph);

        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        //this is where the list is passed on to adapter
        myAdapter = new MyAdapter(posts, activity, 9);
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
                            getUserPosts(currPostsIndex, "pt");
                        }
                    }
                }
            }
        });

        // SwipeRefreshLayout
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_container_ph);
        mSwipeRefreshLayout.setOnRefreshListener(this);


        return rootView;
    }

    @Override
    public void onRefresh() {
        getUserPosts(0, "pt");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //save the activity to a member of this fragment
        activity = (MainContainer)context;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    private void getUserPosts(final int fromIndex, final String ptORt) {
        if(profileUsername.equals("")) {
            return;
        }

        mSwipeRefreshLayout.setRefreshing(true);

        Runnable runnable = new Runnable() {
            public void run() {
                if(fromIndex == 0){
                    posts.clear();
                    currPostsIndex = 0;
                    nowLoading = false;
                }

                PostsListCompactModel result = activity.getClient().postslistcompactGet(profileUsername, "pp", Integer.toString(fromIndex));

                try {
                    List<PostsListCompactModelHitsHitsItem> hits = result.getHits().getHits();
                    if(hits.size() == 0){
                        Log.d("loadmore", "end reached, disabling loadMore");
                        nowLoading = true;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                    for(PostsListCompactModelHitsHitsItem item : hits){
                        PostsListCompactModelHitsHitsItemSource source = item.getSource();
                        String id = item.getId();
                        posts.add(new Post(source, id, profileUsername));
                        currPostsIndex++;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myAdapter.notifyDataSetChanged();
                            mSwipeRefreshLayout.setRefreshing(false);
                            nowLoading = false;
                        }
                    });
                    //System.out.println("Response: " + strResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();



    }

    public void setProfileUsername(String profileUsername){
        if(this.profileUsername != null && !this.profileUsername.equals(profileUsername)){
            this.profileUsername = profileUsername;
            getUserPosts(0, "pt");
        }
    }

    public void removePostFromList(int index, String redName){
        if(posts != null && !posts.isEmpty() && myAdapter != null && index >= 0){
            if(posts.get(index).getRedname().equals(redName)){
                posts.remove(index);
                myAdapter.notifyItemRemoved(index);
            }
        }
    }

    public MyAdapter getMyAdapter() {
        return myAdapter;
    }
}

