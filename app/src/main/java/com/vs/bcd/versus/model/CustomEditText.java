package com.vs.bcd.versus.model;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.support.v7.widget.AppCompatEditText;

import com.vs.bcd.versus.activity.MainContainer;

public class CustomEditText extends AppCompatEditText {

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private String prefix;

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // User has pressed Back key. So hide the keyboard
            InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(this.getWindowToken(), 0);

            ((MainContainer)getContext()).getPostPage().hideCommentInputCursor();

        }
        return true;
    }

    @Override
    public void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect){
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if(!gainFocus){
            prefix = null;
            ((MainContainer)getContext()).getPostPage().clearReplyingTo();
        }
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        if(prefix != null){
            CharSequence text = getText();
            if (text != null) {
                if (start < prefix.length() || end < prefix.length()) {
                    Log.d("iggystore", "start: " + Integer.toString(start) + ", end: " + Integer.toString(end) + ", prefix: " + prefix);
                    if(getText().length() >= prefix.length()){
                        setSelection(prefix.length());
                    }

                    return;
                }
            }
        }

        super.onSelectionChanged(start, end);
    }

    public void setPrefix(String prefix) {
        Log.d("iggystore", prefix);
        this.prefix = prefix;
        if(getText().toString().equals(prefix)){
            setSelection(prefix.length());
        }
    }

    public boolean hasPrefix(){
        return (prefix != null);
    }

    public String getPrefix(){
        return prefix;
    }
}