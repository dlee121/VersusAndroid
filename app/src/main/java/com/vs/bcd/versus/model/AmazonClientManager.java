package com.vs.bcd.versus.model;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
* Created by chelium on 6/6/17.
*/
public class AmazonClientManager {

    private static final String LOG_TAG = "AmazonClientManager";
    private static final String IDENTITY_POOL_ID = "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff";

    private AmazonDynamoDBClient ddb = null;
    private Context context;

    public AmazonClientManager(Context context) {
        this.context = context;
    }

    public AmazonDynamoDBClient ddb() {
        validateCredentials();
        return ddb;
    }

    public void validateCredentials() {
        if (ddb == null) {
            initClients();
        }
    }

    private void initClients() {
        // sessionManager = new AmazonClientManager(this);
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                IDENTITY_POOL_ID, // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
    }
}