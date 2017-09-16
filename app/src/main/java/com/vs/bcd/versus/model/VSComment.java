package com.vs.bcd.versus.model;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.vs.bcd.versus.adapter.PostPageAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by dlee on 6/11/17.
 */

@DynamoDBTable(tableName = "vscomment")
public class VSComment {

    private String post_id; //UUID of the post this comment belongs to
    private String timestamp; //timestamp of when the comment was made
    private String comment_id; //UUID for this comment
    private String parent_id; //UUID of the parent comment that this comment was in response to (use 0 if root comment)
    private String author; //username of the comment's author
    private String content; //the actual content of the comment
    private int topmedal;   //0=none, 1=bronze, 2=silver, 3=gold
    private int upvotes; //number of upvotes for this comment
    private int downvotes; //number of downvotes for this comment
    private int votesum;    //upvotes - downvotes

    private int nestedLevel = 0;    //not used by DB.
    private int uservote = 0; //0 if NOVOTE, 1 if UPVOTE, 2 if DOWNVOTE
    private final int NOVOTE = 0;
    private final int UPVOTE = 1;
    private final int DOWNVOTE = 2;


    public VSComment(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        timestamp = df.format(new Date());
        comment_id = UUID.randomUUID().toString();
        upvotes = 0;
        downvotes = 0;
        uservote = 0;
        topmedal = 0;
        votesum = 0;
    }

    @DynamoDBHashKey(attributeName = "post_id")
    public String getPost_id() {
        return post_id;
    }
    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    @DynamoDBIndexRangeKey(localSecondaryIndexName = "post_id-timestamp-index", attributeName = "timestamp")
    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDBRangeKey(attributeName = "comment_id")
    public String getComment_id() {
        return comment_id;
    }
    public void setComment_id(String comment_id) {
        this.comment_id = comment_id;
    }

    @DynamoDBAttribute(attributeName = "parent_id")
    public String getParent_id() {
        return parent_id;
    }
    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    @DynamoDBAttribute(attributeName = "author")
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    @DynamoDBAttribute(attributeName = "content")
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    @DynamoDBAttribute(attributeName = "upvotes")
    public int getUpvotes() {
        return upvotes;
    }
    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    @DynamoDBAttribute(attributeName = "downvotes")
    public int getDownvotes() {
        return downvotes;
    }
    public void setDownvotes(int downvotes) {
        this.downvotes = downvotes;
    }

    @DynamoDBAttribute(attributeName = "topmedal")
    public int getTopmedal(){
        return topmedal;
    }
    public void setTopmedal(int topmedal){
        this.topmedal = topmedal;
    }

    @DynamoDBIndexRangeKey(localSecondaryIndexName = "post_id-votesum-index", attributeName = "votesum")
    public int getVotesum(){
        return votesum;
    }
    public void setVotesum(int votesum){
        this.votesum = votesum;
    }

    @DynamoDBIgnore
    public int getNestedLevel(){
        return nestedLevel;
    }
    public void setNestedLevel(int nestedLevel){
        this.nestedLevel = nestedLevel;
    }

    @DynamoDBIgnore
    public int getUservote(){
        return uservote;
    }
    public void setUservote(int uservote){  //use this only for when user clicks heart or broken heart. not for initial setting of uservote after download from DB
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
                break;

            case UPVOTE:
                upvotes ++;
                Log.d("uservote update", "upvote ++");
                if(this.uservote == DOWNVOTE){
                    downvotes--;
                    Log.d("uservote update", "downvote --");
                }
                break;

            case DOWNVOTE:
                downvotes ++;
                Log.d("uservote update", "downvote ++");
                if(this.uservote == UPVOTE){
                    upvotes--;
                    Log.d("uservote update", "upvote --");
                }
                break;

            default:
                break;
        }
        this.uservote = uservote;
    }
    public void initialSetUservote(int uservote){
        this.uservote = uservote;
    }


}