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
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAdView;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.vs.bcd.api.model.PostModel;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.GlideUrlCustom;
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
    private final int NATIVE_APP_INSTALL_AD = 2;
    private final int NATIVE_CONTENT_AD = 3;
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

    public NewsfeedAdapter(List<VSComment> comments, MainContainer activity, HashMap<String, Integer> profileImgVersions) {
        this.comments = comments;
        this.activity = activity;
        this.profileImgVersions = profileImgVersions;

        defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);

        activity.addToCentralProfileImgVersionMap(profileImgVersions);

    }

    @Override
    public int getItemViewType(int position) {
        VSComment comment = comments.get(position);
        if(comment == null){
            Log.d("hey", "this happens?");
            return VIEW_TYPE_LOADING;
        }
        if(comment.getAuthor().equals("adn")){
            return NATIVE_APP_INSTALL_AD;
        }
        if(comment.getAuthor().equals("adc")){
            return NATIVE_APP_INSTALL_AD;
        }
        return VIEW_TYPE_C;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_C){
            View view = LayoutInflater.from(activity).inflate(R.layout.card_newsfeed, parent, false);
            return new NewsfeedViewHolder(view);
        } else if (viewType == NATIVE_APP_INSTALL_AD){
            View view = LayoutInflater.from(activity).inflate(R.layout.adview_native_app_install, parent, false);
            return new NAIAdViewHolder(view);
        } else if (viewType == NATIVE_CONTENT_AD){
            View view = LayoutInflater.from(activity).inflate(R.layout.adview_native_content, parent, false);
            return new NCAdViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof NewsfeedViewHolder){

            VSComment comment = comments.get(position);
            NewsfeedViewHolder newsfeedViewHolder = (NewsfeedViewHolder) holder;
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



        }

        else if(holder instanceof NAIAdViewHolder){

            VSComment adSkeleton = comments.get(position);
            NativeAppInstallAd nativeAppInstallAd = adSkeleton.getNAI();

            NAIAdViewHolder naiAdViewHolder = (NAIAdViewHolder) holder;

            naiAdViewHolder.iconView.setImageDrawable(nativeAppInstallAd.getIcon().getDrawable());
            naiAdViewHolder.headlineView.setText(nativeAppInstallAd.getHeadline());
            naiAdViewHolder.bodyView.setText(nativeAppInstallAd.getBody());
            naiAdViewHolder.callToActionView.setText(nativeAppInstallAd.getCallToAction());

            List<NativeAd.Image> images = nativeAppInstallAd.getImages();
            if (images.size() > 0) {
                naiAdViewHolder.imageView.setImageDrawable(images.get(0).getDrawable());
            }

            if (nativeAppInstallAd.getPrice() == null) {
                naiAdViewHolder.priceView.setVisibility(View.INVISIBLE);
            } else {
                naiAdViewHolder.priceView.setVisibility(View.VISIBLE);
                naiAdViewHolder.priceView.setText(nativeAppInstallAd.getPrice());
            }

            if (nativeAppInstallAd.getStore() == null) {
                naiAdViewHolder.storeView.setVisibility(View.INVISIBLE);
            } else {
                naiAdViewHolder.storeView.setVisibility(View.VISIBLE);
                naiAdViewHolder.storeView.setText(nativeAppInstallAd.getStore());
            }

            if (nativeAppInstallAd.getStarRating() == null) {
                naiAdViewHolder.starsView.setVisibility(View.INVISIBLE);
            } else {
                naiAdViewHolder.starsView.setRating(nativeAppInstallAd.getStarRating().floatValue());
                naiAdViewHolder.starsView.setVisibility(View.VISIBLE);
            }

            naiAdViewHolder.nativeAppInstallAdView.setNativeAd(nativeAppInstallAd);

        }

        else if(holder instanceof NCAdViewHolder){

            VSComment adSkeleton = comments.get(position);
            NativeContentAd nativeContentAd = adSkeleton.getNC();

            NCAdViewHolder ncAdViewHolder = (NCAdViewHolder) holder;

            ncAdViewHolder.headlineView.setText(nativeContentAd.getHeadline());
            ncAdViewHolder.bodyView.setText(nativeContentAd.getBody());
            ncAdViewHolder.callToActionView.setText(nativeContentAd.getCallToAction());
            ncAdViewHolder.advertiserView.setText(nativeContentAd.getAdvertiser());

            List<NativeAd.Image> images = nativeContentAd.getImages();

            if (images.size() > 0) {
                ncAdViewHolder.imageView.setImageDrawable(images.get(0).getDrawable());
            }

            NativeAd.Image logoImage = nativeContentAd.getLogo();

            if (logoImage == null) {
                ncAdViewHolder.logoView.setVisibility(View.INVISIBLE);
            } else {
                ncAdViewHolder.logoView.setImageDrawable(logoImage.getDrawable());
                ncAdViewHolder.logoView.setVisibility(View.VISIBLE);
            }

            ncAdViewHolder.nativeContentAdView.setNativeAd(nativeContentAd);


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
        private TextView postAuthor, votecount, question, commentAuthor, time, commentContent, hearts, brokenhearts;
        private CircleImageView circView;

        public NewsfeedViewHolder(View view){
            super(view);
            circView = view.findViewById(R.id.profile_image_nw);
            postAuthor = view.findViewById(R.id.author_nw);
            votecount = view.findViewById(R.id.votecount_nw);
            question = view.findViewById(R.id.question_nw);
            commentAuthor = view.findViewById(R.id.comment_author_nw);
            time = view.findViewById(R.id.timetvnw);
            commentContent = view.findViewById(R.id.usercomment_nw);
            hearts = view.findViewById(R.id.upvotes_nw);
            brokenhearts = view.findViewById(R.id.downvotes_nw);
        }
    }

    private class NCAdViewHolder extends RecyclerView.ViewHolder{
        NativeContentAdView nativeContentAdView;
        ImageView logoView, imageView;
        TextView advertiserView, headlineView, bodyView, callToActionView;

        public NCAdViewHolder(View view){
            super(view);
            nativeContentAdView = view.findViewById(R.id.nc_adview);

            logoView = nativeContentAdView.findViewById(R.id.nc_logo_view);
            nativeContentAdView.setLogoView(logoView);

            imageView = nativeContentAdView.findViewById(R.id.nc_image_view);
            nativeContentAdView.setImageView(imageView);

            advertiserView = nativeContentAdView.findViewById(R.id.nc_advertiser_view);
            nativeContentAdView.setAdvertiserView(advertiserView);

            headlineView = nativeContentAdView.findViewById(R.id.nc_headline_view);
            nativeContentAdView.setHeadlineView(headlineView);

            bodyView = nativeContentAdView.findViewById(R.id.nc_body_view);
            nativeContentAdView.setBodyView(bodyView);

            callToActionView = nativeContentAdView.findViewById(R.id.nc_call_to_action);
            nativeContentAdView.setCallToActionView(callToActionView);
        }
    }

    private class NAIAdViewHolder extends RecyclerView.ViewHolder{
        NativeAppInstallAdView nativeAppInstallAdView;
        ImageView iconView, imageView;
        TextView headlineView, bodyView, priceView, storeView;
        Button callToActionView;
        RatingBar starsView;

        public NAIAdViewHolder(View view){
            super(view);
            nativeAppInstallAdView = view.findViewById(R.id.nai_adview);

            iconView = nativeAppInstallAdView.findViewById(R.id.nai_icon_view);
            nativeAppInstallAdView.setIconView(iconView);

            imageView = nativeAppInstallAdView.findViewById(R.id.nai_image_view);
            nativeAppInstallAdView.setImageView(imageView);

            headlineView = nativeAppInstallAdView.findViewById(R.id.nai_headline_view);
            nativeAppInstallAdView.setHeadlineView(headlineView);

            bodyView = nativeAppInstallAdView.findViewById(R.id.nai_body_view);
            nativeAppInstallAdView.setBodyView(bodyView);

            priceView = nativeAppInstallAdView.findViewById(R.id.nai_price_view);
            nativeAppInstallAdView.setPriceView(priceView);

            storeView = nativeAppInstallAdView.findViewById(R.id.nai_store_view);
            nativeAppInstallAdView.setStoreView(storeView);

            callToActionView = nativeAppInstallAdView.findViewById(R.id.nai_call_to_action);
            nativeAppInstallAdView.setCallToActionView(callToActionView);

            starsView = nativeAppInstallAdView.findViewById(R.id.nai_stars_view);
            nativeAppInstallAdView.setStarRatingView(starsView);
        }
    }

    public void clearList(){
        comments.clear();
        notifyDataSetChanged();
    }

    private void getGAID() {
        Runnable runnable = new Runnable() {
            public void run() {
                AdvertisingIdClient.Info adInfo;
                adInfo = null;
                try {
                    adInfo = AdvertisingIdClient.getAdvertisingIdInfo(activity.getApplicationContext());
                    if (adInfo == null || adInfo.isLimitAdTrackingEnabled()) { // check if user has opted out of tracking
                        GAID = "N/A";
                    }
                    else{
                        GAID = adInfo.getId();
                    }

                    gaidWait = false;

                } catch (Throwable e) {
                    gaidWait = false;
                    e.printStackTrace();
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
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
            int profileImg = profileImgVersions.get(comment.getPostAuthor().toLowerCase()).intValue();
            if (profileImg == 0) {
                return null;
            }
            return GlideApp.with(activity).load(activity.getProfileImgUrl(comment.getPostAuthor(), profileImgVersions.get(comment.getPostAuthor().toLowerCase()).intValue()));


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