package com.vs.bcd.versus.model;

import com.vs.bcd.api.model.CommentsListModelHitsHitsItemSource;

public class NewsfeedItem {
    private String parent_id; //UUID of the parent comment that this comment was in response to (use 0 if root comment)
    private String post_id; //UUID of the post this comment belongs to
    private String time; //time of when the comment was made
    private String comment_id; //UUID for this comment
    private String author; //username of the comment's author
    private String content; //the actual content of the comment
    private int topmedal;   //0=none, 1=bronze, 2=silver, 3=gold
    private int upvotes; //number of upvotes for this comment
    private int downvotes; //number of downvotes for this comment
    private int comment_influence;
    private String root; //root comment id for grandchildren


    public NewsfeedItem(CommentsListModelHitsHitsItemSource source, String id){
        parent_id = source.getPr();
        post_id = source.getPt();
        time = source.getT();
        comment_id = id;
        author = source.getA();
        content = source.getCt();
        topmedal = source.getM().intValue();
        upvotes = source.getU().intValue();
        downvotes = source.getD().intValue();
        comment_influence = source.getCi().intValue();
        root = source.getR();
    }


}
