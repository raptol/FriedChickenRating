package com.example.friedchickenrating.fragments.recipes;

import android.net.Uri;
import android.widget.ImageView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecipesViewModel extends ViewModel {

    private MutableLiveData<Recipe> selectedRecipe = new MutableLiveData<>();
    private MutableLiveData<String> selectedRecipeId = new MutableLiveData<>();
    private MutableLiveData<ImageView> selectedRecipeImage = new MutableLiveData<>();
    private MutableLiveData<Uri> selectedRatingImageFilePath = new MutableLiveData<>();
    private MutableLiveData<Integer> filter = new MutableLiveData<>();
    private MutableLiveData<Integer> selectedPosition = new MutableLiveData<>();

    private static final String TAG = RecipesViewModel.class.getSimpleName();

    public RecipesViewModel() {
        setFilter(0);
        setSelectedPosition(0);
    }

    public LiveData<Recipe> getSelectedRecipe() {
        return selectedRecipe;
    }
    public LiveData<String> getSelectedRecipeId() {
        return selectedRecipeId;
    }
    public LiveData<ImageView> getSelectedRecipeImage() {
        return selectedRecipeImage;
    }
    public LiveData<Uri> getSelectedRecipeImageFilePath() {
        return selectedRatingImageFilePath;
    }
    public LiveData<Integer> getFilter() {
        return filter;
    }
    public LiveData<Integer> getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedRecipe(Recipe selectedRecipe) {
        this.selectedRecipe.setValue(selectedRecipe);
    }

    public void setSelectedRecipeId(String selectedRecipeId) {
        this.selectedRecipeId.setValue(selectedRecipeId);
    }

    public void setSelectedRecipeImage(ImageView selectedRecipeImage) {
        this.selectedRecipeImage.setValue(selectedRecipeImage);
    }

    public void setSelectedRatingImageFilePath(Uri selectedRatingImageFilePath) {
        this.selectedRatingImageFilePath.setValue(selectedRatingImageFilePath);
    }

    public void setFilter(Integer filter) {
        this.filter.setValue(filter);
    }

    public void setSelectedPosition(Integer position) {
        this.selectedPosition.setValue(position);
    }
}