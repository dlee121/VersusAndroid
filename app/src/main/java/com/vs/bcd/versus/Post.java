package com.vs.bcd.versus;

public class Post {


    //TODO: implement thumbnails
    //post_id, question, author, time, {thumbnail1, thumbnail2}*, viewcount, redname, redcount, blackname, blackcount
    private int post_id;
    private String question;
    private String author;
    private String time;
    private int viewcount;
    private String redname;
    private int redcount;
    private String blackname;
    private int blackcount;
    private String category; //for now let's do politics, sports (then sub categories / tags could be basket ball, boxing, ufc, soccer, etc), food, anime / comics

    //getters
    public int getPostID() {
        return post_id;
    }

    public String getQuestion() {
        return question;
    }

    public String getAuthor() {
        return author;
    }

    public String getTime() {
        return time;
    }

    public int getViewcount() {
        return viewcount;
    }

    public String getRedname() {
        return redname;
    }

    public int getRedcount() {
        return redcount;
    }

    public String getBlackname() {
        return blackname;
    }

    public int getBlackcount() {
        return blackcount;
    }

    public String getCategory() {
        return category;
    }

    //setters
    public void setPostID(int post_id) {
        this.post_id = post_id;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setViewcount(int viewcount) {
        this.viewcount = viewcount;
    }

    public void setRedname(String redname) {
        this.redname = redname;
    }

    public void setRedcount(int redcount) {
        this.redcount = redcount;
    }

    public void setBlackname(String blackname) {
        this.blackname = blackname;
    }

    public void setBlackcount(int blackcount) {
        this.blackcount = blackcount;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}