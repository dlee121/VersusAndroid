package com.vs.bcd.versus.model;

import java.util.ArrayList;

/**
 * Created by dlee on 11/17/17.
 */

public class RNumAndUList {
    private String rnum;
    private ArrayList<String > usersList;

    public RNumAndUList(String rnum, ArrayList<String > usersList){
        this.rnum = rnum;
        this.usersList = usersList;
    }

    public String getRNum(){
        return rnum;
    }
    public void setRnum(String rnum){
        this.rnum = rnum;
    }

    public ArrayList<String > getUsersList(){
        return usersList;
    }
    public void setUsersList(ArrayList<String> usersList){
        this.usersList = usersList;
    }



}
