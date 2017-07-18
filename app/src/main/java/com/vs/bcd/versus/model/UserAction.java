package com.vs.bcd.versus.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;



/**
 * Created by dlee on 7/14/17.
 */

@DynamoDBTable(tableName = "useraction")
public class UserAction {

    private String userID;
    private String postID;
    private String votedSide = "none";
    private Map<String, String> actionRecord;    //Key = comment_id, Value = String value, N for novote, U for upvote, D for downvote.

    //Need explicit zero argument constructor for dynamodb
    public UserAction(){
        //nothing needed here
    }

    public UserAction(String userID, String postID){
        this.userID = userID;
        this.postID = postID;
        votedSide = "none";
        actionRecord = new HashMap<>();
    }

    @DynamoDBHashKey(attributeName = "user_id")
    public String getUserID() {
        return userID;
    }
    public void setUserID(String userID) {
        this.userID = userID;
    }

    @DynamoDBRangeKey(attributeName = "post_id")
    public String getPostID() {
        return postID;
    }
    public void setPostID(String postID) {
        this.postID = postID;
    }

    @DynamoDBAttribute(attributeName = "voted_side")
    public String getVotedSide() {
        return votedSide;
    }
    public void setVotedSide(String votedSide) {
        this.votedSide = votedSide;
    }

    @DynamoDBAttribute(attributeName = "action_record")
    public Map<String, String> getActionRecord() {
        return actionRecord;
    }
    public void setActionRecord(Map<String, String> actionRecord) {
        this.actionRecord = actionRecord;
    }



}
