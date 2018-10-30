package com.vs.bcd.versus.model;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.firebase.auth.FirebaseAuth;
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
    public static final String KEY_USERNAME = "pref_username";
    public static final String KEY_PI = "pref_profileimage";
    public static final String KEY_IS_NATIVE = "pref_is_native"; //marks if the user logged in using native login or Facebook/Google
    public static final String KEY_TUTORIAL = "pref_tutorial";


    // Constructor
    public SessionManager(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create login session
     * */
    public void createLoginSession(User user, boolean isNative){
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true);

        //store these in shared pref
        editor.putString(KEY_BDAY, user.getBday());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putInt(KEY_PI, user.getProfileImage());
        editor.putBoolean(KEY_IS_NATIVE, isNative);
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

    public String getEmail(){
        return pref.getString(KEY_EMAIL, "0");
    }
    public void setEmail(String email){
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public boolean isNative(){
        return pref.getBoolean(KEY_IS_NATIVE, false);
    }

    public boolean showTutorial() {
        return !pref.getBoolean(KEY_TUTORIAL, false);
    }

    public void setTutorialShown() {
        editor.putBoolean(KEY_TUTORIAL, true);
        editor.apply();
    }

    public String getCurrentUsername(){
        return pref.getString(KEY_USERNAME, null);  //TODO: null or "" for the default value (second param of getString)?
    }

    public int getProfileImage(){
        return pref.getInt(KEY_PI, 0);
    }

    public void setProfileImage(int newPI){
        editor.putInt(KEY_PI, newPI);
        editor.apply();
    }

    public void setBd(String bd) {
        editor.putString(KEY_BDAY, bd);
        editor.apply();
    }

    public String getBday(){
        return pref.getString(KEY_BDAY, null);
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

        return pref.getBoolean(IS_LOGIN, false) && FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}