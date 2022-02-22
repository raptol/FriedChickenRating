package com.example.friedchickenrating.fragments.ratings;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.AddressComponents;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RatingViewModel extends ViewModel {

    private MutableLiveData<Rating> selectedRating = new MutableLiveData<>();
    ;
    private MutableLiveData<String> selectedRatingId = new MutableLiveData<>();
    ;
    private MutableLiveData<ImageView> selectedRatingImage = new MutableLiveData<>();
    private MutableLiveData<Uri> selectedRatingImageFilePath = new MutableLiveData<>();
    private MutableLiveData<Integer> mapRequestCode = new MutableLiveData<>();

    private static final String TAG = RatingViewModel.class.getSimpleName();

    public RatingViewModel() {
    }

    public LiveData<Rating> getSelectedRating() {
        return selectedRating;
    }

    public LiveData<String> getSelectedRatingId() {
        return selectedRatingId;
    }

    public LiveData<ImageView> getSelectedRatingImage() {
        return selectedRatingImage;
    }

    public LiveData<Uri> getSelectedRatingImageFilePath() {
        return selectedRatingImageFilePath;
    }

    public LiveData<Integer> getMapRequestCode() {
        return mapRequestCode;
    }

    public void setSelectedRating(Rating selectedRating) {
        this.selectedRating.setValue(selectedRating);
    }

    public void setSelectedRatingId(String selectedRatingId) {
        this.selectedRatingId.setValue(selectedRatingId);
    }

    public void setSelectedRatingImage(ImageView selectedRatingImage) {
        this.selectedRatingImage.setValue(selectedRatingImage);
    }

    public void setSelectedRatingImageFilePath(Uri selectedRatingImageFilePath) {
        this.selectedRatingImageFilePath.setValue(selectedRatingImageFilePath);
    }

    public void setMapRequestCode(Integer mapRequestCode) {
        this.mapRequestCode.setValue(mapRequestCode);
    }

    public String getRegionFromLatLng(Context context, Double latitude, Double longitude) {

        String region = "";
        Geocoder gcd = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 1);
            if(addresses.size() > 0) {
                region = addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return region;
    }
}