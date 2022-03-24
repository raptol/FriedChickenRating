package com.example.friedchickenrating.fragments.roulette;

import android.annotation.SuppressLint;
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
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingListAdapter;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.example.friedchickenrating.fragments.recipes.Recipe;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RouletteFragment extends Fragment implements Animation.AnimationListener {
    private RouletteViewModel rouletteViewModel;
    private FragmentRouletteBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Restaurant> restaurantList;
    private List<RatingPlace> placeList;
    private RatingPlace selectedPlace;

    private static final String TAG = RouletteFragment.class.getSimpleName();

    boolean isRotating = false;
    private static final String[] slots = {"1", "2", "3" ,"4" ,"5" , "6", "7", "8", "9"};
    private static final int[] slotDegrees = new int[slots.length];
    private int degree = 0;
    private static final Random random = new Random();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDegreeForSlots();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rouletteViewModel = new ViewModelProvider(requireActivity()).get(RouletteViewModel.class);
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

        restaurantList = new ArrayList<>();
        placeList = new ArrayList<>();

        Restaurant curRestaurant = rouletteViewModel.getSelectedRestaurant().getValue();

        // Listen for realtime updates of the places
        if(curRestaurant != null && curRestaurant.getPlaceid() != null) {
            db.collection("places").document(curRestaurant.getPlaceid())
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value,
                                            @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.w(TAG, "Listen failed.", error);
                                return;
                            }

                            selectedPlace = value.toObject(RatingPlace.class);

//                            // download and display images
//                            Map<String, Object> pictures = curRestaurant.getPictures();
//                            String filename = String.valueOf(pictures.get("filename"));
//                            Log.d(TAG, "filename: " + filename);
//
//                            if (!filename.isEmpty() && filename != null) {
//                                long size;
//                                final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
//                                StorageReference storageReference
//                                        = firebaseStorage.getReference().child("images").child(filename);
//                                storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
//                                    @Override
//                                    public void onComplete(@NonNull Task<Uri> task) {
//                                        if (task.isSuccessful()) {
//
//                                            if(getActivity() != null) {
//                                                Glide.with(getActivity())
//                                                        .load(task.getResult())
//                                                        .into(binding.imgViewPicture);
//                                                binding.imgViewPicture.invalidate();
//                                            }
//                                        } else {
//                                            Toast.makeText(getContext(),
//                                                    "Fail to load image", Toast.LENGTH_SHORT).show();
//                                        }
//                                    }
//                                });
//                            }

//                            binding.txvRouletteResult.setText(curRestaurant.getRestaurantName());
//                            binding.txvRouletteResult.setText(selectedPlace.getName());

                        }
                    });
        }


        //event handler for open map button
        binding.btnOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                rouletteViewModel.setSelectedRestaurant(curRestaurant);
                rouletteViewModel.setSelectedRatingPlace(selectedPlace);
                rouletteViewModel.setMapRequestCode(MapsFragment.REQUEST_MAP_PLACE_FOR_VIEW_RATING);

                NavHostFragment.findNavController(RouletteFragment.this)
                        .navigate(R.id.action_rouletteFragment_to_nav_maps);
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

//                    binding.txvRouletteResult.setText("Result: onClick " + degree);

//                    Query query;
//                    query = db.collection("places")
//                            .orderBy("name", Query.Direction.ASCENDING);
//
//                    query.addSnapshotListener(new EventListener<QuerySnapshot>() {
//                        @Override
//                        public void onEvent(@Nullable QuerySnapshot value,
//                                            @Nullable FirebaseFirestoreException error) {
//                            if(error != null) {
//                                Log.w(TAG, "Listen failed.", error);
//                                return;
//                            }
//
//                            Log.d(TAG, "excute fetch db, value.size: " + value.size());
//
//                            placeList.clear();
//                            for(QueryDocumentSnapshot document: value) {
//                                if (document != null) {
//                                    Log.d(TAG, document.getId() + " => " + document.getData());
//                                    RatingPlace rating = document.toObject(RatingPlace.class);
//                                    placeList.add(rating);
//                                    binding.txvRouletteResult.setText(document.getString("name"));
//
//                                }
//                            }
//                        }
//                    });
                }
            }
        });
    }



    @Override
    public void onAnimationStart(Animation animation) {
        binding.txvRouletteResult.setText("Result: ");
        this.isRotating = false;
//        binding.btnSpin.setText("Stop");
        binding.btnSpin.setEnabled(false);
        Log.d(TAG, "onAnimationStart()");

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        Toast.makeText(this.getContext(), "Animation End", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onAnimationEnd()");
        this.isRotating = false;
//        binding.btnSpin.setText("Spin");
        binding.btnSpin.setEnabled(true);
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
                        Log.d(TAG, "SIZE " + placeList.size());
//                        binding.txvRouletteResult.setText(document.getString("name"));
//                        Log.d(TAG, "RESTAURANT NAME " + document.getString("name"));
//                        binding.txvRouletteResult.setText(placeList.get(0).getName());
//                        Log.d(TAG, "RESTAURANT NAME " + placeList.get(0).getName());
//                        binding.txvRouletteResult.setText(placeList.get(degree).getName());
//                        Log.d(TAG, "RESTAURANT NAME " + placeList.get(degree).getName());

                         Random rand = new Random();
                         Integer randomPlaceNum = rand.nextInt(placeList.size());
                            Log.d(TAG, "RANDOMPLACENUM " + randomPlaceNum);
//                        if(degree > placeList.size()  ) {
                                    binding.txvRouletteResult.setText(placeList.get(randomPlaceNum).getName());
//                                }
//                                else {
//                                    binding.txvRouletteResult.setText(placeList.get(degree).getName());
//                                }

                    }
                }
            }
        });
//        binding.txvRouletteResult.setText("Result: " + slots[slots.length - (degree + 1)]);
    }

    private void getDegreeForSlots() {
        int slotDegree = 360/slots.length;
        for (int i = 0; i < slots.length; i++) {
            slotDegrees[i] = (i+1) * slotDegree;
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
