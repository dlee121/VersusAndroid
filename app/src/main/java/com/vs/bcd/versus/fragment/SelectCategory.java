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

/**
 * Created by dlee on 8/6/17.
 */



public class SelectCategory extends Fragment {
    private View rootView;
    private ArrayList<CategoryObject> categories;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private CategoriesAdapter mCategoriesAdapter;
    private EditText categoryFilterET;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.category_selection_createpost_frag, container, false);

        categories = new ArrayList<>();
        setUpCategoriesList();

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.category_selection_rvcp);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCategoriesAdapter = new CategoriesAdapter(recyclerView, categories, getActivity());
        recyclerView.setAdapter(mCategoriesAdapter);

        categoryFilterET = (EditText)rootView.findViewById(R.id.cat_selection_filterbox);
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
            Log.d("VISIBLE", "SEARCH VISIBLE");
            if(rootView != null)
                enableChildViews();
        }
        else {
            Log.d("VISIBLE", "SEARCH POST GONE");
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

    private void setUpCategoriesList(){
        categories.add(new CategoryObject("Cars", R.drawable.goldmedal, 0));  //TODO: set actual icons, for now gold medal as placeholder
        categories.add(new CategoryObject("Celebrities", R.drawable.goldmedal, 1));
        categories.add(new CategoryObject("Culture", R.drawable.goldmedal, 2));
        categories.add(new CategoryObject("Education", R.drawable.goldmedal, 3));
        categories.add(new CategoryObject("Ethics/Morality", R.drawable.goldmedal, 4));
        categories.add(new CategoryObject("Fashion", R.drawable.goldmedal, 5));
        categories.add(new CategoryObject("Fiction", R.drawable.goldmedal, 6));
        categories.add(new CategoryObject("Finance", R.drawable.goldmedal, 7));
        categories.add(new CategoryObject("Food", R.drawable.goldmedal, 8));
        categories.add(new CategoryObject("Game", R.drawable.goldmedal, 9));
        categories.add(new CategoryObject("Law", R.drawable.goldmedal, 10));
        categories.add(new CategoryObject("Movies", R.drawable.goldmedal, 11));
        categories.add(new CategoryObject("Music/Artists", R.drawable.goldmedal, 12));
        categories.add(new CategoryObject("Politics", R.drawable.goldmedal, 13));
        categories.add(new CategoryObject("Porn", R.drawable.goldmedal, 14));
        categories.add(new CategoryObject("Religion", R.drawable.goldmedal, 15));
        categories.add(new CategoryObject("Restaurants", R.drawable.goldmedal, 16));
        categories.add(new CategoryObject("Science", R.drawable.goldmedal, 17));
        categories.add(new CategoryObject("Sex", R.drawable.goldmedal, 18));
        categories.add(new CategoryObject("Sports", R.drawable.goldmedal, 19));
        categories.add(new CategoryObject("Technology", R.drawable.goldmedal, 20));
        categories.add(new CategoryObject("Travel", R.drawable.goldmedal, 21));
        categories.add(new CategoryObject("TV Shows", R.drawable.goldmedal, 22));
        categories.add(new CategoryObject("Weapons", R.drawable.goldmedal, 23));
    }

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

}
