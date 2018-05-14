package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.text.ParseException;

@DynamoDBTable(tableName = "post")
public class Post {


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
    private int redimg; //0 = default (no image), 1 = s3 image
    private int blackimg;
    private int pt; //last modified time in minutes since epoch, used for popularity score calculation
    private BigDecimal ps; //popularity score
    private double popularityVelocity = 0;  //May move this to ActivePost if we don't plan on using this for Post as well.

    private NativeAppInstallAd NAI; //TODO: rename to AppInstallAd and rename relevant functions accordingly
    private NativeContentAd NC;     //TODO: rename to ContentAd and rename relevant functions accordingly


    /***
     * constants representing CATEGORIES
     */
    public static int AUTOMOBILES = 0;
    public static int CARTOON = 1;
    public static int CELEB = 2;
    public static int CULTURE = 3;
    public static int EDUCATION = 4;
    public static int ELECTRONICS = 5;
    public static int FASHION = 6;
    public static int FINANCE = 7;
    public static int FOOD = 8;
    public static int GAMES = 9;
    public static int MORALITY = 10;
    public static int MOVIES = 11;
    public static int MUSIC = 12;
    public static int POLITICS = 13;
    public static int RANDOM = 14;
    public static int RELIGION = 15;
    public static int SCIENCE = 16;
    public static int SEX = 17;
    public static int SOCIAL = 18;
    public static int SPORTS = 19;
    public static int TECHNOLOGY = 20;
    public static int WEAPONS = 21;

    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());


    @DynamoDBIgnore
    public String getCategoryString(){
        switch(category){
            case 0:
                return "Automobiles";
            case 1:
                return "Cartoon/Anime/Fiction";
            case 2:
                return "Celebrity/Gossip";
            case 3:
                return "Culture";
            case 4:
                return "Education";
            case 5:
                return "Electronics";
            case 6:
                return "Fashion";
            case 7:
                return "Finance";
            case 8:
                return "Food/Restaurant";
            case 9:
                return "Game/Entertainment";
            case 10:
                return "Morality/Ethics/Law";
            case 11:
                return "Movies/TV";
            case 12:
                return "Music/Artists";
            case 13:
                return "Politics";
            case 14:
                return "Random";
            case 15:
                return "Religion";
            case 16:
                return "Science";
            case 17:
                return "Sex/Porn";
            case 18:
                return "Social Issues";
            case 19:
                return "Sports";
            case 20:
                return "Technology";
            case 21:
                return "Weapons";
            case 42069:
                return "NATIVE APP INSTALL AD";
            case 69420:
                return "NATIVE CONTENT AD";
            default:
                return "N/A";
        }
    }

    @DynamoDBHashKey(attributeName = "i")
    public String getPost_id(){
        return  post_id;
    }
    public void setPost_id(String post_id){
        this.post_id = post_id;
    }

    @DynamoDBAttribute(attributeName = "q")
    public String getQuestion() {
        //TODO: since ddb doesn't hold empty strings, empty question will come as null (as in setTopic will not execute on those since the question column doesn't exist for those posts without question. So handle such cases when question is null return empty string
        //TODO: nah actualy question should be required. implement that through form validation
        return question;
    }
    public void setQuestion(String topic) {
        this.question = topic;
    }

    @DynamoDBAttribute(attributeName = "a")
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    @DynamoDBAttribute(attributeName="t")
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }

    @DynamoDBAttribute(attributeName = "rn")
    public String getRedname() {
        return redname;
    }
    public void setRedname(String redname) {
        this.redname = redname;
    }

    @DynamoDBAttribute(attributeName = "rc")
    public int getRedcount() {
        return redcount;
    }
    public void setRedcount(int redcount) {
        this.redcount = redcount;
    }

    @DynamoDBAttribute(attributeName = "bn")
    public String getBlackname() {
        return blackname;
    }
    public void setBlackname(String blackname) {
        this.blackname = blackname;
    }

    @DynamoDBAttribute(attributeName = "bc")
    public int getBlackcount() {
        return blackcount;
    }
    public void setBlackcount(int blackcount) {
        this.blackcount = blackcount;
    }

    @DynamoDBAttribute(attributeName = "c")
    public int getCategory() {
        return category;
    }
    public void setCategory(int category) {
        this.category = category;
    }

    @DynamoDBAttribute(attributeName = "ri")
    public int getRedimg(){
        return redimg;
    }
    public void setRedimg(int redimg){
        this.redimg = redimg;
    }

    @DynamoDBAttribute(attributeName = "bi")
    public int getBlackimg(){
        return blackimg;
    }
    public void setBlackimg(int blackimg){
        this.blackimg = blackimg;
    }

    @DynamoDBAttribute(attributeName = "pt")
    public int getPt(){
        return pt;
    }
    public void setPt(int pt){
        this.pt = pt;
    }

    @DynamoDBAttribute(attributeName = "ps")
    public BigDecimal getPs(){
        return ps;
    }
    public void setPs(BigDecimal ps){
        this.ps = ps;
    }


    @DynamoDBIgnore
    public String getCategoryIntAsString(){
        return Integer.toString(category);
    }


    public Post(){
        redcount = 0;
        blackcount = 0;
        pt = (int)((System.currentTimeMillis()/1000)/60); //this casting is safe because there won't be an overflow for at least 4000 years
        ps = BigDecimal.valueOf(0.0);
        time = df.format(new Date());
        post_id = UUID.randomUUID().toString(); //generate random UUID as post_id when post is created
    }


    public Post(JSONObject postObj, String id, boolean compact) throws JSONException{
        if(!compact){
            question = postObj.getString("q");
            author = postObj.getString("a");
            time = postObj.getString("t");
            redname = postObj.getString("rn");
            redcount = postObj.getInt("rc");
            blackname  = postObj.getString("bn");
            blackcount = postObj.getInt("bc");
            category = postObj.getInt("c");
            post_id = id;
            redimg = postObj.getInt("ri");
            blackimg = postObj.getInt("bi");
            pt = postObj.getInt("pt");
            ps = BigDecimal.valueOf(postObj.getDouble("ps"));
        }
        else{
            question = postObj.getString("q");
            time = postObj.getString("t");
            redname = postObj.getString("rn");
            redcount = postObj.getInt("rc");
            blackname  = postObj.getString("bn");
            blackcount = postObj.getInt("bc");
            author = postObj.getString("a");
            post_id = id;
        }
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
    public NativeAppInstallAd getNAI(){
        return NAI;
    }
    public void setNAI(NativeAppInstallAd NAI){
        this.NAI = NAI;
    }

    @DynamoDBIgnore
    public NativeContentAd getNC(){
        return NC;
    }
    public void setNC(NativeContentAd NC){
        this.NC = NC;
    }

    @DynamoDBIgnore
    public int getVotecount(){
        return redcount + blackcount;
    }

    public void copyPostInfo(Post src){
        redcount = src.getRedcount();
        blackcount = src.getBlackcount();
        category = src.getCategory();
        redimg = src.getRedimg();
        blackimg = src.getBlackimg();
        pt = src.getPt();
        ps = src.getPs();
    }

}