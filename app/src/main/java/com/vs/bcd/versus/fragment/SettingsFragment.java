package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.adapter.CategoriesAdapter;
import com.vs.bcd.versus.adapter.SettingsAdapter;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.SettingObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dlee on 4/29/17.
 */

public class SettingsFragment extends Fragment {
    private View rootView;
    private ArrayList<SettingObject> settingObjects;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private SettingsAdapter mSettingsAdapter;
    private MainContainer activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.settings_fragment, container, false);

        settingObjects = new ArrayList<>();
        setUpSettings();

        RecyclerView recyclerView = rootView.findViewById(R.id.settings_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        mSettingsAdapter = new SettingsAdapter(settingObjects, activity);
        recyclerView.setAdapter(mSettingsAdapter);

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
        activity = (MainContainer) context;
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

    private void setUpSettings(){
        settingObjects.add(new SettingObject("Log Out"));
        if(activity.isUserNative()){
            settingObjects.add(new SettingObject("Set Up Email for Account Recovery"));
        }
        settingObjects.add(new SettingObject("About"));
    }

}
