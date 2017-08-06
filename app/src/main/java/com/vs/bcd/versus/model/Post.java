package com.vs.bcd.versus.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

@DynamoDBTable(tableName = "post")
public class Post implements Parcelable{


    //TODO: implement thumbnails
    //question, author, time, {thumbnail1, thumbnail2}*, viewcount, redname, redcount, blackname, blackcount, category
    private String question;
    private String author;
    private String time;
    //private int viewcount; instead of viewcount we'll do votecount, which is redcount + blackcount
    private String redname;
    private int redcount;
    private String blackname;
    private int blackcount;
    private int category;
    private String post_id;
    private String redimg; //"default", "s3", "in-app (like emojis. to be implemented later)"
    private String blackimg;

    /***
     * constants representing CATEGORIES
     */
    public static int CARS = 0;
    public static int CELEBRITIES = 1;
    public static int CULTURE = 2;
    public static int EDUCATION = 3;
    public static int ETHICS_AND_MORALITY = 4;
    public static int FASHION = 5;
    public static int FICTION = 6;
    public static int FOOD = 7;
    public static int GAME = 8;
    public static int LAW = 9;
    public static int MOVIES = 10;
    public static int MUSIC_AND_ARTISTS = 11;
    public static int POLITICS = 12;
    public static int PORN = 13;
    public static int RELIGION = 14;
    public static int RESTAURANTS = 15;
    public static int SCIENCE = 16;
    public static int SEX = 17;
    public static int SPORTS = 18;
    public static int TECHNOLOGY = 19;
    public static int TRAVEL = 20;
    public static int TV_SHOWS = 21;
    public static int WEAPONS = 22;

    @DynamoDBIgnore
    public String getCategoryString(){
        switch(category){
            case 0:
                return "Cars";
            case 1:
                return "Celebrities";
            case 2:
                return "Culture";
            case 3:
                return "Education";
            case 4:
                return "Ethics/Morality";
            case 5:
                return "Fashion";
            case 6:
                return "Fiction";
            case 7:
                return "Food";
            case 8:
                return "Game";
            case 9:
                return "Law";
            case 10:
                return "Movies";
            case 11:
                return "Music/Artists";
            case 12:
                return "Politics";
            case 13:
                return "Porn";
            case 14:
                return "Religion";
            case 15:
                return "Restaurants";
            case 16:
                return "Science";
            case 17:
                return "Sex";
            case 18:
                return "Sports";
            case 19:
                return "Technology";
            case 20:
                return "Travel";
            case 21:
                return "TV Shows";
            case 22:
                return "Weapons";
            default:
                return "N/A";
        }
    }


    @DynamoDBAttribute(attributeName = "question")
    public String getQuestion() {
        //TODO: since ddb doesn't hold empty strings, empty question will come as null (as in setTopic will not execute on those since the question column doesn't exist for those posts without question. So handle such cases when question is null return empty string
        //TODO: nah actualy question should be required. implement that through form validation
        return question;
    }
    public void setQuestion(String topic) {
        this.question = topic;
    }

    @DynamoDBAttribute(attributeName = "author")
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    @DynamoDBRangeKey(attributeName = "post_id")
    public String getPost_id(){
        return  post_id;
    }
    public void setPost_id(String post_id){
        this.post_id = post_id;
    }

    @DynamoDBAttribute(attributeName="time")
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }

    @DynamoDBAttribute(attributeName = "redname")
    public String getRedname() {
        return redname;
    }
    public void setRedname(String redname) {
        this.redname = redname;
    }

    @DynamoDBAttribute(attributeName = "redcount")
    public int getRedcount() {
        return redcount;
    }
    public void setRedcount(int redcount) {
        this.redcount = redcount;
    }

    @DynamoDBAttribute(attributeName = "blackname")
    public String getBlackname() {
        return blackname;
    }
    public void setBlackname(String blackname) {
        this.blackname = blackname;
    }

    @DynamoDBAttribute(attributeName = "blackcount")
    public int getBlackcount() {
        return blackcount;
    }
    public void setBlackcount(int blackcount) {
        this.blackcount = blackcount;
    }

    @DynamoDBHashKey(attributeName = "category")
    public int getCategory() {
        return category;
    }
    public void setCategory(int category) {
        this.category = category;
    }


    @DynamoDBAttribute(attributeName = "redimg")
    public String getRedimg(){
        return redimg;
    }
    public void setRedimg(String redimg){
        this.redimg = redimg;
    }

    @DynamoDBAttribute(attributeName = "blackimg")
    public String getBlackimg(){
        return blackimg;
    }
    public void setBlackimg(String blackimg){
        this.blackimg = blackimg;
    }

    @DynamoDBIgnore
    public String getCategoryIntAsString(){
        return Integer.toString(category);
    }


    public Post(){
        redcount = 0;
        blackcount = 0;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        time = df.format(new Date());
        post_id = UUID.randomUUID().toString(); //generate random UUID as post_id when post is created
    }

    public Post(Parcel in){
        question = in.readString();
        author = in.readString();
        time = in.readString();
        redname = in.readString();
        redcount = in.readInt();
        blackname = in.readString();
        blackcount = in.readInt();
        category = in.readInt();
        post_id = in.readString();
        redimg = in.readString();
        blackimg = in.readString();
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags){
        dest.writeString(question);
        dest.writeString(author);
        dest.writeString(time);
        dest.writeString(redname);
        dest.writeInt(redcount);
        dest.writeString(blackname);
        dest.writeInt(blackcount);
        dest.writeInt(category);
        dest.writeString(post_id);
        dest.writeString(redimg);
        dest.writeString(blackimg);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    public void incrementRedCount(){
        redcount++;
    }
    public void decrementRedCount(){
        redcount--;
    }
    public void incrementBlackCount(){
        blackcount++;
    }
    public void decrementBlackCount(){
        blackcount--;
    }

}