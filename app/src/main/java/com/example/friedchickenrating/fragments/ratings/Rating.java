package com.example.friedchickenrating.fragments.ratings;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.Map;

public class Rating {
    private String id; //rating id
    private String title; //menu title
    private String type; //chicken type
    private String placeid; //place id
    private String userid; //user id
    private String otheritems;
    private String notes;
    private Map<String, Object> pictures; //picture url
    private float starflavor;
    private float starcrunch;
    private float starspiciness;
    private float starportion;
    private float starprice;
    private float staroverall;
    private Timestamp timestamp;

    public Rating() { }
    public Rating(String id, String title, String type, String placeid, String userid,
                  String otheritems, String notes, Map<String, Object> pictures,
                  float starflavor, float starcrunch, float starspiciness, float starportion, float starprice, float staroverall, Timestamp timestamp) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.placeid = placeid;
        this.userid = userid;
        this.otheritems = otheritems;
        this.notes = notes;
        this.pictures = pictures;
        this.starflavor = starflavor;
        this.starcrunch = starcrunch;
        this.starspiciness = starspiciness;
        this.starportion = starportion;
        this.starprice = starprice;
        this.staroverall = staroverall;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getOtheritems() {
        return otheritems;
    }

    public void setOtheritems(String otheritems) {
        this.otheritems = otheritems;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Map<String, Object> getPictures() {
        return pictures;
    }

    public void setPictures(Map<String, Object> pictures) {
        this.pictures = pictures;
    }

    public float getStarflavor() {
        return starflavor;
    }

    public void setStarflavor(float starflavor) {
        this.starflavor = starflavor;
    }

    public float getStarcrunch() {
        return starcrunch;
    }

    public void setStarcrunch(float starcrunch) {
        this.starcrunch = starcrunch;
    }

    public float getStarspiciness() {
        return starspiciness;
    }

    public void setStarspiciness(float starspiciness) {
        this.starspiciness = starspiciness;
    }

    public float getStarportion() {
        return starportion;
    }

    public void setStarportion(float starportion) {
        this.starportion = starportion;
    }

    public float getStarprice() {
        return starprice;
    }

    public void setStarprice(float starprice) {
        this.starprice = starprice;
    }

    public float getStaroverall() {
        return staroverall;
    }

    public void setStaroverall(float staroverall) {
        this.staroverall = staroverall;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
