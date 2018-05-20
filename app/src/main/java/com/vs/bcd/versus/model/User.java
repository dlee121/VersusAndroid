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

    private String firstName, lastName, bday, username, password;
    private String email = "0"; //default value, since dynamodb doesn't want empty strings either email or phone may be unspecified by user
    private String phone = "0";
    private String mkey; //pw for messenger auth
    private int profileImage; //profile image storage url
    private int influence, g, s, b; //influece and medal count


    @DynamoDBAttribute(attributeName = "fn")
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @DynamoDBAttribute(attributeName = "ln")
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

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

    @DynamoDBAttribute(attributeName = "pw")
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    @DynamoDBAttribute(attributeName = "em")
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDBAttribute(attributeName = "ph")
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @DynamoDBAttribute(attributeName = "mk")
    public String getMkey(){
        return mkey;
    }
    public void setMkey(String mkey){
        this.mkey = mkey;
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

    public User(String input){

        String[] userData = input.split("/");
        firstName = userData[0];
        lastName = userData[1];
        bday = userData[2];
        username = userData[3];
        password = userData[4];
        mkey = UUID.randomUUID().toString().substring(0, 5);
        profileImage = 0; //default value meaning use a default in-app profile image
        influence = 0;
    }

}
