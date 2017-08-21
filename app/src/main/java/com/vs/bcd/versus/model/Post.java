package com.vs.bcd.versus.model;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "post")
public class Post extends PostSkeleton{

    public Post(){
        super();
    }

}