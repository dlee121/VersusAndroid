package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.text.ParseException;

@DynamoDBTable(tableName = "post")
public class PostSkeleton {


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
    private int votecount;
    private int commentcount;
    private long stl;
    private double popularityVelocity = 0;  //May move this to ActivePost if we don't plan on using this for Post as well.


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
    public static int FINANCE = 7;
    public static int FOOD = 8;
    public static int GAME = 9;
    public static int LAW = 10;
    public static int MOVIES = 11;
    public static int MUSIC_AND_ARTISTS = 12;
    public static int POLITICS = 13;
    public static int PORN = 14;
    public static int RELIGION = 15;
    public static int RESTAURANTS = 16;
    public static int SCIENCE = 17;
    public static int SEX = 18;
    public static int SPORTS = 19;
    public static int TECHNOLOGY = 20;
    public static int TRAVEL = 21;
    public static int TV_SHOWS = 22;
    public static int WEAPONS = 23;

    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());


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
                return "Finanace";
            case 8:
                return "Food";
            case 9:
                return "Game";
            case 10:
                return "Law";
            case 11:
                return "Movies";
            case 12:
                return "Music/Artists";
            case 13:
                return "Politics";
            case 14:
                return "Porn";
            case 15:
                return "Religion";
            case 16:
                return "Restaurants";
            case 17:
                return "Science";
            case 18:
                return "Sex";
            case 19:
                return "Sports";
            case 20:
                return "Technology";
            case 21:
                return "Travel";
            case 22:
                return "TV Shows";
            case 23:
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

    @DynamoDBIndexHashKey(globalSecondaryIndexNames = {"author-votecount-index", "author-time-index"}, attributeName = "author")
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

    @DynamoDBIndexRangeKey(localSecondaryIndexName = "category-time-index", globalSecondaryIndexNames = "author-time-index", attributeName="time")
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

    @DynamoDBAttribute(attributeName = "commentcount")
    public int getCommentcount(){
        return commentcount;
    }
    public void setCommentcount(int commentcount){
        this.commentcount = commentcount;
    }

    @DynamoDBIndexRangeKey(localSecondaryIndexName = "category-votecount-index", globalSecondaryIndexName = "author-votecount-index", attributeName = "votecount")
    public int getVotecount(){
        return votecount;
    }
    public void setVotecount(int votecount){
        this.votecount = votecount;
    }

    @DynamoDBAttribute(attributeName = "stl")
    public long getStl(){
        return stl;
    }
    public void setStl(long stl){
        this.stl = stl;
    }

    @DynamoDBIgnore
    public String getCategoryIntAsString(){
        return Integer.toString(category);
    }


    public PostSkeleton(){
        redcount = 0;
        blackcount = 0;
        votecount = 0;
        commentcount = 0;
        time = df.format(new Date());
        post_id = UUID.randomUUID().toString(); //generate random UUID as post_id when post is created
        stl = System.currentTimeMillis()/1000 + 1814400; //gets current time since epoch in seconds and adds 21 days (1814400 seconds)
    }

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

    @DynamoDBIgnore
    public Date getDate(){
        try{
            return df.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @DynamoDBIgnore
    public double getPopularityVelocity(){
        return  popularityVelocity;
    }
    public void setPopularityVelocity(double popularityVelocity){
        this.popularityVelocity = 1000 * popularityVelocity;
    }

}