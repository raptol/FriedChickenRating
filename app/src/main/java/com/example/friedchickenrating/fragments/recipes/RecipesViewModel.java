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

    private static final String TAG = RecipesViewModel.class.getSimpleName();

    public RecipesViewModel() { }

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
}