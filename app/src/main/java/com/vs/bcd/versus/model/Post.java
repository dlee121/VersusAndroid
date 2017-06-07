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
    private String question;   // question can be left empty.
    private String author;
    private String time;
    private int viewcount;
    private String redname;
    private int redcount;
    private String blackname;
    private int blackcount;
    private String category; //for now let's do politics, sports (then sub categories / tags could be basket ball, boxing, ufc, soccer, etc), food, anime / comics
    private String post_id;

    @DynamoDBAttribute(attributeName = "question")
    public String getQuestion() {
        //TODO: since ddb doesn't hold empty strings, empty question will come as null (as in setQuestion will not execute on those since the question column doesn't exist for those posts without question. So handle such cases when question is null return empty string
        return question;
    }
    public void setQuestion(String question) {
        this.question = question;
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

    @DynamoDBAttribute(attributeName = "viewcount")
    public int getViewcount() {
        return viewcount;
    }
    public void setViewcount(int viewcount) {
        this.viewcount = viewcount;
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
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }



    //zero argument constructor. necessary here since implementing packet.
    public Post(){
        redcount = 0;
        blackcount = 0;
        viewcount = 1;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        time = df.format(new Date());
        post_id = UUID.randomUUID().toString(); //generate random UUID as post_id when post is created
    }

    public Post(Parcel in){
        question = in.readString();
        author = in.readString();
        time = in.readString();
        viewcount = in.readInt();
        redname = in.readString();
        redcount = in.readInt();
        blackname = in.readString();
        blackcount = in.readInt();
        category = in.readString();
        post_id = in.readString();
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
        dest.writeInt(viewcount);
        dest.writeString(redname);
        dest.writeInt(redcount);
        dest.writeString(blackname);
        dest.writeInt(blackcount);
        dest.writeString(category);
        dest.writeString(post_id);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

}