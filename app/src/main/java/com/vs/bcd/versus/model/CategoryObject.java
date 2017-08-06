package com.vs.bcd.versus.model;

import android.util.Log;

import static android.R.attr.id;

/**
 * Created by dlee on 8/3/17.
 */

public class CategoryObject {
    private String categoryName = "";
    private int iconResID = -1;
    private int categoryInt = -1;

    public CategoryObject(String categoryName, int iconResID, int categoryInt) {
        this.categoryName = categoryName;
        this.iconResID = iconResID;
        this.categoryInt = categoryInt;
        Log.d("ACTV", "Category Object created for " + toString());
    }

    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getIconResID() {
        return iconResID;
    }
    public void setIconResID(int iconResID) {
        this.iconResID = iconResID;
    }

    public int getCategoryInt(){
        return categoryInt;
    }
    public void setCategoryInt(int categoryInt){
        this.categoryInt = categoryInt;
    }

    @Override
    public String toString() {
        return this.categoryName;
    }
}