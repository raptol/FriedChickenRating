package com.example.friedchickenrating.fragments.ratings;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentNewRatingBinding;
import com.example.friedchickenrating.databinding.FragmentRatingListBinding;
import com.example.friedchickenrating.databinding.FragmentViewRatingBinding;
import com.example.friedchickenrating.fragments.favorites.Favorite;
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
    private Favorite favorite = null;
    private Boolean isFavorite = false;

    private Uri imageUri;

    private static final String TAG = ViewRatingFragment.class.getSimpleName();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

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
                            String filename = null;
                            if(pictures != null)
                                filename = String.valueOf(pictures.get("filename"));
                            Log.d(TAG, "filename: " + filename);

                            if (filename != null && !filename.isEmpty()) {
                                long size;
                                final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                                StorageReference storageReference
                                        = firebaseStorage.getReference().child("images").child(filename);
                                storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {

                                            if(getActivity() != null) {
                                                imageUri = task.getResult();

                                                Glide.with(getActivity())
                                                        .load(imageUri)
                                                        .into(binding.imgViewPicture);

                                                binding.imgViewPicture.invalidate();
                                            }
                                        } else {
                                            Toast.makeText(getContext(),
                                                    "Fail to load image", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }


                            binding.viewRatingTitle.setText(curRating.getTitle());
                            binding.viewRatingPlaceName.setText(selectedPlace.getName().trim().replaceAll("\\s+", " ") );
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
                ratingViewModel.setMapRequestCode(MapsFragment.REQUEST_MAP_PLACE_FOR_VIEW_RATING);

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


        //If login user is user who selected current rating as favorite, show color image
        //else, show grey image
        showNotSelectedFavoriteButton();
        if(user.getUid() != null) {

            db.collection("favorites")
                    .whereEqualTo("userid", user.getUid())
                    .whereEqualTo("ratingid", curRating.getId())
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException error) {
                            if(error != null) {
                                Log.w(TAG, "Listen failed.", error);
                                return;
                            }

                            if(value.size() > 0) {
                                DocumentSnapshot document = value.getDocuments().get(0);
                                if (document != null) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                    favorite = document.toObject(Favorite.class);

                                    if(!isFavorite) {
                                        //wait for binding my favorite button
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                showSelectedFavoriteButton();
                                            }
                                        }, 200);

                                    }
                                }
                            }
                        }
                    });
        }

        //event handler for my favorite button
        binding.btnMyFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Click to my favorite
                if(isFavorite) { // favorite is already selected
                    db.collection("favorites").document(favorite.getId())
                            .delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d(TAG, "The favorite was successfully deleted!");
                                    Toast.makeText(getContext(), "delete the favorite success.", Toast.LENGTH_SHORT).show();

                                    showNotSelectedFavoriteButton();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error deleting the favorite", e);
                                    Toast.makeText(getContext(), "delete the favorite error.", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    String favoriteDocId = db.collection("favorites").document().getId();

                    favorite = new Favorite(favoriteDocId, user.getUid(), curRating.getId());
                    db.collection("favorites").document(favoriteDocId)
                        .set(favorite)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "New favorite was successfully saved!");
                                Log.d(TAG, "Document ID:" + favoriteDocId);
                                Toast.makeText(getContext(), "add new favorite success.", Toast.LENGTH_SHORT).show();

                                showSelectedFavoriteButton();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error saving data", e);
                                Toast.makeText(getContext(), "add new favorite error.", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            }
        });

        //event handler for share button
        binding.btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String contents =   "Title: " + binding.viewRatingTitle.getText().toString() + "\n" +
                        "Restaurant: " + binding.viewRatingPlaceName.getText().toString().trim().replaceAll("\\s+", " ") + "\n" +
                        "\tRegion: " + binding.viewRatingRegion.getText().toString() + "\n" +
                        "\tChicken Type: " + binding.viewRatingChickenType.getText().toString() + "\n" +
                        "\t- Flavor rating: " + binding.ratingBarFlavor.getRating() + "\n" +
                        "\t - Crunch rating: " + binding.ratingBarCrunch.getRating() + "\n" +
                        "\t - Spiciness rating: " + binding.ratingBarSpiciness.getRating() + "\n" +
                        "\t - Portion rating: " + binding.ratingBarPortion.getRating() + "\n" +
                        "\t - Price rating: " + binding.ratingBarPrice.getRating() + "\n" +
                        "\t - Overall rating: " + binding.ratingBarOverall.getRating() + "\n" +
                        "\tOther Items: " + binding.viewRatingOtherItems.getText().toString() + "\n" +
                        "\tNotes: " + binding.viewRatingNotes.getText().toString() + "\n\n" +
                        "Google map: https://www.google.com/maps/@?api=1&map_action=map&center="
                            + selectedPlace.getLatitude() + "%2C"
                            + selectedPlace.getLongitude() + "&zoom=17";

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, binding.viewRatingTitle.getText().toString());
                intent.putExtra(Intent.EXTRA_TEXT, contents);

                if(imageUri != null) {
                    intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    intent.setType("image/jpeg");
                }

                startActivity(Intent.createChooser(intent, "Share fried chicken"));
            }
        });

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
            int count = 0;

            @Override
            public void onClick(View view) {
                //search favorite with deleted rating
                db.collection("favorites")
                        .whereEqualTo("ratingid", curRating.getId())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    if(task.getResult().size() > 0) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Log.d(TAG, document.getId() + " => " + document.getData());

                                            //delete favorite in the favorites collection
                                            db.collection("favorites").document(document.getId())
                                                    .delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            Log.d(TAG, "The favorite was successfully deleted!");

                                                            count++;

                                                            //delete rating in the ratings collection when all favorite was deleted
                                                            if(count == task.getResult().size())
                                                                deleteRating(curRating.getId());
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.w(TAG, "Error deleting the favorite", e);
                                                            Toast.makeText(getContext(), "delete the favorite error.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    } else { //there is no favorite that is made by the rating to be deleted
                                        //delete rating in the ratings collection
                                        deleteRating(curRating.getId());
                                    }
                                } else {
                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                }
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
                ratingViewModel.setMapRequestCode(BottomSheetFragment.REQUEST_BOTTOM_SHEET_FOR_VIEW_RATING);

                BottomSheetFragment bottomSheetFragment
                        = BottomSheetFragment.newInstance(
                        selectedPlace.getPlaceid(),
                        selectedPlace.getName(),
                        selectedPlace.getRegion());
                bottomSheetFragment.show(getParentFragmentManager(), BottomSheetFragment.TAG);
            }
        });
    }

    private void deleteRating(String docId) {

        //delete rating in the ratings collection
        db.collection("ratings").document(docId)
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
    }

    private void showSelectedFavoriteButton() {
        isFavorite = true;
        if(binding != null && binding.btnMyFavorite != null)
            binding.btnMyFavorite.setImageResource(R.drawable.home_favorite);
    }

    private void showNotSelectedFavoriteButton() {
        isFavorite = false;
        if(binding != null && binding.btnMyFavorite != null)
            binding.btnMyFavorite.setImageResource(R.drawable.ic_icon_favorite);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
