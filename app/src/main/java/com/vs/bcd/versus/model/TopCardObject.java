package com.vs.bcd.versus.model;

import com.vs.bcd.versus.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by dlee on 3/12/18.
 */

public class TopCardObject extends VSComment{

    public TopCardObject(VSComment vsComment){
        setParent_id(vsComment.getParent_id());
        setPost_id(vsComment.getPost_id());
        setTime(vsComment.getTime());
        setComment_id(vsComment.getComment_id());
        setAuthor(vsComment.getAuthor());
        setContent(vsComment.getContent());
        setTopmedal(vsComment.getTopmedal());
        setUpvotes(vsComment.getUpvotes());
        setDownvotes(vsComment.getDownvotes());
    }

}
