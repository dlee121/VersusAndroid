package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.vs.bcd.api.model.AIModelHitsHitsItemSource;
import com.vs.bcd.api.model.UserGetModel;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by dlee on 5/29/17.
 */

@DynamoDBTable(tableName = "user")
public class User {

    private String bday, username;
    private String email = "0"; //default value, since dynamodb doesn't want empty strings either email or phone may be unspecified by user
    private String authID; //pw for messenger auth
    private int profileImage; //profile image storage url
    private int influence, g, s, b; //influece and medal count

    @DynamoDBAttribute(attributeName = "bd")
    public String getBday() {
        return bday;
    }
    public void setBday(String bday) {
        this.bday = bday;
    }

    @DynamoDBHashKey(attributeName = "i")
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDBAttribute(attributeName = "em")
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDBAttribute(attributeName = "ai")
    public String getAuthID(){
        return authID;
    }
    public void setAuthID(String authID){
        this.authID = authID;
    }

    @DynamoDBAttribute(attributeName = "pi")
    public int getProfileImage() {
        return profileImage;
    }
    public void setProfileImage(int profileImage){
        this.profileImage = profileImage;
    }

    @DynamoDBAttribute(attributeName = "in")
    public int getInfluence(){
        return influence;
    }
    public void setInfluence(int influence) {
        this.influence = influence;
    }

    @DynamoDBAttribute(attributeName = "g")
    public int getG(){
        return g;
    }

    public void setG(int g) {
        this.g = g;
    }

    @DynamoDBAttribute(attributeName = "s")
    public int getS() {
        return s;
    }

    public void setS(int s) {
        this.s = s;
    }

    @DynamoDBAttribute(attributeName = "b")
    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
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

    public User(JSONObject item, String username) throws JSONException {
        this.username = username;
        bday = item.getString("bd");
        email = item.getString("em");
        authID = item.getString("ai");
        profileImage = item.getInt("pi");
        influence = item.getInt("in");
        g = item.getInt("g");
        s = item.getInt("s");
        b = item.getInt("b");
    }

    public User(AIModelHitsHitsItemSource source, String id){
        username = id;
        bday = source.getBd();
        email = source.getEm();
        profileImage = source.getPi().intValue();
        authID = "";
        g = 0;
        s = 0;
        b = 0;
    }

    public User(UserGetModel source, String id){
        username = id;
        bday = source.getBd();
        email = source.getEm();
        profileImage = source.getPi().intValue();
        authID = "";
        g = 0;
        s = 0;
        b = 0;
    }


}
