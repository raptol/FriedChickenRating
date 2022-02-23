package com.example.friedchickenrating.fragments.maps;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.friedchickenrating.R;
import com.example.friedchickenrating.fragments.ratings.Rating;
import com.example.friedchickenrating.fragments.ratings.RatingPlace;
import com.example.friedchickenrating.fragments.ratings.RatingViewModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.friedchickenrating.databinding.LayoutBottomSheetListItemBinding;
import com.example.friedchickenrating.databinding.FragmentBottomSheetDialogBinding;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BottomSheetFragment extends BottomSheetDialogFragment {

    // TODO: Customize parameter argument names
    public static final String TAG = BottomSheetFragment.class.getSimpleName();
    private static final String ARG_PLACE_ID = "place_id";
    private static final String ARG_PLACE_NAME = "place_name";
    private static final String ARG_PLACE_REGION = "place_region";

    private RatingViewModel ratingViewModel;
    private FragmentBottomSheetDialogBinding binding;

    List<Rating> ratingList;

    public static BottomSheetFragment newInstance(String placeid, String placename, String region) {
        final BottomSheetFragment fragment = new BottomSheetFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_PLACE_ID, placeid);
        args.putString(ARG_PLACE_NAME, placename);
        args.putString(ARG_PLACE_REGION, region);

        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBottomSheetDialogBinding.inflate(inflater, container, false);
        ratingViewModel = new ViewModelProvider(requireActivity()).get(RatingViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ratingList = new ArrayList<>();
        String placeid = getArguments().getString(ARG_PLACE_ID);
        String placename = getArguments().getString(ARG_PLACE_NAME);
        String region = getArguments().getString(ARG_PLACE_REGION);

        Log.d(TAG, "place_id:" + placeid);

        BottomSheetRatingAdapter bottomSheetRatingAdapter = new BottomSheetRatingAdapter(ratingList);

        final TextView txtPlaceName = (TextView) binding.txtPlaceName;
        final TextView txtPlaceRegion = (TextView) binding.txtPlaceRegion;
        final Button btnNewRatingFromMap = (Button) binding.btnNewRatingFromMap;
        final RecyclerView recyclerView = (RecyclerView) binding.recyclerViewBottomList;

        txtPlaceName.setText(placename);
        txtPlaceRegion.setText(region);

        btnNewRatingFromMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RatingPlace ratingPlace = ratingViewModel.getSelectedRatingPlace().getValue();
                Bundle result = new Bundle();
                result.putString("placeId", ratingPlace.getPlaceid());
                result.putString("placeName", ratingPlace.getName());
                result.putDouble("latitude", ratingPlace.getLatitude());
                result.putDouble("longitude", ratingPlace.getLongitude());
                result.putString("region", ratingPlace.getRegion());

                getParentFragmentManager().setFragmentResult("requestMapPlaceInfo", result);

                ratingViewModel.setSelectedRating(new Rating());//Rating initialize
                dismiss();
                NavHostFragment.findNavController(BottomSheetFragment.this)
                        .navigate(R.id.action_nav_maps_to_nav_newRating);
            }
        });

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        recyclerView.setAdapter(bottomSheetRatingAdapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("ratings")
            .whereEqualTo("placeid", "" + placeid)
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

                Log.d(TAG, "ratingList size: " + ratingList.size());
                bottomSheetRatingAdapter.setRatingList(ratingList);
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView imgItemPhoto;
        private TextView txtItemTitle;
        private RatingBar ratingBar;

        ViewHolder(LayoutBottomSheetListItemBinding binding) {
            super(binding.getRoot());

            imgItemPhoto = binding.imgItemPhoto;
            txtItemTitle = binding.txtItemTitle;
            ratingBar = binding.ratingBar;
        }

    }

    private class BottomSheetRatingAdapter extends RecyclerView.Adapter<ViewHolder> {

        private List<Rating> ratingList;

        BottomSheetRatingAdapter(List<Rating> ratingList) {
            this.ratingList = ratingList;
        }

        public void setRatingList(List<Rating> ratingList) {
            this.ratingList = ratingList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutBottomSheetListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
            Rating curRating = ratingList.get(position);

            holder.txtItemTitle.setText(curRating.getTitle());
            holder.ratingBar.setRating(curRating.getStaroverall());

            // download and display images
            Map<String, Object> pictures = curRating.getPictures();
            String filename = String.valueOf(pictures.get("filename"));

            Log.d(TAG, "filename: " + filename);

            if( !filename.isEmpty() && filename != null) {
                long size;
                final FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                StorageReference storageReference
                        = firebaseStorage.getReferenceFromUrl("gs://friedchickenrating.appspot.com/")
                        .child("images").child(filename);
                storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(task.getResult())
                                    .into((holder).imgItemPhoto);
                            (holder).imgItemPhoto.invalidate();
                        } else {
                            Toast.makeText(holder.itemView.getContext(),
                                    "Fail to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            holder.imgItemPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    ratingViewModel.setSelectedRating(curRating);
                    ratingViewModel.setSelectedRatingId(ratingList.get(position).getId());

                    dismiss();
                    NavHostFragment.findNavController(BottomSheetFragment.this)
                            .navigate(R.id.action_nav_maps_to_nav_viewRatings);
                }
            });
        }

        @Override
        public int getItemCount() {
            return ratingList.size();
        }
    }
}