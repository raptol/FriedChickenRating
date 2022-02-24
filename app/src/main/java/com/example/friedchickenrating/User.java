package com.example.friedchickenrating;

public class User {
    private String uid;
    private String name;
    private String email;
    private String hometown;
    private Double latitude;
    private Double longitude;
    private String geohash;
    private float preferflavor;
    private float prefercrunch;
    private float preferspiciness;
    private float preferportion;
    private float preferprice;

    public User() {
    }

    public User(String uid, String name, String email,
                String hometown, Double latitude, Double longitude, String geohash,
                float preferflavor, float prefercrunch, float preferspiciness, float preferportion, float preferprice) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.hometown = hometown;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geohash = geohash;
        this.preferflavor = preferflavor;
        this.prefercrunch = prefercrunch;
        this.preferspiciness = preferspiciness;
        this.preferportion = preferportion;
        this.preferprice = preferprice;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
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

    public float getPreferflavor() {
        return preferflavor;
    }

    public void setPreferflavor(float preferflavor) {
        this.preferflavor = preferflavor;
    }

    public float getPrefercrunch() {
        return prefercrunch;
    }

    public void setPrefercrunch(float prefercrunch) {
        this.prefercrunch = prefercrunch;
    }

    public float getPreferspiciness() {
        return preferspiciness;
    }

    public void setPreferspiciness(float preferspiciness) {
        this.preferspiciness = preferspiciness;
    }

    public float getPreferportion() {
        return preferportion;
    }

    public void setPreferportion(float preferportion) {
        this.preferportion = preferportion;
    }

    public float getPreferprice() {
        return preferprice;
    }

    public void setPreferprice(float preferprice) {
        this.preferprice = preferprice;
    }
}
