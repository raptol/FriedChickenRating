package com.example.friedchickenrating.fragments.ratings;

import java.util.Map;

public class Place {
    private String placeid;
    private String name;
    private Double latitude;
    private Double longitude;
    private String geohash;
    private String region;

    public Place() {}

    public Place(String placeid, String name, Double latitude, Double longitude, String geohash, String region) {
        this.placeid = placeid;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geohash = geohash;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
