package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.R;

import java.util.List;


public class CategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private MainContainer activity;
    private List<CategoryObject> categories;
    private int createpostORtab3 = 0;   //0 = for CreatePost, 1 = for Tab3New, 2 = Trending Filter
    private Dialog currentDialog;

    public CategoriesAdapter(RecyclerView recyclerView, List<CategoryObject> categories, MainContainer activity, int createpostORtab3) {
        this.categories = categories;
        this.activity = activity;
        this.createpostORtab3 = createpostORtab3;
    }

    public CategoriesAdapter(RecyclerView recyclerView, List<CategoryObject> categories, MainContainer activity, int createpostORtab3, Dialog currentDialog) {
        this.categories = categories;
        this.activity = activity;
        this.createpostORtab3 = createpostORtab3;
        this.currentDialog = currentDialog;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.category_row, parent, false);
        return new CategoryViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        //TODO:this is where values are put into the layout, from the post object
        final CategoryObject categoryObject = categories.get(position);
        CategoryViewHolder categoryViewHolder = (CategoryViewHolder) holder;

        categoryViewHolder.catIcon.setImageResource(categoryObject.getIconResID());
        categoryViewHolder.catName.setText(categoryObject.getCategoryName());


        switch (createpostORtab3){
            case 0:
                //CreatePost listener for when user clicks on the category they wish to select for creating post
                categoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Log.d("CATEGORY SELECT", "clicked " + categoryObject.getCategoryName() + ", code: " + categoryObject.getCategoryInt());
                        activity.getCreatePostFragment().setCatSelection(categoryObject.getCategoryName(), categoryObject.getCategoryInt());
                        activity.getViewPager().setCurrentItem(2);
                    }
                });
                break;
            case 1:
                //Tab3New listener for when user clicks on the category they wish to view posts from

                break;

            case 2:
                //Tab1Trending category filter
                categoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.setTab2CategoryFilter(position, categoryObject.getIconResID(), categoryObject.getCategoryName());
                        currentDialog.dismiss();
                    }
                });

                break;
            case 3:
                //Tab3New category filter
                categoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.setTab3CategoryFilter(position, categoryObject.getIconResID(), categoryObject.getCategoryName());
                        currentDialog.dismiss();
                    }
                });

                break;

            default:

                break;
        }


    }

    @Override
    public int getItemCount() {
        return categories == null ? 0 : categories.size();
    }

    private class CategoryViewHolder extends RecyclerView.ViewHolder {

        public ImageView catIcon;   //maybe switch to circular colorful icons
        public TextView catName;

        public CategoryViewHolder(View view) {
            super(view);
            catIcon = view.findViewById(R.id.category_ic_iv);
            catName = view.findViewById(R.id.tv_category);
        }
    }

    public void updateList(List<CategoryObject> list){
        categories = list;
        notifyDataSetChanged();
    }
}