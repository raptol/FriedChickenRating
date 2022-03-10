package com.example.friedchickenrating.fragments.roulette;

import com.example.friedchickenrating.fragments.ratings.RatingPlace;

import java.util.List;

public class RouletteAdapter {
    private List<Restaurant> restaurantList;
    private List<RatingPlace> placeList;
//    private ItemClickListener mListener;
    
    interface ItemClickListener {
        void onListItemClick(Restaurant restaurant, int position);
    }
}
