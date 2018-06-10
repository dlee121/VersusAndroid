package com.vs.bcd.versus.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.vs.bcd.api.model.PIVModel;
import com.vs.bcd.api.model.PIVModelDocsItem;
import com.vs.bcd.api.model.PostsListModel;
import com.vs.bcd.api.model.PostsListModelHitsHitsItem;
import com.vs.bcd.api.model.PostsListModelHitsHitsItemSource;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.ArrayAdapterWithIcon;
import com.vs.bcd.versus.adapter.CategoriesAdapter;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.Post;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by dlee on 8/6/17.
 */



public class Tab3Categories extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private View rootView;
    private ArrayList<CategoryObject> categories;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private CategoriesAdapter mCategoriesAdapter;
    private RelativeLayout categoryPostsList;
    //private EditText categoryFilterET;
    private RecyclerView categoriesRV;


    private boolean categoryPostsListOpen = false;

    private String currentCategoryTitle = "";

    private ArrayList<Post> posts = new ArrayList<>();
    private MyAdapter myAdapter;
    private MainContainer mHostActivity;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    private boolean nowLoading = false;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean initialLoadInProgress = false;
    private int currCategoryInt = 0;
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

    private HashMap<String, Integer> profileImgVersions = new HashMap<>();




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab3categories, container, false);

        categories = new ArrayList<>();
        mHostActivity.setUpCategoriesList(categories);

        categoriesRV = rootView.findViewById(R.id.category_selection_rvt3);
        categoriesRV.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCategoriesAdapter = new CategoriesAdapter(categoriesRV, categories, getActivity(), 1);
        categoriesRV.setAdapter(mCategoriesAdapter);

        categoryPostsList = rootView.findViewById(R.id.category_posts_list);

        posts = new ArrayList<>();

        recyclerView = rootView.findViewById(R.id.recycler_view_cf);

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
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_container_catfrag);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        if(initialLoadInProgress) {
            mSwipeRefreshLayout.setRefreshing(true);
        }

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
        mHostActivity = (MainContainer)context;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null)
                enableChildViews();
        }
        else {
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

    public void setUpCategoryPostsList(int categoryInt, String currentCategoryTitle){
        currCategoryInt = categoryInt;
        categoryTimeESQuery(0);
        categoriesRV.getLayoutParams().height = 0;
        categoryPostsList.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
        mHostActivity.setLeftChevron();
        categoryPostsListOpen = true;
        myAdapter.notifyDataSetChanged();
        this.currentCategoryTitle = currentCategoryTitle;
    }

    public String getCurrentCategoryTitle(){
        return currentCategoryTitle;
    }

    public void closeCategoryPostsList(){
        categoryPostsList.getLayoutParams().height = 0;
        categoriesRV.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
        mHostActivity.setLeftSearchButton();
        categoryPostsListOpen = false;
        myAdapter.clearList();
        mHostActivity.setToolbarTitleTextForTabs("Categories");
        mCategoriesAdapter.notifyDataSetChanged();
    }

    public boolean isCategoryPostsListOpen(){
        return categoryPostsListOpen;
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

    public void clearPosts(){
        posts.clear();
        profileImgVersions.clear();
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
                //myAdapter.notifyItemInserted(0);
                myAdapter.notifyDataSetChanged();
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

                /* Execute URL and attach after execution response handler */
                if(posts == null){
                    posts = new ArrayList<>();
                    myAdapter = new MyAdapter(posts, mHostActivity, profileImgVersions, 0);
                    recyclerView.setAdapter(myAdapter);
                }


                PostsListModel results = mHostActivity.getClient().postslistGet(Integer.toString(currCategoryInt), "t", "ct", Integer.toString(fromIndex));
                if(results != null){
                    List<PostsListModelHitsHitsItem> hits = results.getHits().getHits();
                    if(hits != null && !hits.isEmpty()){
                        int i = 0;
                        StringBuilder strBuilder = new StringBuilder((56*hits.size()) - 1);
                        for(PostsListModelHitsHitsItem item : hits){
                            PostsListModelHitsHitsItemSource source = item.getSource();
                            String id = item.getId();
                            posts.add(new Post(source, id));
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

                /* Execute URL and attach after execution response handler */
                if(posts == null){
                    posts = new ArrayList<>();
                    myAdapter = new MyAdapter(posts, mHostActivity, profileImgVersions, 0);
                    recyclerView.setAdapter(myAdapter);
                }


                PostsListModel results = mHostActivity.getClient().postslistGet(Integer.toString(currCategoryInt), "ps", "ct", Integer.toString(fromIndex));
                if(results != null){
                    List<PostsListModelHitsHitsItem> hits = results.getHits().getHits();
                    if(hits != null && !hits.isEmpty()){
                        int i = 0;
                        StringBuilder strBuilder = new StringBuilder((56*hits.size()) - 1);
                        for(PostsListModelHitsHitsItem item : hits){
                            PostsListModelHitsHitsItemSource source = item.getSource();
                            String id = item.getId();
                            posts.add(new Post(source, id));
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


            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void removePostFromList(int index, String postID){
        if(posts != null && !posts.isEmpty() && myAdapter != null && index >= 0){
            if(posts.get(index).getPost_id().equals(postID)){
                if(sortType == POPULAR){
                    Post deletedPost = posts.get(index);
                    deletedPost.setAuthor("deleted");
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

    private void getProfileImgVersions(String payload){

        PIVModel pivResult = mHostActivity.getClient().pivGet("pis", payload);

        List<PIVModelDocsItem> pivList = pivResult.getDocs();
        if(pivList != null && !pivList.isEmpty()){
            for(PIVModelDocsItem item : pivList){
                profileImgVersions.put(item.getId(), item.getSource().getPi().intValue());
            }
        }

    }

    public MyAdapter getMyAdapter() {
        return myAdapter;
    }

}
