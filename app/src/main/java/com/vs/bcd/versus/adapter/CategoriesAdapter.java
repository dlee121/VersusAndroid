package com.vs.bcd.versus.adapter;

import android.app.Activity;
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

import de.hdodenhof.circleimageview.CircleImageView;


public class CategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity activity;
    private List<CategoryObject> categories;

    public CategoriesAdapter(RecyclerView recyclerView, List<CategoryObject> categories, Activity activity) {
        this.categories = categories;
        this.activity = activity;
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

        //listener for when user clicks on the category they wish to select
        categoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("CATEGORY SELECT", "clicked " + categoryObject.getCategoryName() + ", code: " + categoryObject.getCategoryInt());
                //TODO: this clicked item is the selected category, put that back on to CreatePost category box and navigate back to CreatePost frag
                ((MainContainer)activity).getCreatePostFragment().setCatSelection(categoryObject.getCategoryName(), categoryObject.getCategoryInt());
                ((MainContainer)activity).getViewPager().setCurrentItem(2);
            }
        });
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
            catIcon = (ImageView) view.findViewById(R.id.category_ic_iv);
            catName = (TextView) view.findViewById(R.id.tv_category);
        }
    }
}