package com.vs.bcd.versus.model;

import com.vs.bcd.api.model.RecordPutModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dlee on 7/14/17.
 */

public class UserAction {

    private String i; //username+postID, and since we know the username length we can extract username and postID from i
    private String votedSide = "none";  //"none", "RED", "BLK".
    private Map<String, String> actionRecord;    //Key = comment_id, Value = String value, N for novote, U for upvote, D for downvote.

    public UserAction(){
        //nothing needed here
    }

    public UserAction(String userID, String postID){
        i = userID+postID;
        votedSide = "none";
        actionRecord = new HashMap<>();
    }

    public String getI() {
        return i;
    }
    public void setI(String i) {
        this.i = i;
    }

    public String getPostID(int usernameLegnth) {
        return i.substring(usernameLegnth, i.length());
    }

    public String getVotedSide() {
        return votedSide;
    }
    public void setVotedSide(String votedSide) {
        this.votedSide = votedSide;
    }

    public Map<String, String> getActionRecord() {
        return actionRecord;
    }
    public void setActionRecord(Map<String, String> actionRecord) {
        this.actionRecord = actionRecord;
    }

    public RecordPutModel getRecordPutModel(){
        RecordPutModel recordPutModel = new RecordPutModel();

        List<String> DList = new ArrayList<>();
        List<String> NList = new ArrayList<>();
        List<String> UList = new ArrayList<>();

        for(Map.Entry<String, String> entry : actionRecord.entrySet()){
            switch (entry.getValue()){
                case "U":
                    UList.add(entry.getKey());
                    break;
                case "D":
                    DList.add(entry.getKey());
                    break;
                case "N":
                    NList.add(entry.getKey());
                    break;
            }

        }

        recordPutModel.setD(DList);
        recordPutModel.setU(UList);
        recordPutModel.setN(NList);
        recordPutModel.setV(votedSide);

        return recordPutModel;

    }

    public UserAction(RecordPutModel recordPutModel, String i){
        this.i = i;
        votedSide = recordPutModel.getV();
        actionRecord = new HashMap<>();
        for(String commentID : recordPutModel.getD()){
            actionRecord.put(commentID, "D");
        }
        for(String commentID : recordPutModel.getN()){
            actionRecord.put(commentID, "N");
        }
        for(String commentID : recordPutModel.getU()){
            actionRecord.put(commentID, "U");
        }
    }

}
