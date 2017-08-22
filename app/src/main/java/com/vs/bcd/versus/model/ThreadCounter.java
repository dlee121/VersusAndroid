package com.vs.bcd.versus.model;

import com.vs.bcd.versus.fragment.Tab1Newsfeed;

/**
 * Created by dlee on 8/21/17.
 */

public class ThreadCounter {

    private int n, limit;
    private Tab1Newsfeed tab1;

    public ThreadCounter (int n, int limit, Tab1Newsfeed tab1){
        this.n = n;
        this.limit = limit;
        this.tab1 = tab1;
    }

    public void increment(){

        n++;
        if(n == limit){
            tab1.yesDisplayResults();
        }
    }

    public int getVal(){
        return n;
    }
}
