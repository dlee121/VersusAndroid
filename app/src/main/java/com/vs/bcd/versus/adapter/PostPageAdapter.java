package com.vs.bcd.versus.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.bumptech.glide.Glide;
import com.loopj.android.http.HttpGet;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.model.AWSV4Auth;
import com.vs.bcd.versus.model.GlideUrlCustom;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SquareImageView;
import com.vs.bcd.versus.model.TopCardObject;
import com.vs.bcd.versus.model.UserAction;
import com.vs.bcd.versus.model.VSComment;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
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


public class PostPageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int NOVOTE = 0;
    public static final int UPVOTE = 1;
    public static final int DOWNVOTE = 2;

    private final int NOMASK = 0;
    private final int TINT = 1;
    private final int TINTCHECK = 2;
    private final int RED = 0;
    private final int BLK = 1;

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private final int VIEW_TYPE_POSTCARD = 2;
    private final int VIEW_TYPE_TOPCARD = 3;
    private final int VIEW_TYPE_POSTCARD_TEXTONLY = 4;
    private boolean isLoading;
    private MainContainer activity;
    private Post post;
    private List<Object> masterList;
    private List<VSComment> childrenList = new ArrayList<>();
    private int visibleThreshold = 8;
    private int lastVisibleItem, totalItemCount;
    private RecyclerView.ViewHolder postCard;
    private Bitmap redBMP = null;
    private Bitmap blackBMP = null;
    private int pageLevel;
    private UserAction userAction;
    private Map<String, String> actionMap;
    private Drawable[] redLayers = new Drawable[2];
    private Drawable[] blackLayers = new Drawable[2];
    private ShapeDrawable redTint;
    private ShapeDrawable blackTint;
    private LayerDrawable redLayerDrawable;
    private LayerDrawable blackLayerDrawable;
    private RelativeLayout.LayoutParams graphBoxParams = null;
    private RelativeLayout.LayoutParams graphBoxParamsTextOnly = null;
    private RelativeLayout.LayoutParams sortSelectorLowerLP, sortBackgroundLowerLP, seeMoreContainerLP;

    private Toast mToast;
    private ListPopupWindow listPopupWindow;
    private Button topCardSortButton;

    private int DEFAULT = 0;
    private int S3 = 1;

    private final int MOST_RECENT = 0;
    private final int POPULAR = 1;
    private final int CHRONOLOGICAL = 2;

    private boolean lockButtons = false; //TODO: currently once we lockButtons = true we don't set it back to false because we set it to true for refreshes and after a refresh we get a new adapter and destroy the old one and the new one has lockButtons = false initially.
                                            //TODO: But eventually we should change to not making a new adapter each time, and also properly setting lockButtons back to false (by tracing back why we set it to true and setting it to false once condition flips).


    private CircleImageView profileImageView;

    //to set imageviews, first fill out the drawable[3] with 0=image layer, 1=tint layer, 2=check mark layer, make LayerDrawable out of the array, then use setImageMask which sets the correct mask layers AND ALSO sets imageview drawable as the LayerDrawable

    public PostPageAdapter(List<Object> masterList, Post post, MainContainer activity, int pageLevel) {
        this.masterList = masterList;
        this.post = post;
        this.activity = activity;
        this.pageLevel = pageLevel;
        userAction = activity.getPostPage().getUserAction();
        actionMap = userAction.getActionRecord();

        graphBoxParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 15);
        graphBoxParams.addRule(RelativeLayout.BELOW, R.id.left_percentage);
        graphBoxParamsTextOnly = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 15);
        graphBoxParamsTextOnly.addRule(RelativeLayout.BELOW, R.id.left_percentage_pcto);

        seeMoreContainerLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        seeMoreContainerLP.addRule(RelativeLayout.ALIGN_END, R.id.usercomment);
        seeMoreContainerLP.addRule(RelativeLayout.BELOW, R.id.usercomment);
        //Log.d("DEBUG", "Action Map Size: " + Integer.toString(actionMap.size()));
    }

    @Override
    public int getItemViewType(int position) {
        if(position  == 0){
            if(masterList.get(0) instanceof Post){
                Post thisPost = (Post) masterList.get(0);
                if(thisPost.getRedimg() % 10 == DEFAULT && thisPost.getBlackimg() % 10 == DEFAULT){
                    return VIEW_TYPE_POSTCARD_TEXTONLY;
                }
                else{
                    return VIEW_TYPE_POSTCARD;
                }
            }
            return VIEW_TYPE_TOPCARD;
        }
        else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(activity).inflate(R.layout.comment_group_card, parent, false);
            return new CommentViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(activity).inflate(R.layout.item_loading, parent, false);
            Log.d("hey", "this happens?");
            return new LoadingViewHolder(view);
        } else if (viewType == VIEW_TYPE_POSTCARD) {
            View view = LayoutInflater.from(activity).inflate(R.layout.post_card, parent, false);
            return  new PostCardViewHolder(view);
        } else if(viewType == VIEW_TYPE_TOPCARD){
            View view = LayoutInflater.from(activity).inflate(R.layout.top_card, parent, false);
            return new TopCardViewHolder(view);
        } else if(viewType == VIEW_TYPE_POSTCARD_TEXTONLY){
            View view = LayoutInflater.from(activity).inflate(R.layout.post_card_textonly, parent, false);
            return new PostCardTextOnlyViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof CommentViewHolder) { //holds comments

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

                final CommentViewHolder commentViewHolder = (CommentViewHolder) holder;

                final String authorName = currentComment.getAuthor();

                int margin = activity.getResources().getDimensionPixelSize(R.dimen.comment_margin); // margin in pixels


                setLeftMargin(commentViewHolder.author, margin * currentComment.getNestedLevel());  //left margin (indentation) of 50dp per nested level

                switch (currentComment.getUservote()){
                    case NOVOTE:
                        commentViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                        commentViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                        break;
                    case UPVOTE:
                        commentViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                        break;
                    case DOWNVOTE:
                        commentViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                        break;
                    default:
                        commentViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                        commentViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                        break;
                }

                RelativeLayout.LayoutParams viewMoreContainerLP = (RelativeLayout.LayoutParams) commentViewHolder.viewMoreContainer.getLayoutParams();
                if(currentComment.getChild_count() > 2){
                    viewMoreContainerLP.height = RelativeLayout.LayoutParams.WRAP_CONTENT;

                    int howManyMore = currentComment.getChild_count() - 2;
                    if(howManyMore == 1){
                        commentViewHolder.viewMoreButton.setText("View 1 More Reply");
                    }
                    else{
                        commentViewHolder.viewMoreButton.setText("View " + Integer.toString(howManyMore) + " More Replies");
                    }

                    commentViewHolder.viewMoreButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Log.d("pageLevel", Integer.toString(pageLevel));
                            if(pageLevel < 2 && !lockButtons){ //itemView clicks are handled only for root page and children page
                                activity.getPostPage().itemViewClickHelper(currentComment);
                            }
                        }
                    });
                }
                else{
                    viewMoreContainerLP.height = 0;
                }

                commentViewHolder.author.setText(currentComment.getAuthor());
                if(!currentComment.getAuthor().equals("[deleted]")){
                    commentViewHolder.author.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            activity.goToProfile(authorName, true);
                            activity.setProfileBackDestination(3);
                        }
                    });
                }


                commentViewHolder.overflowMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    if(!lockButtons){
                        showListPopupWindow(activity.getUsername().equals(currentComment.getAuthor()), commentViewHolder.overflowMenu, position);
                    }
                    }
                });

                commentViewHolder.timestamp.setText(getTimeString(currentComment.getTime()));
                //final int imgOffset = 8;
                //final TextView timeTV = commentViewHolder.timestamp;
                switch (currentComment.getCurrentMedal()){
                    case 0:
                        break; //no medal, default currentMedal value
                    case 1: //bronze
                        commentViewHolder.medalImage.setImageResource(R.drawable.bronzemedal);
                        break;
                    case 2: //silver
                        commentViewHolder.medalImage.setImageResource(R.drawable.silvermedal);
                        break;
                    case 3: //gold
                        commentViewHolder.medalImage.setImageResource(R.drawable.goldmedal);
                        break;
                }

                commentViewHolder.content.setText(currentComment.getContent());
                commentViewHolder.content.post(new Runnable() {
                    @Override
                    public void run() {
                        if(commentViewHolder.content.getLineCount() > 2){
                            commentViewHolder.seeMoreContainer.setLayoutParams(seeMoreContainerLP);

                            RelativeLayout.LayoutParams replyButtonLP = (RelativeLayout.LayoutParams) commentViewHolder.replyButton.getLayoutParams();
                            replyButtonLP.removeRule(RelativeLayout.ALIGN_END);
                            replyButtonLP.addRule(RelativeLayout.START_OF, R.id.see_more_container);
                            replyButtonLP.setMarginEnd(activity.getResources().getDimensionPixelSize(R.dimen.reply_button_margin_end));
                            commentViewHolder.replyButton.setLayoutParams(replyButtonLP);

                            commentViewHolder.seeMoreButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if(commentViewHolder.seeMoreButton.getText().equals("See More")){
                                        commentViewHolder.content.setMaxLines(262);
                                        commentViewHolder.seeMoreButton.setText("See Less");
                                        //commentViewHolder.ellipsis.setText("");
                                    }
                                    else{
                                        commentViewHolder.content.setMaxLines(2);
                                        commentViewHolder.seeMoreButton.setText("See More");
                                        //commentViewHolder.ellipsis.setText("...");
                                    }
                                }
                            });
                        }
                        else{
                            commentViewHolder.seeMoreContainer.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 0));

                            RelativeLayout.LayoutParams replyButtonLP = (RelativeLayout.LayoutParams) commentViewHolder.replyButton.getLayoutParams();
                            replyButtonLP.removeRule(RelativeLayout.START_OF);
                            replyButtonLP.addRule(RelativeLayout.ALIGN_END, R.id.usercomment);
                            replyButtonLP.setMarginEnd(0);
                            commentViewHolder.replyButton.setLayoutParams(replyButtonLP);

                        }
                    }
                });

                commentViewHolder.upvotes.setText( Integer.toString(currentComment.getUpvotes()) );
                commentViewHolder.downvotes.setText( Integer.toString(currentComment.getDownvotes()) );
                //set CardView onClickListener to go to PostPage fragment with corresponding Comments data (this will be a PostPage without post_card)

                if(currentComment.getIsNew()){
                    int colorFrom = ContextCompat.getColor(activity, R.color.vsBlue_light);
                    int colorTo = Color.WHITE;
                    int duration = 1000;
                    ObjectAnimator obAnim = ObjectAnimator.ofObject(commentViewHolder.itemView, "backgroundColor", new ArgbEvaluator(), colorFrom, colorTo)
                            .setDuration(duration);
                    obAnim.setRepeatCount(1);
                    obAnim.setStartDelay(350);
                    obAnim.start();
                    currentComment.setIsNew(false);
                }

                if(!currentComment.getIsHighlighted()){
                    commentViewHolder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                }
                else{
                    commentViewHolder.itemView.setBackgroundColor(Color.parseColor("#FEE38F"));
                }

                commentViewHolder.replyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.getPostPage().itemReplyClickHelper(currentComment, position);
                    }
                });

                commentViewHolder.upvoteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!lockButtons){
                            int userVote = currentComment.getUservote();
                            if(userVote == UPVOTE){
                                commentViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                                currentComment.setUservote(NOVOTE);
                                actionMap.put(currentComment.getComment_id(), "N");
                                //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                            }
                            else if(userVote == DOWNVOTE){
                                commentViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                                commentViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                                currentComment.setUservote(UPVOTE);
                                actionMap.put(currentComment.getComment_id(), "U");
                            }
                            else if(userVote == NOVOTE){
                                commentViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                                currentComment.setUservote(UPVOTE);
                                actionMap.put(currentComment.getComment_id(), "U");
                            }
                            activity.getPostPage().addFreshlyVotedComment(currentComment.getComment_id(), new Pair<>(new Integer(currentComment.getUpvotes()), new Integer(currentComment.getDownvotes())));
                            commentViewHolder.upvotes.setText( Integer.toString(currentComment.getUpvotes()) );
                            commentViewHolder.downvotes.setText( Integer.toString(currentComment.getDownvotes()) );
                        }
                    }
                });

                commentViewHolder.downvoteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!lockButtons){
                            int userVote = currentComment.getUservote();
                            if(userVote == DOWNVOTE){
                                commentViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                                currentComment.setUservote(NOVOTE);
                                actionMap.put(currentComment.getComment_id(), "N");
                                //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                            }
                            else if(userVote == UPVOTE){
                                commentViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                                commentViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                                currentComment.setUservote(DOWNVOTE);
                                actionMap.put(currentComment.getComment_id(), "D");
                            }
                            else if(userVote == NOVOTE){
                                commentViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                                currentComment.setUservote(DOWNVOTE);
                                actionMap.put(currentComment.getComment_id(), "D");
                            }
                            activity.getPostPage().addFreshlyVotedComment(currentComment.getComment_id(), new Pair<>(new Integer(currentComment.getUpvotes()), new Integer(currentComment.getDownvotes())));
                            commentViewHolder.upvotes.setText( Integer.toString(currentComment.getUpvotes()) );
                            commentViewHolder.downvotes.setText( Integer.toString(currentComment.getDownvotes()) );
                        }
                    }
                });
            }

        } else if (holder instanceof PostCardViewHolder) {
            //Log.d("DEBUG", "BIND EVENT");
            postCard = holder;

            final PostCardViewHolder postCardViewHolder = (PostCardViewHolder) holder;
            postCardViewHolder.author.setText(post.getAuthor());
            postCardViewHolder.votecount.setText(Integer.toString(post.getVotecount()) + " votes");
            postCardViewHolder.questionTV.setText(post.getQuestion());
            postCardViewHolder.rednameTV.setText(post.getRedname());
            postCardViewHolder.blacknameTV.setText(post.getBlackname());

            postCardViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!post.getAuthor().equals("[deleted]")) {
                        activity.goToProfile(post.getAuthor(), true);
                        activity.setProfileBackDestination(3);
                    }
                }
            });

            postCardViewHolder.profileImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!post.getAuthor().equals("[deleted]")) {
                        activity.goToProfile(post.getAuthor(), true);
                        activity.setProfileBackDestination(3);
                    }
                }
            });

            profileImageView = postCardViewHolder.profileImg;
            loadProfileImage(post.getAuthor());

            redTint = new ShapeDrawable(new RectShape());
            redTint.setIntrinsicWidth(postCardViewHolder.redIV.getWidth());
            redTint.setIntrinsicHeight(postCardViewHolder.redIV.getHeight());
            redTint.getPaint().setColor(Color.argb(175, 165, 35, 57));
            //redLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
            redLayers[0] = redTint;
            redLayers[1] = ContextCompat.getDrawable(activity, R.drawable.ic_check_overlay);

            blackTint = new ShapeDrawable(new RectShape());
            blackTint.setIntrinsicWidth(postCardViewHolder.blkIV.getWidth());
            blackTint.setIntrinsicHeight(postCardViewHolder.blkIV.getHeight());
            blackTint.getPaint().setColor(Color.argb(175, 48, 48, 48));
            //blackLayers[0] = ContextCompat.getDrawable(activity, R.drawable.default_background);
            blackLayers[0] = blackTint;
            blackLayers[1] = ContextCompat.getDrawable(activity, R.drawable.ic_check_overlay_2);

            postCardViewHolder.redimgBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!lockButtons){
                        if (!userAction.getVotedSide().equals("RED")) {
                            activity.getPostPage().redVotePressed();

                            postCardViewHolder.redMask.setVisibility(View.VISIBLE);
                            postCardViewHolder.checkCircleLeft.setVisibility(View.VISIBLE);

                            postCardViewHolder.blkMask.setVisibility(View.INVISIBLE);
                            postCardViewHolder.checkCircleRight.setVisibility(View.INVISIBLE);

                            showGraph();

                            if (mToast != null) {
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(activity, "Vote Submitted", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    }
                }
            });

            postCardViewHolder.blkimgBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!lockButtons){
                        if (!userAction.getVotedSide().equals("BLK")) {
                            activity.getPostPage().blackVotePressed();

                            postCardViewHolder.redMask.setVisibility(View.INVISIBLE);
                            postCardViewHolder.checkCircleLeft.setVisibility(View.INVISIBLE);

                            postCardViewHolder.blkMask.setVisibility(View.VISIBLE);
                            postCardViewHolder.checkCircleRight.setVisibility(View.VISIBLE);

                            showGraph();

                            if (mToast != null) {
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(activity, "Vote Submitted", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    }
                }
            });

            postCardViewHolder.sortTypeSelector.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!lockButtons){
                        activity.getPostPage().selectSortType("p");
                    }
                }
            });

            switch (activity.getPostPageSortType()) {
                case MOST_RECENT:
                    postCardViewHolder.sortTypeSelector.setText("MOST RECENT");
                    postCardViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case POPULAR:
                    postCardViewHolder.sortTypeSelector.setText("POPULAR");
                    postCardViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case CHRONOLOGICAL:
                    postCardViewHolder.sortTypeSelector.setText("CHRONOLOGICAL");
                    postCardViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_chrono_20small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
            }


            if (post.getRedimg() % 10 == S3) {
                try {
                    GlideUrlCustom gurlLeft = new GlideUrlCustom(activity.getImgURI(post, 0));
                    Glide.with(activity).load(gurlLeft).into(postCardViewHolder.redIV);
                } catch (Exception e) {
                    postCardViewHolder.redIV.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_background));
                }
            } else {
                postCardViewHolder.redIV.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_background));
            }

            if (post.getBlackimg() % 10 == S3) {
                try {
                    GlideUrlCustom gurlRight = new GlideUrlCustom(activity.getImgURI(post, 1));
                    Glide.with(activity).load(gurlRight).into(postCardViewHolder.blkIV);
                } catch (Exception e) {
                    postCardViewHolder.blkIV.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_background));
                }
            } else {
                postCardViewHolder.blkIV.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.default_background));
            }

            //set initial image mask;
            if(userAction.getVotedSide().equals("RED")){
                postCardViewHolder.redMask.setVisibility(View.VISIBLE);
                postCardViewHolder.checkCircleLeft.setVisibility(View.VISIBLE);

                postCardViewHolder.blkMask.setVisibility(View.INVISIBLE);
                postCardViewHolder.checkCircleRight.setVisibility(View.INVISIBLE);

                showGraph();
            }
            else if(userAction.getVotedSide().equals("BLK")){
                postCardViewHolder.redMask.setVisibility(View.INVISIBLE);
                postCardViewHolder.checkCircleLeft.setVisibility(View.INVISIBLE);

                postCardViewHolder.blkMask.setVisibility(View.VISIBLE);
                postCardViewHolder.checkCircleRight.setVisibility(View.VISIBLE);

                showGraph();
            }
            else{
                postCardViewHolder.redMask.setVisibility(View.INVISIBLE);
                postCardViewHolder.checkCircleLeft.setVisibility(View.INVISIBLE);

                postCardViewHolder.blkMask.setVisibility(View.INVISIBLE);
                postCardViewHolder.checkCircleRight.setVisibility(View.INVISIBLE);

                hideGraph();
            }



        } else if(holder instanceof PostCardTextOnlyViewHolder){
            postCard = holder;

            final PostCardTextOnlyViewHolder postCardTextOnlyViewHolder = (PostCardTextOnlyViewHolder) holder;

            postCardTextOnlyViewHolder.author.setText(post.getAuthor());
            postCardTextOnlyViewHolder.votecount.setText(Integer.toString(post.getVotecount()) + " votes");
            postCardTextOnlyViewHolder.questionTV.setText(post.getQuestion());
            postCardTextOnlyViewHolder.rednameTV.setText(post.getRedname());
            postCardTextOnlyViewHolder.blacknameTV.setText(post.getBlackname());

            postCardTextOnlyViewHolder.author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!post.getAuthor().equals("[deleted]")) {
                        activity.goToProfile(post.getAuthor(), true);
                        activity.setProfileBackDestination(3);
                    }
                }
            });

            postCardTextOnlyViewHolder.profileImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!post.getAuthor().equals("[deleted]")) {
                        activity.goToProfile(post.getAuthor(), true);
                        activity.setProfileBackDestination(3);
                    }
                }
            });

            profileImageView = postCardTextOnlyViewHolder.profileImg;
            loadProfileImage(post.getAuthor());

            postCardTextOnlyViewHolder.sortTypeSelector.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!lockButtons){
                        activity.getPostPage().selectSortType("p");
                    }
                }
            });

            switch (activity.getPostPageSortType()) {
                case MOST_RECENT:
                    postCardTextOnlyViewHolder.sortTypeSelector.setText("MOST RECENT");
                    postCardTextOnlyViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case POPULAR:
                    postCardTextOnlyViewHolder.sortTypeSelector.setText("POPULAR");
                    postCardTextOnlyViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case CHRONOLOGICAL:
                    postCardTextOnlyViewHolder.sortTypeSelector.setText("CHRONOLOGICAL");
                    postCardTextOnlyViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_chrono_20small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
            }

            postCardTextOnlyViewHolder.leftBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!lockButtons){
                        if (!userAction.getVotedSide().equals("RED")) {
                            activity.getPostPage().redVotePressed();

                            postCardTextOnlyViewHolder.checkCircleLeft.setVisibility(View.VISIBLE);
                            postCardTextOnlyViewHolder.checkCircleRight.setVisibility(View.INVISIBLE);

                            showGraph();

                            if (mToast != null) {
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(activity, "Vote Submitted", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    }
                }
            });

            postCardTextOnlyViewHolder.rightBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!lockButtons){
                        if (!userAction.getVotedSide().equals("BLK")) {
                            activity.getPostPage().blackVotePressed();

                            postCardTextOnlyViewHolder.checkCircleLeft.setVisibility(View.INVISIBLE);
                            postCardTextOnlyViewHolder.checkCircleRight.setVisibility(View.VISIBLE);

                            showGraph();

                            if (mToast != null) {
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(activity, "Vote Submitted", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    }
                }
            });


            if(userAction.getVotedSide().equals("RED")){
                postCardTextOnlyViewHolder.checkCircleLeft.setVisibility(View.VISIBLE);
                postCardTextOnlyViewHolder.checkCircleRight.setVisibility(View.INVISIBLE);
                showGraph();
            }
            else if(userAction.getVotedSide().equals("BLK")){
                postCardTextOnlyViewHolder.checkCircleLeft.setVisibility(View.INVISIBLE);
                postCardTextOnlyViewHolder.checkCircleRight.setVisibility(View.VISIBLE);
                showGraph();
            }
            else{
                postCardTextOnlyViewHolder.checkCircleLeft.setVisibility(View.INVISIBLE);
                postCardTextOnlyViewHolder.checkCircleRight.setVisibility(View.INVISIBLE);
                hideGraph();
            }


        } else if(holder instanceof TopCardViewHolder){

            final TopCardObject topCardObject = (TopCardObject) masterList.get(position);
            final TopCardViewHolder topCardViewHolder = (TopCardViewHolder) holder;

            switch (activity.getPostPage().getSortType()){
                case MOST_RECENT:
                    topCardViewHolder.sortButton.setText("MOST RECENT");
                    topCardViewHolder.sortButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;

                case POPULAR:
                    topCardViewHolder.sortButton.setText("POPULAR");
                    topCardViewHolder.sortButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;

                case CHRONOLOGICAL:
                    topCardViewHolder.sortButton.setText("CHRONOLOGICAL");
                    topCardViewHolder.sortButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_chrono_20small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
            }

            switch (topCardObject.getUservote()){
                case NOVOTE:
                    topCardViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                    topCardViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                    break;
                case UPVOTE:
                    topCardViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                    break;
                case DOWNVOTE:
                    topCardViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                    break;
                default:
                    topCardViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                    topCardViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                    break;
            }

            topCardViewHolder.author.setText(topCardObject.getAuthor());
            final String authorName = topCardObject.getAuthor();
            if(!authorName.equals("[deleted]")){
                topCardViewHolder.author.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.goToProfile(authorName, true);
                        activity.setProfileBackDestination(3);
                    }
                });
            }

            topCardViewHolder.overflowMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!lockButtons){
                        showListPopupWindow(activity.getUsername().equals(authorName), topCardViewHolder.overflowMenu, position);
                    }
                }
            });

            topCardViewHolder.timestamp.setText(getTimeString(topCardObject.getTime()));

            switch (topCardObject.getCurrentMedal()){
                case 0:
                    break; //no medal, default currentMedal value
                case 1: //bronze
                    topCardViewHolder.medalImage.setImageResource(R.drawable.bronzemedal);
                    break;
                case 2: //silver
                    topCardViewHolder.medalImage.setImageResource(R.drawable.silvermedal);
                    break;
                case 3: //gold
                    topCardViewHolder.medalImage.setImageResource(R.drawable.goldmedal);
                    break;
            }

            topCardViewHolder.content.setText(topCardObject.getContent());

            topCardViewHolder.upvotesCount.setText(Integer.toString(topCardObject.getUpvotes()));
            topCardViewHolder.downvotesCount.setText(Integer.toString(topCardObject.getDownvotes()));

            topCardViewHolder.upvotesCount.setText(Integer.toString(topCardObject.getUpvotes()));
            topCardViewHolder.downvotesCount.setText(Integer.toString(topCardObject.getDownvotes()));

            topCardViewHolder.upvoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!lockButtons){
                        int userVote = topCardObject.getUservote();
                        if(userVote == UPVOTE){
                            topCardViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                            topCardObject.setUservote(NOVOTE);
                            actionMap.put(topCardObject.getComment_id(), "N");
                            //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                        }
                        else if(userVote == DOWNVOTE){
                            topCardViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                            topCardViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                            topCardObject.setUservote(UPVOTE);
                            actionMap.put(topCardObject.getComment_id(), "U");
                        }
                        else if(userVote == NOVOTE){
                            topCardViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart_highlighted);
                            topCardObject.setUservote(UPVOTE);
                            actionMap.put(topCardObject.getComment_id(), "U");
                        }
                        activity.getPostPage().addFreshlyVotedComment(topCardObject.getComment_id(), new Pair<>(new Integer(topCardObject.getUpvotes()), new Integer(topCardObject.getDownvotes())));
                        topCardViewHolder.upvotesCount.setText(Integer.toString(topCardObject.getUpvotes()));
                        topCardViewHolder.downvotesCount.setText(Integer.toString(topCardObject.getDownvotes()));
                    }
                }
            });

            topCardViewHolder.downvoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!lockButtons){
                        int userVote = topCardObject.getUservote();
                        if(userVote == DOWNVOTE){
                            topCardViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken);
                            topCardObject.setUservote(NOVOTE);
                            actionMap.put(topCardObject.getComment_id(), "N");
                            //actionMap.remove(currentComment.getComment_id());   //instead of removing, set record to "N" so that we'll find it in wrteActionsToDB and decrement the past vote if there were a past vote
                        }
                        else if(userVote == UPVOTE){
                            topCardViewHolder.upvoteButton.setImageResource(R.drawable.ic_heart);
                            topCardViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                            topCardObject.setUservote(DOWNVOTE);
                            actionMap.put(topCardObject.getComment_id(), "D");
                        }
                        else if(userVote == NOVOTE){
                            topCardViewHolder.downvoteButton.setImageResource(R.drawable.ic_heart_broken_highlighted);
                            topCardObject.setUservote(DOWNVOTE);
                            actionMap.put(topCardObject.getComment_id(), "D");
                        }
                        activity.getPostPage().addFreshlyVotedComment(topCardObject.getComment_id(), new Pair<>(new Integer(topCardObject.getUpvotes()), new Integer(topCardObject.getDownvotes())));
                        topCardViewHolder.upvotesCount.setText(Integer.toString(topCardObject.getUpvotes()));
                        topCardViewHolder.downvotesCount.setText(Integer.toString(topCardObject.getDownvotes()));
                    }
                }
            });

            topCardViewHolder.replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.getCommentEnterFragment().setContentReplyToComment(topCardObject, 0);
                    activity.getViewPager().setCurrentItem(4);
                }
            });

            topCardSortButton = topCardViewHolder.sortButton;
            topCardViewHolder.sortButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!lockButtons){
                        activity.getPostPage().selectSortType("c");
                    }
                }
            });

        }
    }

    public void setLockButtons(boolean setting){
        lockButtons = setting;
    }

    public void setTopCardSortTypeHint(int sortType){
        if(topCardSortButton != null){
            switch (sortType){
                case MOST_RECENT:
                    topCardSortButton.setText("MOST RECENT");
                    topCardSortButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;

                case POPULAR:
                    topCardSortButton.setText("POPULAR");
                    topCardSortButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;

                case CHRONOLOGICAL:
                    topCardSortButton.setText("CHRONOLOGICAL");
                    topCardSortButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_chrono_20small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
            }
        }
    }

    public void clearList(){
        masterList.clear();
        notifyDataSetChanged();
    }

    public String getPostID(){
        return post.getPost_id();
    }

    public List<Object> getMasterList(){
        return masterList;
    }

    @Override
    public int getItemCount() {
        return masterList == null ? 0 : masterList.size();
    }

    private class PostCardViewHolder extends RecyclerView.ViewHolder {

        public TextView questionTV, rednameTV, blacknameTV, leftPercentage, rightPercentage, author, votecount;
        public SquareImageView redIV, blkIV, redMask, blkMask;
        public View redgraphView;
        public RelativeLayout graphBox;
        public Button sortTypeSelector;
        public RelativeLayout redimgBox, blkimgBox;
        public LinearLayout sortTypeBackground;
        public CircleImageView profileImg;
        public ImageView checkCircleLeft, checkCircleRight;


        public PostCardViewHolder (View view){
            super(view);
            author = view.findViewById(R.id.author_pc);
            votecount = view.findViewById(R.id.votecount_pc);
            profileImg = view.findViewById(R.id.profile_image_pc);
            questionTV = view.findViewById(R.id.post_page_question);
            rednameTV = view.findViewById(R.id.rednametvpc);
            blacknameTV = view.findViewById(R.id.blacknametvpc);
            redimgBox = view.findViewById(R.id.redimgbox);
            checkCircleLeft = redimgBox.findViewById(R.id.check_circle_leftimg);
            redMask = redimgBox.findViewById(R.id.rediv_mask);
            redIV = redimgBox.findViewById(R.id.rediv);
            blkimgBox = view.findViewById(R.id.blkimgbox);
            checkCircleRight = blkimgBox.findViewById(R.id.check_circle_rightimg);
            blkMask = blkimgBox.findViewById(R.id.blkiv_mask);
            blkIV = blkimgBox.findViewById(R.id.blackiv);
            redgraphView = view.findViewById(R.id.redgraphview);
            graphBox = view.findViewById(R.id.graphbox);
            sortTypeSelector = view.findViewById(R.id.sort_type_selector_pc);
            sortTypeBackground = view.findViewById(R.id.sort_type_background);
            leftPercentage = view.findViewById(R.id.left_percentage);
            rightPercentage = view.findViewById(R.id.right_percentage);
        }
    }

    private class PostCardTextOnlyViewHolder extends RecyclerView.ViewHolder {

        public TextView questionTV, rednameTV, blacknameTV, leftPercentage, rightPercentage, author, votecount;
        public View redgraphView;
        public RelativeLayout graphBox;
        public Button sortTypeSelector;
        public LinearLayout sortTypeBackground;
        public CircleImageView profileImg;
        public ImageView checkCircleLeft, checkCircleRight;
        public RelativeLayout leftBox, rightBox;


        public PostCardTextOnlyViewHolder (View view){
            super(view);
            author = view.findViewById(R.id.author_pcto);
            votecount = view.findViewById(R.id.votecount_pcto);
            profileImg = view.findViewById(R.id.profile_image_pcto);
            questionTV = view.findViewById(R.id.question_pcto);

            leftBox = view.findViewById(R.id.left_box);
            rednameTV = leftBox.findViewById(R.id.vsc_r_pcto);
            checkCircleLeft = leftBox.findViewById(R.id.check_red);

            rightBox = view.findViewById(R.id.right_box);
            blacknameTV = rightBox.findViewById(R.id.vsc_b_pcto);
            checkCircleRight = rightBox.findViewById(R.id.check_blue);

            redgraphView = view.findViewById(R.id.redgraphview_pcto);
            graphBox = view.findViewById(R.id.graphbox_pcto);
            sortTypeSelector = view.findViewById(R.id.sort_type_selector_pcto);
            sortTypeBackground = view.findViewById(R.id.sort_type_background_pcto);
            leftPercentage = view.findViewById(R.id.left_percentage_pcto);
            rightPercentage = view.findViewById(R.id.right_percentage_pcto);
        }
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public LoadingViewHolder(View view) {
            super(view);
            progressBar = view.findViewById(R.id.progressBar1);
        }
    }

    private class TopCardViewHolder extends RecyclerView.ViewHolder {

        public TextView timestamp, author, content, upvotesCount, downvotesCount;
        public ImageButton upvoteButton, downvoteButton, overflowMenu;
        public ImageView medalImage;
        public Button sortButton, replyButton;

        public TopCardViewHolder(View view) {
            super(view);
            author = view.findViewById(R.id.usernametvtc);
            overflowMenu = view.findViewById(R.id.topcard_overflow_menu);
            timestamp = view.findViewById(R.id.timetvtc);
            content = view.findViewById(R.id.usercommenttc);
            upvotesCount = view.findViewById(R.id.upvotes_tc);
            upvoteButton = view.findViewById(R.id.heartbuttontc);
            downvotesCount = view.findViewById(R.id.downvotes_tc);
            downvoteButton = view.findViewById(R.id.broken_heart_button_tc);
            medalImage = view.findViewById(R.id.medal_image_tc);
            replyButton = view.findViewById(R.id.replybuttontc);
            sortButton = view.findViewById(R.id.sort_type_selector_topcard);
        }


    }

    private class CommentViewHolder extends RecyclerView.ViewHolder {

        public TextView timestamp, author, content, upvotes, downvotes;
        public Button replyButton, seeMoreButton, viewMoreButton;
        public ImageButton upvoteButton, downvoteButton, overflowMenu;
        public ImageView medalImage;
        public LinearLayout seeMoreContainer, viewMoreContainer;

        //public TextView ellipsis;

        public CommentViewHolder(View view) {
            super(view);
            author = view.findViewById(R.id.usernametvcs);
            overflowMenu = view.findViewById(R.id.comment_overflow_menu);
            timestamp = view.findViewById(R.id.timetvcs);
            content = view.findViewById(R.id.usercomment);
            upvotes = view.findViewById(R.id.upvotes_cc);
            downvotes = view.findViewById(R.id.downvotes_cc);
            replyButton = view.findViewById(R.id.replybuttoncs);
            upvoteButton = view.findViewById(R.id.heartbutton);
            downvoteButton = view.findViewById(R.id.broken_heart_button);
            medalImage = view.findViewById(R.id.medal_image);

            seeMoreContainer = view.findViewById(R.id.see_more_container);
            viewMoreContainer = view.findViewById(R.id.view_replies_button_container);
            viewMoreButton = viewMoreContainer.findViewById(R.id.view_replies_button);
            seeMoreButton = seeMoreContainer.findViewById(R.id.see_more_button);
        }


    }


    private void setLeftMargin (View v, int margin) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(margin, 0, 0, 0);
            v.requestLayout();
        }
    }

    private String getTimeString(String timeStamp){
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

    private void hideGraph(){
        //Log.d("graph", "hide");
        if(postCard instanceof  PostCardViewHolder){
            PostCardViewHolder currentPostCard = (PostCardViewHolder) postCard;
            currentPostCard.leftPercentage.setVisibility(View.INVISIBLE);
            currentPostCard.rightPercentage.setVisibility(View.INVISIBLE);
            currentPostCard.graphBox.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,0));
        }
        else{
            PostCardTextOnlyViewHolder currentPostCard = (PostCardTextOnlyViewHolder) postCard;
            currentPostCard.leftPercentage.setVisibility(View.INVISIBLE);
            currentPostCard.rightPercentage.setVisibility(View.INVISIBLE);
            currentPostCard.graphBox.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,0));
        }


    }

    private void showGraph(){
        if(postCard == null){
            return;
        }

        if(postCard instanceof PostCardViewHolder){
            PostCardViewHolder currentViewHolder = (PostCardViewHolder) postCard;

            sortBackgroundLowerLP = (RelativeLayout.LayoutParams) currentViewHolder.sortTypeBackground.getLayoutParams();
            sortBackgroundLowerLP.addRule(RelativeLayout.BELOW, R.id.graphbox);
            currentViewHolder.sortTypeBackground.setLayoutParams(sortBackgroundLowerLP);

            sortSelectorLowerLP = (RelativeLayout.LayoutParams) currentViewHolder.sortTypeSelector.getLayoutParams();
            sortSelectorLowerLP.addRule(RelativeLayout.BELOW, R.id.graphbox);
            currentViewHolder.sortTypeSelector.setLayoutParams(sortSelectorLowerLP);

            float leftFloat, rightFloat;
            leftFloat = (float)post.getRedcount() / (float)(post.getRedcount() + post.getBlackcount());

            int redWidth = (int)(activity.getWindowWidth() * leftFloat);
            RelativeLayout.LayoutParams redgraphParams = new RelativeLayout.LayoutParams(redWidth, RelativeLayout.LayoutParams.MATCH_PARENT);
            redgraphParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            currentViewHolder.redgraphView.setLayoutParams(redgraphParams);
            currentViewHolder.redgraphView.setBackground(ContextCompat.getDrawable(activity, R.drawable.redgraph));
            currentViewHolder.graphBox.setLayoutParams(graphBoxParams);

            String leftPercentageText, rightPercentageText;

            leftPercentageText = Integer.toString((int)(leftFloat * 100)) + "%";
            rightPercentageText = Integer.toString((int)((1-leftFloat) * 100)) + "%";

            currentViewHolder.leftPercentage.setText(leftPercentageText);
            currentViewHolder.leftPercentage.setVisibility(View.VISIBLE);
            currentViewHolder.rightPercentage.setText(rightPercentageText);
            currentViewHolder.rightPercentage.setVisibility(View.VISIBLE);

        }
        else{
            PostCardTextOnlyViewHolder currentViewHolder = (PostCardTextOnlyViewHolder) postCard;

            sortBackgroundLowerLP = (RelativeLayout.LayoutParams) currentViewHolder.sortTypeBackground.getLayoutParams();
            sortBackgroundLowerLP.addRule(RelativeLayout.BELOW, R.id.graphbox);
            currentViewHolder.sortTypeBackground.setLayoutParams(sortBackgroundLowerLP);

            sortSelectorLowerLP = (RelativeLayout.LayoutParams) currentViewHolder.sortTypeSelector.getLayoutParams();
            sortSelectorLowerLP.addRule(RelativeLayout.BELOW, R.id.graphbox_pcto);
            currentViewHolder.sortTypeSelector.setLayoutParams(sortSelectorLowerLP);

            float leftFloat, rightFloat;
            leftFloat = (float)post.getRedcount() / (float)(post.getRedcount() + post.getBlackcount());

            int redWidth = (int)(activity.getWindowWidth() * leftFloat);
            RelativeLayout.LayoutParams redgraphParams = new RelativeLayout.LayoutParams(redWidth, RelativeLayout.LayoutParams.MATCH_PARENT);
            redgraphParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            currentViewHolder.redgraphView.setLayoutParams(redgraphParams);
            currentViewHolder.redgraphView.setBackground(ContextCompat.getDrawable(activity, R.drawable.redgraph));
            currentViewHolder.graphBox.setLayoutParams(graphBoxParamsTextOnly);

            String leftPercentageText, rightPercentageText;

            leftPercentageText = Integer.toString((int)(leftFloat * 100)) + "%";
            rightPercentageText = Integer.toString((int)((1-leftFloat) * 100)) + "%";

            currentViewHolder.leftPercentage.setText(leftPercentageText);
            currentViewHolder.leftPercentage.setVisibility(View.VISIBLE);
            currentViewHolder.rightPercentage.setText(rightPercentageText);
            currentViewHolder.rightPercentage.setVisibility(View.VISIBLE);

        }

    }

    public void appendToList(List<Object> listInput){
        masterList.addAll(listInput);
        notifyDataSetChanged();
    }

    public void setSortTypeHint(int sortTypeNum){
        if(postCard instanceof PostCardViewHolder){
            PostCardViewHolder currentViewHolder = (PostCardViewHolder) postCard;

            switch (sortTypeNum){
                case MOST_RECENT:
                    currentViewHolder.sortTypeSelector.setText("MOST RECENT");
                    currentViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case POPULAR:
                    currentViewHolder.sortTypeSelector.setText("POPULAR");
                    currentViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case CHRONOLOGICAL:
                    currentViewHolder.sortTypeSelector.setText("CHRONOLOGICAL");
                    currentViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_chrono_20small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
            }
        }
        else{
            PostCardTextOnlyViewHolder currentViewHolder = (PostCardTextOnlyViewHolder) postCard;

            switch (sortTypeNum){
                case MOST_RECENT:
                    currentViewHolder.sortTypeSelector.setText("MOST RECENT");
                    currentViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_new_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case POPULAR:
                    currentViewHolder.sortTypeSelector.setText("POPULAR");
                    currentViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_thumb_10small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
                case CHRONOLOGICAL:
                    currentViewHolder.sortTypeSelector.setText("CHRONOLOGICAL");
                    currentViewHolder.sortTypeSelector.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gray_chrono_20small, 0, R.drawable.ic_gray_arrow_dropdown, 0);
                    break;
            }
        }



    }

    private void showListPopupWindow(final boolean isAuthor, ImageButton anchorPoint, final int index){
        final String [] items;
        final Integer[] icons;
        boolean yesEdit = false;
        final boolean editAvailable;

        if(isAuthor){
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
            try {
                yesEdit = (System.currentTimeMillis() - df.parse(((VSComment)masterList.get(index)).getTime()).getTime() <= 300000);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            editAvailable = yesEdit;


            if(editAvailable){
                items = new String[]{"Edit", "Delete"};
                icons = new Integer[]{R.drawable.ic_edit, R.drawable.ic_delete};
            }
            else{
                items = new String[]{"Delete"};
                icons = new Integer[]{R.drawable.ic_delete};
            }

        }
        else{
            editAvailable = false;
            items = new String[] {"Report"};
            icons = new Integer[] {R.drawable.ic_flag};
        }
        int width = activity.getResources().getDimensionPixelSize(R.dimen.overflow_width);


        ListAdapter adapter = new ArrayAdapterWithIcon(activity, items, icons);

        listPopupWindow = new ListPopupWindow(activity);
        listPopupWindow.setAnchorView(anchorPoint);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setWidth(width);

        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(isAuthor){
                    if(editAvailable) {
                        switch (position) {
                            case 0:
                                editComment(index);
                                break;
                            case 1:
                                deleteComment(index);
                                break;
                        }
                    }
                    else{ //in this case, delete is the only option for now
                        deleteComment(index);
                    }

                }
                else{
                    //for now there's only one option for when not author of the comment, Report
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    //Yes button clicked
                                    final String reportCommentID = ((VSComment)masterList.get(index)).getComment_id();

                                    Runnable runnable = new Runnable() {
                                        public void run() {
                                            String reportPath = "reports/c/" + reportCommentID;
                                            activity.getFirebaseDatabaseReference().child(reportPath).setValue(true);
                                        }
                                    };
                                    Thread mythread = new Thread(runnable);
                                    mythread.start();

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage("Report this comment?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();

                }

                activity.enableClicksForListPopupWindowClose();
                listPopupWindow.dismiss();
            }
        });
        listPopupWindow.show();
        activity.disableClicksForListPopupWindowOpen();
    }

    private void editComment(final int index){
        boolean yesEdit = false;

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        try {
            yesEdit = (System.currentTimeMillis() - df.parse(((VSComment)masterList.get(index)).getTime()).getTime() <= 300000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(yesEdit){
            activity.getPostPage().editComment((VSComment)masterList.get(index), index);
        }
        else{
            Toast.makeText(activity, "Too late to edit.", Toast.LENGTH_SHORT).show();
        }
    }

    public void editCommentLocal(int index, String text, String commentID){
        if(index >= 0 && index < masterList.size()){
            Object commentToEdit = masterList.get(index);
            if(!(commentToEdit instanceof  Post)){
                ((VSComment)commentToEdit).setContent(text);
                ((VSComment)commentToEdit).setIsNew(true);
                masterList.set(index, commentToEdit);
                notifyItemChanged(index);
            }
        }
    }

    private void deleteComment(final int index){

        Runnable runnable = new Runnable() {
            public void run() {
                //sets author of the comment to "[deleted]"
                final VSComment commentToEdit = ((VSComment) masterList.get(index));

                HashMap<String, AttributeValue> keyMap = new HashMap<>();
                keyMap.put("i", new AttributeValue().withS(commentToEdit.getComment_id()));
                HashMap<String, AttributeValueUpdate> updates = new HashMap<>();

                AttributeValueUpdate newA = new AttributeValueUpdate()
                        .withValue(new AttributeValue().withS("[deleted]"))
                        .withAction(AttributeAction.PUT);
                updates.put("a", newA);

                AttributeValueUpdate newM = new AttributeValueUpdate()
                        .withValue(new AttributeValue().withN(Integer.toString(0)))
                        .withAction(AttributeAction.PUT);
                updates.put("m", newM);

                UpdateItemRequest request = new UpdateItemRequest()
                        .withTableName("vscomment")
                        .withKey(keyMap)
                        .withAttributeUpdates(updates);
                activity.getDDBClient().updateItem(request);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        commentToEdit.setAuthor("[deleted]");
                        masterList.set(index, commentToEdit);
                        notifyItemChanged(index);
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

    }

    public boolean overflowMenuIsOpen(){
        if(listPopupWindow == null){
            return false;
        }
        else{
            return listPopupWindow.isShowing();
        }
    }

    public void closeOverflowMenu(){
        if(listPopupWindow != null && listPopupWindow.isShowing()){
            listPopupWindow.dismiss();
        }
    }

    public void insertItem(VSComment newComment, int index){
        newComment.setIsNew(true);
        masterList.add(index, newComment);

        notifyItemInserted(index);
    }

    private void loadProfileImage(final String username) {

        //Log.d("IMGUPLOAD", postIDin + "-" + side + ".jpeg");

        AsyncTask<String, String, String> _Task = new AsyncTask<String, String, String>() {

            GlideUrlCustom imageURL;

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected String doInBackground(String... arg0)
            {
                //if (NetworkAvailablity.checkNetworkStatus(MyActivity.this))
                //{
                try {

                    int profileImgVersion;

                    if(username.equals(activity.getUsername())){
                        profileImgVersion = activity.getUserProfileImageVersion();
                    }
                    else{
                        profileImgVersion = activity.getProfileImageVersion(username);
                        if(profileImgVersion == -1){
                            profileImgVersion = getProfileImgVersion(username);
                            activity.addToCentralProfileImgVersionMap(username, profileImgVersion);
                        }
                    }

                    if(profileImgVersion == 0){
                        imageURL = null;
                    }
                    else{
                        imageURL = activity.getProfileImgUrl(username, profileImgVersion);
                    }

                } catch (Exception e) {
                    // writing error to Log
                    e.printStackTrace();
                }
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
                if(profileImageView != null){
                    if(imageURL != null){
                        Glide.with(activity.getProfileTab()).load(imageURL).into(profileImageView);
                    }
                    else{
                        Glide.with(activity.getProfileTab()).load(ContextCompat.getDrawable(activity, R.drawable.default_profile)).into(profileImageView);
                    }
                }
            }
        };
        _Task.execute((String[]) null);
    }

    private int getProfileImgVersion(String username){
        String host = activity.getESHost();
        String region = activity.getESRegion();
        String query = "/user/user_type/"+username;
        String url = "https://" + host + query;

        TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
        awsHeaders.put("host", host);

        AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                .regionName(region)
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
            return item.getInt("pi");

            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //if the ES GET fails, then return old topCardContent
        return 0;
    }

}