package com.vs.bcd.versus;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.PorterDuff;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

public class MainActivity extends AppCompatActivity {
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentStatePagerAdapter} derivative
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private DynamoDBMapper mapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                this.getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        //set tab icons

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_trending_selected);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_trending_unselected);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_randomvs_unselected);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_trending_unselected);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: //newsfeed
                        tab.setIcon(R.drawable.ic_trending_selected);
                        break;
                    case 1: //trending
                        tab.setIcon(R.drawable.ic_trending_selected);
                        break;
                    case 2: //randomvs
                        tab.setIcon(R.drawable.ic_randomvs_selected);
                        break;
                    case 3: //categories
                        tab.setIcon(R.drawable.ic_trending_selected);
                        break;
                    default:
                        tab.setIcon(R.drawable.ic_trending_selected);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: //newsfeed
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                    case 1: //trending
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                    case 2: //randomvs
                        tab.setIcon(R.drawable.ic_randomvs_unselected);
                        break;
                    case 3: //categories
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                    default:
                        tab.setIcon(R.drawable.ic_trending_unselected);
                        break;
                }

            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //Search_Main button was pressed
        if (id == R.id.action_search_main) {
            //TODO:Activate Search UI and Implement Main Search function
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //deleted PlaceholderFragment class
    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            //Return current tabs
            switch (position) {
                case 0:
                    Tab1Newsfeed tab1 = new Tab1Newsfeed();
                    return tab1;
                case 1:
                    Tab2Trending tab2 = new Tab2Trending();
                    return tab2;
                case 2:
                    Tab3RandomVS tab3 = new Tab3RandomVS();
                    return  tab3;
                case 3:
                    Tab4Categories tab4 = new Tab4Categories();
                    return tab4;
            /*
                case 4:
                    Tab5Leaderboard tab5 = new Tab5Leaderboard();
                    return tab5;
                case 5:
                    Tab6Me tab6 = new Tab6Me();
                    return tab6;
            */
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }
/*
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "NEWSFEED";

                case 1:
                    return "TRENDING";

                case 2:
                    return "RANDOMVS";
                case 3:
                    return "CATEGORIES";

                case 4:
                    return "LEADERBOARD";
                case 5:
                    return "ME";

            }
            return null;
        }
*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public DynamoDBMapper getMapper(){
        return mapper;
    }
}