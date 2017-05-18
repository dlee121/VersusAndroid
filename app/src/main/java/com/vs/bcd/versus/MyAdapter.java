package com.vs.bcd.versus;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean isLoading;
    private Activity activity;
    private List<Post> posts;
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;

    public MyAdapter(RecyclerView recyclerView, List<Post> posts, Activity activity) {
        this.posts = posts;
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
        return posts.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(activity).inflate(R.layout.vs_card, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserViewHolder) {

            //set onClickListener for profile pic
            ((UserViewHolder) holder).circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    profileClicked(v);
                }
            });

            //TODO:this is where values are put into the layout, from the post object
            Post post = posts.get(position);
            int timeFormat = 0;
            UserViewHolder userViewHolder = (UserViewHolder) holder;
            userViewHolder.author.setText(post.getAuthor());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
            Date myDate = null;
            try {
                myDate = format.parse(post.getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //TODO: test all possible cases to make sure date format conversion works correctly, for seconds, for all time format constants (secs, mins, ... , years), singulars / plurals
            long timediff = ((new Date()).getTime() - myDate.getTime()) / 1000;  //time elapsed since post creation, in seconds

            //time format constants: 0 = seconds, 1 = minutes, 2 = hours, 3 = days , 4 = weeks, 5 = months, 6 = years
            if(timediff >= 60) {  //if 60 seconds or more, convert to minutes
                timediff /= 60;
                timeFormat = 1;
            }
            if(timediff >= 60) { //if 60 minutes or more, convert to hours
                timediff /= 60;
                timeFormat = 2;
            }
            if(timediff >= 24) { //if 24 hours or more, convert to days
                timediff /= 24;
                timeFormat = 3;
            }

            if(timediff >= 365) { //if 365 days or more, convert to years
                timediff /= 365;
                timeFormat = 6;
            }

            if (timeFormat < 6 && timediff >= 30) { //if 30 days or more and not yet converted to years, convert to months
                timediff /= 30;
                timeFormat = 5;
            }

            if(timeFormat < 5 && timediff >= 7) { //if 7 days or more and not yet converted to months or years, convert to weeks
                timediff /= 7;
                timeFormat = 4;
            }

            if(timediff > 1) //if timediff is not a singular value
                timeFormat += 7;

            switch (timeFormat) {
                //plural
                case 7:  userViewHolder.time.setText(String.valueOf(timediff) + " seconds ago");
                    break;
                case 8:  userViewHolder.time.setText(String.valueOf(timediff) + " minutes ago");
                    break;
                case 9:  userViewHolder.time.setText(String.valueOf(timediff) + " hours ago");
                    break;
                case 10:  userViewHolder.time.setText(String.valueOf(timediff) + " days ago");
                    break;
                case 11:  userViewHolder.time.setText(String.valueOf(timediff) + " weeks ago");
                    break;
                case 12:  userViewHolder.time.setText(String.valueOf(timediff) + " months ago");
                    break;
                case 13:  userViewHolder.time.setText(String.valueOf(timediff) + " years ago");
                    break;

                //singular
                case 0:  userViewHolder.time.setText(String.valueOf(timediff) + " second ago");
                    break;
                case 1:  userViewHolder.time.setText(String.valueOf(timediff) + " minute ago");
                    break;
                case 2:  userViewHolder.time.setText(String.valueOf(timediff) + " hour ago");
                    break;
                case 3:  userViewHolder.time.setText(String.valueOf(timediff) + " day ago");
                    break;
                case 4:  userViewHolder.time.setText(String.valueOf(timediff) + " week ago");
                    break;
                case 5:  userViewHolder.time.setText(String.valueOf(timediff) + " month ago");
                    break;
                case 6:  userViewHolder.time.setText(String.valueOf(timediff) + " year ago");
                    break;

                default: userViewHolder.time.setText("");
                    break;
            }


            userViewHolder.question.setText(post.getQuestion());
            userViewHolder.mainVSText.setText(post.getRedname() + " vs " + post.getBlackname());
            userViewHolder.category.setText(post.getCategory());
            userViewHolder.viewcount.setText(Integer.toString(post.getViewcount()) + " views");

        } else if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
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
        public TextView question;
        public TextView author;
        public TextView time;
        public TextView viewcount;
        public TextView redname;
        public TextView redcount;
        public TextView blackname;
        public TextView blackcount;
        public TextView category;
        */

        public TextView author;
        public TextView time;
        public TextView question;
        public TextView mainVSText;
        public TextView category;
        public TextView viewcount;

        public CircleImageView circView;


        //TODO: thumnails

        public UserViewHolder(View view) {
            super(view);
            circView = (CircleImageView)view.findViewById(R.id.profile_image);
            author = (TextView) view.findViewById(R.id.txt_author);
            time = (TextView) view.findViewById(R.id.txt_time);
            question = (TextView) view.findViewById(R.id.txt_question);
            mainVSText = (TextView) view.findViewById(R.id.maintitletxt);
            category = (TextView) view.findViewById(R.id.txt_category);
            viewcount = (TextView) view.findViewById(R.id.txt_viewcount);
        }
    }

    //TODO: update function intent to launch profile page once profile page is available. For now, it leads to StartScreen.
    public void profileClicked(View view){
        Intent intent = new Intent(activity, WhatsYourBirthday.class);
        //EditText editText = (EditText) findViewById(R.id.editText);
        //String message = editText.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }
}