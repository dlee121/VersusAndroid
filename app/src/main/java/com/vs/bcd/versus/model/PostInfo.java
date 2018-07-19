package com.vs.bcd.versus.model;

/**
 * Created by dlee on 2/2/18.
 */

public class PostInfo {
    String r;
    String b;
    String q;
    String a;
    int rc, bc;

    public PostInfo(){
    }

    public void setAQRCBC(String a, String q, int rc, int bc){
        this.a = a;
        this.q = q;
        this.rc = rc;
        this.bc = bc;
    }

    public void setRB(String r, String b){
        this.r = r;
        this.b = b;
    }

    public String getR(){
        return r;
    }

    public String getB(){
        return b;
    }

    public String getQ(){
        return q;
    }

    public String getA() {
        return a;
    }

    public int getRc() {
        return rc;
    }

    public int getBc() {
        return bc;
    }
}
