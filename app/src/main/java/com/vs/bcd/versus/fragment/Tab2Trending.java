package com.vs.bcd.versus.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
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
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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
import com.vs.bcd.versus.adapter.CategoriesAdapter;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.Post;

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
    private Button filterSelector;

    private int loadThreshold = 8;
    private int adCount = 0;
    private int retrievalSize = 16;
    private int randomNumberMin = 10;
    private int randomNumberMax = 15;

    private int NATIVE_AD = 69420;

    private int currPostsIndex = 0;
    private Random randomNumber = new Random();
    private int nextAdIndex = randomNumber.nextInt(randomNumberMax - randomNumberMin + 1) + randomNumberMin;

    private int categorySelection = -1;
    private LinearLayout categorySelectionView;
    private RelativeLayout.LayoutParams categorySelectionViewLP;
    private ImageView categoryIcon;
    private TextView categoryName;



    private HashMap<String, Integer> profileImgVersions = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab2trending, container, false);
        //mHostActivity.setToolbarTitleTextForTabs("Trending");

        categorySelectionView = rootView.findViewById(R.id.category_selection_tr);
        categoryIcon = categorySelectionView.findViewById(R.id.category_ic_tr);
        categoryName = categorySelectionView.findViewById(R.id.tv_category_tr);
        hideCategorySelection();


        posts = new ArrayList<>();

        filterSelector = rootView.findViewById(R.id.filter_selector_tr);
        filterSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterSelectorClicked();
            }
        });

        recyclerView = rootView.findViewById(R.id.recycler_view_tr);

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

                    boolean endHasBeenReached = lastVisible + loadThreshold >= currPostsIndex + adCount;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                    if (currPostsIndex > 0 && endHasBeenReached) {
                        //you have reached to the bottom of your recycler view
                        if (!nowLoading) {
                            nowLoading = true;
                            Log.d("loadmore", "now loading more");
                            trendingESQuery(currPostsIndex + adCount);
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
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_container_tr);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        rootView.findViewById(R.id.category_clear_tr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                categorySelection = -1;
                hideCategorySelection();
                onRefresh();
            }
        });

        //mHostActivity.getMainFrag().getViewPager().setCurrentItem(1);

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
        nextAdIndex = randomNumber.nextInt(randomNumberMax - randomNumberMin + 1) + randomNumberMin;
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

                /* Execute URL and attach after execution response handler */
                if(posts == null){
                    posts = new ArrayList<>();
                    myAdapter = new MyAdapter(posts, mHostActivity, profileImgVersions, 0);
                    recyclerView.setAdapter(myAdapter);
                }

                PostsListModel results;
                if(categorySelection > -1){
                    results = mHostActivity.getClient().postslistGet(Integer.toString(categorySelection), null, "tr", Integer.toString(fromIndex));
                }
                else{
                    Log.d("fsldfijsljd", "sdfljfdl");
                    results = mHostActivity.getClient().postslistGet(null, null, "tr", Integer.toString(fromIndex));
                }
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

                            if(currPostsIndex == nextAdIndex){
                                Post adSkeleton = new Post();
                                nextAdIndex = currPostsIndex + randomNumber.nextInt(randomNumberMax - randomNumberMin + 1) + randomNumberMin;
                                adSkeleton.setCategory(NATIVE_AD);
                                posts.add(adSkeleton);
                                adCount++;
                            }

                            //add username to parameter string, then at loop finish we do multiget of those users and create hashmap of username:profileImgVersion
                            if(!source.getA().equals("deleted")){
                                if(i == 0){
                                    strBuilder.append("\""+source.getA()+"\"");
                                }
                                else{
                                    strBuilder.append(",\""+source.getA()+"\"");
                                }
                                i++;
                            }
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

        PIVModel pivResult = mHostActivity.getClient().pivGet("pis", payload.toLowerCase());

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

    private void filterSelectorClicked(){
        FragmentTransaction ft = mHostActivity.getFragmentManager().beginTransaction();
        DialogFragment newFragment = CategoryFilterFragment.newInstance();
        newFragment.show(ft, "dialog");

    }

    public static class CategoryFilterFragment extends DialogFragment {

        private ArrayList<CategoryObject> categories;
        private CategoriesAdapter mCategoriesAdapter;

        static Tab2Trending.CategoryFilterFragment newInstance() {
            Tab2Trending.CategoryFilterFragment f = new Tab2Trending.CategoryFilterFragment();
            return f;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View v = inflater.inflate(R.layout.category_filter, container, false);


            categories = new ArrayList<>();
            ((MainContainer)getActivity()).setUpCategoriesList(categories);

            RecyclerView recyclerView = v.findViewById(R.id.category_rv_cf);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mCategoriesAdapter = new CategoriesAdapter(recyclerView, categories, (MainContainer)getActivity(), 2, getDialog());
            recyclerView.setAdapter(mCategoriesAdapter);

            v.findViewById(R.id.frag_exit_button_cf).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getDialog().dismiss();

                }
            });


            return v;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);

            // request a window without the title
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            return dialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            Dialog dialog = getDialog();
            if (dialog != null) {
                int width = ViewGroup.LayoutParams.MATCH_PARENT;
                int height = ViewGroup.LayoutParams.MATCH_PARENT;
                dialog.getWindow().setLayout(width, height);
            }
        }
    }

    public void setCategorySelection(int selection, int iconResID, String name){
        categorySelection = selection;
        categoryIcon.setImageResource(iconResID);
        categoryName.setText(name);
        showCategorySelection();
        onRefresh();
    }

    private void showCategorySelection(){
        categorySelectionView.setVisibility(View.VISIBLE);

    }

    private void hideCategorySelection(){
        categorySelectionView.setVisibility(View.GONE);
    }
}