package com.vs.bcd.versus.model;

import android.graphics.Bitmap;

import com.google.firebase.database.Exclude;

/**
 * Created by dlee on 10/22/17.
 */

public class UserSearchItem {

    private String username;
    private int usernameHash;
    private Bitmap profileImage;


    public UserSearchItem(String username){
        this.username = username;
        if(username.length() < 5){
            usernameHash = username.hashCode();
        }
        else{
            String hashIn = "" + username.charAt(0) + username.charAt(username.length() - 2) + username.charAt(1) + username.charAt(username.length() - 1);
            usernameHash = hashIn.hashCode();
        }
    }

    public String getUsername(){
        return username;
    }
    public void setUsername(String username){
        this.username = username;
    }

    public int getHash(){
        return usernameHash;
    }
    public void setHash(int usernameHash){
        this.usernameHash = usernameHash;
    }

    public Bitmap getProfileImage(){
        return profileImage;
    }
    public void setProfileImage(Bitmap profileImage){
        this.profileImage = profileImage;
    }

    @Override
    public boolean equals(Object usi){
        return this.username.equals(((UserSearchItem)usi).getUsername());
    }

    @Override
    public int hashCode(){
        return username.hashCode();
    }

}
