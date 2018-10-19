package com.vs.bcd.versus.activity;

import android.content.Context;
import android.support.design.widget.TabLayout;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.Tab1MyCircle;
import com.vs.bcd.versus.fragment.Tab2Trending;
import com.vs.bcd.versus.fragment.Tab3New;
import com.vs.bcd.versus.model.Post;

import java.util.ArrayList;

public class MainActivity extends Fragment {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    private View rootView;
    private boolean childrenFragmentsUIActive = false;
    private ArrayList<View> childViews;
    private ArrayList<ViewGroup.LayoutParams> LPStore;
    private LinearLayout tabStrip;
    private MainContainer mainContainer;
    //private FloatingActionButton fab;
    private Tab1MyCircle tab1 = new Tab1MyCircle();
    private Tab2Trending tab2 = new Tab2Trending();
    private Tab3New tab3 = new Tab3New();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_main, container, false);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = rootView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        mainContainer = (MainContainer)getActivity();

        TabLayout tabLayout = rootView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        //tabStrip = ((LinearLayout)tabLayout.getChildAt(0));
        //set tab icons

        tabLayout.getTabAt(0).setText("My Circle");
        tabLayout.getTabAt(1).setText("Trending");
        tabLayout.getTabAt(2).setText("New");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: //my circle
                        tab.setText("My Circle");
                        mainContainer.setToolbarTitleTextForTabs("");
                        //enableCPFab();
                        if(tab1 != null && !tab1.postsLoaded()){
                            tab1.newsfeedESQuery(0);
                        }
                        //mainContainer.setLeftSearchButton();

                        break;
                    case 1: //trending
                        tab.setText("Trending");
                        mainContainer.setToolbarTitleTextForTabs("");
                        //enableCPFab();
                        if(tab2 != null && !tab2.postsLoaded()){
                            tab2.trendingESQuery(0);
                        }
                        //mainContainer.setLeftSearchButton();
                        break;
                    case 2: //new
                        tab.setText("New");
                        mainContainer.setToolbarTitleTextForTabs("");
                        //enableCPFab();
                        if(tab3 != null && !tab3.postsLoaded()){
                            tab3.newsfeedESQuery(0);
                        }
                        break;
                    default:
                        //tab.setIcon(R.drawable.newsfeed_red);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                /*
                switch (tab.getPosition()) {
                    case 0: //newsfeed
                        tab.setIcon(R.drawable.newsfeed_blue);
                        break;
                    case 1: //trending
                        tab.setIcon(R.drawable.fire_blue);
                        break;
                    case 2: //categories
                        tab.setIcon(R.drawable.categories_blue);
                        break;
                    default:
                        tab.setIcon(R.drawable.newsfeed_blue);
                        break;
                }
                */

            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        childViews = new ArrayList<>();
        LPStore = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
            LPStore.add(childViews.get(i).getLayoutParams());
        }

        mViewPager.setCurrentItem(0);
        return rootView;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
    /*
        //Search_Main button was pressed
        if (id == R.id.action_search_main) {
            //TODO:Activate Search UI and Implement Main Search function
            return true;
        }
    */
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
                    //tab1 = new Tab1MyCircle();
                    return tab1;
                case 1:
                    //tab2 = new Tab2Trending();
                    return tab2;
                case 2:
                    //tab3 = new Tab3New();
                    return tab3;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

    public ViewPager getViewPager(){
        return mViewPager;
    }


    public boolean getUILifeStatus(){
        return childrenFragmentsUIActive;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            childrenFragmentsUIActive = true;
            if(rootView != null){
                //enableChildViews();
                rootView.setVisibility(View.VISIBLE);
                //rootView.bringToFront();

                /*
                int currItem = mViewPager.getCurrentItem();
                if(currItem == 2 ){
                    disableCPFab();
                }
                */
            }
        }
        else{
            childrenFragmentsUIActive = false;
            if (rootView != null) {
                //disableChildViews();
                rootView.setVisibility(View.GONE);
            }

        }
    }

    public void enableChildViews(){
        /* commented these out since resetCatSelection handles these operations now
        redimgSet = "default";
        blackimgSet = "default";
        */
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);
            childViews.get(i).setLayoutParams(LPStore.get(i));

        }
    }

    public void disableChildViews(){
        Log.d("disabling", "This many: " + Integer.toString(childViews.size()));
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);
            childViews.get(i).setLayoutParams(new RelativeLayout.LayoutParams(0,0));
        }
    }
    /*
    private void enableCPFab(){
        fab.setEnabled(true);
        fab.setClickable(true);
        fab.setLayoutParams(fabLP);
    }

    private void disableCPFab(){
        fab.setEnabled(false);
        fab.setClickable(false);
        fab.setLayoutParams(new RelativeLayout.LayoutParams(0,0));
    }
    */

    public Tab1MyCircle getTab1(){
        return tab1;
    }

    public Tab2Trending getTab2() {
        return tab2;
    }

    public void addPostToTop(Post post, int tabNum){
        if(tabNum == 0){
            if(tab1 != null){
                tab1.addPostToTop(post);
            }
        }
        else if(tabNum == 2){
            if(tab3 != null){
                tab3.addPostToTop(post);
            }
        }

    }

    public Tab3New getTab3() {
        return tab3;
    }
}