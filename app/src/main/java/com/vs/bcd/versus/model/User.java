package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Created by dlee on 5/29/17.
 */

@DynamoDBTable(tableName = "user")
public class User {

    private String firstName;
    private String lastName;
    private String bday;
    private String username; //partition key
    private String password;
    private String email = "n/a"; //default value, since dynamodb doesn't want empty strings either email or phone may be unspecified by user
    private String phone = "n/a";
    private int timecode;
    private int points;
    private int num_g;  //number of gold medals this user has
    private int num_s;  //number of silver medals this user has
    private int num_b;  //number of bronze medals this user has
    private List<String> fw; //list of username of users this user is following (following list)
    private int fc;   //number of followers this user has (follower count)
    private String mkey; //pw for messenger auth
    private String purl; //profile image storage url


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

    @DynamoDBAttribute(attributeName = "bday")
    public String getBday() {
        return bday;
    }
    public void setBday(String bday) {
        this.bday = bday;
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

    @DynamoDBAttribute(attributeName = "num_g")
    public int getNum_g(){
        return num_g;
    }
    public void setNum_g(int num_g){
        this.num_g = num_g;
    }

    @DynamoDBAttribute(attributeName = "num_s")
    public int getNum_s(){
        return num_s;
    }
    public void setNum_s(int num_s){
        this.num_s = num_s;
    }

    @DynamoDBAttribute(attributeName = "num_b")
    public int getNum_b(){
        return num_b;
    }
    public void setNum_b(int num_b){
        this.num_b = num_b;
    }

    @DynamoDBAttribute(attributeName = "fw")
    public List<String> getFw(){
        return fw;
    }
    public void setFw(List<String> fw){
        this.fw = fw;
    }

    @DynamoDBAttribute(attributeName = "fc")
    public int getFc(){
        return fc;
    }
    public void setFc(int fc){
        this.fc = fc;
    }

    @DynamoDBAttribute(attributeName = "mkey")
    public String getMkey(){
        return mkey;
    }
    public void setMkey(String mkey){
        this.mkey = mkey;
    }

    @DynamoDBAttribute(attributeName = "purl")
    public String getPurl() {
        return purl;
    }
    public void setPurl(String purl){
        this.purl = purl;
    }

    public User(){

    }

    public User(String input){

        String[] userData = input.split("/");
        firstName = userData[0];
        lastName = userData[1];
        bday = userData[2];
        username = userData[3];
        password = userData[4];
        timecode = (int)(System.currentTimeMillis()%10); //possible outputs: 0,1,2,3,4,5,6,7,8,9 based on current millisecond from epoch
        points = 0;
        num_g = 0;
        num_s = 0;
        num_b = 0;
        fw = new ArrayList<>();
        fc = 0;
        mkey = UUID.randomUUID().toString().substring(0, 5);
        purl = "0"; //default value meaning use a default in-app profile image

        //finish this thing, then do the write to db, then write session info to sharedpref and we're done with basic signup!
    }

}
