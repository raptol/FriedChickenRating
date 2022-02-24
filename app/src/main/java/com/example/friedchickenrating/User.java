package com.example.friedchickenrating;

public class User {
    private String uid;
    private String name;
    private String email;
    private String birthYear;
    private float preferflavor;
    private float prefercrunch;
    private float preferspiciness;
    private float preferportion;
    private float preferprice;

    public User() {
    }

    public User(String uid, String name, String email, String birthYear,
                float preferflavor, float prefercrunch, float preferspiciness,
                float preferportion, float preferprice) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.birthYear = birthYear;
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

    public String getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(String birthYear) {
        this.birthYear = birthYear;
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
