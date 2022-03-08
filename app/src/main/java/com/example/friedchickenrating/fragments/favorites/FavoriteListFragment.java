package com.example.friedchickenrating.fragments.favorites;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentFavoriteListBinding;
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.example.friedchickenrating.fragments.ratings.RatingViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoriteListFragment extends Fragment implements FavoriteListAdapter.ItemClickListener{

    private static FavoriteListFragment instance = null;
    private RatingViewModel ratingViewModel;
    private FragmentFavoriteListBinding binding;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Favorite> favoriteList;
    private List<Rating> ratingList;
    private List<RatingPlace> placeList;
    private FavoriteListAdapter favoriteListAdapter;

    private static final String TAG = FavoriteListFragment.class.getSimpleName();

    @Override
    public void onListItemClick(Favorite favorite, Rating rating, int position) {
        ratingViewModel.setSelectedRating(rating);
        ratingViewModel.setSelectedRatingId(ratingList.get(position).getId());

        NavHostFragment.findNavController(FavoriteListFragment.this)
                .navigate(R.id.action_nav_my_favorites_to_nav_viewRatings);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);

        binding = FragmentFavoriteListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        favoriteList = new ArrayList<>();
        ratingList = new ArrayList<>();
        placeList = new ArrayList<>();

        favoriteListAdapter = new FavoriteListAdapter(favoriteList, ratingList, placeList);
        favoriteListAdapter.setListener(this);

        binding.recyclerViewFavoriteList.setLayoutManager(new GridLayoutManager(this.getContext(), 2));
        binding.recyclerViewFavoriteList.setAdapter(favoriteListAdapter);

        readPlaceList();
        readRatingList();
        readFavoriteList();
    }

    private void readPlaceList() {
        // Listen for realtime updates of the places
        db.collection("places")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        placeList.clear();
                        for(QueryDocumentSnapshot document: value) {
                            if (document != null) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                RatingPlace place = document.toObject(RatingPlace.class);
                                placeList.add(place);
                            }
                        }
                        favoriteListAdapter.setFavoriteList(favoriteList, ratingList, placeList);
                    }
                });
    }

    private void readRatingList() {
        // Listen for realtime updates of the ratings
        db.collection("ratings")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        ratingList.clear();
                        for(QueryDocumentSnapshot document: value) {
                            if (document != null) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                Rating rating = document.toObject(Rating.class);
                                ratingList.add(rating);
                            }
                        }
                        favoriteListAdapter.setFavoriteList(favoriteList, ratingList, placeList);
                    }
                });
    }

    private void readFavoriteList() {
        // Listen for realtime updates of the favorites
        db.collection("favorites")
                .whereEqualTo("userid", user.getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        favoriteList.clear();
                        for(QueryDocumentSnapshot document: value) {
                            if (document != null) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                Favorite favorite = document.toObject(Favorite.class);
                                favoriteList.add(favorite);
                            }
                        }
                        favoriteListAdapter.setFavoriteList(favoriteList, ratingList, placeList);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

