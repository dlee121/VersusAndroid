package com.vs.bcd.versus.model;

/**
 * Created by dlee on 2/21/18.
 */

public class CEFObject {
    private int cefObjectType; //0 = post, 1 = comment, 2 = text input box
    private String question, rname, bname;
    private String username, content;

    public CEFObject(Post post){
        cefObjectType = 0;
        question = post.getQuestion();
        rname = post.getRedname();
        bname = post.getBlackname();
    }

    public CEFObject(VSComment comment){
        cefObjectType = 1;
        username = comment.getAuthor();
        content = comment.getContent();
    }

    public CEFObject(){
        cefObjectType = 2;
    }

    public int getCefObjectType(){
        return cefObjectType;
    }
    public String getQuestion(){
        return question;
    }

    public String getRname() {
        return rname;
    }

    public String getBname() {
        return bname;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }
}