package com.example.friedchickenrating.fragments.ratings;

import java.util.HashMap;
import java.util.Map;

public class Photo {
    private String filename;
    private String date;

    public Photo() {
        filename = "";
        date = "";
    }

    public Photo(String filename, String date) {
        this.filename = filename;
        this.date = date;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("date", date);

        return result;
    }
}
