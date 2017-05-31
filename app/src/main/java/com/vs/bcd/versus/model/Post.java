package com.vs.bcd.versus.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

@DynamoDBTable(tableName = "post")
public class Post implements Parcelable{


    //TODO: implement thumbnails
    //post_id, question, author, time, {thumbnail1, thumbnail2}*, viewcount, redname, redcount, blackname, blackcount, category
    private int post_id;
    private String question = "";   // question can be left empty.
    private String author;
    private String time;
    private int viewcount;
    private String redname;
    private int redcount;
    private String blackname;
    private int blackcount;
    private String category; //for now let's do politics, sports (then sub categories / tags could be basket ball, boxing, ufc, soccer, etc), food, anime / comics

    //getters
    @DynamoDBAttribute(attributeName = "post_id")
    public int getPostID() {
        return post_id;
    }
    public void setPostID(int post_id) {
        this.post_id = post_id;
    }

    @DynamoDBAttribute(attributeName = "question")
    public String getQuestion() {
        return question;
    }
    public void setQuestion(String question) {
        this.question = question;
    }

    @DynamoDBIndexRangeKey(attributeName = "author")
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    @DynamoDBRangeKey(attributeName="time")
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



    //zero argument constructor. necessary here since non-zero argument constructor has been defined.
    public Post(){

    }

    public Post(Parcel in){
        post_id = in.readInt();
        question = in.readString();
        author = in.readString();
        time = in.readString();
        viewcount = in.readInt();
        redname = in.readString();
        redcount = in.readInt();
        blackname = in.readString();
        blackcount = in.readInt();
        category = in.readString();
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags){
        dest.writeInt(post_id);
        dest.writeString(question);
        dest.writeString(author);
        dest.writeString(time);
        dest.writeInt(viewcount);
        dest.writeString(redname);
        dest.writeInt(redcount);
        dest.writeString(blackname);
        dest.writeInt(blackcount);
        dest.writeString(category);
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