package com.vs.bcd.versus.activity;

import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.vs.bcd.versus.R;
import com.vs.bcd.versus.fragment.Tab1Newsfeed;
import com.vs.bcd.versus.fragment.Tab2Trending;
import com.vs.bcd.versus.fragment.Tab3RandomVS;
import com.vs.bcd.versus.fragment.Tab4Categories;

import java.util.ArrayList;

public class MainActivity extends Fragment {
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
    private View rootView;
    private boolean childrenFragmentsUIActive = false;
    private ArrayList<View> childViews;
    private LinearLayout tabStrip;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_main, container, false);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) rootView.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabStrip = ((LinearLayout)tabLayout.getChildAt(0));
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

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainContainer)getActivity()).getViewPager().setCurrentItem(2);
                ((MainContainer)getActivity()).getToolbarTitleText().setText("Create Post");
                ((MainContainer)getActivity()).getToolbarButton().setImageResource(R.drawable.ic_left_chevron);
                ((MainContainer)getActivity()).getCreatePostFragment().resetCatSelection();
            }
        });

        childViews = new ArrayList<>();
        for (int i = 0; i<((ViewGroup)rootView).getChildCount(); i++){
            childViews.add(((ViewGroup)rootView).getChildAt(i));
        }

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
    }

    public ViewPager getViewPager(){
        return mViewPager;
    }

    public DynamoDBMapper getMapper(){
        return mapper;
    }

    public boolean getUILifeStatus(){
        return childrenFragmentsUIActive;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            childrenFragmentsUIActive = true;
            if(rootView != null)
                enableChildViews();
        }
        else{
            childrenFragmentsUIActive = false;
            if (rootView != null)
                disableChildViews();
        }
    }

    public void enableChildViews(){
        tabStrip.setEnabled(true);
        for(int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setClickable(true);
        }
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(true);
            childViews.get(i).setClickable(true);

        }
    }

    public void disableChildViews(){
        tabStrip.setEnabled(false);
        for(int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setClickable(false);
        }
        for(int i = 0; i<childViews.size(); i++){
            childViews.get(i).setEnabled(false);
            childViews.get(i).setClickable(false);

        }
    }

}