package com.vs.bcd.versus.adapter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.renderscript.ScriptGroup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.vs.bcd.versus.OnLoadMoreListener;
import com.vs.bcd.versus.activity.MainActivity;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.activity.PhoneOrEmail;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.VSCNode;
import com.vs.bcd.versus.model.VSComment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.os.Build.ID;


public class PostPageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private final int VIEW_TYPE_POSTCARD = 2;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean isLoading;
    private MainContainer activity;
    private Post post;
    private List<Object> masterList;
    private List<VSComment> childrenList = new ArrayList<>();
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;
    private AmazonS3 s3;
    private PostCardViewHolder postCard;
    private Bitmap redBMP = null;
    private Bitmap blackBMP = null;
    private boolean downloadImages, includesPost;

    public PostPageAdapter(RecyclerView recyclerView, List<Object> vsComments, Post post, MainContainer activity, boolean downloadImages, boolean includesPost) {
        this.s3 = ((MainContainer)activity).getS3Client();
        masterList = vsComments;
        this.post = post;
        this.activity = activity;
        this.downloadImages = downloadImages;
        this.includesPost = includesPost;

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

            final VSComment currentComment = (VSComment)masterList.get(position);

            setLeftMargin(((UserViewHolder) holder).circView, 150 * currentComment.getNestedLevel());  //left margin (indentation) of 150dp per nested level

            //set onClickListener for profile pic
            ((UserViewHolder) holder).circView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    profileClicked(v);
                }
            });

            //this is where values are put into the layout, from the VSComment object

            UserViewHolder userViewHolder = (UserViewHolder) holder;
            userViewHolder.author.setText(currentComment.getAuthor());
            userViewHolder.timestamp.setText(getTimeString(currentComment.getTimestamp()));
            userViewHolder.content.setText(currentComment.getContent());
            userViewHolder.heartCount.setText( Integer.toString(currentComment.getUpvotes() - currentComment.getDownvotes()) );
            //set CardView onClickListener to go to PostPage fragment with corresponding Comments data (this will be a PostPage without post_card)
            userViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.getPostPage().setCommentsPage(currentComment);
                }
            });
            userViewHolder.replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.getCommentEnterFragment().setContentReplyToComment(currentComment);
                    activity.getViewPager().setCurrentItem(4);
                }
            });


        } else if (holder instanceof PostCardViewHolder) {
            postCard = (PostCardViewHolder) holder;
            TransferUtility transferUtility = new TransferUtility(s3, activity.getApplicationContext());
            postCard.questionTV.setText(post.getQuestion());
            postCard.rednameTV.setText(post.getRedname());
            postCard.blacknameTV.setText(post.getBlackname());
            //TODO: have a field that lets me know if this post contains left / right images so that I do S3 query only when I have to.
            //TODO: and clear the BMP's whenever the fragment detaches, i think we already clear most fields anyway but still
            if(downloadImages){
                downloadImageFromAWS(); //downloads images and calls the helper method setImages() once download completes to set the images on post card
                downloadImages = false; //so that we don't repeat download while same adapter is alive and performing routine refresh function or whatever.
                                        // when we want to update the whole post page including the images, we simply create new adapter in PostPage.setContent(with downloadImages = true)
            }
            else{
                if(activity.hasXBMP()){
                    postCard.redImage.setImageBitmap(activity.getXBMP());
                }
                if(activity.hasYBMP()){
                    postCard.blackImage.setImageBitmap(activity.getYBMP());
                }
            }

        } else if (holder instanceof LoadingViewHolder) { //TODO: handle loading view to be implemented soon
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    private void setUpCommentSectionList(VSCNode node){
        node.setNestedLevelandGetComment(0);


    }

    public void setCommentList(VSCNode rootNode){
        VSCNode tempChildNode, tempGCNode;
        childrenList.add(rootNode.setNestedLevelandGetComment(0)); //root node
        if(rootNode.hasChild()){    //first child
            tempChildNode = rootNode.getFirstChild();
            childrenList.add(tempChildNode.setNestedLevelandGetComment(1));

            if(tempChildNode.hasChild()){   //first child's first child
                tempGCNode = tempChildNode.getFirstChild();
                childrenList.add(tempGCNode.setNestedLevelandGetComment(2));

                if(tempGCNode.hasTailSibling()){    //first child's second child
                    childrenList.add((tempGCNode.getTailSibling()).setNestedLevelandGetComment(2));
                }
            }

            if(tempChildNode.hasTailSibling()){ //second child
                tempChildNode = tempChildNode.getTailSibling();
                childrenList.add(tempChildNode.setNestedLevelandGetComment(1));

                if(tempChildNode.hasChild()){   //second child's first child
                    tempGCNode = tempChildNode.getFirstChild();
                    childrenList.add(tempGCNode.setNestedLevelandGetComment(2));

                    if(tempGCNode.hasTailSibling()){    //second child's second child
                        childrenList.add((tempGCNode.getTailSibling()).setNestedLevelandGetComment(2));
                    }
                }

            }
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

    public void setImages(final Bitmap leftBmp, final Bitmap rightBmp){


        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(leftBmp != null){
                    postCard.redImage.setImageBitmap(leftBmp);
                }

                if(rightBmp != null){
                    postCard.blackImage.setImageBitmap(rightBmp);
                }
            }
        });
    }

    private void setImages(){


        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //TODO: set in-app images here, i.e. emojis and clipart, into redBMP and blackBMP
                //otherise redBMP/blackBMP would be either null, or if marked for image download they would hold the download image as BMP

                activity.setBMP(redBMP, blackBMP);

                if(redBMP != null){
                    postCard.redImage.setImageBitmap(redBMP);
                }
                if(blackBMP != null){
                    postCard.blackImage.setImageBitmap(blackBMP);
                }

            }
        });
    }

    public void clearList(){
        Log.d("Clear", "Before List Size: " + Integer.toString(masterList.size()));
        masterList.clear();
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

        public PostCardViewHolder (View view){
            super(view);
            questionTV = (TextView)view.findViewById(R.id.post_page_question);
            rednameTV = (TextView)view.findViewById(R.id.rednametvpc);
            blacknameTV = (TextView)view.findViewById(R.id.blacknametvpc);
            redImage = (ImageView)view.findViewById(R.id.rediv);
            blackImage = (ImageView)view.findViewById(R.id.blackiv);
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

        public UserViewHolder(View view) {
            super(view);
            circView = (CircleImageView)view.findViewById(R.id.profile_image_cs);
            author = (TextView) view.findViewById(R.id.usernametvcs);
            timestamp = (TextView) view.findViewById(R.id.timetvcs);
            content = (TextView) view.findViewById(R.id.usercomment);
            heartCount = (TextView) view.findViewById(R.id.heartCount);
            replyButton = (Button) view.findViewById(R.id.replybuttoncs);
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


}