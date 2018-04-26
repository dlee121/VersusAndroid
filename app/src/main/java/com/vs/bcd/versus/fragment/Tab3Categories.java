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
import com.vs.bcd.versus.model.CategoryObject;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by dlee on 8/6/17.
 */



public class Tab3Categories extends Fragment {
    private View rootView;
    private ArrayList<CategoryObject> categories;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private CategoriesAdapter mCategoriesAdapter;
    //private EditText categoryFilterET;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab3categories, container, false);

        categories = new ArrayList<>();
        ((MainContainer)getActivity()).setUpCategoriesList(categories);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.category_selection_rvt3);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCategoriesAdapter = new CategoriesAdapter(recyclerView, categories, getActivity(), 1);
        recyclerView.setAdapter(mCategoriesAdapter);
        /*
        categoryFilterET = (EditText)rootView.findViewById(R.id.tab3catselect_filterbox);
        categoryFilterET.addTextChangedListener(new TextWatcher() {
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
        */


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
    /*
    void filter(String text){
        List<CategoryObject> temp = new ArrayList<>();
        for(CategoryObject co: categories){
            if(co.getCategoryName().toLowerCase().contains(text.toLowerCase())){
                temp.add(co);
            }
        }
        //update recyclerview
        mCategoriesAdapter.updateList(temp);
        Log.d("Categories", "still has same " + Integer.toString(categories.size()) + "items");
    }
    */

}
