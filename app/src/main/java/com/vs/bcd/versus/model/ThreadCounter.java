package com.vs.bcd.versus.model;

import com.vs.bcd.versus.fragment.Tab1Newsfeed;
import com.vs.bcd.versus.fragment.Tab2Trending;

/**
 * Created by dlee on 8/21/17.
 */

public class ThreadCounter {

    private int n, limit;
    private int tabNumber = 0;
    private Tab1Newsfeed tab1;
    private Tab2Trending tab2;

    public ThreadCounter (int n, int limit, Tab1Newsfeed tab1){
        this.n = n;
        this.limit = limit;
        this.tab1 = tab1;
        tabNumber = 1;
    }

    public ThreadCounter (int n, int limit, Tab2Trending tab2){
        this.n = n;
        this.limit = limit;
        this.tab2 = tab2;
        tabNumber = 2;
    }

    public void increment(){

        n++;
        if(n == limit){
            switch (tabNumber){
                case 1:
                    tab1.yesDisplayResults();
                    break;
                case 2:
                    tab2.yesDisplayResults();
                    break;
                default:
                    break;
            }
        }
    }

    public int getVal(){
        return n;
    }
}
