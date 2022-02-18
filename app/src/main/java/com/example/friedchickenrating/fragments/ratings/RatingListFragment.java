package com.example.friedchickenrating.fragments.ratings;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentRatingListBinding;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class RatingListFragment extends Fragment implements RatingListAdapter.ItemClickListener{

    private RatingViewModel ratingViewModel;
    private FragmentRatingListBinding binding;

    private FirebaseFirestore db;

    private List<Rating> ratingList;
    private List<Place> placeList;
    private RatingListAdapter ratingListAdapter;

    private static final String TAG = RatingListFragment.class.getSimpleName();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ratingViewModel = new ViewModelProvider(this).get(RatingViewModel.class);

        binding = FragmentRatingListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        ratingList = new ArrayList<>();
        placeList = new ArrayList<>();

        ratingListAdapter = new RatingListAdapter(ratingList, placeList);
        ratingListAdapter.setListener(this);

        binding.recyclerViewRatingList.setLayoutManager(new GridLayoutManager(this.getContext(), 1));
        binding.recyclerViewRatingList.setAdapter(ratingListAdapter);

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
                                Place place = document.toObject(Place.class);
                                placeList.add(place);
                            }
                        }
                        ratingListAdapter.setRatingList(ratingList, placeList);
                    }
                });

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
                    ratingListAdapter.setRatingList(ratingList, placeList);
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onListItemClick(Rating rating, int position) {
        ratingViewModel.setSelectedRating(rating);
        ratingViewModel.setSelectedRatingId(ratingList.get(position).getId());

        NavHostFragment.findNavController(RatingListFragment.this)
                .navigate(R.id.action_nav_ratings_to_nav_viewRatings);
    }
}