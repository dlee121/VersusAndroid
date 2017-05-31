package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapperConfig;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;

import java.util.ArrayList;
import java.util.Iterator;

import com.vs.bcd.versus.R;

/**
 * Created by dlee on 4/29/17.
 */

public class Tab3RandomVS extends Fragment {

    MainContainer mHostActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab3randomvs, container, false);


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
        if (isVisibleToUser) {
            mHostActivity.setToolbarTitleText("Random VS");
        }
    }
}

