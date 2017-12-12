package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AmazonHttpClient;
import  com.amazonaws.http.ExecutionContext;

import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.http.HttpMethodName;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.ElasticRestClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dlee on 5/19/17.
 */



public class SearchPage extends Fragment {
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private static MainContainer activity;
    private Button searchButton;
    String url ="https://search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com/post/_search?size=10&pretty=true&q=*";

    private static final String SERVICE_NAME = "es";
    private static final String REGION = "us-east-1";
    private static final String HOST = "search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com/post/_search?size=10&pretty=true&q=*";
    private static final String ENDPOINT_ROOT = "https://" + HOST;
    private static final String PATH = "/";
    private static final String ENDPOINT = ENDPOINT_ROOT + PATH;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.search_page, container, false);

        searchButton = (Button) rootView.findViewById(R.id.searchbutton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchTest();
            }
        });


        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }
        disableChildViews();
        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainContainer)context;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d("VISIBLE", "SEARCH VISIBLE");
            if(rootView != null)
                enableChildViews();
        }
        else {
            Log.d("VISIBLE", "SEARCH POST GONE");
            if (rootView != null)
                disableChildViews();
        }
    }

    public void enableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));
        }
    }

    public void disableChildViews(){
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }

    public void searchTest() {

        Runnable runnable = new Runnable() {
            public void run() {

                // Generate the request
                Request<?> request = generateRequest();

                // Perform Signature Version 4 signing
                performSigningSteps(request);

                // Send the request to the server
                sendRequest(request);

                /*
                //Instantiate the request
                Request<Void> request = new DefaultRequest<Void>("es"); //Request to ElasticSearch
                request.setHttpMethod(HttpMethodName.GET);
                request.setEndpoint(URI.create("https://search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com/post/_search?size=10&pretty=true&q=*"));

                //Sign it...
                AWS4Signer signer = new AWS4Signer();
                signer.setRegionName("us-east-1");
                signer.setServiceName(request.getServiceName());
                signer.sign(request, activity.getCred());


                MyHttpResponseHandler<Void> responseHandler = new MyHttpResponseHandler<Void>();
                MyErrorHandler errorHandler = new MyErrorHandler();

                //Execute it and get the response...
                Response<Void> rsp = new AmazonHttpClient(new ClientConfiguration())
                        .execute(request, responseHandler, errorHandler, new ExecutionContext(true));
                */

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();


        /*

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url ="https://search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com/post/_search?size=10&pretty=true&q=*";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("SEARCHTEST", "Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("SEARCHTEST", "Error: "+ error.toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        */

        /*

        AWS4Signer aws4Signer = new AWS4Signer();
        aws4Signer.setServiceName("es");
        aws4Signer.setRegionName("us-east-1");
        aws4Signer.sign(awsHttpRequest, activity.getCred());

        String url = "https://search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com/post/_search?size=10&pretty=true&q=*";
        ElasticRestClient.get(url, activity.getCred(), new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                String str = new String(response, StandardCharsets.UTF_8);
                Log.d("SEARCHTEST", "Success:\n"+str);

            }

            @Override
            public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {

            }
        });
        */
    }


    public static class MyHttpResponseHandler<T> implements HttpResponseHandler<AmazonWebServiceResponse<T>> {

        @Override
        public AmazonWebServiceResponse<T> handle(
                com.amazonaws.http.HttpResponse response) throws Exception {

            InputStream responseStream = response.getContent();
            String responseString = responseStream.toString();
            Log.d("SEARCHTEST", responseString);

            AmazonWebServiceResponse<T> awsResponse = new AmazonWebServiceResponse<T>();
            return awsResponse;
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return false;
        }
    }

    public static class MyErrorHandler implements HttpResponseHandler<AmazonServiceException> {

        @Override
        public AmazonServiceException handle(
                com.amazonaws.http.HttpResponse response) throws Exception {
            System.out.println("In exception handler!");

            AmazonServiceException ase = new AmazonServiceException("Fake service exception.");
            ase.setStatusCode(response.getStatusCode());
            ase.setErrorCode(response.getStatusText());
            return ase;
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return false;
        }
    }

    /// Set up the request
    private static Request<?> generateRequest() {
        Request<?> request = new DefaultRequest<Void>(SERVICE_NAME);
        request.setContent(new ByteArrayInputStream("".getBytes()));
        request.setEndpoint(URI.create(ENDPOINT));
        request.setHttpMethod(HttpMethodName.GET);
        return request;
    }

    /// Perform Signature Version 4 signing
    private static void performSigningSteps(Request<?> requestToSign) {
        AWS4Signer signer = new AWS4Signer();
        signer.setServiceName(SERVICE_NAME);
        signer.setRegionName(REGION);

        // Get credentials
        // NOTE: *Never* hard-code credentials
        //       in source code

        AWSCredentials creds = activity.getCred();

        // Sign request with supplied creds
        signer.sign(requestToSign, creds);
    }

    /// Send the request to the server
    private static void sendRequest(Request<?> request) {
        ExecutionContext context = new ExecutionContext(true);

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        AmazonHttpClient client = new AmazonHttpClient(clientConfiguration);

        MyHttpResponseHandler<Void> responseHandler = new MyHttpResponseHandler<Void>();
        MyErrorHandler errorHandler = new MyErrorHandler();

        Response<Void> response =
                client.execute(request, responseHandler, errorHandler, context);
    }

    //TODO: also implement request cancelling where cancel() is called on the Request, in case user exists search before current search completes, so as to not trigger handler unnecessarily, although it may not matter and may actually work better that way to not cancel...think about that too, not cancelling.
}


