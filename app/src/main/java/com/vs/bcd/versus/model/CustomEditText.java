package com.vs.bcd.versus.model;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
    private int viewTreeCount = 0;

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if(viewTreeCount != 0) {
                return true;
            }
            viewTreeCount++;

            if(isInUse()){
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                mgr.hideSoftInputFromWindow(getWindowToken(), 0);
                                ((MainContainer)getContext()).getPostPage().hideCommentInputCursor();
                                viewTreeCount = 0;
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                viewTreeCount = 0;

                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure? The text you entered will be discarded.").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
            else{
                // User has pressed Back key. So hide the keyboard
                InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(getWindowToken(), 0);
                ((MainContainer)getContext()).getPostPage().hideCommentInputCursor();
            }
        }
        return true;
    }

    @Override
    public void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect){
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if(gainFocus){
            viewTreeCount = 0;
        }
        else{
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

    public boolean isInUse(){
        if(prefix == null){
            return getText().length() > 0;
        }
        else{
            return !(getText().toString().equals(prefix));
        }
    }
}