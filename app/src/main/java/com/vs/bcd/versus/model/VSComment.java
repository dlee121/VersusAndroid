package com.vs.bcd.versus.model;

import android.util.Log;

import com.vs.bcd.api.model.CGCModelResponsesItemHitsHitsItemSource;
import com.vs.bcd.api.model.CommentModelSource;
import com.vs.bcd.api.model.CommentPutModel;
import com.vs.bcd.api.model.CommentsListModelHitsHitsItemSource;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by dlee on 6/11/17.
 */

public class VSComment {

    private String parent_id; //UUID of the parent comment that this comment was in response to (use 0 if root comment)
    private String post_id; //UUID of the post this comment belongs to
    private String time; //time of when the comment was made
    private String comment_id; //UUID for this comment
    private String author; //username of the comment's author
    private String content; //the actual content of the comment
    private int topmedal;   //0=none, 1=bronze, 2=silver, 3=gold
    private int upvotes; //number of upvotes for this comment
    private int downvotes; //number of downvotes for this comment
    private int comment_influence;
    private String root; //root comment id for grandchildren

    private int nestedLevel = 0;    //not used by DB.
    private int uservote = 0; //0 if NOVOTE, 1 if UPVOTE, 2 if DOWNVOTE
    private final int NOVOTE = 0;
    private final int UPVOTE = 1;
    private final int DOWNVOTE = 2;
    private int currentMedal = 0;   //current medal to display for UI
    private int child_count = 0;

    private boolean isNew = false;
    private boolean isHighlighted = false;

    private String r;
    private String b;


    public VSComment(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        time = df.format(new Date());
        comment_id = UUID.randomUUID().toString().replace("-","");
        upvotes = 0;
        downvotes = 0;
        uservote = 0;
        topmedal = 0;
        comment_influence = 0;
        root = "0";
    }

    public VSComment(CommentModelSource source, String id){
        parent_id = source.getPr();
        post_id = source.getPt();
        time = source.getT();
        comment_id = id;
        author = source.getA();
        content = source.getCt();
        topmedal = source.getM().intValue();
        upvotes = source.getU().intValue();
        downvotes = source.getD().intValue();
        comment_influence = source.getCi().intValue();
        root = source.getR();
    }

    public VSComment(CommentsListModelHitsHitsItemSource source, String id){
        parent_id = source.getPr();
        post_id = source.getPt();
        time = source.getT();
        comment_id = id;
        author = source.getA();
        content = source.getCt();
        topmedal = source.getM().intValue();
        upvotes = source.getU().intValue();
        downvotes = source.getD().intValue();
        comment_influence = source.getCi().intValue();
        root = source.getR();
    }

    public VSComment(CGCModelResponsesItemHitsHitsItemSource source, String id){
        parent_id = source.getPr();
        post_id = source.getPt();
        time = source.getT();
        comment_id = id;
        author = source.getA();
        content = source.getCt();
        topmedal = source.getM().intValue();
        upvotes = source.getU().intValue();
        downvotes = source.getD().intValue();
        comment_influence = source.getCi().intValue();
        root = source.getR();
    }

    public String getComment_id() {
        return comment_id;
    }
    public void setComment_id(String comment_id) {
        this.comment_id = comment_id;
    }

    public String getParent_id() {
        return parent_id;
    }
    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public String getPost_id() {
        return post_id;
    }
    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }

    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public int getUpvotes() {
        return upvotes;
    }
    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    public int getDownvotes() {
        return downvotes;
    }
    public void setDownvotes(int downvotes) {
        this.downvotes = downvotes;
    }

    public int getTopmedal(){
        return topmedal;
    }
    public void setTopmedal(int topmedal){
        this.topmedal = topmedal;
    }

    public int getComment_influence(){
        return comment_influence;
    }

    public String getRoot(){
        return root;
    }
    public void setRoot(String root) {
        this.root = root;
    }

    public int getCurrentMedal(){
        return currentMedal;
    }
    public void setCurrentMedal(int currentMedal){
        this.currentMedal = currentMedal;
    }

    public int getNestedLevel(){
        return nestedLevel;
    }
    public void setNestedLevel(int nestedLevel){
        this.nestedLevel = nestedLevel;
    }

    public String getR(){
        if(r == null){
            return "";
        }
        return r;
    }
    public VSComment setR(String r){
        this.r = r;
        return this;
    }

    public String getB(){
        if(b == null){
            return "";
        }
        return b;
    }
    public void setB(String b){
        this.b = b;
    }

    public boolean getIsNew(){
        return isNew;
    }
    public void setIsNew(boolean isNew){
        this.isNew = isNew;
    }

    public int getChild_count(){
        return child_count;
    }
    public void setChild_count(int child_count){
        this.child_count = child_count;
    }

    public boolean getIsHighlighted(){
        return isHighlighted;
    }
    public void setIsHighlighted(boolean isHighlighted){
        this.isHighlighted = isHighlighted;
    }

    public int getUservote(){
        return uservote;
    }
    public void setUservote(int uservote){  //use this only for when user clicks heart or broken heart. not for initial setting of uservote after download from DB
        Log.d("uservote update", "heartcount before change: " + Integer.toString(heartsTotal()));

        switch (uservote){
            case NOVOTE:
                if(this.uservote == UPVOTE){
                    upvotes--;
                    Log.d("uservote update", "upvote --");
                }
                else {  //automatically evaluates to downvote here because NOVOTE only gets set when current uservote is either UPVOTE or DOWNVOTE
                    downvotes--;
                    Log.d("uservote update", "downvote --");
                }
                Log.d("uservote update", "heartcount: " + Integer.toString(heartsTotal()));

                break;

            case UPVOTE:
                upvotes ++;
                Log.d("uservote update", "upvote ++");
                if(this.uservote == DOWNVOTE){
                    downvotes--;
                    Log.d("uservote update", "downvote --");
                }
                Log.d("uservote update", "heartcount: " + Integer.toString(heartsTotal()));

                break;

            case DOWNVOTE:
                downvotes ++;
                Log.d("uservote update", "downvote ++");
                if(this.uservote == UPVOTE){
                    upvotes--;
                    Log.d("uservote update", "upvote --");
                }
                Log.d("uservote update", "heartcount: " + Integer.toString(heartsTotal()));

                break;

            default:
                break;
        }
        this.uservote = uservote;
    }
    public void initialSetUservote(int uservote){
        this.uservote = uservote;
    }

    public void decrementAndSetN(int prevVote){

        switch (prevVote){
            case UPVOTE:
                upvotes--;
                break;

            case DOWNVOTE:
                downvotes--;
                break;
        }

        uservote = NOVOTE;

    }

    public void updateUservoteAndDecrement(int uservote){
        switch (uservote){
            case UPVOTE:
                this.uservote = uservote;
                upvotes++;
                if(downvotes > 0){
                    downvotes--;
                }
                break;

            case DOWNVOTE:
                this.uservote = uservote;
                downvotes++;
                if(upvotes > 0){
                    upvotes--;
                }
                break;

            //we don't use this for NOVOTE so ignore all other cases
        }
    }

    public void updateUservote(int uservote){
        switch (uservote){
            case UPVOTE:
                this.uservote = uservote;
                upvotes++;
                break;
            case DOWNVOTE:
                this.uservote = uservote;
                downvotes++;
                break;
        }
    }

    public int heartsTotal(){
        return upvotes - downvotes;
    }
    public int votesCount(){
        return upvotes + downvotes;
    }

    public CommentPutModel getPutModel(){
        CommentPutModel putModel = new CommentPutModel();

        putModel.setPr(parent_id);
        putModel.setPt(post_id);
        putModel.setT(time);
        putModel.setA(author);
        putModel.setCt(content);
        putModel.setM(BigDecimal.ZERO);
        putModel.setU(BigDecimal.ZERO);
        putModel.setD(BigDecimal.ZERO);
        putModel.setCi(BigDecimal.ZERO);
        putModel.setR(root);

        return putModel;
    }


}