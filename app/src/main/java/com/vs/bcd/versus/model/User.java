package com.vs.bcd.versus.model;

import com.vs.bcd.api.model.AIModelHitsHitsItemSource;
import com.vs.bcd.api.model.UserGetModel;


/**
 * Created by dlee on 5/29/17.
 */

public class User {

    private String bday, username;
    private String email = "0"; //default value, since dynamodb doesn't want empty strings either email or phone may be unspecified by user
    private String authID; //pw for messenger auth
    private int profileImage; //profile image storage url
    private int influence, g, s, b; //influece and medal count

    public String getBday() {
        return bday;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getAuthID(){
        return authID;
    }

    public int getProfileImage() {
        return profileImage;
    }

    public User(){

    }

    public User(String bday, String username){

        this.bday = bday;
        this.username = username;
        authID = "0"; //TODO: once we've moved on from ddb usage, this can be blank string (make sure ES can handle empty strings), but 0 for now since ddb doesn't accept empty string
        profileImage = 0; //default value meaning use a default in-app profile image
        influence = 0;
        g = 0;
        s = 0;
        b = 0;
    }

    //for signing up user from facebook login and google login
    public User(String bday, String username, String authID){
        this.bday = bday;
        this.username = username;
        this.authID = authID;
        profileImage = 0; //default value meaning use a default in-app profile image
        influence = 0;
        g = 0;
        s = 0;
        b = 0;
    }

    public User(AIModelHitsHitsItemSource source){
        username = source.getCs();
        bday = source.getBd();
        email = source.getEm();
        profileImage = source.getPi().intValue();
        authID = "";
        g = 0;
        s = 0;
        b = 0;
    }

    public User(UserGetModel source){
        username = source.getCs();
        bday = source.getBd();
        email = source.getEm();
        profileImage = source.getPi().intValue();
        authID = "";
        g = 0;
        s = 0;
        b = 0;
    }

}
