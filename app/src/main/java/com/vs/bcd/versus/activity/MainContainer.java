package com.vs.bcd.versus.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeAppInstallAd.OnAppInstallAdLoadedListener;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.google.android.gms.ads.formats.NativeContentAd.OnContentAdLoadedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vs.bcd.versus.fragment.CategoryFragment;
import com.vs.bcd.versus.fragment.CommentEnterFragment;
import com.vs.bcd.versus.fragment.CreateMessage;
import com.vs.bcd.versus.fragment.LeaderboardTab;
import com.vs.bcd.versus.fragment.MessageRoom;
import com.vs.bcd.versus.fragment.NotificationsTab;
import com.vs.bcd.versus.fragment.PostPage;
import com.vs.bcd.versus.fragment.ProfileTab;
import com.vs.bcd.versus.fragment.SelectCategory;
import com.vs.bcd.versus.fragment.SettingsFragment;
import com.vs.bcd.versus.model.CategoryObject;
import com.vs.bcd.versus.model.Post;
import com.vs.bcd.versus.model.RNumAndUList;
import com.vs.bcd.versus.model.RoomObject;
import com.vs.bcd.versus.model.SessionManager;
import com.vs.bcd.versus.fragment.CreatePost;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.SearchPage;
import com.vs.bcd.versus.model.ViewPagerCustomDuration;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;


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
    private ImageButton toolbarButtonRight;
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
    private boolean meClicked = false;
    private ProfileTab profileTab;
    private int profileTabParent = 0;   //default parent is MainActivity, here parent just refers to previous page before the profile page was opened
    private HashSet<String> localFns;
    private String currUsername = null;
    private String beforeProfileTitle = "";
    private RelativeLayout.LayoutParams toolbarButtonRightLP, bottomNavLP, toolbarTextButtonLP;
    private RelativeLayout vpContainer;
    private RelativeLayout.LayoutParams vpContainerLP;
    private MessageRoom messageRoom;
    private String userMKey = "";
    private String profileImageURL = "";
    private Button toolbarTextButton;
    private CreateMessage createMessageFragment;
    private HashMap<String, String> following, followers;
    private String userPath = "";
    private String FOLLOWERS_CHILD, FOLLOWING_CHILD;
    private boolean initialFollowingLoaded = false;
    private String fcmToken = "";
    private boolean goToMainActivityOnResume = false;
    private int myAdapterFragInt = 0;
    private String postParentProfileUsername; //this could be a Stack instead of String to address nested profile-post-profile-post-etc case, for now we only keep immediate postParentProfile
    private CognitoCachingCredentialsProvider credentialsProvider;
    private DatabaseReference mFirebaseDatabaseReference;
    private ArrayList<NativeAd> nativeAds;
    private InputMethodManager imm;

    private String esHost = "search-versus-7754bycdilrdvubgqik6i6o7c4.us-east-1.es.amazonaws.com";
    private String esRegion = "us-east-1";

    @Override
    public void onBackPressed(){
        meClicked = false;
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
                        //postPage.clearList();
                        if(myAdapterFragInt == 9 && postParentProfileUsername != null){
                            goToProfile(postParentProfileUsername);
                        }
                        else{
                            mViewPager.setCurrentItem(myAdapterFragInt);
                        }
                        xBmp = null;
                        yBmp = null;
                    }
                    break;

                case 4:
                    mViewPager.setCurrentItem(3);
                    titleTxtView.setText(lastSetTitle);
                    commentEnterFragment.clearTextInput();
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

                case 9: //Me (ProfileTab with user == me)
                    toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                    mViewPager.setCurrentItem(0);
                    titleTxtView.setText(lastSetTitle);
                    profileTab.clearProfilePage();
                    break;

                case 10: //currently in SettingsFragment
                    //go back to Profile page (with current logged-in user's info), since SettingsFragment is only accessed from Profile page.
                    //Profile page info should be stored so just go back to it
                    profileTab.restoreUI();
                    mViewPager.setCurrentItem(9);
                    titleTxtView.setText("");
                    enableBottomTabs();
                    break;

                case 11: //currently in MessageRoom fragment
                    toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                    mViewPager.setCurrentItem(0);
                    titleTxtView.setText(lastSetTitle);
                    messageRoom.cleanUp();
                    break;

                //default might be enough to handle case 12 (CreateMessage)
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
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:88614505-c8df-4dce-abd8-79a0543852ff", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        /*
        //these two for signing REST requests to Amazon Elasticsearch
        credentialsProvider.getCredentials().getAWSAccessKeyId()
        credentialsProvider.getCredentials().getAWSSecretKey()
        */

        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        mapper = new DynamoDBMapper(ddbClient);
        s3 = new AmazonS3Client(credentialsProvider);

        userTimecode = sessionManager.getUserTimecode();
        localFns = sessionManager.getFollowingHashSet();
        currUsername = sessionManager.getCurrentUsername();
        userMKey = sessionManager.getMKey();
        profileImageURL = sessionManager.getProfileImageURL();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        nativeAds = new ArrayList<>();
        MobileAds.initialize(this, "ca-app-pub-3940256099942544/2247696110"); //TODO: this loads test ads. Replace the app_id_string with our adMob account app_id_string to get real ads.
        loadNativeAds();

        int usernameHash;
        if(currUsername.length() < 5){
            usernameHash = currUsername.hashCode();
        }
        else{
            String hashIn = "" + currUsername.charAt(0) + currUsername.charAt(currUsername.length() - 2) + currUsername.charAt(1) + currUsername.charAt(currUsername.length() - 1);
            usernameHash = hashIn.hashCode();
        }

        userPath = Integer.toString(usernameHash) + "/" + sessionManager.getCurrentUsername();
        FOLLOWERS_CHILD = userPath + "/f";
        FOLLOWING_CHILD = userPath + "/g";

        //soft input (keyboard) settings
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPagerCustomDuration) findViewById(R.id.container2);
        mViewPager.setScrollDurationFactor(1);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(13);
        mViewPager.setPageTransformer(false, new FadePageTransformer());
        //mViewPager.setPageTransformer(false, new NoPageTransformer());

        vpContainer = (RelativeLayout) findViewById(R.id.vpcontainer);
        vpContainerLP = (RelativeLayout.LayoutParams) vpContainer.getLayoutParams();

        windowSize = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(windowSize);

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
                meClicked = false;
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
                        imm.hideSoftInputFromWindow(toolbarButtonLeft.getWindowToken(), 0);
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
                            if(myAdapterFragInt == 9 && postParentProfileUsername != null){
                                goToProfile(postParentProfileUsername);
                            }
                            else{
                                mViewPager.setCurrentItem(myAdapterFragInt);
                            }
                            xBmp = null;
                            yBmp = null;
                        }
                        break;

                    case 4: //commentEnterFragment
                        imm.hideSoftInputFromWindow(toolbarButtonLeft.getWindowToken(), 0);
                        mViewPager.setCurrentItem(3);
                        titleTxtView.setText(lastSetTitle);
                        commentEnterFragment.clearTextInput();
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
                    //In cases 7 and 8, we disable the toolbarButtonLeft (Search/Up button), so no need to program them for now.
                    /*
                    case 7: //LeaderboardTab

                        break;

                    case 8: //NotificationsTab

                        break;
                    */


                    case 9: //Me (ProfileTab with user == me)
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        profileTab.clearProfilePage();
                        break;

                    case 10: //currently in SettingsFragment
                        //go back to Profile page (with current logged-in user's info), since SettingsFragment is only accessed from Profile page.
                        //Profile page info should be stored so just go back to it
                        profileTab.restoreUI();
                        mViewPager.setCurrentItem(9);
                        titleTxtView.setText("");
                        enableBottomTabs();
                        break;

                    case 11:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        messageRoom.cleanUp();
                        break;

                    default:
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        mViewPager.setCurrentItem(0);
                        titleTxtView.setText(lastSetTitle);
                        break;
                }
            }
        });
        toolbarButtonRight = (ImageButton) mActionBarView.findViewById(R.id.top_right_img_button);
        toolbarButtonRightLP = (RelativeLayout.LayoutParams) toolbarButtonRight.getLayoutParams();
        toolbarButtonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(10);
            }
        });

        toolbarTextButton = (Button) mActionBarView.findViewById(R.id.top_right_text_button);
        toolbarTextButtonLP = (RelativeLayout.LayoutParams) toolbarTextButton.getLayoutParams();
        toolbarTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currPage = mViewPager.getCurrentItem();
                switch (currPage){

                    case 2: //CreatePost
                        toolbarTextButton.setClickable(false);
                        if(createPost.createButtonPressed()){
                            imm.hideSoftInputFromWindow(toolbarButtonLeft.getWindowToken(), 0);
                        }
                        else{
                            toolbarTextButton.setClickable(true);
                        }

                        break;

                    case 4: //CommentEnterFragment
                        //Log.d("buttontest", "button press");
                        toolbarTextButton.setClickable(false);
                        if(commentEnterFragment.submitButtonPressed()){
                            imm.hideSoftInputFromWindow(toolbarButtonLeft.getWindowToken(), 0);
                        }
                        else{
                            toolbarTextButton.setClickable(true);
                        }
                        break;

                    case 12:    //CreateMessage fragment
                        String dmTarget = createMessageFragment.getDMTarget();
                        if(!(dmTarget.equals(""))){
                            RNumAndUList rNumAndUList = mainActivityFragRef.getTab4Messenger().getRNumAndUList(dmTarget);
                            if(rNumAndUList != null){
                                setUpAndOpenMessageRoom(rNumAndUList.getRNum(), rNumAndUList.getUsersList(), dmTarget);
                            }
                            else{
                                createMessageFragment.createMessageRoom();
                            }
                        }
                        else{
                            createMessageFragment.createMessageRoom();
                        }
                        break;
                }
            }
        });

        bottomNavigation = (AHBottomNavigation) findViewById(R.id.bottom_navigation);

        bottomNavLP = (RelativeLayout.LayoutParams) bottomNavigation.getLayoutParams();

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
                        mViewPager.setCurrentItem(7);
                        break;

                    case 2: //NotificationsTab
                        mViewPager.setCurrentItem(8);
                        break;

                    case 3: //Me (ProfileTab with user == me)
                        meClicked = true;
                        profileTab.setUpProfile(sessionManager.getCurrentUsername(), true);
                        mViewPager.setCurrentItem(9);
                        break;

                    default:
                        break;
                }
                return true;
            }
        });
        bottomNavigation.setBehaviorTranslationEnabled(false);

        // Set current item programmatically
        bottomNavigation.setCurrentItem(0, false);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                //handling fragment-based UI operations
                switch(position){
                    //We probably only need to account for when the transition between yes-tab / no-tab happens (tabs 0, 2, 3, 6)
                    //But if it makes no significant performance difference to include all the other tabs, we might as well, to be on safe side especially in case future developments necessitate doing so.
                    case 0: //MainActivity
                        hideToolbarButtonRight();
                        hideToolbarTextButton();
                        enableBottomTabs();
                        titleTxtView.setText(lastSetTitle);
                        bottomNavigation.setCurrentItem(0, false);
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_search_white);
                        break;

                    case 1: //SearchPage
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        disableBottomTabs();
                        break;

                    case 2: //CreatePost
                        hideToolbarButtonRight();
                        showToolbarTextButton("POST");
                        disableBottomTabs();
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        break;

                    case 3: //PostPage
                        hideToolbarButtonRight();
                        hideToolbarTextButton();
                        disableBottomTabs();
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        break;

                    case 4: //CommentEnterFragment
                        //disableBottomTabs();  //accessed from PostPage which already disables bottom tabs
                        hideToolbarButtonRight();
                        showToolbarTextButton("POST");
                        break;

                    case 5: //SelectCategory
                        //disableBottomTabs(); //accessed from CreatePost which already disables bottom tabs
                        break;

                    case 6: //CategoryFragment
                        hideToolbarButtonRight();
                        hideToolbarTextButton();
                        enableBottomTabs();
                        bottomNavigation.setCurrentItem(0, false);
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        break;

                    case 7: //LeaderboardTab
                        //enableBottomTabs();   //Leaderboard is accessible through bottom tabs only so bottom tabs are already enabled
                        hideToolbarButtonRight();
                        hideToolbarTextButton();
                        hideToolbarButtonLeft();
                        titleTxtView.setText("Leaderboard");
                        bottomNavigation.setCurrentItem(1, false);
                        break;

                    case 8: //NotificationsTab
                        //enableBottomTabs();   //Notifications is accessible through bottom tabs only so bottom tabs are already enabled
                        hideToolbarButtonRight();
                        hideToolbarTextButton();
                        enableBottomTabs(); //because we might make notifications not bottom tab, putting this here just in case
                        hideToolbarButtonLeft();
                        titleTxtView.setText("Notifications");
                        bottomNavigation.setCurrentItem(2, false);
                        break;

                    case 9: //Me (ProfileTab with user == me)
                        titleTxtView.setText("");
                        hideToolbarTextButton();
                        if(meClicked){
                            showSettingsButton();
                            hideToolbarButtonLeft();
                            enableBottomTabs(); //we need to recover bottom tabs in cases like coming back from SettingsFragment
                            bottomNavigation.setCurrentItem(3, false);
                        }
                        else{
                            hideToolbarButtonRight();
                            showToolbarButtonLeft();
                            toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                            disableBottomTabs();
                        }

                        break;

                    case 10: //SettingsFragment
                        titleTxtView.setText("Settings");
                        hideToolbarButtonRight();
                        hideToolbarTextButton();
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        disableBottomTabs();
                        break;

                    case 11:
                        hideToolbarButtonRight();
                        hideToolbarTextButton();
                        disableBottomTabs();
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        break;

                    case 12:
                        hideToolbarButtonRight();
                        showToolbarTextButton("OK");
                        disableBottomTabs();
                        showToolbarButtonLeft();
                        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
                        break;

                    default:

                        break;
                }

            }
        });

        setToolbarTitleTextForTabs("Newsfeed");
        goToMainActivityOnResume = true;

        //Log.d("USER_INFO", sessionManager.getUserDetails().get(SessionManager.KEY_USERNAME));
        Log.d("ORDER", "MainContainer OnCreate finished");
    }

    @Override
    protected void onResume(){
        Log.d("ORDER", "MainContainer onResume called");
        super.onResume();
        FirebaseMessaging.getInstance().subscribeToTopic(currUsername); //subscribe to user topic for messenger push notification

        if(getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().get("rnum") != null){
            String intentRNum = getIntent().getExtras().get("rnum").toString();
            getIntent().removeExtra("rnum");
            goToMsgRoom(intentRNum);
        }
        else if(goToMainActivityOnResume){
            mViewPager.setCurrentItem(0);
            goToMainActivityOnResume = false;
        }
    }

    private void goToMsgRoom(final String rnum){
        Log.d("OPENROOM", "going to room# " + rnum);
        mViewPager.setCurrentItem(11);

        //TODO: show a indeterminate progress bar while message room is getting set up

        if(messageRoom == null){
            Log.d("OPENROOM", "message room null");
        }
        else {
            String rPath = userPath + "/r/" + rnum;
            mFirebaseDatabaseReference.child(rPath).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getValue() != null){
                        RoomObject clickedRoom = dataSnapshot.getValue(RoomObject.class);
                        messageRoom.setUpRoom(rnum, clickedRoom.getUsers(), clickedRoom.getName());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }


    @Override
    protected void onNewIntent(Intent intent){
        if(intent != null){
            setIntent(intent); //this intent will subsequently be handled by onResume()
        }
    }


    private void showToolbarButtonLeft(){
        toolbarButtonLeft.setEnabled(true);
        toolbarButtonLeft.setVisibility(View.VISIBLE);
    }
    private void hideToolbarButtonLeft(){
        toolbarButtonLeft.setEnabled(false);
        toolbarButtonLeft.setVisibility(View.INVISIBLE);
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

    public void setMessageRoomTitle(String title){
        titleTxtView.setText(title);
    }

    public void setLeftChevron(){
        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
    }

    public void setToolbarTitleForCF(String titleForCF){
        titleTxtView.setText(titleForCF);
    }

    public ImageButton getToolbarButtonLeft(){
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
            messageRoom = new MessageRoom(); //we create it here because we might need it initiated right away (instead of waiting for getItem to be called) for messenger push notification click
        }

        @Override
        public Fragment getItem(int position) {
            //Return current tabs
            switch (position) {
                case 0:
                    mainActivityFragRef = new MainActivity();
                    return mainActivityFragRef;
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
                case 7:
                    return new LeaderboardTab();
                case 8:
                    return new NotificationsTab();
                case 9:
                    profileTab = new ProfileTab();
                    return profileTab;
                case 10:
                    return new SettingsFragment();
                case 11:
                    //messageRoom = new MessageRoom(); //now messageRoom is initiated in the SectionPagerAdapter constructor.
                    return messageRoom;
                case 12:
                    createMessageFragment = new CreateMessage();
                    return createMessageFragment;
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            return 13;
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
                case 7:
                    return "LEADERBOARD";
                case 8:
                    return "NOTIFICATIONS";
                case 9:
                    return "ME";
                case 10:
                    return "SETTINGS";
                case 11:
                    return "MESSENGER";
                case 12:
                    return "NEW MESSAGE";
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
    public void postClicked(Post post, int fragmentInt){
        String temp = postInDownload.get(post.getPost_id());
        if(temp == null || !temp.equals("in progress")){
            postInDownload.put(post.getPost_id(), "in progress");
            postPage.clearList();
            postPage.setContent(post);
        }
        toolbarButtonLeft.setImageResource(R.drawable.ic_left_chevron);
        myAdapterFragInt = fragmentInt;
        postParentProfileUsername = post.getAuthor();
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
        return (currFragIndex == 0  || currFragIndex == 1 || currFragIndex == 6 || currFragIndex == 9); //MainActivity, Search, Category, or Me (Profile)
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
        bottomNavigation.setLayoutParams(bottomNavLP);
        vpContainer.setLayoutParams(vpContainerLP);
    }

    private void disableBottomTabs(){
        bottomNavigation.hideBottomNavigation(false);
        bottomNavigation.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        vpContainer.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
    }

    public int getUserTimecode(){
        return userTimecode;
    }

    public String getUsername(){
        if(currUsername != null){
            return currUsername;
        }
        else{
            return sessionManager.getCurrentUsername();
        }
    }

    public void goToProfile(String username){
        if(username.equals(sessionManager.getCurrentUsername())){
            meClicked = true;
            profileTab.setUpProfile(username, true);
            mViewPager.setCurrentItem(9);
        }
        else{
            meClicked = false;
            profileTab.setUpProfile(username, false);
            mViewPager.setCurrentItem(9);
        }
    }

    public boolean isFollowing(String username){
        return createMessageFragment.isFollowing(username);
    }

    public boolean followedBy(String username){
        return createMessageFragment.followedBy(username);
    }

    public int getFollowingNum(){
        return createMessageFragment.getFollowingCount();
    }

    public int getFollowerNum(){
        return createMessageFragment.getFollowerCount();
    }

    private void showSettingsButton(){
        toolbarButtonRight.setEnabled(true);
        toolbarButtonRight.setLayoutParams(toolbarButtonRightLP);
        toolbarButtonRight.setImageResource(R.drawable.ic_settings_white_24dp);

    }
    private void hideToolbarButtonRight(){
        toolbarButtonRight.setEnabled(false);
        toolbarButtonRight.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }

    public void sessionLogOut(){
        FirebaseMessaging.getInstance().unsubscribeFromTopic(currUsername); //unsubscribe from user topic for messenger push notification
        FirebaseAuth.getInstance().signOut();
        sessionManager.logoutUser();
    }

    public void meClickTrue(){
        meClicked = true;
    }

    public String getUserMKey(){
        return userMKey;
    }

    public void setUpAndOpenMessageRoom(String rnum, ArrayList<String> usersMap, String roomTitle){
        messageRoom.setUpRoom(rnum, usersMap, roomTitle);
        mViewPager.setCurrentItem(11);
    }

    public String getProfileImageURL(){
        return profileImageURL;
    }

    private void hideToolbarTextButton(){
        toolbarTextButton.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        toolbarTextButton.setText("");
        toolbarTextButton.setVisibility(View.INVISIBLE);
        toolbarTextButton.setClickable(false);
        toolbarTextButton.setEnabled(false);
    }

    private void showToolbarTextButton(String buttonText){
        toolbarTextButton.setEnabled(true);
        toolbarTextButton.setClickable(true);
        toolbarTextButton.setVisibility(View.VISIBLE);
        toolbarTextButton.setText(buttonText);
        toolbarTextButton.setLayoutParams(toolbarTextButtonLP);
    }

    public MessageRoom getMessageRoom(){
        return messageRoom;
    }

    public String getUserPath(){
        return userPath + "/";
    }

    public String getBday(){
        return sessionManager.getBday();
    }

    public boolean isInMessageRoom(){
        return mViewPager.getCurrentItem() == 11;
    }

    public AWSCredentials getCred(){
        return credentialsProvider.getCredentials();
    }

    private void loadNativeAds(){
        AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
                .forAppInstallAd(new OnAppInstallAdLoadedListener() {
                    @Override
                    public void onAppInstallAdLoaded(NativeAppInstallAd appInstallAd) {
                        nativeAds.add(appInstallAd);
                    }
                })
                .forContentAd(new OnContentAdLoadedListener() {
                    @Override
                    public void onContentAdLoaded(NativeContentAd contentAd) {
                        nativeAds.add(contentAd);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, and so on.
                        //TODO: handle ad loading failure event
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .setImageOrientation(NativeAdOptions.ORIENTATION_LANDSCAPE)
                        .build())
                .build();

        adLoader.loadAds(new AdRequest.Builder().build(), 5); //TODO: use keywords on the AdRequest.Builder to get targeted ads. for now, it's location based generic ads I believe.
    }

    public NativeAd getNextAd(){
        if(nativeAds == null || nativeAds.isEmpty()){
            return null; //TODO: I hear returning null is not the best design pattern, so let's see if we can refactor this.
        } else {
            NativeAd nextAd = nativeAds.get(0);
            nativeAds.remove(0);
            if(nativeAds.isEmpty()){
                loadNativeAds();
            }
            return nextAd;
        }
    }

    public Post getCurrentPost(){
        return getPostPage().getCurrentPost();
    }

    public String getESHost(){
        return esHost;
    }
    public String getESRegion(){
        return esRegion;
    }

    public URL getImgURI(String bucket, String key) throws URISyntaxException{
        long expirationTime = System.currentTimeMillis() + 86400000; //24 hours from current time
        return s3.generatePresignedUrl(bucket, key, new Date(expirationTime));
    }

    public int getImageWidthPixels(){
        return 696;
    }

    public int getImageHeightPixels(){
        return 747;
    }

    public void closeSoftKeyboard(){
        imm.hideSoftInputFromWindow(toolbarButtonLeft.getWindowToken(), 0);
    }

}
