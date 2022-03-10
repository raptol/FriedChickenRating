package com.example.friedchickenrating.fragments.roulette;

import com.google.firebase.Timestamp;

import java.util.Map;

public class Restaurant {
    private String rouletteId; // id
    private String restaurantName; //restaurant name
    private String placeid; //place id
    private String userid; //user id
    private Map<String, Object> pictures; //picture url
    private Timestamp timestamp;

    public Restaurant() { }
    public Restaurant(String rouletteId, String restaurantName, String placeid, String userid, Map<String, Object> pictures, Timestamp timestamp) {
        this.rouletteId = rouletteId;
        this.restaurantName = restaurantName;
        this.placeid = placeid;
        this.userid = userid;
        this.pictures = pictures;
        this.timestamp = timestamp;
    }

    public String getRouletteId() {
        return rouletteId;
    }

    public void setRouletteId(String rouletteId) {
        this.rouletteId = rouletteId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getPlaceid() {
        return placeid;
    }

    public void setPlaceid(String placeid) {
        this.placeid = placeid;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public Map<String, Object> getPictures() {
        return pictures;
    }

    public void setPictures(Map<String, Object> pictures) {
        this.pictures = pictures;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
