package com.example.friedchickenrating.fragments.roulette;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.friedchickenrating.R;
import com.example.friedchickenrating.User;
import com.example.friedchickenrating.databinding.FragmentRouletteBinding;
import com.example.friedchickenrating.fragments.maps.MapsFragment;
import com.example.friedchickenrating.fragments.ratings.NewRatingFragment;
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingListAdapter;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.example.friedchickenrating.fragments.ratings.RatingViewModel;
import com.example.friedchickenrating.fragments.recipes.Recipe;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RouletteFragment extends Fragment implements Animation.AnimationListener {
    private RatingViewModel ratingViewModel;
    private FragmentRouletteBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<RatingPlace> placeList;
    private RatingPlace selectedPlace;

    private static final String TAG = RouletteFragment.class.getSimpleName();

    private boolean isRotating = false;
    private boolean isClicked = false;

    private static final String[] slots = {"1", "2", "3" ,"4" ,"5" , "6", "7", "8", "9"};
    private static final int[] slotDegrees = new int[slots.length];
    private static final Random random = new Random();
    private Random rand = new Random();
    private int degree = 0;
    private int randomPlaceNum ;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDegreeForSlots();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);

        binding = FragmentRouletteBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        placeList = new ArrayList<>();
        selectedPlace = new RatingPlace();

        //event handler for open map button
            binding.btnOpenMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    ratingViewModel.setSelectedRatingPlace(selectedPlace);
                    ratingViewModel.setMapRequestCode(MapsFragment.REQUEST_MAP_PLACE_FOR_VIEW_RATING);

                    if(isClicked) {
                        NavHostFragment.findNavController(RouletteFragment.this)
                            .navigate(R.id.action_rouletteFragment_to_nav_maps);
                        isClicked = false;
                    }
                    else {
                        Toast.makeText(getContext(), "Please Pick the Restaurant First.", Toast.LENGTH_SHORT).show();
                    }
                }
            });


        //event handler for spin button
        binding.btnSpin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRotating) {
                    degree = random.nextInt(slots.length - 1);
                    Log.d(TAG, "DEGREE " + degree);
                    RotateAnimation rotateAnimation = new RotateAnimation(0,
                            (360 * slots.length) + slotDegrees[degree],
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                            RotateAnimation.RELATIVE_TO_SELF, 0.5f);
                    rotateAnimation.setDuration(3600);
                    rotateAnimation.setFillAfter(true);
                    rotateAnimation.setInterpolator(new DecelerateInterpolator());
                    rotateAnimation.setAnimationListener(RouletteFragment.this);
                    binding.imgRouletteLarge.setAnimation(rotateAnimation);
                    binding.imgRouletteLarge.startAnimation(rotateAnimation);
                    isClicked = true;
                }
            }
        });
    }

    @Override
    public void onAnimationStart(Animation animation) {
        binding.txvRouletteResult.setText("Spinning... ");
        this.isRotating = false;
        binding.btnSpin.setEnabled(false);
        binding.btnSpin.setTextColor(Color.GRAY);
        Log.d(TAG, "onAnimationStart()");
    }


    @Override
    public void onAnimationEnd(Animation animation) {
        Log.d(TAG, "onAnimationEnd()");
        this.isRotating = false;
        binding.btnSpin.setEnabled(true);
        binding.btnSpin.setTextColor(Color.WHITE);

        Query query;
        query = db.collection("places");

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value,
                                @Nullable FirebaseFirestoreException error) {
                if(error != null) {
                    Log.w(TAG, "Listen failed.", error);
                    return;
                }

                Log.d(TAG, "excute fetch db, value.size: " + value.size());

                placeList.clear();
                for(QueryDocumentSnapshot document: value) {
                    if (document != null) {
                        Log.d(TAG, document.getId() + " => " + document.getData());
                        RatingPlace placeName = document.toObject(RatingPlace.class);
                        placeList.add(placeName);
                    }
                }

                Log.d(TAG, "Place list SIZE: " + placeList.size());

                if(placeList.size() > 0) {
                    randomPlaceNum = rand.nextInt(placeList.size());
                    Log.d(TAG, "RANDOMPLACE_NUM " + randomPlaceNum);

                    selectedPlace = placeList.get(randomPlaceNum);
                    Log.d(TAG, "SELECTED_PLACE_ID" + selectedPlace.getPlaceid());

                    if(placeList != null && placeList.get(randomPlaceNum) != null &&
                            placeList.get(randomPlaceNum).getName() != null) {
                        binding.txvRouletteResult.setText(
                                placeList.get(randomPlaceNum).getName()
                                        .trim().replaceAll("\\s+", " "));
                    }
                }
            }
        });
    }

    private void getDegreeForSlots() {
        int slotDegree = 360 / slots.length;
        for (int i = 0; i < slots.length; i++) {
            slotDegrees[i] = (i + 1) * slotDegree;
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) { }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
