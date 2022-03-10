package com.example.friedchickenrating.fragments.roulette;

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

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentRouletteBinding;
import com.example.friedchickenrating.fragments.maps.MapsFragment;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.example.friedchickenrating.fragments.ratings.ViewRatingFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouletteFragment extends Fragment {
    private RouletteViewModel rouletteViewModel;
    private FragmentRouletteBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;

    private List<Restaurant> restaurantList;
    private RatingPlace selectedPlace;

    private static final String TAG = ViewRatingFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                            binding.txvRouletteResult.setText(selectedPlace.getName());

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
