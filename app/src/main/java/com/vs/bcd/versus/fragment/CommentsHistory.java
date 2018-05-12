package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.loopj.android.http.HttpGet;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CommentHistoryAdapter;
import com.vs.bcd.versus.adapter.MyAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostInfo;
import com.vs.bcd.versus.model.VSComment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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



public class CommentsHistory extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private ArrayList<Post> postSearchResults;
    private static MainContainer activity;
    private EditText searchET;
    private RecyclerView recyclerView;
    private MyAdapter searchResultsPostsAdapter;
    private int retrievalSize = 20;
    private int loadThreshold = 2;
    private boolean nowLoading = false;
    private ArrayList<VSComment> comments;
    private CommentHistoryAdapter commentsAdapter;
    private int currCommentsIndex = 0;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private String profileUsername = "";
    private HashMap<String, PostInfo> postInfoMap;
    private String host, region;
    private HashSet<String> problemPosts = new HashSet<>();



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.comments_history, container, false);

        host = activity.getESHost();
        region = activity.getESRegion();

        comments = new ArrayList<>();
        postInfoMap = new HashMap<>();

        recyclerView = rootView.findViewById(R.id.recycler_view_ch);

        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        //this is where the list is passed on to adapter
        commentsAdapter = new CommentHistoryAdapter(comments, activity, this);
        recyclerView.setAdapter(commentsAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //only if postSearchResults.size()%retrievalSize == 0, meaning it's possible there's more matching documents for this search
                if(comments != null && !comments.isEmpty() && currCommentsIndex%retrievalSize == 0) {
                    LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                    int lastVisible = layoutManager.findLastVisibleItemPosition();

                    boolean endHasBeenReached = lastVisible + loadThreshold >= currCommentsIndex;  //TODO: increase the loadThreshold as we get more posts, but capping it at 5 is probably sufficient
                    if (currCommentsIndex > 0 && endHasBeenReached) {
                        //you have reached to the bottom of your recycler view
                        if (!nowLoading) {
                            nowLoading = true;
                            Log.d("loadmore", "now loading more");
                            getUserComments(currCommentsIndex, "t");
                        }
                    }
                }
            }
        });

        // SwipeRefreshLayout
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_container_ch);
        mSwipeRefreshLayout.setOnRefreshListener(this);


        return rootView;
    }

    @Override
    public void onRefresh() {
        getUserComments(0, "t");
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

    private void getUserComments(final int fromIndex, final String uORt) {

        if(profileUsername.equals("")){
            return;
        }

        mSwipeRefreshLayout.setRefreshing(true);

        Runnable runnable = new Runnable() {
            public void run() {
                if(fromIndex == 0){
                    comments.clear();
                    currCommentsIndex = 0;
                    nowLoading = false;
                }

                String query = "/vscomment/_search";
                String payload = "{\"from\":"+Integer.toString(fromIndex)+",\"size\":"+Integer.toString(retrievalSize)+",\"sort\":[{\""+uORt+"\":{\"order\":\"desc\"}}],\"query\":{\"match\":{\"a\":\""+profileUsername+"\"}}}";

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

                    JSONObject obj = new JSONObject(strResponse);
                    JSONArray hits = obj.getJSONObject("hits").getJSONArray("hits");
                    //Log.d("idformat", hits.getJSONObject(0).getString("_id"));
                    int hitsLength = hits.length();
                    if(hitsLength == 0) {
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

                    currCommentsIndex+=hitsLength;

                    if(hitsLength == 1){
                        //do a single GET for the post info for the VSComment item
                        JSONObject item = hits.getJSONObject(0).getJSONObject("_source");
                        VSComment vsc = new VSComment(item);
                        comments.add(vsc);
                        PostInfo postInfo = postInfoMap.get(vsc.getPost_id());
                        if(postInfo != null){
                            comments.get(fromIndex).setR(postInfo.getR()).setB(postInfo.getB());
                        }
                        else{
                            addPostInfo(false, vsc.getPost_id(), fromIndex);
                        }
                    }
                    else{   //this means hitsLength > 1
                        //use StringBuilder to generate postIDs payload and do Multi-GET to get the post info for the VSComment items

                        StringBuilder strBuilder = new StringBuilder((67*hitsLength) - 1);
                        JSONObject item = hits.getJSONObject(0).getJSONObject("_source");
                        VSComment vsc = new VSComment(item);
                        comments.add(vsc);
                        if(postInfoMap.get(vsc.getPost_id()) == null){
                            postInfoMap.put(vsc.getPost_id(), new PostInfo());
                            strBuilder.append("{\"_id\":\""+vsc.getPost_id()+"\",\"_source\":[\"rn\",\"bn\"]}");
                        }

                        for(int i = 1; i < hits.length(); i++){
                            item = hits.getJSONObject(i).getJSONObject("_source");
                            vsc = new VSComment(item);
                            comments.add(vsc);
                            if(postInfoMap.get(vsc.getPost_id()) == null){
                                postInfoMap.put(vsc.getPost_id(), new PostInfo());
                                if(strBuilder.length() != 0){
                                    strBuilder.append(",{\"_id\":\""+vsc.getPost_id()+"\",\"_source\":[\"rn\",\"bn\"]}");
                                }
                                else{
                                    strBuilder.append("{\"_id\":\""+vsc.getPost_id()+"\",\"_source\":[\"rn\",\"bn\"]}"); //start without comma since this is the first one appended to the string builder
                                }

                            }
                        }
                        if(strBuilder.length() > 0){
                            String postIDs = "{\"docs\":["+strBuilder.toString()+"]}";
                            addPostInfo(true, postIDs, fromIndex);
                        }
                        else{
                            PostInfo postInfo;
                            for (int j = fromIndex; j<comments.size(); j++){
                                postInfo = postInfoMap.get(comments.get(j).getPost_id());
                                comments.get(j).setR(postInfo.getR()).setB(postInfo.getB());
                            }
                        }

                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            commentsAdapter.notifyDataSetChanged();
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


    private void addPostInfo(boolean multiGet, String payload, int fromIndex) {
        if(problemPosts == null){
            problemPosts = new HashSet<>();
        }
        String query;
        AWSV4Auth aWSV4Auth;
        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);

        if(multiGet){
            query = "/post/post_type/_mget";
            aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
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

                //iterate through hits and put the info in postInfoMap
                JSONObject obj = new JSONObject(strResponse);
                JSONArray hits = obj.getJSONArray("docs");
                for(int i = 0; i<hits.length(); i++){
                    JSONObject item = hits.getJSONObject(i);
                    JSONObject src = item.getJSONObject("_source");
                    String id = null;
                    String rn = null;
                    String bn = null;
                    try{
                        id = item.getString("_id");
                        rn = src.getString("rn");
                        bn = src.getString("bn");
                    }
                    catch (Exception e) {
                        if(id != null){
                            Log.d("skipNullPost", "skipped in CH: " + id);
                            problemPosts.add(id); //trigger a function that deletes this post and its comments
                            deleteProblemPostAndComments();
                        }
                        e.printStackTrace();
                    }

                    if(id == null || rn == null || bn == null){
                        continue;
                    }

                    postInfoMap.get(id).setRB(rn, bn);

                }
                //iterate comments array and add the post info in order starting from fromIndex-th element, using postInfoMap
                PostInfo postInfo;
                for (int j = fromIndex; j<comments.size(); j++){
                    postInfo = postInfoMap.get(comments.get(j).getPost_id());
                    if(postInfo.getR() == null || postInfo.getB() == null){
                        comments.get(j).setR("").setB("");
                    }
                    else{
                        comments.get(j).setR(postInfo.getR()).setB(postInfo.getB());
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }



        }
        else{
            TreeMap<String, String> queryParams = new TreeMap<>();
            queryParams.put("_source", "bn,rn");
            query = "/post/post_type/" + payload + "/_source";
            aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                    .regionName(region)
                    .serviceName("es") // es - elastic search. use your service name
                    .httpMethodName("GET") //GET, PUT, POST, DELETE, etc...
                    .canonicalURI(query) //end point
                    .queryParametes(queryParams) //query parameters if any
                    .awsHeaders(awsHeaders) //aws header parameters
                    .debug() // turn on the debug mode
                    .build();

            String url = "https://" + host + query + "?_source=bn,rn";

            HttpGet httpGet = new HttpGet(url);

		        /* Get header calculated for request */
            Map<String, String> header = aWSV4Auth.getHeaders();
            for (Map.Entry<String, String> entrySet : header.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

                httpGet.addHeader(key, value);
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

                String strResponse = httpClient.execute(httpGet, responseHandler);

                //TODO: there's only one comment in the comments array, add the post info to the fromIndex-th of the comments array
                JSONObject obj = new JSONObject(strResponse);
                //JSONObject item = obj.getJSONObject("_source");
                PostInfo postInfo = new PostInfo();

                String id = null;
                String rn = null;
                String bn = null;
                try{
                    id = payload;
                    rn = obj.getString("rn");
                    bn = obj.getString("bn");
                }catch (Exception e){
                    if(id != null){
                        Log.d("skipNullPost", "skipped in CH: " + id);
                        problemPosts.add(id); //trigger a function that deletes this post and its comments
                        deleteProblemPostAndComments();
                    }
                    e.printStackTrace();
                }
                if(id != null && rn != null && bn != null){
                    postInfo.setRB(rn, bn);
                    comments.get(fromIndex).setR(postInfo.getR()).setB(postInfo.getB());
                    postInfoMap.put(payload, postInfo);
                }

                //System.out.println("Response: " + strResponse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setProfileUsername(String profileUsername){
        if(this.profileUsername != null && !this.profileUsername.equals(profileUsername)){
            this.profileUsername = profileUsername;
            getUserComments(0, "t");
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