package com.vs.bcd.versus.adapter;

import android.support.v7.widget.LinearLayoutManager;
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

import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAdView;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.R;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private final int NATIVE_APP_INSTALL_AD = 1;
    private final int NATIVE_CONTENT_AD = 2;
    private final int VIEW_TYPE_LOADING = 3;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean isLoading;
    private MainContainer activity;
    private List<Post> posts;
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;
    private final int fragmentInt; //0 = MainActivity, 1 = Search, 6 = Category, 9 = Me (Profile). Default value of 0.
    private String GAID;
    private boolean gaidWait;

    public MyAdapter(RecyclerView recyclerView, List<Post> posts, MainContainer activity, int fragmentInt) {
        this.posts = posts;
        this.activity = activity;
        this.fragmentInt = fragmentInt;
        /*
        gaidWait = true;
        getGAID();
        long end = System.currentTimeMillis() + 3*1000; // 3 sec * 1000 ms/sec
        //automatic timeout at 3 seconds to prevent infinite loop
        while(gaidWait && System.currentTimeMillis() < end){
            //wait for getGAID()'s thread to finish retrieving device GAID
        }
        if(GAID == null || GAID.equals("N/A")){

        }
        else{   //we can serve targeted ads

        }
        */
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

    @Override
    public int getItemViewType(int position) {
        if(posts.get(position) == null){
            Log.d("hey", "this happens?");
            return VIEW_TYPE_LOADING;
        }
        switch (posts.get(position).getCategory()){
            case 42069:
                return NATIVE_APP_INSTALL_AD;
            case 69420:
                return NATIVE_CONTENT_AD;
            default:
                return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(activity).inflate(R.layout.vs_card, parent, false);
            return new UserViewHolder(view);
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
        if (holder instanceof UserViewHolder) {

            //TODO:this is where values are put into the layout, from the post object
            Post post = posts.get(position);
            final String postID = post.getPost_id();
            int timeFormat = 0;
            UserViewHolder userViewHolder = (UserViewHolder) holder;

            final String authorName = post.getAuthor();
            //set onClickListener for profile pic
            ((UserViewHolder) holder).circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    activity.goToProfile(authorName);
                }
            });

            userViewHolder.author.setText(authorName);
            userViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.goToProfile(authorName);
                }
            });

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
            Date myDate = null;
            try {
                myDate = df.parse(post.getTime());
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

            //TODO: handle cases where question doesn't exist (as in the column doesn't even exist. in that case question.setText(""), leaving it as empty string
            userViewHolder.question.setText(post.getQuestion());
            userViewHolder.mainVSText.setText(post.getRedname() + " vs " + post.getBlackname());
            userViewHolder.category.setText(post.getCategoryString());
            //userViewHolder.votecount.setText(Double.toString(post.getPopularityVelocity()) + " PV");
            userViewHolder.votecount.setText(Integer.toString(post.getVotecount()) + " votes");
            //set CardView onClickListener to go to PostPage fragment with corresponding Post data
            userViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //if(((MainContainer)activity).getMainFrag().getUILifeStatus())

                    if(activity.showPost()){
                        activity.postClicked(posts.get(position), fragmentInt);
                    }
                }
            });

        }

        else if(holder instanceof NAIAdViewHolder){
            Post adSkeleton = posts.get(position);
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
            Post adSkeleton = posts.get(position);
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

        public TextView author;
        public TextView time;
        public TextView question;
        public TextView mainVSText;
        public TextView category;
        public TextView votecount;

        public CircleImageView circView;

        //TODO: thumbnails

        public UserViewHolder(View view) {
            super(view);
            circView = (CircleImageView)view.findViewById(R.id.profile_image);
            author = (TextView) view.findViewById(R.id.txt_author);
            time = (TextView) view.findViewById(R.id.txt_time);
            question = (TextView) view.findViewById(R.id.txt_question);
            mainVSText = (TextView) view.findViewById(R.id.maintitletxt);
            category = (TextView) view.findViewById(R.id.txt_category);
            votecount = (TextView) view.findViewById(R.id.txt_votecount);

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
        posts.clear();
        notifyDataSetChanged();
    }

    //TODO: update function intent to launch profile page once profile page is available. For now, it leads to StartScreen.
    public void profileClicked(String username){
        //TODO: implement this for when profile pic is clicked on PostCard
    }
    /*
    public boolean addToPostsList(ArrayList<Post> additionalPosts){
        if(additionalPosts.isEmpty()){
            return false;
        }
        else{
            posts.addAll(additionalPosts);
            Log.d("Load", "now posts in adapter has " + Integer.toString(posts.size()) + " items");
            return true;
        }
    }

    public void refreshPostsList(ArrayList<Post> postsIn){
        posts.clear();
        posts.addAll(postsIn);
        Log.d("Refresh", "Now posts has " + Integer.toString(posts.size()) + " items");
        notifyDataSetChanged();
    }
    */

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

}