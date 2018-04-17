package com.vs.bcd.versus.model;

/**
 * Created by dlee on 11/29/17.
 */

public class LeaderboardEntry {
    private String username;
    private int points;
    private int g,s,b; //medal counts for top 3

    public LeaderboardEntry(String username, int points){
        this.username = username;
        this.points = points;
    }

    public LeaderboardEntry(String username, int points, int g, int s, int b){
        this.username = username;
        this.points = points;
        this.g = g;
        this.s = s;
        this.b = b;
    }

    public void setMedalCount(int g, int s, int b){
        this.g = g;
        this.s = s;
        this.b = b;
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

    public int getG(){
        return g;
    }

    public int getS(){
        return s;
    }

    public int getB(){
        return b;
    }
}
