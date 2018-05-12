package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.fragment.CommentsHistory;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.NotificationItem;
import com.vs.bcd.versus.model.User;
import com.vs.bcd.versus.model.VSComment;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CommentHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private MainContainer activity;
    private List<VSComment> comments;
    private CommentsHistory commentsHistory;
    private Toast mToast;
    private boolean itemViewClickLock;

    public CommentHistoryAdapter(List<VSComment> comments, MainContainer activity, CommentsHistory commentsHistory) {
        this.comments = comments;
        this.activity = activity;
        this.commentsHistory = commentsHistory;
        itemViewClickLock = false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.comment_history_item, parent, false);
        return new CommentHistoryViewHolder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        //TODO:this is where values are put into the layout, from the post object
        final VSComment itemComment = comments.get(position);


        CommentHistoryViewHolder commentHistoryViewHolder = (CommentHistoryViewHolder) holder;

        if(!commentsHistory.skipThisComment(itemComment.getPost_id())){
            commentHistoryViewHolder.redTv.setText(itemComment.getR());
            commentHistoryViewHolder.blueTv.setText(itemComment.getB());
            commentHistoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(itemViewClickLock){
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                itemViewClickLock = false;
                            }
                        }, 2000);
                    }
                    else{
                        if(itemComment.getParent_id().equals(itemComment.getPost_id())){ //clicked item is root comment
                            activity.getPostPage().rootCommentHistoryItemClicked(itemComment, true, "");
                        }
                        else{
                            activity.getPostPage().childOrGrandchildHistoryItemClicked(itemComment, true, "");
                        }

                        itemViewClickLock = true;

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                itemViewClickLock = false;
                            }
                        }, 2000);
                    }

                }
            });
        }
        else{
            commentHistoryViewHolder.redTv.setText("");
            commentHistoryViewHolder.blueTv.setText("");
            commentHistoryViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mToast != null){
                        mToast.cancel();
                    }
                    mToast = Toast.makeText(activity, "This comment is unavailable", Toast.LENGTH_SHORT);
                    mToast.show();
                }
            });
        }

        commentHistoryViewHolder.timeTV.setText(getTimeString(itemComment.getTime()));
        commentHistoryViewHolder.upvotes.setText(Integer.toString(itemComment.getUpvotes()));
        commentHistoryViewHolder.downvotes.setText(Integer.toString(itemComment.getDownvotes()));
        commentHistoryViewHolder.content.setText(itemComment.getContent());

        switch (itemComment.getTopmedal()){
            case 0:
                commentHistoryViewHolder.medalView.setImageResource(android.R.color.transparent);
                break; //no medal, default currentMedal value
            case 1: //bronze
                commentHistoryViewHolder.medalView.setImageResource(R.drawable.ic_bronze_medal);
                break;
            case 2: //silver
                commentHistoryViewHolder.medalView.setImageResource(R.drawable.ic_silver_medal);
                break;
            case 3: //gold
                commentHistoryViewHolder.medalView.setImageResource(R.drawable.ic_gold_medal);
                break;
        }




    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    private class CommentHistoryViewHolder extends RecyclerView.ViewHolder {

        //public TextView itemHeading;   //maybe switch to circular colorful icons
        public TextView redTv;
        public TextView blueTv;
        public TextView timeTV;
        public ImageView medalView;
        public TextView content;
        public TextView upvotes, downvotes;

        public CommentHistoryViewHolder(View view) {
            super(view);
            redTv = view.findViewById(R.id.red_chi);
            blueTv = view.findViewById(R.id.blue_chi);
            timeTV = view.findViewById(R.id.time_chi);
            upvotes = view.findViewById(R.id.upvotes_chi);
            downvotes = view.findViewById(R.id.downvotes_chi);
            medalView = view.findViewById(R.id.medal_chi);
            content = view.findViewById(R.id.usercomment_history);
        }
    }

    public String getTimeString(String timeStamp){
        int timeFormat = 0;

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        Date myDate = null;
        try {
            myDate = df.parse(timeStamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //TODO: test all possible cases to make sure date format conversion works correctly, for seconds, for all time format constants (secs, mins, ... , years), singulars / plurals
        long timediff = ((new Date()).getTime() - myDate.getTime()) / 1000;  //time elapsed since post creation, in seconds

        //time format constants: 0 = seconds, 1 = minutes, 2 = hours, 3 = days , 4 = weeks, 5 = months, 6 = years
        if(timediff >= 60) {  //if 60 seconds or more, convert to minutes
            timediff /= 60;
            timeFormat = 1;
            if(timediff >= 60) { //if 60 minutes or more, convert to hours
                timediff /= 60;
                timeFormat = 2;
                if(timediff >= 24) { //if 24 hours or more, convert to days
                    timediff /= 24;
                    timeFormat = 3;

                    if(timediff >= 365) { //if 365 days or more, convert to years
                        timediff /= 365;
                        timeFormat = 6;
                    }

                    else if (timeFormat < 6 && timediff >= 30) { //if 30 days or more and not yet converted to years, convert to months
                        timediff /= 30;
                        timeFormat = 5;
                    }

                    else if(timeFormat < 5 && timediff >= 7) { //if 7 days or more and not yet converted to months or years, convert to weeks
                        timediff /= 7;
                        timeFormat = 4;
                    }

                }
            }
        }


        if(timediff > 1) //if timediff is not a singular value
            timeFormat += 7;

        switch (timeFormat) {
            //plural
            case 7:
                return String.valueOf(timediff) + " seconds ago";
            case 8:
                return String.valueOf(timediff) + " minutes ago";
            case 9:
                return String.valueOf(timediff) + " hours ago";
            case 10:
                return String.valueOf(timediff) + " days ago";
            case 11:
                return String.valueOf(timediff) + " weeks ago";
            case 12:
                return String.valueOf(timediff) + " months ago";
            case 13:
                return String.valueOf(timediff) + " years ago";

            //singular
            case 0:
                return String.valueOf(timediff) + " second ago";
            case 1:
                return String.valueOf(timediff) + " minute ago";
            case 2:
                return String.valueOf(timediff) + " hour ago";
            case 3:
                return String.valueOf(timediff) + " day ago";
            case 4:
                return String.valueOf(timediff) + " week ago";
            case 5:
                return String.valueOf(timediff) + " month ago";
            case 6:
                return String.valueOf(timediff) + " year ago";

            default:
                return "";
        }
    }

    public void unlockItemViewClickLock(){
        itemViewClickLock = false;
    }
}