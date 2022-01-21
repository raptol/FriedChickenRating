package com.example.friedchickenrating;

public class User {
    private String uid;
    private String name;
    private String email;
    private String birthYear;

    public User() {
    }

    public User(String uid, String name, String email, String birthYear) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.birthYear = birthYear;
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
}
