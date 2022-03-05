package com.example.friedchickenrating.fragments.recipes;

import com.google.firebase.Timestamp;

import java.util.Map;

public class Recipe {
    private String recipeId; //recipe id
    private String recipeTitle; //recipe title
    private String recipeIngredients;
    private String recipeSteps;
    private Map<String, Object> pictures;
    private Timestamp timestamp;

    public Recipe() { }

    public Recipe(String recipeId, String recipeTitle, String recipeIngredients, String recipeSteps, Map<String, Object> pictures, Timestamp timestamp) {
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.recipeIngredients = recipeIngredients;
        this.recipeSteps = recipeSteps;
        this.pictures = pictures;
        this.timestamp = timestamp;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public String getRecipeIngredients() {
        return recipeIngredients;
    }

    public void setRecipeIngredients(String recipeIngredients) {
        this.recipeIngredients = recipeIngredients;
    }

    public String getRecipeSteps() {
        return recipeSteps;
    }

    public void setRecipeSteps(String recipeSteps) {
        this.recipeSteps = recipeSteps;
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

