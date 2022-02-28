package com.example.friedchickenrating.fragments.ratings;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentNewRatingBinding;
import com.example.friedchickenrating.databinding.FragmentRatingListBinding;
import com.example.friedchickenrating.databinding.FragmentViewRatingBinding;
import com.example.friedchickenrating.fragments.maps.BottomSheetFragment;
import com.example.friedchickenrating.fragments.maps.MapsFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewRatingFragment extends Fragment {

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);
        binding = FragmentViewRatingBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        ratingList = new ArrayList<>();

        Rating curRating = ratingViewModel.getSelectedRating().getValue();

        Log.d(TAG, "curRating.id: " + curRating.getPlaceid());
        Log.d(TAG, "curRating.title: " + curRating.getTitle());

        // Listen for realtime updates of the places
        if(curRating != null && curRating.getPlaceid() != null) {
            db.collection("places").document(curRating.getPlaceid())
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot value,
                                            @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.w(TAG, "Listen failed.", error);
                                return;
                            }

                            selectedPlace = value.toObject(RatingPlace.class);

                            // download and display images
                            Map<String, Object> pictures = curRating.getPictures();
                            String filename = String.valueOf(pictures.get("filename"));
                            Log.d(TAG, "filename: " + filename);

                            if (!filename.isEmpty() && filename != null) {
                                long size;
                                final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                                StorageReference storageReference
                                        = firebaseStorage.getReference().child("images").child(filename);
                                storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            Glide.with(getActivity())
                                                    .load(task.getResult())
                                                    .into(binding.imgViewPicture);
                                            binding.imgViewPicture.invalidate();
                                        } else {
                                            Toast.makeText(getContext(),
                                                    "Fail to load image", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }


                            binding.viewRatingTitle.setText(curRating.getTitle());
                            binding.viewRatingPlaceName.setText(selectedPlace.getName());
                            binding.viewRatingChickenType.setText(curRating.getType());
                            binding.ratingBarFlavor.setRating(curRating.getStarflavor());
                            binding.ratingBarCrunch.setRating(curRating.getStarcrunch());
                            binding.ratingBarSpiciness.setRating(curRating.getStarspiciness());
                            binding.ratingBarPortion.setRating(curRating.getStarportion());
                            binding.ratingBarPrice.setRating(curRating.getStarprice());
                            binding.ratingBarOverall.setRating(curRating.getStaroverall());
                            binding.viewRatingOtherItems.setText(curRating.getOtheritems());
                            binding.viewRatingNotes.setText(curRating.getNotes());

                        }
                    });
        }

        //event handler for open map button
        binding.btnOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ratingViewModel.setSelectedRating(curRating);
                ratingViewModel.setSelectedRatingPlace(selectedPlace);
                ratingViewModel.setMapRequestCode(REQUEST_MAP_PLACE_FOR_VIEW_RATING);

                NavHostFragment.findNavController(ViewRatingFragment.this)
                        .navigate(R.id.action_nav_viewRatings_to_nav_maps);
            }
        });

        //If login user is user who created current rating, enable Edit button
        //else, disable Edit button
        if(user.getUid() != null && user.getUid().equals(curRating.getUserid())) {
            binding.btnEditRating.setVisibility(View.VISIBLE);
            binding.btnDeleteRating.setVisibility(View.VISIBLE);
        } else {
            binding.btnEditRating.setVisibility(View.INVISIBLE);
            binding.btnDeleteRating.setVisibility(View.INVISIBLE);
        }

        //event handler for edit button
        binding.btnEditRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String region = ratingViewModel.getRegionFromLatLng(requireContext(),
                        selectedPlace.getLatitude(), selectedPlace.getLongitude());

                Integer requestCode = ratingViewModel.getMapRequestCode().getValue();
                Bundle result = new Bundle();
                result.putString("placeId", selectedPlace.getPlaceid());
                result.putString("placeName", selectedPlace.getName());
                result.putDouble("latitude", selectedPlace.getLatitude());
                result.putDouble("longitude", selectedPlace.getLongitude());
                result.putString("region", region);

                ratingViewModel.setSelectedRating(curRating);
                ratingViewModel.setSelectedRatingImage(binding.imgViewPicture);

                getParentFragmentManager().setFragmentResult("passByViewRating", result);

                NavHostFragment.findNavController(ViewRatingFragment.this)
                        .navigate(R.id.action_nav_viewRatings_to_nav_newRating);

            }
        });

        //event handler for delete button
        binding.btnDeleteRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.collection("ratings").document(curRating.getId())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "The rating was successfully deleted!");
                                Toast.makeText(getContext(), "delete the rating success.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error deleting the rating", e);
                                Toast.makeText(getContext(), "delete the rating error.", Toast.LENGTH_SHORT).show();
                            }
                        });

                //myAdapter.notifyItemRangeChanged()
                NavHostFragment.findNavController(ViewRatingFragment.this)
                        .navigate(R.id.action_nav_viewRatings_to_nav_ratings);
            }
        });


        //event handler for show all rating list of this place
        binding.btnShowAllRatingList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ratingViewModel.setSelectedRatingPlace(selectedPlace);
                ratingViewModel.setMapRequestCode(REQUEST_BOTTOM_SHEET_FOR_VIEW_RATING);

                BottomSheetFragment bottomSheetFragment
                        = BottomSheetFragment.newInstance(
                        selectedPlace.getPlaceid(),
                        selectedPlace.getName(),
                        selectedPlace.getRegion());
                bottomSheetFragment.show(getParentFragmentManager(), BottomSheetFragment.TAG);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
