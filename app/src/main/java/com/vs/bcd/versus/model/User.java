package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by dlee on 5/29/17.
 */

@DynamoDBTable(tableName = "user")
public class User {

    private String firstName;
    private String lastName;
    private String birthday;
    private String username; //partition key
    private String password;
    private String email = "n/a"; //default value, since dynamodb doesn't want empty strings either email or phone may be unspecified by user
    private String phone = "n/a";
    private int timecode;
    private Map<String, String> medalmap;   //<post_id -> g,s,b for gold,silver,bronze
    private int points;
    private List<String> posts; //holds post_id UUIDs of posts created by this user
    private List<String> comments; //holds comment_id UUIDs of comments created by this user


    @DynamoDBAttribute(attributeName = "firstname")
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @DynamoDBAttribute(attributeName = "lastname")
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @DynamoDBAttribute(attributeName = "birthday")
    public String getBirthday() {
        return birthday;
    }
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    @DynamoDBHashKey(attributeName = "username")
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    @DynamoDBAttribute(attributeName = "password")
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    @DynamoDBAttribute(attributeName = "email")
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDBAttribute(attributeName = "phone")
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @DynamoDBIndexRangeKey(globalSecondaryIndexName = "timecode-points-index", attributeName = "points")
    public int getPoints(){
        return points;
    }
    public void setPoints(int points){
        this.points = points;
    }

    @DynamoDBIndexHashKey(globalSecondaryIndexName = "timecode-points-index", attributeName = "timecode")
    public int getTimecode(){
        return timecode;
    }
    public void setTimecode(int timecode){
        this.timecode = timecode;
    }

    @DynamoDBAttribute(attributeName = "medalmap")
    public Map<String, String> getMedalmap(){
        return medalmap;
    }
    public void setMedalmap(Map<String, String> medalmap){
        this.medalmap = medalmap;
    }

    @DynamoDBAttribute(attributeName = "posts")
    public List<String> getPosts(){
        return posts;
    }
    public void setPosts(List<String> posts){
        this.posts = posts;
    }

    @DynamoDBAttribute(attributeName = "comments")
    public List<String> getComments(){
        return comments;
    }
    public void setComments(List<String> comments){
        this.comments = comments;
    }

    public User(){

    }

    public User(String input){

        String[] userData = input.split("%");
        firstName = userData[0];
        lastName = userData[1];
        birthday = userData[2];
        username = userData[3];
        password = userData[4];
        timecode = (int)(System.currentTimeMillis()%10); //possible outputs: 0,1,2,3,4,5,6,7,8,9 based on current millisecond from epoch
        points = 0;
        medalmap = new HashMap<>();
        posts = new ArrayList<>();
        comments = new ArrayList<>();

        //finish this thing, then do the write to db, then write session info to sharedpref and we're done with basic signup!
    }

}
