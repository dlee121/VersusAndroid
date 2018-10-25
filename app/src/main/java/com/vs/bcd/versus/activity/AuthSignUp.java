package com.vs.bcd.versus.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.amazonaws.regions.Regions;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.vs.bcd.api.VersusAPIClient;
import com.vs.bcd.api.model.UserPutModel;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.AuthBirthdayInput;
import com.vs.bcd.versus.fragment.AuthGDPRFragment;
import com.vs.bcd.versus.fragment.AuthSignUpFragment;
import com.vs.bcd.versus.model.ViewPagerCustomDuration;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class AuthSignUp extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPagerCustomDuration mViewPager;
    private String bday, username;
    private SessionManager sessionManager;
    private AuthSignUp thisActivity;
    private AuthBirthdayInput wyb;
    private AuthSignUpFragment signUpFragment;
    private FirebaseAuth mFirebaseAuth;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private String authID = " ";
    private String authToken = " ";
    private Toast mToast;
    private ApiClientFactory factory;
    private VersusAPIClient client;

    private String currentToken = "";
    private String currentUsername = "";

    @Override
    public void onBackPressed(){
        int currentItem = mViewPager.getCurrentItem();
        if(currentItem == 0){  //MainContainer's current fragment is MainActivity fragment
            super.onBackPressed();  //call superclass's onBackPressed
        }
        else {
            //count as noButton click and go to MainContainer
            GDPRNoButtonClicked();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        factory = new ApiClientFactory().credentialsProvider(credentialsProvider);
        client = factory.build(VersusAPIClient.class);

        mFirebaseAuth = FirebaseAuth.getInstance();

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            authID = extras.getString("authid");
            //Log.d("fblogincreate", authID);
            authToken = extras.getString("token");
        }

        Toolbar toolbar = findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");

        thisActivity = this;
        sessionManager = new SessionManager(this);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());


        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPagerCustomDuration) findViewById(R.id.containersup);
        mViewPager.setScrollDurationFactor(1);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPageTransformer(false, new FadePageTransformer());

        mViewPager.setCurrentItem(0);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home){
            switch (mViewPager.getCurrentItem()){
                case 0:
                    return super.onOptionsItemSelected(item);
                default:
                    return true;
            }
        }


        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            //Return current tabs
            switch (position) {
                case 0:
                    signUpFragment = new AuthSignUpFragment();
                    return signUpFragment;
                case 1:
                    return new AuthGDPRFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Create an Account";
                case 1:
                    return "GDPR Consent Form";
            }
            return null;
        }
    }

    public ViewPagerCustomDuration getViewPager(){
        return mViewPager;
    }

    public void setB(String b){
        bday = b;
    }

    public void setU(String u){
        username = u;
    }

    public VersusAPIClient getClient(){
        return client;
    }

    public void signUpUser(){

        Log.d("suauthid", authID);
        final User newUser = new User(bday, username, authID);
        final AuthCredential credential;
        if(authID.charAt(authID.length()-1) == '_'){ //we append facebook authIDs with an '_'
            credential = FacebookAuthProvider.getCredential(authToken);
        }
        else{
            credential = GoogleAuthProvider.getCredential(authToken, null);
        }

        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("facebookLogin", "signInWithCredential:success");
                            FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();

                            if(firebaseUser != null){
                                mFirebaseAuth.getCurrentUser().getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                                    @Override
                                    public void onSuccess(final GetTokenResult getTokenResult) {
                                        Map<String, String> logins = new HashMap<>();
                                        logins.put("securetoken.google.com/bcd-versus", getTokenResult.getToken());

                                        credentialsProvider.setLogins(logins);

                                        Runnable runnable = new Runnable() {
                                            public void run() {
                                                try{
                                                    credentialsProvider.refresh();

                                                    factory = new ApiClientFactory().credentialsProvider(credentialsProvider);
                                                    client = factory.build(VersusAPIClient.class);

                                                    UserPutModel userPutModel = new UserPutModel();
                                                    userPutModel.setAi(newUser.getAuthID());
                                                    userPutModel.setB(BigDecimal.ZERO);
                                                    userPutModel.setBd(newUser.getBday());
                                                    userPutModel.setCs(newUser.getUsername());
                                                    userPutModel.setEm(newUser.getEmail());
                                                    userPutModel.setG(BigDecimal.ZERO);
                                                    userPutModel.setIn(BigDecimal.ZERO);
                                                    userPutModel.setPi(BigDecimal.valueOf(newUser.getProfileImage()));
                                                    userPutModel.setS(BigDecimal.ZERO);
                                                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                                                    userPutModel.setT(df.format(new Date()));

                                                    client.userputPost(userPutModel, newUser.getUsername().toLowerCase(), "put", "user");

                                                    currentUsername = newUser.getUsername();
                                                    sessionManager.createLoginSession(newUser, true);
                                                    new JsonTask().execute("http://adservice.google.com/getconfig/pubvendors", getTokenResult.getToken());

                                                }catch (Exception e){
                                                    thisActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sessionManager.logoutUser();
                                                            credentialsProvider.clear();
                                                            credentialsProvider = new CognitoCachingCredentialsProvider(
                                                                    getApplicationContext(),
                                                                    "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                                                                    Regions.US_EAST_1 // Region
                                                            );
                                                            credentialsProvider.refresh();
                                                            LoginManager.getInstance().logOut();
                                                            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(); //google logout
                                                            GoogleSignIn.getClient(thisActivity, gso).signOut();
                                                            if(mToast != null){
                                                                mToast.cancel();
                                                            }
                                                            mToast = Toast.makeText(thisActivity, "Something went wrong. Please try again.", Toast.LENGTH_SHORT);
                                                            mToast.show();
                                                        }
                                                    });
                                                }
                                            }
                                        };
                                        Thread mythread = new Thread(runnable);
                                        mythread.start();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        signUpFragment.reenableSignupButton();
                                        Toast.makeText(thisActivity, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            else {
                                signUpFragment.reenableSignupButton();
                                Toast.makeText(thisActivity, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            // If sign in fails, display a message to the user.
                            signUpFragment.reenableSignupButton();
                            Log.w("facebookLogin", "signInWithCredential:failure", task.getException());
                            Toast.makeText(AuthSignUp.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public class FadePageTransformer implements ViewPager.PageTransformer {
        public void transformPage(View view, float position) {
            view.setTranslationX(view.getWidth() * -position);

            if(position <= -1.0F || position >= 1.0F) {
                view.setAlpha(0.0F);
            } else if( position == 0.0F ) {
                view.setAlpha(1.0F);
            } else {
                // position is between -1.0F & 0.0F OR 0.0F & 1.0F
                view.setAlpha(1.0F - Math.abs(position));
            }
        }
    }

    public void handleUnauthException(){
        credentialsProvider.clear();
        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        credentialsProvider.refresh();

        factory = new ApiClientFactory().credentialsProvider(credentialsProvider);
        client = factory.build(VersusAPIClient.class);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AuthSignUp.this, "Something went wrong. Please try again", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private class JsonTask extends AsyncTask<String, String, String> {

        String token;

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... params) {

            token = params[1];
            currentToken = token;
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }

                return buffer.toString();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {

                JSONObject responseObject = new JSONObject(result);
                if(responseObject.getBoolean("is_request_in_eea_or_unknown")) {
                    //User is in EU so show GDPR consent page
                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mViewPager.setCurrentItem(1);
                            try{
                                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                            }catch (Exception e){
                                Log.d("hi", "hey");
                            }
                        }
                    });
                }
                else {
                    //User is not in EU so proceed to MainContainer
                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(thisActivity, MainContainer.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   //clears back stack for navigation
                            intent.putExtra("oitk", token);
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        }
                    });
                }

            }catch (Exception e){
                Log.d("JSONRESULT", "Exception encountered");
            }
        }
    }

    public void GDPRYesButtonClicked() {
        Log.d("gdpraction", "yes clicked");

        //update user "bd" as "gdpr1" then move to MainContainer and initialize Appodeal with consent = true
        Runnable runnable = new Runnable() {
            public void run() {
                try{

                    if(factory == null) {
                        factory = new ApiClientFactory().credentialsProvider(credentialsProvider);
                    }
                    if(client == null) {
                        client = factory.build(VersusAPIClient.class);
                    }
                    client.setemailGet("gdpr1", "bd", currentUsername);

                }catch (Exception e){
                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sessionManager.logoutUser();
                            credentialsProvider.clear();
                            credentialsProvider = new CognitoCachingCredentialsProvider(
                                    getApplicationContext(),
                                    "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                                    Regions.US_EAST_1 // Region
                            );
                            credentialsProvider.refresh();
                            LoginManager.getInstance().logOut();
                            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(); //google logout
                            GoogleSignIn.getClient(thisActivity, gso).signOut();
                            if(mToast != null){
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(thisActivity, "Something went wrong. Please try again.", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    });
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

        sessionManager.setBd("gdpr1");

        thisActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(thisActivity, MainContainer.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   //clears back stack for navigation
                intent.putExtra("oitk", currentToken);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
    }

    public void GDPRNoButtonClicked() {
        Log.d("gdpraction", "no clicked");


        //update user "bd" as "gdpr0" then move to MainContainer and initialize Appodeal with consent = false
        Runnable runnable = new Runnable() {
            public void run() {
                try{

                    if(factory == null) {
                        factory = new ApiClientFactory().credentialsProvider(credentialsProvider);
                    }
                    if(client == null) {
                        client = factory.build(VersusAPIClient.class);
                    }
                    client.setemailGet("gdpr0", "bd", currentUsername);

                }catch (Exception e){
                    thisActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sessionManager.logoutUser();
                            credentialsProvider.clear();
                            credentialsProvider = new CognitoCachingCredentialsProvider(
                                    getApplicationContext(),
                                    "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                                    Regions.US_EAST_1 // Region
                            );
                            credentialsProvider.refresh();
                            LoginManager.getInstance().logOut();
                            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(); //google logout
                            GoogleSignIn.getClient(thisActivity, gso).signOut();
                            if(mToast != null){
                                mToast.cancel();
                            }
                            mToast = Toast.makeText(thisActivity, "Something went wrong. Please try again.", Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    });
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();

        sessionManager.setBd("gdpr0");

        thisActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(thisActivity, MainContainer.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   //clears back stack for navigation
                intent.putExtra("oitk", currentToken);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

    }

}
