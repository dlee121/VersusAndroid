package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.HashMap;
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

    @DynamoDBAttribute(attributeName = "points")
    public int getPoints(){
        return points;
    }
    public void setPoints(int points){
        this.points = points;
    }

    @DynamoDBAttribute(attributeName = "timecode")
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

    public User(){

    }

    public User(String input){

        String[] userData = input.split("%");
        firstName = userData[0];
        lastName = userData[1];
        birthday = userData[2];
        username = userData[3];
        password = userData[4];
        timecode = (int)( ( (System.currentTimeMillis()%10) *2 )%10 ); //possible outputs: 0,2,4,6,8, based on current millisecond from epoch
        points = 0;
        medalmap = new HashMap<>();

        //finish this thing, then do the write to db, then write session info to sharedpref and we're done with basic signup!
    }

}
