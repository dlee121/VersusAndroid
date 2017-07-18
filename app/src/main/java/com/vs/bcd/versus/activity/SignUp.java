package com.vs.bcd.versus.activity;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.ViewPagerCustomDuration;
import com.vs.bcd.versus.fragment.CommentEnterFragment;
import com.vs.bcd.versus.fragment.CreatePost;
import com.vs.bcd.versus.fragment.PostPage;
import com.vs.bcd.versus.fragment.SearchPage;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.model.User;

import static com.vs.bcd.versus.R.id.editText;

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
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
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
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sign_up, menu);
        return true;
    }
*/

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
        return first + "%" + second + "%" + third + "%" + fourth + "%" + biebs;
    }

    public void signUpUser(){
        //TODO: validate forms, and hash/salt password

        ypfrag.displayProgressBar(true);

        //validate, write to db (which completes registration), write session data to SharedPref (same as login) then move on to MainContainer



        final User newUser = new User(getUserString());

        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    mapper.save(newUser);
                    //TODO: Ensure that lines below are called only if mapper.save(newUser) is successful (in other words, make sure thread waits until mapper.save(newUser) finishes its job successfully, otherwise throwing exception to exit thread before calling below lines to login the new user).
                    //TODO: So far this seems to be the case, as any exception thrown is caught and lines below don't get executed because we exit the thread when exception is thrown.
                    //TODO: Cuz if there is a case where lines below are executed despite mapper.save failure, that would probably cause some bugs.
                    //TODO: maybe do some synchronization/thread magic just to be on the safe side. We're good for now though.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sessionManager.createLoginSession(newUser);
                            Intent intent = new Intent(thisActivity, MainContainer.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   //clears back stack for navigation
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        }
                    });
                }
                catch (Throwable t){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ypfrag.displayProgressBar(false);
                            Toast.makeText(thisActivity, "There was a problem signing up. Please check your network connection and try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
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

    public DynamoDBMapper getMapper(){
        return mapper;
    }
}
