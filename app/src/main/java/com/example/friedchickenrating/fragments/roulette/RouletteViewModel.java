package com.example.friedchickenrating.fragments.roulette;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.friedchickenrating.fragments.ratings.RatingPlace;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class RouletteViewModel extends ViewModel {

    private MutableLiveData<RatingPlace> selectedRatingPlace = new MutableLiveData<>();
    private MutableLiveData<Restaurant> selectedRestaurant = new MutableLiveData<>();
    private MutableLiveData<String> selectedRestaurantId = new MutableLiveData<>();
    private MutableLiveData<Integer> mapRequestCode = new MutableLiveData<>();

    public LiveData<RatingPlace> getSelectedRatingPlace() {
        return selectedRatingPlace;
    }

    public void setSelectedRatingPlace(RatingPlace selectedRatingPlace) {
        this.selectedRatingPlace.setValue(selectedRatingPlace);
    }

    public LiveData<String> getSelectedRestaurantId() {
        return selectedRestaurantId;
    }

    public LiveData<Restaurant> getSelectedRestaurant() {
        return selectedRestaurant;
    }

    public void setSelectedRestaurant(Restaurant selectedRestaurant) {
        this.selectedRestaurant.setValue(selectedRestaurant);
    }

    public void setSelectedRestaurantId(String selectedRestaurantId) {
        this.selectedRestaurantId.setValue(selectedRestaurantId);
    }

    public LiveData<Integer> getMapRequestCode() {
        return mapRequestCode;
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
