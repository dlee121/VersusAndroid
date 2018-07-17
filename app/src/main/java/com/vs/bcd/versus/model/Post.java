package com.vs.bcd.versus.model;

import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;
import com.vs.bcd.api.model.PostModelSource;
import com.vs.bcd.api.model.PostPutModel;
import com.vs.bcd.api.model.PostsListCompactModelHitsHitsItemSource;
import com.vs.bcd.api.model.PostsListModelHitsHitsItemSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.text.ParseException;

public class Post {


    //TODO: implement thumbnails
    //question, author, time, {thumbnail1, thumbnail2}*, viewcount, redname, redcount, blackname, blackcount, category
    private String question;
    private String author;
    private String time;
    //private int viewcount; instead of viewcount we'll do votecount, which is redcount + blackcount
    private String redname;
    private int redcount;
    private String blackname;
    private int blackcount;
    private int category;
    private String post_id;
    private int redimg; //0 = default (no image), 1 = s3 image
    private int blackimg;
    private int pt; //last modified time in minutes since epoch, used for popularity score calculation
    private BigDecimal ps; //popularity score
    private double popularityVelocity = 0;  //May move this to ActivePost if we don't plan on using this for Post as well.

    private NativeAppInstallAd NAI; //TODO: rename to AppInstallAd and rename relevant functions accordingly
    private NativeContentAd NC;     //TODO: rename to ContentAd and rename relevant functions accordingly

    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());


    public String getCategoryString(){
        switch(category){
            case 0:
                return "Automobiles";
            case 1:
                return "Cartoon/Anime/Fiction";
            case 2:
                return "Celebrity/Gossip";
            case 3:
                return "Culture";
            case 4:
                return "Education";
            case 5:
                return "Electronics";
            case 6:
                return "Fashion";
            case 7:
                return "Finance";
            case 8:
                return "Food/Restaurant";
            case 9:
                return "Game/Entertainment";
            case 10:
                return "Morality/Ethics/Law";
            case 11:
                return "Movies/TV";
            case 12:
                return "Music/Artists";
            case 13:
                return "Politics";
            case 14:
                return "Random";
            case 15:
                return "Religion";
            case 16:
                return "Science";
            case 17:
                return "Social Issues";
            case 18:
                return "Sports";
            case 19:
                return "Technology";
            case 20:
                return "Weapons";
            case 42069:
                return "NATIVE APP INSTALL AD";
            case 69420:
                return "NATIVE CONTENT AD";
            default:
                return "N/A";
        }
    }

    public String getPost_id(){
        return  post_id;
    }

    public String getQuestion() {
        //TODO: since ddb doesn't hold empty strings, empty question will come as null (as in setTopic will not execute on those since the question column doesn't exist for those posts without question. So handle such cases when question is null return empty string
        //TODO: nah actualy question should be required. implement that through form validation
        return question;
    }
    public void setQuestion(String topic) {
        this.question = topic;
    }

    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }

    public String getRedname() {
        return redname;
    }
    public void setRedname(String redname) {
        this.redname = redname;
    }

    public int getRedcount() {
        return redcount;
    }

    public String getBlackname() {
        return blackname;
    }
    public void setBlackname(String blackname) {
        this.blackname = blackname;
    }

    public int getBlackcount() {
        return blackcount;
    }

    public int getCategory() {
        return category;
    }
    public void setCategory(int category) {
        this.category = category;
    }

    public int getRedimg(){
        return redimg;
    }
    public void setRedimg(int redimg){
        this.redimg = redimg;
    }

    public int getBlackimg(){
        return blackimg;
    }
    public void setBlackimg(int blackimg){
        this.blackimg = blackimg;
    }

    public int getPt(){
        return pt;
    }
    public void setPt(int pt){
        this.pt = pt;
    }

    public BigDecimal getPs(){
        return ps;
    }
    public void setPs(BigDecimal ps){
        this.ps = ps;
    }

    public String getCategoryIntAsString(){
        return Integer.toString(category);
    }


    public Post(){
        redcount = 0;
        blackcount = 0;
        pt = (int)((((System.currentTimeMillis()/1000)/60)/60)/24); //this casting is safe because there won't be an overflow for at least 4000 years
        ps = BigDecimal.valueOf(0.0);
        time = df.format(new Date());
        post_id = UUID.randomUUID().toString().replace("-", ""); //generate random UUID as post_id when post is created
    }

    public Post(PostsListModelHitsHitsItemSource source, String id){
        question = source.getQ();
        author = source.getA();
        time = source.getT();
        redname = source.getRn();
        redcount = source.getRc().intValue(); //TODO: will we ever need to change this to intValueExact()?
        blackname  = source.getBn();
        blackcount = source.getBc().intValue();
        category = source.getC().intValue();
        post_id = id;
        redimg = source.getRi().intValue();
        blackimg = source.getBi().intValue();
        pt = source.getPt().intValue();
        ps = source.getPs();
    }

    public Post(PostModelSource source, String id){
        question = source.getQ();
        author = source.getA();
        time = source.getT();
        redname = source.getRn();
        redcount = source.getRc().intValue(); //TODO: will we ever need to change this to intValueExact()?
        blackname  = source.getBn();
        blackcount = source.getBc().intValue();
        category = source.getC().intValue();
        post_id = id;
        redimg = source.getRi().intValue();
        blackimg = source.getBi().intValue();
        pt = source.getPt().intValue();
        ps = source.getPs();
    }

    public Post(PostsListCompactModelHitsHitsItemSource source, String id, String author){
        question = source.getQ();
        time = source.getT();
        redname = source.getRn();
        redcount = source.getRc().intValue();
        blackname  = source.getBn();
        blackcount = source.getBc().intValue();
        this.author = author;
        post_id = id;
    }

    public Post(PostsListCompactModelHitsHitsItemSource source, String id){
        question = source.getQ();
        time = source.getT();
        redname = source.getRn();
        redcount = source.getRc().intValue();
        blackname  = source.getBn();
        blackcount = source.getBc().intValue();
        author = source.getA();
        post_id = id;
    }

    public Post(JSONObject postObj, String id, boolean compact) throws JSONException{
        if(!compact){
            question = postObj.getString("q");
            author = postObj.getString("a");
            time = postObj.getString("t");
            redname = postObj.getString("rn");
            redcount = postObj.getInt("rc");
            blackname  = postObj.getString("bn");
            blackcount = postObj.getInt("bc");
            category = postObj.getInt("c");
            post_id = id;
            redimg = postObj.getInt("ri");
            blackimg = postObj.getInt("bi");
            pt = postObj.getInt("pt");
            ps = BigDecimal.valueOf(postObj.getDouble("ps"));
        }
        else{
            question = postObj.getString("q");
            time = postObj.getString("t");
            redname = postObj.getString("rn");
            redcount = postObj.getInt("rc");
            blackname  = postObj.getString("bn");
            blackcount = postObj.getInt("bc");
            author = postObj.getString("a");
            post_id = id;
        }
    }

    public void incrementRedCount(){
        redcount++;
    }
    public void decrementRedCount(){
        redcount--;
    }
    public void incrementBlackCount(){
        blackcount++;
    }
    public void decrementBlackCount(){
        blackcount--;
    }

    public Date getDate(){
        try{
            return df.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public NativeAppInstallAd getNAI(){
        return NAI;
    }
    public void setNAI(NativeAppInstallAd NAI){
        this.NAI = NAI;
    }

    public NativeContentAd getNC(){
        return NC;
    }
    public void setNC(NativeContentAd NC){
        this.NC = NC;
    }

    public int getVotecount(){
        return redcount + blackcount;
    }

    public void copyPostInfo(Post src){
        question = src.getQuestion();
        author = src.getAuthor();
        time = src.getTime();
        redname = src.getRedname();
        redcount = src.getRedcount();
        blackname  = src.getBlackname();
        blackcount = src.getBlackcount();
        category = src.getCategory();
        post_id = src.getPost_id();
        redimg = src.getRedimg();
        blackimg = src.getBlackimg();
        pt = src.getPt();
        ps = src.getPs();
    }

    public PostPutModel getPutModel(){
        PostPutModel putModel = new PostPutModel();
        putModel.setQ(question);
        putModel.setA(author);
        putModel.setT(time);
        putModel.setRn(redname);
        putModel.setRc(BigDecimal.ZERO);
        putModel.setBn(blackname);
        putModel.setBc(BigDecimal.ZERO);
        putModel.setC(BigDecimal.valueOf(category));
        putModel.setRi(BigDecimal.valueOf(redimg));
        putModel.setBi(BigDecimal.valueOf(blackimg));
        putModel.setPt(BigDecimal.valueOf(pt));
        putModel.setPs(ps);

        return putModel;
    }

}