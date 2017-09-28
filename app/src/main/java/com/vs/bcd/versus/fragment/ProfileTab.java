package com.vs.bcd.versus.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.activity.MainContainer;
import com.vs.bcd.versus.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dlee on 4/29/17.
 */

public class ProfileTab extends Fragment {

    private MainContainer activity;
    private TextView usernameTV, goldTV, silverTV, bronzeTV, pointsTV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.profile, container, false);

        usernameTV = (TextView) rootView.findViewById(R.id.username_pt);
        goldTV = (TextView) rootView.findViewById(R.id.gmedal_pt);
        silverTV = (TextView) rootView.findViewById(R.id.smedal_pt);
        bronzeTV = (TextView) rootView.findViewById(R.id.bmedal_pt);
        pointsTV = (TextView) rootView.findViewById(R.id.points_pt);

        Log.d("ptab", "onCreateView() called");
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //save the activity to a member of this fragment
        activity = (MainContainer)context;
    }

    //for accessing another user's profile page
    public void setUpProfile(final String username, boolean myProfile){

        if(myProfile){
            //this is setting up the profile page for the logged-in user, as in "Me" page
            //disable toolbarButtonLeft
            //use projection attribute to reduce network traffic; get posts list and comments list from SharedPref
                //so only grab: num_g, num_s, num_b, points
            //TODO: hide all page content and show refresh animation during loading, no other UI element

            Log.d("ptab", "setting up my profile");

            Runnable runnable = new Runnable() {
                public void run() {

                    HashMap<String, AttributeValue> keyMap =
                            new HashMap<>();
                    keyMap.put("username", new AttributeValue().withS(username));  //partition key

                    GetItemRequest request = new GetItemRequest()
                            .withTableName("user")
                            .withKey(keyMap)
                            .withProjectionExpression("num_g,num_s,num_b,points");
                    GetItemResult result = activity.getDDBClient().getItem(request);

                    final Map<String, AttributeValue> resultMap = result.getItem();

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            usernameTV.setText(username);

                            for (Map.Entry<String, AttributeValue> entry : resultMap.entrySet()) {
                                Log.d("ptab", "attrName: " + entry.getKey() + "    attrValue: " + entry.getValue().getN());
                                String attrName = entry.getKey();
                                if(attrName.equals("num_g")){
                                    goldTV.setText(entry.getValue().getN());
                                }
                                else if(attrName.equals("num_s")){
                                    silverTV.setText(entry.getValue().getN());
                                }
                                else if(attrName.equals("num_b")){
                                    bronzeTV.setText(entry.getValue().getN());
                                }
                                else if(attrName.equals("points")){
                                    pointsTV.setText(entry.getValue().getN());
                                }
                            }

                        }
                    });
                    //TODO: enable UI elements to show the updated profile info and remove the refreshing animation
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();

        }
        else{
            //this is setting up the profile page for another user that the logged-in user clicked on
            //enable toolbarButtonLeft and set it to "x" or "<" and set it to go back to the page that user came from
            //use projection attribute to exclude private info.
                //so only grab: comments list, posts list, first name, last name, num_g, num_s, num_b, points
            Log.d("ptab", "setting up another user's profile");


            //TODO: hide all page content and show refresh animation during loading, no other UI element

            Log.d("ptab", "setting up my profile");

            Runnable runnable = new Runnable() {
                public void run() {

                    HashMap<String, AttributeValue> keyMap =
                            new HashMap<>();
                    keyMap.put("username", new AttributeValue().withS(username));  //partition key

                    GetItemRequest request = new GetItemRequest()
                            .withTableName("user")
                            .withKey(keyMap)
                            .withProjectionExpression("comments,posts,num_g,num_s,num_b,points");
                    GetItemResult result = activity.getDDBClient().getItem(request);

                    final Map<String, AttributeValue> resultMap = result.getItem();

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            usernameTV.setText(username);

                            for (Map.Entry<String, AttributeValue> entry : resultMap.entrySet()) {
                                Log.d("ptab", "attrName: " + entry.getKey() + "    attrValue: " + entry.getValue().getN());
                                String attrName = entry.getKey();
                                if(attrName.equals("num_g")){
                                    goldTV.setText(entry.getValue().getN());
                                }
                                else if(attrName.equals("num_s")){
                                    silverTV.setText(entry.getValue().getN());
                                }
                                else if(attrName.equals("num_b")){
                                    bronzeTV.setText(entry.getValue().getN());
                                }
                                else if(attrName.equals("points")){
                                    pointsTV.setText(entry.getValue().getN());
                                }
                            }

                        }
                    });
                    //TODO: enable UI elements to show the updated profile info and remove the refreshing animation
                }
            };
            Thread mythread = new Thread(runnable);
            mythread.start();






















        }



    }

}

