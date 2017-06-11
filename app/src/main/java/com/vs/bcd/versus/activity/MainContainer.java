package com.vs.bcd.versus.activity;

import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.vs.bcd.versus.fragment.PostPage;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.fragment.CreatePost;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.SearchPage;
import com.vs.bcd.versus.ViewPagerCustomDuration;

public class MainContainer extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentStatePagerAdapter} derivative
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private DynamoDBMapper mapper;
    private ImageButton toolbarButtonLeft;
    private Button logoutbuttontemp;
    private TextView titleTxtView;
    private String lastSetTitle = "";
    private MainActivity mainActivityFragRef;
    private CreatePost createPost;
    private PostPage postPage;
    private SessionManager sessionManager;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPagerCustomDuration mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);
        sessionManager = new SessionManager(this);
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
        titleTxtView = (TextView) mActionBarView.findViewById(R.id.textView);
        toolbarButtonLeft = (ImageButton) mActionBarView.findViewById(R.id.btn_slide);
        toolbarButtonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = mViewPager.getCurrentItem();
                switch (i) {
                    case 0:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        mViewPager.setCurrentItem(1);
                        titleTxtView.setText("Search");
                        break;
                    case 1:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        break;
                    case 2:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        break;
                    case 3:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        break;
                    default:
                        break;
                }
            }
        });
        logoutbuttontemp = (Button) mActionBarView.findViewById(R.id.logoutbutton);
        logoutbuttontemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            sessionManager.logoutUser();
            }
        });


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPagerCustomDuration) findViewById(R.id.container2);
        mViewPager.setScrollDurationFactor(1);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setPageTransformer(false, new FadePageTransformer());
        //mViewPager.setPageTransformer(false, new NoPageTransformer());

        mViewPager.setCurrentItem(0);

        Log.d("USER_INFO", sessionManager.getUserDetails().get(SessionManager.KEY_USERNAME));

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
                    MainActivity mainActivityFragment = new MainActivity();
                    mainActivityFragRef = mainActivityFragment;
                    return mainActivityFragment;
                case 1:
                    SearchPage searchPage = new SearchPage();
                    return searchPage;
                case 2:
                    createPost = new CreatePost();
                    return createPost;
                case 3:
                    postPage = new PostPage();
                    return postPage;
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "MAIN";
                case 1:
                    return "SEARCH";
                case 2:
                    return "CREATE POST";
                case 3:
                    return "POST PAGE";
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

    public void postClicked(String postID){
        Log.d("POSTID", postID);
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
