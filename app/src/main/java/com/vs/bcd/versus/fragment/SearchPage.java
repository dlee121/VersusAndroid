package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.vs.bcd.api.model.PIVModel;
import com.vs.bcd.api.model.PIVModelDocsItem;
import com.vs.bcd.api.model.PostsListCompactModel;
import com.vs.bcd.api.model.PostsListCompactModelHitsHitsItem;
import com.vs.bcd.api.model.PostsListCompactModelHitsHitsItemSource;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.Post;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by dlee on 5/19/17.
 */



public class SearchPage extends Fragment {
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private ArrayList<Post> postSearchResults;
    private static MainContainer activity;
    private EditText searchET;
    private RecyclerView recyclerView;
    private MyAdapter searchResultsPostsAdapter;
    private int retrievalSize = 10;
    private int loadThreshold = 2;
    private boolean nowLoading = false;

    private String host, region;

    private HashMap<String, Integer> profileImgVersions = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.search_page, container, false);

        host = activity.getESHost();
        region = activity.getESRegion();

        postSearchResults = new ArrayList<>();

        searchET = (EditText) rootView.findViewById(R.id.search_et);

        searchET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if(searchET.getText().toString() != null && searchET.getText().toString().trim().length() > 0){
                        if(postSearchResults != null && searchResultsPostsAdapter != null){
                            postSearchResults.clear();
                            profileImgVersions.clear();
                            searchResultsPostsAdapter.notifyDataSetChanged();
                        }
                        nowLoading = false;
                        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
                        executeSearch(0);
                    }
                    return true;
                }
                return false;
            }
        });

        recyclerView = rootView.findViewById(R.id.search_results_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        searchResultsPostsAdapter = new MyAdapter(postSearchResults, activity, profileImgVersions, 1);
        recyclerView.setAdapter(searchResultsPostsAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //only if postSearchResults.size()%retrievalSize == 0, meaning it's possible there's more matching documents for this search
                if(postSearchResults != null && !postSearchResults.isEmpty() && postSearchResults.size()%retrievalSize == 0) {
                    LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisible = layoutManager.findLastVisibleItemPosition();

                    boolean endHasBeenReached = lastVisible + loadThreshold >= totalItemCount;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                    if (totalItemCount > 0 && endHasBeenReached) {
                        //you have reached to the bottom of your recycler view
                        if (!nowLoading) {
                            nowLoading = true;
                            Log.d("Load", "Now Loadin More");
                            executeSearch(postSearchResults.size());
                        }
                    }
                }
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
        activity = (MainContainer)context;
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

    public void executeSearch(final int fromIndex) {


        Runnable runnable = new Runnable() {
            public void run() {

                String query = "/post/_search";
                String searchTerm = searchET.getText().toString();

                if(searchTerm.trim().length() == 0){
                    return;
                }

                try {
                    if(postSearchResults == null){
                        postSearchResults = new ArrayList<>();
                    }

                    PostsListCompactModel results = activity.getClient().postslistcompactGet(searchTerm,"sp", Integer.toString(fromIndex));

                    if(results != null){
                        List<PostsListCompactModelHitsHitsItem> hits = results.getHits().getHits();
                        if(hits != null && !hits.isEmpty()){
                            int i = 0;
                            StringBuilder strBuilder = new StringBuilder((56*hits.size()) - 1);
                            for(PostsListCompactModelHitsHitsItem item : hits){
                                PostsListCompactModelHitsHitsItemSource source = item.getSource();
                                String id = item.getId();
                                postSearchResults.add(new Post(source, id));

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

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(nowLoading){
                                        nowLoading = false;
                                    }
                                    if(postSearchResults != null && !postSearchResults.isEmpty()){
                                        searchResultsPostsAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                        else{
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("loadmore", "end reached, disabling loadMore");
                                    nowLoading = true;
                                }
                            });
                        }
                    }
                    else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("loadmore", "end reached, disabling loadMore");
                                nowLoading = true;
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

    public void removePostFromList(int index, String redName){
        if(postSearchResults != null && !postSearchResults.isEmpty() && searchResultsPostsAdapter != null && index >= 0){
            if(postSearchResults.get(index).getRedname().equals(redName)){
                Post deletedPost = postSearchResults.get(index);
                deletedPost.setAuthor("deleted");
                postSearchResults.set(index, deletedPost);
                searchResultsPostsAdapter.notifyItemChanged(index);
            }
        }
    }

    //TODO: also implement request cancelling where cancel() is called on the Request, in case user exists search before current search completes, so as to not trigger handler unnecessarily, although it may not matter and may actually work better that way to not cancel...think about that too, not cancelling.

    private void getProfileImgVersions(String payload){
        try {
            PIVModel pivResult = activity.getClient().pivGet("pis", payload);

            List<PIVModelDocsItem> pivList = pivResult.getDocs();
            if(pivList != null && !pivList.isEmpty()){
                for(PIVModelDocsItem item : pivList){
                    profileImgVersions.put(item.getId(), item.getSource().getPi().intValue());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public MyAdapter getSearchResultsPostsAdapter(){
        return searchResultsPostsAdapter;
    }

}

