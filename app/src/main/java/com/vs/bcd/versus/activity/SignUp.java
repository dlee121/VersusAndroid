package com.vs.bcd.versus.activity;

import android.content.Intent;
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
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.model.NotAuthorizedException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.vs.bcd.api.VersusAPIClient;
import com.vs.bcd.api.model.UserPutModel;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.WhatsYourBirthday;
import com.vs.bcd.versus.fragment.WhatsYourName;
import com.vs.bcd.versus.fragment.WhatsYourPassword;
import com.vs.bcd.versus.fragment.WhatsYourUsername;
import com.vs.bcd.versus.model.ViewPagerCustomDuration;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class SignUp extends AppCompatActivity {

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
    private WhatsYourPassword ypfrag;
    private String first, second, third, fourth, biebs;
    private DynamoDBMapper mapper;
    private SessionManager sessionManager;
    private SignUp thisActivity;
    private WhatsYourName wyn;
    private WhatsYourBirthday wyb;
    private WhatsYourUsername wyun;
    private FirebaseAuth mFirebaseAuth;
    private CognitoCachingCredentialsProvider credentialsProvider;
    Toast mToast;
    private ApiClientFactory factory;
    private VersusAPIClient client;

    @Override
    public void onBackPressed(){
        int currentItem = mViewPager.getCurrentItem();
        if(currentItem == 0){  //MainContainer's current fragment is MainActivity fragment
            super.onBackPressed();  //call superclass's onBackPressed
        }
        else {
            mViewPager.setCurrentItem(currentItem - 1);
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

        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar1);
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
        mViewPager.setOffscreenPageLimit(4);
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
                case 1:
                    mViewPager.setCurrentItem(0);
                    wyn.enableChildViews();
                    return true;
                case 2:
                    mViewPager.setCurrentItem(1);
                    wyb.enableChildViews();
                    return true;
                case 3:
                    mViewPager.setCurrentItem(2);
                    wyun.enableChildViews();
                    return true;
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
                    wyn = new WhatsYourName();
                    return wyn;
                case 1:
                    wyb = new WhatsYourBirthday();
                    return wyb;
                case 2:
                    wyun = new WhatsYourUsername();
                    return wyun;
                case 3:
                    ypfrag = new WhatsYourPassword();
                    return ypfrag;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "What's Your Name?";
                case 1:
                    return "When's Your Birthday?";
                case 2:
                    return "Choose a Username";
                case 3:
                    return "Choose a Password";
            }
            return null;
        }
    }

    public ViewPagerCustomDuration getViewPager(){
        return mViewPager;
    }

    public VersusAPIClient getClient(){
        return client;
    }

    public void setTwo(String firsh, String sec){
        first = firsh;
        second = sec;
    }

    public void setThr(String thur){
        third = thur;
    }

    public void setFo(String fo){
        fourth = fo;
    }

    public void setBiebs(String biebs){
        this.biebs = biebs;
    }

    public String getUserString(){
        return first + "/" + second + "/" + third + "/" + fourth;
    }

    public void signUpUser(){
        //TODO: validate forms, and hash/salt password

        ypfrag.displayProgressBar(true);

        //validate, write to db (which completes registration), write session data to SharedPref (same as login) then move on to MainContainer



        final User newUser = new User(getUserString()); //TODO: don't hold password in user object anymore. In fact, don't hold it anywhere.

        mFirebaseAuth.createUserWithEmailAndPassword(newUser.getUsername() + "@versusbcd.com", biebs)
                .addOnCompleteListener(thisActivity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
                            //TODO: create Cognito Auth credentials using OIDC, use that to send the user info through API Gateway to Lambda function that will insert the new user data into ES
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
                                                    userPutModel.setEm(newUser.getEmail());
                                                    userPutModel.setFn(newUser.getFirstName());
                                                    userPutModel.setG(BigDecimal.ZERO);
                                                    userPutModel.setIn(BigDecimal.ZERO);
                                                    userPutModel.setLn(newUser.getLastName());
                                                    userPutModel.setPh(newUser.getPhone());
                                                    userPutModel.setPi(BigDecimal.valueOf(newUser.getProfileImage()));
                                                    userPutModel.setS(BigDecimal.ZERO);
                                                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
                                                    userPutModel.setT(df.format(new Date()));

                                                    client.userputPost(userPutModel, newUser.getUsername(), "put", "user");

                                                    thisActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sessionManager.createLoginSession(newUser);
                                                            Intent intent = new Intent(thisActivity, MainContainer.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   //clears back stack for navigation
                                                            intent.putExtra("oitk", getTokenResult.getToken());
                                                            startActivity(intent);
                                                            overridePendingTransition(0, 0);
                                                        }
                                                    });

                                                }catch (Exception e){
                                                    credentialsProvider.clear();
                                                    credentialsProvider = new CognitoCachingCredentialsProvider(
                                                            getApplicationContext(),
                                                            "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                                                            Regions.US_EAST_1 // Region
                                                    );
                                                    credentialsProvider.refresh();
                                                    ypfrag.displayProgressBar(false);
                                                    thisActivity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {

                                                            //LoginManager.getInstance().logOut();
                                                            //GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(); //google logout
                                                            //GoogleSignIn.getClient(thisActivity, gso).signOut();
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
                                        ypfrag.displayProgressBar(false);
                                        Toast.makeText(thisActivity, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            else {
                                ypfrag.displayProgressBar(false);
                                Toast.makeText(thisActivity, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            ypfrag.displayProgressBar(false);
                            Toast.makeText(thisActivity, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SignUp.this, "Something went wrong. Please try again", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
