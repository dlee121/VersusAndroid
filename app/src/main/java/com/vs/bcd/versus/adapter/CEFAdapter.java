package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.PostPage;
import com.vs.bcd.versus.model.CEFObject;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.VSComment;

import java.util.HashMap;
import java.util.List;


public class CEFAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Activity activity;
    private List<CEFObject> cefObjects;
    private int VIEW_TYPE_POST = 0;
    private int VIEW_TYPE_COMMENT = 1;
    private int VIEW_TYPE_TXTINPUT = 2;
    private EditText textInput;

    public CEFAdapter(List<CEFObject> cefObjects, MainContainer activity) {
        this.cefObjects = cefObjects;
        this.activity = activity;
    }

    @Override
    public int getItemViewType(int position) {
        return cefObjects.get(position).getCefObjectType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_POST){
            View view = LayoutInflater.from(activity).inflate(R.layout.cef_post_card, parent, false);
            return new CEFPostViewHolder(view);
        }
        else if(viewType == VIEW_TYPE_COMMENT){
            View view = LayoutInflater.from(activity).inflate(R.layout.cef_comment_card, parent, false);
            return new CEFCommentViewHolder(view);
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.cef_text_input, parent, false);
        return new CEFTextInputViewHolder(view);

    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof CEFPostViewHolder){
            CEFPostViewHolder cefPostViewHolder = (CEFPostViewHolder) holder;
            CEFObject cefObject = cefObjects.get(position);
            cefPostViewHolder.questionTV.setText(cefObject.getQuestion());
            cefPostViewHolder.rnameTV.setText(cefObject.getRname());
            cefPostViewHolder.bnameTV.setText(cefObject.getBname());
        }
        else if(holder instanceof CEFCommentViewHolder){
            CEFCommentViewHolder cefCommentViewHolder = (CEFCommentViewHolder) holder;
            CEFObject cefObject = cefObjects.get(position);
            cefCommentViewHolder.usernameTV.setText(cefObject.getUsername());
            cefCommentViewHolder.contentTV.setText(cefObject.getContent());
        }
        else if(holder instanceof CEFTextInputViewHolder){
            CEFTextInputViewHolder cefTextInputViewHolder = (CEFTextInputViewHolder) holder;
            textInput = cefTextInputViewHolder.cefTextInput;
            String text = cefObjects.get(position).getText();
            if(text != null && !text.equals("")){
                textInput.setText(text);
            }
        }
    }

    @Override
    public int getItemCount() {
        return cefObjects == null ? 0 : cefObjects.size();
    }

    private class CEFPostViewHolder extends RecyclerView.ViewHolder {

        public TextView questionTV, rnameTV, bnameTV;

        public CEFPostViewHolder(View view) {
            super(view);
            questionTV = view.findViewById(R.id.cef_qtxt);
            rnameTV = view.findViewById(R.id.cef_rtxt);
            bnameTV = (TextView) view.findViewById(R.id.cef_btxt);
        }
    }

    private class CEFCommentViewHolder extends RecyclerView.ViewHolder {

        //public TextView itemHeading;   //maybe switch to circular colorful icons
        public TextView usernameTV, contentTV;

        public CEFCommentViewHolder(View view) {
            super(view);
            usernameTV = view.findViewById(R.id.cef_username);
            contentTV = view.findViewById(R.id.cef_content);
        }
    }

    private class CEFTextInputViewHolder extends RecyclerView.ViewHolder {

        //public TextView itemHeading;   //maybe switch to circular colorful icons
        public EditText cefTextInput;

        public CEFTextInputViewHolder(View view) {
            super(view);
            cefTextInput = view.findViewById(R.id.cef_text_input);
        }
    }

    public String getTextInput(){
        String textIn = textInput.getText().toString().trim();
        if(textIn != null && textIn.length() > 8888){ //limits comments to 8888 characters
            textIn = textIn.substring(0, 8888).concat("...");
        }
        return textIn;
    }

    public void clearTextInput(){
        if(textInput != null){
            textInput.setText("");
        }
    }

}