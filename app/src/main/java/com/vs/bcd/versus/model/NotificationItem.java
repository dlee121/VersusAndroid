package com.vs.bcd.versus.model;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by dlee on 11/30/17.
 */

public class NotificationItem {

    private final int TYPE_U = 0; //new comment upvote notification
    private final int TYPE_C = 1; //new comment reply notification
    private final int TYPE_V = 2; //new post vote notification
    private final int TYPE_R = 3; //new post root comment notification
    private final int TYPE_F = 4; //new follower notification
    private final int TYPE_M = 5; //new medal notification

    private String body;
    private int type;
    private String payload;
    private long timestamp;
    private String medalType = "";
    private String identifier = "";
    private String key = "";

    public NotificationItem(String body, int type, String payload, long timestamp, String key){
        this.body = body;
        this.type = type;
        this.payload = payload;
        this.timestamp = timestamp;
        this.key = key;
    }

    public NotificationItem(String body, int type, long timestamp, String key){
        this.body = body;
        this.type = type;
        this.timestamp = timestamp;
        this.key = key;
    }

    public NotificationItem(String body, int type, String payload, long timestamp, String medalType, String key){
        this.body = body;
        this.type = type;
        this.payload = payload;
        this.timestamp = timestamp;
        this.medalType = medalType;
        this.key = key;
    }

    public void setMedalType(String medalType) {
        this.medalType = medalType;
    }

    public String getMedalType() {
        return medalType;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getBody(){
        return body;
    }

    public NotificationItem setBody(String body){
        this.body = body;
        return this;
    }

    public int getType(){
        return type;
    }

    public void setType(int type){
        this.type = type;
    }

    public void setPayload(String payload){
        this.payload = payload;
    }

    public String getPayload(){
        return payload;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

    public long getTimestamp(){
        return timestamp;
    }

    public String getTimeString(){

        if(timestamp == 0){ //for when we're not using timestamp we set it to 0, as is the case with medal notifications
            return "";
        }

        int timeFormat = 0;

        //TODO: test all possible cases to make sure date format conversion works correctly, for seconds, for all time format constants (secs, mins, ... , years), singulars / plurals
        long timediff = ((new Date()).getTime() - (timestamp * 1000)) / 1000;  //time elapsed since post creation, in seconds

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

    @Override
    public int hashCode() {
        switch (type){
            case TYPE_C:
                return (payload + Integer.toString(TYPE_C)).hashCode();
            case TYPE_F:
                return TYPE_F;
            case TYPE_M:
                return (payload + Integer.toString(TYPE_M)).hashCode();
            case TYPE_R:
                return (payload + Integer.toString(TYPE_R)).hashCode();
            case TYPE_U:
                return (payload + Integer.toString(TYPE_U)).hashCode();
            case TYPE_V:
                return (payload + Integer.toString(TYPE_V)).hashCode();
            default:
                return 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NotificationItem){
            switch (type){
                case TYPE_C:
                    return (payload + Integer.toString(TYPE_C)).hashCode() == obj.hashCode();
                case TYPE_F:
                    return TYPE_F == obj.hashCode();
                case TYPE_M:
                    return (payload + Integer.toString(TYPE_M)).hashCode() == obj.hashCode();
                case TYPE_R:
                    return (payload + Integer.toString(TYPE_R)).hashCode() == obj.hashCode();
                case TYPE_U:
                    return (payload + Integer.toString(TYPE_U)).hashCode() == obj.hashCode();
                case TYPE_V:
                    return (payload + Integer.toString(TYPE_V)).hashCode() == obj.hashCode();
                default:
                    return body.equals(((NotificationItem)obj).getBody());
            }
        }
        return super.equals(obj);
    }
}
