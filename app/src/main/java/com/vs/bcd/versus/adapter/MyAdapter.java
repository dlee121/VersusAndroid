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
import android.widget.RatingBar;
import android.widget.RelativeLayout;
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
import com.loopj.android.http.HttpGet;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.GlideApp;
import com.vs.bcd.versus.model.GlideUrlCustom;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.SquareImageView;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;
import de.hdodenhof.circleimageview.CircleImageView;


public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ListPreloader.PreloadModelProvider<Post>{
    private final int VIEW_TYPE_IT = 0;
    private final int VIEW_TYPE_T = 1;
    private final int VIEW_TYPE_C = 2;
    private final int NATIVE_APP_INSTALL_AD = 3;
    private final int NATIVE_CONTENT_AD = 4;
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
            case 42069:
                return NATIVE_APP_INSTALL_AD;
            case 69420:
                return NATIVE_CONTENT_AD;
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
            txtImgViewHolder.author.setText(authorName);

            //set onClickListener for profile pic
            txtImgViewHolder.circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(!authorName.equals("[deleted]")){
                        activity.goToProfile(authorName, true);
                    }
                }
            });
            txtImgViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!authorName.equals("[deleted]")){
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
                int profileImg = profileImgVersions.get(authorName).intValue();
                if(profileImg == 0){
                    GlideApp.with(activity).load(defaultProfileImage).into(txtImgViewHolder.circView);
                }
                else{
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

            }

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
            txtOnlyViewHolder.author.setText(authorName);


            //set onClickListener for profile pic
            txtOnlyViewHolder.circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(!authorName.equals("[deleted]")){
                        activity.goToProfile(authorName, true);
                    }
                }
            });

            txtOnlyViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!authorName.equals("[deleted]")){
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
                int profileImg = profileImgVersions.get(authorName).intValue();
                if(profileImg == 0){
                    GlideApp.with(activity).load(defaultProfileImage).into(txtOnlyViewHolder.circView);
                }
                else{
                    GlideApp.with(activity).load(activity.getProfileImgUrl(authorName, profileImg)).into(txtOnlyViewHolder.circView);
                }

            }catch (Throwable t){

            }

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
            final Post compactPost = posts.get(position);

            CompactViewHolder compactViewHolder = (CompactViewHolder) holder;

            if(fragmentInt == 1){ //Search item, so show author
                final String authorName = compactPost.getAuthor();
                compactViewHolder.author.setText(authorName);
                compactViewHolder.timeTop.setText(getFormattedTime(compactPost.getTime()));
                compactViewHolder.time.setText("");

                compactViewHolder.author.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!authorName.equals("[deleted]")){
                            activity.goToProfile(authorName, true);
                        }
                    }
                });

                compactViewHolder.circView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!authorName.equals("[deleted]")){
                            activity.goToProfile(authorName, true);
                        }
                    }
                });

                try{
                    int profileImg = profileImgVersions.get(compactPost.getAuthor()).intValue();
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

    private Post supplementCompactPost(String postID){
        String host = activity.getESHost();
        String query = "/post/post_type/"+postID;
        String url = "https://" + host + query;

        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);

        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(activity.getESRegion())
                .serviceName("es") // es - elastic search. use your service name
                .httpMethodName("GET") //GET, PUT, POST, DELETE, etc...
                .canonicalURI(query) //end point
                .queryParametes(null) //query parameters if any
                .awsHeaders(awsHeaders) //aws header parameters
                .debug() // turn on the debug mode
                .build();

        HttpGet httpGet = new HttpGet(url);

		        /* Get header calculated for request */
        Map<String, String> header = aWSV4Auth.getHeaders();
        for (Map.Entry<String, String> entrySet : header.entrySet()) {
            String key = entrySet.getKey();
            String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

            httpGet.addHeader(key, value);
        }

        /* Create object of CloseableHttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

		/* Response handler for after request execution */
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				/* Get status code */
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
					/* Convert response to String */
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        try {
			/* Execute URL and attach after execution response handler */

            String strResponse = httpClient.execute(httpGet, responseHandler);

            JSONObject obj = new JSONObject(strResponse);
            JSONObject item = obj.getJSONObject("_source");
            String id = obj.getString("_id");
            return new Post(item, id, false);

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
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
        public SquareImageView leftIV, rightIV;

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
        private TextView author, question, votecount, rname, bname, time, timeTop;
        private RelativeLayout authorContainer;
        private CircleImageView circView;

        public CompactViewHolder(View view){
            super(view);
            authorContainer = view.findViewById(R.id.cc_author_container);
            circView = authorContainer.findViewById(R.id.profile_image_cc);
            author = authorContainer.findViewById(R.id.cc_author_tv);
            timeTop = authorContainer.findViewById(R.id.time_cc);
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
                    profileImg = profileImgVersions.get(post.getAuthor()).intValue();
                    if (profileImg == 0) {
                        return null;
                    }
                    return GlideApp.with(activity).load(activity.getProfileImgUrl(post.getAuthor(), profileImgVersions.get(post.getAuthor()).intValue()));

                case 9:
                    return null;

                default:
                    profileImg = profileImgVersions.get(post.getAuthor()).intValue();
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

    public void postRefreshUpdate(int index, String targetID, Post refreshedPost, boolean writingPostVoteToDB){
        if(posts.get(index).getPost_id().equals(targetID)){
            /*

            if(writingPostVoteToDB){
                Post finalPost = refreshedPost;
                if(refreshedPost.getVotecount() <= posts.get(index).getVotecount()){
                    finalPost.setRedcount(posts.get(index).getRedcount());
                    finalPost.setBlackcount(posts.get(index).getBlackcount());
                }
                else{
                    String postRefreshCode = activity.getPostPage().getPostRefreshCode();
                    switch (postRefreshCode){
                        case "r":
                            finalPost.setRedcount(finalPost.getRedcount()+1);
                            break;
                        case "b":
                            finalPost.setBlackcount(finalPost.getBlackcount()+1);
                            break;
                        case "rb":
                            finalPost.setRedcount(finalPost.getRedcount()+1);
                            finalPost.setBlackcount(finalPost.getBlackcount()-1);
                            break;
                        case "br":
                            finalPost.setBlackcount(finalPost.getBlackcount()+1);
                            finalPost.setRedcount(finalPost.getRedcount()-1);
                            break;
                    }
                }
                posts.set(index, finalPost);
                //notifyItemChanged(index);
            }
            else{
                posts.set(index, refreshedPost);
                //notifyItemChanged(index);
            }
            notifyDataSetChanged();
            */
            //activity.getPostPage().getPPAdapter().notifyItemChanged(0);
        }
    }

    public void incrementItemVotecount(int index, String targetID){
        if(posts.get(index).getPost_id().equals(targetID)){
            notifyItemChanged(index);
        }
    }

}