package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.GroupMembersAdapter;
import com.vs.bcd.versus.model.AWSV4Auth;

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

public class FollowersAndFollowings extends Fragment {

    private View rootView;
    private MainContainer activity;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;

    private HashMap<String, Integer> profileImgVersions = new HashMap<>();

    private boolean fromProfile = false;

    private RecyclerView ffRecyclerView;
    private EditText usersFilter;

    private LinearLayoutManager mLinearLayoutManager;

    private ArrayList<String> usersList = new ArrayList<>();
    private GroupMembersAdapter usersAdapter;
    private String mUsername = "";
    String userPath = "";
    private boolean followersMode = true;
    private Stack<String> ffStack = new Stack<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.followers_and_followings, container, false);

        ffRecyclerView = rootView.findViewById(R.id.ff_rv);
        mLinearLayoutManager = new LinearLayoutManager(activity);
        ffRecyclerView.setLayoutManager(mLinearLayoutManager);
        usersAdapter = new GroupMembersAdapter(usersList, activity, profileImgVersions, true);
        ffRecyclerView.setAdapter(usersAdapter);

        usersFilter = rootView.findViewById(R.id.ff_filter);
        usersFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
                //you can use runnable postDelayed like 500 ms to delay search text
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
        activity = (MainContainer)context;
    }



    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (rootView != null){
                enableChildViews();
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
        else if (rootView != null){
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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

    public void setUpFollowersPage(boolean fromProfile, String profileUsername){
        this.fromProfile = fromProfile;
        activity.setToolbarTitleText("Followers");
        followersMode = true;

        if(profileUsername.equals(activity.getUsername())){
            userPath = activity.getUserPath();
        }
        else{
            userPath = Integer.toString(getUsernameHash(profileUsername)) + "/" + profileUsername + "/";
        }

        usersList.clear();
        usersFilter.setText("");
        usersAdapter.notifyDataSetChanged();

        mUsername = profileUsername;

        if(profileImgVersions == null){
            profileImgVersions = activity.getMessengerFragment().getProfileImgVersionsArray();
        }
        if(profileImgVersions == null){
            profileImgVersions = new HashMap<>();
        }

        activity.getmFirebaseDatabaseReference().child(userPath+"h").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshotH) {

                activity.getViewPager().setCurrentItem(6);

                activity.getmFirebaseDatabaseReference().child(userPath+"f").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshotF) {

                        StringBuilder strBuilder = new StringBuilder();
                        int i = 0;
                        for(DataSnapshot followerH : dataSnapshotH.getChildren()){
                            usersList.add(followerH.getKey());
                            if(profileImgVersions.get(followerH.getKey()) == null){
                                if(i == 0){
                                    strBuilder.append("{\"_id\":\""+followerH.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                else{
                                    strBuilder.append(",{\"_id\":\""+followerH.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                i++;
                            }
                        }
                        for(DataSnapshot followerF : dataSnapshotF.getChildren()){
                            usersList.add(followerF.getKey());
                            if(profileImgVersions.get(followerF.getKey()) == null){
                                if(i == 0){
                                    strBuilder.append("{\"_id\":\""+followerF.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                else{
                                    strBuilder.append(",{\"_id\":\""+followerF.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                i++;
                            }
                        }

                        usersAdapter.setProfileImgVersions(profileImgVersions);

                        if(strBuilder.length() > 0){
                            final String payload = "{\"docs\":["+strBuilder.toString()+"]}";
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    getProfileImgVersions(payload);
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }

                        usersAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void setUpFollowingsPage(boolean fromProfile, String profileUsername){
        this.fromProfile = fromProfile;
        activity.setToolbarTitleText("Following");
        followersMode = false;

        if(profileUsername.equals(activity.getUsername())){
            userPath = activity.getUserPath();
        }
        else{
            userPath = Integer.toString(getUsernameHash(profileUsername)) + "/" + profileUsername + "/";
        }

        usersList.clear();
        usersFilter.setText("");
        usersAdapter.notifyDataSetChanged();

        mUsername = profileUsername;

        if(profileImgVersions == null){
            profileImgVersions = activity.getMessengerFragment().getProfileImgVersionsArray();
        }
        if(profileImgVersions == null){
            profileImgVersions = new HashMap<>();
        }

        activity.getmFirebaseDatabaseReference().child(userPath+"h").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshotH) {

                activity.getViewPager().setCurrentItem(6);

                activity.getmFirebaseDatabaseReference().child(userPath+"g").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshotG) {

                        StringBuilder strBuilder = new StringBuilder();
                        int i = 0;
                        for(DataSnapshot followingH : dataSnapshotH.getChildren()){
                            usersList.add(followingH.getKey());
                            if(profileImgVersions.get(followingH.getKey()) == null){
                                if(i == 0){
                                    strBuilder.append("{\"_id\":\""+followingH.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                else{
                                    strBuilder.append(",{\"_id\":\""+followingH.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                i++;
                            }
                        }
                        for(DataSnapshot followingG : dataSnapshotG.getChildren()){
                            usersList.add(followingG.getKey());
                            if(profileImgVersions.get(followingG.getKey()) == null){
                                if(i == 0){
                                    strBuilder.append("{\"_id\":\""+followingG.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                else{
                                    strBuilder.append(",{\"_id\":\""+followingG.getKey()+"\",\"_source\":\"pi\"}");
                                }
                                i++;
                            }
                        }

                        usersAdapter.setProfileImgVersions(profileImgVersions);

                        if(strBuilder.length() > 0){
                            final String payload = "{\"docs\":["+strBuilder.toString()+"]}";
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    getProfileImgVersions(payload);
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }

                        usersAdapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public boolean isFromProfile(){
        return fromProfile;
    }

    private void getProfileImgVersions(String payload){
        String query = "/user/user_type/_mget";
        String host = activity.getESHost();
        String region = activity.getESRegion();
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

            //iterate through hits and put the info in postInfoMap
            JSONObject obj = new JSONObject(strResponse);
            JSONArray hits = obj.getJSONArray("docs");
            for(int i = 0; i<hits.length(); i++){
                JSONObject item = hits.getJSONObject(i);
                JSONObject src = item.getJSONObject("_source");
                profileImgVersions.put(item.getString("_id"), src.getInt("pi"));
            }

            if(usersAdapter != null){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        usersAdapter.notifyDataSetChanged();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void filter(String text){
        if(text.isEmpty()){
            usersAdapter.updateList(usersList);
        }
        else{
            ArrayList<String> temp = new ArrayList<>();
            for(String username: usersList) {
                if (username.toLowerCase().contains(text.toLowerCase())) {
                    temp.add(username);
                }
            }

            usersAdapter.updateList(temp);
        }

    }

    private int getUsernameHash(String username){
        int usernameHash;
        if(username.length() < 5){
            usernameHash = username.hashCode();
        }
        else {
            String hashIn = "" + username.charAt(0) + username.charAt(username.length() - 2) + username.charAt(1) + username.charAt(username.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        return usernameHash;
    }

    public boolean isFollowersMode(){
        return followersMode;
    }

    public void ffStackPush(){
        if(ffStack == null){
            ffStack = new Stack<>();
        }

        if(followersMode){
            ffStack.push(mUsername+":f");
        }
        else{
            ffStack.push(mUsername+":g");
        }
    }
    public String ffStackPop(){
        if(ffStack == null || ffStack.isEmpty()){
            return null;
        }
        return ffStack.pop();
    }
    public boolean ffStackIsEmpty(){
        if(ffStack == null || ffStack.isEmpty()){
            return true;
        }
        return false;
    }

    public void clearStack(){
        if(ffStack == null){
            ffStack = new Stack<>();
        }
        else{
            ffStack.clear();
        }
    }

}