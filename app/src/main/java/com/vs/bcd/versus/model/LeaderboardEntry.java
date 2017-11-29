package com.vs.bcd.versus.model;

/**
 * Created by dlee on 11/29/17.
 */

public class LeaderboardEntry {
    private String username;
    private int points;

    public LeaderboardEntry(String username, int points){
        this.username = username;
        this.points = points;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public int getPoints(){
        return points;
    }

    public void setPoints(int points){
        this.points = points;
    }

}
