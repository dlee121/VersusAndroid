package com.vs.bcd.versus.adapter;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.vs.bcd.api.model.PostModel;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.VSComment;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;


public class NewsfeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ListPreloader.PreloadModelProvider<VSComment>{
    private final int VIEW_TYPE_C = 1;
    private final int NATIVE_AD = 2;
    private final int VIEW_TYPE_LOADING = 4;
    private boolean isLoading;
    private MainContainer activity;
    private List<VSComment> comments;
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;
    private String GAID;
    private boolean gaidWait;

    private int DEFAULT = 0;
    private int S3 = 1;

    private int imageWidthPixels = 696;
    private int imageHeightPixels = 747;

    private HashMap<String, Integer> profileImgVersions;
    private Toast mToast;

    Drawable defaultProfileImage;

    private RelativeLayout.LayoutParams seeMoreContainerLP;

    public NewsfeedAdapter(List<VSComment> comments, MainContainer activity, HashMap<String, Integer> profileImgVersions) {
        this.comments = comments;
        this.activity = activity;
        this.profileImgVersions = profileImgVersions;

        defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);

        activity.addToCentralProfileImgVersionMap(profileImgVersions);

        seeMoreContainerLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        seeMoreContainerLP.addRule(RelativeLayout.ALIGN_END, R.id.usercomment_nw);
        seeMoreContainerLP.addRule(RelativeLayout.BELOW, R.id.comment_author_nw);

    }

    @Override
    public int getItemViewType(int position) {
        VSComment comment = comments.get(position);
        if(comment == null){
            Log.d("hey", "this happens?");
            return VIEW_TYPE_LOADING;
        }
        if(comment.getAuthor().equals("d0")){
            return NATIVE_AD;
        }
        return VIEW_TYPE_C;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_C){
            View view = LayoutInflater.from(activity).inflate(R.layout.card_newsfeed, parent, false);
            return new NewsfeedViewHolder(view);
        } else if (viewType == NATIVE_AD){
            View view = LayoutInflater.from(activity).inflate(R.layout.native_ad_post, parent, false); //TODO: perhaps change to native_ad_mycircle.xml
            return new NativeAdViewHolder(view);
        } else {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof NewsfeedViewHolder){

            final VSComment comment = comments.get(position);
            final NewsfeedViewHolder newsfeedViewHolder = (NewsfeedViewHolder) holder;

            newsfeedViewHolder.commentContent.post(new Runnable() {
                @Override
                public void run() {
                    if(newsfeedViewHolder.commentContent.getLineCount() > 2){
                        newsfeedViewHolder.seeMoreContainer.setLayoutParams(seeMoreContainerLP);

                        RelativeLayout.LayoutParams replyButtonLP = (RelativeLayout.LayoutParams) newsfeedViewHolder.replyButton.getLayoutParams();
                        replyButtonLP.removeRule(RelativeLayout.ALIGN_END);
                        replyButtonLP.addRule(RelativeLayout.START_OF, R.id.see_more_container_nw);
                        replyButtonLP.setMarginEnd(activity.getResources().getDimensionPixelSize(R.dimen.reply_button_margin_end));
                        newsfeedViewHolder.replyButton.setLayoutParams(replyButtonLP);

                        newsfeedViewHolder.seeMoreButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if(newsfeedViewHolder.seeMoreButton.getText().equals("See More")){
                                    newsfeedViewHolder.commentContent.setMaxLines(262);
                                    newsfeedViewHolder.seeMoreButton.setText("See Less");
                                    //commentViewHolder.ellipsis.setText("");
                                }
                                else{
                                    newsfeedViewHolder.commentContent.setMaxLines(2);
                                    newsfeedViewHolder.seeMoreButton.setText("See More");
                                    //commentViewHolder.ellipsis.setText("...");
                                }
                            }
                        });
                    }
                    else{
                        newsfeedViewHolder.seeMoreContainer.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 0));

                        RelativeLayout.LayoutParams replyButtonLP = (RelativeLayout.LayoutParams) newsfeedViewHolder.replyButton.getLayoutParams();
                        replyButtonLP.removeRule(RelativeLayout.START_OF);
                        replyButtonLP.addRule(RelativeLayout.ALIGN_END, R.id.usercomment_nw);
                        replyButtonLP.setMarginEnd(0);
                        newsfeedViewHolder.replyButton.setLayoutParams(replyButtonLP);

                    }
                }
            });


            newsfeedViewHolder.commentAuthor.setText(comment.getAuthor());
            newsfeedViewHolder.time.setText(getFormattedTime(comment.getTime()));
            newsfeedViewHolder.commentContent.setText(comment.getContent());
            newsfeedViewHolder.hearts.setText(Integer.toString(comment.getUpvotes()));
            newsfeedViewHolder.brokenhearts.setText(Integer.toString(comment.getDownvotes()));
            newsfeedViewHolder.postAuthor.setText(comment.getPostAuthor());
            newsfeedViewHolder.question.setText(comment.getQuestion());
            newsfeedViewHolder.votecount.setText(Integer.toString(comment.getRc()+comment.getBc())+" votes");

            try{
                int profileImg = profileImgVersions.get(comment.getPostAuthor().toLowerCase()).intValue();
                if(profileImg == 0){
                    GlideApp.with(activity).load(defaultProfileImage).into(newsfeedViewHolder.circView);
                }
                else{
                    GlideApp.with(activity).load(activity.getProfileImgUrl(comment.getPostAuthor(), profileImg)).into(newsfeedViewHolder.circView);
                }

            }catch (Throwable t){

            }

            try{
                int profileImg = profileImgVersions.get(comment.getAuthor().toLowerCase()).intValue();
                if(profileImg == 0){
                    GlideApp.with(activity).load(defaultProfileImage).into(newsfeedViewHolder.commentProfile);
                }
                else{
                    GlideApp.with(activity).load(activity.getProfileImgUrl(comment.getAuthor(), profileImg)).into(newsfeedViewHolder.commentProfile);
                }

            }catch (Throwable t){

            }

            newsfeedViewHolder.replyCount.setText(Integer.toString(comment.getReplyCount()));

            RelativeLayout.LayoutParams medalLP;
            switch(comment.getTopmedal()){
                case 3:
                    newsfeedViewHolder.medalView.setImageResource(R.drawable.ic_gold_medal);
                    medalLP = (RelativeLayout.LayoutParams) newsfeedViewHolder.medalView.getLayoutParams();
                    medalLP.width = activity.getResources().getDimensionPixelSize(R.dimen.eighteen);
                    medalLP.setMarginEnd(activity.getResources().getDimensionPixelSize(R.dimen.four));
                    newsfeedViewHolder.medalView.setLayoutParams(medalLP);
                    break;

                case 2:
                    newsfeedViewHolder.medalView.setImageResource(R.drawable.ic_silver_medal);
                    medalLP = (RelativeLayout.LayoutParams) newsfeedViewHolder.medalView.getLayoutParams();
                    medalLP.width = activity.getResources().getDimensionPixelSize(R.dimen.eighteen);
                    medalLP.setMarginEnd(activity.getResources().getDimensionPixelSize(R.dimen.four));
                    newsfeedViewHolder.medalView.setLayoutParams(medalLP);
                    break;

                case 1:
                    newsfeedViewHolder.medalView.setImageResource(R.drawable.ic_bronze_medal);
                    medalLP = (RelativeLayout.LayoutParams) newsfeedViewHolder.medalView.getLayoutParams();
                    medalLP.width = activity.getResources().getDimensionPixelSize(R.dimen.eighteen);
                    medalLP.setMarginEnd(activity.getResources().getDimensionPixelSize(R.dimen.four));
                    newsfeedViewHolder.medalView.setLayoutParams(medalLP);
                    break;

                default:
                    medalLP = (RelativeLayout.LayoutParams) newsfeedViewHolder.medalView.getLayoutParams();
                    medalLP.width = 0;
                    medalLP.setMarginEnd(0);
                    newsfeedViewHolder.medalView.setLayoutParams(medalLP);
                    break;
            }

            newsfeedViewHolder.replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    activity.getPostPage().getPPAdapter().setNewsfeedReplyTargetID(comment.getComment_id());

                    if(comment.getParent_id().equals(comment.getPost_id())){ //clicked item is root comment
                        activity.getPostPage().rootCommentHistoryItemClicked(comment, "newsfeed", "");
                    }
                    else{
                        activity.getPostPage().childOrGrandchildHistoryItemClicked(comment, "newsfeed", "");
                    }
                }
            });

            newsfeedViewHolder.postAuthor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!comment.getPostAuthor().equals("deleted")){
                        activity.goToProfile(comment.getPostAuthor(), true);
                    }
                }
            });

            newsfeedViewHolder.circView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!comment.getPostAuthor().equals("deleted")){
                        activity.goToProfile(comment.getPostAuthor(), true);
                    }
                }
            });

            newsfeedViewHolder.commentProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!comment.getAuthor().equals("deleted")){
                        activity.goToProfile(comment.getAuthor(), true);
                    }
                }
            });

            newsfeedViewHolder.commentAuthor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!comment.getAuthor().equals("deleted")){
                        activity.goToProfile(comment.getAuthor(), true);
                    }
                }
            });

            newsfeedViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(comment.getParent_id().equals(comment.getPost_id())){ //clicked item is root comment
                        activity.getPostPage().rootCommentHistoryItemClicked(comment, "newsfeed", "");
                    }
                    else{
                        activity.getPostPage().childOrGrandchildHistoryItemClicked(comment, "newsfeed", "");
                    }

                }
            });



        }

        else if(holder instanceof NativeAdViewHolder){

            VSComment adSkeleton = comments.get(position);


        }
    }

    private Post supplementCompactPost(String postID){
        PostModel result = activity.getClient().postGet("p", postID);

        try {

            return new Post(result.getSource(), result.getId());

        } catch (Exception e) {
            activity.handleNotAuthorizedException();
        }

        //if the ES GET fails, then return old topCardContent
        return null;
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
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

    private class NewsfeedViewHolder extends RecyclerView.ViewHolder{
        private TextView postAuthor, votecount, question, commentAuthor, time, commentContent, hearts, brokenhearts, replyCount;
        private CircleImageView circView, commentProfile;
        private Button replyButton, seeMoreButton;
        private LinearLayout seeMoreContainer;
        private ImageView medalView;

        public NewsfeedViewHolder(View view){
            super(view);
            circView = view.findViewById(R.id.profile_image_nw);
            postAuthor = view.findViewById(R.id.author_nw);
            votecount = view.findViewById(R.id.votecount_nw);
            question = view.findViewById(R.id.question_nw);
            commentProfile = view.findViewById(R.id.comment_profile_nw);
            commentAuthor = view.findViewById(R.id.comment_author_nw);
            time = view.findViewById(R.id.timetvnw);
            commentContent = view.findViewById(R.id.usercomment_nw);
            medalView = view.findViewById(R.id.medal_nw);
            hearts = view.findViewById(R.id.upvotes_nw);
            brokenhearts = view.findViewById(R.id.downvotes_nw);
            replyCount = view.findViewById(R.id.replycount_nw);
            replyButton = view.findViewById(R.id.replybuttonnw);
            seeMoreContainer = view.findViewById(R.id.see_more_container_nw);
            seeMoreButton = seeMoreContainer.findViewById(R.id.see_more_button_nw);
        }
    }

    private class NativeAdViewHolder extends RecyclerView.ViewHolder{
        ImageView logoView, imageView;
        TextView advertiserView, headlineView, bodyView, callToActionView;

        public NativeAdViewHolder(View view){
            super(view);

        }
    }



    public void clearList(){
        comments.clear();
        notifyDataSetChanged();
    }

    private String getFormattedTime(String timestring){
        int timeFormat = 0;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        Date myDate = null;
        try {
            myDate = df.parse(timestring);
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
            case 7:  return String.valueOf(timediff) + " seconds ago";
            case 8:  return String.valueOf(timediff) + " minutes ago";
            case 9:  return String.valueOf(timediff) + " hours ago";
            case 10:  return String.valueOf(timediff) + " days ago";
            case 11:  return String.valueOf(timediff) + " weeks ago";
            case 12:  return String.valueOf(timediff) + " months ago";
            case 13:  return String.valueOf(timediff) + " years ago";

            //singular
            case 0:  return String.valueOf(timediff) + " second ago";
            case 1:  return String.valueOf(timediff) + " minute ago";
            case 2:  return String.valueOf(timediff) + " hour ago";
            case 3:  return String.valueOf(timediff) + " day ago";
            case 4:  return String.valueOf(timediff) + " week ago";
            case 5:  return String.valueOf(timediff) + " month ago";
            case 6:  return String.valueOf(timediff) + " year ago";

            default: return "";
        }
    }

    @Override
    @NonNull
    public List<VSComment> getPreloadItems(int position) {
        return Collections.singletonList(comments.get(position));
    }

    @Override
    @Nullable
    public RequestBuilder getPreloadRequestBuilder(VSComment comment) {

        try {
            int postProfileImg = profileImgVersions.get(comment.getPostAuthor().toLowerCase()).intValue();
            int commentProfileImg = profileImgVersions.get(comment.getAuthor().toLowerCase()).intValue();
            if (postProfileImg == 0) {
                if (commentProfileImg == 0) { //postProfileImg == 0 && commentProfileImg == 0
                    return null;
                }
                else { //postProfileImg == 0 && commentProfileImg != 0
                    return GlideApp.with(activity).load(activity.getProfileImgUrl(comment.getAuthor(), commentProfileImg));
                }

            }
            else if (commentProfileImg == 0) { //postProfileImg != 0 && commentProfileImg == 0
                return GlideApp.with(activity).load(activity.getProfileImgUrl(comment.getPostAuthor(), postProfileImg));
            }
            else { //postProfileImg != 0 && commentProfileImg != 0
                return GlideApp.with(activity).load(activity.getProfileImgUrl(comment.getPostAuthor(), postProfileImg)).load(activity.getProfileImgUrl(comment.getAuthor(), commentProfileImg));
            }



        } catch (Throwable t) {

        }

        return null;
    }

    public void incrementItemVotecount(int index, String targetID){
        /*
        if(posts.get(index).getPost_id().equals(targetID)){
            notifyItemChanged(index);
        }
        */
    }

}