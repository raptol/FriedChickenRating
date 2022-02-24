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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.friedchickenrating.R;
import com.example.friedchickenrating.databinding.FragmentNewRatingBinding;
import com.example.friedchickenrating.databinding.FragmentRatingListBinding;
import com.example.friedchickenrating.databinding.FragmentViewRatingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewRatingFragment extends Fragment {

    private RatingViewModel ratingViewModel;
    private FragmentViewRatingBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ImageView imgViewNewPhoto;
    private Uri filePath;
    private String fileName;

    private List<Rating> ratingList;
    private List<RatingPlace> placeList;

    private static final String TAG = ViewRatingFragment.class.getSimpleName();
    static final int REQUEST_IMAGE_SELECT = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;


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

        ratingList = new ArrayList<>();
        placeList = new ArrayList<>();

        Rating curRating = ratingViewModel.getSelectedRating().getValue();

        Log.d(TAG, "curRating.id: " + curRating.getPlaceid());
        Log.d(TAG, "curRating.title: " + curRating.getTitle());
//        Log.d(TAG, "curRating.region: " + curRating.getRegion());

        // Listen for realtime updates of the places
        db.collection("places").document(curRating.getPlaceid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if(error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        String placeName = value.getString("name");
                        Log.d(TAG, "curRating.placeName: " + placeName);

                        Map<String, Object> pictures = curRating.getPictures();
                        String filename = String.valueOf(pictures.get("filename"));
                        Log.d(TAG, "filename: " + filename);

                        ImageView tempPhoto = ratingViewModel.getSelectedRatingImage().getValue();
                        BitmapDrawable tempPhotoDrawable = (BitmapDrawable)tempPhoto.getDrawable();
                        if(tempPhotoDrawable != null) {
                            Bitmap bitmap = tempPhotoDrawable.getBitmap();
                            binding.imgViewPicture.setImageBitmap(bitmap);
                            binding.imgViewPicture.invalidate();
                        }

//                        binding.imgViewPicture.setImageResource();
//                        binding.imgViewPicture.setImageBitmap(bitmap);
                        binding.viewRatingTitle.setText(curRating.getTitle());
                        binding.viewRatingPlaceName.setText(placeName);
//                        binding.viewRatingRegion.setText(curRating.getRegion());
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
//                        ratingListAdapter.setRatingList(ratingList, placeList);
                    }
                });

        //event handler for edit button
        binding.btnEditRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}
