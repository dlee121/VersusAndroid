package com.vs.bcd.versus.model;

import com.vs.bcd.api.model.LeaderboardModelHitsHitsItemSource;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dlee on 11/29/17.
 */

public class LeaderboardEntry {
    private String username;
    private int influence;
    private int g,s,b; //medal counts for top 3
    private int pi;

    public LeaderboardEntry(String username, int influence){
        this.username = username;
        this.influence = influence;
        g = 0;
        s = 0;
        b = 0;
        pi = 0;
    }

    public LeaderboardEntry(JSONObject item, String id) throws JSONException{
        username = id;
        b = item.getInt("b");
        g = item.getInt("g");
        influence = item.getInt("in");
        pi = item.getInt(("pi"));
        s = item.getInt("s");
    }

    public LeaderboardEntry(LeaderboardModelHitsHitsItemSource source, String id) throws JSONException{
        username = id;
        b = source.getB().intValue();
        g = source.getG().intValue();
        influence = source.getIn().intValue();
        pi = source.getPi().intValue();
        s = source.getS().intValue();
    }

    public String getUsername(){
        return username;
    }

    public int getInfluence(){
        return influence;
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

    public int getPi(){
        return pi;
    }
}
