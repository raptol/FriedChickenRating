package com.example.friedchickenrating.fragments.favorites;

public class Favorite {
    private String id; //favorite id
    private String userid; //user id
    private String ratingid; //rating id

    public Favorite() {
    }

    public Favorite(String id, String userid, String ratingid) {
        this.id = id;
        this.userid = userid;
        this.ratingid = ratingid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getRatingid() {
        return ratingid;
    }

    public void setRatingid(String ratingid) {
        this.ratingid = ratingid;
    }
}
