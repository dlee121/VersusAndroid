package com.vs.bcd.versus;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;


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
            View view = LayoutInflater.from(activity).inflate(R.layout.item_recycler_view_row, parent, false);
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
            //TODO:this is where values are put into the layout, from the post object
            Post post = posts.get(position);

            UserViewHolder userViewHolder = (UserViewHolder) holder;
            userViewHolder.post_id.setText("Post ID: " + Integer.toString(post.getPostID()));
            userViewHolder.question.setText("Question: " + post.getQuestion());
            userViewHolder.author.setText("By: " + post.getAuthor());
            userViewHolder.time.setText(post.getTime());
            userViewHolder.viewcount.setText(Integer.toString(post.getViewcount()) + " views");
            userViewHolder.redname.setText(post.getRedname());
            userViewHolder.redcount.setText(Integer.toString(post.getRedcount()));
            userViewHolder.blackname.setText(post.getBlackname());
            userViewHolder.blackcount.setText(Integer.toString(post.getBlackcount()));
            userViewHolder.category.setText(post.getCategory());

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

        //TODO: thumnails
        //post_id, question, author, time, {thumbnail1, thumbnail2}*, viewcount, redname, redcount, blackname, blackcount

        public UserViewHolder(View view) {
            super(view);
            post_id = (TextView) view.findViewById(R.id.txt_post_id);
            question = (TextView) view.findViewById(R.id.txt_question);
            author = (TextView) view.findViewById(R.id.txt_author);
            time = (TextView) view.findViewById(R.id.txt_time);
            viewcount = (TextView) view.findViewById(R.id.txt_viewcount);
            redname = (TextView) view.findViewById(R.id.txt_redname);
            redcount = (TextView) view.findViewById(R.id.txt_redcount);
            blackname = (TextView) view.findViewById(R.id.txt_blackname);
            blackcount = (TextView) view.findViewById(R.id.txt_blackcount);
            category = (TextView) view.findViewById(R.id.txt_category);
        }
    }
}