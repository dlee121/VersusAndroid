package com.vs.bcd.versus.model;

import android.content.Context;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatImageView;

public class SquareImageView extends AppCompatImageView{

    public SquareImageView(Context context){
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, (width * 56) / 45);
    }



}
