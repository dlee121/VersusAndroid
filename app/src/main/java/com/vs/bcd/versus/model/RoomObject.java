package com.vs.bcd.versus.model;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dlee on 10/13/17.
 */

public class RoomObject {

    private String name;
    private Long time;
    private String preview;
    private ArrayList<String> users;

    //TODO: make variables for rest of room info data, for now we just have name, time , preview to get it going

    public RoomObject() {
    }

    public RoomObject(String title, Long time, String preview, String rnum, ArrayList<String> users) {
        this.name = title;
        this.time = time;
        this.preview = preview;
        this.users = users;
    }

    public RoomObject(Long time, String preview, String rnum, ArrayList<String> users) {
        this.time = time;
        this.preview = preview;
        this.users = users;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public boolean hasName(){
        return name != null;
    }

    public Long getTime(){
        return time;
    }

    public void setTime(Long time){
        this.time = time;
    }

    public String getPreview(){
        return preview;
    }

    public void setPreview(String preview){
        this.preview = preview;
    }

    public ArrayList<String> getUsers(){
        return users;
    }

    public void setUsers(ArrayList<String> users){
        this.users = users;
    }

}
