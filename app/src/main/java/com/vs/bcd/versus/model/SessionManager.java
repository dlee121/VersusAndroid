package com.vs.bcd.versus.model;

import java.util.HashMap;
import java.util.HashSet;
import java.lang.reflect.Type;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vs.bcd.versus.activity.StartScreen;

public class SessionManager {
    // Shared Preferences
    SharedPreferences pref;

    // Editor for Shared preferences
    Editor editor;

    // Context
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "AndroidVSPref";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";

    //birthday, email, firstname, lastname, password, phone, username
    //make these public to access from outside
    public static final String KEY_BDAY = "pref_birthday";
    public static final String KEY_EMAIL = "pref_email";
    public static final String KEY_FIRSTNAME = "pref_firstname";
    public static final String KEY_LASTNAME = "pref_lastname";
    public static final String KEY_PHONE = "pref_phone";
    public static final String KEY_USERNAME = "pref_username";
    public static final String KEY_TIMECODE = "pref_timecode";
    public static final String KEY_FW = "pref_fw";  //following
    public static final String KEY_FC = "pref_fc";  //follower count
    public static final String KEY_MKEY = "pref_mkey";
    public static final String KEY_PURL = "pref_purl";

    //keep password private
    private static final String KEY_PASSWORD = "pref_password";


    // Constructor
    public SessionManager(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create login session
     * */
    public void createLoginSession(User user){
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true);

        //store these in shared pref
        editor.putString(KEY_BDAY, user.getBirthday());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_FIRSTNAME, user.getFirstName());
        editor.putString(KEY_LASTNAME, user.getLastName());
        editor.putString(KEY_PASSWORD, user.getPassword());
        editor.putString(KEY_PHONE, user.getPhone());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putInt(KEY_TIMECODE, user.getTimecode());
        editor.putInt(KEY_FC, user.getFc());
        editor.putString(KEY_MKEY, user.getMkey());
        editor.putString(KEY_PURL, user.getPurl());

        Gson gson = new Gson();

        String fnsStr = gson.toJson(user.getFw());
        editor.putString(KEY_FW, fnsStr);

        // commit changes
        editor.commit();
    }

    /**
     * Check login method wil check user login status
     * If false it will redirect user to login page
     * Else won't do anything
     * */
    public void checkLogin(){
        // Check login status
        if(!this.isLoggedIn()){
            // user is not logged in redirect him to Login Activity
            Intent i = new Intent(_context, StartScreen.class);
            // Closing all the Activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Add new Flag to start new Activity
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Staring Login Activity
            _context.startActivity(i);
        }

    }

    public String getCurrentUsername(){
        return pref.getString(KEY_USERNAME, null);  //TODO: null or "" for the default value (second param of getString)?
    }

    public String getMKey(){
        return pref.getString(KEY_MKEY, null);
    }

    public String getProfileImageURL(){
        return pref.getString(KEY_PURL, "0");
    }

    public String getBday(){
        return pref.getString(KEY_BDAY, null);
    }

    public int getUserTimecode(){
        return pref.getInt(KEY_TIMECODE, -1);
    }

    public HashSet<String> getFnsHashSet(){
        Gson gson = new Gson();
        String json = pref.getString(KEY_FW, null);
        Type type = new TypeToken<HashSet<String>>() {}.getType();
        HashSet<String> ret = gson.fromJson(json, type);
        return ret;
    }

    public void updateFollowingLocal(HashSet<String> updatedList){
        Gson gson = new Gson();
        String fwStr = gson.toJson(updatedList);
        editor.putString(KEY_FW, fwStr);
        editor.apply();
    }

    /**
     * Get stored session data
     * */
    public HashMap<String, String> getUserDetails(){
        HashMap<String, String> user = new HashMap<String, String>();

        //birthday, email, firstname, lastname, password, phone, username
        user.put(KEY_BDAY, pref.getString(KEY_BDAY, null));
        user.put(KEY_EMAIL, pref.getString(KEY_EMAIL, null));
        user.put(KEY_FIRSTNAME, pref.getString(KEY_FIRSTNAME, null));
        user.put(KEY_LASTNAME, pref.getString(KEY_LASTNAME, null));
        user.put(KEY_PASSWORD, pref.getString(KEY_PASSWORD, null));
        user.put(KEY_PHONE, pref.getString(KEY_PHONE, null));
        user.put(KEY_USERNAME, pref.getString(KEY_USERNAME, null));

        // return user
        return user;
    }

    /**
     * Clear session details
     * */
    public void logoutUser(){
        // Clearing all data from Shared Preferences
        editor.clear();
        editor.commit();

        // After logout redirect user to Loing Activity
        Intent i = new Intent(_context, StartScreen.class);
        // Closing all the Activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add new Flag to start new Activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Starting Login Activity
        _context.startActivity(i);
    }

    /**
     * Quick check for login
     * **/
    // Get Login State
    public boolean isLoggedIn(){
        return pref.getBoolean(IS_LOGIN, false);
    }
}