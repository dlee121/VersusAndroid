package com.vs.bcd.versus.model;

import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.text.SimpleDateFormat;


import cz.msebera.android.httpclient.Header;

public class ElasticRestClient {

    private static final String CLASS_NAME = ElasticRestClient.class.getSimpleName();

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, AWSCredentials awsCredentials, AsyncHttpResponseHandler responseHandler) {

        Date cDate = new Date();
        String fDate = new SimpleDateFormat("yyyyMMdd").format(cDate);
        String signature = "";

        String headerValue = "AWS4-HMAC-SHA256 "
                + "Credential=" + awsCredentials.getAWSAccessKeyId() + "/" + fDate + "/us-east-1/es/aws4_request, SignedHeaders=host;range;x-amz-date, "
                + "Signature=" + signature;

        client.addHeader("Authorization", headerValue);

        client.get(url, null, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }


    public void getHttpRequest() {
        try {


            ElasticRestClient.get("get", null, new JsonHttpResponseHandler() { // instead of 'get' use twitter/tweet/1
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // If the response is JSONObject instead of expected JSONArray
                    Log.i(CLASS_NAME, "onSuccess: " + response.toString());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    Log.i(CLASS_NAME, "onSuccess: " + response.toString());
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    Log.e(CLASS_NAME, "onFailure");
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                }

                @Override
                public void onRetry(int retryNo) {
                    Log.i(CLASS_NAME, "onRetry " + retryNo);
                    // called when request is retried
                }
            });
        }
        catch (Exception e){
            Log.e(CLASS_NAME, e.getLocalizedMessage());
        }
    }
}