package com.vs.bcd.versus.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.vs.bcd.versus.fragment.CategoryFragment;
import com.vs.bcd.versus.fragment.CommentEnterFragment;
import com.vs.bcd.versus.fragment.PostPage;
import com.vs.bcd.versus.fragment.SelectCategory;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.PostSkeleton;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.fragment.CreatePost;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.SearchPage;
import com.vs.bcd.versus.ViewPagerCustomDuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.R.id.edit;
import static com.amazonaws.regions.ServiceAbbreviations.S3;

public class MainContainer extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentStatePagerAdapter} derivative
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper;
    private ImageButton toolbarButtonLeft;
    private Button logoutbuttontemp;
    private TextView titleTxtView;
    private String lastSetTitle = "";
    private MainActivity mainActivityFragRef;
    private CreatePost createPost;
    private PostPage postPage;
    private CategoryFragment categoryFragment;
    private SessionManager sessionManager;
    private CommentEnterFragment commentEnterFragment;
    private AmazonS3 s3;
    private Bitmap xBmp = null;
    private Bitmap yBmp = null;
    private ViewPagerCustomDuration mViewPager;
    private DisplayMetrics windowSize;
    private HashMap<String, String> postInDownload = new HashMap<>();
    private boolean fromCategoryFragment = false;
    private String currentCFTitle = "";
    private AHBottomNavigation bottomNavigation;
    private int userTimecode = -1;


    @Override
    public void onBackPressed(){
        int mainContainerCurrentItem = mViewPager.getCurrentItem();
        int mainActivityCurrentItem = getMainFrag().getViewPager().getCurrentItem();
        if(mainContainerCurrentItem == 0){  //MainContainer's current fragment is MainActivity fragment
            if(mainActivityCurrentItem == 0){   //MainActivity fragment's current fragment is Tab1Newsfeed
                super.onBackPressed();  //call superclass's onBackPressed, closing the app
            }
            else {  //MainActivity fragment's current fragment is not Tab1Newsfeed, so we need to first navigate to Tab1Newsfeed on MainActivity fragment
                getMainFrag().getViewPager().setCurrentItem(0);
            }
        }
        else {

            switch(mainContainerCurrentItem){
                case 2: //CreatePost
                    if(!fromCategoryFragment){
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                    }
                    else{
                        mViewPager.setCurrentItem(6);
                        titleTxtView.setText(currentCFTitle);
                    }
                    break;

                case 3:  //PostPage
                    //Log.d("debug", "is 1");
                    if(!postPage.isRootLevel()){
                        //Log.d("debug", "is not root");
                        postPage.backToParentPage();
                    }
                    else{
                        //Log.d("debug", "is root");
                        postPage.writeActionsToDB();
                        xBmp = null;
                        yBmp = null;
                        //postPage.clearList();
                        if(!fromCategoryFragment){  //not from CategoryFragment, i.e. currently from Newsfeed or Trending
                            toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                            titleTxtView.setText(lastSetTitle);
                            mViewPager.setCurrentItem(0);
                        }
                        else {  //from CategoryFragment
                            mViewPager.setCurrentItem(6);
                        }
                    }
                    break;

                case 5: //currently in SelectCategory for CreatePost
                    mViewPager.setCurrentItem(2);   //go back to CreatePost
                    titleTxtView.setText("Create Post");
                    //TODO: reset fragment instead of leaving it in same state
                    break;

                case 6: //currently in CategoryFragment
                    categoryFragment.clearPosts();
                    toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                    mViewPager.setCurrentItem(0);
                    categoryFragmentOut();
                    titleTxtView.setText(lastSetTitle);
                    break;

                default:
                    toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                    mViewPager.setCurrentItem(0);
                    titleTxtView.setText(lastSetTitle);
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);
        sessionManager = new SessionManager(this);
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);
        s3 = new AmazonS3Client(credentialsProvider);

        userTimecode = sessionManager.getUserTimecode();


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

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
                    case 0: //MainActivity Fragment
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        mViewPager.setCurrentItem(1);
                        titleTxtView.setText("Search");
                        break;

                    case 1: //SearchPage
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        break;

                    case 2: //CreatePost
                        if(!fromCategoryFragment){
                            mViewPager.setCurrentItem(0);
                            titleTxtView.setText(lastSetTitle);
                            toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        }
                        else{
                            mViewPager.setCurrentItem(6);
                            titleTxtView.setText(currentCFTitle);
                        }
                        break;

                    case 3: //PostPage
                        if(!postPage.isRootLevel()){
                            postPage.backToParentPage();
                        }
                        else{
                            postPage.writeActionsToDB();
                            //postPage.clearList();

                            if(!fromCategoryFragment){  //not from CategoryFragment, i.e. currently from Newsfeed or Trending
                                toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                                titleTxtView.setText(lastSetTitle);
                                mViewPager.setCurrentItem(0);
                            }
                            else {  //from CategoryFragment
                                mViewPager.setCurrentItem(6);
                            }

                            xBmp = null;
                            yBmp = null;
                        }
                        break;

                    case 4: //commentEnterFragment
                        mViewPager.setCurrentItem(3);
                        titleTxtView.setText(lastSetTitle);
                        /*  why were these lines here in the first place?
                        xBmp = null;
                        yBmp = null;
                        */
                        break;

                    case 5: //category selection screen for CreatePost
                        mViewPager.setCurrentItem(2);   //go back to CreatePost
                        titleTxtView.setText("Create Post");
                        //TODO: reset fragment instead of leaving it in same state
                        break;

                    case 6: //CategoryFragment
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        categoryFragmentOut();
                        titleTxtView.setText(lastSetTitle);
                        categoryFragment.clearPosts();
                        break;
                /*
                    case 7: //LeaderboardTab

                        break;

                    case 8: //NotificationsTab

                        break;

                    case 9: //MeTab

                        break;
                */
                    //In cases 7 - 9, we disable the toolbarButtonLeft (Search/Up button), so no need to program them for now.


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
        mViewPager.setOffscreenPageLimit(7);
        mViewPager.setPageTransformer(false, new FadePageTransformer());
        //mViewPager.setPageTransformer(false, new NoPageTransformer());

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                // Check if this is the page you want.
                switch(position){
                    //We probably only need to account for when the transition between yes-tab / no-tab happens (tabs 0, 2, 3, 6)
                    //But if it makes no significant performance difference to include all the other tabs, we might as well, to be on safe side especially in case future developments necessitate doing so.
                    case 0: //MainActivity
                        enableBottomTabs();
                        break;

                    case 1: //SearchPage
                        enableBottomTabs();
                        break;

                    case 2: //CreatePost
                        disableBottomTabs();
                        break;

                    case 3: //PostPage
                        disableBottomTabs();
                        break;

                    case 4: //CommentEnterFragment
                        disableBottomTabs();
                        break;

                    case 5: //SelectCategory
                        disableBottomTabs();
                        break;

                    case 6: //CategoryFragment
                        enableBottomTabs();
                        break;

                    case 7: //LeaderboardTab
                        enableBottomTabs();
                        break;

                    case 8: //NotificationsTab
                        enableBottomTabs();
                        break;

                    case 9: //MeTab
                        enableBottomTabs();
                        break;

                    default:

                        break;
                }

            }
        });

        mViewPager.setCurrentItem(0);
        setToolbarTitleTextForTabs("Newsfeed");

        windowSize = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(windowSize);



        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);

        // Create items
        AHBottomNavigationItem item1 = new AHBottomNavigationItem("Main", R.drawable.goldmedal);
        AHBottomNavigationItem item2 = new AHBottomNavigationItem("Leaderboard", R.drawable.goldmedal);
        AHBottomNavigationItem item3 = new AHBottomNavigationItem("Notifications", R.drawable.goldmedal);
        AHBottomNavigationItem item4 = new AHBottomNavigationItem("Me", R.drawable.goldmedal);

        // Add items
        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);
        bottomNavigation.addItem(item4);

        // Set background color
        bottomNavigation.setDefaultBackgroundColor(Color.parseColor("#FEFEFE"));

        //always show tab titles
        bottomNavigation.setTitleState(AHBottomNavigation.TitleState.ALWAYS_SHOW);

        // Set listeners
        bottomNavigation.setOnTabSelectedListener(new AHBottomNavigation.OnTabSelectedListener() {
            @Override
            public boolean onTabSelected(int position, boolean wasSelected) {
                switch (position){
                    case 0: //MainActivity
                        mViewPager.setCurrentItem(0);
                        break;

                    case 1: //LeaderboardTab

                        break;

                    case 2: //NotificationsTab

                        break;

                    case 3: //MeTab

                        break;

                    default:
                        break;
                }
                return true;
            }
        });
        bottomNavigation.setBehaviorTranslationEnabled(false);

        // Set current item programmatically
        bottomNavigation.setCurrentItem(0);

        Log.d("USER_INFO", sessionManager.getUserDetails().get(SessionManager.KEY_USERNAME));

    }

    public TextView getToolbarTitleText(){
        return titleTxtView;
    }

    public void setToolbarTitleTextForTabs(String str){
        titleTxtView.setText(str);
        lastSetTitle = str;
    }

    public void setToolbarTitleTextForCP(){
        if(fromCategoryFragment){
            titleTxtView.setText(currentCFTitle);
        }
        else{
            titleTxtView.setText(lastSetTitle);
        }
    }

    public void setLeftChevron(){
        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
    }

    public void setToolbarTitleForCF(String titleForCF){
        titleTxtView.setText(titleForCF);
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
                case 4:
                    commentEnterFragment = new CommentEnterFragment();
                    return commentEnterFragment;
                case 5:
                    return new SelectCategory();
                case 6:
                    categoryFragment = new CategoryFragment();
                    return categoryFragment;
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            return 7;
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
                case 4:
                    return "COMMENT ENTER PAGE";
                case 5:
                    return "SELECT CATEGORY";
                case 6:
                    return "CATEGORY";
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
    public AmazonS3 getS3Client(){
        return s3;
    }
    public ViewPager getViewPager(){
        return mViewPager;
    }

    //pass post information from MyAdapter CardView click handler, through this helper method, to PostPage fragment
    public void postClicked(PostSkeleton post){
        String temp = postInDownload.get(post.getPost_id());
        if(temp == null || !temp.equals("in progress")){
            postInDownload.put(post.getPost_id(), "in progress");
            postPage.clearList();
            postPage.setContent(post, true);
        }
        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
        mViewPager.setCurrentItem(3);
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

    public CommentEnterFragment getCommentEnterFragment(){
        return commentEnterFragment;
    }

    public PostPage getPostPage(){
        return postPage;
    }

    public CategoryFragment getCategoryFragment(){
        return categoryFragment;
    }

    public void createButtonPressed(View view){
        createPost.createButtonPressed(view);
    }

    public SessionManager getSessionManager(){
        return sessionManager;
    }

    public void setBMP(Bitmap xBmp, Bitmap yBmp){
        this.xBmp = xBmp == null? null:xBmp;
        this.yBmp = yBmp == null? null:yBmp;
        if(xBmp != null && yBmp != null){
            Log.d("BMPs set", "BMPs set");
        }
    }

    public AmazonDynamoDBClient getDDBClient(){
        return ddbClient;
    }

    public boolean hasXBMP(){
        return xBmp != null;
    }

    public boolean hasYBMP(){
        return yBmp != null;
    }

    public Bitmap getXBMP(){
        return xBmp;
    }

    public Bitmap getYBMP(){
        return yBmp;
    }

    public int getWindowWidth(){
        return windowSize.widthPixels;
    }

    public void addPostInDownload(String postID){
        postInDownload.put(postID, "true");
    }
    public void removePostInDownload(String postID){
        postInDownload.remove(postID);
    }
    public boolean existsInPostInDownload(String postID){
        return postInDownload.get(postID) != null;
    }
    public void setPostInDownload(String postID, String setting){
        postInDownload.put(postID, setting);
    }
    public String getPostInDownloadStatus(String postID){
        String temp = postInDownload.get(postID);
        if(temp == null){
            return "entry not found";
        }
        else{
            return temp;
        }

    }

    public int getWindowHeight(){
        return windowSize.heightPixels;
    }

    public CreatePost getCreatePostFragment(){
        return createPost;
    }

    public void setUpCategoriesList(ArrayList<CategoryObject> categories){
        categories.add(new CategoryObject("Cars", R.drawable.goldmedal, 0));  //TODO: set actual icons, for now gold medal as placeholder
        categories.add(new CategoryObject("Celebrities", R.drawable.goldmedal, 1));
        categories.add(new CategoryObject("Culture", R.drawable.goldmedal, 2));
        categories.add(new CategoryObject("Education", R.drawable.goldmedal, 3));
        categories.add(new CategoryObject("Ethics/Morality", R.drawable.goldmedal, 4));
        categories.add(new CategoryObject("Fashion", R.drawable.goldmedal, 5));
        categories.add(new CategoryObject("Fiction", R.drawable.goldmedal, 6));
        categories.add(new CategoryObject("Finance", R.drawable.goldmedal, 7));
        categories.add(new CategoryObject("Food", R.drawable.goldmedal, 8));
        categories.add(new CategoryObject("Game", R.drawable.goldmedal, 9));
        categories.add(new CategoryObject("Law", R.drawable.goldmedal, 10));
        categories.add(new CategoryObject("Movies", R.drawable.goldmedal, 11));
        categories.add(new CategoryObject("Music/Artists", R.drawable.goldmedal, 12));
        categories.add(new CategoryObject("Politics", R.drawable.goldmedal, 13));
        categories.add(new CategoryObject("Porn", R.drawable.goldmedal, 14));
        categories.add(new CategoryObject("Religion", R.drawable.goldmedal, 15));
        categories.add(new CategoryObject("Restaurants", R.drawable.goldmedal, 16));
        categories.add(new CategoryObject("Science", R.drawable.goldmedal, 17));
        categories.add(new CategoryObject("Sex", R.drawable.goldmedal, 18));
        categories.add(new CategoryObject("Sports", R.drawable.goldmedal, 19));
        categories.add(new CategoryObject("Technology", R.drawable.goldmedal, 20));
        categories.add(new CategoryObject("Travel", R.drawable.goldmedal, 21));
        categories.add(new CategoryObject("TV Shows", R.drawable.goldmedal, 22));
        categories.add(new CategoryObject("Weapons", R.drawable.goldmedal, 23));
    }

    public boolean showPost(){
        int currFragIndex = mViewPager.getCurrentItem();
        return (currFragIndex == 0 || currFragIndex == 6);
    }

    public void categoryFragmentIn(String currentCFTitle){
        fromCategoryFragment = true;
        this.currentCFTitle = currentCFTitle;
    }
    public void categoryFragmentOut(){
        fromCategoryFragment = false;
        this.currentCFTitle = "";
    }

    private void enableBottomTabs(){
        bottomNavigation.restoreBottomNavigation(false);
    }

    private void disableBottomTabs(){
        bottomNavigation.hideBottomNavigation(false);
    }

    public int getUserTimecode(){
        return userTimecode;
    }

    public String getUsername(){
        return sessionManager.getCurrentUsername();
    }

    public ArrayList<String> getUserPostsList(){
        return sessionManager.getUserPostsList();
    }

    public ArrayList<String> getUserCommentsList(){
        return sessionManager.getUserCommentsList();
    }

    public void updateUserLocalPostsList(String entry){
        sessionManager.updateUserLocalPostsList(entry);
    }

    public void updateUserLocalCommentsList(String entry){
        sessionManager.updateUserLocalCommentsList(entry);
    }

}
