package com.vs.bcd.versus.model;

/**
 * Created by dlee on 10/13/17.
 */

public class RoomObject {

    private String id;
    private String name;
    private Long time;
    private String preview;

    //TODO: make variables for rest of room info data, for now we just have name, time , preview to get it going

    public RoomObject() {
    }

    public RoomObject(String title, Long time, String preview) {
        this.name = title;
        this.time = time;
        this.preview = preview;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
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

}
