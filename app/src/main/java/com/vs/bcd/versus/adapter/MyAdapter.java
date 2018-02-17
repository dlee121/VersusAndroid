package com.vs.bcd.versus.adapter;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private final int VIEW_TYPE_IT = 0;
    private final int VIEW_TYPE_T = 1;
    private final int VIEW_TYPE_C = 2;
    private final int NATIVE_APP_INSTALL_AD = 3;
    private final int NATIVE_CONTENT_AD = 4;
    private final int VIEW_TYPE_LOADING = 5;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean isLoading;
    private MainContainer activity;
    private List<Post> posts;
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;
    private final int fragmentInt; //0 = MainActivity, 1 = Search, 6 = Category, 9 = Me (Profile). Default value of 0.
    private String GAID;
    private boolean gaidWait;

    private int DEFAULT = 0;
    private int S3 = 1;
    private int VSRED = 0;
    private int VSBLUE = 0;

    public MyAdapter(RecyclerView recyclerView, List<Post> posts, MainContainer activity, int fragmentInt) {
        this.posts = posts;
        this.activity = activity;
        this.fragmentInt = fragmentInt;

        if(fragmentInt == 0 || fragmentInt == 6){   //load from cache or download the images for the posts using Glide
            VSRED = ContextCompat.getColor(this.activity, R.color.vsRed);
            VSBLUE = ContextCompat.getColor(this.activity, R.color.vsBlue);
            //TODO: glide grab images


        }

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
        Post post = posts.get(position);
        if(post == null){
            Log.d("hey", "this happens?");
            return VIEW_TYPE_LOADING;
        }
        switch (post.getCategory()){
            case 42069:
                return NATIVE_APP_INSTALL_AD;
            case 69420:
                return NATIVE_CONTENT_AD;
            default:
                if(fragmentInt == 0 || fragmentInt == 6){
                    if(post.getRedimg() == S3 || post.getBlackimg() == S3){
                        return VIEW_TYPE_IT;
                    }
                    return VIEW_TYPE_T;
                }
                return VIEW_TYPE_C;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_IT) {
            View view = LayoutInflater.from(activity).inflate(R.layout.vscard_with_img, parent, false);
            return new TxtImgViewHolder(view);
        } else if (viewType == VIEW_TYPE_T){
            View view = LayoutInflater.from(activity).inflate(R.layout.vscard_txt_only, parent, false);
            return new TxtOnlyViewHolder(view);
        } else if (viewType == VIEW_TYPE_C){
            View view = LayoutInflater.from(activity).inflate(R.layout.vscard_compact, parent, false);
            return new CompactViewHolder(view);
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
        if(holder instanceof TxtImgViewHolder){

            Post post = posts.get(position);
            TxtImgViewHolder txtImgViewHolder = (TxtImgViewHolder) holder;

            txtImgViewHolder.rname.setText(post.getRedname());
            txtImgViewHolder.bname.setText(post.getBlackname());
            if(position % 2 == 0){
                txtImgViewHolder.txtV.setTextColor(VSRED);
                txtImgViewHolder.txtS.setTextColor(VSBLUE);
            }
            else{
                txtImgViewHolder.txtV.setTextColor(VSBLUE);
                txtImgViewHolder.txtS.setTextColor(VSRED);
            }

            final String authorName = post.getAuthor();
            //set onClickListener for profile pic
            txtImgViewHolder.circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    activity.goToProfile(authorName);
                }
            });

            txtImgViewHolder.author.setText(authorName);
            txtImgViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.goToProfile(authorName);
                }
            });

            txtImgViewHolder.time.setText(getFormattedTime(post.getTime()));

            //TODO: handle cases where question doesn't exist (as in the column doesn't even exist. in that case question.setText(""), leaving it as empty string
            txtImgViewHolder.question.setText(post.getQuestion());
            txtImgViewHolder.category.setText(post.getCategoryString());
            txtImgViewHolder.votecount.setText(Integer.toString(post.getVotecount()) + " votes");
            //set CardView onClickListener to go to PostPage fragment with corresponding Post data
            txtImgViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //if(((MainContainer)activity).getMainFrag().getUILifeStatus())

                    if(activity.showPost()){
                        activity.postClicked(posts.get(position), fragmentInt);
                    }
                }
            });
        }

        else if (holder instanceof TxtOnlyViewHolder) {

            //TODO:this is where values are put into the layout, from the post object
            Post post = posts.get(position);
            TxtOnlyViewHolder txtOnlyViewHolder = (TxtOnlyViewHolder) holder;

            txtOnlyViewHolder.rname.setText(post.getRedname());
            txtOnlyViewHolder.bname.setText(post.getBlackname());
            if(position % 2 == 0){
                txtOnlyViewHolder.txtV.setTextColor(VSRED);
                txtOnlyViewHolder.txtS.setTextColor(VSBLUE);
            }
            else{
                txtOnlyViewHolder.txtV.setTextColor(VSBLUE);
                txtOnlyViewHolder.txtS.setTextColor(VSRED);
            }

            final String authorName = post.getAuthor();
            //set onClickListener for profile pic
            txtOnlyViewHolder.circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    activity.goToProfile(authorName);
                }
            });

            txtOnlyViewHolder.author.setText(authorName);
            txtOnlyViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.goToProfile(authorName);
                }
            });

            txtOnlyViewHolder.time.setText(getFormattedTime(post.getTime()));


            //TODO: handle cases where question doesn't exist (as in the column doesn't even exist. in that case question.setText(""), leaving it as empty string
            txtOnlyViewHolder.question.setText(post.getQuestion());
            txtOnlyViewHolder.category.setText(post.getCategoryString());
            txtOnlyViewHolder.votecount.setText(Integer.toString(post.getVotecount()) + " votes");
            //set CardView onClickListener to go to PostPage fragment with corresponding Post data
            txtOnlyViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
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

        else { //the only remaining possibility; the vscard_compact for Search and Post History
            Post compactPost = posts.get(position);

            CompactViewHolder compactViewHolder = (CompactViewHolder) holder;
            compactViewHolder.question.setText(compactPost.getQuestion());
            compactViewHolder.rname.setText(compactPost.getRedname());
            compactViewHolder.bname.setText(compactPost.getBlackname());
            compactViewHolder.votecount.setText(Integer.toString(compactPost.getVotecount()));
            compactViewHolder.time.setText(getFormattedTime(compactPost.getTime()));

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

    private class TxtOnlyViewHolder extends RecyclerView.ViewHolder {

        public TextView author, time, question, category, votecount, rname, bname, txtV, txtS;
        public LinearLayout textOnly;
        public CircleImageView circView;

        //TODO: thumbnails

        public TxtOnlyViewHolder(View view) {
            super(view);

            textOnly = view.findViewById(R.id.only_texts);
            rname = textOnly.findViewById(R.id.vsc_r_t);
            bname = textOnly.findViewById(R.id.vsc_b_t);
            txtV = textOnly.findViewById(R.id.vsc_v_t);
            txtS = textOnly.findViewById(R.id.vsc_s_t);

            circView = view.findViewById(R.id.profile_image_t);
            author = view.findViewById(R.id.author_t);
            time = view.findViewById(R.id.time_t);
            question = view.findViewById(R.id.question_t);
            category = view.findViewById(R.id.category_t);
            votecount = view.findViewById(R.id.votecount_t);
        }
    }

    private class TxtImgViewHolder extends RecyclerView.ViewHolder {

        public TextView author, time, question, category, votecount;
        public LinearLayout withImages;

        public TextView rname, bname, txtV, txtS; //for images+text version TODO: if we only have one image then put a placeholder image in the iv without image
        public CircleImageView circView;
        public ImageView leftIV, rightIV;

        //TODO: thumbnails

        public TxtImgViewHolder(View view) {
            super(view);
            withImages = view.findViewById(R.id.images_and_texts);
            rname = withImages.findViewById(R.id.vsc_r_it);
            bname = withImages.findViewById(R.id.vsc_b_it);
            txtV = withImages.findViewById(R.id.vsc_v_it);
            txtS = withImages.findViewById(R.id.vsc_s_it);
            leftIV = withImages.findViewById(R.id.vsc_r_iv);
            rightIV = withImages.findViewById(R.id.vsc_b_iv);

            circView = view.findViewById(R.id.profile_image_it);
            author = view.findViewById(R.id.author_it);
            time = view.findViewById(R.id.time_it);
            question = view.findViewById(R.id.question_it);
            category = view.findViewById(R.id.category_it);
            votecount = view.findViewById(R.id.votecount_it);
        }
    }

    private class CompactViewHolder extends RecyclerView.ViewHolder{
        private TextView question, votecount, rname, bname, time;

        public CompactViewHolder(View view){
            super(view);
            question = view.findViewById(R.id.question_vc);
            votecount = view.findViewById(R.id.votes_vc);
            rname = view.findViewById(R.id.red_vc);
            bname = view.findViewById(R.id.blue_vc);
            time = view.findViewById(R.id.time_vc);
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

}