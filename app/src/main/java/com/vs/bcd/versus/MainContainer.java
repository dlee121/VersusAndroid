package com.vs.bcd.versus;

import android.media.Image;
import android.support.v7.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import org.w3c.dom.Text;

public class MainContainer extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentStatePagerAdapter} derivative
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private DynamoDBMapper mapper;
    private ImageButton toolbarButtonLeft;
    private TextView titleTxtView;
    private String lastSetTitle = "";
    private MainActivity mainActivityFragRef;
    private CreatePost createPost;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPagerCustomDuration mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                this.getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);

    /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    */
        //hiding default app icon
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        //displaying custom ActionBar
        View mActionBarView = getLayoutInflater().inflate(R.layout.custom_action_bar, null);
        actionBar.setCustomView(mActionBarView);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setElevation(0);
        toolbarButtonLeft = (ImageButton) mActionBarView.findViewById(R.id.btn_slide);
        titleTxtView = (TextView) mActionBarView.findViewById(R.id.textView);

        toolbarButtonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = mViewPager.getCurrentItem();
                switch (i) {
                    case 0:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(1);
                        titleTxtView.setText(lastSetTitle);
                        break;
                    case 1:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText("Search");
                        break;
                    case 2:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(1);
                        titleTxtView.setText(lastSetTitle);
                        break;
                    default:
                        break;
                }
            }
        });


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPagerCustomDuration) findViewById(R.id.container2);
        mViewPager.setScrollDurationFactor(1);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setPageTransformer(false, new FadePageTransformer());
        //mViewPager.setPageTransformer(false, new NoPageTransformer());

        mViewPager.setCurrentItem(1);

    }

    public TextView getToolbarTitleText(){
        return titleTxtView;
    }

    public void setToolbarTitleText(String str){
        titleTxtView.setText(str);
        lastSetTitle = str;
    }


    public ImageButton getToolbarButton(){
        return toolbarButtonLeft;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_container, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
                    SearchPage searchPage = new SearchPage();
                    return searchPage;
                case 1:
                    MainActivity mainActivityFragment = new MainActivity();
                    mainActivityFragRef = mainActivityFragment;
                    return mainActivityFragment;
                case 2:
                    createPost = new CreatePost();
                    return createPost;
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SEARCH";
                case 1:
                    return "MAIN";
                case 2:
                    return "CREATE POST";
            }
            return null;
        }
    }

    public MainActivity getMainFrag(){
        return mainActivityFragRef;
    }

    public DynamoDBMapper getMapper(){
        return mapper;
    }

/*  same thing in MainActivity's FAB onclick listener
    public void createPostClicked(View view){
        if(mViewPager.getCurrentItem() == 0){
            mViewPager.setCurrentItem(1, false);
            mViewPager.setCurrentItem(2);
        }
        else {
            mViewPager.setCurrentItem(2);
        }
    }
*/
    public ViewPager getViewPager(){
        return mViewPager;
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


    public void createButtonPressed(View view){
        createPost.createButtonPressed(view);
    }
}
