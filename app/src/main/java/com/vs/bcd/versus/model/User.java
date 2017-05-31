package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.unmarshallers.IntegerSetUnmarshaller;

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


    public User(String input){
        String[] userData = input.split("%");
        firstName = userData[0];
        lastName = userData[1];
        birthday = userData[2];
        username = userData[3];
        password = userData[4];
        if(userData[5].indexOf('@') == -1){    //is phone number
            phone = userData[5];
        }
        else{
            email = userData[5];
        }

        //finish this thing, then do the write to db, then write session info to sharedpref and we're done with basic signup!
    }

}
