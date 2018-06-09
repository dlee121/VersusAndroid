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

    private String i; //username+postID, and since we know the username length we can extract username and postID from i
    private String votedSide = "none";  //"none", "RED", "BLK".
    private Map<String, String> actionRecord;    //Key = comment_id, Value = String value, N for novote, U for upvote, D for downvote.

    //Need explicit zero argument constructor for dynamodb
    public UserAction(){
        //nothing needed here
    }

    public UserAction(String userID, String postID){
        i = userID+postID;
        votedSide = "none";
        actionRecord = new HashMap<>();
    }

    @DynamoDBHashKey(attributeName = "i")
    public String getI() {
        return i;
    }
    public void setI(String i) {
        this.i = i;
    }

    @DynamoDBIgnore
    public String getPostID(int usernameLegnth) {
        return i.substring(usernameLegnth, i.length());
    }

    @DynamoDBAttribute(attributeName = "vs")
    public String getVotedSide() {
        return votedSide;
    }
    public void setVotedSide(String votedSide) {
        this.votedSide = votedSide;
    }

    @DynamoDBAttribute(attributeName = "ar")
    public Map<String, String> getActionRecord() {
        return actionRecord;
    }
    public void setActionRecord(Map<String, String> actionRecord) {
        this.actionRecord = actionRecord;
    }

}
