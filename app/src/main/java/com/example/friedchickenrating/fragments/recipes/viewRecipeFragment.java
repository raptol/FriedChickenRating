package com.example.friedchickenrating.fragments.recipes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.friedchickenrating.databinding.FragmentViewRatingBinding;
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.example.friedchickenrating.fragments.ratings.RatingViewModel;
import com.example.friedchickenrating.fragments.ratings.ViewRatingFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class viewRecipeFragment extends Fragment {
    private RatingViewModel ratingViewModel;
    private FragmentViewRatingBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Rating> ratingList;
    private RatingPlace selectedPlace;
    static final int REQUEST_MAP_PLACE_FOR_VIEW_RATING = 2;
    static final int REQUEST_BOTTOM_SHEET_FOR_VIEW_RATING = 2;

    private static final String TAG = ViewRatingFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
