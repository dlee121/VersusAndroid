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

    static String host = "search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com";
    static String region = "us-east-1";



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.comments_history, container, false);

        comments = new ArrayList<>();

        recyclerView = rootView.findViewById(R.id.recycler_view_ch);

        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        //this is where the list is passed on to adapter
        commentsAdapter = new CommentHistoryAdapter(comments, activity);
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
                            getUserComments(currCommentsIndex, "u");
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
        getUserComments(0, "u");
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
                    if(hits.length() == 0){
                        Log.d("loadmore", "end reached, disabling loadMore");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                        return;
                    }
                    for(int i = 0; i < hits.length(); i++){
                        JSONObject item = hits.getJSONObject(i).getJSONObject("_source");
                        comments.add(new VSComment(item));
                        currCommentsIndex++;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            commentsAdapter.notifyDataSetChanged();
                            mSwipeRefreshLayout.setRefreshing(false);
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
            getUserComments(0, "u");
        }
    }


}