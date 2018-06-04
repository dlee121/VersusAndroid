package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.vs.bcd.api.model.PIVModel;
import com.vs.bcd.api.model.PIVModelDocsItem;
import com.vs.bcd.versus.R;

import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.GroupMembersAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupMembersPage extends Fragment {

    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private MainContainer activity;
    private EditText membersFilter;
    private RecyclerView membersRV;
    private LinearLayoutManager mLinearLayoutManager;
    private GroupMembersAdapter groupMembersAdapter;
    private HashMap<String, Integer> profileImgVersions;
    private ArrayList<String> membersList = new ArrayList<>();
    private String mUsername;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.group_members_page, container, false);
        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();

        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        membersFilter = rootView.findViewById(R.id.group_members_filter);
        membersFilter.addTextChangedListener(new TextWatcher() {
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



        membersRV = rootView.findViewById(R.id.group_members_rv);
        mLinearLayoutManager = new LinearLayoutManager(activity);
        membersRV.setLayoutManager(mLinearLayoutManager);
        groupMembersAdapter = new GroupMembersAdapter(membersList, activity, profileImgVersions, false);
        membersRV.setAdapter(groupMembersAdapter);

        disableChildViews();

        return rootView;
    }

    public void setUpMembersList(){
        membersList.clear();
        membersFilter.setText("");
        groupMembersAdapter.notifyDataSetChanged();

        mUsername = activity.getUsername();

        if(profileImgVersions == null){
            profileImgVersions = activity.getMessengerFragment().getProfileImgVersionsArray();
        }
        if(profileImgVersions == null){
            profileImgVersions = new HashMap<>();
        }

        StringBuilder strBuilder = new StringBuilder();
        int i = 0;
        for(String username : activity.getMessageRoom().getUsersList()){
            if(username.indexOf('*') > 0){
                int numberCode = Integer.parseInt(username.substring(username.indexOf('*')+1));
                if(numberCode == 1 || numberCode == 3){
                    username = username.substring(0, username.indexOf('*'));
                    if(username.equals(mUsername)){
                        continue;
                    }
                    membersList.add(username);
                    if(profileImgVersions.get(username) == null){
                        if(i == 0){
                            strBuilder.append("{\"_id\":\""+username+"\",\"_source\":\"pi\"}");
                        }
                        else{
                            strBuilder.append(",{\"_id\":\""+username+"\",\"_source\":\"pi\"}");
                        }
                        i++;
                    }
                }
                else{
                    continue;
                }
            }
            else{
                if(username.equals(mUsername)){
                    continue;
                }
                membersList.add(username);
                if(profileImgVersions.get(username) == null){
                    if(i == 0){
                        strBuilder.append("\""+username+"\"");
                    }
                    else{
                        strBuilder.append(",\""+username+"\"");
                    }
                    i++;
                }
            }
        }
        groupMembersAdapter.setProfileImgVersions(profileImgVersions);


        if(strBuilder.length() > 0){
            final String payload = "{\"ids\":["+strBuilder.toString()+"]}";
            Runnable runnable = new Runnable() {
                public void run() {
                    getProfileImgVersions(payload);
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();
        }

        groupMembersAdapter.notifyDataSetChanged();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer) context;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if(rootView != null){
                enableChildViews();
                //activity.setToolbarTitleText(activity.getMessageRoom().getCurrentRoomTitle());
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
        else if(rootView != null){
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

    private void getProfileImgVersions(String payload){
        try {
            PIVModel pivResult = activity.getClient().pivGet("pis", payload);

            List<PIVModelDocsItem> pivList = pivResult.getDocs();
            if(pivList != null && !pivList.isEmpty()){
                for(PIVModelDocsItem item : pivList){
                    profileImgVersions.put(item.getId(), item.getSource().getPi().intValue());
                }
            }
            if(groupMembersAdapter != null){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        groupMembersAdapter.notifyDataSetChanged();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void filter(String text){
        if(text.isEmpty()){
            groupMembersAdapter.updateList(membersList);
        }
        else{
            ArrayList<String> temp = new ArrayList<>();
            for(String username: membersList) {
                if (username.toLowerCase().contains(text.toLowerCase())) {
                    temp.add(username);
                }
            }

            groupMembersAdapter.updateList(temp);
        }

    }

}
