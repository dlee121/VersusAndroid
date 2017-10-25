package com.vs.bcd.versus.model;

import android.graphics.Bitmap;

import com.google.firebase.database.Exclude;

/**
 * Created by dlee on 10/22/17.
 */

public class UserSearchItem {

    private String username;
    private Bitmap profileImage;

    public UserSearchItem(){
        profileImage = null;
    }

    public UserSearchItem(String username){
        this.username = username;
    }

    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        this.username = username;
    }

    public Bitmap getProfileImage(){
        return profileImage;
    }
    public void setProfileImage(Bitmap profileImage){
        this.profileImage = profileImage;
    }

    public boolean equals(UserSearchItem usi){
        return this.username.equals(usi.getUsername());
    }

    public int hashCode(){
        return username.hashCode();
    }

}
