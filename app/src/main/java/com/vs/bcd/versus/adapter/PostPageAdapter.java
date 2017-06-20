package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.activity.PhoneOrEmail;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.VSComment;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.R.attr.level;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;


public class PostPageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean isLoading;
    private Activity activity;
    private List<VSComment> vsComments;
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;

    public PostPageAdapter(RecyclerView recyclerView, List<VSComment> vsComments, Activity activity) {
        this.vsComments = vsComments;
        this.activity = activity;

        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if (onLoadMoreListener != null) {
                        onLoadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }
        });
    }

    public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
        this.onLoadMoreListener = mOnLoadMoreListener;
    }

    @Override
    public int getItemViewType(int position) {
        return vsComments.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(activity).inflate(R.layout.comment_group_card, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof UserViewHolder) {

            VSComment currentComment = vsComments.get(position);

            setLeftMargin(((UserViewHolder) holder).circView, 150 * currentComment.getNestedLevel());  //left margin (indentation) of 150dp per nested level

            //set onClickListener for profile pic
            ((UserViewHolder) holder).circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    profileClicked(v);
                }
            });

            //this is where values are put into the layout, from the VSComment object

            int timeFormat = 0;
            UserViewHolder userViewHolder = (UserViewHolder) holder;

            userViewHolder.author.setText(currentComment.getAuthor());

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
            Date myDate = null;
            try {
                myDate = df.parse(currentComment.getTimestamp());
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
                case 7:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " seconds ago");
                    break;
                case 8:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " minutes ago");
                    break;
                case 9:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " hours ago");
                    break;
                case 10:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " days ago");
                    break;
                case 11:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " weeks ago");
                    break;
                case 12:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " months ago");
                    break;
                case 13:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " years ago");
                    break;

                //singular
                case 0:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " second ago");
                    break;
                case 1:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " minute ago");
                    break;
                case 2:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " hour ago");
                    break;
                case 3:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " day ago");
                    break;
                case 4:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " week ago");
                    break;
                case 5:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " month ago");
                    break;
                case 6:  userViewHolder.timestamp.setText(String.valueOf(timediff) + " year ago");
                    break;

                default: userViewHolder.timestamp.setText("");
                    break;
            }

            userViewHolder.content.setText(currentComment.getContent());
            userViewHolder.heartCount.setText( Integer.toString(currentComment.getUpvotes() - currentComment.getDownvotes()) );
            //set CardView onClickListener to go to PostPage fragment with corresponding Post data
            userViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO: this should take user to comment section page corresponding to clicked VSC cardview
                    /*
                    if(((MainContainer)activity).getMainFrag().getUILifeStatus()){
                        ((MainContainer)activity).postClicked(posts.get(position));
                    }
                    */
                }
            });

        } else if (holder instanceof LoadingViewHolder) { //TODO: handle loading view to be implemented soon
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        return vsComments == null ? 0 : vsComments.size();
    }

    public void setLoaded() {
        isLoading = false;
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public LoadingViewHolder(View view) {
            super(view);
            progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);
        }
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        /*
        public TextView post_id;
        public TextView timestamp;
        public TextView author;
        public TextView comment_id;
        public TextView parent_id;
        public TextView content;
        public TextView upvotes;
        public TextView downvotes;
        */

        public CircleImageView circView;
        public TextView timestamp;
        public TextView author;
        public TextView content;
        public TextView heartCount;

        public UserViewHolder(View view) {
            super(view);
            circView = (CircleImageView)view.findViewById(R.id.profile_image_cs);
            author = (TextView) view.findViewById(R.id.usernametvcs);
            timestamp = (TextView) view.findViewById(R.id.timetvcs);
            content = (TextView) view.findViewById(R.id.usercomment);
            heartCount = (TextView) view.findViewById(R.id.heartCount);
        }


    }

    //TODO: update function intent to launch profile page once profile page is available. For now, it leads to StartScreen.
    public void profileClicked(View view){
        if(((MainContainer)activity).getMainFrag().getUILifeStatus()){
            Intent intent = new Intent(activity, PhoneOrEmail.class);
            //EditText editText = (EditText) findViewById(R.id.editText);
            //String message = editText.getText().toString();
            //intent.putExtra(EXTRA_MESSAGE, message);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }
    public static void setLeftMargin (View v, int margin) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(margin, 0, 0, 0);
            v.requestLayout();
        }
    }
}