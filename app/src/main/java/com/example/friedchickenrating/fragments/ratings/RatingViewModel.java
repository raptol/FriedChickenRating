package com.example.friedchickenrating.fragments.ratings;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RatingViewModel extends ViewModel {

    private MutableLiveData<Rating> selectedRating;
    private MutableLiveData<String> selectedRatingId;

    private static final String TAG = RatingViewModel.class.getSimpleName();

    public RatingViewModel() {
        selectedRating = new MutableLiveData<>();
        selectedRatingId = new MutableLiveData<>();
    }

    public LiveData<Rating> getSelectedRating() {
        return selectedRating;
    }

    public void setSelectedRating(Rating selectedRating) {
        this.selectedRating.setValue(selectedRating);
    }

    public void setSelectedRatingId(String selectedRatingId) {
        this.selectedRatingId.setValue(selectedRatingId);
    }
}