package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.LeaderboardAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.LeaderboardEntry;
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
 * Created by dlee on 8/6/17.
 */



public class LeaderboardTab extends Fragment {
    private View rootView;
    private ArrayList<LeaderboardEntry> leaders;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private MainContainer activity;
    private long lastRefreshTime = 0;
    RecyclerView recyclerView;
    LeaderboardAdapter mLeaderboardAdapter;
    private ProgressBar lbProgressBar;

    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.leaderboard, container, false);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        leaders = new ArrayList<>();

        recyclerView = rootView.findViewById(R.id.leaderboard_rv);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        mLeaderboardAdapter = new LeaderboardAdapter(leaders, activity);
        recyclerView.setAdapter(mLeaderboardAdapter);

        //recyclerview preloader setup
        ListPreloader.PreloadSizeProvider sizeProvider =
                new FixedPreloadSizeProvider(activity.getResources().getDimensionPixelSize(R.dimen.profile_img_general), activity.getResources().getDimensionPixelSize(R.dimen.profile_img_general));
        RecyclerViewPreloader<LeaderboardEntry> preloader =
                new RecyclerViewPreloader<>(Glide.with(activity), mLeaderboardAdapter, sizeProvider, 20);
        recyclerView.addOnScrollListener(preloader);


        lbProgressBar = rootView.findViewById(R.id.lb_progressbar);

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        //setUpLeaderboard();

        disableChildViews();
        Log.d("leaderboard", "lb set up complete and disablechildViews called");
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //save the activity to a member of this fragment
        activity = (MainContainer)context;
        Log.d("leaderboard", "onAttach() called");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                Log.d("leaderboard", "now visible");
                enableChildViews();
                setUpLeaderboard();
            }
        }
        else {
            if (rootView != null){
                disableChildViews();
            }
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

    private void setUpLeaderboard(){
        leaders.clear();
        mLeaderboardAdapter.notifyDataSetChanged();
        lbProgressBar.setVisibility(View.VISIBLE);

        Runnable runnable = new Runnable() {
            public void run() {
                String query = "/user/_search";
                String payload;

                payload = "{\"size\":100,\"sort\":[{\"in\":{\"order\":\"desc\"}}, {\"g\":{\"order\":\"desc\"}}, {\"s\":{\"order\":\"desc\"}}, {\"b\":{\"order\":\"desc\"}}, {\"@t\":{\"order\":\"desc\"}}],\"_source\":[\"b\",\"g\",\"in\",\"pi\",\"s\"],\"query\":{\"match_all\":{}}}";


                String host = activity.getESHost();
                String region = activity.getESRegion();
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

                    for(int i = 0; i < hits.length(); i++){
                        JSONObject item = hits.getJSONObject(i).getJSONObject("_source");
                        String id = hits.getJSONObject(i).getString("_id");
                        leaders.add(new LeaderboardEntry(item, id));
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lbProgressBar.setVisibility(View.GONE);
                            mLeaderboardAdapter.notifyDataSetChanged();
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

    private String getUsernameHash(String usernameIn){
        int usernameHash;
        if(usernameIn.length() < 5){
            usernameHash = usernameIn.hashCode();
        }
        else{
            String hashIn = "" + usernameIn.charAt(0) + usernameIn.charAt(usernameIn.length() - 2) + usernameIn.charAt(1) + usernameIn.charAt(usernameIn.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        return Integer.toString(usernameHash);
    }

}
