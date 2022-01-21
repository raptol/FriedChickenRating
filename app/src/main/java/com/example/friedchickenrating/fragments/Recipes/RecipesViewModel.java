package com.example.friedchickenrating.fragments.Recipes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecipesViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public RecipesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Recipes fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}