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
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentRatingListBinding;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
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

    private static final int SORT_OPTION_LOCATION = 1;
    private static final int SORT_OPTION_ASCENDING = 2;
    private static final int SORT_OPTION_DESCENDING = 3;
    private static final int SORT_OPTION_LATEST = 4;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);

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

        displayPlaceList();
        displayRatingList(SORT_OPTION_LOCATION); //default sorting option: location

        binding.btnSortLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_LOCATION);
            }
        });

        binding.btnSortAscending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_ASCENDING);
            }
        });

        binding.btnSortDescending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_DESCENDING);
            }
        });

        binding.btnSortLatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRatingList(SORT_OPTION_LATEST);
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

    private void displayPlaceList() {
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
    }

    private void displayRatingList(int sortOption) {

        Query query;

        switch(sortOption) {
            case SORT_OPTION_ASCENDING:
                query = db.collection("ratings").orderBy("title", Query.Direction.ASCENDING);
                break;

            case SORT_OPTION_DESCENDING:
                query = db.collection("ratings").orderBy("title", Query.Direction.DESCENDING);
                break;

            case SORT_OPTION_LATEST:
                query = db.collection("ratings");
                break;

            default:
                query = db.collection("ratings");
        }

        // Listen for realtime updates of the ratings
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
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
}