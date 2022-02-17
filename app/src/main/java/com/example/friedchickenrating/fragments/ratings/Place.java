package com.example.friedchickenrating.fragments.ratings;

import java.util.Map;

public class Place {
    private String placeid;
    private String name;
    private String latitude;
    private String longitude;
    private String region;

    public Place() {}

    public Place(String placeid, String name, String latitude, String longitude, String region) {
        this.placeid = placeid;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.region = region;
    }

    public String getPlaceid() {
        return placeid;
    }

    public void setPlaceid(String placeid) {
        this.placeid = placeid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
