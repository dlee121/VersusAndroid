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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appodeal.ads.Native;
import com.appodeal.ads.NativeAd;
import com.appodeal.ads.NativeAdView;
import com.appodeal.ads.NativeMediaView;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.vs.bcd.api.model.PostModel;
import com.vs.bcd.versus.activity.MainActivity;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.GlideUrlCustom;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.SquareImageView;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;


public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ListPreloader.PreloadModelProvider<Post>{
    private final int VIEW_TYPE_IT = 0;
    private final int VIEW_TYPE_T = 1;
    private final int VIEW_TYPE_C = 2;
    private final int NATIVE_APP_INSTALL_AD = 3;
    private final int NATIVE_AD = 4;
    private final int VIEW_TYPE_LOADING = 5;
    private boolean isLoading;
    private MainContainer activity;
    private List<Post> posts;
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;
    private final int fragmentInt; //0 = MainActivity, 1 = Search, 9 = Me (Profile). Default value of 0.
    private String GAID;
    private boolean gaidWait;

    private int DEFAULT = 0;
    private int S3 = 1;
    private int VSRED = 0;
    private int VSBLUE = 0;

    private int imageWidthPixels = 696;
    private int imageHeightPixels = 747;

    private HashMap<String, Integer> profileImgVersions;
    private Toast mToast;

    Drawable defaultImage, defaultProfileImage;

    //constructor for Profile Post History posts list
    public MyAdapter(List<Post> posts, MainContainer activity, int fragmentInt) {
        this.posts = posts;
        this.activity = activity;
        this.fragmentInt = fragmentInt;

        VSRED = ContextCompat.getColor(this.activity, R.color.vsRed);
        VSBLUE = ContextCompat.getColor(this.activity, R.color.vsBlue);

    }

    public MyAdapter(List<Post> posts, MainContainer activity, HashMap<String, Integer> profileImgVersions, int fragmentInt) {
        this.posts = posts;
        this.activity = activity;
        this.profileImgVersions = profileImgVersions;
        this.fragmentInt = fragmentInt;

        VSRED = ContextCompat.getColor(this.activity, R.color.vsRed);
        VSBLUE = ContextCompat.getColor(this.activity, R.color.vsBlue);

        if(fragmentInt == 0){ //MainActivity
            defaultImage = ContextCompat.getDrawable(activity, R.drawable.default_background);
        }
        if(fragmentInt != 9){ //if not from Profile page
            defaultProfileImage = ContextCompat.getDrawable(activity, R.drawable.default_profile);
        }

        activity.addToCentralProfileImgVersionMap(profileImgVersions);

    }

    @Override
    public int getItemViewType(int position) {
        Post post = posts.get(position);
        if(post == null){
            Log.d("hey", "this happens?");
            return VIEW_TYPE_LOADING;
        }
        switch (post.getCategory()){
            case 69420:
                return NATIVE_AD;
            default:
                if(fragmentInt == 0 || fragmentInt == 6){
                    if(post.getRedimg()%10 == S3 || post.getBlackimg()%10 == S3){
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
        } else if (viewType == NATIVE_AD){
            View view = LayoutInflater.from(activity).inflate(R.layout.native_ad_post, parent, false);
            return new NativeAdViewHolder(view);
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

            final String authorName = post.getAuthor();
            txtImgViewHolder.author.setText(authorName);

            //set onClickListener for profile pic
            txtImgViewHolder.circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(!authorName.equals("deleted")){
                        activity.goToProfile(authorName, true);
                    }
                }
            });
            txtImgViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!authorName.equals("deleted")){
                        activity.goToProfile(authorName, true);
                    }
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
                        activity.postClicked(posts.get(position), fragmentInt, position);
                    }
                }
            });
            txtImgViewHolder.leftIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //if(((MainContainer)activity).getMainFrag().getUILifeStatus())

                    if(activity.showPost()){
                        activity.postClicked(posts.get(position), fragmentInt, position);
                    }
                }
            });
            txtImgViewHolder.rightIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //if(((MainContainer)activity).getMainFrag().getUILifeStatus())

                    if(activity.showPost()){
                        activity.postClicked(posts.get(position), fragmentInt, position);
                    }
                }
            });


            try{

                int profileImg = profileImgVersions.get(authorName.toLowerCase()).intValue();
                if(profileImg == 0){
                    Log.d("postloading",authorName+profileImg);
                    GlideApp.with(activity).load(defaultProfileImage).into(txtImgViewHolder.circView);
                }
                else{
                    Log.d("postloading",authorName+profileImg);
                    GlideApp.with(activity).load(activity.getProfileImgUrl(authorName, profileImg)).into(txtImgViewHolder.circView);
                }

                if(post.getRedimg()%10 == S3){
                    GlideUrlCustom gurlLeft = new GlideUrlCustom(activity.getImgURI(post, 0));
                    GlideApp.with(activity).load(gurlLeft).override(imageWidthPixels, imageHeightPixels).into(txtImgViewHolder.leftIV);
                }
                else if(post.getRedimg()%10 == DEFAULT){
                    //set default image
                    GlideApp.with(activity).load(defaultImage).override(imageWidthPixels, imageHeightPixels).into(txtImgViewHolder.leftIV);
                }

                if(post.getBlackimg()%10 == S3){
                    GlideUrlCustom gurlRight = new GlideUrlCustom(activity.getImgURI(post, 1));
                    GlideApp.with(activity).load(gurlRight).override(imageWidthPixels, imageHeightPixels).into(txtImgViewHolder.rightIV);
                }
                else if(post.getBlackimg()%10 == DEFAULT){
                    //set default image
                    GlideApp.with(activity).load(defaultImage).override(imageWidthPixels, imageHeightPixels).into(txtImgViewHolder.rightIV);
                }

            }catch (Throwable t){
                //Log.d("postloading", t.toString());

            }

        }

        else if (holder instanceof TxtOnlyViewHolder) {

            //TODO:this is where values are put into the layout, from the post object
            Post post = posts.get(position);
            TxtOnlyViewHolder txtOnlyViewHolder = (TxtOnlyViewHolder) holder;

            txtOnlyViewHolder.rname.setText(post.getRedname());
            txtOnlyViewHolder.bname.setText(post.getBlackname());
            /*
            if(position % 2 == 0){
                txtOnlyViewHolder.txtV.setTextColor(VSRED);
                txtOnlyViewHolder.txtS.setTextColor(VSBLUE);
            }
            else{
                txtOnlyViewHolder.txtV.setTextColor(VSBLUE);
                txtOnlyViewHolder.txtS.setTextColor(VSRED);
            }
            */

            final String authorName = post.getAuthor();
            txtOnlyViewHolder.author.setText(authorName);


            //set onClickListener for profile pic
            txtOnlyViewHolder.circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(!authorName.equals("deleted")){
                        activity.goToProfile(authorName, true);
                    }
                }
            });

            txtOnlyViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!authorName.equals("deleted")){
                        activity.goToProfile(authorName, true);
                    }
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
                        activity.postClicked(posts.get(position), fragmentInt, position);
                    }
                }
            });


            try{
                int profileImg = profileImgVersions.get(authorName.toLowerCase()).intValue();
                if(profileImg == 0){
                    GlideApp.with(activity).load(defaultProfileImage).into(txtOnlyViewHolder.circView);
                }
                else{
                    GlideApp.with(activity).load(activity.getProfileImgUrl(authorName, profileImg)).into(txtOnlyViewHolder.circView);
                }

            }catch (Throwable t){

            }

        }

        else if(holder instanceof NativeAdViewHolder){
            NativeAdViewHolder nativeAdViewHolder = (NativeAdViewHolder) holder;

            NativeAd nativeAd = activity.getNativeAd();
            if (nativeAd != null) {
                nativeAdViewHolder.showNativeAd(nativeAd);
            }
            else {
                //collapse this nativeAdViewHolder since an ad for it isn't available yet
                //nativeAdViewHolder.nativeAdView.setVisibility(View.GONE);
            }

        }

        else { //the only remaining possibility; the vscard_compact for Search and Post History
            final Post compactPost = posts.get(position);

            CompactViewHolder compactViewHolder = (CompactViewHolder) holder;

            if(fragmentInt == 1){ //Search item, so show author
                final String authorName = compactPost.getAuthor();
                compactViewHolder.author.setText(authorName);
                compactViewHolder.time.setText(getFormattedTime(compactPost.getTime()));

                compactViewHolder.author.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!authorName.equals("deleted")){
                            activity.goToProfile(authorName, true);
                        }
                    }
                });

                compactViewHolder.circView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!authorName.equals("deleted")){
                            activity.goToProfile(authorName, true);
                        }
                    }
                });

                try{
                    int profileImg = profileImgVersions.get(authorName.toLowerCase()).intValue();
                    if(profileImg == 0){
                        GlideApp.with(activity).load(defaultProfileImage).into(compactViewHolder.circView);
                    }
                    else{
                        GlideApp.with(activity).load(activity.getProfileImgUrl(compactPost.getAuthor(), profileImg)).into(compactViewHolder.circView);
                    }

                }catch (Throwable t){

                }
            }
            else{ //Post History item, so no need to show author
                compactViewHolder.authorContainer.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
                RelativeLayout.LayoutParams questionLP = (RelativeLayout.LayoutParams) compactViewHolder.question.getLayoutParams();
                questionLP.removeRule(RelativeLayout.BELOW);
                questionLP.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                compactViewHolder.question.setLayoutParams(questionLP);
                compactViewHolder.time.setText(getFormattedTime(compactPost.getTime()));
            }

            compactViewHolder.question.setText(compactPost.getQuestion());
            compactViewHolder.rname.setText(compactPost.getRedname());
            compactViewHolder.bname.setText(compactPost.getBlackname());
            compactViewHolder.votecount.setText(Integer.toString(compactPost.getVotecount())+ " votes");


            compactViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(activity.showPost()){

                        Runnable runnable = new Runnable() {
                            public void run() {
                                final Post clickedPost = supplementCompactPost(posts.get(position).getPost_id());
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(clickedPost == null){
                                            if(mToast != null){
                                                mToast.cancel();
                                            }
                                            mToast = Toast.makeText(activity, "Network Error. Please try again.", Toast.LENGTH_SHORT);
                                            mToast.show();
                                        }
                                        else{
                                            activity.postClicked(clickedPost, fragmentInt, position);

                                        }
                                    }
                                });
                            }
                        };
                        Thread mythread = new Thread(runnable);
                        mythread.start();
                    }
                }
            });

        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof NativeAdViewHolder) {
            ((NativeAdViewHolder) holder).nativeAdView.unregisterViewForInteraction();
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

        public TextView author, time, question, category, votecount, rname, bname;//, txtV, txtS;
        public LinearLayout textOnly;
        public CircleImageView circView;

        //TODO: thumbnails

        public TxtOnlyViewHolder(View view) {
            super(view);

            textOnly = view.findViewById(R.id.only_texts);
            rname = textOnly.findViewById(R.id.vsc_r_t);
            bname = textOnly.findViewById(R.id.vsc_b_t);
            //txtV = textOnly.findViewById(R.id.vsc_v_t);
            //txtS = textOnly.findViewById(R.id.vsc_s_t);

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

        public TextView rname, bname;//, txtV, txtS; //for images+text version TODO: if we only have one image then put a placeholder image in the iv without image
        public CircleImageView circView;
        public SquareImageView leftIV, rightIV;

        //TODO: thumbnails

        public TxtImgViewHolder(View view) {
            super(view);
            withImages = view.findViewById(R.id.images_and_texts);
            rname = withImages.findViewById(R.id.vsc_r_it);
            bname = withImages.findViewById(R.id.vsc_b_it);
            //txtV = withImages.findViewById(R.id.vsc_v_it);
            //txtS = withImages.findViewById(R.id.vsc_s_it);
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
        private TextView author, question, votecount, rname, bname, time;
        private RelativeLayout authorContainer;
        private CircleImageView circView;

        public CompactViewHolder(View view){
            super(view);
            authorContainer = view.findViewById(R.id.cc_author_container);
            circView = authorContainer.findViewById(R.id.profile_image_cc);
            author = authorContainer.findViewById(R.id.cc_author_tv);
            question = view.findViewById(R.id.question_vc);
            votecount = view.findViewById(R.id.votes_vc);
            rname = view.findViewById(R.id.red_vc);
            bname = view.findViewById(R.id.blue_vc);
            time = view.findViewById(R.id.time_vc);
        }
    }

    private class NativeAdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView nativeAdView;
        CircleImageView adIcon;
        TextView adTitle, adAdLabel, adDescription;
        RelativeLayout adChoicesContainer;
        NativeMediaView nativeMediaView;
        Button adMediaCTA;

        NativeAdViewHolder(View view){
            super(view);
            nativeAdView = (NativeAdView) view;
            adIcon = view.findViewById(R.id.native_ad_icon);
            adTitle = view.findViewById(R.id.native_ad_title);
            adAdLabel = view.findViewById(R.id.native_ad_Ad);
            adDescription = view.findViewById(R.id.native_ad_description);
            adChoicesContainer = view.findViewById(R.id.adChoices_container);
            nativeMediaView = view.findViewById(R.id.native_ad_media);
            adMediaCTA = view.findViewById(R.id.native_ad_media_cta);
        }

        void showNativeAd(NativeAd nativeAd) {
            adIcon.setImageBitmap(nativeAd.getIcon());
            nativeAdView.setIconView(adIcon);

            adTitle.setText(nativeAd.getTitle());
            nativeAdView.setTitleView(adTitle);

            View providerView = nativeAd.getProviderView(activity);
            if (providerView != null) {
                adAdLabel.setVisibility(View.GONE);
                adChoicesContainer.setVisibility(View.VISIBLE);
                adChoicesContainer.addView(providerView);
            }
            else {
                adAdLabel.setVisibility(View.VISIBLE);
                adChoicesContainer.setVisibility(View.GONE);
            }
            nativeAdView.setProviderView(providerView);

            adDescription.setText(nativeAd.getDescription());
            nativeAdView.setDescriptionView(adDescription);

            nativeAdView.setNativeMediaView(nativeMediaView);

            adMediaCTA.setText(nativeAd.getCallToAction());
            nativeAdView.setCallToActionView(adMediaCTA);

            nativeAdView.registerView(nativeAd);
            nativeAdView.setVisibility(View.VISIBLE);
        }
    }

    public void clearList(){
        posts.clear();
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
    public List<Post> getPreloadItems(int position) {
        if(fragmentInt == 9){
            return Collections.emptyList();
        }
        return Collections.singletonList(posts.get(position));
    }

    @Override
    @Nullable
    public RequestBuilder getPreloadRequestBuilder(Post post) {

        try {
            int profileImg;
            switch (fragmentInt) {
                case 1:
                    profileImg = profileImgVersions.get(post.getAuthor().toLowerCase()).intValue();
                    if (profileImg == 0) {
                        return null;
                    }
                    return GlideApp.with(activity).load(activity.getProfileImgUrl(post.getAuthor(), profileImg = profileImgVersions.get(post.getAuthor().toLowerCase()).intValue()));

                case 9:
                    return null;

                default:
                    profileImg = profileImgVersions.get(post.getAuthor().toLowerCase()).intValue();
                    if (profileImg == 0) {
                        if (post.getRedimg() % 10 == S3) {
                            if (post.getBlackimg() % 10 == S3) {
                                GlideUrlCustom gurlLeft = new GlideUrlCustom(activity.getImgURI(post, 0));
                                GlideUrlCustom gurlRight = new GlideUrlCustom(activity.getImgURI(post, 1));
                                return GlideApp.with(activity).load(gurlLeft).override(imageWidthPixels, imageHeightPixels).load(gurlRight).override(imageWidthPixels, imageHeightPixels);
                            } else {
                                GlideUrlCustom gurlLeft = new GlideUrlCustom(activity.getImgURI(post, 0));
                                return GlideApp.with(activity).load(gurlLeft).override(imageWidthPixels, imageHeightPixels).load(defaultImage).override(imageWidthPixels, imageHeightPixels);
                            }
                        } else if (post.getBlackimg() % 10 == S3) {
                            GlideUrlCustom gurlRight = new GlideUrlCustom(activity.getImgURI(post, 1));
                            return GlideApp.with(activity).load(defaultImage).override(imageWidthPixels, imageHeightPixels).load(gurlRight).override(imageWidthPixels, imageHeightPixels);
                        } else {
                            return GlideApp.with(activity).load(defaultImage).override(imageWidthPixels, imageHeightPixels).load(defaultImage).override(imageWidthPixels, imageHeightPixels);

                        }
                    } else {
                        GlideUrlCustom gurlProfile = activity.getProfileImgUrl(post.getAuthor(), profileImg);
                        if (post.getRedimg() % 10 == S3) {
                            if (post.getBlackimg() % 10 == S3) {
                                GlideUrlCustom gurlLeft = new GlideUrlCustom(activity.getImgURI(post, 0));
                                GlideUrlCustom gurlRight = new GlideUrlCustom(activity.getImgURI(post, 1));
                                return GlideApp.with(activity).load(gurlProfile).load(gurlLeft).override(imageWidthPixels, imageHeightPixels).load(gurlRight).override(imageWidthPixels, imageHeightPixels);
                            } else {
                                GlideUrlCustom gurlLeft = new GlideUrlCustom(activity.getImgURI(post, 0));
                                return GlideApp.with(activity).load(gurlProfile).load(gurlLeft).override(imageWidthPixels, imageHeightPixels).load(defaultImage).override(imageWidthPixels, imageHeightPixels);
                            }
                        } else if (post.getBlackimg() % 10 == S3) {
                            GlideUrlCustom gurlRight = new GlideUrlCustom(activity.getImgURI(post, 1));
                            return GlideApp.with(activity).load(gurlProfile).load(defaultImage).override(imageWidthPixels, imageHeightPixels).load(gurlRight).override(imageWidthPixels, imageHeightPixels);
                        } else {
                            return GlideApp.with(activity).load(gurlProfile).load(defaultImage).override(imageWidthPixels, imageHeightPixels).load(defaultImage).override(imageWidthPixels, imageHeightPixels);
                        }
                    }
            }


        } catch (Throwable t) {

        }

        return null;
    }

    public void incrementItemVotecount(int index, String targetID){
        if(posts.get(index).getPost_id().equals(targetID)){
            notifyItemChanged(index);
        }
    }

}