package com.example.friedchickenrating.fragments.recipes;

import com.google.firebase.Timestamp;

import java.util.Map;

public class Recipe {
    private String recipeId; //recipe id
    private String recipeTitle; //recipe title
    private String ingredients;
    private String steps;
    private Map<String, Object> pictures;
    private Timestamp timestamp;

    public Recipe() { }

    public Recipe(String recipeId, String recipeTitle, String ingredients, String steps, Map<String, Object> pictures, Timestamp timestamp) {
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.ingredients = ingredients;
        this.steps = steps;
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

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
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

