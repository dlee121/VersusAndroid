package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

/**
 * Created by dlee on 8/16/17.
 */

@DynamoDBTable(tableName = "active_post")
public class ActivePost extends PostSkeleton {

    public ActivePost(){
        super();
    }

    public ActivePost(Post post){
        setQuestion(post.getQuestion());
        setAuthor(post.getAuthor());
        setTime(post.getTime());
        setRedname(post.getRedname());
        setRedcount(post.getRedcount());
        setBlackname(post.getBlackname());
        setBlackcount(post.getBlackcount());
        setCategory(post.getCategory());
        setPost_id(post.getPost_id());
        setRedimg(post.getRedimg());
        setBlackimg(post.getBlackimg());
        setCommentcount(post.getCommentcount());
        setStl(post.getStl());
    }




}
