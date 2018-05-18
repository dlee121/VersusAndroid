package com.vs.bcd.versus.model;

import java.util.ArrayList;
import java.util.HashMap;

import static android.R.attr.name;

/**
 * Created by dlee on 10/13/17.
 */

public class MedalUpdateRequest {

    private int p; //points increment
    private long t; //timestamp in seconds from epoch
    private String c; //sanitized comment content

    public MedalUpdateRequest() {
    }

    public MedalUpdateRequest(int p, long t, String c) {
        this.p = p;
        this.t = t;
        this.c = c;
    }


    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public long getT(){
        return t;
    }
    public void setT(long t){
        this.t = t;
    }

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
    }
}