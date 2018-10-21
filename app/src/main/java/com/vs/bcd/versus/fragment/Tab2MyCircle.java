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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.vs.bcd.api.model.CommentsListModel;
import com.vs.bcd.api.model.CommentsListModelHitsHitsItem;
import com.vs.bcd.api.model.CommentsListModelHitsHitsItemSource;
import com.vs.bcd.api.model.PIVModel;
import com.vs.bcd.api.model.PIVModelDocsItem;
import com.vs.bcd.api.model.PostQModel;
import com.vs.bcd.api.model.PostQMultiModel;
import com.vs.bcd.api.model.PostQMultiModelDocsItem;
import com.vs.bcd.api.model.PostQMultiModelDocsItemSource;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainActivity;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.NewsfeedAdapter;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostInfo;
import com.vs.bcd.versus.model.VSComment;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab2MyCircle extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<VSComment> myCircleComments;
    private NewsfeedAdapter newsfeedAdapter;
    private boolean fragmentSelected = false; //marks if initial loading for this fragment was already done (as in, fragment was already selected once before if true). Used so that we don't load content every time the tab gets selected.
    private View rootView;
    private MainContainer mHostActivity;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    private boolean displayResults = false;
    private boolean nowLoading = false;
    private RecyclerView recyclerView;
    SwipeRefreshLayout mSwipeRefreshLayout;

    private int loadThreshold = 8;
    private int adFrequency = 8; //place native ad after every 8 myCircleComments
    private int adCount = 0;
    private int retrievalSize = 16;
    private int randomNumberMin = 10;
    private int randomNumberMax = 15;

    private int NATIVE_APP_INSTALL_AD = 42069;
    private int NATIVE_CONTENT_AD = 69420;

    private int currCommentsIndex = 0;
    private Random randomNumber = new Random();
    private int nextAdIndex = randomNumber.nextInt(randomNumberMax - randomNumberMin + 1) + randomNumberMin;


    private HashMap<String, Integer> profileImgVersions = new HashMap<>();
    private HashMap<String, PostInfo> postInfoMap;
    private HashSet<String> problemPosts = new HashSet<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab1newsfeed, container, false);
        //mHostActivity.setToolbarTitleTextForTabs("Newsfeed");
        postInfoMap = new HashMap<>();
        myCircleComments = new ArrayList<>();

        recyclerView = rootView.findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(mHostActivity));
        //this is where the list is passed on to adapter
        newsfeedAdapter = new NewsfeedAdapter(myCircleComments, mHostActivity, profileImgVersions);
        recyclerView.setAdapter(newsfeedAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //only if postSearchResults.size()%retrievalSize == 0, meaning it's possible there's more matching documents for this search
                if(myCircleComments != null && !myCircleComments.isEmpty() && currCommentsIndex %retrievalSize == 0) {
                    LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                    int lastVisible = layoutManager.findLastVisibleItemPosition();

                    boolean endHasBeenReached = lastVisible + loadThreshold >= currCommentsIndex + adCount;  //TODO: increase the loadThreshold as we get more myCircleComments, but capping it at 5 is probably sufficient
                    if (currCommentsIndex > 0 && endHasBeenReached) {
                        //you have reached to the bottom of your recycler view
                        if (!nowLoading) {
                            nowLoading = true;
                            Log.d("loadmore", "now loading more");
                            newsfeedESQuery(currCommentsIndex + adCount);
                        }
                    }
                }
            }
        });


        //recyclerview preloader setup
        ListPreloader.PreloadSizeProvider sizeProvider =
                new FixedPreloadSizeProvider(mHostActivity.getImageWidthPixels(), mHostActivity.getImageHeightPixels());
        RecyclerViewPreloader<VSComment> preloader =
                new RecyclerViewPreloader<>(Glide.with(mHostActivity), newsfeedAdapter, sizeProvider, 10);
        recyclerView.addOnScrollListener(preloader);


        // SwipeRefreshLayout
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_container_tab1);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setRefreshing(true);



        return rootView;
    }

    @Override
    public void onAttach(Context context) {

        Log.d("mainattach", "attached");
        super.onAttach(context);
        //save the activity to a member of this fragment
        mHostActivity = (MainContainer)context;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Fragment parentFrag = getParentFragment();
            if (parentFrag != null) {
                View rootView = parentFrag.getView();
                if (rootView != null) {
                    rootView.bringToFront();
                }
            }

        }
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

        myCircleComments.clear();
        profileImgVersions.clear();
        newsfeedESQuery(0);

        Log.d("Refresh", "Now myCircleComments has " + Integer.toString(myCircleComments.size()) + " items");
    }

    public void addPostToTop(Post post){
        /*
        if(myCircleComments != null && newsfeedAdapter != null){
            myCircleComments.add(0, post);
            //newsfeedAdapter.notifyItemInserted(0);
            newsfeedAdapter.notifyDataSetChanged();
        }
        */
    }

    public void removePostFromList(int index, String postID){
        /*
        if(myCircleComments != null && !myCircleComments.isEmpty() && newsfeedAdapter != null && index >= 0){
            if(myCircleComments.get(index).getPost_id().equals(postID)){
                myCircleComments.remove(index);
                newsfeedAdapter.notifyItemRemoved(index);
            }
        }
        */
    }


    public void newsfeedESQuery(final int fromIndex) {
        if(fromIndex == 0){
            mSwipeRefreshLayout.setRefreshing(true);
            currCommentsIndex = 0;
            nowLoading = false;
        }

        Runnable runnable = new Runnable() {
            public void run() {
                if(myCircleComments == null){
                    myCircleComments = new ArrayList<>();
                    newsfeedAdapter = new NewsfeedAdapter(myCircleComments, mHostActivity, profileImgVersions);
                    recyclerView.setAdapter(newsfeedAdapter);
                }

                CommentsListModel results = mHostActivity.getClient().commentslistGet(mHostActivity.getNewsfeedUsernamesPayload(fromIndex, retrievalSize), null, "nwv2", Integer.toString(fromIndex));

                if(results != null){
                    List<CommentsListModelHitsHitsItem> hits = results.getHits().getHits();
                    //Log.d("newsfeedqueryresults", "got " + hits.size() + " items");

                    if(hits != null && !hits.isEmpty()){
                        if(hits.size() == 1){
                            CommentsListModelHitsHitsItemSource source = hits.get(0).getSource();
                            String id0 = hits.get(0).getId();
                            VSComment vsc = new VSComment(source, id0);
                            myCircleComments.add(vsc);
                            currCommentsIndex++;
                            if(currCommentsIndex == nextAdIndex){
                                VSComment adSkeleton = new VSComment();
                                nextAdIndex = currCommentsIndex + randomNumber.nextInt(randomNumberMax - randomNumberMin + 1) + randomNumberMin;
                                adSkeleton.setAuthor("d0");
                                myCircleComments.add(adSkeleton);
                                adCount++;
                            }

                            PostInfo postInfo = postInfoMap.get(vsc.getPost_id());
                            if(postInfo != null){
                                myCircleComments.get(fromIndex).setAQRCBC(postInfo.getA(), postInfo.getQ(), postInfo.getRc(), postInfo.getBc());
                            }
                            else {
                                addPostAQ(false, vsc.getPost_id(), fromIndex);
                            }


                        }
                        else{
                            StringBuilder strBuilder = new StringBuilder((67*hits.size()) - 1);

                            for(CommentsListModelHitsHitsItem item : hits){
                                CommentsListModelHitsHitsItemSource source = item.getSource();
                                String id = item.getId();
                                VSComment vsc = new VSComment(source, id);
                                myCircleComments.add(vsc);
                                currCommentsIndex++;

                                if(currCommentsIndex == nextAdIndex){
                                    VSComment adSkeleton = new VSComment();
                                    nextAdIndex = currCommentsIndex + randomNumber.nextInt(randomNumberMax - randomNumberMin + 1) + randomNumberMin;
                                    adSkeleton.setAuthor("d0");
                                    myCircleComments.add(adSkeleton);
                                    adCount++;
                                }

                                if(postInfoMap.get(vsc.getPost_id()) == null){
                                    postInfoMap.put(vsc.getPost_id(), new PostInfo());
                                    if(strBuilder.length() == 0){
                                        strBuilder.append("\""+vsc.getPost_id()+"\"");
                                    }
                                    else{
                                        strBuilder.append(",\""+vsc.getPost_id()+"\""); //start without comma since this is the first one appended to the string builder
                                    }

                                }
                            }
                            if(strBuilder.length() > 0){
                                String postIDs = "{\"ids\":["+strBuilder.toString()+"]}";
                                Log.d("postidsayy", postIDs);
                                addPostAQ(true, postIDs, fromIndex);
                            }
                            else{
                                PostInfo postInfo;
                                for (int j = fromIndex; j< myCircleComments.size(); j++){
                                    postInfo = postInfoMap.get(myCircleComments.get(j).getPost_id());
                                    if (postInfo != null) {
                                        myCircleComments.get(j).setAQRCBC(postInfo.getA(), postInfo.getQ(), postInfo.getRc(), postInfo.getBc());
                                    }
                                }
                            }
                        }

                        mHostActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);
                                if(nowLoading){
                                    nowLoading = false;
                                }
                                if(myCircleComments != null && !myCircleComments.isEmpty()){
                                    newsfeedAdapter.notifyDataSetChanged();
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

    public void editedPostRefresh(int index, Post editedPost){
        /*
        if(!myCircleComments.isEmpty() && index >= 0 && myCircleComments.get(index) != null){
            if(myCircleComments.get(index).getPost_id().equals(editedPost.getPost_id())){
                myCircleComments.set(index, editedPost);
                newsfeedAdapter.notifyItemChanged(index);
            }
        }
        */

    }

    public boolean postsLoaded() {
        return myCircleComments != null && !myCircleComments.isEmpty();
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

    public NewsfeedAdapter getNewsfeedAdapter() {
        return newsfeedAdapter;
    }

    private void addPostAQ(boolean multiGet, String payload, int fromIndex) {
        if(problemPosts == null){
            problemPosts = new HashSet<>();
        }

        if(multiGet){
            PostQMultiModel result = mHostActivity.getClient().postqmultiGet("mpinfq", payload);

            try {

                List<PostQMultiModelDocsItem> hits = result.getDocs();
                StringBuilder strBuilder = new StringBuilder((56*hits.size()) - 1);
                int i = 0;
                for(PostQMultiModelDocsItem item : hits){
                    PostQMultiModelDocsItemSource src = item.getSource();
                    String id = null;
                    String a = null;
                    String q = null;
                    int rc = 0;
                    int bc = 0;
                    try{
                        id = item.getId();
                        a = src.getA();
                        q = src.getQ();
                        rc = src.getRc().intValue();
                        bc = src.getBc().intValue();

                    }
                    catch (Exception e) {
                        if(id != null){
                            Log.d("skipNullPost", "skipped in CH: " + id);
                            problemPosts.add(id); //trigger a function that deletes this post and its comments
                            deleteProblemPostAndComments();
                        }
                        e.printStackTrace();
                    }

                    if(id == null || a == null || q == null){
                        continue;
                    }

                    postInfoMap.get(id).setAQRCBC(a, q, rc, bc);

                }
                //iterate comments array and add the post info in order starting from fromIndex-th element, using postInfoMap
                PostInfo postInfo;
                for (int j = fromIndex; j< myCircleComments.size(); j++){
                    postInfo = postInfoMap.get(myCircleComments.get(j).getPost_id());

                    if (postInfo != null) {
                        String commentAuthor = myCircleComments.get(j).getAuthor();
                        if(postInfo.getA() == null || postInfo.getQ() == null){
                            myCircleComments.get(j).setAQRCBC("", "", 0, 0);
                        }
                        else{
                            myCircleComments.get(j).setAQRCBC(postInfo.getA(), postInfo.getQ(), postInfo.getRc(), postInfo.getBc());
                        }

                        if(!postInfo.getA().equals("deleted") && profileImgVersions.get(postInfo.getA().toLowerCase()) == null){
                            if(i == 0){
                                strBuilder.append("\""+postInfo.getA()+"\"");
                            }
                            else{
                                strBuilder.append(",\""+postInfo.getA()+"\"");
                            }
                            i++;
                        }

                        if(!commentAuthor.equals("deleted") && profileImgVersions.get(commentAuthor.toLowerCase()) == null){
                            if(i == 0){
                                strBuilder.append("\""+commentAuthor+"\"");
                            }
                            else{
                                strBuilder.append(",\""+commentAuthor+"\"");
                            }
                            i++;
                        }
                    }
                }

                if(strBuilder.length() > 0){
                    String pivPayload = "{\"ids\":["+strBuilder.toString()+"]}";
                    getProfileImgVersions(pivPayload);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }



        }
        else{

            PostQModel obj = mHostActivity.getClient().postqGet("pinfq", payload);
            //JSONObject item = obj.getJSONObject("_source");
            PostInfo postInfo = new PostInfo();

            String id = null;
            String a = null;
            String q = null;
            int rc = 0;
            int bc = 0;
            try{
                id = payload;
                a = obj.getA();
                q = obj.getQ();
                rc = obj.getRc().intValue();
                bc = obj.getBc().intValue();
            }catch (Exception e){
                if(id != null){
                    Log.d("skipNullPost", "skipped in CH: " + id);
                    problemPosts.add(id); //trigger a function that deletes this post and its comments
                    deleteProblemPostAndComments();
                }
                e.printStackTrace();
            }
            if(id != null && a != null && q != null){
                postInfo.setAQRCBC(a, q, rc, bc);
                //there's only one comment in the comments array, add the post info to the fromIndex-th index of the comments array
                myCircleComments.get(fromIndex).setAQRCBC(postInfo.getA(), postInfo.getQ(), postInfo.getRc(), postInfo.getBc());
                postInfoMap.put(payload, postInfo);
            }

            String pivPayload = "{\"ids\":[\""+postInfo.getA()+"\"]}";
            getProfileImgVersions(pivPayload);

            //System.out.println("Response: " + strResponse);

        }
    }

    public boolean skipThisComment(String commentPostID){
        if(problemPosts != null){
            return problemPosts.contains(commentPostID);
        }
        return true;
    }

    private void deleteProblemPostAndComments(){
        Runnable runnable = new Runnable() {
            public void run() {
                if(problemPosts != null && !problemPosts.isEmpty()){
                    for(String postID : problemPosts){
                        //delete the post and its comments
                        //TODO: implement this, here, and also in Trending


                    }
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }
}

