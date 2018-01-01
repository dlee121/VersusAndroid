package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.ExecutionContext;

import com.amazonaws.http.HttpMethodName;
import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.AWSV4Auth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;


/**
 * Created by dlee on 5/19/17.
 */



public class SearchPage extends Fragment {
    private View rootView;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private static MainContainer activity;
    private Button searchButton;
    private EditText searchET;

    private static final String SERVICE_NAME = "es";
    private static final String REGION = "us-east-1";
    private static final String HOST = "search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com/post/_search?size=10&pretty=true&q=*";
    private static final String ENDPOINT_ROOT = "https://" + HOST;
    private static final String PATH = "/";
    private static final String ENDPOINT = ENDPOINT_ROOT + PATH;

    // replace following 4 String with real values to make it work
    // following 3 are fake values, so it wont run as it is...
    static String host = "search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com";
    static String accessKey = "";
    static String secretKey = "";
    static String region = "us-east-1";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.search_page, container, false);

        searchET = (EditText) rootView.findViewById(R.id.search_et);

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
                /*
                if(accessKey == null || accessKey.equals("")){
                    accessKey = activity.getCred().getAWSAccessKeyId();
                }
                if(secretKey == null || secretKey.equals("")){
                    secretKey = activity.getCred().getAWSSecretKey();
                }
                */
                //TODO: get accesskey and secretkey

                String query = "/_search";
                String searchInput = searchET.getText().toString();
                if(searchInput == null){
                    return;
                }
                String payload = "{\"query\":{\"multi_match\":{\"query\": \"" + searchInput +
                        "\",\"fields\": [\"redname\", \"blackname\", \"question\"],\"type\": \"most_fields\"}}}";

                String url = "https://" + host + query;

                TreeMap<String, String> awsHeaders = new TreeMap<String, String>();
                awsHeaders.put("host", host);

                AWSV4Auth aWSV4Auth = new AWSV4Auth.Builder("AKIAIYIOPLD3IUQY2U5A", "DFs84zylbBPjR/JrJcLBatXviJm26P6r/IJc6EOE")
                        .regionName(region)
                        .serviceName("es") // es - elastic search. use your service name
                        .httpMethodName("POST") //GET, PUT, POST, DELETE, etc...
                        .canonicalURI(query) //end point
                        .queryParametes(null) //query parameters if any
                        .awsHeaders(awsHeaders) //aws header parameters
                        .payload(payload) // payload if any
                        .debug() // turn on the debug mode
                        .build();

                HttpPost httpPost = new HttpPost(url);
                StringEntity requestEntity = new StringEntity(payload, ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);

		        /* Get header calculated for request */
                Map<String, String> header = aWSV4Auth.getHeaders();
                for (Map.Entry<String, String> entrySet : header.entrySet()) {
                    String key = entrySet.getKey();
                    String value = entrySet.getValue();

			    /* Attach header in your request */
			    /* Simple get request */

                    httpPost.addHeader(key, value);
                }
                httpPostRequest(httpPost);

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void httpPostRequest(HttpPost httpPost) {
		/* Create object of CloseableHttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();

		/* Response handler for after request execution */
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				/* Get status code */
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
					/* Convert response to String */
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        try {
			/* Execute URL and attach after execution response handler */
            String strResponse = httpClient.execute(httpPost, responseHandler);

            JSONObject obj = new JSONObject(strResponse);
            JSONArray hits = obj.getJSONObject("hits").getJSONArray("hits");
            for(int i = 0; i < hits.length(); i++){
                JSONObject item = hits.getJSONObject(i).getJSONObject("_source");
                Log.d("SEARCHRESULTS", "R: " + item.getString("redname") + ", B: " + item.getString("blackname") + ", Q: " + item.getString("question"));
                //TODO: display search results. If zero results then display empty results page. Items should be clickable, but we may want to use a new adapter, differentiating search view from MainActivity views, mainly that searchview should be more concise to display more search results in one page. Or should it be same as MainActivity's way of displaying posts list?
            }
            //System.out.println("Response: " + strResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //TODO: also implement request cancelling where cancel() is called on the Request, in case user exists search before current search completes, so as to not trigger handler unnecessarily, although it may not matter and may actually work better that way to not cancel...think about that too, not cancelling.
}

