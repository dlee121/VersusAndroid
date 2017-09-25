package com.vs.bcd.versus.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.media.Image;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.UserAction;
import com.vs.bcd.versus.model.VSComment;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class PostPageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int NOVOTE = 0;
    public static final int UPVOTE = 1;
    public static final int DOWNVOTE = 2;

    private final int NOMASK = 0;
    private final int TINT = 1;
    private final int TINTCHECK = 2;
    private final int REDINT = 0;
    private final int BLKINT = 1;

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private final int VIEW_TYPE_POSTCARD = 2;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean isLoading;
    private MainContainer activity;
    private PostSkeleton post;
    private List<Object> masterList;
    private List<VSComment> childrenList = new ArrayList<>();
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;
    private AmazonS3 s3;
    private PostCardViewHolder postCard;
    private Bitmap redBMP = null;
    private Bitmap blackBMP = null;
    private boolean downloadImages, includesPost;
    private UserAction userAction;
    private Map<String, String> actionMap;
    private Drawable[] redLayers = new Drawable[3];
    private Drawable[] blackLayers = new Drawable[3];
    private ShapeDrawable redTint;
    private ShapeDrawable blackTint;
    private LayerDrawable redLayerDrawable;
    private LayerDrawable blackLayerDrawable;
    private RelativeLayout.LayoutParams graphBoxParams = null;


    //to set imageviews, first fill out the drawable[3] with 0=image layer, 1=tint layer, 2=check mark layer, make LayerDrawable out of the array, then use setImageMask which sets the correct mask layers AND ALSO sets imageview drawable as the LayerDrawable

    public PostPageAdapter(RecyclerView recyclerView, List<Object> vsComments, PostSkeleton post, MainContainer activity, boolean downloadImages, boolean includesPost) {
        this.s3 = ((MainContainer)activity).getS3Client();
        masterList = vsComments;
        this.post = post;
        this.activity = activity;
        this.downloadImages = downloadImages;
        this.includesPost = includesPost;
        userAction = activity.getPostPage().getUserAction();
        actionMap = userAction.getActionRecord();
        graphBoxParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 10);
        graphBoxParams.addRule(RelativeLayout.BELOW, R.id.linlaypoca);
        Log.d("DEBUG", "Action Map Size: " + Integer.toString(actionMap.size()));


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
        if (position > masterList.size())
            return VIEW_TYPE_LOADING;
        else if(position  == 0 && includesPost)
            return VIEW_TYPE_POSTCARD;
        else
            return  VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(activity).inflate(R.layout.comment_group_card, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        } else if (viewType == VIEW_TYPE_POSTCARD) {
            View view = LayoutInflater.from(activity).inflate(R.layout.post_card, parent, false);
            return  new PostCardViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof UserViewHolder) { //holds comments

            boolean skip = false;
            if(position < 8){
                if(! (masterList.get(position) instanceof VSComment)){
                    Log.d("wow", "duplicate post at top of list");
                    skip = true;
                }
            }

            if(!skip){
                final VSComment currentComment = (VSComment)masterList.get(position);

                //this is where values are put into the layout, from the VSComment object

                final UserViewHolder userViewHolder = (UserViewHolder) holder;

                if(activity.getSessionManager().getCurrentUsername().equals(currentComment)){
                    //TODO: implement UI for comments that user wrote, like edit and delete options
                }


                //set onClickListener for profile pic
                userViewHolder.circView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        profileClicked(v);
                    }
                });


                setLeftMargin(userViewHolder.circView, 150 * currentComment.getNestedLevel());  //left margin (indentation) of 150dp per nested level

                if(currentComment.getComment_id().equals("2ebf9760-d9bf-4785-af68-c3993be8945d")){
                    Log.d("debug", "this is the 2eb comment");
                }
                switch (currentComment.getUservote()){
                    case NOVOTE:
                        userViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                        userViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                        break;
                    case UPVOTE:
                        userViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                        break;
                    case DOWNVOTE:
                        userViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                        break;
                    default:
                        userViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                        userViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                        break;
                }

                userViewHolder.author.setText(currentComment.getAuthor());
                userViewHolder.timestamp.setText(getTimeString(currentComment.getTimestamp()));
                final int imgOffset = 8;
                final TextView timeTV = userViewHolder.timestamp;
                switch (currentComment.getCurrentMedal()){
                    case 0:
                        break; //no medal, default currentMedal value
                    case 1: //bronze
                        timeTV.getViewTreeObserver()
                                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        Drawable img = ContextCompat.getDrawable(activity, R.drawable.bronzemedal);
                                        img.setBounds(0, 0, img.getIntrinsicWidth() * timeTV.getMeasuredHeight() / img.getIntrinsicHeight() + imgOffset, timeTV.getMeasuredHeight()+imgOffset);
                                        timeTV.setCompoundDrawables(null, null, img, null);
                                        timeTV.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                        timeTV.setCompoundDrawablePadding(10);
                                    }
                                });

                        break;
                    case 2: //silver
                        timeTV.getViewTreeObserver()
                                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        Drawable img = ContextCompat.getDrawable(activity, R.drawable.silvermedal);
                                        img.setBounds(0, 0, img.getIntrinsicWidth() * timeTV.getMeasuredHeight() / img.getIntrinsicHeight() + imgOffset, timeTV.getMeasuredHeight()+imgOffset);
                                        timeTV.setCompoundDrawables(null, null, img, null);
                                        timeTV.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                        timeTV.setCompoundDrawablePadding(10);
                                    }
                                });

                        break;
                    case 3: //gold
                        timeTV.getViewTreeObserver()
                                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        Drawable img = ContextCompat.getDrawable(activity, R.drawable.goldmedal);
                                        img.setBounds(0, 0, img.getIntrinsicWidth() * timeTV.getMeasuredHeight() / img.getIntrinsicHeight() + imgOffset, timeTV.getMeasuredHeight()+imgOffset);
                                        timeTV.setCompoundDrawables(null, null, img, null);
                                        timeTV.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                        timeTV.setCompoundDrawablePadding(10);
                                    }
                                });

                        break;
                }

                userViewHolder.content.setText(currentComment.getContent());
                userViewHolder.heartCount.setText( Integer.toString(currentComment.heartsTotal()) );
                //set CardView onClickListener to go to PostPage fragment with corresponding Comments data (this will be a PostPage without post_card)

                userViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("topmedal", Integer.toString(currentComment.getCurrentMedal()));
                        if(currentComment.getNestedLevel() == 2){
                            activity.getPostPage().addGrandParentToCache(currentComment.getParent_id()); //pass in parent's id, then the function will get that parent's parent, the grandparent, and add it to the parentCache
                        }
                        activity.getPostPage().addParentToCache(currentComment.getParent_id());
                        activity.getPostPage().addThisToCache(currentComment.getComment_id());
                        activity.getPostPage().setCommentsPage(currentComment);
                    }
                });

                userViewHolder.replyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(currentComment.getNestedLevel() == 2){
                            activity.getPostPage().addGrandParentToCache(currentComment.getParent_id()); //pass in parent's id, then the function will get that parent's parent, the grandparent, and add it to the parentCache
                        }
                        activity.getPostPage().addParentToCache(currentComment.getParent_id());
                        //TODO: Make sure we don't need to add currentComment's node to parentCache. Test it.
                        activity.getCommentEnterFragment().setContentReplyToComment(currentComment);
                        activity.getViewPager().setCurrentItem(4);
                    }
                });

                userViewHolder.upvoteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int userVote = currentComment.getUservote();
                        if(userVote == UPVOTE){
                            userViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                            currentComment.setUservote(NOVOTE);
                            actionMap.put(currentComment.getComment_id(), "N");
                            //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                        }
                        else if(userVote == DOWNVOTE){
                            userViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                            userViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                            currentComment.setUservote(UPVOTE);
                            actionMap.put(currentComment.getComment_id(), "U");
                        }
                        else if(userVote == NOVOTE){
                            userViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                            currentComment.setUservote(UPVOTE);
                            actionMap.put(currentComment.getComment_id(), "U");
                        }
                        userViewHolder.heartCount.setText( Integer.toString(currentComment.heartsTotal()) ); //refresh heartcount display
                    }
                });

                userViewHolder.downvoteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int userVote = currentComment.getUservote();
                        if(userVote == DOWNVOTE){
                            userViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                            currentComment.setUservote(NOVOTE);
                            actionMap.put(currentComment.getComment_id(), "N");
                            //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                        }
                        else if(userVote == UPVOTE){
                            userViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                            userViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                            currentComment.setUservote(DOWNVOTE);
                            actionMap.put(currentComment.getComment_id(), "D");
                        }
                        else if(userVote == NOVOTE){
                            userViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                            currentComment.setUservote(DOWNVOTE);
                            actionMap.put(currentComment.getComment_id(), "D");
                        }
                        userViewHolder.heartCount.setText( Integer.toString(currentComment.heartsTotal()) );
                    }
                });
            }

        } else if (holder instanceof PostCardViewHolder) {
            //Log.d("DEBUG", "BIND EVENT");
            postCard = (PostCardViewHolder) holder;
            TransferUtility transferUtility = new TransferUtility(s3, activity.getApplicationContext());
            postCard.questionTV.setText(post.getQuestion());
            postCard.rednameTV.setText(post.getRedname());
            postCard.blacknameTV.setText(post.getBlackname());

            redTint = new ShapeDrawable (new RectShape());
            redTint.setIntrinsicWidth (postCard.redImage.getWidth());
            redTint.setIntrinsicHeight (postCard.redImage.getHeight());
            redTint.getPaint().setColor(Color.argb(175, 165, 35, 57));
            redLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
            redLayers[1] = redTint;
            redLayers[2] = ContextCompat.getDrawable(activity, R.drawable.ic_check_overlay);

            blackTint = new ShapeDrawable(new RectShape());
            blackTint.setIntrinsicWidth(postCard.blackImage.getWidth());
            blackTint.setIntrinsicHeight(postCard.blackImage.getHeight());
            blackTint.getPaint().setColor(Color.argb(175, 48, 48, 48));
            blackLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
            blackLayers[1] = blackTint;
            blackLayers[2] = ContextCompat.getDrawable(activity, R.drawable.ic_check_overlay_2);


            postCard.redImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!userAction.getVotedSide().equals("RED")){
                        activity.getPostPage().redVotePressed();
                        setImageMask(TINTCHECK, REDINT);
                        setImageMask(TINT, BLKINT);
                    }
                }
            });

            postCard.blackImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!userAction.getVotedSide().equals("BLK")){
                        activity.getPostPage().blackVotePressed();
                        setImageMask(TINTCHECK, BLKINT);
                        setImageMask(TINT, REDINT);
                    }
                }
            });

            postCard.sortTypeSelector.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.getPostPage().selectSortType();
                }
            });

            //TODO: have a field that lets me know if this post contains left / right images so that I do S3 query only when I have to.
            //TODO: and clear the BMP's whenever the fragment detaches, i think we already clear most fields anyway but still
            if(downloadImages){
                downloadImageFromAWS(); //downloads images and calls the helper method setImages() once download completes to set the images on post card
                downloadImages = false; //so that we don't repeat download while same adapter is alive and performing routine refresh function or whatever.
                                        // when we want to update the whole post page including the images, we simply create new adapter in PostPage.setContent(with downloadImages = true)
            }
            else{

                if(activity.hasXBMP()){
                    Log.d("THIS", "NYOK");
                    redLayers[0] = new BitmapDrawable(activity.getResources(), activity.getXBMP());
                }
                else{
                    redLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
                }
                if(activity.hasYBMP()){
                    Log.d("THIS", "FYOK");
                    blackLayers[0] = new BitmapDrawable(activity.getResources(), activity.getYBMP());
                }
                else{
                    blackLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
                }
                setInitialMask();
            }

        } else if (holder instanceof LoadingViewHolder) { //TODO: handle loading view to be implemented soon
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }
    private void setInitialMask(){
        switch (userAction.getVotedSide()){
            case "none":
                setImageMask(NOMASK, REDINT);
                setImageMask(NOMASK, BLKINT);
                break;

            case "RED":
                setImageMask(TINTCHECK, REDINT);
                setImageMask(TINT, BLKINT);
                break;

            case "BLK":
                setImageMask(TINT, REDINT);
                setImageMask(TINTCHECK, BLKINT);
                break;

            default:
                return;
        }
    }


    private void downloadImageFromAWS() {

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                //if (NetworkAvailablity.checkNetworkStatus(MyActivity.this))
                //{
                try {
                    java.util.Date expiration = new java.util.Date();
                    long msec = expiration.getTime();
                    msec += 1000 * 60 * 60; // 1 hour.
                    expiration.setTime(msec);
                    publishProgress(arg0);

                    if(post.getRedimg().equals("s3")){
                        Log.d("Debug", "download red");
                        S3Object object1 = s3.getObject(
                                new GetObjectRequest("versus.pictures", post.getPost_id() + "-left.jpeg"));
                        InputStream ins1 = object1.getObjectContent();
                        redBMP = BitmapFactory.decodeStream(ins1);
                        ins1.close();
                    }

                    if(post.getBlackimg().equals("s3")){
                        Log.d("Debug", "download black");
                        S3Object object2 = s3.getObject(
                                new GetObjectRequest("versus.pictures", post.getPost_id() + "-right.jpeg"));
                        InputStream ins2 = object2.getObjectContent();
                        blackBMP = BitmapFactory.decodeStream(ins2);
                        ins2.close();
                    }
                    setImages();

                } catch (Exception e) {
                    // writing error to Log
                    e.printStackTrace();
                }
            /*
                }
                else
                {

                }
            */
                return null;
            }
            @Override
            protected void onProgressUpdate(String... values) {
                // TODO Auto-generated method stub
                super.onProgressUpdate(values);
                System.out.println("Progress : "  + values);
            }
            @Override
            protected void onPostExecute(String result)
            {

            }
        };
        _Task.execute((String[]) null);
    }

    private void setImages(){

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //TODO: set in-app images here, i.e. emojis and clipart, into redBMP and blackBMP
                //otherise redBMP/blackBMP would be either null, or if marked for image download they would hold the download image as BMP

                //TODO: Create LayerDrawables and construct them here. also have them in MainContainer and set them here.


                activity.setBMP(redBMP, blackBMP);

                if(redBMP != null){
                    redLayers[0] = new BitmapDrawable(activity.getResources(), redBMP);
                } else{
                    redLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
                }

                if(blackBMP != null){
                    blackLayers[0] = new BitmapDrawable(activity.getResources(), blackBMP);
                } else{
                    blackLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
                }

                switch (userAction.getVotedSide()){
                    case "none":
                        setImageMask(NOMASK, REDINT);   //sets mask and also sets the drawable to corresponding imageview
                        setImageMask(NOMASK, BLKINT);
                        //Log.d("vote", "user voted none");
                        break;
                    case "RED":
                        setImageMask(TINTCHECK, REDINT);
                        setImageMask(TINT, BLKINT);
                        //Log.d("vote", "user voted red");
                        break;
                    case "BLK":
                        setImageMask(TINT, REDINT);
                        setImageMask(TINTCHECK, BLKINT);
                        //Log.d("vote", "user voted black");
                        break;
                    default:
                        break;
                }

            }
        });
    }

    public void clearList(){
        Log.d("Clear", "Before List Size: " + Integer.toString(masterList.size()));
        masterList.clear();
        notifyDataSetChanged();
        Log.d("Clear", "After List Size: " + Integer.toString(masterList.size()));

    }

    @Override
    public int getItemCount() {
        return masterList == null ? 0 : masterList.size();
    }

    public void setLoaded() {
        isLoading = false;
    }

    private class PostCardViewHolder extends RecyclerView.ViewHolder {

        public TextView questionTV;
        public TextView rednameTV;
        public TextView blacknameTV;
        public ImageView redImage;
        public ImageView blackImage;
        public View redgraphView;
        public RelativeLayout graphBox;
        public Button sortTypeSelector;

        public PostCardViewHolder (View view){
            super(view);
            questionTV = (TextView)view.findViewById(R.id.post_page_question);
            rednameTV = (TextView)view.findViewById(R.id.rednametvpc);
            blacknameTV = (TextView)view.findViewById(R.id.blacknametvpc);
            redImage = (ImageView)view.findViewById(R.id.rediv);
            blackImage = (ImageView)view.findViewById(R.id.blackiv);
            redgraphView = view.findViewById(R.id.redgraphview);
            graphBox = (RelativeLayout)view.findViewById(R.id.graphbox);
            sortTypeSelector = (Button)view.findViewById(R.id.sort_type_selector_pc);
        }
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
        public TextView heartCount; //TODO: perhaps we should show two counts, one for heard and one for broken hearts, instead of summing it up into one heartCount? Or is that not necessary, after all major websites seem to just sum them into one single count.
        public Button replyButton;
        public ImageButton upvoteButton, downvoteButton;

        public UserViewHolder(View view) {
            super(view);
            circView = (CircleImageView)view.findViewById(R.id.profile_image_cs);
            author = (TextView) view.findViewById(R.id.usernametvcs);
            timestamp = (TextView) view.findViewById(R.id.timetvcs);
            content = (TextView) view.findViewById(R.id.usercomment);
            heartCount = (TextView) view.findViewById(R.id.heartCount);
            replyButton = (Button) view.findViewById(R.id.replybuttoncs);
            upvoteButton = (ImageButton) view.findViewById(R.id.heartbutton);
            downvoteButton = (ImageButton) view.findViewById(R.id.broken_heart_button);
        }


    }

    //TODO: update function intent to launch profile page once profile page is available. For now, it leads to StartScreen.
    public void profileClicked(View view){
        if(((MainContainer)activity).getMainFrag().getUILifeStatus()){
            //TODO: implement this for when profile picture is clicked on CommentCard and TopCard
        }
    }

    public void setLeftMargin (View v, int margin) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(margin, 0, 0, 0);
            v.requestLayout();
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

    //sets mask and also sets the drawable to corresponding imageview
    private void setImageMask(int maskCode, int redOrBlack){

        //Log.d("input", "maskCode = " + Integer.toString(maskCode) + ", redOrBlack = " + Integer.toString(redOrBlack));
        switch (redOrBlack){
            case REDINT:
                switch (maskCode){
                    case NOMASK:
                        redLayers[1].setAlpha(0);
                        redLayers[2].setAlpha(0);
                        hideGraph();
                        break;
                    case TINT:
                        redLayers[1].setAlpha(175);
                        redLayers[2].setAlpha(0);
                        showGraph();
                        break;
                    case TINTCHECK:
                        redLayers[1].setAlpha(175);
                        redLayers[2].setAlpha(255);
                        showGraph();
                        break;
                    default:
                        return;
                }
                redLayerDrawable = new LayerDrawable(redLayers);
                postCard.redImage.setImageDrawable(redLayerDrawable);
                postCard.redImage.invalidate();
                break;

            case BLKINT:
                switch (maskCode){
                    case NOMASK:
                        blackLayers[1].setAlpha(0);
                        blackLayers[2].setAlpha(0);
                        break;
                    case TINT:
                        blackLayers[1].setAlpha(175);
                        blackLayers[2].setAlpha(0);
                        break;
                    case TINTCHECK:
                        blackLayers[1].setAlpha(175);
                        blackLayers[2].setAlpha(255);
                        break;
                    default:
                        return;
                }
                blackLayerDrawable = new LayerDrawable(blackLayers);
                postCard.blackImage.setImageDrawable(blackLayerDrawable);
                postCard.blackImage.invalidate();
                break;

            default:
                return;
        }

    }

    private void hideGraph(){
        //Log.d("graph", "hide");
        postCard.graphBox.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,0));
    }

    private void showGraph(){
        //Log.d("graph", "show");
        //Log.d("graph", "RED: " + Integer.toString(post.getRedcount()));
        //Log.d("graph", "BLK: " + Integer.toString(post.getBlackcount()));
        int redWidth = (int)((activity.getWindowWidth() - 8 ) * ( (float)post.getRedcount() / (float)(post.getRedcount() + post.getBlackcount()) ));   //TODO: the - 8 is for padding. Whenever we update the post_card content padding, also update that integer
        RelativeLayout.LayoutParams redgraphParams = new RelativeLayout.LayoutParams(redWidth, RelativeLayout.LayoutParams.MATCH_PARENT);
        redgraphParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        postCard.redgraphView.setLayoutParams(redgraphParams);
        postCard.redgraphView.setBackground(ContextCompat.getDrawable(activity, R.drawable.redgraph));

        postCard.graphBox.setLayoutParams(graphBoxParams);
    }

    public void appendToList(List<Object> listInput){
        masterList.addAll(listInput);
        notifyDataSetChanged();
    }

}