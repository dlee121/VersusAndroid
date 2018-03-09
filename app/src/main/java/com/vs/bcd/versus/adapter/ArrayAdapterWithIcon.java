package com.vs.bcd.versus.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vs.bcd.versus.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dlee on 9/9/17.
 */

public class ArrayAdapterWithIcon extends ArrayAdapter<String> {

    private List<Integer> images;
    private Context context;

    public ArrayAdapterWithIcon(Context context, List<String> items, List<Integer> images) {
        super(context, android.R.layout.select_dialog_item, items);
        this.context = context;
        this.images = images;
    }

    public ArrayAdapterWithIcon(Context context, String[] items, Integer[] images) {
        super(context, android.R.layout.select_dialog_item, items);
        this.context = context;
        this.images = Arrays.asList(images);
    }

    /*
        public ArrayAdapterWithIcon(Context context, int items, int images) {
            super(context, android.R.layout.select_dialog_item, context.getResources().getTextArray(items));

            final TypedArray imgs = context.getResources().obtainTypedArray(images);
            this.images = new ArrayList<Integer>() {{ for (int i = 0; i < imgs.length(); i++) {add(imgs.getResourceId(i, -1));} }};

            // recycle the array
            imgs.recycle();
        }
    */

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        int height = context.getResources().getDimensionPixelSize(R.dimen.overflow_row_height); //8dp
        AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height);
        view.setLayoutParams(layoutParams);

        TextView textView = view.findViewById(android.R.id.text1);
        textView.setTextSize(18);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(images.get(position), 0, 0, 0);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(images.get(position), 0, 0, 0);
        }
        textView.setCompoundDrawablePadding(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics()));
        return view;
    }

}
