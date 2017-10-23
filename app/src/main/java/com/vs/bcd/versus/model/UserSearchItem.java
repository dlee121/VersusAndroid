package com.vs.bcd.versus.model;

import android.graphics.Bitmap;

/**
 * Created by dlee on 10/22/17.
 */

public class UserSearchItem {

    private String username;
    private Bitmap profilePhoto;

    public UserSearchItem(String username){
        this.username = username;
        //profilePhoto = TODO: get photo from S3, if none then use default photo
    }

    public String getUsername(){
        return username;
    }

    //TODO: getter and setter for photo



}
